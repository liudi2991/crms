# 示例：客户列表页（前端）

## 输入

### 系统消息
[crms/prompts/system-frontend.md] 完整加载。

### 用户消息

```markdown
## 上下文

- **任务**：I1-CU-002-FE 客户列表页（含搜索、分页、新建、详情）
- **后端 API**：GET /api/v1/customers, POST /api/v1/customers, GET /api/v1/customers/{id}
- **依赖**：
  - http.ts 已封装；
  - generated client `CustomerService` 已生成；
  - `enum.ts` 含 `CustomerType / CustomerLevel`；
  - 路由已配 `customers` / `customers/:id`。

## 要求

1. 抽 `CustomerSearchBar.vue`（关键词 + 类型 + 等级 + 状态）；
2. 主表 `el-table` + 排序 + 操作列（详情、编辑、停用/启用、删除）；
3. 新建对话框使用 `<el-drawer>` 而非 `<el-dialog>`；
4. 列表行点击客户名称跳转详情页（`router.push`）；
5. v-perm 控制 新建/编辑/删除/合并 按钮；
6. 操作后保留分页与筛选（持久化在 sessionStorage）。
```

## 期望输出节选

### 文件清单

| 文件 | 用途 |
| --- | --- |
| `views/customer/CustomerList.vue` | 主页面 |
| `views/customer/components/CustomerSearchBar.vue` | 搜索栏 |
| `views/customer/components/CustomerFormDrawer.vue` | 新建/编辑抽屉 |
| `api/customer.ts` | 接口封装（基于 generated） |
| `utils/persist.ts` | 列表筛选 / 分页持久化（如不存在则新增） |

### 代码

(每个文件一个 fenced code block，标头注释完整路径)
