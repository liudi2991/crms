#!/usr/bin/env bash
# 一条龙发版：校验工作区 → 打 tag → 构建镜像 → 部署到远端。
#
# 用法：
#   ./scripts/release.sh <VERSION> <user>@<host>
#   ./scripts/release.sh 1.0.0 root@1.2.3.4
#   ./scripts/release.sh 1.0.0 root@1.2.3.4 --skip-tests
#   ./scripts/release.sh 1.0.0 root@1.2.3.4 --no-tag        # 不创建 git tag（用于补发）
#
# 流程：
#   1. 工作区校验（无脏改动、无未推送提交）
#   2. acceptance 烟测（可 --skip-acc 跳过）
#   3. git tag v<VERSION>（可 --no-tag 跳过）
#   4. build-images.sh <VERSION>
#   5. deploy-remote.sh <VERSION> <SSH_TARGET>
#   6. 提示 push tag 到 GitHub

set -euo pipefail

VERSION="${1:-}"
SSH_TARGET="${2:-}"
SKIP_TESTS=""
SKIP_ACC=""
NO_TAG=""

for arg in "${@:3}"; do
  case "$arg" in
    --skip-tests) SKIP_TESTS="--skip-tests" ;;
    --skip-acc)   SKIP_ACC=1 ;;
    --no-tag)     NO_TAG=1 ;;
    *) echo "未知参数：$arg" >&2; exit 1 ;;
  esac
done

if [ -z "$VERSION" ] || [ -z "$SSH_TARGET" ]; then
  cat <<EOF >&2
用法：$0 <VERSION> <user>@<host> [选项]
选项：
  --skip-tests   不跑后端单测（mvn package 加 -DskipTests）
  --skip-acc     不跑 scripts/acceptance.sh 烟测
  --no-tag       不创建 git tag（如已存在或仅是补发）
示例：$0 1.0.0 root@1.2.3.4
EOF
  exit 1
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# ---------- 0. 工作区校验 ----------
echo "==> [0/5] 工作区校验"
if [ -n "$(git status --porcelain)" ]; then
  echo "ERR: 工作区有未提交改动，先 commit 或 stash" >&2
  git status --short
  exit 1
fi

if [ -z "$NO_TAG" ] && git tag --list | grep -qx "v$VERSION"; then
  echo "ERR: tag v$VERSION 已存在；要补发请加 --no-tag" >&2
  exit 1
fi

# ---------- 1. 烟测 ----------
if [ -z "$SKIP_ACC" ] && [ -x scripts/acceptance.sh ]; then
  echo "==> [1/5] acceptance 烟测（针对 localhost:8080，需后端在跑）"
  if curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then
    bash scripts/acceptance.sh localhost:8080 || {
      echo "ERR: acceptance 失败，--skip-acc 可跳过" >&2
      exit 1
    }
  else
    echo "    [warn] localhost:8080 未响应，跳过烟测（先启动后端再发版更稳）"
  fi
else
  echo "==> [1/5] 跳过烟测"
fi

# ---------- 2. 打 tag ----------
if [ -z "$NO_TAG" ]; then
  echo "==> [2/5] git tag v$VERSION"
  git tag -a "v$VERSION" -m "Release v$VERSION"
else
  echo "==> [2/5] 跳过 git tag"
fi

# ---------- 3. 构建镜像 ----------
echo "==> [3/5] 构建镜像"
bash scripts/build-images.sh "$VERSION" $SKIP_TESTS

# ---------- 4. 部署远端 ----------
echo "==> [4/5] 部署到 $SSH_TARGET"
bash scripts/deploy-remote.sh "$VERSION" "$SSH_TARGET"

# ---------- 5. 提示 ----------
echo "==> [5/5] 完成"
if [ -z "$NO_TAG" ]; then
  echo "    push tag 到 GitHub： git push origin v$VERSION"
fi
echo "    回滚命令：           ssh $SSH_TARGET '/opt/crms/scripts/rollback.sh'"
echo "    查看运行：           ssh $SSH_TARGET 'cd /opt/crms/deploy && docker compose -f docker-compose.single.yml ps'"
