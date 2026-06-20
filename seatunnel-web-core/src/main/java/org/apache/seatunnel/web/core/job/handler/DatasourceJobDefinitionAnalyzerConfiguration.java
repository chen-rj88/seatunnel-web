package org.apache.seatunnel.web.core.job.handler;

import org.apache.seatunnel.plugin.datasource.api.analysis.DatasourceJobDefinitionAnalyzerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatasourceJobDefinitionAnalyzerConfiguration {

    @Bean
    public DatasourceJobDefinitionAnalyzerRegistry datasourceJobDefinitionAnalyzerRegistry() {
        return new DatasourceJobDefinitionAnalyzerRegistry();
    }
}
