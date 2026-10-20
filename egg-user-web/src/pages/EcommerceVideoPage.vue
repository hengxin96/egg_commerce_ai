<template>
  <div class="page">
    <section class="tool-hero">
      <el-tag type="warning" effect="plain">Beta</el-tag>
      <h1>电商视频</h1>
      <p>
        第一版不做真实视频生成，这里只保留视频脚本、分镜和素材选取入口。等图片生成、RAG、素材库和导出闭环稳定后再进入视频阶段。
      </p>
    </section>

    <section class="generator-layout">
      <div class="generator-panel soft-card">
        <h2>视频脚本规划</h2>
        <div class="form-stack">
          <el-select model-value="p-1001" style="width:100%">
            <el-option label="磁吸无线充电支架" value="p-1001" />
            <el-option label="便携榨汁杯" value="p-1002" />
          </el-select>
          <el-input type="textarea" :rows="5" placeholder="输入视频目标，例如：15 秒抖音商品种草短片" />
          <el-button type="primary" disabled>后续阶段启用真实生成</el-button>
        </div>
      </div>

      <div class="generator-panel soft-card">
        <h2>分镜预览</h2>
        <div class="planning-card">
          <strong>开场 0-3s</strong>
          <p>展示商品使用痛点和核心场景。</p>
        </div>
        <div class="planning-card">
          <strong>卖点 3-10s</strong>
          <p>突出功能、材质、规格和真实可验证卖点。</p>
        </div>
        <div class="planning-card">
          <strong>收尾 10-15s</strong>
          <p>生成上架图包和导出素材提示。</p>
        </div>
      </div>
    </section>
  </div>
</template>
