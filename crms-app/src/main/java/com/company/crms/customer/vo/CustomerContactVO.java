package com.company.crms.customer.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerContactVO {
    private Long id;
    private Long customerId;
    private String name;
    private String title;
    /** 已脱敏 */
    private String phone;
    /** 已脱敏 */
    private String email;
    private String wechat;
    private Boolean isPrimary;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
