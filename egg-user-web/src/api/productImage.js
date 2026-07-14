import { clearAuthSession, getAuthToken, isAuthFailure } from './auth.js'

const API_BASE = import.meta.env?.VITE_API_BASE_URL || import.meta.env?.VITE_API_BASE || '/api'

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

export function createProductImageTask(payload) {
  return jsonRequest('/ai/product-image/tasks', 'POST', payload)
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
  const contentType = response.headers.get('content-type') || ''

  if (!response.ok || contentType.includes('application/json')) {
    const payload = await response.json().catch(() => ({}))
    throw createRequestError(payload, response, `图片下载失败(${response.status})`)
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
