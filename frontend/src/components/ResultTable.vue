<script setup lang="ts">
import type { TemperatureAnomaly } from '../types/weather'

defineProps<{
  data: TemperatureAnomaly[]
  loading: boolean
}>()

const formatTemperature = (_row: TemperatureAnomaly, _column: unknown, value: number) => `${value.toFixed(2)} ℃`
const formatAnomaly = (value: number) => `${value > 0 ? '+' : ''}${value.toFixed(2)} ℃`
const anomalyClass = ({ row }: { row: TemperatureAnomaly }) =>
  row.anomaly > 0 ? 'anomaly-positive' : row.anomaly < 0 ? 'anomaly-negative' : 'anomaly-neutral'
const cellClassName = ({ row, column }: { row: TemperatureAnomaly; column: { property?: string } }) =>
  column.property === 'anomaly' ? anomalyClass({ row }) : ''
</script>

<template>
  <el-table
    :data="data"
    v-loading="loading"
    stripe
    height="430"
    empty-text="当前筛选条件下暂无分析结果"
    class="result-table"
    :cell-class-name="cellClassName"
  >
    <el-table-column prop="year" label="年份" min-width="90" sortable />
    <el-table-column prop="month" label="月份" min-width="85" sortable>
      <template #default="scope">{{ scope.row.month }} 月</template>
    </el-table-column>
    <el-table-column prop="region" label="地区" min-width="120" />
    <el-table-column prop="monthlyAverage" label="月平均气温" min-width="150" :formatter="formatTemperature" />
    <el-table-column prop="baselineAverage" label="历史同期平均" min-width="160" :formatter="formatTemperature" />
    <el-table-column prop="anomaly" label="距平值" min-width="130" fixed="right">
      <template #default="scope">
        <span class="anomaly-pill">{{ formatAnomaly(scope.row.anomaly) }}</span>
      </template>
    </el-table-column>
  </el-table>
</template>
