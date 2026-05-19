package org.apache.seatunnel.web.api.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.web.api.metrics.fetch.EngineJobInfo;
import org.apache.seatunnel.web.api.metrics.fetch.EngineMetricsFetchService;
import org.apache.seatunnel.web.api.metrics.streaming.model.StreamingParsedJobMetrics;
import org.apache.seatunnel.web.api.metrics.streaming.model.StreamingPipelineMetrics;
import org.apache.seatunnel.web.api.metrics.streaming.model.StreamingTableMetrics;
import org.apache.seatunnel.web.api.metrics.streaming.parser.StreamingJobInfoMetricsParser;
import org.apache.seatunnel.web.api.service.StreamingJobMetricsService;
import org.apache.seatunnel.web.core.exceptions.ServiceException;
import org.apache.seatunnel.web.dao.entity.StreamingJobMetrics;
import org.apache.seatunnel.web.dao.entity.StreamingJobTableMetrics;
import org.apache.seatunnel.web.dao.repository.StreamingJobMetricsDao;
import org.apache.seatunnel.web.dao.repository.StreamingJobTableMetricsDao;
import org.apache.seatunnel.web.spi.bean.vo.StreamingMetricsSnapshotVO;
import org.apache.seatunnel.web.spi.bean.vo.StreamingMetricsTrendItemVO;
import org.apache.seatunnel.web.spi.bean.vo.StreamingMetricsTrendVO;
import org.apache.seatunnel.web.spi.bean.vo.StreamingTableMetricsVO;
import org.apache.seatunnel.web.spi.enums.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class StreamingJobMetricsServiceImpl implements StreamingJobMetricsService {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private static final DateTimeFormatter SECOND_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter MINUTE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00");

    private static final DateTimeFormatter HOUR_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00:00");

    private static final DateTimeFormatter DAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00");

    @Resource
    private EngineMetricsFetchService engineMetricsFetchService;

    @Resource
    private StreamingJobInfoMetricsParser streamingJobInfoMetricsParser;

    @Resource
    private StreamingJobMetricsDao streamingJobMetricsDao;

    @Resource
    private StreamingJobTableMetricsDao streamingJobTableMetricsDao;

    @Override
    public StreamingParsedJobMetrics getRealtimeMetricsFromEngine(Long clientId, Long engineJobId) {
        validatePositive(clientId, "clientId");
        validatePositive(engineJobId, "engineJobId");

        EngineJobInfo jobInfo = engineMetricsFetchService.fetchJobInfo(clientId, engineJobId);
        return streamingJobInfoMetricsParser.parse(jobInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSnapshot(Long jobInstanceId,
                             Long jobDefinitionId,
                             Long clientId,
                             Long engineJobId,
                             StreamingParsedJobMetrics parsed) {
        validatePositive(jobInstanceId, "jobInstanceId");
        validatePositive(jobDefinitionId, "jobDefinitionId");

        if (parsed == null || parsed.isEmpty()) {
            return;
        }

        Long collectTimeMs = parsed.getCollectTimeMs() == null
                ? System.currentTimeMillis()
                : parsed.getCollectTimeMs();

        Date collectTime = new Date(collectTimeMs);
        Date now = new Date();

        List<StreamingJobMetrics> pipelineList = buildPipelineMetrics(
                jobInstanceId,
                jobDefinitionId,
                clientId,
                engineJobId,
                parsed,
                collectTimeMs,
                collectTime,
                now
        );

        List<StreamingJobTableMetrics> tableList = buildTableMetrics(
                jobInstanceId,
                jobDefinitionId,
                clientId,
                engineJobId,
                parsed,
                collectTimeMs,
                collectTime,
                now
        );

        if (!pipelineList.isEmpty()) {
            streamingJobMetricsDao.insertBatch(pipelineList);
        }

        if (!tableList.isEmpty()) {
            streamingJobTableMetricsDao.insertBatch(tableList);
        }
    }

    @Override
    public StreamingMetricsSnapshotVO latest(Long instanceId) {
        validatePositive(instanceId, "instanceId");

        StreamingJobMetrics latest = streamingJobMetricsDao.selectLatestByInstanceId(instanceId);
        List<StreamingJobTableMetrics> tableMetrics =
                streamingJobTableMetricsDao.selectLatestByInstanceId(instanceId);

        StreamingMetricsSnapshotVO vo = new StreamingMetricsSnapshotVO();

        if (latest != null) {
            vo.setCollectTimeMs(latest.getCollectTimeMs());
            vo.setJobInstanceId(latest.getJobInstanceId());
            vo.setJobDefinitionId(latest.getJobDefinitionId());
            vo.setEngineJobId(latest.getEngineJobId());
            vo.setClientId(latest.getClientId());
            vo.setJobStatus(latest.getJobStatus());
            vo.setReadRowCount(defaultLong(latest.getReadRowCount()));
            vo.setWriteRowCount(defaultLong(latest.getWriteRowCount()));
            vo.setReadQps(defaultDecimal(latest.getReadQps()));
            vo.setWriteQps(defaultDecimal(latest.getWriteQps()));
            vo.setReadBytes(defaultLong(latest.getReadBytes()));
            vo.setWriteBytes(defaultLong(latest.getWriteBytes()));
            vo.setReadBps(defaultDecimal(latest.getReadBps()));
            vo.setWriteBps(defaultDecimal(latest.getWriteBps()));
            vo.setIntermediateQueueSize(defaultLong(latest.getIntermediateQueueSize()));
        }

        vo.setTableMetrics(toTableVOList(tableMetrics));
        return vo;
    }

    @Override
    public StreamingMetricsTrendVO trend(Long instanceId,
                                         Long startTimeMs,
                                         Long endTimeMs,
                                         String granularity) {
        validatePositive(instanceId, "instanceId");

        long now = System.currentTimeMillis();
        Long start = startTimeMs == null ? now - Duration.ofHours(1).toMillis() : startTimeMs;
        Long end = endTimeMs == null ? now : endTimeMs;
        String finalGranularity = granularity == null || granularity.isBlank()
                ? "minute"
                : granularity;

        List<StreamingJobMetrics> rows =
                streamingJobMetricsDao.selectByInstanceIdAndTimeRange(instanceId, start, end);

        StreamingMetricsTrendVO vo = new StreamingMetricsTrendVO();
        vo.setItems(buildTrendItems(rows, finalGranularity));
        return vo;
    }

    @Override
    public List<StreamingTableMetricsVO> listLatestTableMetrics(Long instanceId) {
        validatePositive(instanceId, "instanceId");
        return toTableVOList(streamingJobTableMetricsDao.selectLatestByInstanceId(instanceId));
    }

    @Override
    public void deleteByInstanceId(Long instanceId) {
        if (instanceId == null) {
            return;
        }

        streamingJobTableMetricsDao.deleteByInstanceId(instanceId);
        streamingJobMetricsDao.deleteByInstanceId(instanceId);
    }

    @Override
    public void deleteByDefinitionId(Long definitionId) {
        if (definitionId == null) {
            return;
        }

        streamingJobTableMetricsDao.deleteByDefinitionId(definitionId);
        streamingJobMetricsDao.deleteByDefinitionId(definitionId);
    }

    @Override
    public void deleteExpired(Long retentionDays) {
        if (retentionDays == null || retentionDays <= 0) {
            return;
        }

        long before = System.currentTimeMillis() - Duration.ofDays(retentionDays).toMillis();

        streamingJobTableMetricsDao.deleteBefore(before);
        streamingJobMetricsDao.deleteBefore(before);
    }

    private List<StreamingMetricsTrendItemVO> buildTrendItems(List<StreamingJobMetrics> rows,
                                                              String granularity) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<StreamingJobMetrics>> groupMap = new LinkedHashMap<>();

        for (StreamingJobMetrics row : rows) {
            String bucket = buildBucket(row.getCollectTimeMs(), granularity);
            groupMap.computeIfAbsent(bucket, key -> new ArrayList<>()).add(row);
        }

        List<StreamingMetricsTrendItemVO> result = new ArrayList<>();

        for (Map.Entry<String, List<StreamingJobMetrics>> entry : groupMap.entrySet()) {
            List<StreamingJobMetrics> bucketRows = entry.getValue();

            long readRowCount = 0L;
            long writeRowCount = 0L;
            long readBytes = 0L;
            long writeBytes = 0L;
            long intermediateQueueSize = 0L;

            BigDecimal readQps = BigDecimal.ZERO;
            BigDecimal writeQps = BigDecimal.ZERO;
            BigDecimal readBps = BigDecimal.ZERO;
            BigDecimal writeBps = BigDecimal.ZERO;

            for (StreamingJobMetrics item : bucketRows) {
                readRowCount = Math.max(readRowCount, defaultLong(item.getReadRowCount()));
                writeRowCount = Math.max(writeRowCount, defaultLong(item.getWriteRowCount()));
                readBytes = Math.max(readBytes, defaultLong(item.getReadBytes()));
                writeBytes = Math.max(writeBytes, defaultLong(item.getWriteBytes()));
                intermediateQueueSize = Math.max(
                        intermediateQueueSize,
                        defaultLong(item.getIntermediateQueueSize())
                );

                readQps = readQps.add(defaultDecimal(item.getReadQps()));
                writeQps = writeQps.add(defaultDecimal(item.getWriteQps()));
                readBps = readBps.add(defaultDecimal(item.getReadBps()));
                writeBps = writeBps.add(defaultDecimal(item.getWriteBps()));
            }

            BigDecimal bucketSize = BigDecimal.valueOf(Math.max(bucketRows.size(), 1));

            StreamingMetricsTrendItemVO vo = new StreamingMetricsTrendItemVO();
            vo.setDate(entry.getKey());
            vo.setReadRowCount(readRowCount);
            vo.setWriteRowCount(writeRowCount);
            vo.setReadBytes(readBytes);
            vo.setWriteBytes(writeBytes);
            vo.setIntermediateQueueSize(intermediateQueueSize);
            vo.setReadQps(readQps.divide(bucketSize, 4));
            vo.setWriteQps(writeQps.divide(bucketSize, 4));
            vo.setReadBps(readBps.divide(bucketSize, 4));
            vo.setWriteBps(writeBps.divide(bucketSize, 4));

            result.add(vo);
        }

        return result;
    }

    private String buildBucket(Long collectTimeMs, String granularity) {
        long timestamp = collectTimeMs == null ? System.currentTimeMillis() : collectTimeMs;

        LocalDateTime time = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZONE_ID
        );

        String finalGranularity = granularity == null ? "minute" : granularity;

        if ("second".equalsIgnoreCase(finalGranularity)) {
            return time.format(SECOND_FORMATTER);
        }

        if ("hour".equalsIgnoreCase(finalGranularity)) {
            return time.format(HOUR_FORMATTER);
        }

        if ("day".equalsIgnoreCase(finalGranularity)) {
            return time.format(DAY_FORMATTER);
        }

        return time.format(MINUTE_FORMATTER);
    }

    private List<StreamingJobMetrics> buildPipelineMetrics(Long jobInstanceId,
                                                           Long jobDefinitionId,
                                                           Long clientId,
                                                           Long engineJobId,
                                                           StreamingParsedJobMetrics parsed,
                                                           Long collectTimeMs,
                                                           Date collectTime,
                                                           Date now) {
        if (parsed.getPipelineMetrics() == null || parsed.getPipelineMetrics().isEmpty()) {
            return Collections.emptyList();
        }

        List<StreamingJobMetrics> result = new ArrayList<>();

        for (StreamingPipelineMetrics item : parsed.getPipelineMetrics().values()) {
            StreamingJobMetrics po = new StreamingJobMetrics();

            po.setCollectTimeMs(collectTimeMs);
            po.setJobInstanceId(jobInstanceId);
            po.setJobDefinitionId(jobDefinitionId);
            po.setClientId(clientId);
            po.setEngineJobId(engineJobId);
            po.setPipelineId(defaultPipelineId(item.getPipelineId()));
            po.setJobStatus(parsed.getJobStatus());

            po.setReadRowCount(defaultLong(item.getReadRowCount()));
            po.setWriteRowCount(defaultLong(item.getWriteRowCount()));
            po.setReadQps(defaultDecimal(item.getReadQps()));
            po.setWriteQps(defaultDecimal(item.getWriteQps()));

            po.setReadBytes(defaultLong(item.getReadBytes()));
            po.setWriteBytes(defaultLong(item.getWriteBytes()));
            po.setReadBps(defaultDecimal(item.getReadBps()));
            po.setWriteBps(defaultDecimal(item.getWriteBps()));

            po.setIntermediateQueueSize(defaultLong(item.getIntermediateQueueSize()));
            po.setLagCount(defaultLong(item.getLagCount()));
            po.setRecordDelay(defaultLong(item.getRecordDelay()));

            po.setCollectTime(collectTime);
            po.setCreateTime(now);

            result.add(po);
        }

        return result;
    }

    private List<StreamingJobTableMetrics> buildTableMetrics(Long jobInstanceId,
                                                             Long jobDefinitionId,
                                                             Long clientId,
                                                             Long engineJobId,
                                                             StreamingParsedJobMetrics parsed,
                                                             Long collectTimeMs,
                                                             Date collectTime,
                                                             Date now) {
        if (parsed.getTableMetrics() == null || parsed.getTableMetrics().isEmpty()) {
            return Collections.emptyList();
        }

        List<StreamingJobTableMetrics> result = new ArrayList<>();

        for (StreamingTableMetrics item : parsed.getTableMetrics()) {
            StreamingJobTableMetrics po = new StreamingJobTableMetrics();
            String tableKey = buildTableKey(
                    item.getSourceTable(),
                    item.getSinkTable(),
                    item.getTableKey()
            );

            po.setCollectTimeMs(collectTimeMs);
            po.setJobInstanceId(jobInstanceId);
            po.setJobDefinitionId(jobDefinitionId);
            po.setClientId(clientId);
            po.setEngineJobId(engineJobId);
            po.setPipelineId(defaultPipelineId(item.getPipelineId()));

            po.setSourceTable(item.getSourceTable());
            po.setSinkTable(item.getSinkTable());
            po.setTableKey(buildTableKey(item.getSourceTable(), item.getSinkTable(), item.getTableKey()));

            po.setReadRowCount(defaultLong(item.getReadRowCount()));
            po.setWriteRowCount(defaultLong(item.getWriteRowCount()));
            po.setReadQps(defaultDecimal(item.getReadQps()));
            po.setWriteQps(defaultDecimal(item.getWriteQps()));

            po.setReadBytes(defaultLong(item.getReadBytes()));
            po.setWriteBytes(defaultLong(item.getWriteBytes()));
            po.setReadBps(defaultDecimal(item.getReadBps()));
            po.setWriteBps(defaultDecimal(item.getWriteBps()));

            po.setStatus(item.getStatus());
            po.setErrorMsg(item.getErrorMsg());

            po.setCollectTime(collectTime);
            po.setCreateTime(now);

            po.setTableKey(tableKey);
            po.setTableKeyHash(md5Hex(tableKey));

            result.add(po);
        }

        return result;
    }

    private String md5Hex(String value) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest((value == null ? "" : value).getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Generate tableKey hash failed", e);
        }
    }

    private List<StreamingTableMetricsVO> toTableVOList(List<StreamingJobTableMetrics> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        List<StreamingTableMetricsVO> result = new ArrayList<>();

        for (StreamingJobTableMetrics item : list) {
            StreamingTableMetricsVO vo = new StreamingTableMetricsVO();
            vo.setCollectTimeMs(item.getCollectTimeMs());
            vo.setJobInstanceId(item.getJobInstanceId());
            vo.setJobDefinitionId(item.getJobDefinitionId());
            vo.setEngineJobId(item.getEngineJobId());
            vo.setClientId(item.getClientId());
            vo.setPipelineId(item.getPipelineId());

            vo.setSourceTable(item.getSourceTable());
            vo.setSinkTable(item.getSinkTable());
            vo.setTableKey(item.getTableKey());

            vo.setReadRowCount(defaultLong(item.getReadRowCount()));
            vo.setWriteRowCount(defaultLong(item.getWriteRowCount()));
            vo.setReadQps(defaultDecimal(item.getReadQps()));
            vo.setWriteQps(defaultDecimal(item.getWriteQps()));
            vo.setReadBytes(defaultLong(item.getReadBytes()));
            vo.setWriteBytes(defaultLong(item.getWriteBytes()));
            vo.setReadBps(defaultDecimal(item.getReadBps()));
            vo.setWriteBps(defaultDecimal(item.getWriteBps()));

            vo.setStatus(item.getStatus());
            vo.setErrorMsg(item.getErrorMsg());

            result.add(vo);
        }

        return result;
    }

    private String buildTableKey(String sourceTable, String sinkTable, String tableKey) {
        if (tableKey != null && !tableKey.isBlank()) {
            return tableKey;
        }
        return safe(sourceTable) + "->" + safe(sinkTable);
    }

    private int defaultPipelineId(Integer pipelineId) {
        return pipelineId == null ? 0 : pipelineId;
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    private void validatePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR, name);
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }

        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return new BigDecimal(String.valueOf(value)).longValue();
        } catch (Exception e) {
            return 0L;
        }
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            }
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}