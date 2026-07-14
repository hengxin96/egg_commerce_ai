<template>
  <GeneratorWorkspace
    eyebrow="AI 全品类商品图"
    title="一键生成主图 & 详情图组"
    desc="上传产品图，AI 智能分析产品特征，自动生成电商主图与多角度多场景的详情图组。"
    upload-label="上传清晰的产品图片"
    :uploaded-images="uploadedImages"
    :uploading="uploading"
    :show-cancel-button="Boolean(currentTask)"
    :prompt-value="workspacePrompt"
    @upload-image="handleUploadImage"
    @remove-image="handleRemoveImage"
    @generate="handleGenerate"
    @rewrite-requirement="handleRewriteRequirement"
    @cancel="handleCancel"
  />

  <el-dialog
    v-model="rewriteDialogVisible"
    title="AI 帮写结果"
    width="520px"
    class="product-rewrite-dialog"
  >
    <p class="product-rewrite-preview">
      {{ rewriteResult?.optimizedRequirement || '暂无可替换内容' }}
    </p>
    <template #footer>
      <el-button @click="rewriteDialogVisible = false">退出</el-button>
      <el-button type="primary" @click="applyRewriteResult">替换</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
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
const rewriteResult = shallowRef(null)
const rewriteDialogVisible = shallowRef(false)
const workspacePrompt = shallowRef('')

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
  rewriteResult.value = await rewriteProductRequirement({
    productAssetId: currentDraft.value?.asset?.id,
    assetType: form.assetType,
    targetPlatform: form.platform,
    userRequirement: form.prompt
  })
  rewriteDialogVisible.value = true
  ElMessage.success(rewriteResult.value?.optimizedRequirement || 'AI 帮写完成')
}

function applyRewriteResult() {
  if (!rewriteResult.value?.optimizedRequirement) {
    return
  }

  workspacePrompt.value = rewriteResult.value.optimizedRequirement
  rewriteDialogVisible.value = false
}

async function handleCancel() {
  if (!currentTask.value?.id) {
    return
  }

  await cancelProductImageTask(currentTask.value.id)
  currentTask.value = null
  currentDraft.value = null
  uploadedImages.value = []
}

refreshDraft().catch(() => {})
</script>
