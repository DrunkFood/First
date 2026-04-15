import { request } from '@/utils/request'
import type { OperationLogInfo, OperationLogQueryParams } from '@/types/operation-log'
import type { ApiResponse, PageResult } from '@/types'

export const operationLogApi = {
  getList(params: OperationLogQueryParams): Promise<ApiResponse<PageResult<OperationLogInfo>>> {
    return request.get<PageResult<OperationLogInfo>>('/v1/operation-logs', { params })
  },
}
