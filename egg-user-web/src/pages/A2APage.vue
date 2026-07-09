<template>
  <div class="page">
    <section class="generator-layout">
      <div class="generator-panel">
        <el-tag type="info" effect="plain">Phase-one terminal feature</el-tag>
        <h2>A2A 分发准备区</h2>
        <p class="muted">
          A2A 放在第一阶段最后开发。当前页面只保留配置与任务入口，不影响前面的真实出图、RAG、素材和审核闭环。
        </p>
        <div class="form-stack">
          <el-input placeholder="A2A 应用名称，例如：商品图素材分发" />
          <el-select model-value="disabled" style="width:100%">
            <el-option label="暂不启用" value="disabled" />
            <el-option label="准备接入" value="ready" />
          </el-select>
          <el-button type="primary" disabled>核心闭环完成后启用</el-button>
        </div>
      </div>
      <div class="table-panel">
        <h3>A2A 开发条件</h3>
        <el-timeline>
          <el-timeline-item timestamp="先决条件">视觉生成 MVP 可用</el-timeline-item>
          <el-timeline-item timestamp="先决条件">智能聊天与 RAG 可用</el-timeline-item>
          <el-timeline-item timestamp="先决条件">素材审核、下载、导出可用</el-timeline-item>
          <el-timeline-item timestamp="最后阶段">开发轻量 A2A handoff</el-timeline-item>
        </el-timeline>
      </div>
    </section>
  </div>
</template>
