package org.apache.seatunnel.plugin.datasource.api.hocon.table;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.seatunnel.web.common.enums.HoconBuildStage;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.seatunnel.plugin.datasource.api.hocon.JdbcBatchConstants.DATABASE;
import static org.apache.seatunnel.plugin.datasource.api.hocon.JdbcBatchConstants.QUERY;
import static org.apache.seatunnel.plugin.datasource.api.hocon.JdbcBatchConstants.TABLE_LIST;
import static org.apache.seatunnel.plugin.datasource.api.hocon.JdbcBatchConstants.TABLE_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JdbcSourceTargetBuilderTest {

    private final JdbcSingleSourceTargetBuilder singleBuilder =
            new JdbcSingleSourceTargetBuilder(
                    new JdbcTableNameResolver(),
                    (query, context) -> query);
    private final JdbcMultiSourceTargetBuilder multiBuilder =
            new JdbcMultiSourceTargetBuilder(new JdbcTableNameResolver());

    @Test
    void singlePgTablePathIncludesDatabaseFromConnection() {
        Map<String, Object> map = new HashMap<>();

        singleBuilder.build(
                config("table_path = \"st_src.issue108_user\""),
                pgConnection("database = st_test"),
                map,
                HoconBuildStage.DEFINITION);

        assertEquals("st_test", map.get(DATABASE));
        assertEquals("st_test.st_src.issue108_user", map.get(TABLE_PATH));
    }

    @Test
    void singlePgTableNameUsesSchemaNameFromConnection() {
        Map<String, Object> map = new HashMap<>();

        singleBuilder.build(
                config("table = \"issue108_user\""),
                pgConnection("database = st_test\nschemaName = st_src"),
                map,
                HoconBuildStage.DEFINITION);

        assertEquals("st_test", map.get(DATABASE));
        assertEquals("st_test.st_src.issue108_user", map.get(TABLE_PATH));
    }

    @Test
    void singleMysqlDatabaseQualifiedTablePathIsKept() {
        Map<String, Object> map = new HashMap<>();

        singleBuilder.build(
                config("table_path = \"st_src.issue108_user\""),
                mysqlConnection("database = st_test"),
                map,
                HoconBuildStage.DEFINITION);

        assertEquals("st_test", map.get(DATABASE));
        assertEquals("st_src.issue108_user", map.get(TABLE_PATH));
    }

    @Test
    @SuppressWarnings("unchecked")
    void multiPgTableListIncludesDatabaseFromConnection() {
        Map<String, Object> map = new HashMap<>();

        multiBuilder.build(
                config("table_list = [\"st_src.issue108_user\", \"st_src.issue108_role\"]"),
                pgConnection("database = st_test"),
                map,
                HoconBuildStage.DEFINITION);

        List<Map<String, Object>> tableList = (List<Map<String, Object>>) map.get(TABLE_LIST);
        assertEquals("st_test", map.get(DATABASE));
        assertEquals("st_test.st_src.issue108_user", tableList.get(0).get(TABLE_PATH));
        assertEquals("st_test.st_src.issue108_role", tableList.get(1).get(TABLE_PATH));
    }

    @Test
    void querySourceDoesNotIncludeDatabase() {
        Map<String, Object> map = new HashMap<>();

        singleBuilder.build(
                config("sql = \"select * from st_src.issue108_user\""),
                pgConnection("database = st_test"),
                map,
                HoconBuildStage.DEFINITION);

        assertEquals("select * from st_src.issue108_user", map.get(QUERY));
        assertFalse(map.containsKey(DATABASE));
    }

    private Config config(String body) {
        return ConfigFactory.parseString(body);
    }

    private Config pgConnection(String body) {
        return ConfigFactory.parseString(
                "url = \"jdbc:postgresql://postgres:5432/st_test?ssl=false\"\n"
                        + "driver = \"org.postgresql.Driver\"\n"
                        + "user = st\n"
                        + "password = st_pass\n"
                        + body);
    }

    private Config mysqlConnection(String body) {
        return ConfigFactory.parseString(
                "url = \"jdbc:mysql://mysql:3306/st_test\"\n"
                        + "driver = \"com.mysql.cj.jdbc.Driver\"\n"
                        + "user = st\n"
                        + "password = st_pass\n"
                        + body);
    }
}
