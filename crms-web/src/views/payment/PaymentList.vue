<template>
  <div class="crms-page">
    <el-card class="crms-card" shadow="never">
      <el-tabs v-model="tab">
        <el-tab-pane label="实际回款" name="records">
          <el-form inline class="search-bar">
            <el-form-item label="关键字">
              <el-input v-model="rq.keyword" placeholder="付款方 / 凭证号" clearable style="width: 200px" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="rq.status" placeholder="全部" clearable style="width: 120px">
                <el-option v-for="(meta, key) in PaymentRecordStatus" :key="key" :label="meta.label" :value="key" />
              </el-select>
            </el-form-item>
            <el-form-item label="到账日期">
              <el-date-picker
                v-model="rRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                range-separator="~"
                start-placeholder="起"
                end-placeholder="止"
                style="width: 280px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadRecords">查询</el-button>
              <el-button @click="onResetRecords">重置</el-button>
            </el-form-item>
          </el-form>

          <div class="toolbar">
            <el-button v-perm="'payment:record'" type="primary" :icon="Plus" @click="recordDrawer = true">登记</el-button>
            <el-button v-perm="'payment:import'" :icon="Upload" @click="importDialog = true">导入 Excel</el-button>
            <el-button :icon="Refresh" circle @click="loadRecords" />
          </div>

          <el-table v-loading="recordsLoading" :data="records" stripe border>
            <el-table-column prop="contractCode" label="合同编号" width="160" />
            <el-table-column label="到账日期" width="120">
              <template #default="{ row }">{{ row.arrivalDate }}</template>
            </el-table-column>
            <el-table-column label="金额" width="160" align="right">
              <template #default="{ row }">{{ Number(row.amount).toLocaleString() }}</template>
            </el-table-column>
            <el-table-column label="未分配" width="140" align="right">
              <template #default="{ row }">{{ Number(row.unallocatedAmount || 0).toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="payer" label="付款方" min-width="120" show-overflow-tooltip />
            <el-table-column prop="voucherNo" label="凭证号" width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="PaymentRecordStatus[row.status]?.type">
                  {{ PaymentRecordStatus[row.status]?.label || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'NORMAL'"
                  v-perm="'payment:settle'"
                  link
                  size="small"
                  @click="onManualSettle(row)"
                >
                  手工核销
                </el-button>
                <el-button
                  v-if="row.status === 'NORMAL'"
                  v-perm="'payment:red'"
                  link
                  size="small"
                  type="warning"
                  @click="onRedReverse(row)"
                >
                  红冲
                </el-button>
                <el-popconfirm
                  v-if="row.status === 'NORMAL'"
                  title="确认删除该回款记录？"
                  @confirm="onDeleteRecord(row)"
                >
                  <template #reference>
                    <el-button v-perm="'payment:record'" link size="small">
                      <span class="text-danger">删除</span>
                    </el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="rq.page"
            v-model:page-size="rq.size"
            :total="recordsTotal"
            background
            layout="total, sizes, prev, pager, next, jumper"
            class="mt-2"
            @current-change="loadRecords"
            @size-change="loadRecords"
          />
        </el-tab-pane>

        <el-tab-pane label="回款计划" name="plans">
          <el-form inline class="search-bar">
            <el-form-item label="状态">
              <el-select v-model="pq.status" placeholder="全部" clearable style="width: 140px">
                <el-option v-for="(meta, key) in PaymentPlanStatus" :key="key" :label="meta.label" :value="key" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="pq.overdueOnly">仅看逾期</el-checkbox>
            </el-form-item>
            <el-form-item label="计划日期">
              <el-date-picker
                v-model="pRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                range-separator="~"
                start-placeholder="起"
                end-placeholder="止"
                style="width: 280px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadPlans">查询</el-button>
              <el-button @click="onResetPlans">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="plansLoading" :data="plans" stripe border>
            <el-table-column prop="contractCode" label="合同编号" width="160" />
            <el-table-column prop="periodNo" label="期数" width="80" align="center" />
            <el-table-column prop="planDate" label="计划日期" width="120" />
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
            <el-table-column label="逾期" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.overdue" type="danger">逾期 {{ row.overdueDays }} 天</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="pq.page"
            v-model:page-size="pq.size"
            :total="plansTotal"
            background
            layout="total, sizes, prev, pager, next, jumper"
            class="mt-2"
            @current-change="loadPlans"
            @size-change="loadPlans"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <PaymentRecordDrawer v-model:visible="recordDrawer" @saved="onRecordSaved" />
    <PaymentImportDialog v-model:visible="importDialog" @finished="loadRecords" />
    <RedReverseDialog v-model:visible="redDialog" :record="redTarget" @saved="onRedSaved" />

    <el-dialog v-model="settleDialog" title="手工核销" width="640px">
      <p v-if="settleTarget" class="text-muted">
        记录 #{{ settleTarget.id }} 金额 {{ settleTarget.amount }} 未分配 {{ settleTarget.unallocatedAmount }}
      </p>
      <el-table
        v-loading="settleLoading"
        :data="settlePlans"
        border
        :row-key="(row: PaymentPlanVO) => String(row.id)"
        @selection-change="(rows: PaymentPlanVO[]) => (selectedPlans = rows)"
      >
        <el-table-column type="selection" width="48" :selectable="(row: PaymentPlanVO) => row.status !== 'SETTLED'" reserve-selection />
        <el-table-column prop="periodNo" label="期数" width="60" />
        <el-table-column prop="planDate" label="计划日期" width="120" />
        <el-table-column label="待核销" width="140" align="right">
          <template #default="{ row }">{{ row.unsettledAmount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="PaymentPlanStatus[row.status]?.type" size="small">
              {{ PaymentPlanStatus[row.status]?.label }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="settleDialog = false">取消</el-button>
        <el-button type="primary" @click="onSettleConfirm">核销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Upload } from '@element-plus/icons-vue'
import {
  paymentPlanApi,
  paymentRecordApi,
  PaymentPlanStatus,
  PaymentRecordStatus,
  type PaymentPlanVO,
  type PaymentRecordVO,
  type PlanQuery,
  type RecordQuery
} from '@/api/payment'
import PaymentRecordDrawer from './components/PaymentRecordDrawer.vue'
import PaymentImportDialog from './components/PaymentImportDialog.vue'
import RedReverseDialog from './components/RedReverseDialog.vue'

const tab = ref<'records' | 'plans'>('records')

const rq = reactive<RecordQuery>({ page: 1, size: 20 })
const rRange = ref<[string, string] | null>(null)
const records = ref<PaymentRecordVO[]>([])
const recordsTotal = ref(0)
const recordsLoading = ref(false)

watch(rRange, (v) => {
  rq.fromDate = v?.[0]
  rq.toDate = v?.[1]
})

const pq = reactive<PlanQuery>({ page: 1, size: 20 })
const pRange = ref<[string, string] | null>(null)
const plans = ref<PaymentPlanVO[]>([])
const plansTotal = ref(0)
const plansLoading = ref(false)

watch(pRange, (v) => {
  pq.fromDate = v?.[0]
  pq.toDate = v?.[1]
})

const recordDrawer = ref(false)
const importDialog = ref(false)
const redDialog = ref(false)
const redTarget = ref<PaymentRecordVO | null>(null)

const settleDialog = ref(false)
const settleTarget = ref<PaymentRecordVO | null>(null)
const settlePlans = ref<PaymentPlanVO[]>([])
const settleLoading = ref(false)
const selectedPlans = ref<PaymentPlanVO[]>([])

async function loadRecords() {
  recordsLoading.value = true
  try {
    const res = await paymentRecordApi.list(rq)
    records.value = res.items
    recordsTotal.value = res.total
  } finally {
    recordsLoading.value = false
  }
}

function onResetRecords() {
  Object.assign(rq, { page: 1, size: 20 })
  rRange.value = null
  loadRecords()
}

async function loadPlans() {
  plansLoading.value = true
  try {
    const res = await paymentPlanApi.list(pq)
    plans.value = res.items
    plansTotal.value = res.total
  } finally {
    plansLoading.value = false
  }
}

function onResetPlans() {
  Object.assign(pq, { page: 1, size: 20 })
  pRange.value = null
  loadPlans()
}

async function onDeleteRecord(row: PaymentRecordVO) {
  await paymentRecordApi.remove(row.id)
  ElMessage.success('已删除')
  loadRecords()
}

function onRecordSaved() {
  recordDrawer.value = false
  loadRecords()
}

function onRedReverse(row: PaymentRecordVO) {
  redTarget.value = row
  redDialog.value = true
}

function onRedSaved() {
  redDialog.value = false
  loadRecords()
}

async function onManualSettle(row: PaymentRecordVO) {
  settleTarget.value = row
  selectedPlans.value = []
  settleLoading.value = true
  settleDialog.value = true
  try {
    settlePlans.value = (await paymentPlanApi.byContract(row.contractId)).filter(
      (p) => p.status !== 'SETTLED'
    )
  } finally {
    settleLoading.value = false
  }
}

async function onSettleConfirm() {
  if (!settleTarget.value) return
  const ids = selectedPlans.value.map((p) => p.id)
  if (!ids.length) {
    ElMessage.warning('请选择至少一条回款计划')
    return
  }
  await paymentRecordApi.manualSettle({ recordId: settleTarget.value.id, planIds: ids })
  ElMessage.success('已核销')
  settleDialog.value = false
  loadRecords()
}

onMounted(loadRecords)

watch(tab, (v) => {
  if (v === 'plans' && !plans.value.length) loadPlans()
})
</script>

<style scoped lang="scss">
/* tab 内紧凑筛选条：用 flex 重排 el-form-item，保留原 inline 模式但视觉更紧凑 */
.search-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin: 4px 0 12px;
  padding: 8px 0;

  :deep(.el-form-item) {
    margin: 0 !important;
  }
}

.toolbar {
  display: flex;
  gap: 8px;
  margin: 0 0 12px;
}

.mt-2 {
  margin-top: 12px;
}

.text-muted {
  color: #909399;
}
</style>
