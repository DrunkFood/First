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
