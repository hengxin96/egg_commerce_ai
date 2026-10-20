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
        <div
          class="profile-entry"
          @mouseenter="showProfilePanel = true"
          @mouseleave="showProfilePanel = false"
          @focusin="showProfilePanel = true"
          @focusout="showProfilePanel = false"
        >
          <router-link
            v-if="!isAuthenticated"
            to="/login"
            class="avatar-button"
            aria-label="登录/注册"
          >
            <el-icon><User /></el-icon>
          </router-link>
          <button
            v-else
            class="avatar-button is-authenticated"
            type="button"
            aria-label="当前登录用户信息"
          >
            <el-icon><User /></el-icon>
          </button>

          <div v-if="isAuthenticated" v-show="showProfilePanel" class="profile-popover" role="status">
            <span class="profile-eyebrow">当前登录</span>
            <div class="profile-row profile-row-strong">
              <span class="profile-label">昵称：</span>
              <strong>{{ displayNickName }}</strong>
            </div>
            <div class="profile-row">
              <span class="profile-label">手机号：</span>
              <span>{{ displayPhone }}</span>
            </div>
            <button class="profile-logout" type="button" @click.stop="handleLogout">退出登录</button>
          </div>
        </div>
      </div>
    </header>

    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, User } from '@element-plus/icons-vue'
import { primaryNavItems } from '@/data/mock'
import {
  clearAuthSession,
  fetchAuthProfile,
  getAuthSession,
  isAuthFailure,
  onAuthStateChange,
  saveAuthUser,
  touchAuthActivity
} from '@/api/auth'

const route = useRoute()
const router = useRouter()
const session = shallowRef(getAuthSession())
const showProfilePanel = shallowRef(false)

const isAuthenticated = computed(() => Boolean(session.value))
const currentUser = computed(() => session.value?.user || {})
const displayNickName = computed(() => currentUser.value.nickName || currentUser.value.userName || '资料加载中')
const displayPhone = computed(() => currentUser.value.phonenumber || '手机号加载中')

let removeAuthStateListener = () => {}
let expireTimer = 0
let lastTouchAt = 0
let hadSession = Boolean(session.value)
let isLoadingProfile = false

const hasCompleteProfile = (user) => Boolean((user?.nickName || user?.userName) && user?.phonenumber)

const routeToLoginWhenRequired = () => {
  if (!route.meta.requiresAuth) {
    return
  }

  router.push({
    path: '/login',
    query: { redirect: route.fullPath }
  })
}

const loadMissingProfile = async () => {
  const currentSession = getAuthSession()
  if (!currentSession || hasCompleteProfile(currentSession.user) || isLoadingProfile) {
    return
  }

  isLoadingProfile = true
  try {
    const profile = await fetchAuthProfile()
    saveAuthUser(profile)
    session.value = getAuthSession()
    hadSession = Boolean(session.value)
  } catch (error) {
    if (isAuthFailure(error)) {
      clearAuthSession()
      refreshSession({ notifyExpired: true })
    }
  } finally {
    isLoadingProfile = false
  }
}

const refreshSession = ({ notifyExpired = false } = {}) => {
  const nextSession = getAuthSession()
  const expired = hadSession && !nextSession
  session.value = nextSession
  hadSession = Boolean(nextSession)

  if (nextSession && !hasCompleteProfile(nextSession.user)) {
    loadMissingProfile()
  }

  if (!expired || !notifyExpired) {
    return
  }

  showProfilePanel.value = false
  ElMessage.warning('登录已过期，请重新登录')
  routeToLoginWhenRequired()
}

const handleLogout = () => {
  clearAuthSession()
  session.value = null
  hadSession = false
  showProfilePanel.value = false
  ElMessage.success('已退出登录')
  routeToLoginWhenRequired()
}

const handleUserActivity = () => {
  const now = Date.now()
  if (now - lastTouchAt < 30000) {
    return
  }

  lastTouchAt = now
  if (touchAuthActivity(now)) {
    refreshSession()
    return
  }

  refreshSession({ notifyExpired: true })
}

const activityEvents = ['click', 'keydown', 'mousemove', 'scroll', 'touchstart']

onMounted(() => {
  refreshSession()
  removeAuthStateListener = onAuthStateChange(() => refreshSession())
  activityEvents.forEach((eventName) => {
    window.addEventListener(eventName, handleUserActivity, { passive: true })
  })
  expireTimer = window.setInterval(() => refreshSession({ notifyExpired: true }), 60000)
})

onUnmounted(() => {
  removeAuthStateListener()
  activityEvents.forEach((eventName) => {
    window.removeEventListener(eventName, handleUserActivity)
  })
  window.clearInterval(expireTimer)
})
</script>