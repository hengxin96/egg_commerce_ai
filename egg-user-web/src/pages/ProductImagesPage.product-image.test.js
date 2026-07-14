import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('product image page imports real API wrappers', () => {
  const page = readSource('./ProductImagesPage.vue')
  const workspace = readSource('../components/GeneratorWorkspace.vue')

  assert.match(page, /const PRODUCT_IMAGE_STEPS = \['输入', '分析中', '确认规划', '生成中', '完成'\]/)
  assert.match(page, /getProductDraft/)
  assert.match(page, /uploadDraftImage/)
  assert.match(page, /createProductImageTask/)
  assert.match(page, /cancelProductImageTask/)
  assert.match(page, /rewriteProductRequirement/)
  assert.match(page, /loadDraft/)
  assert.match(page, /handleUploadImage/)
  assert.match(page, /handleGenerate/)
  assert.match(page, /startPolling/)
  assert.match(page, /handleCancelTask/)
  assert.match(page, /showCompletionNotice/)
  assert.match(page, /rewriteDialogVisible/)
  assert.match(page, /handleRewriteRequirement/)
  assert.match(page, /rewriteResult\.value = result/)
  assert.match(page, /rewriteDialogVisible/)
  assert.match(page, /optimizedRequirement/)
  assert.match(page, /applyRewriteResult/)
  assert.match(page, /:prompt-value="workspacePrompt"/)
  assert.match(page, /workspaceResetKey/)
  assert.match(page, /:reset-key="workspaceResetKey"/)
  assert.match(page, /requestGeneration/)
  assert.match(page, /requestGeneration\.value \+= 1[\s\S]*await cancelProductImageTask/)
  assert.match(page, /generation !== requestGeneration\.value/)
  assert.match(page, /if \(isUploading\.value\)/)
  assert.match(page, /rewriteDialogVisible\.value = false/)
  assert.match(workspace, /resetKey/)
  assert.match(workspace, /resetWorkspaceForm/)
  assert.match(page, /draft\.value = null/)
})

test('shared workspace lets product page own uploaded image state', () => {
  const workspace = readSource('../components/GeneratorWorkspace.vue')

  assert.match(workspace, /uploaded-images/)
  assert.match(workspace, /@upload-image/)
  assert.match(workspace, /@remove-image/)
  assert.match(workspace, /@cancel/)
})
