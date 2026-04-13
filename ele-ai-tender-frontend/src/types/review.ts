export interface ReviewItemInfo {
  id: number
  projectId: number
  parentId: number
  level: number
  itemName: string
  itemContent?: string
  sortOrder: number
  createTime: string
}

export interface ReviewItemTree extends ReviewItemInfo {
  children: ReviewItemTree[]
}
