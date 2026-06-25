package org.apache.seatunnel.plugin.datasource.mysql.analysis.script;

import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceAnalysisContext;
import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceAnalysisRole;
import org.apache.seatunnel.plugin.datasource.api.analysis.JobDefinitionAnalyzer;
import org.apache.seatunnel.plugin.datasource.mysql.analysis.MySQLJobDefinitionAnalyzeUtils;
import org.apache.seatunnel.web.common.enums.JobDefinitionMode;
import org.apache.seatunnel.web.common.modal.JobDefinitionAnalysisResult;

public class MySQLScriptJobDefinitionAnalyzer implements JobDefinitionAnalyzer {

    @Override
    public boolean supports(DatasourceAnalysisContext context) {
        return context != null && context.getMode() == JobDefinitionMode.SCRIPT;
    }

    @Override
    public JobDefinitionAnalysisResult analyze(DatasourceAnalysisContext context) {
        if (context == null) {
            return JobDefinitionAnalysisResult.builder().build();
        }

        String table;
        if (context.getRole() == DatasourceAnalysisRole.SOURCE) {
            table = resolveSourceTable(context);
        } else {
            table = resolveSinkTable(context);
        }

        return MySQLJobDefinitionAnalyzeUtils.buildResult(
                context.getRole(),
                context.getDatasourceId(),
                table
        );
    }

    private String resolveSourceTable(DatasourceAnalysisContext context) {
        return MySQLJobDefinitionAnalyzeUtils.normalizeTable(
                MySQLJobDefinitionAnalyzeUtils.firstNonBlank(
                        MySQLJobDefinitionAnalyzeUtils.safeGetString(context.getPluginConfig(), "table_path"),
                        MySQLJobDefinitionAnalyzeUtils.safeGetString(context.getPluginConfig(), "table"),
                        MySQLJobDefinitionAnalyzeUtils.safeGetString(context.getPluginConfig(), "table_name"),
                        MySQLJobDefinitionAnalyzeUtils.safeGetString(context.getPluginConfig(), "query")
                )
        );
    }

    private String resolveSinkTable(DatasourceAnalysisContext context) {
        return MySQLJobDefinitionAnalyzeUtils.normalizeTable(
                MySQLJobDefinitionAnalyzeUtils.firstNonBlank(
                        MySQLJobDefinitionAnalyzeUtils.safeGetString(context.getPluginConfig(), "table"),
                        MySQLJobDefinitionAnalyzeUtils.safeGetString(context.getPluginConfig(), "table_path"),
                        MySQLJobDefinitionAnalyzeUtils.safeGetString(context.getPluginConfig(), "targetTableName"),
                        MySQLJobDefinitionAnalyzeUtils.safeGetString(context.getPluginConfig(), "query")
                )
        );
    }
}