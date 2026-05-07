<template>
  <el-container class="app-layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
      <div class="logo">{{ collapsed ? 'C' : 'CRMS 合同回款' }}</div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        router
        background-color="#001529"
        text-color="#cfd5dc"
        active-text-color="#fff"
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <el-menu-item v-if="!visibleChildren(route).length" :index="'/' + route.path">
            <el-icon><component :is="route.meta?.icon || 'Menu'" /></el-icon>
            <template #title>{{ route.meta?.title }}</template>
          </el-menu-item>
          <el-sub-menu v-else :index="'/' + route.path">
            <template #title>
              <el-icon><component :is="route.meta?.icon || 'Menu'" /></el-icon>
              <span>{{ route.meta?.title }}</span>
            </template>
            <el-menu-item
              v-for="child in visibleChildren(route)"
              :key="child.path"
              :index="('/' + route.path + '/' + child.path).replace(/\/+/g, '/')"
            >
              {{ child.meta?.title }}
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-icon class="toggle" @click="collapsed = !collapsed">
          <Fold v-if="!collapsed" />
          <Expand v-else />
        </el-icon>
        <el-breadcrumb separator="/" class="breadcrumb">
          <el-breadcrumb-item v-for="b in breadcrumbs" :key="b">{{ b }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="spacer" />
        <NotificationBell />
        <el-dropdown @command="onCommand">
          <span class="user">
            <el-avatar :size="28">
              {{ userInitials }}
            </el-avatar>
            <span class="name">{{ auth.user?.realName || auth.user?.username }}</span>
            <el-icon><CaretBottom /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="changePwd">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <keep-alive :max="10">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { CaretBottom, Expand, Fold } from '@element-plus/icons-vue'
import { businessRoutes } from '@/router'
import { useAuthStore } from '@/stores/auth'
import NotificationBell from '@/components/NotificationBell.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const collapsed = ref(false)

const menuRoutes = computed(() => {
  const root = businessRoutes[0]
  if (!root.children) return []
  return root.children
    .filter((r) => !r.meta?.hidden)
    .filter((r) => !r.meta?.perm || auth.hasPermission(r.meta.perm as string))
})

function visibleChildren(r: any) {
  if (!r.children) return []
  return r.children.filter(
    (c: any) => !c.meta?.hidden && (!c.meta?.perm || auth.hasPermission(c.meta.perm))
  )
}

const activeMenu = computed(() => {
  const segs = route.path.split('/').filter(Boolean)
  if (!segs.length) return '/dashboard'
  const root = businessRoutes[0].children || []
  const top: any = root.find((r: any) => r.path === segs[0])
  if (top?.children?.length && segs[1]) {
    return '/' + segs.slice(0, 2).join('/')
  }
  return '/' + segs[0]
})

const breadcrumbs = computed(() => route.matched.map((m) => m.meta?.title).filter(Boolean))

const userInitials = computed(() => {
  const n = auth.user?.realName || auth.user?.username || 'U'
  return n.slice(0, 1).toUpperCase()
})

function onCommand(cmd: string) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
      .then(async () => {
        await auth.logout()
        router.push('/login')
      })
      .catch(() => {})
  } else if (cmd === 'changePwd') {
    router.push('/system/change-password')
  }
}
</script>

<style scoped lang="scss">
.app-layout {
  height: 100vh;
}

.aside {
  background: #001529;
  transition: width 0.2s;
  overflow: hidden;
  :deep(.el-menu) {
    border-right: none;
  }
  .logo {
    height: 56px;
    line-height: 56px;
    text-align: center;
    color: #fff;
    font-weight: 600;
    background: #002140;
    overflow: hidden;
    white-space: nowrap;
  }
}

.header {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 16px;
  gap: 12px;
  .toggle {
    cursor: pointer;
    font-size: 18px;
  }
  .breadcrumb {
    font-size: 13px;
  }
  .spacer {
    flex: 1;
  }
  .header-icon {
    cursor: pointer;
    margin-right: 8px;
    color: #606266;
  }
  .user {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    .name {
      font-size: 13px;
    }
  }
}

.main {
  background: #f5f7fa;
  padding: 0;
}
</style>
