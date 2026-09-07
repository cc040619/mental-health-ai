export const fileBaseUrl = 'http://127.0.0.1:8080'

// 解析文件访问地址：绝对URL（如阿里云OSS）直接返回，相对路径拼接后端地址
export const resolveFileUrl = (path) => {
    if (!path) {
        return path
    }
    return path.startsWith('http://') || path.startsWith('https://') ? path : fileBaseUrl + path
}
