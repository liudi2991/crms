<template>
  <div class="crms-page">
    <el-card class="crms-card" shadow="never">
      <div class="toolbar">
        <h3 style="margin:0">系统参数</h3>
        <div>
          <el-button :icon="Refresh" circle @click="loadData" />
          <el-button type="primary" :icon="Check" :disabled="!dirty" :loading="saving" @click="onSave">
            保存修改 ({{ dirtyCount }})
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="paramKey" label="参数 Key" width="280" />
        <el-table-column label="参数值" min-width="220">
          <template #default="{ row }">
            <el-input v-model="row.paramValue" @input="markDirty(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="最近更新" width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Refresh } from '@element-plus/icons-vue'
import { systemParamApi, type SystemParamVO } from '@/api/system'

const rows = ref<(SystemParamVO & { _dirty?: boolean })[]>([])
const original = ref<Record<string, string>>({})
const loading = ref(false)
const saving = ref(false)

const dirty = computed(() => rows.value.some((r) => r._dirty))
const dirtyCount = computed(() => rows.value.filter((r) => r._dirty).length)

async function loadData() {
  loading.value = true
  try {
    const list = await systemParamApi.list()
    rows.value = list.map((r) => ({ ...r, _dirty: false }))
    original.value = Object.fromEntries(list.map((r) => [r.paramKey, r.paramValue]))
  } finally {
    loading.value = false
  }
}

function markDirty(row: SystemParamVO & { _dirty?: boolean }) {
  row._dirty = original.value[row.paramKey] !== row.paramValue
}

async function onSave() {
  const items = rows.value
    .filter((r) => r._dirty)
    .map((r) => ({ paramKey: r.paramKey, paramValue: r.paramValue }))
  if (items.length === 0) return
  saving.value = true
  try {
    await systemParamApi.batchUpdate(items)
    ElMessage.success(`已更新 ${items.length} 项`)
    await loadData()
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>
