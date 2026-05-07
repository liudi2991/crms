package com.company.crms.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解，与 {@code OperationLogAspect} 配合实现自动审计。
 *
 * <pre>
 * &#64;OperationLog(module = "客户", action = "新建客户")
 * public Long createCustomer(CreateCustomerDTO dto) { ... }
 * </pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    /** 模块名，例：合同 / 客户 / 回款。 */
    String module();

    /** 动作描述，例："新建合同"。 */
    String action();

    /** 操作类型：CREATE / UPDATE / DELETE / HARD_DELETE / LOGIN / EXPORT。 */
    String type() default "UPDATE";

    /** 是否记录请求参数（默认记录，硬删除等敏感场景可关闭）。 */
    boolean recordParams() default true;
}
