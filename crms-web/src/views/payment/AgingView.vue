<template>
  <div class="crms-page">
    <el-card class="crms-card" shadow="never">
      <div class="header">
        <h3 style="margin: 0">应收账龄分析</h3>
        <div>
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

      <el-row :gutter="12" class="stat-row">
        <el-col v-for="b in buckets" :key="b.bucket" :span="24 / Math.max(buckets.length, 1)">
          <el-card
            :body-style="{ cursor: 'pointer', padding: '16px' }"
            class="bucket-card"
            :class="{ active: selected === b.bucket }"
            shadow="hover"
            @click="onSelect(b.bucket)"
          >
            <div class="text-muted">{{ AgingBucketLabel[b.bucket] }}</div>
            <div class="amt">{{ Number(b.amount).toLocaleString() }}</div>
            <div class="text-muted small">{{ b.count }} 条</div>
          </el-card>
        </el-col>
      </el-row>

      <div ref="chartRef" class="chart" />

      <div v-if="selected" class="mt-2">
        <h4>{{ AgingBucketLabel[selected] }}（明细）</h4>
        <el-table v-loading="drillLoading" :data="drillRows" border>
          <el-table-column prop="contractCode" label="合同编号" width="160" />
          <el-table-column prop="contractName" label="合同名称" min-width="220" show-overflow-tooltip />
          <el-table-column prop="periodNo" label="期" width="60" align="center" />
          <el-table-column prop="planDate" label="计划日期" width="120" />
          <el-table-column label="待核销" width="160" align="right">
            <template #default="{ row }">{{ Number(row.unsettledAmount).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="逾期天数" width="120" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.overdueDays > 0" type="danger" size="small">{{ row.overdueDays }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button link size="small" @click="$router.push(`/contracts/${row.contractId}`)">查看合同</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { agingApi, AgingBucketLabel, type AgingBucketVO, type AgingDrillVO } from '@/api/payment'

const today = ref(new Date().toISOString().slice(0, 10))
const buckets = ref<AgingBucketVO[]>([])
const selected = ref<string>('')
const drillRows = ref<AgingDrillVO[]>([])
const drillLoading = ref(false)

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

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
    tooltip: { trigger: 'item', formatter: '{b}<br/>金额：{c} ({d}%)' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        data: buckets.value.map((b) => ({
          name: AgingBucketLabel[b.bucket],
          value: Number(b.amount)
        })),
        label: { formatter: '{b}\n{d}%' }
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

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.stat-row {
  margin-bottom: 12px;
}
.bucket-card {
  text-align: center;
  transition: all .2s;
}
.bucket-card.active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, .2);
}
.amt {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 6px 0 4px;
}
.text-muted {
  color: #909399;
}
.small {
  font-size: 12px;
}
.chart {
  width: 100%;
  height: 320px;
  margin: 8px 0 16px;
}
.mt-2 {
  margin-top: 12px;
}
</style>
