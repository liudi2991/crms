package com.company.crms.customer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCustomerDTO {

    @NotNull
    private Long id;

    @NotNull
    private Integer version;

    @Size(max = 100)
    private String name;

    @Size(max = 50)
    private String shortName;

    @Pattern(regexp = "ENTERPRISE|GOVERNMENT|INDIVIDUAL")
    private String type;

    @Pattern(regexp = "^$|^[0-9A-HJ-NPQRTUWXY]{18}$")
    private String uscc;

    private String regionCode;
    private String address;
    private String industry;

    @Pattern(regexp = "A|B|C")
    private String level;

    /** 仅超管可改变所有权。 */
    private Long ownerId;

    @Size(max = 1000)
    private String remark;
}
