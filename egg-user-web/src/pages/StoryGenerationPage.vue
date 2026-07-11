<template>
  <GeneratorWorkspace
    eyebrow="AI 剧情生成"
    title="剧情生成"
    desc="输入剧情主题和产品信息，AI一键生成完整剧情、剧本、分镜脚本及人物三视图。"
    :steps="['输入', '剧情生成', '剧本生成', '分镜生成', '分镜校验', '三视图/场景图', '完成']"
    :show-upload-panel="false"
    :show-asset-tabs="false"
    :show-prompt-helper="false"
    :show-quality-field="false"
    :show-count-field="false"
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
    result-subtitle="输入剧情主题和产品信息，点击开始生成"
    empty-message="请先输入你的剧情主题和产品信息吧"
  />
</template>

<script setup>
import GeneratorWorkspace from '@/components/GeneratorWorkspace.vue'

// 剧情页下拉值后续接后端字典；当前先提供本地占位，避免影响其它生成页默认配置。
const targetAudienceOptions = [
  { label: '中老年人群', value: 'middle_aged_and_elderly' },
  { label: '家庭主妇', value: 'homemakers' }
]

const recommendedStyleOptions = [
  { label: '家庭伦理', value: 'family_ethics' },
  { label: '反转爽剧', value: 'plot_twist' }
]

const storyDurationOptions = [
  { label: '30 秒', value: '30s' },
  { label: '60 秒', value: '60s' }
]
</script>
