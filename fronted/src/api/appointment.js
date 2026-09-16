import service from '@/utils/request'

// ===== 用户端 =====

// 创建预约
export const createAppointment = (data) => {
    return service.post('/appointment', data)
}

// 我的预约（状态筛选 + 分页）
export const getMyAppointments = (params) => {
    return service.get('/appointment/mine', { params })
}

// 预约详情
export const getAppointmentDetail = (id) => {
    return service.get(`/appointment/${id}`)
}

// 取消预约
export const cancelAppointment = (id) => {
    return service.post(`/appointment/${id}/cancel`)
}

// 留言历史（分页倒序）
export const getAppointmentMessages = (id, params) => {
    return service.get(`/appointment/${id}/messages`, { params })
}

// 发送留言（HTTP 兜底，WebSocket 不可用时使用）
export const sendAppointmentMessage = (id, data) => {
    return service.post(`/appointment/${id}/messages`, data)
}

// 提交评价
export const submitAppointmentReview = (id, data) => {
    return service.post(`/appointment/${id}/review`, data)
}

// ===== 后台管理 =====

// 预约管理列表（status/consultantId/keyword/startDate/endDate 筛选）
export const getAdminAppointmentPage = (params) => {
    return service.get('/admin/appointments', { params })
}

// 预约管理详情（含问题描述/取消原因/评价回复）
export const getAdminAppointmentDetail = (id) => {
    return service.get(`/admin/appointments/${id}`)
}

// 变更预约状态：CONFIRMED/CANCELED/COMPLETED；取消可带 cancelReason
export const changeAppointmentStatus = (id, status, cancelReason) => {
    return service.put(`/admin/appointments/${id}/status`, { status, cancelReason })
}

// 改期：原子抢占新时段
export const rescheduleAppointment = (id, slotId) => {
    return service.put(`/admin/appointments/${id}/reschedule`, { slotId })
}

// 查询咨询师未来空闲时段（改期选择用）
export const getFreeSlots = (consultantId) => {
    return service.get(`/admin/appointments/consultants/${consultantId}/free-slots`)
}

// 评价管理分页（consultantId/rating/keyword 筛选）
export const getAdminReviewPage = (params) => {
    return service.get('/admin/appointments/reviews', { params })
}

// 回复评价
export const replyReview = (reviewId, content) => {
    return service.put(`/admin/appointments/reviews/${reviewId}/reply`, { content })
}

// 删除不当评价
export const deleteReview = (reviewId) => {
    return service.delete(`/admin/appointments/reviews/${reviewId}`)
}
