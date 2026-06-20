package org.apache.seatunnel.plugin.datasource.api.analysis;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@Builder
public class DatasourceAnalysisResult {

    private String type;

    private Long datasourceId;

    @Builder.Default
    private List<String> objects = Collections.emptyList();
}
