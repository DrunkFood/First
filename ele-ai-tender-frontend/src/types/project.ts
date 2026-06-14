export interface ProjectInfo {
  id: number
  projectCode: string
  projectName: string
  projectCategory: string
  projectType: string
  serviceSubType?: string
  budget?: number
  reviewType?: string
  status: string
  currentPhase?: number
  progress?: number
  templateId?: number
  requirementId?: number
  requirementSource?: string
  requirementContent?: string
  generatedFileId?: number
  matchMode?: string
  matchedFileId?: number
  matchedSimilarity?: number
  uploadedFileId?: number
  projectDescription?: string
  tenderUnit?: string
  projectLocation?: string
  contactPerson?: string
  contactPhone?: string
  createTime: string
  createName: string
}

export interface ProjectQueryParams {
  pageNum: number
  pageSize: number
  projectName?: string
  projectCode?: string
  status?: string
  projectCategory?: string
  projectType?: string
  createTimeStart?: string
  createTimeEnd?: string
}

export interface ProjectCreateParams {
  projectCode?: string
  projectName: string
  projectCategory: string
  projectType: string
  serviceSubType?: string
  budget?: number
  reviewType?: string
  requirementContent?: string
  templateId?: number
  requirementId?: number
  requirementSource?: string
  tenderUnit?: string
  contactPerson?: string
  contactPhone?: string
  projectDescription?: string
  matchMode?: string
  matchedFileId?: number
  matchedSimilarity?: number
  uploadedFileId?: number
}

export interface ProjectVersionInfo {
  id: number
  versionNo: number
  changeDescription?: string
  createTime: string
  createName: string
}
