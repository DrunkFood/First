import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ApiResponse } from '@/types'
import { useUserStore } from '@/store/user'
import router from '@/router'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: '/support-api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    const token = userStore.token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    
    // 业务状态码判断
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      
      // Token 过期或无效
      if (res.code === 401) {
        ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning',
        }).then(() => {
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
        })
      }
      
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    
    return response
  },
  (error) => {
    console.error('Response error:', error)
    
    let message = '网络错误，请稍后重试'
    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 400:
          message = '请求参数错误'
          break
        case 401:
          message = '未授权，请重新登录'
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求的资源不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = `服务器错误 (${status})`
      }
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
    }
    
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

// 封装请求方法
export const request = {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.get(url, config).then(res => res.data)
  },
  
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.post(url, data, config).then(res => res.data)
  },
  
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.put(url, data, config).then(res => res.data)
  },
  
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.delete(url, config).then(res => res.data)
  },
}

export default service
