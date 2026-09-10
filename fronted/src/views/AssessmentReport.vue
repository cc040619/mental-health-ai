<template>
    <div class="report-container">
        <!-- 加载 -->
        <div v-if="loading" class="loading-tip">报告生成中... 🌿</div>

        <template v-else-if="report">
            <!-- 风险提示置顶 -->
            <div v-if="report.riskFlag === 1" class="risk-alert">
                <div class="risk-icon">⚠️</div>
                <div class="risk-content">
                    <div class="risk-title">您的结果值得关注</div>
                    <p class="risk-text">{{ report.riskMessage }}</p>
                </div>
            </div>

            <!-- 标题 -->
            <div class="page-header">
                <div class="header-tape"></div>
                <h1 class="page-title">📊 {{ report.scaleName }} 测评报告</h1>
                <p class="page-subtitle">完成时间：{{ formatTime(report.completedAt) }}</p>
            </div>

            <div class="report-grid">
                <!-- 仪表盘 -->
                <div class="card chart-card">
                    <div class="card-pin"></div>
                    <h3 class="card-title">🎯 总分与等级</h3>
                    <div ref="gaugeRef" class="chart-box"></div>
                    <div class="level-tag" :style="{ background: report.levelColor }">
                        {{ report.levelLabel }}
                    </div>
                </div>

                <!-- 雷达图 -->
                <div class="card chart-card" v-if="hasDimensions">
                    <div class="card-pin"></div>
                    <h3 class="card-title">🕸️ 维度分析</h3>
                    <div ref="radarRef" class="chart-box"></div>
                </div>
            </div>

            <!-- 等级解读 -->
            <div class="card interpret-card">
                <div class="card-pin"></div>
                <h3 class="card-title">📝 结果解读</h3>
                <pre class="interpret-text">{{ report.interpretation }}</pre>
            </div>

            <!-- 建议 -->
            <div class="card suggest-card">
                <div class="card-pin"></div>
                <h3 class="card-title">💡 心理调适建议</h3>
                <ul class="suggest-list">
                    <li v-for="(s, i) in report.suggestions" :key="i">{{ s }}</li>
                </ul>
            </div>

            <!-- 免责声明 -->
            <div class="disclaimer">
                {{ report.disclaimer }}
            </div>

            <!-- 操作按钮 -->
            <div class="action-btns">
                <router-link to="/assessment" class="action-link">返回测评广场</router-link>
                <router-link to="/assessment/history" class="action-link">查看测评历史</router-link>
            </div>
        </template>
    </div>
</template>

<script setup>
import { ref, watch, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { getRecordDetail } from '@/api/assessment'

const route = useRoute()
const recordId = route.params.recordId

const loading = ref(true)
const report = ref(null)
const gaugeRef = ref(null)
const radarRef = ref(null)
let gaugeChart = null
let radarChart = null

const hasDimensions = ref(false)

const formatTime = (t) => {
    if (!t) return ''
    return String(t).replace('T', ' ').substring(0, 16)
}

// 各量表的总分上限，用于仪表盘动态 max
const SCALE_MAX_SCORE = {
    'PHQ-9': 27,
    'GAD-7': 21,
    'PSS-10': 40,
    'PSQI': 21
}
const getMaxScore = (scaleCode) => SCALE_MAX_SCORE[scaleCode] || 100

const loadReport = async () => {
    loading.value = true
    try {
        const data = await getRecordDetail(recordId)
        report.value = data
        hasDimensions.value = data.dimensionScores && Object.keys(data.dimensionScores).length > 0
    } finally {
        loading.value = false
    }
}

/**
 * watch report 变化 + setTimeout 确保容器有尺寸后再初始化图表
 * ECharts 要求容器有明确的宽高，nextTick 只保证 Vue DOM 更新，
 * 浏览器布局可能还没完成，加 50ms 延迟等待布局
 */
watch(report, async (val) => {
    if (!val) return
    await nextTick()
    setTimeout(() => {
        initGauge()
        if (hasDimensions.value) {
            initRadar()
        }
    }, 50)
}, { flush: 'post' })

const initGauge = () => {
    if (!gaugeRef.value) {
        console.warn('[Report] gaugeRef 未就绪，跳过仪表盘初始化')
        return
    }
    const rect = gaugeRef.value.getBoundingClientRect()
    if (rect.width === 0 || rect.height === 0) {
        console.warn('[Report] 仪表盘容器尺寸为 0，跳过初始化')
        return
    }
    if (gaugeChart) gaugeChart.dispose()
    gaugeChart = echarts.init(gaugeRef.value)

    const score = Number(report.value.totalScore) || 0
    const code = report.value.scaleCode || ''
    const maxScore = getMaxScore(code)
    const color = report.value.levelColor || '#E8927C'

    gaugeChart.setOption({
        series: [{
            type: 'gauge',
            min: 0,
            max: maxScore,
            radius: '90%',
            startAngle: 210,
            endAngle: -30,
            progress: { show: true, width: 18, roundCap: true, itemStyle: { color: color } },
            axisLine: { lineStyle: { width: 18, color: [[1, '#F0E6D2']] } },
            pointer: { show: false },
            axisTick: { show: false },
            splitLine: { show: false },
            axisLabel: { show: false },
            anchor: { show: false },
            title: { show: false },
            detail: {
                valueAnimation: true,
                formatter: `${score} / ${maxScore}`,
                fontSize: 22,
                fontWeight: 'bold',
                color: color,
                offsetCenter: [0, '30%']
            },
            data: [{ value: score }]
        }]
    })
}

const initRadar = () => {
    if (!radarRef.value) {
        console.warn('[Report] radarRef 未就绪，跳过雷达图初始化')
        return
    }
    const rect = radarRef.value.getBoundingClientRect()
    if (rect.width === 0 || rect.height === 0) {
        console.warn('[Report] 雷达图容器尺寸为 0，跳过初始化')
        return
    }
    if (radarChart) radarChart.dispose()
    radarChart = echarts.init(radarRef.value)

    const dims = report.value.dimensionScores
    const keys = Object.keys(dims)
    const values = keys.map(k => Number(dims[k]))
    // 动态 max：取维度最大值的适当上限
    const maxRaw = Math.max(...values, 1)
    const max = maxRaw <= 3 ? 3 : maxRaw <= 10 ? 10 : Math.ceil(maxRaw / 5) * 5

    const indicators = keys.map(k => ({ name: k, max: max }))

    radarChart.setOption({
        tooltip: { trigger: 'item' },
        radar: {
            indicator: indicators,
            radius: '60%',
            center: ['50%', '52%'],
            axisName: { color: '#6B5B4B', fontSize: 11 },
            splitNumber: 4,
            splitArea: {
                show: true,
                areaStyle: { color: ['rgba(232,146,124,0.03)', 'rgba(232,146,124,0.07)', 'rgba(232,146,124,0.11)', 'rgba(232,146,124,0.07)'] }
            },
            splitLine: { lineStyle: { color: '#E8DCC8' } },
            axisLine: { lineStyle: { color: '#E8DCC8' } }
        },
        series: [{
            type: 'radar',
            data: [{
                value: values,
                name: '维度得分',
                areaStyle: { color: 'rgba(232,146,124,0.4)' },
                lineStyle: { color: '#E8927C', width: 2 },
                itemStyle: { color: '#D9826B' }
            }]
        }]
    })
}

const handleResize = () => {
    gaugeChart && gaugeChart.resize()
    radarChart && radarChart.resize()
}

onMounted(() => {
    loadReport()
    window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize)
    gaugeChart && gaugeChart.dispose()
    radarChart && radarChart.dispose()
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

.report-container {
    min-height: calc(100vh - 130px);
    background: $bg-page;
    font-family: $font-rounded;
    color: $text-primary;
    padding: 20px 16px 48px;
    max-width: 960px;
    margin: 0 auto;
}

.loading-tip {
    text-align: center;
    padding: 80px 0;
    color: $text-secondary;
    font-size: 16px;
}

.risk-alert {
    display: flex;
    gap: 14px;
    background: #FFF0EE;
    border: 2px solid #D9534F;
    border-radius: $radius-lg;
    padding: 16px 18px;
    margin-bottom: 22px;
    box-shadow: 0 4px 16px rgba(217, 83, 79, 0.2);

    .risk-icon {
        font-size: 28px;
        flex-shrink: 0;
    }

    .risk-title {
        font-size: 16px;
        font-weight: 700;
        color: #C9302C;
        margin-bottom: 4px;
    }

    .risk-text {
        font-size: 13px;
        color: #A02825;
        line-height: 1.6;
        margin: 0;
    }
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
        font-size: 26px;
        color: $text-primary;
        margin: 0 0 6px;
    }

    .page-subtitle {
        font-size: 13px;
        color: $text-secondary;
        margin: 0;
    }
}

.report-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 20px;
    margin-bottom: 20px;
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

    .card-title {
        font-family: $font-handwritten;
        font-size: 18px;
        color: $text-primary;
        text-align: center;
        margin: 0 0 16px;
    }
}

.chart-card {
    text-align: center;

    .chart-box {
        width: 100%;
        height: 240px;
    }

    .level-tag {
        display: inline-block;
        color: #fff;
        font-size: 15px;
        font-weight: 600;
        padding: 6px 22px;
        border-radius: 20px;
        margin-top: 4px;
    }
}

.interpret-card {
    .interpret-text {
        font-family: $font-rounded;
        font-size: 14px;
        color: $text-regular;
        line-height: 1.8;
        white-space: pre-wrap;
        margin: 0;
    }
}

.suggest-card {
    .suggest-list {
        margin: 0;
        padding-left: 20px;

        li {
            font-size: 14px;
            color: $text-regular;
            line-height: 2;
        }
    }
}

.disclaimer {
    background: $bg-card;
    border: 1.5px dashed $border-color;
    border-radius: $radius-md;
    padding: 14px 16px;
    font-size: 12px;
    color: $text-secondary;
    line-height: 1.7;
    text-align: center;
    margin-bottom: 24px;
}

.action-btns {
    display: flex;
    justify-content: center;
    gap: 14px;

    .action-link {
        font-family: $font-rounded;
        color: $primary;
        background: $primary-light;
        border: 1.5px dashed $primary;
        border-radius: $radius-md;
        padding: 10px 22px;
        text-decoration: none;
        font-size: 14px;
        transition: all 0.25s ease;

        &:hover {
            background: $primary;
            color: #fff;
            border-style: solid;
        }
    }
}
</style>
