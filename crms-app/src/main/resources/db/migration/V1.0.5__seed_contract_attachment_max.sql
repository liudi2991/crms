-- =====================================================================
-- 新增系统参数：合同附件数量上限（UC-03-05，SRS §4.3 期望 20）
-- 与 reminder.payment_due_days 等参数同模式，可在「系统参数」页面调整。
-- =====================================================================

INSERT INTO system_param (id, param_key, param_value, description, updated_at)
VALUES (9, 'contract.attachment.max_count', '20', '单合同最大附件数量', NOW())
ON DUPLICATE KEY UPDATE
    param_value = IF(param_value = '10', '20', param_value),
    description = '单合同最大附件数量',
    updated_at  = NOW();
