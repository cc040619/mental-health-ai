import service from '@/utils/request'

// ===== 用户端 =====

// 咨询师列表（擅长领域筛选 + 分页，走缓存）
export const getConsultantList = (params) => {
    return service.get('/consultant/list', { params })
}

// 咨询师详情
export const getConsultantDetail = (id) => {
    return service.get(`/consultant/${id}`)
}

// 某日可约时段；不传 date 返回未来 7 天聚合
export const getConsultantSlots = (id, date) => {
    return service.get(`/consultant/${id}/slots`, { params: date ? { date } : {} })
}

// 来访者评价列表（匿名）
export const getConsultantReviews = (id, params) => {
    return service.get(`/consultant/${id}/reviews`, { params })
}

// ===== 后台管理 =====

// 咨询师管理列表（keyword 姓名/擅长领域，status 0待审核/1正常/2禁用）
export const getAdminConsultantPage = (params) => {
    return service.get('/admin/consultants', { params })
}

// 列表状态 Tab 计数
export const getConsultantStatusCounts = () => {
    return service.get('/admin/consultants/status-counts')
}

// 咨询师管理详情（含资质/审核信息，任意状态）
export const getAdminConsultantDetail = (id) => {
    return service.get(`/admin/consultants/${id}`)
}

// 资质审核：{ approve: true } 通过；{ approve: false, rejectReason } 驳回
export const reviewConsultant = (id, data) => {
    return service.put(`/admin/consultants/${id}/review`, data)
}

// 咨询记录 Tab（分页）
export const getConsultantAppointments = (id, params) => {
    return service.get(`/admin/consultants/${id}/appointments`, { params })
}

// 评价列表 Tab（分页，管理端）
export const getAdminConsultantReviews = (id, params) => {
    return service.get(`/admin/consultants/${id}/reviews`, { params })
}

// 收入统计
export const getConsultantIncome = (id) => {
    return service.get(`/admin/consultants/${id}/income`)
}

// 新增咨询师（默认待审核）
export const createConsultant = (data) => {
    return service.post('/admin/consultants', data)
}

// 编辑咨询师（排序权重/价格并入；待审核状态编辑=重新提交）
export const updateConsultant = (data) => {
    return service.put('/admin/consultants', data)
}

// 禁用/恢复：status 1正常 2禁用
export const changeConsultantStatus = (id, status) => {
    return service.put(`/admin/consultants/${id}/status`, { status })
}

/**
 * 排班批量生成：
 * - 按日 { date, slots }
 * - 周模板 { weekStart, slots }（周一~周日，已存在跳过）
 * 返回 { created, skipped }
 */
export const createConsultantSlots = (id, data) => {
    return service.post(`/admin/consultants/${id}/slots`, data)
}

// 查看某日全部时段（含已约/停用）
export const getAdminConsultantSlots = (id, date) => {
    return service.get(`/admin/consultants/${id}/slots`, { params: { date } })
}

// 周视图：传周内任意一天，后端归一到周一，一次返回整周时段
export const getConsultantWeekSlots = (id, weekStart) => {
    return service.get(`/admin/consultants/${id}/slots`, { params: { weekStart } })
}

// 单时段停用/恢复：status 0可约 2停用（已约时段后端拒绝）
export const updateSlotStatus = (slotId, status) => {
    return service.put(`/admin/consultants/slots/${slotId}/status`, { status })
}

// 咨询师头像/资质图片上传（复用通用文件接口，返回 { filePath }）
export const uploadConsultantImage = (file) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', 'CONSULTANT')
    formData.append('businessField', 'image')
    return service.post('/file/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}

// ===== 咨询师端（user_type=3，/consultant/** 全部 Service 层校验角色+归属）=====

// 工作台 - 数据概览
export const getWorkbenchOverview = () => {
    return service.get('/consultant/workbench/overview')
}

// 工作台 - 待办三组（即将开始/未回复评价/待处理预约，各 ≤5）
export const getWorkbenchTodos = () => {
    return service.get('/consultant/workbench/todos')
}

// 我的预约分页（status 状态码，date=yyyy-MM-dd）
export const getMyAppointments = (params) => {
    return service.get('/consultant/appointments', { params })
}

// 确认(PENDING→CONFIRMED)/完成(CONFIRMED→COMPLETED)自己的预约
export const changeMyAppointmentStatus = (id, status) => {
    return service.put(`/consultant/appointments/${id}/status`, { status })
}

// 5.3 咨询师取消预约（特权：不受用户取消时限约束；原因必填，来访者可见）
export const cancelMyAppointment = (id, reason) => {
    return service.put(`/consultant/appointments/${id}/cancel`, { reason })
}

// 日程周视图（weekStart 传周内任意一天，后端归一到周一；与管理端 4.3 同表实时一致）
export const getMySchedule = (weekStart) => {
    return service.get('/consultant/schedule', { params: { weekStart } })
}

// 批量生成自己的排班：按日 { date, slots } 或周模板 { weekStart, slots }，已存在跳过
export const createMySlots = (data) => {
    return service.post('/consultant/schedule/slots', data)
}

// 停用/恢复自己的单个时段（status: 0-可约 2-停用；已约时段后端拒绝）
export const updateMySlotStatus = (slotId, status) => {
    return service.put(`/consultant/schedule/slots/${slotId}/status`, { status })
}

// 删除自己的单个时段（仅可约/停用可删，已约拒绝）
export const deleteMySlot = (slotId) => {
    return service.delete(`/consultant/schedule/slots/${slotId}`)
}

// 我的预约设置：价格（空=不收费）/ 可预约天数 1-90 / 用户取消时限 0-48 小时
export const getMySettings = () => {
    return service.get('/consultant/settings')
}

export const updateMySettings = (data) => {
    return service.put('/consultant/settings', data)
}

// 我的评价分页
export const getMyReviews = (params) => {
    return service.get('/consultant/reviews', { params })
}

// 回复自己的评价（覆盖更新）
export const replyMyReview = (id, content) => {
    return service.put(`/consultant/reviews/${id}/reply`, { content })
}

// 发文分类列表
export const getArticleCategories = () => {
    return service.get('/consultant/article/categories')
}

// 发布文章（后端默认 status=0 草稿，管理员上架后可见）
export const publishArticle = (data) => {
    return service.post('/consultant/article', data)
}

// 文章封面上传（复用通用文件接口）
export const uploadArticleCover = (file) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', 'ARTICLE')
    formData.append('businessField', 'cover')
    return service.post('/file/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}

// ===== 咨询会话管理（会话=预约，归属校验在 Service 层 ensureConsultantOfAppointment）=====

// 会话列表=当前咨询师名下预约：status 状态码，keyword 昵称搜索
export const getSessions = (params) => {
    return service.get('/consultant/sessions', { params })
}

// 会话详情：预约信息+来访者基本信息（昵称/头像，不含手机号邮箱）+咨询记录
export const getSessionDetail = (appointmentId) => {
    return service.get(`/consultant/sessions/${appointmentId}`)
}

// 来访者测评记录摘要（复用 listRecordsForAdmin，前置归属校验）
export const getSessionAssessments = (appointmentId, params) => {
    return service.get(`/consultant/sessions/${appointmentId}/assessments`, { params })
}

// 来访者情绪日记摘要（截断为摘要，不返回全文/AI分析）
export const getSessionDiaries = (appointmentId, params) => {
    return service.get(`/consultant/sessions/${appointmentId}/diaries`, { params })
}

// 来访者历史咨询记录（按 user_id 查当前咨询师名下）
export const getSessionHistoryRecords = (appointmentId, params) => {
    return service.get(`/consultant/sessions/${appointmentId}/history-records`, { params })
}

// 留言列表（复用 pageHistory，前置归属校验）
export const getSessionMessages = (appointmentId, params) => {
    return service.get(`/consultant/sessions/${appointmentId}/messages`, { params })
}

// 发送留言（senderType=2 咨询师，前置归属校验防越权）
export const sendSessionMessage = (appointmentId, content) => {
    return service.post(`/consultant/sessions/${appointmentId}/messages`, { content })
}

// 咨询记录（无则返回 null，前端展示空模板）
export const getSessionRecord = (appointmentId) => {
    return service.get(`/consultant/sessions/${appointmentId}/record`)
}

// 保存咨询记录（upsert，一单一记录）
export const saveSessionRecord = (appointmentId, data) => {
    return service.put(`/consultant/sessions/${appointmentId}/record`, data)
}

// ===== 管理员只读联动（4.8 预约详情）=====

// 预约详情咨询记录只读
export const getAdminAppointmentRecord = (appointmentId) => {
    return service.get(`/admin/appointments/${appointmentId}/record`)
}
