<template>
  <div class="page">
    <section class="section-title">
      <div>
        <h2>任务中心</h2>
        <p class="muted">查看剧情生成和商品图任务的进度、失败原因和节点内容。</p>
      </div>
      <el-button :loading="isLoading" @click="loadTasks">刷新</el-button>
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
        <article v-for="section in selectedTaskSections" :key="section.title" class="result-section">
          <h3>{{ section.title }}</h3>
          <img
            v-if="section.imageUrl && !deletedImageUrls[section.imageUrl]"
            class="task-result-image"
            :src="section.imageUrl"
            :alt="section.title"
            @error="markImageDeleted(section.imageUrl)"
          />
          <p
            v-else-if="section.imageMissing || (section.imageUrl && deletedImageUrls[section.imageUrl])"
            class="task-image-missing"
          >
            图片已删除
          </p>
          <pre>{{ section.content || '未生成' }}</pre>
        </article>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadStoryTask, getStoryTask, listStoryTasks, retryStoryTask } from '@/api/story'
import {
  cancelProductImageTask,
  getProductImageTask,
  listProductImageTasks,
  retryProductImageTask
} from '@/api/productImage'

const TASK_KIND_STORY = 'story'
const TASK_KIND_PRODUCT_IMAGE = 'product_image'
const STORY_STEPS = ['输入', '剧情生成', '剧本生成', '分镜生成', '分镜校验', '三视图/场景图', '完成']
const PRODUCT_IMAGE_STEPS = ['输入', '分析中', '确认规划', '生成中', '完成']

const tasks = ref([])
const selectedTask = ref(null)
const isLoading = ref(false)
const isDetailDialogVisible = ref(false)
const retryingTaskIds = ref({})
const cancellingTaskIds = ref({})
const deletedImageUrls = ref({})

const selectedTaskSections = computed(() => {
  if (!selectedTask.value) {
    return []
  }

  return allTaskSections(selectedTask.value)
})

const loadTasks = async () => {
  isLoading.value = true
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
    if (errors.length && tasks.value.length) {
      ElMessage.warning(errors.join('；'))
    } else if (errors.length) {
      ElMessage.error(errors.join('；'))
    }
  } catch (error) {
    ElMessage.error(error.message || '任务列表加载失败')
  } finally {
    isLoading.value = false
  }
}

const viewTask = async (task) => {
  try {
    const detail = task.taskKind === TASK_KIND_PRODUCT_IMAGE
      ? normalizeProductImageTask(await getProductImageTask(task.id))
      : normalizeStoryTask(await getStoryTask(task.id))
    deletedImageUrls.value = {}
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

const retryTask = async (task) => {
  retryingTaskIds.value = { ...retryingTaskIds.value, [task.id]: true }
  try {
    if (task.taskKind === TASK_KIND_PRODUCT_IMAGE) {
      await retryProductImageTask(task.id)
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
  const imageUrl = generation.ossUrl || ''

  return [
    { title: '输入摘要', content: productInputSummary(task) },
    { title: 'AI 分析', content: jsonText(task.analysisOutput) },
    { title: '确认规划', content: jsonText(task.planningOutput) },
    {
      title: '生成结果',
      content: jsonText(task.generationOutput),
      imageUrl,
      imageMissing: task.status === 'completed' && !imageUrl
    }
  ]
}

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

const markImageDeleted = (url) => {
  if (!url) {
    return
  }
  deletedImageUrls.value = { ...deletedImageUrls.value, [url]: true }
}

onMounted(() => {
  loadTasks()
})
</script>

<style scoped>
.task-result-image {
  width: 100%;
  max-height: 260px;
  display: block;
  object-fit: contain;
  margin: 0 0 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel-muted);
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
</style>
