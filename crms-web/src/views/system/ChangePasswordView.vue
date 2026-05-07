<template>
  <div class="change-pwd">
    <el-card class="pwd-card">
      <template #header>
        <div class="card-header">
          <span class="title">修改密码</span>
          <el-tag v-if="forced" type="warning" size="small" effect="light">首次登录必须修改</el-tag>
        </div>
      </template>

      <el-alert
        v-if="forced"
        type="warning"
        show-icon
        :closable="false"
        title="为了账户安全，请将初始密码修改为只有您知道的强密码后再继续使用系统。"
        class="alert"
      />

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="onSubmit"
      >
        <el-form-item prop="oldPassword" label="当前密码">
          <el-input
            v-model="form.oldPassword"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="请输入当前密码"
          />
        </el-form-item>

        <el-form-item prop="newPassword" label="新密码">
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="8-64 位，建议包含大小写字母、数字与符号"
          />
        </el-form-item>

        <el-form-item prop="confirmPassword" label="确认新密码">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="再次输入新密码"
          />
        </el-form-item>

        <div class="actions">
          <el-button v-if="!forced" @click="onCancel">取消</el-button>
          <el-button type="primary" :loading="loading" @click="onSubmit">提交</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const forced = computed(() => !!auth.user?.forceChangePassword)

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '新密码长度 8-64 位', trigger: 'blur' },
    {
      validator: (_rule, value: string, cb) => {
        if (value && value === form.oldPassword) {
          cb(new Error('新密码不能与当前密码相同'))
        } else {
          cb()
        }
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, cb) => {
        if (value !== form.newPassword) {
          cb(new Error('两次输入的密码不一致'))
        } else {
          cb()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await authApi.changePassword({
        oldPassword: form.oldPassword,
        newPassword: form.newPassword
      })
      ElMessage.success('密码修改成功')
      await auth.fetchMe()
      router.push('/dashboard')
    } finally {
      loading.value = false
    }
  })
}

function onCancel() {
  router.back()
}
</script>

<style scoped lang="scss">
.change-pwd {
  display: flex;
  justify-content: center;
  padding: 32px 16px;
}

.pwd-card {
  width: 100%;
  max-width: 480px;
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    .title {
      font-size: 16px;
      font-weight: 600;
    }
  }
}

.alert {
  margin-bottom: 16px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>
