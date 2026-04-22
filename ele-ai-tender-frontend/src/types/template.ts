// Word章节结构
export interface WordChapter {
  level: number
  title: string
  bookmarks?: string[]
}

export interface WordStructure {
  chapters: WordChapter[]
  placeholders: string[]
  bookmarks: string[]
}

export interface TemplateInfo {
  id: number
  templateCode?: string
  templateName: string
  projectCategory: string
  projectType: string
  fileId?: number               // 新增: 模板文件ID
  content?: string              // 语义变更: 模板用途说明
  description?: string
  structureDefinition?: WordStructure  // 语义变更: Word章节结构
  versionNo?: number
  isDefault: number
  status: string
  createTime?: string
  modifyTime?: string
}
