// 模型路由规则信息（VO）
export interface ModelRouteRuleInfo {
  id: number
  usageScenario: string
  primaryModelId: number
  primaryModelName?: string
  fallbackModelId?: number
  fallbackModelName?: string
  priority: number
  isActive: number
  description?: string
  createTime?: string
}

// 模型路由规则查询参数
export interface ModelRouteQueryParams {
  pageNum: number
  pageSize: number
  usageScenario?: string
}

// 模型路由规则创建/更新参数
export interface ModelRouteRuleParams {
  id?: number
  usageScenario: string
  primaryModelId: number
  fallbackModelId?: number
  priority?: number
  isActive?: number
  description?: string
}
