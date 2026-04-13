import axios from 'axios'
import type { ApiResponse, FileUploadResult } from '@/types'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

// 创建文件服务专用 axios 实例
const fileService = axios.create({
  baseURL: '/file-api',
  timeout: 60000,
})

// 请求拦截器
fileService.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    const token = userStore.token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
fileService.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '文件操作失败')
      return Promise.reject(new Error(res.message))
    }
    return response
  },
  (error) => {
    ElMessage.error('文件服务错误，请稍后重试')
    return Promise.reject(error)
  }
)

export const fileApi = {
  // 上传文件
  upload(file: File, bizType: string, onProgress?: (percent: number) => void): Promise<ApiResponse<FileUploadResult>> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('bizType', bizType)
    
    return fileService.post('/api/file/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      },
    }).then(res => res.data)
  },
  
  // 获取文件下载地址
  getDownloadUrl(fileId: string): string {
    return `/file-api/api/file/download/${fileId}`
  },
  
  // 删除文件
  delete(fileId: string): Promise<ApiResponse<void>> {
    return fileService.delete(`/api/file/delete/${fileId}`).then(res => res.data)
  },
}
