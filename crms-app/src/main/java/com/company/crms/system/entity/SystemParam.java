package com.company.crms.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("system_param")
public class SystemParam implements Serializable {
    @TableId
    private Long id;

    @TableField("param_key")
    private String paramKey;

    @TableField("param_value")
    private String paramValue;

    private String description;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
