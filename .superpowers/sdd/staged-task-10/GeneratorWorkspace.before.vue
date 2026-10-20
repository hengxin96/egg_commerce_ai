<template>
  <div class="page generator-workbench">
    <section class="generator-page-title">
      <span class="eyebrow-pill">
        <el-icon><MagicStick /></el-icon>
        {{ props.eyebrow }}
      </span>
      <h1>{{ props.title }}</h1>
      <p>{{ props.desc }}</p>
    </section>

    <section class="process-steps" aria-label="生成流程">
      <template v-for="(step, index) in props.steps" :key="step">
        <button
          class="process-step"
          type="button"
          :disabled="!isStepSelectable(step)"
          :class="stepClasses(step)"
          @click="selectStep(step)"
        >
          <strong>{{ index + 1 }}</strong>
          <span>{{ step }}</span>
        </button>
        <span v-if="index < props.steps.length - 1" class="process-line"></span>
      </template>
    </section>

    <div class="generator-layout">
      <section class="generator-sidebar" aria-label="生成参数">
        <div v-if="props.showUploadPanel" class="generator-panel upload-panel">
          <div class="panel-heading compact-heading">
            <span class="panel-icon">
              <el-icon><Picture /></el-icon>
            </span>
            <div>
              <h2>产品图</h2>
              <p>{{ props.uploadLabel }}</p>
            </div>
            <span class="upload-count">{{ uploadedImages.length }}/{{ UPLOAD_LIMIT }}</span>
          </div>

          <input
            ref="fileInput"
            class="upload-input"
            type="file"
            accept="image/*"
            multiple
            @change="handleFileChange"
          />

          <div
            class="upload-drop"
            :class="{ 'is-dragging': isDragging, 'has-previews': uploadedImages.length }"
            role="button"
            tabindex="0"
            aria-label="上传产品图片"
            @click="openFilePicker"
            @keydown.enter.prevent="openFilePicker"
            @keydown.space.prevent="openFilePicker"
            @dragenter.prevent="handleDragEnter"
            @dragover.prevent="handleDragOver"
            @dragleave.prevent="handleDragLeave"
            @drop.prevent="handleDrop"
          >
            <template v-if="uploadedImages.length">
              <ul class="upload-preview-list" aria-label="已上传产品图片">
                <li v-for="image in uploadedImages" :key="image.id" class="upload-preview-item">
                  <button
                    class="upload-remove"
                    type="button"
                    :aria-label="`删除 ${image.name}`"
                    @click.stop="removeImage(image.id)"
                  >
                    <el-icon><Close /></el-icon>
                  </button>
                  <img :src="image.url" :alt="image.name" />
                </li>
              </ul>
              <span class="upload-add-hint">{{ uploadHint }}</span>
            </template>
            <template v-else>
              <span class="upload-symbol">
                <el-icon><Upload /></el-icon>
              </span>
              <span class="upload-copy">多图上传时建议仅上传必要的视角或 SKU 图，图片不是越多越好</span>
            </template>
          </div>
        </div>

        <div class="generator-panel controls-panel">
          <div v-if="props.showAssetTabs" class="asset-tabs" role="tablist" aria-label="生成类型">
            <button
              v-for="type in assetTypes"
              :key="type.label"
              type="button"
              role="tab"
              :aria-selected="form.assetType === type.label"
              :class="{ 'is-active': form.assetType === type.label }"
              @click="form.assetType = type.label"
            >
              {{ type.label }}
              <em v-if="type.badge">{{ type.badge }}</em>
            </button>
          </div>

          <div class="form-stack">
            <label class="field-block" :style="platformFieldStyle">
              <span>目标平台</span>
              <el-select v-model="form.platform" style="width:100%">
                <el-option
                  v-for="option in props.platformOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </label>

            <label class="field-block" :style="promptFieldStyle">
              <span>{{ promptFieldLabel }}</span>
              <div class="prompt-shell">
                <el-input
                  v-model="form.prompt"
                  type="textarea"
                  :rows="5"
                  :placeholder="props.promptPlaceholder"
                />
                <button v-if="props.showPromptHelper" class="prompt-helper" type="button">
                  <el-icon><MagicStick /></el-icon>
                  AI 帮写
                </button>
              </div>
            </label>

            <label v-if="props.showProductInfoField" class="field-block" :style="productInfoFieldStyle">
              <span>{{ props.productInfoLabel }}</span>
              <div class="prompt-shell">
                <el-input
                  v-model="form.productInfo"
                  type="textarea"
                  :rows="5"
                  :placeholder="props.productInfoPlaceholder"
                />
              </div>
            </label>

            <label class="field-block" :style="languageFieldStyle">
              <span>{{ props.languageFieldLabel }}</span>
              <el-select v-model="form.language" style="width:100%">
                <el-option
                  v-for="option in props.languageOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </label>

            <div class="form-grid-two" :style="advancedFieldsStyle">
              <label class="field-block">
                <span>{{ props.modelFieldLabel }}</span>
                <el-select v-model="form.model" style="width:100%">
                  <el-option
                    v-for="option in props.modelOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </label>
              <label v-if="props.showRatioField" class="field-block">
                <span>{{ props.ratioFieldLabel }}</span>
                <el-select v-model="form.ratio" style="width:100%">
                  <el-option
                    v-for="option in props.ratioOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </label>
              <label v-if="props.showQualityField" class="field-block">
                <span>清晰度</span>
                <el-select v-model="form.quality" style="width:100%">
                  <el-option label="2K 高清" value="2K" />
                  <el-option label="1K 标准" value="1K" />
                </el-select>
              </label>
              <label v-if="props.showCountField" class="field-block">
                <span>生成数量</span>
                <el-select v-model="form.count" style="width:100%">
                  <el-option label="1 张" :value="1" />
                  <el-option label="4 张" :value="4" />
                  <el-option label="8 张" :value="8" />
                </el-select>
              </label>
            </div>
          </div>
        </div>


        <el-button
          class="generate-button"
          type="primary"
          size="large"
          :icon="MagicStick"
          :loading="props.generating"
          @click="emitGenerate"
        >
          {{ props.generateButtonLabel }}
        </el-button>
      </section>

      <section class="generator-panel result-panel" aria-label="生成结果">
        <div class="panel-heading result-heading">
          <span class="panel-icon">
            <el-icon><MagicStick /></el-icon>
          </span>
          <div>
            <div class="result-title-row">
              <h2>{{ props.resultTitle }}</h2>
              <el-icon v-if="props.resultLoading" class="result-loading-icon"><Loading /></el-icon>
            </div>
            <p>{{ props.resultSubtitle }}</p>
          </div>
        </div>

        <div class="result-canvas">
          <div v-if="props.resultSections.length" class="result-content" :class="{ 'is-single-section': props.resultSections.length === 1 }">
            <div v-if="props.canDownload" class="result-actions">
              <el-button :icon="Download" @click="emit('download', 'docx')">Word</el-button>
              <el-button :icon="Download" @click="emit('download', 'md')">Markdown</el-button>
            </div>
            <article
              v-for="section in props.resultSections"
              :key="section.title"
              class="result-section"
            >
              <h3>{{ section.title }}</h3>
              <pre>{{ section.content || section.placeholder || '未生成' }}</pre>
            </article>
          </div>
          <div v-else class="result-empty">
            <span class="empty-symbol">
              <el-icon><MagicStick /></el-icon>
            </span>
            <p>
              <template v-for="(line, index) in emptyMessageLines" :key="line">
                <br v-if="index" />
                {{ line }}
              </template>
            </p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, shallowRef, useTemplateRef, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Close, Download, Loading, MagicStick, Picture, Upload } from '@element-plus/icons-vue'
import { selectImageFiles, UPLOAD_LIMIT } from '@/utils/imageUpload'
import { requireLogin } from '@/utils/authGuard'

const emit = defineEmits(['generate', 'select-step', 'download'])
const router = useRouter()

const props = defineProps({
  eyebrow: { type: String, default: 'AI 生成' },
  title: { type: String, required: true },
  desc: { type: String, required: true },
  uploadLabel: { type: String, default: '拖拽或点击上传商品图 / 参考图' },
  steps: { type: Array, default: () => ['输入', '分析中', '确认规划', '生成中', '完成'] },
  activeStep: { type: String, default: '' },
  selectedStep: { type: String, default: '' },
  completedSteps: { type: Array, default: () => [] },
  failedStep: { type: String, default: '' },
  showUploadPanel: { type: Boolean, default: true },
  showAssetTabs: { type: Boolean, default: true },
  showPromptHelper: { type: Boolean, default: true },
  showProductInfoField: { type: Boolean, default: false },
  showRatioField: { type: Boolean, default: true },
  showQualityField: { type: Boolean, default: true },
  showCountField: { type: Boolean, default: true },
  promptBeforePlatform: { type: Boolean, default: false },
  promptFieldLabel: { type: String, default: '' },
  promptPlaceholder: { type: String, default: '建议输入：产品名称、卖点、目标人群、目标电商平台、图片风格等' },
  productInfoLabel: { type: String, default: '产品信息' },
  productInfoPlaceholder: { type: String, default: '' },
  platformOptions: {
    type: Array,
    default: () => [
      { label: '抖音', value: '抖音' },
      { label: '淘宝/天猫', value: '淘宝/天猫' },
      { label: '小红书', value: '小红书' },
      { label: 'Amazon', value: 'Amazon' }
    ]
  },
  languageFieldLabel: { type: String, default: '目标语言' },
  languageOptions: {
    type: Array,
    default: () => [
      { label: '中文(简体)', value: 'zh-CN' },
      { label: 'English', value: 'en' }
    ]
  },
  modelFieldLabel: { type: String, default: '模型' },
  modelOptions: {
    type: Array,
    default: () => [
      { label: 'Nova 2.0', value: 'Nova 2.0' },
      { label: 'Nova 2.0 Lite', value: 'Nova 2.0 Lite' },
      { label: 'Nova Pro', value: 'Nova Pro' },
      { label: 'Nova IMG-2', value: 'Nova IMG-2' }
    ]
  },
  ratioFieldLabel: { type: String, default: '尺寸比例' },
  ratioOptions: {
    type: Array,
    default: () => [
      { label: '3:4 竖版', value: '3:4' },
      { label: '1:1 正方形', value: '1:1' },
      { label: '4:5 竖图', value: '4:5' }
    ]
  },
  generateButtonLabel: { type: String, default: '分析产品' },
  resultTitle: { type: String, default: '生成结果' },
  resultSubtitle: { type: String, default: '上传产品图并点击分析开始' },
  emptyMessage: { type: String, default: '上传产品图并填写要求后\n点击分析产品开始' },
  generating: { type: Boolean, default: false },
  resultLoading: { type: Boolean, default: false },
  resultSections: { type: Array, default: () => [] },
  canDownload: { type: Boolean, default: false }
})

const assetTypes = [
  { label: '主图' },
  { label: '详情图' }
]

const firstOptionValue = (options, fallback) => options[0]?.value ?? fallback

const optionContainsValue = (options, value) => {
  return options.some((option) => option.value === value)
}

const form = reactive({
  assetType: '主图',
  platform: firstOptionValue(props.platformOptions, '抖音'),
  ratio: firstOptionValue(props.ratioOptions, '3:4'),
  count: 1,
  language: firstOptionValue(props.languageOptions, 'zh-CN'),
  model: firstOptionValue(props.modelOptions, 'Nova 2.0'),
  quality: '2K',
  prompt: '',
  productInfo: ''
})

const fileInput = useTemplateRef('fileInput')
const uploadedImages = shallowRef([])
const isDragging = shallowRef(false)
let nextImageId = 0
let dragDepth = 0

// 剧情生成页会重排字段，但商品图等页面继续沿用默认顺序。
const fieldOrder = computed(() => (
  props.promptBeforePlatform
    ? { prompt: 1, productInfo: 2, platform: 3, language: 4, advanced: 5 }
    : { platform: 1, prompt: 2, productInfo: 3, language: 4, advanced: 5 }
))

const platformFieldStyle = computed(() => ({ order: fieldOrder.value.platform }))
const promptFieldStyle = computed(() => ({ order: fieldOrder.value.prompt }))
const productInfoFieldStyle = computed(() => ({ order: fieldOrder.value.productInfo }))
const languageFieldStyle = computed(() => ({ order: fieldOrder.value.language }))
const advancedFieldsStyle = computed(() => ({ order: fieldOrder.value.advanced }))
const promptFieldLabel = computed(() => props.promptFieldLabel || `${form.assetType}要求`)
const emptyMessageLines = computed(() => props.emptyMessage.split('\n'))
const activeStepName = computed(() => props.activeStep || props.steps[0])
const selectedStepName = computed(() => props.selectedStep || activeStepName.value)

const syncOptionField = (field, options, fallback) => {
  if (!options.length || optionContainsValue(options, form[field])) {
    return
  }

  form[field] = firstOptionValue(options, fallback)
}

// 字典数据是异步加载的；真实 options 到达后同步默认选中值，避免继续显示页面 fallback。
watch(
  () => [props.platformOptions, props.languageOptions, props.modelOptions, props.ratioOptions],
  () => {
    syncOptionField('platform', props.platformOptions, '抖音')
    syncOptionField('language', props.languageOptions, 'zh-CN')
    syncOptionField('model', props.modelOptions, 'Nova 2.0')
    syncOptionField('ratio', props.ratioOptions, '3:4')
  },
  { immediate: true }
)

const uploadHint = computed(() => {
  if (uploadedImages.value.length >= UPLOAD_LIMIT) {
    return '已达到 6 张上限，删除图片后可继续添加'
  }

  return '点击或拖入图片继续添加'
})

const openFilePicker = () => {
  if (uploadedImages.value.length >= UPLOAD_LIMIT) {
    return
  }

  fileInput.value?.click()
}

const isStepSelectable = (step) => (
  props.completedSteps.includes(step)
  || step === activeStepName.value
  || step === props.failedStep
)

const stepClasses = (step) => ({
  'is-active': selectedStepName.value === step,
  'is-current': activeStepName.value === step,
  'is-completed': props.completedSteps.includes(step),
  'is-selected-completed': selectedStepName.value === step
    && props.completedSteps.includes(step)
    && activeStepName.value !== step,
  'is-failed': props.failedStep === step,
  'is-muted': !props.completedSteps.includes(step) && activeStepName.value !== step
})

const selectStep = (step) => {
  if (!isStepSelectable(step)) {
    return
  }
  emit('select-step', step)
}

const emitGenerate = () => {
  if (!requireLogin(router)) {
    return
  }

  emit('generate', { ...form })
}

const addFiles = (files) => {
  const selectedFiles = selectImageFiles(files, uploadedImages.value.length)

  if (!selectedFiles.length) {
    return
  }

  const previews = selectedFiles.map((file) => ({
    id: `${Date.now()}-${nextImageId += 1}`,
    name: file.name || `产品图 ${nextImageId}`,
    url: URL.createObjectURL(file),
    file
  }))

  uploadedImages.value = [...uploadedImages.value, ...previews]
}

const handleFileChange = (event) => {
  addFiles(event.target.files)
  event.target.value = ''
}

const hasImagePayload = (dataTransfer) => {
  return Array.from(dataTransfer?.items ?? []).some((item) => (
    item.kind === 'file' && item.type.startsWith('image/')
  ))
}

const handleDragEnter = (event) => {
  if (!hasImagePayload(event.dataTransfer)) {
    return
  }

  dragDepth += 1
  isDragging.value = true
}

const handleDragOver = (event) => {
  if (!hasImagePayload(event.dataTransfer)) {
    return
  }

  event.dataTransfer.dropEffect = uploadedImages.value.length >= UPLOAD_LIMIT ? 'none' : 'copy'
  isDragging.value = true
}

const handleDragLeave = () => {
  dragDepth = Math.max(dragDepth - 1, 0)

  if (!dragDepth) {
    isDragging.value = false
  }
}

const handleDrop = (event) => {
  dragDepth = 0
  isDragging.value = false
  addFiles(event.dataTransfer?.files)
}

const removeImage = (imageId) => {
  const image = uploadedImages.value.find((item) => item.id === imageId)

  if (image) {
    URL.revokeObjectURL(image.url)
  }

  uploadedImages.value = uploadedImages.value.filter((item) => item.id !== imageId)
}

onBeforeUnmount(() => {
  uploadedImages.value.forEach((image) => URL.revokeObjectURL(image.url))
})
</script>
