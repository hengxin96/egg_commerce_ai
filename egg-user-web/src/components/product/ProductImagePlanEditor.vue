<template>
  <section class="product-plan-editor" :class="{ 'is-embedded': props.embedded }" aria-label="商品图规划确认">
    <div class="product-plan-editor-header">
      <div>
        <p class="product-plan-editor-kicker">确认规划</p>
        <h2>生成前校准</h2>
        <p class="product-plan-editor-subtitle">核对视觉分析、整体设计规范和每张图的执行方案。</p>
      </div>
    </div>

    <div class="product-plan-editor-scroll">
      <div class="product-plan-summary" aria-label="规划摘要">
        <div v-for="item in planStats" :key="item.label" class="product-plan-summary-item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>

      <div class="product-plan-editor-flow">
        <article class="product-plan-editor-card product-plan-analysis-card">
          <div class="product-plan-card-heading">
            <span>01</span>
            <h3>AI 视觉分析</h3>
          </div>
          <dl class="product-plan-editor-list">
            <template v-for="item in analysisItems" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value }}</dd>
            </template>
          </dl>
        </article>

        <article class="product-plan-editor-card product-plan-design-card">
          <div class="product-plan-card-heading">
            <span>02</span>
            <h3>整体设计规范</h3>
          </div>
          <label class="product-plan-editor-field product-plan-field">
            <span>可编辑摘要</span>
            <el-input v-model="designText" type="textarea" :rows="5" :disabled="props.readonly" />
          </label>

          <div v-if="designSections.length" class="product-plan-design-groups" aria-label="整体设计规范明细">
            <section v-for="section in designSections" :key="section.title" class="product-plan-readonly-section">
              <h4>{{ section.title }}</h4>
              <dl class="product-plan-readonly-list">
                <template v-for="row in section.rows" :key="`${section.title}-${row.label}`">
                  <dt>{{ row.label }}</dt>
                  <dd>{{ row.value }}</dd>
                </template>
              </dl>
            </section>
          </div>

          <label v-if="!props.readonly" class="product-plan-editor-field product-plan-field product-plan-replan-field">
            <span>重新规划补充要求</span>
            <el-input
              v-model="replanRequirement"
              type="textarea"
              :rows="3"
              placeholder="可补充新的主图/详情图要求，不填则沿用当前要求"
            />
          </label>
        </article>
      </div>

      <article class="product-plan-editor-card product-plan-schemes">
        <div class="product-plan-card-heading">
          <span>03</span>
          <h3>分图方案</h3>
        </div>
        <div v-if="editableImagePlans.length" class="product-plan-editor-plans">
          <div v-for="(plan, index) in editableImagePlans" :key="plan.promptId || index" class="product-plan-item">
            <div class="product-plan-item-header">
              <label class="product-plan-field product-plan-title-field">
                <span class="product-plan-title-label">
                  <span class="product-plan-index-badge">{{ index + 1 }}</span>
                  方案标题
                </span>
                <el-input v-model="plan.title" :disabled="props.readonly" />
              </label>
            </div>

            <div class="product-plan-item-body">
              <label class="product-plan-field is-wide">
                <span>设计目标</span>
                <el-input v-model="plan.designTarget" type="textarea" :rows="3" :disabled="props.readonly" />
              </label>

              <div v-if="plan.readonlyGroups.length" class="product-plan-readonly-groups is-wide">
                <section v-for="group in plan.readonlyGroups" :key="group.title" class="product-plan-readonly-section">
                  <h4>{{ group.title }}</h4>
                  <dl class="product-plan-readonly-list">
                    <template v-for="row in group.rows" :key="`${plan.promptId || index}-${group.title}-${row.label}`">
                      <dt>{{ row.label }}</dt>
                      <dd>{{ row.value }}</dd>
                    </template>
                  </dl>
                </section>
              </div>

              <template v-if="props.textEnabled">
                <label class="product-plan-field">
                  <span>主标题文案</span>
                  <el-input v-model="plan.overlayText" :disabled="props.readonly" />
                </label>
                <label class="product-plan-field">
                  <span>副标题/补充文案</span>
                  <el-input v-model="plan.overlaySubtitle" :disabled="props.readonly" />
                </label>
                <label class="product-plan-field is-wide">
                  <span>卖点文案</span>
                  <el-input v-model="plan.overlaySellingPoints" type="textarea" :rows="2" :disabled="props.readonly" />
                </label>
                <label class="product-plan-field">
                  <span>文字位置</span>
                  <el-input v-model="plan.textLayout" :disabled="props.readonly" />
                </label>
                <label class="product-plan-field">
                  <span>融合字效</span>
                  <el-input v-model="plan.textEffect" type="textarea" :rows="2" :disabled="props.readonly" />
                </label>
              </template>
            </div>
          </div>
        </div>
        <p v-else class="product-plan-empty">当前规划未返回分图方案，可重新规划。</p>
      </article>

      <div v-if="!props.readonly" class="product-plan-editor-actions">
        <el-button :loading="props.replanning" @click="emitReplan">重新规划</el-button>
        <el-button type="primary" :loading="props.confirming" @click="emitConfirm">确认生成</el-button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, shallowRef, watch } from 'vue'

const props = defineProps({
  analysisOutput: { type: String, default: '' },
  planningOutput: { type: String, default: '' },
  textEnabled: { type: Boolean, default: false },
  confirming: { type: Boolean, default: false },
  replanning: { type: Boolean, default: false },
  embedded: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
  userRequirement: { type: String, default: '' }
})

const emit = defineEmits(['confirm', 'replan'])

const editablePlan = shallowRef({})
const designText = shallowRef('')
const replanRequirement = shallowRef('')
const editableImagePlans = shallowRef([])

const parseJsonObject = (value) => {
  if (!value || value === '{}') {
    return {}
  }
  try {
    return JSON.parse(value)
  } catch {
    return {}
  }
}

const analysis = computed(() => parseJsonObject(props.analysisOutput))
const sourceSummary = computed(() => analysis.value.sourceSummary || {})
const designSections = computed(() => designSpecSections(editablePlan.value.overallDesignSpec || {}))

const analysisItems = computed(() => [
  { label: '商品名称', value: valueText(sourceSummary.value.productNameGuess, '未识别') },
  { label: '品类', value: valueText(sourceSummary.value.categoryGuess, '未识别') },
  { label: '主体保护', value: valueText(sourceSummary.value.identityGuards, '保持商品颜色、结构、logo 和数量') },
  { label: '材质/结构', value: valueText(sourceSummary.value.materials, sourceSummary.value.mainVisualFeatures, '未识别') },
  { label: '风险提示', value: valueText(sourceSummary.value.riskWarnings, '暂无明显风险') }
])

const planStats = computed(() => [
  { label: '分图方案', value: `${editableImagePlans.value.length || 0} 张` },
  { label: '文字模式', value: props.textEnabled ? '带文字' : '无文字' },
  { label: '确认状态', value: editableImagePlans.value.length ? '可确认' : '需重新规划' }
])

watch(
  () => props.planningOutput,
  (value) => {
    const plan = parseJsonObject(value)
    editablePlan.value = plan
    designText.value = designSummary(plan.overallDesignSpec)
    editableImagePlans.value = normalizeImagePlans(plan)
  },
  { immediate: true }
)

watch(
  () => props.userRequirement,
  (value) => {
    replanRequirement.value = value || ''
  },
  { immediate: true }
)

function designSummary(spec = {}) {
  if (!isPlainObject(spec)) {
    return ''
  }
  const visualStyle = objectValue(spec.visualStyle)
  const atmosphere = objectValue(visualStyle.atmosphere)
  const lines = [
    labelledText('视觉风格', valueText(visualStyle.keywords, spec.styleKeywords)),
    labelledText('情绪关键词', valueText(atmosphere.emotionKeywords, spec.emotionKeywords)),
    labelledText('光影效果', valueText(atmosphere.lightingEffect, spec.lighting)),
    labelledText('构图', valueText(spec.composition, spec.compositionSystem)),
    labelledText('材质', valueText(spec.materialRendering)),
    labelledText('色彩', valueText(spec.colorSystem))
  ]
  return lines.filter(Boolean).join('\n')
}

function designSpecSections(spec = {}) {
  if (!isPlainObject(spec)) {
    return []
  }
  const visualStyle = objectValue(spec.visualStyle)
  const atmosphere = objectValue(visualStyle.atmosphere)
  const typography = objectValue(spec.typographySystem || spec.fontSystem)
  const visualLanguage = objectValue(spec.visualLanguage)
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

function normalizeImagePlans(plan = {}) {
  const plans = Array.isArray(plan.imagePlans) && plan.imagePlans.length
    ? plan.imagePlans
    : Array.isArray(plan.imagePrompts) && plan.imagePrompts.length
      ? plan.imagePrompts
      : plan.imagePrompt
        ? [plan.imagePrompt]
        : []

  return plans.map((item, index) => {
    const overlay = item.textOverlay || {}
    const typography = overlay.typography || item.typography || {}
    const textEffect = [
      typography.fontMood,
      typography.color,
      typography.shadow,
      typography.stroke,
      typography.lightIntegration,
      typography.effectStyle,
      overlay.styleDescription,
      item.textEffect
    ].filter(Boolean).join('；')

    return {
      ...item,
      title: item.title || item.sectionName || `方案 ${index + 1}`,
      designTarget: item.designTarget || item.purpose || '',
      overlayText: overlay.title || item.overlayText || '',
      overlaySubtitle: overlay.subtitle || item.overlaySubtitle || '',
      overlaySellingPoints: valueText(overlay.sellingPoints, item.overlaySellingPoints, item.sellingPoints),
      textLayout: overlay.position || typography.position || item.textLayout || '',
      textEffect: cleanDisplayText(textEffect),
      promptPreview: cleanDisplayText(item.positivePrompt || item.baseImagePrompt || ''),
      readonlyGroups: imagePlanGroups(item)
    }
  })
}

function imagePlanGroups(item = {}) {
  const content = objectValue(item.contentElements)
  return filterSections([
    {
      title: '图像定位',
      rows: [
        row('图片类型', item.imageType, item.sectionName, item.purpose),
        row('产品复杂结构判定', item.productComplexity, item.isComplexProduct),
        row('图中图元素', item.inImageReference, item.pictureInPicture)
      ]
    },
    {
      title: '视觉执行',
      rows: [
        row('视觉风格锚点', item.visualStyleAnchor, item.styleAnchor),
        row('产品出现', item.productPresence),
        row('产品还原', item.productRestoration, item.productIdentity)
      ]
    },
    {
      title: '内容要素',
      rows: [
        row('展示重点', content.displayFocus, content.showcaseFocus, item.displayFocus),
        row('突出卖点', content.sellingPoint, content.sellingPoints, item.sellingPoint),
        row('痛点场景', content.painPointScenario, content.painPoint, item.painPointScenario),
        row('背景元素', content.backgroundElements, item.backgroundElements),
        row('视觉元素', content.visualElements, item.visualElements)
      ]
    },
    {
      title: '文字内容',
      rows: [
        row('文案规划', item.textContent, item.copyPlan),
        row('文字限制', item.textRestriction, item.textPolicy)
      ]
    }
  ])
}

function mergedPlanningOutput() {
  const nextPlan = {
    ...editablePlan.value,
    userEditedDesignText: designText.value,
    imagePlans: editableImagePlans.value.map((item) => {
      const {
        overlayText,
        overlaySubtitle,
        overlaySellingPoints,
        textLayout,
        textEffect,
        promptPreview,
        readonlyGroups,
        ...rawPlan
      } = item
      const typography = rawPlan.textOverlay?.typography || {}

      // Only user-editable fields are merged back; read-only display helpers are kept out of the persisted plan JSON.
      return {
        ...rawPlan,
        title: item.title || rawPlan.title || rawPlan.sectionName || '',
        designTarget: item.designTarget || rawPlan.designTarget || rawPlan.purpose || '',
        textOverlay: {
          ...(rawPlan.textOverlay || {}),
          enabled: props.textEnabled,
          title: props.textEnabled ? (overlayText || rawPlan.textOverlay?.title || '') : '',
          subtitle: props.textEnabled ? (overlaySubtitle || rawPlan.textOverlay?.subtitle || '') : '',
          sellingPoints: props.textEnabled ? splitTextList(overlaySellingPoints) : [],
          typography: {
            ...typography,
            position: textLayout || typography.position || '',
            effectStyle: textEffect || typography.effectStyle || ''
          }
        }
      }
    })
  }
  return JSON.stringify(nextPlan)
}

function filterSections(sections) {
  return sections
    .map((section) => ({
      ...section,
      rows: section.rows.filter((item) => item.value)
    }))
    .filter((section) => section.rows.length)
}

function colorRows(colorSystem) {
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

function row(label, ...values) {
  return { label, value: valueText(...values) }
}

function labelledText(label, value) {
  return value ? `${label}：${value}` : ''
}

function valueText(...values) {
  for (const value of values) {
    const text = cleanDisplayText(readableText(value))
    if (text) {
      return text
    }
  }
  return ''
}

function readableText(value) {
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
  return String(value)
}

function humanLabel(key) {
  const labels = {
    keywords: '关键词',
    emotionKeywords: '情绪关键词',
    lightingEffect: '光影效果',
    primaryColor: '主色调',
    secondaryColor: '辅助色',
    accentColor: '点缀色',
    backgroundColor: '背景色',
    displayFocus: '展示重点',
    sellingPoint: '突出卖点',
    sellingPoints: '突出卖点',
    painPointScenario: '痛点场景',
    backgroundElements: '背景元素',
    visualElements: '视觉元素'
  }
  return labels[key] || key
}

function objectValue(value) {
  return isPlainObject(value) ? value : {}
}

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
}

function splitTextList(value) {
  return String(value || '')
    .split(/[、,，;；\n]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function cleanDisplayText(value) {
  return String(value || '')
    .replace(/([、，。；：！？,.!?])\1+/g, '$1')
    .replace(/(^|\n)\s*[、，。；：！？,.!?]+\s*(?=\n|$)/g, '$1')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function emitConfirm() {
  emit('confirm', { planningOutput: mergedPlanningOutput() })
}

function emitReplan() {
  emit('replan', {
    planningOutput: mergedPlanningOutput(),
    userRequirement: replanRequirement.value.trim()
  })
}
</script>

<style scoped>
.product-plan-editor {
  max-width: 1280px;
  margin: 18px auto 0;
  padding: 0 32px 32px;
}

.product-plan-editor.is-embedded {
  flex: 1;
  min-height: 0;
  max-width: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.product-plan-editor-scroll {
  min-height: 0;
  display: grid;
  gap: 14px;
}

.product-plan-editor.is-embedded .product-plan-editor-scroll {
  flex: 1;
  overflow-y: auto;
  padding-right: 6px;
  scrollbar-gutter: stable;
}

.product-plan-editor-header {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(17, 18, 23, 0.08);
}

.product-plan-editor-kicker {
  margin: 0 0 4px;
  color: var(--muted);
  font-size: 13px;
  font-weight: 760;
}

.product-plan-editor h2,
.product-plan-editor h3,
.product-plan-editor h4 {
  margin: 0;
  color: var(--text);
}

.product-plan-editor h2 {
  font-size: 22px;
  font-weight: 880;
}

.product-plan-editor-subtitle {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.5;
}

.product-plan-editor h3 {
  font-size: 15px;
  font-weight: 840;
}

.product-plan-editor h4 {
  font-size: 13px;
  font-weight: 840;
}

.product-plan-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.product-plan-summary-item {
  min-height: 62px;
  display: grid;
  align-content: center;
  gap: 5px;
  padding: 12px 14px;
  border: 1px solid rgba(17, 18, 23, 0.08);
  border-radius: 8px;
  background: #f7f8fa;
}

.product-plan-summary-item span {
  color: var(--muted);
  font-size: 12px;
  font-weight: 760;
}

.product-plan-summary-item strong {
  color: var(--text);
  font-size: 16px;
  font-weight: 880;
}

.product-plan-editor-actions {
  display: inline-flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 2px;
}

.product-plan-editor-flow {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 14px;
}

.product-plan-editor-card {
  min-width: 0;
  padding: 18px;
  border: 1px solid rgba(17, 18, 23, 0.08);
  border-radius: 8px;
  background: #fff;
}

.product-plan-design-card,
.product-plan-schemes {
  background: linear-gradient(180deg, #fff 0%, #fbfbfc 100%);
}

.product-plan-card-heading {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 14px;
}

.product-plan-card-heading span {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #fff;
  background: #111217;
  font-size: 12px;
  font-weight: 850;
}

.product-plan-editor-list {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 10px 14px;
  margin: 0;
  color: var(--text);
  font-size: 13px;
  line-height: 1.6;
}

.product-plan-editor-list dt {
  color: var(--muted);
  font-weight: 760;
}

.product-plan-editor-list dd {
  margin: 0;
  word-break: break-word;
}

.product-plan-field {
  display: grid;
  align-content: start;
  gap: 8px;
  min-width: 0;
  color: var(--muted);
  font-size: 13px;
  font-weight: 760;
}

.product-plan-field.is-wide {
  grid-column: 1 / -1;
}

.product-plan-editor :deep(.el-input__wrapper),
.product-plan-editor :deep(.el-textarea__inner) {
  border-radius: 8px;
  color: #4f5664;
  background: #fff;
  box-shadow: 0 0 0 1px rgba(17, 18, 23, 0.08) inset;
  font-weight: 720;
}

.product-plan-editor :deep(.el-textarea__inner) {
  line-height: 1.65;
}

.product-plan-design-groups,
.product-plan-readonly-groups {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 10px;
}

.product-plan-design-groups {
  margin-top: 14px;
}

.product-plan-readonly-section {
  min-width: 0;
  display: grid;
  gap: 9px;
  padding: 12px;
  border: 1px solid rgba(17, 18, 23, 0.07);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
}

.product-plan-readonly-list {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 7px 12px;
  margin: 0;
  color: #4f5664;
  font-size: 12px;
  line-height: 1.55;
}

.product-plan-readonly-list dt {
  color: var(--muted);
  font-weight: 760;
}

.product-plan-readonly-list dd {
  margin: 0;
  word-break: break-word;
}

.product-plan-editor-plans {
  display: grid;
  gap: 14px;
}

.product-plan-item {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid rgba(17, 18, 23, 0.08);
  border-radius: 8px;
  background: #f6f7f9;
}

.product-plan-item-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(17, 18, 23, 0.07);
}

.product-plan-title-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.product-plan-index-badge {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #fff;
  background: var(--text);
  font-size: 11px;
  line-height: 1;
  flex: 0 0 20px;
}

.product-plan-item-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px 14px;
}

.product-plan-empty {
  margin: 12px 0 0;
  color: var(--muted);
}

@media (max-width: 900px) {
  .product-plan-editor {
    padding: 0 18px 24px;
  }

  .product-plan-editor.is-embedded {
    padding: 0;
  }

  .product-plan-summary,
  .product-plan-editor-flow,
  .product-plan-item-body {
    grid-template-columns: 1fr;
  }

  .product-plan-field.is-wide {
    grid-column: auto;
  }

  .product-plan-readonly-list {
    grid-template-columns: 1fr;
  }

  .product-plan-editor-actions {
    justify-content: stretch;
  }

  .product-plan-editor-actions :deep(.el-button) {
    flex: 1;
  }
}
</style>
