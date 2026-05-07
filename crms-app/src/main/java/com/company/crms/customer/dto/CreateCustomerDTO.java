package com.company.crms.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCustomerDTO {

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100)
    private String name;

    @Size(max = 50)
    private String shortName;

    @NotBlank
    @Pattern(regexp = "ENTERPRISE|GOVERNMENT|INDIVIDUAL", message = "客户类型非法")
    private String type;

    @Pattern(regexp = "^$|^[0-9A-HJ-NPQRTUWXY]{18}$", message = "统一社会信用代码格式不正确")
    private String uscc;

    private String regionCode;

    @Size(max = 255)
    private String address;

    private String industry;

    @Pattern(regexp = "A|B|C", message = "客户等级仅支持 A/B/C")
    private String level = "C";

    /** 不传时默认当前用户。 */
    private Long ownerId;

    @Size(max = 1000)
    private String remark;
}
