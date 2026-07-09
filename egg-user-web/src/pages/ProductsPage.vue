<template>
  <div class="page">
    <section class="section-title">
      <div>
        <h2>商品库</h2>
        <p class="muted">商品是所有 AI 生成、审核、素材版本和导出的归属核心。</p>
      </div>
      <el-button type="primary">新增商品</el-button>
    </section>
    <div class="table-panel">
      <el-table :data="products" style="width:100%">
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <router-link :to="`/products/${row.id}`"><strong>{{ row.name }}</strong></router-link>
            <p class="muted">{{ row.highlights.join(' / ') }}</p>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="类目" width="140" />
        <el-table-column prop="platform" label="平台" width="180" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="updatedAt" label="最近生成" width="180" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/products/${row.id}`)">详情</el-button>
            <el-button link>生成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { products } from '@/data/mock'
</script>
