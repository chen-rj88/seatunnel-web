package org.apache.seatunnel.web.core.job.handler.multi;

import org.apache.seatunnel.web.common.enums.JobDefinitionMode;
import org.apache.seatunnel.web.common.utils.JSONUtils;
import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceAnalysisContext;
import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceAnalysisResult;
import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceAnalysisRole;
import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceJobDefinitionAnalyzerRegistry;
import org.apache.seatunnel.web.core.job.model.JobDefinitionAnalysisResult;
import org.apache.seatunnel.web.spi.bean.dto.config.GuideMultiJobContent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GuideMultiJobAnalyzer {

    private final GuideMultiTableMatchResolver tableMatchResolver;
    private final DatasourceJobDefinitionAnalyzerRegistry datasourceAnalyzerRegistry;

    public GuideMultiJobAnalyzer(GuideMultiTableMatchResolver tableMatchResolver,
                                 DatasourceJobDefinitionAnalyzerRegistry datasourceAnalyzerRegistry) {
        this.tableMatchResolver = tableMatchResolver;
        this.datasourceAnalyzerRegistry = datasourceAnalyzerRegistry;
    }

    public JobDefinitionAnalysisResult analyze(GuideMultiJobContent content) {
        List<String> sourceTables = tableMatchResolver.resolveSourceTables(content);
        List<String> sinkTables = tableMatchResolver.resolveSinkTables(content);
        DatasourceAnalysisResult sourceAnalysis = datasourceAnalyzerRegistry.analyze(buildSourceContext(content));
        DatasourceAnalysisResult sinkAnalysis = datasourceAnalyzerRegistry.analyze(buildSinkContext(content));

        return JobDefinitionAnalysisResult.builder()
                .sourceType(sourceAnalysis.getType())
                .sinkType(sinkAnalysis.getType())
                .sourceDatasourceId(sourceAnalysis.getDatasourceId())
                .sinkDatasourceId(sinkAnalysis.getDatasourceId())
                .sourceTable(JSONUtils.toJsonString(sourceTables))
                .sinkTable(JSONUtils.toJsonString(sinkTables))
                .build();
    }

    private DatasourceAnalysisContext buildSourceContext(GuideMultiJobContent content) {
        GuideMultiJobContent.WorkflowSourceConfig source = content == null ? null : content.getSource();
        return DatasourceAnalysisContext.builder()
                .mode(JobDefinitionMode.GUIDE_MULTI)
                .role(DatasourceAnalysisRole.SOURCE)
                .dbType(source == null ? "" : source.getDbType())
                .pluginName(resolveSourcePluginName(source))
                .datasourceId(parseLong(source == null ? null : source.getDatasourceId()))
                .rawContent(content)
                .build();
    }

    private DatasourceAnalysisContext buildSinkContext(GuideMultiJobContent content) {
        GuideMultiJobContent.WorkflowTargetConfig target = content == null ? null : content.getTarget();
        return DatasourceAnalysisContext.builder()
                .mode(JobDefinitionMode.GUIDE_MULTI)
                .role(DatasourceAnalysisRole.SINK)
                .dbType(target == null ? "" : target.getDbType())
                .pluginName(resolveTargetPluginName(target))
                .datasourceId(parseLong(target == null ? null : target.getDatasourceId()))
                .rawContent(content)
                .build();
    }

    private String resolveSourcePluginName(GuideMultiJobContent.WorkflowSourceConfig source) {
        if (source == null) {
            return "";
        }
        return source.getPluginName() == null ? source.getConnectorType() : source.getPluginName();
    }

    private String resolveTargetPluginName(GuideMultiJobContent.WorkflowTargetConfig target) {
        if (target == null) {
            return "";
        }
        return target.getPluginName() == null ? target.getConnectorType() : target.getPluginName();
    }

    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
