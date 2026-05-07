<template>
  <div class="crms-page">
    <el-card class="crms-card" shadow="never">
      <el-form inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="操作人 / 动作 / URI"
            clearable
            style="width: 220px"
            @keyup.enter="onSearch"
          />
        </el-form-item>
        <el-form-item label="模块">
          <el-select
            v-model="query.module"
            placeholder="全部"
            clearable
            style="width: 120px"
            @change="onSearch"
          >
            <el-option v-for="m in moduleOptions" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select
            v-model="query.opType"
            placeholder="全部"
            clearable
            style="width: 140px"
            @change="onSearch"
          >
            <el-option label="新建" value="CREATE" />
            <el-option label="更新" value="UPDATE" />
            <el-option label="软删" value="DELETE" />
            <el-option label="硬删" value="HARD_DELETE" />
            <el-option label="登录" value="LOGIN" />
            <el-option label="导出" value="EXPORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select
            v-model="query.result"
            placeholder="全部"
            clearable
            style="width: 110px"
            @change="onSearch"
          >
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="~"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 360px"
            @change="onSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="operatorIp" label="IP" width="140" />
        <el-table-column prop="module" label="模块" width="80" />
        <el-table-column prop="action" label="动作" width="160" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="opTypeColor(row.opType)">{{ opTypeLabel(row.opType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" align="right" />
        <el-table-column label="详情" min-width="200">
          <template #default="{ row }">
            <el-button link size="small" @click="onView(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        background
        layout="total, sizes, prev, pager, next, jumper"
        class="mt-2"
        @current-change="loadData"
        @size-change="loadData"
      />
    </el-card>

    <el-drawer v-model="detailVisible" title="操作日志详情" size="600px">
      <pre class="detail">{{ detailText }}</pre>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import {
  operationLogApi,
  type OperationLogQuery,
  type OperationLogVO
} from '@/api/system'

const query = reactive<OperationLogQuery>({ page: 1, size: 20 })
const timeRange = ref<[string, string] | null>(null)
const rows = ref<OperationLogVO[]>([])
const total = ref(0)
const loading = ref(false)

const moduleOptions = ['客户', '合同', '回款', '系统', '认证']

const detailVisible = ref(false)
const detailText = ref('')

watch(timeRange, (v) => {
  query.fromTime = v?.[0]
  query.toTime = v?.[1]
})

async function loadData() {
  loading.value = true
  try {
    const res = await operationLogApi.list(query)
    rows.value = res.items
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  loadData()
}

function onReset() {
  Object.keys(query).forEach((k) => {
    if (k !== 'page' && k !== 'size') {
      // @ts-expect-error dynamic clear
      query[k] = undefined
    }
  })
  timeRange.value = null
  query.page = 1
  loadData()
}

function opTypeLabel(t: string) {
  const m: Record<string, string> = {
    CREATE: '新建',
    UPDATE: '更新',
    DELETE: '软删',
    HARD_DELETE: '硬删',
    LOGIN: '登录',
    EXPORT: '导出'
  }
  return m[t] || t
}

function opTypeColor(t: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (t === 'CREATE') return 'success'
  if (t === 'UPDATE') return 'primary'
  if (t === 'DELETE' || t === 'HARD_DELETE') return 'danger'
  if (t === 'EXPORT') return 'warning'
  return 'info'
}

function onView(row: OperationLogVO) {
  detailText.value = JSON.stringify(row, null, 2)
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.search-bar {
  margin-bottom: 8px;
}
.mt-2 {
  margin-top: 12px;
}
.detail {
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 10px;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  margin: 0;
}
</style>
