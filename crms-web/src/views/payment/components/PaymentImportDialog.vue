<template>
  <el-dialog
    :model-value="visible"
    title="批量导入实际回款"
    width="640px"
    @update:model-value="(v: boolean) => emit('update:visible', v)"
  >
    <el-alert type="info" :closable="false" show-icon>
      Excel 列顺序：合同编号 / 到账日期 (yyyy-MM-dd) / 金额 / 付款方 / 凭证号 / 备注。第一行为表头。
    </el-alert>

    <el-upload
      drag
      class="mt-2"
      :show-file-list="false"
      :http-request="onUpload"
      :before-upload="beforeUpload"
      accept=".xls,.xlsx"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">
        将 Excel 文件拖到此处，或<em>点击上传</em>
      </div>
    </el-upload>

    <div v-if="uploading" class="mt-2">
      <el-progress :percentage="100" :indeterminate="true" />
      正在上传与解析...
    </div>

    <div v-if="result" class="mt-2">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="总条数">{{ result.total }}</el-descriptions-item>
        <el-descriptions-item label="成功">
          <el-tag type="success">{{ result.success }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="失败">
          <el-tag type="danger">{{ result.failed }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-table v-if="result.errors.length" :data="result.errors" class="mt-2" border size="small">
        <el-table-column prop="row" label="行号" width="80" />
        <el-table-column prop="message" label="错误" />
      </el-table>
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { paymentRecordApi, type ImportResultVO } from '@/api/payment'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{ 'update:visible': [boolean]; finished: [] }>()

const uploading = ref(false)
const result = ref<ImportResultVO | null>(null)

watch(
  () => props.visible,
  (v) => {
    if (v) {
      result.value = null
    }
  }
)

function beforeUpload(file: File) {
  if (!/\.xls[x]?$/i.test(file.name)) {
    ElMessage.error('仅支持 .xls / .xlsx 文件')
    return false
  }
  return true
}

async function onUpload(opt: UploadRequestOptions) {
  uploading.value = true
  result.value = null
  try {
    result.value = await paymentRecordApi.importExcel(opt.file as File)
    if (result.value.failed === 0) {
      ElMessage.success(`已导入 ${result.value.success} 条记录`)
    } else {
      ElMessage.warning(`导入完成：成功 ${result.value.success}、失败 ${result.value.failed}`)
    }
    emit('finished')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.mt-2 {
  margin-top: 12px;
}
</style>
