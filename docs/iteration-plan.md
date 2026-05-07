# CRMS 剩余工作迭代实施计划

> 关联文档：《SRS V1.1》《DSS V1.1》《tasks.md》《prompts/system-backend.md》《prompts/system-frontend.md》
> 文档版本：V1.0
> 编写日期：2026-05-05
> 编写人：Cursor Agent
>
> 本文档基于 `docs/tasks.md`（152 任务）与当前代码盘点结果，把**剩余约 60% 任务**重新拆解为 **10 个 Sprint** 的端到端可交付切片，便于 AI 助手按 Sprint 自动迭代落地。

---

## 0. 现状盘点

### 0.1 已完成（约 40%）

| 维度 | 状态 |
| --- | --- |
| 基础设施 | Spring Boot 3.2 + MyBatis-Plus + Flyway + Sa-Token + ShedLock + MinIO + EasyExcel + Redis + Testcontainers 全部就绪 |
| 数据库 | DDL 完整（V1.0.0）+ 权限/部门/参数 seed（V1.0.1~1.0.3）+ admin 密码修复（V1.0.4） |
| 横切能力 | `Result`/`BizException`、Snowflake、AES、`@SensitiveField`、`OperationLogAspect`+异步写入、`DataScopeInterceptor`、`MybatisPlusConfig`、`ShedLockConfig`、`MinioProperties` |
| IAM | Sa-Token 登录/登出、失败计数+锁定、修改密码、`/auth/me`、强制改密拦截 |
| 客户 | 实体+CRUD+编号+数据范围（基础） |
| 合同 | 实体+服务（草稿/履行/关闭/终止状态机）、自动到期任务（08:00、09:00） |
| 回款 | 实体+核销算法（FIFO）+账龄分桶服务 |
| 报表 | dashboard、trend、aging（基础） |
| 前端 | Vite + Pinia + Element Plus 骨架，登录页、修改密码页、客户列表/详情/编辑抽屉、Dashboard（KPI+趋势+账龄饼）、`v-perm` 指令 |
| DevOps | Jenkinsfile（Lint/Test/Build/Push/Deploy）、prod/test compose、Nginx、巡检/备份脚本、脱敏脚本 |

### 0.2 待实现（约 60%）

按"用户可见价值"+"垂直切片"维度归类：

| 类别 | 重点缺口 |
| --- | --- |
| **系统管理 UI 后台** | User/Role/Dept REST + 全部 FE 页面；`SystemHome.vue` 缺 `<router-view>`；ParamManage、OperationLogView、RecycleBin 全是占位 |
| **客户模块完善** | 联系人 CRUD、客户合并真正实现、查重接口、硬删日志写入、二次密码校验、详情聚合接口、列表高级筛选与导出 |
| **合同模块** | `ContractController` 缺失、`ContractAttachmentService` 仅接口、ChangeLog 切面缺、备注 API、CSV 导出、级联删除；FE 全部 8 个任务为占位 |
| **回款模块** | 计划 CRUD REST、记录 REST、红冲实现、Excel 导入、手工调整、账龄钻取、逾期清单、02:00 逾期定时；FE 全部 7 个任务为占位 |
| **通知中心** | 实体/Mapper/Service 全部缺、事件总线、6 个场景分发、去重、归档定时；FE 顶栏铃铛/中心/偏好全部占位 |
| **看板与报表** | TOP-10、我的待办、Redis 缓存（当前仅本地内存）、明细/汇总报表、SXSSF 大数据量导出、缓存预热；FE 报表中心整页占位 |
| **测试** | 自动核销算法、红冲、批量导入、通知去重、硬删 E2E、数据范围矩阵未覆盖 |
| **跨阶段** | OpenAPI 自动同步与 FE 客户端再生、加密落地 E2E、Redis 替换本地缓存 |

---

## 1. 拆分原则

1. **按垂直切片组织**：每个 Sprint 同时交付 BE+FE+测试，避免单端孤岛；
2. **以用户可感知价值排序**：先交付用户能在浏览器里点出来的东西；
3. **共享组件优先沉淀**：二次密码弹窗、文件上传、富搜索抽屉等先抽出再被各模块复用；
4. **每个 Sprint 自带 DOD**：列表中明确"做什么/做完后能演示什么/单测最少覆盖哪几个分支"；
5. **任务卡 ↔ tasks.md 反向引用**：每个 Sprint 列出对应的 `I*-*` ID，便于回填进度。

---

## 2. Sprint 总览

| Sprint | 名称 | 关联 task ID | 主要交付 | 优先级 | 估算（人天） |
| --- | --- | --- | --- | --- | --- |
| S1 | 系统管理三件套 | I1-IAM-001~003 + I1-FE-003~005 | 用户/角色/部门 BE+FE，修复 `SystemHome` 路由 bug | P0 | 6 |
| S2 | 系统管理收尾 | I1-SY-001~004 + I1-FE-011~013 + I1-IAM-008 | 系统参数 / 操作日志 / 回收站，硬删日志写入，二次密码 API | P0 | 5 |
| S3 | 客户模块完善 | I1-CU-003~008 + I1-FE-007~010 | 联系人、合并向导、查重、硬删、详情聚合、二次密码组件 | P0 | 6 |
| S4 | 合同后端 | I2-CT-002~007 + I2-AT-001~003 + I1-CM-004 | `ContractController`、ChangeLog 切面、附件服务、备注、CSV 导出、级联删除 | P0 | 6 |
| S5 | 合同前端 | I2-FE-001~008 | 列表 / 表单 / 详情 Tab / 附件上传 / 状态操作 / 时间线 / 备注 | P0 | 7 |
| S6 | 回款后端 | I3-PP-* + I3-PR-* + I3-AG-001~004 + I2-JB-003 | 计划 CRUD / 记录 REST / 红冲 / Excel 导入 / 02:00 逾期 / 账龄钻取 / 逾期清单 / 周提醒 | P0 | 7 |
| S7 | 回款前端 | I3-FE-001~007 | 计划 Tab、登记表单、列表、批量导入向导、手工核销、账龄、逾期 | P0 | 6 |
| S8 | 通知中心 | I3-NT-001~005 + I3-FE-008~010 | 实体 + 事件总线 + 6 场景分发 + 去重 + 归档；铃铛 / 中心 / 偏好 | P1 | 5 |
| S9 | 看板与报表 | I4-DS-001~005 + I4-RP-001~004 + I4-FE-001~007 | TOP / Todos / Redis 缓存 / 明细汇总 / SXSSF / 预热；报表中心 | P1 | 5 |
| S10 | 测试与 OpenAPI 同步 | I*-QA-* + X-DOC-001 | 核销 / 红冲 / 导入 / 通知去重测试，跑 `scripts/export-openapi.sh` + `pnpm gen:api` | P1 | 4 |

> 总计：57 人天 ≈ 4 人 × 3 周。与 tasks.md 残留量基本吻合。

---

## 3. Sprint 详细切片

### S1 系统管理三件套

**问题驱动**：`SystemHome.vue` 没有 `<router-view />`，导致 `/system/users` 等子路由进不去；同时所有系统管理 FE 全是占位文案，对应 BE REST 也缺。

**BE 任务**

1. **I1-IAM-001-rest**：`UserController`（`/api/v1/users`）+ `UserService`：分页/详情/新建/编辑/重置密码/启用停用/分配角色/解锁。校验：用户名唯一、密码强度、不可删除超管。
2. **I1-IAM-002-rest**：`RoleController`（`/api/v1/roles`）+ `RoleService`：CRUD + 设置 `permissions`/`dataScope`；列出权限点接口（`GET /api/v1/permissions`）。
3. **I1-IAM-003-rest**：`DepartmentController`（`/api/v1/departments`）+ `DeptService`：树查询、新增、编辑、删除（校验子节点+用户引用）、移动校验循环。

**FE 任务**

4. **修复 `SystemHome.vue`**：加 `<router-view>` + 二级菜单卡片（导航到 users/roles/depts/params/logs/recycle）。
5. **`UserManage.vue`**：列表（关键字、部门、状态过滤）+ 新建/编辑抽屉 + 操作列（重置密码/分配角色/启用停用/解锁）。
6. **`RoleManage.vue`**：列表 + 新建/编辑（带权限点 Tree + dataScope 单选）。
7. **`DeptManage.vue`**：左树 + 右详情，右上角"新增子部门/编辑/删除"。
8. **`src/api/iam.ts`**：用户、角色、部门、权限点 API 类型化封装。

**DOD**

- 用 admin 登录后能在 `/system/users` 看到自己；可创建一个 `tester` 用户、分配 `R03 业务员` 角色、用 `tester/Test@123456` 登录看到的菜单符合权限点；
- 角色管理页能勾选权限点（按模块分组的 `Tree`）并保存；
- 部门管理页能新增/移动/删除部门（删除时若有用户应提示）。

**关联 ID**：I1-IAM-001/002/003，I1-FE-003/004/005。

---

### S2 系统管理收尾

**BE 任务**

1. **I1-SY-001**：系统参数 REST（list/page/update）+ 改用 Redis `RedisCacheManager`，更新后 `convertAndSend` 广播失效；
2. **I1-SY-002**：`OperationLogController`（`/api/v1/operation-logs`）：多条件分页（关键字、操作人、模块、动作、时间区间）；
3. **I1-SY-003**：`RecycleBinController`（`/api/v1/recycle-bin`）：按 `bizType` 聚合查询软删数据 + 还原接口；首期支持 `customer/contract/payment_record`；
4. **I1-IAM-008**：`POST /api/v1/auth/verify-password`：返回短期 `challengeToken`（Redis TTL 5min），用于硬删等敏感操作；硬删除接口必须带此 token，否则 `AUTH_PWD_VERIFY_REQUIRED`；
5. **I1-SY-004**：`HardDeleteLogService.recordDeletion(bizType, bizId, snapshotJson)`：客户/合同硬删时调用，含 `operator_id`、`reason`、`snapshot`；提供查询 API。

**FE 任务**

6. **`ParamManage.vue`**：键值表格 + 编辑弹窗（区分类型：string/number/json/boolean）。
7. **`OperationLogView.vue`**：筛选栏 + 时间范围 + 操作详情抽屉。
8. **`RecycleBin.vue`**：左 Tab（客户/合同/回款）+ 右列表 + 还原按钮。
9. **`SecondaryPasswordDialog.vue`**：抽到 `src/components/`，调用 `verifyPassword`，输出 `challengeToken`，由调用方接力到硬删 API。

**DOD**

- 修改"逾期提醒提前天数"参数后，30 秒内 BE 取到新值（Redis 监听）；
- 操作日志页可按"用户=admin、动作=登录、最近 7 天"过滤；
- 回收站可还原一个软删客户；
- 客户硬删触发二次密码弹窗，正确密码后才能删除，删除写入 `hard_delete_log`。

**关联 ID**：I1-SY-001/002/003/004，I1-FE-011/012/013，I1-IAM-008。

---

### S3 客户模块完善

**BE 任务**

1. **I1-CU-003**：`Contact` 实体 + Mapper + REST（`/api/v1/customers/{id}/contacts`）+ 主联系人唯一约束（DB + 业务校验）；
2. **I1-CU-004**：`POST /api/v1/customers/check-duplicate`：信用代码强校验（精确）+ 名称模糊（trigram/like）；
3. **I1-CU-005**：客户合并真正实现：事务内迁移合同、联系人、回款；保留主客户主联系人；写 `change_log`；
4. **I1-CU-006**：硬删使用 S2 的 `verifyPassword` token + `HardDeleteLogService`；
5. **I1-CU-007**：列表筛选支持"是否有合同"、"是否逾期"（关联子查询）；
6. **I1-CU-008**：`GET /api/v1/customers/{id}/aggregate`：返回基本信息 + 合同列表（前 5）+ 回款 KPI（已收/应收/逾期）+ 联系人。

**FE 任务**

7. **`CustomerDetail.vue` Tabs 真实化**：联系人 CRUD（行内）、合同列表（跳转）、回款列表（跳转 + 概览数字）；
8. **`CustomerFormDrawer.vue`**：内联联系人编辑 + 失焦时调用 `check-duplicate` + 重复时弹卡片"使用现有客户/继续创建"；
9. **`CustomerMergeWizard.vue`**：3 步——选源 → 选目标 → 影响预览（合同/联系人计数）→ 确认；
10. **`CustomerList.vue`** 新增"高级筛选抽屉"（含子查询字段）+ 列设置 + CSV 导出。

**DOD**

- 客户详情联系人 Tab 可增删改主联系人，主联系人切换时旧主联系人自动取消；
- 信用代码相同时，新建表单失焦提示已存在客户并可跳转；
- 合并向导执行后源客户软删，所有合同/回款/联系人迁移到目标，`change_log` 可见 2 条记录；
- 列表能筛选"有逾期回款的客户"。

**关联 ID**：I1-CU-003~008，I1-FE-007/008/009/010。

---

### S4 合同后端

**BE 任务**

1. **I2-CT-002**：`ContractController`：分页/详情/新建/编辑/删除（软）；
2. **I2-CT-003 收尾**：补 `submit/terminate/close` 三个状态跃迁端点；非法跃迁返回 `BIZ_INVALID_STATE`；
3. **I1-CM-004 / I2-CT-004**：`ChangeLogAspect`（`@LogChange` 注解）+ `ChangeLogWriter`：在 `Contract.update` 时对比关键字段（金额、币种、期限、负责人、客户、合同类型）写 `change_log`；
4. **I2-CT-005**：`ContractRemarkController`：`POST /contracts/{id}/remarks` + `GET /contracts/{id}/remarks`（仅主管角色 `R02`）；
5. **I2-CT-006**：`GET /api/v1/contracts/export.csv`：流式响应（≤ 5 万行内存安全）；
6. **I2-AT-001/002/003**：`ContractAttachmentServiceImpl`：上传（白名单 .pdf/.docx/.xlsx/.png/.jpg/.zip + 单个 ≤ 50MB + 单合同 ≤ 20 个）/ 列表 / 下载（预签名 URL，TTL 10 分钟）/ 软删 / 硬删时同步清理 MinIO；
7. **I2-CT-007**：合同硬删 → 二次密码 + 级联（附件 MinIO + 变更 + 回款计划/记录） + 写硬删日志；
8. **I2-CT-001 收尾**：合同编号 Lua 脚本（`script/contract-code.lua`，原子 INCR + 月份 reset），替换现有 RedisTemplate.increment。

**DOD**

- `POST /contracts` 创建草稿，`PUT /contracts/{id}/submit` 进入履行中，金额变更写入 `change_log`；
- 主管角色才能写备注，业务员只读；
- 上传 PDF 后 `GET /contracts/{id}/attachments` 可见，下载 URL 在 10 分钟内有效；
- 合同硬删后 MinIO 文件被清理；
- 编号生成 500 QPS 无重复（集成测试）。

**关联 ID**：I2-CT-001~007，I2-AT-001~003，I1-CM-004。

---

### S5 合同前端

**FE 任务**

1. **`ContractList.vue`**：高级筛选抽屉（客户/状态/期限/金额段/负责人）+ 状态色块 + 导出按钮；
2. **`ContractFormDrawer.vue`**：含动态合同类型字典 + 期限校验（结束 ≥ 开始）+ 客户选择器（远程搜索）+ 主管自动联想；
3. **`ContractDetail.vue`**：5 个 Tab——基本信息（含 `inline edit`）/ 附件 / 回款（占位至 S7）/ 变更（时间线）/ 备注；
4. **`AttachmentUploader.vue`**：抽到 `src/components/`，多文件 + 进度 + 类型校验 + 删除；
5. **`ContractStatusActions.vue`**：根据当前状态显示可执行动作（提交/终止/关闭）+ `ElMessageBox.confirm` 二次确认；
6. **`ChangeTimeline.vue`**：变更日志渲染（字段名、旧值 / 新值、操作人、时间）；
7. **`SupervisorRemark.vue`**：备注列表 + 输入框（仅主管角色显示）；
8. **合同硬删/软删**：复用 S2 的 `SecondaryPasswordDialog`。

**DOD**

- 业务员能创建草稿合同；主管能审核进入履行；金额变更后变更 Tab 时间线可见 1 条；附件可上传/下载 PDF；备注业务员只读、主管可写。

**关联 ID**：I2-FE-001~008。

---

### S6 回款后端

**BE 任务**

1. **I3-PP-002**：`POST /api/v1/contracts/{id}/payment-plans`（批量创建/重排）+ 金额合计校验（合同金额 ± 0.01）；
2. **I3-PP-003**：`PUT/DELETE /api/v1/payment-plans/{id}`：仅未发生回款的期次可改/删；
3. **I3-PR-001 收尾 & I3-PR-005**：`PaymentRecordController`（list/detail/create + 自动核销 + 凭证）；
4. **I3-PR-003**：`POST /api/v1/payment-records/{id}/manual-settle`：手工核销调整（一对多 / 多对一）；
5. **I3-PR-004**：`RedReverseServiceImpl`：反向记录（金额取负） + 反向核销 + 状态机（NORMAL→REVERSED）；REST：`POST /api/v1/payment-records/{id}/red-reverse`；
6. **I3-PR-006**：`POST /api/v1/payment-records/import`：模板下载 + EasyExcel 流式读 + 行级校验 + 事务批量入库 + 错误清单 JSON；
7. **I3-AG-001**：每日 02:00 `OverdueMarkScheduler`：扫描 `payment_plan`，对超过到期日且未核销的标记 `overdue=1`；
8. **I3-AG-002**：每日 09:00 `PaymentDueReminderScheduler`：提前 X 天（参数）发"将到期"通知；
9. **I3-AG-003 收尾**：账龄钻取（按客户 / 部门）；
10. **I3-AG-004**：`GET /api/v1/payments/overdue`：逾期清单分页；
11. **I2-JB-003**：每周一 09:00 合同到期未续签持续提醒。

**DOD**

- 创建合同后能定 6 期等额计划，金额合计校验；
- 登记一笔实际回款触发自动核销，最早一期被核销；
- 反向（红冲）一笔后该笔状态变 `REVERSED`，对应核销也回退；
- 上传错误的 Excel 行（金额负数）返回错误清单，正确行入库；
- 02:00 任务运行后 `payment_plan.overdue=1` 数量正确。

**关联 ID**：I3-PP-002/003，I3-PR-001/003/004/005/006，I3-AG-001~004，I2-JB-003。

---

### S7 回款前端

**FE 任务**

1. **`PaymentPlanTab.vue`**（嵌入 `ContractDetail`）：表格编辑 + 一键平均分 + 重排校验提示；
2. **`PaymentRecordForm.vue`**：金额 + 收款日期 + 凭证上传 + 自动核销结果展示；
3. **`PaymentList.vue`**：筛选（客户/合同/期间/状态）+ 红冲入口 + 红冲历史 Tag；
4. **`PaymentImportWizard.vue`**：3 步（下载模板 → 上传 → 错误清单/成功汇总）；
5. **`ManualSettleDialog.vue`**：选择一笔记录 → 选择目标计划列表 → 分配金额；
6. **`AgingView.vue`**（落地）：饼图 + 表格 + 客户/部门钻取 Tab；
7. **`OverdueListView.vue`**：路由 `/payments/overdue`，分页表格。

**DOD**

- 在合同详情新增 6 期计划并保存；
- 在 `/payments` 登记一笔回款看到自动核销结果；
- 批量导入失败后能下载错误 CSV；
- 账龄页饼图可点击钻取部门维度。

**关联 ID**：I3-FE-001~007。

---

### S8 通知中心

**BE 任务**

1. **I3-NT-001**：`Notification` 实体 + `NotificationPreference` 实体 + Mappers + `NotificationController`：分页 / 批量已读 / 偏好读写；
2. **I3-NT-002**：`DomainEventBus` 抽象（`ApplicationEventPublisher` + `@TransactionalEventListener` + `@Async`）；
3. **I3-NT-003**：6 个场景实现：
   - `ContractDueEvent` → 合同到期前 N 天
   - `ContractNotRenewedEvent` → 已到期未续签每周一
   - `PaymentDueEvent` → 计划即将到期前 N 天
   - `PaymentOverdueEvent` → 计划逾期当日 + 周
   - `PaymentReceivedEvent` → 实际回款登记给主管
   - `SystemAnnouncementEvent` → 系统公告广播
4. **I3-NT-004**：去重：`(user_id, scenario, biz_type, biz_id, date)` 唯一键 + Redis Set 当日缓存；
5. **I3-NT-005**：每周日 03:00 通知归档（>90 天 `is_archived=1`）；
6. **接入 S4/S6**：合同状态机、回款登记成功、自动到期任务全部 publish 对应事件。

**FE 任务**

7. **`MainLayout.vue` 顶栏铃铛**：`Pinia notificationStore`：每 30s `GET /notifications/unread-count`，`<el-badge :value="count">`；
8. **`NotificationDropdown.vue`**：下拉最新 5 条 + 跳详情/中心；
9. **`NotificationList.vue`**：按场景分类 Tab + 筛选已读 + 批量已读；
10. **`NotificationPreference.vue`**：场景级开关 + 提前天数覆盖。

**DOD**

- 创建合同到期前 30 天的合同，触发到期任务后被 admin 收到 1 条通知；
- 同一合同当日重复触发只产生 1 条；
- 批量已读后角标归零；
- 偏好关闭"回款逾期"后不再收到对应通知。

**关联 ID**：I3-NT-001~005，I3-FE-008/009/010，I2-JB-003 接入。

---

### S9 看板与报表

**BE 任务**

1. **I4-DS-001**：dashboard KPI 改 Redis 缓存（TTL 5 min，key `report:dashboard:{userId}:{role}`）；
2. **I4-DS-004**：`topCustomers` 真实实现（按未核销金额 DESC，前 10）；
3. **I4-DS-005**：`GET /api/v1/dashboard/todos`：聚合到期合同（30 天内）+ 逾期回款（用户范围内）；
4. **I4-RP-001**：`GET /api/v1/reports/payment-detail`：明细报表（合同号/客户/期次/金额/状态/收款日）；
5. **I4-RP-002**：`GET /api/v1/reports/payment-summary`：按部门/负责人/月份汇总；
6. **I4-RP-003**：`GET /api/v1/reports/payment-detail/export`：SXSSF 流式 Excel（≥ 5 万行）；
7. **I4-RP-004**：每 5 min `DashboardCachePrewarmJob`：预热全公司维度 KPI。

**FE 任务**

8. **`DashboardView.vue`** 增 `TopCustomersBlock` + `TodoListBlock`；
9. **KPI 卡** 加同比 / 环比百分比；
10. **`ChartContainer.vue`**：柱 / 折 / 饼通用封装抽到 `src/components/`；
11. **`ReportView.vue`** 落地：明细 Tab + 汇总 Tab + 公共筛选栏 + 导出按钮 + 大数据量分页。

**DOD**

- Dashboard KPI P95 ≤ 500ms（缓存命中）；
- TOP-10 客户榜可点击跳详情；
- 报表导出 5 万行成功且内存稳定 < 200MB；
- 同比/环比指标在客户量充足时可正确显示。

**关联 ID**：I4-DS-001~005，I4-RP-001~004，I4-FE-001~007。

---

### S10 测试与 OpenAPI 同步

**BE 任务**

1. **I3-QA-001**：`SettlementServiceImplTest`：跨期/多收/精度边界至少 8 个用例；
2. **I3-QA-002**：`RedReverseServiceImplTest`：账龄回退 + 状态机验证；
3. **I3-QA-003**：`PaymentImportServiceTest`：脏数据/超大文件/事务回滚；
4. **I3-QA-004**：`NotificationDispatcherImplTest`：6 场景 + 去重 + 偏好关闭；
5. **I1-QA-002**：`DataScopeIntegrationTest`：3 角色 × 3 模块矩阵；
6. **I1-QA-004**：`HardDeleteFlowIT`：二次密码 + 删除日志 + 还原失败；
7. **I2-QA-002**：`ContractCodeConcurrencyTest`：500 QPS Lua 脚本无重复。

**X 任务**

8. **X-DOC-001**：启动后端 → 跑 `./scripts/export-openapi.sh` → `pnpm gen:api`；提交生成产物到 `crms-web/src/api/generated/`，并把现有手写 `customer.ts/auth.ts/report.ts` 改为对生成产物的薄封装；
9. **README 更新**：根 README 增"如何在本地从零启动 + 生成 OpenAPI 客户端"小节。

**DOD**

- `mvn test` 全绿，关键服务覆盖率 ≥ 80%；
- `crms-web/src/api/generated/` 与后端 Controller 一一对应；
- 仓库根目录 README 描述完整本地起步流程。

**关联 ID**：I*-QA-*，X-DOC-001。

---

## 4. 共享组件 / 基础设施沉淀

| 组件 / 模块 | 路径 | 引入 Sprint |
| --- | --- | --- |
| `SecondaryPasswordDialog` | `crms-web/src/components/` | S2 引入，S3/S4/S5/S7 复用 |
| `AttachmentUploader` | `crms-web/src/components/` | S5 引入，S7 复用 |
| `ChartContainer` | `crms-web/src/components/` | S9 引入，未来报表复用 |
| `ChangeTimeline` | `crms-web/src/components/` | S5 引入，未来其它实体复用 |
| `ChangeLogAspect` + `@LogChange` | `crms-app/.../common/aop/` | S4 引入，回款/客户后续接入 |
| `DomainEventBus` | `crms-app/.../common/event/` | S8 引入，S6 后端在登记时 publish |
| `RedisCacheManager` | 替换 `CacheConfig` | S2 引入，S9 配合 TTL |
| `HardDeleteLogService` | `crms-app/.../common/audit/` | S2 引入，S3/S4 复用 |

---

## 5. 任务卡 ↔ Sprint 映射（速查表）

| Task ID 区段 | Sprint |
| --- | --- |
| I1-IAM-001/002/003 + I1-FE-003/004/005 | S1 |
| I1-IAM-008 / I1-SY-001~004 / I1-FE-011/012/013 | S2 |
| I1-CU-003~008 + I1-FE-007~010 + I1-CM-004(部分) | S3 |
| I2-CT-001(收尾)/002~007 + I2-AT-001~003 + I1-CM-004(收尾) | S4 |
| I2-FE-001~008 | S5 |
| I3-PP-001~003 + I3-PR-001~006 + I3-AG-001~004 + I2-JB-003 | S6 |
| I3-FE-001~007 | S7 |
| I3-NT-001~005 + I3-FE-008~010 | S8 |
| I4-DS-001~005 + I4-RP-001~004 + I4-FE-001~007 | S9 |
| I*-QA-* + X-DOC-001 | S10 |

---

## 6. 执行约束

- **每个 Sprint 必须可独立部署**：合并到 `main` 后系统仍能用旧路径继续工作；
- **数据库迁移采用增量 V1.x.x**：禁止修改历史脚本；
- **OpenAPI 在 S4/S6/S8 节点同步刷新一次**（防止 FE 拉到陈旧定义）；
- **每 Sprint 完成后**：在 `tasks.md` 对应任务行后追加 `状态: DONE` 标记或在 commit message 引用 ID；
- **PR 标题约定**：`feat(<module>): Sx-<ID> <短描述>`；
- **分支约定**：`feat/sprint-<N>-<scope>`，例如 `feat/sprint-1-iam-admin`。

---

## 7. 风险与回退

| 风险 | 影响 Sprint | 处置 |
| --- | --- | --- |
| Redis 替换本地缓存导致测试环境压力 | S2/S9 | 优先在 dev/test 验证；保留本地缓存 fallback profile |
| 红冲算法与既有核销账龄勾稽出错 | S6 | 引入 `__before/__after` 快照对比单测 |
| Excel 导入大文件 OOM | S6 | 强制 `easyexcel` 分批读 + 行数上限（10 万） |
| 通知去重 Redis 失效后重复触发 | S8 | 落 DB 唯一索引兜底 |
| 二次密码 Token 泄漏 | S2 | TTL 5 分钟 + 一次性消费 |

---

## 8. 修订记录

| 版本 | 日期 | 修订人 | 说明 |
| --- | --- | --- | --- |
| V1.0 | 2026-05-05 | Cursor Agent | 基于 tasks.md 与代码盘点初版；10 Sprint × 平均 5–7 人天 |

---

> **执行起点**：从 S1 开始；每完成一个 Sprint 在 `docs/iteration-plan.md` 顶部追加进度勾选 `[x] S1` 即可。
