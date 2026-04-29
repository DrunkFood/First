/** 评审类型配置项 */
export interface ReviewTypeConfig {
  reviewType: string
  enabled: boolean
  generateStandard: boolean
}

/** 评审项配置 */
export interface ReviewConfig {
  reviewTypes: ReviewTypeConfig[]
}

/** 评审类型中文标签映射 */
export const REVIEW_TYPE_LABELS: Record<string, string> = {
  COMPLIANCE: '符合性审查',
  TECHNICAL: '技术标评审',
  CREDIT: '资信标评审',
  COMMERCIAL: '商务评审',
}

/** 构建默认评审配置 */
export function buildDefaultReviewConfig(): ReviewConfig {
  return {
    reviewTypes: Object.keys(REVIEW_TYPE_LABELS).map(type => ({
      reviewType: type,
      enabled: true,
      generateStandard: true,
    })),
  }
}

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
  reviewConfig?: ReviewConfig
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
  reviewConfig?: string
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
  reviewConfig?: string
}
