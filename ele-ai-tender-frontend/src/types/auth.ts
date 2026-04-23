export interface LoginRequest {
  username: string
  password: string
  keyId?: string // RSA加密后由auth.ts自动填充
}

export interface PhoneLoginRequest {
  phone: string
  code: string
}

export interface SendSmsCodeRequest {
  phone: string
  scene?: string // LOGIN | RESET_PWD，默认 LOGIN
}

export interface ResetPasswordRequest {
  phone: string
  code: string
  keyId: string
  newPassword: string // RSA 加密后的密码
}

export interface UserInfo {
  userId: number
  username: string
  realName: string
  email: string
  phone: string
  roles: string[]
}

export interface LoginResult {
  token: string
  expireIn: number
  permissions: string[]
  userInfo: UserInfo
}

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}
