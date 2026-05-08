<template>
  <div class="crms-page">
    <el-card v-loading="loading" class="crms-card" shadow="never">
      <template v-if="customer">
        <div class="header">
          <div>
            <h2 style="margin: 0">{{ customer.name }}</h2>
            <span class="text-muted">{{ customer.code }}</span>
            <el-tag size="small" type="info" style="margin-left: 8px">
              {{ CustomerType[customer.type as keyof typeof CustomerType] || customer.type }}
            </el-tag>
            <el-tag size="small" style="margin-left: 4px">{{ customer.level }}</el-tag>
            <el-tag
              size="small"
              :type="ActiveStatus[customer.status]?.type || 'info'"
              style="margin-left: 4px"
            >
              {{ ActiveStatus[customer.status]?.label || statusLabel }}
            </el-tag>
          </div>
          <div>
            <el-button @click="$router.back()">返回</el-button>
            <el-button v-perm="'customer:update'" type="primary" @click="onEdit">编辑</el-button>
            <el-popconfirm
              v-if="customer.status !== 'MERGED'"
              :title="`确认删除客户 ${customer.name}？`"
              @confirm="onDelete"
            >
              <template #reference>
                <el-button v-perm="'customer:delete'" plain type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>

        <el-divider />

        <el-row :gutter="16" class="stat-row">
          <el-col :span="6">
            <el-statistic title="合同数量" :value="Number(agg?.totalContracts ?? 0)" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="合同总金额" :value="Number(agg?.totalContractAmount ?? 0)">
              <template #suffix>元</template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="联系人" :value="agg?.contacts.length ?? 0" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="变更记录" :value="agg?.recentChanges.length ?? 0" />
          </el-col>
        </el-row>

        <el-descriptions :column="2" border size="default" class="mt-2">
          <el-descriptions-item label="负责人">{{ customer.ownerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属部门">{{ customer.deptName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="行业">{{ customer.industry || '-' }}</el-descriptions-item>
          <el-descriptions-item label="统一信用代码">{{ customer.uscc || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(customer.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="最后更新">{{ formatDateTime(customer.updatedAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="tab" class="mt-2">
          <el-tab-pane label="合同" name="contracts">
            <EmptyHint v-if="!agg?.recentContracts.length" description="暂无合同" />
            <el-table v-else :data="agg.recentContracts" border>
              <el-table-column prop="code" label="合同编号" width="160" />
              <el-table-column label="合同名称" min-width="180">
                <template #default="{ row }">
                  <el-link type="primary" @click="$router.push(`/contracts/${row.id}`)">
                    {{ row.name }}
                  </el-link>
                </template>
              </el-table-column>
              <el-table-column prop="amount" label="金额" width="140" align="right">
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
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="联系人" name="contacts">
            <CustomerContactTab :customer-id="id" />
          </el-tab-pane>

          <el-tab-pane label="变更记录" name="changes">
            <CustomerChangeLogTab :customer-id="id" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-card>

    <CustomerFormDrawer
      v-model:visible="formVisible"
      :record="customer"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onDeactivated, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { customerApi, type CustomerAggregateVO, type CustomerVO } from '@/api/customer'
import { ContractStatus } from '@/api/contract'
import { ActiveStatus, CustomerType } from '@/utils/enum'
import { formatDate, formatDateTime } from '@/utils/format'
import EmptyHint from '@/components/EmptyHint.vue'
import CustomerFormDrawer from './components/CustomerFormDrawer.vue'
import CustomerContactTab from './components/CustomerContactTab.vue'
import CustomerChangeLogTab from './components/CustomerChangeLogTab.vue'

const route = useRoute()
const router = useRouter()
// Vue Router 对同名路由（/customers/A → /customers/B）默认复用组件实例，
// 这里用 computed 让 id 跟随路由变化，并通过 watch 触发重新加载。
// 使用 string 形态：Snowflake ID 超出 JS 安全整数范围，禁止 Number() 强转。
// 注意 1：router.back() 卸载组件时 route.params.id 会是 undefined，
//        直接 String(undefined) 会得到字面量 "undefined"，触发对 /customers/undefined
//        的请求；这里只接受纯数字字符串（Snowflake），否则返回空让 watch 与 loadData 短路。
// 注意 2：本组件外层有 <keep-alive>，跳到其他详情页（如 /contracts/:id）时本实例
//        不会卸载，但 route 是全局响应式 → 此处不加 route.name 守卫的话，watcher 会
//        拿到别人的 id 去查客户，触发 "客户不存在" toast。
const id = computed(() => {
  if (route.name !== 'CustomerDetail') return ''
  const raw = route.params.id
  return typeof raw === 'string' && /^\d+$/.test(raw) ? raw : ''
})

const agg = ref<CustomerAggregateVO | null>(null)
const customer = computed<CustomerVO | null>(() => agg.value?.customer || null)
const loading = ref(false)
const tab = ref('contracts')
const formVisible = ref(false)

const statusLabel = computed(() => {
  const s = customer.value?.status
  if (s === 'ACTIVE') return '启用'
  if (s === 'DISABLED') return '停用'
  if (s === 'MERGED') return '已合并'
  return s || '-'
})

async function loadData() {
  if (!id.value) return
  loading.value = true
  try {
    agg.value = await customerApi.aggregate(id.value)
  } finally {
    loading.value = false
  }
}

// keep-alive 缓存页面：仅在 active 时监听路由变化，否则会用别人的
// id 去查客户而触发 "客户不存在" toast。
let stopRouteWatch: (() => void) | null = null
onActivated(() => {
  loadData()
  stopRouteWatch = watch(id, () => loadData())
})
onDeactivated(() => {
  stopRouteWatch?.()
  stopRouteWatch = null
})

function onEdit() {
  formVisible.value = true
}

async function onDelete() {
  await customerApi.remove(id.value)
  ElMessage.success('已删除')
  router.back()
}

function onSaved() {
  formVisible.value = false
  loadData()
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.stat-row {
  margin: 8px 0;
}
.mt-2 {
  margin-top: 16px;
}
.text-muted {
  color: #909399;
}
</style>
