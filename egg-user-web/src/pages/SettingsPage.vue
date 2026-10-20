<template>
  <div class="page">
    <section class="section-title">
      <div>
        <h2>业务模块配置说明</h2>
        <p class="muted">这里展示前后端需要对齐的核心环境配置。真实配置写在后端 `application.yml` 和 Docker Compose 中。</p>
      </div>
    </section>
    <div class="feature-grid three">
      <article v-for="item in moduleConfigs" :key="item.name" class="feature-card">
        <h3>{{ item.name }}</h3>
        <p><strong>为什么：</strong>{{ item.why }}</p>
        <p><strong>怎么配：</strong>{{ item.how }}</p>
        <el-tag>{{ item.file }}</el-tag>
      </article>
    </div>
  </div>
</template>

<script setup>
import { moduleConfigs } from '@/data/mock'
</script>
