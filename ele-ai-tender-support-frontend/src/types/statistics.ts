// 统计概览数据
export interface StatisticsOverview {
  userCount: number
  roleCount: number
  accessSystemCount: number
  versionCount: number
  projectCount: number
  requirementCount: number
  templateCount: number
  knowledgeCount: number
  modelConfigCount: number
  operationLogCount: number
  policyFileCount: number
  todayOperationCount: number
  todayProjectCount: number
  unreadMessageCount: number
  dailyOperations: DailyStatItem[]
}

// 每日统计项
export interface DailyStatItem {
  date: string
  count: number
}
