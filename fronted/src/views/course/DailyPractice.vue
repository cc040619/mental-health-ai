<template>
    <div class="daily-container">
        <!-- 页头 -->
        <div class="page-header">
            <div class="header-tape"></div>
            <h1 class="page-title">🌸 每日一练</h1>
            <p class="page-subtitle">每天三分钟，让心安顿下来 ✨</p>
        </div>

        <div v-if="loadingDaily" class="loading-tip">载入中... 🌿</div>
        <template v-else-if="daily">
            <!-- 今日推荐卡片 -->
            <div class="recommend-card">
                <div class="card-pin"></div>
                <div class="rec-main">
                    <div class="rec-cover">
                        <img v-if="daily.courseCover" :src="daily.courseCover" alt="封面" class="cover-img" />
                        <span v-else class="cover-emoji">🧘</span>
                    </div>
                    <div class="rec-info">
                        <div class="rec-tag">{{ daily.categoryLabel }}</div>
                        <h2 class="rec-title">{{ daily.chapterTitle }}</h2>
                        <div class="rec-meta">
                            <span>📖 {{ daily.courseTitle }}</span>
                            <span class="meta-dot">·</span>
                            <span>⏱️ {{ daily.durationText }}</span>
                        </div>
                        <p v-if="daily.intro" class="rec-intro">{{ daily.intro }}</p>
                        <p v-else class="rec-intro">跟着音频慢慢呼吸，让思绪柔软地落下。</p>
                    </div>
                </div>
                <div class="rec-actions">
                    <button class="primary-btn" @click="playToday">
                        <span v-if="isCurrentPlaying">⏸ 正在播放</span>
                        <span v-else>▶ 开始练习</span>
                    </button>
                    <button class="checkin-btn"
                            :class="{ done: daily.hasCheckedInToday }"
                            :disabled="daily.hasCheckedInToday || checkingIn"
                            @click="doCheckin">
                        <span v-if="daily.hasCheckedInToday">✓ 今日已打卡</span>
                        <span v-else-if="checkingIn">打卡中...</span>
                        <span v-else>📝 完成打卡</span>
                    </button>
                </div>
                <div class="rec-tip">提示：当日播放满 60 秒后即可打卡</div>
            </div>

            <!-- 连续打卡徽标 -->
            <div class="streak-bar">
                <div class="streak-badge">
                    <span class="streak-emoji">🔥</span>
                    <span class="streak-num">{{ streak }}</span>
                    <span class="streak-text">天连续打卡</span>
                </div>
                <div class="month-picker">
                    <el-date-picker v-model="monthValue" type="month" format="YYYY 年 MM 月"
                                    value-format="YYYY-MM" :clearable="false"
                                    @change="loadCalendar" placeholder="选择月份" />
                </div>
            </div>

            <!-- 打卡日历 -->
            <div class="calendar-card">
                <div class="cal-header">
                    <span class="cal-month">{{ monthLabel }}</span>
                    <span class="cal-count">本月已打卡 {{ checkinDays.length }} 天 🌱</span>
                </div>
                <div class="cal-weekdays">
                    <span v-for="w in weekdays" :key="w">{{ w }}</span>
                </div>
                <div class="cal-grid">
                    <div v-for="(cell, idx) in calendarCells" :key="idx"
                         class="cal-cell" :class="{ empty: !cell.day, checked: cell.checked, today: cell.isToday }">
                        <template v-if="cell.day">
                            <span class="day-num">{{ cell.day }}</span>
                            <span v-if="cell.checked" class="day-mark">✓</span>
                            <span v-else-if="cell.isToday" class="day-today-dot">·</span>
                        </template>
                    </div>
                </div>
                <div v-if="checkinDays.length === 0" class="cal-empty">本月还没有打卡记录，今天就开始吧 🌸</div>
            </div>
        </template>
        <div v-else class="empty-tip">
            <div class="empty-emoji">🌼</div>
            <p>暂无每日练习内容，敬请期待～</p>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { usePlayerStore } from '@/stores/player'
import { getDailyPractice, checkinDaily, getCheckinCalendar } from '@/api/course'

const player = usePlayerStore()

const loadingDaily = ref(false)
const checkingIn = ref(false)
const daily = ref(null)

// 日历相关
const monthValue = ref(formatMonth(new Date()))
const checkinRecords = ref([]) // [{checkinDate, courseTitle, chapterTitle}]
const streak = ref(0)

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

function formatMonth(d) {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    return `${y}-${m}`
}

const monthLabel = computed(() => {
    if (!monthValue.value) return ''
    const [y, m] = monthValue.value.split('-')
    return `${y} 年 ${parseInt(m)} 月`
})

// 当前日期（用于"今日"标记）
const todayStr = computed(() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

// 已打卡日期集合（YYYY-MM-DD）
const checkinDaySet = computed(() => new Set(checkinRecords.value.map(r => r.checkinDate)))

// 已打卡日期数量
const checkinDays = computed(() => checkinRecords.value)

// 构造日历网格：本月第一天是星期几 + 本月天数
const calendarCells = computed(() => {
    if (!monthValue.value) return []
    const [y, m] = monthValue.value.split('-').map(Number)
    const first = new Date(y, m - 1, 1)
    const firstWeekday = first.getDay()
    const daysInMonth = new Date(y, m, 0).getDate()
    const cells = []
    // 前置空格
    for (let i = 0; i < firstWeekday; i++) cells.push({ day: null })
    for (let d = 1; d <= daysInMonth; d++) {
        const ds = `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
        cells.push({
            day: d,
            dateStr: ds,
            checked: checkinDaySet.value.has(ds),
            isToday: ds === todayStr.value
        })
    }
    return cells
})

// 当前正在播放今日推荐章节？
const isCurrentPlaying = computed(() => {
    if (!daily.value || !player.visible) return false
    return player.currentChapter?.id === daily.value.chapterId && player.isPlaying
})

const loadDaily = () => {
    loadingDaily.value = true
    getDailyPractice().then(data => {
        daily.value = data
    }).finally(() => {
        loadingDaily.value = false
    })
}

const loadCalendar = () => {
    getCheckinCalendar({ month: monthValue.value }).then(data => {
        checkinRecords.value = data.records || []
        streak.value = data.streak || 0
    })
}

const playToday = () => {
    if (!daily.value) return
    // 若当前已在播今日章节，则切换暂停/播放
    if (player.visible && player.currentChapter?.id === daily.value.chapterId) {
        player.togglePlay()
        return
    }
    const chapters = [{
        id: daily.value.chapterId,
        title: daily.value.chapterTitle,
        audioUrl: daily.value.audioUrl,
        duration: daily.value.duration || 0,
        progressSeconds: 0,
        isFinished: false
    }]
    player.play({
        course: {
            title: daily.value.courseTitle,
            coverUrl: daily.value.courseCover,
            category: daily.value.category
        },
        chapters,
        startIndex: 0
    })
}

const doCheckin = () => {
    if (!daily.value || daily.value.hasCheckedInToday) return
    checkingIn.value = true
    checkinDaily({
        courseId: daily.value.courseId,
        chapterId: daily.value.chapterId,
        duration: daily.value.duration || 0
    }).then(() => {
        ElMessage.success('打卡成功，继续保持 🌸')
        daily.value.hasCheckedInToday = true
        // 同步刷新日历与 streak
        loadCalendar()
    }).finally(() => {
        checkingIn.value = false
    })
}

onMounted(() => {
    loadDaily()
    loadCalendar()
})
</script>

<style lang="scss" scoped>
$bg-page: #FFF8E7;
$bg-card: #FFFEF9;
$primary: #E8927C;
$primary-dark: #D9826B;
$primary-darker: #C4705C;
$primary-light: rgba(232, 146, 124, 0.15);
$accent-yellow: #F7E8A6;
$text-primary: #5C4A3A;
$text-regular: #6B5B4B;
$text-secondary: #8B7D6B;
$border-color: #E8DCC8;
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$shadow-button: 0 3px 0 $primary-darker, 0 4px 8px rgba(180, 140, 100, 0.2);
$radius-md: 12px;
$radius-lg: 16px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

.daily-container {
    min-height: calc(100vh - 130px);
    background: $bg-page; font-family: $font-rounded; color: $text-primary;
    padding: 24px 16px 80px; max-width: 960px; margin: 0 auto;
}

.page-header {
    text-align: center; margin-bottom: 28px;
    .header-tape {
        width: 120px; height: 10px;
        background: linear-gradient(90deg, transparent, rgba(232,146,124,0.5), transparent);
        border-radius: 5px; margin: 0 auto 14px;
    }
    .page-title { font-family: $font-handwritten; font-size: 30px; font-weight: 600; color: $text-primary; margin: 0 0 8px; }
    .page-subtitle { font-size: 14px; color: $text-secondary; margin: 0; }
}

.loading-tip, .empty-tip {
    text-align: center; padding: 60px 0; color: $text-secondary; font-size: 15px;
    .empty-emoji { font-size: 48px; margin-bottom: 12px; }
}

/* ===== 今日推荐卡片 ===== */
.recommend-card {
    position: relative; background: $bg-card; border-radius: $radius-lg;
    border: 1.5px solid $border-color; box-shadow: $shadow-card; overflow: hidden;
    padding: 22px 24px 20px; margin-bottom: 28px;
    .card-pin {
        position: absolute; top: -8px; left: 50%; transform: translateX(-50%);
        width: 14px; height: 14px; background: $primary; border-radius: 50%;
        box-shadow: 0 2px 4px rgba(180,140,100,0.4); z-index: 2;
    }
    .rec-main { display: flex; gap: 18px; margin-bottom: 18px;
        .rec-cover {
            flex-shrink: 0; width: 120px; height: 120px; border-radius: $radius-md;
            background: linear-gradient(135deg, rgba(232,146,124,0.18), rgba(247,232,166,0.25));
            display: flex; align-items: center; justify-content: center; overflow: hidden;
            .cover-img { width: 100%; height: 100%; object-fit: cover; }
            .cover-emoji { font-size: 56px; }
        }
        .rec-info {
            flex: 1; min-width: 0;
            .rec-tag {
                display: inline-block; font-size: 12px; color: #fff;
                background: linear-gradient(135deg, $primary, $primary-dark);
                padding: 2px 12px; border-radius: 10px; margin-bottom: 8px;
            }
            .rec-title { font-family: $font-handwritten; font-size: 22px; color: $text-primary; margin: 0 0 8px; line-height: 1.4; }
            .rec-meta { font-size: 13px; color: $text-secondary; margin-bottom: 10px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
                .meta-dot { color: $border-color; }
            }
            .rec-intro { font-size: 14px; color: $text-regular; line-height: 1.65; margin: 0;
                display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
            }
        }
    }
    .rec-actions {
        display: flex; gap: 14px; flex-wrap: wrap; margin-bottom: 12px;
        .primary-btn, .checkin-btn {
            font-family: $font-rounded; font-size: 15px; font-weight: 600;
            border-radius: $radius-md; cursor: pointer; transition: all 0.25s ease;
            padding: 10px 22px; border: none;
        }
        .primary-btn {
            background: linear-gradient(135deg, $primary, $primary-dark); color: #fff; box-shadow: $shadow-button;
            &:hover { transform: translateY(-1px); box-shadow: 0 4px 0 $primary-darker, 0 5px 12px rgba(180,140,100,0.25); }
            &:active { transform: translateY(1px); }
        }
        .checkin-btn {
            background: $bg-page; color: $primary-dark; border: 1.5px dashed $primary;
            &:hover:not(:disabled) { background: $primary-light; border-style: solid; }
            &.done { background: rgba(232,146,124,0.18); color: $primary-dark; border-style: solid; cursor: default; }
            &:disabled { opacity: 0.7; cursor: not-allowed; }
        }
    }
    .rec-tip { font-size: 12px; color: $text-secondary; text-align: center; }
}

/* ===== 连续打卡徽标 + 月份选择 ===== */
.streak-bar {
    display: flex; align-items: center; justify-content: space-between;
    flex-wrap: wrap; gap: 14px; margin-bottom: 22px;
    .streak-badge {
        display: inline-flex; align-items: center; gap: 8px;
        background: linear-gradient(135deg, #FFE4B5, $accent-yellow);
        border-radius: 30px; padding: 8px 18px;
        border: 1.5px dashed rgba(232,146,124,0.5);
        box-shadow: 0 2px 8px rgba(180,140,100,0.15);
        .streak-emoji { font-size: 20px; }
        .streak-num { font-family: $font-handwritten; font-size: 24px; color: $primary-darker; font-weight: 600; }
        .streak-text { font-size: 13px; color: $text-regular; }
    }
    .month-picker {
        :deep(.el-input__wrapper) {
            background: $bg-card; box-shadow: 0 0 0 1.5px $border-color inset;
            border-radius: $radius-md;
        }
        :deep(.el-input__inner) { font-family: $font-rounded; color: $text-regular; }
    }
}

/* ===== 打卡日历卡片 ===== */
.calendar-card {
    background: $bg-card; border-radius: $radius-lg;
    border: 1.5px solid $border-color; box-shadow: $shadow-card;
    padding: 20px 22px 22px;
    .cal-header {
        display: flex; align-items: baseline; justify-content: space-between;
        margin-bottom: 14px; flex-wrap: wrap; gap: 6px;
        .cal-month { font-family: $font-handwritten; font-size: 22px; color: $text-primary; }
        .cal-count { font-size: 13px; color: $text-secondary; }
    }
    .cal-weekdays {
        display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; margin-bottom: 8px;
        span { text-align: center; font-size: 12px; color: $text-secondary; padding: 4px 0; }
    }
    .cal-grid {
        display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px;
        .cal-cell {
            position: relative; aspect-ratio: 1 / 1; min-height: 48px;
            background: $bg-page; border: 1.5px dashed $border-color; border-radius: $radius-md;
            display: flex; flex-direction: column; align-items: center; justify-content: center;
            transition: all 0.25s ease;
            &.empty { background: transparent; border: none; }
            &.today { border-color: $primary; background: rgba(232,146,124,0.08); }
            &.checked {
                background: linear-gradient(135deg, rgba(232,146,124,0.18), rgba(247,232,166,0.28));
                border-color: $primary; border-style: solid;
                .day-mark { font-size: 18px; color: $primary-darker; font-weight: 700; }
            }
            .day-num { font-family: $font-handwritten; font-size: 15px; color: $text-regular; }
            .day-today-dot { font-size: 18px; color: $primary; line-height: 0; margin-top: -2px; }
        }
    }
    .cal-empty { text-align: center; color: $text-secondary; font-size: 13px; margin-top: 14px; }
}

@media (max-width: 600px) {
    .recommend-card .rec-main { flex-direction: column;
        .rec-cover { width: 100%; height: 160px; }
    }
    .cal-grid .cal-cell { min-height: 38px; }
}
</style>
