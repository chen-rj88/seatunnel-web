package org.apache.seatunnel.web.api.metrics.fetch;

import lombok.Data;

import java.util.Collections;
import java.util.Map;

@Data
public class EngineJobInfo {

    private Long clientId;

    private Long engineJobId;

    private String jobName;

    private String jobStatus;

    private Map<String, Object> rawJobInfo = Collections.emptyMap();

    private Map<String, Object> rawMetrics = Collections.emptyMap();

    public boolean hasMetrics() {
        return rawMetrics != null && !rawMetrics.isEmpty();
    }
}