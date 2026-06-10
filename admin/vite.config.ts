import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  // 开发环境 vs 测试环境
  const isDev = mode === 'development' || !mode
  const proxyTarget = isDev
    ? 'http://localhost:8080'
    : 'https://sedative-squishy-worry.ngrok-free.dev'

  return {
    base: '/api/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    server: {
      port: 3001,
      proxy: {
        // 代理后端 API，其余由 Vite 提供 HMR
        // /api/user/ (单数+斜杠) 精确匹配移动端 API，不会拦截 /api/users/ (SPA 路由)
        '^/api/(admin|user/|common|ws)': {
          target: proxyTarget,
          changeOrigin: true,
          configure: (proxy) => {
            proxy.on('proxyReq', (proxyReq, req) => {
              proxyReq.setHeader('ngrok-skip-browser-warning', 'true')
              proxyReq.setHeader('User-Agent', 'runningerrands-admin/1.0')
              const ip = req.socket.remoteAddress
              if (ip) {
                proxyReq.setHeader('X-Forwarded-For', ip)
                proxyReq.setHeader('X-Real-IP', ip)
              }
            })
          }
        }
      }
    }
  }
})
