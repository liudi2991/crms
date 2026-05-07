<template>
  <el-drawer
    :model-value="visible"
    :title="isEdit ? '编辑合同' : '新建合同'"
    size="560px"
    direction="rtl"
    @update:model-value="(v: boolean) => emit('update:visible', v)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item v-if="isEdit" label="编号">
        <el-input :model-value="form.code" disabled />
      </el-form-item>
      <el-form-item label="合同名称" prop="name">
        <el-input v-model="form.name" maxlength="100" show-word-limit />
      </el-form-item>
      <el-form-item label="合同类型" prop="type">
        <el-select v-model="form.type" style="width: 100%">
          <el-option v-for="(label, key) in ContractType" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="客户" prop="customerId">
        <el-select
          v-model="form.customerId"
          filterable
          remote
          :remote-method="searchCustomer"
          :loading="customerLoading"
          placeholder="输入客户名称查询"
          style="width: 100%"
        >
          <el-option
            v-for="c in customerOptions"
            :key="c.id"
            :label="`${c.name} (${c.code})`"
            :value="c.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="合同金额" prop="amount">
        <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="1000" style="width: 100%" />
      </el-form-item>
      <el-form-item label="签订日期" prop="signedAt">
        <el-date-picker v-model="form.signedAt" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="履约开始" prop="performStartAt">
        <el-date-picker v-model="form.performStartAt" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="履约结束" prop="performEndAt">
        <el-date-picker v-model="form.performEndAt" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="提醒提前">
        <el-input-number v-model="form.remindDays" :min="0" :max="365" />
        <span class="text-muted ml-1">天（留空使用全局参数）</span>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="1000" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { customerApi, type CustomerVO } from '@/api/customer'
import {
  contractApi,
  ContractType,
  type ContractVO,
  type CreateContractDTO,
  type UpdateContractDTO
} from '@/api/contract'

const props = defineProps<{ visible: boolean; record: ContractVO | null }>()
const emit = defineEmits<{ 'update:visible': [boolean]; saved: [] }>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const isEdit = computed(() => !!props.record)

const form = reactive<Record<string, any>>(emptyForm())

const rules: FormRules = {
  name: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择合同类型', trigger: 'change' }],
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  amount: [{ required: true, message: '请填写金额', trigger: 'blur' }],
  signedAt: [{ required: true, message: '请选择签订日期', trigger: 'change' }],
  performStartAt: [{ required: true, message: '请选择履约开始', trigger: 'change' }],
  performEndAt: [{ required: true, message: '请选择履约结束', trigger: 'change' }]
}

const customerOptions = ref<CustomerVO[]>([])
const customerLoading = ref(false)

watch(
  () => props.visible,
  async (v) => {
    if (v) {
      Object.assign(form, props.record ? { ...props.record } : emptyForm())
      if (props.record?.customerId) {
        try {
          const c = await customerApi.detail(props.record.customerId)
          customerOptions.value = [c]
        } catch {
          customerOptions.value = []
        }
      } else {
        customerOptions.value = []
      }
    }
  }
)

async function searchCustomer(keyword: string) {
  if (!keyword) {
    customerOptions.value = []
    return
  }
  customerLoading.value = true
  try {
    const res = await customerApi.list({ keyword, page: 1, size: 20 })
    customerOptions.value = res.items
  } finally {
    customerLoading.value = false
  }
}

async function onSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value && form.id) {
      await contractApi.update(form.id, form as UpdateContractDTO)
      ElMessage.success('已更新')
    } else {
      await contractApi.create(form as CreateContractDTO)
      ElMessage.success('已新建')
    }
    emit('saved')
  } finally {
    saving.value = false
  }
}

function emptyForm() {
  return {
    name: '',
    type: 'SALES' as const,
    customerId: undefined as unknown as string,
    amount: 0,
    signedAt: '',
    performStartAt: '',
    performEndAt: '',
    remindDays: 30,
    remark: ''
  }
}
</script>

<style scoped>
.text-muted {
  color: #909399;
}
.ml-1 {
  margin-left: 6px;
}
</style>
