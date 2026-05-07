package com.company.crms.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注敏感字段（电话、邮箱、凭证号等）。
 *
 * <p>由 MyBatis 类型处理器与 Jackson 序列化器配合：
 * <ul>
 *   <li>持久化时 AES-256-GCM 加密；</li>
 *   <li>读取时透明解密；</li>
 *   <li>对外 JSON 输出时按 {@link Mask} 进行脱敏展示。</li>
 * </ul>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveField {
    Mask mask() default Mask.NONE;

    enum Mask {
        NONE,
        /** 138****0000 */
        PHONE,
        /** zs****@example.com */
        EMAIL,
        /** 第一字符 + ***（中文姓名） */
        NAME,
        /** 前 4 + 后 4 */
        ID_CARD
    }
}
