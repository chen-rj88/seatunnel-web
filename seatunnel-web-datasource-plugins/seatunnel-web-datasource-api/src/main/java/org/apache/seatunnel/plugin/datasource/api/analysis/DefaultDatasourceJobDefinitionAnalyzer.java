package org.apache.seatunnel.plugin.datasource.api.analysis;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.plugin.datasource.api.utils.DataSourceUtils;
import org.apache.seatunnel.web.spi.enums.DbType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DefaultDatasourceJobDefinitionAnalyzer implements DatasourceJobDefinitionAnalyzer {

    private static final Pattern FROM_TABLE_PATTERN = Pattern.compile(
            "(?i)\\bfrom\\s+([`\"\\[]?[a-zA-Z0-9_.$-]+[`\"\\]]?)"
    );

    private static final Pattern INTO_TABLE_PATTERN = Pattern.compile(
            "(?i)\\binto\\s+([`\"\\[]?[a-zA-Z0-9_.$-]+[`\"\\]]?)"
    );

    private static final Pattern UPDATE_TABLE_PATTERN = Pattern.compile(
            "(?i)\\bupdate\\s+([`\"\\[]?[a-zA-Z0-9_.$-]+[`\"\\]]?)"
    );

    @Override
    public boolean supports(DatasourceAnalysisContext context) {
        return true;
    }

    @Override
    public DatasourceAnalysisResult analyze(DatasourceAnalysisContext context) {
        return DatasourceAnalysisResult.builder()
                .type(resolveType(context))
                .datasourceId(resolveDatasourceId(context))
                .objects(resolveObjects(context))
                .build();
    }

    private String resolveType(DatasourceAnalysisContext context) {
        if (StringUtils.isNotBlank(context.getDbType())) {
            return context.getDbType().trim();
        }

        String pluginName = StringUtils.trimToEmpty(context.getPluginName());
        if (StringUtils.isBlank(pluginName)) {
            pluginName = resolveWorkflowDbType(context.getWorkflowNode());
        }
        if (StringUtils.isBlank(pluginName)) {
            return "";
        }

        if (!"Jdbc".equalsIgnoreCase(pluginName)) {
            return pluginName;
        }

        String jdbcUrl = safeGetString(context.getPluginConfig(), "url");
        DbType dbType = DataSourceUtils.resolveDbTypeByJdbcUrl(jdbcUrl);
        if (dbType != null) {
            return dbType.name();
        }
        return pluginName;
    }

    private Long resolveDatasourceId(DatasourceAnalysisContext context) {
        if (context.getDatasourceId() != null) {
            return context.getDatasourceId();
        }

        Map<String, Object> node = context.getWorkflowNode();
        if (node == null || node.isEmpty()) {
            return null;
        }

        Map<String, Object> data = safeMap(node.get("data"));
        Map<String, Object> config = safeMap(data.get("config"));
        return parseLong(config.get("dataSourceId"));
    }

    private List<String> resolveObjects(DatasourceAnalysisContext context) {
        Set<String> objects = new LinkedHashSet<>();

        if (context.getPluginConfig() != null) {
            if (context.getRole() == DatasourceAnalysisRole.SINK) {
                objects.addAll(extractSinkObjects(context.getPluginConfig()));
            } else {
                objects.addAll(extractSourceObjects(context.getPluginConfig()));
            }
        }

        Map<String, Object> node = context.getWorkflowNode();
        if (node != null && !node.isEmpty()) {
            if (context.getRole() == DatasourceAnalysisRole.SINK) {
                objects.addAll(extractSinkObjects(node));
            } else {
                objects.addAll(extractSourceObjects(node));
            }
        }

        return new ArrayList<>(objects);
    }

    private String resolveWorkflowDbType(Map<String, Object> node) {
        if (node == null || node.isEmpty()) {
            return "";
        }
        Map<String, Object> data = safeMap(node.get("data"));
        Map<String, Object> config = safeMap(data.get("config"));
        return firstNonBlank(
                getString(data, "dbType"),
                getString(config, "dbType")
        );
    }

    private List<String> extractSourceObjects(Config config) {
        Set<String> objects = new LinkedHashSet<>();
        objects.addAll(extractTablesFromSourceQuery(safeGetString(config, "query")));
        addIfNotBlank(objects, safeGetString(config, "table_path"));
        addIfNotBlank(objects, safeGetString(config, "table"));
        addStringList(objects, config, "table_list");
        return new ArrayList<>(objects);
    }

    private List<String> extractSinkObjects(Config config) {
        Set<String> objects = new LinkedHashSet<>();
        objects.addAll(extractTablesFromSinkQuery(safeGetString(config, "query")));
        addIfNotBlank(objects, safeGetString(config, "table"));
        addIfNotBlank(objects, safeGetString(config, "table_path"));
        return new ArrayList<>(objects);
    }

    private List<String> extractSourceObjects(Map<String, Object> node) {
        Set<String> objects = new LinkedHashSet<>();
        Map<String, Object> data = safeMap(node.get("data"));
        Map<String, Object> config = safeMap(data.get("config"));
        addFirstNonBlank(objects, data, config, "sourceTable", "table", "sourceTableName");
        if (objects.isEmpty()) {
            objects.addAll(extractTablesFromSourceQuery(firstNonBlank(
                    getString(config, "sql"),
                    getString(data, "sql")
            )));
        }
        return new ArrayList<>(objects);
    }

    private List<String> extractSinkObjects(Map<String, Object> node) {
        Set<String> objects = new LinkedHashSet<>();
        Map<String, Object> data = safeMap(node.get("data"));
        Map<String, Object> config = safeMap(data.get("config"));
        addFirstNonBlank(objects, data, config, "sinkTableName", "targetTableName", "table", "targetTable");
        if (objects.isEmpty()) {
            objects.addAll(extractTablesFromSinkQuery(firstNonBlank(
                    getString(data, "sinkSql"),
                    getString(config, "sinkSql"),
                    getString(data, "sql"),
                    getString(config, "sql")
            )));
        }
        return new ArrayList<>(objects);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return java.util.Collections.emptyMap();
    }

    private String getString(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty() || key == null) {
            return "";
        }
        Object value = map.get(key);
        return value == null ? "" : StringUtils.trimToEmpty(String.valueOf(value));
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

    private void addFirstNonBlank(Set<String> objects, Map<String, Object> data,
                                  Map<String, Object> config, String... keys) {
        for (String key : keys) {
            String value = firstNonBlank(
                    getString(data, key),
                    getString(config, key)
            );
            if (StringUtils.isNotBlank(value)) {
                objects.add(normalizeObjectName(value));
                return;
            }
        }
    }

    private void addStringList(Set<String> objects, Config config, String path) {
        try {
            if (config != null && config.hasPath(path)) {
                List<String> tableList = config.getStringList(path);
                if (CollectionUtils.isNotEmpty(tableList)) {
                    for (String item : tableList) {
                        addIfNotBlank(objects, item);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Read datasource object list failed, path={}", path, e);
        }
    }

    private void addIfNotBlank(Set<String> objects, String value) {
        if (StringUtils.isNotBlank(value)) {
            objects.add(normalizeObjectName(value));
        }
    }

    private List<String> extractTablesFromSourceQuery(String query) {
        return extractTables(query, FROM_TABLE_PATTERN);
    }

    private List<String> extractTablesFromSinkQuery(String query) {
        Set<String> tables = new LinkedHashSet<>();
        tables.addAll(extractTables(query, INTO_TABLE_PATTERN));
        tables.addAll(extractTables(query, UPDATE_TABLE_PATTERN));
        tables.addAll(extractTables(query, FROM_TABLE_PATTERN));
        return new ArrayList<>(tables);
    }

    private List<String> extractTables(String query, Pattern pattern) {
        Set<String> tables = new LinkedHashSet<>();
        if (StringUtils.isBlank(query)) {
            return new ArrayList<>(tables);
        }
        Matcher matcher = pattern.matcher(query);
        while (matcher.find()) {
            addIfNotBlank(tables, matcher.group(1));
        }
        return new ArrayList<>(tables);
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

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            String text = StringUtils.trimToEmpty(String.valueOf(value));
            return StringUtils.isBlank(text) ? null : Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeObjectName(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("`") && value.endsWith("`"))
                || (value.startsWith("[") && value.endsWith("]"))) {
            value = value.substring(1, value.length() - 1);
        }
        return value.trim();
    }
}
