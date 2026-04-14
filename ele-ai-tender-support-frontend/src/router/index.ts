import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'
import type { MenuInfo } from '@/types'
import { normalizeRoutePath, resolveMenuRoutePath } from '@/utils/menu-route'

// 静态路由
const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' },
      },
      // 系统管理
      {
        path: 'system/user',
        name: 'UserManagement',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' },
      },
      {
        path: 'system/role',
        name: 'RoleManagement',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'UserFilled' },
      },
      {
        path: 'system/menu',
        name: 'MenuManagement',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'Menu' },
      },
      {
        path: 'system/access-log',
        name: 'AccessLogManagement',
        component: () => import('@/views/system/access-log/index.vue'),
        meta: { title: '访问日志', icon: 'Document' },
      },
      // 接入系统管理
      {
        path: 'external',
        name: 'ExternalSystem',
        component: () => import('@/views/external/index.vue'),
        meta: { title: '接入系统管理', icon: 'Connection' },
      },
      // 版本管理
      {
        path: 'version',
        name: 'VersionManagement',
        component: () => import('@/views/version/index.vue'),
        meta: { title: '版本管理', icon: 'Files' },
      },
      {
        path: 'version/:id/plugins',
        name: 'PluginManagement',
        component: () => import('@/views/version/plugins.vue'),
        meta: { title: '插件管理', icon: 'Coordinate' },
      },
      // AI编制支撑管理
      {
        path: 'template',
        name: 'TemplateManagement',
        component: () => import('@/views/template/TemplateList.vue'),
        meta: { title: '模板管理', icon: 'Document' },
      },
      {
        path: 'knowledge',
        name: 'KnowledgeManagement',
        component: () => import('@/views/knowledge/KnowledgeList.vue'),
        meta: { title: '知识库管理', icon: 'Collection' },
      },
      {
        path: 'model-config',
        name: 'ModelConfigManagement',
        component: () => import('@/views/model-config/ModelConfigList.vue'),
        meta: { title: 'AI模型配置', icon: 'Setting' },
      },
      // Phase1 新增页面
      {
        path: 'system/config',
        name: 'SysParamManagement',
        component: () => import('@/views/system/sys-param/index.vue'),
        meta: { title: '系统参数', icon: 'Setting' },
      },
      {
        path: 'system/operation-log',
        name: 'OperationLogManagement',
        component: () => import('@/views/system/operation-log/index.vue'),
        meta: { title: '操作日志', icon: 'Notebook' },
      },
      {
        path: 'policy-file',
        name: 'PolicyFileManagement',
        component: () => import('@/views/policy-file/index.vue'),
        meta: { title: '政策文件', icon: 'FolderOpened' },
      },
      {
        path: 'message',
        name: 'MessageCenter',
        component: () => import('@/views/message/index.vue'),
        meta: { title: '消息中心', icon: 'Bell' },
      },
      {
        path: 'statistics',
        name: 'StatisticsOverview',
        component: () => import('@/views/statistics/index.vue'),
        meta: { title: '统计分析', icon: 'DataAnalysis' },
      },
      {
        path: 'model-route',
        name: 'ModelRouteManagement',
        component: () => import('@/views/model-route/index.vue'),
        meta: { title: '模型路由', icon: 'Share' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/auth/NotFound.vue'),
    meta: { title: '404', requiresAuth: false },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: staticRoutes,
})

const WHITE_LIST_PATHS = new Set(['/login', '/404'])

function collectAllowedPaths(menuList: MenuInfo[]): Set<string> {
  const allowed = new Set<string>()
  const collect = (items: MenuInfo[]) => {
    for (const item of items || []) {
      if (item.status === 1 && item.visible !== 0 && item.menuType !== 2) {
        const routePath = resolveMenuRoutePath(item)
        if (routePath) {
          allowed.add(routePath)
        }
      }
      if (item.children && item.children.length > 0) {
        collect(item.children)
      }
    }
  }
  collect(menuList)
  return allowed
}

function isPathAuthorized(path: string, allowedPaths: Set<string>) {
  const normalizedPath = normalizeRoutePath(path)
  if (normalizedPath === '/' || normalizedPath === '/dashboard') {
    return true
  }
  for (const allowedPath of allowedPaths) {
    if (normalizedPath === allowedPath || normalizedPath.startsWith(`${allowedPath}/`)) {
      return true
    }
  }
  return false
}

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  // 设置页面标题
  document.title = `${to.meta.title || ''} - EleAITender支撑中心`
  
  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth !== false
  
  if (requiresAuth) {
    if (!userStore.isLoggedIn) {
      next({ path: '/login', query: { redirect: to.fullPath } })
    } else {
      // 如果没有菜单数据，获取用户菜单
      if (userStore.menus.length === 0) {
        try {
          await userStore.getUserMenus()
        } catch {
          userStore.logout()
          next('/login')
          return
        }
      }
      const allowedPaths = collectAllowedPaths(userStore.menus)
      if (!isPathAuthorized(to.path, allowedPaths)) {
        next('/dashboard')
        return
      }
      next()
    }
  } else {
    if (WHITE_LIST_PATHS.has(to.path)) {
      next()
      return
    }
    // 已登录用户访问登录页，重定向到首页
    if (to.path === '/login' && userStore.isLoggedIn) {
      next('/')
    } else {
      next()
    }
  }
})

export default router
