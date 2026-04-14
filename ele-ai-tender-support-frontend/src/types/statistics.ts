// 统计概览数据
export interface StatisticsOverview {
  userCount: number
  templateCount: number
  knowledgeCount: number
  modelConfigCount: number
  operationLogCount: number
  policyFileCount: number
  todayOperationCount: number
  unreadMessageCount: number
  dailyOperations: DailyStatItem[]
}

// 每日统计项
export interface DailyStatItem {
  date: string
  count: number
}
