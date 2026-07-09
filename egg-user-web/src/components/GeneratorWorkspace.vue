<template>
  <div class="page generator-workbench">
    <section class="generator-page-title">
      <span class="eyebrow-pill">
        <el-icon><MagicStick /></el-icon>
        {{ eyebrow }}
      </span>
      <h1>{{ title }}</h1>
      <p>{{ desc }}</p>
    </section>

    <section class="process-steps" aria-label="生成流程">
      <template v-for="(step, index) in steps" :key="step">
        <span class="process-step" :class="{ 'is-active': index === 0, 'is-muted': index > 0 }">
          <strong>{{ index + 1 }}</strong>
          <span>{{ step }}</span>
        </span>
        <span v-if="index < steps.length - 1" class="process-line"></span>
      </template>
    </section>

    <div class="generator-layout">
      <section class="generator-sidebar" aria-label="生成参数">
        <div class="generator-panel upload-panel">
          <div class="panel-heading compact-heading">
            <span class="panel-icon">
              <el-icon><Picture /></el-icon>
            </span>
            <div>
              <h2>产品图</h2>
              <p>{{ uploadLabel }}</p>
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
          <div class="asset-tabs" role="tablist" aria-label="生成类型">
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
            <label class="field-block">
              <span>目标平台</span>
              <el-select v-model="form.platform" style="width:100%">
                <el-option label="抖音" value="抖音" />
                <el-option label="淘宝/天猫" value="淘宝/天猫" />
                <el-option label="小红书" value="小红书" />
                <el-option label="Amazon" value="Amazon" />
              </el-select>
            </label>

            <label class="field-block">
              <span>{{ form.assetType }}要求</span>
              <div class="prompt-shell">
                <el-input
                  v-model="form.prompt"
                  type="textarea"
                  :rows="5"
                  placeholder="建议输入：产品名称、卖点、目标人群、目标电商平台、图片风格等"
                />
                <button class="prompt-helper" type="button">
                  <el-icon><MagicStick /></el-icon>
                  AI 帮写
                </button>
              </div>
            </label>

            <label class="field-block">
              <span>目标语言</span>
              <el-select v-model="form.language" style="width:100%">
                <el-option label="中文(简体)" value="zh-CN" />
                <el-option label="English" value="en" />
              </el-select>
            </label>

            <div class="form-grid-two">
              <label class="field-block">
                <span>模型</span>
                <el-select v-model="form.model" style="width:100%">
                  <el-option label="Nova 2.0" value="Nova 2.0" />
                  <el-option label="Nova 2.0 Lite" value="Nova 2.0 Lite" />
                  <el-option label="Nova Pro" value="Nova Pro" />
                  <el-option label="Nova IMG-2" value="Nova IMG-2" />
                </el-select>
              </label>
              <label class="field-block">
                <span>尺寸比例</span>
                <el-select v-model="form.ratio" style="width:100%">
                  <el-option label="3:4 竖版" value="3:4" />
                  <el-option label="1:1 正方形" value="1:1" />
                  <el-option label="4:5 竖图" value="4:5" />
                </el-select>
              </label>
              <label class="field-block">
                <span>清晰度</span>
                <el-select v-model="form.quality" style="width:100%">
                  <el-option label="2K 高清" value="2K" />
                  <el-option label="1K 标准" value="1K" />
                </el-select>
              </label>
              <label class="field-block">
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


        <el-button class="generate-button" type="primary" size="large" :icon="MagicStick">
          分析产品
        </el-button>
      </section>

      <section class="generator-panel result-panel" aria-label="生成结果">
        <div class="panel-heading result-heading">
          <span class="panel-icon">
            <el-icon><MagicStick /></el-icon>
          </span>
          <div>
            <h2>生成结果</h2>
            <p>上传产品图并点击分析开始</p>
          </div>
        </div>

        <div class="result-canvas">
          <div class="result-empty">
            <span class="empty-symbol">
              <el-icon><MagicStick /></el-icon>
            </span>
            <p>上传产品图并填写要求后<br />点击分析产品开始</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, shallowRef, useTemplateRef } from 'vue'
import { Close, MagicStick, Picture, Upload } from '@element-plus/icons-vue'
import { selectImageFiles, UPLOAD_LIMIT } from '@/utils/imageUpload'

defineProps({
  eyebrow: { type: String, default: 'AI 生成' },
  title: { type: String, required: true },
  desc: { type: String, required: true },
  uploadLabel: { type: String, default: '拖拽或点击上传商品图 / 参考图' }
})

const assetTypes = [
  { label: '主图' },
  { label: '详情图' }
]

const form = reactive({
  assetType: '主图',
  platform: '抖音',
  ratio: '3:4',
  count: 1,
  language: 'zh-CN',
  model: 'Nova 2.0',
  quality: '2K',
  prompt: ''
})

const steps = ['输入', '分析中', '确认规划', '生成中', '完成']

const fileInput = useTemplateRef('fileInput')
const uploadedImages = shallowRef([])
const isDragging = shallowRef(false)
let nextImageId = 0
let dragDepth = 0

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