<template>
    <div class="detail-container">
        <div class="back-row">
            <span class="back-link" @click="goBack">← 返回</span>
        </div>

        <template v-if="appointment">
            <!-- 预约信息卡 -->
            <div class="info-card">
                <div class="card-tape"></div>
                <div class="info-head">
                    <el-avatar :size="60" :src="appointment.consultantAvatar" class="avatar">
                        {{ (appointment.consultantName || '?').charAt(0) }}
                    </el-avatar>
                    <div class="head-info">
                        <div class="title-row">
                            <h1 class="c-name">{{ appointment.consultantName }}</h1>
                            <span class="status-badge" :class="'st-' + appointment.status.toLowerCase()">
                                {{ appointment.statusLabel }}
                            </span>
                        </div>
                        <p class="c-title">{{ appointment.consultantTitle }}</p>
                        <p class="c-time">🗓️ {{ appointment.appointmentDate }} {{ appointment.startTime }} - {{ appointment.endTime }}</p>
                    </div>
                </div>
                <div v-if="appointment.problemDesc" class="problem-box">
                    <span class="problem-label">📝 咨询问题：</span>{{ appointment.problemDesc }}
                </div>
                <div class="info-actions">
                    <el-button v-if="appointment.canCancel" round plain size="small" @click="onCancel">取消预约</el-button>
                    <el-button v-if="appointment.canReview" round type="primary" size="small" @click="reviewVisible = true">写评价</el-button>
                </div>
            </div>

            <!-- 留言室 -->
            <div class="chat-card">
                <div class="chat-header">
                    <h2 class="section-title">💬 留言沟通</h2>
                    <span class="conn-pill" :class="connected ? 'online' : 'offline'">
                        {{ connected ? '实时连接中' : '连接断开（重连中/普通发送）' }}
                    </span>
                </div>

                <div ref="msgBoxRef" class="msg-list">
                    <div v-if="messages.length === 0" class="chat-empty">
                        还没有留言，有什么想提前告诉咨询师的吗？🌱
                    </div>
                    <div v-for="m in messages" :key="m.id"
                         class="bubble-row" :class="isSelf(m) ? 'right' : 'left'">
                        <div class="bubble">
                            <div class="bubble-sender">{{ isSelf(m) ? '我' : (selfType === 2 ? '来访者' : '咨询师') }}</div>
                            <div class="bubble-content">{{ m.content }}</div>
                            <div class="bubble-time">{{ formatTime(m.createdAt) }}</div>
                        </div>
                    </div>
                </div>

                <div class="chat-input">
                    <el-input v-model="draft" type="textarea" :rows="2" resize="none"
                              maxlength="1000"
                              :placeholder="'写下想对' + (selfType === 2 ? '来访者' : '咨询师') + '说的话…（Enter 发送，Shift+Enter 换行）'"
                              @keydown.enter.exact.prevent="onSend" />
                    <el-button type="primary" round :loading="sending" class="send-btn" @click="onSend">发送</el-button>
                </div>
            </div>
        </template>

        <!-- 评价弹窗（与我的预约页同款） -->
        <el-dialog v-model="reviewVisible" title="咨询评价" width="460px" class="review-dialog">
            <div class="rate-row">
                <span>整体感受：</span>
                <el-rate v-model="reviewRating" :max="5" show-text />
            </div>
            <el-input v-model="reviewContent" type="textarea" :rows="4" maxlength="500" show-word-limit
                      placeholder="分享这次咨询的感受（选填，500 字以内）" />
            <template #footer>
                <el-button round @click="reviewVisible = false">取消</el-button>
                <el-button type="primary" round :loading="reviewSubmitting" @click="submitReview">提交评价</el-button>
            </template>
        </el-dialog>

        <!-- 取消预约弹窗：必填取消原因 + 30 分钟冷却提醒 -->
        <el-dialog v-model="cancelVisible" title="取消预约" width="460px">
            <el-alert type="warning" :closable="false" show-icon
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
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { getAppointmentDetail, getAppointmentMessages, sendAppointmentMessage,
    cancelAppointment, submitAppointmentReview } from '@/api/appointment'
import { connectAppointmentChat } from '@/utils/websocket'

const route = useRoute()
const router = useRouter()

const appointment = ref(null)
const messages = ref([])
const connected = ref(false)
const draft = ref('')
const sending = ref(false)
const msgBoxRef = ref(null)
let chatSocket = null

// 管理员在后台模拟咨询师：本人消息 senderType=2；普通用户为 1
const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
const selfType = Number(userInfo.userType) === 2 ? 2 : 1
// 管理员从后台进入本页（复用同一组件）
const isAdminEntry = route.path.startsWith('/back')

const reviewVisible = ref(false)
const reviewRating = ref(5)
const reviewContent = ref('')
const reviewSubmitting = ref(false)

// 取消预约弹窗
const cancelVisible = ref(false)
const cancelReason = ref('')
const cancelSubmitting = ref(false)

const isSelf = (m) => Number(m.senderType) === selfType

const formatTime = (value) => {
    if (!value) return ''
    if (typeof value === 'string') {
        // 后端 LocalDateTime ISO：2026-09-14T10:30:00
        return value.replace('T', ' ').slice(11, 16)
    }
    return String(value)
}

const scrollToBottom = () => {
    nextTick(() => {
        const el = msgBoxRef.value
        if (el) el.scrollTop = el.scrollHeight
    })
}

const appendMessage = (msg) => {
    // WS 回执与 HTTP 兜底可能重复，按 id 去重
    if (msg.id && messages.value.some(m => m.id === msg.id)) return
    messages.value.push(msg)
    scrollToBottom()
}

const loadDetail = () => {
    return getAppointmentDetail(route.params.id).then(data => {
        appointment.value = data
    })
}

const loadMessages = () => {
    getAppointmentMessages(route.params.id, { page: 1, size: 50 }).then(data => {
        // 后端按时间倒序返回最近消息，展示反转为时间正序
        messages.value = (data.records || []).slice().reverse()
        scrollToBottom()
    })
}

const onSend = () => {
    const content = draft.value.trim()
    if (!content || sending.value) return
    sending.value = true
    // WebSocket 优先：send 返回 false 表示未连接，走 HTTP 兜底
    const sentViaWs = chatSocket && chatSocket.send(content)
    if (!sentViaWs) {
        sendAppointmentMessage(route.params.id, { content }).then(saved => {
            appendMessage(saved)
        }).finally(() => {
            sending.value = false
            draft.value = ''
        })
        return
    }
    // WS 广播会作为发送成功回执推回来，这里只清空输入
    sending.value = false
    draft.value = ''
}

const onCancel = () => {
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
    cancelAppointment(appointment.value.id, reason).then(() => {
        ElNotification({ title: '已取消预约', message: '时段已释放，30 分钟内无法再次预约该咨询师 🌿', type: 'info' })
        cancelVisible.value = false
        loadDetail()
    }).finally(() => {
        cancelSubmitting.value = false
    })
}

const submitReview = () => {
    if (!reviewRating.value) {
        ElMessage.warning('请先选择评分')
        return
    }
    reviewSubmitting.value = true
    submitAppointmentReview(appointment.value.id, {
        rating: reviewRating.value,
        content: reviewContent.value || null
    }).then(() => {
        reviewVisible.value = false
        ElNotification({ title: '感谢你的分享 💌', message: '评价已提交', type: 'success' })
        loadDetail()
    }).finally(() => {
        reviewSubmitting.value = false
    })
}

const goBack = () => {
    if (isAdminEntry) {
        router.push('/back/appointments')
        return
    }
    if (window.history.length > 1) router.back()
    else router.push('/appointments')
}

onMounted(async () => {
    await loadDetail()
    loadMessages()
    chatSocket = connectAppointmentChat(route.params.id, {
        onMessage: (msg) => appendMessage(msg),
        onSystem: (text) => ElMessage.warning(text),
        onStatusChange: (online) => { connected.value = online },
        onReconnectFailed: () => {
            ElMessage.warning('实时连接暂不可用，消息将通过普通通道发送')
        }
    })
})

onBeforeUnmount(() => {
    if (chatSocket) chatSocket.close()
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
    max-width: 820px;
    margin: 0 auto;
}

.back-row { margin-bottom: 14px;
    .back-link { color: $primary; cursor: pointer; font-size: 14px; &:hover { text-decoration: underline; } }
}

.info-card, .chat-card {
    position: relative; background: $bg-card; border: 1.5px solid $border-color;
    border-radius: $radius-lg; box-shadow: $shadow-card;
    padding: 24px 24px 20px; margin-bottom: 22px;
}

.card-tape {
    position: absolute; top: -8px; left: 36px;
    width: 70px; height: 16px; transform: rotate(-2deg);
    background: linear-gradient(135deg, rgba(232,146,124,0.45), rgba(247,232,166,0.55));
    border-radius: 2px;
}

.info-head { display: flex; gap: 18px; align-items: flex-start;
    .avatar {
        flex-shrink: 0; border: 2px solid #fff;
        box-shadow: 0 2px 8px rgba(180,140,100,0.25);
        background: $primary-light; color: $primary; font-size: 24px;
    }
    .head-info { flex: 1; min-width: 0; }
    .title-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
    .c-name { font-family: $font-handwritten; font-size: 23px; margin: 0; }
    .c-title { margin: 6px 0 4px; font-size: 13px; color: $text-secondary; }
    .c-time { margin: 0; font-size: 14px; color: $text-regular; }
}

.status-badge {
    font-size: 12px; border-radius: 10px; padding: 2px 10px; border: 1px dashed;
    &.st-pending { color: #c9922f; background: rgba(247,200,110,0.18); border-color: #e4b960; }
    &.st-confirmed { color: #4f8060; background: rgba(155,196,163,0.25); border-color: #9bc4a3; }
    &.st-completed { color: #5a7d9e; background: rgba(167,196,224,0.25); border-color: #a7c4e0; }
    &.st-canceled { color: #9a8f7f; background: rgba(200,190,175,0.2); border-color: #c8beaf; }
}

.problem-box {
    margin-top: 14px; background: #f7efe0; border: 1.5px dashed $border-color;
    border-radius: $radius-md; padding: 12px 14px; font-size: 13px; line-height: 1.8; color: $text-regular;
    .problem-label { color: $text-primary; }
}

.info-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 12px;
    .el-button--primary {
        background: $primary; border-color: $primary;
        &:hover { background: $primary-dark; border-color: $primary-dark; }
    }
}

.chat-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px;
    .section-title { font-family: $font-handwritten; font-size: 20px; margin: 0; }
}

.conn-pill {
    font-size: 12px; border-radius: 10px; padding: 3px 10px;
    &.online { color: #4f8060; background: rgba(155,196,163,0.25); border: 1px dashed #9bc4a3; }
    &.offline { color: #b07a3f; background: rgba(247,200,110,0.18); border: 1px dashed #e4b960; }
}

.msg-list {
    height: 380px; overflow-y: auto; padding: 14px;
    background: #fdf6e7; border: 1.5px dashed $border-color; border-radius: $radius-md;
    display: flex; flex-direction: column; gap: 14px;
    .chat-empty { margin: auto; color: $text-secondary; font-size: 13px; }
}

.bubble-row { display: flex;
    &.left { justify-content: flex-start; }
    &.right { justify-content: flex-end; }
}

.bubble {
    max-width: 72%; padding: 10px 14px; border-radius: 14px; position: relative;
    .bubble-sender { font-size: 11px; color: $text-secondary; margin-bottom: 4px; }
    .bubble-content { font-size: 14px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; }
    .bubble-time { font-size: 10px; color: $text-secondary; text-align: right; margin-top: 4px; }
}

.left .bubble {
    background: #fff; border: 1.5px solid $border-color;
    border-top-left-radius: 4px;
    .bubble-sender { color: #7a9a86; }
}

.right .bubble {
    background: linear-gradient(135deg, rgba(232,146,124,0.92), rgba(217,130,107,0.92));
    color: #fff; border-top-right-radius: 4px;
    .bubble-sender, .bubble-time { color: rgba(255,255,255,0.8); }
}

.chat-input {
    display: flex; gap: 10px; align-items: flex-end; margin-top: 14px;
    flex: 1;
    .el-textarea { flex: 1; }
    .send-btn {
        background: $primary; border-color: $primary; flex-shrink: 0;
        &:hover { background: $primary-dark; border-color: $primary-dark; }
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
