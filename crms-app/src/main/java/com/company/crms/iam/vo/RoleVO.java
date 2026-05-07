package com.company.crms.iam.vo;

import lombok.Data;

import java.util.List;

@Data
public class RoleVO {
    private Long id;
    private String code;
    private String name;
    private String dataScope;
    private String description;
    private Boolean builtin;
    private Integer sort;
    private Integer version;
    private List<String> permissionCodes;
    private Long userCount;
}
