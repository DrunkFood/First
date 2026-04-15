export interface RequirementInfo {
  id: number
  requirementName: string
  projectCategory: string
  projectType: string
  budget?: number
  requirementDescription?: string
  matchMode?: string
  matchedFileId?: number
  matchedSimilarity?: number
  uploadedFileId?: number
  projectId?: number
  content?: string
  status: string
  progress?: number
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
  budget?: number
  requirementDescription?: string
  matchMode?: string
  uploadedFileId?: number
  projectId?: number
  content?: string
  status?: string
}
