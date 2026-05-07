-- =====================================================================
-- 系统参数初值（DSS §4.4）
-- =====================================================================

INSERT INTO system_param (id, param_key, param_value, description, updated_at) VALUES
  (1, 'reminder.contract_due_days',  '30',  '合同到期提醒提前天数', NOW()),
  (2, 'reminder.payment_due_days',   '7',   '回款计划到期提醒提前天数', NOW()),
  (3, 'reminder.overdue_cycle_days', '3',   '逾期重复提醒间隔天数', NOW()),
  (4, 'security.password_min_len',   '8',   '密码最小长度', NOW()),
  (5, 'security.login_max_failed',   '5',   '登录失败上限', NOW()),
  (6, 'security.login_lock_minutes', '15',  '登录锁定分钟数', NOW()),
  (7, 'storage.max_file_mb',         '50',  '附件最大大小（MB）', NOW()),
  (8, 'storage.allowed_extensions',  'pdf,doc,docx,xls,xlsx,jpg,jpeg,png', '允许上传的扩展名', NOW());
