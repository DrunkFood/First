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
  templateId?: number
  requirementId?: number
  requirementSource?: string
  requirementContent?: string
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
