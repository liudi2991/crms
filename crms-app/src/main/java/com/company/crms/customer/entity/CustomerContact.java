package com.company.crms.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.crms.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_contact")
public class CustomerContact extends BaseEntity {
    private Long customerId;
    private String name;
    private String title;
    /** 加密存储 */
    private String phone;
    /** 加密存储 */
    private String email;
    private String wechat;
    private Integer isPrimary;
    private String remark;
}
