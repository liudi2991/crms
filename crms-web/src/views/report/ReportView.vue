<template>
  <div class="crms-page">
    <el-card class="crms-card" shadow="never">
      <template #header>
        <div class="head">
          <span>报表中心</span>
          <div>
            <el-button v-perm="'system:manage'" :icon="Refresh" @click="onEvict">失效缓存</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="tab">
        <el-tab-pane label="月度趋势" name="trend">
          <div class="chart-toolbar">
            <span class="text-muted">最近 12 个月合同 / 回款汇总</span>
            <el-button :icon="Download" type="primary" @click="onExport('trend')">导出 Excel</el-button>
          </div>
          <div ref="trendRef" class="chart" />
          <el-table :data="trend" border size="small">
            <el-table-column prop="month" label="月份" width="120" />
            <el-table-column label="合同金额" align="right">
              <template #default="{ row }">{{ formatCurrency(row.contractAmount) }}</template>
            </el-table-column>
            <el-table-column label="回款金额" align="right">
              <template #default="{ row }">{{ formatCurrency(row.paidAmount) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="账龄分析" name="aging">
          <div class="chart-toolbar">
            <span class="text-muted">应收款按逾期天数分桶</span>
            <el-button :icon="Download" type="primary" @click="onExport('aging')">导出 Excel</el-button>
          </div>
          <div ref="agingRef" class="chart" />
          <el-table :data="aging" border size="small">
            <el-table-column prop="bucket" label="桶" width="120" />
            <el-table-column label="金额" align="right">
              <template #default="{ row }">{{ formatCurrency(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="条数" prop="count" align="right" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="TOP 客户" name="top">
          <div class="chart-toolbar">
            <el-radio-group v-model="topMetric" @change="loadTop">
              <el-radio-button value="PAID">已回款</el-radio-button>
              <el-radio-button value="UNPAID">未回款</el-radio-button>
              <el-radio-button value="CONTRACT">合同额</el-radio-button>
            </el-radio-group>
            <el-input-number v-model="topN" :min="5" :max="100" :step="5" />
            <el-button @click="loadTop">查询</el-button>
            <el-button :icon="Download" type="primary" @click="onExport('top-customers')">导出 Excel</el-button>
          </div>
          <div ref="topRef" class="chart" />
          <el-table :data="top" border size="small">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="customerName" label="客户" min-width="240" show-overflow-tooltip>
              <template #default="{ row }">
                <el-link type="primary" @click="$router.push(`/customers/${row.customerId}`)">
                  {{ row.customerName }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column label="金额" align="right">
              <template #default="{ row }">{{ formatCurrency(row.amount) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="我的待办" name="todos">
          <div class="chart-toolbar">
            <span class="text-muted">合同到期 / 回款临期 / 回款逾期</span>
            <el-button :icon="Download" type="primary" @click="onExport('todos')">导出 Excel</el-button>
          </div>
          <el-table :data="todos" border>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="todoTagType(row.type)" size="small">{{ todoLabel(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="项目" min-width="240" show-overflow-tooltip />
            <el-table-column prop="date" label="日期" width="120" />
            <el-table-column label="金额" width="160" align="right">
              <template #default="{ row }">{{ formatCurrency(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="逾期" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.overdueDays > 0" type="danger" size="small">{{ row.overdueDays }} 天</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button link size="small" @click="$router.push(row.linkUrl)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { Download, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  reportApi,
  type AgingBucket,
  type TodoItemVO,
  type TopCustomerVO,
  type TrendPoint
} from '@/api/report'
import { formatCurrency } from '@/utils/format'
import { tokenStore } from '@/api/http'

const tab = ref<'trend' | 'aging' | 'top' | 'todos'>('trend')

const trend = ref<TrendPoint[]>([])
const aging = ref<AgingBucket[]>([])
const top = ref<TopCustomerVO[]>([])
const todos = ref<TodoItemVO[]>([])

const topMetric = ref<'PAID' | 'UNPAID' | 'CONTRACT'>('PAID')
const topN = ref(10)

const trendRef = ref<HTMLDivElement>()
const agingRef = ref<HTMLDivElement>()
const topRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let agingChart: echarts.ECharts | null = null
let topChart: echarts.ECharts | null = null

/* el-tabs 在非 active pane 上用 display:none → 容器 clientWidth=0，
 * 此时 echarts.init 会拿到 0 宽度并打 console warning，setOption 也无法正确绘图。
 * 用 ResizeObserver 监听容器尺寸：首次拿到正常宽度时再 init，之后变化自动 resize。 */
const observers: ResizeObserver[] = []
function bindResize(el: HTMLElement, chart: echarts.ECharts) {
  const ro = new ResizeObserver(() => chart.resize())
  ro.observe(el)
  observers.push(ro)
}
/** 容器宽度=0 时先挂 RO 等待，避免 init 时打 "Can't get DOM width or height" warning。 */
function whenSized(el: HTMLElement, cb: () => void) {
  if (el.clientWidth > 0 && el.clientHeight > 0) {
    cb()
    return
  }
  const ro = new ResizeObserver(() => {
    if (el.clientWidth > 0 && el.clientHeight > 0) {
      ro.disconnect()
      cb()
    }
  })
  ro.observe(el)
}

async function loadAll() {
  trend.value = await reportApi.trend(12)
  aging.value = await reportApi.aging()
  todos.value = await reportApi.myTodos().catch(() => [])
  await loadTop()
  renderTrend()
  renderAging()
}

async function loadTop() {
  top.value = await reportApi.topCustomers(topN.value, topMetric.value)
  renderTop()
}

/* 把 ¥124,100 这种金额压缩成 12.4w / 1.2 亿等短形式，
 * 避免 Y 轴 tick 全是 0,000 0,000 看起来像 bug */
function formatAxisAmount(v: number): string {
  if (v === 0) return '0'
  const abs = Math.abs(v)
  if (abs >= 1_0000_0000) return (v / 1_0000_0000).toFixed(1).replace(/\.0$/, '') + ' 亿'
  if (abs >= 10000)        return (v / 10000).toFixed(1).replace(/\.0$/, '') + ' 万'
  if (abs >= 1000)         return (v / 1000).toFixed(0) + ' k'
  return String(v)
}

function renderTrend() {
  if (!trendRef.value) return
  if (!trendChart) {
    const el = trendRef.value
    whenSized(el, () => {
      trendChart = echarts.init(el)
      bindResize(el, trendChart)
      renderTrend()
    })
    return
  }
  trendChart.setOption({
    color: ['#1677ff', '#52c41a'],
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v: number) => formatCurrency(v)
    },
    legend: {
      data: ['合同金额', '回款金额'],
      icon: 'roundRect',
      itemWidth: 14,
      itemHeight: 8,
      top: 8,
      textStyle: { color: '#606266' }
    },
    grid: { left: 56, right: 24, top: 44, bottom: 36, containLabel: true },
    xAxis: {
      type: 'category',
      data: trend.value.map((p) => p.month),
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399', fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#909399', fontSize: 11, formatter: formatAxisAmount },
      splitLine: { lineStyle: { color: '#f0f2f5' } }
    },
    series: [
      {
        name: '合同金额',
        type: 'bar',
        barMaxWidth: 24,
        itemStyle: { color: '#1677ff', borderRadius: [4, 4, 0, 0] },
        data: trend.value.map((p) => p.contractAmount)
      },
      {
        name: '回款金额',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: '#52c41a' },
        itemStyle: { color: '#52c41a' },
        data: trend.value.map((p) => p.paidAmount)
      }
    ]
  })
}

function renderAging() {
  if (!agingRef.value) return
  if (!agingChart) {
    const el = agingRef.value
    whenSized(el, () => {
      agingChart = echarts.init(el)
      bindResize(el, agingChart)
      renderAging()
    })
    return
  }
  /* 与 AgingView / Dashboard 保持同一套色板 */
  const PIE_COLORS = ['#1677ff', '#52c41a', '#faad14', '#fa8c16', '#f5222d']
  const total = aging.value.reduce((s, b) => s + Number(b.amount || 0), 0)
  agingChart.setOption({
    color: PIE_COLORS,
    tooltip: {
      trigger: 'item',
      formatter: (p: { name: string; value: number; percent: number }) =>
        `${p.name}<br/>金额：${formatCurrency(p.value)} (${p.percent}%)`
    },
    legend: { bottom: 0, icon: 'circle', textStyle: { color: '#606266', fontSize: 12 } },
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '38%',
        style: { text: '应收合计', fill: '#909399', fontSize: 12 }
      },
      {
        type: 'text',
        left: 'center',
        top: '46%',
        style: { text: formatCurrency(total), fill: '#1f2329', fontSize: 18, fontWeight: 600 }
      }
    ],
    series: [
      {
        type: 'pie',
        radius: ['52%', '72%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: true,
        label: {
          show: true,
          position: 'inside',
          formatter: (p: { percent: number }) =>
            p.percent > 3 ? `${p.percent.toFixed(0)}%` : '',
          color: '#fff',
          fontSize: 12,
          fontWeight: 600
        },
        labelLine: { show: false },
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        data: aging.value.map((b) => ({ name: b.bucket, value: b.amount }))
      }
    ]
  })
}

function renderTop() {
  if (!topRef.value) return
  if (!topChart) {
    const el = topRef.value
    whenSized(el, () => {
      topChart = echarts.init(el)
      bindResize(el, topChart)
      renderTop()
    })
    return
  }
  topChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (v: number) => formatCurrency(v)
    },
    grid: { left: 160, right: 60, top: 20, bottom: 30, containLabel: false },
    yAxis: {
      type: 'category',
      data: top.value.map((t) => t.customerName).reverse(),
      axisLabel: { width: 140, overflow: 'truncate', color: '#606266', fontSize: 12 },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#909399', fontSize: 11, formatter: formatAxisAmount },
      splitLine: { lineStyle: { color: '#f0f2f5' } }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 20,
        data: top.value.map((t) => Number(t.amount)).reverse(),
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: '#69b1ff' },
              { offset: 1, color: '#1677ff' }
            ]
          },
          borderRadius: [0, 4, 4, 0]
        },
        label: {
          show: true,
          position: 'right',
          color: '#606266',
          fontSize: 11,
          formatter: (p: { value: number }) => formatCurrency(p.value)
        }
      }
    ]
  })
}

async function onExport(name: 'trend' | 'aging' | 'top-customers' | 'todos') {
  const url = reportApi.exportUrl(name)
  const token = tokenStore.get()
  try {
    /* 后端 sa-token 配置 token-name=Authorization + token-prefix=Bearer，
     * 必须用 Authorization: Bearer <token>，原来的 satoken header 会 401 */
    const res = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const blob = await res.blob()
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `${name}_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    URL.revokeObjectURL(link.href)
  } catch (e) {
    ElMessage.error('导出失败：' + (e as Error).message)
  }
}

async function onEvict() {
  await reportApi.evictCache()
  ElMessage.success('已失效缓存，下一次请求将重建')
  loadAll()
}

function todoLabel(t: string) {
  return ({ CONTRACT_DUE: '合同到期', PAYMENT_DUE: '回款临期', PAYMENT_OVERDUE: '回款逾期' } as Record<string, string>)[t] || t
}
function todoTagType(t: string): 'warning' | 'primary' | 'danger' {
  return t === 'PAYMENT_OVERDUE' ? 'danger' : t === 'CONTRACT_DUE' ? 'warning' : 'primary'
}

function onResize() {
  trendChart?.resize()
  agingChart?.resize()
  topChart?.resize()
}

watch(tab, () => {
  /* 切到一个之前隐藏的 tab，容器从 display:none 变为 block，
   * 这里用 nextTick 确保 DOM 完成 layout 后再 render，
   * 否则 echarts.init 会拿到 0 宽度（必须 F12 才能看见图） */
  nextTick(() => {
    if (tab.value === 'trend') {
      renderTrend()
      trendChart?.resize()
    } else if (tab.value === 'aging') {
      renderAging()
      agingChart?.resize()
    } else if (tab.value === 'top') {
      renderTop()
      topChart?.resize()
    }
  })
})

onMounted(() => {
  loadAll()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  observers.forEach((ro) => ro.disconnect())
  observers.length = 0
  trendChart?.dispose()
  agingChart?.dispose()
  topChart?.dispose()
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin: 4px 0 12px;
}
.chart {
  width: 100%;
  height: 320px;
  margin-bottom: 12px;
}
.text-muted {
  color: #909399;
  flex: 1;
}
</style>
