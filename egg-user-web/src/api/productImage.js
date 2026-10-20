import { clearAuthSession, getAuthToken, isAuthFailure } from './auth.js'

const API_BASE = import.meta.env?.VITE_API_BASE_URL || import.meta.env?.VITE_API_BASE || '/api'
const DEFAULT_STREAM_TIMEOUT_MS = 180000

function createRequestError(payload, response, fallbackMessage) {
  const error = new Error(payload.msg || fallbackMessage)
  error.status = response.status
  error.code = payload.code

  if (isAuthFailure(error)) {
    clearAuthSession()
    error.message = '登录已过期，请重新登录'
  }

  return error
}

function authHeaders(options = {}) {
  const token = getAuthToken()
  const headers = { ...(options.headers || {}) }

  if (options.json !== false) {
    headers['Content-Type'] = 'application/json'
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  return headers
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: authHeaders(options)
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
      onError?.(data ? JSON.parse(data) : {})
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
      streamError = createRequestError(payload, response, 'AI 帮写失败')
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
      throw new Error('AI 帮写超时，请稍后重试')
    }
    throw error
  } finally {
    if (timeoutId) {
      globalThis.clearTimeout(timeoutId)
    }
  }
}

function jsonRequest(path, method, payload) {
  return request(path, {
    method,
    body: JSON.stringify(payload || {})
  })
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

export function getProductDraft() {
  return request('/ai/product-assets/draft')
}

export function uploadDraftImage(file) {
  const formData = new FormData()
  formData.append('file', file)

  return request('/ai/product-assets/draft/images', {
    method: 'POST',
    body: formData,
    json: false
  })
}

export function deleteDraftImage(imageId) {
  return request(`/ai/product-assets/draft/images/${encodeURIComponent(imageId)}`, {
    method: 'DELETE'
  })
}

export function clearProductDraft() {
  return request('/ai/product-assets/draft', {
    method: 'DELETE'
  })
}

export function updateDraftImageOrder(payload) {
  return jsonRequest('/ai/product-assets/draft/images/order', 'PUT', payload)
}

export function createProductImageTask(payload) {
  return jsonRequest('/ai/product-image/tasks', 'POST', payload)
}

export function confirmProductImageTask(taskId, payload = {}) {
  return jsonRequest(`/ai/product-image/tasks/${encodeURIComponent(taskId)}/confirm`, 'POST', payload)
}

export function replanProductImageTask(taskId, payload = {}) {
  return jsonRequest(`/ai/product-image/tasks/${encodeURIComponent(taskId)}/replan`, 'POST', payload)
}

export function getProductImageTask(taskId) {
  return request(`/ai/product-image/tasks/${encodeURIComponent(taskId)}`)
}

export function listProductImageTasks() {
  return request('/ai/product-image/tasks')
}

export function retryProductImageTask(taskId) {
  return request(`/ai/product-image/tasks/${encodeURIComponent(taskId)}/retry`, {
    method: 'POST'
  })
}

export function cancelProductImageTask(taskId) {
  return request(`/ai/product-image/tasks/${encodeURIComponent(taskId)}/cancel`, {
    method: 'POST'
  })
}

export function rewriteProductRequirement(payload) {
  return jsonRequest('/ai/product-image/requirement/rewrite', 'POST', payload)
}

export function rewriteProductRequirementStream(payload, { onDelta, timeoutMs } = {}) {
  return streamRequest('/ai/product-image/requirement/rewrite/stream', {
    method: 'POST',
    body: JSON.stringify(payload || {})
  }, { onDelta, timeoutMs })
}

export function listProductAssets() {
  return request('/ai/product-assets')
}

export function getProductAsset(assetId) {
  return request(`/ai/product-assets/${encodeURIComponent(assetId)}`)
}

export function renameProductAsset(assetId, name) {
  return jsonRequest(`/ai/product-assets/${encodeURIComponent(assetId)}/name`, 'PUT', { name })
}

export function reuseProductAsset(assetId) {
  return request(`/ai/product-assets/${encodeURIComponent(assetId)}/reuse`, {
    method: 'POST'
  })
}

export function deleteProductAsset(assetId) {
  return request(`/ai/product-assets/${encodeURIComponent(assetId)}`, {
    method: 'DELETE'
  })
}

export async function downloadProductAsset(assetId, fallbackName = '商品图.png') {
  const response = await fetch(`${API_BASE}/ai/product-assets/${encodeURIComponent(assetId)}/download`, {
    headers: authHeaders({ json: false })
  })
  await downloadBlobResponse(response, fallbackName, `图片下载失败(${response.status})`)
}

export async function downloadProductAssetImage(imageId, fallbackName = '商品图.png') {
  const response = await fetch(`${API_BASE}/ai/product-assets/images/${encodeURIComponent(imageId)}/download`, {
    headers: authHeaders({ json: false })
  })
  await downloadBlobResponse(response, fallbackName, `图片下载失败(${response.status})`)
}

export async function downloadProductAssetImagesZip(imageIds, fallbackName = '商品图.zip') {
  const response = await fetch(`${API_BASE}/ai/product-assets/images/download-zip`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ imageIds: imageIds || [] })
  })
  await downloadBlobResponse(response, fallbackName, `ZIP下载失败(${response.status})`)
}

async function downloadBlobResponse(response, fallbackName, fallbackMessage) {
  const contentType = response.headers.get('content-type') || ''

  if (!response.ok || contentType.includes('application/json')) {
    const payload = await response.json().catch(() => ({}))
    throw createRequestError(payload, response, fallbackMessage)
  }

  const blob = await response.blob()
  const fileName = decodeFileName(response, fallbackName)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
