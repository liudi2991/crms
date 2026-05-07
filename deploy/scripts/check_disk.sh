#!/usr/bin/env bash
# 磁盘水位巡检：超过阈值则发送告警邮件。建议加入 cron：
# 0 * * * * /opt/crms/deploy/scripts/check_disk.sh
set -euo pipefail

THRESHOLD="${DISK_THRESHOLD:-85}"
RECIPIENTS="${ALERT_EMAIL:-crms-dev@company.com}"
HOST="$(hostname)"

usage="$(df -P -BG | awk 'NR>1 {print $5,$6}')"
over=$(echo "$usage" | awk -v t="$THRESHOLD" '{ pct=$1+0; if (pct >= t) print $0 }')

if [[ -n "$over" ]]; then
  body="[CRMS] $HOST 磁盘水位预警（阈值 ${THRESHOLD}%）：\n$over"
  echo -e "$body"
  if command -v mail >/dev/null 2>&1; then
    echo -e "$body" | mail -s "[CRMS][告警] $HOST 磁盘超阈值" "$RECIPIENTS"
  fi
fi
