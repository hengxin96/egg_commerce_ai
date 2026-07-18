import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    allowedHosts: ['35uq02gn3623.vicp.fun'],
    proxy: {
      '/api': {
        target: process.env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
        // 后端控制器没有 /api 前缀，开发代理需要剥离前端统一的 API base。
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
