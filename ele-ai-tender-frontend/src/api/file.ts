import request from '@/utils/request'

export interface FileInfoVO {
  id?: number
  fileName?: string
  filePath?: string
  fileSize?: number | null
  fileSha256?: string
  bizType?: string
}

export const fileApi = {
  /** 通过文件ID下载文件 */
  download(fileId: number) {
    return request.get(`/file-api/file/download/${fileId}`, {
      responseType: 'blob',
    })
  },

  /** 获取文件信息 */
  getInfo(fileId: number) {
    return request.get<any, FileInfoVO>(`/file-api/file/info/${fileId}`)
  },
}
