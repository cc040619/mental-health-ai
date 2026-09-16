import service from '@/utils/request'

// 获取我的安全计划（无记录返回空模板，不报错）
export const getSafetyPlan = () => {
    return service.get('/crisis/safety-plan')
}

// 保存安全计划（仅本人可写，user_id 从登录态获取）
export const saveSafetyPlan = (data) => {
    return service.put('/crisis/safety-plan', data)
}
