<template>
  <el-container class="app-layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
      <div class="logo">
        <el-icon class="logo-mark"><Promotion /></el-icon>
        <span v-if="!collapsed" class="logo-text">CRMS 合同回款</span>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          class="side-menu"
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
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
                <el-icon><component :is="child.meta?.icon || 'Minus'" /></el-icon>
                <template #title>{{ child.meta?.title }}</template>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-scrollbar>
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
import { CaretBottom, Expand, Fold, Promotion } from '@element-plus/icons-vue'
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

const breadcrumbs = computed<string[]>(() =>
  route.matched
    .map((m) => m.meta?.title)
    .filter((t): t is string => typeof t === 'string' && t.length > 0)
)

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
  display: flex;
  flex-direction: column;

  .logo {
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: #fff;
    font-weight: 600;
    background: #002140;
    overflow: hidden;
    white-space: nowrap;
    flex-shrink: 0;

    .logo-mark {
      font-size: 22px;
      color: #4096ff;
    }
    .logo-text {
      font-size: 16px;
      letter-spacing: 0.5px;
    }
  }

  .menu-scroll {
    flex: 1;
    /* 隐藏水平滚动条，保持菜单干净 */
    :deep(.el-scrollbar__view) {
      height: 100%;
    }
  }

  :deep(.side-menu) {
    border-right: none;
    background-color: #001529 !important;

    /* 一级菜单项 */
    .el-menu-item,
    .el-sub-menu__title {
      height: 44px;
      line-height: 44px;
      margin: 2px 8px;
      border-radius: 6px;
      transition: background-color 0.15s, color 0.15s;

      .el-icon {
        font-size: 16px;
        margin-right: 8px;
      }

      &:hover {
        background-color: rgba(255, 255, 255, 0.06) !important;
        color: #fff !important;
      }
    }

    /* 一级激活态：实色蓝底 + 加粗文字 */
    > .el-menu-item.is-active {
      background-color: #1677ff !important;
      color: #fff !important;
      font-weight: 500;
    }

    /* sub-menu 展开容器：背景比父级深一点（暗主题下显示二级层级感） */
    .el-sub-menu .el-menu {
      background-color: #000c17 !important;
      padding: 4px 0;
    }

    /* 二级菜单项：相对父级 sub-menu title 多 16px 缩进，
     * 覆盖 element-plus 默认通过 inline style padding-left 的行为，
     * 否则窄主题下视觉上会感觉子菜单"没有缩进"或反向偏移。
     */
    .el-sub-menu .el-menu-item {
      height: 38px;
      line-height: 38px;
      padding-left: 44px !important;
      margin: 2px 8px;
      font-size: 13px;

      .el-icon {
        font-size: 14px;
        margin-right: 6px;
        opacity: 0.85;
      }

      &.is-active {
        background-color: #1677ff !important;
        color: #fff !important;
        font-weight: 500;

        .el-icon {
          opacity: 1;
        }
      }
    }

    /* sub-menu title 处于"展开/激活后代"时高亮箭头 */
    .el-sub-menu.is-active > .el-sub-menu__title {
      color: #fff !important;

      .el-sub-menu__icon-arrow {
        color: #fff !important;
      }
    }

    /* collapse 折叠时把每项的左右 margin 抹平，避免出现"左缩进 + tooltip 错位" */
    &.el-menu--collapse {
      .el-menu-item,
      .el-sub-menu__title {
        margin: 2px 0;
        border-radius: 0;
      }
    }
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
