#!/usr/bin/env bash
# 生成可对外发布的一键安装包（tarball + sha256 + 简短 INSTALL.md）。
#
# 用法：
#   ./scripts/make-installer.sh                  # 用 git describe 当版本号
#   ./scripts/make-installer.sh 1.0.2            # 显式版本号
#   VERSION=1.0.2 ./scripts/make-installer.sh    # 同上
#
# 产出（在 dist/ 下）：
#   dist/crms-installer-v1.0.2.tar.gz       — 用户解压即用
#   dist/crms-installer-v1.0.2.tar.gz.sha256 — 校验和
#
# 包内容（git archive HEAD 出来的工作树副本）：
#   install.sh           ← 一键入口
#   INSTALL.md           ← 一页说明
#   crms-app/            ← 后端源码（多阶段 Dockerfile）
#   crms-web/            ← 前端源码（多阶段 Dockerfile）
#   deploy/              ← compose、env 模板、运维脚本
#   scripts/             ← build/release/backup/...
#   db/                  ← Flyway 迁移脚本
#   docs/                ← 用户/管理员手册
#
# 不包含：.git、node_modules、target、*.iml、.env（敏感）

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VERSION="${1:-${VERSION:-}}"
if [ -z "$VERSION" ]; then
  VERSION="$(git describe --tags --always 2>/dev/null | sed 's/^v//' || date +%Y%m%d-%H%M)"
fi
NAME="crms-installer-v${VERSION}"
DIST="$ROOT/dist"

echo "==> [1/4] 工作区检查"
if [ -n "$(git status --porcelain)" ]; then
  echo "    [warn] 工作区有未提交修改，包里只会带最近 commit 的内容"
fi
if [ ! -f install.sh ]; then
  echo "ERR: install.sh 不存在" >&2; exit 1
fi

echo "==> [2/4] 准备 dist 目录"
rm -rf "$DIST"
mkdir -p "$DIST"

echo "==> [3/4] 生成 tarball $NAME.tar.gz"
# git archive 自动按 .gitattributes 过滤，且不会带 .git 自己
git archive --format=tar --prefix="$NAME/" HEAD \
  | gzip -9 > "$DIST/$NAME.tar.gz"

# git archive 不会保留可执行位 -- shell 脚本解压后必须能直接跑，所以重新打一遍
echo "==> [3.5/4] 修可执行位（git archive 不保留 +x）"
WORK="$(mktemp -d)"
tar -xzf "$DIST/$NAME.tar.gz" -C "$WORK"
chmod +x "$WORK/$NAME/install.sh" 2>/dev/null || true
find "$WORK/$NAME/scripts" "$WORK/$NAME/deploy/scripts" -type f -name "*.sh" \
  -exec chmod +x {} \; 2>/dev/null || true
( cd "$WORK" && tar --owner=0 --group=0 -czf "$DIST/$NAME.tar.gz" "$NAME" )
rm -rf "$WORK"

echo "==> [4/4] 生成 sha256"
( cd "$DIST" && shasum -a 256 "$NAME.tar.gz" > "$NAME.tar.gz.sha256" )

cd "$DIST"
SIZE_HUMAN="$(du -h "$NAME.tar.gz" | awk '{print $1}')"
SHA="$(awk '{print $1}' "$NAME.tar.gz.sha256")"

cat <<EOF

==> 完成
  $DIST/$NAME.tar.gz         ($SIZE_HUMAN)
  $DIST/$NAME.tar.gz.sha256  sha256: $SHA

==> 用户安装命令（贴到 README）：
  curl -fsSL https://github.com/<user>/crms/releases/download/v${VERSION}/${NAME}.tar.gz \\
    -o ${NAME}.tar.gz
  echo "$SHA  ${NAME}.tar.gz" | sha256sum -c -
  tar xzf ${NAME}.tar.gz
  cd $NAME
  sudo ./install.sh

==> 上传到 GitHub Release（需 gh CLI）：
  gh release create v${VERSION} \\
    "$DIST/$NAME.tar.gz" \\
    "$DIST/$NAME.tar.gz.sha256" \\
    --title "v${VERSION}" \\
    --notes-file docs/release-notes-v${VERSION}.md

EOF
