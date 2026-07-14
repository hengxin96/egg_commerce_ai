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

test('task center shows retry only for failed tasks', () => {
  const page = readSource('./TasksPage.vue')

  assert.match(page, /retryStoryTask/)
  assert.match(page, /retryProductImageTask/)
  assert.match(page, /const canRetryTask = \(task\) => task\?\.status === 'failed'/)
  assert.match(page, /\u91cd\u8bd5/)
  assert.match(page, /retryTask\(row\)/)
  assert.match(page, /retryingTaskIds/)
})

test('task center only exposes downloads for completed story tasks', () => {
  const page = readSource('./TasksPage.vue')

  assert.match(page, /const canDownloadTask = \(task\) => task\?\.taskKind === TASK_KIND_STORY && task\?\.status === 'completed'/)
  assert.match(page, /v-if="canDownloadTask\(row\)"[^>]*@click="download\(row, 'docx'\)"/s)
  assert.match(page, /v-if="canDownloadTask\(row\)"[^>]*@click="download\(row, 'md'\)"/s)
  assert.match(page, /if \(!canDownloadTask\(task\)\)/)
  assert.match(page, /当前任务还在生成中，完成后才能下载/)
})

test('task center supports product image tasks without product downloads', () => {
  const page = readSource('./TasksPage.vue')

  assert.match(page, /listProductImageTasks/)
  assert.match(page, /getProductImageTask/)
  assert.match(page, /retryProductImageTask/)
  assert.match(page, /cancelProductImageTask/)
  assert.match(page, /TASK_KIND_PRODUCT_IMAGE = 'product_image'/)
  assert.match(page, /Promise\.allSettled/)
  assert.match(page, /canCancelTask/)
  assert.match(page, /已终止/)
  assert.match(page, /图片已删除/)
  assert.match(page, /productTaskSections/)
  assert.doesNotMatch(page, /downloadProductAsset/)
})
