# UC-03-05 合同附件 — 偏差修复 Issue 列表

> 来源：2026-05-06 验收过程中对 SRS §4.3 UC-03-05 的端到端实测发现 3 项偏差。
> 关联：`scripts/acceptance.sh`、`crms-app/src/main/java/com/company/crms/contract`、`crms-app/src/main/java/com/company/crms/file`、`crms-app/src/main/java/com/company/crms/common/exception/GlobalExceptionHandler.java`。

## Issue #1 · 附件数量上限与 SRS 不一致（确定 bug）

| 字段 | 内容 |
| --- | --- |
| 严重度 | P2（功能性偏差，影响合同附件落档场景） |
| SRS 期望 | 「最多 **20** 个」 |
| 当前实现 | `ContractAttachmentServiceImpl.MAX_PER_CONTRACT = 10`（硬编码） |
| 可重现 | `for i in 1..11; POST /contracts/{id}/attachments` → 第 11 个返回 `CT-004 附件数量已达上限` |

**修复策略**：把硬编码挪到系统参数表，与 `reminder.payment_due_days` 同模式，便于管理员调整。

- 新增系统参数 `contract.attachment.max_count` 默认 20（Flyway V1.0.5）
- `ContractAttachmentServiceImpl.upload` 改为 `systemParamService.getInt("contract.attachment.max_count", 20)`
- 移除 `MAX_PER_CONTRACT` 常量

---

## Issue #2 · 超大文件返回 SYS-500 而不是 FL-003（体验问题）

| 字段 | 内容 |
| --- | --- |
| 严重度 | P3（用户拿到不友好错误码，不影响业务） |
| 当前实现 | Spring 在请求解析期抛 `MaxUploadSizeExceededException`，`GlobalExceptionHandler` 兜底为 `SYS-500` |
| 可重现 | `POST /contracts/{id}/attachments` 上传 51MB → `{"code":"SYS-500","message":"系统错误"}` |
| SRS 期望 | 给出明确的"文件大小超出限制" |

**修复策略**：在 `GlobalExceptionHandler` 增加 `MaxUploadSizeExceededException` 专用处理，统一映射到 `FILE_SIZE_LIMIT (FL-003)`，HTTP 413。

---

## Issue #3 · 生产 MinIO 实现缺失，仍用本地磁盘（设计与实现不符）

| 字段 | 内容 |
| --- | --- |
| 严重度 | P1（生产部署阻断：多实例下文件读不到） |
| 当前实现 | 仅 `LocalFileStorage`；MinIO 依赖 + `MinioProperties` 已配齐，但缺 `MinioFileStorage` |
| README/DSS 描述 | 「文件存储 MinIO」 |
| 可重现 | 上传后落到 `crms-app/uploads/...`，`previewUrl` 返回 `/api/v1/files/{id}/preview`（本地路径） |

**修复策略**：

- 新增 `MinioFileStorage implements FileStorage`，实现 `save / load / delete / previewUrl / bucket`
- 通过配置开关 `crms.storage.type=local|minio` 二选一（默认 `local`，向后兼容当前部署）
- `previewUrl` 走 MinIO 预签名 URL（`getPresignedObjectUrl` GET，过期时间由调用方传入）
- 启动时若 bucket 不存在则自动创建
- 不强制切换：`acceptance.sh` 与现有用户保持 local 即可跑通

---

## 验证计划

| 项 | 方法 | 通过标准 |
| --- | --- | --- |
| #1 | 上传 21 个附件 | 第 1–20 OK，第 21 个返回 `CT-004` |
| #1 系统参数可调 | 改成 5，再传 6 个 | 第 6 个返回 `CT-004` |
| #2 | 上传 51MB 文件 | HTTP 413 + `code=FL-003` |
| #3 单测 | `mvn test` | 33 + 全过 |
| 整体 | `./scripts/acceptance.sh` | PASS=70 / FAIL=0 |

## 时间盒

- ① + ② 一次提交完成（约 30 分钟）
- ③ MinIO 实现一次提交完成（约 60 分钟，含切换文档）
