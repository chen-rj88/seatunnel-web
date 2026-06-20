package org.apache.seatunnel.web.core.job.handler.single;

import org.apache.seatunnel.web.common.enums.JobDefinitionMode;
import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceAnalysisContext;
import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceAnalysisResult;
import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceAnalysisRole;
import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceJobDefinitionAnalyzerRegistry;
import org.apache.seatunnel.web.core.job.model.JobDefinitionAnalysisResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GuideSingleWorkflowAnalyzer {

    private final DatasourceJobDefinitionAnalyzerRegistry datasourceAnalyzerRegistry;

    public GuideSingleWorkflowAnalyzer(DatasourceJobDefinitionAnalyzerRegistry datasourceAnalyzerRegistry) {
        this.datasourceAnalyzerRegistry = datasourceAnalyzerRegistry;
    }

    public JobDefinitionAnalysisResult analyze(Object workflowObj) {
        Map<String, Object> workflow = WorkflowNodeHelper.safeMap(workflowObj);

        Map<String, Object> sourceNode = WorkflowNodeHelper.findFirstNodeByType(workflow, "source");
        Map<String, Object> sinkNode = WorkflowNodeHelper.findFirstNodeByType(workflow, "sink");

        DatasourceAnalysisResult sourceAnalysis = datasourceAnalyzerRegistry.analyze(
                buildContext(DatasourceAnalysisRole.SOURCE, sourceNode, workflowObj)
        );
        DatasourceAnalysisResult sinkAnalysis = datasourceAnalyzerRegistry.analyze(
                buildContext(DatasourceAnalysisRole.SINK, sinkNode, workflowObj)
        );

        return JobDefinitionAnalysisResult.builder()
                .sourceType(sourceAnalysis.getType())
                .sinkType(sinkAnalysis.getType())
                .sourceDatasourceId(sourceAnalysis.getDatasourceId())
                .sinkDatasourceId(sinkAnalysis.getDatasourceId())
                .sourceTable(firstObject(sourceAnalysis.getObjects()))
                .sinkTable(firstObject(sinkAnalysis.getObjects()))
                .build();
    }

    private DatasourceAnalysisContext buildContext(DatasourceAnalysisRole role,
                                                   Map<String, Object> node,
                                                   Object workflowObj) {
        return DatasourceAnalysisContext.builder()
                .mode(JobDefinitionMode.GUIDE_SINGLE)
                .role(role)
                .workflowNode(node)
                .rawContent(workflowObj)
                .build();
    }

    private String firstObject(List<String> objects) {
        if (objects == null || objects.isEmpty()) {
            return "";
        }
        return objects.get(0);
    }
}
