<template>
  <el-popover placement="bottom-end" :width="360" trigger="click" @show="loadTop">
    <template #reference>
      <el-badge :value="count" :hidden="!count" :max="99" class="bell-badge">
        <el-icon size="20" class="header-icon">
          <Bell />
        </el-icon>
      </el-badge>
    </template>
    <div class="bell-pop">
      <div class="head">
        <strong>未读通知 {{ count }}</strong>
        <el-link type="primary" :underline="false" @click="onMarkAll">全部已读</el-link>
      </div>
      <el-empty v-if="!items.length" description="暂无未读" :image-size="60" />
      <ul v-else class="list">
        <li v-for="n in items" :key="n.id" class="item" @click="onClick(n)">
          <div class="title">{{ n.title }}</div>
          <div class="content">{{ n.content }}</div>
          <div class="time">{{ n.createdAt }}</div>
        </li>
      </ul>
      <div class="foot">
        <el-link type="primary" :underline="false" @click="goMore">查看全部 &rarr;</el-link>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { notificationApi, type NotificationVO } from '@/api/notification'

const router = useRouter()
const count = ref(0)
const items = ref<NotificationVO[]>([])
let timer: ReturnType<typeof setInterval> | null = null

async function refreshCount() {
  try {
    const r = await notificationApi.unreadCount()
    count.value = r.count
  } catch {
    /* ignore */
  }
}

async function loadTop() {
  try {
    items.value = await notificationApi.top(5)
  } catch {
    /* ignore */
  }
}

async function onMarkAll() {
  await notificationApi.markAllRead()
  count.value = 0
  items.value = []
}

async function onClick(n: NotificationVO) {
  await notificationApi.markRead(n.id).catch(() => null)
  if (n.linkUrl) {
    router.push(n.linkUrl)
  } else {
    router.push('/notifications')
  }
  refreshCount()
}

function goMore() {
  router.push('/notifications')
}

onMounted(() => {
  refreshCount()
  timer = setInterval(refreshCount, 60_000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.bell-badge {
  cursor: pointer;
  margin-right: 12px;
}
.header-icon {
  cursor: pointer;
  color: #606266;
}
.bell-pop {
  display: flex;
  flex-direction: column;
  max-height: 460px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1;
  overflow-y: auto;
  max-height: 320px;
}
.item {
  padding: 8px 4px;
  cursor: pointer;
  border-bottom: 1px dashed #f0f0f0;
}
.item:hover {
  background: #fafbfc;
}
.title {
  font-weight: 600;
  color: #303133;
  font-size: 13px;
}
.content {
  color: #606266;
  font-size: 12px;
  margin: 2px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.time {
  color: #c0c4cc;
  font-size: 12px;
}
.foot {
  text-align: center;
  padding-top: 6px;
  border-top: 1px solid #ebeef5;
}
</style>
