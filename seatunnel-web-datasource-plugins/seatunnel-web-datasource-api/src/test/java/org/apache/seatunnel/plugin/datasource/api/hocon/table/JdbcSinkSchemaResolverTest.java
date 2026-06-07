package org.apache.seatunnel.plugin.datasource.api.hocon.table;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.apache.seatunnel.plugin.datasource.api.hocon.JdbcBatchConstants.DATABASE;
import static org.apache.seatunnel.plugin.datasource.api.hocon.JdbcBatchConstants.GENERATE_SINK_SQL;
import static org.apache.seatunnel.plugin.datasource.api.hocon.JdbcBatchConstants.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcSinkSchemaResolverTest {

    private final JdbcSingleSinkTargetBuilder singleBuilder =
            new JdbcSingleSinkTargetBuilder(new JdbcTableNameResolver());
    private final JdbcMultiSinkTargetBuilder multiBuilder = new JdbcMultiSinkTargetBuilder();

    @Test
    void singlePgSinkUsesSchemaNameFromConnection() {
        Map<String, Object> map = new HashMap<>();

        singleBuilder.build(
                config("targetTableName = user_info"),
                pgConnection("database = test_db\nschemaName = public"),
                map);

        assertEquals("test_db", map.get(DATABASE));
        assertEquals("public.user_info", map.get(TABLE));
        assertEquals(true, map.get(GENERATE_SINK_SQL));
    }

    @Test
    void singlePgSinkRewritesDatabaseQualifiedTargetTableToSchemaTable() {
        Map<String, Object> map = new HashMap<>();

        singleBuilder.build(
                config("targetTableName = \"test_db.user_info\""),
                pgConnection("database = test_db\nschemaName = public"),
                map);

        assertEquals("test_db", map.get(DATABASE));
        assertEquals("public.user_info", map.get(TABLE));
    }

    @Test
    void singlePgSinkKeepsAlreadySchemaQualifiedTable() {
        Map<String, Object> map = new HashMap<>();

        singleBuilder.build(
                config("targetTableName = \"public.user_info\""),
                pgConnection("database = test_db\nschemaName = public"),
                map);

        assertEquals("public.user_info", map.get(TABLE));
    }

    @Test
    void multiPgSinkUsesSchemaNameInDefaultTablePattern() {
        Map<String, Object> map = new HashMap<>();

        multiBuilder.build(
                config("multiTable = true"),
                pgConnection("database = test_db\nschemaName = public"),
                map);

        assertEquals("test_db", map.get(DATABASE));
        assertEquals("public.${table_name}", map.get(TABLE));
    }

    @Test
    void multiPgSinkKeepsExplicitTablePattern() {
        Map<String, Object> map = new HashMap<>();

        multiBuilder.build(
                config("multiTable = true\ntablePattern = \"custom_${table_name}\""),
                pgConnection("database = test_db\nschemaName = public"),
                map);

        assertEquals("custom_${table_name}", map.get(TABLE));
    }

    @Test
    void sinkKeepsOriginalTableWithoutSchemaName() {
        Map<String, Object> singleMap = new HashMap<>();
        singleBuilder.build(
                config("targetTableName = user_info"),
                pgConnection("database = test_db"),
                singleMap);

        Map<String, Object> multiMap = new HashMap<>();
        multiBuilder.build(
                config("multiTable = true"),
                pgConnection("database = test_db"),
                multiMap);

        assertEquals("user_info", singleMap.get(TABLE));
        assertEquals("${table_name}", multiMap.get(TABLE));
    }

    @Test
    void nonPostgresSinkIgnoresSchemaName() {
        Map<String, Object> map = new HashMap<>();

        singleBuilder.build(
                config("targetTableName = user_info"),
                mysqlConnection("database = test_db\nschemaName = public"),
                map);

        assertEquals("user_info", map.get(TABLE));
    }

    private Config config(String body) {
        return ConfigFactory.parseString(body);
    }

    private Config pgConnection(String body) {
        return ConfigFactory.parseString(
                "url = \"jdbc:postgresql://localhost:5432/test_db\"\n"
                        + "user = test\n"
                        + body);
    }

    private Config mysqlConnection(String body) {
        return ConfigFactory.parseString(
                "url = \"jdbc:mysql://localhost:3306/test_db\"\n"
                        + "user = test\n"
                        + body);
    }
}
