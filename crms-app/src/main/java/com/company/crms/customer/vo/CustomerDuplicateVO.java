package com.company.crms.customer.vo;

import lombok.Data;

@Data
public class CustomerDuplicateVO {
    private Long id;
    private String code;
    private String name;
    private String uscc;
    private String status;
    /** 命中字段：NAME / USCC */
    private String hitField;
}
