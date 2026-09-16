<template>
  <div class="dashboard" v-loading="loading.overview">
    <!-- 顶部：时间范围切换 -->
    <div class="dash-toolbar">
      <PageHead title="数据看板" />
      <div class="dash-toolbar__controls">
        <el-radio-group v-model="range" @change="onRangePreset">
          <el-radio-button label="TODAY">今日</el-radio-button>
          <el-radio-button label="7D">近7天</el-radio-button>
          <el-radio-button label="30D">近30天</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-model="customRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :clearable="false"
          class="dash-date-picker"
          @change="onCustomRange"
        />
        <el-button type="primary" :loading="anyLoading" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <!-- 六项核心指标卡 -->
    <el-row :gutter="16" class="metric-row">
      <el-col v-for="m in metrics" :key="m.key" :xs="12" :sm="12" :md="8">
        <el-card shadow="hover" class="metric-card" :body-style="{ padding: '18px 20px' }">
          <div class="metric-card__inner">
            <el-icon :size="32" :color="m.color"><component :is="m.icon" /></el-icon>
            <div class="metric-info">
              <p class="metric-label">{{ m.label }}</p>
              <p class="metric-value">{{ m.item ? m.item.value : '—' }}</p>
              <p class="metric-rate">
                <span>环比 <b :class="rateClass(m.item?.mom)">{{ fmtRate(m.item?.mom) }}</b></span>
                <span>同比 <b :class="rateClass(m.item?.yoy)">{{ fmtRate(m.item?.yoy) }}</b></span>
              </p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 用户增长 -->
    <ChartCard title="用户增长" :loading="loading.userTrend" :is-empty="isEmptyUserTrend" :height="320">
      <template #extra>
        <el-radio-group v-model="granularity" size="small" @change="loadUserTrend">
          <el-radio-button label="day">按日</el-radio-button>
          <el-radio-button label="month">按月</el-radio-button>
        </el-radio-group>
      </template>
      <div :ref="setChartEl('userGrowth')" class="chart-fill"></div>
    </ChartCard>

    <!-- 咨询统计 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <ChartCard title="会话数趋势" :loading="loading.consultation" :is-empty="isEmptySessionTrend" :height="320">
          <template #extra>
            <el-tag v-if="consultation" type="info" effect="plain" size="small">
              平均时长 {{ consultation.avgDurationMinutes ?? 0 }} 分钟
            </el-tag>
          </template>
          <div :ref="setChartEl('sessionTrend')" class="chart-fill"></div>
        </ChartCard>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <ChartCard title="咨询主题分布" :loading="loading.consultation" :is-empty="isEmptyScenes" :height="320">
          <div :ref="setChartEl('scenePie')" class="chart-fill"></div>
        </ChartCard>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <ChartCard title="消息情绪分布" :loading="loading.consultation" :is-empty="isEmptyMsgEmotions" :height="320">
          <div :ref="setChartEl('emotionPie')" class="chart-fill"></div>
        </ChartCard>
      </el-col>
    </el-row>

    <!-- 测评分析 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <ChartCard title="量表测评次数" :loading="loading.assessment" :is-empty="isEmptyScales" :height="300">
          <div :ref="setChartEl('scaleBar')" class="chart-fill"></div>
        </ChartCard>
      </el-col>
      <el-col :xs="24" :lg="12">
        <ChartCard title="测评结果等级分布" :loading="loading.assessment" :is-empty="isEmptyLevels" :height="300">
          <div :ref="setChartEl('levelBar')" class="chart-fill"></div>
        </ChartCard>
      </el-col>
    </el-row>
    <div class="chart-row-block">
      <ChartCard
        title="高危用户预警（仅管理员可见，信息已脱敏）"
        :loading="loading.assessment"
        :is-empty="false"
        :height="420"
      >
        <div class="table-fill">
          <el-table
            :data="assessment?.riskUsers || []"
            size="small"
            height="100%"
            :row-class-name="riskRowClass"
          >
            <el-table-column prop="nickname" label="用户昵称" min-width="120" />
            <el-table-column prop="phone" label="手机号" min-width="130">
              <template #default="{ row }">{{ row.phone || '—' }}</template>
            </el-table-column>
            <el-table-column prop="riskType" label="风险类型" min-width="120">
              <template #default="{ row }">
                <el-tag type="danger" effect="plain" size="small">{{ row.riskType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最高等级" width="100">
              <template #default="{ row }">
                <el-tag :type="levelTagType(row.riskLevel)" size="small">
                  {{ levelMeta(row.riskLevel).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastTime" label="最近测评时间" min-width="150">
              <template #default="{ row }">{{ row.lastTime || '—' }}</template>
            </el-table-column>
            <el-table-column prop="count" label="次数" width="80" />
            <template #empty>暂无高危预警</template>
          </el-table>
        </div>
      </ChartCard>
    </div>

    <!-- 情绪大数据 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="8">
        <ChartCard title="全站情绪分布" :loading="loading.emotion" :is-empty="isEmptyEmotionDist" :height="340">
          <div :ref="setChartEl('emotionDonut')" class="chart-fill"></div>
        </ChartCard>
      </el-col>
      <el-col :xs="24" :lg="16">
        <ChartCard title="情绪趋势热力图（近30天）" :loading="loading.emotion" :is-empty="isEmptyHeatmap" :height="340">
          <div :ref="setChartEl('emotionHeatmap')" class="chart-fill"></div>
        </ChartCard>
      </el-col>
    </el-row>

    <!-- 内容排行 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="8">
        <ChartCard title="文章浏览 TOP10" :loading="loading.content" :is-empty="isEmptyArticles" :height="380">
          <div :ref="setChartEl('articleBar')" class="chart-fill"></div>
        </ChartCard>
      </el-col>
      <el-col :xs="24" :lg="8">
        <ChartCard title="课程学习 TOP10" :loading="loading.content" :is-empty="isEmptyCourses" :height="380">
          <div :ref="setChartEl('courseBar')" class="chart-fill"></div>
        </ChartCard>
      </el-col>
      <el-col :xs="24" :lg="8">
        <ChartCard title="热门帖子 TOP10" :loading="loading.content" :is-empty="isEmptyPosts" :height="380">
          <div :ref="setChartEl('postBar')" class="chart-fill"></div>
        </ChartCard>
      </el-col>
    </el-row>

    <!-- 预约统计 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="10">
        <ChartCard title="预约量趋势" :loading="loading.appointment" :is-empty="isEmptyAppointTrend" :height="320">
          <div :ref="setChartEl('appointTrend')" class="chart-fill"></div>
        </ChartCard>
      </el-col>
      <el-col :xs="24" :lg="8">
        <ChartCard title="咨询师预约排行" :loading="loading.appointment" :is-empty="isEmptyRanking" :height="320">
          <div :ref="setChartEl('consultantBar')" class="chart-fill"></div>
        </ChartCard>
      </el-col>
      <el-col :xs="24" :lg="6">
        <ChartCard title="预约完成率" :loading="loading.appointment" :is-empty="false" :height="320">
          <div :ref="setChartEl('rateGauge')" class="chart-fill"></div>
        </ChartCard>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { User, UserFilled, ChatDotRound, Document, Calendar } from '@element-plus/icons-vue'
import PageHead from '@/components/PageHead.vue'
import ChartCard from '@/components/ChartCard.vue'
import {
  getDashboardOverview,
  getUserTrend,
  getConsultationStats,
  getAssessmentStats,
  getEmotionStats,
  getContentStats,
  getAppointmentStats
} from '@/api/admin'
import { lineChart, barChart, pieChart, heatmapChart, horizBarChart, gaugeChart } from '@/utils/echartsOptions'

// ===== 时间范围状态 =====
const range = ref('7D')
const customRange = ref([])
const granularity = ref('day')

// ===== 各图表组独立 loading / 数据 =====
const loading = reactive({
  overview: false,
  userTrend: false,
  consultation: false,
  assessment: false,
  emotion: false,
  content: false,
  appointment: false
})
const anyLoading = computed(() => Object.values(loading).some(Boolean))

const overview = ref(null)
const userTrend = ref(null)
const consultation = ref(null)
const assessment = ref(null)
const emotion = ref(null)
const content = ref(null)
const appointment = ref(null)

// 等级编码 -> 中文标签
const LEVEL_META = {
  NORMAL: { label: '正常' },
  MILD: { label: '轻度' },
  MODERATE: { label: '中度' },
  SEVERE: { label: '重度' }
}
const LEVEL_COLORS = { NORMAL: '#67C23A', MILD: '#E6A23C', MODERATE: '#E6A23C', SEVERE: '#F56C6C' }

function levelMeta(code) {
  return LEVEL_META[code] || { label: code || '—' }
}
function levelTagType(code) {
  return { NORMAL: 'success', MILD: 'warning', MODERATE: 'warning', SEVERE: 'danger' }[code] || 'info'
}

function riskRowClass({ row }) {
  return row.riskLevel === 'SEVERE' ? 'severe-row' : ''
}

// ===== 指标卡 =====
const METRIC_DEFS = [
  { key: 'totalUsers', label: '用户总数', icon: User, color: '#409EFF' },
  { key: 'todayNewUsers', label: '今日新增', icon: UserFilled, color: '#67C23A' },
  { key: 'activeUsers', label: '活跃用户', icon: UserFilled, color: '#E6A23C' },
  { key: 'sessions', label: '咨询会话', icon: ChatDotRound, color: '#409EFF' },
  { key: 'assessments', label: '测评完成', icon: Document, color: '#67C23A' },
  { key: 'appointments', label: '预约数', icon: Calendar, color: '#E6A23C' }
]
const metrics = computed(() =>
  METRIC_DEFS.map(def => ({ ...def, item: overview.value?.[def.key] || null }))
)

function fmtRate(v) {
  if (v === null || v === undefined) return '—'
  if (v === 0) return '0.0%'
  return `${v > 0 ? '↑' : '↓'}${Math.abs(v).toFixed(1)}%`
}
function rateClass(v) {
  if (v === null || v === undefined) return 'is-na'
  if (v > 0) return 'is-up'
  if (v < 0) return 'is-down'
  return 'is-flat'
}

// ===== 空态判定 =====
const isEmptyUserTrend = computed(() => !userTrend.value?.newUsers?.some(v => v > 0))
const isEmptySessionTrend = computed(
  () => !consultation.value?.dailyTrend?.some(i => i.sessionCount > 0 || i.userCount > 0)
)
const isEmptyScenes = computed(() => !consultation.value?.sceneDistribution?.length)
const isEmptyMsgEmotions = computed(() => !consultation.value?.emotionDistribution?.length)
const isEmptyScales = computed(() => !assessment.value?.scaleCounts?.length)
const isEmptyLevels = computed(() => !assessment.value?.levelDistribution?.some(i => i.value > 0))
const isEmptyEmotionDist = computed(() => !emotion.value?.emotionDistribution?.length)
const isEmptyHeatmap = computed(() => !emotion.value?.heatmap?.data?.length)
const isEmptyArticles = computed(() => !content.value?.articles?.length)
const isEmptyCourses = computed(() => !content.value?.courses?.length)
const isEmptyPosts = computed(() => !content.value?.posts?.length)
const isEmptyAppointTrend = computed(() => !appointment.value?.dailyTrend?.some(i => i.value > 0))
const isEmptyRanking = computed(() => !appointment.value?.consultantRanking?.length)

// ===== 参数构建 =====
function buildParams() {
  const params = { range: range.value }
  if (range.value === 'CUSTOM' && customRange.value?.length === 2) {
    params.startDate = customRange.value[0]
    params.endDate = customRange.value[1]
  }
  return params
}

async function loadGroup(key, fn, apply) {
  loading[key] = true
  try {
    apply(await fn())
  } catch {
    // 单组失败保持空态，不影响其他图表
  } finally {
    loading[key] = false
  }
}

function loadOverview() {
  return loadGroup('overview', () => getDashboardOverview(buildParams()), res => { overview.value = res })
}
function loadUserTrend() {
  return loadGroup('userTrend', () => getUserTrend({ ...buildParams(), granularity: granularity.value }), res => {
    userTrend.value = res
    renderUserTrend(res)
  })
}
function loadConsultation() {
  return loadGroup('consultation', () => getConsultationStats(buildParams()), res => {
    consultation.value = res
    renderConsultation(res)
  })
}
function loadAssessment() {
  return loadGroup('assessment', () => getAssessmentStats(buildParams()), res => {
    assessment.value = res
    renderAssessment(res)
  })
}
function loadEmotion() {
  return loadGroup('emotion', () => getEmotionStats(buildParams()), res => {
    emotion.value = res
    renderEmotion(res)
  })
}
function loadContent() {
  return loadGroup('content', () => getContentStats(), res => {
    content.value = res
    renderContent(res)
  })
}
function loadAppointment() {
  return loadGroup('appointment', () => getAppointmentStats(buildParams()), res => {
    appointment.value = res
    renderAppointment(res)
  })
}

function loadAll() {
  return Promise.all([
    loadOverview(), loadUserTrend(), loadConsultation(), loadAssessment(),
    loadEmotion(), loadContent(), loadAppointment()
  ])
}

// ===== 范围切换 =====
function onRangePreset() {
  customRange.value = []
  loadAll()
}
function onCustomRange(val) {
  if (!val || val.length !== 2) return
  const start = new Date(val[0])
  const end = new Date(val[1])
  if (start > end) {
    ElMessage.warning('开始日期不能晚于结束日期')
    customRange.value = []
    return
  }
  const days = Math.round((end - start) / 86400000) + 1
  if (days > 366) {
    ElMessage.warning('查询时间跨度不能超过1年')
    customRange.value = []
    return
  }
  range.value = 'CUSTOM'
  loadAll()
}

// ===== ECharts 实例复用 =====
const chartDom = {}
const chartInsts = {}

function setChartEl(key) {
  return el => { if (el) chartDom[key] = el }
}
function setChart(key, option) {
  const el = chartDom[key]
  if (!el) return
  if (!chartInsts[key]) chartInsts[key] = echarts.init(el)
  if (option) chartInsts[key].setOption(option, true)
  else chartInsts[key].clear()
}
function resizeAll() {
  Object.values(chartInsts).forEach(c => c.resize())
}

// ===== 各图表渲染 =====
function renderUserTrend(d) {
  if (!d?.dates?.length) { setChart('userGrowth', null); return }
  const rotate = granularity.value === 'day' && d.dates.length > 15 ? 40 : 0
  setChart('userGrowth', lineChart(d.dates, [
    { name: '新增用户', data: d.newUsers, color: '#409EFF', area: true },
    { name: '累计用户', data: d.cumulativeUsers, color: '#67C23A', yIndex: 1, dashed: true }
  ], { rotate }))
}
function renderConsultation(d) {
  const trend = d?.dailyTrend || []
  if (trend.length) {
    setChart('sessionTrend', lineChart(
      trend.map(i => i.date),
      [
        { name: '会话数', data: trend.map(i => i.sessionCount), color: '#409EFF', area: true },
        { name: '参与用户', data: trend.map(i => i.userCount), color: '#67C23A' }
      ],
      { rotate: trend.length > 15 ? 40 : 0 }
    ))
  } else { setChart('sessionTrend', null) }
  setChart('scenePie', d?.sceneDistribution?.length ? pieChart(d.sceneDistribution) : null)
  setChart('emotionPie', d?.emotionDistribution?.length ? pieChart(d.emotionDistribution) : null)
}
function renderAssessment(d) {
  const scales = d?.scaleCounts || []
  setChart('scaleBar', scales.length
    ? barChart(scales.map(i => i.name), [{ name: '测评次数', data: scales.map(i => i.value) }], { rotate: scales.length > 4 ? 30 : 0 })
    : null)
  const levels = d?.levelDistribution || []
  if (levels.length) {
    setChart('levelBar', barChart(
      levels.map(i => i.label),
      [{ name: '人数', data: levels.map(i => ({ value: i.value, itemStyle: { color: LEVEL_COLORS[i.level] || '#409EFF' } })) }]
    ))
  } else { setChart('levelBar', null) }
}
function renderEmotion(d) {
  setChart('emotionDonut', d?.emotionDistribution?.length ? pieChart(d.emotionDistribution, { ring: true }) : null)
  setChart('emotionHeatmap', d?.heatmap?.dates?.length && d.heatmap.data?.length ? heatmapChart(d.heatmap) : null)
}
function renderContent(d) {
  setChart('articleBar', d?.articles?.length ? horizBarChart(d.articles, '#409EFF') : null)
  setChart('courseBar', d?.courses?.length ? horizBarChart(d.courses, '#67C23A') : null)
  setChart('postBar', d?.posts?.length ? horizBarChart(d.posts, '#E6A23C') : null)
}
function renderAppointment(d) {
  const trend = d?.dailyTrend || []
  setChart('appointTrend', trend.length
    ? lineChart(trend.map(i => i.name), [{ name: '预约量', data: trend.map(i => i.value), color: '#409EFF', area: true }], { legend: false, rotate: trend.length > 15 ? 40 : 0 })
    : null)
  setChart('consultantBar', d?.consultantRanking?.length ? horizBarChart(d.consultantRanking, '#67C23A') : null)
  setChart('rateGauge', gaugeChart(d?.completionRate ?? null))
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', resizeAll)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeAll)
  Object.values(chartInsts).forEach(c => c.dispose())
})
</script>

<style lang="scss" scoped>
.dashboard {
  padding: 0 4px;
}

.dash-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;

  &__controls {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    margin-bottom: 20px;
  }
}

.dash-date-picker {
  width: 250px;
}

// ===== 指标卡 =====
.metric-row {
  margin-top: 16px;
}

.metric-card {
  margin-bottom: 16px;

  &__inner {
    display: flex;
    align-items: center;
    gap: 14px;
  }

  .metric-label {
    margin: 0 0 4px;
    font-size: 13px;
    color: #909399;
  }

  .metric-value {
    margin: 0 0 6px;
    font-size: 26px;
    font-weight: 700;
    color: #303133;
    line-height: 1.1;
  }

  .metric-rate {
    margin: 0;
    display: flex;
    gap: 12px;
    font-size: 11px;
    color: #C0C4CC;

    b {
      font-weight: 600;
      margin-left: 2px;

      &.is-up { color: #67C23A; }
      &.is-down { color: #F56C6C; }
      &.is-flat, &.is-na { color: #C0C4CC; font-weight: 400; }
    }
  }
}

// ===== 图表区 =====
.chart-row {
  margin-top: 16px;
}

.chart-row-block {
  margin-top: 16px;
}

.chart-row .el-col {
  margin-bottom: 16px;
}

.chart-fill,
.table-fill {
  position: absolute;
  inset: 0;
}

// ===== 高危表格 =====
:deep(.el-table) {
  .severe-row {
    background: #FEF0F0;

    td.el-table__cell {
      background: #FEF0F0;
    }
  }
}
</style>
