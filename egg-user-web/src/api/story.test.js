import assert from 'node:assert/strict'
import { test } from 'node:test'

import { AUTH_LAST_ACTIVE_KEY, AUTH_TOKEN_KEY } from './auth.js'
import { createStoryTask, downloadStoryTask, retryStoryTask } from './story.js'

test('downloadStoryTask surfaces json backend errors instead of saving corrupt files', async () => {
  const previousFetch = globalThis.fetch
  globalThis.fetch = async () => new Response(JSON.stringify({
    code: 500,
    msg: 'Word 导出失败'
  }), {
    status: 200,
    headers: {
      'content-type': 'application/json'
    }
  })

  try {
    await assert.rejects(() => downloadStoryTask('task-1', 'docx'), /Word 导出失败/)
  } finally {
    globalThis.fetch = previousFetch
  }
})

test('createStoryTask clears stale frontend session when backend rejects authentication', async () => {
  const previousFetch = globalThis.fetch
  const previousLocalStorage = globalThis.localStorage
  const previousSessionStorage = globalThis.sessionStorage

  globalThis.localStorage = new MapStorage()
  globalThis.sessionStorage = new MapStorage()
  localStorage.setItem(AUTH_TOKEN_KEY, 'stale-token')
  localStorage.setItem(AUTH_LAST_ACTIVE_KEY, String(Date.now()))

  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/ai/story/tasks')
    assert.equal(options.headers.Authorization, 'Bearer stale-token')
    return new Response(JSON.stringify({
      code: 401,
      msg: '请求访问：/ai/story/tasks，认证失败，无法访问系统资源'
    }), {
      status: 200,
      headers: {
        'content-type': 'application/json'
      }
    })
  }

  try {
    await assert.rejects(() => createStoryTask({ storyTheme: '主题' }), /登录已过期/)
    assert.equal(localStorage.getItem(AUTH_TOKEN_KEY), null)
    assert.equal(localStorage.getItem(AUTH_LAST_ACTIVE_KEY), null)
  } finally {
    globalThis.fetch = previousFetch
    globalThis.localStorage = previousLocalStorage
    globalThis.sessionStorage = previousSessionStorage
  }
})

test('retryStoryTask posts to the story retry endpoint', async () => {
  const previousFetch = globalThis.fetch
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/ai/story/tasks/task-1/retry')
    assert.equal(options.method, 'POST')
    return new Response(JSON.stringify({
      code: 200,
      data: { id: 'task-1', status: 'processing' }
    }), {
      status: 200,
      headers: {
        'content-type': 'application/json'
      }
    })
  }

  try {
    const task = await retryStoryTask('task-1')
    assert.equal(task.status, 'processing')
  } finally {
    globalThis.fetch = previousFetch
  }
})

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
