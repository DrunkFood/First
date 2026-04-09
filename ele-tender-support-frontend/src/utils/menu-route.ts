import type { MenuInfo } from '@/types'

const LEGACY_MENU_PATH_MAP: Record<string, string> = {
  '/system/access-system': '/external',
  '/external-systems': '/external',
  '/api/external-systems': '/external',
  '/support-api/external-systems': '/external',
  '/version/main': '/version',
  '/version/plugin': '/version',
  '/versions': '/version',
  '/api/versions': '/version',
  '/support-api/versions': '/version',
  '/system/access-log': '/system/access-log',
}

const MENU_CODE_ROUTE_MAP: Record<string, string> = {
  user: '/system/user',
  role: '/system/role',
  menu: '/system/menu',
  'access-system': '/external',
  'external-system': '/external',
  'access-log': '/system/access-log',
  version: '/version',
  'main-version': '/version',
  'plugin-version': '/version',
}

export function normalizeRoutePath(path?: string) {
  if (!path) return ''
  const normalized = path.startsWith('/') ? path : `/${path}`
  return normalized.replace(/\/+$/, '') || '/'
}

export function resolveMenuRoutePath(menu: MenuRouteSource) {
  const rawPath = normalizeRoutePath(menu.menuUrl || menu.path)
  if (rawPath && LEGACY_MENU_PATH_MAP[rawPath]) {
    return LEGACY_MENU_PATH_MAP[rawPath]
  }
  if (menu.menuCode && MENU_CODE_ROUTE_MAP[menu.menuCode]) {
    return MENU_CODE_ROUTE_MAP[menu.menuCode]
  }
  return rawPath
}
type MenuRouteSource = Partial<MenuInfo> & {
  menuCode?: string
}
