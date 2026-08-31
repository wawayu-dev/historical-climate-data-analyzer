<script setup lang="ts">
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { EChartsOption, SeriesOption } from 'echarts'
import type { TemperatureAnomaly } from '../types/weather'

const props = defineProps<{
  data: TemperatureAnomaly[]
  loading: boolean
}>()

const chartEl = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null
const symbols = ['circle', 'diamond', 'rect', 'triangle', 'roundRect', 'pin', 'arrow']

const regions = computed(() => [...new Set(props.data.map((item) => item.region))])

const renderChart = async () => {
  await nextTick()
  if (!chartEl.value || props.data.length === 0) return
  chart ??= echarts.init(chartEl.value)

  const series: SeriesOption[] = regions.value.map((region, index) => ({
    name: region,
    type: 'line',
    symbol: symbols[index % symbols.length],
    symbolSize: 10,
    showSymbol: true,
    connectNulls: false,
    smooth: 0.16,
    lineStyle: { width: 3 },
    emphasis: { focus: 'series', lineStyle: { width: 4 } },
    data: props.data
      .filter((item) => item.region === region)
      .map((item) => ({
        value: [`${item.year}-${String(item.month).padStart(2, '0')}`, item.anomaly],
        detail: item,
      })),
    markLine: index === 0 ? {
      silent: true,
      symbol: 'none',
      label: { formatter: '0℃ 基准线', color: '#5f6c7b', position: 'insideEndTop' },
      lineStyle: { color: '#7d8998', type: 'dashed', width: 1.5 },
      data: [{ yAxis: 0 }],
    } : undefined,
  }))

  const option: EChartsOption = {
    animationDuration: 500,
    grid: { left: 56, right: 34, top: 74, bottom: 74 },
    legend: {
      top: 8,
      left: 0,
      itemWidth: 20,
      textStyle: { color: '#445062', fontSize: 13 },
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(20, 31, 50, 0.94)',
      borderWidth: 0,
      textStyle: { color: '#fff' },
      formatter: (params) => {
        const items = Array.isArray(params) ? params : [params]
        if (!items.length) return ''
        const rows = items.map((param) => {
          const data = param.data as { detail?: TemperatureAnomaly }
          const detail = data.detail
          if (!detail) return ''
          const value = detail.anomaly > 0 ? `+${detail.anomaly.toFixed(2)}` : detail.anomaly.toFixed(2)
          const color = detail.anomaly > 0 ? '#ff7a45' : detail.anomaly < 0 ? '#4d9fff' : '#aab2bd'
          return `<div style="display:flex;gap:18px;justify-content:space-between;margin-top:7px">
            <span>${param.marker}${detail.region}</span><strong style="color:${color}">${value} ℃</strong>
          </div>`
        }).join('')
        const firstItem = items[0] as typeof items[number] & { axisValue?: string }
        return `<strong>${firstItem?.axisValue ?? ''}</strong>${rows}`
      },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#c8d0dc' } },
      axisTick: { show: false },
      axisLabel: { color: '#6b7788', margin: 14 },
    },
    yAxis: {
      type: 'value',
      name: '距平值（℃）',
      nameTextStyle: { color: '#687487', padding: [0, 0, 8, 0] },
      axisLabel: { color: '#6b7788', formatter: '{value}°' },
      splitLine: { lineStyle: { color: '#e9edf3', type: 'dashed' } },
    },
    visualMap: {
      type: 'piecewise',
      show: true,
      dimension: 1,
      seriesIndex: series.map((_, index) => index),
      orient: 'horizontal',
      right: 4,
      bottom: 4,
      itemWidth: 16,
      itemHeight: 9,
      textStyle: { color: '#657185' },
      pieces: [
        { gt: 0, label: '正距平 · 暖色', color: '#f06445' },
        { value: 0, label: '零距平', color: '#98a2b1' },
        { lt: 0, label: '负距平 · 冷色', color: '#3c83d5' },
      ],
    },
    series,
  }
  chart.setOption(option, true)
}

const resize = () => chart?.resize()

const exportPng = () => {
  if (!chart || props.data.length === 0) return false
  const url = chart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#ffffff' })
  const link = document.createElement('a')
  link.href = url
  link.download = 'temperature-anomaly-chart.png'
  link.click()
  return true
}

defineExpose({ exportPng })

watch(() => props.data, renderChart, { deep: true })
onMounted(() => {
  renderChart()
  window.addEventListener('resize', resize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
})
</script>

<template>
  <div class="chart-wrapper" v-loading="loading">
    <div v-show="data.length" ref="chartEl" class="chart-canvas" />
    <el-empty v-if="!data.length && !loading" description="当前筛选条件下暂无趋势数据" />
  </div>
</template>
