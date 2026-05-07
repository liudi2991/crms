import { get, post, put } from './http'
import type { PageResult } from './customer'

// 注意：所有 ID 字段后端使用 Snowflake 64bit Long，已序列化为字符串。
// 前端必须以 string 形态传递，禁止 Number() 强转。

export interface NotificationVO {
  id: string
  receiverId: string
  scene: string
  bizType: string
  bizId?: string
  title: string
  content: string
  linkUrl?: string
  isRead: number
  readAt?: string
  createdAt: string
  archived: number
}

export interface NotificationSettingVO {
  id?: string
  userId?: string
  scene: string
  enabled: number
  advanceDays?: number
}

export const notificationApi = {
  list: (params: { page?: number; size?: number; scene?: string; unreadOnly?: boolean }) =>
    get<PageResult<NotificationVO>>('/notifications', { params }),
  unreadCount: () => get<{ count: number }>('/notifications/unread-count'),
  top: (limit = 5) => get<NotificationVO[]>('/notifications/top', { params: { limit } }),
  markRead: (id: string) => put<void>(`/notifications/${id}/read`),
  markAllRead: () => put<{ affected: number }>('/notifications/read-all'),
  archive: (id: string) => put<void>(`/notifications/${id}/archive`),
  settings: () => get<NotificationSettingVO[]>('/notifications/settings'),
  saveSettings: (settings: Record<string, NotificationSettingVO>) =>
    post<void>('/notifications/settings', settings)
}

export const NotificationScene: Record<string, { label: string; defaultAdvance?: number }> = {
  CONTRACT_DUE: { label: '合同到期', defaultAdvance: 30 },
  PAYMENT_DUE: { label: '回款临期', defaultAdvance: 7 },
  PAYMENT_OVERDUE: { label: '回款逾期' },
  CUSTOMER_MERGE: { label: '客户合并' }
}
