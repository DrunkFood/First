// AI模型配置信息
export interface ModelConfigInfo {
  id: number
  modelName: string
  modelType: string
  provider?: string       // 模型供应商: OPENAI/ZHIPU
  endpoint: string
  apiKey?: string        // 脱敏后的密钥（如 sk-****1234）
  modelCode: string
  maxTokens: number
  temperature: number
  topP: number
  timeout: number
  parameters?: string
  tokenUsage: number
  tokenLimit: number
  isActive: number
  usageScenario?: string
  cost?: number
  remark?: string
  createTime?: string
  modifyTime?: string
  createId?: number
  createName?: string
}

// AI模型配置查询参数
export interface ModelConfigQueryParams {
  pageNum: number
  pageSize: number
  modelName?: string
  modelType?: string
  provider?: string
  usageScenario?: string
}

// AI模型配置创建参数
export interface ModelConfigCreateParams {
  modelName: string
  modelType: string
  provider?: string       // 模型供应商: OPENAI/ZHIPU，默认OPENAI
  endpoint: string
  apiKey?: string         // RSA加密后的密文
  keyId?: string          // RSA密钥ID
  modelCode: string
  maxTokens?: number
  temperature?: number
  topP?: number
  timeout?: number
  parameters?: string
  tokenLimit?: number
  usageScenario?: string
  remark?: string
}

// AI模型配置更新参数
export interface ModelConfigUpdateParams {
  id: number
  modelName?: string
  modelType?: string
  provider?: string       // 模型供应商: OPENAI/ZHIPU
  endpoint?: string
  apiKey?: string         // RSA加密后的密文（为空则不更新）
  keyId?: string          // RSA密钥ID
  modelCode?: string
  maxTokens?: number
  temperature?: number
  topP?: number
  timeout?: number
  parameters?: string
  tokenLimit?: number
  usageScenario?: string
  remark?: string
}
