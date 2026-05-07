<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="380px"
    :close-on-click-modal="false"
    align-center
    @open="onOpen"
  >
    <p class="warn">{{ message }}</p>
    <el-input
      ref="inputRef"
      v-model="password"
      type="password"
      placeholder="当前账号密码"
      show-password
      @keyup.enter="onConfirm"
    />
    <p v-if="error" class="error">{{ error }}</p>
    <template #footer>
      <el-button @click="onCancel">取消</el-button>
      <el-button type="danger" :loading="verifying" @click="onConfirm">{{ confirmText }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { securityApi } from '@/api/customer'
import type { ElInput } from 'element-plus'

const props = withDefaults(
  defineProps<{
    title?: string
    message?: string
    confirmText?: string
  }>(),
  {
    title: '二次密码验证',
    message: '此操作不可逆，请输入您的登录密码以确认。',
    confirmText: '确认'
  }
)

const visible = ref(false)
const password = ref('')
const error = ref('')
const verifying = ref(false)
const inputRef = ref<InstanceType<typeof ElInput>>()

let resolveFn: ((ok: boolean) => void) | null = null

function open(): Promise<boolean> {
  visible.value = true
  password.value = ''
  error.value = ''
  return new Promise((res) => {
    resolveFn = res
  })
}

function onOpen() {
  nextTick(() => inputRef.value?.focus())
}

async function onConfirm() {
  if (!password.value) {
    error.value = '请输入密码'
    return
  }
  verifying.value = true
  try {
    const res = await securityApi.verifyPassword(password.value)
    if (res.ok) {
      visible.value = false
      resolveFn?.(true)
      resolveFn = null
    } else {
      error.value = '密码错误'
    }
  } finally {
    verifying.value = false
  }
}

function onCancel() {
  visible.value = false
  resolveFn?.(false)
  resolveFn = null
}

defineExpose({ open })
</script>

<style scoped>
.warn {
  color: #e6a23c;
  margin-top: 0;
  font-size: 13px;
}
.error {
  color: #f56c6c;
  font-size: 12px;
  margin: 6px 0 0;
}
</style>
