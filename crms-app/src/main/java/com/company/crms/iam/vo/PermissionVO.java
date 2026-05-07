package com.company.crms.iam.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PermissionVO {
    private String code;
    private String name;
    private String type;
    private String parentCode;
    private Integer sort;
    private List<PermissionVO> children = new ArrayList<>();
}
