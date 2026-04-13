import { defineStore } from 'pinia'
import { requirementApi } from '@/api/requirement'
import type { RequirementInfo, RequirementCreateParams } from '@/types/requirement'

interface RequirementState {
  currentRequirement: RequirementInfo | null
  list: RequirementInfo[]
  total: number
  loading: boolean
}

export const useRequirementStore = defineStore('requirement', {
  state: (): RequirementState => ({
    currentRequirement: null,
    list: [],
    total: 0,
    loading: false,
  }),
  actions: {
    async fetchList(params: any) {
      this.loading = true
      try {
        const result = await requirementApi.getList(params)
        this.list = result.records
        this.total = result.total
      } finally {
        this.loading = false
      }
    },
    async createRequirement(data: RequirementCreateParams) {
      return requirementApi.create(data)
    },
    async updateRequirement(id: number, data: any) {
      return requirementApi.update(id, data)
    },
    async deleteRequirement(id: number) {
      return requirementApi.deleteById(id)
    },
  },
})
