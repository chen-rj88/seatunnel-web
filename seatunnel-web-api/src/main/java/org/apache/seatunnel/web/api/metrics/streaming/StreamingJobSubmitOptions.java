package org.apache.seatunnel.web.api.metrics.streaming;

import lombok.Getter;

/**
 * Options used when submitting a streaming job to SeaTunnel Zeta engine.
 */
@Getter
public class StreamingJobSubmitOptions {

    private final Long restoreEngineJobId;
    private final boolean startWithSavepoint;

    private StreamingJobSubmitOptions(Long restoreEngineJobId, boolean startWithSavepoint) {
        this.restoreEngineJobId = restoreEngineJobId;
        this.startWithSavepoint = startWithSavepoint;
    }

    public static StreamingJobSubmitOptions normal() {
        return new StreamingJobSubmitOptions(null, false);
    }

    public static StreamingJobSubmitOptions restoreFromSavepoint(Long restoreEngineJobId) {
        if (restoreEngineJobId == null || restoreEngineJobId <= 0) {
            throw new IllegalArgumentException("restoreEngineJobId must be positive");
        }
        return new StreamingJobSubmitOptions(restoreEngineJobId, true);
    }

    public String getEngineJobIdParam() {
        return restoreEngineJobId == null ? null : String.valueOf(restoreEngineJobId);
    }
}