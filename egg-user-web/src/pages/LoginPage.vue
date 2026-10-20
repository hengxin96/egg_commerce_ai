<template>
  <div class="auth-page">
    <section class="auth-card auth-card-refined">
      <div class="auth-logo">
        <el-icon><MagicStick /></el-icon>
      </div>
      <h2>{{ isResetMode ? '重置密码' : '登录/注册龙蛋 AI' }}</h2>
      <p class="muted">{{ subtitle }}</p>

      <div class="form-stack auth-form-stack">
        <label>
          <span class="field-label">手机号</span>
          <el-input v-model="form.phonenumber" placeholder="请输入 11 位手机号" maxlength="11" inputmode="numeric" />
        </label>

        <template v-if="!isResetMode">
          <label>
            <span class="field-label">密码</span>
            <el-input v-model="form.password" placeholder="5-20 位密码" show-password @keyup.enter="submitAuth" />
          </label>
          <button class="auth-link-button" type="button" @click="switchToReset">忘记密码？</button>
          <el-button type="primary" size="large" :loading="isSubmitting" @click="submitAuth">登录 / 注册</el-button>
          <p class="auth-note">手机号已注册将直接登录，未注册将自动创建账号。</p>
        </template>

        <template v-else>
          <label>
            <span class="field-label">新密码</span>
            <el-input v-model="form.password" placeholder="5-20 位新密码" show-password />
          </label>
          <label>
            <span class="field-label">确认新密码</span>
            <el-input v-model="form.confirmPassword" placeholder="再次输入新密码" show-password @keyup.enter="submitReset" />
          </label>
          <el-button type="primary" size="large" :loading="isSubmitting" @click="submitReset">重置密码</el-button>
          <button class="auth-link-button auth-link-center" type="button" @click="switchToAuth">返回登录</button>
        </template>

        <el-checkbox v-if="!isResetMode" v-model="agreed">您已阅读并同意《服务条款》和《隐私政策》</el-checkbox>
        <router-link to="/" class="muted">← 返回首页</router-link>
      </div>
    </section>
    <div class="support-float">☎</div>
  </div>
</template>

<script setup>
import { computed, reactive, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import {
  isValidPassword,
  isValidPhone,
  loginOrRegister,
  resetPassword,
  saveAuthSession
} from '@/api/auth'

const router = useRouter()
const route = useRoute()
const mode = shallowRef('auth')
const isSubmitting = shallowRef(false)
const agreed = shallowRef(false)

const form = reactive({
  phonenumber: '',
  password: '',
  confirmPassword: ''
})

const isResetMode = computed(() => mode.value === 'reset')
const subtitle = computed(() => (
  isResetMode.value
    ? '输入手机号和两次新密码即可重置。'
    : '第一版仅支持手机号登录，未注册会自动创建前台账号。'
))

const validatePhoneAndPassword = () => {
  if (!isValidPhone(form.phonenumber)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return false
  }
  if (!isValidPassword(form.password)) {
    ElMessage.warning('密码长度必须在 5 到 20 位之间')
    return false
  }
  return true
}

const resolveRedirect = () => {
  const redirect = Array.isArray(route.query.redirect) ? route.query.redirect[0] : route.query.redirect
  if (redirect && redirect.startsWith('/') && !redirect.startsWith('/login')) {
    return redirect
  }
  return '/generate/product-images'
}

const submitAuth = async () => {
  if (!validatePhoneAndPassword()) {
    return
  }

  isSubmitting.value = true
  try {
    const result = await loginOrRegister({
      phonenumber: form.phonenumber.trim(),
      password: form.password
    })
    saveAuthSession(result)
    ElMessage.success(result.registered ? '已自动注册并登录' : '登录成功')
    router.replace(resolveRedirect())
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    isSubmitting.value = false
  }
}

const submitReset = async () => {
  if (!validatePhoneAndPassword()) {
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  isSubmitting.value = true
  try {
    await resetPassword({
      phonenumber: form.phonenumber.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword
    })
    ElMessage.success('密码已重置，请使用新密码登录')
    form.password = ''
    form.confirmPassword = ''
    mode.value = 'auth'
  } catch (error) {
    ElMessage.error(error.message || '重置密码失败')
  } finally {
    isSubmitting.value = false
  }
}

const switchToReset = () => {
  form.password = ''
  form.confirmPassword = ''
  mode.value = 'reset'
}

const switchToAuth = () => {
  form.password = ''
  form.confirmPassword = ''
  mode.value = 'auth'
}
</script>
