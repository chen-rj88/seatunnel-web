package org.apache.seatunnel.web.api.metrics;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.web.api.metrics.streaming.StreamingJobMetricsMonitor;
import org.apache.seatunnel.web.common.enums.JobSubmitStage;
import org.apache.seatunnel.web.common.exception.JobSubmitException;
import org.apache.seatunnel.web.core.exceptions.ServiceException;
import org.apache.seatunnel.web.engine.client.rest.SeaTunnelRestClient;
import org.apache.seatunnel.web.spi.bean.vo.JobInstanceVO;
import org.apache.seatunnel.web.spi.enums.Status;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class JobSubmitter {

    private static final String JOB_TYPE_STREAMING = "STREAMING";
    private static final String JOB_TYPE_BATCH = "BATCH";

    private final JobConfigFileService configFileService;
    private final SeaTunnelRestClient restClient;

    /**
     * Batch / offline metrics monitor.
     *
     * <p>
     * Keep the old JobMetricsMonitor for batch job final metrics snapshot.
     * </p>
     */
    private final JobMetricsMonitor batchMetricsMonitor;

    /**
     * Streaming metrics monitor.
     *
     * <p>
     * Streaming jobs use a separated metrics pipeline:
     * EngineMetricsFetchService -> StreamingJobInfoMetricsParser -> StreamingJobMetricsService.
     * </p>
     */
    private final StreamingJobMetricsMonitor streamingMetricsMonitor;

    private final JobResultWatcher resultWatcher;
    private final JobResultHandler resultHandler;

    public JobSubmitter(JobConfigFileService configFileService,
                        SeaTunnelRestClient restClient,
                        JobMetricsMonitor batchMetricsMonitor,
                        StreamingJobMetricsMonitor streamingMetricsMonitor,
                        JobResultWatcher resultWatcher,
                        JobResultHandler resultHandler) {
        this.configFileService = configFileService;
        this.restClient = restClient;
        this.batchMetricsMonitor = batchMetricsMonitor;
        this.streamingMetricsMonitor = streamingMetricsMonitor;
        this.resultWatcher = resultWatcher;
        this.resultHandler = resultHandler;
    }

    public void submit(JobInstanceVO instance) {
        validate(instance);

        Long instanceId = instance.getId();
        Long jobDefinitionId = instance.getJobDefinitionId();
        Long clientId = instance.getClientId();

        String hoconConfig = instance.getRuntimeConfig();
        String logPath = instance.getLogPath();

        JobFileLogger jobLogger = new JobFileLogger(logPath);
        jobLogger.info("=== Job Submit Start (REST API) ===");
        jobLogger.info("Job instanceId: " + instanceId);
        jobLogger.info("Job definitionId: " + jobDefinitionId);
        jobLogger.info("Client id: " + clientId);

        String configFile = null;
        Long engineId = null;
        boolean submitted = false;

        try {
            jobLogger.info("Writing config file...");
            configFile = configFileService.writeConfig(instanceId, hoconConfig);
            jobLogger.info("Config file written to: " + configFile);

            jobLogger.info("Submitting job via REST API...");
            log.info("Submitting job to Zeta, instanceId={}, clientId={}", instanceId, clientId);

            String filename = "job-" + instanceId + ".conf";

            Map<?, ?> resp = restClient.submitJobUpload(
                    clientId,
                    safeConfig(hoconConfig).getBytes(StandardCharsets.UTF_8),
                    filename
            );

            engineId = extractJobId(resp);
            submitted = true;

            log.info("Submit job response received, instanceId={}, resp={}", instanceId, resp);
            jobLogger.info("Submit job response received: " + resp);

            resultHandler.updateEngineId(instanceId, engineId);

            JobRuntimeContext ctx = buildRuntimeContext(
                    instance,
                    engineId,
                    configFile
            );

            registerPostSubmitWatchers(instance, ctx, jobLogger);

            jobLogger.info("=== Job Submit Complete ===");

        } catch (Exception e) {
            if (submitted) {
                handlePostSubmitFailure(jobLogger, instanceId, engineId, e);
                return;
            }

            handleCoreFailure(jobLogger, instanceId, e);

        } finally {
            jobLogger.close();
        }
    }

    public void pause(JobInstanceVO instance) {
        if (instance == null || instance.getId() == null) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR, "jobInstance");
        }

        if (instance.getClientId() == null || instance.getClientId() <= 0) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR, "clientId");
        }

        if (instance.getEngineJobId() == null || instance.getEngineJobId() <= 0) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR, "engineJobId");
        }

        Long instanceId = instance.getId();
        Long clientId = instance.getClientId();
        Long engineJobId = instance.getEngineJobId();

        log.info("Stopping SeaTunnel job: instanceId={}, clientId={}, engineJobId={}",
                instanceId, clientId, engineJobId);

        Map<?, ?> resp = restClient.stopJob(clientId, engineJobId, false);

        log.info("Stop SeaTunnel job response: instanceId={}, clientId={}, engineJobId={}, resp={}",
                instanceId, clientId, engineJobId, resp);
    }

    private void registerPostSubmitWatchers(JobInstanceVO instance,
                                            JobRuntimeContext ctx,
                                            JobFileLogger jobLogger) {
        boolean metricsRegistered = false;
        boolean watcherRegistered = false;

        try {
            registerMetricsMonitor(instance, ctx, jobLogger);
            metricsRegistered = true;
        } catch (Exception e) {
            jobLogger.warn("Metrics monitor register failed: " + e.getMessage());

            log.warn(
                    "Metrics monitor register failed, instanceId={}, engineId={}, jobType={}",
                    ctx.getInstanceId(),
                    ctx.getEngineId(),
                    ctx.getJobType(),
                    e
            );
        }

        try {
            resultWatcher.registerByRest(ctx);
            watcherRegistered = true;
            jobLogger.info("REST result watcher registered");
        } catch (Exception e) {
            jobLogger.warn("Result watcher register failed: " + e.getMessage());

            log.warn(
                    "Result watcher register failed, instanceId={}, engineId={}, jobType={}",
                    ctx.getInstanceId(),
                    ctx.getEngineId(),
                    ctx.getJobType(),
                    e
            );
        }

        if (!metricsRegistered || !watcherRegistered) {
            jobLogger.warn(
                    "Post-submit watcher registration incomplete. " +
                            "The job has already been submitted to SeaTunnel Engine. " +
                            "metricsRegistered=" + metricsRegistered +
                            ", watcherRegistered=" + watcherRegistered
            );
        }
    }

    private void registerMetricsMonitor(JobInstanceVO instance,
                                        JobRuntimeContext ctx,
                                        JobFileLogger jobLogger) {
        if (isStreamingJob(instance, ctx)) {
            streamingMetricsMonitor.register(ctx);
            jobLogger.info("Streaming metrics monitor registered");
            log.info("Streaming metrics monitor registered, instanceId={}, engineId={}",
                    ctx.getInstanceId(), ctx.getEngineId());
            return;
        }

        batchMetricsMonitor.register(ctx);
        jobLogger.info("Batch metrics monitor registered");
        log.info("Batch metrics monitor registered, instanceId={}, engineId={}",
                ctx.getInstanceId(), ctx.getEngineId());
    }

    private JobRuntimeContext buildRuntimeContext(JobInstanceVO instance,
                                                  Long engineId,
                                                  String configFile) {
        JobRuntimeContext ctx = new JobRuntimeContext();

        ctx.setInstanceId(instance.getId());
        ctx.setJobDefinitionId(instance.getJobDefinitionId());
        ctx.setClientId(instance.getClientId());
        ctx.setEngineId(engineId);
        ctx.setConfigFile(configFile);
        ctx.setJobType(resolveJobType(instance));

        return ctx;
    }

    private String resolveJobType(JobInstanceVO instance) {
        if (instance == null) {
            return JOB_TYPE_BATCH;
        }

        /*
         * Prefer jobType if your JobInstanceVO has this field.
         *
         * In your project, some places use job type / mode to distinguish batch
         * and streaming. If JobInstanceVO does not currently expose getJobType(),
         * you can either:
         *
         * 1. add jobType to JobInstanceVO;
         * 2. add jobType to JobRuntimeContext when creating instance;
         * 3. or fallback to runtimeConfig contains "mode=STREAMING".
         */

        try {
            Object jobType = instance.getClass().getMethod("getJobType").invoke(instance);
            if (jobType != null) {
                return String.valueOf(jobType);
            }
        } catch (Exception ignored) {
            // JobInstanceVO may not have getJobType now.
        }

        try {
            Object runMode = instance.getClass().getMethod("getRunMode").invoke(instance);
            if (runMode != null) {
                String value = String.valueOf(runMode);
                if (JOB_TYPE_STREAMING.equalsIgnoreCase(value)) {
                    return JOB_TYPE_STREAMING;
                }
                if (JOB_TYPE_BATCH.equalsIgnoreCase(value)) {
                    return JOB_TYPE_BATCH;
                }
            }
        } catch (Exception ignored) {
            // JobInstanceVO may not have getRunMode now.
        }

        String runtimeConfig = instance.getRuntimeConfig();
        if (isStreamingRuntimeConfig(runtimeConfig)) {
            return JOB_TYPE_STREAMING;
        }

        return JOB_TYPE_BATCH;
    }

    private boolean isStreamingJob(JobInstanceVO instance,
                                   JobRuntimeContext ctx) {
        if (ctx != null && JOB_TYPE_STREAMING.equalsIgnoreCase(ctx.getJobType())) {
            return true;
        }

        if (instance == null) {
            return false;
        }

        String runtimeConfig = instance.getRuntimeConfig();
        return isStreamingRuntimeConfig(runtimeConfig);
    }

    private boolean isStreamingRuntimeConfig(String runtimeConfig) {
        if (StringUtils.isBlank(runtimeConfig)) {
            return false;
        }

        String normalized = runtimeConfig
                .replace(" ", "")
                .replace("\n", "")
                .replace("\r", "")
                .replace("\t", "")
                .toUpperCase();

        return normalized.contains("MODE=STREAMING")
                || normalized.contains("MODE:STREAMING")
                || normalized.contains("JOB.MODE=STREAMING")
                || normalized.contains("JOB{MODE=STREAMING}")
                || normalized.contains("JOB={MODE=STREAMING}");
    }

    private void handleCoreFailure(JobFileLogger jobLogger,
                                   Long instanceId,
                                   Exception e) {
        jobLogger.error("Job submit failed before engine accepted the job", e);

        try {
            resultHandler.handleFailure(instanceId, e);
        } catch (Exception handlerEx) {
            log.error("handleFailure threw exception, instanceId={}", instanceId, handlerEx);
        }

        throw (e instanceof JobSubmitException)
                ? (JobSubmitException) e
                : new JobSubmitException(JobSubmitStage.SUBMIT, "Submit job failed", e);
    }

    private void handlePostSubmitFailure(JobFileLogger jobLogger,
                                         Long instanceId,
                                         Long engineId,
                                         Exception e) {
        jobLogger.error(
                "Job was submitted to SeaTunnel Engine, but post-submit handling failed. " +
                        "instanceId=" + instanceId + ", engineId=" + engineId,
                e
        );

        log.warn(
                "Post-submit handling failed after job accepted by engine, instanceId={}, engineId={}",
                instanceId,
                engineId,
                e
        );

        /*
         * Do not mark the job instance as FAILED here.
         *
         * Reason:
         * The SeaTunnel Engine has already accepted the job. If we mark the local
         * instance as failed now, local state may conflict with the real engine state.
         */
    }

    private Long extractJobId(Map<?, ?> resp) {
        Object jobIdObj = resp == null ? null : resp.get("jobId");

        if (jobIdObj == null) {
            throw new IllegalStateException("REST submit response missing jobId, resp=" + resp);
        }

        return Long.valueOf(jobIdObj.toString());
    }

    private void validate(JobInstanceVO instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Job instance must not be null");
        }

        if (instance.getId() == null) {
            throw new IllegalArgumentException("Job instance id must not be null");
        }

        if (instance.getClientId() == null) {
            throw new IllegalArgumentException("Job client id must not be null");
        }

        if (StringUtils.isBlank(instance.getLogPath())) {
            throw new IllegalArgumentException("Job log path must not be blank");
        }

        if (StringUtils.isBlank(instance.getRuntimeConfig())) {
            throw new IllegalArgumentException("Job runtime config must not be blank");
        }
    }

    private String safeConfig(String config) {
        return config == null ? "" : config;
    }
}