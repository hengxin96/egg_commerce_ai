<template>
  <div class="auth-page">
    <section class="auth-card">
      <div class="auth-logo">
        <el-icon><MagicStick /></el-icon>
      </div>
      <h2>注册龙蛋 AI</h2>
      <p class="muted">第一版用户平权，不包含积分、套餐、团队或租户。</p>
      <el-segmented model-value="注册" :options="['登录', '注册']" style="width:100%; margin:24px 0 10px" />
      <div class="form-stack">
        <label>
          <span class="field-label">邮箱</span>
          <el-input placeholder="name@example.com" />
        </label>
        <label>
          <span class="field-label">密码</span>
          <el-input placeholder="至少 6 位" show-password />
        </label>
        <el-button type="primary" size="large" @click="$router.push('/workspace')">创建账号</el-button>
        <el-checkbox>您已阅读并同意《服务条款》和《隐私政策》</el-checkbox>
        <router-link to="/login">已有账号？登录</router-link>
      </div>
    </section>
    <div class="support-float">☎</div>
  </div>
</template>

<script setup>
import { MagicStick } from '@element-plus/icons-vue'
</script>
