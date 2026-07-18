<template>
  <div class="page product-detail-page">
    <section class="section-title">
      <div>
        <h2>{{ asset?.name || '商品详情' }}</h2>
        <p class="muted">
          {{ assetTypeLabel(asset?.assetType) }} · {{ asset?.targetPlatform || '抖音' }} · {{ formatDate(asset?.updatedAt || asset?.createdAt) }}
        </p>
      </div>
      <div class="product-detail-actions">
        <el-button :loading="isReusing" @click="reuseAsset">继续使用</el-button>
        <el-button type="danger" :loading="isDeleting" @click="deleteAsset">删除</el-button>
      </div>
    </section>

    <div v-loading="isLoading" class="product-detail-layout">
      <section class="generator-panel product-detail-panel">
        <h3>生成图片</h3>
        <div v-if="generatedImages.length" class="product-generated-downloads">
          <SelectableImageGallery
            :images="generatedImages"
            :selected-ids="selectedGeneratedImageIds"
            empty-text="暂无生成图片"
            @update:selected-ids="selectedGeneratedImageIds = $event"
          />
          <div class="product-download-actions">
            <el-button
              type="primary"
              :disabled="!selectedGeneratedImageIds.length"
              @click="downloadSelectedImagesNormal"
            >
              普通下载
            </el-button>
            <el-button
              :disabled="!selectedGeneratedImageIds.length"
              @click="downloadSelectedImagesZip"
            >
              ZIP下载
            </el-button>
          </div>
        </div>
        <el-empty v-else description="暂无生成图片" />
      </section>

      <section class="generator-panel product-detail-panel">
        <h3>商品信息</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="商品名称">{{ asset?.name || '未命名商品' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ assetTypeLabel(asset?.assetType) }}</el-descriptions-item>
          <el-descriptions-item label="平台">{{ asset?.targetPlatform || '抖音' }}</el-descriptions-item>
          <el-descriptions-item label="比例">{{ asset?.aspectRatio || '-' }}</el-descriptions-item>
          <el-descriptions-item label="清晰度">{{ asset?.quality || '-' }}</el-descriptions-item>
          <el-descriptions-item label="图片要求">{{ asset?.userRequirement || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="生成时间">{{ formatDate(asset?.updatedAt || asset?.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="designSpecSections.length" class="product-design-spec">
          <h4>整体设计规范</h4>
          <div class="product-design-spec-grid">
            <section v-for="section in designSpecSections" :key="section.title" class="product-design-spec-section">
              <h5>{{ section.title }}</h5>
              <dl class="product-design-spec-list">
                <template v-for="row in section.rows" :key="`${section.title}-${row.label}`">
                  <dt>{{ row.label }}</dt>
                  <dd>{{ row.value }}</dd>
                </template>
              </dl>
            </section>
          </div>
        </div>
      </section>
    </div>

    <section class="generator-panel product-detail-panel">
      <h3>上传产品图</h3>
      <div v-if="sourceImages.length" class="product-image-grid">
        <figure v-for="image in sourceImages" :key="image.id">
          <img :src="image.ossUrl" :alt="image.analysisRole || '上传产品图'" />
          <figcaption>{{ image.analysisRole || image.ossObjectKey || '产品图' }}</figcaption>
        </figure>
      </div>
      <el-empty v-else description="暂无上传产品图" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import SelectableImageGallery from '@/components/product/SelectableImageGallery.vue'
import {
  deleteProductAsset,
  downloadProductAssetImage,
  downloadProductAssetImagesZip,
  getProductAsset,
  reuseProductAsset
} from '@/api/productImage'
import { handleAuthFailure } from '@/utils/authGuard'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const isLoading = ref(false)
const isReusing = ref(false)
const isDeleting = ref(false)
const selectedGeneratedImageIds = ref([])

const asset = computed(() => detail.value?.asset || null)
const sourceImages = computed(() => detail.value?.sourceImages || [])
const auditOutput = computed(() => parseJsonObject(detail.value?.auditOutput))
const planningOutput = computed(() => parseJsonObject(detail.value?.planningOutput))
const overallDesignSpec = computed(() => planningOutput.value.overallDesignSpec || {})
const designSpecSections = computed(() => designSpecSectionRows(overallDesignSpec.value))
const auditByImageId = computed(() => {
  const map = {}
  for (const image of auditOutput.value.images || []) {
    for (const id of [image.imageId, image.finalImageId, image.id].filter(Boolean)) {
      map[id] = { ...image, auditSummary: auditOutput.value.auditSummary }
    }
  }
  return map
})
const generatedImages = computed(() => (detail.value?.generatedImages || []).map((image, index) => ({
  ...image,
  audit: auditForGeneratedImage(image, index)
})))

const loadDetail = async () => {
  isLoading.value = true
  try {
    detail.value = await getProductAsset(route.params.id)
    selectedGeneratedImageIds.value = []
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '商品详情加载失败')
      router.push('/products')
    }
  } finally {
    isLoading.value = false
  }
}

const reuseAsset = async () => {
  if (!asset.value?.id) {
    return
  }

  isReusing.value = true
  try {
    await reuseProductAsset(asset.value.id)
    ElMessage.success('已复制商品数据，可继续生成')
    router.push('/generate/product-images')
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '继续使用失败')
    }
  } finally {
    isReusing.value = false
  }
}

const downloadSelectedImagesNormal = async () => {
  if (!selectedGeneratedImageIds.value.length) {
    ElMessage.warning('请先选择要下载的图片')
    return
  }

  try {
    for (const imageId of selectedGeneratedImageIds.value) {
      await downloadProductAssetImage(imageId)
    }
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '下载失败')
    }
  }
}

const downloadSelectedImagesZip = async () => {
  if (!selectedGeneratedImageIds.value.length) {
    ElMessage.warning('请先选择要下载的图片')
    return
  }

  try {
    await downloadProductAssetImagesZip(selectedGeneratedImageIds.value)
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || 'ZIP下载失败')
    }
  }
}

const deleteAsset = async () => {
  if (!asset.value?.id) {
    return
  }

  try {
    await ElMessageBox.confirm(
      `删除「${asset.value.name || '未命名商品'}」后，数据库记录和 OSS 文件都会被删除，是否继续？`,
      '删除商品',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  isDeleting.value = true
  try {
    await deleteProductAsset(asset.value.id)
    ElMessage.success('已删除商品和 OSS 文件')
    router.push('/products')
  } catch (error) {
    if (!handleAuthFailure(error, router)) {
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    isDeleting.value = false
  }
}

const assetTypeLabel = (assetType) => ({
  main: '主图',
  detail: '详情图'
}[assetType] || '商品图')

const formatDate = (value) => {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

const parseJsonObject = (value) => {
  try {
    const parsed = JSON.parse(value || '{}')
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

const designSpecSectionRows = (spec = {}) => {
  if (!isPlainObject(spec)) {
    return []
  }
  const visualStyle = objectValue(spec.visualStyle)
  const atmosphere = objectValue(visualStyle.atmosphere)
  const typography = objectValue(spec.typographySystem || spec.fontSystem)
  const visualLanguage = objectValue(spec.visualLanguage)

  // Product planning JSON is nested; grouped rows keep product-detail display aligned with the confirmation node.
  return filterSections([
    {
      title: '视觉风格',
      rows: [
        row('关键词', visualStyle.keywords, spec.styleKeywords),
        row('风格描述', visualStyle.description, spec.atmosphere),
        row('构图', spec.composition, spec.compositionSystem)
      ]
    },
    {
      title: '氛围营造',
      rows: [
        row('情绪关键词', atmosphere.emotionKeywords, spec.emotionKeywords),
        row('光影效果', atmosphere.lightingEffect, spec.lighting),
        row('氛围描述', atmosphere.description, spec.atmosphere)
      ]
    },
    {
      title: '色彩系统',
      rows: colorRows(spec.colorSystem)
    },
    {
      title: '字体系统',
      rows: [
        row('字体气质', typography.fontMood, typography.fontStyle),
        row('标题层级', typography.hierarchy, typography.titleStyle),
        row('字体颜色', typography.colors, typography.titleColor),
        row('文字策略', spec.textStylePolicy, spec.noTextPolicy)
      ]
    },
    {
      title: '视觉语言',
      rows: [
        row('装饰元素', visualLanguage.decorativeElements, visualLanguage.decorations),
        row('图标风格', visualLanguage.iconStyle),
        row('视觉元素', visualLanguage.visualElements, spec.visualElements),
        row('商品身份保护', spec.productIdentityGuards)
      ]
    }
  ])
}

const filterSections = (sections) => sections
  .map((section) => ({
    ...section,
    rows: section.rows.filter((item) => item.value)
  }))
  .filter((section) => section.rows.length)

const colorRows = (colorSystem) => {
  if (Array.isArray(colorSystem)) {
    return colorSystem.map((item, index) => ({
      label: item.name || item.role || `颜色 ${index + 1}`,
      value: valueText([item.hex, item.usage, item.description])
    }))
  }
  const colors = objectValue(colorSystem)
  return [
    row('主色调', colors.primaryColor, colors.primary),
    row('辅助色', colors.secondaryColor, colors.secondary),
    row('点缀色', colors.accentColor, colors.accent),
    row('背景色', colors.backgroundColor, colors.background)
  ]
}

const row = (label, ...values) => ({ label, value: valueText(...values) })

const valueText = (...values) => {
  for (const value of values) {
    const text = cleanDisplayText(readableText(value))
    if (text) {
      return text
    }
  }
  return ''
}

const readableText = (value) => {
  if (value == null || value === false) {
    return ''
  }
  if (value === true) {
    return '是'
  }
  if (Array.isArray(value)) {
    return value.map(readableText).filter(Boolean).join('、')
  }
  if (isPlainObject(value)) {
    return Object.entries(value)
      .map(([key, item]) => {
        const text = readableText(item)
        return text ? `${humanLabel(key)}：${text}` : ''
      })
      .filter(Boolean)
      .join('；')
  }
  return String(value).trim()
}

const cleanDisplayText = (value) => String(value || '')
  .replace(/([、，。；：！？.!?])\1+/g, '$1')
  .trim()

const humanLabel = (key) => ({
  keywords: '关键词',
  emotionKeywords: '情绪关键词',
  lightingEffect: '光影效果',
  description: '描述',
  primaryColor: '主色调',
  secondaryColor: '辅助色',
  accentColor: '点缀色',
  backgroundColor: '背景色',
  decorativeElements: '装饰元素',
  visualElements: '视觉元素',
  fontMood: '字体气质',
  hierarchy: '标题层级'
}[key] || key)

const objectValue = (value) => (isPlainObject(value) ? value : {})

const isPlainObject = (value) => value && typeof value === 'object' && !Array.isArray(value)

const auditForGeneratedImage = (image, index) => auditByImageId.value[image.id]
  || (auditOutput.value.images?.[index]
    ? { ...auditOutput.value.images[index], auditSummary: auditOutput.value.auditSummary }
    : null)

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.product-detail-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.product-detail-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  align-items: start;
}

.product-detail-panel {
  padding: 24px;
}

.product-detail-panel h3 {
  margin: 0 0 16px;
}

.product-design-spec {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}

.product-design-spec h4 {
  margin: 0 0 12px;
  color: var(--text);
  font-size: 16px;
  font-weight: 850;
}

.product-design-spec-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}

.product-design-spec-section {
  min-width: 0;
  display: grid;
  gap: 9px;
  padding: 14px;
  border: 1px solid rgba(17, 18, 23, 0.07);
  border-radius: 8px;
  background: #fbfbfc;
}

.product-design-spec-section h5 {
  margin: 0;
  color: var(--text);
  font-size: 14px;
  font-weight: 850;
}

.product-design-spec-list {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 7px 12px;
  margin: 0;
  color: #4f5664;
  font-size: 12px;
  line-height: 1.55;
}

.product-design-spec dt {
  color: var(--muted);
  font-weight: 760;
}

.product-design-spec dd {
  margin: 0;
  color: var(--text);
  line-height: 1.7;
  word-break: break-word;
}

.product-image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 14px;
}

.product-image-grid.is-generated {
  grid-template-columns: minmax(0, 1fr);
}

.product-generated-downloads {
  display: grid;
  gap: 14px;
}

.product-download-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.product-image-grid figure {
  margin: 0;
  min-width: 0;
}

.product-image-grid img {
  width: 100%;
  aspect-ratio: 1 / 1;
  display: block;
  object-fit: contain;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel-muted);
}

.product-image-grid.is-generated img {
  max-height: 540px;
  aspect-ratio: auto;
}

.product-image-grid figcaption {
  margin-top: 8px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

@media (max-width: 900px) {
  .product-detail-layout {
    grid-template-columns: 1fr;
  }
}
</style>
