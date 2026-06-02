package org.apache.seatunnel.web.api.metrics.streaming;

import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.web.api.service.StreamingJobInstanceService;
import org.apache.seatunnel.web.api.utils.JobUtils;
import org.apache.seatunnel.web.common.enums.JobResult;
import org.apache.seatunnel.web.common.enums.JobStatus;
import org.apache.seatunnel.web.dao.entity.StreamingJobInstance;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class StreamingJobResultHandler {

    private final StreamingJobInstanceService streamingJobInstanceService;
    private final StreamingJobMetricsMonitor streamingJobMetricsMonitor;

    public StreamingJobResultHandler(StreamingJobInstanceService streamingJobInstanceService,
                                     StreamingJobMetricsMonitor streamingJobMetricsMonitor) {
        this.streamingJobInstanceService = streamingJobInstanceService;
        this.streamingJobMetricsMonitor = streamingJobMetricsMonitor;
    }

    public void handleSuccess(Long jobInstanceId) {
        updateStatus(jobInstanceId, JobStatus.FINISHED, null);
        log.info("Streaming job completed successfully. instanceId={}", jobInstanceId);
    }

    public void handleFailure(Long jobInstanceId, Throwable error) {
        String message = JobUtils.getJobInstanceErrorMessage(error.getMessage());

        updateStatus(jobInstanceId, JobStatus.FAILED, message);
        log.error("Streaming job failed. instanceId={}, error={}", jobInstanceId, message, error);
    }

    public void handleFailure(Long jobInstanceId, JobResult jobResult) {
        String message = jobResult != null ? jobResult.getError() : "Unknown error";

        updateStatus(jobInstanceId, JobStatus.FAILED, message);

        log.error(
                "Streaming job failed. instanceId={}, status={}, error={}",
                jobInstanceId,
                jobResult != null ? jobResult.getStatus() : "null",
                message
        );
    }

    public void updateEngineId(Long instanceId, Long engineId) {
        StreamingJobInstance po = new StreamingJobInstance();
        po.setId(instanceId);
        po.setEngineJobId(engineId);
        po.setSubmitTime(new Date());
        po.setStartTime(new Date());
        po.setJobStatus(JobStatus.RUNNING);

        streamingJobInstanceService.updateById(po);

        log.info("Streaming job submitted. instanceId={}, engineId={}", instanceId, engineId);
    }

    private void updateStatus(Long jobInstanceId, JobStatus status, String errorMessage) {
        StreamingJobInstance po = new StreamingJobInstance();
        po.setId(jobInstanceId);
        po.setJobStatus(status);
        po.setEndTime(new Date());
        po.setErrorMessage(errorMessage);

        streamingJobInstanceService.updateById(po);

        streamingJobMetricsMonitor.finalizeAndPersist(jobInstanceId, status.name());
    }
}