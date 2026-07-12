<template>
  <div class="page">
    <section class="section-title">
      <div>
        <h2>任务中心</h2>
        <p class="muted">查看剧情生成任务的进度、失败原因和已生成内容。</p>
      </div>
      <el-button :loading="isLoading" @click="loadTasks">刷新</el-button>
    </section>

    <div class="table-panel">
      <el-table v-loading="isLoading" :data="tasks" style="width:100%">
        <el-table-column label="任务名称" min-width="240">
          <template #default="{ row }">{{ row.storyTheme || '剧情生成任务' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default>剧情生成</template>
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
        <el-table-column prop="failureReason" label="失败原因" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'failed'"
              link
              type="primary"
              :loading="Boolean(retryingTaskIds[row.id])"
              @click="retryTask(row)"
            >
              重试
            </el-button>
            <el-button link type="primary" @click="viewTask(row)">查看内容</el-button>
            <el-button link type="primary" @click="download(row, 'docx')">下载为Word</el-button>
            <el-button link type="primary" @click="download(row, 'md')">下载为MD</el-button>
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
          <h3>{{ selectedTask?.storyTheme || '剧情生成任务' }}</h3>
          <p v-if="selectedTask" class="muted">
            {{ statusLabel(selectedTask.status) }} · 当前节点：{{ selectedTask.currentNode }}
          </p>
        </div>
      </template>

      <div v-if="selectedTask" class="story-task-sections">
        <article v-for="section in selectedTaskSections" :key="section.title" class="result-section">
          <h3>{{ section.title }}</h3>
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

const STORY_STEPS = ['输入', '剧情生成', '剧本生成', '分镜生成', '分镜校验', '三视图/场景图', '完成']

const tasks = ref([])
const selectedTask = ref(null)
const isLoading = ref(false)
const isDetailDialogVisible = ref(false)
const retryingTaskIds = ref({})

const selectedTaskSections = computed(() => {
  if (!selectedTask.value) {
    return []
  }

  return allTaskSections(selectedTask.value)
})

const loadTasks = async () => {
  isLoading.value = true
  try {
    tasks.value = await listStoryTasks()
    if (!selectedTask.value && tasks.value.length) {
      selectedTask.value = tasks.value[0]
    }
  } catch (error) {
    ElMessage.error(error.message || '任务列表加载失败')
  } finally {
    isLoading.value = false
  }
}

const viewTask = async (task) => {
  try {
    selectedTask.value = await getStoryTask(task.id)
    isDetailDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '任务详情加载失败')
  }
}

const download = async (task, format) => {
  try {
    await downloadStoryTask(task.id, format)
  } catch (error) {
    ElMessage.error(error.message || '下载失败')
  }
}

const retryTask = async (task) => {
  retryingTaskIds.value = { ...retryingTaskIds.value, [task.id]: true }
  try {
    await retryStoryTask(task.id)
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

const statusLabel = (status) => ({
  processing: '生成中',
  completed: '成功',
  failed: '失败'
}[status] || status || '未知')

const statusClass = (status) => ({
  processing: 'warning',
  completed: 'success',
  failed: 'danger'
}[status] || 'warning')

const progress = (task) => {
  if (task.status === 'completed') {
    return 100
  }

  const nodeIndex = Math.max(STORY_STEPS.indexOf(task.currentNode), 0)
  return Math.round((nodeIndex / (STORY_STEPS.length - 1)) * 100)
}

const inputSummary = (task) => [
  `剧情主题：${task.storyTheme || ''}`,
  `产品信息：${task.productInfo || ''}`,
  `目标平台：${task.targetPlatform || ''}`,
  `目标人群：${task.targetAudience || ''}`,
  `推荐风格：${task.storyStyle || ''}`,
  `剧情时长：${task.storyDuration || ''}`
].join('\n')

const allTaskSections = (task) => [
  { title: '输入参数', content: inputSummary(task) },
  { title: '剧情生成', content: task.storyOutput },
  { title: '剧本生成', content: task.scriptOutput },
  { title: '分镜生成', content: task.storyboardOutput },
  { title: '分镜校验', content: task.storyboardCheckOutput },
  { title: '三视图/场景图', content: task.visualOutput }
]

onMounted(() => {
  loadTasks()
})
</script>
