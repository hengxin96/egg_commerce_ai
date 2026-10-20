import test from 'node:test'
import assert from 'node:assert/strict'
import {
  AUTH_LAST_ACTIVE_KEY,
  AUTH_SESSION_TIMEOUT_MS,
  AUTH_TOKEN_KEY,
  AUTH_USER_KEY,
  clearAuthToken,
  fetchAuthProfile,
  getAuthSession,
  getAuthToken,
  getAuthUser,
  isLoggedIn,
  isAuthFailure,
  isValidPassword,
  isValidPhone,
  saveAuthSession,
  saveAuthToken,
  saveAuthUser,
  touchAuthActivity
} from './auth.js'

test('auth helpers validate first-version phone and password rules', () => {
  assert.equal(isValidPhone('13800138000'), true)
  assert.equal(isValidPhone('1380013800'), false)
  assert.equal(isValidPhone('138001380000'), false)
  assert.equal(isValidPassword('12345'), true)
  assert.equal(isValidPassword('1234'), false)
  assert.equal(isValidPassword('123456789012345678901'), false)
})

test('auth helpers store and clear the frontend user token', () => {
  installStorage()

  clearAuthToken()
  assert.equal(isLoggedIn(), false)

  saveAuthToken('Bearer front-token', 1000)
  assert.equal(localStorage.getItem(AUTH_TOKEN_KEY), 'front-token')
  assert.equal(getAuthToken(1000), 'front-token')
  assert.equal(isLoggedIn(1000), true)

  clearAuthToken()
  assert.equal(getAuthToken(), '')
  assert.equal(isLoggedIn(), false)
})

test('auth helpers only trust the frontend user token for login state', () => {
  installStorage()

  localStorage.setItem('Egg-Token', 'admin-token')
  localStorage.setItem('token', 'legacy-token')
  localStorage.setItem('Authorization', 'Bearer old-token')

  assert.equal(getAuthToken(), '')
  assert.equal(isLoggedIn(), false)

  clearAuthToken()
  assert.equal(localStorage.getItem('Egg-Token'), null)
  assert.equal(localStorage.getItem('token'), null)
  assert.equal(localStorage.getItem('Authorization'), null)
})

test('auth session stores current user profile and expires after thirty idle minutes', () => {
  installStorage()

  saveAuthSession({
    token: 'Bearer front-token',
    userName: 'egg_alpha',
    nickName: 'egg_alpha',
    phonenumber: '13800138000'
  }, 1000)

  assert.equal(AUTH_SESSION_TIMEOUT_MS, 30 * 60 * 1000)
  assert.equal(localStorage.getItem(AUTH_TOKEN_KEY), 'front-token')
  assert.equal(localStorage.getItem(AUTH_LAST_ACTIVE_KEY), '1000')
  assert.deepEqual(getAuthUser(), {
    userName: 'egg_alpha',
    nickName: 'egg_alpha',
    phonenumber: '13800138000'
  })
  assert.equal(getAuthSession(1000 + AUTH_SESSION_TIMEOUT_MS - 1).token, 'front-token')

  assert.equal(isLoggedIn(1000 + AUTH_SESSION_TIMEOUT_MS), false)
  assert.equal(localStorage.getItem(AUTH_TOKEN_KEY), null)
  assert.equal(localStorage.getItem(AUTH_USER_KEY), null)
})

test('auth session refreshes the idle deadline when the user operates the app', () => {
  installStorage()

  saveAuthSession({
    token: 'front-token',
    nickName: 'egg_active',
    phonenumber: '13800138001'
  }, 1000)

  const touchTime = 1000 + AUTH_SESSION_TIMEOUT_MS - 1
  assert.equal(touchAuthActivity(touchTime), true)
  assert.equal(localStorage.getItem(AUTH_LAST_ACTIVE_KEY), String(touchTime))
  assert.equal(isLoggedIn(touchTime + AUTH_SESSION_TIMEOUT_MS - 1), true)
  assert.equal(isLoggedIn(touchTime + AUTH_SESSION_TIMEOUT_MS), false)
})

test('auth helpers can backfill the current user profile for an existing token', async () => {
  installStorage()
  saveAuthToken('front-token', Date.now())

  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/user/auth/profile')
    assert.equal(options.headers.Authorization, 'Bearer front-token')
    return {
      ok: true,
      json: async () => ({
        code: 200,
        userName: 'egg_profile',
        nickName: 'egg_profile',
        phonenumber: '13800138002'
      })
    }
  }

  const profile = await fetchAuthProfile()
  saveAuthUser(profile)

  assert.deepEqual(getAuthUser(), {
    userName: 'egg_profile',
    nickName: 'egg_profile',
    phonenumber: '13800138002'
  })
  delete globalThis.fetch
})

test('auth helpers detect backend authentication failures', () => {
  assert.equal(isAuthFailure({ status: 401 }), true)
  assert.equal(isAuthFailure({ code: 401 }), true)
  assert.equal(isAuthFailure({ status: 500 }), false)
})

function installStorage() {
  globalThis.localStorage = new MapStorage()
  globalThis.sessionStorage = new MapStorage()
  delete globalThis.fetch
}

class MapStorage {
  constructor() {
    this.items = new Map()
  }

  getItem(key) {
    return this.items.get(key) || null
  }

  setItem(key, value) {
    this.items.set(key, String(value))
  }

  removeItem(key) {
    this.items.delete(key)
  }
}
