<template>
  <div class="product-image-page">
    <GeneratorWorkspace
      eyebrow="AI 全品类商品图"
      title="一键生成主图 & 详情图组"
      desc="上传产品图，AI 智能分析产品特征，自动生成电商主图与多角度多场景的详情图组。"
      upload-label="上传清晰的产品图片"
      :steps="PRODUCT_IMAGE_STEPS"
      :active-step="activeStep"
      :selected-step="selectedStep"
      :completed-steps="completedSteps"
      :failed-step="failedStep"
      :uploaded-images="uploadedImages"
      :uploading="isUploading"
      :show-cancel-button="canCancelCurrentTask"
      :prompt-value="workspacePrompt"
      :reset-key="workspaceResetKey"
      :generating="isCreating"
      :result-loading="isTaskProcessing"
      :result-title="resultTitle"
      :result-subtitle="resultSubtitle"
      :empty-message="emptyMessage"
      :result-sections="resultSections"
      :can-download="canDownload"
      :download-options="[{ label: '下载图片', value: 'image' }]"
      :prompt-helper-loading="isRewriting"
      @upload-image="handleUploadImage"
      @remove-image="handleRemoveImage"
      @reorder-image="handleReorderImages"
      @set-primary-image="handleReorderImages"
      @generate="handleGenerate"
      @select-step="handleSelectStep"
      @download="handleDownload"
      @rewrite-requirement="handleRewriteRequirement"
      @cancel="handleCancelTask"
    >
      <template #plan-confirmation="{ section }">
        <ProductImagePlanEditor
          embedded
          :analysis-output="section.analysisOutput"
          :planning-output="section.planningOutput"
          :text-enabled="section.textEnabled"
          :confirming="section.confirming"
          :replanning="section.replanning"
          :user-requirement="section.userRequirement"
          @confirm="handleConfirmPlan"
          @replan="handleReplan"
        />
      </template>
    </GeneratorWorkspace>

    <el-dialog
      v-model="rewriteDialogVisible"
      width="520px"
      class="product-rewrite-dialog"
    >
      <template #header>
        <div class="product-rewrite-title">
          <span>AI 帮写结果</span>
          <el-icon v-if="isRewriting" class="product-rewrite-loading"><Loading /></el-icon>
        </div>
      </template>
      <p
        class="product-rewrite-preview"
        :class="{ 'is-waiting': isRewriting && !rewriteStreamText }"
      >
        {{ rewritePreviewText }}
      </p>
      <p v-if="rewriteError" class="product-rewrite-error">{{ rewriteError }}</p>
      <template #footer>
        <el-button :disabled="isRewriting" @click="rewriteDialogVisible = false">退出</el-button>
        <el-button
          type="primary"
          :loading="isRewriting"
          :disabled="!canApplyRewrite"
          @click="applyRewriteResult"
        >
          替换
        </el-button>
      </template>
    </el-dialog>

    <Transition name="story-task-notice">
      <aside
        v-if="taskNotice"
        class="story-task-notice"
        :class="`is-${taskNotice.type}`"
        role="status"
        aria-live="polite"
      >
        <span class="story-task-notice-icon" aria-hidden="true">
          <el-icon v-if="taskNotice.type === 'success'"><CircleCheck /></el-icon>
          <el-icon v-else><CircleClose /></el-icon>
        </span>
        <div class="story-task-notice-body">
          <strong>{{ taskNotice.title }}</strong>
          <p>{{ taskNotice.message }}</p>
          <button type="button" class="story-task-notice-action" @click="goProducts">
            去商品库查看
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
        <button
          type="button"
          class="story-task-notice-close"
          aria-label="关闭任务提醒"
          @click="taskNotice = null"
        >
          <el-icon><Close /></el-icon>
        </button>
      </aside>
    </Transition>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, CircleCheck, CircleClose, Close, Loading } from '@element-plus/icons-vue'
import GeneratorWorkspace from '@/components/GeneratorWorkspace.vue'
import ProductImagePlanEditor from '@/components/product/ProductImagePlanEditor.vue'
import { handleAuthFailure } from '@/utils/authGuard'
import {
  cancelProductImageTask,
  confirmProductImageTask,
  createProductImageTask,
  deleteDraftImage,
  downloadProductAsset,
  getProductAsset,
  getProductDraft,
  getProductImageTask,
  replanProductImageTask,
  rewriteProductRequirementStream,
  updateDraftImageOrder,
  uploadDraftImage
} from '@/api/productImage'

const PRODUCT_IMAGE_STEPS = ['输入', '分析中', '确认规划', '生成中', '完成']

const router = useRouter()
const draft = shallowRef(null)
const uploadedImages = shallowRef([])
const currentTask = shallowRef(null)
const generatedAsset = shallowRef(null)
const selectedStep = shallowRef('输入')
const isUploading = shallowRef(false)
const isCreating = shallowRef(false)
const isConfirmingPlan = shallowRef(false)
const isReplanning = shallowRef(false)
const taskNotice = shallowRef(null)
const rewriteDialogVisible = shallowRef(false)
const rewriteResult = shallowRef(null)
const rewriteStreamText = shallowRef('')
const rewriteError = shallowRef('')
const isRewriting = shallowRef(false)
const workspacePrompt = shallowRef('')
const workspaceResetKey = shallowRef(0)
const requestGeneration = shallowRef(0)
let pollTimer = null

const activeStep = computed(() => currentTask.value?.currentNode || '输入')
const failedStep = computed(() => currentTask.value?.status === 'failed' ? currentTask.value.currentNode : '')
const isTaskProcessing = computed(() => currentTask.value?.status === 'processing' || isCreating.value)
const showPlanEditor = computed(() => currentTask.value?.status === 'waiting_confirmation')
const canCancelCurrentTask = computed(() => Boolean(currentTask.value?.id)
  && ['processing', 'waiting_confirmation'].includes(currentTask.value.status))
const canDownload = computed(() => currentTask.value?.status === 'completed' && Boolean(generatedImages.value.length))
const canApplyRewrite = computed(() => !isRewriting.value && Boolean(rewriteResult.value?.optimizedRequirement))
const rewritePreviewText = computed(() => (
  rewriteStreamText.value
  || rewriteResult.value?.optimizedRequirement
  || (isRewriting.value ? '正在帮写，请稍候...' : '暂无可替换内容')
))
const activeStepIndex = computed(() => Math.max(PRODUCT_IMAGE_STEPS.indexOf(activeStep.value), 0))
const completedSteps = computed(() => {
  if (!currentTask.value) {
    return []
  }
  if (currentTask.value.status === 'completed') {
    return PRODUCT_IMAGE_STEPS
  }
  return PRODUCT_IMAGE_STEPS.filter((_, index) => index < activeStepIndex.value)
})
const generatedImages = computed(() => generatedAsset.value?.generatedImages || [])
const generationOutput = computed(() => parseGenerationOutput(currentTask.value?.generationOutput))
const currentPlanTextEnabled = computed(() => taskTextEnabled(currentTask.value))
const sourceImages = computed(() => generatedAsset.value?.sourceImages || uploadedImages.value)
const resultTitle = computed(() => {
  if (selectedStep.value === '分析中') {
    return '分析中'
  }
  if (selectedStep.value === '确认规划') {
    return '确认规划'
  }
  if (selectedStep.value === '生成中') {
    return '生成中'
  }
  return currentTask.value?.status === 'completed' ? '生成结果' : '生成结果'
})
const resultSubtitle = computed(() => {
  if (!currentTask.value) {
    return '上传产品图并填写要求后，点击开始分析'
  }
  if (currentTask.value.status === 'completed') {
    return '商品图已生成完成，可下载或前往商品库查看'
  }
  if (currentTask.value.status === 'failed') {
    return currentTask.value.failureReason || '当前节点生成失败，可前往任务中心重试'
  }
  if (currentTask.value.status === 'waiting_confirmation') {
    return '规划已生成，可在右侧确认生成或重新规划'
  }
  return `当前节点：${currentTask.value.currentNode || '分析中'}`
})
const emptyMessage = computed(() => {
  if (!currentTask.value) {
    return '上传产品图并填写要求后\n点击开始分析'
  }
  if (currentTask.value.status === 'failed') {
    return currentTask.value.failureReason || '任务生成失败，请前往任务中心查看或重试'
  }
  if (currentTask.value.status === 'waiting_confirmation') {
    return '规划已生成\n请在右侧确认生成或重新规划'
  }
  return '后台正在生成商品图，节点完成后会展示内容'
})
const resultSections = computed(() => {
  if (!currentTask.value) {
    return []
  }
  if (selectedStep.value === '输入') {
    return [{
      title: '输入参数',
      imageUrl: sourceImages.value[0]?.url || sourceImages.value[0]?.ossUrl,
      content: inputSummary()
    }]
  }
  if (selectedStep.value === '分析中') {
    return [processingSection('AI 正在分析', '正在分析商品图，请稍候')]
  }
  if (selectedStep.value === '确认规划') {
    if (showPlanEditor.value) {
      return [{
        type: 'plan-confirmation',
        title: '确认规划',
        analysisOutput: currentTask.value.analysisOutput,
        planningOutput: currentTask.value.planningOutput,
        textEnabled: currentPlanTextEnabled.value,
        confirming: isConfirmingPlan.value,
        replanning: isReplanning.value,
        userRequirement: (draft.value?.asset || generatedAsset.value?.asset || {}).userRequirement || ''
      }]
    }
    return [{ title: '规划内容', content: jsonText(currentTask.value.planningOutput) }]
  }
  if (selectedStep.value === '生成中' && currentTask.value.status !== 'completed') {
    return [processingSection('AI 正在生成', '正在生成商品图，请稍候')]
  }
  if (selectedStep.value === '生成中' || selectedStep.value === '完成') {
    if (generatedImages.value.length) {
      return generatedImages.value.map((image, index) => ({
        title: `${selectedStep.value === '完成' ? '生成结果' : '生成中'} ${index + 1}`,
        imageUrl: image.ossUrl || image.url || image.temporaryUrl,
        content: `生成图 ${index + 1}`,
        audit: auditForImage(image, index)
      }))
    }
    return [{
      title: selectedStep.value === '完成' ? '生成结果' : '生成中',
      content: jsonText(currentTask.value.generationOutput)
    }]
  }
  return []
})

function processingSection(title, statusText) {
  // Processing nodes intentionally hide raw intermediate JSON; users only need to know the backend is still working.
  return {
    type: 'loading',
    title,
    statusText
  }
}

function normalizeDraftImages(detail) {
  return detail?.sourceImages?.map((image, index) => ({
    id: image.id,
    name: image.analysisRole || image.ossObjectKey?.split('/').pop() || '商品图',
    url: image.ossUrl,
    isPrimary: index === 0,
    uploaded: true,
    raw: image
  })) || []
}

async function loadDraft(generation = requestGeneration.value) {
  const detail = await getProductDraft()
  if (generation !== requestGeneration.value) {
    return
  }
  draft.value = detail
  uploadedImages.value = normalizeDraftImages(detail)
}

async function handleUploadImage(files) {
  if (isUploading.value) {
    return
  }

  const generation = requestGeneration.value
  isUploading.value = true
  try {
    for (const file of files) {
      const detail = await uploadDraftImage(file)
      if (generation !== requestGeneration.value) {
        return
      }
      draft.value = detail
    }
    await loadDraft(generation)
  } catch (error) {
    if (generation === requestGeneration.value) {
      await loadDraft(generation).catch(() => {})
    }
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '上传图片失败')
    }
  } finally {
    if (generation === requestGeneration.value) {
      isUploading.value = false
    }
  }
}

async function handleRemoveImage(imageId) {
  const generation = requestGeneration.value
  try {
    await deleteDraftImage(imageId)
    await loadDraft(generation)
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '删除图片失败')
    }
  }
}

async function handleReorderImages(payload) {
  if (!payload?.imageIds?.length || !draft.value?.asset?.id) {
    return
  }
  const generation = requestGeneration.value
  try {
    const detail = await updateDraftImageOrder({
      imageIds: payload.imageIds,
      primaryImageId: payload.primaryImageId || payload.imageIds[0]
    })
    if (generation !== requestGeneration.value) {
      return
    }
    draft.value = detail
    uploadedImages.value = normalizeDraftImages(detail)
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '图片排序失败')
    }
  }
}

async function handleGenerate(form) {
  if (!uploadedImages.value.length) {
    ElMessage.warning('请先上传产品图')
    return
  }
  if (uploadedImages.value.length === 1) {
    ElMessage.warning('已使用单张产品图开始分析，多视角或 SKU 图可提升规划稳定性')
  }

  const generation = requestGeneration.value
  isCreating.value = true
  taskNotice.value = null
  generatedAsset.value = null
  stopPolling()
  try {
    const task = await createProductImageTask({
      productAssetId: draft.value?.asset?.id,
      assetType: form.assetType === '主图' ? 'main' : 'detail',
      targetPlatform: form.platform,
      targetLanguage: form.language,
      textEnabled: form.textEnabled,
      selectedModel: form.model,
      aspectRatio: form.ratio,
      quality: form.quality,
      imageCount: form.count,
      userRequirement: form.prompt
    })
    if (generation !== requestGeneration.value) {
      return
    }
    currentTask.value = task
    selectedStep.value = task.currentNode || '分析中'
    await refreshCurrentTask(generation)
    if (shouldPollProductImageTask(currentTask.value)) {
      startPolling()
    }
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '创建商品图任务失败')
    }
  } finally {
    if (generation === requestGeneration.value) {
      isCreating.value = false
    }
  }
}

async function handleConfirmPlan(payload) {
  if (!currentTask.value?.id || isConfirmingPlan.value) {
    return
  }
  const generation = requestGeneration.value
  isConfirmingPlan.value = true
  try {
    const task = await confirmProductImageTask(currentTask.value.id, payload || {})
    if (generation !== requestGeneration.value) {
      return
    }
    currentTask.value = task
    selectedStep.value = task.currentNode || '生成中'
    if (task.status === 'completed') {
      await loadGeneratedAsset(task, generation)
      showCompletionNotice()
    } else if (shouldPollProductImageTask(task)) {
      startPolling()
    }
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '确认生成失败')
    }
  } finally {
    if (generation === requestGeneration.value) {
      isConfirmingPlan.value = false
    }
  }
}

async function handleReplan(payload) {
  if (!currentTask.value?.id || isReplanning.value) {
    return
  }
  const generation = requestGeneration.value
  isReplanning.value = true
  try {
    const task = await replanProductImageTask(currentTask.value.id, payload || {})
    if (generation !== requestGeneration.value) {
      return
    }
    currentTask.value = task
    selectedStep.value = task.currentNode || '确认规划'
    if (shouldPollProductImageTask(task)) {
      startPolling()
    }
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '重新规划失败')
    }
  } finally {
    if (generation === requestGeneration.value) {
      isReplanning.value = false
    }
  }
}

async function handleRewriteRequirement(form) {
  if (isRewriting.value) {
    return
  }
  const rewriteRequirement = form.prompt?.trim() || ''
  const rewriteRequirementLabel = `${form.assetType || '主图'}要求`
  if (!rewriteRequirement) {
    ElMessage.warning(`${rewriteRequirementLabel}不能为空`)
    return
  }

  const generation = requestGeneration.value
  rewriteDialogVisible.value = true
  rewriteResult.value = null
  rewriteStreamText.value = ''
  rewriteError.value = ''
  isRewriting.value = true
  try {
    const result = await rewriteProductRequirementStream({
      targetPlatform: form.platform,
      userRequirement: rewriteRequirement
    }, {
      onDelta: (chunk) => appendRewriteDelta(chunk)
    })
    if (generation !== requestGeneration.value) {
      return
    }
    rewriteResult.value = result
    if (!rewriteStreamText.value && result?.optimizedRequirement) {
      rewriteStreamText.value = result.optimizedRequirement
    }
  } catch (error) {
    if (generation !== requestGeneration.value) {
      return
    }
    if (!handleAuthFailure(error, router)) {
      const errorMessage = error.message || 'AI 帮写失败'
      rewriteError.value = errorMessage
      ElMessage.error(errorMessage)
    }
  } finally {
    if (generation === requestGeneration.value) {
      isRewriting.value = false
    }
  }
}

function appendRewriteDelta(chunk) {
  if (!chunk) {
    return
  }
  rewriteStreamText.value = `${rewriteStreamText.value|| ''}${chunk}`
}

function applyRewriteResult() {
  if (!rewriteResult.value?.optimizedRequirement) {
    return
  }
  workspacePrompt.value = rewriteResult.value.optimizedRequirement
  rewriteDialogVisible.value = false
}

async function refreshCurrentTask(generation = requestGeneration.value) {
  if (!currentTask.value?.id) {
    return
  }
  const task = await getProductImageTask(currentTask.value.id)
  if (generation !== requestGeneration.value) {
    return
  }
  currentTask.value = task
  selectedStep.value = task.status === 'completed' ? '完成' : (task.currentNode || selectedStep.value)
  if (task.status === 'completed') {
    await loadGeneratedAsset(task, generation)
    stopPolling()
    showCompletionNotice()
  } else if (task.status === 'waiting_confirmation') {
    selectedStep.value = '确认规划'
    stopPolling()
  } else if (task.status === 'failed' || task.status === 'cancelled') {
    stopPolling()
  } else if (!shouldPollProductImageTask(task)) {
    stopPolling()
  }
}

async function loadGeneratedAsset(task, generation = requestGeneration.value) {
  if (!task?.productAssetId) {
    return
  }
  const detail = await getProductAsset(task.productAssetId)
  if (generation !== requestGeneration.value) {
    return
  }
  generatedAsset.value = detail
}

function startPolling() {
  stopPolling()
  if (!shouldPollProductImageTask(currentTask.value)) {
    return
  }
  pollTimer = window.setInterval(() => {
    refreshCurrentTask().catch((error) => {
      if (!handleAuthFailure(error, router)) {
        ElMessage.error(error.message || '刷新任务进度失败')
      }
      stopPolling()
    })
  }, 2500)
}

function shouldPollProductImageTask(task) {
  // Planning confirmation is a user decision point, so polling detail rows there only creates noisy repeated reads.
  return Boolean(task?.id)
    && task?.status === 'processing'
    && task?.status !== 'waiting_confirmation'
    && task?.currentNode !== '确认规划'
}

function stopPolling() {
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

function handleSelectStep(step) {
  selectedStep.value = step
}

async function handleDownload() {
  const assetId = currentTask.value?.productAssetId
  if (!assetId) {
    return
  }
  try {
    await downloadProductAsset(assetId)
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '下载图片失败')
    }
  }
}

function clearPageSurface() {
  stopPolling()
  currentTask.value = null
  // Task-backed cancelled assets stay in history; clearing here is only the page surface.
  draft.value = null
  generatedAsset.value = null
  uploadedImages.value = []
  workspacePrompt.value = ''
  rewriteResult.value = null
  rewriteStreamText.value = ''
  rewriteError.value = ''
  rewriteDialogVisible.value = false
  isRewriting.value = false
  isUploading.value = false
  isCreating.value = false
  isConfirmingPlan.value = false
  isReplanning.value = false
  selectedStep.value = '输入'
  workspaceResetKey.value += 1
}

async function handleCancelTask() {
  if (!currentTask.value?.id) {
    return
  }
  const taskId = currentTask.value.id
  requestGeneration.value += 1
  clearPageSurface()
  ElMessage.success('当前任务已终止，如需查看请前往任务中心')
  try {
    await cancelProductImageTask(taskId)
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '终止任务请求失败，请前往任务中心确认状态')
    }
  }
}

function showCompletionNotice() {
  taskNotice.value = {
    type: 'success',
    title: '商品图已生成',
    message: '当前任务已完成，可前往商品库查看和继续使用'
  }
}

function goProducts() {
  taskNotice.value = null
  router.push('/products')
}

function inputSummary() {
  const task = currentTask.value
  const asset = draft.value?.asset || generatedAsset.value?.asset || {}
  return [
    `商品：${asset.name || '未命名商品'}`,
    `类型：${task?.assetType === 'detail' ? '详情图' : '主图'}`,
    `平台：${task?.targetPlatform || asset.targetPlatform || '抖音'}`,
    `源图：${uploadedImages.value.length || sourceImages.value.length} 张`,
    `要求：${asset.userRequirement || task?.userRequirement || '未填写'}`
  ].join('\n')
}

function jsonText(value) {
  if (!value || value === '{}') {
    return '等待节点输出'
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return String(value)
  }
}

function parseGenerationOutput(value) {
  try {
    const parsed = JSON.parse(value || '{}')
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

function taskTextEnabled(task) {
  try {
    const params = JSON.parse(task?.modelParams || '{}')
    return params?.textEnabled === true
  } catch {
    return false
  }
}

function auditForImage(image, index) {
  const output = generationOutput.value
  const auditImages = Array.isArray(output.images) ? output.images : []
  const matched = auditImages.find((item) => [item.imageId, item.finalImageId, item.id].includes(image.id))
    || auditImages[index]
  return matched ? { ...matched, auditSummary: output.auditSummary } : null
}

onMounted(() => {
  loadDraft().catch((error) => {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '恢复草稿失败')
    }
  })
})

onBeforeUnmount(() => {
  stopPolling()
  taskNotice.value = null
})
</script>

<style scoped>
.product-image-page :deep(.cancel-generate-button.el-button) {
  width: 100%;
  min-height: 52px;
  margin-left: 0;
  border-radius: 16px;
  font-weight: 850;
}

.product-image-page :deep(.result-image-preview) {
  width: 100%;
  max-height: 320px;
  display: block;
  object-fit: contain;
  margin: 0 0 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel-muted);
}

.product-rewrite-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text);
  font-size: 18px;
  font-weight: 850;
}

.product-rewrite-loading {
  color: #858994;
  animation: product-rewrite-spin 1s linear infinite;
}

.product-rewrite-preview {
  max-height: 360px;
  overflow-y: auto;
  margin: 0;
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  color: var(--text);
  background: var(--panel-muted);
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.product-rewrite-preview.is-waiting {
  color: var(--muted);
}

.product-rewrite-error {
  margin: 10px 0 0;
  color: var(--danger);
  font-size: 13px;
  line-height: 1.6;
}

@keyframes product-rewrite-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
