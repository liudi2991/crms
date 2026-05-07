package com.company.crms.iam.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DepartmentVO {
    private Long id;
    private Long parentId;
    private String name;
    private String fullPath;
    private Integer sort;
    private Integer version;
    private Long userCount;
    private List<DepartmentVO> children = new ArrayList<>();
}
