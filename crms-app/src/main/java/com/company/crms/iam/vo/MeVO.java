package com.company.crms.iam.vo;

import lombok.Data;

import java.util.List;

@Data
public class MeVO {
    private Long id;
    private String username;
    private String realName;
    private Long deptId;
    private List<String> roles;
    private List<String> permissions;
    private boolean superAdmin;
    private boolean forceChangePassword;
}
