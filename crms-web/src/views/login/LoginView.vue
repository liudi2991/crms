<template>
  <div class="login-bg">
    <!-- 背景装饰：两个柔和的发光圆点 -->
    <div class="bg-glow bg-glow--a" />
    <div class="bg-glow bg-glow--b" />

    <div class="login-shell">
      <!-- 左侧品牌区 -->
      <div class="brand-side">
        <div class="brand-mark">
          <el-icon><Promotion /></el-icon>
        </div>
        <h1 class="brand-title">合同回款管理系统</h1>
        <p class="brand-tagline">
          统一管理客户、合同、回款全流程<br />
          为你的销售团队保驾护航
        </p>
        <ul class="brand-bullets">
          <li><el-icon><Check /></el-icon>全链路客户与合同管理</li>
          <li><el-icon><Check /></el-icon>回款计划 / 实际回款 / 账龄</li>
          <li><el-icon><Check /></el-icon>多角色权限与数据隔离</li>
        </ul>
      </div>

      <!-- 右侧登录卡 -->
      <el-card class="login-card" shadow="never">
        <h2 class="card-title">欢迎登录</h2>
        <p class="card-sub">请输入账号和密码</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @keyup.enter="onSubmit"
        >
          <el-form-item prop="username" label="账号">
            <el-input
              v-model="form.username"
              :prefix-icon="User"
              autocomplete="username"
              placeholder="请输入账号"
            />
          </el-form-item>
          <el-form-item prop="password" label="密码">
            <el-input
              v-model="form.password"
              type="password"
              :prefix-icon="Lock"
              show-password
              autocomplete="current-password"
              placeholder="请输入密码"
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="submit"
            @click="onSubmit"
          >
            登 录
          </el-button>
        </el-form>

        <div class="footer-hint">CRMS · 公司内部 v1.0.0</div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Check, Lock, Promotion, User } from '@element-plus/icons-vue'
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
  position: relative;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0e2a99 0%, #165dff 55%, #1677ff 100%);
  overflow: hidden;
}

.bg-glow {
  position: absolute;
  width: 480px;
  height: 480px;
  border-radius: 50%;
  filter: blur(120px);
  opacity: 0.45;
  pointer-events: none;

  &--a {
    top: -120px;
    left: -120px;
    background: #4096ff;
  }
  &--b {
    bottom: -160px;
    right: -120px;
    background: #69b1ff;
  }
}

.login-shell {
  position: relative;
  z-index: 1;
  display: flex;
  width: 920px;
  max-width: calc(100vw - 32px);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

/* 左侧品牌区：在窄屏（< 880px）下隐藏 */
.brand-side {
  flex: 1;
  padding: 56px 48px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;

  .brand-mark {
    width: 56px;
    height: 56px;
    border-radius: 14px;
    background: rgba(255, 255, 255, 0.16);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    color: #fff;
    margin-bottom: 28px;
  }

  .brand-title {
    margin: 0 0 12px;
    font-size: 28px;
    font-weight: 600;
    letter-spacing: 0.5px;
  }

  .brand-tagline {
    color: rgba(255, 255, 255, 0.78);
    font-size: 14px;
    line-height: 1.7;
    margin: 0 0 32px;
  }

  .brand-bullets {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;

    li {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      color: rgba(255, 255, 255, 0.85);

      .el-icon {
        font-size: 16px;
        padding: 3px;
        background: rgba(255, 255, 255, 0.16);
        border-radius: 50%;
        color: #b6e3ff;
      }
    }
  }
}

.login-card {
  width: 420px;
  flex-shrink: 0;
  border: none !important;
  border-radius: 0 !important;
  background: #fff;

  :deep(.el-card__body) {
    padding: 56px 48px;
  }

  .card-title {
    margin: 0 0 6px;
    font-size: 22px;
    font-weight: 600;
    color: #1d2129;
  }
  .card-sub {
    margin: 0 0 28px;
    color: #86909c;
    font-size: 13px;
  }

  :deep(.el-form-item__label) {
    font-size: 13px;
    color: #4e5969;
    padding-bottom: 4px;
  }

  .submit {
    width: 100%;
    height: 44px;
    font-size: 15px;
    font-weight: 500;
    margin-top: 8px;
    background: linear-gradient(135deg, #1677ff 0%, #0e5fcc 100%);
    border: none;
    letter-spacing: 4px;

    &:hover {
      background: linear-gradient(135deg, #4096ff 0%, #1677ff 100%);
    }
  }

  .footer-hint {
    text-align: center;
    margin-top: 24px;
    color: #c0c4cc;
    font-size: 12px;
  }
}

@media (max-width: 880px) {
  .brand-side {
    display: none;
  }
  .login-shell {
    width: 420px;
  }
}
</style>
