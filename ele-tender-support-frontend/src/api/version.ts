import { request } from '@/utils/request'
import type { VersionInfo, PluginInfo, PageParams, PageResult, ApiResponse } from '@/types'

export interface VersionQueryParams extends PageParams {
  versionNumber?: string
  versionName?: string
  status?: number
}

export interface CreateVersionParams {
  versionNumber: string
  versionName: string
  description?: string
  releaseNotes?: string
  fileId?: string
}

export interface UpdateVersionParams {
  id: number
  versionName?: string
  description?: string
  releaseNotes?: string
  status?: number
  fileId?: string
  releaseDate?: string
}

export interface CreatePluginParams {
  versionId: number
  pluginName: string
  pluginCode: string
  description?: string
  fileId?: string
  sortOrder: number
}

export interface UpdatePluginParams {
  id: number
  pluginCode?: string
  pluginName?: string
  description?: string
  status?: number
  fileId?: string
  sortOrder?: number
}

interface BackendVersion {
  id: number
  systemName: string
  versionNumber: string
  fileId?: number
  releaseNotes?: string
  status: number
  createTime?: string
  modifyTime?: string
}

interface BackendPlugin {
  id: number
  pluginName: string
  versionNumber: string
  compatibleMainVersion: string
  fileId?: number
  releaseNotes?: string
  status: number
  createTime?: string
  modifyTime?: string
}

const mapVersion = (item: BackendVersion): VersionInfo => ({
  id: item.id,
  versionNumber: item.versionNumber,
  versionName: item.systemName,
  description: '',
  releaseNotes: item.releaseNotes,
  status: item.status,
  fileId: item.fileId != null ? String(item.fileId) : undefined,
  createTime: item.createTime,
  updateTime: item.modifyTime,
})

const mapPlugin = (item: BackendPlugin): PluginInfo => ({
  id: item.id,
  versionId: 0,
  pluginName: item.pluginName,
  pluginCode: item.versionNumber,
  description: item.releaseNotes,
  status: item.status,
  fileId: item.fileId != null ? String(item.fileId) : undefined,
  sortOrder: 0,
  createTime: item.createTime,
  updateTime: item.modifyTime,
})

export const versionApi = {
  // 分页查询版本
  async getList(params: VersionQueryParams): Promise<ApiResponse<PageResult<VersionInfo>>> {
    const res = await request.get<PageResult<BackendVersion>>('/versions', { params })
    return {
      ...res,
      data: {
        ...res.data,
        records: (res.data.records || []).map(mapVersion),
      },
    }
  },
  
  // 获取版本详情
  async getById(id: number): Promise<ApiResponse<VersionInfo>> {
    const res = await request.get<BackendVersion>(`/versions/${id}`)
    return { ...res, data: mapVersion(res.data) }
  },
  
  // 创建版本
  create(params: CreateVersionParams): Promise<ApiResponse<VersionInfo>> {
    return request.post<VersionInfo>('/versions', {
      versionNumber: params.versionNumber,
      systemName: params.versionName,
      releaseNotes: params.releaseNotes || params.description,
      fileId: params.fileId ? Number(params.fileId) : undefined,
    })
  },
  
  // 更新版本
  update(params: UpdateVersionParams): Promise<ApiResponse<void>> {
    return request.put(`/versions/${params.id}`, {
      systemName: params.versionName,
      releaseNotes: params.releaseNotes || params.description,
      status: params.status,
      fileId: params.fileId ? Number(params.fileId) : undefined,
    })
  },
  
  // 删除版本
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/versions/${id}`)
  },
  
  // 发布版本
  publish(id: number): Promise<ApiResponse<void>> {
    return request.post(`/versions/${id}/publish`)
  },
  
  // 废弃版本
  deprecate(id: number): Promise<ApiResponse<void>> {
    return request.post(`/versions/${id}/deprecate`)
  },
  
  // 获取版本的插件列表
  async getPlugins(versionId: number): Promise<ApiResponse<PluginInfo[]>> {
    const res = await request.get<BackendPlugin[]>(`/versions/${versionId}/plugins`)
    return { ...res, data: (res.data || []).map(mapPlugin) }
  },
  
  // 创建插件
  createPlugin(params: CreatePluginParams): Promise<ApiResponse<PluginInfo>> {
    return request.post<PluginInfo>(`/versions/${params.versionId}/plugins`, {
      pluginName: params.pluginName,
      versionNumber: params.pluginCode,
      fileId: params.fileId ? Number(params.fileId) : undefined,
      releaseNotes: params.description,
    })
  },
  
  // 更新插件
  updatePlugin(params: UpdatePluginParams): Promise<ApiResponse<void>> {
    return request.put(`/versions/plugins/${params.id}`, {
      pluginName: params.pluginName,
      versionNumber: params.pluginCode,
      fileId: params.fileId ? Number(params.fileId) : undefined,
      releaseNotes: params.description,
      status: params.status,
    })
  },
  
  // 删除插件
  deletePlugin(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/versions/plugins/${id}`)
  },
}
