package com.company.crms.iam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateUserDTO {

    @NotBlank
    @Size(min = 3, max = 64)
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_.-]{2,63}$", message = "用户名以字母开头，仅允许字母数字与 _.-")
    private String username;

    @NotBlank
    @Size(min = 1, max = 64)
    private String realName;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @Email
    private String email;

    @NotNull
    private Long deptId;

    /** 角色 ID 列表（创建时必填至少一个）。 */
    @NotNull
    @Size(min = 1, message = "至少分配一个角色")
    private List<Long> roleIds;

    /** 选填，未提供时使用系统默认密码。 */
    @Size(min = 8, max = 64)
    private String password;
}
