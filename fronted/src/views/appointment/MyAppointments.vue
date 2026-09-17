<template>
    <div class="appt-container">
        <!-- 便签标题 -->
        <div class="page-header">
            <div class="header-tape"></div>
            <h1 class="page-title">📒 我的预约</h1>
            <p class="page-subtitle">在这里查看咨询安排，和咨询师说说话 💛</p>
        </div>

        <!-- 状态 Tab -->
        <div class="status-tabs">
            <button v-for="t in tabs" :key="t.code"
                    class="tab-btn" :class="{ active: activeStatus === t.code }"
                    @click="switchTab(t.code)">
                {{ t.label }}
            </button>
        </div>

        <div v-if="loading" class="loading-tip">载入中... 🌿</div>
        <div v-else-if="list.length === 0" class="empty-tip">
            <div class="empty-emoji">🗒️</div>
            <p>暂无预约记录</p>
            <router-link to="/consultants" class="go-book">去预约咨询师 →</router-link>
        </div>
        <div v-else class="appt-list">
            <div v-for="a in list" :key="a.id" class="appt-card" @click="goDetail(a.id)">
                <div class="card-tape" :class="'tape-' + a.status.toLowerCase()"></div>
                <div class="card-main">
                    <el-avatar :size="56" :src="a.consultantAvatar" class="avatar">
                        {{ (a.consultantName || '?').charAt(0) }}
                    </el-avatar>
                    <div class="info">
                        <div class="info-row">
                            <span class="c-name">{{ a.consultantName }}</span>
                            <span class="status-badge" :class="'st-' + a.status.toLowerCase()">{{ a.statusLabel }}</span>
                        </div>
                        <div class="info-sub">
                            <span>🗓️ {{ a.appointmentDate }} {{ a.startTime }} - {{ a.endTime }}</span>
                        </div>
                        <div v-if="a.status === 'CANCELED' && a.cancelReason" class="cancel-mark">
                            取消原因：{{ a.cancelReason }}
                        </div>
                        <div v-if="a.evaluated && a.status === 'COMPLETED'" class="evaluated-mark">✨ 已评价</div>
                    </div>
                </div>
                <div class="card-actions" @click.stop>
                    <el-button v-if="a.canCancel" round size="small" plain @click="onCancel(a)">取消预约</el-button>
                    <el-button v-if="a.canReview" round size="small" type="primary" @click="openReview(a)">写评价</el-button>
                    <span class="enter-chat" @click="goDetail(a.id)">
                        {{ a.status === 'COMPLETED' || a.status === 'CANCELED' ? '查看详情' : '留言室' }} →
                    </span>
                </div>
            </div>
        </div>

        <div v-if="total > pageSize" class="pager">
            <el-pagination background layout="prev, pager, next"
                :current-page="page" :page-size="pageSize" :total="total"
                @current-change="onPageChange" />
        </div>

        <!-- 评价弹窗 -->
        <el-dialog v-model="reviewVisible" title="咨询评价" width="460px" class="review-dialog">
            <div class="rate-row">
                <span>整体感受：</span>
                <el-rate v-model="reviewRating" :max="5" :texts="['', '', '', '', '很有帮助']" show-text />
            </div>
            <el-input v-model="reviewContent" type="textarea" :rows="4" maxlength="500" show-word-limit
                      placeholder="分享这次咨询的感受，也能帮助其他来访者（选填，500 字以内）" />
            <template #footer>
                <el-button round @click="reviewVisible = false">取消</el-button>
                <el-button type="primary" round :loading="reviewSubmitting" @click="submitReview">提交评价</el-button>
            </template>
        </el-dialog>
        <!-- 取消预约弹窗：必填取消原因 + 30 分钟冷却提醒 -->
        <el-dialog v-model="cancelVisible" title="取消预约" width="460px">
            <el-alert type="warning" :closable="false" show-icon class="cooldown-tip"
                title="取消后 30 分钟内无法再次预约该咨询师，请慎重考虑" />
            <el-input v-model="cancelReason" type="textarea" :rows="4" maxlength="500" show-word-limit
                placeholder="请填写取消原因（必填，500 字以内）" style="margin-top: 16px" />
            <template #footer>
                <el-button round @click="cancelVisible = false">再想想</el-button>
                <el-button type="primary" round :loading="cancelSubmitting" :disabled="!cancelReason.trim()"
                    @click="submitCancel">确定取消</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { getMyAppointments, cancelAppointment, submitAppointmentReview } from '@/api/appointment'

const router = useRouter()

const tabs = [
    { code: '', label: '全部' },
    { code: 'PENDING', label: '待确认' },
    { code: 'CONFIRMED', label: '已确认' },
    { code: 'COMPLETED', label: '已完成' },
    { code: 'CANCELED', label: '已取消' }
]

const activeStatus = ref('')
const list = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)

const reviewVisible = ref(false)
const reviewAppointmentId = ref(null)
const reviewRating = ref(5)
const reviewContent = ref('')
const reviewSubmitting = ref(false)

// 取消预约弹窗
const cancelVisible = ref(false)
const cancelReason = ref('')
const cancelSubmitting = ref(false)
const cancelingAppointment = ref(null)

const switchTab = (code) => {
    activeStatus.value = code
    page.value = 1
    loadList()
}

const onPageChange = (p) => {
    page.value = p
    loadList()
}

const loadList = () => {
    loading.value = true
    const params = { page: page.value, size: pageSize }
    if (activeStatus.value) params.status = activeStatus.value
    getMyAppointments(params).then(data => {
        list.value = data.records || []
        total.value = data.total || 0
    }).finally(() => {
        loading.value = false
    })
}

const goDetail = (id) => {
    router.push(`/appointments/${id}`)
}

const onCancel = (a) => {
    cancelingAppointment.value = a
    cancelReason.value = ''
    cancelVisible.value = true
}

const submitCancel = () => {
    const reason = cancelReason.value.trim()
    if (!reason) {
        ElMessage.warning('请填写取消原因')
        return
    }
    cancelSubmitting.value = true
    cancelAppointment(cancelingAppointment.value.id, reason).then(() => {
        ElNotification({ title: '已取消预约', message: '时段已释放，30 分钟内无法再次预约该咨询师 🌿', type: 'info' })
        cancelVisible.value = false
        loadList()
    }).finally(() => {
        cancelSubmitting.value = false
    })
}

const openReview = (a) => {
    reviewAppointmentId.value = a.id
    reviewRating.value = 5
    reviewContent.value = ''
    reviewVisible.value = true
}

const submitReview = () => {
    if (!reviewRating.value) {
        ElMessage.warning('请先选择评分')
        return
    }
    reviewSubmitting.value = true
    submitAppointmentReview(reviewAppointmentId.value, {
        rating: reviewRating.value,
        content: reviewContent.value || null
    }).then(() => {
        reviewVisible.value = false
        ElNotification({ title: '感谢你的分享 💌', message: '评价已提交', type: 'success' })
        loadList()
    }).finally(() => {
        reviewSubmitting.value = false
    })
}

onMounted(() => {
    loadList()
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

.appt-container {
    min-height: calc(100vh - 130px);
    background: $bg-page;
    font-family: $font-rounded;
    color: $text-primary;
    padding: 24px 16px 80px;
    max-width: 860px;
    margin: 0 auto;
}

.page-header {
    text-align: center; margin-bottom: 24px;
    .header-tape {
        width: 120px; height: 10px;
        background: linear-gradient(90deg, transparent, rgba(232,146,124,0.5), transparent);
        border-radius: 5px; margin: 0 auto 14px;
    }
    .page-title {
        font-family: $font-handwritten; font-size: 30px; font-weight: 600; margin: 0 0 8px;
    }
    .page-subtitle { font-size: 14px; color: $text-secondary; margin: 0; }
}

.status-tabs {
    display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; margin-bottom: 26px;
    .tab-btn {
        font-family: $font-rounded; background: $bg-card; border: 1.5px dashed $border-color;
        color: $text-regular; border-radius: 20px; padding: 8px 18px; font-size: 14px;
        cursor: pointer; transition: all 0.25s ease;
        &:hover { border-color: $primary; color: $primary; }
        &.active {
            background: linear-gradient(135deg, $primary, $primary-dark);
            border-style: solid; color: #fff;
        }
    }
}

.appt-list { display: flex; flex-direction: column; gap: 18px; }

.appt-card {
    position: relative; background: $bg-card; border: 1.5px solid $border-color;
    border-radius: $radius-lg; box-shadow: $shadow-card;
    padding: 20px 22px 16px; cursor: pointer;
    transition: transform 0.25s ease, box-shadow 0.25s ease;
    &:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(180,140,100,0.18); }
    .card-tape {
        position: absolute; top: -7px; left: 30px;
        width: 56px; height: 14px; border-radius: 2px; transform: rotate(-2deg);
    }
    .tape-pending { background: rgba(247, 200, 110, 0.65); }
    .tape-confirmed { background: rgba(155, 196, 163, 0.65); }
    .tape-completed { background: rgba(167, 196, 224, 0.7); }
    .tape-canceled { background: rgba(200, 190, 175, 0.6); }
}

.card-main {
    display: flex; gap: 16px; align-items: center; margin-bottom: 12px;
    .avatar {
        flex-shrink: 0; border: 2px solid #fff;
        box-shadow: 0 2px 8px rgba(180,140,100,0.2);
        background: $primary-light; color: $primary;
    }
    .info { flex: 1; min-width: 0; }
    .info-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
    .c-name { font-family: $font-handwritten; font-size: 19px; }
    .info-sub { font-size: 13px; color: $text-regular; }
    .evaluated-mark { font-size: 12px; color: #b8923f; margin-top: 4px; }
    .cancel-mark {
        font-size: 12px; color: #b45309; margin-top: 4px;
        background: rgba(247, 200, 110, 0.15); border-radius: 8px; padding: 2px 8px;
        width: fit-content;
    }
}

.status-badge {
    font-size: 12px; border-radius: 10px; padding: 2px 10px; border: 1px dashed;
    &.st-pending { color: #c9922f; background: rgba(247,200,110,0.18); border-color: #e4b960; }
    &.st-confirmed { color: #4f8060; background: rgba(155,196,163,0.25); border-color: #9bc4a3; }
    &.st-completed { color: #5a7d9e; background: rgba(167,196,224,0.25); border-color: #a7c4e0; }
    &.st-canceled { color: #9a8f7f; background: rgba(200,190,175,0.2); border-color: #c8beaf; }
}

.card-actions {
    display: flex; align-items: center; justify-content: flex-end; gap: 12px;
    padding-top: 12px; border-top: 1.5px dashed $border-color;
    .el-button--primary {
        background: $primary; border-color: $primary;
        &:hover { background: $primary-dark; border-color: $primary-dark; }
    }
    .enter-chat { font-size: 13px; color: $primary; cursor: pointer; &:hover { text-decoration: underline; } }
}

.loading-tip, .empty-tip {
    text-align: center; padding: 60px 0; color: $text-secondary; font-size: 15px;
    .empty-emoji { font-size: 48px; margin-bottom: 12px; }
    .go-book {
        display: inline-block; margin-top: 14px; color: $primary;
        background: $primary-light; border: 1.5px dashed $primary;
        border-radius: $radius-md; padding: 8px 20px; text-decoration: none; font-size: 14px;
        &:hover { background: $primary; color: #fff; border-style: solid; }
    }
}

.pager {
    display: flex; justify-content: center; margin-top: 28px;
    :deep(.el-pagination.is-background .el-pager li.is-active) {
        background: $primary; color: #fff;
    }
}

.rate-row { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; font-size: 14px; }

:deep(.review-dialog) {
    border-radius: 18px;
    .el-dialog__header { border-bottom: 1.5px dashed $border-color; }
    .el-button--primary {
        background: $primary; border-color: $primary;
        &:hover { background: $primary-dark; border-color: $primary-dark; }
    }
}
</style>
