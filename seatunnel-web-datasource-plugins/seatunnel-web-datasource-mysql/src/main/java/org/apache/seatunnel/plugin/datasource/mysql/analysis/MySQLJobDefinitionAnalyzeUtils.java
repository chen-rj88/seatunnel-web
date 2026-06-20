package org.apache.seatunnel.plugin.datasource.mysql.analysis;

import com.typesafe.config.Config;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceAnalysisRole;
import org.apache.seatunnel.web.common.modal.JobDefinitionAnalysisResult;
import org.apache.seatunnel.web.spi.enums.DbType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MySQLJobDefinitionAnalyzeUtils {

    public static JobDefinitionAnalysisResult buildResult(DatasourceAnalysisRole role,
                                                          Long datasourceId,
                                                          String table) {
        if (role == DatasourceAnalysisRole.SOURCE) {
            return JobDefinitionAnalysisResult.builder()
                    .sourceType(DbType.MYSQL.name())
                    .sourceDatasourceId(datasourceId)
                    .sourceTable(StringUtils.trimToEmpty(table))
                    .build();
        }

        return JobDefinitionAnalysisResult.builder()
                .sinkType(DbType.MYSQL.name())
                .sinkDatasourceId(datasourceId)
                .sinkTable(StringUtils.trimToEmpty(table))
                .build();
    }

    public static String safeGetString(Config config, String path) {
        try {
            if (config != null && config.hasPath(path)) {
                return StringUtils.trimToEmpty(config.getString(path));
            }
        } catch (Exception e) {
            log.debug("Read mysql job definition config failed, path={}", path, e);
        }
        return "";
    }

    public static List<String> safeGetStringList(Config config, String path) {
        try {
            if (config != null && config.hasPath(path)) {
                return cleanTables(config.getStringList(path));
            }
        } catch (Exception e) {
            log.debug("Read mysql job definition table list failed, path={}", path, e);
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> safeMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static List<String> getStringList(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty() || key == null) {
            return Collections.emptyList();
        }

        Object value = map.get(key);
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }

        List<Object> values = (List<Object>) value;
        List<String> result = new ArrayList<>();

        for (Object item : values) {
            if (item != null && StringUtils.isNotBlank(String.valueOf(item))) {
                result.add(normalizeTable(String.valueOf(item)));
            }
        }

        return result;
    }

    public static String getString(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty() || key == null) {
            return "";
        }

        Object value = map.get(key);
        return value == null ? "" : StringUtils.trimToEmpty(String.valueOf(value));
    }

    public static String firstNonBlank(String... values) {
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

    public static List<String> cleanTables(List<String> tables) {
        if (CollectionUtils.isEmpty(tables)) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (String table : tables) {
            if (StringUtils.isNotBlank(table)) {
                result.add(normalizeTable(table));
            }
        }

        return result;
    }

    public static List<String> distinct(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.isNotBlank(value) && !result.contains(value.trim())) {
                result.add(value.trim());
            }
        }

        return result;
    }

    public static String normalizeTable(String raw) {
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