/**
 * 前端字典（与后端枚举一一对应）。后续接入字典 API 后可由 dictStore 动态获取。
 *
 * 状态类字典使用 StatusMeta 结构 { label, type }，type 对应 el-tag/el-button 的语义色。
 * 保持各模块状态色规则一致：
 *   - success（绿） → 已生效 / 启用 / 已结清 / 正常 等"健康"态
 *   - warning（橙） → 待办 / 待结清 / 即将到期 等"需关注"态
 *   - danger（红）  → 已终止 / 已锁定 / 已删除 / 红冲 等"异常"态
 *   - info（灰）    → 草稿 / 已停用 / 已红冲 等"中性"态
 *   - ''（蓝）      → 已完成 / 已归档 等"中性已完成"态
 */

export interface StatusMeta {
  label: string
  type: 'success' | 'warning' | 'info' | 'danger' | ''
}

export const ContractType = {
  SALES: '销售合同',
  PROCUREMENT: '采购合同',
  SERVICE: '服务合同',
  OTHER: '其他'
} as const

export const ContractStatus: Record<string, StatusMeta> = {
  DRAFT:      { label: '草稿',   type: 'info' },
  EFFECTIVE:  { label: '执行中', type: 'success' },
  COMPLETED:  { label: '已完成', type: '' },
  TERMINATED: { label: '已终止', type: 'danger' },
  EXPIRED:    { label: '已到期', type: 'warning' }
}

export const PaymentPlanStatus: Record<string, StatusMeta> = {
  PENDING: { label: '未结清',   type: 'warning' },
  PARTIAL: { label: '部分核销', type: '' },
  SETTLED: { label: '已结清',   type: 'success' }
}

export const PaymentRecordStatus: Record<string, StatusMeta> = {
  NORMAL:   { label: '正常',    type: 'success' },
  REVERSED: { label: '已红冲',  type: 'info' },
  RED:      { label: '红冲单',  type: 'danger' }
}

/** 客户 / 用户 通用启停状态 */
export const ActiveStatus: Record<string, StatusMeta> = {
  ACTIVE:   { label: '启用', type: 'success' },
  DISABLED: { label: '停用', type: 'info' },
  MERGED:   { label: '已合并', type: 'warning' },
  LOCKED:   { label: '已锁定', type: 'danger' }
}

export const CustomerType = {
  ENTERPRISE: '企业',
  GOVERNMENT: '政府',
  INDIVIDUAL: '个人'
} as const

export const CustomerLevel = ['A', 'B', 'C'] as const

export const DataScope = {
  SELF: '本人',
  DEPT: '本部门',
  ALL: '全公司'
} as const
