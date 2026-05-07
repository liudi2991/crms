#!/usr/bin/env bash
# MySQL 全量备份脚本（DSS §10.5）。
# 建议 cron：0 1 * * * /opt/crms/deploy/scripts/backup.sh
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/opt/crms/backups}"
RETAIN_DAYS="${RETAIN_DAYS:-30}"
MYSQL_HOST="${MYSQL_HOST:-mysql}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PWD="${MYSQL_ROOT_PASSWORD:?未设置 MYSQL_ROOT_PASSWORD}"
MYSQL_DB="${MYSQL_DB:-crms}"
RECIPIENTS="${ALERT_EMAIL:-crms-dev@company.com}"

ts="$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
out="$BACKUP_DIR/${MYSQL_DB}_${ts}.sql.gz"

if mysqldump --single-transaction --routines --triggers --events \
  -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PWD" "$MYSQL_DB" \
  | gzip > "$out"; then
  size=$(du -h "$out" | cut -f1)
  echo "[OK] backup: $out ($size)"
else
  msg="[CRMS] MySQL 备份失败：$out"
  echo "$msg"
  if command -v mail >/dev/null 2>&1; then
    echo "$msg" | mail -s "[CRMS][告警] 备份失败" "$RECIPIENTS"
  fi
  exit 1
fi

# 清理过期备份
find "$BACKUP_DIR" -name "${MYSQL_DB}_*.sql.gz" -mtime +"$RETAIN_DAYS" -delete
