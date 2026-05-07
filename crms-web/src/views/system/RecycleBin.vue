<template>
  <div class="crms-page">
    <el-card class="crms-card" shadow="never">
      <el-tabs v-model="query.bizType" @tab-change="onSearch">
        <el-tab-pane label="客户" name="CUSTOMER" />
        <el-tab-pane label="合同" name="CONTRACT" />
        <el-tab-pane label="回款" name="PAYMENT_RECORD" />
      </el-tabs>

      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索关键字"
          clearable
          style="width: 240px"
          @keyup.enter="onSearch"
        />
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="Refresh" circle @click="loadData" />
      </div>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="code" label="编号" width="200" />
        <el-table-column prop="name" label="名称 / 摘要" min-width="240" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="删除时间" width="170" />
        <el-table-column label="操作" fixed="right" width="220" align="center">
          <template #default="{ row }">
            <el-popconfirm
              :title="`确认还原 ${row.name || row.code}？`"
              @confirm="onRestore(row)"
            >
              <template #reference>
                <el-button link size="small" type="success">还原</el-button>
              </template>
            </el-popconfirm>
            <el-button v-perm="hardDeletePerm" link size="small" type="danger" @click="onHardDelete(row)">
              彻底删除
            </el-button>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  recycleBinApi,
  type RecycleBinItemVO,
  type RecycleBinQuery,
  type RecycleBizType
} from '@/api/system'

const query = reactive<RecycleBinQuery>({ bizType: 'CUSTOMER', page: 1, size: 20 })
const rows = ref<RecycleBinItemVO[]>([])
const total = ref(0)
const loading = ref(false)

const hardDeletePerm = computed(() => {
  if (query.bizType === 'CUSTOMER') return 'customer:hard_delete'
  if (query.bizType === 'CONTRACT') return 'contract:hard_delete'
  return 'payment:hard_delete'
})

async function loadData() {
  loading.value = true
  try {
    const res = await recycleBinApi.list(query)
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

async function onRestore(row: RecycleBinItemVO) {
  await recycleBinApi.restore(query.bizType as RecycleBizType, row.id)
  ElMessage.success('已还原')
  loadData()
}

async function onHardDelete(row: RecycleBinItemVO) {
  try {
    const { value } = await ElMessageBox.prompt(
      `彻底删除 ${row.name || row.code} 后无法恢复，请填写原因（必填）`,
      '高危：硬删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        inputPlaceholder: '原因（最长 500 字）',
        inputValidator: (v) => (v && v.trim().length > 0 ? true : '原因必填')
      }
    )
    await recycleBinApi.hardDelete(query.bizType as RecycleBizType, row.id, value)
    ElMessage.success('已彻底删除')
    loadData()
  } catch {
    /* user cancel */
  }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin: 8px 0 12px;
}
.mt-2 {
  margin-top: 12px;
}
</style>
