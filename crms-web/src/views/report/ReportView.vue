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
import { onMounted, onUnmounted, ref, watch } from 'vue'
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

function renderTrend() {
  if (!trendRef.value) return
  if (!trendChart) trendChart = echarts.init(trendRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['合同金额', '回款金额'] },
    grid: { left: 60, right: 30, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: trend.value.map((p) => p.month) },
    yAxis: { type: 'value' },
    series: [
      { name: '合同金额', type: 'bar', data: trend.value.map((p) => p.contractAmount) },
      { name: '回款金额', type: 'line', smooth: true, data: trend.value.map((p) => p.paidAmount) }
    ]
  })
}

function renderAging() {
  if (!agingRef.value) return
  if (!agingChart) agingChart = echarts.init(agingRef.value)
  agingChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        data: aging.value.map((b) => ({ name: b.bucket, value: b.amount })),
        label: { formatter: '{b}: {d}%' }
      }
    ]
  })
}

function renderTop() {
  if (!topRef.value) return
  if (!topChart) topChart = echarts.init(topRef.value)
  topChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 160, right: 30, top: 30, bottom: 30 },
    yAxis: {
      type: 'category',
      data: top.value.map((t) => t.customerName).reverse(),
      axisLabel: { width: 140, overflow: 'truncate' }
    },
    xAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        data: top.value.map((t) => Number(t.amount)).reverse(),
        itemStyle: { color: '#409EFF' }
      }
    ]
  })
}

async function onExport(name: 'trend' | 'aging' | 'top-customers' | 'todos') {
  const url = reportApi.exportUrl(name)
  const token = tokenStore.get()
  try {
    const res = await fetch(url, { headers: token ? { satoken: token } : {} })
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
  setTimeout(() => {
    if (tab.value === 'trend') renderTrend()
    else if (tab.value === 'aging') renderAging()
    else if (tab.value === 'top') renderTop()
  }, 0)
})

onMounted(() => {
  loadAll()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
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
