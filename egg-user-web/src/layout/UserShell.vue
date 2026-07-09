<template>
  <div class="shell">
    <header class="site-header">
      <router-link class="brand" to="/">
        <span class="brand-mark">
          <el-icon><MagicStick /></el-icon>
        </span>
        <span>
          <strong>龙蛋 AI</strong>
        </span>
      </router-link>

      <nav class="top-nav" aria-label="前台主导航">
        <router-link
          v-for="item in primaryNavItems"
          :key="item.path"
          :to="item.path"
          class="top-nav-item"
          active-class="is-active"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>

      </nav>

      <div class="header-actions">
        <el-button class="gift-button" :icon="Present">邀请有礼</el-button>
        <router-link to="/login" class="avatar-button" aria-label="登录">
          <el-icon><User /></el-icon>
        </router-link>
      </div>
    </header>

    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { MagicStick, Present, User } from '@element-plus/icons-vue'
import { primaryNavItems } from '@/data/mock'
</script>
