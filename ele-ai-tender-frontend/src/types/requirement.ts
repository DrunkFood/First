/** 匹配文件信息 */
export interface MatchFile {
  id: number
  fileName: string
  fileType: string
  budget?: number
  matchPercent?: number
  matchDesc?: string
  uploadTime?: string
}

/** 需求类型枚举 */
export type RequirementType = 'NEW' | 'MODIFY' | 'CONTINUE'

/** 需求类型映射 */
export const REQUIREMENT_TYPE_MAP: Record<string, { label: string }> = {
  NEW: { label: '新增需求' },
  MODIFY: { label: '修改需求' },
  CONTINUE: { label: '延续需求' },
}

export interface RequirementInfo {
  id: number
  requirementName: string
  projectCategory: string
  projectType: string
  projectSubType?: string
  budget?: number
  requirementDescription?: string
  requirementType?: RequirementType
  matchMode?: string
  matchedFileId?: number
  matchedFileIds?: number[]
  matchedSimilarity?: number
  uploadedFileId?: number
  projectId?: number
  content?: string
  status: string
  progress?: number
  qualifications?: string[]
  createTime: string
  createName: string
}

export interface RequirementQueryParams {
  pageNum: number
  pageSize: number
  requirementName?: string
  status?: string
  projectType?: string
  createTimeStart?: string
  createTimeEnd?: string
}

export interface RequirementCreateParams {
  requirementName: string
  projectCategory: string
  projectType: string
  projectSubType?: string
  budget?: number
  requirementDescription?: string
  matchMode?: string
  matchedFileId?: number
  uploadedFileId?: number
  projectId?: number
  content?: string
  status?: string
}

export interface RequirementEditParams {
  requirementName: string
  projectType: string
  budget?: number
  requirementType: RequirementType
  requirementDescription?: string
  matchMode?: string
  matchedFileIds?: number[]
  uploadedFileId?: number
  qualifications: string[]
}
