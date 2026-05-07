# 多阶段构建：node 阶段构建 dist，nginx 阶段提供静态文件
FROM node:20-alpine AS builder
# 大陆主机 build 时走 npmmirror 加速；境外环境可 docker build --build-arg NPM_REGISTRY=https://registry.npmjs.org/
ARG NPM_REGISTRY=https://registry.npmmirror.com/
WORKDIR /build
RUN npm config set registry "$NPM_REGISTRY"
COPY package.json pnpm-lock.yaml* ./
RUN corepack enable && corepack prepare pnpm@9 --activate \
 && pnpm config set registry "$NPM_REGISTRY" \
 && (pnpm install --frozen-lockfile || pnpm install)
COPY . .
RUN pnpm build

FROM nginx:1.25-alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
