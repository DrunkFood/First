// AI模型配置信息
export interface ModelConfigInfo {
  id: number
  modelName: string
  modelType: string
  endpoint: string
  apiKey?: string
  modelCode: string
  maxTokens: number
  temperature: number
  topP: number
  timeout: number
  parameters?: string
  tokenUsage: number
  tokenLimit: number
  isActive: number
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
  createId?: number
  createName?: string
}

// AI模型配置查询参数
export interface ModelConfigQueryParams {
  pageNum: number
  pageSize: number
  modelName?: string
  modelType?: string
  status?: number
}

// AI模型配置创建参数
export interface ModelConfigCreateParams {
  modelName: string
  modelType: string
  endpoint: string
  apiKey?: string
  modelCode: string
  maxTokens?: number
  temperature?: number
  topP?: number
  timeout?: number
  parameters?: string
  tokenLimit?: number
  remark?: string
}

// AI模型配置更新参数
export interface ModelConfigUpdateParams {
  id: number
  modelName?: string
  modelType?: string
  endpoint?: string
  apiKey?: string
  modelCode?: string
  maxTokens?: number
  temperature?: number
  topP?: number
  timeout?: number
  parameters?: string
  tokenLimit?: number
  status?: number
  remark?: string
}
