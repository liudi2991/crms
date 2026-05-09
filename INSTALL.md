# CRMS 一键安装

合同回款管理系统 (CRMS) 单机部署指南，一条命令装完。

## 你需要准备

- 一台 Linux 服务器：Ubuntu 20.04+/Debian 11+，2 核 4G 起步，磁盘 ≥ 20G
- root 或 sudo 权限
- 公网 IP，且云厂商安全组放行：
  - `80/tcp` — 前端
  - `9000/tcp` — MinIO 附件下载（用于生成预签名 URL，必须公网可达）
  - `22/tcp` — SSH（你登录用）

## 三步装完

```bash
# 1. 下载安装包（替换成 release 页面给出的链接）
curl -fsSL https://github.com/liudi2991/crms/releases/latest/download/crms-installer.tar.gz \
  -o crms-installer.tar.gz

# 2. 解压
tar xzf crms-installer.tar.gz && cd crms-installer-*

# 3. 安装（全自动：装 Docker → 生成密钥 → 编译镜像 → 拉起容器）
sudo ./install.sh
```

首次运行 `install.sh` 大约 8–15 分钟，主要时间花在拉 Maven 依赖和构建镜像上。装好后会输出：

```
🎉  CRMS 部署完成！

  访问地址：     http://<你的公网 IP>/
  默认管理员：   admin
  默认密码：     Admin@12345
```

打开浏览器访问就行。**首次登录后请立即修改 admin 密码**。

## 进阶用法

```bash
# 显式指定公网 IP（探测失败或多网卡时用）
sudo ./install.sh --public-ip 1.2.3.4

# 改 HTTP 端口（不想占用 80）
sudo ./install.sh --http-port 8080

# 升级（已装过，重新拉源码后想刷新镜像）
sudo /opt/crms/install.sh upgrade

# 看运行状态
sudo /opt/crms/install.sh status

# 卸载（保留数据卷 /opt/crms/deploy/data）
sudo /opt/crms/install.sh uninstall
```

## 装在哪了

| 路径 | 内容 |
|---|---|
| `/opt/crms/`            | 源码 + 脚本 |
| `/opt/crms/deploy/.env` | **所有密钥**，权限 600，不要进 git |
| `/opt/crms/deploy/data/`| MySQL / Redis / MinIO / 本地附件数据卷 |
| `/opt/crms/deploy/logs/`| app + nginx 日志 |

## 常见问题

**Q: 80 端口被占用？**
A: 用 `--http-port 8080` 换端口，或 `systemctl stop nginx` 停掉占用的服务。

**Q: 附件下载失败 / URL 里有 `minio:9000`？**
A: 云厂商安全组没放行 9000；登录到云控台「安全组」放行 `9000/tcp`。

**Q: 想换公网 IP？**
A: 改 `/opt/crms/deploy/.env` 里的 `MINIO_PUBLIC_ENDPOINT=http://<新IP>:9000`，然后 `cd /opt/crms/deploy && docker compose -f docker-compose.single.yml up -d crms-app`。

**Q: 备份在哪？**
A: 自动每天 01:00 备份到 `/opt/crms/backups/`。手动备份：`sudo /opt/crms/scripts/backup.sh`。

**Q: 内存不够 build？**
A: 2G 内存的机器编译前端会 OOM。要么升 4G，要么先在本地 build 好镜像再 `docker save` / `scp` / `docker load`。

**Q: 怎么完全卸载？**
A:

```bash
sudo /opt/crms/install.sh uninstall
sudo rm -rf /opt/crms
```

## 文档

详细文档在解压目录的 `docs/` 下：
- `docs/user-manual.md` — 用户手册
- `docs/admin-manual.md` — 管理员手册
- `docs/security-checklist.md` — 安全清单（生产部署务必过一遍）
- `docs/faq.md` — 常见问题
