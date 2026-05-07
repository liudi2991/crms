-- =====================================================================
-- 默认部门 + 超级管理员
-- 默认密码：Admin@123456（password_hash 占位，首次登录强制改密）
-- 占位 hash 由应用启动时校验并替换为真实 BCrypt 值。
-- =====================================================================

-- 默认部门（公司根 + 销售部 + 财务部）
INSERT INTO iam_department (id, parent_id, name, full_path, sort, created_at, updated_at) VALUES
  (1, 0, '公司',     '/公司',           1,  NOW(), NOW()),
  (2, 1, '销售部',   '/公司/销售部',     10, NOW(), NOW()),
  (3, 1, '财务部',   '/公司/财务部',     20, NOW(), NOW()),
  (4, 1, '管理部',   '/公司/管理部',     30, NOW(), NOW());

-- 默认超级管理员（用户名 admin / 密码 Admin@123456）
-- bcrypt('Admin@123456') 的预生成值（实际值在 init-data 启动钩子中校验/重置）
INSERT INTO iam_user (
  id, username, password_hash, real_name, dept_id, data_scope, status,
  must_change_pwd, super_admin, created_at, updated_at
) VALUES (
  1, 'admin',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  '超级管理员', 4, 'ALL', 'ACTIVE',
  1, 1, NOW(), NOW()
);

-- 绑定 R05 角色
INSERT INTO iam_user_role (id, user_id, role_id) VALUES (1, 1, 5);

-- =====================================================================
-- 字典通过权限码 + 系统参数表组合表达，详见 V1.0.3
-- =====================================================================
