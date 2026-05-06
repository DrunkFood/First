import JSEncrypt from 'jsencrypt'

/**
 * RSA公钥加密工具
 */

/**
 * 使用公钥加密数据
 */
export function encryptByPublicKey(publicKey: string, data: string): string | false {
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(publicKey)
  return encrypt.encrypt(data)
}

/**
 * 格式化公钥（确保PEM格式正确）
 */
export function formatPublicKey(_keyId: string, publicKey: string): string {
  if (publicKey.startsWith('-----BEGIN')) {
    return publicKey
  }
  return `-----BEGIN PUBLIC KEY-----\n${publicKey}\n-----END PUBLIC KEY-----`
}

/**
 * 生成UUID（兼容所有浏览器）
 * 优先 crypto.randomUUID → crypto.getRandomValues → Date.now+Math.random
 */
export function generateUUID(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    const bytes = new Uint8Array(16)
    crypto.getRandomValues(bytes)
    bytes[6] = ((bytes[6] ?? 0) & 0x0f) | 0x40
    bytes[8] = ((bytes[8] ?? 0) & 0x3f) | 0x80
    const hex = Array.from(bytes, b => b.toString(16).padStart(2, '0')).join('')
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`
}
