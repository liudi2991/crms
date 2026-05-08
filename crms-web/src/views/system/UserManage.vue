<template>
  <div class="crms-page">
    <el-card class="crms-card" shadow="never">
      <el-form inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="用户名 / 姓名 / 手机号"
            clearable
            style="width: 220px"
            @keyup.enter="onSearch"
          />
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="query.deptId"
            :data="deptTree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            check-strictly
            clearable
            placeholder="全部部门"
            style="width: 220px"
            @change="onSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部"
            clearable
            style="width: 120px"
            @change="onSearch"
          >
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="onCreate">新建用户</el-button>
        <el-button :icon="Refresh" circle @click="loadData" />
      </div>

      <el-table v-loading="loading" :data="rows" stripe border>
        <el-table-column prop="username" label="账号" width="160" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column label="部门" width="180">
          <template #default="{ row }">{{ row.deptName || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="200">
          <template #default="{ row }">
            <el-tag
              v-for="r in row.roleNames"
              :key="r"
              size="small"
              type="info"
              class="role-tag"
            >
              {{ r }}
            </el-tag>
            <span v-if="!row.roleNames?.length" class="text-muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.locked" type="danger" size="small">已锁定</el-tag>
            <el-tag v-else-if="row.status === 'ACTIVE'" type="success" size="small">启用</el-tag>
            <el-tag v-else type="info" size="small">停用</el-tag>
            <el-tag v-if="row.superAdmin" type="warning" size="small" class="role-tag">超管</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最近登录" width="170" />
        <el-table-column label="操作" fixed="right" width="320" align="center">
          <template #default="{ row }">
            <el-button link size="small" @click="onEdit(row)">编辑</el-button>
            <el-button link size="small" @click="onAssignRoles(row)">角色</el-button>
            <el-button link size="small" type="warning" @click="onResetPassword(row)">
              重置密码
            </el-button>
            <el-button
              v-if="!row.superAdmin"
              link
              size="small"
              :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
              @click="onToggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
            </el-button>
            <el-button v-if="row.locked" link size="small" type="success" @click="onUnlock(row)">
              解锁
            </el-button>
            <el-popconfirm
              v-if="!row.superAdmin"
              :title="`确认删除用户 ${row.username}？`"
              @confirm="onDelete(row)"
            >
              <template #reference>
                <el-button link size="small">
                  <span class="text-danger">删除</span>
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

    <!-- 新建 / 编辑抽屉 -->
    <el-drawer
      v-model="formVisible"
      :title="formMode === 'create' ? '新建用户' : '编辑用户'"
      size="480px"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item v-if="formMode === 'create'" label="账号" prop="username">
          <el-input v-model="form.username" placeholder="3-64 位字母数字" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            placeholder="选择部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="formMode === 'create'" label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="请选择" style="width: 100%">
            <el-option
              v-for="r in roleOptions"
              :key="r.id"
              :label="r.name"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="formMode === 'create'" label="初始密码" prop="password">
          <el-input v-model="form.password" placeholder="留空使用系统默认" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-drawer>

    <!-- 分配角色 -->
    <el-dialog v-model="rolesVisible" title="分配角色" width="420px">
      <el-form>
        <el-form-item>
          <el-select v-model="rolesEditing" multiple placeholder="请选择" style="width: 100%">
            <el-option
              v-for="r in roleOptions"
              :key="r.id"
              :label="r.name"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rolesVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSaveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import {
  userApi,
  roleApi,
  deptApi,
  type UserVO,
  type UserQuery,
  type CreateUserDTO,
  type UpdateUserDTO,
  type RoleVO,
  type DepartmentVO
} from '@/api/iam'

const query = reactive<UserQuery>({ page: 1, size: 20 })
const rows = ref<UserVO[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)

const deptTree = ref<DepartmentVO[]>([])
const roleOptions = ref<RoleVO[]>([])

const formRef = ref<FormInstance>()
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingId = ref<string | null>(null)
const form = reactive<CreateUserDTO & { id?: string }>({
  username: '',
  realName: '',
  phone: '',
  email: '',
  deptId: undefined as unknown as string,
  roleIds: [],
  password: ''
})
const formRules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  roleIds: [{ required: true, message: '请选择至少一个角色', trigger: 'change' }]
}

const rolesVisible = ref(false)
const rolesEditingUser = ref<UserVO | null>(null)
const rolesEditing = ref<string[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await userApi.list(query)
    rows.value = res.items
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function loadDictionaries() {
  const [tree, roles] = await Promise.all([deptApi.tree(), roleApi.list()])
  deptTree.value = tree
  roleOptions.value = roles
}

function onSearch() {
  query.page = 1
  loadData()
}

function onReset() {
  query.keyword = undefined
  query.deptId = undefined
  query.status = undefined
  query.page = 1
  loadData()
}

function onCreate() {
  formMode.value = 'create'
  editingId.value = null
  Object.assign(form, {
    username: '',
    realName: '',
    phone: '',
    email: '',
    deptId: undefined,
    roleIds: [],
    password: ''
  })
  formVisible.value = true
}

function onEdit(row: UserVO) {
  formMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, {
    username: row.username,
    realName: row.realName,
    phone: row.phone || '',
    email: row.email || '',
    deptId: row.deptId,
    roleIds: row.roleIds || [],
    password: ''
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
        await userApi.create({
          username: form.username,
          realName: form.realName,
          phone: form.phone || undefined,
          email: form.email || undefined,
          deptId: form.deptId,
          roleIds: form.roleIds,
          password: form.password || undefined
        })
        ElMessage.success('创建成功，初始密码已发送给该用户')
      } else if (editingId.value != null) {
        const dto: UpdateUserDTO = {
          realName: form.realName,
          phone: form.phone || undefined,
          email: form.email || undefined,
          deptId: form.deptId
        }
        await userApi.update(editingId.value, dto)
        ElMessage.success('已更新')
      }
      formVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

async function onDelete(row: UserVO) {
  await userApi.remove(row.id)
  ElMessage.success('已删除')
  loadData()
}

async function onToggleStatus(row: UserVO) {
  if (row.status === 'ACTIVE') {
    await userApi.disable(row.id)
    ElMessage.success('已停用')
  } else {
    await userApi.enable(row.id)
    ElMessage.success('已启用')
  }
  loadData()
}

async function onUnlock(row: UserVO) {
  await userApi.unlock(row.id)
  ElMessage.success('已解锁')
  loadData()
}

async function onResetPassword(row: UserVO) {
  try {
    const { value } = await ElMessageBox.prompt(
      `重置 ${row.username} 的密码（留空使用系统默认密码）`,
      '重置密码',
      {
        confirmButtonText: '重置',
        cancelButtonText: '取消',
        inputPlaceholder: '至少 8 位，留空则使用默认',
        inputType: 'password'
      }
    )
    await userApi.resetPassword(row.id, value || undefined)
    ElMessage.success('密码已重置，用户下次登录需修改密码')
  } catch {
    /* user cancel */
  }
}

function onAssignRoles(row: UserVO) {
  rolesEditingUser.value = row
  rolesEditing.value = [...(row.roleIds || [])]
  rolesVisible.value = true
}

async function onSaveRoles() {
  if (!rolesEditingUser.value) return
  if (rolesEditing.value.length === 0) {
    ElMessage.warning('请至少选择一个角色')
    return
  }
  saving.value = true
  try {
    await userApi.assignRoles(rolesEditingUser.value.id, rolesEditing.value)
    ElMessage.success('角色已更新')
    rolesVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadDictionaries()
  await loadData()
})
</script>

<style scoped>
.search-bar {
  margin-bottom: 8px;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.role-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}
.text-muted {
  color: #909399;
}
.mt-2 {
  margin-top: 12px;
}
</style>
