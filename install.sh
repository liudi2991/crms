#!/usr/bin/env bash
# CRMS 一键安装/升级脚本。
#
# 用法：
#   sudo ./install.sh                   # 全自动新装（推荐第一次用）
#   sudo ./install.sh --public-ip 1.2.3.4 --http-port 80
#   sudo ./install.sh upgrade           # 已装过：只重 build 当前源码 + 重启
#   sudo ./install.sh status            # 看运行状态
#
# 适用：
#   - Ubuntu 20.04 / 22.04 / 24.04
#   - Debian 11 / 12
#   - 内存 ≥ 2GB（建议 4GB），磁盘 ≥ 20GB
#   - 已开放安全组：80/tcp（前端）、9000/tcp（MinIO 预签名下载）
#
# 全自动新装会：
#   1. 装 docker + docker compose（已装则跳过）
#   2. 把代码部署到 /opt/crms
#   3. 自动生成强密码写到 /opt/crms/deploy/.env
#   4. 在本机 buildx 多阶段构建 crms-app + crms-web 镜像
#   5. docker compose up -d 全栈拉起
#   6. 等待 healthy 后输出访问地址 + 默认账号

set -euo pipefail

# -------- 默认参数 --------
INSTALL_DIR="${INSTALL_DIR:-/opt/crms}"
HTTP_PORT="${HTTP_PORT:-80}"
MINIO_PORT="${MINIO_PORT:-9000}"
PUBLIC_IP=""
ACTION="install"

# -------- 解析参数 --------
while [ $# -gt 0 ]; do
  case "$1" in
    install|upgrade|status|uninstall) ACTION="$1"; shift ;;
    --public-ip)   PUBLIC_IP="$2"; shift 2 ;;
    --http-port)   HTTP_PORT="$2"; shift 2 ;;
    --minio-port)  MINIO_PORT="$2"; shift 2 ;;
    --install-dir) INSTALL_DIR="$2"; shift 2 ;;
    -h|--help)
      sed -n '2,32p' "$0"; exit 0 ;;
    *) echo "未知参数：$1" >&2; exit 1 ;;
  esac
done

# -------- 颜色输出 --------
if [ -t 1 ]; then
  C_GRN=$'\e[32m'; C_YLW=$'\e[33m'; C_RED=$'\e[31m'; C_BLU=$'\e[34m'; C_RST=$'\e[0m'
else
  C_GRN=""; C_YLW=""; C_RED=""; C_BLU=""; C_RST=""
fi
log()  { printf "%s==>%s %s\n" "$C_BLU" "$C_RST" "$*"; }
ok()   { printf "    %s✓%s %s\n" "$C_GRN" "$C_RST" "$*"; }
warn() { printf "    %s⚠%s %s\n" "$C_YLW" "$C_RST" "$*"; }
err()  { printf "    %s✗%s %s\n" "$C_RED" "$C_RST" "$*" >&2; }

# -------- 必须 root --------
if [ "$(id -u)" -ne 0 ]; then
  err "需要 root 或 sudo 运行：sudo $0 $*"
  exit 1
fi

# 当前脚本所在目录（解压出来的 installer 根目录）
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ============ status 子命令 ============
if [ "$ACTION" = "status" ]; then
  if [ ! -f "$INSTALL_DIR/deploy/docker-compose.single.yml" ]; then
    err "未在 $INSTALL_DIR 找到部署，先跑 install"
    exit 1
  fi
  cd "$INSTALL_DIR/deploy"
  docker compose -f docker-compose.single.yml ps
  exit 0
fi

# ============ uninstall 子命令 ============
if [ "$ACTION" = "uninstall" ]; then
  warn "将停止并删除所有 CRMS 容器，但保留 $INSTALL_DIR/deploy/data 数据卷"
  read -r -p "确认继续？(y/N) " ans
  [ "$ans" = "y" ] || [ "$ans" = "Y" ] || exit 0
  cd "$INSTALL_DIR/deploy" 2>/dev/null && docker compose -f docker-compose.single.yml down || true
  ok "已停止"
  echo "    数据仍在 $INSTALL_DIR/deploy/data，需要彻底删请手动 rm -rf"
  exit 0
fi

# -------- 探测公网 IP（未显式指定时） --------
if [ -z "$PUBLIC_IP" ]; then
  log "探测公网 IP"
  PUBLIC_IP="$(curl -fsS --max-time 5 https://ipinfo.io/ip 2>/dev/null \
            || curl -fsS --max-time 5 https://api.ipify.org 2>/dev/null \
            || hostname -I | awk '{print $1}')"
  if [ -z "$PUBLIC_IP" ]; then
    err "无法探测公网 IP，请用 --public-ip 显式指定"
    exit 1
  fi
  ok "公网 IP = $PUBLIC_IP（如不对请加 --public-ip 重跑）"
fi

# -------- [1/7] 装 docker --------
log "[1/7] 检查/安装 Docker"
if ! command -v docker >/dev/null 2>&1; then
  if [ -f "$SCRIPT_DIR/deploy/scripts/init-server.sh" ]; then
    bash "$SCRIPT_DIR/deploy/scripts/init-server.sh"
  else
    err "找不到 deploy/scripts/init-server.sh，安装包不完整"
    exit 1
  fi
else
  ok "docker 已就绪：$(docker --version)"
fi
docker compose version >/dev/null 2>&1 || {
  err "需要 docker compose v2 插件：apt-get install -y docker-compose-plugin"
  exit 1
}

# -------- [2/7] 部署源码到 INSTALL_DIR --------
log "[2/7] 部署源码到 $INSTALL_DIR"
mkdir -p "$INSTALL_DIR"
# rsync 比 cp -r 安全：保留权限、跳过已存在的 .env
if [ "$SCRIPT_DIR" != "$INSTALL_DIR" ]; then
  if command -v rsync >/dev/null 2>&1; then
    rsync -a --exclude='deploy/.env' --exclude='deploy/data' --exclude='deploy/logs' \
          --exclude='.git' --exclude='*/node_modules' --exclude='*/target' \
          "$SCRIPT_DIR/" "$INSTALL_DIR/"
  else
    apt-get install -y rsync >/dev/null 2>&1 || true
    cp -rn "$SCRIPT_DIR/." "$INSTALL_DIR/" 2>/dev/null || true
    cp -r  "$SCRIPT_DIR/crms-app" "$INSTALL_DIR/"
    cp -r  "$SCRIPT_DIR/crms-web" "$INSTALL_DIR/"
    cp -r  "$SCRIPT_DIR/scripts"  "$INSTALL_DIR/"
    cp -r  "$SCRIPT_DIR/db"       "$INSTALL_DIR/" 2>/dev/null || true
  fi
fi
mkdir -p "$INSTALL_DIR/deploy/data/"{mysql,redis,minio,uploads}
mkdir -p "$INSTALL_DIR/deploy/logs/"{app,nginx}
chown -R 999:999 "$INSTALL_DIR/deploy/data/uploads" "$INSTALL_DIR/deploy/logs/app"
ok "源码已就位"

# -------- [3/7] 生成或读取 .env --------
ENV_FILE="$INSTALL_DIR/deploy/.env"
if [ "$ACTION" = "upgrade" ]; then
  if [ ! -f "$ENV_FILE" ]; then
    err "升级模式但找不到 $ENV_FILE，请先跑全新安装"
    exit 1
  fi
  log "[3/7] 升级模式，保留现有 .env"
elif [ -f "$ENV_FILE" ]; then
  log "[3/7] .env 已存在，保留不覆盖"
  warn "如果之前 .env 有错可以删掉它再重跑：rm $ENV_FILE && sudo $0"
else
  log "[3/7] 生成 .env（强密码）"
  rand() { openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c "${1:-20}"; }
  AES_KEY="$(openssl rand -hex 16)"
  cat > "$ENV_FILE" <<EOF
# CRMS 部署配置（由 install.sh 自动生成于 $(date '+%Y-%m-%d %H:%M:%S')）
# 不要进 git！

IMAGE_TAG=current
HTTP_PORT=$HTTP_PORT
MINIO_PORT=$MINIO_PORT

# MySQL
MYSQL_DATABASE=crms
MYSQL_USERNAME=crms
MYSQL_PASSWORD=$(rand 20)
MYSQL_ROOT_PASSWORD=$(rand 20)

# Redis（无密码）
REDIS_PASSWORD=

# MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=$(rand 20)
MINIO_BUCKET=crms
# 浏览器可达的 MinIO 公网地址，用于生成预签名下载 URL
MINIO_PUBLIC_ENDPOINT=http://$PUBLIC_IP:$MINIO_PORT

# 文件存储后端：local（本地磁盘） | minio（推荐）
CRMS_STORAGE_TYPE=minio

# AES-256 主密钥（32 字节十六进制）
CRMS_AES_KEY=$AES_KEY

# 首次创建用户的默认密码（admin 也用此初始密码）
CRMS_DEFAULT_PASSWORD=Admin@12345
EOF
  chmod 600 "$ENV_FILE"
  ok "已生成 $ENV_FILE（包含所有密钥；权限 600）"
fi

# -------- [4/7] 防火墙放行 --------
log "[4/7] 防火墙放行 $HTTP_PORT/tcp 与 $MINIO_PORT/tcp"
if command -v ufw >/dev/null 2>&1; then
  ufw status | grep -q active || true
  ufw allow "$HTTP_PORT/tcp"  >/dev/null 2>&1 || true
  ufw allow "$MINIO_PORT/tcp" >/dev/null 2>&1 || true
  ok "ufw 已放行"
fi
warn "云厂商安全组要手动放行 $HTTP_PORT 和 $MINIO_PORT 端口！"

# -------- [5/7] 构建镜像 --------
log "[5/7] 构建镜像（首次约 8-15 分钟，含 maven 依赖下载）"
cd "$INSTALL_DIR"
if ! docker buildx version >/dev/null 2>&1; then
  err "docker buildx 不可用，请确保安装了 docker-buildx-plugin"
  exit 1
fi
docker buildx build --platform linux/amd64 --load \
  -t crms-app:current crms-app 2>&1 \
  | tail -3 || { err "crms-app 构建失败"; exit 1; }
ok "crms-app 镜像 ready"
docker buildx build --platform linux/amd64 --load \
  -t crms-web:current crms-web 2>&1 \
  | tail -3 || { err "crms-web 构建失败"; exit 1; }
ok "crms-web 镜像 ready"

# -------- [6/7] 启动 --------
log "[6/7] docker compose up -d"
cd "$INSTALL_DIR/deploy"
docker compose -f docker-compose.single.yml --env-file .env up -d 2>&1 | tail -20

# -------- [7/7] 等待 healthy --------
log "[7/7] 等待 crms-app healthy（最多 120 秒）"
healthy=0
for i in $(seq 1 24); do
  if docker exec crms-app wget -qO- http://localhost:8080/actuator/health 2>/dev/null \
     | grep -q '"status":"UP"'; then
    ok "crms-app UP（用了 $((i*5)) 秒）"
    healthy=1
    break
  fi
  sleep 5
done
if [ "$healthy" != "1" ]; then
  err "120 秒内 crms-app 未就绪，最近日志："
  docker logs --tail 60 crms-app
  exit 1
fi

# -------- 总结 --------
echo
printf '%s🎉  CRMS 部署完成！%s\n' "$C_GRN" "$C_RST"
cat <<EOF

  访问地址：     http://$PUBLIC_IP:$HTTP_PORT/
  默认管理员：   admin
  默认密码：     Admin@12345（首次登录后请到「系统管理 → 修改密码」改掉）

  常用命令：
    查看状态：   sudo $INSTALL_DIR/install.sh status
    升级镜像：   sudo $INSTALL_DIR/install.sh upgrade
    查看日志：   cd $INSTALL_DIR/deploy && docker compose -f docker-compose.single.yml logs -f --tail=200
    停止服务：   cd $INSTALL_DIR/deploy && docker compose -f docker-compose.single.yml stop
    数据备份：   $INSTALL_DIR/scripts/backup.sh

  附件下载用 MinIO 预签名 URL，请确保 $MINIO_PORT 端口
  从公网可达（云厂商安全组 + ufw 都放行）。

EOF
