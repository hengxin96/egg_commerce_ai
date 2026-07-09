<template>
  <div class="page">
    <section class="chat-panel">
      <aside class="chat-sidebar">
        <el-button type="primary" style="width:100%">新建会话</el-button>
        <div class="section-title"><h3>商品上下文</h3></div>
        <el-select model-value="p-1001" style="width:100%">
          <el-option v-for="item in products" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <div class="section-title"><h3>RAG 来源</h3></div>
        <el-tag v-for="source in sources" :key="source" style="margin: 0 6px 8px 0">{{ source }}</el-tag>
      </aside>
      <main class="chat-main">
        <div class="messages">
          <div class="message">我会结合商品资料、平台规则和审核规则回答，并给出下一步生成建议。</div>
          <div class="message user">这款磁吸支架的抖音主图应该突出什么卖点？</div>
          <div class="message">
            建议突出桌面收纳、强磁稳固、边充边用三点。主图可使用清爽桌面场景，避免夸大充电速度，并保留商品结构和 logo。
          </div>
        </div>
        <div class="composer">
          <el-input placeholder="询问平台规则、详情页结构、图片风险或生成建议" />
          <el-button type="primary">发送</el-button>
        </div>
      </main>
    </section>
  </div>
</template>

<script setup>
import { products } from '@/data/mock'

const sources = ['抖音商品主图规则', '品牌视觉规范', '夸大宣传审核', '详情页结构案例']
</script>
