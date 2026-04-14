import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import Login from '@/views/auth/Login.vue'
import MainLayout from '@/layouts/MainLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录', guest: true },
  },
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
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
      {
        path: 'project/:id/wizard',
        name: 'ProjectWizard',
        component: () => import('@/views/project/ProjectWizard.vue'),
        meta: { title: '项目编制' },
      },
      {
        path: 'policy-file',
        name: 'PolicyFileList',
        component: () => import('@/views/policy/PolicyFileList.vue'),
        meta: { title: '政策文件管理' },
      },
      {
        path: 'message',
        name: 'MessageCenter',
        component: () => import('@/views/message/MessageCenter.vue'),
        meta: { title: '消息中心' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  // 直接从 localStorage 读取 token，避免 Pinia 时序问题
  const token = localStorage.getItem('token')

  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - AI招标文件编制` : 'AI招标文件编制'

  // 如果访问登录页且已登录，重定向到首页
  if (to.meta.guest && token) {
    next('/')
    return
  }

  // 如果路由需要认证但未登录，重定向到登录页
  if (to.meta.requiresAuth && !token) {
    next({
      path: '/login',
      query: { redirect: to.fullPath },
    })
    return
  }

  next()
})

export default router
