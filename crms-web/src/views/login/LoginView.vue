<template>
  <div class="login-bg">
    <el-card class="login-card">
      <div class="brand">
        <h1>合同回款管理系统</h1>
        <p class="subtitle">CRMS · 公司内部</p>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="onSubmit"
      >
        <el-form-item prop="username" label="账号">
          <el-input v-model="form.username" autocomplete="username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item prop="password" label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="submit" @click="onSubmit">
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await auth.login(form.username, form.password)
      ElMessage.success('登录成功')
      if (res.forceChangePassword) {
        router.push('/system/change-password')
      } else {
        const redirect = (route.query.redirect as string) || '/'
        router.push(redirect)
      }
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.login-bg {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #165dff 0%, #0e2a99 100%);
}

.login-card {
  width: 380px;
  padding: 32px 24px 24px;
  border-radius: 8px;
  .brand {
    text-align: center;
    margin-bottom: 24px;
    h1 {
      margin: 0;
      font-size: 20px;
      color: #303133;
    }
    .subtitle {
      color: #909399;
      font-size: 12px;
      margin-top: 4px;
    }
  }
  .submit {
    width: 100%;
  }
}
</style>
