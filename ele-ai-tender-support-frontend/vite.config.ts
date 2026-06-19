import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

function formatProxyTargetUrl(target: string, path?: string) {
  if (!path) return target
  try {
    return new URL(path, target).toString()
  } catch {
    return `${target}${path}`
  }
}

function logProxyRequest(target: string) {
  return (proxyReq: { path?: string }, req: { method?: string; url?: string }) => {
    const method = req.method || 'GET'
    const sourceUrl = req.url || ''
    const targetUrl = formatProxyTargetUrl(target, proxyReq.path)
    console.log(`[proxy] ${method} ${sourceUrl} -> ${targetUrl}`)
  }
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const isProd = mode === 'production'
  const fileApiTarget = env.VITE_FILE_API_URL || 'http://localhost:8081'
  const supportApiTarget = env.VITE_SUPPORT_API_URL || 'http://localhost:8080'

  return {
    base: isProd ? '/ele-ai-tender-support-web/' : '/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      port: 3060,
      host: true,
      proxy: {
        '/file-api': {
          target: fileApiTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/file-api/, ''),
          configure: (proxy) => {
            proxy.on('proxyReq', logProxyRequest(fileApiTarget))
          },
        },
        '/support-api': {
          target: supportApiTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/support-api/, '/api'),
          configure: (proxy) => {
            proxy.on('proxyReq', logProxyRequest(supportApiTarget))
          },
        },
      },
    },
    build: {
      outDir: 'ele-ai-tender-support-web',
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
