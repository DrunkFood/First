// 系统参数信息
export interface SysParamInfo {
  id: number
  paramGroup: string
  paramKey: string
  paramValue?: string
  paramType: string
  paramName: string
  description?: string
  sortOrder: number
  createTime?: string
  modifyTime?: string
}
