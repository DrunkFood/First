// Word章节结构
export interface WordChapter {
  level: number
  title: string
  bookmarks?: string[]
}

export interface WordStructure {
  chapters: WordChapter[]
  placeholders: string[]
  bookmarks: string[]
}

// 模板信息
export interface TemplateInfo {
  id: number
  templateName: string
  projectCategory: string
  projectType: string
  fileId?: number
  content?: string
  structureDefinition?: WordStructure
  isDefault: number
  version: string
  description?: string
  status: string
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
  projectCategory?: string
  projectType?: string
  status?: string
}

// 模板创建参数
export interface TemplateCreateParams {
  templateName: string
  projectCategory: string
  projectType: string
  fileId?: number
  content?: string
  structureDefinition?: string
  isDefault?: number
  description?: string
}

// 模板更新参数
export interface TemplateUpdateParams {
  id: number
  templateName?: string
  projectCategory?: string
  projectType?: string
  fileId?: number
  content?: string
  structureDefinition?: string
  isDefault?: number
  description?: string
  status?: string
}
