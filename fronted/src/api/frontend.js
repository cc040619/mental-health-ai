import service from '@/utils/request'

export const register = (data) => {
    return service.post('/user/add', data)
}

export const startSession = (data) => {
    return service.post('/psychological-chat/session/start', data)
}

export const getSessionList = (params) => {
    return service.get('/psychological-chat/sessions', { params })
}

export const deleteSession = (sessionId) => {
    return service.delete(`/psychological-chat/sessions/${sessionId}`)
}

export const getSessionDetail = (sessionId) => {
    return service.get(`/psychological-chat/sessions/${sessionId}/messages`)
}

export const getSessionEmotion = (sessionId) => {
    return service.get(`/psychological-chat/session/${sessionId}/emotion`)
}

// 触发会话总结（异步生成，已有总结则直接返回）
export const triggerSessionSummary = (sessionId) => {
    return service.post(`/psychological-chat/sessions/${sessionId}/summary`)
}

// 获取会话总结
export const getSessionSummary = (sessionId) => {
    return service.get(`/psychological-chat/sessions/${sessionId}/summary`)
}

// 收藏/取消收藏 AI 回复（toggle）
export const toggleMessageFavorite = (messageId) => {
    return service.post(`/psychological-chat/messages/${messageId}/favorite`)
}

// 收藏的 AI 回复分页列表
export const getFavoriteList = (params) => {
    return service.get('/psychological-chat/favorites', { params })
}

// 提交会话评价（评分 + 文字反馈）
export const submitSessionFeedback = (sessionId, data) => {
    return service.put(`/psychological-chat/sessions/${sessionId}/feedback`, data)
}

// 转人工引导（模板文案）
export const requestHumanHandoff = (sessionId) => {
    return service.post(`/psychological-chat/sessions/${sessionId}/human-handoff`)
}

export const addEmotionDiary = (data) => {
    return service.post('/emotion-diary', data)
}

export const getKnowledgeList = (params) => {
    return service.get('/knowledge/article/page', { params })
}

export const getKnowledgeDetail = (articleId) => {
    return service.get(`/knowledge/article/${articleId}`)
}

// ==================== 文章互动（点赞/评论/阅读时长） ====================

// 点赞 toggle（返回 {liked, likeCount}）
export const toggleArticleLike = (articleId) => {
    return service.post(`/knowledge/article/${articleId}/like`)
}

// 查询已通过评论列表（status=1，分页）
export const getArticleComments = (articleId, params) => {
    return service.get(`/knowledge/article/${articleId}/comments`, { params })
}

// 发表评论（过敏感词过滤，默认待审核）
export const createArticleComment = (articleId, data) => {
    return service.post(`/knowledge/article/${articleId}/comment`, data)
}

// 上报阅读时长（秒，1..7200，离开页面时调用）
export const reportReadDuration = (articleId, data) => {
    return service.put(`/knowledge/article/${articleId}/read-duration`, data)
}

// ==================== 公开配置（无需登录，白名单 key） ====================

// 公开系统设置（系统名称/Logo/备案号/客服/简介；失败时页面用默认值兜底）
export const getPublicSettings = () => service.get('/settings/public')

// 公开危机干预配置（热线数组 + 干预提示文案；失败时用 config/crisis.js 常量兜底）
export const getCrisisConfig = () => service.get('/crisis/config')