import request from '@/utils/request'
import type { DetectionProgressVO, DetectionReportVO, DetectionSubmitRequest } from '@/types/detection'

export const detectionApi = {
  /** 提交最终文档检测 */
  submit(projectId: number, data?: DetectionSubmitRequest) {
    return request.post<any, Record<string, number>>(`/core-api/v1/detection/submit/${projectId}`, data)
  },

  /** 获取检测进度 */
  getProgress(projectId: number) {
    return request.get<any, DetectionProgressVO>(`/core-api/v1/detection/progress/${projectId}`)
  },

  /** 获取检测报告 */
  getReport(projectId: number) {
    return request.get<any, DetectionReportVO>(`/core-api/v1/detection/report/${projectId}`)
  },

  /** 接受检测建议 */
  accept(recordId: number, issueIndex: number) {
    return request.post(`/core-api/v1/detection/${recordId}/accept`, null, { params: { issueIndex } })
  },

  /** 拒绝检测建议 */
  reject(recordId: number, issueIndex: number) {
    return request.post(`/core-api/v1/detection/${recordId}/reject`, null, { params: { issueIndex } })
  },

  /** 一键接受所有建议 */
  acceptAll(projectId: number) {
    return request.post(`/core-api/v1/detection/accept-all/${projectId}`)
  },

  /** 跳过检测 */
  skip(projectId: number) {
    return request.post(`/core-api/v1/detection/skip/${projectId}`)
  },

  /** 重新检测 */
  retry(projectId: number) {
    return request.post<any, Record<string, number>>(`/core-api/v1/detection/retry/${projectId}`)
  },
}
