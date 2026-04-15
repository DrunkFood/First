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
  pendingDetectionCount: number
  detectionFailedCount: number
  inProgressCount: number
  dailyOperations: DailyStatItem[]
  projectStatusDist: StatusDistItem[]
}

// 每日统计项
export interface DailyStatItem {
  date: string
  count: number
}

// 状态分布项
export interface StatusDistItem {
  status: string
  statusName: string
  count: number
}
