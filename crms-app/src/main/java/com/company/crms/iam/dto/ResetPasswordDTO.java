package com.company.crms.iam.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDTO {

    /** 选填；未传时使用系统默认密码（应用配置）。 */
    @Size(min = 8, max = 64)
    private String newPassword;
}
