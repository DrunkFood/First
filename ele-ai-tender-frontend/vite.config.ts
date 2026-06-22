import { defineConfig, loadEnv, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'
import { appendFileSync, mkdirSync } from 'fs'
import { resolve } from 'path'

const apiBasePath = '/api'
const proxyLogFile = resolve(__dirname, 'logs', 'vite-proxy.log')
let proxyLogDirReady = false

function ensureProxyLogDir() {
  if (proxyLogDirReady) {
    return
  }
  mkdirSync(resolve(__dirname, 'logs'), { recursive: true })
  proxyLogDirReady = true
}

function formatLogTime(date = new Date()) {
  const pad = (value: number, length = 2) => value.toString().padStart(length, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}.${pad(date.getMilliseconds(), 3)}`
}

function writeProxyLog(message: string) {
  const line = `[${formatLogTime()}] ${message}`
  console.log(line)

  try {
    ensureProxyLogDir()
    appendFileSync(proxyLogFile, `${line}\n`, 'utf8')
  } catch (error) {
    console.warn('[vite-proxy] write log failed:', error)
  }
}

function buildTargetUrl(target: string, path: string) {
  const targetUrl = new URL(target)
  const targetPath = targetUrl.pathname === '/' ? '' : targetUrl.pathname.replace(/\/$/, '')
  const requestUrl = new URL(path.startsWith('/') ? path : `/${path}`, targetUrl.origin)

  targetUrl.pathname = [targetPath, requestUrl.pathname.replace(/^\//, '')].filter(Boolean).join('/')
  targetUrl.search = requestUrl.search
  targetUrl.hash = ''
  return targetUrl.toString()
}

function createApiProxy(prefix: string, target: string): ProxyOptions {
  const prefixPattern = new RegExp(`^${prefix}`)

  return {
    target,
    changeOrigin: true,
    rewrite: (path) => {
      const forwardedPath = path.replace(prefixPattern, apiBasePath)
      writeProxyLog(`[vite-proxy] ${path} -> ${buildTargetUrl(target, forwardedPath)}`)
      return forwardedPath
    },
  }
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const isProd = mode === 'production'
  const coreApiUrl = env.VITE_CORE_API_URL || 'http://localhost:8082'
  const aiApiUrl = env.VITE_AI_API_URL || 'http://localhost:8083'
  const fileApiUrl = env.VITE_FILE_API_URL || 'http://localhost:8081'
  const supportApiUrl = env.VITE_SUPPORT_API_URL || 'http://localhost:8080'

  return {
    base: isProd ? '/ele-ai-tender-web/' : '/',
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
        '/core-api': createApiProxy('/core-api', coreApiUrl),
        '/ai-api': createApiProxy('/ai-api', aiApiUrl),
        '/file-api': createApiProxy('/file-api', fileApiUrl),
        '/support-api': createApiProxy('/support-api', supportApiUrl),
      },
    },
    build: {
      sourcemap: false,
      chunkSizeWarningLimit: 1500,
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes('element-plus')) {
              return 'element-plus'
            }
            if (id.includes('node_modules/vue/') || id.includes('node_modules/vue-router/') || id.includes('node_modules/pinia/')) {
              return 'vue-vendor'
            }
          },
        },
      },
    },
  }
})
