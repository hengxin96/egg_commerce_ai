import { clearAuthSession, getAuthToken, isAuthFailure } from './auth.js'

const API_BASE = import.meta.env?.VITE_API_BASE_URL || import.meta.env?.VITE_API_BASE || '/api'

function createRequestError(payload, response, fallbackMessage) {
  const error = new Error(payload.msg || fallbackMessage)
  error.status = response.status
  error.code = payload.code

  // 后端依赖 Redis 中的 LoginUser 校验 token；失效时立即清理本地登录态，避免继续使用旧 token。
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

function authHeaders() {
  const token = getAuthToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

function decodeFileName(response, fallback) {
  const encoded = response.headers.get('download-filename')
  if (!encoded) {
    return fallback
  }

  try {
    return decodeURIComponent(encoded)
  } catch {
    return fallback
  }
}

export function createStoryTask(payload) {
  return request('/ai/story/tasks', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function listStoryTasks() {
  return request('/ai/story/tasks')
}

export function getStoryTask(taskId) {
  return request(`/ai/story/tasks/${encodeURIComponent(taskId)}`)
}

export function retryStoryTask(taskId) {
  return request(`/ai/story/tasks/${encodeURIComponent(taskId)}/retry`, {
    method: 'POST'
  })
}

export async function downloadStoryTask(taskId, format = 'docx') {
  const response = await fetch(`${API_BASE}/ai/story/tasks/${encodeURIComponent(taskId)}/export?format=${encodeURIComponent(format)}`, {
    headers: authHeaders()
  })
  const contentType = response.headers.get('content-type') || ''

  // 文件导出失败时后端会返回 JSON 错误；这里先识别错误响应，避免把 JSON 保存成损坏的 docx。
  if (!response.ok || contentType.includes('application/json')) {
    const payload = await response.json().catch(() => ({}))
    throw createRequestError(payload, response, `文件下载失败(${response.status})`)
  }

  const blob = await response.blob()
  const fileName = decodeFileName(response, `剧情生成结果.${format}`)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

export function listDictData(dictType) {
  return request(`/system/dict/data/type/${encodeURIComponent(dictType)}`)
}
