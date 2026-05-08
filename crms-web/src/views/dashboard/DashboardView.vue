<template>
  <div class="crms-page">
    <el-row v-if="canDashboard" :gutter="16">
      <el-col v-for="kpi in kpis" :key="kpi.label" :span="6">
        <el-card class="kpi-card" shadow="never">
          <div class="kpi-label">{{ kpi.label }}</div>
          <div class="kpi-value">{{ kpi.value }}</div>
          <div v-if="kpi.hint" class="kpi-hint">{{ kpi.hint }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="canDashboard || canPaymentReport" :gutter="16" class="mt">
      <el-col v-if="canDashboard" :span="canPaymentReport ? 16 : 24">
        <el-card class="crms-card" shadow="never">
          <template #header>合同与回款月度趋势（近 12 个月）</template>
          <div ref="trendRef" style="width: 100%; height: 320px"></div>
        </el-card>
      </el-col>
      <el-col v-if="canPaymentReport" :span="canDashboard ? 8 : 24">
        <el-card class="crms-card" shadow="never">
          <template #header>账龄结构</template>
          <div ref="agingRef" style="width: 100%; height: 320px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt">
      <el-col :span="canPaymentReport ? 14 : 24">
        <el-card class="crms-card" shadow="never">
          <template #header>
            <div class="card-head">
              <span>我的待办</span>
              <el-radio-group v-model="todoFilter" size="small">
                <el-radio-button value="ALL">全部</el-radio-button>
                <el-radio-button value="CONTRACT_DUE">合同到期</el-radio-button>
                <el-radio-button value="PAYMENT_DUE">回款临期</el-radio-button>
                <el-radio-button value="PAYMENT_OVERDUE">逾期</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <EmptyHint v-if="!filteredTodos.length" description="暂无待办" />
          <el-table v-else :data="filteredTodos" size="small" border>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="todoTagType(row.type)" size="small">{{ todoLabel(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="项目" min-width="220" show-overflow-tooltip />
            <el-table-column label="日期" width="120">
              <template #default="{ row }">{{ formatDate(row.date) }}</template>
            </el-table-column>
            <el-table-column label="金额" width="140" align="right">
              <template #default="{ row }">{{ formatCurrency(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row }">
                <el-button link size="small" @click="$router.push(row.linkUrl)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col v-if="canPaymentReport" :span="10">
        <el-card class="crms-card" shadow="never">
          <template #header>
            <div class="card-head">
              <span>TOP 客户</span>
              <el-radio-group v-model="topMetric" size="small" @change="loadTop">
                <el-radio-button value="PAID">已回款</el-radio-button>
                <el-radio-button value="UNPAID">未回款</el-radio-button>
                <el-radio-button value="CONTRACT">合同额</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <el-table :data="topRows" size="small" border>
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="customerName" label="客户" min-width="180" show-overflow-tooltip>
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
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw, nextTick, onMounted, onUnmounted, ref, shallowRef } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  reportApi,
  type AgingBucket,
  type DashboardVO,
  type TodoItemVO,
  type TopCustomerVO,
  type TrendPoint
} from '@/api/report'
import { formatCurrency, formatDate, formatNumber } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import EmptyHint from '@/components/EmptyHint.vue'

const auth = useAuthStore()
const canDashboard = computed(() => auth.hasPermission('report:dashboard'))
const canPaymentReport = computed(() => auth.hasPermission('report:payment'))

echarts.use([
  LineChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  TitleComponent,
  LegendComponent,
  CanvasRenderer
])

const dashboard = ref<DashboardVO | null>(null)
const trend = ref<TrendPoint[]>([])
const aging = ref<AgingBucket[]>([])

const trendRef = ref<HTMLDivElement>()
const agingRef = ref<HTMLDivElement>()
const trendChart = shallowRef<echarts.ECharts>()
const agingChart = shallowRef<echarts.ECharts>()

const kpis = computed(() => {
  const d = dashboard.value
  if (!d) return []
  return [
    { label: '合同总额', value: formatCurrency(d.contractAmount) },
    { label: '已回款', value: formatCurrency(d.paidAmount), hint: '本月 ' + formatCurrency(d.paidThisMonth) },
    { label: '待回款', value: formatCurrency(d.unpaidAmount) },
    { label: '逾期金额', value: formatCurrency(d.overdueAmount), hint: '需重点关注' },
    { label: '客户数', value: formatNumber(d.customerCount) },
    { label: '合同数', value: formatNumber(d.contractCount) },
    { label: '30 天内到期', value: formatNumber(d.contractDueIn30Days), hint: '合同条数' }
  ]
})

async function loadData() {
  const tasks: Array<Promise<unknown>> = []
  // 看板分区按权限点条件加载，避免 R01 销售这种没有 report:dashboard 权限的角色
  // 一登录就 403 → axios 拦截器弹「权限不足」toast
  if (canDashboard.value) {
    tasks.push(reportApi.dashboard().then((d) => (dashboard.value = d)))
    tasks.push(reportApi.trend(12).then((t) => (trend.value = t)))
  }
  if (canPaymentReport.value) {
    tasks.push(reportApi.aging().then((a) => (aging.value = a)))
  }
  tasks.push(reportApi.myTodos().catch(() => []).then((td) => (todos.value = td as TodoItemVO[])))
  await Promise.all(tasks)
  // 等 v-if 控制的 dom 渲染出来再 init echarts
  await nextTick()
  if (canDashboard.value) renderTrend()
  if (canPaymentReport.value) {
    renderAging()
    loadTop()
  }
}

const todos = ref<TodoItemVO[]>([])
const todoFilter = ref<'ALL' | 'CONTRACT_DUE' | 'PAYMENT_DUE' | 'PAYMENT_OVERDUE'>('ALL')
const filteredTodos = computed(() =>
  todoFilter.value === 'ALL' ? todos.value : todos.value.filter((t) => t.type === todoFilter.value)
)

function todoLabel(t: string) {
  return ({ CONTRACT_DUE: '合同到期', PAYMENT_DUE: '回款临期', PAYMENT_OVERDUE: '回款逾期' } as Record<string, string>)[t] || t
}
function todoTagType(t: string): 'warning' | 'primary' | 'danger' {
  return t === 'PAYMENT_OVERDUE' ? 'danger' : t === 'CONTRACT_DUE' ? 'warning' : 'primary'
}

const topRows = ref<TopCustomerVO[]>([])
const topMetric = ref<'PAID' | 'UNPAID' | 'CONTRACT'>('PAID')

async function loadTop() {
  topRows.value = await reportApi.topCustomers(10, topMetric.value).catch(() => [])
}

function renderTrend() {
  if (!trendRef.value) return
  if (!trendChart.value) {
    trendChart.value = markRaw(echarts.init(trendRef.value))
  }
  trendChart.value.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['合同金额', '回款金额'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: trend.value.map((p) => p.month) },
    yAxis: { type: 'value' },
    series: [
      {
        name: '合同金额',
        type: 'line',
        smooth: true,
        data: trend.value.map((p) => p.contractAmount)
      },
      {
        name: '回款金额',
        type: 'line',
        smooth: true,
        data: trend.value.map((p) => p.paidAmount)
      }
    ]
  })
}

function renderAging() {
  if (!agingRef.value) return
  if (!agingChart.value) {
    agingChart.value = markRaw(echarts.init(agingRef.value))
  }
  agingChart.value.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: true,
        label: { formatter: '{b}: {d}%' },
        data: aging.value.map((b) => ({ name: b.bucket, value: b.amount }))
      }
    ]
  })
}

function onResize() {
  trendChart.value?.resize()
  agingChart.value?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  trendChart.value?.dispose()
  agingChart.value?.dispose()
})
</script>

<style scoped lang="scss">
.kpi-card {
  margin-bottom: 12px;
  .kpi-label {
    color: #909399;
    font-size: 12px;
  }
  .kpi-value {
    font-size: 22px;
    font-weight: 600;
    margin-top: 6px;
    color: #303133;
  }
  .kpi-hint {
    margin-top: 4px;
    color: #909399;
    font-size: 12px;
  }
}
.mt {
  margin-top: 4px;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
