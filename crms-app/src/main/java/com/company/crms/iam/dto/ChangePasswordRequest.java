package com.company.crms.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank
    @Size(min = 6, max = 64)
    private String oldPassword;

    @NotBlank
    @Size(min = 8, max = 64, message = "新密码长度 8-64 位")
    private String newPassword;
}
