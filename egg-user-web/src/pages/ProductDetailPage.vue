<template>
  <div class="page">
    <section class="section-title">
      <div>
        <h2>{{ product.name }}</h2>
        <p class="muted">{{ product.category }} · {{ product.platform }} · {{ product.status }}</p>
      </div>
      <div>
        <el-button @click="$router.push('/generate/product-images')">生成组图</el-button>
        <el-button type="primary">导出素材包</el-button>
      </div>
    </section>

    <div class="generator-layout">
      <div class="visual-stage" :style="{ background: product.image }">
        <div class="product-shot shot-a"></div>
      </div>
      <div class="generator-panel">
        <h3>AI 分析摘要</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="核心卖点">{{ product.highlights.join('、') }}</el-descriptions-item>
          <el-descriptions-item label="视觉策略">清晰商品主体、场景化使用价值、平台尺寸适配。</el-descriptions-item>
          <el-descriptions-item label="风险提醒">生成时必须保持颜色、结构、logo 和数量一致。</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <section class="section-title"><h3>关联素材</h3></section>
    <div class="preview-grid">
      <article v-for="asset in assets" :key="asset.id" class="asset-thumb">
        <div class="asset-meta">
          <strong>{{ asset.title }}</strong>
          <p class="muted">{{ asset.type }} · {{ asset.audit }} · {{ asset.version }}</p>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { assets, products } from '@/data/mock'

const route = useRoute()
const product = computed(() => products.find((item) => item.id === route.params.id) || products[0])
</script>
