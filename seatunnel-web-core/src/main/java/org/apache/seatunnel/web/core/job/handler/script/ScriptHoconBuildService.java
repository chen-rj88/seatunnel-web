package org.apache.seatunnel.web.core.job.handler.script;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.plugin.datasource.api.hocon.DataSourceHoconBuilder;
import org.apache.seatunnel.plugin.datasource.api.hocon.HoconBuildContext;
import org.apache.seatunnel.plugin.datasource.api.jdbc.DataSourceProcessor;
import org.apache.seatunnel.plugin.datasource.api.utils.DataSourceUtils;
import org.apache.seatunnel.web.common.config.ConfigValidator;
import org.apache.seatunnel.web.common.config.ReadonlyConfig;
import org.apache.seatunnel.web.common.enums.HoconBuildStage;
import org.apache.seatunnel.web.core.utils.SeaTunnelConfigUtil;
import org.apache.seatunnel.web.dao.entity.DataSource;
import org.apache.seatunnel.web.dao.repository.DataSourceDao;
import org.apache.seatunnel.web.spi.bean.dto.command.JobDefinitionSaveCommand;
import org.apache.seatunnel.web.spi.bean.dto.config.ScriptJobContent;
import org.apache.seatunnel.web.spi.enums.DbType;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ScriptHoconBuildService {

    private static final String SOURCE = "source";
    private static final String SINK = "sink";
    private static final String TRANSFORM = "transform";
    private static final String ENV = "env";

    private static final String KEY_DATASOURCE_ID = "datasourceId";
    private static final String KEY_DATA_SOURCE_ID = "dataSourceId";
    private static final String KEY_DATASOURCE_ID_UNDERLINE = "datasource_id";

    private static final String KEY_DB_TYPE = "dbType";
    private static final String KEY_DB_TYPE_UNDERLINE = "db_type";
    private static final String KEY_CONNECTOR_TYPE = "connectorType";
    private static final String KEY_PLUGIN_NAME = "pluginName";

    private final ScriptJobDefinitionParser scriptJobDefinitionParser;
    private final DataSourceDao dataSourceDao;

    public ScriptHoconBuildService(ScriptJobDefinitionParser scriptJobDefinitionParser,
                                   DataSourceDao dataSourceDao) {
        this.scriptJobDefinitionParser = scriptJobDefinitionParser;
        this.dataSourceDao = dataSourceDao;
    }

    public String build(ScriptJobContent content, JobDefinitionSaveCommand command) {
        if (content == null) {
            throw new IllegalArgumentException("content can not be null");
        }

        if (StringUtils.isBlank(content.getHoconContent())) {
            throw new IllegalArgumentException("hoconContent can not be blank");
        }

        Config rootConfig = scriptJobDefinitionParser.parseAndValidate(content.getHoconContent());

        validateRequiredBlocks(rootConfig);

        /*
         * SeaTunnelConfigUtil.generateConfig(...) already wraps these sections:
         *
         * env { ... }
         * source { ... }
         * transform { ... }
         * sink { ... }
         *
         * So here we only return the inner body of each section.
         */
        String envHocon = rootConfig.hasPath(ENV)
                ? renderConfigBody(rootConfig.getConfig(ENV))
                : "";

        String sourceHocon = buildPluginSectionBody(rootConfig, SOURCE);

        String transformHocon = rootConfig.hasPath(TRANSFORM)
                ? renderConfigBody(rootConfig.getConfig(TRANSFORM))
                : "";

        String sinkHocon = buildPluginSectionBody(rootConfig, SINK);

        return SeaTunnelConfigUtil.generateConfig(
                envHocon,
                sourceHocon,
                transformHocon,
                sinkHocon
        );
    }

    private void validateRequiredBlocks(Config config) {
        if (!config.hasPath(SOURCE)) {
            throw new IllegalArgumentException("hocon source can not be empty");
        }

        if (!config.hasPath(SINK)) {
            throw new IllegalArgumentException("hocon sink can not be empty");
        }
    }

    /**
     * Builds only the inner body of source/sink section.
     *
     * Correct:
     *
     * Jdbc {
     *     url = "..."
     * }
     *
     * Not:
     *
     * source {
     *     Jdbc {
     *         url = "..."
     *     }
     * }
     */
    private String buildPluginSectionBody(Config rootConfig, String section) {
        List<PluginConfig> plugins = scriptJobDefinitionParser.getPluginConfigs(rootConfig, section);

        if (plugins.isEmpty()) {
            throw new IllegalArgumentException("hocon " + section + " can not be empty");
        }

        StringBuilder builder = new StringBuilder();

        for (PluginConfig plugin : plugins) {
            Config builtPluginConfig;

            if (SOURCE.equals(section)) {
                builtPluginConfig = buildSourcePlugin(plugin);
            } else {
                builtPluginConfig = buildSinkPlugin(plugin);
            }

            /*
             * Some builders may already return:
             *
             * source {
             *     Jdbc { ... }
             * }
             *
             * But SeaTunnelConfigUtil will wrap source/sink again.
             * So unwrap single source/sink root if it exists.
             */
            Config normalizedConfig = unwrapSingleSectionIfNecessary(builtPluginConfig, section);

            /*
             * Do not append plugin.getPluginName() here.
             *
             * buildSourceHocon/buildSinkHocon already returns the real SeaTunnel plugin block,
             * such as Jdbc, MySQL-CDC, Kafka, etc.
             */
            builder.append(renderConfigBody(normalizedConfig)).append("\n");
        }

        return builder.toString().trim();
    }

    private Config buildSourcePlugin(PluginConfig plugin) {
        Long datasourceId = resolveDatasourceId(plugin.getConfig());
        DataSource dataSource = getRequiredDataSource(datasourceId, SOURCE);

        Config connectionConfig = parseConnectionConfig(dataSource);
        DbType dbType = resolveDbType(plugin, connectionConfig);

        DataSourceProcessor processor = DataSourceUtils.getDatasourceProcessor(dbType);
        if (processor == null) {
            throw new IllegalArgumentException("Can not find datasource processor, dbType=" + dbType);
        }

        String builderName = (plugin.getPluginName() + "-" + dbType).toUpperCase();
        DataSourceHoconBuilder hoconBuilder = processor.getQueryBuilder(builderName);
        if (hoconBuilder == null) {
            throw new IllegalArgumentException("Can not find hocon builder, pluginName=" + plugin.getPluginName());
        }

        if (!hoconBuilder.supportsSource()) {
            throw new IllegalArgumentException(plugin.getPluginName() + " does not support source side");
        }

        Config nodeConfig = removeWebOnlyFields(plugin.getConfig());

        HoconBuildContext buildContext = HoconBuildContext.builder()
                .connectionParam(dataSource.getConnectionParams())
                .connectionConfig(connectionConfig)
                .nodeConfig(nodeConfig)
                .stage(HoconBuildStage.INSTANCE)
                .build();

        Config sourceConfig = hoconBuilder.buildSourceHocon(buildContext);

        validateSourceConfig(processor, builderName, sourceConfig);

        return sourceConfig;
    }

    private Config buildSinkPlugin(PluginConfig plugin) {
        Long datasourceId = resolveDatasourceId(plugin.getConfig());
        DataSource dataSource = getRequiredDataSource(datasourceId, SINK);

        Config connectionConfig = parseConnectionConfig(dataSource);
        DbType dbType = resolveDbType(plugin, connectionConfig);

        DataSourceProcessor processor = DataSourceUtils.getDatasourceProcessor(dbType);
        if (processor == null) {
            throw new IllegalArgumentException("Can not find datasource processor, dbType=" + dbType);
        }

        String builderName = (plugin.getPluginName() + "-" + dbType).toUpperCase();
        DataSourceHoconBuilder hoconBuilder = processor.getQueryBuilder(builderName);
        if (hoconBuilder == null) {
            throw new IllegalArgumentException("Can not find hocon builder, pluginName=" + plugin.getPluginName());
        }

        if (!hoconBuilder.supportsSink()) {
            throw new IllegalArgumentException(plugin.getPluginName() + " does not support sink side");
        }

        Config nodeConfig = removeWebOnlyFields(plugin.getConfig());

        HoconBuildContext buildContext = HoconBuildContext.builder()
                .connectionParam(dataSource.getConnectionParams())
                .connectionConfig(connectionConfig)
                .nodeConfig(nodeConfig)
                .stage(HoconBuildStage.INSTANCE)
                .build();

        Config sinkConfig = hoconBuilder.buildSinkHocon(buildContext);

        validateSinkConfig(processor, sinkConfig);

        return sinkConfig;
    }

    private DataSource getRequiredDataSource(Long datasourceId, String role) {
        if (datasourceId == null) {
            throw new IllegalArgumentException(role + " datasourceId can not be empty");
        }

        DataSource dataSource = dataSourceDao.queryById(datasourceId);
        if (dataSource == null) {
            throw new IllegalArgumentException(
                    role + " data source does not exist, datasourceId=" + datasourceId);
        }

        return dataSource;
    }

    private Config parseConnectionConfig(DataSource dataSource) {
        if (dataSource == null || StringUtils.isBlank(dataSource.getConnectionParams())) {
            throw new IllegalArgumentException("datasource connection params can not be empty");
        }

        return ConfigFactory.parseString(dataSource.getConnectionParams()).resolve();
    }

    private DbType resolveDbType(PluginConfig plugin, Config connectionConfig) {
        String dbTypeText = firstNonBlank(
                safeGetString(plugin.getConfig(), KEY_DB_TYPE),
                safeGetString(plugin.getConfig(), KEY_DB_TYPE_UNDERLINE),
                safeGetString(connectionConfig, KEY_DB_TYPE),
                safeGetString(connectionConfig, KEY_DB_TYPE_UNDERLINE)
        );

        DbType dbType = parseDbType(dbTypeText);
        if (dbType != null) {
            return dbType;
        }

        String url = firstNonBlank(
                safeGetString(connectionConfig, "url"),
                safeGetString(connectionConfig, "jdbcUrl"),
                safeGetString(connectionConfig, "jdbc_url")
        );

        dbType = DataSourceUtils.resolveDbTypeByJdbcUrl(url);
        if (dbType != null) {
            return dbType;
        }

        dbType = parseDbType(plugin.getPluginName());
        if (dbType != null) {
            return dbType;
        }

        throw new IllegalArgumentException("Can not resolve dbType, pluginName=" + plugin.getPluginName());
    }

    private DbType parseDbType(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        String normalized = value.trim()
                .replace("-", "_")
                .toUpperCase();

        if (normalized.startsWith("JDBC_")) {
            normalized = normalized.substring("JDBC_".length());
        }

        try {
            return DbType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Long resolveDatasourceId(Config config) {
        String value = firstNonBlank(
                safeGetString(config, KEY_DATA_SOURCE_ID),
                safeGetString(config, KEY_DATASOURCE_ID),
                safeGetString(config, KEY_DATASOURCE_ID_UNDERLINE)
        );

        if (StringUtils.isBlank(value)) {
            return null;
        }

        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid datasourceId: " + value, e);
        }
    }

    private Config removeWebOnlyFields(Config config) {
        if (config == null) {
            return ConfigFactory.empty();
        }

        Map<String, Object> map = new LinkedHashMap<>(config.root().unwrapped());

        map.remove(KEY_DATA_SOURCE_ID);
        map.remove(KEY_DATASOURCE_ID);
        map.remove(KEY_DATASOURCE_ID_UNDERLINE);

        map.remove(KEY_DB_TYPE);
        map.remove(KEY_DB_TYPE_UNDERLINE);
        map.remove(KEY_CONNECTOR_TYPE);
        map.remove(KEY_PLUGIN_NAME);

        return ConfigFactory.parseMap(map).resolve();
    }

    private void validateSourceConfig(DataSourceProcessor processor,
                                      String pluginName,
                                      Config sourceConfig) {
        ConfigValidator.of(ReadonlyConfig.fromConfig(sourceConfig))
                .validate(processor.sourceOptionRule(pluginName));
    }

    private void validateSinkConfig(DataSourceProcessor processor,
                                    Config sinkConfig) {
        ConfigValidator.of(ReadonlyConfig.fromConfig(sinkConfig))
                .validate(processor.sinkOptionRule());
    }

    private String safeGetString(Config config, String path) {
        try {
            if (config != null && config.hasPath(path)) {
                return StringUtils.trimToEmpty(config.getString(path));
            }
        } catch (Exception e) {
            log.debug("Read config string failed, path={}", path, e);
        }
        return "";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }

        return "";
    }

    /**
     * Render config as section body only.
     *
     * Example:
     *
     * Input render:
     * {
     *     job {
     *         mode = "BATCH"
     *     }
     *     parallelism = 1
     * }
     *
     * Output:
     * job {
     *     mode = "BATCH"
     * }
     * parallelism = 1
     */
    private String renderConfigBody(Config config) {
        if (config == null || config.isEmpty()) {
            return "";
        }

        ConfigRenderOptions options = ConfigRenderOptions.defaults()
                .setComments(false)
                .setOriginComments(false)
                .setJson(false)
                .setFormatted(true);

        String rendered = config.root().render(options);

        return unwrapObject(rendered);
    }

    private String unwrapObject(String rendered) {
        if (StringUtils.isBlank(rendered)) {
            return "";
        }

        String text = rendered.trim();

        if (text.startsWith("{") && text.endsWith("}")) {
            return text.substring(1, text.length() - 1).trim();
        }

        return text;
    }

    /**
     * If builder result is:
     *
     * source {
     *     Jdbc { ... }
     * }
     *
     * unwrap it to:
     *
     * Jdbc { ... }
     */
    private Config unwrapSingleSectionIfNecessary(Config config, String section) {
        if (config == null || config.isEmpty()) {
            return ConfigFactory.empty();
        }

        try {
            if (config.root().keySet().size() == 1 && config.hasPath(section)) {
                return config.getConfig(section);
            }
        } catch (Exception ignored) {
            return config;
        }

        return config;
    }
}