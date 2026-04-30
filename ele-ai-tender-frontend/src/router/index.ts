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
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      // 首页
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页' },
      },
      // 业务需求管理
      {
        path: 'requirement',
        name: 'RequirementList',
        component: () => import('@/views/requirement/RequirementList.vue'),
        meta: { title: '业务需求列表' },
      },
      {
        path: 'requirement/create',
        name: 'RequirementCreate',
        component: () => import('@/views/requirement/RequirementCreate.vue'),
        meta: { title: '新建业务需求' },
      },
      {
        path: 'requirement/edit/:id',
        name: 'RequirementEdit',
        component: () => import('@/views/requirement/RequirementEdit.vue'),
        meta: { title: '编辑业务需求' },
      },
      {
        path: 'requirement/editor/:id',
        name: 'RequirementEditor',
        component: () => import('@/views/requirement/RequirementEditor.vue'),
        meta: { title: '内容编辑' },
      },
      {
        path: 'requirement/generate/:id',
        name: 'RequirementGenerate',
        component: () => import('@/views/requirement/RequirementGenerate.vue'),
        meta: { title: 'AI生成需求' },
      },
      {
        path: 'requirement/detect/:id',
        name: 'RequirementDetect',
        component: () => import('@/views/requirement/RequirementDetect.vue'),
        meta: { title: '需求智能检测' },
      },
      // 招标文件管理
      {
        path: 'project',
        name: 'ProjectList',
        component: () => import('@/views/project/ProjectList.vue'),
        meta: { title: '项目列表' },
      },
      {
        path: 'project/create',
        name: 'ProjectCreate',
        component: () => import('@/views/project/ProjectCreate.vue'),
        meta: { title: '新建项目' },
      },
      {
        path: 'project/edit/:id',
        name: 'ProjectEdit',
        component: () => import('@/views/project/ProjectCreate.vue'),
        meta: { title: '编辑项目' },
      },
      {
        path: 'project/:id',
        name: 'ProjectDetail',
        component: () => import('@/views/project/ProjectDetail.vue'),
        meta: { title: '项目详情' },
      },
      {
        path: 'project/:id/wizard',
        name: 'ProjectWizard',
        component: () => import('@/views/project/ProjectWizard.vue'),
        meta: { title: '项目编制' },
      },
      // 政策文件管理
      {
        path: 'policy-file',
        name: 'PolicyFileList',
        component: () => import('@/views/policy/PolicyFileList.vue'),
        meta: { title: '政策文件管理' },
      },
      // 消息中心
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
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  document.title = to.meta.title ? `${to.meta.title} - AI招标文件编制` : 'AI招标文件编制'
  if (to.meta.guest && token) {
    next('/dashboard')
    return
  }
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  next()
})

export default router
