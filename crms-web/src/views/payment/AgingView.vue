<template>
  <div class="crms-page">
    <!-- 顶部工具栏：基准日期 + 刷新，独立成 toolbar 不与卡片混在一起 -->
    <div class="aging-toolbar">
      <div>
        <h3 class="aging-title">应收账龄分析</h3>
        <span class="aging-subtitle">应收款按逾期天数分桶，点击桶卡片查看明细</span>
      </div>
      <div class="aging-toolbar__right">
        <el-date-picker
          v-model="today"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="基准日期"
          style="width: 160px"
          @change="loadBuckets"
        />
        <el-button :icon="Refresh" circle @click="loadBuckets" />
      </div>
    </div>

    <!-- 5 张桶卡片：按逾期严重程度使用渐进色（未到期蓝 → 0-30 绿 → 31-60 黄 → 61-90 橙 → 90+ 红），
         点击切换 selected 高亮 + 加载明细。
         用 css grid 而非 el-row/col，因为 5 列在 24 栅格里 24/5=4.8 不整除会换行 -->
    <div class="bucket-row">
      <el-card
        v-for="b in buckets"
        :key="b.bucket"
        class="bucket-card"
        :class="[`tone-${bucketTone(b.bucket)}`, { active: selected === b.bucket }]"
        shadow="never"
        @click="onSelect(b.bucket)"
      >
        <div class="bucket-head">
          <span class="bucket-label">{{ AgingBucketLabel[b.bucket] }}</span>
          <div class="bucket-icon">
            <el-icon><component :is="bucketIcon(b.bucket)" /></el-icon>
          </div>
        </div>
        <div class="bucket-amt" :title="formatCurrency(b.amount)">
          {{ formatCurrency(b.amount) }}
        </div>
        <div class="bucket-count">{{ b.count }} 条</div>
      </el-card>
    </div>

    <!-- 环图：标签内嵌、中心显示总额（替代被外引线撑爆的旧方案） -->
    <el-card class="crms-card chart-card" shadow="never">
      <template #header>
        <div class="card-head">
          <span>账龄结构</span>
          <span class="text-muted small">合计 {{ formatCurrency(totalAmount) }} · {{ totalCount }} 条</span>
        </div>
      </template>
      <div ref="chartRef" class="chart" />
    </el-card>

    <!-- 明细表：只有点击桶卡片后才显示 -->
    <el-card v-if="selected" class="crms-card mt" shadow="never">
      <template #header>
        <div class="card-head">
          <span>{{ AgingBucketLabel[selected] }}（明细）</span>
          <span class="text-muted small">共 {{ drillRows.length }} 条</span>
        </div>
      </template>
      <el-table v-loading="drillLoading" :data="drillRows" border>
        <el-table-column prop="contractCode" label="合同编号" width="160" />
        <el-table-column prop="contractName" label="合同名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="periodNo" label="期" width="60" align="center" />
        <el-table-column label="计划日期" width="120">
          <template #default="{ row }">{{ formatDate(row.planDate) }}</template>
        </el-table-column>
        <el-table-column label="待核销" width="160" align="right">
          <template #default="{ row }">{{ formatCurrency(row.unsettledAmount) }}</template>
        </el-table-column>
        <el-table-column label="逾期天数" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.overdueDays > 0" type="danger" size="small">{{ row.overdueDays }} 天</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button link size="small" @click="$router.push(`/contracts/${row.contractId}`)">查看合同</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { agingApi, AgingBucketLabel, type AgingBucketVO, type AgingDrillVO } from '@/api/payment'
import { formatCurrency, formatDate } from '@/utils/format'

const today = ref(new Date().toISOString().slice(0, 10))
const buckets = ref<AgingBucketVO[]>([])
const selected = ref<string>('')
const drillRows = ref<AgingDrillVO[]>([])
const drillLoading = ref(false)

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

/* 桶严重程度 -> tone 色板（与 KPI 卡片同一套色规则）。
 * 后端 bucket 取值（见 api/payment.ts AgingBucketLabel）：
 * UNDUE / 0-30 / 31-60 / 61-90 / 90+ */
const TONE_MAP: Record<string, string> = {
  UNDUE:   'primary',
  '0-30':  'success',
  '31-60': 'warning',
  '61-90': 'orange',
  '90+':   'danger'
}
const ICON_MAP: Record<string, string> = {
  UNDUE:   'CircleCheck',
  '0-30':  'Promotion',
  '31-60': 'Clock',
  '61-90': 'Warning',
  '90+':   'CircleClose'
}
function bucketTone(b: string) { return TONE_MAP[b] ?? 'info' }
function bucketIcon(b: string) { return ICON_MAP[b] ?? 'PieChart' }

/* 与 echarts 饼图保持一致的色板，顺序必须与上面 TONE_MAP 严格对齐。 */
const PIE_COLORS = ['#1677ff', '#52c41a', '#faad14', '#fa8c16', '#f5222d']

const totalAmount = computed(() =>
  buckets.value.reduce((s, b) => s + Number(b.amount || 0), 0)
)
const totalCount = computed(() =>
  buckets.value.reduce((s, b) => s + Number(b.count || 0), 0)
)

async function loadBuckets() {
  buckets.value = await agingApi.buckets(today.value)
  renderChart()
  if (selected.value) loadDrill()
}

async function loadDrill() {
  if (!selected.value) return
  drillLoading.value = true
  try {
    drillRows.value = await agingApi.drill(selected.value, today.value, 1, 100)
  } finally {
    drillLoading.value = false
  }
}

function onSelect(bucket: string) {
  selected.value = bucket
  loadDrill()
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    color: PIE_COLORS,
    tooltip: {
      trigger: 'item',
      formatter: (p: { name: string; value: number; percent: number }) =>
        `${p.name}<br/>金额：${formatCurrency(p.value)} (${p.percent}%)`
    },
    legend: { bottom: 0, icon: 'circle' },
    /* 中心展示总额：echarts graphic 比 series.label 更可控 */
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '38%',
        style: {
          text: '应收合计',
          fill: '#909399',
          fontSize: 12
        }
      },
      {
        type: 'text',
        left: 'center',
        top: '46%',
        style: {
          text: formatCurrency(totalAmount.value),
          fill: '#1f2329',
          fontSize: 18,
          fontWeight: 600
        }
      }
    ],
    series: [
      {
        type: 'pie',
        radius: ['52%', '72%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: true,
        /* 关键改动：取消外引线，标签直接画在饼块内部，0% 的桶不显示标签 */
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
        data: buckets.value.map((b) => ({
          name: AgingBucketLabel[b.bucket],
          value: Number(b.amount)
        }))
      }
    ]
  })
}

function onResize() {
  chart?.resize()
}

watch(today, loadBuckets)

onMounted(() => {
  loadBuckets()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  chart?.dispose()
  chart = null
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped lang="scss">
.aging-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 14px 18px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .aging-title {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #1f2329;
  }
  .aging-subtitle {
    margin-left: 12px;
    color: #909399;
    font-size: 12px;
  }
  &__right {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.bucket-row {
  display: grid;
  /* 自适应：宽屏 5 列；窄屏退化为 minmax(160px, 1fr) 自然换行 */
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;

  @media (max-width: 1100px) {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  @media (max-width: 700px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

/* 桶卡片：与 dashboard KPI 卡片同一套设计语言（左侧 tone 色条 + 右上彩色图标徽标）。
 * 注意：el-card 的根元素自带 .el-card 类与 padding，scoped 选择器要 :deep 才能穿透到 .el-card__body。
 * 左侧色条用 border-left 而非 ::before，更直观、不依赖 position context。 */
.bucket-card {
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  border-left: 3px solid #dcdfe6;

  &.tone-primary { border-left-color: #1677ff; }
  &.tone-success { border-left-color: #52c41a; }
  &.tone-warning { border-left-color: #faad14; }
  &.tone-orange  { border-left-color: #fa8c16; }
  &.tone-danger  { border-left-color: #f5222d; }

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  }
  &.active {
    box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.3);
    &.tone-success { box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.3); }
    &.tone-warning { box-shadow: 0 0 0 2px rgba(250, 173, 20, 0.3); }
    &.tone-orange  { box-shadow: 0 0 0 2px rgba(250, 140, 22, 0.3); }
    &.tone-danger  { box-shadow: 0 0 0 2px rgba(245, 34, 45, 0.3); }
  }

  :deep(.el-card__body) {
    padding: 16px;
  }

  .bucket-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }
  .bucket-label {
    color: #606266;
    font-size: 13px;
    font-weight: 500;
  }
  .bucket-icon {
    width: 28px;
    height: 28px;
    border-radius: 7px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    color: #fff;
    flex-shrink: 0;
    background: #909399; /* fallback */
  }
  &.tone-primary .bucket-icon { background: linear-gradient(135deg, #4096ff, #1677ff); }
  &.tone-success .bucket-icon { background: linear-gradient(135deg, #73d13d, #389e0d); }
  &.tone-warning .bucket-icon { background: linear-gradient(135deg, #ffc53d, #d48806); }
  &.tone-orange  .bucket-icon { background: linear-gradient(135deg, #ffa940, #d46b08); }
  &.tone-danger  .bucket-icon { background: linear-gradient(135deg, #ff7875, #cf1322); }

  .bucket-amt {
    font-size: 20px;
    font-weight: 700;
    color: #1f2329;
    line-height: 1.25;
    letter-spacing: -0.3px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    font-variant-numeric: tabular-nums;
  }
  .bucket-count {
    margin-top: 6px;
    color: #909399;
    font-size: 12px;
    min-height: 18px;
  }
}

.chart-card {
  .chart {
    width: 100%;
    height: 360px;
  }
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.text-muted {
  color: #909399;
}
.small {
  font-size: 12px;
}
.mt {
  margin-top: 16px;
}
</style>
