<template>
  <div class="crms-page">
    <el-card v-loading="loading" class="crms-card" shadow="never">
      <template v-if="contract">
        <div class="header">
          <div>
            <h2 style="margin: 0">{{ contract.name }}</h2>
            <span class="text-muted">{{ contract.code }}</span>
            <el-tag size="small" type="info" style="margin-left: 8px">
              {{ ContractType[contract.type] || contract.type }}
            </el-tag>
            <el-tag
              size="small"
              style="margin-left: 4px"
              :type="ContractStatus[contract.status]?.type || 'info'"
            >
              {{ ContractStatus[contract.status]?.label || contract.status }}
            </el-tag>
          </div>
          <div class="actions">
            <el-button @click="$router.back()">返回</el-button>
            <el-button v-perm="'contract:update'" type="primary" @click="onEdit">编辑</el-button>

            <el-dropdown
              v-if="transitionTargets.length > 0"
              trigger="click"
              @command="onTransition"
            >
              <el-button v-perm="'contract:update'" type="warning">
                状态流转 <el-icon class="ml-1"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="t in transitionTargets" :key="t" :command="t">
                    {{ ContractStatus[t]?.label || t }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <el-popconfirm
              v-if="canDelete"
              :title="`确认删除合同 ${contract.name}？`"
              @confirm="onDelete"
            >
              <template #reference>
                <el-button v-perm="'contract:delete'" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>

        <el-divider />

        <el-row :gutter="16" class="stat-row">
          <el-col :span="6">
            <el-statistic title="合同金额" :value="contract.amount">
              <template #suffix>元</template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <div class="el-statistic">
              <div class="el-statistic__head">签订日期</div>
              <div class="el-statistic__content">
                <span class="el-statistic__number">{{ contract.signedAt || '-' }}</span>
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="el-statistic">
              <div class="el-statistic__head">履约结束</div>
              <div class="el-statistic__content">
                <span class="el-statistic__number">{{ contract.performEndAt || '-' }}</span>
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <el-statistic title="剩余天数" :value="daysLeft">
              <template #suffix>天</template>
            </el-statistic>
          </el-col>
        </el-row>

        <el-descriptions :column="2" border size="default" class="mt-2">
          <el-descriptions-item label="客户">
            <el-link type="primary" @click="$router.push(`/customers/${contract.customerId}`)">
              #{{ contract.customerId }}
            </el-link>
          </el-descriptions-item>
          <el-descriptions-item label="负责人">{{ contract.ownerId }}</el-descriptions-item>
          <el-descriptions-item label="履约开始">{{ contract.performStartAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="履约结束">{{ contract.performEndAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提醒提前">{{ contract.remindDays || '-' }} 天</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ contract.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ contract.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="tab" class="mt-2">
          <el-tab-pane label="回款计划" name="plans">
            <ContractPaymentPlanTab :contract-id="id" />
          </el-tab-pane>
          <el-tab-pane label="附件" name="attachments">
            <ContractAttachmentTab :contract-id="id" />
          </el-tab-pane>
          <el-tab-pane label="备注" name="notes">
            <ContractNoteTab :contract-id="id" />
          </el-tab-pane>
          <el-tab-pane label="时间线" name="timeline">
            <ContractTimelineTab :contract-id="id" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-card>

    <ContractFormDrawer
      v-model:visible="formVisible"
      :record="contract"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import {
  contractApi,
  ContractStatus,
  ContractTransitions,
  ContractType,
  type ContractVO
} from '@/api/contract'
import ContractFormDrawer from './components/ContractFormDrawer.vue'
import ContractAttachmentTab from './components/ContractAttachmentTab.vue'
import ContractNoteTab from './components/ContractNoteTab.vue'
import ContractTimelineTab from './components/ContractTimelineTab.vue'
import ContractPaymentPlanTab from './components/ContractPaymentPlanTab.vue'

const route = useRoute()
const router = useRouter()
// Vue Router 对同名路由（/contracts/A → /contracts/B）默认复用组件实例，
// 这里用 computed 让 id 跟随路由变化，并通过 watch 触发重新加载。
// 使用 string 形态：Snowflake ID 超出 JS 安全整数范围，禁止 Number() 强转。
// 注意：router.back() 卸载组件时 route.params.id 会是 undefined，
//       直接 String(undefined) 会得到字面量 "undefined" 触发对 /contracts/undefined 的请求；
//       这里只接受纯数字字符串（Snowflake），否则返回空，让 watch 与 loadData 短路。
const id = computed(() => {
  const raw = route.params.id
  return typeof raw === 'string' && /^\d+$/.test(raw) ? raw : ''
})

const contract = ref<ContractVO | null>(null)
const loading = ref(false)
const tab = ref('plans')
const formVisible = ref(false)

const transitionTargets = computed(() =>
  contract.value ? ContractTransitions[contract.value.status] || [] : []
)

const canDelete = computed(() => {
  const s = contract.value?.status
  return s && ['DRAFT', 'TERMINATED', 'EXPIRED'].includes(s)
})

const daysLeft = computed(() => {
  if (!contract.value?.performEndAt) return 0
  const end = new Date(contract.value.performEndAt).getTime()
  const now = Date.now()
  return Math.max(0, Math.floor((end - now) / 86400000))
})

async function loadData() {
  if (!id.value) return
  loading.value = true
  try {
    contract.value = await contractApi.detail(id.value)
  } finally {
    loading.value = false
  }
}

function onEdit() {
  formVisible.value = true
}

async function onTransition(target: string) {
  try {
    const { value } = await ElMessageBox.prompt(
      `确认将合同状态流转到 ${ContractStatus[target]?.label}？`,
      '状态流转',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputPlaceholder: '原因（可选）',
        inputValue: ''
      }
    )
    await contractApi.transition(id.value, target, value)
    ElMessage.success('已流转')
    loadData()
  } catch {
    /* user cancel */
  }
}

async function onDelete() {
  await contractApi.remove(id.value)
  ElMessage.success('已删除')
  router.back()
}

function onSaved() {
  formVisible.value = false
  loadData()
}

watch(id, () => loadData(), { immediate: true })
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.actions {
  display: flex;
  gap: 8px;
}
.stat-row {
  margin: 8px 0;
}
.mt-2 {
  margin-top: 16px;
}
.ml-1 {
  margin-left: 4px;
}
.text-muted {
  color: #909399;
}
</style>
