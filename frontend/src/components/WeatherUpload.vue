<script setup lang="ts">
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'

const props = defineProps<{
  loading: boolean
  onUpload: (file: File) => Promise<void>
}>()

const submitUpload = async (options: UploadRequestOptions) => {
  await props.onUpload(options.file)
}

const beforeUpload = (file: File) => {
  if (!file.name.toLowerCase().endsWith('.csv')) {
    ElMessage.error('请选择 .csv 文件')
    return false
  }
  return true
}
</script>

<template>
  <section class="upload-panel">
    <div class="upload-copy">
      <span class="eyebrow">DATA IMPORT</span>
      <h2>上传历史气温数据</h2>
      <p>使用中文表头“日期、地区、气温”，系统将自动校验并计算月度距平。</p>
      <div class="format-chip">CSV · UTF-8 · 最大 10MB</div>
    </div>
    <el-upload
      class="weather-uploader"
      drag
      accept=".csv,text/csv"
      :show-file-list="false"
      :http-request="submitUpload"
      :before-upload="beforeUpload"
      :disabled="loading"
    >
      <el-icon class="upload-icon"><UploadFilled /></el-icon>
      <div class="el-upload__text">
        <strong>{{ loading ? '正在分析数据…' : '拖拽 CSV 到这里' }}</strong>
        <span>{{ loading ? '请稍候，计算完成后页面将自动刷新' : '或点击选择本地文件' }}</span>
      </div>
    </el-upload>
  </section>
</template>
