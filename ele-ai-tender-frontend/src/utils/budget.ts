/**
 * 预算金额单位转换工具
 * 后端接口统一单位：元
 * 前端展示统一单位：万元
 */

/** 元 → 万元（用于后端数据加载到表单展示） */
export function toWanYuan(yuan: number | undefined | null): number | undefined {
  if (yuan == null) return undefined
  return yuan / 10000
}

/** 万元 → 元（用于表单提交到后端） */
export function toYuan(wanYuan: number | undefined | null): number | undefined {
  if (wanYuan == null) return undefined
  return wanYuan * 10000
}

/** 格式化预算金额：元 → "¥xxx 万元" 字符串（用于列表和详情展示，万元保留6位小数精确到分） */
export function formatBudgetWanYuan(yuan: number | undefined | null): string {
  if (yuan == null) return '-'
  const wanYuan = yuan / 10000
  return `¥${wanYuan.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 6 })} 万元`
}
