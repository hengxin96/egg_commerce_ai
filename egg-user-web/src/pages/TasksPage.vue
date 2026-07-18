<template>
  <div class="page">
    <section class="section-title">
      <div>
        <h2>任务中心</h2>
        <p class="muted">查看剧情生成和商品图任务的进度、失败原因和节点内容。</p>
      </div>
      <el-button :loading="isLoading" @click="loadTasks()">刷新</el-button>
    </section>

    <div class="table-panel">
      <el-table v-loading="isLoading" :data="tasks" style="width:100%">
        <el-table-column label="任务名称" min-width="240">
          <template #default="{ row }">{{ row.name }}</template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ row.typeLabel }}</template>
        </el-table-column>
        <el-table-column label="目标平台" width="120">
          <template #default="{ row }">{{ row.targetPlatform }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <span :class="['status-dot', statusClass(row.status)]">
              {{ statusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="当前节点" width="150">
          <template #default="{ row }">{{ row.currentNode }}</template>
        </el-table-column>
        <el-table-column label="进度" width="180">
          <template #default="{ row }"><el-progress :percentage="progress(row)" /></template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.failureReason || row.cancelReason || '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="310" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canRetryTask(row)"
              link
              type="primary"
              :loading="Boolean(retryingTaskIds[row.id])"
              @click="retryTask(row)"
            >
              重试
            </el-button>
            <el-button
              v-if="canCancelTask(row)"
              link
              type="danger"
              :loading="Boolean(cancellingTaskIds[row.id])"
              @click="cancelTask(row)"
            >
              终止
            </el-button>
            <el-button link type="primary" @click="viewTask(row)">查看内容</el-button>
            <el-button
              v-if="canDownloadTask(row)"
              link
              type="primary"
              @click="download(row, 'docx')"
            >
              下载为Word
            </el-button>
            <el-button
              v-if="canDownloadTask(row)"
              link
              type="primary"
              @click="download(row, 'md')"
            >
              下载为MD
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="isDetailDialogVisible"
      class="story-task-detail-dialog"
      width="min(1100px, 92vw)"
      top="6vh"
      destroy-on-close
    >
      <template #header>
        <div class="task-dialog-header">
          <h3>{{ selectedTask?.name || '任务详情' }}</h3>
          <p v-if="selectedTask" class="muted">
            {{ selectedTask.typeLabel }} · {{ statusLabel(selectedTask.status) }} · 当前节点：{{ selectedTask.currentNode }}
          </p>
        </div>
      </template>

      <div v-if="selectedTask" class="story-task-sections">
        <article
          v-for="section in selectedTaskSections"
          :key="section.title"
          class="result-section"
          :class="{ 'is-plan-confirmation': section.type === 'plan-confirmation' }"
        >
          <ProductImagePlanEditor
            v-if="section.type === 'plan-confirmation'"
            embedded
            :analysis-output="section.analysisOutput"
            :planning-output="section.planningOutput"
            :text-enabled="section.textEnabled"
            :confirming="section.confirming"
            :replanning="section.replanning"
            :readonly="section.readonly"
            :user-requirement="section.userRequirement"
            @confirm="handleConfirmSelectedPlan"
            @replan="handleReplanSelectedTask"
          />
          <template v-else>
            <h3>{{ section.title }}</h3>
            <SelectableImageGallery
              v-if="section.images?.length"
              :images="section.images"
              :selected-ids="selectedTaskImageIds"
              empty-text="暂无生成图"
              @update:selected-ids="selectedTaskImageIds = $event"
            />
            <div v-if="section.images?.length" class="task-section-actions">
              <el-button
                type="primary"
                :icon="Download"
                :disabled="!selectedTaskImageIds.length"
                @click="downloadSelectedProductImages('normal')"
              >
                普通下载
              </el-button>
              <el-button
                :icon="Download"
                :disabled="!selectedTaskImageIds.length"
                @click="downloadSelectedProductImages('zip')"
              >
                ZIP下载
              </el-button>
            </div>
            <div
              v-else-if="section.imageUrl && !deletedImageUrls[section.imageUrl]"
              class="task-result-image-wrap"
            >
              <img
                class="task-result-image"
                :src="section.imageUrl"
                :alt="section.title"
                @error="markImageDeleted(section.imageUrl)"
              />
              <button
                type="button"
                class="task-result-preview"
                :aria-label="`预览 ${section.title}`"
                title="预览图片"
                @click="openTaskImagePreview(section.imageUrl)"
              >
                <el-icon><View /></el-icon>
              </button>
            </div>
            <p
              v-else-if="section.imageMissing || (section.imageUrl && deletedImageUrls[section.imageUrl])"
              class="task-image-missing"
            >
              图片已删除
            </p>
            <pre v-if="section.showContent !== false">{{ section.content || '未生成' }}</pre>
          </template>
        </article>
      </div>
    </el-dialog>

    <el-image-viewer
      v-if="previewTaskImageUrl"
      :url-list="[previewTaskImageUrl]"
      :initial-index="0"
      hide-on-click-modal
      @close="closeTaskImagePreview"
    />

    <Transition name="story-task-notice">
      <aside
        v-if="retryPlanNotice"
        class="story-task-notice is-success"
        role="status"
        aria-live="polite"
      >
        <span class="story-task-notice-icon" aria-hidden="true">
          <el-icon><CircleCheck /></el-icon>
        </span>
        <div class="story-task-notice-body">
          <strong>重试任务已规划整体设计规范</strong>
          <p>请在任务中心-查看内容中确认规划</p>
          <button type="button" class="story-task-notice-action" @click="viewRetryPlanningNotice">
            直达查看内容
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
        <button
          type="button"
          class="story-task-notice-close"
          aria-label="关闭任务提醒"
          @click="retryPlanNotice = null"
        >
          <el-icon><Close /></el-icon>
        </button>
      </aside>
    </Transition>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight, CircleCheck, Close, Download, View } from '@element-plus/icons-vue'
import ProductImagePlanEditor from '@/components/product/ProductImagePlanEditor.vue'
import SelectableImageGallery from '@/components/product/SelectableImageGallery.vue'
import { downloadStoryTask, getStoryTask, listStoryTasks, retryStoryTask } from '@/api/story'
import {
  cancelProductImageTask,
  confirmProductImageTask,
  downloadProductAssetImage,
  downloadProductAssetImagesZip,
  getProductImageTask,
  listProductImageTasks,
  replanProductImageTask,
  retryProductImageTask
} from '@/api/productImage'

const TASK_KIND_STORY = 'story'
const TASK_KIND_PRODUCT_IMAGE = 'product_image'
const TASK_LIST_POLL_INTERVAL_MS = 5000
const STORY_STEPS = ['输入', '剧情生成', '剧本生成', '分镜生成', '分镜校验', '三视图/场景图', '完成']
const PRODUCT_IMAGE_STEPS = ['输入', '分析中', '确认规划', '生成中', '完成']

const tasks = ref([])
const selectedTask = ref(null)
const isLoading = ref(false)
const isDetailDialogVisible = ref(false)
const retryingTaskIds = ref({})
const cancellingTaskIds = ref({})
const deletedImageUrls = ref({})
const selectedTaskImageIds = ref([])
const isConfirmingSelectedPlan = ref(false)
const isReplanningSelectedTask = ref(false)
const retryPlanNotice = ref(null)
const previewTaskImageUrl = shallowRef('')
let retryPlanPollTimer = null
let selectedTaskPollTimer = null
let taskListPollTimer = null
let taskListPollInFlight = false

const selectedTaskSections = computed(() => {
  if (!selectedTask.value) {
    return []
  }

  return allTaskSections(selectedTask.value)
})

const loadTasks = async ({ silent = false } = {}) => {
  if (taskListPollInFlight) {
    return
  }
  taskListPollInFlight = true
  if (!silent) {
    isLoading.value = true
  }
  try {
    const [storyResult, productResult] = await Promise.allSettled([
      listStoryTasks(),
      listProductImageTasks()
    ])
    const nextTasks = []
    const errors = []

    if (storyResult.status === 'fulfilled') {
      nextTasks.push(...storyResult.value.map(normalizeStoryTask))
    } else {
      errors.push(storyResult.reason?.message || '剧情任务加载失败')
    }

    if (productResult.status === 'fulfilled') {
      nextTasks.push(...productResult.value.map(normalizeProductImageTask))
    } else {
      errors.push(productResult.reason?.message || '商品图任务加载失败')
    }

    tasks.value = nextTasks.sort(compareTaskTime)
    if (!selectedTask.value || !tasks.value.some((task) => task.id === selectedTask.value.id)) {
      selectedTask.value = tasks.value[0] || null
    }
    if (!silent && errors.length && tasks.value.length) {
      ElMessage.warning(errors.join('；'))
    } else if (!silent && errors.length) {
      ElMessage.error(errors.join('；'))
    }
  } catch (error) {
    if (!silent) {
      ElMessage.error(error.message || '任务列表加载失败')
    }
  } finally {
    if (!silent) {
      isLoading.value = false
    }
    taskListPollInFlight = false
  }
}

const viewTask = async (task) => {
  try {
    const detail = task.taskKind === TASK_KIND_PRODUCT_IMAGE
      ? normalizeProductImageTask(await getProductImageTask(task.id))
      : normalizeStoryTask(await getStoryTask(task.id))
    deletedImageUrls.value = {}
    selectedTaskImageIds.value = []
    selectedTask.value = detail
    isDetailDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '任务详情加载失败')
  }
}

const canRetryTask = (task) => task?.status === 'failed'
const canCancelTask = (task) => task?.taskKind === TASK_KIND_PRODUCT_IMAGE && task?.status === 'processing'
const canDownloadTask = (task) => task?.taskKind === TASK_KIND_STORY && task?.status === 'completed'

const download = async (task, format) => {
  if (!canDownloadTask(task)) {
    ElMessage.warning('当前任务还在生成中，完成后才能下载')
    return
  }

  try {
    await downloadStoryTask(task.id, format)
  } catch (error) {
    ElMessage.error(error.message || '下载失败')
  }
}

const downloadSelectedProductImages = async (mode) => {
  if (!selectedTaskImageIds.value.length) {
    ElMessage.warning('请先选择要下载的图片')
    return
  }

  try {
    if (mode === 'zip') {
      await downloadProductAssetImagesZip(selectedTaskImageIds.value)
      return
    }
    for (const imageId of selectedTaskImageIds.value) {
      await downloadProductAssetImage(imageId)
    }
  } catch (error) {
    ElMessage.error(error.message || '图片下载失败')
  }
}

const retryTask = async (task) => {
  retryingTaskIds.value = { ...retryingTaskIds.value, [task.id]: true }
  try {
    if (task.taskKind === TASK_KIND_PRODUCT_IMAGE) {
      const shouldNoticePlanning = !hasPlanningOutput(task.planningOutput)
      await retryProductImageTask(task.id)
      if (shouldNoticePlanning) {
        startRetryPlanningNoticePoll(task.id)
      }
    } else {
      await retryStoryTask(task.id)
    }
    ElMessage.success('已开始重试')
    await loadTasks()
  } catch (error) {
    ElMessage.error(error.message || '重试失败')
  } finally {
    const next = { ...retryingTaskIds.value }
    delete next[task.id]
    retryingTaskIds.value = next
  }
}

const cancelTask = async (task) => {
  if (!canCancelTask(task)) {
    return
  }

  cancellingTaskIds.value = { ...cancellingTaskIds.value, [task.id]: true }
  try {
    await cancelProductImageTask(task.id)
    ElMessage.success('已终止')
    await loadTasks()
  } catch (error) {
    ElMessage.error(error.message || '终止失败')
  } finally {
    const next = { ...cancellingTaskIds.value }
    delete next[task.id]
    cancellingTaskIds.value = next
  }
}

const handleConfirmSelectedPlan = async (payload) => {
  if (!selectedTask.value?.id || selectedTask.value.taskKind !== TASK_KIND_PRODUCT_IMAGE) {
    return
  }
  if (selectedTask.value.status !== 'waiting_confirmation') {
    ElMessage.warning('当前任务不在待确认规划状态')
    return
  }

  isConfirmingSelectedPlan.value = true
  try {
    const task = normalizeProductImageTask(await confirmProductImageTask(selectedTask.value.id, payload || {}))
    selectedTask.value = task
    updateTaskInList(task)
    ElMessage.success('已确认生成')
    if (task.status === 'processing') {
      startSelectedTaskPoll(task.id)
    }
    await loadTasks()
  } catch (error) {
    ElMessage.error(error.message || '确认生成失败')
  } finally {
    isConfirmingSelectedPlan.value = false
  }
}

const handleReplanSelectedTask = async (payload) => {
  if (!selectedTask.value?.id || selectedTask.value.taskKind !== TASK_KIND_PRODUCT_IMAGE) {
    return
  }
  if (selectedTask.value.status !== 'waiting_confirmation') {
    ElMessage.warning('当前任务不在待确认规划状态')
    return
  }

  isReplanningSelectedTask.value = true
  try {
    const task = normalizeProductImageTask(await replanProductImageTask(selectedTask.value.id, payload || {}))
    selectedTask.value = task
    updateTaskInList(task)
    ElMessage.success('已开始重新规划')
    if (task.status === 'processing') {
      startSelectedTaskPoll(task.id)
    }
    await loadTasks()
  } catch (error) {
    ElMessage.error(error.message || '重新规划失败')
  } finally {
    isReplanningSelectedTask.value = false
  }
}

const statusLabel = (status) => ({
  processing: '生成中',
  completed: '成功',
  failed: '失败',
  cancelled: '已终止'
}[status] || status || '未知')

const statusClass = (status) => ({
  processing: 'warning',
  completed: 'success',
  failed: 'danger',
  cancelled: 'danger'
}[status] || 'warning')

const progress = (task) => {
  if (task.status === 'completed') {
    return 100
  }

  const steps = task.taskKind === TASK_KIND_PRODUCT_IMAGE ? PRODUCT_IMAGE_STEPS : STORY_STEPS
  const nodeIndex = Math.max(steps.indexOf(task.currentNode), 0)
  return Math.round((nodeIndex / (steps.length - 1)) * 100)
}

const normalizeStoryTask = (task) => ({
  ...task,
  taskKind: TASK_KIND_STORY,
  name: task.storyTheme || '剧情生成任务',
  typeLabel: '剧情生成',
  failureReason: task.failureReason || ''
})

const normalizeProductImageTask = (task) => ({
  ...task,
  taskKind: TASK_KIND_PRODUCT_IMAGE,
  name: `${assetTypeLabel(task.assetType)}生成任务`,
  typeLabel: '商品图',
  failureReason: task.failureReason || '',
  cancelReason: task.cancelReason || ''
})

const compareTaskTime = (left, right) => {
  const leftTime = Date.parse(left.updatedAt || left.createdAt || '') || 0
  const rightTime = Date.parse(right.updatedAt || right.createdAt || '') || 0
  return rightTime - leftTime
}

const assetTypeLabel = (assetType) => ({
  main: '主图',
  detail: '详情图'
}[assetType] || '商品图')

const inputSummary = (task) => [
  `剧情主题：${task.storyTheme || ''}`,
  `产品信息：${task.productInfo || ''}`,
  `目标平台：${task.targetPlatform || ''}`,
  `目标人群：${task.targetAudience || ''}`,
  `推荐风格：${task.storyStyle || ''}`,
  `剧情时长：${task.storyDuration || ''}`
].join('\n')

const productInputSummary = (task) => [
  `任务类型：${assetTypeLabel(task.assetType)}`,
  `目标平台：${task.targetPlatform || ''}`,
  `当前节点：${task.currentNode || ''}`,
  `任务状态：${statusLabel(task.status)}`
].join('\n')

const allTaskSections = (task) => (
  task.taskKind === TASK_KIND_PRODUCT_IMAGE ? productTaskSections(task) : storyTaskSections(task)
)

const storyTaskSections = (task) => [
  { title: '输入参数', content: inputSummary(task) },
  { title: '剧情生成', content: task.storyOutput },
  { title: '剧本生成', content: task.scriptOutput },
  { title: '分镜生成', content: task.storyboardOutput },
  { title: '分镜校验', content: task.storyboardCheckOutput },
  { title: '三视图/场景图', content: task.visualOutput }
]

const productTaskSections = (task) => {
  const generation = parseJsonObject(task.generationOutput)
  const images = productGeneratedImages(task, generation)
  const imageUrl = images[0]?.ossUrl || images[0]?.url || generation.ossUrl || generation.temporaryUrl || ''
  const hideGenerationJson = Boolean(images.length || imageUrl)

  return [
    { title: '输入摘要', content: productInputSummary(task) },
    {
      title: '确认规划',
      type: 'plan-confirmation',
      analysisOutput: task.analysisOutput,
      planningOutput: task.planningOutput,
      textEnabled: taskTextEnabled(task),
      confirming: isConfirmingSelectedPlan.value,
      replanning: isReplanningSelectedTask.value,
      readonly: task.status !== 'waiting_confirmation',
      userRequirement: ''
    },
    {
      title: '生成结果',
      content: hideGenerationJson ? '' : jsonText(task.generationOutput),
      images,
      imageUrl,
      imageMissing: task.status === 'completed' && !imageUrl,
      showContent: !hideGenerationJson
    }
  ]
}

const productGeneratedImages = (task, generation = parseJsonObject(task.generationOutput)) => {
  if (Array.isArray(task.generatedImages) && task.generatedImages.length) {
    return task.generatedImages
  }
  if (Array.isArray(generation.images)) {
    return generation.images
      .map((image, index) => ({
        ...image,
        id: image.imageId || image.id || `${task.id}-${index}`,
        url: image.ossUrl || image.url || image.temporaryUrl,
        label: `生成图 ${index + 1}`,
        audit: imageAudit(image, generation.auditSummary)
      }))
      .filter((image) => image.url)
  }
  const imageUrl = generation.ossUrl || generation.temporaryUrl || ''
  return imageUrl ? [{
    id: generation.imageId || `${task.id}-image`,
    url: imageUrl,
    label: '生成图 1',
    audit: imageAudit(generation, generation.auditSummary)
  }] : []
}

const imageAudit = (image, auditSummary) => (image?.ruleAudit || image?.visionAudit || auditSummary)
  ? { ...image, auditSummary }
  : null

const jsonText = (value) => {
  if (!value || value === '{}') {
    return '等待节点输出'
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return String(value)
  }
}

const parseJsonObject = (value) => {
  try {
    const parsed = JSON.parse(value || '{}')
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

const hasPlanningOutput = (value) => {
  const planning = parseJsonObject(value)
  return Object.keys(planning).length > 0
}

const taskTextEnabled = (task) => {
  const params = parseJsonObject(task?.modelParams)
  return params.textEnabled === true
}

const updateTaskInList = (task) => {
  tasks.value = tasks.value.map((item) => item.id === task.id ? { ...item, ...task } : item)
}

const refreshProductTask = async (taskId) => {
  const task = normalizeProductImageTask(await getProductImageTask(taskId))
  updateTaskInList(task)
  if (selectedTask.value?.id === task.id) {
    selectedTask.value = task
  }
  return task
}

const refreshVisibleTaskDetail = async () => {
  if (!shouldPollVisibleTaskDetail()) {
    return
  }
  try {
    const detail = selectedTask.value.taskKind === TASK_KIND_PRODUCT_IMAGE
      ? normalizeProductImageTask(await getProductImageTask(selectedTask.value.id))
      : normalizeStoryTask(await getStoryTask(selectedTask.value.id))
    selectedTask.value = detail
    updateTaskInList(detail)
  } catch (error) {
    // Background detail polling should never interrupt the user while they are reviewing a task.
  }
}

const shouldPollVisibleTaskDetail = () => {
  // Open task details only need live detail reads while the backend is still mutating the task.
  return isDetailDialogVisible.value
    && Boolean(selectedTask.value?.id)
    && selectedTask.value?.status === 'processing'
}

const startTaskListPolling = () => {
  stopTaskListPolling()
  taskListPollTimer = window.setInterval(async () => {
    await loadTasks({ silent: true })
    await refreshVisibleTaskDetail()
  }, TASK_LIST_POLL_INTERVAL_MS)
}

const stopTaskListPolling = () => {
  if (taskListPollTimer) {
    window.clearInterval(taskListPollTimer)
    taskListPollTimer = null
  }
}

const startRetryPlanningNoticePoll = (taskId) => {
  stopRetryPlanningNoticePoll()
  retryPlanPollTimer = window.setInterval(async () => {
    try {
      const task = await refreshProductTask(taskId)
      if (task.status === 'waiting_confirmation' && hasPlanningOutput(task.planningOutput)) {
        retryPlanNotice.value = {
          id: task.id,
          task
        }
        stopRetryPlanningNoticePoll()
      } else if (['failed', 'completed', 'cancelled'].includes(task.status)) {
        stopRetryPlanningNoticePoll()
      }
    } catch (error) {
      stopRetryPlanningNoticePoll()
    }
  }, 2500)
}

const stopRetryPlanningNoticePoll = () => {
  if (retryPlanPollTimer) {
    window.clearInterval(retryPlanPollTimer)
    retryPlanPollTimer = null
  }
}

const startSelectedTaskPoll = (taskId) => {
  stopSelectedTaskPoll()
  selectedTaskPollTimer = window.setInterval(async () => {
    try {
      const task = await refreshProductTask(taskId)
      if (task.status !== 'processing') {
        stopSelectedTaskPoll()
      }
    } catch (error) {
      stopSelectedTaskPoll()
    }
  }, 2500)
}

const stopSelectedTaskPoll = () => {
  if (selectedTaskPollTimer) {
    window.clearInterval(selectedTaskPollTimer)
    selectedTaskPollTimer = null
  }
}

const viewRetryPlanningNotice = async () => {
  const task = retryPlanNotice.value?.task
  retryPlanNotice.value = null
  if (task) {
    await viewTask(task)
  }
}

const markImageDeleted = (url) => {
  if (!url) {
    return
  }
  deletedImageUrls.value = { ...deletedImageUrls.value, [url]: true }
}

const openTaskImagePreview = (url) => {
  previewTaskImageUrl.value = url || ''
}

const closeTaskImagePreview = () => {
  previewTaskImageUrl.value = ''
}

onMounted(() => {
  loadTasks()
  startTaskListPolling()
})

onBeforeUnmount(() => {
  stopTaskListPolling()
  stopRetryPlanningNoticePoll()
  stopSelectedTaskPoll()
})
</script>

<style scoped>
.task-result-image-wrap {
  position: relative;
  margin: 0 0 14px;
}

.task-result-image {
  width: 100%;
  max-height: 260px;
  display: block;
  object-fit: contain;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel-muted);
}

.task-result-preview {
  position: absolute;
  right: 10px;
  top: 10px;
  z-index: 2;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 999px;
  color: #fff;
  background: rgba(17, 18, 23, 0.72);
  box-shadow: 0 6px 16px rgba(17, 18, 23, 0.22);
  backdrop-filter: blur(8px);
  cursor: pointer;
  transition: transform 0.16s ease, background 0.16s ease;
}

.task-result-preview:hover {
  transform: translateY(-1px);
  background: rgba(17, 18, 23, 0.86);
}

.task-image-missing {
  margin: 0 0 14px;
  padding: 12px 14px;
  border: 1px dashed var(--line-strong);
  border-radius: 8px;
  color: var(--muted);
  background: var(--panel-muted);
  line-height: 1.6;
}

.story-task-detail-dialog :deep(.el-dialog__body) {
  max-height: 76vh;
  overflow-y: auto;
}

.story-task-sections {
  display: grid;
  gap: 14px;
}

.story-task-sections .result-section {
  min-height: 0;
}

.story-task-sections .result-section.is-plan-confirmation {
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.task-section-actions {
  display: flex;
  justify-content: flex-end;
  margin: 12px 0 0;
}

.story-task-sections .result-section pre {
  max-height: 260px;
  overflow-y: auto;
  scrollbar-gutter: stable;
}
</style>
