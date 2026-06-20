package org.apache.seatunnel.plugin.datasource.mysql.analysis.single;

import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceAnalysisContext;
import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceAnalysisRole;
import org.apache.seatunnel.plugin.datasource.api.analysis.JobDefinitionAnalyzer;
import org.apache.seatunnel.plugin.datasource.mysql.analysis.MySQLJobDefinitionAnalyzeUtils;
import org.apache.seatunnel.web.common.enums.JobDefinitionMode;
import org.apache.seatunnel.web.common.modal.JobDefinitionAnalysisResult;

public class MySQLGuideSingleJobDefinitionAnalyzer implements JobDefinitionAnalyzer {

    @Override
    public boolean supports(DatasourceAnalysisContext context) {
        return context != null && context.getMode() == JobDefinitionMode.GUIDE_SINGLE;
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
        String tablePath = MySQLJobDefinitionAnalyzeUtils.safeGetString(
                context.getPluginConfig(),
                "table"
        );
        return MySQLJobDefinitionAnalyzeUtils.normalizeTable(tablePath);
    }

    private String resolveSinkTable(DatasourceAnalysisContext context) {
        String targetTableName = MySQLJobDefinitionAnalyzeUtils.safeGetString(
                context.getPluginConfig(),
                "targetTableName"
        );
        String table = MySQLJobDefinitionAnalyzeUtils.safeGetString(
                context.getPluginConfig(),
                "table"
        );
        return MySQLJobDefinitionAnalyzeUtils.normalizeTable(StringUtils.isNotBlank(targetTableName) ? targetTableName : table);
    }
}