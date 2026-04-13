import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const isProd = mode === 'production'

  return {
    base: isProd ? '/ele-ai-tender/' : '/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      port: 5173,
      host: true,
      proxy: {
        '/core-api': {
          target: env.VITE_CORE_API_URL || 'http://localhost:8082',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/core-api/, '/api'),
        },
        '/ai-api': {
          target: env.VITE_AI_API_URL || 'http://localhost:8083',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/ai-api/, '/api'),
        },
        '/file-api': {
          target: env.VITE_FILE_API_URL || 'http://localhost:8081',
          changeOrigin: true,
        },
        '/support-api': {
          target: env.VITE_SUPPORT_API_URL || 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/support-api/, '/api'),
        },
      },
    },
  }
})
