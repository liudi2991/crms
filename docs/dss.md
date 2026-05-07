# 合同回款管理系统 开发规格说明书（DSS）

| 项目名称 | 合同回款管理系统（CRMS） |
| --- | --- |
| 文档版本 | V1.1（草案） |
| 编写日期 | 2026-04-30 |
| 文档状态 | 待评审 |
| 文档类型 | 开发规格说明书（Development Specification） |
| 关联文档 | 《合同回款管理系统 软件需求规格说明书 V1.1》（以下简称 SRS） |

---

## 1. 引言

### 1.1 编写目的
本文档面向 CRMS 项目研发团队，将 SRS 中的业务需求转换为可直接执行的技术方案，覆盖系统架构、技术选型、数据库设计、接口规范、安全设计、部署运维与代码规范，是开发、测试、运维的统一技术基线。

### 1.2 读者对象
- 后端工程师、前端工程师
- DBA 与运维工程师（兼）
- 测试工程师
- 项目经理与技术负责人

### 1.3 术语
| 术语 | 含义 |
| --- | --- |
| HLD | High-Level Design，概要设计 |
| DD | Detailed Design，详细设计 |
| DDL | Data Definition Language，数据定义语言 |
| DTO | Data Transfer Object，数据传输对象 |
| VO | View Object，视图对象 |
| BO | Business Object，业务对象 |
| RBAC | 基于角色的访问控制 |
| IDOR | Insecure Direct Object Reference，越权访问 |

### 1.4 设计原则
1. **领域驱动**：按"客户 / 合同 / 回款 / 通知 / 系统管理"五大领域划分模块边界，模块间通过应用服务接口交互；
2. **前后端分离**：前端独立部署，通过 RESTful API 与后端交互，OpenAPI 即接口契约；
3. **可演进**：为二期能力（审批流、外部集成、移动端）预留扩展点，避免重写；
4. **安全默认开**：所有写接口默认走鉴权 + 数据范围校验，敏感字段默认加密；
5. **可观测**：统一日志、监控指标、链路追踪从第一天接入；
6. **简单优先**：本期不引入分布式微服务，单体应用配合模块化即可满足规模目标。

---

## 2. 总体设计

### 2.1 系统逻辑架构

```
┌────────────────────────────────────────────────────────────────────────┐
│                              用户（PC 浏览器）                          │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ HTTPS
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     反向代理层（Nginx）：HTTPS 卸载、静态资源、限流     │
└──────────────────┬───────────────────────────────────┬─────────────────┘
                   │                                   │
       ┌───────────▼───────────┐           ┌───────────▼──────────────┐
       │  前端静态站（Vue 3）  │           │  后端应用（Spring Boot）  │
       │  Vite 构建产物        │           │  REST API + 定时任务      │
       └───────────────────────┘           └───────────────┬──────────┘
                                                           │
                       ┌───────────────────┬───────────────┼───────────────┐
                       ▼                   ▼               ▼               ▼
                 ┌──────────┐        ┌───────────┐   ┌──────────┐   ┌────────────┐
                 │  MySQL   │        │   Redis   │   │  MinIO   │   │  日志/监控  │
                 │ (主从)   │        │  缓存/锁  │   │ 附件存储 │   │ ELK + Prom │
                 └──────────┘        └───────────┘   └──────────┘   └────────────┘
```

### 2.2 应用分层架构（后端）

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Web 层（Controller）                                      │
│    - 接收 HTTP 请求、参数校验、统一响应封装                  │
├─────────────────────────────────────────────────────────────┤
│ 2. 应用服务层（Application Service）                          │
│    - 业务编排、事务边界、跨领域协作                          │
├─────────────────────────────────────────────────────────────┤
│ 3. 领域层（Domain）                                           │
│    - 实体、值对象、领域服务、领域事件                        │
├─────────────────────────────────────────────────────────────┤
│ 4. 基础设施层（Infrastructure）                               │
│    - 持久化（MyBatis-Plus）、缓存、消息、外部 SDK            │
└─────────────────────────────────────────────────────────────┘
                       横切关注点
   认证 / 权限 / 审计 / 日志 / 异常 / 限流 / 国际化 / 链路追踪
```

### 2.3 模块划分

| 模块代号 | 名称 | 职责 | 关键聚合根 |
| --- | --- | --- | --- |
| `iam` | 身份与权限 | 用户、角色、权限、部门、登录、会话 | `User` / `Role` / `Department` |
| `customer` | 客户档案 | 客户主数据、联系人、合并、查重 | `Customer` / `Contact` |
| `contract` | 合同管理 | 合同、附件、变更记录 | `Contract` / `ContractAttachment` |
| `payment` | 回款管理 | 回款计划、实际回款、核销、账龄 | `PaymentPlan` / `PaymentRecord` |
| `report` | 报表看板 | KPI、趋势、账龄、导出 | （只读视图） |
| `notification` | 通知 | 站内信、定时触发 | `Notification` |
| `system` | 系统管理 | 字典、参数、操作日志、回收站 | `OperationLog` / `SystemParam` |
| `common` | 公共能力 | 异常、响应封装、加密、工具 | — |

### 2.4 部署架构（生产）

```
                         ┌─ 反向代理（Nginx，主备 2 台）─┐
                         │                                │
            ┌─────────────────────────────────────────────┴───────────────────┐
            │                                                                 │
   ┌────────▼────────┐                                              ┌─────────▼────────┐
   │ App-1 (Docker)  │   ◀── 同 LB 集群 ──▶                         │ App-2 (Docker)   │
   │  Spring Boot    │                                              │  Spring Boot     │
   └────┬───────┬────┘                                              └────┬───────┬─────┘
        │       │                                                        │       │
        ▼       ▼                                                        ▼       ▼
   ┌─────────┐ ┌───────────┐                                       ┌─────────┐ ┌───────────┐
   │  MySQL  │ │  Redis    │ ◀── 同实例 ──▶                        │  MinIO  │ │   备份盘  │
   │ Master  │ │ (单点)    │                                       │ 4 节点  │ │ 每日全量  │
   └────┬────┘ └───────────┘                                       └─────────┘ └───────────┘
        │
        ▼
   ┌─────────┐
   │  MySQL  │
   │  Slave  │  （只读，用于报表 + 灾备）
   └─────────┘
```

> 测试 / 预发环境单台部署即可；生产建议至少应用层 2 台 + DB 主从。

### 2.5 技术栈与版本

| 分类 | 选型 | 版本 | 备注 |
| --- | --- | --- | --- |
| 后端语言 | Java | 17（LTS） | 使用 record、Pattern Matching |
| 后端框架 | Spring Boot | 3.2.x | Jakarta EE 9+ |
| ORM | MyBatis-Plus | 3.5.x | 与 MyBatis 兼容，简化 CRUD |
| 鉴权 | Sa-Token | 1.38.x | 替代 Spring Security，集成简单 |
| 校验 | Jakarta Validation | 3.x | `@Valid` |
| 任务调度 | Spring `@Scheduled` + ShedLock | — | 单机调度 + 分布式锁防重复 |
| 文档 | Springdoc-OpenAPI | 2.x | 自动生成 OpenAPI 3 |
| 缓存 | Redis | 7.2 | 单实例 + RDB 备份 |
| 数据库 | MySQL | 8.0 | 主从复制 |
| 连接池 | HikariCP | 5.x | Spring Boot 默认 |
| 对象存储 | MinIO | 最新 | 兼容 S3 协议 |
| 前端框架 | Vue | 3.4.x | Composition API |
| 前端语言 | TypeScript | 5.x | strict 模式 |
| 构建工具 | Vite | 5.x | — |
| UI 库 | Element Plus | 2.6.x | — |
| 状态管理 | Pinia | 2.x | — |
| HTTP | Axios | 1.x | 拦截器统一处理 |
| 表单 | VForm 或 Element Plus 原生 | — | — |
| 图表 | ECharts | 5.x | 看板与报表 |
| 容器化 | Docker / Docker Compose | 24+ | — |
| CI/CD | **Jenkins** | LTS | 公司统一 CI/CD 平台 |
| 健康检查 | Spring Boot Actuator | — | `/actuator/health` 暴露给 Nginx 与值班 |
| 日志聚合（可选） | 文件 + 定期归档 | — | 本期不引入 ELK；通过 Nginx + 应用日志按天滚动 |

---

## 3. 模块详细设计

### 3.1 IAM（身份与权限）

#### 3.1.1 领域模型
```
User (1)──N (1)──Role (M)──N (1)──Permission
 │
 └─ belongsTo ──▶ Department (tree)
```

- `User`：账户信息、所属部门、关联角色（多对多，但本期一人一角色简化为多对一）；
- `Role`：内置 5 类角色（R01–R05），代码 + 名称；
- `Permission`：菜单权限点、操作权限点（按钮级）、特殊权限点（如"硬删除"）；
- `Department`：父子树形结构（最多 5 级）。

#### 3.1.2 数据范围（Data Scope）
通过用户的 `data_scope` 字段决定：
- `SELF`：仅本人创建/负责的数据；
- `DEPT`：本部门 + 子部门数据；
- `ALL`：全公司数据。

实现：在 MyBatis-Plus 拦截器中按当前登录用户的数据范围 **自动追加 SQL 条件**（`WHERE owner_id = ?` / `WHERE dept_id IN (?)`），避免在每个 Mapper 写重复逻辑。

#### 3.1.3 鉴权流程

```
Login → 校验账号密码 → 写 Sa-Token 会话（30 分钟空闲过期）→ 返回 token
        │
        ▼
后续请求：拦截器校验 token → 加载用户上下文 → 写 ThreadLocal
        │
        ▼
Controller 注解 @SaCheckPermission("contract:edit") → 校验权限点
        │
        ▼
MyBatis-Plus 数据范围拦截器 → 追加数据范围条件
```

#### 3.1.4 硬删除特殊处理
- 在 `Permission` 表中预置权限点：`customer:hard_delete`、`contract:hard_delete`、`payment:hard_delete`；
- 仅"超级管理员"账户可被分配；
- 硬删除接口必须要求二次密码校验（独立接口 `POST /api/iam/verify-password`，返回短期 token，3 分钟内有效）；
- 删除前先把记录关键字段快照写入 `hard_delete_log` 表，再执行物理删除（同事务）。

### 3.2 客户档案（customer）

#### 3.2.1 关键流程：客户合并
```
sequenceDiagram
    管理员-->>系统: 选择源客户 B、目标客户 A
    系统-->>系统: 校验：A、B 都启用 / 不存在循环
    系统-->>DB: BEGIN TRANSACTION
    系统-->>DB: UPDATE contract SET customer_id = A WHERE customer_id = B
    系统-->>DB: UPDATE contact SET customer_id = A WHERE customer_id = B
    系统-->>DB: UPDATE customer SET status='MERGED', merged_to=A WHERE id=B
    系统-->>DB: INSERT change_log(...)
    系统-->>DB: COMMIT
    系统-->>用户: 返回成功，列出迁移条数
```
要点：
- 全程同事务，保证原子性；
- 联系人按"姓名 + 电话"去重，重复的不迁移并提示；
- 合并不可撤销（仅可通过新建/再合并修复）。

#### 3.2.2 查重算法
- 新建/编辑时按以下规则提示已有客户：
  1. 统一社会信用代码完全相同（强提示，建议不允许重复）；
  2. 客户名称完全相同 → 高相似（提示但允许）；
  3. 客户名称去除"有限公司/股份"等后缀后相同 → 中相似（提示但允许）。

### 3.3 合同管理（contract）

#### 3.3.1 合同状态机
```
[草稿 DRAFT]
   │ 提交
   ▼
[履行中 EXECUTING] ──回款结清──▶ [已关闭 CLOSED]
   │ 终止
   ▼
[已终止 TERMINATED]
```
- 仅 `DRAFT`、`EXECUTING` 可编辑；
- `CLOSED`、`TERMINATED` 仅可查看与添加备注；
- 删除限制：仅 `DRAFT` 可软删除，其它状态需走"终止"或硬删除。

#### 3.3.2 合同编号生成
- 规则：`HT-YYYYMM-NNNN`，`NNNN` 为月内自增；
- 实现：Redis `INCR contract:no:202604` + Lua 保证并发；
- 月初 0 点 ShedLock 任务重置上月计数（保留历史不删除）。

#### 3.3.3 附件存储
- 上传接口：`POST /api/files/upload?bizType=contract&bizId=xxx`；
- 单文件 ≤ 50MB，单合同 ≤ 20 个，仅允许扩展名：`pdf,jpg,jpeg,png,doc,docx,xls,xlsx`；
- 文件命名：`{bizType}/{yyyy/MM/dd}/{uuid}.{ext}`，避免重名；
- 病毒扫描：本期不强制，预留 ClamAV 接入扩展点。

### 3.4 回款管理（payment）

#### 3.4.1 回款计划与核销

**自动核销算法（先到期先核销）**：
```python
def auto_settle(record: PaymentRecord):
    plans = query_unsettled_plans(record.contract_id, order_by='plan_date ASC')
    remain = record.amount
    for p in plans:
        if remain <= 0: break
        unsettled = p.plan_amount - p.settled_amount
        settle = min(remain, unsettled)
        p.settled_amount += settle
        if p.settled_amount >= p.plan_amount:
            p.status = 'SETTLED'
        save(p)
        link(record, p, settle)  # 写核销关系表
        remain -= settle
    if remain > 0:
        record.unallocated_amount = remain  # 多收的留挂账
```

#### 3.4.2 红冲设计
- 红冲生成一条金额为负的 `payment_record`，并通过 `red_ref_id` 指向原记录；
- 同步反向核销：从最后核销的 `payment_plan` 开始回退；
- 红冲记录不可再红冲；
- 通过状态机控制：`NORMAL` → `RED_REVERSED`，原记录置为 `RED_REVERSED` 后不可再操作。

#### 3.4.3 逾期与账龄
- 每日 02:00 跑 ShedLock 任务，扫描 `plan_date < CURRENT_DATE AND settled_amount < plan_amount` 的计划；
- 标记 `is_overdue=1` 与 `overdue_days = DATEDIFF(NOW, plan_date)`；
- 触发 `notification:overdue` 事件，按规则推送站内信。

账龄分桶 SQL（核心计算）示例：
```sql
SELECT
  c.customer_id,
  SUM(CASE WHEN DATEDIFF(CURDATE(), pp.plan_date) BETWEEN 1 AND 30 THEN pp.unsettled_amount ELSE 0 END) AS aging_0_30,
  SUM(CASE WHEN DATEDIFF(CURDATE(), pp.plan_date) BETWEEN 31 AND 60 THEN pp.unsettled_amount ELSE 0 END) AS aging_31_60,
  SUM(CASE WHEN DATEDIFF(CURDATE(), pp.plan_date) BETWEEN 61 AND 90 THEN pp.unsettled_amount ELSE 0 END) AS aging_61_90,
  SUM(CASE WHEN DATEDIFF(CURDATE(), pp.plan_date) > 90 THEN pp.unsettled_amount ELSE 0 END) AS aging_90_plus
FROM payment_plan pp
JOIN contract c ON pp.contract_id = c.id
WHERE pp.unsettled_amount > 0 AND pp.is_deleted = 0
GROUP BY c.customer_id;
```

### 3.5 通知与提醒（notification）

#### 3.5.1 触发引擎
- 内部使用 Spring `ApplicationEventPublisher` 发布领域事件，`@EventListener(condition = ...)` 订阅；
- 定时任务（每 5 分钟）扫描即将到期合同/计划，避免长时间阻塞业务事务；
- 通知去重：同一 `(user_id, scene, biz_id)` 当天不重复推送。

#### 3.5.2 通知中心数据模型
- `notification`：接收人、场景类型、业务对象、标题、内容、跳转 URL、是否已读、创建时间；
- `notification_setting`：用户级偏好（场景级开/关、提前天数覆盖）。

### 3.6 系统管理与回收站

#### 3.6.1 回收站
- 所有软删除对象都进 `recycle_bin` 视图（按 `is_deleted=1` 聚合查询）；
- 还原：将 `is_deleted` 置 0 + 校验关联完整性（如还原合同时，客户必须存在且未删除）；
- 自动清理：保留 90 天后的软删除记录由定时任务硬删除（写日志）。

#### 3.6.2 操作日志
- AOP 切面 `@OperationLog` 自动记录关键操作；
- 日志包含：操作人、IP、UA、模块、操作类型、目标对象类型与 ID、关键参数（脱敏后 JSON）、结果、耗时；
- 写日志走异步线程池 + 数据库批量插入，避免影响主链路性能。

---

## 4. 数据库设计

### 4.1 数据库总体规范
- **字符集**：`utf8mb4`，排序规则 `utf8mb4_0900_ai_ci`；
- **引擎**：InnoDB；
- **主键**：统一使用雪花 ID（BIGINT），由后端生成；
- **公共字段**（所有业务表必须包含）：
  - `id` BIGINT PK
  - `created_by` BIGINT、`created_at` DATETIME
  - `updated_by` BIGINT、`updated_at` DATETIME
  - `is_deleted` TINYINT DEFAULT 0
  - `version` INT DEFAULT 0（乐观锁，MyBatis-Plus 自动管理）
- **金额字段**：`DECIMAL(18,2)`；
- **加密字段**：使用 `VARBINARY` 存储 AES-256-GCM 密文，应用层透明加解密；
- **索引规范**：所有外键、查询条件、唯一约束必须建索引；命名 `idx_table_col` / `uk_table_col`。

### 4.2 表清单

| # | 表名 | 模块 | 说明 |
| --- | --- | --- | --- |
| 1 | `iam_user` | iam | 用户 |
| 2 | `iam_role` | iam | 角色 |
| 3 | `iam_permission` | iam | 权限点 |
| 4 | `iam_user_role` | iam | 用户-角色 |
| 5 | `iam_role_permission` | iam | 角色-权限 |
| 6 | `iam_department` | iam | 部门树 |
| 7 | `customer` | customer | 客户主数据 |
| 8 | `customer_contact` | customer | 客户联系人 |
| 9 | `contract` | contract | 合同 |
| 10 | `contract_attachment` | contract | 合同附件 |
| 11 | `payment_plan` | payment | 回款计划 |
| 12 | `payment_record` | payment | 实际回款 |
| 13 | `payment_settlement` | payment | 核销关系（多对多） |
| 14 | `notification` | notification | 站内信 |
| 15 | `notification_setting` | notification | 通知偏好 |
| 16 | `change_log` | system | 业务变更记录 |
| 17 | `operation_log` | system | 操作审计日志 |
| 18 | `hard_delete_log` | system | 硬删除审计日志 |
| 19 | `system_param` | system | 系统参数 |
| 20 | `file_object` | common | 文件元数据 |

### 4.3 关键表 DDL

> 完整 DDL 请放至 `db/schema/V1.0.0__init.sql`，以下展示业务核心表。

```sql
-- 部门
CREATE TABLE iam_department (
  id           BIGINT PRIMARY KEY,
  parent_id    BIGINT NOT NULL DEFAULT 0,
  name         VARCHAR(64) NOT NULL,
  full_path    VARCHAR(255) NOT NULL,
  sort         INT NOT NULL DEFAULT 0,
  created_by   BIGINT, created_at DATETIME NOT NULL,
  updated_by   BIGINT, updated_at DATETIME NOT NULL,
  is_deleted   TINYINT NOT NULL DEFAULT 0,
  version      INT NOT NULL DEFAULT 0,
  KEY idx_department_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户
CREATE TABLE iam_user (
  id            BIGINT PRIMARY KEY,
  username      VARCHAR(64)  NOT NULL,
  password_hash VARCHAR(128) NOT NULL,    -- bcrypt
  real_name     VARCHAR(64)  NOT NULL,
  phone         VARBINARY(255),           -- AES 加密
  email         VARCHAR(128),
  dept_id       BIGINT NOT NULL,
  data_scope    VARCHAR(16) NOT NULL,     -- SELF / DEPT / ALL
  status        VARCHAR(16) NOT NULL,     -- ACTIVE / DISABLED / LOCKED
  last_login_at DATETIME,
  failed_count  INT NOT NULL DEFAULT 0,
  created_by    BIGINT, created_at DATETIME NOT NULL,
  updated_by    BIGINT, updated_at DATETIME NOT NULL,
  is_deleted    TINYINT NOT NULL DEFAULT 0,
  version       INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_username (username),
  KEY idx_user_dept (dept_id)
) ENGINE=InnoDB;

-- 客户
CREATE TABLE customer (
  id              BIGINT PRIMARY KEY,
  code            VARCHAR(32) NOT NULL,         -- CU-YYYYMM-NNNN
  name            VARCHAR(100) NOT NULL,
  short_name      VARCHAR(50),
  type            VARCHAR(16) NOT NULL,         -- ENTERPRISE/GOV/INDIVIDUAL/PERSON/OTHER
  uscc            VARCHAR(32),                  -- 统一社会信用代码
  region_code     VARCHAR(12),
  address         VARCHAR(255),
  industry        VARCHAR(32),
  level           VARCHAR(4) NOT NULL DEFAULT 'C',
  owner_id        BIGINT NOT NULL,
  dept_id         BIGINT NOT NULL,
  status          VARCHAR(16) NOT NULL,         -- ACTIVE/DISABLED/MERGED
  merged_to       BIGINT,
  remark          TEXT,
  created_by      BIGINT, created_at DATETIME NOT NULL,
  updated_by      BIGINT, updated_at DATETIME NOT NULL,
  is_deleted      TINYINT NOT NULL DEFAULT 0,
  version         INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_customer_code (code),
  UNIQUE KEY uk_customer_uscc (uscc),
  KEY idx_customer_name (name),
  KEY idx_customer_owner (owner_id),
  KEY idx_customer_dept (dept_id),
  KEY idx_customer_status (status, is_deleted)
) ENGINE=InnoDB;

-- 联系人
CREATE TABLE customer_contact (
  id           BIGINT PRIMARY KEY,
  customer_id  BIGINT NOT NULL,
  name         VARCHAR(64) NOT NULL,
  title        VARCHAR(64),
  phone        VARBINARY(255),               -- 加密
  email        VARBINARY(255),               -- 加密
  wechat       VARCHAR(64),
  is_primary   TINYINT NOT NULL DEFAULT 0,
  remark       VARCHAR(255),
  created_by   BIGINT, created_at DATETIME NOT NULL,
  updated_by   BIGINT, updated_at DATETIME NOT NULL,
  is_deleted   TINYINT NOT NULL DEFAULT 0,
  version      INT NOT NULL DEFAULT 0,
  KEY idx_contact_customer (customer_id)
) ENGINE=InnoDB;

-- 合同
CREATE TABLE contract (
  id                BIGINT PRIMARY KEY,
  code              VARCHAR(32) NOT NULL,         -- HT-YYYYMM-NNNN
  name              VARCHAR(100) NOT NULL,
  type              VARCHAR(16) NOT NULL,
  customer_id       BIGINT NOT NULL,              -- 强外键
  amount            DECIMAL(18,2) NOT NULL,
  signed_at         DATE NOT NULL,
  perform_start_at  DATE NOT NULL,
  perform_end_at    DATE NOT NULL,
  remind_days       INT,                          -- NULL 表示沿用系统参数 30
  owner_id          BIGINT NOT NULL,
  dept_id           BIGINT NOT NULL,
  status            VARCHAR(16) NOT NULL,         -- DRAFT/EXECUTING/CLOSED/TERMINATED
  remark            TEXT,
  created_by        BIGINT, created_at DATETIME NOT NULL,
  updated_by        BIGINT, updated_at DATETIME NOT NULL,
  is_deleted        TINYINT NOT NULL DEFAULT 0,
  version           INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_contract_code (code),
  KEY idx_contract_customer (customer_id),
  KEY idx_contract_owner (owner_id),
  KEY idx_contract_status (status, is_deleted),
  KEY idx_contract_perform_end (perform_end_at)
) ENGINE=InnoDB;

-- 合同附件
CREATE TABLE contract_attachment (
  id              BIGINT PRIMARY KEY,
  contract_id     BIGINT NOT NULL,
  file_object_id  BIGINT NOT NULL,
  file_name       VARCHAR(255) NOT NULL,
  file_size       BIGINT NOT NULL,
  uploaded_by     BIGINT NOT NULL,
  uploaded_at     DATETIME NOT NULL,
  is_deleted      TINYINT NOT NULL DEFAULT 0,
  KEY idx_attachment_contract (contract_id)
) ENGINE=InnoDB;

-- 回款计划
CREATE TABLE payment_plan (
  id                  BIGINT PRIMARY KEY,
  contract_id         BIGINT NOT NULL,
  period_no           INT NOT NULL,                -- 第几期
  plan_date           DATE NOT NULL,
  plan_amount         DECIMAL(18,2) NOT NULL,
  settled_amount      DECIMAL(18,2) NOT NULL DEFAULT 0,
  unsettled_amount    DECIMAL(18,2) NOT NULL,      -- 冗余便于查询
  status              VARCHAR(16) NOT NULL,        -- PENDING / PARTIAL / SETTLED
  is_overdue          TINYINT NOT NULL DEFAULT 0,
  overdue_days        INT NOT NULL DEFAULT 0,
  remind_days         INT,                         -- NULL 表示沿用系统参数 7
  created_by          BIGINT, created_at DATETIME NOT NULL,
  updated_by          BIGINT, updated_at DATETIME NOT NULL,
  is_deleted          TINYINT NOT NULL DEFAULT 0,
  version             INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_plan_contract_period (contract_id, period_no),
  KEY idx_plan_status (status, is_overdue),
  KEY idx_plan_date (plan_date)
) ENGINE=InnoDB;

-- 实际回款
CREATE TABLE payment_record (
  id                  BIGINT PRIMARY KEY,
  contract_id         BIGINT NOT NULL,
  arrival_date        DATE NOT NULL,
  amount              DECIMAL(18,2) NOT NULL,
  payer               VARCHAR(100),
  voucher_no          VARBINARY(255),              -- 凭证号加密
  status              VARCHAR(16) NOT NULL,        -- NORMAL / RED_REVERSED / RED_RECORD
  red_ref_id          BIGINT,                      -- 红冲指向的原记录
  unallocated_amount  DECIMAL(18,2) NOT NULL DEFAULT 0,
  remark              VARCHAR(500),
  voucher_file_id     BIGINT,
  created_by          BIGINT, created_at DATETIME NOT NULL,
  updated_by          BIGINT, updated_at DATETIME NOT NULL,
  is_deleted          TINYINT NOT NULL DEFAULT 0,
  version             INT NOT NULL DEFAULT 0,
  KEY idx_record_contract (contract_id),
  KEY idx_record_arrival (arrival_date)
) ENGINE=InnoDB;

-- 核销关系
CREATE TABLE payment_settlement (
  id                BIGINT PRIMARY KEY,
  payment_plan_id   BIGINT NOT NULL,
  payment_record_id BIGINT NOT NULL,
  settle_amount     DECIMAL(18,2) NOT NULL,
  settle_at         DATETIME NOT NULL,
  settle_type       VARCHAR(16) NOT NULL,          -- AUTO / MANUAL / RED
  KEY idx_settlement_plan (payment_plan_id),
  KEY idx_settlement_record (payment_record_id)
) ENGINE=InnoDB;

-- 通知
CREATE TABLE notification (
  id            BIGINT PRIMARY KEY,
  receiver_id   BIGINT NOT NULL,
  scene         VARCHAR(32) NOT NULL,        -- CONTRACT_DUE / PAYMENT_OVERDUE ...
  biz_type      VARCHAR(32) NOT NULL,
  biz_id        BIGINT,
  title         VARCHAR(128) NOT NULL,
  content       VARCHAR(500) NOT NULL,
  link_url      VARCHAR(255),
  is_read       TINYINT NOT NULL DEFAULT 0,
  read_at       DATETIME,
  created_at    DATETIME NOT NULL,
  KEY idx_notification_receiver (receiver_id, is_read, created_at),
  KEY idx_notification_dedupe (receiver_id, scene, biz_id, created_at)
) ENGINE=InnoDB;

-- 操作日志（按月分表，命名 operation_log_YYYYMM）
CREATE TABLE operation_log (
  id           BIGINT PRIMARY KEY,
  operator_id  BIGINT NOT NULL,
  operator_ip  VARCHAR(64),
  module       VARCHAR(32) NOT NULL,
  op_type      VARCHAR(16) NOT NULL,         -- CREATE/UPDATE/DELETE/HARD_DELETE/LOGIN/EXPORT
  biz_type     VARCHAR(32),
  biz_id       BIGINT,
  params_json  TEXT,
  result       VARCHAR(16),
  duration_ms  INT,
  created_at   DATETIME NOT NULL,
  KEY idx_oplog_operator (operator_id, created_at),
  KEY idx_oplog_module (module, op_type, created_at)
) ENGINE=InnoDB;

-- 硬删除日志（永久保留）
CREATE TABLE hard_delete_log (
  id              BIGINT PRIMARY KEY,
  operator_id     BIGINT NOT NULL,
  biz_type        VARCHAR(32) NOT NULL,
  biz_id          BIGINT NOT NULL,
  snapshot_json   JSON NOT NULL,
  reason          VARCHAR(500),
  created_at      DATETIME NOT NULL,
  KEY idx_hd_biz (biz_type, biz_id),
  KEY idx_hd_operator (operator_id, created_at)
) ENGINE=InnoDB;

-- 系统参数
CREATE TABLE system_param (
  id            BIGINT PRIMARY KEY,
  param_key     VARCHAR(64) NOT NULL,
  param_value   VARCHAR(255) NOT NULL,
  description   VARCHAR(255),
  updated_by    BIGINT, updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_param_key (param_key)
) ENGINE=InnoDB;
```

### 4.4 系统参数初始化值

| key | value | 描述 |
| --- | --- | --- |
| `contract.remind.days_before` | 30 | 合同到期提前提醒天数 |
| `payment.remind.days_before` | 7 | 回款计划提前提醒天数 |
| `payment.overdue.notify.cycle_days` | 3 | 逾期重复提醒间隔 |
| `password.min.length` | 8 | 密码最小长度 |
| `login.max.failed` | 5 | 登录最大失败次数 |
| `login.lock.minutes` | 15 | 锁定时长（分钟） |
| `session.idle.minutes` | 30 | 会话空闲超时（分钟） |
| `recycle.bin.retain.days` | 90 | 回收站保留天数 |

### 4.5 索引设计建议

| 表 | 索引 | 场景 |
| --- | --- | --- |
| `customer` | `(status, is_deleted)`、`(name)`、`(uscc)` | 列表筛选、查重 |
| `contract` | `(customer_id)`、`(owner_id)`、`(perform_end_at)`、`(status, is_deleted)` | 列表、到期扫描 |
| `payment_plan` | `(plan_date)`、`(status, is_overdue)`、`(contract_id, period_no)` | 提醒任务、逾期任务、防重复 |
| `payment_record` | `(contract_id)`、`(arrival_date)` | 合同详情、按月统计 |
| `notification` | `(receiver_id, is_read, created_at DESC)` | 通知中心 |
| `operation_log` | `(operator_id, created_at DESC)` | 审计查询 |

---

## 5. 接口（API）规范

### 5.1 通用约定
- 协议：HTTPS；
- 编码：UTF-8；
- 风格：RESTful，资源名使用小写复数（`/customers`、`/contracts`）；
- 路径前缀：`/api/v1`；
- 鉴权：除 `/api/v1/auth/login` 外均需 `Authorization: Bearer <token>` 或 Cookie；
- 时间格式：ISO 8601（`2026-04-30T16:30:00+08:00`）；
- 金额：字符串，避免精度丢失，单位为元；
- 分页：`?page=1&size=20&sort=createdAt,desc`；
- 字段命名：JSON 使用 `camelCase`，DB 使用 `snake_case`。

### 5.2 统一响应封装
```json
{
  "code": "0",
  "message": "OK",
  "data": { ... },
  "traceId": "f1e2d3c4-..."
}
```
分页响应：
```json
{
  "code": "0",
  "data": {
    "items": [ ... ],
    "total": 1234,
    "page": 1,
    "size": 20
  }
}
```

### 5.3 错误码规范
- 格式：`<模块两位>-<错误码三位>`（例如 `CT-001`）；
- HTTP 状态码：业务异常返回 `200`，系统异常 `5xx`，鉴权 `401`，权限 `403`，参数 `400`，资源 `404`。

| 错误码 | 含义 |
| --- | --- |
| `0` | 成功 |
| `SYS-500` | 系统未知错误 |
| `AUTH-401` | 未登录或会话过期 |
| `AUTH-403` | 权限不足 |
| `VAL-400` | 参数校验失败 |
| `CU-001` | 客户不存在 |
| `CU-002` | 客户信用代码已存在 |
| `CT-001` | 合同状态不允许此操作 |
| `PM-001` | 回款计划已结清 |
| `PM-002` | 红冲金额不匹配 |
| `IAM-001` | 用户名或密码错误 |
| `IAM-002` | 账号被锁定 |

### 5.4 核心接口清单（节选）

> 完整版以 OpenAPI 文档为准（`/api/v1/swagger-ui.html`）。

#### 5.4.1 认证
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/auth/login` | 账号密码登录 |
| POST | `/api/v1/auth/logout` | 退出 |
| POST | `/api/v1/auth/change-password` | 修改密码 |
| POST | `/api/v1/auth/verify-password` | 二次密码校验（用于硬删除） |
| GET | `/api/v1/auth/me` | 当前用户信息 + 权限点 |

#### 5.4.2 客户
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/customers` | 新建客户 |
| GET | `/api/v1/customers` | 列表查询（分页 + 筛选） |
| GET | `/api/v1/customers/{id}` | 详情 |
| PUT | `/api/v1/customers/{id}` | 编辑 |
| DELETE | `/api/v1/customers/{id}` | 软删除 |
| DELETE | `/api/v1/customers/{id}/hard` | 硬删除（特权 + 二次密码） |
| POST | `/api/v1/customers/check-duplicate` | 查重 |
| POST | `/api/v1/customers/{id}/merge` | 合并到目标客户 |
| POST | `/api/v1/customers/{id}/disable` | 停用 |
| POST | `/api/v1/customers/{id}/enable` | 启用 |
| GET | `/api/v1/customers/{id}/contracts` | 客户的合同列表 |
| GET | `/api/v1/customers/{id}/payments` | 客户的回款汇总 |
| POST | `/api/v1/customers/{id}/contacts` | 新增联系人 |
| PUT | `/api/v1/customers/{id}/contacts/{contactId}` | 编辑联系人 |
| DELETE | `/api/v1/customers/{id}/contacts/{contactId}` | 删除联系人 |

#### 5.4.3 合同
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/contracts` | 新建合同（自动生成编号） |
| GET | `/api/v1/contracts` | 列表查询 |
| GET | `/api/v1/contracts/{id}` | 详情 |
| PUT | `/api/v1/contracts/{id}` | 编辑（写变更日志） |
| POST | `/api/v1/contracts/{id}/submit` | 草稿 → 履行中 |
| POST | `/api/v1/contracts/{id}/terminate` | 终止 |
| POST | `/api/v1/contracts/{id}/close` | 关闭 |
| GET | `/api/v1/contracts/{id}/changes` | 变更历史 |
| GET | `/api/v1/contracts/{id}/attachments` | 附件列表 |
| POST | `/api/v1/contracts/{id}/attachments` | 上传附件 |
| DELETE | `/api/v1/contracts/{id}/attachments/{attId}` | 删除附件 |
| POST | `/api/v1/contracts/{id}/notes` | 添加主管备注 |
| DELETE | `/api/v1/contracts/{id}` | 软删除 |
| DELETE | `/api/v1/contracts/{id}/hard` | 硬删除 |
| GET | `/api/v1/contracts/export` | 导出 Excel |

#### 5.4.4 回款
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/contracts/{id}/payment-plans` | 制定/重排回款计划 |
| GET | `/api/v1/contracts/{id}/payment-plans` | 计划列表 |
| PUT | `/api/v1/payment-plans/{id}` | 编辑某期 |
| POST | `/api/v1/payment-records` | 登记实际回款（自动核销） |
| POST | `/api/v1/payment-records/import` | 批量导入 Excel |
| POST | `/api/v1/payment-records/{id}/red-reverse` | 红冲 |
| POST | `/api/v1/payment-records/{id}/manual-settle` | 手工核销调整 |
| GET | `/api/v1/payment-records` | 回款记录列表 |
| GET | `/api/v1/payments/aging` | 账龄分析 |
| GET | `/api/v1/payments/overdue` | 逾期清单 |
| GET | `/api/v1/payments/export` | 导出 |

#### 5.4.5 看板与报表
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/dashboard/kpis` | 关键指标 |
| GET | `/api/v1/dashboard/trend` | 12 个月趋势 |
| GET | `/api/v1/dashboard/aging` | 账龄分布 |
| GET | `/api/v1/dashboard/top-receivables` | 应收 TOP 客户 |
| GET | `/api/v1/dashboard/todos` | 我的待办 |
| GET | `/api/v1/reports/payment-detail` | 回款明细 |
| GET | `/api/v1/reports/payment-summary` | 回款汇总 |

#### 5.4.6 通知与系统
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/notifications` | 通知列表 |
| POST | `/api/v1/notifications/{id}/read` | 标记已读 |
| POST | `/api/v1/notifications/read-all` | 全部已读 |
| GET | `/api/v1/system/params` | 参数列表 |
| PUT | `/api/v1/system/params/{key}` | 更新参数 |
| GET | `/api/v1/system/operation-logs` | 操作日志 |
| GET | `/api/v1/system/recycle-bin` | 回收站 |
| POST | `/api/v1/system/recycle-bin/{bizType}/{id}/restore` | 还原 |

### 5.5 接口示例

**新建客户（请求）**：
```http
POST /api/v1/customers
Content-Type: application/json

{
  "name": "深圳前沿科技有限公司",
  "shortName": "前沿科技",
  "type": "ENTERPRISE",
  "uscc": "91440300MA5XXXXXXX",
  "regionCode": "440300",
  "address": "深圳市南山区科技园...",
  "industry": "IT",
  "level": "B",
  "contacts": [
    {
      "name": "张三",
      "title": "采购经理",
      "phone": "13800000000",
      "email": "zs@company.com",
      "isPrimary": true
    }
  ],
  "remark": "—"
}
```

**新建客户（响应）**：
```json
{
  "code": "0",
  "message": "OK",
  "data": {
    "id": "1812345678901234567",
    "code": "CU-202604-0001"
  }
}
```

**登记实际回款（请求）**：
```http
POST /api/v1/payment-records
{
  "contractId": "...",
  "arrivalDate": "2026-04-28",
  "amount": "120000.00",
  "payer": "深圳前沿科技有限公司",
  "voucherNo": "BANK-20260428-001",
  "voucherFileId": "...",
  "remark": "首期款"
}
```

---

## 6. 前端设计规范

### 6.1 工程结构
```
crms-web/
├─ public/
├─ src/
│  ├─ api/             # axios 封装 + 各模块接口
│  │   ├─ http.ts
│  │   ├─ customer.ts
│  │   ├─ contract.ts
│  │   └─ payment.ts
│  ├─ assets/
│  ├─ components/      # 业务通用组件
│  │   ├─ AppTable.vue
│  │   ├─ AppForm.vue
│  │   └─ AppUpload.vue
│  ├─ layouts/         # 布局
│  ├─ router/          # 路由 + 权限路由表
│  ├─ stores/          # Pinia
│  ├─ views/
│  │   ├─ dashboard/
│  │   ├─ customer/
│  │   ├─ contract/
│  │   ├─ payment/
│  │   ├─ report/
│  │   ├─ notification/
│  │   └─ system/
│  ├─ utils/
│  ├─ App.vue
│  └─ main.ts
├─ vite.config.ts
└─ package.json
```

### 6.2 路由与权限
- 路由表分为"基础路由"（登录、404）和"动态路由"（按权限点过滤）；
- 权限点保存在 Pinia 的 `useAuthStore`，登录后由 `/auth/me` 拉取；
- 自定义指令 `v-perm="'contract:edit'"` 控制按钮可见性。

### 6.3 状态管理
- `useAuthStore`：用户信息、权限点、token；
- `useDictStore`：数据字典（合同类型、客户类型等）应用启动时一次性加载；
- 业务页面页内状态使用本地 `ref/reactive`，避免滥用全局 store。

### 6.4 UI / 交互规范
- 主色：`#165DFF`（信任蓝）；
- 表格：默认 10 行/页，最多 100；
- 金额：千分位 + 两位小数 + 右对齐；
- 必填字段：红色星号；
- 危险操作（终止/删除/硬删除）：二次确认 + 文案中明确"不可恢复"；
- 列表筛选：右上角"高级筛选"抽屉，支持保存常用筛选条件（前端 localStorage）。

### 6.5 API 请求约定
- 统一通过 `src/api/http.ts` 的 axios 实例：
  - 请求拦截：注入 token、traceId；
  - 响应拦截：解包 `data`、统一错误提示（`code !== '0'` 弹 message）、`401` 跳登录；
- 所有接口必须有 TypeScript 类型，集中放在 `src/api/types.ts`。

---

## 7. 安全设计

### 7.1 认证
- 密码使用 `bcrypt(cost=12)` 加盐存储；
- 登录失败 5 次锁定 15 分钟；
- 会话使用 Sa-Token，token 通过 `Authorization` 头或 `httpOnly` Cookie 传递；
- 会话空闲 30 分钟过期，最长 12 小时强制登出。

### 7.2 权限
- 菜单权限：前端路由 + 后端 Controller 双重校验；
- 操作权限：注解 `@SaCheckPermission`；
- 数据权限：MyBatis-Plus 拦截器自动追加（按 `owner_id` / `dept_id`）；
- 越权防护（IDOR）：每个详情/编辑接口必须校验"目标对象的数据范围归属当前用户"。

### 7.3 加密
- **传输**：全站 HTTPS（TLS 1.2+），禁用弱密码套件；
- **存储**：
  - 密码：bcrypt（不可逆）；
  - 客户联系人电话/邮箱、合同金额、付款凭证号：AES-256-GCM；
  - 密钥管理：放置在配置中心或环境变量，定期轮换；
- **敏感字段脱敏展示**：
  - 电话：`138****0000`
  - 邮箱：`zs****@company.com`

### 7.4 审计
- 所有写操作记录 `operation_log`；
- 硬删除独立 `hard_delete_log` + 业务数据快照；
- 关键字段（金额、客户、状态）变更记录 `change_log`；
- 日志只追加，不允许业务侧修改/删除（DBA 仅可归档）。

### 7.5 常见漏洞防护
| 风险 | 防护措施 |
| --- | --- |
| SQL 注入 | MyBatis 参数化绑定，禁止拼接 SQL |
| XSS | 前端使用 Vue 自动转义 + 后端对富文本 sanitize（DOMPurify 等） |
| CSRF | Cookie 模式启用 SameSite=Lax + CSRF Token；Bearer 模式天然不受影响 |
| 文件上传 | 白名单扩展名 + MIME 校验 + 大小限制 + 文件名重命名 |
| 越权（IDOR） | 服务端二次校验数据范围 |
| 重放 | 关键接口（硬删除、密码变更）使用一次性短期 token |
| 暴力登录 | 失败次数锁定 + 后续可加图形验证码 |
| 敏感信息泄漏 | 日志脱敏、错误信息不暴露堆栈给用户 |

---

## 8. 非功能性设计

### 8.1 性能
- 列表查询：合理索引，避免大字段（备注/JSON）参与排序；
- 看板：使用 Redis 缓存 KPI，TTL 5 分钟；
- 报表导出：≥ 1 万行使用流式（SXSSF）写 Excel，避免 OOM；
- 文件下载：从 MinIO 直传，使用预签名 URL，避免应用层中转。

### 8.2 缓存策略
| 内容 | 存储 | TTL |
| --- | --- | --- |
| 用户会话 | Redis | 30 分钟（滑动） |
| 数据字典 | Redis | 1 小时 + 主动失效 |
| 系统参数 | Redis | 永久（变更时刷新） |
| 看板 KPI | Redis | 5 分钟 |
| 合同编号计数器 | Redis | 永久 |

### 8.3 定时任务清单
| 名称 | Cron | 说明 |
| --- | --- | --- |
| 合同到期扫描 | `0 0 8 * * ?` | 每日 8 点扫描，推送站内信 |
| 回款计划提醒扫描 | `0 0 8 * * ?` | 每日 8 点 |
| 逾期标记 | `0 0 2 * * ?` | 每日凌晨 2 点 |
| 看板缓存预热 | `0 */5 * * * ?` | 每 5 分钟 |
| 通知归档 | `0 30 2 * * ?` | 每日凌晨 2:30，>90 天置归档 |
| 回收站清理 | `0 0 3 * * ?` | 每日凌晨 3 点 |
| 数据库备份 | `0 0 1 * * ?` | 每日凌晨 1 点 |

所有任务通过 ShedLock 占锁，避免多实例重复执行。

### 8.4 日志规范
- 框架：SLF4J + Logback；
- 输出：JSON 格式（字段：时间、级别、traceId、用户、模块、消息）；
- 文件：按天滚动，单文件 ≤ 500MB，保留 30 天；
- 等级：
  - `ERROR`：影响业务的异常；
  - `WARN`：可恢复异常 / 安全事件（登录失败等）；
  - `INFO`：关键操作 / 启停；
  - `DEBUG`：仅开发环境开启。

### 8.5 异常处理
- 全局 `@RestControllerAdvice`：业务异常 `BizException`、参数异常 `MethodArgumentNotValidException`、系统异常 `Exception`；
- 业务异常：返回 `code` + `message`，HTTP 200；
- 系统异常：屏蔽堆栈，返回 `SYS-500`，traceId 用于排查。

---

## 9. 编码与协作规范

### 9.1 命名
- Java：包名小写、类名 UpperCamelCase、方法/变量 lowerCamelCase；
- TypeScript：组件文件 PascalCase（`CustomerForm.vue`），变量 camelCase；
- DB：表与字段 snake_case；
- 接口路径：小写 + 短横线（`payment-records`）。

### 9.2 Git 分支策略（GitFlow 简化版）
- `main`：生产；
- `develop`：开发主干；
- `feature/<JIRA-ID>-<desc>`：功能分支；
- `release/<version>`：发版分支；
- `hotfix/<id>`：紧急修复。

### 9.3 提交信息（Conventional Commits）
```
<type>(<scope>): <subject>

<body>
```
type：`feat`、`fix`、`refactor`、`docs`、`test`、`chore`、`perf`。

### 9.4 代码审查（Code Review）
- 所有合并必须通过至少 1 名 Reviewer；
- 检查项：
  1. 是否覆盖单元测试；
  2. 是否有 SQL 注入/越权风险；
  3. 是否包含调试代码、`console.log`、`System.out.println`；
  4. 是否符合命名规范与目录约定；
  5. 是否更新 OpenAPI 文档。

### 9.5 静态扫描
- 后端：SpotBugs + Checkstyle（必选）、Sonar（如公司已部署可接入）；
- 前端：ESLint + Prettier + TypeScript strict；
- 提交前：`pre-commit` 钩子运行 lint；
- Jenkins 流水线在"构建"阶段会再次执行 lint，与本地保持一致。

---

## 10. 部署与运维设计

### 10.1 环境划分
| 环境 | 用途 | 数据 |
| --- | --- | --- |
| 本地（local） | 开发自测 | mock |
| 测试（test） | 联调与集成测试 | 测试数据，可重置 |
| 预发（staging） | 上线前验证 | 接近生产 |
| 生产（prod） | 正式运行 | 真实 |

### 10.2 配置管理
- `application-{env}.yml`，敏感配置使用环境变量注入（`SPRING_DATASOURCE_PASSWORD` 等）；
- Docker 镜像不含敏感配置，部署时挂载 `.env`。

### 10.3 容器化
**Dockerfile（后端示例）**：
```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/crms-app.jar app.jar
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
```

**docker-compose.yml（生产骨架）**：
```yaml
version: "3.9"
services:
  app:
    image: crms-app:1.0.0
    deploy:
      replicas: 2
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/crms
      - SPRING_REDIS_HOST=redis
    ports: ["8080:8080"]
    depends_on: [mysql, redis, minio]
  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT}
    volumes: ["./data/mysql:/var/lib/mysql"]
  redis:
    image: redis:7.2
    volumes: ["./data/redis:/data"]
  minio:
    image: minio/minio
    command: server /data
    volumes: ["./data/minio:/data"]
  nginx:
    image: nginx:1.25
    ports: ["443:443","80:80"]
    volumes: ["./nginx:/etc/nginx/conf.d","./web/dist:/usr/share/nginx/html"]
```

### 10.4 CI/CD（Jenkins）

> 本期统一使用 **Jenkins** 作为 CI/CD 平台。

#### 10.4.1 流水线总览

```
       ┌──────────┐
       │  Push    │
       └────┬─────┘
            │ Webhook
            ▼
   ┌──────────────────────────────────────────────────────────────┐
   │  Jenkins Pipeline（Jenkinsfile，多分支流水线）                │
   │                                                               │
   │  Stage 1: Checkout                                            │
   │  Stage 2: Lint & Static Scan（Checkstyle / ESLint）           │
   │  Stage 3: Test（mvn test / vitest，产出覆盖率报告）           │
   │  Stage 4: Build（mvn package / pnpm build）                   │
   │  Stage 5: Docker Build & Push（带 git commit 短 hash + tag）  │
   │  Stage 6: Deploy（按分支策略自动 / 手动）                     │
   │  Stage 7: 部署后烟测（curl /actuator/health）                 │
   └──────────────────────────────────────────────────────────────┘
```

#### 10.4.2 分支与触发策略
| 分支 | 触发动作 | 部署目标 | 是否需审批 |
| --- | --- | --- | --- |
| `feature/*` | 推送时执行 lint + test | 不部署 | — |
| `develop` | 合并后自动构建并部署 | 测试环境 | 否 |
| `release/*` | 构建并部署 | 预发环境 | 否 |
| `main` | 打 tag 后构建 | 生产环境 | **是**（人工 Approval 步骤） |
| `hotfix/*` | 紧急流水线 | 预发→生产 | 是 |

#### 10.4.3 Jenkinsfile 骨架

```groovy
pipeline {
  agent any
  options { timestamps(); ansiColor('xterm'); buildDiscarder(logRotator(numToKeepStr: '30')) }

  environment {
    REGISTRY = "registry.internal/crms"
    APP_NAME = "crms-app"
  }

  stages {
    stage('Checkout') { steps { checkout scm } }

    stage('Lint') {
      parallel {
        stage('Backend') { steps { sh 'mvn -B checkstyle:check' } }
        stage('Frontend') { steps { dir('crms-web') { sh 'pnpm lint' } } }
      }
    }

    stage('Test') {
      parallel {
        stage('Backend') { steps { sh 'mvn -B test' }
          post { always { junit 'target/surefire-reports/*.xml' } } }
        stage('Frontend') { steps { dir('crms-web') { sh 'pnpm test --run' } } }
      }
    }

    stage('Build') {
      parallel {
        stage('Backend') { steps { sh 'mvn -B -DskipTests package' } }
        stage('Frontend') { steps { dir('crms-web') { sh 'pnpm build' } } }
      }
    }

    stage('Docker') {
      steps {
        script {
          def tag = "${env.BRANCH_NAME}-${env.GIT_COMMIT.take(8)}"
          sh "docker build -t ${REGISTRY}/${APP_NAME}:${tag} ."
          sh "docker push ${REGISTRY}/${APP_NAME}:${tag}"
          env.IMAGE_TAG = tag
        }
      }
    }

    stage('Deploy-Test') {
      when { branch 'develop' }
      steps { sh "ssh deploy@test 'cd /opt/crms && IMAGE_TAG=${IMAGE_TAG} docker compose up -d'" }
    }

    stage('Deploy-Prod') {
      when { buildingTag() }
      steps {
        input message: '确认部署到生产环境？', ok: '部署'
        sh "ssh deploy@prod 'cd /opt/crms && IMAGE_TAG=${IMAGE_TAG} docker compose up -d'"
      }
    }

    stage('Smoke Test') {
      steps { sh 'curl -fsS http://$DEPLOY_HOST/actuator/health' }
    }
  }

  post {
    failure { mail to: 'crms-dev@company.com', subject: "❌ ${env.JOB_NAME} #${env.BUILD_NUMBER} 失败", body: "${env.BUILD_URL}" }
  }
}
```

#### 10.4.4 Jenkins 凭据与 Agent
- 凭据：Git 部署密钥、Docker Registry 账号、目标服务器 SSH Key、邮件 SMTP——全部托管在 Jenkins Credentials；
- Agent：使用容器化 Agent（`maven:3.9-eclipse-temurin-17` + `node:20`），避免污染 Master；
- 制品保留：30 个最近构建，旧的自动清理。

### 10.5 备份与恢复
- **MySQL**：每日 1 点 `mysqldump --single-transaction` 全量备份，保留 30 天；每月归档 1 份至冷存储；
- **MinIO**：每周日 02:00 通过 `mc mirror` 同步到备份盘，保留 4 周；
- **演练**：每季度执行一次"备份恢复演练"，记录 RTO / RPO（目标 RTO ≤ 2 小时、RPO ≤ 24 小时）。

### 10.6 健康检查与运维监控（轻量方案）

> 本期 **不引入 Prometheus + Grafana** 等独立监控栈。运维由开发团队承担，采用轻量方案降低维护负担。

| 维度 | 方案 |
| --- | --- |
| 应用健康 | Spring Boot Actuator `/actuator/health`，由 Nginx 主动探测，宕机自动从负载均衡摘除 |
| 启动状态 | Jenkins 部署后烟测；失败邮件告警到 dev 群组 |
| 业务异常 | 后端集中捕获 → 写 ERROR 日志 → 通过 logback `SmtpAppender` 在 5 分钟窗口内合并发送告警邮件 |
| 慢 SQL | MySQL 开启 `slow_query_log`，阈值 1s；DBA 周巡检 |
| 资源水位 | 服务器层 `top` / `df` / `free` 巡检脚本（cron 每小时）+ 阈值邮件告警 |
| 日志查询 | 应用日志按天滚动到本地，保留 30 天；登录服务器 `grep` / `less` 查询；不部署 ELK |
| 备份监控 | 备份脚本失败自动发邮件 + 在系统管理"数据备份监控"页面展示 |

#### 10.6.1 邮件告警（脚本示例）
- `scripts/check_disk.sh`：磁盘 > 85% 时调用 `mail` 命令发送告警；
- `scripts/check_app.sh`：探测 `/actuator/health`，连续 3 次失败后告警；
- 这些脚本通过系统 cron 调度，归档到 `deploy/scripts/` 目录。

#### 10.6.2 升级演进
后续若运维压力增大，再行评估接入：Prometheus + Grafana（指标）、Loki（日志）、AlertManager（告警），均可在不改业务代码的前提下接入 Actuator 端点。

### 10.7 数据脱敏（测试/预发环境）

> 测试与预发环境用于联调与验证，不允许直接使用生产真实数据。我们提供脱敏脚本生成可分发数据集。

#### 10.7.1 适用场景
- 复现生产疑难 BUG，需要近似真实的数据形态；
- 新成员入职、第三方联调、压测样本。

#### 10.7.2 脱敏规则

| 表 | 字段 | 脱敏规则 |
| --- | --- | --- |
| `iam_user` | `password_hash` | 统一替换为测试密码 `Test@123456` 的 bcrypt 值 |
| `iam_user` | `phone`、`email` | 电话保留前 3 后 4，中间 `****`；邮箱用户名前两位 + `****` |
| `iam_user` | `real_name` | 保留姓 + `*`，例如 "张三" → "张*" |
| `customer` | `name`、`short_name` | 客户名前 2 字 + 随机数，例 "深圳前沿科技…" → "深圳客户0001" |
| `customer` | `uscc` | 替换为合法格式的随机串（保持长度 18） |
| `customer` | `address` | 仅保留省市，详细地址用 `***` |
| `customer_contact` | `name`、`phone`、`email`、`wechat` | 同 `iam_user` 规则 + 随机重排 |
| `contract` | `name` | 替换为 "合同{自增编号}" |
| `contract` | `amount` | **金额按比例随机扰动 ±20%**，但保留 2 位小数 |
| `contract` | `remark` | 清空 |
| `payment_record` | `voucher_no`、`payer` | 凭证号随机化、付款方与客户保持一致映射 |
| `payment_record` | `remark` | 清空 |
| `operation_log`、`hard_delete_log` | 全表 | **不导出**（含敏感快照） |
| `notification` | `content` | 清空（重新生成） |
| 全部表 | `is_deleted=1` 的记录 | 默认不导出 |

#### 10.7.3 实现方式

- 脚本位置：`db/desensitize/desensitize.sql`（基于视图）+ `db/desensitize/run.sh`（编排执行）；
- 流程：
  1. 在生产从库（只读）上执行 `mysqldump --single-transaction` 导出原始库 → `dump_raw.sql`；
  2. 在隔离机器上恢复到 `crms_raw` 临时库；
  3. 执行 `desensitize.sql` 在该库内做就地脱敏 UPDATE；
  4. 再次 `mysqldump` 导出脱敏后的 `dump_masked.sql`，作为分发产物；
  5. 临时库销毁；分发物在分发完成后 7 天自动删除；
- **强制约束**：脱敏过程不可在测试/预发环境之外的网络流转，原始 dump 立即销毁，仅 `dump_masked.sql` 可上传到测试机；
- **审批**：每次执行需开发负责人书面同意，并记录到 `操作日志`（外部记录）。

#### 10.7.4 周期
- 默认 **每月 1 次** 在测试环境刷新；预发只在特殊故障复现时按需执行；
- 紧急情况下，可应需求方临时申请，单次有效。

### 10.8 上线运行手册（开发团队负责）
- **值班机制**：开发人员轮值，覆盖工作日 9:00–21:00；
- **应急流程**：故障级别（P0/P1/P2）→ 处置时限 → 回滚预案；
- **变更窗口**：默认每周二、周四 22:00–23:00；
- **运维知识库**：维护"常见故障 - 处置步骤"Wiki。

---

## 11. 测试策略

### 11.1 测试金字塔
```
       ┌──────┐
       │ E2E  │   关键链路（登录→新建合同→登记回款）
       └──────┘
      ┌────────┐
      │  集成  │   API 层 + DB（Testcontainers MySQL）
      └────────┘
   ┌──────────────┐
   │   单元测试   │   领域服务、工具类，目标覆盖率 ≥ 70%
   └──────────────┘
```

### 11.2 测试工具
| 类型 | 后端 | 前端 |
| --- | --- | --- |
| 单元测试 | JUnit 5 + Mockito | Vitest |
| 集成测试 | Spring Boot Test + Testcontainers | — |
| API 测试 | RestAssured / Postman 集合 | Playwright（可选） |
| E2E | Playwright | Playwright |
| 性能测试 | JMeter / k6 | — |

### 11.3 关键测试场景
- 登录失败 5 次锁定；
- 数据范围过滤：销售只看到本人客户与合同；
- 客户合并：合同与联系人正确迁移、原客户置 MERGED；
- 自动核销：单笔实际回款覆盖多期、跨期；
- 红冲：金额、状态、关联日志正确；
- 逾期标记：跨日任务在 02:00 执行后状态正确；
- 硬删除：必须二次密码 + 仅超管可执行 + 写日志。

---

## 12. 项目里程碑（与 SRS §8 对齐）

| 阶段 | 周期 | 关键交付物 |
| --- | --- | --- |
| 设计 | 2 周 | 本 DSS 终稿、ER 图、OpenAPI、UI 视觉稿 |
| 迭代 1 | 3 周 | IAM、客户档案、系统管理（含回收站）、CI/CD 通路 |
| 迭代 2 | 3 周 | 合同管理、附件、变更记录 |
| 迭代 3 | 3 周 | 回款管理、通知中心 |
| 迭代 4 | 2 周 | 综合看板、报表中心 |
| 系统测试 + UAT | 2 周 | 测试报告、验收 |
| 上线与试运行 | 2 周 | 上线、培训、答疑 |

---

## 13. 附录

### 13.1 工程仓库结构（推荐）
```
crms/
├─ crms-app/                # 后端单体应用
│  ├─ src/main/java/com/company/crms/
│  │   ├─ iam/
│  │   ├─ customer/
│  │   ├─ contract/
│  │   ├─ payment/
│  │   ├─ report/
│  │   ├─ notification/
│  │   ├─ system/
│  │   └─ common/
│  ├─ src/main/resources/
│  ├─ src/test/...
│  └─ pom.xml
├─ crms-web/                # 前端
├─ db/
│  ├─ schema/V1.0.0__init.sql
│  └─ data/V1.0.1__seed.sql
├─ deploy/
│  ├─ docker-compose.prod.yml
│  ├─ nginx/
│  └─ scripts/
└─ docs/
   ├─ srs.md
   ├─ dss.md
   └─ openapi.yml
```

### 13.2 与 SRS 对应关系
| SRS 章节 | DSS 对应章节 |
| --- | --- |
| §2.3 角色 | §3.1 IAM |
| §4.2 客户 | §3.2、§4.3 客户表、§5.4.2 接口 |
| §4.3 合同 | §3.3、§4.3 合同表、§5.4.3 接口 |
| §4.4 回款 | §3.4、§4.3 回款表、§5.4.4 接口 |
| §4.5 报表 | §5.4.5 接口、§8.1 性能 |
| §4.6 通知 | §3.5、§5.4.6 接口、§8.3 任务 |
| §4.7 系统管理 | §3.6 回收站/审计 |
| §6 非功能 | §7、§8 |
| §7 部署 | §10 |

### 13.3 已确认决策（V1.1 落版）

| 编号 | 议题 | 结论 | 落版位置 |
| --- | --- | --- | --- |
| D1 | 后端技术栈 | **Java 17 + Spring Boot 3 一致采用** | §2.5 技术栈 |
| D2 | 是否启用 Prometheus + Grafana | **不启用**，采用轻量监控（Actuator + 邮件告警 + cron 巡检） | §2.5、§10.6 |
| D3 | 文件存储 | **使用 MinIO**（不接入公司其他文件服务） | §2.5、§3.3.3 |
| D4 | CI/CD 平台 | **使用 Jenkins**（含 Jenkinsfile 骨架） | §10.4 |
| D5 | 数据脱敏 | **建立测试/预发环境脱敏脚本**，每月刷新 | §10.7 |

### 13.4 当前未决问题
*（暂无；后续评审/实施过程中如有新问题，在此追加。）*

### 13.5 修订记录

| 版本 | 日期 | 修订人 | 说明 |
| --- | --- | --- | --- |
| V1.0 | 2026-04-30 | — | 草案初稿，基于 SRS V1.1 编写 |
| V1.1 | 2026-04-30 | — | 1）确认 Java 17 + Spring Boot 3 技术栈；2）取消 Prometheus + Grafana，改为轻量监控；3）明确 MinIO 作为文件存储；4）CI/CD 锁定 Jenkins，补充 Jenkinsfile 骨架；5）新增 §10.7 数据脱敏方案 |

---

> **文档结束。** 本文档为开发团队的技术基线，正式发布前请由技术负责人、架构师、研发、测试、安全等共同评审签字。
