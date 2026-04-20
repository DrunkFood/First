/** 检测类型（需求级只用 SENSITIVE_WORD/TYPO，项目级用全部） */
export type DetectionType = 'SENSITIVE_WORD' | 'TYPO' | 'POLICY_REVIEW' | 'FORMAT_CHECK' | 'FAIRNESS' | 'COMPLIANCE'

/** 需求级检测类型 */
export type RequirementDetectionType = 'SENSITIVE_WORD' | 'TYPO'

/** 检测项进度 */
export interface DetectionItemProgress {
  detectionType: DetectionType
  typeName: string
  taskStatus: string
  issueCount: number
  score?: number
}

/** 检测进度 */
export interface DetectionProgressVO {
  projectId: number
  overallStatus: string
  items: DetectionItemProgress[]
}

/** 检测问题项 */
export interface DetectionIssueVO {
  recordId: number
  detectionType: DetectionType
  typeName: string
  description: string
  location: string
  original: string
  suggestion: string
  severity: 'HIGH' | 'MEDIUM' | 'LOW'
  handleStatus: number
  issueIndex: number
}

/** 检测报告 */
export interface DetectionReportVO {
  projectId: number
  projectName: string
  overallStatus: string
  totalIssueCount: number
  totalScore?: number
  issues: DetectionIssueVO[]
}

/** 检测提交请求 */
export interface DetectionSubmitRequest {
  policyFileIds?: number[]
}

/** 需求检测记录 */
export interface RequirementDetectionRecord {
  id: number
  projectId: number
  requirementId: number
  detectionType: string
  status: string
  taskId: number
  result: string | null
}
