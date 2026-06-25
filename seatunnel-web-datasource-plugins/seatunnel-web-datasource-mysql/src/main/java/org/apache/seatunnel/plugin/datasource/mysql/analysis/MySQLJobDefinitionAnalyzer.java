package org.apache.seatunnel.plugin.datasource.mysql.analysis;

import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceAnalysisContext;
import org.apache.seatunnel.plugin.datasource.api.analysis.JobDefinitionAnalyzer;
import org.apache.seatunnel.plugin.datasource.mysql.analysis.multi.MySQLGuideMultiJobDefinitionAnalyzer;
import org.apache.seatunnel.plugin.datasource.mysql.analysis.script.MySQLScriptJobDefinitionAnalyzer;
import org.apache.seatunnel.plugin.datasource.mysql.analysis.single.MySQLGuideSingleJobDefinitionAnalyzer;
import org.apache.seatunnel.web.common.enums.JobDefinitionMode;
import org.apache.seatunnel.web.common.modal.JobDefinitionAnalysisResult;

public class MySQLJobDefinitionAnalyzer implements JobDefinitionAnalyzer {

    private final JobDefinitionAnalyzer singleAnalyzer = new MySQLGuideSingleJobDefinitionAnalyzer();
    private final JobDefinitionAnalyzer multiAnalyzer = new MySQLGuideMultiJobDefinitionAnalyzer();
    private final JobDefinitionAnalyzer scriptAnalyzer = new MySQLScriptJobDefinitionAnalyzer();

    @Override
    public boolean supports(DatasourceAnalysisContext context) {
        return true;
    }

    @Override
    public JobDefinitionAnalysisResult analyze(DatasourceAnalysisContext context) {
        if (context == null) {
            return JobDefinitionAnalysisResult.builder().build();
        }

        if (context.getMode() == JobDefinitionMode.GUIDE_SINGLE) {
            return singleAnalyzer.analyze(context);
        }

        if (context.getMode() == JobDefinitionMode.GUIDE_MULTI) {
            return multiAnalyzer.analyze(context);
        }

        return scriptAnalyzer.analyze(context);
    }
}