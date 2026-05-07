#!/usr/bin/env bash
# 应用健康检查：连续 3 次失败则告警。建议加入 cron：
# */2 * * * * /opt/crms/deploy/scripts/check_app.sh
set -uo pipefail

URL="${HEALTH_URL:-http://localhost:8080/actuator/health}"
STATE="/var/tmp/crms_health_fails"
RECIPIENTS="${ALERT_EMAIL:-crms-dev@company.com}"
HOST="$(hostname)"

ok=false
for _ in 1 2 3; do
  if curl -fsS --max-time 5 "$URL" | grep -q '"status":"UP"'; then
    ok=true; break
  fi
  sleep 2
done

if $ok; then
  echo 0 > "$STATE"
  exit 0
fi

fails=$(( $(cat "$STATE" 2>/dev/null || echo 0) + 1 ))
echo "$fails" > "$STATE"

if [[ "$fails" -ge 3 ]]; then
  body="[CRMS] $HOST 健康检查连续 ${fails} 次失败：$URL"
  echo -e "$body"
  if command -v mail >/dev/null 2>&1; then
    echo -e "$body" | mail -s "[CRMS][告警] $HOST 应用不可用" "$RECIPIENTS"
  fi
  echo 0 > "$STATE"
fi
