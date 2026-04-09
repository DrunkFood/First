import { request } from '@/utils/request'
import type { AccessLogInfo, ApiResponse, PageParams, PageResult } from '@/types'

export interface AccessLogQueryParams extends PageParams {
  traceId?: string
  serviceName?: string
  statusCode?: number
  bizType?: string
  bizId?: string
  projectId?: string
  tenderId?: string
  startTime?: string
  endTime?: string
}

interface BackendAccessLog {
  id: number
  traceId?: string
  serviceName?: string
  logType?: string
  httpMethod?: string
  requestUri?: string
  clientIp?: string
  statusCode?: number
  successFlag?: number
  elapsedMs?: number
  tokenType?: string
  appKey?: string
  userId?: string
  userName?: string
  enterpriseId?: string
  enterpriseName?: string
  bizType?: string
  bizId?: string
  projectId?: string
  tenderId?: string
  fileId?: string
  fileName?: string
  requestHeaders?: string
  requestBody?: string
  responseBody?: string
  errorType?: string
  errorMessage?: string
  createTime?: string
}

const mapAccessLog = (item: BackendAccessLog): AccessLogInfo => ({
  id: item.id,
  traceId: item.traceId,
  serviceName: item.serviceName,
  logType: item.logType,
  httpMethod: item.httpMethod,
  requestUri: item.requestUri,
  clientIp: item.clientIp,
  statusCode: item.statusCode,
  successFlag: item.successFlag,
  elapsedMs: item.elapsedMs,
  tokenType: item.tokenType,
  appKey: item.appKey,
  userId: item.userId,
  userName: item.userName,
  enterpriseId: item.enterpriseId,
  enterpriseName: item.enterpriseName,
  bizType: item.bizType,
  bizId: item.bizId,
  projectId: item.projectId,
  tenderId: item.tenderId,
  fileId: item.fileId,
  fileName: item.fileName,
  requestHeaders: item.requestHeaders,
  requestBody: item.requestBody,
  responseBody: item.responseBody,
  errorType: item.errorType,
  errorMessage: item.errorMessage,
  createTime: item.createTime,
})

export const accessLogApi = {
  async getList(params: AccessLogQueryParams): Promise<ApiResponse<PageResult<AccessLogInfo>>> {
    const res = await request.get<PageResult<BackendAccessLog>>('/access-logs', {
      params: {
        pageNum: params.pageNum,
        pageSize: params.pageSize,
        traceId: params.traceId,
        serviceName: params.serviceName,
        statusCode: params.statusCode,
        bizType: params.bizType,
        bizId: params.bizId,
        projectId: params.projectId,
        tenderId: params.tenderId,
        startTime: params.startTime,
        endTime: params.endTime,
      },
    })
    return {
      ...res,
      data: {
        ...res.data,
        records: (res.data.records || []).map(mapAccessLog),
      },
    }
  },
}
