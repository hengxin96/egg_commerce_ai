<template>
  <div class="page">
    <section class="tool-hero">
      <el-tag effect="plain">工作台</el-tag>
      <h1>今天从哪张商品图开始？</h1>
      <p>选择一个生成入口，或者从最近商品继续编辑、审核和导出。</p>
    </section>

    <div class="metric-grid">
      <div class="metric"><strong>18</strong><span>今日生成素材</span></div>
      <div class="metric"><strong>6</strong><span>执行中任务</span></div>
      <div class="metric"><strong>91%</strong><span>审核通过率</span></div>
      <div class="metric"><strong>42</strong><span>RAG 引用来源</span></div>
    </div>

    <section class="section-title">
      <div>
        <h2>快速开始</h2>
        <p class="muted">选择最接近当前目标的能力入口。</p>
      </div>
    </section>
    <div class="feature-grid">
      <router-link v-for="mode in generationModes" :key="mode.title" class="feature-card" :to="mode.route">
        <span class="icon-box" :style="{ backgroundColor: mode.color }">
          <el-icon><component :is="mode.icon" /></el-icon>
        </span>
        <div>
          <h3>{{ mode.title }}</h3>
          <p>{{ mode.desc }}</p>
        </div>
      </router-link>
    </div>

    <section class="section-title">
      <h2>最近商品</h2>
      <el-button @click="$router.push('/products')">查看全部</el-button>
    </section>
    <div class="card-grid three">
      <router-link v-for="item in products" :key="item.id" class="feature-card" :to="`/products/${item.id}`">
        <div class="asset-thumb" :style="{ background: item.image }"></div>
        <div>
          <h3>{{ item.name }}</h3>
          <p>{{ item.category }} · {{ item.platform }}</p>
          <el-tag>{{ item.status }}</el-tag>
        </div>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { generationModes, products } from '@/data/mock'
</script>
