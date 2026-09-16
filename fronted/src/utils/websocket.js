/**
 * 预约留言室原生 WebSocket 封装：
 * - token 通过 query 参数携带（浏览器原生 WS 无法自定义请求头，握手拦截器校验）
 * - 断线自动重连：指数退避 1s→2s→4s... 上限 30s，最多 5 次；重连失败由调用方走 HTTP 兜底
 * - 不引入 stomp 等第三方库
 *
 * 返回 { send, close, isOpen }
 */
export function connectAppointmentChat(appointmentId, handlers = {}) {
  const token = localStorage.getItem('token')
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const url = `${protocol}://${window.location.host}/ws/appointment?appointmentId=${appointmentId}&token=${encodeURIComponent(token)}`

  const maxRetries = 5
  let ws = null
  let manualClose = false
  let retries = 0
  let reconnectTimer = null

  const scheduleReconnect = () => {
    if (manualClose || retries >= maxRetries) {
      handlers.onReconnectFailed?.()
      return
    }
    const delay = Math.min(1000 * Math.pow(2, retries), 30000)
    retries += 1
    reconnectTimer = setTimeout(openSocket, delay)
  }

  const openSocket = () => {
    ws = new WebSocket(url)

    ws.onopen = () => {
      retries = 0
      handlers.onStatusChange?.(true)
    }

    ws.onmessage = (event) => {
      let msg
      try {
        msg = JSON.parse(event.data)
      } catch (e) {
        return
      }
      if (msg.type === 'MESSAGE') {
        handlers.onMessage?.(msg)
      } else if (msg.type === 'SYSTEM') {
        handlers.onSystem?.(msg.content)
      }
    }

    ws.onclose = () => {
      handlers.onStatusChange?.(false)
      if (!manualClose) {
        scheduleReconnect()
      }
    }

    ws.onerror = () => {
      // 错误后浏览器会紧接 onclose，交由 onclose 统一重连
      try {
        ws.close()
      } catch (e) {
        /* noop */
      }
    }
  }

  openSocket()

  return {
    isOpen: () => ws !== null && ws.readyState === WebSocket.OPEN,
    send: (content) => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ content }))
        return true
      }
      return false
    },
    close: () => {
      manualClose = true
      clearTimeout(reconnectTimer)
      if (ws) {
        try {
          ws.close()
        } catch (e) {
          /* noop */
        }
      }
    }
  }
}
