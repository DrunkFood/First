/** 项目状态映射 */
export const PROJECT_STATUS_MAP: Record<string, { label: string; type?: '' | 'success' | 'warning' | 'info' | 'danger'; pulse?: boolean }> = {
  DRAFT: { label: '草稿', type: 'info' },
  IN_PROGRESS: { label: '编制中', type: '' },
  PENDING_DETECTION: { label: '待检测', type: 'warning' },
  DETECTING: { label: '检测中', type: '', pulse: true },
  DETECTION_PASSED: { label: '检测通过', type: 'success' },
  DETECTION_FAILED: { label: '检测失败', type: 'danger' },
  PUBLISHED: { label: '已发布', type: 'success' },
  ARCHIVED: { label: '已归档', type: 'info' },
  CANCELLED: { label: '已取消', type: 'info' },
}

/** 需求状态映射 */
export const REQUIREMENT_STATUS_MAP: Record<string, { label: string; type?: '' | 'success' | 'warning' | 'info' | 'danger'; pulse?: boolean }> = {
  DRAFT: { label: '草稿', type: 'info' },
  GENERATING: { label: '生成中', type: '', pulse: true },
  PENDING_REVIEW: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'danger' },
}

/** 项目类别映射 */
export const PROJECT_CATEGORY_MAP: Record<string, { label: string; type?: '' | 'success' | 'warning' | 'info' | 'danger' }> = {
  LIMITED_BELOW: { label: '限额以下', type: '' },
  PROPERTY_TRADE: { label: '产权交易', type: 'warning' },
  GOVERNMENT_PROCUREMENT: { label: '政府采购', type: 'success' },
}

/** 项目类型映射 */
export const PROJECT_TYPE_MAP: Record<string, { label: string; type?: '' | 'success' | 'warning' | 'info' | 'danger'; color?: string }> = {
  ENGINEERING: { label: '工程', color: '#336CFF' },
  GOODS: { label: '货物', color: '#31E3FD' },
  SERVICE: { label: '服务', color: '#FF8D3B' },
}
