package org.apache.seatunnel.web.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_seatunnel_web_cdc_server_id_allocation")
public class CdcServerIdAllocation extends BaseEntity {

    private Long poolId;

    private Long serverId;

    private Long jobDefinitionId;

    private Long jobInstanceId;

    private String source;

    private Integer active;

    private Date allocatedTime;

    private Date releasedTime;
}
