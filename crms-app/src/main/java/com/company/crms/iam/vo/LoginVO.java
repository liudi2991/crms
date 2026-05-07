package com.company.crms.iam.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private boolean forceChangePassword;
}
