import service from '@/utils/request'

export function login(data) {
    return service.post('/user/login', data)
}

// 发送邮箱登录验证码
export function sendEmailCode(data) {
    return service.post('/user/email/send-code', data)
}

// 邮箱验证码登录
export function emailLogin(data) {
    return service.post('/user/email/login', data)
}


export function categoryTree() {
    return service.get('/knowledge/category/tree')
}

export function articlePage(params) {
     return service.get('/knowledge/article/page', { params })
}

export function uploadFile(file, businessInfo) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', 'ARTICLE')
    formData.append('businessId', businessInfo.businessId)
    formData.append('businessField', 'cover')

    return service.post('/file/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}


export function createArticle(data) {
    return service.post('/knowledge/article', data)
}

export function getArticleDetail(id) {
    return service.get(`/knowledge/article/${id}`)
}


export function updateArticle(id, data) {
    return service.put(`/knowledge/article/${id}`, data)
}

export function changeArticleStatus(id, data) {
    return service.put(`/knowledge/article/${id}/status`, data)
}

export function deleteArticle(id) {
    return service.delete(`/knowledge/article/${id}`)
}

// ==================== 文章批量操作 ====================

// 批量上架/下架（ids + status 1|2）
export function batchUpdateArticleStatus(data) {
    return service.post('/knowledge/article/batch/status', data)
}

// 批量删除（物理删除，级联清理收藏/浏览/点赞/评论）
export function batchDeleteArticles(data) {
    return service.delete('/knowledge/article/batch', { data })
}

// 批量移动分类（ids + categoryId）
export function batchMoveArticleCategory(data) {
    return service.put('/knowledge/article/batch/category', data)
}

// 文章数据统计（近30天浏览趋势/阅读时长/各项计数）
export function getArticleStats(id) {
    return service.get(`/knowledge/article/${id}/stats`)
}

// ==================== 文章评论管理 ====================

// 评论列表（status/articleId 筛选）
export function getCommentPage(params) {
    return service.get('/knowledge/comment/page', { params })
}

// 审核评论（approve=true 通过，false 拒绝）
export function reviewComment(id, data) {
    return service.put(`/knowledge/comment/${id}/review`, data)
}

// 删除评论
export function deleteComment(id) {
    return service.delete(`/knowledge/comment/${id}`)
}

// ==================== 分类管理 ====================

// 全部分类（含文章数量统计）
export function getCategoryList() {
    return service.get('/knowledge/category/list')
}

// 新增分类
export function createCategory(data) {
    return service.post('/knowledge/category', data)
}

// 更新分类
export function updateCategory(id, data) {
    return service.put(`/knowledge/category/${id}`, data)
}

// 删除分类
export function deleteCategory(id) {
    return service.delete(`/knowledge/category/${id}`)
}

// ==================== 咨询记录管理（后台） ====================

// 会话列表（多条件筛选 + 聚合时长/消息数）
export function getAdminConsultationPage(params) {
    return service.get('/admin/consultations/page', { params })
}

// 会话详情（消息时间轴 + 情绪值 + 总结 + 评价 + 标记）
export function getAdminConsultationDetail(id) {
    return service.get(`/admin/consultations/${id}`)
}

// 标记会话（高质量/危机/备注，幂等）
export function markConsultation(id, data) {
    return service.put(`/admin/consultations/${id}/mark`, data)
}

// 导出单会话 Excel（blob 下载，文件名取后端 Content-Disposition）
export function exportConsultation(id) {
    return service.get(`/admin/consultations/export/${id}`, { responseType: 'blob' })
}

export function getEmotionalPage(params) {
    return service.get('/emotion-diary/admin/page', { params })
}

export function deleteEmotional(id) {
    return service.delete(`/emotion-diary/admin/${id}`)
}

// ==================== 数据看板 ====================

// 核心指标卡（range/startDate/endDate）
export function getDashboardOverview(params) {
    return service.get('/data-analytics/overview', { params })
}

// 用户增长趋势（granularity=day/month）
export function getUserTrend(params) {
    return service.get('/data-analytics/user-trend', { params })
}

// 咨询统计
export function getConsultationStats(params) {
    return service.get('/data-analytics/consultation-stats', { params })
}

// 测评分析（含高危用户预警）
export function getAssessmentStats(params) {
    return service.get('/data-analytics/assessment-stats', { params })
}

// 情绪大数据
export function getEmotionStats(params) {
    return service.get('/data-analytics/emotion-stats', { params })
}

// 内容排行 TOP10（不随时间范围变化）
export function getContentStats() {
    return service.get('/data-analytics/content-stats')
}

// 预约统计
export function getAppointmentStats(params) {
    return service.get('/data-analytics/appointment-stats', { params })
}

export function logout() {
    return service.post('/user/logout')
}

// ==================== 用户管理 ====================

// 用户列表（keyword/phone/status/startDate/endDate/page/size）
export function getAdminUserList(params) {
    return service.get('/admin/user/list', { params })
}

// 用户详情（基本信息 + 简要统计）
export function getAdminUserDetail(id) {
    return service.get(`/admin/user/${id}/detail`)
}

// 用户咨询记录（懒加载）
export function getAdminUserConsultations(id, params) {
    return service.get(`/admin/user/${id}/consultations`, { params })
}

// 用户测评记录（懒加载）
export function getAdminUserAssessments(id, params) {
    return service.get(`/admin/user/${id}/assessments`, { params })
}

// 用户情绪日记（懒加载）
export function getAdminUserDiaries(id, params) {
    return service.get(`/admin/user/${id}/diaries`, { params })
}

// 用户发帖记录（懒加载）
export function getAdminUserPosts(id, params) {
    return service.get(`/admin/user/${id}/posts`, { params })
}

// 用户预约记录（懒加载）
export function getAdminUserAppointments(id, params) {
    return service.get(`/admin/user/${id}/appointments`, { params })
}

// 禁用/启用用户（status=0 禁用，1 启用）
export function updateAdminUserStatus(id, data) {
    return service.put(`/admin/user/${id}/status`, data)
}

// 重置密码（重置为默认密码并强制下线）
export function resetAdminUserPassword(id) {
    return service.put(`/admin/user/${id}/password`)
}

// 强制下线（删除 Redis 白名单 token）
export function forceLogoutAdminUser(id) {
    return service.post(`/admin/user/${id}/force-logout`)
}

// 用户统计（性别/年龄段/注册趋势，走 Redis 缓存 TTL 10 分钟）
export function getAdminUserStats() {
    return service.get('/admin/user/stats')
}

// ==================== 课程内容管理 ====================

// 课程列表（keyword/category/status/page/size，聚合章节数/总时长）
export function getAdminCoursePage(params) {
    return service.get('/admin/courses/page', { params })
}

// 各状态课程计数（前端 Tab 数字）
export function getAdminCourseStatusCount() {
    return service.get('/admin/courses/status-count')
}

// 课程详情（基本信息 + 章节列表）
export function getAdminCourseDetail(id) {
    return service.get(`/admin/courses/${id}`)
}

// 创建课程（默认草稿状态）
export function createAdminCourse(data) {
    return service.post('/admin/courses', data)
}

// 编辑课程基本信息（仅草稿/已驳回可编辑）
export function updateAdminCourse(id, data) {
    return service.put(`/admin/courses/${id}`, data)
}

// 提交审核（草稿/已驳回 → 待审核）
export function submitCourseReview(id) {
    return service.put(`/admin/courses/${id}/submit`)
}

// 审核课程（approve=true 通过，false 驳回 + rejectReason）
export function reviewCourse(id, data) {
    return service.put(`/admin/courses/${id}/review`, data)
}

// 上架/下架（status: 2 已发布 ↔ 4 已下架）
export function updateCourseStatus(id, data) {
    return service.put(`/admin/courses/${id}/status`, data)
}

// 新增章节（sortOrder 自增，重算 totalDuration）
export function addCourseChapter(courseId, data) {
    return service.post(`/admin/courses/${courseId}/chapters`, data)
}

// 编辑章节
export function updateCourseChapter(chapterId, data) {
    return service.put(`/admin/courses/chapters/${chapterId}`, data)
}

// 删除章节（级联清理 progress，重算 totalDuration）
export function deleteCourseChapter(chapterId) {
    return service.delete(`/admin/courses/chapters/${chapterId}`)
}

// 批量重排章节（items: [{id, sortOrder}]）
export function sortCourseChapters(data) {
    return service.put('/admin/courses/chapters/sort', data)
}

// 课程学习数据（学习人数/完成率/平均学习时长/章节热力图）
export function getCourseStats(id) {
    return service.get(`/admin/courses/${id}/stats`)
}

// 课程评论列表（courseId/keyword/page/size）
export function getAdminCourseComments(params) {
    return service.get('/admin/courses/comments', { params })
}

// 回复评论
export function replyCourseComment(id, data) {
    return service.put(`/admin/courses/comments/${id}/reply`, data)
}

// 删除评论
export function deleteCourseComment(id) {
    return service.delete(`/admin/courses/comments/${id}`)
}

// ==================== 心理测评管理 ====================

// 量表列表
export function assessmentScalePage(params) {
    return service.get('/admin/assessments/scales/page', { params })
}
// 创建量表
export function createAssessmentScale(data) {
    return service.post('/admin/assessments/scales', data)
}
// 量表详情（含题目+维度+模板）
export function assessmentScaleDetail(id) {
    return service.get(`/admin/assessments/scales/${id}`)
}
// 编辑量表基本信息
export function updateAssessmentScale(id, data) {
    return service.put(`/admin/assessments/scales/${id}`, data)
}
// 整体保存题目+维度+模板
export function saveScaleQuestions(id, data) {
    return service.put(`/admin/assessments/scales/${id}/questions`, data)
}
// 启用/停用量表
export function updateScaleEnabled(id, enabled) {
    return service.put(`/admin/assessments/scales/${id}/enabled`, null, { params: { enabled } })
}

// 题目库分页
export function questionBankPage(params) {
    return service.get('/admin/assessments/questions/page', { params })
}
// 新增题库题
export function createQuestion(data) {
    return service.post('/admin/assessments/questions', data)
}
// 编辑题库题
export function updateQuestion(id, data) {
    return service.put(`/admin/assessments/questions/${id}`, data)
}
// 删除题库题
export function deleteQuestion(id) {
    return service.delete(`/admin/assessments/questions/${id}`)
}

// 测评记录分页
export function assessmentRecordPage(params) {
    return service.get('/admin/assessments/records/page', { params })
}
// 报告详情
export function assessmentRecordDetail(id) {
    return service.get(`/admin/assessments/records/${id}`)
}

// 高危预警列表
export function assessmentAlerts(params) {
    return service.get('/admin/assessments/alerts', { params })
}
// 标记干预
export function updateIntervention(recordId, data) {
    return service.put(`/admin/assessments/alerts/${recordId}/intervention`, data)
}
// ==================== AI 模型配置模块 ====================
// 配置列表
export function getAiConfig() {
    return service.get('/admin/ai/config')
}
// 保存配置
export function saveAiConfig(data) {
    return service.put('/admin/ai/config', data)
}
// 实时测试
export function testAi(data) {
    return service.post('/admin/ai/test', data)
}
// 提示词分组列表
export function getAiPrompts() {
    return service.get('/admin/ai/prompts')
}
// 某 key 版本列表
export function getAiPromptVersions(key) {
    return service.get(`/admin/ai/prompts/${key}/versions`)
}
// 预览某版本全文
export function previewAiPrompt(key, version) {
    return service.get(`/admin/ai/prompts/${key}`, { params: { version } })
}
// 保存新版本（草稿）
export function saveAiPrompt(data) {
    return service.post('/admin/ai/prompts', data)
}
// 激活版本
export function activateAiPrompt(id) {
    return service.put(`/admin/ai/prompts/${id}/activate`)
}
// 恢复默认
export function restoreAiPrompt(key) {
    return service.post(`/admin/ai/prompts/${key}/restore`)
}
// 提示词试跑
export function testAiPrompt(data) {
    return service.post('/admin/ai/prompts/test', data)
}
// 拦截记录
export function getAiBlocks(params) {
    return service.get('/admin/ai/blocks', { params })
}
// 发起抽检
export function generateAiSamples(data) {
    return service.post('/admin/ai/samples/generate', data)
}
// 抽检列表
export function getAiSamples(params) {
    return service.get('/admin/ai/samples', { params })
}
// 抽检详情
export function getAiSampleDetail(id) {
    return service.get(`/admin/ai/samples/${id}`)
}
// 标注
export function annotateAiSample(id, data) {
    return service.put(`/admin/ai/samples/${id}`, data)
}
// 调用统计
export function getAiStats(params) {
    return service.get('/admin/ai/stats', { params })
}

// ==================== 系统设置模块 ====================

// 按组读取配置（group=BASIC/REGISTER/CONTENT/CRISIS，不传返回全部）
export function getAdminSettings(group) {
    return service.get('/admin/settings', { params: group ? { group } : {} })
}

// 批量保存配置（items: [{configKey, configValue}]，key 白名单校验，写后删缓存）
export function saveAdminSettings(data) {
    return service.put('/admin/settings', data)
}

// 系统 Logo 上传（businessType=SYSTEM，返回 filePath 为 OSS 完整 URL）
export function uploadSystemLogo(file) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', 'SYSTEM')
    return service.post('/file/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}

// 危机词分页列表（keyword 模糊/page/size）
export function getCrisisWords(params) {
    return service.get('/admin/crisis-words', { params })
}

// 新增危机词（去重校验，保存后不重启即生效）
export function addCrisisWord(data) {
    return service.post('/admin/crisis-words', data)
}

// 启用/停用危机词（status: 1 启用 / 0 停用；停用受护栏保护）
export function updateCrisisWord(id, data) {
    return service.put(`/admin/crisis-words/${id}`, data)
}

// 删除危机词（启用词剩余不足 5 个时后端拒绝）
export function deleteCrisisWord(id) {
    return service.delete(`/admin/crisis-words/${id}`)
}

// 恢复默认危机词（清空词表 + 从 crisis-words.txt 全量重建）
export function restoreCrisisWords() {
    return service.post('/admin/crisis-words/restore')
}

// 操作日志分页（operator/actionType/keyword/startDate/endDate/page/size）
export function getOperationLogs(params) {
    return service.get('/admin/logs', { params })
}
