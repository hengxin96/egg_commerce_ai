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
            <span class="upload-count">{{ effectiveUploadedImages.length }}/{{ UPLOAD_LIMIT }}</span>
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
            :class="{ 'is-dragging': isDragging, 'has-previews': effectiveUploadedImages.length, 'is-uploading': props.uploading }"
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
            <template v-if="effectiveUploadedImages.length">
              <ul class="upload-preview-list" aria-label="已上传产品图片">
                <li
                  v-for="(image, index) in effectiveUploadedImages"
                  :key="image.id"
                  class="upload-preview-item"
                  draggable="true"
                  @click.stop
                  @dragstart.stop="handlePreviewDragStart(index)"
                  @dragover.prevent.stop
                  @drop.prevent.stop="handlePreviewDrop(index)"
                >
                  <span class="upload-index">
                    {{ index + 1 }}
                  </span>
                  <button
                    class="upload-remove"
                    type="button"
                    :aria-label="`删除 ${image.name}`"
                    @click.stop.prevent="removeImage(image.id)"
                  >
                    <el-icon><Close /></el-icon>
                  </button>
                  <span
                    v-if="index === 0"
                    class="upload-primary"
                    :aria-label="`主参考图 ${image.name}`"
                  >
                    主参考
                  </span>
                  <img :src="image.url" :alt="image.name" />
                </li>
              </ul>
              <span class="upload-add-hint">{{ uploadHint }}</span>
            </template>
            <template v-else>
              <span class="upload-symbol">
                <el-icon v-if="props.uploading"><Loading /></el-icon>
                <el-icon v-else><Upload /></el-icon>
              </span>
              <span class="upload-copy">{{ props.uploading ? '图片上传中，请稍候' : '多图上传时建议仅上传必要的视角或 SKU 图，图片不是越多越好' }}</span>
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

            <div class="field-block" :style="promptFieldStyle">
              <span>{{ promptFieldLabel }}</span>
              <div class="prompt-shell" :class="{ 'has-helper': props.showPromptHelper }">
                <el-input
                  v-model="form.prompt"
                  type="textarea"
                  :rows="5"
                  :placeholder="props.promptPlaceholder"
                />
                <!-- 帮写按钮独立于输入项 label，避免浏览器把点击转成文本域聚焦而丢失业务事件。 -->
                <button
                  v-if="props.showPromptHelper"
                  class="prompt-helper"
                  type="button"
                  :class="{ 'is-loading': props.promptHelperLoading }"
                  :disabled="props.promptHelperLoading"
                  :aria-busy="props.promptHelperLoading"
                  @mousedown.stop.prevent
                  @click.stop.prevent="emitRewriteRequirement"
                >
                  <el-icon class="prompt-helper-main-icon">
                    <Loading v-if="props.promptHelperLoading" />
                    <MagicStick v-else />
                  </el-icon>
                  <span>{{ props.promptHelperLoading ? '帮写中' : 'AI帮写' }}</span>
                </button>
              </div>
            </div>

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
                  <el-option
                    v-for="option in countOptions"
                    :key="option"
                    :label="`${option} 张`"
                    :value="option"
                  />
                </el-select>
              </label>
              <label v-if="props.showTextEnabledField" class="field-block">
                <span>图片带文字</span>
                <el-select v-model="form.textEnabled" style="width:100%">
                  <el-option label="否" :value="false" />
                  <el-option label="是" :value="true" />
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
        <el-button
          v-if="props.showCancelButton"
          class="cancel-generate-button"
          size="large"
          @click="emit('cancel')"
        >
          终止
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
              <el-button
                v-for="option in props.downloadOptions"
                :key="option.value"
                :icon="Download"
                @click="emit('download', option.value)"
              >
                {{ option.label }}
              </el-button>
            </div>
            <article
              v-for="section in props.resultSections"
              :key="section.title"
              class="result-section"
              :class="{
                'is-loading-section': section.type === 'loading',
                'is-plan-confirmation': section.type === 'plan-confirmation'
              }"
            >
              <slot
                v-if="section.type === 'plan-confirmation'"
                name="plan-confirmation"
                :section="section"
              >
                <pre>{{ section.content || section.placeholder || '未生成' }}</pre>
              </slot>
              <template v-else-if="section.type === 'loading'">
                <h3>{{ section.title }}</h3>
                <div class="result-progress-state" role="status" aria-live="polite">
                  <span>{{ section.statusText || section.content || '正在处理，请稍候' }}</span>
                  <div class="result-progress-bar" aria-hidden="true">
                    <i></i>
                  </div>
                </div>
              </template>
              <template v-else>
                <h3>{{ section.title }}</h3>
                <ProductImageAuditBadge v-if="section.audit" :audit="section.audit" />
                <img v-if="section.imageUrl" class="result-image-preview" :src="section.imageUrl" :alt="section.title" />
                <pre>{{ section.content || section.placeholder || '未生成' }}</pre>
              </template>
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
import { computed, getCurrentInstance, onBeforeUnmount, reactive, shallowRef, useTemplateRef, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Close, Download, Loading, MagicStick, Picture, Upload } from '@element-plus/icons-vue'
import ProductImageAuditBadge from '@/components/product/ProductImageAuditBadge.vue'
import { selectImageFiles, UPLOAD_LIMIT } from '@/utils/imageUpload'
import { requireLogin } from '@/utils/authGuard'

// Parent contract: :uploaded-images, @upload-image, @remove-image, @cancel.
const emit = defineEmits([
  'generate',
  'select-step',
  'download',
  'upload-image',
  'remove-image',
  'reorder-image',
  'set-primary-image',
  'rewrite-requirement',
  'cancel'
])
const router = useRouter()
const instance = getCurrentInstance()

const props = defineProps({
  eyebrow: { type: String, default: 'AI 生成' },
  title: { type: String, required: true },
  desc: { type: String, required: true },
  uploadLabel: { type: String, default: '拖拽或点击上传商品图 / 参考图' },
  steps: { type: Array, default: () => ['输入', '分析中', '确认规划', '生成中', '完成'] },
  promptValue: { type: String, default: '' },
  resetKey: { type: [String, Number], default: 0 },
  activeStep: { type: String, default: '' },
  selectedStep: { type: String, default: '' },
  completedSteps: { type: Array, default: () => [] },
  failedStep: { type: String, default: '' },
  showUploadPanel: { type: Boolean, default: true },
  showAssetTabs: { type: Boolean, default: true },
  showPromptHelper: { type: Boolean, default: true },
  promptHelperLoading: { type: Boolean, default: false },
  showProductInfoField: { type: Boolean, default: false },
  showRatioField: { type: Boolean, default: true },
  showQualityField: { type: Boolean, default: true },
  showCountField: { type: Boolean, default: true },
  showTextEnabledField: { type: Boolean, default: true },
  uploadedImages: { type: Array, default: () => [] },
  uploading: { type: Boolean, default: false },
  showCancelButton: { type: Boolean, default: false },
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
      { label: 'wan2.7-image-pro', value: 'wan2.7-image-pro' }
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
  canDownload: { type: Boolean, default: false },
  downloadOptions: {
    type: Array,
    default: () => [
      { label: 'Word', value: 'docx' },
      { label: 'Markdown', value: 'md' }
    ]
  }
})

const MAIN_ASSET_TYPE = '主图'
const DETAIL_ASSET_TYPE = '详情图'
const mainImageCountOptions = [1, 2, 3, 4]
const detailImageCountOptions = [1, 2, 3, 4, 5, 6, 7, 8]

const assetTypes = [
  { label: MAIN_ASSET_TYPE },
  { label: DETAIL_ASSET_TYPE }
]

const firstOptionValue = (options, fallback) => options[0]?.value ?? fallback

const optionContainsValue = (options, value) => {
  return options.some((option) => option.value === value)
}

const form = reactive({
  assetType: MAIN_ASSET_TYPE,
  platform: firstOptionValue(props.platformOptions, '抖音'),
  ratio: firstOptionValue(props.ratioOptions, '3:4'),
  count: 1,
  language: firstOptionValue(props.languageOptions, 'zh-CN'),
  model: firstOptionValue(props.modelOptions, 'wan2.7-image-pro'),
  quality: '2K',
  textEnabled: false,
  prompt: '',
  productInfo: ''
})

const fileInput = useTemplateRef('fileInput')
const localUploadedImages = shallowRef([])
const isDragging = shallowRef(false)
const draggedPreviewIndex = shallowRef(null)
let nextImageId = 0
let dragDepth = 0

const hasExternalUploadState = computed(() => {
  const vnodeProps = instance?.vnode?.props || {}
  return Object.prototype.hasOwnProperty.call(vnodeProps, 'uploadedImages')
    || Object.prototype.hasOwnProperty.call(vnodeProps, 'uploaded-images')
})

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
const effectiveUploadedImages = computed(() => (
  hasExternalUploadState.value ? props.uploadedImages : localUploadedImages.value
))
const countOptions = computed(() => (
  form.assetType === DETAIL_ASSET_TYPE ? detailImageCountOptions : mainImageCountOptions
))

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
    syncOptionField('model', props.modelOptions, 'wan2.7-image-pro')
    syncOptionField('ratio', props.ratioOptions, '3:4')
  },
  { immediate: true }
)

watch(
  () => props.promptValue,
  (value) => {
    if (typeof value === 'string' && value !== form.prompt) {
      form.prompt = value
    }
  }
)

watch(
  () => form.assetType,
  () => {
    if (!countOptions.value.includes(form.count)) {
      form.count = countOptions.value[0]
    }
  }
)

const resetWorkspaceForm = () => {
  form.assetType = MAIN_ASSET_TYPE
  form.platform = firstOptionValue(props.platformOptions, '抖音')
  form.ratio = firstOptionValue(props.ratioOptions, '3:4')
  form.count = 1
  form.language = firstOptionValue(props.languageOptions, 'zh-CN')
  form.model = firstOptionValue(props.modelOptions, 'wan2.7-image-pro')
  form.quality = '2K'
  form.textEnabled = false
  form.prompt = ''
  form.productInfo = ''
  localUploadedImages.value.forEach((image) => URL.revokeObjectURL(image.url))
  localUploadedImages.value = []
}

watch(
  () => props.resetKey,
  () => {
    resetWorkspaceForm()
  }
)

const uploadHint = computed(() => {
  if (effectiveUploadedImages.value.length >= UPLOAD_LIMIT) {
    return '已达到 6 张上限，删除图片后可继续添加'
  }

  return '点击或拖入图片继续添加'
})

const openFilePicker = () => {
  if (props.uploading || effectiveUploadedImages.value.length >= UPLOAD_LIMIT) {
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
  const selectedFiles = selectImageFiles(files, effectiveUploadedImages.value.length)

  if (!selectedFiles.length) {
    return
  }

  if (hasExternalUploadState.value) {
    emit('upload-image', selectedFiles)
    return
  }

  const previews = selectedFiles.map((file) => ({
    id: `${Date.now()}-${nextImageId += 1}`,
    name: file.name || `产品图 ${nextImageId}`,
    url: URL.createObjectURL(file),
    file
  }))

  localUploadedImages.value = [...localUploadedImages.value, ...previews]
}

const emitRewriteRequirement = () => {
  emit('rewrite-requirement', { ...form })
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

  event.dataTransfer.dropEffect = props.uploading || effectiveUploadedImages.value.length >= UPLOAD_LIMIT ? 'none' : 'copy'
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
  if (props.uploading) {
    return
  }
  addFiles(event.dataTransfer?.files)
}

const removeImage = (imageId) => {
  if (hasExternalUploadState.value) {
    emit('remove-image', imageId)
    return
  }

  const image = localUploadedImages.value.find((item) => item.id === imageId)

  if (image) {
    URL.revokeObjectURL(image.url)
  }

  localUploadedImages.value = localUploadedImages.value.filter((item) => item.id !== imageId)
}

const persistPreviewOrder = (nextImages, primaryImageId = nextImages[0]?.id) => {
  if (!nextImages.length) {
    return
  }
  if (!hasExternalUploadState.value) {
    localUploadedImages.value = nextImages
  }
  emit('reorder-image', {
    imageIds: nextImages.map((image) => image.id),
    primaryImageId
  })
}

const handlePreviewDragStart = (index) => {
  draggedPreviewIndex.value = index
}

const handlePreviewDrop = (index) => {
  const fromIndex = draggedPreviewIndex.value
  draggedPreviewIndex.value = null
  if (fromIndex === null || fromIndex === index) {
    return
  }
  const nextImages = [...effectiveUploadedImages.value]
  const [moved] = nextImages.splice(fromIndex, 1)
  nextImages.splice(index, 0, moved)
  persistPreviewOrder(nextImages)
}

onBeforeUnmount(() => {
  localUploadedImages.value.forEach((image) => URL.revokeObjectURL(image.url))
})
</script>

<style scoped>
.prompt-shell {
  position: relative;
}

.prompt-shell.has-helper :deep(.el-textarea__inner) {
  padding-right: 16px !important;
  padding-bottom: 50px !important;
}

.prompt-shell .prompt-helper {
  position: absolute;
  right: 14px;
  bottom: 14px;
  z-index: 2;
  min-width: 82px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 0 10px;
  border: 1px solid rgba(17, 18, 23, 0.08);
  border-radius: 999px;
  color: #111217;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 3px 10px rgba(17, 18, 23, 0.09);
  cursor: pointer;
  font-size: 13px;
  font-weight: 780;
  white-space: nowrap;
  transition: transform 0.12s ease, box-shadow 0.12s ease, background 0.12s ease;
}

.prompt-helper:hover {
  background: #fff;
  box-shadow: 0 7px 18px rgba(17, 18, 23, 0.12);
}

.prompt-helper:active {
  transform: translateY(1px);
  box-shadow: 0 3px 8px rgba(17, 18, 23, 0.1);
}

.prompt-helper.is-loading,
.prompt-helper:disabled {
  cursor: wait;
  opacity: 0.78;
}

.prompt-helper-main-icon {
  font-size: 15px;
}

.upload-preview-item {
  position: relative;
}

.upload-index,
.upload-primary {
  position: absolute;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  font-weight: 820;
}

.upload-index {
  left: 4px;
  top: 4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(17, 18, 23, 0.62);
  box-shadow: 0 2px 8px rgba(17, 18, 23, 0.12);
}

.upload-primary {
  left: 50%;
  bottom: 8px;
  min-width: 52px;
  height: 22px;
  padding: 0 7px;
  transform: translateX(-50%);
  color: #111217;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 3px 10px rgba(17, 18, 23, 0.1);
  border-radius: 999px;
}

.prompt-helper.is-loading .prompt-helper-main-icon {
  animation: prompt-helper-spin 1s linear infinite;
}

.result-canvas,
.result-content,
.result-section {
  min-height: 0;
}

.result-content {
  min-height: 0;
  overflow: hidden;
}

.result-section {
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.result-section.is-plan-confirmation {
  overflow: hidden;
}

.result-section.is-loading-section {
  justify-content: flex-start;
}

.result-progress-state {
  flex: 0 0 auto;
  min-height: 108px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  gap: 14px;
  margin-top: 12px;
  padding: 18px;
  border: 1px solid rgba(17, 18, 23, 0.08);
  border-radius: 8px;
  color: var(--muted);
  background: #fff;
  font-size: 14px;
  font-weight: 760;
  box-shadow: 0 8px 24px rgba(17, 18, 23, 0.04);
}

.result-progress-bar {
  position: relative;
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(17, 18, 23, 0.08);
}

.result-progress-bar i {
  position: absolute;
  inset: 0 auto 0 0;
  width: 42%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(17, 18, 23, 0.22), rgba(17, 18, 23, 0.72));
  animation: result-progress-slide 1.2s ease-in-out infinite;
}

.result-section pre {
  max-height: 320px;
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.result-content.is-single-section .result-section pre {
  flex: 1;
  min-height: 0;
}

@keyframes prompt-helper-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes result-progress-slide {
  0% {
    transform: translateX(-105%);
  }
  55% {
    transform: translateX(85%);
  }
  100% {
    transform: translateX(245%);
  }
}
</style>
