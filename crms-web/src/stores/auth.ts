import { defineStore } from 'pinia'
import { authApi, type MeResult } from '@/api/auth'
import { tokenStore } from '@/api/http'

interface AuthState {
  token: string | null
  user: MeResult | null
  permissions: Set<string>
  loaded: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: tokenStore.get(),
    user: null,
    permissions: new Set(),
    loaded: false
  }),

  getters: {
    isLoggedIn: (s) => !!s.token,
    superAdmin: (s) => s.user?.superAdmin ?? false
  },

  actions: {
    async login(username: string, password: string) {
      const res = await authApi.login({ username, password })
      tokenStore.set(res.token)
      this.token = res.token
      await this.fetchMe()
      return res
    },

    async fetchMe() {
      const me = await authApi.me()
      this.user = me
      this.permissions = new Set(me.permissions || [])
      this.loaded = true
    },

    async logout() {
      try {
        await authApi.logout()
      } catch {
        /* ignore */
      }
      this.reset()
    },

    reset() {
      tokenStore.clear()
      this.token = null
      this.user = null
      this.permissions.clear()
      this.loaded = false
    },

    hasPermission(code: string) {
      return this.user?.superAdmin || this.permissions.has(code)
    }
  }
})
