<template>
  <div class="ai-stats-page">
    <PageHead title="AI 调用统计">
      <template #buttons>
        <el-radio-group v-model="range" @change="load">
          <el-radio-button value="7D">近 7 天</el-radio-button>
          <el-radio-button value="30D">近 30 天</el-radio-button>
        </el-radio-group>
      </template>
    </PageHead>

    <!-- 汇总卡 -->
    <el-row :gutter="16" class="summary-row">
      <el-col :span="6">
        <div class="stat-card">
          <p class="stat-label">调用次数</p>
          <p class="stat-value">{{ summary.totalCount ?? '-' }}</p>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <p class="stat-label">成功率</p>
          <p class="stat-value">{{ summary.successRate != null ? `${summary.successRate}%` : '-' }}</p>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <p class="stat-label">平均耗时</p>
          <p class="stat-value">{{ summary.avgDurationMs ?? '-' }}<span class="stat-unit">ms</span></p>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <p class="stat-label">估算费用</p>
          <p class="stat-value">
            {{ summary.totalCost != null ? `¥${summary.totalCost}` : '-' }}
          </p>
          <p class="stat-tip" v-if="summary.totalCost == null">流式调用无法提取 token，按可用数据估算</p>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="14">
        <ChartCard title="调用次数与费用趋势" :height="300" :is-empty="!daily.length">
          <div ref="trendChartEl" class="chart-box"></div>
        </ChartCard>
      </el-col>
      <el-col :span="10">
        <ChartCard title="用途分布" :height="300" :is-empty="!purposes.length">
          <div ref="purposeChartEl" class="chart-box"></div>
        </ChartCard>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="14">
        <ChartCard title="Token 消耗" :height="280" :is-empty="!hasTokens">
          <div ref="tokenChartEl" class="chart-box"></div>
        </ChartCard>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import PageHead from '@/components/PageHead.vue'
import ChartCard from '@/components/ChartCard.vue'
import { lineChart, barChart, pieChart } from '@/utils/echartsOptions'
import { getAiStats } from '@/api/admin'

const range = ref('7D')
const summary = reactive({})
const daily = ref([])
const purposes = ref([])
const hasTokens = ref(false)

const trendChartEl = ref(null)
const purposeChartEl = ref(null)
const tokenChartEl = ref(null)
let trendChart = null
let purposeChart = null
let tokenChart = null

const PURPOSE_LABELS = {
  STREAM_CHAT: '疏导对话', EMOTION_ANALYSIS: '情绪分析',
  SUMMARY: '会话总结', AI_TEST: '配置测试', PROMPT_TEST: '提示词试跑'
}

const load = async () => {
  const res = await getAiStats({ range: range.value })
  const data = res.data || {}
  Object.assign(summary, data.summary || {})
  daily.value = data.daily || []
  purposes.value = (data.purposes || []).map(p => ({
    ...p,
    label: PURPOSE_LABELS[p.purpose] || p.purpose
  }))

  const anyTokens = daily.value.some(d => d.inputTokens != null || d.outputTokens != null)
  hasTokens.value = anyTokens
  summary.totalCost = data.summary?.totalCost ?? null

  await nextTick()
  renderCharts()
}

const renderCharts = () => {
  const dates = daily.value.map(d => String(d.date).slice(5))

  // 次数 + 费用双轴（费用可能有 null，补 0）
  if (trendChartEl.value) {
    trendChart = trendChart || echarts.init(trendChartEl.value)
    trendChart.setOption(lineChart(dates, [
      { name: '调用次数', type: 'bar', data: daily.value.map(d => d.count) },
      { name: '费用(元)', type: 'line', data: daily.value.map(d => d.cost ?? 0), yIndex: 1, smooth: true }
    ]))
  }

  // 用途分布
  if (purposeChartEl.value) {
    purposeChart = purposeChart || echarts.init(purposeChartEl.value)
    purposeChart.setOption(pieChart(purposes.value.map(p => ({
      name: p.label, value: p.count
    })), { ring: true }))
  }

  // token 消耗
  if (tokenChartEl.value) {
    tokenChart = tokenChart || echarts.init(tokenChartEl.value)
    tokenChart.setOption(barChart(dates, [
      { name: '输入 token', type: 'bar', data: daily.value.map(d => d.inputTokens ?? 0), stack: 'total' },
      { name: '输出 token', type: 'bar', data: daily.value.map(d => d.outputTokens ?? 0), stack: 'total' }
    ], { legend: true }))
  }
}

const resize = () => {
  trendChart?.resize()
  purposeChart?.resize()
  tokenChart?.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  trendChart?.dispose()
  purposeChart?.dispose()
  tokenChart?.dispose()
})
</script>

<style lang="scss" scoped>
.summary-row {
  margin-bottom: 16px;
}
.stat-card {
  padding: 18px 20px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  .stat-label {
    margin-bottom: 8px;
    font-size: 13px;
    color: #6b7280;
  }
  .stat-value {
    font-size: 26px;
    font-weight: 600;
    color: #1f2937;
    .stat-unit {
      margin-left: 4px;
      font-size: 13px;
      font-weight: normal;
      color: #6b7280;
    }
  }
  .stat-tip {
    margin-top: 6px;
    font-size: 12px;
    color: #9ca3af;
  }
}
.chart-box {
  width: 100%;
  height: 100%;
}
</style>
