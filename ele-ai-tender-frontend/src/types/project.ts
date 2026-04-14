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
  status?: string
}

export interface ProjectCreateParams {
  projectName: string
  projectCategory: string
  projectType: string
  serviceSubType?: string
  budget?: number
  reviewType?: string
  requirementContent?: string
}

export interface ProjectVersionInfo {
  id: number
  versionNo: number
  changeDescription?: string
  createTime: string
  createName: string
}
