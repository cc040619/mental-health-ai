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
          <el-image :src="iconUrl" style="width: 25px;height:25px" alt="AI助手"/>
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
          <div class="emotion-name">{{ currentEmotion.primaryEmotion }}</div>
          <div class="emotion-score">{{ currentEmotion.emotionScore }}</div>
        </div>
        <div class="warm-tips">
          <div class="emotion-status-text">
            <span class="status-label">今天感觉</span>
            <span class="status-emotion">{{ currentEmotion.isNegative ? '需要关注' : '很不错' }}</span>
          </div>
          <div class="emotion-intensity">
                        <span class="intensity-dots">
                            <span v-for="dot in 3" :key="dot" class="dot"
                                  :class="{'active': getIntensityClass(currentEmotion.emotionScore) >= dot}"></span>
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
              <el-button size="small" class="handoff-btn" @click="requestHandoff">
                🫶 寻求专业帮助
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 手账风：会话列表便签卡 ===== -->
      <div class="session-card">
        <div class="card-header session-card-header">
          <span class="card-icon">🗂️</span>
          <span class="card-title">会话列表</span>
          <el-button text size="small" class="fav-entry-btn" @click="openFavorites">
            ⭐ 我的收藏
          </el-button>
        </div>
        <div class="session-list">
          <div v-for="session in sessionList" :key="session.id" @click="handleSessionClick(session)"
               class="session-item"
               :class="{active: currentSession && ('session_' + session.id) === currentSession.sessionId}">
            <div class="session-info">
              <div class="session-title">
                <span>{{ session.sessionTitle }}</span>
                <div class="session-tags">
                  <span class="scene-tag">{{ getSceneIcon(session.scene) }} {{ getSceneLabel(session.scene) }}</span>
                  <span class="rating-tag" v-if="session.rating">
                                        {{ '★'.repeat(session.rating) }}{{ '☆'.repeat(5 - session.rating) }}
                                    </span>
                  <span class="summary-tag" v-if="session.hasSummary">📋 已总结</span>
                </div>
                <div class="session-meta">
                  <span class="session-time">{{ session.startedAt }}</span>
                </div>
                <div class="session-preview">
                  {{ session.lastMessageContent }}
                </div>
                <div class="session-stats">
                                    <span>
                                        <el-icon>
                                            <ChatRound/>
                                        </el-icon>
                                        {{ session.messageCount || 0 }}
                                    </span>
                  <span>
                                        <el-icon>
                                            <Clock/>
                                        </el-icon>
                                        {{ formatTimeAgo(session.startedAt) }}
                                    </span>
                </div>
              </div>
              <div class="session-actions">
                <el-button text type="danger" size="mini" @click.stop="handleDeleteSession(session.id)">
                  <el-icon>
                    <DeleteFilled/>
                  </el-icon>
                </el-button>
                <el-button text size="mini" class="session-summary-btn" title="查看会话总结"
                           @click.stop="handleViewSummary(session)">
                  📋
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
            <el-image :src="iconUrl1" style="width: 30px;height: 30px"/>
          </div>
          <div class="chat-info">
            <h2>小聪AI助手</h2>
            <p>{{ getSceneIcon(currentSceneCode) }} {{ getSceneLabel(currentSceneCode) }} · 您的贴心AI心理健康助手</p>
          </div>
        </div>
        <div class="header-actions">
          <el-button class="header-action-btn" @click="endCurrentSession" title="结束会话并生成总结">
            🏁 结束会话
          </el-button>
          <el-button circle @click="createNewFrontendSession" title="新建会话">
            <el-icon>
              <Plus/>
            </el-icon>
          </el-button>
        </div>
      </div>
      <!-- 实时情绪徽标条 -->
      <div class="emotion-badge-bar">
        <div class="emotion-badge" :class="{warning: currentEmotion.riskLevel >= 2}">
          <span class="badge-emoji">{{ emotionBadgeEmoji }}</span>
          <span class="badge-label">{{ currentEmotion.primaryEmotion }}</span>
          <span class="badge-score">情绪值 {{ currentEmotion.emotionScore }}</span>
          <span class="badge-risk" v-if="currentEmotion.riskLevel >= 2">⚠️ 建议寻求专业帮助</span>
        </div>
        <el-button v-if="currentEmotion.riskLevel >= 2" size="small" class="handoff-btn" @click="requestHandoff">
          🫶 寻求专业帮助
        </el-button>
      </div>
      <!-- 聊天消息区域 -->
      <div class="chat-messages" ref="chatMessagesRef">
        <!-- 欢迎用语 -->
        <div class="message-item ai-message" v-if="messages.length === 0">
          <div class="message-avatar">
            <el-image :src="iconUrl" style="width: 18px;height: 18px"/>
          </div>
          <div class="message-content">
            <div class="message-bubble">
              <p>
                您好！我是小聪，您的AI心理健康助手。很高兴陪伴您，为您提供温暖的心理支持。请告诉我，今天您感觉怎么样？有什么想要分享的吗？</p>
            </div>
            <div class="message-time">刚刚</div>
          </div>
        </div>
        <!-- 消息列表 -->
        <div v-for="msg in messages" :key="msg.id" class="message-item"
             :class="msg.senderType === 1 ?  'user-message' : 'ai-message'">
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
              <MarkdownRenderer v-else-if="msg.senderType === 2 && !msg.isError" :content="msg.content"
                                :is-ai-message="true"/>
              <p v-else-if="msg.content" v-html="formatMessageContent(msg.content)"></p>
            </div>
            <div class="message-time">{{ msg.senderType === 2 && isAiTyping ? '正在输入中...' : msg.createdAt }}</div>
            <!-- AI 回复收藏按钮：仅已落库的消息可收藏，hover 气泡时显现 -->
            <div v-if="msg.senderType === 2 && !msg.isError && isPersistedMessage(msg)"
                 class="message-favorite"
                 :class="{favorited: msg.isFavorite === 1 || msg.isFavorite === true}"
                 :title="(msg.isFavorite === 1 || msg.isFavorite === true) ? '取消收藏' : '收藏这段回复'"
                 @click="toggleFavorite(msg)">
              {{ (msg.isFavorite === 1 || msg.isFavorite === true) ? '★' : '☆' }}
            </div>
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
              clearable/>
          <div class="input-footer">
            <span>按Enter发送，Shift+Enter换行</span>
            <span>{{ userMessage.length }}/500</span>
          </div>
        </div>
        <el-button :disabled="!userMessage.trim() || userMessage.length > 500" type="primary" class="send-btn"
                   @click="sendMessage">
          <el-icon>
            <Promotion/>
          </el-icon>
        </el-button>
      </div>
    </div>

    <!-- ===== 新建会话：场景选择弹窗（手账风便签） ===== -->
    <el-dialog v-model="sceneDialogVisible" width="560px" class="scene-dialog" :close-on-click-modal="false"
               title="🌈 开启一段新的陪伴">
      <div class="scene-dialog-body">
        <p class="scene-dialog-tip">先选一个今天想聊的话题吧，小聪会用更贴合的方式陪伴你～</p>
        <div class="scene-options">
          <div v-for="item in sceneOptions" :key="item.code"
               class="scene-option"
               :class="{selected: newSessionScene === item.code}"
               @click="newSessionScene = item.code">
            <div class="scene-option-icon">{{ item.icon }}</div>
            <div class="scene-option-name">{{ item.label }}</div>
            <div class="scene-option-desc">{{ item.desc }}</div>
          </div>
        </div>
        <el-input v-model="newSessionMessage" type="textarea" :rows="3" maxlength="500" show-word-limit
                  class="scene-first-input"
                  placeholder="也可以先写下你最想说的第一句话（选填）..."/>
      </div>
      <template #footer>
        <el-button @click="sceneDialogVisible = false">再想想</el-button>
        <el-button type="primary" class="scene-confirm-btn" :disabled="!newSessionScene" @click="confirmStartSession">
          🌱 开始倾诉
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 会话总结便签卡片 ===== -->
    <el-dialog v-model="summaryDialogVisible" width="520px" class="summary-dialog" title="📋 本次陪伴小结">
      <div v-if="summaryData && summaryData.status === 'READY'" class="summary-card">
        <div class="summary-washi"></div>
        <div class="summary-section">
          <div class="summary-section-title">📝 对话摘要</div>
          <p class="summary-text">{{ summaryData.summary }}</p>
        </div>
        <div class="summary-section" v-if="summaryData.coreIssues && summaryData.coreIssues.length">
          <div class="summary-section-title">🧩 我们聊到的</div>
          <ul class="summary-list">
            <li v-for="(issue, i) in summaryData.coreIssues" :key="'i' + i">📌 {{ issue }}</li>
          </ul>
        </div>
        <div class="summary-section" v-if="summaryData.suggestions && summaryData.suggestions.length">
          <div class="summary-section-title">💡 给你的小建议</div>
          <ul class="summary-list">
            <li v-for="(sug, i) in summaryData.suggestions" :key="'s' + i">✨ {{ sug }}</li>
          </ul>
        </div>
      </div>
      <div v-else class="summary-loading">
        <div class="summary-loading-emoji">🌸</div>
        <p>小聪正在认真整理你们的对话...</p>
        <p class="summary-loading-sub">小结马上就好，稍等片刻哦</p>
      </div>
      <template #footer>
        <el-button v-if="summaryData && summaryData.status === 'READY'" type="primary" class="scene-confirm-btn"
                   @click="openRatingDialog">⭐ 给这次陪伴打分
        </el-button>
        <el-button @click="summaryDialogVisible = false">收起</el-button>
      </template>
    </el-dialog>

    <!-- ===== 评价反馈弹窗 ===== -->
    <el-dialog v-model="ratingDialogVisible" width="420px" class="rating-dialog" title="⭐ 陪伴反馈">
      <div class="rating-body">
        <p class="rating-tip">这次的陪伴对你有帮助吗？你的反馈会让小聪变得更温暖～</p>
        <div class="rating-stars">
          <el-rate v-model="ratingForm.rating" :max="5" size="large"/>
        </div>
        <el-input v-model="ratingForm.feedback" type="textarea" :rows="4" maxlength="500" show-word-limit
                  placeholder="还想对小聪说点什么？（选填）" class="rating-feedback"/>
      </div>
      <template #footer>
        <el-button @click="ratingDialogVisible = false">取消</el-button>
        <el-button type="primary" class="scene-confirm-btn" :disabled="!ratingForm.rating" @click="submitFeedback">
          提交反馈
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 转人工引导便签 ===== -->
    <el-dialog v-model="handoffDialogVisible" width="460px" class="handoff-dialog" title="🫶 寻求专业帮助">
      <div class="handoff-card" v-if="handoffData">
        <div class="handoff-msg">{{ handoffData.message }}</div>
        <div class="handoff-hotline">
          <span class="hotline-icon">📞</span>
          <span>{{ handoffData.hotline }}</span>
        </div>
        <div class="handoff-suggestion">💌 {{ handoffData.suggestion }}</div>
        <div class="handoff-note">🌱 人工咨询师功能正在紧张筹备中，敬请期待</div>
      </div>
      <template #footer>
        <el-button type="primary" class="scene-confirm-btn" @click="handoffDialogVisible = false">我知道了</el-button>
      </template>
    </el-dialog>

    <!-- ===== 我的收藏抽屉 ===== -->
    <el-drawer v-model="favoritesVisible" title="⭐ 我的收藏" size="420px" class="favorites-drawer">
      <div class="favorites-body">
        <div v-if="favoriteList.length === 0 && !favoritesLoading" class="favorites-empty">
          <div class="favorites-empty-emoji">🌷</div>
          <p>还没有收藏的内容</p>
          <p class="favorites-empty-sub">在 AI 回复上 hover 点击 ☆ 就能收藏暖心的话啦</p>
        </div>
        <div v-for="fav in favoriteList" :key="fav.id" class="favorite-item">
          <div class="favorite-item-session">📁 {{ fav.sessionTitle || '历史会话' }}</div>
          <div class="favorite-item-content">{{ fav.content }}</div>
          <div class="favorite-item-footer">
            <span class="favorite-item-time">🕒 {{ formatTimeAgo(fav.favoritedAt) }}</span>
            <span class="favorite-item-actions">
                            <el-button text size="small" @click="jumpToSession(fav.sessionId)">回到对话</el-button>
                            <el-button text size="small" type="warning"
                                       @click="removeFavorite(fav)">取消收藏</el-button>
                        </span>
          </div>
        </div>
        <div v-if="favoriteList.length < favoriteTotal" class="favorites-more">
          <el-button text @click="loadMoreFavorites" :loading="favoritesLoading">加载更多...</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>
<script setup>
import {ref, computed, onMounted, onUnmounted, nextTick} from 'vue'
import {
  startSession, getSessionList, deleteSession, getSessionDetail, getSessionEmotion,
  triggerSessionSummary, getSessionSummary, toggleMessageFavorite, getFavoriteList,
  submitSessionFeedback, requestHumanHandoff
} from '@/api/frontend'
import {ElMessage, ElMessageBox} from 'element-plus'
import {ChatRound, DeleteFilled} from '@element-plus/icons-vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import {fetchEventSource} from '@microsoft/fetch-event-source'

const iconUrl = new URL('@/assets/images/robot-fill.png', import.meta.url).href
const iconUrl1 = new URL('@/assets/images/like.png', import.meta.url).href
const iconUrl2 = new URL('@/assets/images/users.png', import.meta.url).href

// ===== 会话场景选项（与后端 SessionScene 枚举对应）=====
const sceneOptions = [
  {code: 'EMOTION', label: '情绪疏导', icon: '🌈', desc: '难过、焦虑、烦躁，想被倾听和理解'},
  {code: 'RELATIONSHIP', label: '人际关系', icon: '🤝', desc: '与朋友、家人、同学相处的烦恼'},
  {code: 'ACADEMIC', label: '学业压力', icon: '📚', desc: '考试、论文、学习节奏带来的压力'},
  {code: 'SLEEP', label: '睡眠问题', icon: '🌙', desc: '睡不着、睡不好，想调整作息'},
  {code: 'OTHER', label: '其他', icon: '💬', desc: '其他想聊的话题'}
]
const getSceneLabel = (code) => sceneOptions.find(s => s.code === code)?.label || '其他'
const getSceneIcon = (code) => sceneOptions.find(s => s.code === code)?.icon || '💬'

// 定义一个当前会话对象
const currentSession = ref(null)
const sessionList = ref([])
// 当前会话场景码（用于头部展示）
const currentSceneCode = ref('OTHER')

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

// 从当前会话中解析数据库会话ID，临时会话返回 null
const getDbSessionId = () => {
  const sid = currentSession.value && currentSession.value.sessionId
  if (!sid || typeof sid !== 'string' || !sid.startsWith('session_')) return null
  const id = Number(sid.replace('session_', ''))
  return Number.isFinite(id) && id > 0 ? id : null
}
// 消息是否已落库（流式中的AI消息为临时字符串ID，不可收藏）
const isPersistedMessage = (msg) => typeof msg.id === 'number'

// ===== 新建会话（场景选择弹窗）=====
const sceneDialogVisible = ref(false)
const newSessionScene = ref('')
const newSessionMessage = ref('')

// 点击「新建会话」：打开场景选择弹窗
const createNewFrontendSession = () => {
  newSessionScene.value = ''
  newSessionMessage.value = ''
  sceneDialogVisible.value = true
}

// 情绪花园重置为默认状态
const resetEmotion = () => {
  currentEmotion.value = {
    primaryEmotion: '中性',
    emotionScore: 50,
    isNegative: false,
    riskLevel: 0,
    suggestion: '情绪状态平稳',
    improvementSuggestions: [],
    riskDescription: ''
  }
}

// 初始化一个空白临时会话（页面挂载时使用）
const initTempSession = () => {
  stopEmotionPolling()
  currentSession.value = {
    sessionId: `temp_${Date.now()}`,
    status: 'TEMP',
    sessionTitle: '新对话',
    scene: 'OTHER'
  }
  messages.value = []
  userMessage.value = ''
  currentSceneCode.value = 'OTHER'
  resetEmotion()
}

// 确认场景选择：创建带场景的临时会话，可选附带第一句消息
const confirmStartSession = () => {
  if (!newSessionScene.value) {
    ElMessage.warning('请先选择一个今天想聊的话题哦')
    return
  }
  sceneDialogVisible.value = false
  stopEmotionPolling()
  currentSession.value = {
    sessionId: `temp_${Date.now()}`,
    status: 'TEMP',
    sessionTitle: '新对话',
    scene: newSessionScene.value
  }
  messages.value = []
  currentSceneCode.value = newSessionScene.value
  resetEmotion()
  const firstMsg = newSessionMessage.value.trim()
  if (firstMsg) {
    userMessage.value = firstMsg
    sendMessage()
  }
}

// 情绪花园
const currentEmotion = ref({
  primaryEmotion: '中性',
  emotionScore: 50,
  isNegative: false,
  riskLevel: 0,
  suggestion: '情绪状态平稳',
  improvementSuggestions: [],
  riskDescription: ''
})

// 对话区顶部实时情绪徽标 emoji
const emotionBadgeEmoji = computed(() => {
  if (currentEmotion.value.riskLevel >= 2) return '⚠️'
  if (currentEmotion.value.isNegative) return '🌧️'
  return '🌤️'
})

// 将情绪分析结果合并到当前情绪状态（缺失字段保留默认值）
const applyEmotion = (res) => {
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
}

// 立即拉取一次情绪分析（切换会话/回复完成时使用）
const loadSessionEmotion = (sessionId) => {
  // 确保sessionID格式正确
  const id = sessionId.toString().startsWith('session_') ? sessionId : `session_${sessionId}`
  if (id.startsWith('temp_')) return
  getSessionEmotion(id).then(res => {
    if (res) applyEmotion(res)
  }).catch(() => { /* 情绪查询失败静默，不影响对话 */
  })
}

// ===== 情绪节流轮询：递归 setTimeout 链 + 在途标志，避免并发堆积 =====
const EMOTION_POLL_INTERVAL = 9000
// 对话结束后轮询延续窗口（等待 @Async 情绪分析落库）
const EMOTION_POLL_TAIL = 20000
let emotionPollTimer = null
let emotionPollActive = false
let emotionInFlight = false
let emotionPollUntil = 0

const fetchEmotionOnce = () => {
  const sid = currentSession.value && currentSession.value.sessionId
  if (!sid || sid.startsWith('temp_') || emotionInFlight) return Promise.resolve()
  emotionInFlight = true
  return getSessionEmotion(sid).then(res => {
    if (res) applyEmotion(res)
  }).catch(() => { /* 轮询失败静默，不影响对话 */
  }).finally(() => {
    emotionInFlight = false
  })
}

const scheduleEmotionPoll = () => {
  clearTimeout(emotionPollTimer)
  emotionPollTimer = setTimeout(async () => {
    if (!emotionPollActive) return
    await fetchEmotionOnce()
    // 对话进行中持续轮询；对话结束后延续一小段时间等异步情绪分析结果，然后停止
    if (emotionPollActive && (isAiTyping.value || Date.now() < emotionPollUntil)) {
      scheduleEmotionPoll()
    } else {
      emotionPollActive = false
    }
  }, EMOTION_POLL_INTERVAL)
}

// 对话开始时启动轮询
const startEmotionPolling = () => {
  const sid = currentSession.value && currentSession.value.sessionId
  if (!sid || sid.startsWith('temp_')) return
  emotionPollUntil = Date.now() + EMOTION_POLL_TAIL
  if (emotionPollActive) return
  emotionPollActive = true
  scheduleEmotionPoll()
}

// 回复结束后延长轮询窗口（异步情绪分析可能在 done 之后数秒才完成）
const extendEmotionPolling = () => {
  emotionPollUntil = Date.now() + EMOTION_POLL_TAIL
  if (!emotionPollActive) {
    emotionPollActive = true
    scheduleEmotionPoll()
  }
}

const stopEmotionPolling = () => {
  emotionPollActive = false
  if (emotionPollTimer) {
    clearTimeout(emotionPollTimer)
    emotionPollTimer = null
  }
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
  // 构建会话参数（携带场景码，后端按场景选择系统提示词）
  const sessionParams = {
    initialMessage: message,
    scene: currentSession.value.scene || 'OTHER'
  }
  if (currentSession.value.sessionTitle === '新对话') {
    sessionParams.sessionTitle = `小聪AI助手 - ${new Date().toLocaleString()}`
  } else {
    // 如果历史会话记录
    sessionParams.sessionTitle = currentSession.value.sessionTitle
  }
  // 调用后端接口创建新会话
  startSession(sessionParams).then(res => {
    // 将后端返回的数据转为前端会话格式
    const sessionData = {
      sessionId: res.sessionId,
      status: res.status,
      sessionTitle: sessionParams.sessionTitle,
      scene: sessionParams.scene
    }
    // 如果当前是临时会话，更新数据
    if (currentSession.value && currentSession.value.status === 'TEMP') {
      // 更新为正式会话
      Object.assign(currentSession.value, sessionData)
    } else {
      // 否则，创建一个新的会话
      currentSession.value = sessionData
    }
    currentSceneCode.value = sessionParams.scene
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
  // 对话进行中开启情绪节流轮询
  startEmotionPolling()

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
    onopen: async (response) => {
      // 登录态失效（token过期/无效）：中止请求（中止后库不再自动重试），清登录态并跳转登录页
      if (response.status === 401 || response.status === 403) {
        ctrl.abort()
        isAiTyping.value = false
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        ElMessage.error('登录状态已过期，请重新登录')
        window.location.href = '/auth/login?redirect=' + encodeURIComponent(window.location.pathname + window.location.search)
        return
      }
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
        // 立即拉取一次情绪分析，并延长轮询窗口等待异步分析结果
        fetchEmotionOnce()
        extendEmotionPolling()
        // 刷新消息列表，拿到已落库消息的真实ID（用于收藏）
        refreshMessages()
        // 用户消息命中转人工触发词时，AI 回复后自动弹出引导卡片
        if (containsHandoffTrigger(userMessage)) {
          requestHandoff()
        }
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
      // 拉取情绪分析（在途标志保证不并发）
      fetchEmotionOnce()
      extendEmotionPolling()
    }
  })

}

// 错误处理函数
const handleError = (error) => {
  // 当前会话的AI消息
  const aiMessage = messages.value[messages.value.length - 1]
  if (aiMessage) {
    aiMessage.content = 'AI回复失败，请重试'
    aiMessage.isError = true
  }
  isAiTyping.value = false
  ElMessage.error('AI回复失败，请重试')
}

// 重新拉取当前会话消息（拿到真实消息ID与收藏状态）
const refreshMessages = () => {
  const dbId = getDbSessionId()
  if (!dbId) return
  getSessionDetail(dbId).then(res => {
    if (Array.isArray(res)) {
      messages.value = res
      scrollToBottom()
    }
  }).catch(() => { /* 刷新失败静默，不影响对话 */
  })
}

const getSessionPage = () => {
  getSessionList({
    pageNum: 1,
    pageSize: 10
  }).then(res => {
    sessionList.value = res.records
  })
}

// 获取会话数据
const handleSessionClick = (session) => {
  // 切换会话时停止轮询
  stopEmotionPolling()
  // 点击会话时，获取会话详情
  getSessionDetail(session.id).then(res => {
    // 防止接口返回非数组导致渲染中断
    messages.value = Array.isArray(res) ? res : []
    // 加载历史消息后滚动到底部
    scrollToBottom()
  })
  loadSessionEmotion(session.id)
  // 更新当前会话对象数据
  currentSession.value = {
    sessionId: "session_" + session.id,
    status: 'ACTIVE',
    sessionTitle: session.sessionTitle,
    scene: session.scene || 'OTHER'
  }
  currentSceneCode.value = session.scene || 'OTHER'
}

const handleDeleteSession = (sessionId) => {
  deleteSession(sessionId).then(res => {
    ElMessage.success('删除成功')
    getSessionPage()
  })
}

// ===== 消息收藏 =====
const toggleFavorite = (msg) => {
  if (!isPersistedMessage(msg)) return
  toggleMessageFavorite(msg.id).then(res => {
    msg.isFavorite = res.isFavorite ? 1 : 0
    ElMessage.success(res.isFavorite ? '已收藏这段回复 ⭐' : '已取消收藏')
  })
}

// ===== 我的收藏抽屉 =====
const favoritesVisible = ref(false)
const favoriteList = ref([])
const favoriteTotal = ref(0)
const favoritesLoading = ref(false)
const favPage = ref(1)
const favSize = 10

const openFavorites = () => {
  favoritesVisible.value = true
  favoriteList.value = []
  favoriteTotal.value = 0
  favPage.value = 1
  loadFavorites()
}

const loadFavorites = () => {
  favoritesLoading.value = true
  getFavoriteList({pageNum: favPage.value, pageSize: favSize}).then(res => {
    const records = res.records || []
    favoriteList.value = favPage.value === 1 ? records : [...favoriteList.value, ...records]
    favoriteTotal.value = res.total || 0
  }).finally(() => {
    favoritesLoading.value = false
  })
}

const loadMoreFavorites = () => {
  favPage.value += 1
  loadFavorites()
}

const removeFavorite = (fav) => {
  toggleMessageFavorite(fav.id).then(res => {
    favoriteList.value = favoriteList.value.filter(f => f.id !== fav.id)
    favoriteTotal.value = Math.max(0, favoriteTotal.value - 1)
    // 同步更新当前对话中该消息的收藏状态
    const msg = messages.value.find(m => m.id === fav.id)
    if (msg) msg.isFavorite = res.isFavorite ? 1 : 0
    ElMessage.success('已取消收藏')
  })
}

const jumpToSession = (sessionId) => {
  const session = sessionList.value.find(s => s.id === sessionId)
  favoritesVisible.value = false
  if (session) {
    handleSessionClick(session)
  } else {
    ElMessage.info('该会话可能已被删除')
  }
}

// ===== 会话总结 =====
const summaryDialogVisible = ref(false)
const summaryData = ref(null)
const summarySessionId = ref(null)
let summaryPollTimer = null

// 触发总结并打开小结弹窗（已有总结直接展示，否则轮询等待异步生成）
const openSummaryDialog = (dbId) => {
  summarySessionId.value = dbId
  summaryData.value = {status: 'GENERATING'}
  summaryDialogVisible.value = true
  triggerSessionSummary(dbId).then(res => {
    if (res) {
      summaryData.value = res
      if (res.status !== 'READY') {
        pollSummaryReady(dbId)
      }
    } else {
      pollSummaryReady(dbId)
    }
  }).catch(() => {
    pollSummaryReady(dbId)
  })
}

// 轮询总结结果（约每3秒一次，最多12次）
const pollSummaryReady = (dbId, attempt = 0) => {
  if (summaryPollTimer) clearTimeout(summaryPollTimer)
  if (attempt >= 12) return
  summaryPollTimer = setTimeout(() => {
    if (!summaryDialogVisible.value || summarySessionId.value !== dbId) return
    getSessionSummary(dbId).then(res => {
      if (res && res.status === 'READY') {
        summaryData.value = res
      } else {
        pollSummaryReady(dbId, attempt + 1)
      }
    }).catch(() => {
      pollSummaryReady(dbId, attempt + 1)
    })
  }, 3000)
}

// 「结束会话」：确认后触发异步总结
const endCurrentSession = () => {
  const dbId = getDbSessionId()
  if (!dbId || messages.value.length === 0) {
    ElMessage.info('先和小聪聊几句，再来生成小结吧～')
    return
  }
  ElMessageBox.confirm('结束后小聪会为你整理本次对话小结，之后也可以随时回来查看。确定结束本次会话吗？', '🏁 结束会话', {
    confirmButtonText: '生成小结',
    cancelButtonText: '继续聊聊',
    type: 'warning'
  }).then(() => {
    stopEmotionPolling()
    openSummaryDialog(dbId)
    getSessionPage()
  }).catch(() => { /* 用户取消 */
  })
}

// 从会话列表查看历史总结
const handleViewSummary = (session) => {
  getSessionSummary(session.id).then(res => {
    if (res && res.status === 'READY') {
      summarySessionId.value = session.id
      summaryData.value = res
      summaryDialogVisible.value = true
    } else {
      // 无总结或生成中：触发生成并展示等待态
      openSummaryDialog(session.id)
    }
  }).catch(() => {
    ElMessage.info('小结还在准备中，可以稍后再看哦')
  })
}

// ===== 评价反馈 =====
const ratingDialogVisible = ref(false)
const ratingForm = ref({rating: 0, feedback: ''})

const openRatingDialog = () => {
  summaryDialogVisible.value = false
  ratingForm.value = {rating: 0, feedback: ''}
  ratingDialogVisible.value = true
}

const submitFeedback = () => {
  if (!ratingForm.value.rating) {
    ElMessage.warning('请先选择星级评分')
    return
  }
  submitSessionFeedback(summarySessionId.value, {
    rating: ratingForm.value.rating,
    feedback: ratingForm.value.feedback || null
  }).then(() => {
    ElMessage.success('感谢你的反馈，小聪会继续努力陪伴你～')
    ratingDialogVisible.value = false
    getSessionPage()
  })
}

// ===== 转人工引导 =====
const handoffDialogVisible = ref(false)
const handoffData = ref(null)
const HANDOFF_TRIGGERS = ['人工', '真人', '咨询师', '预约']

const containsHandoffTrigger = (text) => {
  if (!text) return false
  return HANDOFF_TRIGGERS.some(word => text.includes(word))
}

const requestHandoff = () => {
  const dbId = getDbSessionId()
  if (!dbId) {
    ElMessage.info('先和小聪聊聊你的情况吧，小聪一直都在～')
    return
  }
  requestHumanHandoff(dbId).then(res => {
    handoffData.value = res
    handoffDialogVisible.value = true
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
  // 初始化时创建一个新的临时会话
  initTempSession()
})

// 离开页面清理所有定时器
onUnmounted(() => {
  stopEmotionPolling()
  if (summaryPollTimer) {
    clearTimeout(summaryPollTimer)
    summaryPollTimer = null
  }
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
  background-image: linear-gradient(rgba(232, 156, 124, 0.06) 1px, transparent 1px),
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
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 2px;
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

// ===== 手账风：新增功能样式（徽标条/收藏/标签/按钮）=====
// 通用：寻求专业帮助按钮
.handoff-btn {
  background: linear-gradient(135deg, $primary, $primary-dark) !important;
  border: none !important;
  color: #fff !important;
  border-radius: $radius-xl;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(232, 146, 124, 0.3);

  &:hover {
    opacity: 0.92;
    color: #fff !important;
  }
}

// 会话列表：卡片头部收藏入口
.session-card {
  .session-card-header {
    .fav-entry-btn {
      margin-left: auto;
      color: $primary-dark;
      font-size: 12px;
      font-weight: 600;
      padding: 2px 6px;

      &:hover {
        color: $primary-darker;
        background: $primary-light;
      }
    }
  }

  .session-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin: 4px 0 6px;
    white-space: normal;

    .scene-tag {
      font-size: 11px;
      color: $primary-darker;
      background: rgba(232, 146, 124, 0.12);
      border-radius: 10px;
      padding: 1px 8px;
    }

    .rating-tag {
      font-size: 11px;
      color: #E8A33D;
      letter-spacing: 1px;
      line-height: 1.6;
    }

    .summary-tag {
      font-size: 11px;
      color: #6E9E72;
      background: rgba(184, 216, 190, 0.3);
      border-radius: 10px;
      padding: 1px 8px;
    }
  }

  .session-actions {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;

    .session-summary-btn {
      font-size: 14px;
      padding: 4px;
      margin-left: -2px;
    }
  }
}

// 情绪花园：风险提示内的求助按钮
.risk-notice {
  .notice-content {
    .handoff-btn {
      margin-top: 10px;
    }
  }
}

// 聊天头部：操作按钮组
.chat-main {
  .chat-header {
    .header-actions {
      display: flex;
      align-items: center;
      gap: 10px;

      .header-action-btn {
        background: rgba(255, 255, 255, 0.25);
        border: 2px solid rgba(255, 255, 255, 0.35);
        color: #fff;
        border-radius: $radius-xl;
        font-weight: 600;

        &:hover {
          background: rgba(255, 255, 255, 0.42);
          color: #fff;
        }
      }
    }
  }

  // 实时情绪徽标条
  .emotion-badge-bar {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 24px;
    background: rgba(255, 254, 249, 0.85);
    border-bottom: 2px dashed $divider;

    .emotion-badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      background: $primary-light;
      border: 1px solid rgba(232, 146, 124, 0.25);
      border-radius: $radius-xl;
      padding: 4px 14px;
      font-size: 13px;
      color: $text-primary;
      transition: all 0.3s ease;

      .badge-emoji {
        font-size: 15px;
      }

      .badge-label {
        font-family: $font-handwritten;
        font-weight: 600;
        font-size: 15px;
        color: $primary-dark;
      }

      .badge-score {
        color: $text-secondary;
        font-size: 12px;
      }

      .badge-risk {
        color: #C0563F;
        font-size: 12px;
        font-weight: 600;
      }

      &.warning {
        background: rgba(245, 198, 198, 0.4);
        border-color: rgba(196, 112, 92, 0.45);
      }
    }

    .handoff-btn {
      margin-left: auto;
    }
  }

  // AI 消息收藏贴纸
  .chat-messages {
    .message-item {
      .message-content {
        position: relative;

        .message-favorite {
          position: absolute;
          top: -10px;
          right: 6px;
          width: 26px;
          height: 26px;
          line-height: 24px;
          text-align: center;
          font-size: 15px;
          color: $text-placeholder;
          background: $bg-card;
          border: 1px solid $border-color;
          border-radius: 50%;
          box-shadow: 0 2px 6px rgba(180, 140, 100, 0.15);
          cursor: pointer;
          opacity: 0;
          transition: all 0.2s ease;
          user-select: none;

          &:hover {
            transform: scale(1.18);
            color: #E8A33D;
            border-color: #E8A33D;
          }

          &.favorited {
            opacity: 1;
            color: #E8A33D;
            border-color: rgba(232, 163, 61, 0.5);
            background: #FFF7E0;
          }
        }
      }

      &:hover .message-content .message-favorite {
        opacity: 0.85;
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

<!-- ===== 手账风：弹窗/抽屉样式（el-dialog/el-drawer teleport 到 body，需非 scoped） ===== -->
<style lang="scss">
$dlg-primary: #E8927C;
$dlg-primary-dark: #D9826B;
$dlg-primary-darker: #C4705C;
$dlg-bg: #FFFEF9;
$dlg-bg-page: #FFF8E7;
$dlg-text: #5C4A3A;
$dlg-text-regular: #6B5B4B;
$dlg-text-secondary: #8B7D6B;
$dlg-border: #E8DCC8;
$dlg-divider: #E0D5C0;

// 通用确认按钮
.scene-confirm-btn {
  background: linear-gradient(135deg, $dlg-primary, $dlg-primary-dark) !important;
  border: none !important;
  border-radius: 20px !important;
  color: #fff !important;
  font-weight: 600;
  padding: 10px 22px !important;
  box-shadow: 0 3px 0 $dlg-primary-darker;

  &:hover {
    opacity: 0.92;
  }
}

// ===== 弹窗通用手账风外壳 =====
.scene-dialog,
.summary-dialog,
.rating-dialog,
.handoff-dialog {
  border-radius: 18px;
  overflow: hidden;

  .el-dialog__header {
    background: linear-gradient(135deg, $dlg-primary, $dlg-primary-dark);
    margin: 0;
    padding: 16px 22px;
  }

  .el-dialog__title {
    color: #fff;
    font-weight: 700;
    font-size: 16px;
  }

  .el-dialog__headerbtn .el-dialog__close {
    color: #fff;
    font-size: 18px;
  }

  .el-dialog__body {
    background: $dlg-bg;
    padding: 22px 24px;
    color: $dlg-text;
  }

  .el-dialog__footer {
    background: $dlg-bg;
    border-top: 2px dashed $dlg-divider;
    padding: 12px 22px;
  }
}

// ===== 场景选择弹窗 =====
.scene-dialog {
  .scene-dialog-tip {
    text-align: center;
    color: $dlg-text-secondary;
    font-size: 13px;
    margin: 0 0 16px;
  }

  .scene-options {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
    margin-bottom: 16px;

    .scene-option {
      border: 2px dashed $dlg-border;
      border-radius: 12px;
      padding: 12px 10px;
      text-align: center;
      cursor: pointer;
      background: rgba(255, 255, 255, 0.6);
      transition: all 0.25s ease;

      &:hover {
        border-color: rgba(232, 146, 124, 0.5);
        background: rgba(232, 146, 124, 0.08);
      }

      &.selected {
        border: 2px solid $dlg-primary;
        background: rgba(232, 146, 124, 0.12);
        box-shadow: 0 3px 10px rgba(232, 146, 124, 0.2);
      }

      .scene-option-icon {
        font-size: 24px;
        margin-bottom: 4px;
      }

      .scene-option-name {
        font-weight: 700;
        color: $dlg-text;
        font-size: 14px;
        margin-bottom: 4px;
      }

      .scene-option-desc {
        font-size: 11px;
        color: $dlg-text-secondary;
        line-height: 1.4;
      }
    }
  }

  .scene-first-input {
    .el-textarea__inner {
      border-radius: 12px;
      border: 2px dashed $dlg-border;
      background: rgba(255, 255, 255, 0.7);
    }
  }
}

// ===== 会话总结便签 =====
.summary-dialog {
  .summary-card {
    position: relative;
    background: linear-gradient(180deg, #FFFBF0, #FFFEF9);
    border: 1px solid $dlg-border;
    border-radius: 14px;
    padding: 22px 20px 16px;

    .summary-washi {
      position: absolute;
      top: -8px;
      left: 50%;
      transform: translateX(-50%) rotate(-2deg);
      width: 110px;
      height: 20px;
      background: rgba(232, 146, 124, 0.35);
      border-radius: 2px;
    }

    .summary-section {
      margin-bottom: 16px;

      &:last-child {
        margin-bottom: 0;
      }

      .summary-section-title {
        font-weight: 700;
        color: $dlg-primary-dark;
        font-size: 14px;
        margin-bottom: 8px;
      }

      .summary-text {
        margin: 0;
        font-size: 13px;
        color: $dlg-text-regular;
        line-height: 1.7;
        background: rgba(247, 232, 166, 0.18);
        border-radius: 10px;
        padding: 10px 12px;
      }

      .summary-list {
        margin: 0;
        padding: 0;
        list-style: none;

        li {
          font-size: 13px;
          color: $dlg-text-regular;
          line-height: 1.6;
          padding: 6px 10px;
          margin-bottom: 6px;
          background: rgba(255, 255, 255, 0.7);
          border: 1px dashed rgba(232, 220, 200, 0.8);
          border-radius: 10px;
        }
      }
    }
  }

  .summary-loading {
    text-align: center;
    padding: 30px 0 20px;
    color: $dlg-text-secondary;

    .summary-loading-emoji {
      font-size: 40px;
      animation: summaryBounce 1.6s ease-in-out infinite;
      margin-bottom: 12px;
    }

    p {
      margin: 6px 0;
      font-size: 14px;
    }

    .summary-loading-sub {
      font-size: 12px;
      color: #B8A898;
    }
  }
}

@keyframes summaryBounce {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-8px) rotate(8deg);
  }
}

// ===== 评价反馈弹窗 =====
.rating-dialog {
  .rating-body {
    text-align: center;

    .rating-tip {
      color: $dlg-text-secondary;
      font-size: 13px;
      margin: 0 0 18px;
      line-height: 1.6;
    }

    .rating-stars {
      margin-bottom: 18px;

      .el-rate__icon {
        font-size: 36px;
        margin-right: 8px;
      }
    }

    .rating-feedback {
      text-align: left;

      .el-textarea__inner {
        border-radius: 12px;
        border: 2px dashed $dlg-border;
        background: rgba(255, 255, 255, 0.7);
      }
    }
  }
}

// ===== 转人工引导便签 =====
.handoff-dialog {
  .handoff-card {
    background: linear-gradient(180deg, #FFFBF0, #FFFEF9);
    border: 1px solid $dlg-border;
    border-radius: 14px;
    padding: 20px;

    .handoff-msg {
      font-size: 14px;
      color: $dlg-text-regular;
      line-height: 1.8;
      margin-bottom: 14px;
    }

    .handoff-hotline {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      background: rgba(232, 146, 124, 0.12);
      border: 2px solid rgba(232, 146, 124, 0.35);
      border-radius: 12px;
      padding: 12px;
      font-size: 16px;
      font-weight: 700;
      color: $dlg-primary-darker;
      margin-bottom: 14px;

      .hotline-icon {
        font-size: 20px;
      }
    }

    .handoff-suggestion {
      font-size: 13px;
      color: $dlg-text-regular;
      line-height: 1.7;
      margin-bottom: 12px;
    }

    .handoff-note {
      font-size: 12px;
      color: $dlg-text-secondary;
      text-align: center;
      background: rgba(184, 216, 190, 0.2);
      border-radius: 10px;
      padding: 8px;
    }
  }
}

// ===== 我的收藏抽屉 =====
.favorites-drawer {
  .el-drawer__header {
    background: linear-gradient(135deg, $dlg-primary, $dlg-primary-dark);
    margin: 0;
    padding: 16px 20px;
    color: #fff;
  }

  .el-drawer__title {
    color: #fff;
    font-weight: 700;
    font-size: 16px;
  }

  .el-drawer__close-btn .el-icon {
    color: #fff;
    font-size: 18px;
  }

  .el-drawer__body {
    background: $dlg-bg-page;
    padding: 16px 18px;
  }

  .favorites-body {
    .favorite-item {
      background: $dlg-bg;
      border: 1px solid rgba(232, 220, 200, 0.9);
      border-radius: 12px;
      padding: 12px 14px;
      margin-bottom: 12px;
      box-shadow: 0 2px 8px rgba(180, 140, 100, 0.08);

      .favorite-item-session {
        font-size: 12px;
        color: $dlg-primary-dark;
        font-weight: 600;
        margin-bottom: 6px;
      }

      .favorite-item-content {
        font-size: 13px;
        color: $dlg-text-regular;
        line-height: 1.6;
        display: -webkit-box;
        -webkit-line-clamp: 4;
        -webkit-box-orient: vertical;
        overflow: hidden;
        margin-bottom: 8px;
      }

      .favorite-item-footer {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .favorite-item-time {
          font-size: 11px;
          color: #B8A898;
        }

        .favorite-item-actions {
          .el-button {
            font-size: 12px;
            padding: 2px 6px;
          }
        }
      }
    }

    .favorites-empty {
      text-align: center;
      padding: 60px 0;
      color: $dlg-text-secondary;

      .favorites-empty-emoji {
        font-size: 44px;
        margin-bottom: 12px;
      }

      p {
        margin: 6px 0;
        font-size: 14px;
      }

      .favorites-empty-sub {
        font-size: 12px;
        color: #B8A898;
      }
    }

    .favorites-more {
      text-align: center;
    }
  }
}
</style>
