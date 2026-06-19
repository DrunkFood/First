/** 项目状态映射 */
export const PROJECT_STATUS_MAP: Record<string, { label: string; type?: '' | 'success' | 'warning' | 'info' | 'danger'; pulse?: boolean }> = {
  DRAFT: { label: '草稿', type: 'info' },
  IN_PROGRESS: { label: '编制中', type: '' },
  PENDING_DETECTION: { label: '待检测', type: 'warning' },
  DETECTING: { label: '检测中', type: '', pulse: true },
  DETECTION_PASSED: { label: '检测通过', type: 'success' },
  DETECTION_FAILED: { label: '检测未通过', type: 'danger' },
  PUBLISHED: { label: '已发布', type: 'success' },
  ARCHIVED: { label: '已归档', type: 'info' },
  CANCELLED: { label: '已取消', type: 'info' },
}

/** 需求状态映射 */
export const REQUIREMENT_STATUS_MAP: Record<string, { label: string; type?: '' | 'success' | 'warning' | 'info' | 'danger'; pulse?: boolean }> = {
  IN_PROGRESS: { label: '进行中', type: '' },
  COMPLETED: { label: '已完成', type: 'success' },
}

/** 项目类别映射 */
export const PROJECT_CATEGORY_MAP: Record<string, { label: string; type?: '' | 'success' | 'warning' | 'info' | 'danger' }> = {
  SMALL_TRADE: { label: '小额交易', type: '' },
  GOVERNMENT_PROCUREMENT: { label: '政府采购', type: 'success' },
  COMPREHENSIVE_TRADE: { label: '综合交易', type: 'warning' },
}

/** 项目类型映射 */
export const PROJECT_TYPE_MAP: Record<string, { label: string; type?: '' | 'success' | 'warning' | 'info' | 'danger'; color?: string }> = {
  ENGINEERING: { label: '工程', color: '#336CFF' },
  GOODS: { label: '货物', color: '#31E3FD' },
  SERVICE: { label: '服务', color: '#FF8D3B' },
}
