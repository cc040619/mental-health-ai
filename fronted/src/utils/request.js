import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 请求的前缀
  timeout: 5000, // 请求的超时时间
})

// 鉴权失效处理防抖：多个并发请求同时 401 时，只提示并跳转一次
let authRedirecting = false
const handleAuthExpired = (message) => {
  // 已在登录页则不再重复跳转
  if (window.location.pathname.startsWith('/auth/login')) return
  if (authRedirecting) return
  authRedirecting = true
  ElMessage.error(message || '登录状态已过期，请重新登录')
  // 清除登录信息
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  // 携带回跳地址，登录后可返回原页面
  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  window.location.href = `/auth/login?redirect=${redirect}`
}

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 在发送请求之前做些什么
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['token'] = token
    }
    return config
  },
  (error) => {
    // 对请求错误做些什么
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    // 对响应数据做点什么
    const { data, config } = response
    // blob 响应（文件下载）无业务码结构，直通返回整个 response 供调用方取 headers/data
    if (config.responseType === 'blob') {
        return response
    }
    // 处理业务状态码
    if (data.code === '200') {
        return data.data
    }
    // 鉴权失效业务码（后端过滤器在 HTTP 401 时返回；此处对 HTTP 200 场景兜底）
    // A0230: token无效/过期/黑名单  A0301: 访问未授权  401: 未登录
    if (data.code === 'A0230' || data.code === 'A0301' || data.code === '401') {
        // 登录接口自身的失败不跳转，仅提示错误
        if (config.url?.includes('/login')) {
            ElMessage.error(data.msg || '登录失败，请重试')
            return Promise.reject(data)
        }
        handleAuthExpired(data.msg)
        return Promise.reject(data)
    }
    // 其他业务错误码：统一提示并拒绝，避免调用方误判为成功
    ElMessage.error(data.msg || '操作失败')
    return Promise.reject(data)
  },
  (error) => {
    // HTTP 状态码层面的错误处理
    const status = error.response?.status
    const resData = error.response?.data
    // 401 未认证（token过期/无效/未登录）：清登录态并跳转登录页
    if (status === 401) {
        handleAuthExpired(resData?.msg || '登录状态已过期，请重新登录')
        return Promise.reject(error)
    }
    // 403 禁止访问（账号被禁用等）：同样引导重新登录
    if (status === 403) {
        handleAuthExpired(resData?.msg || '账号暂不可用，请联系管理员或重新登录')
        return Promise.reject(error)
    }
    // 超时/断网/服务器错误给出友好提示，其余错误交由调用方处理
    if (error.code === 'ECONNABORTED' || (error.message || '').includes('timeout')) {
        ElMessage.error('请求超时，请稍后重试')
    } else if (!error.response) {
        ElMessage.error('网络异常，请检查网络连接')
    } else if (status >= 500) {
        ElMessage.error('服务器开小差了，请稍后重试')
    } else if (status) {
        ElMessage.error(resData?.msg || '请求失败，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default service
