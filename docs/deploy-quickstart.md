# 部署快速指南（5 分钟版）

> 适用场景：**单机部署**（一台云主机）+ **无 Jenkins** + **无私有 Docker Registry**。
> 走 Jenkins / 双副本 / 私有 Registry 的体系化部署见 [`deploy/Jenkinsfile`](../deploy/Jenkinsfile) 与 [`deploy/docker-compose.prod.yml`](../deploy/docker-compose.prod.yml)。

---

## 0. 你需要什么

| 项 | 最低要求 | 推荐 |
| --- | --- | --- |
| 云主机 | 2 vCPU / 4 GB RAM / 40 GB SSD | 4 vCPU / 8 GB RAM / 80 GB SSD |
| 操作系统 | Debian 11+ / Ubuntu 22.04+ | Ubuntu 22.04 LTS |
| 网络 | 公网 IP，开放 22 / 80 / 443 | 同左 + 域名 + 备案（中国大陆） |
| 本机 | macOS / Linux + Docker + git + ssh | 同左 |

---

## 1. 本机：把代码与镜像准备好

```bash
cd ~/Desktop/crms

# 1.1 本地配 .env（不会进 git）
cp deploy/.env.example deploy/.env
# 编辑 deploy/.env，至少改：
#   MYSQL_PASSWORD / MYSQL_ROOT_PASSWORD
#   MINIO_ROOT_PASSWORD
#   CRMS_AES_KEY  ← openssl rand -base64 24 | head -c 32
#   CRMS_DEFAULT_PASSWORD ← 第一个登录用户的初始密码

# 1.2 一键打镜像（mvn package + pnpm build + docker build × 2）
./scripts/build-images.sh 1.0.0
```

完成后 `docker images` 应能看到：

```
crms-app    1.0.0     ...
crms-app    current   ...
crms-web    1.0.0     ...
crms-web    current   ...
```

---

## 2. 服务器：一次性初始化

```bash
# 2.1 上服务器
ssh root@<your-server-ip>

# 2.2 把仓库 clone 下来（也可以只把 deploy/ 目录通过 scp 推过去）
apt-get update && apt-get install -y git
git clone https://github.com/<你的用户>/crms.git /opt/crms
cd /opt/crms

# 2.3 一键初始化（装 docker + 防火墙 + 目录 + cron）
sudo bash deploy/scripts/init-server.sh

# 2.4 退出服务器，回本机
exit
```

`init-server.sh` 会：

- 装 Docker Engine + compose 插件
- 建 `/opt/crms/{deploy,images,scripts,backups,logs}` 骨架
- ufw 放行 22 / 80 / 443，其它默认拒绝
- cron 每天 01:00 备份、5 分钟巡检
- 内存 < 4G 自动加 2G swap
- 关闭 sshd 密码登录（**先确认你的 ssh key 已在 root 账号里**）

---

## 3. 本机：把镜像推到服务器并启动

```bash
./scripts/deploy-remote.sh 1.0.0 root@<your-server-ip>
```

脚本会：

1. 校验本机有 `crms-app:1.0.0` / `crms-web:1.0.0`
2. `docker save | gzip` 两个镜像（约 200–400 MB）
3. scp 镜像 + `docker-compose.single.yml` + `.env` + `nginx/` 到 `/opt/crms/`
4. 远端 `docker load` → 维护 `:current` / `:previous` 标签 → `compose up -d`
5. 等待 `/actuator/health` 返回 `UP`（最长 90 秒）

成功后浏览器打开 `http://<your-server-ip>/`，用 `admin` / `Admin@123456` 登录（首次登录强制改密）。

---

## 4. 升级与回滚

### 升级到新版本

```bash
# 提交新代码 + push
git commit -am "feat: xxx" && git push

# 一条龙发版（含烟测、tag、构建、部署）
./scripts/release.sh 1.0.1 root@<your-server-ip>

# push tag 让 GitHub 上能看到 release
git push origin v1.0.1
```

### 回滚到上一版本

```bash
ssh root@<your-server-ip> '/opt/crms/scripts/rollback.sh'
```

> ⚠️ Flyway 不支持自动 down，如果新版本带了 schema 变更，回滚后启动会因校验失败报错。
> 评估要点：是否新增了 `V1.0.x` 迁移；如有，需要先准备 `V1.0.(x+1)__revert_x.sql` 反向迁移。

---

## 5. 启用 HTTPS（可选）

前置：

- 已有域名（如 `crms.example.com`）
- 域名 A 记录指向云主机 IP
- 80 端口对外可达

```bash
ssh root@<your-server-ip>
sudo /opt/crms/scripts/letsencrypt.sh issue crms.example.com admin@example.com
```

完成后 `/opt/crms/deploy/nginx/certs/{fullchain,privkey}.pem` 已就位。
然后修改 `crms-web/nginx.conf`（或 `deploy/nginx/conf.d/default.conf`）启用 443，重新部署。

每周日 03:00 自动续证：

```bash
echo '0 3 * * 0 root /opt/crms/scripts/letsencrypt.sh renew >> /opt/crms/logs/cert.log 2>&1' \
  | sudo tee -a /etc/cron.d/crms
```

---

## 6. 常见排障

| 现象 | 排查 |
| --- | --- |
| `deploy-remote.sh` 卡在「健康检查」 | `ssh ... 'docker logs --tail 200 crms-app'` 看 Spring 启动日志，常见是 MySQL 连不上、Flyway 失败、AES_KEY 长度错 |
| 浏览器打开 502 | `docker compose -f docker-compose.single.yml ps` 看 nginx → app 是不是 healthy；`docker logs crms-web` 看 nginx 日志 |
| 上传附件 SYS-500 | 检查 `CRMS_STORAGE_TYPE`，默认 `minio` 需要 MinIO 起来；`docker logs crms-minio` 看是否健康 |
| 登录后立刻 401 | `CRMS_AES_KEY` 在 `.env` 与上次部署不一致，库里旧密文解不开；改回原 key 或 reset 用户密码 |
| 部署后磁盘暴涨 | `docker system df` + `docker image prune -a`；`/opt/crms/images/` 只保留最近 3 个 tarball（脚本自动维护） |

---

## 7. 关联文档

- 详细运维手册：[`docs/operations.md`](./operations.md)
- 安全自查：[`docs/security-checklist.md`](./security-checklist.md) · [`deploy/SECURITY.md`](../deploy/SECURITY.md)
- 备份恢复：[`deploy/scripts/backup.sh`](../deploy/scripts/backup.sh)
- CI 化（如未来上 Jenkins）：[`deploy/Jenkinsfile`](../deploy/Jenkinsfile)
