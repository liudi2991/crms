<template>
  <div class="crms-page">
    <el-card class="crms-card" shadow="never">
      <div class="dept-layout">
        <div class="left">
          <div class="toolbar">
            <el-button type="primary" :icon="Plus" size="small" @click="onCreateRoot">
              新增根部门
            </el-button>
            <el-button :icon="Refresh" circle size="small" @click="loadTree" />
          </div>
          <el-tree
            v-loading="loading"
            :data="tree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            highlight-current
            default-expand-all
            class="dept-tree"
            @node-click="onSelect"
          >
            <template #default="{ node, data }">
              <span class="node-row">
                <span>{{ data.name }}</span>
                <span class="node-meta">{{ data.userCount }} 人</span>
              </span>
            </template>
          </el-tree>
        </div>

        <div class="right">
          <el-empty v-if="!current" description="选择左侧节点查看 / 编辑" />
          <template v-else>
            <h3 class="title">{{ current.name }}</h3>
            <div class="path">{{ current.fullPath }}</div>
            <el-divider />

            <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
              <el-form-item label="名称" prop="name">
                <el-input v-model="form.name" />
              </el-form-item>
              <el-form-item label="父部门" prop="parentId">
                <el-tree-select
                  v-model="form.parentId"
                  :data="treeWithRoot"
                  node-key="id"
                  :props="{ label: 'name', children: 'children' }"
                  check-strictly
                  placeholder="选择父部门"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="排序">
                <el-input-number v-model="form.sort" :min="0" :max="9999" />
              </el-form-item>
            </el-form>

            <div class="actions">
              <el-button type="primary" :loading="saving" @click="onSave">保存修改</el-button>
              <el-button :icon="Plus" @click="onCreateChild">添加子部门</el-button>
              <el-popconfirm
                :title="`确认删除 ${current.name}？（要求无子部门且无成员）`"
                @confirm="onDelete"
              >
                <template #reference>
                  <el-button plain type="danger" :icon="Delete">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </div>
      </div>
    </el-card>

    <!-- 创建抽屉 -->
    <el-dialog v-model="createVisible" title="新建部门" width="420px">
      <el-form ref="createFormRef" :model="createForm" :rules="createFormRules" label-width="80px">
        <el-form-item label="父部门" prop="parentId">
          <el-tree-select
            v-model="createForm.parentId"
            :data="treeWithRoot"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            check-strictly
            placeholder="选择父部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="createForm.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="createForm.sort" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Plus, Refresh } from '@element-plus/icons-vue'
import {
  deptApi,
  type DepartmentVO,
  type CreateDepartmentDTO
} from '@/api/iam'

const tree = ref<DepartmentVO[]>([])
const loading = ref(false)
const saving = ref(false)

const current = ref<DepartmentVO | null>(null)

const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  parentId: '0' as string,
  sort: 0,
  version: 0
})
const formRules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  parentId: [{ required: true, message: '请选择父部门', trigger: 'change' }]
}

const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive<CreateDepartmentDTO>({
  parentId: '0',
  name: '',
  sort: 0
})
const createFormRules: FormRules = {
  parentId: [{ required: true, message: '请选择父部门', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

// 给"父部门"下拉一个虚拟根节点
const treeWithRoot = computed(() => [
  { id: '0', name: '（顶级）', children: tree.value }
])

async function loadTree() {
  loading.value = true
  try {
    tree.value = await deptApi.tree()
    if (current.value) {
      const found = findById(tree.value, current.value.id)
      current.value = found
      if (found) syncForm(found)
    }
  } finally {
    loading.value = false
  }
}

function findById(nodes: DepartmentVO[], id: string): DepartmentVO | null {
  for (const n of nodes) {
    if (n.id === id) return n
    const child = findById(n.children || [], id)
    if (child) return child
  }
  return null
}

function syncForm(d: DepartmentVO) {
  form.name = d.name
  form.parentId = d.parentId
  form.sort = d.sort
  form.version = d.version
}

function onSelect(d: DepartmentVO) {
  current.value = d
  syncForm(d)
}

async function onSave() {
  if (!current.value || !formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      await deptApi.update(current.value!.id, { ...form })
      ElMessage.success('已保存')
      await loadTree()
    } finally {
      saving.value = false
    }
  })
}

function onCreateRoot() {
  Object.assign(createForm, { parentId: '0', name: '', sort: 0 })
  createVisible.value = true
}

function onCreateChild() {
  if (!current.value) return
  Object.assign(createForm, { parentId: current.value.id, name: '', sort: 0 })
  createVisible.value = true
}

async function onSubmitCreate() {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      await deptApi.create({ ...createForm })
      ElMessage.success('已创建')
      createVisible.value = false
      await loadTree()
    } finally {
      saving.value = false
    }
  })
}

async function onDelete() {
  if (!current.value) return
  await deptApi.remove(current.value.id)
  ElMessage.success('已删除')
  current.value = null
  await loadTree()
}

onMounted(loadTree)
</script>

<style scoped lang="scss">
.dept-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  min-height: 500px;
}

.left {
  border-right: 1px solid #ebeef5;
  padding-right: 12px;
  .toolbar {
    display: flex;
    gap: 6px;
    margin-bottom: 8px;
  }
}

.right {
  padding-left: 8px;
  .title {
    margin: 0 0 6px;
  }
  .path {
    color: #909399;
    font-size: 12px;
  }
  .actions {
    display: flex;
    gap: 8px;
    margin-top: 8px;
  }
}

.dept-tree {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 6px;
  max-height: 600px;
  overflow: auto;
}

.node-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex: 1;
  padding-right: 10px;
}

.node-meta {
  font-size: 12px;
  color: #909399;
}
</style>
