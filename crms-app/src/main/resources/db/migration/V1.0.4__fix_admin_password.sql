-- =====================================================================
-- 修复 V1.0.0 中 admin 用户的 password_hash。
--
-- V1.0.0 注释声称默认密码为 Admin@123456，但实际写入的 hash
--   $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- 是众所周知的 bcrypt("password") 占位值，且没有任何启动钩子来替换它，
-- 导致使用 Admin@123456 登录始终失败。
--
-- 本迁移：
--   1) 仅当 hash 仍是占位值时，重置为 bcrypt("Admin@123456", cost=10)。
--   2) 清零 failed_count / locked_until，避免历史失败次数把账号锁住。
--   3) 保持 must_change_pwd = 1，首次登录强制改密。
-- =====================================================================

UPDATE iam_user
   SET password_hash   = '$2a$10$eoBTtlFjf.c5rC9mRnfu6ugfe1DergrL2Vc2JR3PK3b6vhusfxk7W',
       failed_count    = 0,
       locked_until    = NULL,
       status          = 'ACTIVE',
       must_change_pwd = 1,
       updated_at      = NOW()
 WHERE username = 'admin'
   AND password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';
