<template>
  <GeneratorWorkspace
    eyebrow="AI 全品类商品图"
    title="一键生成主图 & 详情图组"
    desc="上传产品图，AI 智能分析产品特征，自动生成电商主图与多角度多场景的详情图组。"
    upload-label="上传清晰的产品图片"
    :uploaded-images="uploadedImages"
    :uploading="uploading"
    :show-cancel-button="Boolean(currentTask)"
    @upload-image="handleUploadImage"
    @remove-image="handleRemoveImage"
    @generate="handleGenerate"
    @rewrite-requirement="handleRewriteRequirement"
    @cancel="handleCancel"
  />
</template>

<script setup>
import { shallowRef } from 'vue'
import GeneratorWorkspace from '@/components/GeneratorWorkspace.vue'
import {
  cancelProductImageTask,
  createProductImageTask,
  getProductDraft,
  rewriteProductRequirement,
  uploadDraftImage,
  deleteDraftImage
} from '@/api/productImage'

const uploadedImages = shallowRef([])
const uploading = shallowRef(false)
const currentDraft = shallowRef(null)
const currentTask = shallowRef(null)

const normalizeDraftImages = (draft) => (
  draft?.sourceImages?.map((image) => ({
    id: image.id,
    name: image.ossObjectKey?.split('/').pop() || '商品图',
    url: image.ossUrl,
    raw: image
  })) || []
)

async function refreshDraft() {
  currentDraft.value = await getProductDraft()
  uploadedImages.value = normalizeDraftImages(currentDraft.value)
}

async function handleUploadImage(files) {
  uploading.value = true
  try {
    for (const file of files) {
      currentDraft.value = await uploadDraftImage(file)
    }
    await refreshDraft()
  } finally {
    uploading.value = false
  }
}

async function handleRemoveImage(imageId) {
  await deleteDraftImage(imageId)
  await refreshDraft()
}

async function handleGenerate(form) {
  currentTask.value = await createProductImageTask({
    productAssetId: currentDraft.value?.asset?.id,
    assetType: form.assetType,
    targetPlatform: form.platform,
    aspectRatio: form.ratio,
    quality: form.quality,
    userRequirement: form.prompt
  })
}

async function handleRewriteRequirement(form) {
  await rewriteProductRequirement({
    productAssetId: currentDraft.value?.asset?.id,
    assetType: form.assetType,
    targetPlatform: form.platform,
    userRequirement: form.prompt
  })
}

async function handleCancel() {
  if (!currentTask.value?.id) {
    return
  }

  await cancelProductImageTask(currentTask.value.id)
  currentTask.value = null
  uploadedImages.value = []
}

refreshDraft().catch(() => {})
</script>
