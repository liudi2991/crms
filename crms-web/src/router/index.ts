import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import nprogress from 'nprogress'
import { useAuthStore } from '@/stores/auth'

const MainLayout = () => import('@/layouts/MainLayout.vue')

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { public: true }
  }
]

export const businessRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '综合看板', icon: 'Odometer' }
      },
      {
        path: 'system/change-password',
        name: 'ChangePassword',
        component: () => import('@/views/system/ChangePasswordView.vue'),
        meta: { title: '修改密码', hidden: true }
      },
      {
        path: 'customers',
        name: 'CustomerList',
        component: () => import('@/views/customer/CustomerList.vue'),
        meta: { title: '客户档案', icon: 'User', perm: 'customer:list' }
      },
      {
        path: 'customers/:id',
        name: 'CustomerDetail',
        component: () => import('@/views/customer/CustomerDetail.vue'),
        meta: { title: '客户详情', hidden: true, perm: 'customer:list' }
      },
      {
        path: 'contracts',
        name: 'ContractList',
        component: () => import('@/views/contract/ContractList.vue'),
        meta: { title: '合同管理', icon: 'Document', perm: 'contract:list' }
      },
      {
        path: 'contracts/:id',
        name: 'ContractDetail',
        component: () => import('@/views/contract/ContractDetail.vue'),
        meta: { title: '合同详情', hidden: true, perm: 'contract:list' }
      },
      {
        path: 'payments',
        name: 'PaymentList',
        component: () => import('@/views/payment/PaymentList.vue'),
        meta: { title: '回款管理', icon: 'Wallet', perm: 'payment:list' }
      },
      {
        path: 'payments/aging',
        name: 'PaymentAging',
        component: () => import('@/views/payment/AgingView.vue'),
        // 账龄接口要求 report:payment，与菜单可见性保持一致
        meta: { title: '账龄分析', perm: 'report:payment' }
      },
      {
        path: 'reports',
        name: 'Report',
        component: () => import('@/views/report/ReportView.vue'),
        // 报表中心默认 tab 是月度趋势（report:dashboard），无该权限的角色（如 R01 销售）不展示菜单
        meta: { title: '报表中心', icon: 'TrendCharts', perm: 'report:dashboard' }
      },
      {
        path: 'notifications',
        name: 'Notification',
        component: () => import('@/views/notification/NotificationList.vue'),
        meta: { title: '通知中心', icon: 'Bell' }
      },
      {
        path: 'system',
        name: 'System',
        component: () => import('@/views/system/SystemHome.vue'),
        meta: { title: '系统管理', icon: 'Setting', perm: 'system:manage' },
        children: [
          {
            path: 'users',
            name: 'UserManage',
            component: () => import('@/views/system/UserManage.vue'),
            meta: { title: '用户管理', perm: 'system:manage' }
          },
          {
            path: 'roles',
            name: 'RoleManage',
            component: () => import('@/views/system/RoleManage.vue'),
            meta: { title: '角色管理', perm: 'system:manage' }
          },
          {
            path: 'departments',
            name: 'DeptManage',
            component: () => import('@/views/system/DeptManage.vue'),
            meta: { title: '部门管理', perm: 'system:manage' }
          },
          {
            path: 'params',
            name: 'ParamManage',
            component: () => import('@/views/system/ParamManage.vue'),
            meta: { title: '系统参数', perm: 'system:manage' }
          },
          {
            path: 'logs',
            name: 'OperationLog',
            component: () => import('@/views/system/OperationLogView.vue'),
            meta: { title: '操作日志', perm: 'system:manage' }
          },
          {
            path: 'recycle',
            name: 'RecycleBin',
            component: () => import('@/views/system/RecycleBin.vue'),
            meta: { title: '回收站', perm: 'system:manage' }
          }
        ]
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/404' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes: [...constantRoutes, ...businessRoutes]
})

router.beforeEach(async (to, _from, next) => {
  nprogress.start()
  const auth = useAuthStore()
  if (to.meta.public) {
    next()
    return
  }
  if (!auth.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (!auth.loaded) {
    try {
      await auth.fetchMe()
    } catch {
      auth.reset()
      next({ path: '/login' })
      return
    }
  }
  if (auth.user?.forceChangePassword && to.path !== '/system/change-password') {
    next({ path: '/system/change-password' })
    return
  }
  const perm = to.meta.perm as string | undefined
  if (perm && !auth.hasPermission(perm)) {
    next({ path: '/404' })
    return
  }
  next()
})

router.afterEach(() => nprogress.done())

export default router
