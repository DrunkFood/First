import request from '@/utils/request'

export interface FileInfoVO {
  id?: number
  fileName?: string
  filePath?: string
  fileSize?: number | null
  fileSha256?: string
  bizType?: string
}

/** Word文档文本提取段：与后端 WordTextExtractor 输出对齐 */
export interface TextSegmentVO {
  elementIndex: number
  text: string
  type: 'paragraph' | 'table'
  fullTextOffset: number
  tableIndex?: number
  rowIndex?: number
  cellIndex?: number
}

export interface ExtractTextVO {
  fullText: string
  segments: TextSegmentVO[]
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

  /** 提取Word文档文本+位置索引（用于检测问题精确定位） */
  extractText(fileId: number) {
    return request.post<any, ExtractTextVO>(`/file-api/file/extract-text`, { fileId })
  },
}
