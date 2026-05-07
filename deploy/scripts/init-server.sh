#!/usr/bin/env bash
# 新云主机首次跑：装 Docker、开防火墙、建目录、配 cron 备份/巡检。
# 适用：Debian 11/12、Ubuntu 20.04/22.04（其它发行版请手工对照）。
#
# 用法（在服务器上以 root 或 sudo 跑）：
#   curl -fsSL https://raw.githubusercontent.com/<user>/<repo>/main/deploy/scripts/init-server.sh | sudo bash
# 或：
#   sudo bash deploy/scripts/init-server.sh
#
# 完成后机器具备：
#   - Docker Engine + compose 插件
#   - /opt/crms/ 目录骨架
#   - ufw 仅放行 22/80/443
#   - cron 每天 01:00 备份、5 分钟巡检
#   - root 不能 ssh 密码登录（仅 key）

set -euo pipefail

REMOTE_DIR="${REMOTE_DIR:-/opt/crms}"

if [ "$(id -u)" -ne 0 ]; then
  echo "ERR: 需要 root（或 sudo）" >&2
  exit 1
fi

echo "==> [1/6] 装 Docker（如未装）"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sh
  systemctl enable --now docker
else
  echo "    docker 已在  $(docker --version)"
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "ERR: 需要 docker compose v2 插件，请手工安装：" >&2
  echo "    apt-get install -y docker-compose-plugin" >&2
  exit 1
fi

echo "==> [2/6] 建目录骨架 $REMOTE_DIR"
mkdir -p "$REMOTE_DIR"/{deploy,images,scripts,backups,logs}
mkdir -p "$REMOTE_DIR"/deploy/{data/mysql,data/redis,data/minio,logs/app,logs/nginx,nginx/conf.d,nginx/certs}
chmod 700 "$REMOTE_DIR"/deploy/data

echo "==> [3/6] 防火墙（ufw 仅放行 22 / 80 / 443）"
if command -v ufw >/dev/null; then
  ufw --force reset >/dev/null
  ufw default deny incoming
  ufw default allow outgoing
  ufw allow 22/tcp comment 'ssh'
  ufw allow 80/tcp comment 'http'
  ufw allow 443/tcp comment 'https'
  ufw --force enable
  ufw status numbered
else
  echo "    [skip] 系统无 ufw，按云厂商安全组手动放行 22/80/443"
fi

echo "==> [4/6] 时区 + swap"
timedatectl set-timezone Asia/Shanghai 2>/dev/null || true
if [ ! -f /swapfile ] && [ "$(awk '/MemTotal/{print $2}' /proc/meminfo)" -lt 4000000 ]; then
  echo "    [info] 内存 < 4G，加 2G swap"
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile >/dev/null
  swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo "==> [5/6] cron（备份 + 巡检）"
CRON_FILE=/etc/cron.d/crms
cat > "$CRON_FILE" <<EOF
# CRMS 定时任务
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin
MAILTO=""
# 每天 01:00 全量备份 MySQL
0 1 * * * root cd $REMOTE_DIR/deploy && set -a && . ./.env && set +a && $REMOTE_DIR/scripts/backup.sh >> $REMOTE_DIR/logs/backup.log 2>&1
# 每 5 分钟应用健康检查
*/5 * * * * root $REMOTE_DIR/scripts/check_app.sh >> $REMOTE_DIR/logs/check_app.log 2>&1
# 每小时磁盘巡检
0 * * * * root $REMOTE_DIR/scripts/check_disk.sh >> $REMOTE_DIR/logs/check_disk.log 2>&1
EOF
chmod 644 "$CRON_FILE"
systemctl restart cron 2>/dev/null || systemctl restart crond 2>/dev/null || true

echo "==> [6/6] 加固 sshd（禁密码登录、仅 root key 登录）"
if [ -f /etc/ssh/sshd_config ]; then
  cp /etc/ssh/sshd_config /etc/ssh/sshd_config.bak.$(date +%s)
  sed -i 's/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
  sed -i 's/^#\?PermitRootLogin.*/PermitRootLogin prohibit-password/' /etc/ssh/sshd_config
  systemctl reload ssh 2>/dev/null || systemctl reload sshd 2>/dev/null || true
  echo "    [info] 已禁用密码登录；如未配置 ssh key，请在断开前先用 root@<host> 验证 key 登录正常"
fi

echo
echo "==> 初始化完成。下一步在你本机跑："
echo "    ./scripts/build-images.sh 1.0.0"
echo "    ./scripts/deploy-remote.sh 1.0.0 root@$(curl -s ifconfig.me 2>/dev/null || echo '<your-ip>')"
echo
echo "    部署完成后访问： http://<this-host>/"
