const API_BASE = import.meta.env?.VITE_API_BASE_URL || '/api'

export const AUTH_TOKEN_KEY = 'User-Token'
export const AUTH_USER_KEY = 'User-Profile'
export const AUTH_LAST_ACTIVE_KEY = 'User-Last-Active-At'
export const AUTH_STATE_EVENT = 'egg-auth-state-change'
export const AUTH_SESSION_TIMEOUT_MS = 30 * 60 * 1000

const LEGACY_TOKEN_KEYS = ['Egg-Token', 'token', 'Authorization']
const CLEAR_TOKEN_KEYS = [AUTH_TOKEN_KEY, ...LEGACY_TOKEN_KEYS]
const PHONE_PATTERN = /^\d{11}$/

export function isValidPhone(phonenumber) {
  return PHONE_PATTERN.test(String(phonenumber || '').trim())
}

export function isValidPassword(password) {
  const value = String(password || '')
  return value.length >= 5 && value.length <= 20
}

export function normalizeToken(token) {
  return String(token || '').replace(/^Bearer\s+/i, '').trim()
}

function readStorageValue(key) {
  const localValue = typeof localStorage !== 'undefined' ? localStorage.getItem(key) : ''
  if (localValue) {
    return localValue
  }

  return typeof sessionStorage !== 'undefined' ? sessionStorage.getItem(key) : ''
}

function writeLocalValue(key, value) {
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(key, String(value))
  }
}

function removeStorageValue(key) {
  if (typeof localStorage !== 'undefined') {
    localStorage.removeItem(key)
  }
  if (typeof sessionStorage !== 'undefined') {
    sessionStorage.removeItem(key)
  }
}

function notifyAuthStateChanged() {
  if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
    window.dispatchEvent(new CustomEvent(AUTH_STATE_EVENT))
  }
}

function getStoredAuthToken() {
  // 前台用户登录态只信任本模块写入的 User-Token，避免误把后台或浏览器遗留 token 当作前台身份。
  return normalizeToken(readStorageValue(AUTH_TOKEN_KEY))
}

function readLastActiveAt() {
  const value = Number(readStorageValue(AUTH_LAST_ACTIVE_KEY))
  return Number.isFinite(value) && value > 0 ? value : 0
}

function writeLastActiveAt(now = Date.now()) {
  writeLocalValue(AUTH_LAST_ACTIVE_KEY, now)
}

function normalizeAuthUser(authResult = {}) {
  const nickName = String(authResult.nickName || authResult.userName || '').trim()
  const userName = String(authResult.userName || nickName).trim()
  const phonenumber = String(authResult.phonenumber || '').trim()

  return { userName, nickName, phonenumber }
}

export function getAuthUser() {
  const raw = readStorageValue(AUTH_USER_KEY)
  if (!raw) {
    return null
  }

  try {
    const user = JSON.parse(raw)
    return user && typeof user === 'object' ? user : null
  } catch {
    return null
  }
}

export function getAuthSession(now = Date.now()) {
  const token = getStoredAuthToken()
  if (!token) {
    return null
  }

  const lastActiveAt = readLastActiveAt()
  if (lastActiveAt && now - lastActiveAt >= AUTH_SESSION_TIMEOUT_MS) {
    clearAuthSession({ notify: false })
    return null
  }

  const effectiveLastActiveAt = lastActiveAt || now
  if (!lastActiveAt) {
    writeLastActiveAt(effectiveLastActiveAt)
  }

  return {
    token,
    user: getAuthUser(),
    lastActiveAt: effectiveLastActiveAt,
    expiresAt: effectiveLastActiveAt + AUTH_SESSION_TIMEOUT_MS
  }
}

export function getAuthToken(now = Date.now()) {
  return getAuthSession(now)?.token || ''
}

export function saveAuthToken(token, now = Date.now()) {
  if (typeof localStorage === 'undefined') {
    return
  }

  localStorage.setItem(AUTH_TOKEN_KEY, normalizeToken(token))
  writeLastActiveAt(now)
  notifyAuthStateChanged()
}

export function saveAuthUser(authResult) {
  if (typeof localStorage === 'undefined') {
    return
  }

  const user = normalizeAuthUser(authResult)
  if (!user.userName && !user.nickName && !user.phonenumber) {
    return
  }

  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
  notifyAuthStateChanged()
}

export function saveAuthSession(authResult, now = Date.now()) {
  if (typeof localStorage === 'undefined') {
    return
  }

  const token = normalizeToken(authResult?.token)
  if (!token) {
    return
  }

  localStorage.setItem(AUTH_TOKEN_KEY, token)
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(normalizeAuthUser(authResult)))
  writeLastActiveAt(now)
  notifyAuthStateChanged()
}

export function clearAuthSession({ notify = true } = {}) {
  CLEAR_TOKEN_KEYS.forEach((key) => removeStorageValue(key))
  removeStorageValue(AUTH_USER_KEY)
  removeStorageValue(AUTH_LAST_ACTIVE_KEY)
  if (notify) {
    notifyAuthStateChanged()
  }
}

export const clearAuthToken = clearAuthSession

export function touchAuthActivity(now = Date.now()) {
  const token = getStoredAuthToken()
  if (!token) {
    return false
  }

  const lastActiveAt = readLastActiveAt()
  if (lastActiveAt && now - lastActiveAt >= AUTH_SESSION_TIMEOUT_MS) {
    clearAuthSession({ notify: false })
    return false
  }

  writeLastActiveAt(now)
  notifyAuthStateChanged()
  return true
}

export function isLoggedIn(now = Date.now()) {
  return Boolean(getAuthSession(now))
}

export function isAuthFailure(error) {
  return [error?.status, error?.code].some((value) => Number(value) === 401)
}

export function onAuthStateChange(callback) {
  if (typeof window === 'undefined' || typeof window.addEventListener !== 'function') {
    return () => {}
  }

  const handler = () => callback(getAuthSession())
  window.addEventListener(AUTH_STATE_EVENT, handler)
  window.addEventListener('storage', handler)
  return () => {
    window.removeEventListener(AUTH_STATE_EVENT, handler)
    window.removeEventListener('storage', handler)
  }
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    }
  })
  const payload = await response.json().catch(() => ({}))

  if (!response.ok) {
    const error = new Error(payload.msg || `接口请求失败(${response.status})`)
    error.status = response.status
    throw error
  }

  const code = payload.code || 200
  if (code !== 200) {
    const error = new Error(payload.msg || '接口返回异常')
    error.code = code
    throw error
  }
  return payload
}

async function authRequest(path, options = {}) {
  const token = getAuthToken()
  return request(path, {
    ...options,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    }
  })
}

export function loginOrRegister({ phonenumber, password }) {
  return request('/user/auth/login-or-register', {
    method: 'POST',
    body: JSON.stringify({ phonenumber, password })
  })
}

export function resetPassword({ phonenumber, password, confirmPassword }) {
  return request('/user/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({ phonenumber, password, confirmPassword })
  })
}

export function fetchAuthProfile() {
  return authRequest('/user/auth/profile')
}
