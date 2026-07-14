import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('product image page imports real API wrappers', () => {
  const page = readSource('./ProductImagesPage.vue')

  assert.match(page, /getProductDraft/)
  assert.match(page, /uploadDraftImage/)
  assert.match(page, /createProductImageTask/)
  assert.match(page, /cancelProductImageTask/)
  assert.match(page, /rewriteProductRequirement/)
})

test('shared workspace lets product page own uploaded image state', () => {
  const workspace = readSource('../components/GeneratorWorkspace.vue')

  assert.match(workspace, /uploaded-images/)
  assert.match(workspace, /@upload-image/)
  assert.match(workspace, /@remove-image/)
  assert.match(workspace, /@cancel/)
})
