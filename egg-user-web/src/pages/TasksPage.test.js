import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('task center opens generated content in a dialog without download actions inside the detail', () => {
  const page = readSource('./TasksPage.vue')

  assert.match(page, /\u4e0b\u8f7d\u4e3aWord/)
  assert.match(page, /\u4e0b\u8f7d\u4e3aMD/)
  assert.match(page, /<el-dialog/)
  assert.match(page, /v-model="isDetailDialogVisible"/)
  assert.match(page, /class="story-task-detail-dialog"/)
  assert.match(page, /story-task-sections/)
  assert.doesNotMatch(page, /task-detail-actions/)
  assert.doesNotMatch(page, /\u4e0b\u8f7d Word/)
  assert.doesNotMatch(page, /\u4e0b\u8f7d Markdown/)
})

test('task center shows retry only for failed story tasks', () => {
  const page = readSource('./TasksPage.vue')

  assert.match(page, /retryStoryTask/)
  assert.match(page, /row\.status === 'failed'/)
  assert.match(page, /\u91cd\u8bd5/)
  assert.match(page, /retryTask\(row\)/)
  assert.match(page, /retryingTaskIds/)
})
