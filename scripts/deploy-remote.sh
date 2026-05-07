#!/usr/bin/env bash
# 把本机刚打好的镜像 + compose 文件 + .env 推到云主机并启动。
#
# 用法：
#   ./scripts/deploy-remote.sh <TAG> <user>@<host> [REMOTE_DIR]
#   ./scripts/deploy-remote.sh 1.0.0 root@1.2.3.4
#   ./scripts/deploy-remote.sh 1.0.0 deploy@crms.example.com /opt/crms
#
# 默认 REMOTE_DIR=/opt/crms
#
# 前置条件：
#   - 本机：已跑过 ./scripts/build-images.sh <TAG> 把镜像打好
#   - 服务器：已跑过 deploy/scripts/init-server.sh 装好 docker
#   - 本机能 ssh <user>@<host>（建议配 ssh key 免密）
#   - 本机有 deploy/.env 且填好真实密钥（不会进 git）
#
# 流程：
#   1. 校验本机镜像存在、ssh 通
#   2. docker save 两个镜像为 .tar.gz
#   3. scp tarball + compose + .env + nginx 配置到服务器
#   4. 远端 docker load → 备份当前 current 为 previous → 切换到新 TAG
#   5. docker compose -f docker-compose.single.yml up -d
#   6. 健康检查 /actuator/health（最长 90 秒）

set -euo pipefail

TAG="${1:-}"
SSH_TARGET="${2:-}"
REMOTE_DIR="${3:-/opt/crms}"

if [ -z "$TAG" ] || [ -z "$SSH_TARGET" ]; then
  echo "用法：$0 <TAG> <user>@<host> [REMOTE_DIR]" >&2
  echo "示例：$0 1.0.0 root@1.2.3.4" >&2
  exit 1
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# ---------- 0. 前置校验 ----------
echo "==> [0/6] 前置校验  TAG=$TAG  TARGET=$SSH_TARGET  REMOTE_DIR=$REMOTE_DIR"

for img in "crms-app:$TAG" "crms-web:$TAG"; do
  if ! docker image inspect "$img" >/dev/null 2>&1; then
    echo "ERR: 本机找不到镜像 ${img}，请先跑 ./scripts/build-images.sh ${TAG}" >&2
    exit 1
  fi
done

if [ ! -f deploy/.env ]; then
  echo "ERR: 缺 deploy/.env（不会进 git，需要本地配好真实值）" >&2
  echo "    cp deploy/.env.example deploy/.env  然后填密钥" >&2
  exit 1
fi

ssh -o ConnectTimeout=10 -o BatchMode=yes "$SSH_TARGET" 'echo ok' >/dev/null 2>&1 \
  || { echo "ERR: 无法免密 ssh 到 ${SSH_TARGET}，先配置 ssh key" >&2; exit 1; }

# ---------- 1. 打包镜像 ----------
TMP="$(mktemp -d)"
trap "rm -rf $TMP" EXIT

echo "==> [1/6] docker save 两个镜像（可能耗时 1-3 分钟）"
docker save "crms-app:$TAG" | gzip -1 > "$TMP/crms-app-$TAG.tgz" &
docker save "crms-web:$TAG" | gzip -1 > "$TMP/crms-web-$TAG.tgz" &
wait
ls -lh "$TMP" | awk '/crms-/ {print "    "$NF" ("$5")"}'

# ---------- 2. 准备远端目录 ----------
echo "==> [2/6] 准备远端目录 $REMOTE_DIR"
ssh "$SSH_TARGET" "
  set -e
  mkdir -p '$REMOTE_DIR'/{deploy,logs,backups,images,scripts}
  mkdir -p '$REMOTE_DIR'/deploy/{data/mysql,data/redis,data/minio,logs/app,logs/nginx,nginx/conf.d,nginx/certs}
"

# ---------- 3. 传输 ----------
echo "==> [3/6] scp 镜像与配置（首次约 200-400 MB）"
scp -q "$TMP/crms-app-$TAG.tgz"  "$SSH_TARGET:$REMOTE_DIR/images/"
scp -q "$TMP/crms-web-$TAG.tgz"  "$SSH_TARGET:$REMOTE_DIR/images/"

# 配置文件（每次部署都同步，覆盖更新）
scp -q deploy/docker-compose.single.yml \
       deploy/SECURITY.md \
       "$SSH_TARGET:$REMOTE_DIR/deploy/"
scp -qr deploy/scripts/. "$SSH_TARGET:$REMOTE_DIR/scripts/"
scp -qr deploy/nginx/.   "$SSH_TARGET:$REMOTE_DIR/deploy/nginx/" 2>/dev/null || true

# .env 单独处理：远端已有就不覆盖（保护生产强密钥不被本机弱密钥覆盖）
if ssh "${SSH_TARGET}" "[ -f '${REMOTE_DIR}/deploy/.env' ]"; then
  echo "    [keep] 远端已有 .env，保留不覆盖（生产强密钥）"
else
  echo "    [first] 远端无 .env，推送 .env.example 模板，请 ssh 进去手工填真值后再次运行本脚本"
  scp -q deploy/.env.example "${SSH_TARGET}:${REMOTE_DIR}/deploy/.env"
  echo "    ⚠️  下一步：ssh ${SSH_TARGET} 'vi ${REMOTE_DIR}/deploy/.env'  填强密钥后重跑"
  exit 0
fi

# ---------- 4. 远端 load + 切版本 ----------
echo "==> [4/6] 远端加载镜像、切换 current/previous 标签"
ssh "$SSH_TARGET" "
  set -euo pipefail
  cd '$REMOTE_DIR'
  chmod +x scripts/*.sh

  # 备份当前 current → previous（用于 rollback）
  for img in crms-app crms-web; do
    if docker image inspect \$img:current >/dev/null 2>&1; then
      docker tag \$img:current \$img:previous
    fi
  done

  # 加载新镜像
  gunzip -c images/crms-app-$TAG.tgz | docker load
  gunzip -c images/crms-web-$TAG.tgz | docker load

  # 把新镜像同时打成 current
  docker tag crms-app:$TAG crms-app:current
  docker tag crms-web:$TAG crms-web:current

  # 仅保留最近 3 个版本的 tarball，省磁盘
  ls -t images/crms-app-*.tgz 2>/dev/null | tail -n +4 | xargs -r rm -f
  ls -t images/crms-web-*.tgz 2>/dev/null | tail -n +4 | xargs -r rm -f
"

# ---------- 5. 启动 ----------
echo "==> [5/6] docker compose up -d"
ssh "$SSH_TARGET" "
  set -e
  cd '$REMOTE_DIR/deploy'
  IMAGE_TAG=$TAG docker compose -f docker-compose.single.yml --env-file .env up -d
"

# ---------- 6. 健康检查 ----------
echo "==> [6/6] 健康检查（最长 90 秒）"
ssh "$SSH_TARGET" '
  set -e
  ok=0
  for i in $(seq 1 18); do
    if docker exec crms-app wget -qO- http://localhost:8080/actuator/health 2>/dev/null \
       | grep -q "\"status\":\"UP\""; then
      echo "    [healthy] crms-app UP after ${i}*5s"
      ok=1
      break
    fi
    sleep 5
  done
  if [ "$ok" -ne 1 ]; then
    echo "    [warn] 90 秒内 crms-app 未达 UP，最近日志："
    docker logs --tail 50 crms-app
    exit 1
  fi
'

echo
echo "==> 部署完成  TAG=$TAG"
echo "    访问：     http://${SSH_TARGET#*@}/"
echo "    回滚：     ssh $SSH_TARGET '$REMOTE_DIR/scripts/rollback.sh'"
echo "    查看日志： ssh $SSH_TARGET 'cd $REMOTE_DIR/deploy && docker compose -f docker-compose.single.yml logs -f --tail=100'"
