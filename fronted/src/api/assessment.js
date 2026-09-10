import service from '@/utils/request'

// 测评广场列表（类别筛选）
export const getScaleList = (params) => {
    return service.get('/assessment/scales', { params })
}

// 量表详情（含题目与选项）
export const getScaleDetail = (scaleId) => {
    return service.get(`/assessment/scales/${scaleId}`)
}

// 开始测评（创建/续答 IN_PROGRESS 记录）
export const startAssessment = (data) => {
    return service.post('/assessment/records/start', data)
}

// 中途保存进度（退出续答）
export const saveProgress = (recordId, data) => {
    return service.put(`/assessment/records/${recordId}/progress`, data)
}

// 提交作答：评分 → 报告
export const submitAssessment = (recordId, data) => {
    return service.post(`/assessment/records/${recordId}/submit`, data)
}

// 记录详情（进度/报告）
export const getRecordDetail = (recordId) => {
    return service.get(`/assessment/records/${recordId}`)
}

// 测评历史（分页）
export const getRecordList = (params) => {
    return service.get('/assessment/records', { params })
}

// 同量表历史趋势
export const getRecordTrend = (params) => {
    return service.get('/assessment/records/trend', { params })
}
