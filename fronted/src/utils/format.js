// 通用时间格式化（后端 LocalDateTime 默认输出 ISO，如 2026-09-10T14:30:00）

const pad = (n) => String(n).padStart(2, '0')

// 格式化为 YYYY-MM-DD HH:mm
export const formatDateTime = (value) => {
    if (!value) return ''
    const d = new Date(value)
    if (Number.isNaN(d.getTime())) return ''
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 相对时间：刚刚 / x分钟前 / x小时前 / x天前，超过 7 天显示日期
export const formatRelative = (value) => {
    if (!value) return ''
    const d = new Date(value)
    if (Number.isNaN(d.getTime())) return ''
    const diff = Date.now() - d.getTime()
    const minute = 60 * 1000
    const hour = 60 * minute
    const day = 24 * hour
    if (diff < minute) return '刚刚'
    if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`
    if (diff < day) return `${Math.floor(diff / hour)} 小时前`
    if (diff < 7 * day) return `${Math.floor(diff / day)} 天前`
    return formatDateTime(value)
}
