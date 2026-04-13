// 模板信息
export interface TemplateInfo {
  id: number
  templateName: string
  templateCategory: string
  templateType: string
  matchMode: string
  content: string
  structureDefinition?: string
  isDefault: number
  version: string
  remark?: string
  status: number
  createTime?: string
  updateTime?: string
  createId?: number
  createName?: string
}

// 模板查询参数
export interface TemplateQueryParams {
  pageNum: number
  pageSize: number
  templateName?: string
  templateCategory?: string
  templateType?: string
  status?: number
}

// 模板创建参数
export interface TemplateCreateParams {
  templateName: string
  templateCategory: string
  templateType: string
  matchMode: string
  content?: string
  structureDefinition?: string
  isDefault?: number
  remark?: string
}

// 模板更新参数
export interface TemplateUpdateParams {
  id: number
  templateName?: string
  templateCategory?: string
  templateType?: string
  matchMode?: string
  content?: string
  structureDefinition?: string
  isDefault?: number
  remark?: string
  status?: number
}
