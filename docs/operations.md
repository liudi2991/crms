# CRMS 运维手册

面向负责部署、监控、备份与排障的工程师。密钥与上线前检查以 [deploy/SECURITY.md](../deploy/SECURITY.md) 与 [安全自查清单](./security-checklist.md) 为准。

---

## 1. 架构与端口（常见）

| 组件 | 默认端口 | 说明 |
| --- | --- | --- |
| 后端 `crms-app` | 8080 | Spring Boot，API 前缀 `/api/v1` |
| 前端 `crms-web` | 5173（dev） | 生产一般由 Nginx 提供静态资源 |
| MySQL | 3306 | 业务库、Flyway 迁移（当前最高 V1.0.5） |
| Redis | 6379 | 会话/缓存等 |
| MinIO | 9000 | 附件对象存储（默认本地磁盘；生产切 MinIO，见 §6） |

---

## 2. 本地与测试环境快速启动

```bash
# 依赖（MySQL + Redis + MinIO）
cd deploy
cp .env.example .env   # 已存在可跳过；生产前必须按 SECURITY.md 换强密钥
docker compose -f docker-compose.test.yml up -d mysql redis minio

# 后端（首次 Flyway 自动建表 + 种子数据）
cd ../crms-app
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

`crms-app` 的 `application-local.yml` 会从 `deploy/.env` 读取 `MYSQL_*`、`REDIS_*`、`CRMS_AES_KEY` 等（见该 yml 的 `spring.config.import`）。

**健康检查：**

```bash
curl -s http://localhost:8080/actuator/health
```

---

## 3. 生产部署要点

| 场景 | 编排文件 | 适配脚本 |
| --- | --- | --- |
| 单机部署（一台云主机，无 CI/CD） | [`docker-compose.single.yml`](../deploy/docker-compose.single.yml) | `scripts/build-images.sh`、`scripts/deploy-remote.sh`、`deploy/scripts/rollback.sh` |
| Jenkins + 私有 Registry + 双副本 | [`docker-compose.prod.yml`](../deploy/docker-compose.prod.yml) | [`deploy/Jenkinsfile`](../deploy/Jenkinsfile) |

- 单机首次上线 5 分钟流程：见 [部署快速指南](./deploy-quickstart.md)。
- **不得** 将未轮换的 `deploy/.env` 提交到公开仓库；生产口令用 `openssl` 等生成（见 [`deploy/SECURITY.md`](../deploy/SECURITY.md)）。
- 应用与数据库、Redis 应处于 **内网** 或安全组隔离；MinIO 控制台勿对公网开放。
- 服务器首次初始化（装 Docker/防火墙/目录/cron）：`sudo bash deploy/scripts/init-server.sh`
- HTTPS 证书：`sudo /opt/crms/scripts/letsencrypt.sh issue <domain> <email>`

---

## 4. 日志与排障

| 项 | 位置 / 方式 |
| --- | --- |
| 应用日志 | `crms-app/logs/`（若已配置 file appender）或标准输出（容器 `docker logs`） |
| 操作审计 | 库表 `operation_log`；硬删除 `hard_delete_log` |
| API 契约 | 服务启动后 `GET /v3/api-docs`，或同步 [openapi.yml](./openapi.yml) |
| 烟测 / 回归 | 项目根目录 `./scripts/acceptance.sh <host:port>` |

**常见现象：**

- 登录 401 / 无权限：检查 Sa-Token 是否过期、Nginx 是否透传 `Authorization`、用户角色与数据范围。
- 敏感字段无法解密：检查全环境 `CRMS_AES_KEY` 是否一致；**勿在库已写密文后改密钥**（需数据治理方案）。

---

## 5. 备份与恢复

- 部署目录下 [deploy/scripts/backup.sh](../deploy/scripts/backup.sh) 为示例/辅助脚本，生产应纳入公司统一备份策略（**全量 + binlog/增量、保留周期、恢复演练**）。
- MySQL 数据卷：见 `docker-compose.*.yml` 中 volume 映射；恢复前在 **空库/从库** 验证。

---

## 6. 文件存储模式（local ↔ MinIO）

> v1.0.5 起新增切换开关；详细背景见 [docs/issues/UC-03-05-fixes.md](./issues/UC-03-05-fixes.md)。

**默认 `local`**：文件落 `crms-app/uploads/<bizType>/<yyyy-MM-dd>/<uuid>.<ext>`，仅适合单实例部署。

**生产推荐 `minio`**：

```bash
# deploy/.env 增加
CRMS_STORAGE_TYPE=minio
MINIO_ENDPOINT=http://minio:9000     # 容器内通常用服务名
MINIO_ACCESS_KEY=<生成的强随机串>
MINIO_SECRET_KEY=<生成的强随机串>
MINIO_BUCKET=crms

# 重启
docker compose -f deploy/docker-compose.prod.yml restart crms-app
```

启动日志确认：

```
[main] INFO  c.c.c.file.service.impl.MinioFileStorage - MinioFileStorage: bucket crms ready
```

切到 MinIO 后：
- `previewUrl` 返回预签名 GET URL（默认 10 分钟过期）；
- 浏览器无需 token 直连 MinIO；
- 多实例部署不再有"实例 A 写实例 B 读不到"的问题；
- `LocalFileStorage` 由 `@ConditionalOnProperty` 自动失活，不会双 bean 冲突。

**历史文件迁移**：见 [admin-manual §8.3](./admin-manual.md#83-历史文件迁移)。

---

## 7. Flyway 版本

| 版本 | 内容 |
| --- | --- |
| V1.0.0 | 初始 DDL（IAM、客户、合同、回款、附件、通知、系统等） |
| V1.0.1 | 5 角色 + 30+ 权限点 + role_permission |
| V1.0.2 | 字典 + admin 用户种子 |
| V1.0.3 | 系统参数初值 |
| V1.0.4 | 修复 V1.0.0 中 admin 占位 hash |
| V1.0.5 | 新增 `contract.attachment.max_count = 20`（UC-03-05） |

新增迁移直接放 `crms-app/src/main/resources/db/migration/V1.0.6__*.sql`，应用启动时 Flyway 自动执行。

**回滚策略**：Flyway Community 不支持自动 down，回滚需写 `V1.0.6__revert_x.sql` 反向迁移。

---

## 8. 数据脱敏（外发、测试库）

从生产库导出给非生产环境前，使用 `db/desensitize/run.sh` 与 `desensitize.sql` 流程（见 DSS），**禁止**在生产库直接执行脱敏脚本。

---

## 9. 巡检脚本（可选）

[deploy/scripts](../deploy/scripts/) 下提供磁盘、应用健康等示例脚本，可按机房规范接入 Cron / 监控系统。

---

## 10. 关联文档

- [用户操作手册](./user-manual.md) · [管理员手册](./admin-manual.md)
- [FAQ](./faq.md) · [验收指南](./acceptance.md) · [ER 图](../db/erd.md)
- [迭代偏差修复](./issues/UC-03-05-fixes.md)
