import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

type ThemeMode = 'dark' | 'light'

export const useThemeStore = defineStore('theme', () => {
  const STORAGE_KEY = 'app-theme'

  const mode = ref<ThemeMode>(
    (localStorage.getItem(STORAGE_KEY) as ThemeMode) || 'dark'
  )

  function setMode(newMode: ThemeMode) {
    mode.value = newMode
  }

  function toggle() {
    mode.value = mode.value === 'dark' ? 'light' : 'dark'
  }

  watch(mode, (val) => {
    document.documentElement.setAttribute('data-theme', val)
    localStorage.setItem(STORAGE_KEY, val)
  }, { immediate: true })

  return { mode, setMode, toggle }
})
