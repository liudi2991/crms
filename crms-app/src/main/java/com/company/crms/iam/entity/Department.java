package com.company.crms.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.crms.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iam_department")
public class Department extends BaseEntity {
    private Long parentId;
    private String name;
    private String fullPath;
    private Integer sort;
}
