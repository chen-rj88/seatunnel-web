package org.apache.seatunnel.web.api.metrics.streaming;

import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.web.api.metrics.JobFileLogger;
import org.apache.seatunnel.web.api.metrics.JobRuntimeContext;
import org.apache.seatunnel.web.api.metrics.streaming.model.StreamingParsedJobMetrics;
import org.apache.seatunnel.web.api.service.StreamingJobInstanceService;
import org.apache.seatunnel.web.api.service.StreamingJobMetricsService;
import org.apache.seatunnel.web.api.websocket.WorkflowWebSocketService;
import org.apache.seatunnel.web.spi.bean.vo.JobInstanceVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class StreamingJobMetricsMonitor {

    private final Map<Long, JobRuntimeContext> monitoringJobs = new ConcurrentHashMap<>();

    private final Map<Long, MonitorState> monitorStates = new ConcurrentHashMap<>();

    private final Map<Long, JobFileLogger> loggers = new ConcurrentHashMap<>();

    @Value("${seatunnel.streaming.metrics.max-empty-times:30}")
    private int maxEmptyTimes;

    @Value("${seatunnel.streaming.metrics.max-error-times:30}")
    private int maxErrorTimes;

    @Value("${seatunnel.streaming.metrics.log-every-times:10}")
    private int logEveryTimes;

    @Resource
    private StreamingJobMetricsService streamingJobMetricsService;

    @Resource
    private StreamingJobInstanceService streamingJobInstanceService;

    @Resource
    private WorkflowWebSocketService webSocketService;

    public void register(JobRuntimeContext context) {
        if (context == null || context.getInstanceId() == null) {
            return;
        }

        Long instanceId = context.getInstanceId();

        monitoringJobs.put(instanceId, context);
        monitorStates.put(instanceId, MonitorState.create());

        try {
            JobInstanceVO instance = streamingJobInstanceService.selectById(instanceId);
            if (instance != null && instance.getLogPath() != null) {
                loggers.put(instanceId, new JobFileLogger(instance.getLogPath()));
            }
        } catch (Exception e) {
            log.warn("Create streaming metrics logger failed, instanceId={}", instanceId, e);
        }

        log.info("Streaming metrics monitor registered, instanceId={}, engineId={}",
                instanceId, context.getEngineId());
    }

    @Scheduled(fixedDelayString = "${seatunnel.streaming.metrics.interval-ms:5000}")
    public void pollAll() {
        if (monitoringJobs.isEmpty()) {
            return;
        }

        monitoringJobs.forEach((instanceId, context) -> {
            try {
                pollSingle(instanceId, context);
            } catch (Exception e) {
                handleFetchError(instanceId, context, e);
            }
        });
    }

    private void pollSingle(Long instanceId, JobRuntimeContext context) {
        MonitorState state = monitorStates.computeIfAbsent(instanceId, key -> MonitorState.create());

        StreamingParsedJobMetrics parsed =
                streamingJobMetricsService.getRealtimeMetricsFromEngine(
                        context.getClientId(),
                        context.getEngineId()
                );

        if (parsed == null || parsed.isEmpty()) {
            handleEmptyMetrics(instanceId, context, state);
            return;
        }

        state.setPollTimes(state.getPollTimes() + 1);
        state.setEmptyTimes(0);
        state.setErrorTimes(0);

        streamingJobMetricsService.saveSnapshot(
                context.getInstanceId(),
                context.getJobDefinitionId(),
                context.getClientId(),
                context.getEngineId(),
                parsed
        );

        writeLogIfNecessary(instanceId, parsed, state);

        webSocketService.sendMessage(
                buildChannel(instanceId, context.getEngineId()),
                buildPayload(instanceId, context.getEngineId(), parsed)
        );
    }

    public void cleanup(Long instanceId) {
        monitoringJobs.remove(instanceId);
        monitorStates.remove(instanceId);

        JobFileLogger logger = loggers.remove(instanceId);
        if (logger != null) {
            try {
                logger.close();
            } catch (Exception e) {
                log.warn("Close streaming metrics logger failed, instanceId={}", instanceId, e);
            }
        }

        log.info("Streaming metrics monitor cleaned, instanceId={}", instanceId);
    }

    private void handleEmptyMetrics(Long instanceId, JobRuntimeContext context, MonitorState state) {
        state.setEmptyTimes(state.getEmptyTimes() + 1);

        if (state.getEmptyTimes() == 1 || state.getEmptyTimes() % 10 == 0) {
            log.warn("Streaming metrics empty, instanceId={}, engineId={}, emptyTimes={}",
                    instanceId, context.getEngineId(), state.getEmptyTimes());
        }

        if (maxEmptyTimes > 0 && state.getEmptyTimes() >= maxEmptyTimes) {
            sendFinalEvent(instanceId, context.getEngineId(), "METRICS_EMPTY_TIMEOUT");
            cleanup(instanceId);
        }
    }

    private void handleFetchError(Long instanceId, JobRuntimeContext context, Exception e) {
        MonitorState state = monitorStates.computeIfAbsent(instanceId, key -> MonitorState.create());
        state.setErrorTimes(state.getErrorTimes() + 1);

        if (state.getErrorTimes() == 1 || state.getErrorTimes() % 10 == 0) {
            log.warn("Fetch streaming metrics failed, instanceId={}, engineId={}, errorTimes={}",
                    instanceId, context.getEngineId(), state.getErrorTimes(), e);
        }

        if (maxErrorTimes > 0 && state.getErrorTimes() >= maxErrorTimes) {
            sendFinalEvent(instanceId, context.getEngineId(), "METRICS_FETCH_FAILED");
            cleanup(instanceId);
        }
    }

    private void writeLogIfNecessary(Long instanceId,
                                     StreamingParsedJobMetrics parsed,
                                     MonitorState state) {
        JobFileLogger logger = loggers.get(instanceId);
        if (logger == null) {
            return;
        }

        long pollTimes = state.getPollTimes();
        boolean shouldWrite = logEveryTimes <= 1 || pollTimes == 1 || pollTimes % logEveryTimes == 0;
        if (!shouldWrite) {
            return;
        }

        logger.info("Streaming Metrics Snapshot: " + parsed);
    }

    private void sendFinalEvent(Long instanceId, Long engineId, String status) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "JOB_STATUS");
        message.put("instanceId", instanceId);
        message.put("engineId", engineId);
        message.put("status", status);
        message.put("timestamp", Instant.now().toEpochMilli());

        webSocketService.sendMessage(buildChannel(instanceId, engineId), message);
    }

    private Map<String, Object> buildPayload(Long instanceId,
                                             Long engineId,
                                             StreamingParsedJobMetrics parsed) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "STREAMING_METRICS");
        message.put("instanceId", instanceId);
        message.put("engineId", engineId);
        message.put("jobStatus", parsed.getJobStatus());
        message.put("pipelineMetrics", parsed.getPipelineMetrics());
        message.put("tableMetrics", parsed.getTableMetrics());
        message.put("timestamp", parsed.getCollectTimeMs() == null ? Instant.now().toEpochMilli() : parsed.getCollectTimeMs());
        return message;
    }

    private String buildChannel(Long instanceId, Long engineId) {
        return "streaming-job-" + instanceId + "-" + engineId;
    }

    @Data
    private static class MonitorState {

        private long startTime;

        private long pollTimes;

        private int emptyTimes;

        private int errorTimes;

        static MonitorState create() {
            MonitorState state = new MonitorState();
            state.setStartTime(System.currentTimeMillis());
            return state;
        }
    }
}