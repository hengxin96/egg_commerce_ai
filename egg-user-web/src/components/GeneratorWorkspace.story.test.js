import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('story generation page configures the shared workspace without changing defaults', () => {
  const page = readSource('../pages/StoryGenerationPage.vue')
  const workspace = readSource('./GeneratorWorkspace.vue')

  assert.match(page, /const STORY_STEPS = \['输入', '剧情生成', '剧本生成', '分镜生成', '分镜校验', '三视图\/场景图', '完成'\]/)
  assert.match(page, /:steps="STORY_STEPS"/)
  assert.match(page, /:active-step="activeStep"/)
  assert.match(page, /:completed-steps="completedSteps"/)
  assert.match(page, /:result-sections="resultSections"/)
  assert.match(page, /@generate="handleGenerate"/)
  assert.match(page, /@download="handleDownload"/)
  assert.match(page, /:show-upload-panel="false"/)
  assert.match(page, /:show-asset-tabs="false"/)
  assert.match(page, /:show-prompt-helper="false"/)
  assert.match(page, /:show-quality-field="false"/)
  assert.match(page, /:show-count-field="false"/)
  assert.match(page, /prompt-field-label="剧情主题"/)
  assert.match(page, /prompt-placeholder="输入参考：轮流赡养老人的兄弟俩因为1万块钱起争执不管老人了"/)
  assert.match(page, /product-info-label="产品信息"/)
  assert.match(page, /product-info-placeholder="输入参考：三全手工鲜肉馄饨，皮薄馅大，汤鲜味美"/)
  assert.match(page, /language-field-label="目标人群"/)
  assert.match(page, /model-field-label="推荐风格"/)
  assert.match(page, /ratio-field-label="剧情时长"/)
  assert.match(page, /generate-button-label="开始生成"/)
  assert.match(page, /:result-subtitle="resultSubtitle"/)
  assert.match(page, /:empty-message="emptyMessage"/)

  assert.match(workspace, /showUploadPanel/)
  assert.match(workspace, /showAssetTabs/)
  assert.match(workspace, /showPromptHelper/)
  assert.match(workspace, /showProductInfoField/)
  assert.match(workspace, /showQualityField/)
  assert.match(workspace, /showCountField/)
  assert.match(workspace, /resultSections/)
  assert.match(workspace, /canDownload/)
})

test('shared workspace syncs async dictionary options into select values', () => {
  const workspace = readSource('./GeneratorWorkspace.vue')

  assert.match(workspace, /import \{[^}]*watch[^}]*\} from 'vue'/s)
  assert.match(workspace, /syncOptionField\('language', props\.languageOptions/)
  assert.match(workspace, /syncOptionField\('model', props\.modelOptions/)
  assert.match(workspace, /syncOptionField\('ratio', props\.ratioOptions/)
})

test('story generation fallback options mirror admin story dictionaries', () => {
  const page = readSource('../pages/StoryGenerationPage.vue')

  assert.match(page, /18-24岁年轻用户/)
  assert.match(page, /45-70岁中老年用户/)
  assert.match(page, /强钩子反转/)
  assert.match(page, /小红书生活种草/)
  assert.match(page, /1分钟/)
  assert.match(page, /5分钟/)
  assert.doesNotMatch(page, /30秒/)
  assert.doesNotMatch(page, /60秒/)
})

test('story result output boxes scroll inside fixed-size content areas', () => {
  const styles = readFileSync(new URL('../styles/main.css', import.meta.url), 'utf8')

  assert.match(styles, /\.generator-workbench \.result-section pre\s*\{[^}]*height:/s)
  assert.match(styles, /\.generator-workbench \.result-section pre\s*\{[^}]*overflow-y:\s*auto/s)
})

test('shared workspace keeps prompt helper anchored and clickable during rewrite', () => {
  const workspace = readSource('./GeneratorWorkspace.vue')
  const styles = readFileSync(new URL('../styles/main.css', import.meta.url), 'utf8')

  assert.match(workspace, /promptHelperLoading/)
  assert.match(workspace, /:disabled="props\.promptHelperLoading"/)
  assert.match(workspace, /'is-loading': props\.promptHelperLoading/)
  assert.match(workspace, /帮写中/)
  assert.doesNotMatch(workspace, /QuestionFilled/)
  assert.doesNotMatch(workspace, /prompt-helper-help/)
  assert.match(workspace, /AI帮写/)
  assert.match(workspace, /\.prompt-shell \.prompt-helper\s*\{[^}]*position:\s*absolute;[^}]*right:\s*14px;[^}]*bottom:\s*14px;[^}]*min-width:\s*82px;[^}]*height:\s*28px/s)
  assert.match(workspace, /\.prompt-helper:active\s*\{[^}]*transform:\s*translateY\(1px\)/s)
  assert.match(workspace, /\.prompt-shell\.has-helper :deep\(\.el-textarea__inner\)\s*\{[^}]*padding-right:\s*16px !important/s)
  assert.match(workspace, /\.prompt-shell\.has-helper :deep\(\.el-textarea__inner\)\s*\{[^}]*padding-bottom:\s*50px !important/s)
  assert.doesNotMatch(workspace, /\.prompt-shell\.has-helper :deep\(\.el-textarea__inner\)\s*\{[^}]*padding-right:\s*112px !important/s)
  assert.match(styles, /\.generator-workbench \.prompt-helper\s*\{[^}]*min-width:\s*82px;[^}]*height:\s*28px;[^}]*right:\s*12px;[^}]*bottom:\s*12px;/s)
  assert.doesNotMatch(styles, /\.generator-workbench \.prompt-helper\s*\{[^}]*bottom:\s*7px;/s)
})

test('shared workspace result nodes keep output text inside scroll panes', () => {
  const workspace = readSource('./GeneratorWorkspace.vue')

  assert.match(workspace, /\.result-content\s*\{[^}]*min-height:\s*0;[^}]*overflow:\s*hidden/s)
  assert.match(workspace, /\.result-section\s*\{[^}]*min-height:\s*0;[^}]*display:\s*flex/s)
  assert.match(workspace, /\.result-section pre\s*\{[^}]*max-height:\s*320px;[^}]*overflow-y:\s*auto/s)
})

test('shared workspace limits product image counts by asset type', () => {
  const workspace = readSource('./GeneratorWorkspace.vue')

  assert.match(workspace, /const mainImageCountOptions = \[1, 2, 3, 4\]/)
  assert.match(workspace, /const detailImageCountOptions = \[1, 2, 3, 4, 5, 6, 7, 8\]/)
  assert.match(workspace, /const countOptions = computed/)
  assert.match(workspace, /v-for="option in countOptions"/)
  assert.match(workspace, /watch\(\s*\(\) => form\.assetType/s)
  assert.match(workspace, /if \(!countOptions\.value\.includes\(form\.count\)\)/)
})
test('story workspace shows generating title with spinner while backend is running', () => {
  const page = readSource('../pages/StoryGenerationPage.vue')
  const workspace = readSource('./GeneratorWorkspace.vue')

  assert.match(page, /:result-title="resultTitle"/)
  assert.match(page, /:result-loading="isTaskGenerating"/)
  assert.match(workspace, /resultTitle/)
  assert.match(workspace, /resultLoading/)
  assert.match(workspace, /Loading/)
  assert.match(workspace, /result-loading-icon/)
})

test('story page shows result title when the selected node already has content', () => {
  const page = readSource('../pages/StoryGenerationPage.vue')

  assert.match(page, /const selectedStepOutput = computed/)
  assert.match(page, /const isSelectedStepGenerating = computed/)
  assert.match(page, /const resultTitle = computed\(\(\) => isSelectedStepGenerating\.value \? '生成中' : '生成结果'\)/)
  assert.match(page, /Boolean\(selectedStepOutput\.value\?\.content\)/)
})

test('current active node without output displays generating placeholder', () => {
  const page = readSource('../pages/StoryGenerationPage.vue')
  const workspace = readSource('./GeneratorWorkspace.vue')

  assert.match(page, /content: '正在生成'/)
  assert.match(page, /isSelectedStepGenerating\.value/)
  assert.match(workspace, /section\.content \|\| section\.placeholder \|\| '未生成'/)
})

test('single story result section fills the available result canvas', () => {
  const workspace = readSource('./GeneratorWorkspace.vue')
  const styles = readFileSync(new URL('../styles/main.css', import.meta.url), 'utf8')

  assert.match(workspace, /'is-single-section': props\.resultSections\.length === 1/)
  assert.match(styles, /\.result-content\.is-single-section\s*\{[^}]*grid-template-rows:\s*minmax\(0,\s*1fr\)/s)
  assert.match(styles, /\.result-content\.is-single-section \.result-section\s*\{[^}]*height:\s*100%/s)
  assert.match(styles, /\.result-content\.is-single-section \.result-section pre\s*\{[^}]*flex:\s*1/s)
})

test('story page follows the backend current node instead of staying on input', () => {
  const page = readSource('../pages/StoryGenerationPage.vue')

  assert.match(page, /selectedStep\.value = activeStep\.value/)
  assert.match(page, /selectedStep\.value === activeStep\.value/)
})

test('completed historical step selection uses success tint while current step stays primary', () => {
  const workspace = readSource('./GeneratorWorkspace.vue')
  const styles = readFileSync(new URL('../styles/main.css', import.meta.url), 'utf8')

  assert.match(workspace, /'is-selected-completed':\s*selectedStepName\.value === step/)
  assert.match(workspace, /props\.completedSteps\.includes\(step\)\s*&&\s*activeStepName\.value !== step/)
  assert.match(styles, /\.process-step\.is-selected-completed strong\s*\{[^}]*background:\s*#[0-9a-fA-F]{6}/s)
  assert.match(styles, /\.process-step\.is-active strong,\s*\.process-step\.is-current strong/s)
})

test('story page shows a bottom-right task completion or failure notice', () => {
  const page = readSource('../pages/StoryGenerationPage.vue')
  const styles = readFileSync(new URL('../styles/main.css', import.meta.url), 'utf8')

  assert.match(page, /taskNotice/)
  assert.match(page, /showTaskNotice\(task\)/)
  assert.match(page, /currentTask\.value = task[\s\S]*showTaskNotice\(task\)/)
  assert.match(page, /\u5f53\u524d\u4efb\u52a1\u5df2\u5b8c\u6210\uff0c\u8bf7\u5230\u4efb\u52a1\u4e2d\u5fc3\u67e5\u770b/)
  assert.match(page, /\u5f53\u524d\u4efb\u52a1\u751f\u6210\u4e2d\u65ad\uff0c\u8bf7\u5230\u4efb\u52a1\u4e2d\u5fc3\u67e5\u770b\u539f\u56e0\u6216\u91cd\u8bd5/)
  assert.match(page, /goTaskCenter/)
  assert.match(page, /router\.push\('\/tasks'\)/)
  assert.match(styles, /\.story-task-notice\s*\{[^}]*position:\s*fixed;[^}]*right:/s)
  assert.match(styles, /\.story-task-notice\s*\{[^}]*bottom:/s)
  assert.match(styles, /\.story-task-notice\s*\{[^}]*border-radius:\s*8px/s)
})
