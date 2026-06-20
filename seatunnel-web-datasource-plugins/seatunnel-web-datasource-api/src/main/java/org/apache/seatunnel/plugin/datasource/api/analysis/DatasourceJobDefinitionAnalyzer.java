package org.apache.seatunnel.plugin.datasource.api.analysis;

public interface DatasourceJobDefinitionAnalyzer {

    boolean supports(DatasourceAnalysisContext context);

    DatasourceAnalysisResult analyze(DatasourceAnalysisContext context);
}
