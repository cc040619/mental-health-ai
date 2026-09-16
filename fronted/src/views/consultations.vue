<template>
    <div>
        <PageHead title="咨询记录管理" />

        <!-- 筛选栏 -->
        <div class="filter-card">
            <el-input v-model="query.keyword" placeholder="用户昵称 / 会话标题" clearable style="width: 200px"
                @keyup.enter="handleSearch" />
            <el-select v-model="query.scene" placeholder="场景" clearable style="width: 130px">
                <el-option v-for="s in sceneOptions" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
            <el-input v-model="query.emotionTag" placeholder="情绪标签" clearable style="width: 120px" />
            <div class="rating-range">
                <el-select v-model="query.ratingMin" placeholder="评分≥" clearable style="width: 90px">
                    <el-option v-for="n in 5" :key="n" :label="`${n}星`" :value="n" />
                </el-select>
                <span class="range-sep">~</span>
                <el-select v-model="query.ratingMax" placeholder="评分≤" clearable style="width: 90px">
                    <el-option v-for="n in 5" :key="n" :label="`${n}星`" :value="n" />
                </el-select>
            </div>
            <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
                start-placeholder="开始日期" end-placeholder="结束日期" style="width: 240px" />
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-checkbox v-model="onlyCrisis" border
                style="margin-left: auto">仅看危机会话</el-checkbox>
        </div>

        <!-- 会话列表（min-height ≈ 表头+10行满页高度：数据不满页时表格留白，分页器位置固定） -->
        <el-table :data="tableData" v-loading="loading"
            style="width: 100%; min-height: 550px; margin-top: 16px"
            :row-class-name="rowCrisisClass">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="userNickname" label="用户" width="120" show-overflow-tooltip />
            <el-table-column prop="sessionTitle" label="会话标题" min-width="200" show-overflow-tooltip />
            <el-table-column label="场景" width="110">
                <template #default="scope">
                    <el-tag type="info" size="small" effect="plain">{{ scope.row.sceneLabel || '其他' }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column label="时长" width="100">
                <template #default="scope">{{ fmtDuration(scope.row.durationSeconds) }}</template>
            </el-table-column>
            <el-table-column prop="msgCount" label="消息数" width="80" />
            <el-table-column label="情绪标签" width="110">
                <template #default="scope">
                    <el-tag v-if="scope.row.emotionTag" size="small" effect="plain">{{ scope.row.emotionTag }}</el-tag>
                    <span v-else class="muted">—</span>
                </template>
            </el-table-column>
            <el-table-column label="评分" width="110">
                <template #default="scope">
                    <span v-if="scope.row.rating" class="stars">{{ stars(scope.row.rating) }}</span>
                    <span v-else class="muted">未评价</span>
                </template>
            </el-table-column>
            <el-table-column label="标记" width="150">
                <template #default="scope">
                    <el-tag v-if="scope.row.isHighQuality === 1" type="primary" size="small">高质量</el-tag>
                    <el-tag v-if="scope.row.isCrisis === 1" type="danger" size="small" style="margin-left: 6px">危机</el-tag>
                    <span v-if="scope.row.isHighQuality !== 1 && scope.row.isCrisis !== 1" class="muted">—</span>
                </template>
            </el-table-column>
            <el-table-column label="开始时间" width="160">
                <template #default="scope">{{ fmtDateTime(scope.row.startedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
                <template #default="scope">
                    <div class="op-cell">
                        <el-button text type="primary" @click="viewDetail(scope.row)">详情</el-button>
                        <el-button text type="warning" @click="openMark(scope.row)">标记</el-button>
                        <el-button text type="success" @click="onExport(scope.row)">导出</el-button>
                    </div>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination style="margin-top: 20px" :current-page="query.page" :page-size="query.size"
            :total="total" layout="total, prev, pager, next" @current-change="onPageChange" />

        <!-- 详情抽屉 -->
        <el-drawer v-model="drawerVisible" title="会话详情" size="800px" destroy-on-close @closed="onDrawerClosed">
            <div v-loading="detailLoading" class="detail-body">
                <template v-if="detail">
                    <!-- 统计条 -->
                    <div class="stat-bar">
                        <div class="stat-item">
                            <div class="stat-label">用户</div>
                            <div class="stat-value">{{ detail.userNickname || '-' }}</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-label">时长</div>
                            <div class="stat-value">{{ fmtDuration(detail.durationSeconds) }}</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-label">消息数</div>
                            <div class="stat-value">{{ detail.msgCount ?? '-' }}</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-label">场景</div>
                            <div class="stat-value">{{ detail.sceneLabel || '其他' }}</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-label">评分</div>
                            <div class="stat-value">{{ detail.rating ? stars(detail.rating) : '未评价' }}</div>
                        </div>
                    </div>

                    <!-- 情绪变化折线 -->
                    <div class="section-card">
                        <h4 class="section-title">情绪变化（用户消息）</h4>
                        <div v-show="hasEmotionData" ref="chartRef" class="emotion-chart"></div>
                        <div v-if="!hasEmotionData" class="empty-chart">暂无情绪数值数据（情绪分析完成后自动生成）</div>
                    </div>

                    <!-- AI 总结 -->
                    <div class="section-card">
                        <h4 class="section-title">AI 总结</h4>
                        <template v-if="detail.summaryObj && detail.summaryObj.status === 'READY'">
                            <p class="summary-text">{{ detail.summaryObj.summary }}</p>
                            <template v-if="detail.summaryObj.coreIssues?.length">
                                <div class="summary-sub">核心问题</div>
                                <ul class="summary-list">
                                    <li v-for="(item, i) in detail.summaryObj.coreIssues" :key="'i' + i">{{ item }}</li>
                                </ul>
                            </template>
                            <template v-if="detail.summaryObj.suggestions?.length">
                                <div class="summary-sub">建议</div>
                                <ul class="summary-list">
                                    <li v-for="(item, i) in detail.summaryObj.suggestions" :key="'s' + i">{{ item }}</li>
                                </ul>
                            </template>
                        </template>
                        <p v-else class="muted">暂无 AI 总结</p>
                    </div>

                    <!-- 用户评价 -->
                    <div class="section-card">
                        <h4 class="section-title">用户评价</h4>
                        <template v-if="detail.rating">
                            <div class="stars detail-stars">{{ stars(detail.rating) }}</div>
                            <p class="summary-text">{{ detail.feedback || '（无文字反馈）' }}</p>
                        </template>
                        <p v-else class="muted">未评价</p>
                    </div>

                    <!-- 消息时间轴 -->
                    <div class="section-card">
                        <h4 class="section-title">对话记录（{{ detail.messages?.length || 0 }} 条）</h4>
                        <div class="msg-list">
                            <div v-for="m in detail.messages" :key="m.id" class="msg-row"
                                :class="m.senderType === 1 ? 'from-user' : 'from-ai'">
                                <div class="msg-bubble">
                                    <div class="msg-meta">
                                        <span class="msg-sender">{{ m.senderType === 1 ? '用户' : 'AI' }}</span>
                                        <span class="msg-time">{{ fmtDateTime(m.createdAt) }}</span>
                                        <el-tag v-if="m.emotionTag" size="small" effect="plain"
                                            class="msg-emotion">{{ m.emotionTag }}</el-tag>
                                    </div>
                                    <div class="msg-content">{{ m.content }}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </template>
            </div>
        </el-drawer>

        <!-- 标记弹窗 -->
        <el-dialog v-model="markVisible" title="标记会话" width="420px" :close-on-click-modal="false">
            <el-form label-width="90px">
                <el-form-item label="高质量">
                    <el-switch v-model="markForm.isHighQuality" :active-value="1" :inactive-value="0" />
                    <span class="mark-tip">AI 训练参考</span>
                </el-form-item>
                <el-form-item label="危机会话">
                    <el-switch v-model="markForm.isCrisis" :active-value="1" :inactive-value="0" />
                    <span class="mark-tip">运营关注标记（列表行高亮）</span>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="markForm.note" type="textarea" :rows="3" maxlength="500" show-word-limit
                        placeholder="标记备注（保存后覆盖原备注）" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="markVisible = false">取消</el-button>
                <el-button type="primary" :loading="markSaving" @click="saveMark">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import PageHead from '@/components/PageHead.vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
    getAdminConsultationPage,
    getAdminConsultationDetail,
    markConsultation,
    exportConsultation
} from '@/api/admin'

const sceneOptions = [
    { value: 'EMOTION', label: '情绪疏导' },
    { value: 'RELATIONSHIP', label: '人际关系' },
    { value: 'ACADEMIC', label: '学业压力' },
    { value: 'SLEEP', label: '睡眠问题' },
    { value: 'OTHER', label: '其他' }
]

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dateRange = ref(null)

const query = reactive({
    keyword: '',
    scene: null,
    emotionTag: '',
    ratingMin: null,
    ratingMax: null,
    page: 1,
    size: 10
})

// 仅看危机会话（布尔勾选，请求参数处换算为 isCrisis=1）
const onlyCrisis = ref(false)

const rowCrisisClass = ({ row }) => (row.isCrisis === 1 ? 'crisis-row' : '')

const buildParams = () => ({
    keyword: query.keyword || undefined,
    scene: query.scene || undefined,
    emotionTag: query.emotionTag || undefined,
    ratingMin: query.ratingMin ?? undefined,
    ratingMax: query.ratingMax ?? undefined,
    startDate: dateRange.value?.[0] || undefined,
    endDate: dateRange.value?.[1] || undefined,
    isCrisis: onlyCrisis.value ? 1 : undefined,
    page: query.page,
    size: query.size
})

const handleSearch = () => {
    query.page = 1
    loadList()
}

const loadList = () => {
    loading.value = true
    getAdminConsultationPage(buildParams()).then(res => {
        tableData.value = res.records || []
        total.value = res.total || 0
    }).finally(() => {
        loading.value = false
    })
}

const handleReset = () => {
    query.keyword = ''
    query.scene = null
    query.emotionTag = ''
    query.ratingMin = null
    query.ratingMax = null
    onlyCrisis.value = false
    dateRange.value = null
    query.page = 1
    loadList()
}

const onPageChange = (page) => {
    query.page = page
    loadList()
}

// ==================== 详情抽屉 ====================

const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const hasEmotionData = ref(false)
const chartRef = ref(null)
let chartInstance = null

const viewDetail = (row) => {
    drawerVisible.value = true
    detailLoading.value = true
    detail.value = null
    getAdminConsultationDetail(row.id).then(res => {
        detail.value = res
        renderChart(res.messages)
    }).finally(() => {
        detailLoading.value = false
    })
}

// 折线图：仅取用户消息中 emotionValue 非空的点（历史消息无值自动跳过）
const renderChart = (messages) => {
    chartInstance?.dispose()
    chartInstance = null
    const points = (messages || []).filter(m =>
        m.senderType === 1 && m.emotionValue !== null && m.emotionValue !== undefined)
    hasEmotionData.value = points.length > 0
    if (!points.length) return
    nextTick(() => {
        if (!chartRef.value) return
        chartInstance = echarts.init(chartRef.value)
        chartInstance.setOption({
            grid: { left: 44, right: 20, top: 30, bottom: 34 },
            tooltip: {
                trigger: 'axis',
                formatter: (params) => {
                    const p = params[0]
                    return `第 ${p.dataIndex + 1} 条用户消息 · ${p.data.name || '未知'}<br/>情绪值 ${p.value}`
                }
            },
            xAxis: { type: 'category', data: points.map((_, i) => i + 1), name: '序号' },
            yAxis: { type: 'value', min: 0, max: 100, name: '情绪值' },
            series: [{
                type: 'line',
                smooth: true,
                symbolSize: 8,
                data: points.map(p => ({ value: Number(p.emotionValue), name: p.emotionTag || '' })),
                label: { show: true, fontSize: 10, position: 'top', formatter: (p) => p.data.name },
                lineStyle: { color: '#409eff' },
                itemStyle: { color: '#409eff' },
                areaStyle: { opacity: 0.08 }
            }]
        })
    })
}

// 抽屉关闭后销毁实例，避免二次打开指向已销毁 DOM
const onDrawerClosed = () => {
    chartInstance?.dispose()
    chartInstance = null
    detail.value = null
}

// ==================== 标记 ====================

const markVisible = ref(false)
const markSaving = ref(false)
const markTargetId = ref(null)
const markForm = reactive({ isHighQuality: 0, isCrisis: 0, note: '' })

const openMark = (row) => {
    markTargetId.value = row.id
    markForm.isHighQuality = row.isHighQuality === 1 ? 1 : 0
    markForm.isCrisis = row.isCrisis === 1 ? 1 : 0
    markForm.note = ''
    markVisible.value = true
}

const saveMark = () => {
    markSaving.value = true
    markConsultation(markTargetId.value, {
        isHighQuality: markForm.isHighQuality,
        isCrisis: markForm.isCrisis,
        note: markForm.note || undefined
    }).then(() => {
        ElMessage.success('标记已保存')
        markVisible.value = false
        loadList()
    }).finally(() => {
        markSaving.value = false
    })
}

// ==================== 导出 ====================

const onExport = async (row) => {
    const res = await exportConsultation(row.id)
    const disposition = res.headers?.['content-disposition'] || ''
    const match = disposition.match(/filename="?([^";]+)"?/)
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = match ? match[1] : `session-${row.id}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
}

// ==================== 格式化 ====================

const pad = (n) => String(n).padStart(2, '0')

const fmtDateTime = (iso) => {
    if (!iso) return '-'
    const d = new Date(iso)
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const fmtDuration = (seconds) => {
    if (seconds === null || seconds === undefined) return '—'
    if (seconds < 60) return '<1分钟'
    const h = Math.floor(seconds / 3600)
    const m = Math.floor((seconds % 3600) / 60)
    return h > 0 ? `${h}小时${m}分钟` : `${m}分钟`
}

const stars = (n) => '★'.repeat(n) + '☆'.repeat(5 - n)

onMounted(() => {
    loadList()
})
</script>

<style lang="scss" scoped>
.filter-card {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
    padding: 16px;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;

    .rating-range {
        display: flex;
        align-items: center;

        .range-sep {
            margin: 0 4px;
            color: #6b7280;
        }
    }
}

/* 操作列按钮单行排列 */
.op-cell {
    display: flex;
    align-items: center;

    :deep(.el-button) {
        padding: 0;
        margin-left: 0;
        margin-right: 12px;
    }

    :deep(.el-button:last-child) {
        margin-right: 0;
    }
}

/* 危机会话行浅红高亮 */
:deep(.el-table .crisis-row) {
    background: #fef2f2;
}

.muted {
    color: #9ca3af;
    font-size: 13px;
}

.stars {
    color: #f59e0b;
    letter-spacing: 2px;
}

.detail-body {
    min-height: 200px;
}

.stat-bar {
    display: flex;
    background: #f7f8fa;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 14px 0;
    margin-bottom: 16px;

    .stat-item {
        flex: 1;
        text-align: center;

        .stat-label {
            font-size: 12px;
            color: #6b7280;
            margin-bottom: 4px;
        }

        .stat-value {
            font-size: 14px;
            font-weight: 500;
            color: #1f2937;
        }
    }
}

.section-card {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 16px;

    .section-title {
        margin: 0 0 12px;
        font-size: 14px;
        font-weight: 600;
        color: #1f2937;
    }
}

.emotion-chart {
    width: 100%;
    height: 260px;
}

.empty-chart {
    height: 120px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #9ca3af;
    font-size: 13px;
    background: #f7f8fa;
    border-radius: 6px;
}

.summary-text {
    margin: 0;
    font-size: 13px;
    line-height: 1.7;
    color: #1f2937;
    white-space: pre-wrap;
}

.summary-sub {
    font-size: 13px;
    font-weight: 600;
    color: #374151;
    margin: 10px 0 4px;
}

.summary-list {
    margin: 0;
    padding-left: 18px;
    font-size: 13px;
    line-height: 1.8;
    color: #4b5563;
}

.detail-stars {
    font-size: 18px;
    margin-bottom: 6px;
}

/* 消息时间轴：用户右蓝、AI 左白 */
.msg-list {
    max-height: 420px;
    overflow-y: auto;
    padding: 4px;
}

.msg-row {
    display: flex;
    margin-bottom: 12px;

    &.from-user {
        justify-content: flex-end;

        .msg-bubble {
            background: #eff6ff;
            border-color: #bfdbfe;
        }
    }

    &.from-ai {
        justify-content: flex-start;

        .msg-bubble {
            background: #ffffff;
            border-color: #e5e7eb;
        }
    }

    .msg-bubble {
        max-width: 78%;
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        padding: 10px 12px;

        .msg-meta {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 4px;

            .msg-sender {
                font-size: 12px;
                font-weight: 600;
                color: #374151;
            }

            .msg-time {
                font-size: 12px;
                color: #9ca3af;
            }

            .msg-emotion {
                margin-left: auto;
            }
        }

        .msg-content {
            font-size: 13px;
            line-height: 1.6;
            color: #1f2937;
            white-space: pre-wrap;
            word-break: break-word;
        }
    }
}

.mark-tip {
    margin-left: 10px;
    font-size: 12px;
    color: #9ca3af;
}
</style>
