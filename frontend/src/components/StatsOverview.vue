<script setup lang="ts">
import type { Metadata } from '../types/weather'

defineProps<{ metadata: Metadata }>()
</script>

<template>
  <div class="stats-grid">
    <article class="stat-card">
      <div class="stat-icon cyan">原</div>
      <div><span>原始记录</span><strong>{{ metadata.rawRecordCount }}</strong><small>条观测数据</small></div>
    </article>
    <article class="stat-card">
      <div class="stat-icon violet">析</div>
      <div><span>分析结果</span><strong>{{ metadata.resultCount }}</strong><small>个月度结果</small></div>
    </article>
    <article class="stat-card">
      <div class="stat-icon green">区</div>
      <div><span>地区数量</span><strong>{{ metadata.regions.length }}</strong><small>{{ metadata.regions.join(' · ') }}</small></div>
    </article>
    <article class="stat-card">
      <div class="stat-icon amber">年</div>
      <div><span>年份范围</span><strong class="year-range">{{ metadata.minYear }}—{{ metadata.maxYear }}</strong><small>历史观测区间</small></div>
    </article>
    <article v-if="metadata.abnormalCount" class="stat-card warning-stat">
      <div class="stat-icon red">!</div>
      <div><span>可疑数据</span><strong>{{ metadata.abnormalCount }}</strong><small>已导入并标记</small></div>
    </article>
  </div>
</template>
