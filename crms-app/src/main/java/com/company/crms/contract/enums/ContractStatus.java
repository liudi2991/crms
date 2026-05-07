package com.company.crms.contract.enums;

import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;

import java.util.EnumSet;
import java.util.Map;

/**
 * 合同状态机（DSS §3.3.4）。
 *
 * <pre>
 * DRAFT ──▶ EFFECTIVE ──▶ COMPLETED
 *    │           │
 *    ▼           ▼
 *  TERMINATED  TERMINATED
 *              │
 *              ▼
 *           EXPIRED  (定时任务在到期日 + 1 自动迁移)
 * </pre>
 */
public enum ContractStatus {
    DRAFT,
    EFFECTIVE,
    COMPLETED,
    TERMINATED,
    EXPIRED;

    private static final Map<ContractStatus, EnumSet<ContractStatus>> ALLOWED = Map.of(
            DRAFT,      EnumSet.of(EFFECTIVE, TERMINATED),
            EFFECTIVE,  EnumSet.of(COMPLETED, TERMINATED, EXPIRED),
            COMPLETED,  EnumSet.noneOf(ContractStatus.class),
            TERMINATED, EnumSet.noneOf(ContractStatus.class),
            EXPIRED,    EnumSet.noneOf(ContractStatus.class)
    );

    public static void assertTransition(ContractStatus from, ContractStatus to) {
        if (from == to || !ALLOWED.getOrDefault(from, EnumSet.noneOf(ContractStatus.class)).contains(to)) {
            throw new BizException(ErrorCode.CT_STATUS_INVALID,
                    "不允许从 " + from + " 流转到 " + to);
        }
    }

    public static ContractStatus of(String code) {
        try {
            return valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.CT_STATUS_INVALID, "未知合同状态：" + code);
        }
    }
}
