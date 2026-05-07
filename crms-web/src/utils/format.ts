/**
 * 数字 / 金额 / 日期 通用格式化工具。
 */
import dayjs from 'dayjs'

export function formatCurrency(v: number | string | null | undefined, currency = '¥') {
  if (v === null || v === undefined || v === '') return '-'
  const n = typeof v === 'string' ? Number(v) : v
  if (isNaN(n)) return '-'
  return currency + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function formatNumber(v: number | string | null | undefined) {
  if (v === null || v === undefined || v === '') return '-'
  const n = typeof v === 'string' ? Number(v) : v
  return n.toLocaleString('zh-CN')
}

export function formatDate(v: string | Date | null | undefined, fmt = 'YYYY-MM-DD') {
  if (!v) return '-'
  return dayjs(v).format(fmt)
}

export function formatDateTime(v: string | Date | null | undefined) {
  return formatDate(v, 'YYYY-MM-DD HH:mm:ss')
}
