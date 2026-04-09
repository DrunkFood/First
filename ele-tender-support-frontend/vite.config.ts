import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const isProd = mode === 'production'
  
  return {
    base: isProd ? '/ele-tender-support/' : '/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      port: 3000,
      host: true,
      proxy: {
        '/support-api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (path) => {
            if (path.startsWith('/support-api/v3/api-docs') || path.startsWith('/support-api/swagger-ui')) {
              return path.replace(/^\/support-api/, '')
            }
            return path.replace(/^\/support-api/, '/api')
          },
        },
        '/file-api': {
          target: env.VITE_FILE_API_URL || 'http://localhost:8081',
          changeOrigin: true,
        },
        '/file-esign-api': {
          target: env.VITE_FILE_API_URL || 'http://localhost:8081',
          changeOrigin: true,
        },
        '/crypto-api': {
          target: env.VITE_CRYPTO_API_URL || 'http://localhost:8083',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/crypto-api/, '/api/crypto'),
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
