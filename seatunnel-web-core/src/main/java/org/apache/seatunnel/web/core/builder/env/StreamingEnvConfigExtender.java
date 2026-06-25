package org.apache.seatunnel.web.core.builder.env;

import org.apache.seatunnel.web.spi.bean.dto.config.JobEnvConfig;
import org.apache.seatunnel.web.spi.bean.dto.config.StreamingJobEnvConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StreamingEnvConfigExtender implements EnvConfigExtender {

    @Override
    public boolean supports(JobEnvConfig envConfig) {
        return envConfig instanceof StreamingJobEnvConfig;
    }

    @Override
    public void fill(Map<String, Object> envMap, JobEnvConfig envConfig) {
        StreamingJobEnvConfig streamingEnvConfig = (StreamingJobEnvConfig) envConfig;

        if (streamingEnvConfig.getCheckpointInterval() != null
                && streamingEnvConfig.getCheckpointInterval() > 0) {
            envMap.put("checkpoint.interval", streamingEnvConfig.getCheckpointInterval());
        }
    }
}