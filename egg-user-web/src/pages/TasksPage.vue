<template>
  <div class="page">
    <section class="section-title">
      <div>
        <h2>任务中心</h2>
        <p class="muted">图片生成、RAG 聊天、审核和导出都进入任务中心，支持进度与失败重试。</p>
      </div>
    </section>
    <div class="table-panel">
      <el-table :data="tasks" style="width:100%">
        <el-table-column prop="name" label="任务名称" min-width="240" />
        <el-table-column prop="type" label="类型" width="140" />
        <el-table-column prop="product" label="商品" width="180" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <span :class="['status-dot', row.status === '成功' ? 'success' : row.status === '失败' ? 'danger' : 'warning']">
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="220">
          <template #default="{ row }"><el-progress :percentage="row.progress" /></template>
        </el-table-column>
        <el-table-column prop="reason" label="失败原因" min-width="220" />
        <el-table-column label="操作" width="140">
          <template #default><el-button link type="primary">查看日志</el-button></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { tasks } from '@/data/mock'
</script>
