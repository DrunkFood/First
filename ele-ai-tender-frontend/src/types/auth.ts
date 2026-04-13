export interface LoginRequest {
  username: string
  password: string
}

export interface PhoneLoginRequest {
  phone: string
  code: string
}

export interface SendSmsCodeRequest {
  phone: string
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
