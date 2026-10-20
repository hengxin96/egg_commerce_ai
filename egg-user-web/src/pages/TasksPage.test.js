import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('task center opens generated content in a dialog without story download actions inside the detail', () => {
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

test('task center keeps every node output inside fixed scroll panes', () => {
  const page = readSource('./TasksPage.vue')

  assert.match(page, /\.story-task-detail-dialog :deep\(\.el-dialog__body\)\s*\{[^}]*overflow-y:\s*auto/s)
  assert.doesNotMatch(page, /\.story-task-sections\s*\{[^}]*overflow-y:\s*auto/s)
  assert.match(page, /\.story-task-sections \.result-section pre\s*\{[^}]*max-height:\s*260px/s)
  assert.match(page, /\.story-task-sections \.result-section pre\s*\{[^}]*overflow-y:\s*auto/s)
})

test('task center hides generated product image url json and offers selectable image downloads in detail', () => {
  const page = readSource('./TasksPage.vue')

  assert.match(page, /SelectableImageGallery/)
  assert.match(page, /View/)
  assert.match(page, /class="task-result-preview"/)
  assert.match(page, /\.task-result-preview\s*\{[\s\S]*border-radius:\s*999px;/)
  assert.match(page, /\.task-result-preview\s*\{[\s\S]*color:\s*#fff;/)
  assert.match(page, /\.task-result-preview\s*\{[\s\S]*background:\s*rgba\(17,\s*18,\s*23,\s*0\.72\);/)
  assert.match(page, /downloadProductAssetImage/)
  assert.match(page, /downloadProductAssetImagesZip/)
  assert.match(page, /selectedTaskImageIds/)
  assert.match(page, /downloadSelectedProductImages\('normal'\)/)
  assert.match(page, /downloadSelectedProductImages\('zip'\)/)
  assert.match(page, /:disabled="!selectedTaskImageIds\.length"/)
  assert.match(page, /showContent !== false/)
  assert.match(page, /const hideGenerationJson = Boolean\(images\.length \|\| imageUrl\)/)
  assert.match(page, /content:\s*hideGenerationJson \? '' : jsonText\(task\.generationOutput\)/s)
  assert.match(page, /\u666e\u901a\u4e0b\u8f7d/)
  assert.match(page, /ZIP\u4e0b\u8f7d/)
  assert.match(page, /audit:\s*imageAudit\(image,\s*generation\.auditSummary\)/)
  assert.match(page, /auditSummary/)
})

test('task center shows retry only for failed tasks', () => {
  const page = readSource('./TasksPage.vue')

  assert.match(page, /retryStoryTask/)
  assert.match(page, /retryProductImageTask/)
  assert.match(page, /const canRetryTask = \(task\) => task\?\.status === 'failed'/)
  assert.match(page, /\u91cd\u8bd5/)
  assert.match(page, /retryTask\(row\)/)
  assert.match(page, /retryingTaskIds/)
  assert.match(page, /剧本生成/)
  assert.doesNotMatch(page, /脚本生成/)
})

test('task center only exposes downloads for completed story tasks', () => {
  const page = readSource('./TasksPage.vue')

  assert.match(page, /const canDownloadTask = \(task\) => task\?\.taskKind === TASK_KIND_STORY && task\?\.status === 'completed'/)
  assert.match(page, /v-if="canDownloadTask\(row\)"[^>]*@click="download\(row, 'docx'\)"/s)
  assert.match(page, /v-if="canDownloadTask\(row\)"[^>]*@click="download\(row, 'md'\)"/s)
  assert.match(page, /if \(!canDownloadTask\(task\)\)/)
  assert.match(page, /当前任务还在生成中，完成后才能下载/)
})

test('task center supports product image tasks without list-level product downloads', () => {
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
  assert.doesNotMatch(page, /canDownloadTask = \(task\) => task\?\.taskKind === TASK_KIND_PRODUCT_IMAGE/s)
})

test('task center reuses product image plan editor for product planning confirmation', () => {
  const page = readSource('./TasksPage.vue')

  assert.match(page, /ProductImagePlanEditor/)
  assert.match(page, /confirmProductImageTask/)
  assert.match(page, /replanProductImageTask/)
  assert.match(page, /type:\s*'plan-confirmation'/)
  assert.match(page, /:analysis-output="section\.analysisOutput"/)
  assert.match(page, /:planning-output="section\.planningOutput"/)
  assert.match(page, /:readonly="section\.readonly"/)
  assert.match(page, /@confirm="handleConfirmSelectedPlan"/)
  assert.match(page, /@replan="handleReplanSelectedTask"/)
  assert.match(page, /handleConfirmSelectedPlan/)
  assert.match(page, /handleReplanSelectedTask/)
  assert.match(page, /task\.status !== 'waiting_confirmation'/)
  assert.doesNotMatch(page, /\{\s*title:\s*'AI [^']*',\s*content:\s*jsonText\(task\.analysisOutput\)/)
})

test('task center shows a direct notice after retried product planning becomes confirmable', () => {
  const page = readSource('./TasksPage.vue')

  assert.match(page, /retryPlanNotice/)
  assert.match(page, /startRetryPlanningNoticePoll/)
  assert.match(page, /stopRetryPlanningNoticePoll/)
  assert.match(page, /hasPlanningOutput/)
  assert.match(page, /viewRetryPlanningNotice/)
  assert.match(page, /getProductImageTask\(taskId\)/)
  assert.match(page, /status === 'waiting_confirmation'/)
  assert.match(page, /retryPlanNotice\.value = \{/)
  assert.match(page, /@click="viewRetryPlanningNotice"/)
  assert.match(page, /\u91cd\u8bd5\u4efb\u52a1\u5df2\u89c4\u5212\u6574\u4f53\u8bbe\u8ba1\u89c4\u8303/)
})

test('task center refreshes the list and open detail automatically', () => {
  const page = readSource('./TasksPage.vue')

  assert.match(page, /TASK_LIST_POLL_INTERVAL_MS/)
  assert.match(page, /startTaskListPolling/)
  assert.match(page, /stopTaskListPolling/)
  assert.match(page, /taskListPollTimer/)
  assert.match(page, /window\.setInterval/)
  assert.match(page, /loadTasks\(\{ silent: true \}\)/)
  assert.match(page, /refreshVisibleTaskDetail/)
  assert.match(page, /isDetailDialogVisible\.value/)
  assert.match(page, /shouldPollVisibleTaskDetail/)
  assert.match(page, /selectedTask\.value\?\.status === 'processing'/)
  assert.match(page, /if \(!shouldPollVisibleTaskDetail\(\)\) \{[\s\S]*return/)
  assert.match(page, /onMounted\(\(\) => \{[\s\S]*startTaskListPolling\(\)/)
  assert.match(page, /onBeforeUnmount\(\(\) => \{[\s\S]*stopTaskListPolling\(\)/)
})
