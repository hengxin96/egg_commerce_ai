import { clearAuthSession, getAuthToken, isAuthFailure } from './auth.js'

const API_BASE = import.meta.env?.VITE_API_BASE_URL || '/api'

const DEFAULT_STREAM_TIMEOUT_MS = 180000

function createRequestError(payload, response, fallbackMessage) {
  const error = new Error(payload.msg || fallbackMessage)
  error.status = response.status
  error.code = payload.code

  // 后端重启或登录缓存失效时会返回 code=401，这时以前端登录态为准会继续反复失败。
  if (isAuthFailure(error)) {
    clearAuthSession()
    error.message = '登录已过期，请重新登录'
  }

  return error
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
    throw createRequestError(payload, response, `接口请求失败(${response.status})`)
  }

  const code = payload.code || 200
  if (code !== 200) {
    throw createRequestError(payload, response, '接口返回异常')
  }
  return payload.data
}

function authHeaders(options = {}) {
  const token = getAuthToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  return headers
}

function parseSseEvent(block) {
  let event = 'message'
  const dataLines = []

  block.split('\n').forEach((line) => {
    if (!line || line.startsWith(':')) {
      return
    }
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
      return
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).replace(/^ /, ''))
    }
  })

  return {
    event,
    data: dataLines.join('\n')
  }
}

function createSseParser({ onDelta, onDone, onError }) {
  let buffer = ''

  const handleBlock = (block) => {
    const { event, data } = parseSseEvent(block)
    if (event === 'delta') {
      onDelta?.(data)
      return
    }
    if (event === 'done' || event === 'message') {
      onDone?.(data ? JSON.parse(data) : {})
      return
    }
    if (event === 'error') {
      const payload = data ? JSON.parse(data) : {}
      onError?.(payload)
    }
  }

  return {
    push(chunk) {
      buffer += chunk.replace(/\r\n/g, '\n')
      let boundary = buffer.indexOf('\n\n')
      while (boundary >= 0) {
        const block = buffer.slice(0, boundary)
        buffer = buffer.slice(boundary + 2)
        handleBlock(block)
        boundary = buffer.indexOf('\n\n')
      }
    },
    flush() {
      if (buffer.trim()) {
        handleBlock(buffer)
      }
      buffer = ''
    }
  }
}

async function readStreamResponse(response, { onDelta } = {}) {
  let finalResponse = null
  let streamError = null
  const decoder = new TextDecoder()
  const parser = createSseParser({
    onDelta,
    onDone: (payload) => {
      finalResponse = payload
    },
    onError: (payload) => {
      streamError = createRequestError(payload, response, '流式回答失败')
    }
  })

  if (!response.body) {
    parser.push(await response.text())
    parser.flush()
  } else {
    const reader = response.body.getReader()
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }
      parser.push(decoder.decode(value, { stream: true }))
    }
    parser.push(decoder.decode())
    parser.flush()
  }

  if (streamError) {
    throw streamError
  }
  return finalResponse || {}
}

async function streamRequest(path, options = {}, streamOptions = {}) {
  const timeoutMs = Number(streamOptions.timeoutMs || DEFAULT_STREAM_TIMEOUT_MS)
  const controller = new AbortController()
  let timedOut = false
  const timeoutId = timeoutMs > 0
    ? globalThis.setTimeout(() => {
        timedOut = true
        controller.abort()
      }, timeoutMs)
    : null

  try {
    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: authHeaders(options),
      signal: controller.signal
    })
    const contentType = response.headers.get('content-type') || ''

    if (!response.ok || contentType.includes('application/json')) {
      const payload = await response.json().catch(() => ({}))
      throw createRequestError(payload, response, `接口请求失败(${response.status})`)
    }

    return await readStreamResponse(response, streamOptions)
  } catch (error) {
    if (timedOut || error?.name === 'AbortError') {
      throw new Error('回答超时，请稍后重试')
    }
    throw error
  } finally {
    if (timeoutId) {
      globalThis.clearTimeout(timeoutId)
    }
  }
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

export function deleteChatSession(sessionId) {
  return request(`/ai/chat/session/${encodeURIComponent(sessionId)}`, {
    method: 'DELETE'
  })
}

export function sendChatMessage(sessionId, content) {
  return request(`/ai/chat/session/${encodeURIComponent(sessionId)}/message`, {
    method: 'POST',
    body: JSON.stringify({ content })
  })
}

export function sendChatMessageStream(sessionId, content, { onDelta, timeoutMs } = {}) {
  return streamRequest(`/ai/chat/session/${encodeURIComponent(sessionId)}/message/stream`, {
    method: 'POST',
    body: JSON.stringify({ content })
  }, { onDelta, timeoutMs })
}
