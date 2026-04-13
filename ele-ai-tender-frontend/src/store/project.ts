import { defineStore } from 'pinia'
import { projectApi } from '@/api/project'
import type { ProjectInfo, ProjectCreateParams } from '@/types/project'

interface ProjectState {
  currentProject: ProjectInfo | null
  projectList: ProjectInfo[]
  total: number
  loading: boolean
}

export const useProjectStore = defineStore('project', {
  state: (): ProjectState => ({
    currentProject: null,
    projectList: [],
    total: 0,
    loading: false,
  }),
  actions: {
    async fetchProjects(params: any) {
      this.loading = true
      try {
        const result = await projectApi.getList(params)
        this.projectList = result.records
        this.total = result.total
      } finally {
        this.loading = false
      }
    },
    async fetchProject(id: number) {
      this.currentProject = await projectApi.getById(id)
    },
    async createProject(data: ProjectCreateParams) {
      return projectApi.create(data)
    },
    async updateProject(id: number, data: any) {
      return projectApi.update(id, data)
    },
    async deleteProjects(ids: number[]) {
      return projectApi.deleteByIds(ids)
    },
  },
})
