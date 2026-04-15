import { request } from '@/utils/request'
import type { StatisticsOverview } from '@/types/statistics'
import type { ApiResponse } from '@/types'

export const statisticsApi = {
  getOverview(): Promise<ApiResponse<StatisticsOverview>> {
    return request.get<StatisticsOverview>('/v1/statistics/overview')
  },
}
