import { ElMessage } from 'element-plus'
import { clearAuthSession, isAuthFailure, isLoggedIn, touchAuthActivity } from '@/api/auth'

export function requireLogin(router, redirectPath = router.currentRoute.value.fullPath) {
  if (isLoggedIn()) {
    touchAuthActivity()
    return true
  }

  ElMessage.warning('暂未登录，请先登录')
  router.push({
    path: '/login',
    query: { redirect: redirectPath }
  })
  return false
}

export function handleAuthFailure(error, router, redirectPath = router.currentRoute.value.fullPath) {
  if (!isAuthFailure(error)) {
    return false
  }

  // 后端判定 token 无效时，以后端结果为准清理前台登录态，并把用户带回登录页。
  clearAuthSession()
  ElMessage.warning('登录已过期，请重新登录')
  router.push({
    path: '/login',
    query: { redirect: redirectPath }
  })
  return true
}
