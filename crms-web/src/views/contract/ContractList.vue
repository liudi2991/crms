<template>
  <div class="crms-page">
    <div class="crms-filter">
      <el-input
        v-model="query.keyword"
        placeholder="搜索合同名称 / 编号"
        clearable
        :prefix-icon="Search"
        style="width: 260px"
        @keyup.enter="onSearch"
      />
      <el-select v-model="query.type" placeholder="类型" clearable style="width: 120px" @change="onSearch">
        <el-option v-for="(label, key) in ContractType" :key="key" :label="label" :value="key" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="onSearch">
        <el-option
          v-for="(meta, key) in ContractStatus"
          :key="key"
          :label="meta.label"
          :value="key"
        />
      </el-select>
      <el-date-picker
        v-model="signRange"
        type="daterange"
        range-separator="~"
        start-placeholder="签订起"
        end-placeholder="签订止"
        value-format="YYYY-MM-DD"
        style="width: 260px"
        @change="onSearch"
      />
      <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
      <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
    </div>

    <el-card class="crms-card" shadow="never">
      <div class="toolbar">
        <el-button v-perm="'contract:create'" type="primary" :icon="Plus" @click="onCreate">
          新建合同
        </el-button>
        <el-button :icon="Refresh" circle @click="loadData" />
      </div>

      <el-table v-loading="loading" :data="rows" stripe border>
        <el-table-column prop="code" label="编号" width="160" />
        <el-table-column label="合同名称" min-width="220">
          <template #default="{ row }">
            <el-link type="primary" @click="goDetail(row.id)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">{{ ContractType[row.type] || row.type }}</template>
        </el-table-column>
        <el-table-column label="金额" width="160" align="right">
          <template #default="{ row }">{{ Number(row.amount).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="ContractStatus[row.status]?.type || 'info'" size="small">
              {{ ContractStatus[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="签订日期" width="120">
          <template #default="{ row }">{{ formatDate(row.signedAt) }}</template>
        </el-table-column>
        <el-table-column label="履约结束" width="120">
          <template #default="{ row }">{{ formatDate(row.performEndAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="180" align="center">
          <template #default="{ row }">
            <el-button v-perm="'contract:update'" link size="small" @click="onEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'EFFECTIVE'"
              v-perm="'contract:terminate'"
              link
              size="small"
              type="warning"
              @click="onTerminate(row)"
            >
              终止
            </el-button>
            <el-popconfirm
              v-if="canDelete(row)"
              :title="`确认删除合同 ${row.name}？`"
              @confirm="onDelete(row)"
            >
              <template #reference>
                <el-button v-perm="'contract:delete'" link size="small">
                  <span class="text-danger">删除</span>
                </el-button>
              </template>
            </el-popconfirm>
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

    <ContractFormDrawer
      v-model:visible="formVisible"
      :record="formRecord"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import {
  contractApi,
  ContractType,
  ContractStatus,
  type ContractQuery,
  type ContractVO
} from '@/api/contract'
import { formatDate } from '@/utils/format'
import ContractFormDrawer from './components/ContractFormDrawer.vue'

const router = useRouter()

const query = reactive<ContractQuery>({ page: 1, size: 20 })
const signRange = ref<[string, string] | null>(null)

watch(signRange, (v) => {
  query.signedFrom = v?.[0]
  query.signedTo = v?.[1]
})

const rows = ref<ContractVO[]>([])
const total = ref(0)
const loading = ref(false)

const formVisible = ref(false)
const formRecord = ref<ContractVO | null>(null)

async function loadData() {
  loading.value = true
  try {
    const res = await contractApi.list(query)
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
  signRange.value = null
  query.page = 1
  loadData()
}

function onCreate() {
  formRecord.value = null
  formVisible.value = true
}

function onEdit(row: ContractVO) {
  formRecord.value = row
  formVisible.value = true
}

function canDelete(row: ContractVO) {
  return ['DRAFT', 'TERMINATED', 'EXPIRED'].includes(row.status)
}

async function onDelete(row: ContractVO) {
  await contractApi.remove(row.id)
  ElMessage.success('已删除')
  loadData()
}

async function onTerminate(row: ContractVO) {
  try {
    const { value } = await ElMessageBox.prompt(
      `终止合同 ${row.name}（请填写原因）`,
      '终止合同',
      {
        confirmButtonText: '终止',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--warning',
        inputPlaceholder: '终止原因（建议）',
        inputValue: ''
      }
    )
    await contractApi.terminate(row.id, value)
    ElMessage.success('已终止')
    loadData()
  } catch {
    /* user cancel */
  }
}

function goDetail(id: string) {
  router.push(`/contracts/${id}`)
}

function onSaved() {
  formVisible.value = false
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.search-bar {
  margin-bottom: 4px;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin: 8px 0 12px;
}
.mt-2 {
  margin-top: 12px;
}
</style>
