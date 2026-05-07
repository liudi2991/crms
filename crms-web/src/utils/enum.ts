/**
 * 前端字典（与后端枚举一一对应）。后续接入字典 API 后可由 dictStore 动态获取。
 */

export const ContractType = {
  SALES: '销售合同',
  PROCUREMENT: '采购合同',
  SERVICE: '服务合同',
  OTHER: '其他'
} as const

export const ContractStatus = {
  DRAFT: '草稿',
  EFFECTIVE: '执行中',
  COMPLETED: '已完成',
  TERMINATED: '已终止',
  EXPIRED: '已到期'
} as const

export const ContractStatusColor: Record<string, string> = {
  DRAFT: 'info',
  EFFECTIVE: 'success',
  COMPLETED: '',
  TERMINATED: 'warning',
  EXPIRED: 'danger'
}

export const PaymentPlanStatus = {
  PENDING: '未结清',
  PARTIAL: '部分核销',
  SETTLED: '已结清'
} as const

export const PaymentRecordStatus = {
  NORMAL: '正常',
  REVERSED: '已红冲',
  RED: '红冲单'
} as const

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
