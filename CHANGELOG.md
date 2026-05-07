# Changelog

本仓库版本变更记录。遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式与 [Semver 2.0](https://semver.org/lang/zh-CN/)。

---

## [Unreleased]

### Added

- 待补：GitHub Actions CI（lint + test + build）
- 待补：监控告警接入（Sentry / Prometheus）

### Fixed

- UC-03-04 封档合同越权可编辑（已记录待批准，见 [docs/issues/UC-03-04-fixes.md](./docs/issues/UC-03-04-fixes.md)）

---

## [1.0.0] – 2026-05-07

首个可交付版本，覆盖 SRS V1.1 全部 152 个任务，含完整文档与单机部署能力。

### Added — 核心业务

- **IAM 模块**：用户 / 角色 / 部门管理，5 角色 + 30+ 权限点，Sa-Token JWT 认证，数据范围拦截器（ALL / DEPT / SELF）
- **客户档案**：CRUD、合并、去重、联系人、变更记录
- **合同管理**：合同状态机（DRAFT / EFFECTIVE / COMPLETED / TERMINATED / EXPIRED）、附件上传、变更记录、到期提醒
- **回款管理**：回款计划生成（月/季/一次性）、实际回款登记、自动核销（FIFO）、红冲、批量导入、账龄分析（0-30/31-60/61-90/90+）
- **看板与报表**：KPI 看板、月度趋势、TOP 客户、我的待办、Excel 导出（趋势 / 账龄 / TOP / 待办）
- **通知中心**：站内信（合同到期、回款临期、回款逾期、附件操作）
- **系统管理**：系统参数、操作审计、硬删除二次密码、回收站

### Added — 基础设施

- **后端**：Spring Boot 3.2 + MyBatis-Plus + Sa-Token + Flyway + Redis + MinIO 客户端
- **前端**：Vue 3 + TypeScript + Vite + Element Plus + ECharts + Pinia
- **数据库**：Flyway 迁移 V1.0.0–V1.0.6（DDL + 种子 + 系统参数）
- **公共能力**：统一响应、全局异常、AES-256-GCM 字段加密、Snowflake ID、操作日志 AOP、Trace ID 透传

### Added — 部署与运维

- **单机部署**：`deploy/docker-compose.single.yml` + `scripts/build-images.sh` + `scripts/deploy-remote.sh` + `scripts/release.sh` + `scripts/rollback.sh`
- **服务器初始化**：`deploy/scripts/init-server.sh`（装 Docker / ufw / cron / 加固 sshd）
- **HTTPS**：`deploy/scripts/letsencrypt.sh` 一键申请 / 续证
- **生产编排**：`deploy/docker-compose.prod.yml` 双副本 + 外置依赖
- **CI/CD**：`deploy/Jenkinsfile`（多分支 + 镜像构建 + ssh 部署）
- **巡检**：`backup.sh` / `check_app.sh` / `check_disk.sh`
- **数据脱敏**：`db/desensitize/desensitize.sql` + `run.sh`

### Added — 文档

- 软件需求规格 SRS V1.1（599 行）
- 设计规格 DSS V1.1（1396 行）
- 任务拆分 V1.0（152 个任务）
- 用户操作手册 / 管理员手册 / 运维手册 / FAQ
- 部署快速指南（5 分钟版）
- 验收指南 + 人工验收清单 + 报告模板
- 代码自动生成计划（三层生成模型 G0–G6）
- AI 提示词体系（system-backend / system-frontend / task-template + 3 个示例）

### Added — 测试与验收

- 自动化验收脚本 `scripts/acceptance.sh`：A–G 共 7 段、70+ 项检查（冒烟 / E2E / 回归 / 权限 / 算法 / 安全 / 可观测）
- 后端单元测试覆盖率达标（核心模块）
- 前端 Vitest 单测

### Fixed — 验收发现

- **UC-03-05 合同附件**：
  - 数量上限从硬编码 10 改为系统参数 `contract.attachment.max_count` 默认 20（V1.0.5）
  - 大文件上传从 `SYS-500` 改为 `FL-003 文件大小超出限制`（HTTP 413）
  - 新增 `MinioFileStorage`，通过 `crms.storage.type` 切换 local ↔ minio
  - 详见 [docs/issues/UC-03-05-fixes.md](./docs/issues/UC-03-05-fixes.md)
- **UC-04-08 逾期预警**：
  - 修复"看板有逾期金额、列表无红 tag"两套口径不一致问题
  - VO 层即时计算 `overdue / overdueDays`，与 Dashboard / 账龄同口径
  - 详见 [docs/issues/UC-04-08-fixes.md](./docs/issues/UC-04-08-fixes.md)
- **数据库列宽**：`file_object.content_type` `VARCHAR(64)→VARCHAR(255)`、`operation_log.error_message` `VARCHAR(500)→TEXT`（V1.0.6）
- **前端**：
  - 合同/客户详情页路由参数变化时正确重新加载（`watch(id, immediate: true)` + 正则白名单防 `undefined`）
  - 多个 Tab 子组件 `reqToken` 并发控制 + `formatSize` 类型容错
  - 附件预览本地存储模式下使用 `fetch + Authorization` 取 Blob 而非裸 `window.open`
  - `ContractDetail.vue` 签订日期/履约结束 `<el-statistic>` 显示 0 的渲染 bug 修复

### Security

- 所有敏感参数走 `${VAR:?}` 环境变量注入，仓库不含真实密钥
- `crms-app/uploads/` 业务附件目录已加入 `.gitignore`
- `deploy/.env`（含真实密码）不进 git，仅 `deploy/.env.example` 模板提交
- AES-256-GCM 默认密钥为占位符，生产环境强制通过 `CRMS_AES_KEY` 覆盖
- ufw + sshd 加固脚本（`init-server.sh`）默认禁用密码登录、仅 22/80/443 入站

---

[Unreleased]: https://github.com/liudi2991/crms/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/liudi2991/crms/releases/tag/v1.0.0
