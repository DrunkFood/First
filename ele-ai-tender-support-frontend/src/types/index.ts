// API 响应类型
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp?: number
}

// 分页请求参数
export interface PageParams {
  pageNum: number
  pageSize: number
}

// 分页响应数据
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface AccessLogInfo {
  id: number
  traceId?: string
  serviceName?: string
  logType?: string
  httpMethod?: string
  requestUri?: string
  clientIp?: string
  statusCode?: number
  successFlag?: number
  elapsedMs?: number
  tokenType?: string
  appKey?: string
  userId?: string
  userName?: string
  enterpriseId?: string
  enterpriseName?: string
  bizType?: string
  bizId?: string
  projectId?: string
  tenderId?: string
  fileId?: string
  fileName?: string
  requestHeaders?: string
  requestBody?: string
  responseBody?: string
  errorType?: string
  errorMessage?: string
  createTime?: string
}

// 用户信息
export interface UserInfo {
  id: number
  username: string
  realName: string
  email?: string
  phone?: string
  status: number
  roleIds: number[]
  roles?: RoleInfo[]
  createTime?: string
  updateTime?: string
}

// 认证接口返回用户信息（后端字段与管理页字段不完全一致）
export interface AuthUserInfo {
  userId?: number
  id?: number
  username: string
  realName: string
  email?: string
  phone?: string
  roles?: string[]
}

// 角色信息
export interface RoleInfo {
  id: number
  roleName: string
  roleCode: string
  description?: string
  status: number
  menuIds?: number[]
  createTime?: string
  updateTime?: string
}

// 菜单信息
export interface MenuInfo {
  id: number
  parentId: number
  menuName: string
  menuType: number // 0-目录 1-菜单 2-按钮
  path?: string
  menuUrl?: string
  component?: string
  permission?: string
  icon?: string
  sortOrder: number
  visible: number
  status: number
  children?: MenuInfo[]
  createTime?: string
  updateTime?: string
}

// 接入系统信息
export interface ExternalSystem {
  id: number
  systemName: string
  systemCode: string
  systemUrl?: string
  appKey: string
  appSecret?: string
  expireTime?: string
  description?: string
  tenderDocumentSuffix?: string
  bidDocumentSuffix?: string
  status: number
  createTime?: string
  updateTime?: string
}

// 版本信息
export interface VersionInfo {
  id: number
  versionNumber: string
  versionName: string
  description?: string
  releaseNotes?: string
  status: number // 0-开发中 1-测试中 2-已发布 3-已废弃
  releaseDate?: string
  fileId?: string
  plugins?: PluginInfo[]
  createTime?: string
  updateTime?: string
}

// 插件信息
export interface PluginInfo {
  id: number
  versionId: number
  pluginName: string
  pluginCode: string
  description?: string
  status: number
  fileId?: string
  sortOrder: number
  createTime?: string
  updateTime?: string
}

// 登录请求参数
export interface LoginParams {
  username: string
  password: string
}

// 登录响应数据
export interface LoginResult {
  token: string
  userInfo: AuthUserInfo
  permissions?: string[]
  expireIn?: number
}

// 文件上传响应
export interface FileUploadResult {
  fileId: string | number
  fileName: string
  fileSize: number
  fileSha256?: string
}
