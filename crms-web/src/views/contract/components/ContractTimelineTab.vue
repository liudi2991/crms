<template>
  <div>
    <div class="toolbar">
      <span class="text-muted">最近 100 条变更</span>
      <el-button :icon="Refresh" circle size="small" @click="loadData" />
    </div>
    <EmptyHint v-if="!loading && !rows.length" description="暂无变更" />
    <el-timeline v-else>
      <el-timeline-item
        v-for="r in rows"
        :key="r.id"
        :timestamp="formatDateTime(r.operatedAt)"
        placement="top"
        :type="colorOf(r.field)"
      >
        <div class="row">
          <strong>{{ r.field }}</strong>
          <span class="op">操作人 #{{ r.operatorId }}</span>
        </div>
        <div class="diff">
          <span class="old">{{ r.oldValue || '∅' }}</span>
          <el-icon><Right /></el-icon>
          <span class="new">{{ r.newValue || '∅' }}</span>
        </div>
        <div v-if="r.reason" class="reason">原因：{{ r.reason }}</div>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Refresh, Right } from '@element-plus/icons-vue'
import { contractApi, type ContractChangeLogVO } from '@/api/contract'
import { formatDateTime } from '@/utils/format'
import EmptyHint from '@/components/EmptyHint.vue'

const props = defineProps<{ contractId: string }>()

const rows = ref<ContractChangeLogVO[]>([])
const loading = ref(false)
let reqToken = 0

async function loadData() {
  if (!props.contractId) return
  const my = ++reqToken
  loading.value = true
  try {
    const data = await contractApi.changes(props.contractId, 100)
    if (my === reqToken) rows.value = data
  } finally {
    if (my === reqToken) loading.value = false
  }
}

watch(() => props.contractId, (id) => { if (id) loadData() }, { immediate: true })

function colorOf(field: string): 'primary' | 'success' | 'warning' | 'danger' {
  if (field === 'status') return 'success'
  if (field.includes('amount')) return 'warning'
  return 'primary'
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}
.row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.op {
  color: #909399;
  font-size: 12px;
}
.diff {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-top: 4px;
  font-size: 13px;
}
.old {
  color: #909399;
  text-decoration: line-through;
}
.new {
  color: #67c23a;
}
.reason {
  color: #c0c4cc;
  font-size: 12px;
  margin-top: 2px;
}
.text-muted {
  color: #909399;
}
</style>
