<template>
  <div>
    <div class="toolbar">
      <el-button v-perm="'payment:plan'" type="primary" :icon="MagicStick" @click="genDialog = true">
        生成计划
      </el-button>
      <el-button v-perm="'payment:plan'" :icon="Plus" @click="openCreate">
        新增一期
      </el-button>
      <el-button v-perm="'payment:record'" :icon="Money" @click="recordDrawer = true">
        登记回款
      </el-button>
      <el-button :icon="Refresh" circle @click="loadData" />
    </div>

    <EmptyHint v-if="!loading && !rows.length" description="暂无回款计划，可点击右上角『生成计划』" />
    <el-table v-else v-loading="loading" :data="rows" stripe border>
      <el-table-column prop="periodNo" label="期" width="60" align="center" />
      <el-table-column label="计划日期" width="120">
        <template #default="{ row }">{{ formatDate(row.planDate) }}</template>
      </el-table-column>
      <el-table-column label="计划金额" width="140" align="right">
        <template #default="{ row }">{{ Number(row.planAmount).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column label="已核销" width="140" align="right">
        <template #default="{ row }">{{ Number(row.settledAmount).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column label="待核销" width="140" align="right">
        <template #default="{ row }">{{ Number(row.unsettledAmount).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="PaymentPlanStatus[row.status]?.type">
            {{ PaymentPlanStatus[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="逾期" width="120" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.overdue" type="danger" size="small">逾期 {{ row.overdueDays }} 天</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-perm="'payment:plan'" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm
            v-if="canDelete(row)"
            :title="`确认删除第 ${row.periodNo} 期？`"
            @confirm="onDelete(row)"
          >
            <template #reference>
              <el-button v-perm="'payment:plan'" link size="small">
                <span class="text-danger">删除</span>
              </el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="genDialog" title="批量生成回款计划" width="500px">
      <el-form :model="genForm" label-width="100px">
        <el-form-item label="首期日期">
          <el-date-picker v-model="genForm.firstPlanDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="期数">
          <el-input-number v-model="genForm.periods" :min="1" :max="120" />
        </el-form-item>
        <el-form-item label="频率">
          <el-radio-group v-model="genForm.frequency">
            <el-radio value="MONTHLY">月</el-radio>
            <el-radio value="QUARTERLY">季</el-radio>
            <el-radio value="ONCE">一次性</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="覆盖现有">
          <el-switch v-model="genForm.overwrite" />
          <span class="text-muted ml-1">开启后会先删除该合同已有计划</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="genDialog = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="onGenerate">生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="formDialog" :title="editing ? '编辑期次' : '新增期次'" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="期数">
          <el-input-number v-model="form.periodNo" :min="1" :disabled="editing" />
        </el-form-item>
        <el-form-item label="计划日期">
          <el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划金额">
          <el-input-number v-model="form.planAmount" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="提醒提前">
          <el-input-number v-model="form.remindDays" :min="0" :max="365" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmitForm">保存</el-button>
      </template>
    </el-dialog>

    <PaymentRecordDrawer
      v-model:visible="recordDrawer"
      :default-contract-id="contractId"
      @saved="onRecordSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Money, Plus, Refresh } from '@element-plus/icons-vue'
import {
  paymentPlanApi,
  PaymentPlanStatus,
  type PaymentPlanVO
} from '@/api/payment'
import { formatDate } from '@/utils/format'
import EmptyHint from '@/components/EmptyHint.vue'
import PaymentRecordDrawer from '@/views/payment/components/PaymentRecordDrawer.vue'

const props = defineProps<{ contractId: string }>()

const rows = ref<PaymentPlanVO[]>([])
const loading = ref(false)

const genDialog = ref(false)
const generating = ref(false)
const genForm = reactive({
  firstPlanDate: new Date().toISOString().slice(0, 10),
  periods: 12,
  frequency: 'MONTHLY' as 'MONTHLY' | 'QUARTERLY' | 'ONCE',
  overwrite: false
})

const formDialog = ref(false)
const saving = ref(false)
const editing = ref(false)
const editId = ref<string | null>(null)
const editVersion = ref(0)
const form = reactive({
  periodNo: 1,
  planDate: '',
  planAmount: 0,
  remindDays: 7
})

const recordDrawer = ref(false)

// 请求 token：仅最后一次结果生效，避免快速切合同时旧请求覆盖新数据
let reqToken = 0

async function loadData() {
  if (!props.contractId) return
  const my = ++reqToken
  loading.value = true
  try {
    const data = await paymentPlanApi.byContract(props.contractId)
    if (my === reqToken) rows.value = data
  } finally {
    if (my === reqToken) loading.value = false
  }
}

watch(() => props.contractId, (id) => { if (id) loadData() }, { immediate: true })

async function onGenerate() {
  generating.value = true
  try {
    const ids = await paymentPlanApi.generate({
      contractId: props.contractId,
      firstPlanDate: genForm.firstPlanDate,
      periods: genForm.periods,
      frequency: genForm.frequency,
      overwrite: genForm.overwrite
    })
    ElMessage.success(`已生成 ${ids.length} 期`)
    genDialog.value = false
    loadData()
  } finally {
    generating.value = false
  }
}

function openCreate() {
  editing.value = false
  editId.value = null
  Object.assign(form, {
    periodNo: (rows.value[rows.value.length - 1]?.periodNo ?? 0) + 1,
    planDate: new Date().toISOString().slice(0, 10),
    planAmount: 0,
    remindDays: 7
  })
  formDialog.value = true
}

function openEdit(row: PaymentPlanVO) {
  editing.value = true
  editId.value = row.id
  editVersion.value = row.version
  Object.assign(form, {
    periodNo: row.periodNo,
    planDate: row.planDate,
    planAmount: row.planAmount,
    remindDays: row.remindDays ?? 7
  })
  formDialog.value = true
}

async function onSubmitForm() {
  saving.value = true
  try {
    if (editing.value && editId.value) {
      await paymentPlanApi.update(editId.value, {
        planDate: form.planDate,
        planAmount: form.planAmount,
        remindDays: form.remindDays,
        version: editVersion.value
      })
      ElMessage.success('已更新')
    } else {
      await paymentPlanApi.create({
        contractId: props.contractId,
        periodNo: form.periodNo,
        planDate: form.planDate,
        planAmount: form.planAmount,
        remindDays: form.remindDays
      })
      ElMessage.success('已新增')
    }
    formDialog.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

function canDelete(row: PaymentPlanVO) {
  return Number(row.settledAmount) === 0
}

async function onDelete(row: PaymentPlanVO) {
  await paymentPlanApi.remove(row.id)
  ElMessage.success('已删除')
  loadData()
}

function onRecordSaved() {
  recordDrawer.value = false
  loadData()
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.text-muted {
  color: #909399;
}
.ml-1 {
  margin-left: 6px;
}
</style>
