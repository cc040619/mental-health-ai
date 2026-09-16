<template>
    <div class="appointment-page">
        <PageHead title="预约管理">
            <template #buttons>
                <el-button @click="openStats">预约统计</el-button>
                <el-button type="primary" @click="openReviews">评价管理</el-button>
            </template>
        </PageHead>

        <el-form inline class="search-bar">
            <el-form-item label="状态">
                <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
                    <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
                </el-select>
            </el-form-item>
            <el-form-item label="咨询师">
                <el-select v-model="query.consultantId" placeholder="全部咨询师" clearable filterable
                           style="width: 170px">
                    <el-option v-for="c in consultants" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
            </el-form-item>
            <el-form-item label="来访者">
                <el-input v-model="query.keyword" placeholder="昵称/用户名" clearable style="width: 160px"
                          @keyup.enter="onSearch" />
            </el-form-item>
            <el-form-item label="预约日期">
                <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
                                start-placeholder="开始日期" end-placeholder="结束日期"
                                value-format="YYYY-MM-DD" style="width: 240px" />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" @click="onSearch">查询</el-button>
                <el-button @click="onReset">重置</el-button>
            </el-form-item>
        </el-form>

        <el-card shadow="never" class="table-card">
            <el-table :data="tableData" v-loading="loading" empty-text="暂无预约">
                <el-table-column prop="id" label="编号" width="80" />
                <el-table-column label="来访者" min-width="140">
                    <template #default="{ row }">
                        {{ row.userNickname || `用户${row.userId}` }}
                        <div v-if="row.userPhone" class="sub-text">{{ row.userPhone }}</div>
                    </template>
                </el-table-column>
                <el-table-column prop="consultantName" label="咨询师" min-width="100" />
                <el-table-column label="预约时间" min-width="190">
                    <template #default="{ row }">
                        {{ row.appointmentDate }} {{ row.startTime }} - {{ row.endTime }}
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                    <template #default="{ row }">
                        <el-tag :type="statusTagType(row.status)" size="small" round>{{ row.statusLabel }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="评价" width="70">
                    <template #default="{ row }">
                        <span v-if="row.evaluated" style="color: #E6A23C">⭐ 已评</span>
                        <span v-else class="sub-text">-</span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="300" fixed="right">
                    <template #default="{ row }">
                        <el-button text type="primary" @click="openDetail(row)">详情</el-button>
                        <el-button text type="primary" @click="enterChat(row)">留言</el-button>
                        <el-button v-if="canReschedule(row)" text type="warning" @click="openReschedule(row)">改期</el-button>
                        <template v-if="row.status === 'PENDING'">
                            <el-button text type="success" @click="changeStatus(row, 'CONFIRMED')">确认</el-button>
                            <el-button text type="danger" @click="openCancel(row)">取消</el-button>
                        </template>
                        <template v-else-if="row.status === 'CONFIRMED'">
                            <el-button text type="success" @click="changeStatus(row, 'COMPLETED')">完成</el-button>
                            <el-button text type="danger" @click="openCancel(row)">取消</el-button>
                        </template>
                    </template>
                </el-table-column>
            </el-table>
            <el-pagination v-model:current-page="query.page" :page-size="pageSize" :total="total"
                           layout="total, prev, pager, next" class="pagination"
                           @current-change="load" />
        </el-card>

        <!-- 预约详情抽屉 -->
        <el-drawer v-model="detailVisible" title="预约详情" size="480px" destroy-on-close>
            <div v-if="detail" class="detail-box">
                <el-descriptions :column="1" border>
                    <el-descriptions-item label="编号">{{ detail.id }}</el-descriptions-item>
                    <el-descriptions-item label="状态">
                        <el-tag :type="statusTagType(detail.status)" size="small">{{ detail.statusLabel }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="来访者">
                        {{ detail.userNickname || `用户${detail.userId}` }}
                        <span v-if="detail.userPhone" class="sub-text">（{{ detail.userPhone }}）</span>
                    </el-descriptions-item>
                    <el-descriptions-item label="咨询师">{{ detail.consultantName }}</el-descriptions-item>
                    <el-descriptions-item label="预约时间">
                        {{ detail.appointmentDate }} {{ detail.startTime }} - {{ detail.endTime }}
                    </el-descriptions-item>
                    <el-descriptions-item label="问题描述">
                        {{ detail.problemDesc || '（未填写）' }}
                    </el-descriptions-item>
                    <el-descriptions-item v-if="detail.status === 'CANCELED'" label="取消原因">
                        {{ detail.cancelReason || '（未填写）' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item>
                </el-descriptions>

                <div v-if="detail.reviewContent || detail.reviewRating" class="detail-review">
                    <div class="block-title">来访者评价</div>
                    <div class="review-stars">{{ '⭐'.repeat(detail.reviewRating || 0) }}</div>
                    <p v-if="detail.reviewContent" class="review-content">{{ detail.reviewContent }}</p>
                    <div v-if="detail.reviewReply" class="review-reply">
                        <span class="reply-label">回复：</span>{{ detail.reviewReply }}
                    </div>
                    <div v-else class="sub-text">暂未回复</div>
                </div>

                <div class="detail-record">
                    <div class="block-title">咨询记录</div>
                    <template v-if="consultationRecord">
                        <div class="record-item">
                            <span class="record-label">主诉</span>
                            <span class="record-text">{{ consultationRecord.chiefComplaint || '（未填写）' }}</span>
                        </div>
                        <div class="record-item">
                            <span class="record-label">分析</span>
                            <span class="record-text">{{ consultationRecord.analysis || '（未填写）' }}</span>
                        </div>
                        <div class="record-item">
                            <span class="record-label">干预方案</span>
                            <span class="record-text">{{ consultationRecord.interventionPlan || '（未填写）' }}</span>
                        </div>
                        <div class="record-item">
                            <span class="record-label">下次建议</span>
                            <span class="record-text">{{ consultationRecord.nextSuggestion || '（未填写）' }}</span>
                        </div>
                        <div class="sub-text" v-if="consultationRecord.updatedAt">
                            更新于 {{ consultationRecord.updatedAt }}
                        </div>
                    </template>
                    <div v-else class="sub-text">未填写</div>
                </div>
            </div>
        </el-drawer>

        <!-- 改期弹窗 -->
        <el-dialog v-model="rescheduleVisible" title="修改预约时间" width="460px" destroy-on-close>
            <div v-if="rescheduleRow" class="reschedule-info">
                当前时间：{{ rescheduleRow.appointmentDate }} {{ rescheduleRow.startTime }} -
                {{ rescheduleRow.endTime }}（{{ rescheduleRow.consultantName }}）
            </div>
            <el-select v-model="newSlotId" placeholder="请选择新时段" style="width: 100%; margin-top: 12px"
                       v-loading="slotLoading" :disabled="slotLoading">
                <el-option-group v-for="g in slotGroups" :key="g.date" :label="g.date">
                    <el-option v-for="s in g.children" :key="s.id"
                               :label="`${s.startTime} - ${s.endTime}`" :value="s.id" />
                </el-option-group>
            </el-select>
            <div v-if="!slotLoading && slotGroups.length === 0" class="sub-text" style="margin-top: 8px">
                该咨询师近期暂无可调整的空闲时段
            </div>
            <template #footer>
                <el-button @click="rescheduleVisible = false">取消</el-button>
                <el-button type="primary" :loading="rescheduleSubmitting" @click="submitReschedule">确认改期</el-button>
            </template>
        </el-dialog>

        <!-- 评价管理抽屉 -->
        <el-drawer v-model="reviewVisible" title="评价管理" size="72%" destroy-on-close @closed="onReviewsClosed">
            <el-form inline class="search-bar">
                <el-form-item label="咨询师">
                    <el-select v-model="reviewQuery.consultantId" placeholder="全部咨询师" clearable filterable
                               style="width: 160px">
                        <el-option v-for="c in consultants" :key="c.id" :label="c.name" :value="c.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="评分">
                    <el-select v-model="reviewQuery.rating" placeholder="全部" clearable style="width: 110px">
                        <el-option v-for="n in 5" :key="n" :label="`${n} 星`" :value="n" />
                    </el-select>
                </el-form-item>
                <el-form-item label="关键字">
                    <el-input v-model="reviewQuery.keyword" placeholder="评价内容/来访者" clearable
                              style="width: 180px" @keyup.enter="onReviewSearch" />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="onReviewSearch">查询</el-button>
                    <el-button @click="onReviewReset">重置</el-button>
                </el-form-item>
            </el-form>

            <el-table :data="reviewList" v-loading="reviewLoading" empty-text="暂无评价">
                <el-table-column label="来访者" min-width="110">
                    <template #default="{ row }">
                        {{ row.userNickname || `用户${row.userId}` }}
                        <div class="sub-text">{{ row.appointmentDate }} 咨询</div>
                    </template>
                </el-table-column>
                <el-table-column prop="consultantName" label="咨询师" min-width="90" />
                <el-table-column label="评分" width="90">
                    <template #default="{ row }">{{ '⭐'.repeat(row.rating) }}</template>
                </el-table-column>
                <el-table-column label="评价内容" min-width="180" show-overflow-tooltip>
                    <template #default="{ row }">
                        <div>{{ row.content || '（未填写）' }}</div>
                        <div v-if="row.replyContent" class="review-reply">
                            <span class="reply-label">回复：</span>{{ row.replyContent }}
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="时间" width="150">
                    <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="130" fixed="right">
                    <template #default="{ row }">
                        <el-button text type="primary" @click="openReply(row)">
                            {{ row.replyContent ? '修改回复' : '回复' }}
                        </el-button>
                        <el-button text type="danger" @click="removeReview(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <el-pagination v-model:current-page="reviewQuery.page" :page-size="reviewPageSize"
                           :total="reviewTotal" layout="total, prev, pager, next" class="pagination"
                           @current-change="loadReviews" />
        </el-drawer>

        <!-- 预约统计抽屉 -->
        <el-drawer v-model="statsVisible" title="预约统计" size="78%" destroy-on-close
                   @opened="onStatsOpened" @closed="disposeStatsCharts">
            <div class="stats-toolbar">
                <el-radio-group v-model="statsRange" @change="loadStats">
                    <el-radio-button label="7D">近 7 天</el-radio-button>
                    <el-radio-button label="30D">近 30 天</el-radio-button>
                    <el-radio-button label="90D">近 90 天</el-radio-button>
                </el-radio-group>
            </div>
            <div v-loading="statsLoading" class="stats-grid">
                <div class="chart-card wide">
                    <div class="chart-title">预约量趋势</div>
                    <div ref="trendChartEl" class="chart-box"></div>
                </div>
                <div class="chart-card">
                    <div class="chart-title">状态占比</div>
                    <div ref="statusPieEl" class="chart-box"></div>
                </div>
                <div class="chart-card">
                    <div class="chart-title">预约完成率</div>
                    <div ref="rateGaugeEl" class="chart-box"></div>
                </div>
                <div class="chart-card">
                    <div class="chart-title">热门时段</div>
                    <div ref="hotHoursEl" class="chart-box"></div>
                </div>
                <div class="chart-card wide">
                    <div class="chart-title">咨询师预约量排行 TOP10</div>
                    <div ref="rankingEl" class="chart-box"></div>
                </div>
            </div>
        </el-drawer>
    </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import PageHead from '@/components/PageHead.vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import {
    getAdminAppointmentPage, getAdminAppointmentDetail, changeAppointmentStatus,
    rescheduleAppointment, getFreeSlots, getAdminReviewPage, replyReview, deleteReview
} from '@/api/appointment'
import { getAdminConsultantPage, getAdminAppointmentRecord } from '@/api/consultant'
import { getAppointmentStats } from '@/api/admin'
import { lineChart, pieChart, barChart, horizBarChart, gaugeChart } from '@/utils/echartsOptions'

const router = useRouter()

const STATUS_OPTIONS = [
    { value: 'PENDING', label: '待确认' },
    { value: 'CONFIRMED', label: '已确认' },
    { value: 'COMPLETED', label: '已完成' },
    { value: 'CANCELED', label: '已取消' }
]
const STATUS_LABEL = Object.fromEntries(STATUS_OPTIONS.map(s => [s.value, s.label]))

const statusTagType = (status) => ({
    PENDING: 'warning', CONFIRMED: 'success', COMPLETED: 'primary', CANCELED: 'info'
}[status] || 'info')

// 改期条件：待确认随时可改；已确认仅开始前可改（与后端 canCancelBeforeStart 同口径）
const canReschedule = (row) => {
    if (row.status === 'PENDING') return true
    if (row.status !== 'CONFIRMED') return false
    return new Date(`${row.appointmentDate}T${row.startTime}`).getTime() > Date.now()
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')

// ===== 列表与筛选 =====
const pageSize = 10
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dateRange = ref([])
const query = ref({ status: '', consultantId: null, keyword: '', page: 1 })
const consultants = ref([])

const loadConsultants = () => {
    getAdminConsultantPage({ status: 1, page: 1, size: 100 }).then(data => {
        consultants.value = data.records || []
    }).catch(() => {})
}

const load = () => {
    loading.value = true
    const params = { page: query.value.page, size: pageSize }
    if (query.value.status) params.status = query.value.status
    if (query.value.consultantId) params.consultantId = query.value.consultantId
    if (query.value.keyword) params.keyword = query.value.keyword
    if (dateRange.value?.length === 2) {
        params.startDate = dateRange.value[0]
        params.endDate = dateRange.value[1]
    }
    getAdminAppointmentPage(params).then(data => {
        tableData.value = data.records || []
        total.value = data.total || 0
    }).finally(() => {
        loading.value = false
    })
}

const onSearch = () => { query.value.page = 1; load() }
const onReset = () => {
    query.value = { status: '', consultantId: null, keyword: '', page: 1 }
    dateRange.value = []
    load()
}

// ===== 状态变更（取消需填原因） =====
const changeStatus = (row, target) => {
    const label = { CONFIRMED: '确认', COMPLETED: '完成' }[target]
    ElMessageBox.confirm(
        `确定${label}「${row.userNickname || row.userId}」与 ${row.consultantName} ${row.appointmentDate} ${row.startTime} 的预约吗？`,
        `${label}预约`, { confirmButtonText: '确定', cancelButtonText: '再想想', type: 'warning' }
    ).then(() => changeAppointmentStatus(row.id, target)).then(() => {
        ElNotification({ title: `已${label}`, message: '操作已生效', type: 'success' })
        load()
    }).catch(() => {})
}

const openCancel = (row) => {
    ElMessageBox.prompt(
        `请填写取消「${row.userNickname || row.userId}」${row.appointmentDate} ${row.startTime} 预约的原因，来访者将收到通知。`,
        '取消预约',
        {
            confirmButtonText: '确认取消', cancelButtonText: '再想想', type: 'warning',
            inputType: 'textarea', inputPlaceholder: '请输入取消原因（500 字以内）',
            inputValidator: (v) => (v && v.trim() ? true : '取消原因不能为空'),
            inputAttrs: { maxlength: 500, rows: 3 }
        }
    ).then(({ value }) => changeAppointmentStatus(row.id, 'CANCELED', value.trim())).then(() => {
        ElNotification({ title: '已取消', message: '来访者将收到取消通知', type: 'success' })
        load()
    }).catch(() => {})
}

// ===== 详情 =====
const detailVisible = ref(false)
const detail = ref(null)
const consultationRecord = ref(null)

const openDetail = (row) => {
    detail.value = null
    consultationRecord.value = null
    detailVisible.value = true
    getAdminAppointmentDetail(row.id).then(data => { detail.value = data })
    // 只读联动：咨询师保存的咨询记录（无记录返回 null，展示「未填写」）
    getAdminAppointmentRecord(row.id).then(data => { consultationRecord.value = data })
}

// ===== 改期 =====
const rescheduleVisible = ref(false)
const rescheduleRow = ref(null)
const newSlotId = ref(null)
const freeSlots = ref([])
const slotLoading = ref(false)
const rescheduleSubmitting = ref(false)

// 按日期分组供 el-option-group 使用
const slotGroups = computed(() => {
    const map = new Map()
    freeSlots.value.forEach(s => {
        if (!map.has(s.slotDate)) map.set(s.slotDate, [])
        map.get(s.slotDate).push(s)
    })
    return [...map.entries()].map(([date, children]) => ({ date, children }))
})

const openReschedule = (row) => {
    rescheduleRow.value = row
    newSlotId.value = null
    freeSlots.value = []
    rescheduleVisible.value = true
    slotLoading.value = true
    getFreeSlots(row.consultantId).then(data => {
        freeSlots.value = data || []
    }).finally(() => {
        slotLoading.value = false
    })
}

const submitReschedule = () => {
    if (!newSlotId.value) {
        ElMessage.warning('请选择新时段')
        return
    }
    rescheduleSubmitting.value = true
    rescheduleAppointment(rescheduleRow.value.id, newSlotId.value).then(() => {
        ElNotification({ title: '改期成功', message: '来访者将收到改期通知', type: 'success' })
        rescheduleVisible.value = false
        load()
    }).finally(() => {
        rescheduleSubmitting.value = false
    })
}

// ===== 留言 =====
const enterChat = (row) => {
    router.push(`/back/appointment-chat/${row.id}`)
}

// ===== 评价管理 =====
const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewList = ref([])
const reviewTotal = ref(0)
const reviewPageSize = 10
const reviewQuery = ref({ consultantId: null, rating: null, keyword: '', page: 1 })

const openReviews = () => {
    reviewVisible.value = true
    loadReviews()
}

const loadReviews = () => {
    reviewLoading.value = true
    const params = { page: reviewQuery.value.page, size: reviewPageSize }
    if (reviewQuery.value.consultantId) params.consultantId = reviewQuery.value.consultantId
    if (reviewQuery.value.rating) params.rating = reviewQuery.value.rating
    if (reviewQuery.value.keyword) params.keyword = reviewQuery.value.keyword
    getAdminReviewPage(params).then(data => {
        reviewList.value = data.records || []
        reviewTotal.value = data.total || 0
    }).finally(() => {
        reviewLoading.value = false
    })
}

const onReviewSearch = () => { reviewQuery.value.page = 1; loadReviews() }
const onReviewReset = () => {
    reviewQuery.value = { consultantId: null, rating: null, keyword: '', page: 1 }
    loadReviews()
}
const onReviewsClosed = () => {
    // 评价变更（回复/删除）可能影响列表评分标记，回到列表时刷新
    load()
}

const openReply = (row) => {
    ElMessageBox.prompt('回复该评价（仅支持单级回复，重复回复将覆盖）', '回复评价', {
        confirmButtonText: '提交回复', cancelButtonText: '取消',
        inputType: 'textarea', inputValue: row.replyContent || '',
        inputPlaceholder: '请输入回复内容（500 字以内）',
        inputValidator: (v) => (v && v.trim() ? true : '回复内容不能为空'),
        inputAttrs: { maxlength: 500, rows: 4 }
    }).then(({ value }) => replyReview(row.id, value.trim())).then(() => {
        ElMessage.success('回复已保存')
        loadReviews()
    }).catch(() => {})
}

const removeReview = (row) => {
    ElMessageBox.confirm(
        `确定删除该评价吗？删除后咨询师评分将基于剩余评价重算，该预约可被重新评价。`,
        '删除评价', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    ).then(() => deleteReview(row.id)).then(() => {
        ElMessage.success('评价已删除')
        loadReviews()
    }).catch(() => {})
}

// ===== 预约统计（复用数据看板 /data-analytics/appointment-stats） =====
const statsVisible = ref(false)
const statsLoading = ref(false)
const statsRange = ref('7D')
const trendChartEl = ref(null)
const statusPieEl = ref(null)
const rateGaugeEl = ref(null)
const hotHoursEl = ref(null)
const rankingEl = ref(null)
const statsCharts = []

const openStats = () => {
    statsVisible.value = true
}

const onStatsOpened = () => {
    loadStats()
}

const disposeStatsCharts = () => {
    statsCharts.forEach(c => c?.dispose())
    statsCharts.length = 0
}

const renderStatsChart = (el, option) => {
    if (!el) return
    // destroy-on-close 抽屉每次打开 DOM 都是新的，先兜底释放旧实例再 init
    const chart = echarts.init(el)
    if (option) chart.setOption(option, true)
    else chart.clear()
    statsCharts.push(chart)
}

const loadStats = () => {
    statsLoading.value = true
    disposeStatsCharts()
    getAppointmentStats({ range: statsRange.value }).then(data => {
        nextTick(() => {
            const trend = data?.dailyTrend || []
            renderStatsChart(trendChartEl.value, trend.length
                ? lineChart(trend.map(i => i.name),
                    [{ name: '预约量', data: trend.map(i => i.value), color: '#409EFF', area: true }],
                    { legend: false, rotate: trend.length > 15 ? 40 : 0 })
                : null)

            const statusData = (data?.statusDistribution || []).map(i => ({
                name: STATUS_LABEL[i.name] || i.name, value: i.value
            }))
            renderStatsChart(statusPieEl.value, statusData.length ? pieChart(statusData, { ring: true }) : null)

            renderStatsChart(rateGaugeEl.value, gaugeChart(data?.completionRate ?? null))

            // 热门时段按小时升序展示，名称补「时」
            const hours = [...(data?.hotHours || [])].sort((a, b) => Number(a.name) - Number(b.name))
            renderStatsChart(hotHoursEl.value, hours.length
                ? barChart(hours.map(i => `${i.name}时`),
                    [{ name: '预约量', data: hours.map(i => i.value), color: '#E6A23C' }])
                : null)

            renderStatsChart(rankingEl.value,
                data?.consultantRanking?.length ? horizBarChart(data.consultantRanking, '#67C23A') : null)
        })
    }).finally(() => {
        statsLoading.value = false
    })
}

onBeforeUnmount(() => {
    disposeStatsCharts()
})

loadConsultants()
</script>

<style scoped>
.search-bar { margin-bottom: 16px; }
.table-card { background: #fff; }
.pagination { margin-top: 16px; text-align: right; }
.sub-text { font-size: 12px; color: #999; }

.detail-box { padding: 0 4px; }
.detail-review { margin-top: 20px; }
.block-title { font-weight: 600; margin-bottom: 10px; color: #303133; }
.review-stars { font-size: 14px; margin-bottom: 6px; }
.review-content { margin: 0 0 8px; font-size: 14px; line-height: 1.8; color: #606266; }
.review-reply {
    margin-top: 8px; padding: 8px 12px;
    background: #F7F8FA; border-left: 3px solid #409EFF; border-radius: 4px;
    font-size: 13px; line-height: 1.7; color: #606266;
}
.reply-label { color: #409EFF; font-weight: 600; }

.detail-record { margin-top: 20px; }
.record-item {
    margin-bottom: 8px; padding: 8px 12px;
    background: #F7F8FA; border-radius: 4px;
    font-size: 13px; line-height: 1.7; color: #606266;
}
.record-label {
    display: inline-block; width: 70px; color: #909399; font-weight: 600;
}
.record-text { white-space: pre-wrap; word-break: break-word; }

.reschedule-info {
    padding: 10px 12px; background: #F7F8FA; border-radius: 6px;
    font-size: 13px; color: #606266; line-height: 1.6;
}

.stats-toolbar { margin-bottom: 16px; }
.stats-grid {
    display: grid; grid-template-columns: 1fr 1fr; gap: 16px;
}
.chart-card {
    background: #fff; border: 1px solid #E5E7EB; border-radius: 10px; padding: 14px 16px;
}
.chart-card.wide { grid-column: 1 / -1; }
.chart-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.chart-box { height: 300px; }
</style>
