import request from '@/utils/request'

export const projectTemplateApi = {
  /** 项目绑定模板 */
  bind(projectId: number, supTemplateId: number) {
    return request.post('/core-api/v1/project-templates/bind', { projectId, supTemplateId })
  },

  /** 获取项目模板 */
  getByProject(projectId: number) {
    return request.get(`/core-api/v1/project-templates/project/${projectId}`)
  },
}
