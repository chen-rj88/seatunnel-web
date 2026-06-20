package org.apache.seatunnel.plugin.datasource.api.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class DatasourceJobDefinitionAnalyzerRegistry {

    private final List<DatasourceJobDefinitionAnalyzer> analyzers;
    private final DefaultDatasourceJobDefinitionAnalyzer defaultAnalyzer;

    public DatasourceJobDefinitionAnalyzerRegistry() {
        this(new DefaultDatasourceJobDefinitionAnalyzer());
    }

    public DatasourceJobDefinitionAnalyzerRegistry(DefaultDatasourceJobDefinitionAnalyzer defaultAnalyzer) {
        this(loadAnalyzers(), defaultAnalyzer);
    }

    public DatasourceJobDefinitionAnalyzerRegistry(List<DatasourceJobDefinitionAnalyzer> analyzers,
                                                   DefaultDatasourceJobDefinitionAnalyzer defaultAnalyzer) {
        this.analyzers = analyzers;
        this.defaultAnalyzer = defaultAnalyzer;
    }

    public DatasourceAnalysisResult analyze(DatasourceAnalysisContext context) {
        for (DatasourceJobDefinitionAnalyzer analyzer : analyzers) {
            if (analyzer != defaultAnalyzer && analyzer.supports(context)) {
                return analyzer.analyze(context);
            }
        }
        return defaultAnalyzer.analyze(context);
    }

    private static List<DatasourceJobDefinitionAnalyzer> loadAnalyzers() {
        List<DatasourceJobDefinitionAnalyzer> result = new ArrayList<>();
        ServiceLoader<DatasourceJobDefinitionAnalyzer> loader = ServiceLoader.load(DatasourceJobDefinitionAnalyzer.class);
        for (DatasourceJobDefinitionAnalyzer analyzer : loader) {
            result.add(analyzer);
        }
        return result;
    }
}
