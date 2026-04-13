import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vite.dev/config/
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
      port: 3001,
      host: true,
      proxy: {
        '/core-api': {
          target: env.VITE_CORE_API_URL || 'http://localhost:8082',
          changeOrigin: true,
          rewrite: (path: string) => {
            if (path.startsWith('/core-api/v3/api-docs') || path.startsWith('/core-api/swagger-ui')) {
              return path.replace(/^\/core-api/, '')
            }
            return path.replace(/^\/core-api/, '/api')
          },
        },
        '/ai-api': {
          target: env.VITE_AI_API_URL || 'http://localhost:8083',
          changeOrigin: true,
          rewrite: (path: string) => path.replace(/^\/ai-api/, '/api'),
        },
        '/file-api': {
          target: env.VITE_FILE_API_URL || 'http://localhost:8083',
          changeOrigin: true,
        },
      },
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      chunkSizeWarningLimit: 1500,
      rollupOptions: {
        output: {
          manualChunks: {
            'element-plus': ['element-plus'],
            'vue-vendor': ['vue', 'vue-router', 'pinia'],
          },
        },
      },
    },
  }
})
