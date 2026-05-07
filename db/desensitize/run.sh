#!/usr/bin/env bash
# =====================================================================
# CRMS 数据脱敏运行脚本（DSS §10.7）
# 用法：
#   ./run.sh <生产库 dump.sql> <脱敏后输出 dir>
# 流程：
#   1) 在隔离 docker mysql 中 import 原始 dump；
#   2) 执行 desensitize.sql；
#   3) 重新 dump 出脱敏后版本；
#   4) 销毁容器。
# 严禁直接对生产库执行！本脚本默认拒绝指向生产 host。
# =====================================================================
set -euo pipefail

DUMP_FILE="${1:?missing dump file}"
OUT_DIR="${2:?missing output dir}"

if [[ -z "${ALLOW_PROD_HOST:-}" ]]; then
  if grep -qiE 'prod|production' <<< "$DUMP_FILE"; then
    echo "[!] 检测到 dump 文件名含 prod/production，请确认非生产环境，并设置 ALLOW_PROD_HOST=1 强制运行" >&2
    exit 1
  fi
fi

CONTAINER="crms-desen-$$"
PASSWD="desen-$(date +%s)"
PORT=$((33000 + RANDOM % 1000))

cleanup() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT

mkdir -p "$OUT_DIR"

echo "[1/4] 启动隔离容器（端口 $PORT）..."
docker run -d --name "$CONTAINER" \
  -e MYSQL_ROOT_PASSWORD="$PASSWD" \
  -e MYSQL_DATABASE=crms \
  -p "$PORT":3306 \
  --health-cmd "mysqladmin ping -h localhost -u root -p$PASSWD" \
  --health-interval 5s --health-retries 20 \
  mysql:8.0 \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_ai_ci \
  --default-authentication-plugin=mysql_native_password >/dev/null

# 等待健康
for _ in $(seq 1 60); do
  if [[ "$(docker inspect -f '{{.State.Health.Status}}' "$CONTAINER")" == "healthy" ]]; then
    break
  fi
  sleep 2
done

echo "[2/4] 导入原始 dump：$DUMP_FILE"
docker exec -i "$CONTAINER" sh -c "exec mysql -uroot -p$PASSWD crms" < "$DUMP_FILE"

echo "[3/4] 执行脱敏脚本"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
docker exec -i "$CONTAINER" sh -c "exec mysql -uroot -p$PASSWD crms" < "$SCRIPT_DIR/desensitize.sql"

OUT_FILE="$OUT_DIR/crms_desensitized_$(date +%Y%m%d_%H%M%S).sql"
echo "[4/4] 导出脱敏后 dump：$OUT_FILE"
docker exec "$CONTAINER" mysqldump --single-transaction --routines --triggers --events \
  -uroot -p"$PASSWD" crms > "$OUT_FILE"
gzip "$OUT_FILE"

echo "[OK] 脱敏完成：${OUT_FILE}.gz"
