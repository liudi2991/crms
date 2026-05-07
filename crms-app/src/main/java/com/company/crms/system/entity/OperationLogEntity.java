package com.company.crms.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作审计日志（独立于 BaseEntity，无软删/版本字段）。
 */
@Data
@TableName("operation_log")
public class OperationLogEntity implements Serializable {
    @TableId
    private Long id;
    private Long operatorId;
    private String operatorName;
    private String operatorIp;
    private String module;
    private String action;
    @TableField("op_type")
    private String opType;
    private String bizType;
    private Long bizId;
    private String uri;
    private String method;
    private String paramsJson;
    private String result;
    private String errorMessage;
    private Integer durationMs;
    private LocalDateTime createdAt;
}
