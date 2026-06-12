import request from '@/utils/request'
import type { PageResult } from '@/types'
import type { RequirementInfo, RequirementQueryParams, RequirementCreateParams, RequirementEditParams, MatchFile } from '@/types/requirement'
import type { RequirementDetectionRecord } from '@/types/detection'

export const requirementApi = {
  getList(params: RequirementQueryParams) {
    return request.get<any, PageResult<RequirementInfo>>('/core-api/v1/requirements', { params })
  },
  getById(id: number) {
    return request.get<any, RequirementInfo>(`/core-api/v1/requirements/${id}`)
  },
  create(data: RequirementCreateParams) {
    return request.post<any, RequirementInfo>('/core-api/v1/requirements', data)
  },
  update(id: number, data: Partial<RequirementCreateParams> | Partial<RequirementEditParams>) {
    return request.put(`/core-api/v1/requirements/${id}`, data)
  },
  deleteById(id: number) {
    return request.delete(`/core-api/v1/requirements/${id}`)
  },
  match(id: number, data: any) {
    return request.post(`/core-api/v1/requirements/${id}/match`, data)
  },
  /** 获取匹配文件列表 */
  getMatchFiles(params: { requirementId?: number; keyword?: string }) {
    return request.get<any, MatchFile[]>('/core-api/v1/requirements/match-files', { params })
  },
  /** 预览匹配文件 */
  previewMatchFile(fileId: number) {
    return request.get<any, any>(`/core-api/v1/requirements/match-files/${fileId}/preview`)
  },
  /** 导出需求文档 */
  exportDocument(id: number) {
    return request.get<any, Blob>(`/core-api/v1/requirements/${id}/export`, { responseType: 'blob' })
  },
  /** 提交AI生成需求 */
  generate(id: number, params: Record<string, any>) {
    return request.post<any, any>(`/core-api/v1/requirements/${id}/generate`, params)
  },
  /** 自动保存 */
  autoSave(id: number, content: string) {
    return request.post(`/core-api/v1/requirements/${id}/auto-save`, { content })
  },
  /** 获取自动保存内容 */
  getAutoSave(id: number) {
    return request.get<any, string | null>(`/core-api/v1/requirements/${id}/auto-save`)
  },
  /** 清除自动保存 */
  clearAutoSave(id: number) {
    return request.delete(`/core-api/v1/requirements/${id}/auto-save`)
  },
  /** 提交需求检测 */
  detect(id: number) {
    return request.post<any, Record<string, number>>(`/core-api/v1/requirements/${id}/detect`)
  },
  /** 接受需求检测建议 */
  acceptDetection(id: number, recordId: number, issueIndex: number) {
    return request.post(`/core-api/v1/requirements/${id}/detect/${recordId}/accept`, null, { params: { issueIndex } })
  },
  /** 拒绝需求检测建议 */
  rejectDetection(id: number, recordId: number, issueIndex: number) {
    return request.post(`/core-api/v1/requirements/${id}/detect/${recordId}/reject`, null, { params: { issueIndex } })
  },
  /** 获取需求检测记录列表 */
  getDetectionRecords(id: number) {
    return request.get<any, RequirementDetectionRecord[]>(`/core-api/v1/requirements/${id}/detect/records`)
  },
  /** 完成需求检测 */
  finishDetection(id: number) {
    return request.post(`/core-api/v1/requirements/${id}/detect/finish`)
  },
  /** 校验需求名称是否唯一 */
  checkName(requirementName: string, excludeId?: number) {
    return request.get<any, boolean>('/core-api/v1/requirements/check-name', {
      params: { requirementName, excludeId: excludeId || undefined },
    })
  },
}
