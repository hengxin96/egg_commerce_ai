<template>
  <div class="page">
    <section class="section-title">
      <div>
        <h2>素材库</h2>
        <p class="muted">所有 AI 输出都必须保存、版本化、审核并可复用。</p>
      </div>
      <el-button type="primary">批量下载</el-button>
    </section>

    <div class="preview-grid">
      <article v-for="asset in assets" :key="asset.id" class="asset-thumb">
        <div class="asset-meta">
          <strong>{{ asset.title }}</strong>
          <p class="muted">{{ asset.product }} · {{ asset.model }}</p>
          <el-tag :type="asset.audit === '通过' ? 'success' : asset.audit === '风险' ? 'danger' : 'warning'">
            {{ asset.audit }}
          </el-tag>
          <el-tag style="margin-left:6px">{{ asset.version }}</el-tag>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { assets } from '@/data/mock'
</script>
