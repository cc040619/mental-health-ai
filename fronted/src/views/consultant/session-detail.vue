<template>
    <div class="session-detail" v-loading="detailLoading">
        <div class="back-row">
            <span class="back-link" @click="goBack">← 返回会话列表</span>
        </div>

        <div class="layout">
            <!-- 左栏：在线沟通 -->
            <el-card shadow="never" class="chat-card">
                <template #header>
                    <div class="chat-header">
                        <span class="section-title">💬 在线沟通</span>
                        <span class="conn-pill" :class="connected ? 'online' : 'offline'">
                            {{ connected ? '在线' : '离线' }}
                        </span>
                    </div>
                </template>

                <div ref="msgBoxRef" class="msg-list">
                    <div v-if="messages.length === 0" class="chat-empty">暂无留言，开始与来访者沟通吧</div>
                    <div v-for="m in messages" :key="m.id"
                         class="bubble-row" :class="isSelf(m) ? 'right' : 'left'">
                        <div class="bubble">
                            <div class="bubble-sender">{{ isSelf(m) ? '我（咨询师）' : '来访者' }}</div>
                            <div class="bubble-content">{{ m.content }}</div>
                            <div class="bubble-time">{{ formatTime(m.createdAt) }}</div>
                        </div>
                    </div>
                </div>

                <div class="chat-input">
                    <el-input v-model="draft" type="textarea" :rows="2" resize="none"
                              placeholder="输入留言（≤500字）" maxlength="500"
                              @keydown.enter.prevent="onSend" :disabled="sending" />
                    <el-button type="primary" :loading="sending" class="send-btn"
                               @click="onSend">发送</el-button>
                </div>
            </el-card>

            <!-- 右栏：Tab 信息 -->
            <el-card shadow="never" class="info-card">
                <template #header>
                    <div class="info-header">
                        <span class="section-title">来访者档案</span>
                        <el-tag :type="statusTagType(detail.status)" size="small">{{ detail.statusLabel }}</el-tag>
                    </div>
                </template>

                <el-tabs v-model="activeTab">
                    <el-tab-pane label="基本信息" name="info">
                        <div class="user-row">
                            <el-avatar :size="56" :src="detail.userAvatar" />
                            <div class="user-meta">
                                <div class="user-name">{{ detail.userNickname || `用户 #${detail.userId}` }}</div>
                                <div class="user-time">🗓️ {{ detail.appointmentDate }} {{ detail.startTime }}-{{ detail.endTime }}</div>
                                <div v-if="detail.problemDesc" class="user-problem">
                                    <span class="label">咨询问题：</span>{{ detail.problemDesc }}
                                </div>
                                <div v-if="detail.status === 'CANCELED' && detail.cancelReason" class="user-problem">
                                    <span class="label">取消原因：</span>{{ detail.cancelReason }}
                                </div>
                            </div>
                        </div>
                    </el-tab-pane>

                    <el-tab-pane label="测评记录" name="assessments">
                        <el-table :data="assessments" size="small" v-loading="tabLoading">
                            <el-table-column prop="scaleName" label="量表" min-width="140" show-overflow-tooltip />
                            <el-table-column prop="completedAt" label="完成时间" width="150">
                                <template #default="{ row }">{{ formatTime(row.completedAt) }}</template>
                            </el-table-column>
                            <el-table-column prop="totalScore" label="总分" width="80" align="center" />
                            <el-table-column label="等级" width="100" align="center">
                                <template #default="{ row }">
                                    <el-tag :type="levelTagType(row.level)" size="small">{{ levelLabel(row.level) }}</el-tag>
                                </template>
                            </el-table-column>
                        </el-table>
                        <el-pagination background small layout="total, prev, pager, next"
                                       :total="assessmentsTotal" :page-size="tabSize"
                                       :current-page="assessmentsPage" @current-change="onAssessmentsPage"
                                       v-if="assessmentsTotal > tabSize" />
                        <div class="hint">仅展示摘要，完整报告归来访者本人查看</div>
                    </el-tab-pane>

                    <el-tab-pane label="情绪日记" name="diaries">
                        <el-table :data="diaries" size="small" v-loading="tabLoading">
                            <el-table-column prop="diaryDate" label="日期" width="120" />
                            <el-table-column prop="dominantEmotion" label="主要情绪" width="100" />
                            <el-table-column prop="moodScore" label="评分" width="70" align="center" />
                            <el-table-column label="内容摘要" min-width="180" show-overflow-tooltip>
                                <template #default="{ row }">{{ row.diaryContent || '—' }}</template>
                            </el-table-column>
                        </el-table>
                        <el-pagination background small layout="total, prev, pager, next"
                                       :total="diariesTotal" :page-size="tabSize"
                                       :current-page="diariesPage" @current-change="onDiariesPage"
                                       v-if="diariesTotal > tabSize" />
                        <div class="hint">仅展示摘要，完整日记归来访者本人查看</div>
                    </el-tab-pane>

                    <el-tab-pane label="历史咨询记录" name="history">
                        <el-table :data="historyRecords" size="small" v-loading="tabLoading">
                            <el-table-column label="时间" width="160">
                                <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
                            </el-table-column>
                            <el-table-column label="主诉摘要" min-width="160" show-overflow-tooltip>
                                <template #default="{ row }">
                                    <el-button link type="primary" @click="showHistory(row)">查看</el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                        <el-pagination background small layout="total, prev, pager, next"
                                       :total="historyTotal" :page-size="tabSize"
                                       :current-page="historyPage" @current-change="onHistoryPage"
                                       v-if="historyTotal > tabSize" />
                    </el-tab-pane>

                    <el-tab-pane label="咨询记录" name="record">
                        <div v-loading="recordLoading">
                            <el-form label-position="top">
                                <el-form-item label="主诉（≤1000字）">
                                    <el-input v-model="record.chiefComplaint" type="textarea" :rows="3"
                                              maxlength="1000" show-word-limit />
                                </el-form-item>
                                <el-form-item label="分析（≤2000字）">
                                    <el-input v-model="record.analysis" type="textarea" :rows="4"
                                              maxlength="2000" show-word-limit />
                                </el-form-item>
                                <el-form-item label="干预方案（≤2000字）">
                                    <el-input v-model="record.interventionPlan" type="textarea" :rows="4"
                                              maxlength="2000" show-word-limit />
                                </el-form-item>
                                <el-form-item label="下次建议（≤1000字）">
                                    <el-input v-model="record.nextSuggestion" type="textarea" :rows="3"
                                              maxlength="1000" show-word-limit />
                                </el-form-item>
                                <div class="record-footer">
                                    <span v-if="record.updatedAt" class="update-time">
                                        更新于 {{ formatTime(record.updatedAt) }}
                                    </span>
                                    <el-button type="primary" :loading="recordSaving" @click="saveRecord">
                                        {{ record.id ? '更新记录' : '保存记录' }}
                                    </el-button>
                                </div>
                            </el-form>
                        </div>
                    </el-tab-pane>
                </el-tabs>
            </el-card>
        </div>
    </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, reactive, ref, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { connectAppointmentChat } from '@/utils/websocket'
import {
    getSessionDetail, getSessionAssessments, getSessionDiaries,
    getSessionHistoryRecords, getSessionMessages, sendSessionMessage,
    getSessionRecord, saveSessionRecord
} from '@/api/consultant'

const route = useRoute()
const router = useRouter()
const appointmentId = route.params.appointmentId

// ===== 详情 + 留言 =====
const detail = ref({})
const detailLoading = ref(false)
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const connected = ref(false)
const msgBoxRef = ref(null)
// 咨询师本人消息 senderType=2
const selfType = 2
let chatSocket = null
let pollTimer = null

const isSelf = (m) => Number(m.senderType) === selfType

const statusTagType = (status) => ({
    PENDING: 'warning', CONFIRMED: 'primary', COMPLETED: 'success', CANCELED: 'info'
}[status] || 'info')

const levelTagType = (level) => ({
    NORMAL: 'success', MILD: '', MODERATE: 'warning', SEVERE: 'danger'
}[level] || 'info')

const levelLabel = (level) => ({
    NORMAL: '正常', MILD: '轻度', MODERATE: '中度', SEVERE: '重度'
}[level] || level || '—')

const formatTime = (value) => {
    if (!value) return ''
    if (typeof value === 'string') return value.replace('T', ' ').slice(0, 16)
    return String(value)
}

const scrollToBottom = () => {
    nextTick(() => {
        const el = msgBoxRef.value
        if (el) el.scrollTop = el.scrollHeight
    })
}

const appendMessage = (msg) => {
    if (msg.id && messages.value.some(m => m.id === msg.id)) return
    messages.value.push(msg)
    scrollToBottom()
}

const loadDetail = () => {
    detailLoading.value = true
    getSessionDetail(appointmentId).then(data => {
        detail.value = data || {}
    }).finally(() => {
        detailLoading.value = false
    })
}

const loadMessages = () => {
    return getSessionMessages(appointmentId, { page: 1, size: 50 }).then(data => {
        // 后端按时间倒序返回最近消息，展示反转为时间正序
        const list = (data.records || []).slice().reverse()
        // 合并去重
        const existing = new Set(messages.value.map(m => m.id))
        list.forEach(m => {
            if (!existing.has(m.id)) messages.value.push(m)
        })
        messages.value.sort((a, b) => {
            const ta = new Date((a.createdAt || '').replace('T', ' ')).getTime()
            const tb = new Date((b.createdAt || '').replace('T', ' ')).getTime()
            return ta - tb
        })
        scrollToBottom()
    })
}

const onSend = () => {
    const content = draft.value.trim()
    if (!content) return
    if (content.length > 500) {
        ElMessage.warning('留言内容不能超过500字')
        return
    }
    sending.value = true
    const sentViaWs = chatSocket && chatSocket.send(content)
    if (!sentViaWs) {
        // WS 未连接，走 HTTP 兜底
        sendSessionMessage(appointmentId, content).then(saved => {
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

const goBack = () => router.push('/consultant/sessions')

// ===== 右栏 Tab 数据（懒加载）=====
const activeTab = ref('info')
const tabLoading = ref(false)
const tabSize = 10

const assessments = ref([])
const assessmentsTotal = ref(0)
const assessmentsPage = ref(1)

const diaries = ref([])
const diariesTotal = ref(0)
const diariesPage = ref(1)

const historyRecords = ref([])
const historyTotal = ref(0)
const historyPage = ref(1)

const loadAssessments = () => {
    tabLoading.value = true
    getSessionAssessments(appointmentId, { page: assessmentsPage.value, size: tabSize }).then(data => {
        assessments.value = data.records || []
        assessmentsTotal.value = Number(data.total) || 0
    }).finally(() => { tabLoading.value = false })
}

const loadDiaries = () => {
    tabLoading.value = true
    getSessionDiaries(appointmentId, { page: diariesPage.value, size: tabSize }).then(data => {
        diaries.value = data.records || []
        diariesTotal.value = Number(data.total) || 0
    }).finally(() => { tabLoading.value = false })
}

const loadHistory = () => {
    tabLoading.value = true
    getSessionHistoryRecords(appointmentId, { page: historyPage.value, size: tabSize }).then(data => {
        historyRecords.value = data.records || []
        historyTotal.value = Number(data.total) || 0
    }).finally(() => { tabLoading.value = false })
}

const onAssessmentsPage = (p) => { assessmentsPage.value = p; loadAssessments() }
const onDiariesPage = (p) => { diariesPage.value = p; loadDiaries() }
const onHistoryPage = (p) => { historyPage.value = p; loadHistory() }

const showHistory = (row) => {
    ElMessageBox.alert(
        `<div style="margin-bottom:8px"><b>主诉：</b>${row.chiefComplaint || '未填写'}</div>
         <div style="margin-bottom:8px"><b>分析：</b>${row.analysis || '未填写'}</div>
         <div style="margin-bottom:8px"><b>干预方案：</b>${row.interventionPlan || '未填写'}</div>
         <div><b>下次建议：</b>${row.nextSuggestion || '未填写'}</div>`,
        '历史咨询记录',
        { dangerouslyUseHTMLString: true, confirmButtonText: '关闭' }
    ).catch(() => {})
}

const onTabChange = (tab) => {
    if (tab === 'assessments' && assessments.value.length === 0) loadAssessments()
    else if (tab === 'diaries' && diaries.value.length === 0) loadDiaries()
    else if (tab === 'history' && historyRecords.value.length === 0) loadHistory()
}
watch(activeTab, onTabChange)

// ===== 咨询记录 =====
const record = reactive({
    id: null, chiefComplaint: '', analysis: '',
    interventionPlan: '', nextSuggestion: '', updatedAt: null
})
const recordLoading = ref(false)
const recordSaving = ref(false)

const loadRecord = () => {
    recordLoading.value = true
    getSessionRecord(appointmentId).then(data => {
        if (data) {
            record.id = data.id
            record.chiefComplaint = data.chiefComplaint || ''
            record.analysis = data.analysis || ''
            record.interventionPlan = data.interventionPlan || ''
            record.nextSuggestion = data.nextSuggestion || ''
            record.updatedAt = data.updatedAt
        }
    }).finally(() => { recordLoading.value = false })
}

const saveRecord = () => {
    recordSaving.value = true
    saveSessionRecord(appointmentId, {
        chiefComplaint: record.chiefComplaint || undefined,
        analysis: record.analysis || undefined,
        interventionPlan: record.interventionPlan || undefined,
        nextSuggestion: record.nextSuggestion || undefined
    }).then(() => {
        ElMessage.success(record.id ? '咨询记录已更新' : '咨询记录已保存')
        loadRecord()
    }).finally(() => { recordSaving.value = false })
}

onMounted(async () => {
    await loadDetail()
    loadMessages()
    loadRecord()
    // 复用 3.7 WS 端点（握手 token 校验已按登录态区分角色）
    chatSocket = connectAppointmentChat(appointmentId, {
        onMessage: (msg) => appendMessage(msg),
        onSystem: (text) => ElMessage.warning(text),
        onStatusChange: (online) => { connected.value = online },
        onReconnectFailed: () => {
            ElMessage.warning('实时连接暂不可用，消息将通过普通通道发送')
        }
    })
    // 轮询兜底：WS 断线期间也能收到来访者回复（20s 轻量拉取，按 id 去重）
    pollTimer = setInterval(loadMessages, 20000)
})

onBeforeUnmount(() => {
    if (chatSocket) chatSocket.close()
    if (pollTimer) clearInterval(pollTimer)
})
</script>

<style lang="scss" scoped>
.session-detail {
    .back-row {
        margin-bottom: 12px;
        .back-link {
            color: #2563eb;
            cursor: pointer;
            font-size: 14px;
            &:hover { text-decoration: underline; }
        }
    }
    .layout {
        display: flex;
        gap: 16px;
        align-items: stretch;
        height: calc(100vh - 220px);
        min-height: 540px;
    }
    .chat-card {
        flex: 1 1 55%;
        display: flex;
        flex-direction: column;
        border-color: #e5e7eb;
        :deep(.el-card__body) {
            flex: 1;
            display: flex;
            flex-direction: column;
            padding: 0;
            overflow: hidden;
        }
        :deep(.el-card__header) {
            padding: 12px 16px;
            border-bottom: 1px solid #e5e7eb;
        }
    }
    .info-card {
        flex: 1 1 45%;
        border-color: #e5e7eb;
        :deep(.el-card__body) { padding: 12px 16px; }
    }
    .chat-header, .info-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        .section-title { font-size: 15px; font-weight: 600; color: #1f2937; }
    }
    .conn-pill {
        font-size: 12px;
        padding: 2px 8px;
        border-radius: 10px;
        &.online { background: #dcfce7; color: #16a34a; }
        &.offline { background: #fee2e2; color: #dc2626; }
    }
    .msg-list {
        flex: 1;
        overflow-y: auto;
        padding: 16px;
        background: #f9fafb;
    }
    .chat-empty {
        text-align: center;
        color: #9ca3af;
        margin-top: 40px;
        font-size: 14px;
    }
    .bubble-row {
        display: flex;
        margin-bottom: 12px;
        &.right { justify-content: flex-end; }
        &.left { justify-content: flex-start; }
    }
    .bubble {
        max-width: 75%;
        padding: 8px 12px;
        border-radius: 10px;
        .bubble-sender { font-size: 11px; color: #6b7280; margin-bottom: 4px; }
        .bubble-content { font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; color: #1f2937; }
        .bubble-time { font-size: 10px; color: #9ca3af; text-align: right; margin-top: 4px; }
    }
    .left .bubble {
        background: #fff;
        border: 1px solid #e5e7eb;
        .bubble-sender { color: #4b5563; }
    }
    .right .bubble {
        background: #2563eb;
        .bubble-content, .bubble-sender, .bubble-time { color: #fff; }
    }
    .chat-input {
        display: flex;
        gap: 8px;
        padding: 12px;
        border-top: 1px solid #e5e7eb;
        background: #fff;
        align-items: flex-end;
        .send-btn { height: 60px; }
    }
    .user-row {
        display: flex;
        gap: 12px;
        align-items: flex-start;
        .user-meta { flex: 1; }
        .user-name { font-size: 16px; font-weight: 600; color: #1f2937; margin-bottom: 6px; }
        .user-time { font-size: 13px; color: #6b7280; margin-bottom: 6px; }
        .user-problem {
            font-size: 13px; color: #374151; background: #f3f4f6;
            padding: 8px 10px; border-radius: 6px;
            .label { color: #6b7280; }
        }
    }
    .hint {
        margin-top: 10px;
        font-size: 12px;
        color: #9ca3af;
        text-align: center;
    }
    .record-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 8px;
        .update-time { font-size: 12px; color: #9ca3af; }
    }
    :deep(.el-pagination) { margin-top: 10px; justify-content: center; }
}
</style>
