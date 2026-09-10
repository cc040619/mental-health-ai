<template>
    <div class="history-container">
        <!-- 标题 -->
        <div class="page-header">
            <div class="header-tape"></div>
            <h1 class="page-title">📊 我的测评历史</h1>
            <p class="page-subtitle">回顾历次测评，看见自己的成长轨迹 ✨</p>
        </div>

        <!-- 趋势图 -->
        <div class="card trend-card">
            <div class="card-pin"></div>
            <div class="trend-header">
                <h3 class="card-title">📈 同量表趋势</h3>
                <el-select
                    v-model="selectedScaleId"
                    @change="loadTrend"
                    class="scale-select"
                    placeholder="请选择量表"
                    popper-class="scale-select-popper"
                    :duration="250"
                    effect="light"
                >
                    <el-option label="请选择量表" :value="0" disabled />
                    <el-option
                        v-for="s in scaleOptions"
                        :key="s.scaleId"
                        :label="s.scaleName"
                        :value="s.scaleId"
                    />
                </el-select>
            </div>
            <div v-if="trendData.length > 0" ref="trendRef" class="chart-box"></div>
            <div v-else class="trend-empty">
                <div class="empty-emoji">🌱</div>
                <p>选择一个量表查看趋势变化（需有 2 次以上测评）</p>
            </div>
        </div>

        <!-- 历史记录列表 -->
        <h3 class="section-title">🗒️ 测评记录</h3>
        <div v-if="loading" class="loading-tip">載入中... 🌿</div>
        <div v-else-if="records.length === 0" class="empty-tip">
            <div class="empty-emoji">📭</div>
            <p>还没有测评记录，去测评广场开始第一次测评吧～</p>
            <router-link to="/assessment" class="go-link">前往测评广场 →</router-link>
        </div>
        <div v-else class="record-list">
            <div v-for="r in records" :key="r.id" class="record-item">
                <div class="record-emoji">{{ categoryEmoji(r.scaleCategory) }}</div>
                <div class="record-main">
                    <div class="record-name">{{ r.scaleName }}</div>
                    <div class="record-time">{{ formatTime(r.completedAt) }}</div>
                </div>
                <div class="record-score">
                    <span class="score-val">{{ Number(r.totalScore) }} 分</span>
                    <span class="level-tag" :style="{ background: levelColor(r.level) }">{{ levelLabel(r.level) }}</span>
                    <span class="risk-mark" v-if="r.riskFlag === 1">⚠️</span>
                </div>
                <button class="view-btn" @click="viewReport(r.id)">查看报告</button>
            </div>
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="total > pageSize">
            <button class="page-btn" :disabled="page <= 1" @click="changePage(-1)">上一页</button>
            <span class="page-info">{{ page }} / {{ totalPages }}</span>
            <button class="page-btn" :disabled="page >= totalPages" @click="changePage(1)">下一页</button>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getRecordList, getRecordTrend } from '@/api/assessment'

const router = useRouter()

const records = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const totalPages = ref(1)
const loading = ref(false)

const scaleOptions = ref([])
const selectedScaleId = ref('')
const trendData = ref([])
const trendRef = ref(null)
let trendChart = null

const emojiMap = {
    EMOTION: '💙', PERSONALITY: '🌟', STRESS: '🎈', SLEEP: '🌙', INTERPERSONAL: '🤝'
}
const categoryEmoji = (code) => emojiMap[code] || '🧭'

const levelLabels = { NORMAL: '正常', MILD: '轻度', MODERATE: '中度', SEVERE: '重度' }
const levelColors = { NORMAL: '#7BC67E', MILD: '#F2C94C', MODERATE: '#E8927C', SEVERE: '#D9534F' }
const levelLabel = (l) => levelLabels[l] || l
const levelColor = (l) => levelColors[l] || '#E8927C'

const formatTime = (t) => {
    if (!t) return ''
    return String(t).replace('T', ' ').substring(0, 16)
}

const loadRecords = () => {
    loading.value = true
    getRecordList({ page: page.value, size: pageSize }).then(data => {
        records.value = data.records || []
        total.value = data.total || 0
        totalPages.value = Math.ceil(total.value / pageSize)
        // 收集量表选项（去重）
        const map = new Map()
        records.value.forEach(r => {
            if (r.scaleId && !map.has(r.scaleId)) {
                map.set(r.scaleId, { scaleId: r.scaleId, scaleName: r.scaleName })
            }
        })
        scaleOptions.value = Array.from(map.values())
        if (!selectedScaleId.value && scaleOptions.value.length > 0) {
            selectedScaleId.value = scaleOptions.value[0].scaleId
            loadTrend()
        }
    }).finally(() => {
        loading.value = false
    })
}

const loadTrend = async () => {
    if (!selectedScaleId.value) {
        trendData.value = []
        return
    }
    try {
        const data = await getRecordTrend({ scaleId: selectedScaleId.value })
        trendData.value = data || []
        await nextTick()
        initTrendChart()
    } catch (e) {
        trendData.value = []
    }
}

const initTrendChart = () => {
    if (!trendRef.value) return
    if (trendChart) trendChart.dispose()
    if (trendData.value.length === 0) return
    trendChart = echarts.init(trendRef.value)
    const xData = trendData.value.map(d => formatTime(d.completedAt))
    const yData = trendData.value.map(d => Number(d.totalScore))
    const markPoints = trendData.value.map(d => ({
        coord: [formatTime(d.completedAt), Number(d.totalScore)],
        value: levelLabel(d.level)
    }))
    trendChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: '4%', right: '4%', top: 40, bottom: '8%', containLabel: true },
        xAxis: { type: 'category', data: xData, axisLabel: { color: '#8B7D6B' } },
        yAxis: { type: 'value', name: '总分', axisLabel: { color: '#8B7D6B' } },
        series: [{
            type: 'line',
            data: yData,
            smooth: true,
            symbol: 'circle',
            symbolSize: 8,
            lineStyle: { color: '#E8927C', width: 2 },
            itemStyle: { color: '#D9826B' },
            areaStyle: { color: 'rgba(232,146,124,0.15)' },
            label: { show: true, color: '#5C4A3A' },
            markPoint: {
                data: markPoints,
                symbol: 'pin',
                symbolSize: 40,
                itemStyle: { color: '#E8927C' },
                label: { color: '#fff', fontSize: 10 }
            }
        }]
    })
}

const viewReport = (recordId) => {
    router.push(`/assessment/report/${recordId}`)
}

const changePage = (delta) => {
    page.value += delta
    loadRecords()
}

const handleResize = () => {
    trendChart && trendChart.resize()
}

onMounted(() => {
    loadRecords()
    window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize)
    trendChart && trendChart.dispose()
})
</script>

<style lang="scss" scoped>
$bg-page: #FFF8E7;
$bg-card: #FFFEF9;
$primary: #E8927C;
$primary-dark: #D9826B;
$primary-darker: #C4705C;
$primary-light: rgba(232, 146, 124, 0.15);
$text-primary: #5C4A3A;
$text-regular: #6B5B4B;
$text-secondary: #8B7D6B;
$border-color: #E8DCC8;
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$radius-md: 12px;
$radius-lg: 16px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

.history-container {
    min-height: calc(100vh - 130px);
    background: $bg-page;
    font-family: $font-rounded;
    color: $text-primary;
    padding: 24px 16px 48px;
    max-width: 960px;
    margin: 0 auto;
}

.page-header {
    text-align: center;
    margin-bottom: 24px;
    position: relative;
    padding: 12px 0;

    .header-tape {
        width: 120px;
        height: 10px;
        background: linear-gradient(90deg, transparent, rgba(232, 146, 124, 0.5), transparent);
        border-radius: 5px;
        margin: 0 auto 14px;
    }

    .page-title {
        font-family: $font-handwritten;
        font-size: 28px;
        color: $text-primary;
        margin: 0 0 6px;
    }

    .page-subtitle {
        font-size: 13px;
        color: $text-secondary;
        margin: 0;
    }
}

.card {
    position: relative;
    background: $bg-card;
    border-radius: $radius-lg;
    border: 1.5px solid $border-color;
    box-shadow: $shadow-card;
    padding: 22px 20px;
    margin-bottom: 20px;

    .card-pin {
        position: absolute;
        top: -8px;
        left: 50%;
        transform: translateX(-50%);
        width: 14px;
        height: 14px;
        background: $primary;
        border-radius: 50%;
        box-shadow: 0 2px 4px rgba(180, 140, 100, 0.4);
    }
}

.trend-card {
    .trend-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 16px;
        flex-wrap: wrap;
        gap: 10px;
    }

    .card-title {
        font-family: $font-handwritten;
        font-size: 18px;
        color: $text-primary;
        margin: 0;
    }

    .scale-select {
        font-family: $font-rounded;
        width: 200px;

        // 自定义 el-select 的输入框，匹配手绘治愈风
        :deep(.el-select__wrapper) {
            background: $bg-page !important;
            border: 1.5px dashed $border-color !important;
            border-radius: $radius-md !important;
            box-shadow: none !important;
            padding: 4px 12px !important;
            font-size: 13px;
            min-height: 32px;
            transition: all 0.25s ease;

            &:hover {
                border-color: $primary !important;
                transform: translateY(-1px);
                box-shadow: 0 2px 6px rgba(232, 146, 124, 0.18) !important;
            }

            &.is-focus {
                border: 1.5px solid $primary !important;
                box-shadow: 0 0 0 3px rgba(232, 146, 124, 0.15) !important;
                transform: translateY(-1px);
            }
        }

        :deep(.el-select__placeholder) {
            color: $text-secondary !important;
            font-size: 13px;
        }

        :deep(.el-select__selected-item) {
            font-size: 13px;
            color: $text-regular;
        }

        :deep(.el-select__caret) {
            color: $primary !important;
            transition: transform 0.25s ease;
        }
    }

    .chart-box {
        width: 100%;
        height: 280px;
    }

    .trend-empty {
        text-align: center;
        padding: 30px 0;
        color: $text-secondary;
        font-size: 14px;

        .empty-emoji {
            font-size: 40px;
            margin-bottom: 10px;
        }
    }
}

.section-title {
    font-family: $font-handwritten;
    font-size: 20px;
    color: $text-primary;
    margin: 24px 0 14px;
}

.loading-tip {
    text-align: center;
    padding: 40px 0;
    color: $text-secondary;
}

.empty-tip {
    text-align: center;
    padding: 40px 0;
    color: $text-secondary;

    .empty-emoji {
        font-size: 48px;
        margin-bottom: 12px;
    }

    p {
        margin: 0 0 16px;
        font-size: 14px;
    }

    .go-link {
        display: inline-block;
        color: $primary;
        background: $primary-light;
        border: 1.5px dashed $primary;
        border-radius: $radius-md;
        padding: 8px 20px;
        text-decoration: none;
        font-size: 14px;

        &:hover {
            background: $primary;
            color: #fff;
            border-style: solid;
        }
    }
}

.record-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.record-item {
    display: flex;
    align-items: center;
    gap: 14px;
    background: $bg-card;
    border: 1.5px solid $border-color;
    border-radius: $radius-lg;
    box-shadow: $shadow-card;
    padding: 14px 18px;
    transition: transform 0.2s ease;

    &:hover {
        transform: translateY(-2px);
    }

    .record-emoji {
        font-size: 28px;
    }

    .record-main {
        flex: 1;

        .record-name {
            font-size: 15px;
            font-weight: 600;
            color: $text-primary;
            margin-bottom: 4px;
        }

        .record-time {
            font-size: 12px;
            color: $text-secondary;
        }
    }

    .record-score {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
        justify-content: flex-end;

        .score-val {
            font-size: 14px;
            font-weight: 600;
            color: $primary-dark;
        }

        .level-tag {
            color: #fff;
            font-size: 12px;
            padding: 3px 10px;
            border-radius: 12px;
        }

        .risk-mark {
            font-size: 16px;
        }
    }

    .view-btn {
        font-family: $font-rounded;
        background: $bg-page;
        border: 1.5px solid $primary;
        color: $primary;
        border-radius: $radius-md;
        padding: 6px 14px;
        font-size: 13px;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
            background: $primary;
            color: #fff;
        }
    }
}

.pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 14px;
    margin-top: 24px;

    .page-btn {
        font-family: $font-rounded;
        background: $bg-card;
        border: 1.5px dashed $border-color;
        color: $text-regular;
        border-radius: $radius-md;
        padding: 6px 16px;
        font-size: 13px;
        cursor: pointer;

        &:not(:disabled):hover {
            border-color: $primary;
            color: $primary;
        }

        &:disabled {
            opacity: 0.4;
            cursor: not-allowed;
        }
    }

    .page-info {
        font-size: 14px;
        color: $text-secondary;
    }
}
</style>

<!-- 全局样式：el-select 弹出层（teleport 到 body，必须无 scoped） -->
<style lang="scss">
// ========== 量表下拉弹出层圆角 + 过渡动画 ==========
.scale-select-popper {
    border-radius: 10px !important;
    border: 1.5px solid #E8DCC8 !important;
    background: #FFFEF9 !important;
    box-shadow: 0 6px 20px rgba(180, 140, 100, 0.18) !important;
    padding: 6px !important;
    min-width: 180px !important;
    overflow: hidden;

    // 弹出过渡动画（Element Plus popper 自动应用 fadeInDown/fadeOutUp）
    &.el-popper.is-light {
        border: 1.5px solid #E8DCC8 !important;
    }

    .el-select-dropdown__item {
        font-family: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;
        font-size: 13px;
        color: #6B5B4B;
        border-radius: 6px;
        padding: 8px 12px;
        margin: 2px 0;
        transition: all 0.2s ease;

        &.is-hovering {
            background: rgba(232, 146, 124, 0.12) !important;
            color: #D9826B !important;
        }

        &.is-selected {
            background: rgba(232, 146, 124, 0.18) !important;
            color: #E8927C !important;
            font-weight: 600;
        }
    }

    // 隐藏底部滚动条装饰线
    .el-scrollbar__bar.is-vertical {
        width: 4px;
    }

    .el-scrollbar__thumb {
        background: #E8DCC8;
        border-radius: 2px;
    }
}

// Element Plus 原生 popper 过渡动画时长覆盖（更顺滑的弹出/收起）
.el-popper.fade-in-down,
.el-popper.fade-in-up,
.el-popper.fade-out-down,
.el-popper.fade-out-up {
    transition-duration: 250ms !important;
    transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1) !important;
}
</style>
