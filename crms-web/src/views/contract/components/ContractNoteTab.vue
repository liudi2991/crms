<template>
  <div>
    <el-input
      v-model="newContent"
      type="textarea"
      :rows="3"
      placeholder="输入备注内容..."
      maxlength="1000"
      show-word-limit
    />
    <div class="add-bar">
      <el-button v-perm="'contract:note'" type="primary" :loading="saving" :disabled="!newContent.trim()" @click="onAdd">
        新增备注
      </el-button>
      <el-button :icon="Refresh" circle size="small" @click="loadData" />
    </div>

    <el-empty v-if="!rows.length && !loading" description="暂无备注" />
    <div v-else v-loading="loading" class="notes">
      <div v-for="n in rows" :key="n.id" class="note">
        <div class="note-head">
          <span class="author">用户 #{{ n.authorId }}</span>
          <span class="time">{{ n.createdAt }}</span>
          <el-popconfirm v-if="canDelete(n)" title="确认删除此备注？" @confirm="onDelete(n)">
            <template #reference>
              <el-button link size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </div>
        <div class="content">{{ n.content }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { contractApi, type ContractNoteVO } from '@/api/contract'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{ contractId: string }>()

const rows = ref<ContractNoteVO[]>([])
const loading = ref(false)
const saving = ref(false)
const newContent = ref('')
const auth = useAuthStore()

// 请求 token：仅最后一次结果生效
let reqToken = 0

async function loadData() {
  if (!props.contractId) return
  const my = ++reqToken
  loading.value = true
  try {
    const data = await contractApi.notes(props.contractId)
    if (my === reqToken) rows.value = data
  } finally {
    if (my === reqToken) loading.value = false
  }
}

watch(() => props.contractId, (id) => { if (id) loadData() }, { immediate: true })

async function onAdd() {
  saving.value = true
  try {
    await contractApi.addNote(props.contractId, newContent.value)
    newContent.value = ''
    ElMessage.success('已添加')
    loadData()
  } finally {
    saving.value = false
  }
}

async function onDelete(n: ContractNoteVO) {
  await contractApi.removeNote(n.id)
  ElMessage.success('已删除')
  loadData()
}

function canDelete(n: ContractNoteVO) {
  return auth.user?.superAdmin || auth.user?.id === n.authorId
}
</script>

<style scoped>
.add-bar {
  display: flex;
  gap: 8px;
  margin: 8px 0 16px;
}
.notes {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.note {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 10px 12px;
  background: #fafbfc;
}
.note-head {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 4px;
}
.author {
  font-weight: 600;
  color: #303133;
}
.time {
  color: #909399;
  font-size: 12px;
  flex: 1;
}
.content {
  white-space: pre-wrap;
  color: #606266;
}
</style>
