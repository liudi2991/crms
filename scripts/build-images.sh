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

echo "==> CRMS build  TAG=${TAG}  ROOT=${ROOT}"

# ---------- 0. 前置检查 ----------
command -v docker >/dev/null || { echo "ERR: docker 未安装" >&2; exit 1; }
docker info >/dev/null 2>&1 || { echo "ERR: docker daemon 未启动" >&2; exit 1; }

# ---------- 1. 后端打包 ----------
echo "==> [1/4] 打包后端 jar"
cd "$ROOT/crms-app"
./mvnw -B -q $SKIP_TESTS clean package
JAR="$(ls target/*.jar | grep -v 'sources\|javadoc' | head -1)"
[ -f "$JAR" ] || { echo "ERR: 找不到 jar"; exit 1; }
# pom.xml 若已设 finalName=crms-app，jar 本身就是 target/crms-app.jar，不必再 cp
if [ "$(basename "$JAR")" != "crms-app.jar" ]; then
  cp -f "$JAR" target/crms-app.jar
fi
echo "    jar = $JAR"

# ---------- 2. 后端镜像 ----------
echo "==> [2/4] 构建 crms-app:${TAG}"
docker build -t "crms-app:${TAG}" -t "crms-app:current" "$ROOT/crms-app"

# ---------- 3. 前端打包 + 镜像 ----------
echo "==> [3/4] 构建 crms-web:${TAG}（多阶段：node 构建 + nginx）"
cd "$ROOT/crms-web"
docker build -t "crms-web:${TAG}" -t "crms-web:current" "$ROOT/crms-web"

# ---------- 4. previous 标签维护 ----------
# current 升级前的镜像保留为 previous，回滚用
echo "==> [4/4] 维护 previous 标签"
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
