import { request } from '@/utils/request'
import type {
  ModelConfigInfo,
  ModelConfigQueryParams,
  ModelConfigCreateParams,
  ModelConfigUpdateParams,
} from '@/types/model-config'
import type { ApiResponse, PageResult } from '@/types'
import { encryptByPublicKey, formatPublicKey } from '@/utils/crypto'

export const modelConfigApi = {
  // 分页查询模型配置
  getList(params: ModelConfigQueryParams): Promise<ApiResponse<PageResult<ModelConfigInfo>>> {
    return request.get<PageResult<ModelConfigInfo>>('/v1/model-configs', { params })
  },

  // 获取模型配置详情
  getById(id: number): Promise<ApiResponse<ModelConfigInfo>> {
    return request.get<ModelConfigInfo>(`/v1/model-configs/${id}`)
  },

  // 创建模型配置（apiKey自动RSA加密）
  async create(params: ModelConfigCreateParams): Promise<ApiResponse<ModelConfigInfo>> {
    const encrypted = await encryptApiKeyIfNeeded(params.apiKey)
    return request.post<ModelConfigInfo>('/v1/model-configs', {
      ...params,
      apiKey: encrypted.apiKey,
      keyId: encrypted.keyId,
    })
  },

  // 更新模型配置（apiKey自动RSA加密）
  async update(params: ModelConfigUpdateParams): Promise<ApiResponse<void>> {
    const encrypted = await encryptApiKeyIfNeeded(params.apiKey)
    return request.put(`/v1/model-configs/${params.id}`, {
      ...params,
      apiKey: encrypted.apiKey,
      keyId: encrypted.keyId,
    })
  },

  // 删除模型配置
  deleteById(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/v1/model-configs/${id}`)
  },

  // 批量删除模型配置
  deleteByIds(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/v1/model-configs/batch', { data: ids })
  },

  // 激活/停用模型配置
  changeStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/v1/model-configs/${id}/active`, undefined, { params: { isActive: status } })
  },

  // 设置为活跃模型
  setActive(id: number): Promise<ApiResponse<void>> {
    return request.put(`/v1/model-configs/${id}/active`, undefined, { params: { isActive: 1 } })
  },
}

/**
 * 如果apiKey不为空且不是脱敏值，则RSA加密后返回
 * 脱敏值特征：包含 **** 的值不加密（用户未修改密钥）
 */
async function encryptApiKeyIfNeeded(apiKey?: string): Promise<{ apiKey?: string; keyId?: string }> {
  if (!apiKey || apiKey.includes('****')) {
    // 未传或未修改，不加密
    return { apiKey: undefined, keyId: undefined }
  }
  // 获取RSA公钥
  const keyRes = await request.get<{ keyId: string; publicKey: string }>('/auth/public-key')
  const { keyId, publicKey } = keyRes.data || (keyRes as any).data
  const formattedKey = formatPublicKey(keyId, publicKey)
  const encrypted = encryptByPublicKey(formattedKey, apiKey)
  if (!encrypted) {
    throw new Error('API密钥加密失败')
  }
  return { apiKey: encrypted, keyId }
}
