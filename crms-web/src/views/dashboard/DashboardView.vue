<template>
  <div class="crms-page">
    <!-- 金额型 KPI：4 张主指标，第一行整齐铺满 24 列（每张 span=6） -->
    <el-row v-if="canDashboard" :gutter="16">
      <el-col v-for="kpi in moneyKpis" :key="kpi.label" :span="6">
        <el-card class="kpi-card" :class="`tone-${kpi.tone}`" shadow="never">
          <div class="kpi-head">
            <span class="kpi-label">{{ kpi.label }}</span>
            <div class="kpi-icon">
              <el-icon><component :is="kpi.icon" /></el-icon>
            </div>
          </div>
          <div class="kpi-value" :title="kpi.value">{{ kpi.value }}</div>
          <div class="kpi-hint">{{ kpi.hint || '\u00A0' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 计数型 KPI：3 张次指标,统一弱化处理(灰色徽标),整行均分 24 列(每张 span=8) -->
    <el-row v-if="canDashboard" :gutter="16" class="mt">
      <el-col v-for="kpi in countKpis" :key="kpi.label" :span="8">
        <el-card class="kpi-card kpi-card--mini" shadow="never">
          <div class="kpi-head">
            <span class="kpi-label">{{ kpi.label }}</span>
            <div class="kpi-icon kpi-icon--mini">
              <el-icon><component :is="kpi.icon" /></el-icon>
            </div>
          </div>
          <div class="kpi-value" :title="kpi.value">{{ kpi.value }}</div>
          <div class="kpi-hint">{{ kpi.hint || '\u00A0' }}</div>
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

/* 给图表加 ResizeObserver，应对侧栏折叠/展开、KeepAlive 切回时容器宽度变化 */
const observers: ResizeObserver[] = []
function bindResize(el: HTMLElement, chart: echarts.ECharts) {
  const ro = new ResizeObserver(() => chart.resize())
  ro.observe(el)
  observers.push(ro)
}

/* 金额型 4 个，做强视觉处理（彩色徽标 + 大字号），是看板的视觉锚点 */
const moneyKpis = computed(() => {
  const d = dashboard.value
  if (!d) return []
  return [
    {
      label: '合同总额',
      value: formatCurrency(d.contractAmount),
      icon: 'Document',
      tone: 'primary',
      hint: ''
    },
    {
      label: '已回款',
      value: formatCurrency(d.paidAmount),
      icon: 'Select',
      tone: 'success',
      hint: '本月 ' + formatCurrency(d.paidThisMonth)
    },
    {
      label: '待回款',
      value: formatCurrency(d.unpaidAmount),
      icon: 'Clock',
      tone: 'warning',
      hint: ''
    },
    {
      label: '逾期金额',
      value: formatCurrency(d.overdueAmount),
      icon: 'Warning',
      tone: 'danger',
      hint: '需重点关注'
    }
  ]
})

/* 计数型 3 个，做弱化处理（灰色徽标 + 偏小字号） */
const countKpis = computed(() => {
  const d = dashboard.value
  if (!d) return []
  return [
    { label: '客户数',      value: formatNumber(d.customerCount),       icon: 'UserFilled', hint: '' },
    { label: '合同数',      value: formatNumber(d.contractCount),       icon: 'Tickets',    hint: '' },
    { label: '30 天内到期', value: formatNumber(d.contractDueIn30Days), icon: 'Calendar',   hint: '合同条数' }
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

/* 把 ¥124,100 这种金额压缩成 12.4w / 1.2亿 这种短形式，
 * 否则趋势图 Y 轴 6 个 tick 全是 0,000 0,000 看起来像 bug。 */
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
  if (!trendChart.value) {
    trendChart.value = markRaw(echarts.init(trendRef.value))
    bindResize(trendRef.value, trendChart.value)
  }
  trendChart.value.setOption({
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
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: false,
        lineStyle: { width: 2, color: '#1677ff' },
        /* 用配置对象代替 echarts.graphic.LinearGradient，避免按需引入下 graphic 不存在 */
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(22, 119, 255, 0.25)' },
              { offset: 1, color: 'rgba(22, 119, 255, 0.02)' }
            ]
          }
        },
        data: trend.value.map((p) => p.contractAmount)
      },
      {
        name: '回款金额',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: false,
        lineStyle: { width: 2, color: '#52c41a' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(82, 196, 26, 0.25)' },
              { offset: 1, color: 'rgba(82, 196, 26, 0.02)' }
            ]
          }
        },
        data: trend.value.map((p) => p.paidAmount)
      }
    ]
  })
}

function renderAging() {
  if (!agingRef.value) return
  if (!agingChart.value) {
    agingChart.value = markRaw(echarts.init(agingRef.value))
    bindResize(agingRef.value, agingChart.value)
  }
  /* 与 AgingView 保持一致的色板：未到期蓝/0-30 绿/31-60 黄/61-90 橙/90+ 红 */
  const PIE_COLORS = ['#1677ff', '#52c41a', '#faad14', '#fa8c16', '#f5222d']
  const total = aging.value.reduce((s, b) => s + Number(b.amount || 0), 0)
  agingChart.value.setOption({
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
        style: { text: formatCurrency(total), fill: '#1f2329', fontSize: 16, fontWeight: 600 }
      }
    ],
    series: [
      {
        type: 'pie',
        radius: ['52%', '72%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: true,
        /* 内嵌标签：占比 < 3% 不显示，避免 0% 桶把外引线撑成乱码 */
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

function onResize() {
  trendChart.value?.resize()
  agingChart.value?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  observers.forEach((ro) => ro.disconnect())
  observers.length = 0
  window.removeEventListener('resize', onResize)
  trendChart.value?.dispose()
  agingChart.value?.dispose()
})
</script>

<style scoped lang="scss">
/* KPI 卡片：通过 :deep 控制 el-card 的内边距，
 * 强制等高 + 三段式（label/value/hint）布局，
 * hint 即使没有内容也用 &nbsp; 占位，避免卡片高度跳动。 */
.kpi-card {
  position: relative;
  overflow: hidden;
  transition: transform 0.15s, box-shadow 0.15s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  }

  /* 左侧 4px 状态条（与 tone 对应），加强金额卡的视觉权重 */
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: transparent;
  }
  &.tone-primary::before { background: #1677ff; }
  &.tone-success::before { background: #52c41a; }
  &.tone-warning::before { background: #faad14; }
  &.tone-danger::before  { background: #f5222d; }

  :deep(.el-card__body) {
    padding: 16px 16px;
    height: 100%;
    display: flex;
    flex-direction: column;
  }

  .kpi-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  .kpi-label {
    color: #606266;
    font-size: 13px;
    font-weight: 500;
  }

  .kpi-icon {
    width: 28px;
    height: 28px;
    border-radius: 7px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    color: #fff;
    flex-shrink: 0;
  }
  &.tone-primary .kpi-icon { background: linear-gradient(135deg, #4096ff, #1677ff); }
  &.tone-success .kpi-icon { background: linear-gradient(135deg, #73d13d, #389e0d); }
  &.tone-warning .kpi-icon { background: linear-gradient(135deg, #ffc53d, #d48806); }
  &.tone-danger  .kpi-icon { background: linear-gradient(135deg, #ff7875, #cf1322); }

  .kpi-icon--mini {
    width: 24px;
    height: 24px;
    font-size: 12px;
    background: linear-gradient(135deg, #c0c4cc, #909399);
  }

  .kpi-value {
    font-size: 18px;
    font-weight: 700;
    color: #1f2329;
    line-height: 1.3;
    letter-spacing: -0.3px;
    /* 金额单行显示；极长金额（>千万）由 ellipsis 优雅截断，hover 由 title 提示完整值。
     * tabular-nums 让数字等宽，避免 1 与 8 宽度差导致整列对不齐。 */
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    font-variant-numeric: tabular-nums;
  }

  .kpi-hint {
    margin-top: 6px;
    color: #909399;
    font-size: 12px;
    /* 即便没内容也保留一行高，确保所有卡片等高 */
    min-height: 18px;
  }

  /* 计数型卡片（次指标）：数字字号小一点，整体更克制 */
  &.kpi-card--mini {
    .kpi-value {
      font-size: 18px;
      color: #303133;
      font-weight: 600;
    }
  }
}

.mt {
  margin-top: 12px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
