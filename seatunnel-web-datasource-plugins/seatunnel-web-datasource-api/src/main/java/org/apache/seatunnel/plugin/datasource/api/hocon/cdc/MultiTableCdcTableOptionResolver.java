package org.apache.seatunnel.plugin.datasource.api.hocon.cdc;

import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MultiTableCdcTableOptionResolver implements CdcTableOptionResolver {

    private static final String DATABASE = "database";
    private static final String TABLE_NAMES = "table-names";

    /**
     * Frontend / node config field.
     */
    private static final String MULTI_TABLE = "multiTable";
    private static final String SOURCE_TABLE_LIST = "source_table_list";
    private static final String TABLE_LIST = "table_list";
    private static final String TABLE_NAMES_NODE = "tableNames";

    @Override
    public boolean supports(Config node) {
        if (node == null) {
            return false;
        }

        if (isMultiTable(node)) {
            return true;
        }

        // 兜底：即使前端忘了传 multiTable，只要传了多表列表，也认为是多表模式
        return !getStringList(node, SOURCE_TABLE_LIST).isEmpty()
                || !getStringList(node, TABLE_LIST).isEmpty()
                || !getStringList(node, TABLE_NAMES_NODE).isEmpty();
    }

    @Override
    public void resolve(Config conn, Config node, Map<String, Object> map) {
        String database = getString(conn, DATABASE);
        List<String> tables = resolveTables(node);

        List<String> fullTableNames = buildFullTableNames(database, tables);
        if (!fullTableNames.isEmpty()) {
            map.put(TABLE_NAMES, fullTableNames);
        }
    }

    private boolean isMultiTable(Config node) {
        if (node == null || !node.hasPath(MULTI_TABLE)) {
            return false;
        }

        try {
            return node.getBoolean(MULTI_TABLE);
        } catch (Exception ignored) {
            String value = getString(node, MULTI_TABLE);
            return "true".equalsIgnoreCase(value);
        }
    }

    private List<String> resolveTables(Config node) {
        List<String> tables = getStringList(node, SOURCE_TABLE_LIST);
        if (!tables.isEmpty()) {
            return tables;
        }

        tables = getStringList(node, TABLE_LIST);
        if (!tables.isEmpty()) {
            return tables;
        }

        tables = getStringList(node, TABLE_NAMES_NODE);
        if (!tables.isEmpty()) {
            return tables;
        }

        return Collections.emptyList();
    }

    private List<String> buildFullTableNames(String database, List<String> tables) {
        List<String> result = new ArrayList<>();

        if (tables == null || tables.isEmpty()) {
            return result;
        }

        for (String table : tables) {
            if (StringUtils.isBlank(table)) {
                continue;
            }

            result.add(buildFullTableName(database, table));
        }

        return result;
    }

    private String buildFullTableName(String database, String table) {
        String trimmedTable = table.trim();

        if (trimmedTable.contains(".")) {
            return trimmedTable;
        }

        if (StringUtils.isBlank(database)) {
            return trimmedTable;
        }

        return database.trim() + "." + trimmedTable;
    }

    private List<String> getStringList(Config config, String key) {
        if (config == null || !config.hasPath(key)) {
            return Collections.emptyList();
        }

        Object value = config.getValue(key).unwrapped();
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }

        List<?> rawList = (List<?>) value;
        List<String> result = new ArrayList<>();

        for (Object item : rawList) {
            if (item == null) {
                continue;
            }

            String text = String.valueOf(item).trim();
            if (StringUtils.isNotBlank(text)) {
                result.add(text);
            }
        }

        return result;
    }

    private String getString(Config config, String key) {
        if (config == null || !config.hasPath(key)) {
            return null;
        }

        String value = config.getString(key);
        return StringUtils.isBlank(value) ? null : value.trim();
    }
}