import service from '@/utils/request'

// ==================== 个人资料 ====================

// 获取当前用户资料（含个人简介）
export const getProfile = () => {
    return service.get('/user/profile')
}

// 更新个人资料（昵称/头像/性别/生日/简介）
export const updateProfile = (data) => {
    return service.put('/user/profile', data)
}

// ==================== 账号安全 ====================

// 修改密码（登录态，校验旧密码）
export const changePassword = (data) => {
    return service.put('/user/password', data)
}

// 首次设置密码（仅无密码用户可用，已有密码则拒绝）
export const setPassword = (data) => {
    return service.put('/user/password/set', data)
}

// 忘记密码重置（免登录，凭邮箱验证码）
export const resetPassword = (data) => {
    return service.put('/user/password/reset', data)
}

// 发送邮箱验证码（忘记密码时向指定邮箱发送，复用登录验证码机制）
export const sendEmailCode = (email) => {
    return service.post('/user/email/send-code', { email })
}

// 向当前用户邮箱发送验证码（绑定/换绑手机号身份校验）
export const sendPhoneBindCode = () => {
    return service.post('/user/phone/send-code')
}

// 绑定/换绑手机号
export const bindPhone = (data) => {
    return service.put('/user/phone', data)
}

// ==================== 我的收藏 ====================

// 收藏文章
export const addFavorite = (articleId) => {
    return service.post('/user/favorite', { articleId })
}

// 分页查询收藏列表
export const getFavorites = (params) => {
    return service.get('/user/favorites', { params })
}

// 取消收藏
export const removeFavorite = (articleId) => {
    return service.delete(`/user/favorites/${articleId}`)
}

// 检查是否已收藏某篇文章（文章详情页使用）
export const checkFavorite = (articleId) => {
    return service.get(`/user/favorites/check/${articleId}`)
}

// ==================== 浏览历史 ====================

// 记录浏览历史（访问文章详情时上报）
export const recordBrowseHistory = (articleId) => {
    return service.post('/user/history', { articleId })
}

// 分页查询浏览历史
export const getBrowseHistory = (params) => {
    return service.get('/user/history', { params })
}

// 删除单条浏览历史
export const deleteBrowseHistory = (id) => {
    return service.delete(`/user/history/${id}`)
}

// 清空浏览历史
export const clearBrowseHistory = () => {
    return service.delete('/user/history')
}

// ==================== 账号注销 ====================

// 注销账号（软删除）
export const deactivateAccount = () => {
    return service.delete('/user/account')
}
