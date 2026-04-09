import { request } from '@/utils/request'
import type { ExternalSystem, PageParams, PageResult, ApiResponse } from '@/types'

export interface ExternalSystemQueryParams extends PageParams {
  systemName?: string
  appKey?: string
  status?: number
}

export interface CreateExternalSystemParams {
  systemName: string
  systemUrl?: string
  expireTime?: string
  description?: string
  tenderDocumentSuffix?: string
  bidDocumentSuffix?: string
}

export interface UpdateExternalSystemParams {
  id: number
  systemName?: string
  systemUrl?: string
  expireTime?: string
  description?: string
  status?: number
  tenderDocumentSuffix?: string
  bidDocumentSuffix?: string
}

interface BackendExternalSystem {
  id: number
  systemName: string
  systemUrl?: string
  appKey: string
  appSecret?: string
  expireTime?: string
  description?: string
  tenderDocumentSuffix?: string
  bidDocumentSuffix?: string
  status: number
  createTime?: string
  modifyTime?: string
}

const mapExternalSystem = (item: BackendExternalSystem): ExternalSystem => ({
  id: item.id,
  systemName: item.systemName,
  systemUrl: item.systemUrl,
  systemCode: item.appKey,
  appKey: item.appKey,
  appSecret: item.appSecret,
  expireTime: item.expireTime,
  description: item.description,
  tenderDocumentSuffix: item.tenderDocumentSuffix,
  bidDocumentSuffix: item.bidDocumentSuffix,
  status: item.status,
  createTime: item.createTime,
  updateTime: item.modifyTime,
})

export const externalSystemApi = {
  // 分页查询接入系统
  async getList(params: ExternalSystemQueryParams): Promise<ApiResponse<PageResult<ExternalSystem>>> {
    const res = await request.get<PageResult<BackendExternalSystem>>('/external-systems', {
      params: {
        pageNum: params.pageNum,
        pageSize: params.pageSize,
        systemName: params.systemName,
        appKey: params.appKey,
        status: params.status,
      },
    })
    return {
      ...res,
      data: {
        ...res.data,
        records: (res.data.records || []).map(mapExternalSystem),
      },
    }
  },
  
  // 获取接入系统详情
  async getById(id: number): Promise<ApiResponse<ExternalSystem>> {
    const res = await request.get<BackendExternalSystem>(`/external-systems/${id}`)
    return { ...res, data: mapExternalSystem(res.data) }
  },
  
  // 创建接入系统
  create(params: CreateExternalSystemParams): Promise<ApiResponse<ExternalSystem>> {
    return request.post<ExternalSystem>('/external-systems', {
      systemName: params.systemName,
      systemUrl: params.systemUrl,
      expireTime: params.expireTime,
      description: params.description,
      tenderDocumentSuffix: params.tenderDocumentSuffix,
      bidDocumentSuffix: params.bidDocumentSuffix,
    })
  },
  
  // 更新接入系统
  update(params: UpdateExternalSystemParams): Promise<ApiResponse<void>> {
    return request.put(`/external-systems/${params.id}`, {
      systemName: params.systemName,
      systemUrl: params.systemUrl,
      expireTime: params.expireTime,
      description: params.description,
      status: params.status,
      tenderDocumentSuffix: params.tenderDocumentSuffix,
      bidDocumentSuffix: params.bidDocumentSuffix,
    })
  },
  
  // 删除接入系统
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/external-systems/${id}`)
  },
  
  // 重新生成密钥
  regenerateSecret(id: number): Promise<ApiResponse<{ appSecret: string }>> {
    return request.post(`/external-systems/${id}/regenerate-secret`)
  },
  
  // 启用/禁用接入系统
  changeStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/external-systems/${id}/status`, undefined, { params: { status } })
  },
}
