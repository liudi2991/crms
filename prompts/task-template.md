# CRMS 单任务提示模板（task-template.md）

> 复制本模板到对话开头，把"{{}}"占位符替换为真实内容，再把 `system-backend.md` 或 `system-frontend.md` 作为系统消息一起加载。

## 系统消息（system role）

- 后端任务：完整复制 `prompts/system-backend.md`；
- 前端任务：完整复制 `prompts/system-frontend.md`。

## 用户消息（user role）

```markdown
## 上下文

- **任务**：{{粘贴 docs/tasks.md 中对应的任务卡，例如 I1-CU-005 客户合并}}
- **关联 SRS**：{{对应 UC 编号 + 简述，如 UC-CU-05 客户合并}}
- **关联 DSS 章节**：{{§3.2.1 客户合并算法 简述要点}}
- **数据库表**：
  ```sql
  -- 仅复制相关 DDL（不要塞全量 init.sql）
  ```
- **依赖代码（已存在）**：
  - `crms-app/src/main/java/com/company/crms/customer/service/CustomerService.java`
  - `crms-app/src/main/java/com/company/crms/customer/entity/Customer.java`
  - 我已经实现了基础 CRUD（来自 MyBatis-Plus Generator）。
- **当前权限点**：`customer:merge`（已在 V1.0.1 中定义）；
- **错误码占位**：使用 `ErrorCode.CU_MERGE_INVALID` 等，详见 `ErrorCode.java`。

## 要求

1. **遵守** `system-backend.md` 中的所有约定；
2. 输出顺序：设计要点 → 文件清单 → 代码；
3. 给出至少 3 条 JUnit 用例（含 1 条并发竞态用例，使用 `CountDownLatch` 模拟）；
4. 任何"我不确定该选 A/B"的决策，都要在"设计要点"中显式列出；
5. 不要创建以下文件（已存在，仅做修改）：
   - `Customer.java`
   - `CustomerMapper.java`
   - `CustomerService.java`（接口扩展即可）

## 验收

- [ ] 编译通过；
- [ ] 单测覆盖率 ≥ 95%；
- [ ] 所有公开方法有 Javadoc；
- [ ] OpenAPI 注解完整；
- [ ] 关键决策点写在 PR description 而非代码注释。
```

## 前端任务示例

```markdown
## 上下文

- **任务**：I1-CU-006-FE 客户合并对话框组件 + 列表页接入
- **关联 SRS**：UC-CU-05
- **关联 DSS**：§3.2.1，§3.2.4 表单交互规范
- **依赖**：
  - 后端 API：`POST /api/v1/customers/merge`（已发布 OpenAPI）
  - generated client：`CustomerService.merge()`
  - 现有页面：`src/views/customer/CustomerList.vue`
- **设计稿**：略（提供 Figma 链接或描述）

## 要求

1. 遵守 `system-frontend.md`；
2. 抽出 `MergeDialog.vue` 子组件，列表页通过 `v-model:visible` 控制；
3. 二次确认（合并不可撤销）使用 `ElMessageBox.confirm` 强制 type=warning + 倒计时按钮；
4. 提交前再次拉取主体客户最新数据，避免被合并对象状态过期；
5. 提交成功后刷新列表 + 关闭对话框 + 弹 success toast；
6. 失败时停留在对话框，按错误码定向提示（CU-004 → 红色横幅）；

## 验收

- [ ] 列表页"合并"按钮带 v-perm="'customer:merge'"；
- [ ] 表单空校验通过；
- [ ] 单测：模拟合并失败/成功路径；
- [ ] 不破坏现有 keep-alive 状态。
```
