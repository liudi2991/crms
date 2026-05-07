import { describe, expect, it } from 'vitest'
import { formatCurrency, formatDate, formatNumber } from '../format'

describe('formatCurrency', () => {
  it('should format with 2 decimals', () => {
    expect(formatCurrency(1234.5)).toBe('¥1,234.50')
  })
  it('should handle null/empty', () => {
    expect(formatCurrency(null)).toBe('-')
    expect(formatCurrency(undefined)).toBe('-')
    expect(formatCurrency('')).toBe('-')
  })
  it('should handle invalid value', () => {
    expect(formatCurrency('abc')).toBe('-')
  })
  it('should format integer', () => {
    expect(formatCurrency(1000)).toBe('¥1,000.00')
  })
})

describe('formatNumber', () => {
  it('should add thousand separator', () => {
    expect(formatNumber(1234567)).toBe('1,234,567')
  })
})

describe('formatDate', () => {
  it('should format ISO string', () => {
    expect(formatDate('2026-04-30T12:34:56')).toBe('2026-04-30')
  })
  it('should support custom format', () => {
    expect(formatDate('2026-04-30', 'YYYY/MM')).toBe('2026/04')
  })
  it('should return - for null', () => {
    expect(formatDate(null)).toBe('-')
  })
})
