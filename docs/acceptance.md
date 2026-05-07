# CRMS 验收指南

本文档说明如何用**自动化脚本**与**人工清单**完成项目验收，并与 [SRS §9 验收标准](./srs.md)（功能 / 性能 / 安全 / 数据 / 培训 / 试运行）对齐。

## 1. 前置条件

| 项 | 要求 |
| --- | --- |
| 后端 | `crms-app` 已启动，`http://localhost:8080/actuator/health` 返回 `UP` |
| 默认账号 | `admin` / `Admin@123456`（若已强制改密，见下文环境变量） |
| 工具 | `curl`、`python3`（脚本解析 JSON） |

**本地依赖（可选参考 README）：**

```bash
cd deploy && docker compose -f docker-compose.test.yml up -d mysql redis minio
cd ../crms-app && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## 2. 自动化验收（必跑）

脚本路径：**仓库根目录** `scripts/acceptance.sh`（非 `deploy/scripts`）。

```bash
cd /path/to/crms
chmod +x scripts/acceptance.sh

# 默认 localhost:8080，账号 admin / Admin@123456
./scripts/acceptance.sh

# 指定后端地址（集成/UAT 环境）
./scripts/acceptance.sh api-uat.company.internal:8080

# 若 admin 已改过密码
ACCEPT_USER=admin ACCEPT_PASS='你的密码' ./scripts/acceptance.sh localhost:8080

# 可选：跳过部分段落（CI 或受限环境）
SKIP_PERM=1 ./scripts/acceptance.sh              # 不建临时销售用户（不测 D 段）
SKIP_ACTUATOR=1 ./scripts/acceptance.sh          # 不测 /actuator/health（G 段）
```

**段落说明：** A 烟测 · B E2E 主流程 · C 已知 bug 回归 · D 权限与数据范围 · E 算法对账 · F 安全 · G 健康与可观测性。

**契约对齐提示（跑 FAIL 时先看）：**

- 回收站列表必须带 `bizType`，取值：`CUSTOMER`、`CONTRACT`、`PAYMENT_RECORD`（见 `RecycleBinController`）。
- `/auth/verify-password` 返回 `data.ok`（布尔），不是 `valid`。
- Dashboard 待回款字段名为 `unpaidAmount`（见 `DashboardVO`）。

**退出码：** `0` 全部通过；`1` 存在失败项（终端会打印 `[FAIL]` 与失败明细）。

## 3. 人工验收清单（脚本不覆盖项）

见 [acceptance-checklist.md](./acceptance-checklist.md)：性能压测、渗透测试、培训与试运行、业务 UAT 签字等。

## 4. 关联文档

| 文档 | 用途 |
| --- | --- |
| [SRS V1.1](./srs.md) | 需求与 §9 验收标准 |
| [DSS V1.1](./dss.md) | 设计、安全、部署 |
| [openapi.yml](./openapi.yml) | REST 契约（与 Springdoc 同步） |
| [deploy/SECURITY.md](../deploy/SECURITY.md) | 上线前密钥与加固清单 |

## 5. CI / Jenkins 建议

在「部署到测试环境」或「合并到 develop」阶段增加一步：

```bash
./scripts/acceptance.sh "${ACCEPT_HOST:-localhost:8080}" || exit 1
```

测试环境若禁止创建用户，设置 `SKIP_PERM=1`。
