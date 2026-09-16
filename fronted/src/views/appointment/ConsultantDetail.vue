<template>
    <div class="detail-container" v-loading="loading">
        <!-- 返回 -->
        <div class="back-row">
            <span class="back-link" @click="router.back()">← 返回列表</span>
        </div>

        <template v-if="consultant">
            <!-- 简介便签卡 -->
            <div class="intro-card">
                <div class="card-tape"></div>
                <div class="intro-head">
                    <el-avatar :size="80" :src="consultant.avatar" class="avatar">
                        {{ (consultant.name || '?').charAt(0) }}
                    </el-avatar>
                    <div class="head-info">
                        <h1 class="c-name">{{ consultant.name }}</h1>
                        <div class="c-sub">
                            <span class="c-title">{{ consultant.title || '心理咨询师' }}</span>
                            <span class="meta-item">⭐ {{ Number(consultant.rating || 0).toFixed(1) }}</span>
                            <span class="meta-item">🌿 从业 {{ consultant.yearsOfPractice || 0 }} 年</span>
                            <span class="meta-item">💬 {{ consultant.consultCount || 0 }} 次咨询</span>
                        </div>
                        <div class="specialty-tags">
                            <span class="s-tag" v-for="s in (consultant.specialties || [])" :key="s">🏷️ {{ s }}</span>
                        </div>
                    </div>
                </div>
                <div class="intro-body">
                    <div class="intro-label">📝 个人简介</div>
                    <p class="intro-text">{{ consultant.intro || '这位咨询师还没有填写简介。' }}</p>
                </div>
            </div>

            <!-- 预约排班 -->
            <div class="booking-card">
                <h2 class="section-title">🗓️ 选择预约时间</h2>

                <!-- 未来 N 天日期选择（N = 咨询师预约设置 bookingWindowDays，默认 14） -->
                <div class="date-strip">
                    <button v-for="d in dateList" :key="d.value"
                            class="date-btn" :class="{ active: selectedDate === d.value }"
                            @click="selectDate(d.value)">
                        <span class="date-week">{{ d.week }}</span>
                        <span class="date-day">{{ d.label }}</span>
                    </button>
                </div>

                <!-- 时段网格 -->
                <div v-if="slotLoading" class="slot-tip">时段载入中... 🌿</div>
                <div v-else-if="slots.length === 0" class="slot-tip">
                    <div>🌙 当日暂无可约时段，换个日期看看吧</div>
                </div>
                <div v-else class="slot-grid">
                    <button v-for="s in slots" :key="s.id" class="slot-btn" @click="openBooking(s)">
                        <span class="slot-time">{{ s.startTime }} - {{ s.endTime }}</span>
                        <span class="slot-state">可预约 ✨</span>
                    </button>
                </div>
            </div>

            <!-- 来访者评价 -->
            <div class="review-card">
                <h2 class="section-title">💌 来访者评价（{{ reviewTotal }}）</h2>
                <div v-if="reviews.length === 0" class="slot-tip">还没有评价，咨询结束后欢迎分享你的感受 🌱</div>
                <div v-else class="review-list">
                    <div v-for="r in reviews" :key="r.id" class="review-item">
                        <div class="review-head">
                            <span class="reviewer">来访者</span>
                            <span class="review-stars">{{ '⭐'.repeat(r.rating) }}</span>
                            <span class="review-time">{{ r.createdAt }}</span>
                        </div>
                        <p v-if="r.content" class="review-content">{{ r.content }}</p>
                        <div v-if="r.replyContent" class="review-reply">
                            <span class="reply-label">咨询师回复：</span>{{ r.replyContent }}
                        </div>
                    </div>
                </div>
                <div v-if="reviewTotal > reviewSize" class="pager">
                    <el-pagination background layout="prev, pager, next" small
                        :current-page="reviewPage" :page-size="reviewSize" :total="reviewTotal"
                        @current-change="onReviewPageChange" />
                </div>
            </div>
        </template>

        <!-- 预约确认弹窗 -->
        <el-dialog v-model="bookingVisible" title="预约确认" width="480px"
                   :close-on-click-modal="false" class="booking-dialog">
            <div v-if="currentSlot" class="booking-summary">
                <p>👩‍⚕️ 咨询师：<b>{{ consultant?.name }}</b></p>
                <p>🗓️ 时间：<b>{{ selectedDate }} {{ currentSlot.startTime }} - {{ currentSlot.endTime }}</b></p>
            </div>
            <el-input v-model="problemDesc" type="textarea" :rows="4" maxlength="500" show-word-limit
                      placeholder="可以简单描述你想聊的困扰（选填，500 字以内），帮助咨询师提前了解" />
            <template #footer>
                <el-button round @click="bookingVisible = false">再想想</el-button>
                <el-button type="primary" round :loading="submitting" @click="confirmBooking">确认预约</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElNotification } from 'element-plus'
import { getConsultantDetail, getConsultantSlots, getConsultantReviews } from '@/api/consultant'
import { createAppointment } from '@/api/appointment'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const consultant = ref(null)

const dateList = ref([])
const selectedDate = ref('')
const slots = ref([])
const slotLoading = ref(false)

const reviews = ref([])
const reviewTotal = ref(0)
const reviewPage = ref(1)
const reviewSize = ref(5)

const bookingVisible = ref(false)
const currentSlot = ref(null)
const problemDesc = ref('')
const submitting = ref(false)

const WEEK = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const buildDateList = (days = 14) => {
    // 后端硬约束 1-90 天，前端只做展示层兜底
    const total = Math.min(Math.max(Number(days) || 14, 1), 90)
    const list = []
    const today = new Date()
    for (let i = 0; i < total; i++) {
        const d = new Date(today)
        d.setDate(today.getDate() + i)
        const y = d.getFullYear()
        const m = String(d.getMonth() + 1).padStart(2, '0')
        const day = String(d.getDate()).padStart(2, '0')
        list.push({
            value: `${y}-${m}-${day}`,
            week: i === 0 ? '今天' : WEEK[d.getDay()],
            label: `${d.getMonth() + 1}/${d.getDate()}`
        })
    }
    dateList.value = list
    selectedDate.value = list[0].value
}

const loadDetail = () => {
    loading.value = true
    getConsultantDetail(route.params.id).then(data => {
        consultant.value = data
        // 按咨询师设置的可预约天数重建日期条（今天始终在列表内，无需重载已选日时段）
        buildDateList(data.bookingWindowDays || 14)
    }).finally(() => {
        loading.value = false
    })
}

const loadSlots = () => {
    slotLoading.value = true
    getConsultantSlots(route.params.id, selectedDate.value).then(data => {
        slots.value = data || []
    }).finally(() => {
        slotLoading.value = false
    })
}

const loadReviews = (page = 1) => {
    reviewPage.value = page
    getConsultantReviews(route.params.id, { page, size: reviewSize.value }).then(data => {
        reviews.value = data.records || []
        reviewTotal.value = data.total || 0
    })
}

const selectDate = (date) => {
    selectedDate.value = date
    loadSlots()
}

const openBooking = (slot) => {
    currentSlot.value = slot
    problemDesc.value = ''
    bookingVisible.value = true
}

const confirmBooking = () => {
    submitting.value = true
    createAppointment({
        consultantId: Number(route.params.id),
        slotId: currentSlot.value.id,
        problemDesc: problemDesc.value || null
    }).then(() => {
        bookingVisible.value = false
        ElNotification({
            title: '预约成功 🌸',
            message: '预约已提交，等待咨询师确认。点击查看我的预约',
            type: 'success',
            duration: 4000,
            onClick: () => router.push('/appointments')
        })
        loadSlots()
    }).finally(() => {
        submitting.value = false
    })
}

const onReviewPageChange = (p) => loadReviews(p)

onMounted(() => {
    buildDateList()
    loadDetail()
    loadSlots()
    loadReviews()
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

.detail-container {
    min-height: calc(100vh - 130px);
    background: $bg-page;
    font-family: $font-rounded;
    color: $text-primary;
    padding: 20px 16px 80px;
    max-width: 960px;
    margin: 0 auto;
}

.back-row { margin-bottom: 14px;
    .back-link {
        color: $primary; cursor: pointer; font-size: 14px;
        &:hover { text-decoration: underline; }
    }
}

.intro-card, .booking-card, .review-card {
    position: relative;
    background: $bg-card; border: 1.5px solid $border-color;
    border-radius: $radius-lg; box-shadow: $shadow-card;
    padding: 26px 26px 22px; margin-bottom: 22px;
}

.card-tape {
    position: absolute; top: -8px; left: 36px;
    width: 70px; height: 16px; transform: rotate(-2deg);
    background: linear-gradient(135deg, rgba(232,146,124,0.45), rgba(247,232,166,0.55));
    border-radius: 2px;
}

.intro-head {
    display: flex; gap: 20px; align-items: flex-start; margin-bottom: 18px;
    .avatar {
        flex-shrink: 0; border: 2px solid #fff;
        box-shadow: 0 2px 8px rgba(180,140,100,0.25);
        background: $primary-light; color: $primary; font-size: 30px;
    }
    .head-info { flex: 1; min-width: 0; }
    .c-name { font-family: $font-handwritten; font-size: 26px; margin: 0 0 10px; }
    .c-sub { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; margin-bottom: 10px;
        font-size: 13px; color: $text-regular;
    }
    .c-title {
        color: $primary; background: $primary-light; border: 1px dashed $primary;
        border-radius: 10px; padding: 2px 10px; font-size: 12px;
    }
}

.specialty-tags { display: flex; flex-wrap: wrap; gap: 6px;
    .s-tag {
        font-size: 12px; color: #7a6a58; background: #f7efe0;
        border: 1px dashed $border-color; border-radius: 10px; padding: 2px 9px;
    }
}

.intro-body { border-top: 1.5px dashed $border-color; padding-top: 14px;
    .intro-label { font-size: 14px; color: $text-primary; margin-bottom: 8px; }
    .intro-text { font-size: 14px; line-height: 1.9; color: $text-regular; margin: 0; white-space: pre-wrap; }
}

.section-title {
    font-family: $font-handwritten; font-size: 21px; margin: 0 0 18px; color: $text-primary;
}

.date-strip {
    display: flex; gap: 10px; overflow-x: auto; padding-bottom: 8px; margin-bottom: 18px;
    .date-btn {
        flex-shrink: 0; width: 68px; background: $bg-card;
        border: 1.5px dashed $border-color; border-radius: $radius-md;
        padding: 8px 0; cursor: pointer; text-align: center;
        transition: all 0.2s ease;
        display: flex; flex-direction: column; gap: 4px;
        .date-week { font-size: 12px; color: $text-secondary; }
        .date-day { font-size: 15px; color: $text-primary; font-weight: 600; }
        &:hover { border-color: $primary; }
        &.active {
            background: linear-gradient(135deg, $primary, $primary-dark);
            border-style: solid;
            .date-week, .date-day { color: #fff; }
        }
    }
}

.slot-grid {
    display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px;
}

.slot-btn {
    background: $primary-light; border: 1.5px dashed $primary; border-radius: $radius-md;
    padding: 12px 8px; cursor: pointer; text-align: center;
    display: flex; flex-direction: column; gap: 6px; transition: all 0.2s ease;
    .slot-time { font-size: 15px; font-weight: 600; color: $primary-darker; }
    .slot-state { font-size: 12px; color: $primary; }
    &:hover {
        background: linear-gradient(135deg, $primary, $primary-dark);
        border-style: solid; transform: translateY(-2px);
        .slot-time, .slot-state { color: #fff; }
    }
}

.slot-tip { text-align: center; padding: 36px 0; color: $text-secondary; font-size: 14px; }

.review-list {
    .review-item {
        padding: 14px 4px; border-bottom: 1.5px dashed $border-color;
        &:last-child { border-bottom: none; }
    }
    .review-head {
        display: flex; align-items: center; gap: 10px; margin-bottom: 6px;
        .reviewer {
            font-size: 12px; color: $primary; background: $primary-light;
            border-radius: 8px; padding: 1px 8px;
        }
        .review-stars { font-size: 12px; }
        .review-time { font-size: 12px; color: $text-secondary; margin-left: auto; }
    }
    .review-content { margin: 0; font-size: 14px; line-height: 1.8; color: $text-regular; }

    .review-reply {
        margin-top: 10px; padding: 10px 12px;
        background: #F7F8FA; border-radius: 8px;
        font-size: 13px; line-height: 1.7; color: $text-regular;
        .reply-label { color: $primary; font-weight: 600; }
    }
}

.pager {
    display: flex; justify-content: center; margin-top: 16px;
    :deep(.el-pagination.is-background .el-pager li.is-active) {
        background: $primary; color: #fff;
    }
}

.booking-summary {
    background: #f7efe0; border: 1.5px dashed $border-color;
    border-radius: $radius-md; padding: 12px 16px; margin-bottom: 14px;
    p { margin: 6px 0; font-size: 14px; color: $text-regular; }
}

:deep(.booking-dialog) {
    border-radius: 18px;
    .el-dialog__header { border-bottom: 1.5px dashed $border-color; }
    .el-button--primary {
        background: $primary; border-color: $primary;
        &:hover { background: $primary-dark; border-color: $primary-dark; }
    }
}
</style>
