import type { App, DirectiveBinding } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * `v-perm="'contract:edit'"` 用法：
 * 当前用户没有该权限时移除元素。
 * 多权限用数组：`v-perm="['contract:edit','contract:delete']"`（任一即可）。
 */
export function setupPermissionDirective(app: App) {
  app.directive('perm', {
    mounted(el: HTMLElement, binding: DirectiveBinding) {
      const auth = useAuthStore()
      const perms = Array.isArray(binding.value) ? binding.value : [binding.value]
      const ok = perms.some((p) => typeof p === 'string' && auth.hasPermission(p))
      if (!ok) {
        el.parentNode?.removeChild(el)
      }
    }
  })
}
