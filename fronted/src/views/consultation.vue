<template>
    <div class="consultation-container">
        <!-- ===== 手账风：纸张纹理背景层（纯装饰） ===== -->
        <div class="paper-bg"></div>

        <!-- ===== 左侧栏：AI助手信息 + 情绪花园 + 会话列表 ===== -->
        <aside class="sidebar">
            <!-- ===== 手账风：AI助手信息便签卡 ===== -->
            <div class="ai-card">
                <div class="washi-tape"></div>
                <div class="breathing-circle">
                    <el-image :src="iconUrl" style="width: 25px;height:25px" alt="AI助手" />
                </div>
                <h3 class="assistant-name">📖 小聪AI助手</h3>
                <div class="online-status">
                    <div class="status-dot"></div>
                    在线服务中
                </div>
            </div>

            <!-- ===== 手账风：情绪花园便签卡 ===== -->
            <div class="emotion-card">
                <div class="card-header">
                    <span class="card-icon">🌻</span>
                    <span class="card-title">情绪花园</span>
                </div>
                <div class="emotion-info">
                    <div class="emotion-name">中性</div>
                    <div class="emotion-score">50</div>
                </div>
                <div class="warm-tips">
                    <div class="emotion-status-text">
                        <span class="status-label">今天感觉</span>
                        <span class="status-emotion">{{ currentEmotion.isNegative ? '需要关注' : '很不错' }}</span>
                    </div>
                    <div class="emotion-intensity">
                        <span class="intensity-dots">
                            <span v-for="dot in 3" :key="dot" class="dot" :class="{'active': getIntensityClass(currentEmotion.emotionScore) >= dot}"></span>
                        </span>
                        <span class="intensity-text">
                            {{ getRiskText(currentEmotion.riskLevel) }}
                        </span>
                    </div>
                    <!-- 温暖建议卡片 -->
                     <div class="warm-suggestion" v-if="currentEmotion.suggestion">
                        <div class="suggestion-icon">💝</div>
                        <div class="suggestion-content">
                            <div class="suggestion-title">给你的小建议</div>
                            <div class="suggestion-text">{{ currentEmotion.suggestion }}</div>
                        </div>
                     </div>
                     <!-- 治愈行动 -->
                      <div class="healing-actions" v-if="currentEmotion.improvementSuggestions.length > 0">
                        <div class="actions-title">治愈小行动</div>
                        <div class="actions-list">
                            <div v-for="action in currentEmotion.improvementSuggestions" :key="action" class="action-item">
                                <div class="action-icon">✨</div>
                                <div class="action-text">{{ action }}</div>
                            </div>
                        </div>
                      </div>
                      <!-- 风险提示 -->
                    <div class="risk-notice" v-if="currentEmotion.isNegative && currentEmotion.riskLevel > 1">
                        <div class="notice-icon">🤗</div>
                        <div class="notice-content">
                            <div class="notice-title">温馨提示</div>
                            <div class="notice-text">{{ currentEmotion.riskDescription }}</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- ===== 手账风：会话列表便签卡 ===== -->
             <div class="session-card">
                <div class="card-header">
                    <span class="card-icon">🗂️</span>
                    <span class="card-title">会话列表</span>
                </div>
                <div class="session-list">
                    <div v-for="session in sessionList" :key="session.id" @click="handleSessionClick(session)" class="session-item">
                        <div class="session-info">
                            <div class="session-title">
                                <span>{{ session.sessionTitle }}</span>
                                <div class="session-meta">
                                    <span class="session-time">{{ session.startedAt }}</span>
                                </div>
                                <div class="session-preview">
                                    {{ session.lastMessageContent }}
                                </div>
                                <div class="session-stats">
                                    <span>
                                        <el-icon>
                                            <ChatRound />
                                        </el-icon>
                                        {{ session.messageCount || 0 }}
                                    </span>
                                    <span>
                                        <el-icon>
                                            <Clock />
                                        </el-icon>
                                        {{ formatTimeAgo(session.startedAt) }}
                                    </span>
                                </div>
                            </div>
                            <div class="session-actions">
                                <el-button text type="danger" size="mini" @click="handleDeleteSession(session.id)">
                                    <el-icon>
                                        <DeleteFilled />
                                    </el-icon>
                                </el-button>
                            </div>
                        </div>
                    </div>
                </div>
             </div>
        </aside>

        <!-- ===== 右侧：聊天主区域 ===== -->
        <div class="chat-main">
            <div class="chat-header">
                <div class="header-left">
                    <div class="chat-avatar">
                        <el-image :src="iconUrl1" style="width: 30px;height: 30px" />
                    </div>
                    <div class="chat-info">
                        <h2>小聪AI助手</h2>
                        <p>您的贴心AI心理健康助手</p>
                    </div>
                </div>
                <el-button circle @click="createNewFrontendSession" title="新建会话">
                    <el-icon>
                        <Plus />
                    </el-icon>
                </el-button>
            </div>
            <!-- 聊天消息区域 -->
            <div class="chat-messages" ref="chatMessagesRef">
                <!-- 欢迎用语 -->
                <div class="message-item ai-message" v-if="messages.length === 0">
                    <div class="message-avatar">
                        <el-image :src="iconUrl" style="width: 18px;height: 18px" />
                    </div>
                    <div class="message-content">
                        <div class="message-bubble">
                            <p>您好！我是小暖，您的AI心理健康助手。很高兴陪伴您，为您提供温暖的心理支持。请告诉我，今天您感觉怎么样？有什么想要分享的吗？</p>
                        </div>
                        <div class="message-time">刚刚</div>
                    </div>
                </div>
                <!-- 消息列表 -->
                <div v-for="msg in messages" :key="msg.id" class="message-item" :class="msg.senderType === 1 ?  'user-message' : 'ai-message'">
                    <div class="message-avatar">
                        <el-image v-if="msg.senderType === 1" style="width: 18px; height:18px" :src="iconUrl2"></el-image>
                        <el-image v-if="msg.senderType === 2" style="width: 18px; height:18px" :src="iconUrl"></el-image>
                    </div>
                    <div class="message-content">
                        <div class="message-bubble">
                            <!-- AI正在思考中 -->
                            <div v-if="msg.senderType === 2 && isAiTyping && !msg.content" class="typing-indicator">
                                <div class="typing-dot"></div>
                                <div class="typing-dot"></div>
                                <div class="typing-dot"></div>
                            </div>
                            <!-- AI错误提示 -->
                            <div v-else-if="msg.isError" class="error-message">
                                <p>{{ msg.content }}</p>
                            </div>
                            <!-- AI正常返回消息 -->
                             <MarkdownRenderer v-else-if="msg.senderType === 2 && !msg.isError" :content="msg.content" :is-ai-message="true" />
                             <p v-else-if="msg.content" v-html="formatMessageContent(msg.content)"></p>
                        </div>
                        <div class="message-time">{{ msg.senderType === 2 && isAiTyping ? '正在输入中...' : msg.createdAt }}</div>
                    </div>
                </div>
            </div>
            <!-- 消息输入区域 -->
            <div class="chat-input">
                <div class="input-container">
                    <el-input
                        v-model="userMessage"
                        placeholder="请输入您想要分享的内容..."
                        type="textarea"
                        :rows="3"
                        :disabled="isAiTyping"
                        @keydown="handleKeyDown"
                        class="message-input"
                        clearable />
                        <div class="input-footer">
                            <span>按Enter发送，Shift+Enter换行</span>
                            <span>{{ userMessage.length }}/500</span>
                        </div>
                </div>
                <el-button :disabled="!userMessage.trim() || userMessage.length > 500" type="primary" class="send-btn" @click="sendMessage">
                    <el-icon>
                        <Promotion />
                    </el-icon>
                </el-button>
            </div>
        </div>
    </div>
</template>
<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { startSession, getSessionList, deleteSession, getSessionDetail, getSessionEmotion } from '@/api/frontend'
import { ElMessage } from 'element-plus'
import { ChatRound, DeleteFilled } from '@element-plus/icons-vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { fetchEventSource } from '@microsoft/fetch-event-source'

const iconUrl = new URL('@/assets/images/robot-fill.png', import.meta.url).href
const iconUrl1 = new URL('@/assets/images/like.png', import.meta.url).href
const iconUrl2 = new URL('@/assets/images/users.png', import.meta.url).href

// 新建会话
const createNewFrontendSession = () => {
    // 已经是空的临时会话时，避免重复创建导致正在输入的内容丢失
    if (currentSession.value && currentSession.value.status === 'TEMP' && messages.value.length === 0) return
    // 创建一个新的会话对象
    const newSession = {
        sessionId: `temp_${Date.now()}`,
        status: 'TEMP',
        sessionTitle: '新对话'
    }
    currentSession.value = newSession
    // 清空聊天记录与输入框，回到欢迎语状态
    messages.value = []
    userMessage.value = ''
    // 重置情绪花园为默认状态
    currentEmotion.value = {
        primaryEmotion: '中性',
        emotionScore: 50,
        isNegative: false,
        riskLevel: 0,
        suggestion: '情绪状态平稳',
        improvementSuggestions: []
    }
}

// 定义一个当前会话对象
const currentSession = ref(null)
const sessionList = ref([])

// 定义对话消息
const messages = ref([])
// 聊天消息区域DOM
const chatMessagesRef = ref(null)
// 滚动聊天区到底部（等待DOM更新后执行）
const scrollToBottom = () => {
    nextTick(() => {
        const el = chatMessagesRef.value
        if (el) {
            el.scrollTop = el.scrollHeight
        }
    })
}
// 定义用户输入消息
const userMessage = ref('')
// 定义AI助手是否正在输入
const isAiTyping = ref(false)

// 情绪花园
const currentEmotion = ref({
    primaryEmotion: '中性',
    emotionScore: 50,
    isNegative: false,
    riskLevel: 0,
    suggestion: '情绪状态平稳',
    improvementSuggestions: []
})

const loadSessionEmotion = (sessionId) => {
   // 确保sessionID格式正确
    const id = sessionId.toString().startsWith('session_') ? sessionId : `session_${sessionId}`

    getSessionEmotion(id).then(res => {
        console.log(res)
        // 与默认值合并：接口缺失的字段保留默认值，避免覆盖后条件渲染不展示
        const prev = currentEmotion.value
        currentEmotion.value = {
            primaryEmotion: res.primaryEmotion || prev.primaryEmotion,
            emotionScore: res.emotionScore ?? prev.emotionScore,
            isNegative: res.isNegative ?? prev.isNegative,
            riskLevel: res.riskLevel ?? prev.riskLevel,
            suggestion: res.suggestion || prev.suggestion || '',
            improvementSuggestions: Array.isArray(res.improvementSuggestions) ? res.improvementSuggestions : (prev.improvementSuggestions || []),
            riskDescription: res.riskDescription || prev.riskDescription || ''
        }
    })
}

const getIntensityClass = (score) => {
    if (score >= 61) {
        return 3
    }
    if (score >= 31) {
        return 2
    }
    return 1
}

const getRiskText = (level) => {
    switch (level) {
        case 0:
            return '正常'
        case 1:
            return '关注'
        case 2:
            return '预警'
        case 3:
            return '危机'
        default:
            return '正常'
    }
}

// 定义处理键盘事件
const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        // 按回车发送消息（内容非空且不超过500字）
        if (userMessage.value.trim() && userMessage.value.length <= 500) {
            sendMessage()
        }
    }
}

// 用户发送消息
const sendMessage = () => {
    if (!userMessage.value.trim()) return

    if (isAiTyping.value) {
        ElMessage.error('AI助手正在输入中，请稍后')
        return
    }

    const message = userMessage.value.trim()
    userMessage.value = ''

    // 如果没有会话或者是临时会话，就需要创建一个新的会话
    if (currentSession.value.status === 'TEMP') {
       startNewSession(message)
    } else {
        // 继续现有会话
        messages.value.push({
            id: Date.now(),
            senderType: 1,
            content: message,
            createAt: new Date().toISOString()
        })
        scrollToBottom()
        startAIResponse(currentSession.value.sessionId, message)
    }
}

const startNewSession = (message) => {
    // 构建会话参数
    const sessionParams = {
        initialMessage: message
    }
    if (currentSession.value.sessionTitle === '新对话') {
        sessionParams.sessionTitle = `小聪AI助手 - ${new Date().toLocaleString()}`
    } else {
        // 如果历史会话记录
        sessionParams.sessionTitle = currentSession.value.sessionTitle
    }
    // 调用后端接口创建新会话
    startSession(sessionParams).then(res => {
        console.log(res)
       // 将后端返回的数据转为前端会话格式
       const sessionData = {
            sessionId: res.sessionId,
            status: res.status,
            sessionTitle: sessionParams.sessionTitle
       }
       // 如果当前是临时会话，更新数据
       if (currentSession.value && currentSession.value.status === 'TEMP') {
            // 更新为正式会话 
            Object.assign(currentSession.value, sessionData)
       } else {
            // 否则，创建一个新的会话
            currentSession.value = sessionData
       }
       // 更新会话列表
       getSessionPage()

       // 添加初始用户消息
       messages.value.push({
        id: Date.now(),
        senderType: 1,
        content: message,
        createAt: new Date().toISOString()
       })
       scrollToBottom()

       // 开始流式对话
       startAIResponse(currentSession.value.sessionId, message)
    })
}

const startAIResponse = (sessionId, userMessage) => {
    // 防止重复发送
    if (isAiTyping.value) {
        ElMessage.error('AI助手正在输入中，请稍后')
        return
    }

    
    isAiTyping.value = true

    const aiMessage = {
        id: `ai_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
        senderType: 2,
        content: '',
        createAt: new Date().toISOString()
    }
    messages.value.push(aiMessage)
    scrollToBottom()

    // 调用流式接口
    const ctrl = new AbortController() // 用来中止fetch请求
    fetchEventSource('/api/psychological-chat/stream', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Token': localStorage.getItem('token'),
            'Accept': 'text/event-stream'
        },
        body: JSON.stringify({
            sessionId,
            userMessage
        }),
        signal: ctrl.signal,
        onopen: (response) => {
            console.log(response)
            if (response.headers.get('Content-Type') !== 'text/event-stream') {
                ElMessage.error('服务器返回非流式数据')
            }
        },
        onmessage: (event) => {
            const raw = event.data.trim()
            if (!raw) return
            const eventName = event.event
            // 当前会话的AI消息
            const aiMessage = messages.value[messages.value.length - 1]

            if (eventName === 'done') {
                isAiTyping.value = false
                ctrl.abort()
                // 回复完成后滚动到底部
                scrollToBottom()
                // 进行情绪分析
                loadSessionEmotion(currentSession.value.sessionId)
                return
            }
            const payload = JSON.parse(raw)
            const ok = String(payload.code) === '200'
            if (ok && payload.data && payload.data.content) {
                aiMessage.content += payload.data.content
                // 流式输出过程中跟随滚动到底部
                scrollToBottom()
            } else if (!ok) {
                // 错误回复的显示
                handleError(payload.message || 'AI回复失败')
            }
        },
        onerror: (err) => {
            handleError(err || 'AI回复失败')
            throw err
        },
        onclose: () => {
            // 开始情绪分析
            loadSessionEmotion(currentSession.value.sessionId)
        }
    })

}

// 错误处理函数
const handleError = (error) => {
    // 当前会话的AI消息
    const aiMessage = messages.value[messages.value.length - 1]
    if (aiMessage) {
        aiMessage.content = 'AI回复失败，请重试'
    }
    isAiTyping.value = false
    ElMessage.error('AI回复失败，请重试')
}

const getSessionPage = () => {
    getSessionList({
        pageNum: 1,
        pageSize: 10
    }).then(res => {
        console.log(res)
        sessionList.value = res.records
    })
}

// 获取会话数据
const handleSessionClick = (session) => {
    console.log(session, 'session')
    // 点击会话时，获取会话详情
    getSessionDetail(session.id).then(res => {
        console.log(res)
        // 防止接口返回非数组导致渲染中断
        messages.value = Array.isArray(res) ? res : []
        // 加载历史消息后滚动到底部
        scrollToBottom()
    })
    loadSessionEmotion(session.id)
    // 更新当前会话对象数据
    const sessionData = {
        sessionId: "session_" + session.id,
        status: 'ACTIVE',
        sessionTitle: session.sessionTitle
    }
    currentSession.value = sessionData
}

const handleDeleteSession = (sessionId) => {
    deleteSession(sessionId).then(res => {
        ElMessage.success('删除成功')
        getSessionPage()
    })
}

// 简单的换行逻辑
const formatMessageContent = (content) => {
    return content.replace(/\n/g, '<br>')
}

// 将时间格式化为相对时间：刚刚/X分钟前/X小时前/X天前/X个月前/X年前
const formatTimeAgo = (time) => {
    if (!time) return ''
    const date = new Date(time)
    // 时间格式无法解析时原样返回
    if (isNaN(date.getTime())) return time
    const diff = Date.now() - date.getTime()
    const minute = 60 * 1000
    const hour = 60 * minute
    const day = 24 * hour
    const month = 30 * day
    const year = 365 * day
    if (diff < minute) return '刚刚'
    if (diff < hour) return `${Math.floor(diff / minute)}分钟前`
    if (diff < day) return `${Math.floor(diff / hour)}小时前`
    if (diff < month) return `${Math.floor(diff / day)}天前`
    if (diff < year) return `${Math.floor(diff / month)}个月前`
    return `${Math.floor(diff / year)}年前`
}

onMounted(() => {
    // 初始化时获取会话列表
    getSessionPage()
    // 初始化时创建一个新会话
    createNewFrontendSession()
})
</script>
<style scoped lang="scss">
// ===== 手账风：局部主题变量（项目暂无全局变量文件时使用）=====
$bg-page: #FFF8E7;
$bg-card: #FFFEF9;
$bg-input: #FFFEF9;
$primary: #E8927C;
$primary-dark: #D9826B;
$primary-darker: #C4705C;
$primary-light: rgba(232, 146, 124, 0.15);
$accent-pink: #F5C6C6;
$accent-green: #B8D8BE;
$accent-yellow: #F7E8A6;
$text-primary: #5C4A3A;
$text-regular: #6B5B4B;
$text-secondary: #8B7D6B;
$text-placeholder: #B8A898;
$border-color: #E8DCC8;
$border-light: #EFE6D5;
$divider: #E0D5C0;
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$shadow-button: 0 3px 0 $primary-darker, 0 4px 8px rgba(180, 140, 100, 0.2);
$radius-sm: 8px;
$radius-md: 12px;
$radius-lg: 16px;
$radius-xl: 24px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

// ===== 手账风：页面容器 =====
.consultation-container {
    position: relative;
    margin: 0 auto;
    max-width: 1200px;
    display: flex;
    gap: 20px;
    padding: 20px;
    // 固定整体高度，聊天区不随内容增多而变化
    height: calc(100vh - 130px);
    box-sizing: border-box;
    font-family: $font-rounded;
    z-index: 1;
}

// ===== 手账风：纸张纹理背景层 =====
.paper-bg {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: -1;
    background-color: $bg-page;
    background-image:
        linear-gradient(rgba(232, 156, 124, 0.06) 1px, transparent 1px),
        linear-gradient(90deg, rgba(232, 156, 124, 0.06) 1px, transparent 1px),
        radial-gradient(ellipse at 20% 80%, rgba(247, 232, 166, 0.3), transparent 50%),
        radial-gradient(ellipse at 80% 20%, rgba(245, 198, 198, 0.2), transparent 50%);
    background-size: 28px 28px, 28px 28px, 100% 100%, 100% 100%;
}

// ===== 手账风：通用便签卡片 mixin =====
%note-card {
    background: $bg-card;
    border-radius: $radius-lg;
    box-shadow: $shadow-card;
    border: 1px solid $border-light;
    position: relative;
    overflow: hidden;
}

%card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 14px;
    padding-bottom: 10px;
    border-bottom: 2px dashed $divider;
    .card-icon {
        font-size: 18px;
    }
    .card-title {
        font-family: $font-handwritten;
        font-size: 17px;
        font-weight: 600;
        color: $text-primary;
    }
}

// ===== 手账风：左侧栏 =====
.sidebar {
    width: 320px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    gap: 16px;
    // 内容超高时侧边栏可滚动，避免卡片被 flex 压缩导致内容被裁切
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: rgba(232, 146, 124, 0.3) transparent;
    &::-webkit-scrollbar {
        width: 6px;
    }
    &::-webkit-scrollbar-thumb {
        background: rgba(232, 146, 124, 0.25);
        border-radius: 3px;
    }
    // 高度由内容自然撑开，卡片不被压缩；右侧聊天区高度保持固定

    // ===== AI助手信息便签卡 =====
    .ai-card {
        @extend %note-card;
        flex-shrink: 0; // 情绪花园伸展时不被压缩
        padding: 20px 16px 16px;
        text-align: center;
        .washi-tape {
            position: absolute;
            top: -6px;
            left: 50%;
            transform: translateX(-50%) rotate(-2deg);
            width: 100px;
            height: 20px;
            background: rgba(232, 146, 124, 0.35);
            border-radius: 2px;
            box-shadow: 0 2px 4px rgba(180, 140, 100, 0.1);
        }
        .breathing-circle {
            width: 60px;
            height: 60px;
            background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 8px auto 12px;
            animation: breathing 4s ease-in-out infinite;
            box-shadow: 0 4px 16px rgba(232, 146, 124, 0.3);
        }
        .assistant-name {
            font-family: $font-handwritten;
            font-size: 18px;
            font-weight: 600;
            color: $primary;
            text-align: center;
            margin: 0 0 10px;
        }
        .online-status {
            display: flex;
            align-items: center;
            justify-content: center;
            color: #7BAE7B;
            font-size: 12px;
            font-weight: 600;
            .status-dot {
                width: 8px;
                height: 8px;
                background: #7BAE7B;
                border-radius: 50%;
                margin-right: 8px;
                animation: pulse 2s infinite;
                box-shadow: 0 0 8px rgba(123, 174, 123, 0.4);
            }
        }
    }

    // ===== 情绪花园便签卡 =====
    .emotion-card {
        @extend %note-card;
        flex-shrink: 0; // 内容多时（温暖建议/治愈行动/风险提示）不被压缩裁切
        padding: 16px;

        .card-header {
            @extend %card-header;
        }
        .emotion-info {
            margin: 0 auto 12px;
            width: 80px;
            height: 80px;
            border-radius: 50%;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            box-shadow: 0 4px 12px rgba(180, 140, 100, 0.1);
            border: 2px solid rgba(255, 255, 255, 0.8);
            background: linear-gradient(135deg, $primary 0%, $accent-pink 100%);
            color: #fff;
            .emotion-name {
                font-family: $font-handwritten;
                font-size: 15px;
                font-weight: 600;
                line-height: 1;
                margin-bottom: 2px;
            }
            .emotion-score {
                font-size: 14px;
                font-weight: 700;
                opacity: 0.9;
            }
        }
        .warm-tips {
            text-align: center;
            .emotion-status-text {
                margin-bottom: 12px;
                .status-label {
                    font-size: 13px;
                    color: $text-secondary;
                    margin-right: 8px;
                }
                .status-emotion {
                    font-family: $font-handwritten;
                    font-size: 16px;
                    font-weight: 600;
                    color: $primary;
                    padding: 3px 12px;
                    border-radius: $radius-xl;
                    display: inline-block;
                    background: $primary-light;
                }
            }
            .emotion-intensity {
                margin-bottom: 14px;
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 8px;
                .intensity-dots {
                    display: flex;
                    gap: 4px;
                    .dot {
                        width: 9px;
                        height: 9px;
                        border-radius: 50%;
                        background: $border-color;
                        transition: all 0.3s ease;
                        &.active {
                            background: linear-gradient(135deg, $primary, $accent-pink);
                            transform: scale(1.25);
                            box-shadow: 0 2px 6px rgba(232, 146, 124, 0.4);
                        }
                    }
                }
                .intensity-text {
                    font-size: 12px;
                    color: $text-secondary;
                    font-weight: 500;
                }
            }
            // ===== 手账风：温暖建议便签 =====
            .warm-suggestion {
                background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(255, 254, 249, 0.85));
                border-radius: $radius-md;
                padding: 12px;
                margin-bottom: 14px;
                display: flex;
                align-items: flex-start;
                gap: 10px;
                border: 1px dashed $border-color;
                box-shadow: 0 2px 8px rgba(180, 140, 100, 0.06);
                .suggestion-icon {
                    font-size: 20px;
                    flex-shrink: 0;
                    margin-top: 2px;
                }
                .suggestion-content {
                    text-align: left;
                    flex: 1;
                    .suggestion-title {
                        font-family: $font-handwritten;
                        font-size: 14px;
                        font-weight: 600;
                        color: $text-primary;
                        margin-bottom: 6px;
                    }
                    .suggestion-text {
                        font-size: 13px;
                        color: $text-regular;
                        line-height: 1.5;
                    }
                }
            }
            // ===== 手账风：治愈行动列表 =====
            .healing-actions {
                margin-bottom: 14px;
                .actions-title {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 6px;
                    font-family: $font-handwritten;
                    font-size: 14px;
                    font-weight: 600;
                    color: $text-primary;
                    margin-bottom: 12px;
                }
                .actions-list {
                    display: flex;
                    flex-direction: column;
                    gap: 8px;
                    .action-item {
                        background: rgba(247, 232, 166, 0.2);
                        border-radius: $radius-sm;
                        padding: 10px 12px;
                        display: flex;
                        align-items: center;
                        gap: 10px;
                        border: 1px solid rgba(247, 232, 166, 0.4);
                        .action-icon {
                            font-size: 14px;
                            flex-shrink: 0;
                        }
                        .action-text {
                            font-size: 12px;
                            color: $text-regular;
                            line-height: 1.4;
                            flex: 1;
                        }
                    }
                }
            }
            // ===== 手账风：风险提示便签 =====
            .risk-notice {
                background: linear-gradient(135deg, rgba(247, 232, 166, 0.4), rgba(245, 198, 198, 0.3));
                border-radius: $radius-md;
                padding: 14px;
                display: flex;
                align-items: flex-start;
                gap: 12px;
                border: 1px dashed rgba(232, 146, 124, 0.3);
                .notice-icon {
                    font-size: 20px;
                    flex-shrink: 0;
                    margin-top: 2px;
                }
                .notice-content {
                    flex: 1;
                    .notice-title {
                        font-family: $font-handwritten;
                        font-size: 14px;
                        font-weight: 600;
                        color: $primary-dark;
                        margin-bottom: 6px;
                    }
                    .notice-text {
                        font-size: 13px;
                        color: $text-regular;
                        line-height: 1.5;
                    }
                }
            }
        }
    }

    // ===== 会话列表便签卡 =====
    .session-card {
        @extend %note-card;
        padding: 16px;
        flex-shrink: 0; // 高度由内容决定，跟随上方卡片自然下移
        display: flex;
        flex-direction: column;
        .card-header {
            @extend %card-header;
        }
        .session-list {
            overflow-y: auto;
            max-height: 300px; // 列表过长时内部滚动
            scrollbar-width: thin;
            scrollbar-color: rgba(232, 146, 124, 0.3) transparent;
            &::-webkit-scrollbar {
                width: 4px;
            }
            &::-webkit-scrollbar-thumb {
                background: rgba(232, 146, 124, 0.25);
                border-radius: 2px;
            }
            .session-item {
                position: relative;
                padding: 12px;
                margin-bottom: 8px;
                border-radius: $radius-md;
                cursor: pointer;
                transition: all 0.3s ease;
                border: 2px dashed transparent;
                background: rgba(255, 254, 249, 0.6);
                &:hover {
                    background: $primary-light;
                    border-color: rgba(232, 146, 124, 0.25);
                }
                &.active {
                    background: $primary-light;
                    border-color: $primary;
                }
                .session-info {
                    flex: 1;
                    .session-title {
                        font-weight: 500;
                        font-size: 14px;
                        color: $text-primary;
                        margin-bottom: 4px;
                        white-space: nowrap;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        .session-meta {
                            display: flex;
                            align-items: center;
                            gap: 8px;
                            margin-bottom: 6px;
                            .session-time {
                                font-size: 12px;
                                color: $text-secondary;
                            }
                        }
                        .session-preview {
                            width: 200px;
                            font-size: 12px;
                            color: $text-secondary;
                            margin-bottom: 6px;
                            white-space: nowrap;
                            overflow: hidden;
                            text-overflow: ellipsis;
                        }
                        .session-stats {
                            display: flex;
                            align-items: center;
                            gap: 12px;
                            span {
                                font-size: 12px;
                                color: $text-secondary;
                                display: flex;
                                align-items: center;
                                gap: 4px;
                            }
                        }
                    }
                    .session-actions {
                        position: absolute;
                        top: 10px;
                        right: 12px;
                        // ===== 手账风：删除按钮穿透定制 =====
                        :deep(.el-button) {
                            color: $primary-dark;
                            &:hover {
                                color: $primary-darker;
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== 手账风：聊天主区域 =====
.chat-main {
    @extend %note-card;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    flex: 1;
    // 固定高度：不随聊天内容或左侧栏变化
    height: 100%;
    min-height: 0;
    min-width: 0;
    border-radius: $radius-xl;
    box-shadow: 0 8px 32px rgba(180, 140, 100, 0.1);

    // ===== 手账风：聊天头部 =====
    .chat-header {
        background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
        color: white;
        padding: 18px 24px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        position: relative;
        flex-shrink: 0;
        &::before {
            content: '';
            position: absolute;
            bottom: -1px;
            left: 0;
            width: 100%;
            height: 6px;
            background: rgba(255, 255, 255, 0.15);
        }
        .header-left {
            display: flex;
            align-items: center;
            .chat-avatar {
                width: 48px;
                height: 48px;
                background: rgba(255, 255, 255, 0.25);
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                margin-right: 16px;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
            }
            .chat-info {
                h2 {
                    font-family: $font-handwritten;
                    font-size: 20px;
                    font-weight: 700;
                    margin-bottom: 4px;
                }
                p {
                    font-size: 13px;
                    opacity: 0.9;
                }
            }
        }
        // ===== 手账风：新建会话按钮穿透定制 =====
        :deep(.el-button.is-circle) {
            background: rgba(255, 255, 255, 0.25);
            border: 2px solid rgba(255, 255, 255, 0.3);
            color: white;
            &:hover {
                background: rgba(255, 255, 255, 0.4);
                border-color: rgba(255, 255, 255, 0.5);
                transform: rotate(90deg);
                transition: transform 0.3s ease;
            }
        }
    }

    // ===== 手账风：聊天消息区域 =====
    .chat-messages {
        flex: 1;
        overflow-y: auto;
        padding: 24px;
        display: flex;
        flex-direction: column;
        gap: 16px;
        min-height: 0;
        scrollbar-width: thin;
        scrollbar-color: rgba(232, 146, 124, 0.3) transparent;
        &::-webkit-scrollbar {
            width: 6px;
        }
        &::-webkit-scrollbar-thumb {
            background: rgba(232, 146, 124, 0.25);
            border-radius: 3px;
        }

        .message-item {
            display: flex;
            align-items: flex-start;
            gap: 12px;
            animation: fadeInUp 0.4s ease-out;

            .message-avatar {
                width: 32px;
                height: 32px;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 14px;
                color: white;
                flex-shrink: 0;
            }

            // ===== 手账风：AI消息头像 =====
            &.ai-message {
                .message-avatar {
                    background: linear-gradient(135deg, $primary, $primary-dark);
                    box-shadow: 0 4px 12px rgba(232, 146, 124, 0.3);
                }
            }

            // ===== 手账风：用户消息头像 =====
            &.user-message {
                .message-avatar {
                    background: linear-gradient(135deg, $accent-green, #9BC9A1);
                    box-shadow: 0 4px 12px rgba(184, 216, 190, 0.4);
                }
            }

            .message-content {
                max-width: 70%;
                .message-bubble {
                    background: $bg-card;
                    border-radius: $radius-lg;
                    padding: 12px 16px;
                    position: relative;
                    border: 1px solid $border-light;
                    box-shadow: 0 2px 8px rgba(180, 140, 100, 0.06);
                    line-height: 1.6;

                    // ===== 手账风：用户消息气泡 =====
                    .user-message & {
                        background: $primary-light;
                        border-color: rgba(232, 146, 124, 0.2);
                    }

                    // ===== 手账风：打字动画 =====
                    .typing-indicator {
                        display: flex;
                        gap: 4px;
                        padding: 8px 0;
                        .typing-dot {
                            width: 8px;
                            height: 8px;
                            background: $primary;
                            border-radius: 50%;
                            animation: typing 1.5s ease-in-out infinite;
                            opacity: 0.5;
                            &:nth-child(2) {
                                animation-delay: 0.2s;
                            }
                            &:nth-child(3) {
                                animation-delay: 0.4s;
                            }
                        }
                    }

                    // ===== 手账风：错误消息 =====
                    .error-message {
                        background: rgba(245, 198, 198, 0.3);
                        border: 1px dashed $accent-pink;
                        border-radius: $radius-sm;
                        padding: 10px 14px;
                        color: $primary-darker;
                        font-weight: 500;
                        p {
                            margin: 0;
                        }
                    }
                }
                .message-time {
                    font-size: 12px;
                    color: $text-secondary;
                    margin-top: 4px;
                }
            }
        }
    }

    // ===== 手账风：消息输入区域 =====
    .chat-input {
        border-top: 2px dashed $divider;
        padding: 16px 24px;
        display: flex;
        gap: 12px;
        align-items: flex-end;
        background: rgba(255, 254, 249, 0.6);
        flex-shrink: 0;

        .input-container {
            flex: 1;
        }

        // ===== 手账风：输入框穿透定制 =====
        :deep(.message-input) {
            .el-textarea__inner {
                background: $bg-input;
                border: 2px dashed $border-color;
                border-radius: $radius-md;
                padding: 12px 16px;
                font-family: $font-rounded;
                font-size: 14px;
                color: $text-primary;
                resize: none;
                transition: all 0.3s ease;
                &::placeholder {
                    color: $text-placeholder;
                }
                &:focus {
                    border-color: $primary;
                    border-style: solid;
                    box-shadow: 0 0 0 3px rgba(232, 146, 124, 0.15);
                }
            }
        }

        .input-footer {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 12px;
            color: $text-secondary;
            font-weight: 500;
            margin-top: 6px;
            padding: 0 4px;
        }

        // ===== 手账风：发送按钮穿透定制 =====
        :deep(.send-btn) {
            height: 56px;
            width: 56px;
            border-radius: $radius-md;
            background: linear-gradient(135deg, $primary, $primary-dark) !important;
            border: none !important;
            box-shadow: $shadow-button;
            transition: all 0.3s ease;
            font-size: 20px;
            &:hover {
                transform: translateY(-2px);
                box-shadow: 0 5px 0 $primary-darker, 0 6px 12px rgba(180, 140, 100, 0.25);
            }
            &:active {
                transform: translateY(1px);
                box-shadow: 0 1px 0 $primary-darker, 0 2px 4px rgba(180, 140, 100, 0.15);
            }
            &.is-disabled {
                background: #ccc !important;
                box-shadow: 0 2px 0 #aaa, 0 2px 4px rgba(0, 0, 0, 0.1);
                transform: none;
            }
        }
    }
}

// ===== 手账风：动画关键帧 =====
@keyframes breathing {
    0%, 100% {
        transform: scale(1);
        box-shadow: 0 4px 16px rgba(232, 146, 124, 0.3);
    }
    50% {
        transform: scale(1.08);
        box-shadow: 0 6px 24px rgba(232, 146, 124, 0.4);
    }
}

@keyframes pulse {
    0%, 100% {
        opacity: 1;
        transform: scale(1);
    }
    50% {
        opacity: 0.6;
        transform: scale(1.3);
    }
}

@keyframes typing {
    0%, 60%, 100% {
        transform: translateY(0);
        opacity: 0.5;
    }
    30% {
        transform: translateY(-6px);
        opacity: 1;
    }
}

@keyframes fadeInUp {
    from {
        opacity: 0;
        transform: translateY(12px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

// ===== 手账风：响应式适配 =====
@media (max-width: 768px) {
    .consultation-container {
        flex-direction: column;
        padding: 12px;
        gap: 12px;
        // 移动端恢复自适应高度
        height: auto;
    }
    .sidebar {
        width: 100%;
        max-height: 50vh;
        overflow-y: auto; // 移动端限高时内部滚动
    }
    .chat-main {
        height: calc(100vh - 100px);
    }
}
</style>
