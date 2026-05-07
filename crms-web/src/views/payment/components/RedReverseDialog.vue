<template>
  <el-dialog
    :model-value="visible"
    title="红冲回款"
    width="480px"
    @update:model-value="(v: boolean) => emit('update:visible', v)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="原金额">
        <span>{{ record?.amount }}</span>
      </el-form-item>
      <el-form-item label="红冲金额" prop="redAmount">
        <el-input-number v-model="form.redAmount" :min="0.01" :precision="2" :max="record?.amount" style="width: 100%" />
      </el-form-item>
      <el-form-item label="原因" prop="reason">
        <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="500" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="danger" :loading="saving" @click="onSubmit">确认红冲</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { paymentRecordApi, type PaymentRecordVO } from '@/api/payment'

const props = defineProps<{ visible: boolean; record: PaymentRecordVO | null }>()
const emit = defineEmits<{ 'update:visible': [boolean]; saved: [] }>()

const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive({ redAmount: 0, reason: '' })

const rules: FormRules = {
  redAmount: [{ required: true, message: '请填写红冲金额', trigger: 'blur' }],
  reason: [{ required: true, message: '请填写红冲原因', trigger: 'blur' }]
}

watch(
  () => props.visible,
  (v) => {
    if (v) {
      form.redAmount = props.record?.amount ?? 0
      form.reason = ''
    }
  }
)

async function onSubmit() {
  if (!formRef.value || !props.record) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await paymentRecordApi.redReverse(props.record.id, form)
    ElMessage.success('已红冲')
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>
