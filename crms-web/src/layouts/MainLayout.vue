<template>
  <el-container class="app-layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
      <div class="logo" :class="{ 'is-collapsed': collapsed }">
        <el-icon class="logo-mark"><Promotion /></el-icon>
        <span v-if="!collapsed" class="logo-text">合同回款管理系统</span>
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
    /* 高度与顶部 el-header 完全一致（el-header 默认 60px + border-bottom 1px），
     * 否则侧栏 logo 下沿与主区域 header 下沿不在同一水平线，视觉上很别扭。 */
    height: 60px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    display: flex;
    align-items: center;
    /* 默认左对齐：飞机标 X 坐标对齐到下方菜单项 icon 的位置（约 24px），
     * 让 logo 与菜单内容形成同一条左基准线，比"居中 logo + 左对齐菜单"自然得多。 */
    justify-content: flex-start;
    gap: 10px;
    padding: 0 24px;
    color: #fff;
    font-weight: 600;
    background: #002140;
    overflow: hidden;
    white-space: nowrap;
    flex-shrink: 0;

    /* 折叠态：只剩飞机标，回归水平居中 */
    &.is-collapsed {
      justify-content: center;
      padding: 0;
    }

    .logo-mark {
      font-size: 22px;
      color: #4096ff;
      flex-shrink: 0;
    }
    .logo-text {
      font-size: 15px;
      letter-spacing: 0.3px;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .menu-scroll {
    flex: 1;
    :deep(.el-scrollbar__view) {
      height: 100%;
      padding: 8px 0;
    }
  }

  :deep(.side-menu) {
    border-right: none;
    background-color: #001529 !important;

    /* 所有菜单项（一级和二级）的胶囊背景统一用 ::before 伪元素实现，
     * 绕开 element-plus 给 .el-menu-item 加的 inline width: 100%
     * 与 padding-left，确保胶囊宽度可预测、左右各内缩 8px。
     *
     * isolation: isolate 在 li 内部建立一个新的 stacking context，
     * 这样 ::before z-index: -1 只会在 li 内部"沉到底"，不会跑到 li 后面。
     * 文本节点（element-plus 直接把标题作为 text node 渲染）就能盖住胶囊。
     */
    .el-menu-item,
    .el-sub-menu__title {
      position: relative;
      isolation: isolate;
      background-color: transparent !important;
      transition: color 0.15s;

      &::before {
        content: '';
        position: absolute;
        left: 8px;
        right: 8px;
        top: 4px;
        bottom: 4px;
        border-radius: 8px;
        background-color: transparent;
        transition: background-color 0.15s, box-shadow 0.15s;
        pointer-events: none;
        z-index: -1;
      }

      &:hover::before {
        background-color: rgba(255, 255, 255, 0.08);
      }
    }

    /* 一级菜单项尺寸 + 强制 padding-left 24px，
     * 让一级 icon 的 X 坐标与上方 logo 飞机标的 X 坐标对齐到同一垂直线。 */
    > .el-menu-item,
    > .el-sub-menu > .el-sub-menu__title {
      height: 48px !important;
      line-height: 48px !important;
      padding-left: 24px !important;
      padding-right: 16px !important;
      font-size: 14px;

      > .el-icon {
        font-size: 16px;
        margin-right: 10px;
        width: 16px;
      }
    }

    /* 一级激活态 */
    > .el-menu-item.is-active {
      color: #fff !important;
      font-weight: 500;

      &::before {
        background-color: #1677ff;
        box-shadow: 0 2px 8px rgba(22, 119, 255, 0.35);
      }
    }

    /* sub-menu 展开容器：背景比父级深一档 */
    .el-sub-menu .el-menu {
      background-color: #000c17 !important;
      padding: 4px 0 !important;
    }

    /* 二级菜单项：padding-left 在一级 24px 基础上再多 24px 形成层级缩进 */
    .el-sub-menu .el-menu-item {
      height: 40px !important;
      line-height: 40px !important;
      padding-left: 48px !important;
      padding-right: 16px !important;
      font-size: 13px;

      > .el-icon {
        font-size: 14px;
        margin-right: 8px;
        width: 14px;
        opacity: 0.75;
      }

      &.is-active {
        color: #fff !important;
        font-weight: 500;

        &::before {
          background-color: #1677ff;
          box-shadow: 0 2px 8px rgba(22, 119, 255, 0.35);
        }

        > .el-icon {
          opacity: 1;
        }
      }
    }

    /* 二级菜单项的 ::before 与一级保持一致的左右内缩 8px，
     * 但因为 sub-menu 容器底色已经更深，看起来层级清晰。 */

    /* sub-menu 展开后的标题箭头跟着白起来 */
    .el-sub-menu.is-active > .el-sub-menu__title {
      color: #fff !important;

      .el-sub-menu__icon-arrow {
        color: #fff !important;
      }
    }

    /* collapse 折叠状态：取消胶囊视觉，恢复 element-plus 默认居中 icon */
    &.el-menu--collapse {
      .el-menu-item,
      .el-sub-menu__title {
        &::before {
          left: 4px;
          right: 4px;
        }
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
