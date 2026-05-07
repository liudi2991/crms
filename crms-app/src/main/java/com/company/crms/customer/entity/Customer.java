package com.company.crms.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.crms.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer")
public class Customer extends BaseEntity {
    private String code;
    private String name;
    private String shortName;
    private String type;
    private String uscc;
    private String regionCode;
    private String address;
    private String industry;
    private String level;
    private Long ownerId;
    private Long deptId;
    private String status;
    private Long mergedTo;
    private String remark;
}
