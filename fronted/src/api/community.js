import service from '@/utils/request'

// ===== 用户端：社区论坛 =====

// 信息流（board 板块 / tab=recommend|latest|hot / page / size）
export const getPostList = (params) => {
    return service.get('/community/posts', { params })
}

// 板块列表
export const getBoards = () => {
    return service.get('/community/boards')
}

// 帖子详情
export const getPostDetail = (postId) => {
    return service.get(`/community/post/${postId}`)
}

// 发帖
export const createPost = (data) => {
    return service.post('/community/post', data)
}

// 删除自己的帖子
export const deletePost = (postId) => {
    return service.delete(`/community/post/${postId}`)
}

// 点赞 toggle，返回最新状态 true=已点赞
export const togglePostLike = (postId) => {
    return service.post(`/community/post/${postId}/like`)
}

// 收藏 toggle，返回最新状态 true=已收藏
export const togglePostFavorite = (postId) => {
    return service.post(`/community/post/${postId}/favorite`)
}

// 评论分页（顶层 + 楼中楼）
export const getComments = (postId, params) => {
    return service.get(`/community/post/${postId}/comments`, { params })
}

// 发表评论 / 楼中楼回复
export const createComment = (postId, data) => {
    return service.post(`/community/post/${postId}/comment`, data)
}

// 删除自己的评论
export const deleteCommentApi = (commentId) => {
    return service.delete(`/community/comment/${commentId}`)
}

// 举报帖子 / 评论（targetType: 1帖子 2评论）
export const reportTarget = (data) => {
    return service.post('/community/report', data)
}

// 搜索
export const searchPosts = (params) => {
    return service.get('/community/search', { params })
}

// 我的社区（帖子 / 评论 / 收藏）
export const getMine = () => {
    return service.get('/community/mine')
}

// 危机干预引导位
export const getCrisisNotice = () => {
    return service.get('/community/daily/practice')
}

// 社区图片上传（返回 { filePath }）
export const uploadCommunityImage = (file) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', 'COMMUNITY')
    formData.append('businessId', crypto.randomUUID())
    formData.append('businessField', 'image')
    return service.post('/file/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}

// ===== 管理端：举报处理 =====

// 举报列表（status: 0待处理 1已下架 2已忽略）
export const getReportList = (params) => {
    return service.get('/admin/reports', { params })
}

// 处理举报（action: DOWN 下架 / IGNORE 忽略）
export const handleReport = (reportId, action) => {
    return service.put(`/admin/reports/${reportId}`, { action })
}
