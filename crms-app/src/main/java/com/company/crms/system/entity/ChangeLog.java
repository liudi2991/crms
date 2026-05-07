package com.company.crms.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("change_log")
public class ChangeLog implements Serializable {
    @TableId
    private Long id;
    private String bizType;
    private Long bizId;
    private String field;
    private String oldValue;
    private String newValue;
    private String reason;
    private Long operatorId;
    private LocalDateTime operatedAt;
}
