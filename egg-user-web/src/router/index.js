import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserShell from '@/layout/UserShell.vue'
import WorkspacePage from '@/pages/WorkspacePage.vue'
import ProductImagesPage from '@/pages/ProductImagesPage.vue'
import StyleClonePage from '@/pages/StyleClonePage.vue'
import ApparelSetsPage from '@/pages/ApparelSetsPage.vue'
import RefinementPage from '@/pages/RefinementPage.vue'
import CanvasPage from '@/pages/CanvasPage.vue'
import ChatPage from '@/pages/ChatPage.vue'
import StoryGenerationPage from '@/pages/StoryGenerationPage.vue'
import ProductsPage from '@/pages/ProductsPage.vue'
import ProductDetailPage from '@/pages/ProductDetailPage.vue'
import AssetsPage from '@/pages/AssetsPage.vue'
import TasksPage from '@/pages/TasksPage.vue'
import PricingPage from '@/pages/PricingPage.vue'
import EcommerceVideoPage from '@/pages/EcommerceVideoPage.vue'
import LoginPage from '@/pages/LoginPage.vue'
import SettingsPage from '@/pages/SettingsPage.vue'
import A2APage from '@/pages/A2APage.vue'
import { isLoggedIn, touchAuthActivity } from '@/api/auth'

const routes = [
  {
    path: '/',
    component: UserShell,
    children: [
      { path: '', redirect: '/generate/product-images' },
      { path: 'workspace', name: 'workspace', component: WorkspacePage, meta: { title: '工作台' } },
      { path: 'generate/product-images', name: 'product-images', component: ProductImagesPage, meta: { title: '全品类商品图' } },
      { path: 'generate/style-clone', name: 'style-clone', component: StyleClonePage, meta: { title: '风格复刻' } },
      { path: 'generate/apparel', name: 'apparel', component: ApparelSetsPage, meta: { title: '服装组图' } },
      { path: 'generate/refinement', name: 'refinement', component: RefinementPage, meta: { title: '批量精修' } },
      { path: 'canvas', name: 'canvas', component: CanvasPage, meta: { title: '万能画布' } },
      { path: 'chat', name: 'chat', component: ChatPage, meta: { title: '智能聊天', requiresAuth: true } },
      { path: 'generate/story', name: 'story-generation', component: StoryGenerationPage, meta: { title: '剧情生成' } },
      { path: 'products', name: 'products', component: ProductsPage, meta: { title: '商品库' } },
      { path: 'products/:id', name: 'product-detail', component: ProductDetailPage, meta: { title: '商品详情' } },
      { path: 'assets', name: 'assets', component: AssetsPage, meta: { title: '素材库' } },
      { path: 'tasks', name: 'tasks', component: TasksPage, meta: { title: '任务中心', requiresAuth: true } },
      { path: 'ecommerce-video', name: 'ecommerce-video', component: EcommerceVideoPage, meta: { title: '电商视频' } },
      { path: 'pricing', name: 'pricing', component: PricingPage, meta: { title: '价格' } },
      { path: 'settings', name: 'settings', component: SettingsPage, meta: { title: '配置说明' } },
      { path: 'a2a', name: 'a2a', component: A2APage, meta: { title: 'A2A' } }
    ]
  },
  { path: '/login', name: 'login', component: LoginPage, meta: { title: '登录/注册' } },
  { path: '/register', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const hasSession = isLoggedIn()
  if (to.meta.requiresAuth && !hasSession) {
    ElMessage.warning('暂未登录，请先登录')
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (hasSession) {
    touchAuthActivity()
  }
  return true
})

router.afterEach((to) => {
  document.title = `${to.meta.title || '工作台'} - 龙蛋 AI`
})

export default router
