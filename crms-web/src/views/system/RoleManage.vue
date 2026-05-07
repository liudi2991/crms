<template>
  <div class="crms-page">
    <el-card class="crms-card" shadow="never">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="onCreate">新建角色</el-button>
        <el-button :icon="Refresh" circle @click="loadData" />
      </div>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="code" label="编码" width="220" />
        <el-table-column prop="name" label="名称" width="180" />
        <el-table-column label="数据范围" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="scopeType(row.dataScope)" size="small">
              {{ scopeLabel(row.dataScope) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限点数" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.permissionCodes?.length || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用户数" width="80" align="center">
          <template #default="{ row }">{{ row.userCount }}</template>
        </el-table-column>
        <el-table-column label="内置" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.builtin" size="small" type="warning">内置</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" width="180" align="center">
          <template #default="{ row }">
            <el-button link size="small" @click="onEdit(row)">编辑</el-button>
            <el-popconfirm
              v-if="!row.builtin"
              :title="`确认删除角色 ${row.name}？`"
              @confirm="onDelete(row)"
            >
              <template #reference>
                <el-button link size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 抽屉 -->
    <el-drawer
      v-model="formVisible"
      :title="formMode === 'create' ? '新建角色' : '编辑角色'"
      size="560px"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="编码" prop="code">
          <el-input
            v-model="form.code"
            placeholder="如 R10_FINANCE_VIEWER"
            :disabled="formMode === 'edit'"
          />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-radio-group v-model="form.dataScope">
            <el-radio-button label="ALL">全公司</el-radio-button>
            <el-radio-button label="DEPT">本部门</el-radio-button>
            <el-radio-button label="SELF">仅本人</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="权限点">
          <el-tree
            ref="permTreeRef"
            :data="permTree"
            show-checkbox
            node-key="code"
            :props="{ label: 'name', children: 'children' }"
            :default-expand-all="false"
            class="perm-tree"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElTree, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  roleApi,
  permissionApi,
  type RoleVO,
  type PermissionVO,
  type CreateRoleDTO
} from '@/api/iam'

const rows = ref<RoleVO[]>([])
const loading = ref(false)
const saving = ref(false)

const permTree = ref<PermissionVO[]>([])
const permTreeRef = ref<InstanceType<typeof ElTree>>()

const formRef = ref<FormInstance>()
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingId = ref<string | null>(null)
const editingVersion = ref<number>(0)

const form = reactive<CreateRoleDTO>({
  code: '',
  name: '',
  dataScope: 'SELF',
  description: '',
  sort: 100,
  permissionCodes: []
})

const formRules: FormRules = {
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  dataScope: [{ required: true, message: '请选择数据范围', trigger: 'change' }]
}

function scopeLabel(s: string) {
  return s === 'ALL' ? '全公司' : s === 'DEPT' ? '本部门' : '仅本人'
}
function scopeType(s: string) {
  return s === 'ALL' ? 'success' : s === 'DEPT' ? 'warning' : 'info'
}

async function loadData() {
  loading.value = true
  try {
    rows.value = await roleApi.list()
  } finally {
    loading.value = false
  }
}

async function loadPermissionTree() {
  permTree.value = await permissionApi.tree()
}

function onCreate() {
  formMode.value = 'create'
  editingId.value = null
  Object.assign(form, {
    code: '',
    name: '',
    dataScope: 'SELF',
    description: '',
    sort: 100,
    permissionCodes: []
  })
  formVisible.value = true
  setTimeout(() => permTreeRef.value?.setCheckedKeys([], false), 0)
}

async function onEdit(row: RoleVO) {
  formMode.value = 'edit'
  editingId.value = row.id
  editingVersion.value = row.version
  const detail = await roleApi.detail(row.id)
  Object.assign(form, {
    code: detail.code,
    name: detail.name,
    dataScope: detail.dataScope,
    description: detail.description || '',
    sort: detail.sort,
    permissionCodes: detail.permissionCodes
  })
  formVisible.value = true
  setTimeout(() => {
    // 仅勾选叶子节点（type !== MENU），避免父子勾选误差
    const leafCodes = detail.permissionCodes.filter((c) => !isMenuCode(c, permTree.value))
    permTreeRef.value?.setCheckedKeys(leafCodes, false)
  }, 50)
}

function isMenuCode(code: string, tree: PermissionVO[]): boolean {
  for (const n of tree) {
    if (n.code === code) return n.type === 'MENU'
    if (n.children?.length && isMenuCode(code, n.children)) return true
  }
  return false
}

async function onSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    const checked = permTreeRef.value?.getCheckedKeys(false) as string[] | undefined
    const halfChecked = permTreeRef.value?.getHalfCheckedKeys() as string[] | undefined
    const codes = [...(checked || []), ...(halfChecked || [])]
    saving.value = true
    try {
      if (formMode.value === 'create') {
        await roleApi.create({ ...form, permissionCodes: codes })
        ElMessage.success('已创建')
      } else if (editingId.value != null) {
        await roleApi.update(editingId.value, {
          name: form.name,
          dataScope: form.dataScope,
          description: form.description,
          sort: form.sort,
          permissionCodes: codes,
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

async function onDelete(row: RoleVO) {
  await roleApi.remove(row.id)
  ElMessage.success('已删除')
  loadData()
}

onMounted(async () => {
  await loadPermissionTree()
  await loadData()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.perm-tree {
  width: 100%;
  max-height: 360px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 6px 8px;
}
.text-muted {
  color: #909399;
}
</style>
