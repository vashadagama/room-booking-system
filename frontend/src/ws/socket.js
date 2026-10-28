import { ref } from 'vue'

const listeners = new Set()

export const wsStatus = ref('disconnected')
const wsError = ref(null)

let socket = null
let reconnectTimer = null
let attempt = 0
let closedByUs = false

const RECONNECT_BASE_MS = 1000
const RECONNECT_MAX_MS = 15000

function buildUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/rooms`
}

function emit(message) {
  listeners.forEach((fn) => {
    try {
      fn(message)
    } catch (e) {
      console.error('[ws] ошибка в подписчике:', e)
    }
  })
}

function scheduleReconnect() {
  if (closedByUs) return

  const delay = Math.min(RECONNECT_BASE_MS * 2 ** attempt, RECONNECT_MAX_MS)
  attempt += 1
  wsStatus.value = 'reconnecting'

  reconnectTimer = setTimeout(() => {
    open()
  }, delay)
}

function open() {
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
    return
  }

  clearTimeout(reconnectTimer)
  wsStatus.value = 'connecting'

  try {
    socket = new WebSocket(buildUrl())
  } catch (e) {
    wsError.value = e.message
    scheduleReconnect()
    return
  }

  socket.onopen = () => {
    attempt = 0
    wsError.value = null
    wsStatus.value = 'connected'
    emit({ type: 'SOCKET_OPEN' })
  }

  socket.onmessage = (event) => {
    try {
      emit(JSON.parse(event.data))
    } catch (e) {
      console.error('[ws] не удалось разобрать сообщение:', e)
    }
  }

  socket.onerror = () => {
    wsError.value = 'Ошибка соединения'
  }

  socket.onclose = () => {
    socket = null
    if (closedByUs) {
      wsStatus.value = 'disconnected'
      return
    }
    scheduleReconnect()
  }
}

/** Открыть соединение. Повторные вызовы безопасны. */
export function connectWs() {
  closedByUs = false
  open()
}
export function disconnectWs() {
  closedByUs = true
  clearTimeout(reconnectTimer)
  if (socket) {
    socket.close()
    socket = null
  }
  wsStatus.value = 'disconnected'
}

/**
 * Подписка на сообщения. Возвращает функцию отписки —
 * её звать не обязательно, подписки живут столько же, сколько стор.
 */
export function onWsMessage(handler) {
  listeners.add(handler)
  return () => listeners.delete(handler)
}