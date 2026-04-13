import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'
import MainLayout from '@/layouts/MainLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'project',
        name: 'ProjectList',
        component: () => import('@/views/project/ProjectList.vue'),
        meta: { title: '项目管理' },
      },
      {
        path: 'project/create',
        name: 'ProjectCreate',
        component: () => import('@/views/project/ProjectCreate.vue'),
        meta: { title: '新建项目' },
      },
      {
        path: 'project/:id',
        name: 'ProjectDetail',
        component: () => import('@/views/project/ProjectDetail.vue'),
        meta: { title: '项目详情' },
      },
      {
        path: 'requirement',
        name: 'RequirementList',
        component: () => import('@/views/requirement/RequirementList.vue'),
        meta: { title: '需求编制' },
      },
      {
        path: 'requirement/create',
        name: 'RequirementCreate',
        component: () => import('@/views/requirement/RequirementCreate.vue'),
        meta: { title: '新建需求' },
      },
      {
        path: 'requirement/edit/:id',
        name: 'RequirementEditor',
        component: () => import('@/views/requirement/RequirementEditor.vue'),
        meta: { title: '编辑需求' },
      },
      {
        path: 'review/:projectId',
        name: 'ReviewEditor',
        component: () => import('@/views/review/ReviewEditor.vue'),
        meta: { title: '评审项管理' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  if (to.path !== '/login' && !isLoggedIn()) {
    next('/login')
  } else {
    next()
  }
})

export default router
