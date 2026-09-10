import service from '@/utils/request'

// 课程广场（类别筛选 + 分页）
export const getCourseList = (params) => {
    return service.get('/course/list', { params })
}

// 课程详情
export const getCourseDetail = (courseId) => {
    return service.get(`/course/${courseId}`)
}

// 收藏 toggle，返回最新状态
export const toggleFavorite = (courseId) => {
    return service.post(`/course/${courseId}/favorite`)
}

// 我的收藏课程列表
export const getFavoriteCourses = (params) => {
    return service.get('/course/favorites', { params })
}

// 我的课程（已学习/学习中/已收藏）
export const getMyCourses = () => {
    return service.get('/course/mine')
}

// 课程评论分页
export const getCourseComments = (courseId, params) => {
    return service.get(`/course/${courseId}/comments`, { params })
}

// 发表评论
export const createComment = (courseId, data) => {
    return service.post(`/course/${courseId}/comments`, data)
}

// 删除评论
export const deleteComment = (commentId) => {
    return service.delete(`/course/comments/${commentId}`)
}

// 上报学习进度
export const updateProgress = (data) => {
    return service.put('/course/progress', data)
}

// 今日推荐（每日一练）
export const getDailyPractice = () => {
    return service.get('/course/daily')
}

// 每日打卡
export const checkinDaily = (data) => {
    return service.post('/course/daily/checkin', data)
}

// 当月打卡日历 + 连续打卡天数
export const getCheckinCalendar = (params) => {
    return service.get('/course/checkins', { params })
}
