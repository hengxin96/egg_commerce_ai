import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('login page is phone-only and keeps forgot-password reset in the same entry', () => {
  const page = readSource('./LoginPage.vue')

  assert.match(page, /loginOrRegister/)
  assert.match(page, /resetPassword/)
  assert.match(page, /saveAuthSession/)
  assert.match(page, /手机号/)
  assert.match(page, /新密码/)
  assert.doesNotMatch(page, /邮箱/)
  assert.doesNotMatch(page, /captcha|验证码/i)
})

test('user shell hides invite button and shows logged-in profile details on avatar hover', () => {
  const shell = readSource('../layout/UserShell.vue')

  assert.doesNotMatch(shell, /邀请有礼/)
  assert.doesNotMatch(shell, /gift-button/)
  assert.match(shell, /v-if="!isAuthenticated"/)
  assert.match(shell, /to="\/login"/)
  assert.match(shell, /profile-popover/)
  assert.match(shell, /displayNickName/)
  assert.match(shell, /displayPhone/)
  assert.match(shell, /昵称：/)
  assert.match(shell, /退出登录/)
  assert.match(shell, /handleLogout/)
  assert.match(shell, /clearAuthSession/)
  assert.match(shell, /fetchAuthProfile/)
  assert.match(shell, /saveAuthUser/)
  assert.match(shell, /onAuthStateChange/)
})

test('router protects chat and task center pages for unauthenticated users and refreshes active sessions', () => {
  const router = readSource('../router/index.js')

  assert.match(router, /requiresAuth/)
  assert.match(router, /name: 'chat'.+requiresAuth: true/s)
  assert.match(router, /name: 'tasks'.+requiresAuth: true/s)
  assert.match(router, /暂未登录，请先登录/)
  assert.match(router, /touchAuthActivity/)
})

test('shared generator button requires login before emitting generation', () => {
  const workspace = readSource('../components/GeneratorWorkspace.vue')

  assert.match(workspace, /requireLogin/)
  assert.match(workspace, /if \(!requireLogin\(router\)\)/)
})

test('feature APIs read token from the shared auth session helper', () => {
  const chat = readSource('../api/chat.js')
  const story = readSource('../api/story.js')

  assert.match(chat, /import \{ getAuthToken \} from '\.\/auth\.js'/)
  assert.match(story, /import \{[^}]*getAuthToken[^}]*\} from '\.\/auth\.js'/)
})

test('backend auth controller exposes profile for logged-in frontend users', () => {
  const controller = readFileSync(new URL('../../../egg-admin/src/main/java/com/egg/commerce/web/controller/system/FrontUserAuthController.java', import.meta.url), 'utf8')

  assert.match(controller, /@GetMapping\("\/profile"\)/)
  assert.match(controller, /getLoginUser\(\)/)
  assert.match(controller, /phonenumber/)
  assert.match(controller, /nickName/)
})
