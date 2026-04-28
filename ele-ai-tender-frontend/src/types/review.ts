export interface ReviewItemInfo {
  id: number
  projectId: number
  parentId: number | null
  level: number
  itemName: string
  itemContent?: string
  sortOrder: number
  reviewType?: string
  score?: number
  maxScore?: number
  weight?: number
  subjectivity?: string
  isRequired?: number
  createTime: string
}

export interface ReviewItemTree extends ReviewItemInfo {
  children: ReviewItemTree[]
}
