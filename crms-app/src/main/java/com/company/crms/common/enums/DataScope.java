package com.company.crms.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 数据范围。决定登录用户在受范围保护的查询/操作中能看到的数据集合。
 */
@Getter
@RequiredArgsConstructor
public enum DataScope {
    /** 仅本人创建/负责的数据。 */
    SELF,
    /** 本部门 + 子部门数据。 */
    DEPT,
    /** 全公司数据。 */
    ALL;

    public static DataScope of(String code) {
        if (code == null) {
            return SELF;
        }
        try {
            return DataScope.valueOf(code);
        } catch (IllegalArgumentException e) {
            return SELF;
        }
    }
}
