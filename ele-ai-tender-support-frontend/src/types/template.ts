/** 评审类型配置项 */
export interface ReviewTypeConfig {
  reviewType: string
  enabled: boolean
  generateStandard: boolean
  distinguishSubjectivity?: boolean
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
      distinguishSubjectivity: type === 'TECHNICAL' || type === 'CREDIT',
    })),
  }
}

/**
 * 规范化评审配置：补全 distinguishSubjectivity 默认值
 * 老数据该字段为 undefined，回退到 CREDIT/TECHNICAL=true（与后端 null 回退一致），保证编辑回填时 UI 显示正确
 */
export function normalizeReviewConfig(config: ReviewConfig): ReviewConfig {
  return {
    reviewTypes: config.reviewTypes.map(t => ({
      reviewType: t.reviewType,
      enabled: t.enabled,
      generateStandard: t.generateStandard,
      distinguishSubjectivity: t.distinguishSubjectivity !== undefined
        ? t.distinguishSubjectivity
        : (t.reviewType === 'TECHNICAL' || t.reviewType === 'CREDIT'),
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
