<template>
  <div class="alerts-page">
    <PageHead title="高危预警" />

    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <el-card shadow="never" class="stat-card stat-pending">
          <div class="stat-num">{{ stats.pending }}</div>
          <div class="stat-label">待处理</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="stat-card stat-intervened">
          <div class="stat-num">{{ stats.intervened }}</div>
          <div class="stat-label">已干预</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="stat-card stat-resolved">
          <div class="stat-num">{{ stats.resolved }}</div>
          <div class="stat-label">已处理</div>
        </el-card>
      </el-col>
    </el-row>

    <el-form inline class="search-bar">
      <el-form-item label="等级">
        <el-select v-model="query.level" placeholder="全部" clearable style="width: 120px" @change="onSearch">
          <el-option label="中度" value="MODERATE" />
          <el-option label="重度" value="SEVERE" />
        </el-select>
      </el-form-item>
      <el-form-item label="干预状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="onSearch">
          <el-option label="未处理" :value="0" />
          <el-option label="已干预" :value="1" />
          <el-option label="已处理" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-card shadow="never" class="table-card">
      <el-table :data="list" v-loading="loading" empty-text="暂无预警记录">
        <el-table-column label="用户" width="140">
          <template #default="{ row }">{{ row.userNickname || `用户#${row.userId}` }}</template>
        </el-table-column>
        <el-table-column prop="scaleName" label="量表" min-width="140" show-overflow-tooltip />
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
        <el-table-column label="干预状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.interventionStatus)" size="small">{{ row.interventionStatusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="completedAt" label="完成时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openReport(row)">报告</el-button>
            <el-button v-if="row.interventionStatus === 0" text type="warning" @click="markIntervention(row, 1)">标记已干预</el-button>
            <el-button v-if="row.interventionStatus === 1" text type="success" @click="markIntervention(row, 2)">标记已处理</el-button>
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
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="用户">{{ report.userNickname || `用户#${report.userId}` }}</el-descriptions-item>
          <el-descriptions-item label="量表">{{ report.scaleName }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ report.totalScore }}</el-descriptions-item>
          <el-descriptions-item label="等级">
            <el-tag :color="levelColor(report.level)" style="color: #fff">{{ report.levelLabel || report.level }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="风险"><el-tag v-if="report.riskFlag === 1" type="danger">需关注</el-tag><span v-else>—</span></el-descriptions-item>
          <el-descriptions-item label="干预状态">{{ interventionLabel(report.interventionStatus) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="report.interventionNote" class="intervention-note">
          <strong>干预备注：</strong>{{ report.interventionNote }}
        </div>
        <div v-if="report.dimensionScores && Object.keys(report.dimensionScores).length" class="radar-wrap">
          <h4>维度雷达图</h4>
          <div ref="radarRef" class="radar-chart"></div>
        </div>
        <div class="report-section"><h4>报告解读</h4><div class="report-text">{{ report.interpretation || '—' }}</div></div>
        <div class="report-section"><h4>建议</h4>
          <ul v-if="report.suggestions?.length"><li v-for="(s, i) in report.suggestions" :key="i">{{ s }}</li></ul>
          <div v-else>—</div>
        </div>
      </div>
    </el-drawer>

    <!-- 干预备注弹窗 -->
    <el-dialog v-model="interventionVisible" :title="interventionTarget === 1 ? '标记已干预' : '标记已处理'" width="480px">
      <el-input v-model="interventionNote" type="textarea" :rows="3" placeholder="干预备注（可选）" />
      <template #footer>
        <el-button @click="interventionVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmIntervention">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import PageHead from '@/components/PageHead.vue'
import { assessmentAlerts, assessmentRecordDetail, updateIntervention } from '@/api/admin'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, level: '', status: null })
const stats = reactive({ pending: 0, intervened: 0, resolved: 0 })

const levels = [
  { value: 'MODERATE', label: '中度', color: '#E8927C' },
  { value: 'SEVERE', label: '重度', color: '#D9534F' }
]
const levelColor = (lv) => { const f = levels.find(l => l.value === lv); return f ? f.color : '#909399' }
const statusTagType = (s) => ({ 0: 'danger', 1: 'warning', 2: 'success' }[s] || 'info')
const interventionLabel = (s) => ({ 0: '未处理', 1: '已干预', 2: '已处理' }[s] || '未处理')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await assessmentAlerts(query)
    list.value = res.records || []
    total.value = res.total || 0
    // 统计干预状态分布（基于当前页数据简化，全量统计可由后端提供）
    stats.pending = list.value.filter(r => r.interventionStatus === 0).length
    stats.intervened = list.value.filter(r => r.interventionStatus === 1).length
    stats.resolved = list.value.filter(r => r.interventionStatus === 2).length
  } finally { loading.value = false }
}
const onSearch = () => { query.page = 1; fetchData() }
const onReset = () => { query.level = ''; query.status = null; query.page = 1; fetchData() }

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

const interventionVisible = ref(false)
const interventionTarget = ref(1)
const interventionNote = ref('')
let interventionRow = null

const markIntervention = (row, target) => {
  interventionRow = row
  interventionTarget.value = target
  interventionNote.value = row.interventionNote || ''
  interventionVisible.value = true
}
const confirmIntervention = async () => {
  await updateIntervention(interventionRow.id, { status: interventionTarget.value, note: interventionNote.value })
  ElMessage.success('操作成功')
  interventionVisible.value = false
  fetchData()
}

onMounted(fetchData)
onBeforeUnmount(() => { radarChart?.dispose() })
</script>

<style scoped>
.alerts-page { padding: 20px; }
.stat-row { margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-num { font-size: 32px; font-weight: 700; }
.stat-label { color: #6b7280; margin-top: 4px; }
.stat-pending .stat-num { color: #d9534f; }
.stat-intervened .stat-num { color: #e8a33d; }
.stat-resolved .stat-num { color: #67c23a; }
.search-bar { margin-bottom: 16px; }
.table-card { background: #fff; }
.pagination { margin-top: 16px; text-align: right; }
.report-detail { padding: 0 8px; }
.intervention-note { margin: 12px 0; padding: 10px; background: #fef0f0; border-radius: 6px; }
.radar-wrap { margin: 16px 0; }
.radar-chart { width: 100%; height: 320px; }
.report-section { margin-top: 16px; }
.report-section h4 { margin: 8px 0; }
.report-text { white-space: pre-wrap; line-height: 1.6; background: #f7f8fa; padding: 12px; border-radius: 6px; }
</style>
