package com.company.crms.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateContactDTO {
    @NotNull
    private Long customerId;

    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 64)
    private String title;

    private String phone;
    private String email;

    @Size(max = 64)
    private String wechat;

    private Boolean isPrimary;

    @Size(max = 255)
    private String remark;
}
