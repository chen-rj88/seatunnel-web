package org.apache.seatunnel.web.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.web.dao.entity.StreamingJobTableMetrics;
import org.apache.seatunnel.web.dao.mapper.StreamingJobTableMetricsMapper;
import org.apache.seatunnel.web.dao.repository.BaseDao;
import org.apache.seatunnel.web.dao.repository.StreamingJobTableMetricsDao;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class StreamingJobTableMetricsDaoImpl
        extends BaseDao<StreamingJobTableMetrics, StreamingJobTableMetricsMapper>
        implements StreamingJobTableMetricsDao {

    @Resource
    private StreamingJobTableMetricsMapper streamingJobTableMetricsMapper;

    public StreamingJobTableMetricsDaoImpl(@NonNull StreamingJobTableMetricsMapper streamingJobTableMetricsMapper) {
        super(streamingJobTableMetricsMapper);
    }

    @Override
    public List<StreamingJobTableMetrics> selectLatestByInstanceId(Long instanceId) {
        if (instanceId == null || instanceId <= 0) {
            return Collections.emptyList();
        }

        /*
         * 暂时不用 XML，所以这里先查最近一批数据，再在内存里按 tableKey 去重。
         *
         * 这个写法适合第一版轻量落地。
         * 后面数据量大以后，建议换成 XML：
         * select t.* from table t
         * join (select table_key, max(collect_time_ms) ...) latest
         */
        List<StreamingJobTableMetrics> rows =
                streamingJobTableMetricsMapper.selectList(
                        new LambdaQueryWrapper<StreamingJobTableMetrics>()
                                .eq(StreamingJobTableMetrics::getJobInstanceId, instanceId)
                                .orderByDesc(StreamingJobTableMetrics::getCollectTimeMs)
                                .orderByAsc(StreamingJobTableMetrics::getPipelineId)
                                .last("LIMIT 1000")
                );

        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, StreamingJobTableMetrics> latestMap = new LinkedHashMap<>();

        for (StreamingJobTableMetrics item : rows) {
            String tableKey = item.getTableKey();
            if (StringUtils.isBlank(tableKey)) {
                tableKey = buildTableKey(item.getSourceTable(), item.getSinkTable());
            }

            /*
             * 因为 rows 已经按 collect_time_ms desc 排序，
             * 第一次遇到某个 tableKey，就是它的最新记录。
             */
            latestMap.putIfAbsent(tableKey, item);
        }

        return new ArrayList<>(latestMap.values());
    }

    @Override
    public List<StreamingJobTableMetrics> selectLatestByInstanceIdAndTable(Long instanceId,
                                                                           String tableKey) {
        if (instanceId == null || instanceId <= 0 || StringUtils.isBlank(tableKey)) {
            return Collections.emptyList();
        }

        StreamingJobTableMetrics latest =
                streamingJobTableMetricsMapper.selectOne(
                        new LambdaQueryWrapper<StreamingJobTableMetrics>()
                                .eq(StreamingJobTableMetrics::getJobInstanceId, instanceId)
                                .eq(StreamingJobTableMetrics::getTableKey, tableKey)
                                .orderByDesc(StreamingJobTableMetrics::getCollectTimeMs)
                                .last("LIMIT 1")
                );

        if (latest == null) {
            return Collections.emptyList();
        }

        return Collections.singletonList(latest);
    }

    @Override
    public List<StreamingJobTableMetrics> selectByInstanceIdAndTimeRange(Long instanceId,
                                                                         Long startTimeMs,
                                                                         Long endTimeMs) {
        if (instanceId == null || instanceId <= 0) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<StreamingJobTableMetrics> wrapper =
                new LambdaQueryWrapper<StreamingJobTableMetrics>()
                        .eq(StreamingJobTableMetrics::getJobInstanceId, instanceId);

        if (startTimeMs != null) {
            wrapper.ge(StreamingJobTableMetrics::getCollectTimeMs, startTimeMs);
        }

        if (endTimeMs != null) {
            wrapper.le(StreamingJobTableMetrics::getCollectTimeMs, endTimeMs);
        }

        wrapper.orderByAsc(StreamingJobTableMetrics::getCollectTimeMs)
                .orderByAsc(StreamingJobTableMetrics::getPipelineId)
                .orderByAsc(StreamingJobTableMetrics::getTableKey);

        return streamingJobTableMetricsMapper.selectList(wrapper);
    }

    @Override
    public void deleteByInstanceId(Long instanceId) {
        if (instanceId == null || instanceId <= 0) {
            return;
        }

        streamingJobTableMetricsMapper.delete(
                new LambdaQueryWrapper<StreamingJobTableMetrics>()
                        .eq(StreamingJobTableMetrics::getJobInstanceId, instanceId)
        );
    }

    @Override
    public void deleteByDefinitionId(Long definitionId) {
        if (definitionId == null || definitionId <= 0) {
            return;
        }

        streamingJobTableMetricsMapper.delete(
                new LambdaQueryWrapper<StreamingJobTableMetrics>()
                        .eq(StreamingJobTableMetrics::getJobDefinitionId, definitionId)
        );
    }

    @Override
    public void deleteBefore(Long collectTimeMs) {
        if (collectTimeMs == null || collectTimeMs <= 0) {
            return;
        }

        streamingJobTableMetricsMapper.delete(
                new LambdaQueryWrapper<StreamingJobTableMetrics>()
                        .lt(StreamingJobTableMetrics::getCollectTimeMs, collectTimeMs)
        );
    }

    private String buildTableKey(String sourceTable, String sinkTable) {
        return safe(sourceTable) + "->" + safe(sinkTable);
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}