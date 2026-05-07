<template>
  <div class="system-home">
    <router-view v-if="hasChild" v-slot="{ Component }">
      <keep-alive :max="6">
        <component :is="Component" />
      </keep-alive>
    </router-view>
    <div v-else class="crms-page">
      <el-card class="crms-card" shadow="never">
        <h3 style="margin: 0 0 12px;">系统管理</h3>
        <p style="color: #909399; margin-top: 0;">请从左侧菜单或下方卡片选择要管理的内容。</p>

        <div class="menu-grid">
          <el-card
            v-for="m in menus"
            :key="m.path"
            class="menu-card"
            shadow="hover"
            @click="$router.push(m.path)"
          >
            <div class="card-inner">
              <el-icon :size="28" class="icon"><component :is="m.icon" /></el-icon>
              <div>
                <div class="title">{{ m.title }}</div>
                <div class="desc">{{ m.desc }}</div>
              </div>
            </div>
          </el-card>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { User, Avatar, OfficeBuilding, Setting, Document, Delete } from '@element-plus/icons-vue'

const route = useRoute()
const hasChild = computed(() => route.path !== '/system' && route.path !== '/system/')

const menus = [
  { path: '/system/users', title: '用户管理', desc: '账号 / 角色 / 启停', icon: User },
  { path: '/system/roles', title: '角色管理', desc: '角色 / 权限点', icon: Avatar },
  { path: '/system/departments', title: '部门管理', desc: '组织树', icon: OfficeBuilding },
  { path: '/system/params', title: '系统参数', desc: '配置项', icon: Setting },
  { path: '/system/logs', title: '操作日志', desc: '审计追踪', icon: Document },
  { path: '/system/recycle', title: '回收站', desc: '已删数据还原', icon: Delete }
]
</script>

<style scoped lang="scss">
.system-home {
  height: 100%;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.menu-card {
  cursor: pointer;
  transition: transform 0.15s ease;
  &:hover {
    transform: translateY(-2px);
  }
}

.card-inner {
  display: flex;
  align-items: center;
  gap: 14px;
  .icon {
    color: #165dff;
    flex-shrink: 0;
  }
  .title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }
  .desc {
    font-size: 12px;
    color: #909399;
    margin-top: 2px;
  }
}
</style>
