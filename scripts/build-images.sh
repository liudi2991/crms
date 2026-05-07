#!/usr/bin/env bash
# 本机一键打 crms-app 与 crms-web 两个 Docker 镜像。
#
# 用法：
#   ./scripts/build-images.sh                 # tag 自动取 git describe 或日期
#   ./scripts/build-images.sh 1.0.0           # 指定 tag
#   ./scripts/build-images.sh 1.0.0 --skip-tests
#
# 产出：
#   crms-app:<TAG>  crms-app:current
#   crms-web:<TAG>  crms-web:current
#
# 同时保留上一次 current 为 previous，便于 rollback.sh 回滚。

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

TAG="${1:-}"
SKIP_TESTS=""
for arg in "${@:2}"; do
  [ "$arg" = "--skip-tests" ] && SKIP_TESTS="-DskipTests"
done

if [ -z "$TAG" ]; then
  TAG="$(git describe --tags --always --dirty 2>/dev/null || date +%Y%m%d-%H%M)"
fi

# 目标平台（默认 linux/amd64，云主机 99% 是 x86_64；mac M 系列本地试跑可设 PLATFORM=linux/arm64）
PLATFORM="${PLATFORM:-linux/amd64}"

echo "==> CRMS build  TAG=${TAG}  PLATFORM=${PLATFORM}  ROOT=${ROOT}"

# ---------- 0. 前置检查 ----------
command -v docker >/dev/null || { echo "ERR: docker 未安装" >&2; exit 1; }
docker info >/dev/null 2>&1 || { echo "ERR: docker daemon 未启动" >&2; exit 1; }
# buildx 跨架构需要 binfmt 支持（Docker Desktop 默认开启）
docker buildx version >/dev/null 2>&1 || { echo "ERR: 需要 docker buildx" >&2; exit 1; }

# ---------- 1+2. 后端镜像（多阶段构建：maven build + jre run，不需要宿主机有 JDK） ----------
echo "==> [1/3] 构建 crms-app:${TAG}  (${PLATFORM})  含 maven 编译 + 打 jar + 拷进 jre"
docker buildx build --platform "${PLATFORM}" --load \
  -t "crms-app:${TAG}" -t "crms-app:current" "$ROOT/crms-app"

# ---------- 2. 前端镜像（多阶段：node 构建 + nginx） ----------
echo "==> [2/3] 构建 crms-web:${TAG}  (${PLATFORM}, 多阶段：node 构建 + nginx)"
docker buildx build --platform "${PLATFORM}" --load \
  -t "crms-web:${TAG}" -t "crms-web:current" "$ROOT/crms-web"

# ---------- 3. previous 标签维护 ----------
# current 升级前的镜像保留为 previous，回滚用
echo "==> [3/3] 维护 previous 标签"
for img in crms-app crms-web; do
  prev_id="$(docker images --format '{{.ID}}' "$img:current" | tail -n +2 | head -1 || true)"
  cur_id="$(docker images --format '{{.ID}}' "$img:current" | head -1 || true)"
  if [ -n "$prev_id" ] && [ "$prev_id" != "$cur_id" ]; then
    docker tag "$prev_id" "$img:previous" 2>/dev/null || true
  fi
done

echo
echo "==> 完成。镜像清单："
docker images --format 'table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedSince}}' \
  | grep -E '^REPOSITORY|^crms-(app|web)' | head -20

echo
echo "==> 下一步："
echo "  本地试跑：    cd deploy && IMAGE_TAG=${TAG} docker compose -f docker-compose.single.yml up -d"
echo "  推到服务器：  ./scripts/deploy-remote.sh ${TAG} <user>@<host>"
