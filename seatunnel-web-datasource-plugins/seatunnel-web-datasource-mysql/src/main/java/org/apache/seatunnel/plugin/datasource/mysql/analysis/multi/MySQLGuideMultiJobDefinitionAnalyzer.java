package org.apache.seatunnel.plugin.datasource.mysql.analysis.multi;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceAnalysisContext;
import org.apache.seatunnel.plugin.datasource.api.analysis.JobDefinitionAnalyzer;
import org.apache.seatunnel.plugin.datasource.mysql.analysis.MySQLJobDefinitionAnalyzeUtils;
import org.apache.seatunnel.web.common.enums.JobDefinitionMode;
import org.apache.seatunnel.web.common.modal.JobDefinitionAnalysisResult;
import org.apache.seatunnel.web.common.utils.JSONUtils;
import org.apache.seatunnel.web.spi.bean.dto.config.GuideMultiJobContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MySQLGuideMultiJobDefinitionAnalyzer implements JobDefinitionAnalyzer {

    @Override
    public boolean supports(DatasourceAnalysisContext context) {
        return context != null && context.getMode() == JobDefinitionMode.GUIDE_MULTI;
    }

    @Override
    public JobDefinitionAnalysisResult analyze(DatasourceAnalysisContext context) {
        if (context == null) {
            return JobDefinitionAnalysisResult.builder().build();
        }

        List<String> tableList = resolveSourceTableList(context);
        String tableJson = JSONUtils.toJsonString(tableList);

        /*
         * 多表模式：
         * source 端取 table_list
         * sink 端也直接复用 source 端 table_list
         */
        return MySQLJobDefinitionAnalyzeUtils.buildResult(
                context.getRole(),
                context.getDatasourceId(),
                tableJson
        );
    }

    private List<String> resolveSourceTableList(DatasourceAnalysisContext context) {
        List<String> result = new ArrayList<>();

        result.addAll(MySQLJobDefinitionAnalyzeUtils.safeGetStringList(
                context.getPluginConfig(),
                "table_list"
        ));
        result.addAll(MySQLJobDefinitionAnalyzeUtils.safeGetStringList(
                context.getPluginConfig(),
                "tableList"
        ));

        Map<String, Object> node = context.getWorkflowNode();
        if (node != null && !node.isEmpty()) {
            Map<String, Object> data = MySQLJobDefinitionAnalyzeUtils.safeMap(node.get("data"));
            Map<String, Object> config = MySQLJobDefinitionAnalyzeUtils.safeMap(data.get("config"));

            result.addAll(MySQLJobDefinitionAnalyzeUtils.getStringList(config, "table_list"));
            result.addAll(MySQLJobDefinitionAnalyzeUtils.getStringList(config, "tableList"));
            result.addAll(MySQLJobDefinitionAnalyzeUtils.getStringList(data, "table_list"));
            result.addAll(MySQLJobDefinitionAnalyzeUtils.getStringList(data, "tableList"));
        }

        /*
         * GUIDE_MULTI 页面结构里，多表一般在 tableMatch.tables。
         * sink 端也直接复用这个表列表。
         */
        if (CollectionUtils.isEmpty(result)
                && context.getRawContent() instanceof GuideMultiJobContent) {
            GuideMultiJobContent content = (GuideMultiJobContent) context.getRawContent();
            if (content.getTableMatch() != null) {
                result.addAll(
                        MySQLJobDefinitionAnalyzeUtils.cleanTables(
                                content.getTableMatch().getTables()
                        )
                );
            }
        }

        return MySQLJobDefinitionAnalyzeUtils.distinct(result);
    }
}