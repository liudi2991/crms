package com.company.crms.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("hard_delete_log")
public class HardDeleteLog implements Serializable {
    @TableId
    private Long id;
    private Long operatorId;
    private String bizType;
    private Long bizId;
    private String snapshotJson;
    private String reason;
    private LocalDateTime createdAt;
}
