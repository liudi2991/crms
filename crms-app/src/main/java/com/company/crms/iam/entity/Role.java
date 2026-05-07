package com.company.crms.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.crms.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iam_role")
public class Role extends BaseEntity {
    private String code;
    private String name;
    private String dataScope;
    private String description;
    private Integer builtin;
    private Integer sort;
}
