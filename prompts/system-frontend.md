# CRMS 前端 AI 辅助代码生成约定（system-frontend.md）

> 在 Cursor / Claude / GPT 中作为前端任务的"系统提示"加载。

## 1. 技术栈

- Vue 3.4（**只用 Composition API + `<script setup>`**）；
- TypeScript 5.x（`strict: true`，禁止 `any`）；
- Vite 5.x；
- Element Plus 2.7（按需自动引入，不要全量 import）；
- Pinia 2.x；
- Vue Router 4.x（hash 模式）；
- ECharts 5.x（按需 import 组件，避免全量 1MB+ 打包）；
- 工具：`@vueuse/core`、`dayjs`、`lodash-es`。

## 2. 目录结构

```
src/
├── api/
│   ├── http.ts              # axios 单例 + 拦截器
│   ├── generated/           # openapi-typescript-codegen 输出（不要手改）
│   ├── auth.ts              # 业务封装：登录/我/改密
│   ├── customer.ts ...      # 业务封装（必要时可包装 generated）
├── components/              # 通用组件，前缀 Crms*
├── layouts/                 # MainLayout / BlankLayout
├── router/index.ts
├── stores/                  # Pinia
├── utils/
│   ├── permission.ts        # v-perm 指令 + hasPermission 函数
│   ├── enum.ts              # 与后端枚举对齐的字典
│   └── format.ts            # 金额/日期/手机号脱敏 format
├── views/{module}/
│   ├── XxxList.vue
│   ├── XxxDetail.vue
│   └── components/
└── main.ts
```

## 3. 强制约束

### 3.1 类型与 API

所有 API 调用使用 generated 中的类型；不要再手写 interface。

```ts
import { CustomerService } from '@/api/generated/services/CustomerService'
const list = await CustomerService.list({ keyword, page, size })
```

如果 generated 还未产出，临时手写 `src/api/{module}.ts` 时也必须导出 interface（前缀 `Api`）。

### 3.2 鉴权与权限

- 登录态从 `useAuthStore()` 读取；
- 按钮级权限用 `v-perm="'contract:edit'"`；
- 路由级权限通过 `meta.perm` 在 `router/index.ts` 拦截。

### 3.3 全局响应

`api/http.ts` 已经处理了：

- 401 → 弹窗 + 跳登录；
- 403 → toast 错误；
- 业务错误码（`Result.code !== '0'`）→ toast；

所以业务代码 **不要再** 自己写 `if (res.code !== '0') ...`。直接 `try / catch` 即可。

### 3.4 列表页统一形态

每个列表页都要有：

- 顶部"工具栏"（搜索、按钮，遵循 `<div class="toolbar"><div class="left"/><div class="right"/></div>`）；
- 中间 `el-table` + `el-table-column`；
- 底部 `el-pagination`；
- 操作列固定右侧 width=160；
- 状态列用 `el-tag`，颜色由 `enum.ts` 中的 `*Color` 字典决定；
- 金额列加 `class="amount"`（右对齐 + 等宽数字）。

### 3.5 表单

- 用 `el-form` + `:rules` + `ref` 调 `validate()`；
- 校验信息中文友好；
- 提交时 loading；
- 同步保留 `keep-alive` 但路由切走后销毁实例（`route.meta.keepAlive` 显式控制）。

### 3.6 状态管理

每个跨页面共享的状态用 Pinia store（`useAuthStore`、`useDictStore`、`useNotificationStore`），单页面状态用组件本地 ref。

### 3.7 国际化（暂只中文）

文案直接用中文字符串。后续接入 `vue-i18n` 时再统一替换。

### 3.8 样式

- 优先使用 Element Plus 内置 spacing；
- 自定义类用 `crms-` 前缀（`crms-page`、`crms-card`）；
- SCSS 变量集中在 `assets/styles/variables.scss`；
- 不要内联 `style="color: red"`；用 utility class（`.text-danger`）。

### 3.9 性能

- 表格 ≥ 1k 行时使用虚拟滚动 / 切换为后端分页；
- ECharts 用 `markRaw()` 避免 Vue reactivity 包装；
- 图片走懒加载；
- 路由组件全部 `() => import(...)`。

## 4. 永远不要做的事

- ❌ Options API；
- ❌ `any`、`@ts-ignore`、`as any`；
- ❌ 手写 fetch；
- ❌ 在组件里直接 axios 而不经 `http.ts`；
- ❌ 在多个组件复制粘贴 dict / formatter；
- ❌ 在 template 里写复杂表达式（移到 computed）；
- ❌ 给 `el-form-item` 漏 `prop`；
- ❌ 用 `window.confirm`（统一 `ElMessageBox`）。

## 5. 输出格式约定

输出顺序：

1. **页面流程要点**（用户故事 + 1–2 句关键交互）；
2. **新增/修改的文件清单**；
3. **代码块** 一个文件一个 fenced code block，注明完整路径；
4. **后端接口依赖**：明确指出依赖的后端 API（如缺少则同时给出 mock 方法签名）。

复杂页面（>300 行）必须拆分子组件。
