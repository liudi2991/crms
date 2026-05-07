package com.company.crms.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 业务错误码枚举。对应 DSS §5.3 错误码规范。
 *
 * <p>命名约定：模块两位 + 序号三位（{@code CT-001}）。
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    SUCCESS("0", "OK"),

    // 系统/通用
    SYS_ERROR("SYS-500", "系统错误"),
    VALIDATION_ERROR("VAL-400", "参数校验失败"),
    NOT_FOUND("SYS-404", "资源不存在"),

    // 鉴权
    AUTH_UNAUTHORIZED("AUTH-401", "未登录或会话已过期"),
    AUTH_FORBIDDEN("AUTH-403", "权限不足"),
    AUTH_PWD_VERIFY_REQUIRED("AUTH-410", "需要二次密码校验"),

    // IAM
    IAM_LOGIN_FAILED("IAM-001", "用户名或密码错误"),
    IAM_ACCOUNT_LOCKED("IAM-002", "账号已被锁定"),
    IAM_ACCOUNT_DISABLED("IAM-003", "账号已被禁用"),
    IAM_PASSWORD_WEAK("IAM-004", "密码强度不足"),
    IAM_OLD_PASSWORD_INCORRECT("IAM-005", "原密码不正确"),

    // 客户
    CU_NOT_FOUND("CU-001", "客户不存在"),
    CU_USCC_DUPLICATE("CU-002", "统一社会信用代码已存在"),
    CU_HAS_CONTRACTS("CU-003", "客户存在合同，不能硬删除"),
    CU_MERGE_INVALID("CU-004", "客户合并参数非法"),
    CU_DISABLED("CU-005", "客户已停用"),

    // 合同
    CT_NOT_FOUND("CT-001", "合同不存在"),
    CT_STATUS_INVALID("CT-002", "合同状态不允许此操作"),
    CT_AMOUNT_INVALID("CT-003", "合同金额非法"),
    CT_ATTACHMENT_LIMIT("CT-004", "附件数量已达上限"),
    CT_ATTACHMENT_TYPE("CT-005", "附件类型不允许"),

    // 回款
    PM_PLAN_NOT_FOUND("PM-001", "回款计划不存在"),
    PM_PLAN_AMOUNT_MISMATCH("PM-002", "回款计划金额合计与合同金额不匹配"),
    PM_PLAN_SETTLED("PM-003", "回款计划已结清"),
    PM_RED_AMOUNT_INVALID("PM-004", "红冲金额非法"),
    PM_RED_REVERSED("PM-005", "记录已红冲"),
    PM_IMPORT_FAILED("PM-006", "批量导入失败"),

    // 通知
    NT_NOT_FOUND("NT-001", "通知不存在"),

    // 文件
    FILE_NOT_FOUND("FL-001", "文件不存在"),
    FILE_UPLOAD_FAILED("FL-002", "文件上传失败"),
    FILE_SIZE_LIMIT("FL-003", "文件大小超出限制"),
    FILE_TYPE_NOT_ALLOWED("FL-004", "文件类型不允许"),

    // 系统管理
    SY_PARAM_NOT_FOUND("SY-001", "系统参数不存在"),
    SY_HARD_DELETE_DENIED("SY-002", "无硬删除权限");

    private final String code;
    private final String message;
}
