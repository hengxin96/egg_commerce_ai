import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('product image page imports real API wrappers', () => {
  const page = readSource('./ProductImagesPage.vue')
  const workspace = readSource('../components/GeneratorWorkspace.vue')

  assert.match(page, /getProductDraft/)
  assert.match(page, /uploadDraftImage/)
  assert.match(page, /createProductImageTask/)
  assert.match(page, /cancelProductImageTask/)
  assert.match(page, /rewriteProductRequirement/)
  assert.match(page, /rewriteResult\.value = await rewriteProductRequirement/)
  assert.match(page, /rewriteDialogVisible/)
  assert.match(page, /optimizedRequirement/)
  assert.match(page, /applyRewriteResult/)
  assert.match(page, /:prompt-value="workspacePrompt"/)
  assert.match(page, /workspaceResetKey/)
  assert.match(page, /:reset-key="workspaceResetKey"/)
  assert.match(page, /rewriteDialogVisible\.value = false/)
  assert.match(workspace, /resetKey/)
  assert.match(workspace, /resetWorkspaceForm/)
  assert.match(page, /currentDraft\.value = null/)
})

test('shared workspace lets product page own uploaded image state', () => {
  const workspace = readSource('../components/GeneratorWorkspace.vue')

  assert.match(workspace, /uploaded-images/)
  assert.match(workspace, /@upload-image/)
  assert.match(workspace, /@remove-image/)
  assert.match(workspace, /@cancel/)
})
