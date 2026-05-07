-- =====================================================================
-- 数据脱敏脚本（DSS §10.7）
-- 用途：从生产库 dump 出测试库前，对敏感字段做不可逆脱敏。
-- 适用：已 import 到独立环境（DESEN_DB），与生产库严格隔离。
-- 严禁直接对生产库执行。
-- =====================================================================

-- 0. 安全提示：执行前 SET FOREIGN_KEY_CHECKS=0; 完成后再恢复。
-- SET FOREIGN_KEY_CHECKS=0;

-- 1. 用户：手机号、邮箱、姓名
UPDATE iam_user
   SET phone     = CONCAT('1', LPAD(FLOOR(RAND(id) * 1e10), 10, '0')),
       email     = CONCAT('user', id, '@desensitized.local'),
       real_name = CONCAT('用户_', id);

-- 2. 客户联系人：手机号、邮箱、微信
UPDATE customer_contact
   SET phone  = CONCAT('1', LPAD(FLOOR(RAND(id) * 1e10), 10, '0')),
       email  = CONCAT('contact', id, '@desensitized.local'),
       wechat = CONCAT('wx_', id);

-- 3. 客户：USCC 用 'U' + 随机 17 位
UPDATE customer
   SET uscc = CONCAT('U', LPAD(FLOOR(RAND(id) * 1e17), 17, '0')),
       address = '地址已脱敏'
 WHERE uscc IS NOT NULL;

-- 4. 实际回款：凭证号
UPDATE payment_record
   SET voucher_no = CONCAT('V', LPAD(id, 12, '0')),
       payer = CONCAT('付款方_', id MOD 100);

-- 5. 操作日志：清空敏感字段
UPDATE operation_log
   SET params_json = '"[REDACTED]"',
       error_message = NULL
 WHERE op_type IN ('LOGIN', 'UPDATE', 'CREATE', 'HARD_DELETE');

-- 6. 硬删除日志：清快照
UPDATE hard_delete_log
   SET snapshot_json = JSON_OBJECT('redacted', true);

-- 7. 重置默认密码（所有用户重置为 Reset@123，bcrypt 预生成）
UPDATE iam_user
   SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       must_change_pwd = 1,
       failed_count = 0,
       locked_until = NULL;

-- SET FOREIGN_KEY_CHECKS=1;

-- 8. 确认行数（手工 review）
SELECT COUNT(1) AS user_count        FROM iam_user;
SELECT COUNT(1) AS customer_count    FROM customer;
SELECT COUNT(1) AS contract_count    FROM contract;
SELECT COUNT(1) AS payment_count     FROM payment_record;
