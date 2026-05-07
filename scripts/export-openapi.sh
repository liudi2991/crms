#!/usr/bin/env bash
# 从运行中的应用导出 OpenAPI 3 文档到 docs/openapi.yml。
# 用法：./scripts/export-openapi.sh [host:port]
set -euo pipefail

HOST="${1:-localhost:8080}"
OUT="$(cd "$(dirname "$0")/.." && pwd)/docs/openapi.yml"

echo "[*] fetching OpenAPI from $HOST ..."
if ! curl -fsS "http://${HOST}/v3/api-docs.yaml" -o "$OUT"; then
  # 后备方案：取 JSON
  curl -fsS "http://${HOST}/v3/api-docs" -o "${OUT%.yml}.json"
  echo "[!] 仅获取到 JSON（${OUT%.yml}.json），请使用 yq 转换为 yaml" >&2
  exit 1
fi
echo "[OK] saved to $OUT ($(wc -l < "$OUT") lines)"
