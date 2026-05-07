package com.company.crms.iam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("iam_permission")
public class Permission {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String code;
    private String name;
    private String type;
    private String parentCode;
    private Integer sort;
}
