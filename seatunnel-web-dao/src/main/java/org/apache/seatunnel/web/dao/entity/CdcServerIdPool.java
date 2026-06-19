package org.apache.seatunnel.web.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_seatunnel_web_cdc_server_id_pool")
public class CdcServerIdPool extends BaseEntity {

    private Long datasourceId;

    private String instanceKey;

    private Long minServerId;

    private Long maxServerId;

    private Integer status;
}
