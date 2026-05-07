package com.company.crms.iam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.crms.common.annotation.SensitiveField;
import com.company.crms.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iam_user")
public class User extends BaseEntity {

    private String username;
    private String passwordHash;
    private String realName;

    @SensitiveField(mask = SensitiveField.Mask.PHONE)
    private String phone;

    @SensitiveField(mask = SensitiveField.Mask.EMAIL)
    private String email;

    private Long deptId;
    private String dataScope;
    private String status;
    private LocalDateTime lastLoginAt;
    private Integer failedCount;
    private LocalDateTime lockedUntil;

    @TableField("must_change_pwd")
    private Integer mustChangePwd;

    @TableField("super_admin")
    private Integer superAdmin;
}
