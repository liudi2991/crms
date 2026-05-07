<template>
  <div>
    <div class="toolbar">
      <el-button v-perm="'customer:update'" type="primary" :icon="Plus" size="small" @click="onCreate">
        新增联系人
      </el-button>
      <el-button :icon="Refresh" circle size="small" @click="loadData" />
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="title" label="职务" width="140" />
      <el-table-column prop="phone" label="电话" width="160" />
      <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
      <el-table-column prop="wechat" label="微信" width="120" />
      <el-table-column label="主联系人" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isPrimary" type="success" size="small">主</el-tag>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" fixed="right" width="240" align="center">
        <template #default="{ row }">
          <el-button v-perm="'customer:update'" link size="small" @click="onEdit(row)">编辑</el-button>
          <el-button
            v-if="!row.isPrimary"
            v-perm="'customer:update'"
            link
            size="small"
            type="success"
            @click="onSetPrimary(row)"
          >
            设为主
          </el-button>
          <el-popconfirm
            :title="`确认删除 ${row.name}？`"
            @confirm="onDelete(row)"
          >
            <template #reference>
              <el-button v-perm="'customer:update'" link size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? '新增联系人' : '编辑联系人'"
      width="480px"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="职务"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="微信"><el-input v-model="form.wechat" /></el-form-item>
        <el-form-item v-if="formMode === 'create'" label="主联系人">
          <el-switch v-model="form.isPrimary" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  customerContactApi,
  type CustomerContactVO,
  type CreateContactDTO
} from '@/api/customer'

const props = defineProps<{ customerId: string }>()

const rows = ref<CustomerContactVO[]>([])
const loading = ref(false)
const saving = ref(false)

const formRef = ref<FormInstance>()
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingId = ref<string | null>(null)
const editingVersion = ref<number>(0)

const form = reactive<CreateContactDTO>({
  customerId: '',
  name: '',
  title: '',
  phone: '',
  email: '',
  wechat: '',
  isPrimary: false,
  remark: ''
})
const formRules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

// 请求 token：仅最后一次结果生效
let reqToken = 0

async function loadData() {
  if (!props.customerId) return
  const my = ++reqToken
  loading.value = true
  try {
    const data = await customerContactApi.list(props.customerId)
    if (my === reqToken) rows.value = data
  } finally {
    if (my === reqToken) loading.value = false
  }
}

watch(() => props.customerId, (id) => {
  if (id) loadData()
}, { immediate: true })

function onCreate() {
  formMode.value = 'create'
  editingId.value = null
  Object.assign(form, {
    customerId: props.customerId,
    name: '',
    title: '',
    phone: '',
    email: '',
    wechat: '',
    isPrimary: false,
    remark: ''
  })
  formVisible.value = true
}

function onEdit(row: CustomerContactVO) {
  formMode.value = 'edit'
  editingId.value = row.id
  editingVersion.value = row.version
  Object.assign(form, {
    customerId: props.customerId,
    name: row.name,
    title: row.title || '',
    phone: '',
    email: '',
    wechat: row.wechat || '',
    isPrimary: row.isPrimary,
    remark: row.remark || ''
  })
  formVisible.value = true
}

async function onSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (formMode.value === 'create') {
        await customerContactApi.create({ ...form })
        ElMessage.success('已创建')
      } else if (editingId.value != null) {
        await customerContactApi.update(editingId.value, {
          name: form.name,
          title: form.title,
          phone: form.phone,
          email: form.email,
          wechat: form.wechat,
          remark: form.remark,
          version: editingVersion.value
        })
        ElMessage.success('已更新')
      }
      formVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

async function onDelete(row: CustomerContactVO) {
  await customerContactApi.remove(row.id)
  ElMessage.success('已删除')
  loadData()
}

async function onSetPrimary(row: CustomerContactVO) {
  await customerContactApi.setPrimary(row.id)
  ElMessage.success('已设为主联系人')
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
</style>
