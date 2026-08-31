<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Download, Picture, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AnomalyChart from './components/AnomalyChart.vue'
import ResultTable from './components/ResultTable.vue'
import StatsOverview from './components/StatsOverview.vue'
import WeatherFilter from './components/WeatherFilter.vue'
import WeatherUpload from './components/WeatherUpload.vue'
import { exportResults, getErrorMessage, getMetadata, getResults, uploadWeatherCsv } from './api/weather'
import { emptyMetadata } from './types/weather'
import type { Metadata, TemperatureAnomaly, WeatherFilters } from './types/weather'

const metadata = ref<Metadata>(emptyMetadata())
const results = ref<TemperatureAnomaly[]>([])
const filters = ref<WeatherFilters>({ regions: [], startYear: null, endYear: null, month: null })
const uploadLoading = ref(false)
const resultsLoading = ref(false)
const exportLoading = ref(false)
const uploadError = ref('')
const chartRef = ref<InstanceType<typeof AnomalyChart>>()
let requestSequence = 0

const hasData = computed(() => metadata.value.rawRecordCount > 0)
const activeFilterCount = computed(() =>
  (filters.value.regions.length ? 1 : 0) +
  (filters.value.month ? 1 : 0) +
  ((filters.value.startYear !== metadata.value.minYear || filters.value.endYear !== metadata.value.maxYear) ? 1 : 0),
)

const refreshResults = async () => {
  if (!hasData.value) return
  const sequence = ++requestSequence
  resultsLoading.value = true
  try {
    const data = await getResults(filters.value)
    if (sequence === requestSequence) results.value = data
  } catch (error) {
    if (sequence === requestSequence) ElMessage.error(getErrorMessage(error))
  } finally {
    if (sequence === requestSequence) resultsLoading.value = false
  }
}

const handleUpload = async (file: File) => {
  uploadLoading.value = true
  uploadError.value = ''
  try {
    const uploadResult = await uploadWeatherCsv(file)
    metadata.value = await getMetadata()
    filters.value = {
      regions: [],
      startYear: metadata.value.minYear,
      endYear: metadata.value.maxYear,
      month: null,
    }
    await refreshResults()
    ElMessage.success(`上传成功，已生成 ${uploadResult.resultCount} 条分析结果`)
  } catch (error) {
    uploadError.value = getErrorMessage(error)
    ElMessage.error('CSV 校验失败，请查看错误详情')
  } finally {
    uploadLoading.value = false
  }
}

const resetFilters = () => {
  filters.value = {
    regions: [],
    startYear: metadata.value.minYear,
    endYear: metadata.value.maxYear,
    month: null,
  }
  refreshResults()
}

const downloadCsv = async () => {
  exportLoading.value = true
  try {
    await exportResults(filters.value)
    ElMessage.success('CSV 已导出')
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    exportLoading.value = false
  }
}

const downloadPng = () => {
  if (chartRef.value?.exportPng()) ElMessage.success('趋势图 PNG 已导出')
}

onMounted(async () => {
  try {
    metadata.value = await getMetadata()
    if (metadata.value.rawRecordCount) {
      filters.value.startYear = metadata.value.minYear
      filters.value.endYear = metadata.value.maxYear
      await refreshResults()
    }
  } catch {
    // 后端未启动时仍展示可用的上传页面，实际操作时给出连接错误。
  }
})
</script>

<template>
  <div class="app-shell">
    <header class="hero">
      <div class="hero-inner">
        <div class="brand-mark"><span></span><span></span><span></span></div>
        <div class="hero-copy">
          <p class="hero-kicker">CLIMATE INTELLIGENCE</p>
          <h1>历史气象数据分析</h1>
          <p>Historical Temperature Anomaly Analysis</p>
        </div>
        <div class="hero-badge"><i></i> 内存实时计算</div>
      </div>
    </header>

    <main class="main-content">
      <WeatherUpload :loading="uploadLoading" :on-upload="handleUpload" />

      <el-alert
        v-if="uploadError"
        class="persistent-alert"
        title="CSV 数据校验未通过"
        :description="uploadError"
        type="error"
        show-icon
        closable
        @close="uploadError = ''"
      />

      <template v-if="hasData">
        <section class="content-section overview-section">
          <div class="section-heading">
            <div><span class="section-number">01</span><h2>数据概览</h2></div>
            <p>本次上传数据的处理摘要</p>
          </div>
          <StatsOverview :metadata="metadata" />
          <el-alert
            v-if="metadata.warnings.length"
            class="warning-alert"
            :title="metadata.warnings.join('；')"
            type="warning"
            show-icon
            :closable="false"
          />
        </section>

        <section class="content-section filter-section">
          <div class="section-heading compact-heading">
            <div><span class="section-number">02</span><h2>筛选条件</h2></div>
            <el-button v-if="activeFilterCount" text :icon="RefreshRight" @click="resetFilters">重置筛选</el-button>
          </div>
          <WeatherFilter v-model="filters" :metadata="metadata" :loading="resultsLoading" @change="refreshResults" />
        </section>

        <section class="content-section chart-section">
          <div class="section-heading">
            <div><span class="section-number">03</span><h2>距平趋势分析</h2></div>
            <div class="heading-actions">
              <span class="semantic-key"><i class="warm"></i>正距平</span>
              <span class="semantic-key"><i class="cold"></i>负距平</span>
              <el-button :icon="Picture" :disabled="!results.length" @click="downloadPng">导出 PNG</el-button>
            </div>
          </div>
          <AnomalyChart ref="chartRef" :data="results" :loading="resultsLoading" />
        </section>

        <section class="content-section table-section">
          <div class="section-heading">
            <div><span class="section-number">04</span><h2>分析结果</h2><em>{{ results.length }} 条</em></div>
            <el-button type="primary" :icon="Download" :loading="exportLoading" :disabled="!results.length" @click="downloadCsv">
              导出 CSV
            </el-button>
          </div>
          <ResultTable :data="results" :loading="resultsLoading" />
        </section>
      </template>

      <section v-else class="empty-workspace">
        <div class="empty-illustration">
          <span class="axis axis-x"></span><span class="axis axis-y"></span>
          <i></i><i></i><i></i><i></i>
        </div>
        <h2>从一份 CSV 开始分析</h2>
        <p>上传历史气温记录后，这里将自动展示统计概览、距平趋势与详细结果。</p>
      </section>
    </main>

    <footer>历史气象数据分析工具 · 数据仅保存在当前服务内存中</footer>
  </div>
</template>
