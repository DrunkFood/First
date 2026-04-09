<template>
  <div class="layout-root" :class="{ 'mobile-open': mobileMenuOpen }">
    <aside class="layout-sidebar" :class="{ collapsed: isCollapse }">
      <Sidebar :is-collapse="isCollapse" @nav-click="closeMobileMenu" />
    </aside>

    <div class="layout-main">
      <header class="layout-header">
        <Header
          :is-collapse="isCollapse"
          :is-mobile-open="mobileMenuOpen"
          @toggle-collapse="toggleCollapse"
          @toggle-mobile="toggleMobile"
        />
      </header>

      <main class="layout-content" @click="closeMobileMenu">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <div v-if="mobileMenuOpen" class="mobile-mask" @click="closeMobileMenu" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'

const isCollapse = ref(false)
const mobileMenuOpen = ref(false)

const toggleCollapse = () => {
  if (window.innerWidth <= 960) {
    mobileMenuOpen.value = !mobileMenuOpen.value
    return
  }
  isCollapse.value = !isCollapse.value
}

const toggleMobile = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

const closeMobileMenu = () => {
  if (window.innerWidth <= 960) {
    mobileMenuOpen.value = false
  }
}
</script>

<style scoped lang="scss">
.layout-root {
  min-height: 100vh;
  display: flex;
  background: transparent;
}

.layout-sidebar {
  width: 248px;
  transition: width 0.24s ease;
  z-index: 22;
}

.layout-sidebar.collapsed {
  width: 78px;
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.layout-header {
  height: 72px;
  padding: 14px 18px 0;
}

.layout-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}

.mobile-mask {
  display: none;
}

@media (max-width: 960px) {
  .layout-sidebar {
    position: fixed;
    left: -260px;
    top: 0;
    bottom: 0;
    width: 240px;
    transition: left 0.24s ease;
  }

  .layout-sidebar.collapsed {
    width: 240px;
  }

  .layout-root.mobile-open .layout-sidebar {
    left: 0;
  }

  .layout-header {
    height: 64px;
    padding: 10px 12px 0;
  }

  .mobile-mask {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.28);
    z-index: 21;
  }
}
</style>
