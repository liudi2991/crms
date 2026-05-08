<template>
  <div>
    <div class="toolbar">
      <el-upload
        v-perm="'contract:update'"
        :show-file-list="false"
        :http-request="onUpload"
        :before-upload="beforeUpload"
        :disabled="uploading || rows.length >= MAX_COUNT"
      >
        <el-button type="primary" :icon="Upload" :loading="uploading">
          上传附件 ({{ rows.length }}/{{ MAX_COUNT }})
        </el-button>
      </el-upload>
      <el-button :icon="Refresh" circle @click="loadData" />
      <span class="text-muted">最大 50MB / 单文件，扩展名：pdf doc docx xls xlsx ppt pptx jpg png zip rar txt</span>
    </div>

    <EmptyHint v-if="!rows.length && !loading" description="暂无附件" />
    <el-table v-else v-loading="loading" :data="rows" border>
      <el-table-column prop="fileName" label="文件名" min-width="280" show-overflow-tooltip />
      <el-table-column label="大小" width="120" align="right">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="上传时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.uploadedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link size="small" @click="onPreview(row)">预览/下载</el-button>
          <el-popconfirm :title="`确认删除 ${row.fileName}？`" @confirm="onDelete(row)">
            <template #reference>
              <el-button v-perm="'contract:update'" link size="small">
                <span class="text-danger">删除</span>
              </el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import { Refresh, Upload } from '@element-plus/icons-vue'
import { contractAttachmentApi, type ContractAttachmentVO } from '@/api/contract'
import { tokenStore } from '@/api/http'
import { formatDateTime } from '@/utils/format'
import EmptyHint from '@/components/EmptyHint.vue'

const props = defineProps<{ contractId: string }>()

// 与后端系统参数 contract.attachment.max_count 默认值一致（SRS UC-03-05）
const MAX_COUNT = 20

const rows = ref<ContractAttachmentVO[]>([])
const loading = ref(false)
const uploading = ref(false)
// 请求 token：仅最后一次 loadData 的结果生效，避免快速切合同时旧请求覆盖新数据
let reqToken = 0

async function loadData() {
  if (!props.contractId) return
  const my = ++reqToken
  loading.value = true
  try {
    const data = await contractAttachmentApi.list(props.contractId)
    if (my === reqToken) rows.value = data
  } finally {
    if (my === reqToken) loading.value = false
  }
}

watch(() => props.contractId, (id) => { if (id) loadData() }, { immediate: true })

function beforeUpload(file: File) {
  const max = 50 * 1024 * 1024
  if (file.size > max) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  return true
}

async function onUpload(opt: UploadRequestOptions) {
  uploading.value = true
  try {
    await contractAttachmentApi.upload(props.contractId, opt.file as File)
    ElMessage.success('上传成功')
    loadData()
  } finally {
    uploading.value = false
  }
}

async function onPreview(row: ContractAttachmentVO) {
  const url = row.previewUrl
  if (!url) {
    ElMessage.error('预览地址为空')
    return
  }
  // MinIO 预签名 URL（http(s):// 开头）已携带 X-Amz-Signature，浏览器可直接打开
  if (/^https?:\/\//i.test(url)) {
    window.open(url, '_blank')
    return
  }
  // 本地存储模式：相对 URL /api/v1/files/.../preview 走后端鉴权，
  // 浏览器原生 navigation 不会带 token，所以这里手动加 Authorization 拉成 blob
  try {
    const res = await fetch(url, {
      headers: { Authorization: `Bearer ${tokenStore.get() ?? ''}` }
    })
    if (!res.ok) {
      ElMessage.error(`预览失败：${res.status}`)
      return
    }
    const blob = await res.blob()
    const blobUrl = URL.createObjectURL(blob)
    window.open(blobUrl, '_blank')
    setTimeout(() => URL.revokeObjectURL(blobUrl), 60_000)
  } catch (e) {
    ElMessage.error('预览失败：网络异常')
  }
}

async function onDelete(row: ContractAttachmentVO) {
  await contractAttachmentApi.remove(row.id)
  ElMessage.success('已删除')
  loadData()
}

// 注意：后端 JacksonConfig 把所有 Long 序列化为字符串（防 JS 精度丢失），
//      包括字节数这种不该转的字段；这里 Number() 兜底，避免 string.toFixed 抛错。
function formatSize(bytes: number | string | null | undefined) {
  const n = typeof bytes === 'number' ? bytes : Number(bytes)
  if (!Number.isFinite(n) || n <= 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let v = n
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return `${v.toFixed(v < 10 ? 1 : 0)} ${units[i]}`
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}
.text-muted {
  color: #909399;
  font-size: 12px;
}
</style>
