<template>
  <div class="auth-page">
    <section class="auth-card">
      <div class="auth-logo">
        <el-icon><MagicStick /></el-icon>
      </div>
      <h2>登录龙蛋 AI</h2>
      <p class="muted">使用邮箱/手机号登录或注册</p>
      <el-segmented model-value="登录" :options="['登录', '注册']" style="width:100%; margin:24px 0 10px" />
      <div class="form-stack">
        <el-segmented model-value="邮箱" :options="['邮箱', '手机号']" style="justify-self:end" />
        <label>
          <span class="field-label">邮箱</span>
          <el-input placeholder="name@example.com" />
        </label>
        <label>
          <span class="field-label">密码</span>
          <el-input placeholder="至少 6 位" show-password />
        </label>
        <router-link class="muted" to="/register" style="justify-self:end">忘记密码？</router-link>
        <el-button type="primary" size="large" @click="$router.push('/workspace')">邮箱登录</el-button>
        <el-checkbox>您已阅读并同意《服务条款》和《隐私政策》</el-checkbox>
        <router-link to="/" class="muted">← 返回</router-link>
      </div>
    </section>
    <div class="support-float">☎</div>
  </div>
</template>

<script setup>
import { MagicStick } from '@element-plus/icons-vue'
</script>
