<script setup lang="ts">
import type { Metadata, WeatherFilters } from '../types/weather'

const props = defineProps<{
  metadata: Metadata
  modelValue: WeatherFilters
  loading: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: WeatherFilters]
  change: []
}>()

const update = <K extends keyof WeatherFilters>(key: K, value: WeatherFilters[K]) => {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
  queueMicrotask(() => emit('change'))
}
</script>

<template>
  <div class="filter-grid" v-loading="loading">
    <div class="filter-field region-field">
      <label>地区</label>
      <el-select
        :model-value="modelValue.regions"
        multiple
        collapse-tags
        collapse-tags-tooltip
        clearable
        placeholder="全部地区"
        @update:model-value="update('regions', $event)"
      >
        <el-option v-for="region in metadata.regions" :key="region" :label="region" :value="region" />
      </el-select>
    </div>
    <div class="filter-field">
      <label>起始年份</label>
      <el-input-number
        :model-value="modelValue.startYear"
        :min="metadata.minYear ?? undefined"
        :max="modelValue.endYear ?? metadata.maxYear ?? undefined"
        :controls="false"
        @update:model-value="update('startYear', $event ?? null)"
      />
    </div>
    <div class="range-divider">—</div>
    <div class="filter-field">
      <label>结束年份</label>
      <el-input-number
        :model-value="modelValue.endYear"
        :min="modelValue.startYear ?? metadata.minYear ?? undefined"
        :max="metadata.maxYear ?? undefined"
        :controls="false"
        @update:model-value="update('endYear', $event ?? null)"
      />
    </div>
    <div class="filter-field">
      <label>月份</label>
      <el-select
        :model-value="modelValue.month"
        clearable
        placeholder="全部月份"
        @update:model-value="update('month', $event ?? null)"
      >
        <el-option v-for="month in metadata.months" :key="month" :label="`${month} 月`" :value="month" />
      </el-select>
    </div>
  </div>
</template>
