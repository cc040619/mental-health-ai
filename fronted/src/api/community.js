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

// ===== 管理端：社区管理 =====

// 帖子列表（board/keyword/status/page/size）
export const getAdminPostPage = (params) => {
    return service.get('/admin/posts', { params })
}

// 帖子审核（approve: true通过 / false驳回，驳回时 rejectReason 必填）
export const reviewAdminPost = (id, data) => {
    return service.put(`/admin/posts/${id}/review`, data)
}

// 帖子删除(软删)/下架（status: 1删除 3下架）
export const updateAdminPostStatus = (id, status) => {
    return service.put(`/admin/posts/${id}/status`, { value: status })
}

// 置顶/取消置顶（isTop: 0/1）
export const updateAdminPostTop = (id, isTop) => {
    return service.put(`/admin/posts/${id}/top`, { value: isTop })
}

// 加精/取消加精（isFeatured: 0/1）
export const updateAdminPostFeature = (id, isFeatured) => {
    return service.put(`/admin/posts/${id}/feature`, { value: isFeatured })
}

// 评论列表（postId/keyword/status/page/size）
export const getAdminCommentPage = (params) => {
    return service.get('/admin/comments', { params })
}

// 评论审核（approve: true通过 / false驳回软删）
export const reviewAdminComment = (id, approve) => {
    return service.put(`/admin/comments/${id}/review`, { approve })
}

// 删除评论（软删 + count-1）
export const deleteAdminComment = (id) => {
    return service.put(`/admin/comments/${id}/delete`)
}

// 黑名单分页
export const getAdminBlacklist = (params) => {
    return service.get('/admin/blacklist', { params })
}

// 屏蔽用户
export const addAdminBlacklist = (data) => {
    return service.post('/admin/blacklist', data)
}

// 解除屏蔽
export const removeAdminBlacklist = (userId) => {
    return service.delete(`/admin/blacklist/${userId}`)
}

// 板块列表（含帖子数/版主昵称）
export const getAdminBoards = () => {
    return service.get('/admin/boards')
}

// 新增/编辑板块（编辑时带 id，code 不可改）
export const saveAdminBoard = (data) => {
    return service.post('/admin/boards', data)
}

// 删除板块（有帖子拒绝）
export const deleteAdminBoard = (id) => {
    return service.delete(`/admin/boards/${id}`)
}

// 举报列表（status: 0待处理 1已下架 2已忽略 3已警告）
export const getReportList = (params) => {
    return service.get('/admin/reports', { params })
}

// 处理举报（action: DELETE_POST 删除内容 / WARN_USER 警告 / IGNORE 忽略）
export const handleReport = (reportId, action) => {
    return service.put(`/admin/reports/${reportId}`, { action })
}

// 敏感词列表（keyword/category/page/size）
export const getAdminSensitiveWords = (params) => {
    return service.get('/admin/sensitive-words', { params })
}

// 新增敏感词
export const addAdminSensitiveWord = (data) => {
    return service.post('/admin/sensitive-words', data)
}

// 编辑敏感词
export const updateAdminSensitiveWord = (id, data) => {
    return service.put(`/admin/sensitive-words/${id}`, data)
}

// 删除敏感词
export const deleteAdminSensitiveWord = (id) => {
    return service.delete(`/admin/sensitive-words/${id}`)
}

// 批量导入（返回 {success, duplicate, invalid}）
export const importAdminSensitiveWords = (data) => {
    return service.post('/admin/sensitive-words/import', data)
}
