#!/usr/bin/env bash
# 用 certbot 容器为单机部署申请/续 Let's Encrypt 证书。
#
# 用法（在服务器上）：
#   sudo /opt/crms/scripts/letsencrypt.sh issue crms.example.com admin@example.com
#   sudo /opt/crms/scripts/letsencrypt.sh renew
#
# 完成后：
#   /opt/crms/deploy/nginx/certs/{fullchain.pem,privkey.pem}
#   续期建议加 cron：0 3 * * 0 root /opt/crms/scripts/letsencrypt.sh renew >> /opt/crms/logs/cert.log 2>&1
#
# 前置：
#   - 域名 A 记录已指向本机
#   - 80 端口对外可达（用 standalone 模式临时占用 80）
#   - 已部署过 docker-compose.single.yml（issue 时会临时停 crms-web 容器）

set -euo pipefail

REMOTE_DIR="${REMOTE_DIR:-/opt/crms}"
CERTS_DIR="$REMOTE_DIR/deploy/nginx/certs"
CERTBOT_DIR="$REMOTE_DIR/deploy/letsencrypt"
mkdir -p "$CERTS_DIR" "$CERTBOT_DIR"

ACTION="${1:-}"

case "$ACTION" in
  issue)
    DOMAIN="${2:?用法：$0 issue <domain> <email>}"
    EMAIL="${3:?用法：$0 issue <domain> <email>}"

    echo "==> 临时停 crms-web 让 80 端口腾出来"
    cd "$REMOTE_DIR/deploy"
    docker compose -f docker-compose.single.yml stop crms-web 2>/dev/null || true

    echo "==> certbot standalone 申请证书 ($DOMAIN)"
    docker run --rm \
      -p 80:80 \
      -v "$CERTBOT_DIR:/etc/letsencrypt" \
      certbot/certbot:latest certonly \
        --standalone --non-interactive --agree-tos \
        --email "$EMAIL" -d "$DOMAIN"

    echo "==> 复制证书到 nginx/certs"
    cp -L "$CERTBOT_DIR/live/$DOMAIN/fullchain.pem" "$CERTS_DIR/fullchain.pem"
    cp -L "$CERTBOT_DIR/live/$DOMAIN/privkey.pem"   "$CERTS_DIR/privkey.pem"
    chmod 640 "$CERTS_DIR"/*.pem

    echo "==> 重启 crms-web（请确认 nginx 配置已启用 443）"
    docker compose -f docker-compose.single.yml up -d crms-web

    echo
    echo "==> 完成。后续每周日 03:00 会自动 renew，请加："
    echo "    echo '0 3 * * 0 root $REMOTE_DIR/scripts/letsencrypt.sh renew >> $REMOTE_DIR/logs/cert.log 2>&1' \\"
    echo "      | tee -a /etc/cron.d/crms"
    ;;

  renew)
    echo "==> certbot renew"
    docker run --rm \
      -v "$CERTBOT_DIR:/etc/letsencrypt" \
      certbot/certbot:latest renew --quiet

    # 复制最新证书覆盖
    for live_dir in "$CERTBOT_DIR/live"/*/; do
      [ -d "$live_dir" ] || continue
      cp -L "$live_dir/fullchain.pem" "$CERTS_DIR/fullchain.pem"
      cp -L "$live_dir/privkey.pem"   "$CERTS_DIR/privkey.pem"
    done

    echo "==> reload nginx"
    docker exec crms-web nginx -s reload 2>/dev/null \
      || docker compose -f "$REMOTE_DIR/deploy/docker-compose.single.yml" restart crms-web
    ;;

  *)
    cat <<EOF
用法：
  $0 issue <domain> <email>   首次申请
  $0 renew                    续期（建议 cron 每周日 03:00）
EOF
    exit 1
    ;;
esac
