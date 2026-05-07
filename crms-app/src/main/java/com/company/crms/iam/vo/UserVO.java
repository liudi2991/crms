package com.company.crms.iam.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Long deptId;
    private String deptName;
    private String status;
    private Boolean superAdmin;
    private Boolean mustChangePwd;
    private Boolean locked;
    private LocalDateTime lastLoginAt;
    private List<Long> roleIds;
    private List<String> roleNames;
    private LocalDateTime createdAt;
    private Integer version;
}
