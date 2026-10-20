import assert from 'node:assert/strict'
import { test } from 'node:test'

import { AUTH_LAST_ACTIVE_KEY, AUTH_TOKEN_KEY } from './auth.js'
import { deleteChatSession, listChatSessions, sendChatMessageStream } from './chat.js'

test('listChatSessions clears stale frontend session when backend rejects authentication', async () => {
  const previousFetch = globalThis.fetch
  const previousLocalStorage = globalThis.localStorage
  const previousSessionStorage = globalThis.sessionStorage

  globalThis.localStorage = new MapStorage()
  globalThis.sessionStorage = new MapStorage()
  localStorage.setItem(AUTH_TOKEN_KEY, 'stale-token')
  localStorage.setItem(AUTH_LAST_ACTIVE_KEY, String(Date.now()))

  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/ai/chat/session/list')
    assert.equal(options.headers.Authorization, 'Bearer stale-token')
    return new Response(JSON.stringify({
      code: 401,
      msg: '请求访问：/ai/chat/session/list，认证失败，无法访问系统资源'
    }), {
      status: 200,
      headers: {
        'content-type': 'application/json'
      }
    })
  }

  try {
    await assert.rejects(() => listChatSessions(), /登录已过期/)
    assert.equal(localStorage.getItem(AUTH_TOKEN_KEY), null)
    assert.equal(localStorage.getItem(AUTH_LAST_ACTIVE_KEY), null)
  } finally {
    globalThis.fetch = previousFetch
    globalThis.localStorage = previousLocalStorage
    globalThis.sessionStorage = previousSessionStorage
  }
})

test('sendChatMessageStream reads delta events and returns final response', async () => {
  const previousFetch = globalThis.fetch
  const previousLocalStorage = globalThis.localStorage
  const previousSessionStorage = globalThis.sessionStorage

  globalThis.localStorage = new MapStorage()
  globalThis.sessionStorage = new MapStorage()
  localStorage.setItem(AUTH_TOKEN_KEY, 'valid-token')
  localStorage.setItem(AUTH_LAST_ACTIVE_KEY, String(Date.now()))

  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/ai/chat/session/session-1/message/stream')
    assert.equal(options.method, 'POST')
    assert.equal(options.headers.Authorization, 'Bearer valid-token')
    assert.equal(options.body, JSON.stringify({ content: '平台规则是什么？' }))

    return new Response([
      'event: delta\n',
      'data: 你好\n\n',
      'event: delta\n',
      'data: ，这是整理后的回答。\n\n',
      'event: done\n',
      'data: {"sessionId":"session-1","messageId":"message-1","answer":"你好，这是整理后的回答。","sources":[{"sourceTitle":"规则文档"}]}\n\n'
    ].join(''), {
      status: 200,
      headers: {
        'content-type': 'text/event-stream'
      }
    })
  }

  const deltas = []

  try {
    const response = await sendChatMessageStream('session-1', '平台规则是什么？', {
      onDelta: (chunk) => deltas.push(chunk)
    })

    assert.deepEqual(deltas, ['你好', '，这是整理后的回答。'])
    assert.equal(response.answer, '你好，这是整理后的回答。')
    assert.equal(response.sources[0].sourceTitle, '规则文档')
  } finally {
    globalThis.fetch = previousFetch
    globalThis.localStorage = previousLocalStorage
    globalThis.sessionStorage = previousSessionStorage
  }
})

test('sendChatMessageStream rejects stalled streams after the configured timeout', async () => {
  const previousFetch = globalThis.fetch
  const previousLocalStorage = globalThis.localStorage
  const previousSessionStorage = globalThis.sessionStorage

  globalThis.localStorage = new MapStorage()
  globalThis.sessionStorage = new MapStorage()
  localStorage.setItem(AUTH_TOKEN_KEY, 'valid-token')
  localStorage.setItem(AUTH_LAST_ACTIVE_KEY, String(Date.now()))

  let aborted = false
  globalThis.fetch = async (_url, options) => new Promise((_resolve, reject) => {
    options.signal?.addEventListener('abort', () => {
      aborted = true
      const error = new Error('aborted')
      error.name = 'AbortError'
      reject(error)
    })
  })

  try {
    await assert.rejects(
      () => Promise.race([
        sendChatMessageStream('session-1', 'hello', { timeoutMs: 10 }),
        new Promise((_, reject) => setTimeout(() => reject(new Error('stream did not timeout')), 80))
      ]),
      /回答超时/
    )
    assert.equal(aborted, true)
  } finally {
    globalThis.fetch = previousFetch
    globalThis.localStorage = previousLocalStorage
    globalThis.sessionStorage = previousSessionStorage
  }
})

test('deleteChatSession sends authenticated delete request', async () => {
  const previousFetch = globalThis.fetch
  const previousLocalStorage = globalThis.localStorage
  const previousSessionStorage = globalThis.sessionStorage

  globalThis.localStorage = new MapStorage()
  globalThis.sessionStorage = new MapStorage()
  localStorage.setItem(AUTH_TOKEN_KEY, 'valid-token')
  localStorage.setItem(AUTH_LAST_ACTIVE_KEY, String(Date.now()))

  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/ai/chat/session/session-1')
    assert.equal(options.method, 'DELETE')
    assert.equal(options.headers.Authorization, 'Bearer valid-token')
    return new Response(JSON.stringify({
      code: 200,
      data: true
    }), {
      status: 200,
      headers: {
        'content-type': 'application/json'
      }
    })
  }

  try {
    const result = await deleteChatSession('session-1')
    assert.equal(result, true)
  } finally {
    globalThis.fetch = previousFetch
    globalThis.localStorage = previousLocalStorage
    globalThis.sessionStorage = previousSessionStorage
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
