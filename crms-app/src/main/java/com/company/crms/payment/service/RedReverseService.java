package com.company.crms.payment.service;

import java.math.BigDecimal;

/**
 * 红冲服务（DSS §3.4.2）。
 *
 * <p>红冲流程：
 * <ol>
 *   <li>原 record 状态置为 REVERSED；</li>
 *   <li>新增 status=RED 的负数金额 record，引用原 record id；</li>
 *   <li>反查原 record 关联的全部 settlement，逐条按"反向核销"重算 plan.settled_amount；</li>
 *   <li>写 change_log + 通知财务负责人。</li>
 * </ol>
 */
public interface RedReverseService {

    Long redReverse(Long originalRecordId, BigDecimal redAmount, String reason);
}
