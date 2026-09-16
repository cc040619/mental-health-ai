<template>
  <div class="users-page">
    <PageHead title="用户管理" />

    <!-- 搜索栏 -->
    <el-form inline class="users-search">
      <el-form-item label="昵称/用户名">
        <el-input v-model="query.keyword" placeholder="昵称或用户名" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="query.phone" placeholder="手机号" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 100px">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
          <el-option label="待审核" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="注册时间">
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 220px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 统计三图 -->
    <el-row :gutter="16" class="users-stats">
      <el-col :xs="24" :sm="12" :lg="8">
        <ChartCard title="性别分布" :loading="statsLoading" :is-empty="isEmpty(stats?.genderDistribution)" :height="280">
          <div ref="genderChartEl" class="chart-fill"></div>
        </ChartCard>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="8">
        <ChartCard title="年龄段分布" :loading="statsLoading" :is-empty="isEmpty(stats?.ageDistribution)" :height="280">
          <div ref="ageChartEl" class="chart-fill"></div>
        </ChartCard>
      </el-col>
      <el-col :xs="24" :lg="8">
        <ChartCard title="注册趋势（近30天）" :loading="statsLoading" :is-empty="isEmpty(stats?.registrationTrend)" :height="280">
          <div ref="trendChartEl" class="chart-fill"></div>
        </ChartCard>
      </el-col>
    </el-row>

    <!-- 用户表格 -->
    <el-card shadow="never" class="users-table-card">
      <el-table :data="users" v-loading="loading" empty-text="暂无用户" style="width: 100%">
        <el-table-column label="头像" width="64">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.avatar">{{ (row.nickname || row.username || '?').charAt(0) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column label="昵称/用户名" width="135">
          <template #default="{ row }">
            <div class="user-name-cell">
              <span class="user-nickname">{{ row.nickname || '—' }}</span>
              <span class="user-username">{{ row.username || '—' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column prop="genderDisplayName" label="性别" width="80">
          <template #default="{ row }">{{ row.genderDisplayName || '未知' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'warning' : (row.status === 1 ? 'success' : 'danger')" size="small" effect="light">
              {{ row.statusDisplayName || (row.status === 1 ? '正常' : row.status === 2 ? '待审核' : '禁用') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最后登录" width="170">
          <template #default="{ row }">{{ fmtTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="300" class-name="col-action">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">详情</el-button>
            <el-popconfirm
              :title="`确认${row.status === 1 ? '禁用' : '启用'}该用户？${row.status === 1 ? '禁用后该用户会话立即失效' : ''}`"
              @confirm="onToggleStatus(row)"
            >
              <template #reference>
                <el-button text :type="row.status === 1 ? 'danger' : 'success'">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm title="将重置为默认密码并强制下线" @confirm="onResetPassword(row)">
              <template #reference>
                <el-button text type="warning">重置密码</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm title="确认强制下线该用户？" @confirm="onForceLogout(row)">
              <template #reference>
                <el-button text type="info">强制下线</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="users-pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :size="760" :with-header="false" :destroy-on-close="true">
      <div class="detail-drawer" v-loading="detailLoading">
        <div class="detail-header">
          <h3>用户详情</h3>
          <el-button text @click="detailVisible = false">关闭</el-button>
        </div>

        <el-descriptions v-if="detail" :column="2" border size="small" class="detail-desc">
          <el-descriptions-item label="头像">
            <el-avatar :size="48" :src="detail.avatar">{{ (detail.nickname || detail.username || '?').charAt(0) }}</el-avatar>
          </el-descriptions-item>
          <el-descriptions-item label="昵称">{{ detail.nickname || '—' }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ detail.username || '—' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.phone || '—' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ detail.genderDisplayName || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="生日">{{ detail.birthday || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detail.status === 2 ? 'warning' : (detail.status === 1 ? 'success' : 'danger')" size="small" effect="light">
              {{ detail.statusDisplayName || (detail.status === 1 ? '正常' : detail.status === 2 ? '待审核' : '禁用') }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ fmtTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="最后登录">{{ fmtTime(detail.lastLoginAt) }}</el-descriptions-item>
          <el-descriptions-item label="日记数">{{ detail.diaryCount }}</el-descriptions-item>
          <el-descriptions-item label="咨询数">{{ detail.sessionCount }}</el-descriptions-item>
          <el-descriptions-item label="测评数">{{ detail.assessmentCount }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="activeTab" class="detail-tabs" @tab-change="onTabChange">
          <el-tab-pane label="咨询记录" name="consultations">
            <el-table :data="tabs.consultations.records" v-loading="tabs.consultations.loading" size="small" style="width: 100%">
              <el-table-column prop="sessionTitle" label="标题" min-width="160" show-overflow-tooltip />
              <el-table-column prop="sceneLabel" label="场景" width="100">
                <template #default="{ row }">{{ row.sceneLabel || row.scene || '—' }}</template>
              </el-table-column>
              <el-table-column prop="messageCount" label="消息数" width="80" />
              <el-table-column prop="startedAt" label="开始时间" width="170">
                <template #default="{ row }">{{ fmtTime(row.startedAt) }}</template>
              </el-table-column>
            </el-table>
            <div class="tab-pager">
              <el-pagination
                v-model:current-page="tabs.consultations.page"
                :page-size="tabs.consultations.size"
                :total="tabs.consultations.total"
                layout="prev, pager, next"
                @current-change="loadTab('consultations')"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="测评记录" name="assessments">
            <el-table :data="tabs.assessments.records" v-loading="tabs.assessments.loading" size="small" style="width: 100%">
              <el-table-column prop="scaleName" label="量表" min-width="100" show-overflow-tooltip />
              <el-table-column prop="totalScore" label="总分" width="80" />
              <el-table-column prop="level" label="等级" width="100" />
              <el-table-column prop="completedAt" label="完成时间" width="170">
                <template #default="{ row }">{{ fmtTime(row.completedAt) }}</template>
              </el-table-column>
            </el-table>
            <div class="tab-pager">
              <el-pagination
                v-model:current-page="tabs.assessments.page"
                :page-size="tabs.assessments.size"
                :total="tabs.assessments.total"
                layout="prev, pager, next"
                @current-change="loadTab('assessments')"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="情绪日记" name="diaries">
            <el-table :data="tabs.diaries.records" v-loading="tabs.diaries.loading" size="small" style="width: 100%">
              <el-table-column prop="diaryDate" label="日期" width="120" />
              <el-table-column prop="dominantEmotion" label="主导情绪" width="100">
                <template #default="{ row }">{{ row.dominantEmotion || '—' }}</template>
              </el-table-column>
              <el-table-column prop="moodScore" label="情绪评分" width="90" />
              <el-table-column prop="diaryContent" label="内容摘要" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">{{ row.diaryContent || '—' }}</template>
              </el-table-column>
            </el-table>
            <div class="tab-pager">
              <el-pagination
                v-model:current-page="tabs.diaries.page"
                :page-size="tabs.diaries.size"
                :total="tabs.diaries.total"
                layout="prev, pager, next"
                @current-change="loadTab('diaries')"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="发帖记录" name="posts">
            <el-table :data="tabs.posts.records" v-loading="tabs.posts.loading" size="small" style="width: 100%">
              <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ row.title || '—' }}
                  <el-tag v-if="row.isAnonymous === 1" type="info" size="small" effect="plain" style="margin-left: 6px">匿名</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="boardLabel" label="板块" width="100">
                <template #default="{ row }">{{ row.boardLabel || row.board || '—' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag v-if="row.status === 0" type="success" size="small" effect="light">正常</el-tag>
                  <el-tag v-else-if="row.status === 2" type="warning" size="small" effect="light">举报待审</el-tag>
                  <el-tag v-else-if="row.status === 3" type="danger" size="small" effect="light">已下架</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="发布时间" width="170">
                <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
            <div class="tab-pager">
              <el-pagination
                v-model:current-page="tabs.posts.page"
                :page-size="tabs.posts.size"
                :total="tabs.posts.total"
                layout="prev, pager, next"
                @current-change="loadTab('posts')"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="预约记录" name="appointments">
            <el-table :data="tabs.appointments.records" v-loading="tabs.appointments.loading" size="small" style="width: 100%">
              <el-table-column prop="consultantName" label="咨询师" min-width="120">
                <template #default="{ row }">{{ row.consultantName || '—' }}</template>
              </el-table-column>
              <el-table-column prop="appointmentDate" label="预约日期" width="120" />
              <el-table-column label="时间段" width="140">
                <template #default="{ row }">{{ row.startTime || '—' }} - {{ row.endTime || '—' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag size="small" effect="light">{{ row.statusLabel || row.status || '—' }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <div class="tab-pager">
              <el-pagination
                v-model:current-page="tabs.appointments.page"
                :page-size="tabs.appointments.size"
                :total="tabs.appointments.total"
                layout="prev, pager, next"
                @current-change="loadTab('appointments')"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import ChartCard from '@/components/ChartCard.vue'
import { pieChart, barChart, lineChart } from '@/utils/echartsOptions'
import {
  getAdminUserList,
  getAdminUserDetail,
  getAdminUserConsultations,
  getAdminUserAssessments,
  getAdminUserDiaries,
  getAdminUserPosts,
  getAdminUserAppointments,
  updateAdminUserStatus,
  resetAdminUserPassword,
  forceLogoutAdminUser,
  getAdminUserStats
} from '@/api/admin'

// 列表查询状态
const query = reactive({
  keyword: '',
  phone: '',
  status: undefined,
  dateRange: null,
  page: 1,
  size: 10
})

const users = ref([])
const total = ref(0)
const loading = ref(false)

function buildQueryParams() {
  const params = { page: query.page, size: query.size }
  if (query.keyword) params.keyword = query.keyword
  if (query.phone) params.phone = query.phone
  if (query.status !== undefined && query.status !== null && query.status !== '') {
    params.status = query.status
  }
  if (Array.isArray(query.dateRange) && query.dateRange.length === 2) {
    params.startDate = query.dateRange[0]
    params.endDate = query.dateRange[1]
  }
  return params
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await getAdminUserList(buildQueryParams())
    users.value = res?.records || []
    total.value = res?.total || 0
  } catch (e) {
    ElMessage.error('用户列表加载失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  loadUsers()
}

function onReset() {
  query.keyword = ''
  query.phone = ''
  query.status = undefined
  query.dateRange = null
  query.page = 1
  loadUsers()
}

// 操作：禁用 / 启用
async function onToggleStatus(row) {
  try {
    await updateAdminUserStatus(row.id, { status: row.status === 1 ? 0 : 1 })
    ElMessage.success(row.status === 1 ? '已禁用并强制下线' : '已启用')
    loadUsers()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '操作失败')
  }
}

async function onResetPassword(row) {
  try {
    await resetAdminUserPassword(row.id)
    ElMessage.success('已重置为默认密码，原会话已失效')
    loadUsers()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '操作失败')
  }
}

async function onForceLogout(row) {
  try {
    await forceLogoutAdminUser(row.id)
    ElMessage.success('已强制下线')
    loadUsers()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '操作失败')
  }
}

// 统计三图
const stats = ref(null)
const statsLoading = ref(false)
const genderChartEl = ref(null)
const ageChartEl = ref(null)
const trendChartEl = ref(null)
let genderChart, ageChart, trendChart

function isEmpty(list) {
  return !list || list.length === 0 || list.every(i => !i.value)
}

async function loadStats() {
  statsLoading.value = true
  try {
    const res = await getAdminUserStats()
    stats.value = res
    await nextTick()
    renderCharts()
  } catch (e) {
    ElMessage.error('用户统计加载失败')
  } finally {
    statsLoading.value = false
  }
}

function renderCharts() {
  if (!stats.value) return
  if (genderChartEl.value) {
    genderChart = genderChart || echarts.init(genderChartEl.value)
    genderChart.setOption(pieChart(stats.value.genderDistribution || []))
  }
  if (ageChartEl.value) {
    ageChart = ageChart || echarts.init(ageChartEl.value)
    const age = stats.value.ageDistribution || []
    ageChart.setOption(barChart(age.map(i => i.name), [{ name: '用户数', data: age.map(i => i.value) }], { legend: false }))
  }
  if (trendChartEl.value) {
    trendChart = trendChart || echarts.init(trendChartEl.value)
    const trend = stats.value.registrationTrend || []
    trendChart.setOption(lineChart(trend.map(i => i.name), [{ name: '新增用户', data: trend.map(i => i.value) }], { legend: false, rotate: 45 }))
  }
}

function onResize() {
  genderChart?.resize()
  ageChart?.resize()
  trendChart?.resize()
}

// 详情抽屉
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const activeTab = ref('consultations')
const currentUserId = ref(null)

function newTabState() {
  return reactive({ records: [], total: 0, page: 1, size: 10, loading: false, loaded: false })
}
const tabs = {
  consultations: newTabState(),
  assessments: newTabState(),
  diaries: newTabState(),
  posts: newTabState(),
  appointments: newTabState()
}

async function openDetail(row) {
  currentUserId.value = row.id
  detailVisible.value = true
  detail.value = null
  detailLoading.value = true
  Object.keys(tabs).forEach(k => {
    tabs[k].records = []
    tabs[k].total = 0
    tabs[k].page = 1
    tabs[k].loaded = false
    tabs[k].loading = false
  })
  activeTab.value = 'consultations'
  try {
    const res = await getAdminUserDetail(row.id)
    detail.value = res
    await loadTab('consultations')
  } catch (e) {
    ElMessage.error('用户详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function onTabChange(name) {
  if (tabs[name] && !tabs[name].loaded) {
    await loadTab(name)
  }
}

async function loadTab(name) {
  const tab = tabs[name]
  if (!currentUserId.value) return
  tab.loading = true
  try {
    const params = { page: tab.page, size: tab.size }
    let res
    switch (name) {
      case 'consultations':
        res = await getAdminUserConsultations(currentUserId.value, params); break
      case 'assessments':
        res = await getAdminUserAssessments(currentUserId.value, params); break
      case 'diaries':
        res = await getAdminUserDiaries(currentUserId.value, params); break
      case 'posts':
        res = await getAdminUserPosts(currentUserId.value, params); break
      case 'appointments':
        res = await getAdminUserAppointments(currentUserId.value, params); break
    }
    tab.records = res?.records || []
    tab.total = res?.total || 0
    tab.loaded = true
  } catch (e) {
    ElMessage.error('记录加载失败')
  } finally {
    tab.loading = false
  }
}

function fmtTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadUsers()
  loadStats()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  genderChart?.dispose()
  ageChart?.dispose()
  trendChart?.dispose()
})
</script>

<style lang="scss" scoped>
.users-page {
  padding: 16px;
  background-color: #F7F8FA;
  min-height: 100%;
}

.users-search {
  background: #FFFFFF;
  padding: 16px 16px 0;
  border: 1px solid #E5E7EB;
  border-radius: 4px;
  margin-bottom: 16px;
}

.users-stats {
  margin-bottom: 16px;
}

.users-table-card {
  border: 1px solid #E5E7EB;
  border-radius: 4px;

  :deep(.el-card__body) {
    padding: 16px;
  }
}

.user-name-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.4;

  .user-nickname {
    color: #1F2937;
    font-size: 14px;
  }

  .user-username {
    color: #6B7280;
    font-size: 12px;
  }
}

.users-pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.chart-fill {
  width: 100%;
  height: 100%;
  min-height: 240px;
}

.detail-drawer {
  padding: 16px 20px;

  .detail-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    h3 {
      margin: 0;
      font-size: 18px;
      color: #1F2937;
    }
  }

  .detail-desc {
    margin-bottom: 16px;
  }

  .detail-tabs {
    margin-top: 8px;
  }

  .tab-pager {
    margin-top: 12px;
    display: flex;
    justify-content: flex-end;
  }
}

// 操作列按钮自然流式排列，不截断为省略号
:deep(.col-action .cell) {
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
