# 部署安全提醒（Pre-Production Checklist）

> 适用范围：`~/Desktop/crms/deploy/.env`、`docker-compose.*.yml`、应用层密钥。
> 上生产、集成测试、UAT 等任何**非个人本机**环境**之前**，必须完成本清单。

---

## 1. 风险点速览

| 风险项 | 当前默认值 | 上线后要求 |
|---|---|---|
| MySQL `root` 密码 | `changeme-root` | 强随机口令 ≥ 20 字符 |
| MySQL `crms` 业务账号密码 | `changeme` | 强随机口令 ≥ 20 字符 |
| Redis 密码 | 空 | 设置或仅监听内网 |
| MinIO Root 用户/密码 | `minioadmin` / `changeme-minio` | 强随机口令；用专用 AccessKey |
| AES-256-GCM 主密钥 `CRMS_AES_KEY` | `please-generate-and-replace-this!` | `openssl` 生成 32 字符随机串，**永久备份** |
| 数据卷权限 | `./data/*` 可被宿主任意用户读写 | 仅 `mysql:mysql` 等所属用户可访问 |
| `.env` 文件 | 默认权限 644 | `chmod 600 .env`，禁止入库 |

---

## 2. 一次性生成强口令并写回 `.env`

```bash
cd ~/Desktop/crms/deploy

# 1) 生成强随机口令（去掉特殊字符避免 shell/SQL 转义坑）
NEW_ROOT_PWD=$(openssl rand -base64 24 | tr -d '/+=' | head -c 20)
NEW_BIZ_PWD=$(openssl rand -base64 24 | tr -d '/+=' | head -c 20)
NEW_MINIO_PWD=$(openssl rand -base64 24 | tr -d '/+=' | head -c 20)
NEW_AES=$(openssl rand -base64 24 | tr -d '/+=' | head -c 32)

# 2) 写回 .env（带 .bak 备份）
sed -i.bak \
  -e "s|^MYSQL_ROOT_PASSWORD=.*|MYSQL_ROOT_PASSWORD=$NEW_ROOT_PWD|" \
  -e "s|^MYSQL_PASSWORD=.*|MYSQL_PASSWORD=$NEW_BIZ_PWD|" \
  -e "s|^MINIO_ROOT_PASSWORD=.*|MINIO_ROOT_PASSWORD=$NEW_MINIO_PWD|" \
  -e "s|^CRMS_AES_KEY=.*|CRMS_AES_KEY=$NEW_AES|" \
  .env

# 3) 收紧权限并核对
chmod 600 .env
grep -E '^(MYSQL_ROOT_PASSWORD|MYSQL_PASSWORD|MINIO_ROOT_PASSWORD|CRMS_AES_KEY)=' .env
```

> ⚠️ 把生成的密钥（特别是 `CRMS_AES_KEY`）登记到企业密钥管理系统/密码本，**遗失即所有 AES 加密的敏感字段无法解密**。

---

## 3. 重置容器以应用新密码

MySQL/MinIO 仅在**首次初始化**（数据目录为空）时读取 `MYSQL_ROOT_PASSWORD`、`MINIO_ROOT_PASSWORD`。改 `.env` 后旧数据卷里的密码不会自动变更。

### 全量重置（推荐，本环境无业务数据时）

```bash
cd ~/Desktop/crms/deploy
docker compose -f docker-compose.test.yml down -v
sudo rm -rf ./data/mysql ./data/redis ./data/minio
docker compose -f docker-compose.test.yml up -d mysql redis minio
docker compose -f docker-compose.test.yml logs -f mysql   # 看到 "ready for connections" 后 Ctrl+C
```

### 增量改密码（保留数据时）

```bash
# MySQL：以旧 root 密码登入，修改 root 与业务账号
docker exec -it crms-mysql mysql -uroot -p'<旧 root 密码>' <<SQL
ALTER USER 'root'@'%'         IDENTIFIED BY '$NEW_ROOT_PWD';
ALTER USER 'root'@'localhost' IDENTIFIED BY '$NEW_ROOT_PWD';
ALTER USER 'crms'@'%'         IDENTIFIED BY '$NEW_BIZ_PWD';
FLUSH PRIVILEGES;
SQL

# MinIO：用 mc 客户端
docker run --rm --network deploy_default minio/mc \
  alias set local http://minio:9000 minioadmin '<旧 minio 密码>'
docker run --rm --network deploy_default minio/mc \
  admin user enable local minioadmin
# 改密码请走 MinIO Console（http://localhost:9001）或 mc admin policy
```

---

## 4. Redis 加固

`docker-compose.*.yml` 默认未配置 Redis 密码。生产建议任选其一：

**方案 A：开启密码认证**

```yaml
# docker-compose.prod.yml
redis:
  command:
    - "redis-server"
    - "--save"
    - "60"
    - "1000"
    - "--appendonly"
    - "yes"
    - "--requirepass"
    - "${REDIS_PASSWORD}"
```

并在 `.env` 中设置 `REDIS_PASSWORD=...`，应用通过 `spring.data.redis.password` 自动读取。

**方案 B：仅监听内网**

```yaml
redis:
  ports:
    - "127.0.0.1:6379:6379"   # 不再 0.0.0.0 暴露
```

---

## 5. AES 主密钥管理

- `CRMS_AES_KEY` 长度**严格 32 字符**（256 bit）。
- 一旦签发并落库后，**禁止改动**——否则历史密文全部失效。
- 备份位置（任选其一）：
  - 企业密钥管理服务（KMS、Vault、Secret Manager）
  - 离线加密 USB（≥ 2 份，分人保管）
  - 1Password/Keepass 团队保管库

轮换密钥需要走"双密钥过渡 → 全量重新加密 → 切换到新密钥"流程，参见 `docs/dss.md` § 安全设计。

---

## 6. 文件权限与 Git 卫生

```bash
# .env 不入库（已在 .gitignore）；二次确认：
grep -F '.env' ~/Desktop/crms/.gitignore || echo '.env' >> ~/Desktop/crms/.gitignore

# 收紧权限
chmod 600 ~/Desktop/crms/deploy/.env
chmod 700 ~/Desktop/crms/deploy/data 2>/dev/null || true

# 检查仓库内是否误提交了密钥/密码
cd ~/Desktop/crms
git log -p --all -S 'MYSQL_ROOT_PASSWORD' 2>/dev/null | head
git log -p --all -S 'CRMS_AES_KEY' 2>/dev/null | head
```

如发现历史 commit 含真密钥，立即旋转该密钥并使用 `git filter-repo` 清理。

---

## 7. 网络与端口

| 端口 | 服务 | 生产建议 |
|---|---|---|
| 3306 | MySQL | 仅 `127.0.0.1` 或内网 VLAN |
| 6379 | Redis | 仅 `127.0.0.1` 或内网 VLAN，且开密码 |
| 9000 | MinIO API | 反向代理 + HTTPS |
| 9001 | MinIO Console | 内网仅运维可达 |
| 8080 | crms-app | 经 Nginx 反代到 443，禁止直接对外 |
| 80   | crms-web | 重定向到 443 |

Nginx 终结 HTTPS，证书最少 **Let's Encrypt + 自动续期** 或企业证书。

---

## 8. 上线前最终核查清单

- [ ] `.env` 中所有 `changeme*`/`please-generate-*` 已替换。
- [ ] `chmod 600 .env`，仅运维账号可读。
- [ ] `CRMS_AES_KEY` 已备份到 KMS/密码管理器。
- [ ] MySQL 业务账号 `crms` 仅授予 `crms` 库的最小必要权限（已默认）。
- [ ] Redis 设密码或仅内网监听。
- [ ] MinIO 已创建专用 AccessKey 给应用使用，不再用 root 凭证。
- [ ] Nginx HTTPS 已配置，HTTP 强制跳转 443。
- [ ] 防火墙仅放行 80/443，3306/6379/9000/9001/8080 均不对公网。
- [ ] 数据库每日全量 + binlog 备份，校验恢复脚本。
- [ ] 监控/告警：日志关键词、`/actuator/health`、磁盘水位。

完成后再 `docker compose -f docker-compose.prod.yml up -d`。
