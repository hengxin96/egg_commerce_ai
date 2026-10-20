<template>
  <div class="page">
    <section class="generator-layout">
      <div class="generator-panel">
        <el-tag type="warning" effect="plain">Beta</el-tag>
        <h2>万能画布</h2>
        <p class="muted">第一版只预留轻量编辑入口：选择素材、叠加文案、裁剪比例、导出图片。高级画布不进入一期核心范围。</p>
        <div class="form-stack">
          <el-select model-value="磁吸支架主图 1:1" style="width:100%">
            <el-option label="磁吸支架主图 1:1" value="1" />
            <el-option label="榨汁杯场景图" value="2" />
          </el-select>
          <el-input placeholder="输入图片文字，例如：强磁快充，桌面更清爽" />
          <el-segmented :model-value="'1:1'" :options="['1:1', '3:4', '4:5', '16:9']" />
          <el-button type="primary">保存为新版本</el-button>
        </div>
      </div>
      <div class="visual-stage">
        <div class="product-shot shot-a"></div>
        <div class="product-shot shot-b"></div>
      </div>
    </section>
  </div>
</template>
