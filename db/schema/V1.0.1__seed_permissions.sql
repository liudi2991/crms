-- =====================================================================
-- 内置角色与权限点（DSS §2.3）
-- 角色：R01 销售 / R02 销售主管 / R03 财务 / R04 高管 / R05 系统管理员
-- =====================================================================

-- 角色
INSERT INTO iam_role (id, code, name, data_scope, builtin, sort, created_at, updated_at) VALUES
  (1, 'R01_SALES',         '销售/业务员',        'SELF', 1, 10, NOW(), NOW()),
  (2, 'R02_SALES_MANAGER', '销售主管',           'DEPT', 1, 20, NOW(), NOW()),
  (3, 'R03_FINANCE',       '财务',               'ALL',  1, 30, NOW(), NOW()),
  (4, 'R04_EXECUTIVE',     '高管/决策层',        'ALL',  1, 40, NOW(), NOW()),
  (5, 'R05_ADMIN',         '系统管理员',         'ALL',  1, 50, NOW(), NOW());

-- 权限点（菜单 + 操作 + 特殊）
INSERT INTO iam_permission (id, code, name, type, parent_code, sort) VALUES
  -- 客户
  (101, 'customer:list',          '客户列表',     'BUTTON', NULL, 10),
  (102, 'customer:create',        '新建客户',     'BUTTON', NULL, 11),
  (103, 'customer:update',        '编辑客户',     'BUTTON', NULL, 12),
  (104, 'customer:delete',        '软删除客户',   'BUTTON', NULL, 13),
  (105, 'customer:hard_delete',   '硬删除客户',   'SPECIAL', NULL, 14),
  (106, 'customer:merge',         '合并客户',     'BUTTON', NULL, 15),
  (107, 'customer:disable',       '启停客户',     'BUTTON', NULL, 16),
  -- 合同
  (201, 'contract:list',          '合同列表',     'BUTTON', NULL, 20),
  (202, 'contract:create',        '新建合同',     'BUTTON', NULL, 21),
  (203, 'contract:update',        '编辑合同',     'BUTTON', NULL, 22),
  (204, 'contract:delete',        '软删除合同',   'BUTTON', NULL, 23),
  (205, 'contract:hard_delete',   '硬删除合同',   'SPECIAL', NULL, 24),
  (206, 'contract:terminate',     '终止合同',     'BUTTON', NULL, 25),
  (207, 'contract:export',        '导出合同',     'BUTTON', NULL, 26),
  (208, 'contract:note',          '合同备注',     'BUTTON', NULL, 27),
  -- 回款
  (301, 'payment:list',           '回款列表',     'BUTTON', NULL, 30),
  (302, 'payment:plan',           '制定回款计划', 'BUTTON', NULL, 31),
  (303, 'payment:record',         '登记实际回款', 'BUTTON', NULL, 32),
  (304, 'payment:import',         '批量导入回款', 'BUTTON', NULL, 33),
  (305, 'payment:red_reverse',    '红冲',         'BUTTON', NULL, 34),
  (306, 'payment:settle',         '手工核销',     'BUTTON', NULL, 35),
  (307, 'payment:hard_delete',    '硬删除回款',   'SPECIAL', NULL, 36),
  -- 报表
  (401, 'report:dashboard',       '看板',         'BUTTON', NULL, 40),
  (402, 'report:payment',         '回款报表',     'BUTTON', NULL, 41),
  (403, 'report:export',          '报表导出',     'BUTTON', NULL, 42),
  -- 系统管理
  (501, 'system:manage',          '系统管理',     'MENU',   NULL, 50),
  (502, 'system:user',            '用户管理',     'BUTTON', 'system:manage', 51),
  (503, 'system:role',            '角色管理',     'BUTTON', 'system:manage', 52),
  (504, 'system:dept',            '部门管理',     'BUTTON', 'system:manage', 53),
  (505, 'system:param',           '系统参数',     'BUTTON', 'system:manage', 54),
  (506, 'system:log',             '操作日志',     'BUTTON', 'system:manage', 55),
  (507, 'system:recycle',         '回收站',       'BUTTON', 'system:manage', 56);

-- R01 销售：自己客户/合同的增改 + 回款登记
INSERT INTO iam_role_permission (id, role_id, permission_code) VALUES
  (1001, 1, 'customer:list'),
  (1002, 1, 'customer:create'),
  (1003, 1, 'customer:update'),
  (1004, 1, 'customer:delete'),
  (1005, 1, 'contract:list'),
  (1006, 1, 'contract:create'),
  (1007, 1, 'contract:update'),
  (1008, 1, 'contract:delete'),
  (1009, 1, 'contract:terminate'),
  (1010, 1, 'payment:list'),
  (1011, 1, 'payment:plan'),
  (1012, 1, 'payment:record');

-- R02 销售主管：本部门数据 + 备注
INSERT INTO iam_role_permission (id, role_id, permission_code) VALUES
  (2001, 2, 'customer:list'),
  (2002, 2, 'customer:disable'),
  (2003, 2, 'contract:list'),
  (2004, 2, 'contract:export'),
  (2005, 2, 'contract:note'),
  (2006, 2, 'payment:list'),
  (2007, 2, 'report:dashboard'),
  (2008, 2, 'report:payment');

-- R03 财务：全公司 + 回款核销/红冲/导入
INSERT INTO iam_role_permission (id, role_id, permission_code) VALUES
  (3001, 3, 'customer:list'),
  (3002, 3, 'contract:list'),
  (3003, 3, 'contract:export'),
  (3004, 3, 'payment:list'),
  (3005, 3, 'payment:record'),
  (3006, 3, 'payment:import'),
  (3007, 3, 'payment:red_reverse'),
  (3008, 3, 'payment:settle'),
  (3009, 3, 'report:dashboard'),
  (3010, 3, 'report:payment'),
  (3011, 3, 'report:export');

-- R04 高管：只读看板与报表
INSERT INTO iam_role_permission (id, role_id, permission_code) VALUES
  (4001, 4, 'customer:list'),
  (4002, 4, 'contract:list'),
  (4003, 4, 'payment:list'),
  (4004, 4, 'report:dashboard'),
  (4005, 4, 'report:payment'),
  (4006, 4, 'report:export');

-- R05 系统管理员：全部（含硬删除）
INSERT INTO iam_role_permission (id, role_id, permission_code)
SELECT 5000 + id, 5, code FROM iam_permission;
