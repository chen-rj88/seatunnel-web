package org.apache.seatunnel.web.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_seatunnel_streaming_job_table_metrics")
public class StreamingJobTableMetrics {

    private String tableKeyHash;

    private Long collectTimeMs;

    private Long jobInstanceId;

    private Long jobDefinitionId;

    private Long engineJobId;

    private Long clientId;

    private Integer pipelineId;

    private String sourceTable;

    private String sinkTable;

    private String tableKey;

    private Long readRowCount;

    private Long writeRowCount;

    private BigDecimal readQps;

    private BigDecimal writeQps;

    private Long readBytes;

    private Long writeBytes;

    private BigDecimal readBps;

    private BigDecimal writeBps;

    private String status;

    private String errorMsg;

    private Date collectTime;

    private Date createTime;
}