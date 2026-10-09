/**
 * 生成 UUID v4 字符串。
 *
 * 浏览器原生 crypto.randomUUID() 属于 Secure Context 限定 API，仅在 HTTPS、localhost、
 * 127.0.0.1 等安全上下文可用。经 HTTP + 公网 IP 访问时该 API 不存在，直接调用会抛
 * TypeError（且常被上层 catch 吞掉，表现为「上传无反应」这类含糊故障）。
 *
 * 这里做特性检测并降级到 Math.random 实现，保证开发（localhost）与线上（HTTP+IP）
 * 行为一致；站点启用 HTTPS 后会自动走原生实现。
 *
 * @author cc
 * @date 2026-09-17
 */

/**
 * @returns {string} 36 位 UUID，形如 xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
 */
export function uuid() {
    if (window.crypto && typeof window.crypto.randomUUID === 'function') {
        return window.crypto.randomUUID()
    }
    // 降级实现（非加密安全，仅用于业务关联 ID）
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = (Math.random() * 16) | 0
        return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16)
    })
}
