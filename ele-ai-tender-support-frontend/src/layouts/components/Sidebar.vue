<template>
  <div class="sidebar-wrap">
    <div class="brand">
      <div class="brand-mark">ET</div>
      <div v-if="!isCollapse" class="brand-text">
        <div class="brand-name">EleAITender</div>
        <div class="brand-desc">Support Console</div>
      </div>
    </div>

    <el-menu
      :default-active="activeMenu"
      :collapse="isCollapse"
      :unique-opened="true"
      router
      class="nav-menu"
      @select="emit('nav-click')"
    >
      <el-menu-item index="/dashboard">
        <el-icon><DataAnalysis /></el-icon>
        <template #title>运营总览</template>
      </el-menu-item>

      <template v-for="menu in topLevelMenus" :key="menu.id">
        <el-sub-menu v-if="hasVisibleChildren(menu)" :index="resolveMenuPath(menu) || `menu-${menu.id}`">
          <template #title>
            <el-icon><Menu /></el-icon>
            <span>{{ menu.menuName }}</span>
          </template>

          <el-menu-item
            v-for="child in toVisibleRouteChildren(menu.children || [])"
            :key="child.id"
            :index="resolveMenuPath(child)"
          >
            <el-icon><Grid /></el-icon>
            <template #title>{{ child.menuName }}</template>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item v-else :index="resolveMenuPath(menu)">
          <el-icon><Grid /></el-icon>
          <template #title>{{ menu.menuName }}</template>
        </el-menu-item>
      </template>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { DataAnalysis, Menu, Grid } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import type { MenuInfo } from '@/types'
import { resolveMenuRoutePath } from '@/utils/menu-route'

defineProps<{
  isCollapse: boolean
}>()

const emit = defineEmits<{
  'nav-click': []
}>()

const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const topLevelMenus = computed(() => {
  const topMenus = toVisibleMenus(userStore.menus || [])
  return topMenus.filter(menu => hasVisibleChildren(menu) || !!resolveMenuPath(menu))
})

function resolveMenuPath(menu: MenuInfo) {
  return resolveMenuRoutePath(menu)
}

function toVisibleMenus(menuList: MenuInfo[]) {
  return (menuList || []).filter(item => item.status === 1 && item.visible !== 0 && item.menuType !== 2)
}

function toVisibleRouteChildren(menuList: MenuInfo[]) {
  return toVisibleMenus(menuList).filter(item => !!resolveMenuPath(item))
}

function hasVisibleChildren(menu: MenuInfo) {
  return toVisibleRouteChildren(menu.children || []).length > 0
}
</script>

<style scoped lang="scss">
.sidebar-wrap {
  height: 100%;
  background: linear-gradient(185deg, #123126 0, #163a2e 42%, #1f4a3b 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
}

.brand {
  height: 72px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: linear-gradient(135deg, #54c796 0, #2e9a72 100%);
  color: #fff;
  font-weight: 800;
  display: grid;
  place-items: center;
  letter-spacing: 0.5px;
}

.brand-name {
  color: #ecf8f2;
  font-weight: 700;
  line-height: 1.1;
}

.brand-desc {
  color: #a7cabc;
  font-size: 11px;
  margin-top: 2px;
}

.nav-menu {
  flex: 1;
  border-right: none;
  background: transparent;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    color: #cfe7dc;
    margin: 4px 10px;
    border-radius: 10px;
  }

  :deep(.el-menu-item:hover),
  :deep(.el-sub-menu__title:hover) {
    background: rgba(255, 255, 255, 0.08);
    color: #ffffff;
  }

  :deep(.el-menu-item.is-active) {
    background: linear-gradient(90deg, rgba(84, 199, 150, 0.35), rgba(84, 199, 150, 0.12));
    color: #f4fffb;
    font-weight: 600;
  }

  :deep(.el-menu--inline) {
    background: rgba(7, 27, 20, 0.28);
  }
}
</style>
