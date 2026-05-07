<template>
  <div class="crms-page">
    <el-card class="crms-card" shadow="never">
      <el-tabs v-model="tab">
        <el-tab-pane label="收件箱" name="inbox">
          <el-form inline class="search-bar">
            <el-form-item label="场景">
              <el-select v-model="query.scene" placeholder="全部" clearable style="width: 160px" @change="loadData">
                <el-option v-for="(meta, key) in NotificationScene" :key="key" :label="meta.label" :value="key" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="query.unreadOnly" @change="loadData">仅未读</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadData">查询</el-button>
              <el-button @click="onMarkAll">全部已读</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="loading" :data="rows" stripe border>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="!row.isRead" type="primary" size="small">未读</el-tag>
                <el-tag v-else type="info" size="small">已读</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="场景" width="120">
              <template #default="{ row }">
                {{ NotificationScene[row.scene]?.label || row.scene }}
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
            <el-table-column prop="content" label="内容" min-width="320" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column label="操作" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <el-button v-if="row.linkUrl" link size="small" @click="onClick(row)">打开</el-button>
                <el-button v-if="!row.isRead" link size="small" @click="onRead(row)">标记已读</el-button>
                <el-button link size="small" type="info" @click="onArchive(row)">归档</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            background
            layout="total, sizes, prev, pager, next, jumper"
            class="mt-2"
            @current-change="loadData"
            @size-change="loadData"
          />
        </el-tab-pane>

        <el-tab-pane label="通知偏好" name="settings">
          <el-table :data="settingRows" border>
            <el-table-column label="场景" width="200">
              <template #default="{ row }">{{ NotificationScene[row.scene]?.label || row.scene }}</template>
            </el-table-column>
            <el-table-column label="启用">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.enabled === 1"
                  @update:model-value="(v) => (row.enabled = v ? 1 : 0)"
                />
              </template>
            </el-table-column>
            <el-table-column label="提前天数">
              <template #default="{ row }">
                <el-input-number v-model="row.advanceDays" :min="0" :max="365" />
              </template>
            </el-table-column>
          </el-table>
          <div class="actions">
            <el-button type="primary" @click="onSaveSettings">保存偏好</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  notificationApi,
  NotificationScene,
  type NotificationVO,
  type NotificationSettingVO
} from '@/api/notification'

const router = useRouter()
const tab = ref<'inbox' | 'settings'>('inbox')

const query = reactive<{ page: number; size: number; scene?: string; unreadOnly?: boolean }>({
  page: 1,
  size: 20
})

const rows = ref<NotificationVO[]>([])
const total = ref(0)
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await notificationApi.list(query)
    rows.value = res.items
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function onClick(row: NotificationVO) {
  if (!row.isRead) await notificationApi.markRead(row.id).catch(() => null)
  if (row.linkUrl) router.push(row.linkUrl)
}

async function onRead(row: NotificationVO) {
  await notificationApi.markRead(row.id)
  loadData()
}

async function onArchive(row: NotificationVO) {
  await notificationApi.archive(row.id)
  ElMessage.success('已归档')
  loadData()
}

async function onMarkAll() {
  const r = await notificationApi.markAllRead()
  ElMessage.success(`已标记 ${r.affected} 条`)
  loadData()
}

const settingRows = ref<(NotificationSettingVO & { scene: string })[]>([])

async function loadSettings() {
  const list = await notificationApi.settings()
  const map: Record<string, NotificationSettingVO> = {}
  list.forEach((s) => (map[s.scene] = s))
  settingRows.value = Object.keys(NotificationScene).map((scene) => ({
    scene,
    enabled: map[scene]?.enabled ?? 1,
    advanceDays: map[scene]?.advanceDays ?? NotificationScene[scene].defaultAdvance
  }))
}

async function onSaveSettings() {
  const payload: Record<string, NotificationSettingVO> = {}
  settingRows.value.forEach((r) => {
    payload[r.scene] = { scene: r.scene, enabled: r.enabled, advanceDays: r.advanceDays }
  })
  await notificationApi.saveSettings(payload)
  ElMessage.success('已保存')
}

watch(tab, (v) => {
  if (v === 'settings' && !settingRows.value.length) loadSettings()
})

onMounted(loadData)
</script>

<style scoped>
.search-bar {
  margin-bottom: 8px;
}
.mt-2 {
  margin-top: 12px;
}
.actions {
  margin-top: 12px;
}
</style>
