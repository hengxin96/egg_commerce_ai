const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

function getAuthToken() {
  const keys = ['Admin-Token', 'Egg-Token', 'User-Token', 'token', 'Authorization']
  for (const key of keys) {
    const value = localStorage.getItem(key) || sessionStorage.getItem(key)
    if (value) {
      return value.replace(/^Bearer\s+/i, '')
    }
  }
  return ''
}

async function request(path, options = {}) {
  const token = getAuthToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  })
  const payload = await response.json().catch(() => ({}))

  if (!response.ok) {
    throw new Error(payload.msg || `接口请求失败(${response.status})`)
  }

  const code = payload.code || 200
  if (code !== 200) {
    throw new Error(payload.msg || '接口返回异常')
  }
  return payload.data
}

export function listChatSessions() {
  return request('/ai/chat/session/list')
}

export function createChatSession(title) {
  return request('/ai/chat/session', {
    method: 'POST',
    body: JSON.stringify({ title })
  })
}

export function listChatMessages(sessionId) {
  return request(`/ai/chat/session/${encodeURIComponent(sessionId)}/messages`)
}

export function sendChatMessage(sessionId, content) {
  return request(`/ai/chat/session/${encodeURIComponent(sessionId)}/message`, {
    method: 'POST',
    body: JSON.stringify({ content })
  })
}
