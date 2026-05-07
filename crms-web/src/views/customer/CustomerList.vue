<template>
  <div class="crms-page">
    <el-card class="crms-card" shadow="never">
      <CustomerSearchBar v-model:query="query" @search="onSearch" @reset="onReset" />

      <div class="toolbar">
        <div class="left">
          <el-button v-perm="'customer:create'" type="primary" @click="onCreate">
            <el-icon><Plus /></el-icon>新建客户
          </el-button>
          <el-button
            v-perm="'customer:merge'"
            :disabled="selected.length < 2"
            @click="onMerge"
          >
            <el-icon><Connection /></el-icon>合并 ({{ selected.length }})
          </el-button>
        </div>
        <div class="right">
          <el-button :icon="Refresh" circle @click="loadData" />
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        stripe
        border
        @selection-change="(s: any[]) => (selected = s)"
      >
        <el-table-column type="selection" width="44" />
        <el-table-column prop="code" label="编号" width="160" />
        <el-table-column label="客户名称" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" @click="goDetail(row.id)">{{ row.name }}</el-link>
            <el-tag v-if="row.shortName" size="small" type="info" class="ml-1">
              {{ row.shortName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            {{ ContractCustomerType[row.type as keyof typeof ContractCustomerType] || row.type }}
          </template>
        </el-table-column>
        <el-table-column prop="level" label="等级" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ownerName" label="负责人" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" fixed="right" width="220" align="center">
          <template #default="{ row }">
            <el-button v-perm="'customer:update'" link size="small" @click="onEdit(row)">
              编辑
            </el-button>
            <el-button
              v-perm="'customer:disable'"
              link
              size="small"
              :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
              @click="onToggle(row)"
            >
              {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
            </el-button>
            <el-popconfirm
              :title="`确认删除客户 ${row.name}？`"
              @confirm="onDelete(row)"
            >
              <template #reference>
                <el-button v-perm="'customer:delete'" link size="small" type="danger">
                  删除
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

    <CustomerFormDrawer
      v-model:visible="formVisible"
      :record="formRecord"
      @saved="onSaved"
    />

    <CustomerMergeDialog
      v-model="mergeVisible"
      :candidates="selected"
      @saved="onMergeDone"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Connection, Plus, Refresh } from '@element-plus/icons-vue'
import { customerApi, type CustomerQuery, type CustomerVO } from '@/api/customer'
import { CustomerType as ContractCustomerType } from '@/utils/enum'
import CustomerSearchBar from './components/CustomerSearchBar.vue'
import CustomerFormDrawer from './components/CustomerFormDrawer.vue'
import CustomerMergeDialog from './components/CustomerMergeDialog.vue'

const router = useRouter()

const query = reactive<CustomerQuery>({ page: 1, size: 20 })
const rows = ref<CustomerVO[]>([])
const total = ref(0)
const loading = ref(false)
const selected = ref<CustomerVO[]>([])

const formVisible = ref(false)
const formRecord = ref<CustomerVO | null>(null)
const mergeVisible = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await customerApi.list(query)
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
  query.page = 1
  loadData()
}

function onCreate() {
  formRecord.value = null
  formVisible.value = true
}

function onEdit(row: CustomerVO) {
  formRecord.value = row
  formVisible.value = true
}

async function onDelete(row: CustomerVO) {
  await customerApi.remove(row.id)
  ElMessage.success('已删除')
  loadData()
}

async function onToggle(row: CustomerVO) {
  if (row.status === 'ACTIVE') {
    await customerApi.disable(row.id)
    ElMessage.success('已停用')
  } else {
    await customerApi.enable(row.id)
    ElMessage.success('已启用')
  }
  loadData()
}

function onMerge() {
  if (selected.value.length < 2) {
    ElMessage.warning('请至少选择 2 个客户')
    return
  }
  mergeVisible.value = true
}

function onMergeDone() {
  selected.value = []
  loadData()
}

function goDetail(id: string) {
  router.push(`/customers/${id}`)
}

function onSaved() {
  formVisible.value = false
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.ml-1 {
  margin-left: 6px;
}
.mt-2 {
  margin-top: 12px;
}
</style>
