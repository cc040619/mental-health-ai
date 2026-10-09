import service from '@/utils/request'
import { uuid } from '@/utils/uuid'

// ===== 用户端：消息通知 =====

// 通知列表（type 可选：INTERACTION/ANNOUNCEMENT/APPOINTMENT/ASSESSMENT）
export const getNotificationList = (params) => {
    return service.get('/notify/list', { params })
}

// 未读数（Redis O(1)）
export const getUnreadCount = () => {
    return service.get('/notify/unread-count')
}

// 单条标记已读
export const markNotificationRead = (id) => {
    return service.put(`/notify/read/${id}`)
}

// 全部标记已读
export const markAllNotificationRead = () => {
    return service.put('/notify/read-all')
}

// 公告列表（未读置顶，含 isRead）
export const getAnnouncements = (params) => {
    return service.get('/notify/announcements', { params })
}

// 公告标记已读
export const markAnnouncementRead = (id) => {
    return service.put(`/notify/announcements/${id}/read`)
}

// 消息设置
export const getNotifySettings = () => {
    return service.get('/notify/settings')
}

export const updateNotifySettings = (data) => {
    return service.put('/notify/settings', data)
}

// ===== 管理端：公告管理（三态：0草稿/1已发布/2已撤回） =====

// 公告管理列表（type/status/keyword/page/size，含阅读人数）
export const getAdminAnnouncements = (params) => {
    return service.get('/admin/announcements', { params })
}

// 创建公告（一律存草稿；publishAt 非空=定时发布）
export const createAnnouncement = (data) => {
    return service.post('/admin/announcements', data)
}

// 编辑公告（仅草稿/已撤回可编辑）
export const updateAnnouncement = (id, data) => {
    return service.put(`/admin/announcements/${id}`, data)
}

// 立即发布（草稿/已撤回 → 已发布）
export const publishAnnouncement = (id) => {
    return service.put(`/admin/announcements/${id}/publish`)
}

// 撤回（已发布 → 已撤回）
export const withdrawAnnouncement = (id) => {
    return service.put(`/admin/announcements/${id}/withdraw`)
}

// 删除公告（仅草稿/已撤回可删）
export const deleteAnnouncement = (id) => {
    return service.delete(`/admin/announcements/${id}`)
}

// ===== 管理端：消息推送 =====

// 批量推送站内通知（ALL 全体 / USERS 指定用户），返回推送数
export const pushNotification = (data) => {
    return service.post('/admin/notifications/push', data)
}

// 推送记录分页（含目标数/阅读数/阅读率）
export const getPushRecords = (params) => {
    return service.get('/admin/notifications/pushes', { params })
}

// ===== 管理端：轮播图 =====

export const getAdminBanners = () => {
    return service.get('/admin/banners')
}

export const createBanner = (data) => {
    return service.post('/admin/banners', data)
}

export const updateBanner = (id, data) => {
    return service.put(`/admin/banners/${id}`, data)
}

// 启用/停用（status: 0 停用 1 启用）
export const updateBannerStatus = (id, status) => {
    return service.put(`/admin/banners/${id}/status`, { status })
}

export const deleteBanner = (id) => {
    return service.delete(`/admin/banners/${id}`)
}

// 轮播图图片上传（OSS，businessType=BANNER）
export const uploadBannerImage = (file) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', 'BANNER')
    formData.append('businessId', uuid())
    formData.append('businessField', 'image')
    return service.post('/file/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}

// ===== 用户端：首页轮播 =====

export const getBanners = () => {
    return service.get('/banners')
}
