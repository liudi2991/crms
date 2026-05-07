<template>
  <el-drawer
    :model-value="visible"
    :title="isEdit ? '编辑客户' : '新建客户'"
    size="520px"
    direction="rtl"
    @update:model-value="(v: boolean) => $emit('update:visible', v)"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      label-position="right"
    >
      <el-form-item v-if="isEdit" label="编号">
        <el-input :model-value="form.code" disabled />
      </el-form-item>
      <el-form-item label="客户名称" prop="name">
        <el-input v-model="form.name" maxlength="100" show-word-limit />
      </el-form-item>
      <el-form-item label="简称" prop="shortName">
        <el-input v-model="form.shortName" maxlength="50" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="form.type" style="width: 100%">
          <el-option
            v-for="(label, key) in CustomerType"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="等级" prop="level">
        <el-radio-group v-model="form.level">
          <el-radio v-for="l in CustomerLevel" :key="l" :value="l">{{ l }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="统一信用代码" prop="uscc">
        <el-input v-model="form.uscc" maxlength="18" placeholder="18 位字符（选填）" @blur="onCheckDup" />
      </el-form-item>
      <el-alert
        v-if="duplicates.length > 0"
        type="warning"
        :closable="false"
        show-icon
        class="dup-alert"
      >
        <template #title>
          检测到 {{ duplicates.length }} 条相似客户：
          <span v-for="d in duplicates" :key="d.id" class="dup-item">
            {{ d.name }} <span class="dup-code">({{ d.code }})</span>
            <el-tag size="small" :type="d.hitField === 'USCC' ? 'danger' : 'warning'" class="ml-1">
              {{ d.hitField === 'USCC' ? 'USCC 重复' : '名称相似' }}
            </el-tag>
          </span>
        </template>
      </el-alert>
      <el-form-item label="行业" prop="industry">
        <el-input v-model="form.industry" />
      </el-form-item>
      <el-form-item label="地址" prop="address">
        <el-input v-model="form.address" type="textarea" :rows="2" maxlength="255" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="1000" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  customerApi,
  type CreateCustomerDTO,
  type CustomerDuplicateVO,
  type CustomerVO,
  type UpdateCustomerDTO
} from '@/api/customer'
import { CustomerLevel, CustomerType } from '@/utils/enum'

const props = defineProps<{ visible: boolean; record: CustomerVO | null }>()
const emit = defineEmits<{ 'update:visible': [boolean]; saved: [] }>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive<Partial<UpdateCustomerDTO> & { code?: string }>(emptyForm())
const isEdit = computed(() => !!props.record)

const rules: FormRules = {
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  level: [{ required: true, message: '请选择等级', trigger: 'change' }],
  uscc: [
    {
      pattern: /^$|^[0-9A-HJ-NPQRTUWXY]{18}$/,
      message: '统一信用代码格式不正确',
      trigger: 'blur'
    }
  ]
}

const duplicates = ref<CustomerDuplicateVO[]>([])
let dupTimer: ReturnType<typeof setTimeout> | null = null

watch(
  () => props.visible,
  (v) => {
    if (v) {
      Object.assign(form, props.record ? { ...props.record } : emptyForm())
      duplicates.value = []
    }
  }
)

watch(
  () => form.name,
  () => {
    if (dupTimer) clearTimeout(dupTimer)
    dupTimer = setTimeout(onCheckDup, 400)
  }
)

async function onCheckDup() {
  const name = (form.name || '').trim()
  const uscc = (form.uscc || '').trim()
  if (!name && !uscc) {
    duplicates.value = []
    return
  }
  try {
    duplicates.value = await customerApi.checkDuplicate({
      name: name || undefined,
      uscc: uscc || undefined,
      selfId: form.id
    })
  } catch {
    duplicates.value = []
  }
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && form.id) {
      await customerApi.update(form.id, form as UpdateCustomerDTO)
      ElMessage.success('已更新')
    } else {
      await customerApi.create(form as CreateCustomerDTO)
      ElMessage.success('已新建')
    }
    emit('saved')
  } finally {
    submitting.value = false
  }
}

function emptyForm() {
  return {
    name: '',
    shortName: '',
    type: 'ENTERPRISE' as const,
    uscc: '',
    industry: '',
    address: '',
    level: 'C' as const,
    remark: ''
  }
}
</script>

<style scoped>
.dup-alert {
  margin-bottom: 12px;
}
.dup-item {
  margin-right: 10px;
}
.dup-code {
  color: #909399;
  font-size: 12px;
}
.ml-1 {
  margin-left: 4px;
}
</style>
