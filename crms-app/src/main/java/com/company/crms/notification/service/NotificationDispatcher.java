package com.company.crms.notification.service;

import com.company.crms.contract.entity.Contract;

/**
 * 通知分发器：合同到期 / 回款临期 / 逾期 / 客户合并等场景。
 * 各 Scene 都按"去重 + 偏好检查"策略落库通知 + 触发邮件（Phase G4-I3 实现）。
 */
public interface NotificationDispatcher {

    /** 合同到期提醒（默认提前 30 天）。 */
    void contractDue(Contract contract, int advanceDays);

    /** 回款计划临期（默认提前 7 天）。 */
    void paymentPlanDue(Long contractId, Long planId, int advanceDays);

    /** 回款计划逾期。 */
    void paymentPlanOverdue(Long contractId, Long planId, int overdueDays);
}
