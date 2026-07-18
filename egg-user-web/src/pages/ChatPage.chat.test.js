import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'

const source = readFileSync(new URL('./ChatPage.vue', import.meta.url), 'utf8')

test('chat page keeps the composer fixed while messages scroll inside the panel', () => {
  assert.match(source, /\.chat-workbench\s*{[^}]*height:\s*calc\(100vh/s)
  assert.match(source, /\.conversation-main\s*{[^}]*overflow:\s*hidden/s)
  assert.match(source, /\.chat-messages\s*{[^}]*overflow:\s*auto/s)
  assert.match(source, /\.session-sidebar\s*{[^}]*overflow:\s*hidden/s)
  assert.match(source, /\.session-list\s*{[^}]*overflow:\s*auto/s)
})

test('chat page streams assistant answers and hides internal retrieval details', () => {
  assert.match(source, /sendChatMessageStream/)
  assert.doesNotMatch(source, /切片/)
  assert.doesNotMatch(source, /相似度/)
  assert.doesNotMatch(source, /sourceSnippet/)
})

test('chat page reuses empty sessions and exposes confirmed deletion', () => {
  assert.match(source, /deleteChatSession/)
  assert.match(source, /ElMessageBox\.confirm/)
  assert.match(source, /session-delete-button/)
  assert.match(source, /findEmptySession/)
  assert.match(source, /messageCount/)
  assert.doesNotMatch(source, /if \(sessions\.value\.length === 0\)\s*{\s*await handleNewSession\(\)/)
})

test('chat page shows a friendly assistant greeting for a newly created empty session', () => {
  assert.match(source, /WELCOME_MESSAGE/)
  assert.match(source, /displayMessages/)
  assert.match(source, /你好！我是“龙蛋 AI”的智能知识库问答助手。/)
  assert.match(source, /您可以向我询问平台规则、商品内容规范、审核标准、风险排查或是文案生成建议等相关问题。/)
  assert.match(source, /v-for="message in displayMessages"/)
})
