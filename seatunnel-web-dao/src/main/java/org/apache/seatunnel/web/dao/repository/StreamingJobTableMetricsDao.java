package org.apache.seatunnel.web.dao.repository;

import org.apache.ibatis.annotations.Param;
import org.apache.seatunnel.web.dao.entity.StreamingJobTableMetrics;

import java.util.List;

public interface StreamingJobTableMetricsDao extends IDao<StreamingJobTableMetrics> {

    List<StreamingJobTableMetrics> selectLatestByInstanceId(Long instanceId);

    List<StreamingJobTableMetrics> selectLatestByInstanceIdAndTable( Long instanceId,
                                                                    String tableKey);

    List<StreamingJobTableMetrics> selectByInstanceIdAndTimeRange( Long instanceId,
                                                                   Long startTimeMs,
                                                                   Long endTimeMs);

    void deleteByInstanceId(Long instanceId);

    void deleteByDefinitionId( Long definitionId);

    void deleteBefore(Long collectTimeMs);
}