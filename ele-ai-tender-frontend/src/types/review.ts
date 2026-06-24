export interface ReviewItemInfo {
  id: number
  projectId: number
  parentId: number | null
  level: number
  itemName: string
  itemContent?: string
  sortOrder: number
  reviewType?: string
  score?: number
  maxScore?: number
  weight?: number
  subjectivity?: string
  isRequired?: number
  createTime: string
}

export interface ReviewItemTree extends ReviewItemInfo {
  children: ReviewItemTree[]
}

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
  /** 评分模式：SCORE(分值模式,默认) / WEIGHT(权重模式) */
  scoreMode?: string
}

/** 评审类型中文标签映射 */
export const REVIEW_TYPE_LABELS: Record<string, string> = {
  COMPLIANCE: '符合性审查',
  TECHNICAL: '技术标评审',
  CREDIT: '资信标评审',
  COMMERCIAL: '商务评审',
}
