<template>
  <el-dialog
    v-model="visible"
    title="客户合并"
    width="640px"
    :close-on-click-modal="false"
  >
    <el-alert type="warning" :closable="false" show-icon class="mb-2">
      合并后被合并客户的合同 / 联系人将归属到主体客户，被合并客户标记为 MERGED 并进入回收站。该操作不可撤销。
    </el-alert>

    <el-form label-width="100px">
      <el-form-item label="主体客户">
        <el-select v-model="mainId" placeholder="选择保留的客户" style="width: 100%">
          <el-option
            v-for="c in candidates"
            :key="c.id"
            :label="`${c.name}（${c.code}）`"
            :value="c.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="待合并">
        <ul class="merged-list">
          <li v-for="c in mergedList" :key="c.id">
            <span>{{ c.name }} <span class="code">{{ c.code }}</span></span>
            <el-tag v-if="c.id === mainId" type="success" size="small">保留</el-tag>
            <el-tag v-else type="warning" size="small">合并</el-tag>
          </li>
        </ul>
      </el-form-item>

      <el-form-item label="合并原因" required>
        <el-input v-model="reason" type="textarea" :rows="2" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="danger" :loading="saving" @click="onConfirm">确认合并</el-button>
    </template>

    <PasswordPromptDialog
      ref="pwdRef"
      title="二次确认"
      message="客户合并不可撤销，请输入登录密码确认。"
      confirm-text="确认合并"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { customerApi, type CustomerVO } from '@/api/customer'
import PasswordPromptDialog from '@/components/PasswordPromptDialog.vue'

const props = defineProps<{
  modelValue: boolean
  candidates: CustomerVO[]
}>()
const emit = defineEmits<{
  'update:modelValue': [v: boolean]
  saved: []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const mainId = ref<string | null>(null)
const reason = ref('')
const saving = ref(false)
const pwdRef = ref<InstanceType<typeof PasswordPromptDialog>>()

const mergedList = computed(() => props.candidates)

watch(
  () => props.modelValue,
  (v) => {
    if (v && props.candidates.length > 0) {
      mainId.value = props.candidates[0].id
      reason.value = ''
    }
  }
)

async function onConfirm() {
  if (!mainId.value) {
    ElMessage.warning('请选择主体客户')
    return
  }
  if (!reason.value.trim()) {
    ElMessage.warning('请填写合并原因')
    return
  }
  const ok = await pwdRef.value?.open()
  if (!ok) return

  const mergedIds = props.candidates.map((c) => c.id).filter((id) => id !== mainId.value)
  saving.value = true
  try {
    await customerApi.merge({
      mainId: mainId.value,
      mergedIds,
      reason: reason.value
    })
    ElMessage.success(`已合并 ${mergedIds.length} 个客户到主体`)
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.mb-2 {
  margin-bottom: 12px;
}
.merged-list {
  list-style: none;
  margin: 0;
  padding: 0;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.merged-list li {
  display: flex;
  justify-content: space-between;
  padding: 6px 12px;
  border-bottom: 1px solid #ebeef5;
}
.merged-list li:last-child {
  border-bottom: none;
}
.code {
  color: #909399;
  font-size: 12px;
}
</style>
