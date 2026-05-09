# 合同回款管理系统（CRMS）

[![License](https://img.shields.io/github/license/liudi2991/crms)](./LICENSE)
[![Last Commit](https://img.shields.io/github/last-commit/liudi2991/crms)](https://github.com/liudi2991/crms/commits/main)
[![Stars](https://img.shields.io/github/stars/liudi2991/crms?style=social)](https://github.com/liudi2991/crms)
[![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-4FC08D?logo=vue.js&logoColor=white)](https://vuejs.org/)

> 合同与回款全流程管理一体化平台：客户档案 → 合同状态机 → 回款计划 → 自动核销 → 账龄分析 → 看板报表。
>
> 当前版本：**v1.0.0**（首个可交付版本，覆盖 SRS V1.1 全部 152 个任务）。详见 [CHANGELOG](./CHANGELOG.md)。

## 项目概览

| 模块 | 技术栈 | 端口 |
| --- | --- | --- |
| 后端 [crms-app](./crms-app) | Java 17 + Spring Boot 3.2 + MyBatis-Plus + Sa-Token | 8080 |
| 前端 [crms-web](./crms-web) | Vue 3 + TypeScript + Vite + Element Plus + Pinia | 5173 |
| 数据库 | MySQL 8.0 + Flyway | 3306 |
| 缓存 | Redis 7.2 | 6379 |
| 文件存储 | MinIO | 9000 |

## 仓库结构

```
crms/
├── crms-app/              Spring Boot 后端（含 IAM/客户/合同/回款/通知/报表/系统）
│   ├── src/main/java/com/company/crms/
│   │   ├── common/        公共能力：Result/异常/AOP/AES/数据范围/雪花 ID/编号生成
│   │   ├── iam/           认证、用户、角色、权限、部门
│   │   ├── customer/      客户档案 + 联系人
│   │   ├── contract/      合同 + 状态机 + 附件 + 备注
│   │   ├── payment/       回款计划/实际/核销/红冲/账龄
│   │   ├── notification/  通知中心 + 提醒调度
│   │   ├── report/        看板 + 报表
│   │   └── system/        系统参数 + 操作日志 + 回收站
│   ├── src/main/resources/db/migration/  Flyway V1.0.0~V1.0.3
│   └── scripts/GenerateCode.java         MyBatis-Plus Generator
├── crms-web/              Vue 3 前端（按模块分页面）
├── db/
│   ├── schema/            DDL（与 crms-app 同步）
│   ├── desensitize/       数据脱敏脚本（DSS §10.7）
│   └── erd.md             ER 图与索引说明
├── deploy/
│   ├── docker-compose.test.yml / .prod.yml
│   ├── Jenkinsfile        多分支流水线
│   ├── nginx/             反向代理 + HTTPS
│   ├── scripts/           check_disk.sh / check_app.sh / backup.sh
│   └── .env.example
├── docs/
│   ├── acceptance.md          # 验收指南（脚本用法 + 关联 SRS §9）
│   ├── acceptance-checklist.md # 人工清单（性能/培训/试运行等）
│   ├── srs.md → 软链 SRS V1.1
│   ├── dss.md → 软链 DSS V1.1
│   ├── tasks.md → 软链 任务拆分
│   └── openapi.yml            Springdoc 自动同步覆盖
├── prompts/               AI 辅助生成提示词
│   ├── system-backend.md
│   ├── system-frontend.md
│   ├── task-template.md
│   └── examples/
├── scripts/
│   ├── acceptance.sh      # 自动化验收（A~G 段，见 docs/acceptance.md）
│   └── export-openapi.sh
```

## 阶段交付物（与代码自动生成计划对应）

| 阶段 | 交付物 | 状态 |
| --- | --- | --- |
| G0 | 仓库结构、文档软链、`.gitignore` / `.editorconfig` / `README` | ✅ |
| G1.1 | Spring Boot 脚手架 + Result/异常/AES/雪花 ID/数据范围/AOP | ✅ |
| G1.2 | Vue 3 脚手架 + 路由 + 鉴权 + 全局布局 + 登录页 | ✅ |
| G1.3 | Dockerfile / docker-compose / Jenkinsfile / 巡检脚本 | ✅ |
| G2 | Flyway DDL（4 个版本，含 5 角色 + 30+ 权限点 + 系统参数） | ✅ |
| G3.1 | MyBatis-Plus Generator 脚本 + 使用说明 | ✅ |
| G3.2 | Springdoc 配置 + OpenAPI 导出脚本 + `docs/openapi.yml` 占位 | ✅ |
| G3.3 | 前端 `openapi-typescript-codegen` 配置 + 字典枚举 | ✅ |
| G4 提示词 | system-backend / system-frontend / task-template / 3 个 example | ✅ |
| G4-I1 IAM | 登录 / Me / 改密 / 上下文加载 + 客户 CRUD + 系统参数 | ✅ 参考实现 |
| G4-I2 合同 | 合同状态机 + 创建 + 到期标记 + 提醒调度 | ✅ 参考实现 |
| G4-I3 回款 | 自动核销算法 + 账龄分桶 + 红冲接口 | ✅ 参考实现 |
| G4-I4 报表 | Dashboard KPI + 月度趋势 + 账龄环图（前后端） | ✅ 参考实现 |
| G5 测试 | AES/Mask/状态机/账龄单测 + Testcontainers 配置 + 前端 Vitest | ✅ |
| G6 文档 / 脱敏 | `desensitize.sql/run.sh` + ERD + 完整 README | ✅ |

> ✅ **参考实现** 表示该模块已包含从 Mapper → Service → Controller → DTO/VO 的端到端骨架与关键算法，可作为后续 AI 辅助任务的"金标准"模板。

## 一键部署（推荐生产 / 演示环境）

> 一台干净的 Ubuntu 20.04+ / Debian 11+ 云主机，2 核 4G 起步，三条命令装完。详见 [INSTALL.md](./INSTALL.md)。

```bash
# 1. 下载安装包（替换为 release 页面给出的链接）
curl -fsSL https://github.com/liudi2991/crms/releases/latest/download/crms-installer.tar.gz \
  -o crms-installer.tar.gz

# 2. 解压
tar xzf crms-installer.tar.gz && cd crms-installer-*

# 3. 一键安装（装 Docker → 编译镜像 → 拉起容器，约 10 分钟）
sudo ./install.sh
```

装好后浏览器访问 `http://<你的公网IP>/`，默认账号 `admin` / `Admin@12345`。

云厂商安全组要放行：`80/tcp`（前端）+ `9000/tcp`（MinIO 附件下载）+ `22/tcp`（SSH）。

> 已发布的版本：见 [Releases](https://github.com/liudi2991/crms/releases)。

## 快速开始（本地源码开发）

### 前置依赖
- Java 17+ / Maven 3.9+
- Node 20+ / pnpm 9+
- Docker 24+

### 本地开发

```bash
# 1) 起依赖
cd deploy && cp .env.example .env
docker compose -f docker-compose.test.yml up -d mysql redis minio

# 2) 启动后端（首次启动 Flyway 自动建表 + 写入种子）
cd ../crms-app
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 3) 启动前端
cd ../crms-web
pnpm install
pnpm dev
```

默认账号：`admin` / `Admin@123456`（首登强制改密）

### 自动生成 API 客户端

```bash
# 后端
cd crms-app
mvn -DskipTests test-compile exec:java \
  -Dexec.mainClass="com.company.crms.scripts.GenerateCode" \
  -Dexec.classpathScope=test \
  -Dexec.args="all"

# 同步 OpenAPI（需后端先启动）
cd ../scripts && ./export-openapi.sh localhost:8080

# 前端 TS 类型 + Axios 客户端
cd ../crms-web && pnpm gen:api
```

## 验收（自动化 + 人工清单）

后端启动后，在项目根目录执行：

```bash
./scripts/acceptance.sh localhost:8080
```

详见 [验收指南](./docs/acceptance.md) 与 [人工验收清单](./docs/acceptance-checklist.md)（性能 / 渗透 / 培训 / 试运行等与 SRS §9 对齐项）。

## CI/CD

[deploy/Jenkinsfile](deploy/Jenkinsfile) 多分支流水线：

- `feature/*`：lint + 单测；
- `develop`：lint + 单测 + 构建 + 推送测试环境；
- `tag`：人工审批后部署生产 + smoke test。

## 安全要点

- 所有 API 走 Sa-Token JWT；
- AES-256-GCM 加密敏感字段（电话/邮箱/凭证号）；
- 数据范围拦截器自动追加 SQL 条件；
- 操作日志全量入 `operation_log`，硬删除单独入 `hard_delete_log`；
- 数据脱敏 SOP 见 `db/desensitize/README` 与 `run.sh`；
- 密码 BCrypt 散列；登录失败 5 次锁定 15 分钟；
- HTTPS 由 Nginx 提供，反向代理后端集群。

## 文档

> 完整索引见 [docs/README.md](./docs/README.md)。

**面向使用者**

- [用户操作手册](./docs/user-manual.md) — 业务用户（销售 / 财务 / 高管）按钮级指南
- [管理员手册](./docs/admin-manual.md) — 系统管理员（用户/角色/部门/参数/日志/回收站/MinIO 切换）
- [常见问题 FAQ](./docs/faq.md)

**面向交付与运维**

- [验收指南](./docs/acceptance.md) · [人工验收清单](./docs/acceptance-checklist.md) · [验收报告模板](./docs/acceptance-report-template.md)
- [运维手册](./docs/operations.md) · [上线安全自查](./docs/security-checklist.md)
- [迭代偏差与修复](./docs/issues/)（如 [UC-03-05 修复](./docs/issues/UC-03-05-fixes.md)）

**面向研发**

- [SRS V1.1](./docs/srs.md) · [DSS V1.1](./docs/dss.md)
- [开发任务拆分 V1.0](./docs/tasks.md) · [迭代计划](./docs/iteration-plan.md)
- [OpenAPI](./docs/openapi.yml) · [ER 图](./db/erd.md)
- [代码自动生成计划](./docs/code-gen-plan.md) · [AI 提示词](./prompts/)

## License

Internal use only.
