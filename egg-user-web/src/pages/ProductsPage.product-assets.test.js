import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('product asset list uses real product asset APIs and actions', () => {
  const page = readSource('./ProductsPage.vue')

  assert.match(page, /listProductAssets/)
  assert.match(page, /renameProductAsset/)
  assert.match(page, /reuseProductAsset/)
  assert.match(page, /downloadProductAsset/)
  assert.match(page, /deleteProductAsset/)
  assert.match(page, /router\.push\('\/generate\/product-images'\)/)
  assert.match(page, /OSS 文件都会被删除/)
})

test('product detail uses asset detail APIs and renders read-only planning design spec', () => {
  const page = readSource('./ProductDetailPage.vue')

  assert.match(page, /getProductAsset/)
  assert.match(page, /sourceImages/)
  assert.match(page, /generatedImages/)
  assert.match(page, /reuseProductAsset/)
  assert.match(page, /deleteProductAsset/)
  assert.match(page, /planningOutput/)
  assert.match(page, /overallDesignSpec/)
  assert.match(page, /designSpecItems/)
  assert.match(page, /product-design-spec/)
  assert.match(page, /product-detail-layout\s*\{[\s\S]*grid-template-columns:\s*1fr;/)
  assert.doesNotMatch(page, /analysisOutput/)
})

test('navigation exposes product library but not asset library', () => {
  const nav = readFileSync(new URL('../data/mock.js', import.meta.url), 'utf8')
  const shell = readFileSync(new URL('../layout/UserShell.vue', import.meta.url), 'utf8')

  assert.match(nav, /商品库/)
  assert.match(nav, /\/products/)
  assert.doesNotMatch(nav, /素材库/)
  assert.match(shell, /primaryNavItems/)
})
