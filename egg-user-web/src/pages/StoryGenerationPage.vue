<template>
  <div class="story-generation-page">
    <GeneratorWorkspace
      eyebrow="AI 剧情生成"
      title="剧情生成"
      desc="输入剧情主题和产品信息，AI一键生成完整剧情、剧本、分镜脚本及人物三视图。"
      :steps="STORY_STEPS"
      :active-step="activeStep"
      :selected-step="selectedStep"
      :completed-steps="completedSteps"
      :failed-step="failedStep"
      :show-upload-panel="false"
      :show-asset-tabs="false"
      :show-prompt-helper="false"
      :show-quality-field="false"
      :show-count-field="false"
      :platform-options="platformOptions"
      prompt-before-platform
      prompt-field-label="剧情主题"
      prompt-placeholder="输入参考：轮流赡养老人的兄弟俩因为1万块钱起争执不管老人了"
      :show-product-info-field="true"
      product-info-label="产品信息"
      product-info-placeholder="输入参考：三全手工鲜肉馄饨，皮薄馅大，汤鲜味美"
      language-field-label="目标人群"
      :language-options="targetAudienceOptions"
      model-field-label="推荐风格"
      :model-options="recommendedStyleOptions"
      ratio-field-label="剧情时长"
      :ratio-options="storyDurationOptions"
      generate-button-label="开始生成"
      :result-title="resultTitle"
      :result-subtitle="resultSubtitle"
      :empty-message="emptyMessage"
      :generating="isCreating"
      :result-loading="isTaskGenerating"
      :result-sections="resultSections"
      :can-download="canDownload"
      @generate="handleGenerate"
      @select-step="handleSelectStep"
      @download="handleDownload"
    />

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
          <button type="button" class="story-task-notice-action" @click="goTaskCenter">
            去任务中心
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
        <button
          type="button"
          class="story-task-notice-close"
          aria-label="关闭任务提醒"
          @click="closeTaskNotice"
        >
          <el-icon><Close /></el-icon>
        </button>
      </aside>
    </Transition>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, CircleCheck, CircleClose, Close } from '@element-plus/icons-vue'
import GeneratorWorkspace from '@/components/GeneratorWorkspace.vue'
import { handleAuthFailure } from '@/utils/authGuard'
import {
  createStoryTask,
  downloadStoryTask,
  getStoryTask,
  listDictData
} from '@/api/story'

const STORY_STEPS = ['输入', '剧情生成', '剧本生成', '分镜生成', '分镜校验', '三视图/场景图', '完成']

const fallbackPlatformOptions = [
  { label: '抖音', value: '抖音' },
  { label: '快手', value: '快手' },
  { label: '视频号', value: '视频号' },
  { label: '小红书', value: '小红书' }
]

// 后台字典请求失败时仍要展示业务可用选项，兜底值与当前 story_* 字典保持一致。
const fallbackTargetAudienceOptions = [
  { label: '自动判断', value: '自动判断' },
  { label: '18-24岁年轻用户', value: '18-24岁年轻用户' },
  { label: '25-34岁都市青年', value: '25-34岁都市青年' },
  { label: '30-45岁家庭消费人群', value: '30-45岁家庭消费人群' },
  { label: '35-55岁实用型消费人群', value: '35-55岁实用型消费人群' },
  { label: '45-70岁中老年用户', value: '45-70岁中老年用户' },
  { label: '宝妈/宝爸', value: '宝妈/宝爸' },
  { label: '职场人', value: '职场人' },
  { label: '学生党', value: '学生党' },
  { label: '银发家庭', value: '银发家庭' }
]

const fallbackRecommendedStyleOptions = [
  { label: '自动判断', value: '自动判断' },
  { label: '强钩子反转', value: '强钩子反转' },
  { label: '家庭伦理', value: '家庭伦理' },
  { label: '温情治愈', value: '温情治愈' },
  { label: '轻喜剧', value: '轻喜剧' },
  { label: '接地气生活', value: '接地气生活' },
  { label: '熟人社会人情味', value: '熟人社会人情味' },
  { label: '小红书生活种草', value: '小红书生活种草' },
  { label: '精致日常', value: '精致日常' },
  { label: '情绪爽感', value: '情绪爽感' }
]

const fallbackStoryDurationOptions = [
  { label: '1分钟', value: '1分钟' },
  { label: '2分钟', value: '2分钟' },
  { label: '3分钟', value: '3分钟' },
  { label: '4分钟', value: '4分钟' },
  { label: '5分钟', value: '5分钟' }
]

const platformOptions = ref(fallbackPlatformOptions)
const targetAudienceOptions = ref(fallbackTargetAudienceOptions)
const recommendedStyleOptions = ref(fallbackRecommendedStyleOptions)
const storyDurationOptions = ref(fallbackStoryDurationOptions)
const currentTask = ref(null)
const selectedStep = ref('输入')
const isCreating = ref(false)
const autoFollowLatest = ref(true)
const taskNotice = ref(null)
const route = useRoute()
const router = useRouter()
let pollTimer = null

const activeStep = computed(() => currentTask.value?.currentNode || '输入')
const failedStep = computed(() => currentTask.value?.status === 'failed' ? currentTask.value.currentNode : '')
const activeStepIndex = computed(() => Math.max(STORY_STEPS.indexOf(activeStep.value), 0))
const completedSteps = computed(() => {
  if (!currentTask.value) {
    return []
  }

  if (currentTask.value.status === 'completed') {
    return STORY_STEPS
  }

  return STORY_STEPS.filter((_, index) => index < activeStepIndex.value)
})
const isTaskGenerating = computed(() => isCreating.value || currentTask.value?.status === 'processing')
const isSelectedActiveStep = computed(() => selectedStep.value === activeStep.value)
const selectedStepOutput = computed(() => {
  if (!currentTask.value || selectedStep.value === '输入' || selectedStep.value === '完成') {
    return null
  }

  return outputByStep(currentTask.value, selectedStep.value)
})
const isSelectedStepGenerating = computed(() => (
  isTaskGenerating.value
  && isSelectedActiveStep.value
  && !Boolean(selectedStepOutput.value?.content)
))
const resultTitle = computed(() => isSelectedStepGenerating.value ? '生成中' : '生成结果')
const resultSubtitle = computed(() => {
  if (!currentTask.value) {
    return '输入剧情主题和产品信息，点击开始生成'
  }

  const statusText = currentTask.value.status === 'completed'
    ? '已完成'
    : currentTask.value.status === 'failed'
      ? '生成失败'
      : '后台生成中'
  return `${statusText} · 当前节点：${currentTask.value.currentNode}`
})
const emptyMessage = computed(() => {
  if (!currentTask.value) {
    return '请先输入你的剧情主题和产品信息吧'
  }

  if (currentTask.value.status === 'failed') {
    return currentTask.value.failureReason || '任务生成失败，请在任务中心查看已完成内容'
  }

  return '后台正在按节点生成，完成一个节点后会显示在这里'
})
const resultSections = computed(() => {
  if (
    !currentTask.value
    || (!completedSteps.value.includes(selectedStep.value) && !isSelectedActiveStep.value)
  ) {
    return []
  }

  if (selectedStep.value === '完成') {
    return allTaskSections(currentTask.value)
  }

  if (selectedStep.value === '输入') {
    return [{ title: '输入参数', content: inputSummary(currentTask.value) }]
  }

  const output = selectedStepOutput.value
  if (isSelectedStepGenerating.value) {
    return [{ title: selectedStep.value, content: '正在生成' }]
  }
  return output ? [output] : []
})
const canDownload = computed(() => currentTask.value?.status === 'completed' && selectedStep.value === '完成')

const normalizeDictOptions = (items, fallback) => {
  if (!Array.isArray(items) || !items.length) {
    return fallback
  }

  const options = items
    .filter((item) => item.status === undefined || item.status === null || item.status === '' || item.status === '0' || item.status === 0 || item.status === '正常')
    .map((item) => ({
      label: item.dictLabel || item.label || item.dictValue || item.value,
      value: item.dictValue || item.value || item.dictLabel || item.label
    }))
    .filter((item) => item.label && item.value)

  return options.length ? options : fallback
}

const applyDictionaryResult = (result, target, fallback, dictType) => {
  if (result.status === 'fulfilled') {
    target.value = normalizeDictOptions(result.value, fallback)
    return
  }

  // 字典异常时不阻断页面，但保留日志方便排查代理、鉴权或后端未重启的问题。
  console.warn(`[StoryGeneration] ${dictType} 字典加载失败`, result.reason)
  target.value = fallback
}

const loadDictionaries = async () => {
  const [platforms, audiences, styles, durations] = await Promise.allSettled([
    listDictData('story_platform'),
    listDictData('story_target_audience'),
    listDictData('story_style'),
    listDictData('story_duration')
  ])

  applyDictionaryResult(platforms, platformOptions, fallbackPlatformOptions, 'story_platform')
  applyDictionaryResult(audiences, targetAudienceOptions, fallbackTargetAudienceOptions, 'story_target_audience')
  applyDictionaryResult(styles, recommendedStyleOptions, fallbackRecommendedStyleOptions, 'story_style')
  applyDictionaryResult(durations, storyDurationOptions, fallbackStoryDurationOptions, 'story_duration')
}

const validateStoryForm = (form) => {
  if (!form.prompt?.trim()) {
    ElMessage.warning('请填写剧情主题')
    return false
  }
  if (!form.productInfo?.trim()) {
    ElMessage.warning('请填写产品信息')
    return false
  }
  return true
}

const toStoryPayload = (form) => ({
  storyTheme: form.prompt.trim(),
  productInfo: form.productInfo.trim(),
  targetPlatform: form.platform,
  targetAudience: form.language,
  storyStyle: form.model,
  storyDuration: form.ratio
})

const handleGenerate = async (form) => {
  if (!validateStoryForm(form)) {
    return
  }

  isCreating.value = true
  autoFollowLatest.value = true
  closeTaskNotice()
  stopPolling()
  try {
    const task = await createStoryTask(toStoryPayload(form))
    currentTask.value = task
    selectedStep.value = activeStep.value
    ElMessage.success('剧情任务已创建，后台正在生成')
    await refreshCurrentTask()
    startPolling()
  } catch (error) {
    if (handleAuthFailure(error, router, route.fullPath)) {
      return
    }
    ElMessage.error(error.message || '创建剧情任务失败')
  } finally {
    isCreating.value = false
  }
}

const handleSelectStep = (step) => {
  selectedStep.value = step
  autoFollowLatest.value = false
}

const handleDownload = async (format) => {
  if (!currentTask.value?.id) {
    return
  }

  try {
    await downloadStoryTask(currentTask.value.id, format)
  } catch (error) {
    if (handleAuthFailure(error, router, route.fullPath)) {
      return
    }
    ElMessage.error(error.message || '下载失败')
  }
}

const refreshCurrentTask = async () => {
  if (!currentTask.value?.id) {
    return
  }

  const task = await getStoryTask(currentTask.value.id)
  currentTask.value = task
  syncSelectedStep()
  if (task.status === 'completed' || task.status === 'failed') {
    stopPolling()
    showTaskNotice(task)
  }
}

const showTaskNotice = (task) => {
  if (task.status === 'completed') {
    taskNotice.value = {
      type: 'success',
      title: '任务已完成',
      message: '当前任务已完成，请到任务中心查看'
    }
    return
  }

  if (task.status === 'failed') {
    taskNotice.value = {
      type: 'danger',
      title: '任务生成中断',
      message: '当前任务生成中断，请到任务中心查看原因或重试'
    }
  }
}

const closeTaskNotice = () => {
  taskNotice.value = null
}

const goTaskCenter = () => {
  closeTaskNotice()
  router.push('/tasks')
}

const startPolling = () => {
  if (!currentTask.value || currentTask.value.status === 'completed' || currentTask.value.status === 'failed') {
    return
  }

  pollTimer = window.setInterval(() => {
    refreshCurrentTask().catch((error) => {
      if (handleAuthFailure(error, router, route.fullPath)) {
        stopPolling()
        return
      }
      ElMessage.error(error.message || '刷新任务进度失败')
      stopPolling()
    })
  }, 2500)
}

const stopPolling = () => {
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

const syncSelectedStep = () => {
  if (currentTask.value?.status === 'completed') {
    selectedStep.value = '完成'
    return
  }

  if (autoFollowLatest.value) {
    selectedStep.value = activeStep.value
    return
  }

  if (!completedSteps.value.includes(selectedStep.value) && !isSelectedActiveStep.value) {
    selectedStep.value = activeStep.value
  }
}

const inputSummary = (task) => [
  `剧情主题：${task.storyTheme || ''}`,
  `产品信息：${task.productInfo || ''}`,
  `目标平台：${task.targetPlatform || ''}`,
  `目标人群：${task.targetAudience || ''}`,
  `推荐风格：${task.storyStyle || ''}`,
  `剧情时长：${task.storyDuration || ''}`
].join('\n')

const outputByStep = (task, step) => {
  const outputs = {
    剧情生成: { title: '剧情生成', content: task.storyOutput },
    剧本生成: { title: '剧本生成', content: task.scriptOutput },
    分镜生成: { title: '分镜生成', content: task.storyboardOutput },
    分镜校验: { title: '分镜校验', content: task.storyboardCheckOutput },
    '三视图/场景图': { title: '三视图/场景图', content: task.visualOutput }
  }
  return outputs[step]
}

const allTaskSections = (task) => [
  { title: '输入参数', content: inputSummary(task) },
  { title: '剧情生成', content: task.storyOutput },
  { title: '剧本生成', content: task.scriptOutput },
  { title: '分镜生成', content: task.storyboardOutput },
  { title: '分镜校验', content: task.storyboardCheckOutput },
  { title: '三视图/场景图', content: task.visualOutput }
]

onMounted(() => {
  loadDictionaries().catch(() => {
    platformOptions.value = fallbackPlatformOptions
    targetAudienceOptions.value = fallbackTargetAudienceOptions
    recommendedStyleOptions.value = fallbackRecommendedStyleOptions
    storyDurationOptions.value = fallbackStoryDurationOptions
  })
})

onBeforeUnmount(() => {
  stopPolling()
  closeTaskNotice()
})
</script>
