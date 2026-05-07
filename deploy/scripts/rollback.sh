#!/usr/bin/env bash
# 一键回滚到上一个镜像版本（previous 标签）。
#
# 在服务器上跑：
#   /opt/crms/scripts/rollback.sh
#
# 原理：
#   build-images.sh 与 deploy-remote.sh 每次都会维护两套标签：
#     crms-app:current   ← 当前生产版本
#     crms-app:previous  ← 上一个版本
#   本脚本把 current 与 previous 互换，然后 compose up -d 重启服务。
#
# 注意：
#   - 数据库迁移不可逆（Flyway down 不存在），如果 previous 版本对应更老的 schema，
#     回滚后启动会因 Flyway 校验失败而报错。先评估再用。

set -euo pipefail

REMOTE_DIR="${REMOTE_DIR:-/opt/crms}"
cd "$REMOTE_DIR/deploy"

if [ ! -f docker-compose.single.yml ] || [ ! -f .env ]; then
  echo "ERR: $REMOTE_DIR/deploy 下找不到 docker-compose.single.yml 或 .env" >&2
  exit 1
fi

echo "==> 检查 previous 镜像是否存在"
for img in crms-app crms-web; do
  if ! docker image inspect "$img:previous" >/dev/null 2>&1; then
    echo "ERR: 找不到 $img:previous，无法回滚（这是首次部署？）" >&2
    exit 1
  fi
done

echo "==> 当前与上一版本"
docker images --format 'table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}' \
  | grep -E '^REPOSITORY|^crms-(app|web)\s+(current|previous)'

echo
read -p "确认回滚？current 与 previous 将互换 (y/N) " ans
[ "$ans" = "y" ] || [ "$ans" = "Y" ] || { echo "已取消"; exit 0; }

echo "==> 互换 current 与 previous 标签"
for img in crms-app crms-web; do
  cur="$(docker images --format '{{.ID}}' "$img:current"  | head -1)"
  prev="$(docker images --format '{{.ID}}' "$img:previous" | head -1)"
  docker tag "$prev" "$img:current"
  docker tag "$cur"  "$img:previous"
done

echo "==> 重启服务"
docker compose -f docker-compose.single.yml --env-file .env up -d

echo "==> 健康检查"
ok=0
for i in $(seq 1 18); do
  if docker exec crms-app wget -qO- http://localhost:8080/actuator/health 2>/dev/null \
     | grep -q '"status":"UP"'; then
    echo "    [healthy] 回滚成功"
    ok=1
    break
  fi
  sleep 5
done

if [ "$ok" -ne 1 ]; then
  echo "    [warn] 90 秒内未达 UP，最近日志："
  docker logs --tail 80 crms-app
  echo
  echo "如确实失败，可再次 rollback.sh 切回原版本（current/previous 已互换两次回到原状）"
  exit 1
fi

echo
echo "==> 回滚完成。如需重新升级到新版本，从本机重跑 deploy-remote.sh"
