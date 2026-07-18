import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('product detail page shows generated images with selectable normal and zip downloads', () => {
  const page = readSource('./ProductDetailPage.vue')

  assert.match(page, /SelectableImageGallery/)
  assert.match(page, /:images="generatedImages"/)
  assert.match(page, /selectedGeneratedImageIds/)
  assert.match(page, /downloadProductAssetImage/)
  assert.match(page, /downloadProductAssetImagesZip/)
  assert.match(page, /downloadSelectedImagesNormal/)
  assert.match(page, /downloadSelectedImagesZip/)
  assert.match(page, /:disabled="!selectedGeneratedImageIds\.length"/)
  assert.match(page, /\u666e\u901a\u4e0b\u8f7d/)
  assert.match(page, /ZIP\u4e0b\u8f7d/)
  assert.doesNotMatch(page, /downloadProductAsset\(/)
})

test('selectable image gallery defaults to no selection and renders top-left checkboxes', () => {
  const gallery = readSource('../components/product/SelectableImageGallery.vue')

  assert.match(gallery, /selectedIds:\s*\{ type: Array, default: \(\) => \[\] \}/)
  assert.match(gallery, /class="gallery-check"/)
  assert.match(gallery, /type="checkbox"/)
  assert.match(gallery, /全选/)
  assert.match(gallery, /已选/)
  assert.match(gallery, /update:selectedIds/)
})

test('selectable image gallery offers preview controls for every generated image', () => {
  const gallery = readSource('../components/product/SelectableImageGallery.vue')

  assert.match(gallery, /View/)
  assert.match(gallery, /class="gallery-preview"/)
  assert.match(gallery, /@click\.stop="openImagePreview\(index\)"/)
  assert.match(gallery, /\.gallery-preview\s*\{[\s\S]*border-radius:\s*999px;/)
  assert.match(gallery, /\.gallery-preview\s*\{[\s\S]*color:\s*#fff;/)
  assert.match(gallery, /\.gallery-preview\s*\{[\s\S]*background:\s*rgba\(17,\s*18,\s*23,\s*0\.72\);/)
  assert.match(gallery, /\.gallery-preview\s*\{[\s\S]*backdrop-filter:\s*blur\(8px\);/)
  assert.match(gallery, /previewUrls/)
  assert.match(gallery, /previewImageIndex/)
  assert.match(gallery, /<el-image-viewer/)
  assert.match(gallery, /:url-list="previewUrls"/)
  assert.match(gallery, /:initial-index="previewImageIndex"/)
  assert.match(gallery, /@close="closeImagePreview"/)
})

test('product detail page maps audit output onto generated images', () => {
  const page = readSource('./ProductDetailPage.vue')
  const gallery = readSource('../components/product/SelectableImageGallery.vue')
  const auditBadge = readSource('../components/product/ProductImageAuditBadge.vue')

  assert.match(page, /auditOutput/)
  assert.match(page, /auditByImageId/)
  assert.match(page, /audit:\s*auditForGeneratedImage\(image,\s*index\)/)
  assert.match(gallery, /ProductImageAuditBadge/)
  assert.match(gallery, /:audit="image\.audit"/)
  assert.match(auditBadge, /audit-status/)
})

test('product detail page renders structured overall design spec like plan confirmation', () => {
  const page = readSource('./ProductDetailPage.vue')

  assert.match(page, /designSpecSections/)
  assert.match(page, /product-design-spec-grid/)
  assert.match(page, /product-design-spec-section/)
  assert.match(page, /readableText/)
  assert.match(page, /humanLabel/)
  assert.match(page, /colorRows/)
  assert.doesNotMatch(page, /designSpecItems/)
  assert.doesNotMatch(page, /String\(value \|\| ''\)\.trim\(\)/)
})
