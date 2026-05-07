# CRMS 系统管理员手册

> 面向 **R05 系统管理员**（拥有 `system:manage`、`*:hard_delete` 等特权）。
> 业务操作请见 [用户操作手册](./user-manual.md)；部署/排障请见 [运维手册](./operations.md)。

---

## 1. 你能做但别人不能做

| 功能 | 路径 | 关键权限 |
| --- | --- | --- |
| 用户管理（增删改、改密、解锁、分配角色） | `#/system/users` | `system:manage` |
| 角色管理（增删改、绑权限） | `#/system/roles` | `system:manage` |
| 部门管理（树形维护） | `#/system/departments` | `system:manage` |
| 系统参数（提醒天数 / 附件上限等） | `#/system/params` | `system:manage` |
| 操作日志（全员审计） | `#/system/logs` | `system:manage` |
| 回收站（还原 / 硬删除） | `#/system/recycle` | `system:manage` + 硬删除特权 |

非超管的"系统管理员"只能管系统配置；**硬删除**只有 `super_admin=1` 的账号才能做（数据库字段，初始仅 `admin`）。

---

## 2. 用户管理

### 2.1 新建用户

`#/system/users` → 「+ 新建用户」：

| 字段 | 说明 |
| --- | --- |
| 用户名 | 登录名，唯一 |
| 真实姓名 | 显示用 |
| 部门 | 树形选择 |
| 数据范围 | `SELF` 本人 / `DEPT` 本部门 / `ALL` 全公司，**通常和角色挂钩** |
| 角色 | 多选，至少一个；默认密码由 `crms.security.default-password` 控制（`Crms@123456`） |

新建后系统会发系统参数里配置的初始密码（不发邮件，运维侧告知用户即可），用户首登强制改密。

### 2.2 重置密码

行内「重置密码」→ 输入新密码（≥ 8 位）→ 该用户下次登录强制再改。

### 2.3 解锁

用户输错 5 次后被锁 15 分钟。如果业务紧急，行内「解锁」立即生效，无需等待。

### 2.4 启用 / 停用

- 「停用」：用户保留，登录会被拒。
- 「删除」：软删进回收站，可还原。

### 2.5 分配角色

行内「分配角色」→ 多选角色码 → 保存即时生效（用户当前会话也会刷新权限）。

---

## 3. 角色管理

`#/system/roles`

### 3.1 内置 5 个角色

| 角色码 | 名称 | 数据范围 | 不可删除 |
| --- | --- | --- | --- |
| `R01_SALES` | 销售 / 业务员 | SELF | ✅ |
| `R02_SALES_MANAGER` | 销售主管 | DEPT | ✅ |
| `R03_FINANCE` | 财务 | ALL | ✅ |
| `R04_EXECUTIVE` | 高管 / 决策层 | ALL | ✅ |
| `R05_ADMIN` | 系统管理员 | ALL（含管理） | ✅ |

`builtin=1` 标志的角色 **只能编辑权限点，不能删除**，避免破坏 RBAC 矩阵。

### 3.2 自定义角色

需要时点 「+ 新建角色」：填角色码（如 `R10_AUDIT`）、名称、数据范围，然后到 **权限树** 勾选权限点保存。

### 3.3 权限点参考

完整列表 `#/system/roles → 编辑 → 权限树`，常用：

| 模块 | 关键权限码 |
| --- | --- |
| 客户 | `customer:list / create / update / delete / disable / hard_delete` |
| 合同 | `contract:list / create / update / delete / terminate / note / hard_delete` |
| 回款 | `payment:record / settle / red / import / hard_delete / plan` |
| 报表 | `report:dashboard / payment / aging / export` |
| 系统 | `system:manage / system:user / system:role / system:dept / system:param / system:recycle` |

---

## 4. 系统参数

`#/system/params`，参数表 `system_param`，所有可调项见下表。

### 4.1 修改方式

行内「编辑」→ 改值 → 保存。**多数参数实时生效**，不用重启。

### 4.2 参数说明

| Key | 默认值 | 含义 | 是否热生效 |
| --- | --- | --- | --- |
| `reminder.contract_due_days` | 30 | 合同到期提醒提前天数 | ✅ |
| `reminder.payment_due_days` | 7 | 回款计划到期提醒提前天数 | ✅ |
| `reminder.overdue_cycle_days` | 3 | 逾期重复提醒间隔 | ✅ |
| `security.password_min_len` | 8 | 密码最小长度 | ✅（下次改密生效） |
| `security.login_max_failed` | 5 | 登录失败上限 | ✅ |
| `security.login_lock_minutes` | 15 | 登录锁定分钟 | ✅ |
| `storage.max_file_mb` | 50 | 附件最大大小（MB），与 `application.yml` 中 multipart 限制需一致 | ⚠️ multipart 改了要重启 |
| `storage.allowed_extensions` | `pdf,doc,docx,xls,xlsx,jpg,jpeg,png` | 允许扩展名 | ⚠️（部分代码白名单仍写死，以代码为准） |
| `contract.attachment.max_count` | 20 | 单合同最大附件数 | ✅（每次上传时读） |

> 真实生效请以「重启后回归」为准；改完关键参数建议在测试环境验证。

---

## 5. 部门管理

`#/system/departments`：

- 树形结构，默认根 `公司`，下挂销售部 / 财务部 / 行政部 / IT 部等。
- 拖拽改父级；删除前需确认下面**没有用户与合同**。
- 部门主管字段决定 `R02 销售主管` 看到的范围。

---

## 6. 操作日志

`#/system/logs`，存储在 `operation_log` 表（异步写入，吞吐高）。

### 6.1 字段含义

| 字段 | 说明 |
| --- | --- |
| traceId | 与前端报错弹窗里的一致，**最重要的查问题钥匙** |
| 模块 | `客户 / 合同 / 回款 / 系统 / 通知` |
| 动作 | `CREATE / UPDATE / DELETE / IMPORT / RED / TRANSITION` 等 |
| 操作人 | 用户名 + 用户 ID |
| 入参 | 部分敏感请求会标注 `recordParams=false` 不记录 |
| 结果 | `success` / 失败原因 |

### 6.2 常用排查

- 用户报"我刚才操作没生效"：让其复制操作时间，按 traceId 或用户 + 时间窗反查；
- 看是谁删了某个客户：模块=`客户`，动作=`DELETE` + bizId 过滤；
- 异常激增：按错误码列倒序看高频。

操作日志**保留期由公司合规策略决定**，运维侧应纳入归档。

---

## 7. 回收站与硬删除

`#/system/recycle`

### 7.1 列表筛选

下拉选 `bizType`（`CUSTOMER` / `CONTRACT` / `PAYMENT_RECORD`），关键字模糊匹配名称。

### 7.2 还原

行内「还原」→ 记录回到原表，`is_deleted=0`。**软删时的级联子记录不会一起还原**（如合同还原后，附件 / 计划 / 回款仍在回收站；要分别还原）。

### 7.3 硬删除（不可逆）

**仅 super_admin = 1 的账号** 才能调，会：

1. 弹二次密码确认（`/auth/verify-password`）；
2. 提示再次确认；
3. 写 `hard_delete_log`（含原数据快照、原因、操作人）；
4. 物理删除当前表行 + 级联子表。

**不可恢复**。建议只对：
- 测试环境清理用；
- 业务方书面要求"确实需要彻底清"的合规场景。

---

## 8. 文件存储切换（local ↔ MinIO）

> 由 v1.0.5 起新增；详见 [迭代记录](./issues/UC-03-05-fixes.md)。

### 8.1 当前模式

```bash
curl -s http://localhost:8080/actuator/info  # 暂未暴露
# 或看启动日志，会有：
#   LocalFileStorage initialized at /.../uploads
#   或
#   MinioFileStorage: bucket crms ready
```

### 8.2 切到 MinIO（生产推荐）

`deploy/.env` 增加：

```bash
CRMS_STORAGE_TYPE=minio
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=...
MINIO_SECRET_KEY=...
MINIO_BUCKET=crms
```

重启后端：`docker compose restart crms-app`。

切换后：
- 文件落到 MinIO bucket；
- 前端拿到的 `previewUrl` 是预签名 URL（含 `X-Amz-Signature`），浏览器无需 token 直连下载；
- 多实例部署不再有"实例 A 写、实例 B 读不到"问题。

### 8.3 历史文件迁移

如果之前已用 local 模式跑了一段时间，**切换前**需把 `crms-app/uploads/` 下的文件批量上传到 MinIO 并更新 `file_object.bucket` / `object_key`。简易迁移：

```bash
# 1. 上传到 MinIO
docker exec crms-minio mc alias set local http://localhost:9000 admin password
docker cp crms-app/uploads/contract crms-minio:/tmp/
docker exec crms-minio mc cp --recursive /tmp/contract local/crms/contract

# 2. 改 file_object.bucket 为 crms（原本是 crms-local）
mysql -e "UPDATE crms.file_object SET bucket='crms' WHERE bucket='crms-local';"
```

具体迁移脚本可在切换前找运维定制。

---

## 9. 常用 SQL 速查

> 直接连 MySQL 或用 phpMyAdmin / DBeaver。生产建议**只读账号**。

```sql
-- 查某用户最近 24 小时操作
SELECT created_at, module, action, biz_id, success, message
FROM operation_log
WHERE user_id = (SELECT id FROM iam_user WHERE username = 'someone')
  AND created_at > NOW() - INTERVAL 1 DAY
ORDER BY created_at DESC LIMIT 200;

-- 查某客户的所有合同与回款汇总
SELECT c.code, c.name, c.amount,
       COALESCE(SUM(pr.amount), 0) AS paid
FROM contract c
LEFT JOIN payment_record pr ON pr.contract_id = c.id
WHERE c.customer_id = ?
GROUP BY c.id;

-- 解锁某用户（极端故障时直接改库）
UPDATE iam_user SET failed_count = 0, locked_until = NULL WHERE username = 'lockedguy';

-- 查回收站某类型有多少条
SELECT COUNT(*) FROM customer WHERE is_deleted = 1;
```

---

## 10. 紧急情况手册

| 现象 | 优先动作 |
| --- | --- |
| admin 也登不进去 | DB 直改 `iam_user.password_hash`（V1.0.4 同款 bcrypt），见 [V1.0.4 迁移](../crms-app/src/main/resources/db/migration/V1.0.4__fix_admin_password.sql) |
| 全员报 401 | 查 Redis 是否挂；Sa-Token 走 redis-jackson |
| 突然全员报无权限 | 查是不是有人在 `iam_role_permission` 误删；用最近一次备份回滚 |
| 数据库连接炸了 | 看 `actuator/health` 是否 `DOWN`；查 hikari 池配置；联系运维扩容 |
| 附件下载 403 | 切到 MinIO 后预签名 URL 过期（默认 10 分钟），刷新页面重新拿 URL |

---

## 11. 关联文档

- [用户操作手册](./user-manual.md)（业务用户）
- [运维手册](./operations.md)（部署、监控、备份）
- [安全自查清单](./security-checklist.md)（OWASP / 上线前）
- [SRS](./srs.md) · [DSS](./dss.md)
- [FAQ](./faq.md)
- [验收指南](./acceptance.md) · [人工验收清单](./acceptance-checklist.md)
