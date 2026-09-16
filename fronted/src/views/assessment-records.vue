<template>
  <div class="records-page">
    <PageHead title="测评记录" />

    <el-form inline class="search-bar">
      <el-form-item label="量表">
        <el-select v-model="query.scaleId" placeholder="全部" clearable style="width: 180px" @change="onSearch">
          <el-option v-for="s in scaleList" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="用户">
        <el-input v-model="query.keyword" placeholder="用户昵称" clearable style="width: 150px" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="等级">
        <el-select v-model="query.level" placeholder="全部" clearable style="width: 110px">
          <el-option v-for="l in levels" :key="l.value" :label="l.label" :value="l.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间">
        <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
          start-placeholder="开始" end-placeholder="结束" style="width: 240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-card shadow="never" class="table-card">
      <el-table :data="list" v-loading="loading" empty-text="暂无记录">
        <el-table-column label="用户" width="140">
          <template #default="{ row }">{{ row.userNickname || `用户#${row.userId}` }}</template>
        </el-table-column>
        <el-table-column prop="scaleName" label="量表" min-width="150" show-overflow-tooltip />
        <el-table-column prop="totalScore" label="总分" width="90" />
        <el-table-column label="等级" width="100">
          <template #default="{ row }">
            <el-tag :color="levelColor(row.level)" style="color: #fff" size="small">{{ row.levelLabel || row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.riskFlag === 1" type="danger" size="small">需关注</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="completedAt" label="完成时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openReport(row)">报告</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
        :page-sizes="[10, 20]" layout="total, sizes, prev, pager, next, jumper" class="pagination"
        @size-change="fetchData" @current-change="fetchData" />
    </el-card>

    <!-- 报告详情抽屉 -->
    <el-drawer v-model="reportVisible" title="测评报告详情" size="60%" destroy-on-close @open="initRadar">
      <div v-if="report" class="report-detail">
        <el-descriptions :column="3" border size="small" class="report-basic">
          <el-descriptions-item label="用户">{{ report.userNickname || `用户#${report.userId}` }}</el-descriptions-item>
          <el-descriptions-item label="量表">{{ report.scaleName }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ report.totalScore }}</el-descriptions-item>
          <el-descriptions-item label="等级">
            <el-tag :color="levelColor(report.level)" style="color: #fff">{{ report.levelLabel || report.level }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="风险">
            <el-tag v-if="report.riskFlag === 1" type="danger">需关注</el-tag>
            <span v-else>—</span>
          </el-descriptions-item>
          <el-descriptions-item label="干预状态">{{ interventionLabel(report.interventionStatus) }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="report.dimensionScores && Object.keys(report.dimensionScores).length" class="radar-wrap">
          <h4>维度雷达图</h4>
          <div ref="radarRef" class="radar-chart"></div>
        </div>

        <div class="report-section">
          <h4>答案明细</h4>
          <el-table :data="report.answers" size="small" border>
            <el-table-column label="题号" width="80">
              <template #default="{ row }">第 {{ (row.questionIndex + 1) }} 题</template>
            </el-table-column>
            <el-table-column label="得分" width="100">
              <template #default="{ row }">{{ row.score }} 分</template>
            </el-table-column>
          </el-table>
        </div>

        <div class="report-section">
          <h4>报告解读</h4>
          <div class="report-text">{{ report.interpretation || '—' }}</div>
        </div>
        <div class="report-section">
          <h4>建议</h4>
          <ul v-if="report.suggestions && report.suggestions.length">
            <li v-for="(s, i) in report.suggestions" :key="i">{{ s }}</li>
          </ul>
          <div v-else>—</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import PageHead from '@/components/PageHead.vue'
import { assessmentScalePage, assessmentRecordPage, assessmentRecordDetail } from '@/api/admin'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, scaleId: null, keyword: '', level: '' })
const dateRange = ref([])
const scaleList = ref([])

const levels = [
  { value: 'NORMAL', label: '正常', color: '#7BC67E' },
  { value: 'MILD', label: '轻度', color: '#F2C94C' },
  { value: 'MODERATE', label: '中度', color: '#E8927C' },
  { value: 'SEVERE', label: '重度', color: '#D9534F' }
]
const levelColor = (lv) => { const f = levels.find(l => l.value === lv); return f ? f.color : '#909399' }
const interventionLabel = (s) => ({ 0: '未处理', 1: '已干预', 2: '已处理' }[s] || '未处理')

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...query }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await assessmentRecordPage(params)
    list.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}
const onSearch = () => { query.page = 1; fetchData() }
const onReset = () => { query.scaleId = null; query.keyword = ''; query.level = ''; dateRange.value = []; query.page = 1; fetchData() }

const reportVisible = ref(false)
const report = ref(null)
const radarRef = ref(null)
let radarChart = null

const openReport = async (row) => {
  report.value = await assessmentRecordDetail(row.id)
  reportVisible.value = true
}

const initRadar = async () => {
  await nextTick()
  if (!radarRef.value || !report.value?.dimensionScores) return
  const dims = Object.entries(report.value.dimensionScores)
  if (!dims.length) return
  const maxVal = Math.max(...dims.map(([, v]) => Number(v) || 0), 1)
  radarChart?.dispose()
  radarChart = echarts.init(radarRef.value)
  radarChart.setOption({
    tooltip: {},
    radar: { indicator: dims.map(([name]) => ({ name, max: Math.ceil(maxVal * 1.2) })) },
    series: [{ type: 'radar', data: [{ value: dims.map(([, v]) => Number(v) || 0), name: '维度得分' }] }]
  })
}

onMounted(async () => {
  const sRes = await assessmentScalePage({ size: 50 })
  scaleList.value = sRes.records || []
  fetchData()
})
onBeforeUnmount(() => { radarChart?.dispose() })
</script>

<style scoped>
.records-page { padding: 20px; }
.search-bar { margin-bottom: 16px; }
.table-card { background: #fff; }
.pagination { margin-top: 16px; text-align: right; }
.report-detail { padding: 0 8px; }
.report-basic { margin-bottom: 16px; }
.radar-wrap { margin: 16px 0; }
.radar-chart { width: 100%; height: 320px; }
.report-section { margin-top: 16px; }
.report-section h4 { margin: 8px 0; }
.report-text { white-space: pre-wrap; line-height: 1.6; color: #1f2937; background: #f7f8fa; padding: 12px; border-radius: 6px; }
</style>
