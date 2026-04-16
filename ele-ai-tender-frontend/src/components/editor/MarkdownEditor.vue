<template>
  <div class="markdown-editor">
    <MdEditor
      v-model="modelValue"
      :theme="themeStore.mode"
      :preview="preview"
      :disabled="disabled"
      :toolbarsExclude="toolbarsExclude"
      @onChange="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount } from 'vue'
import { MdEditor, type ToolbarNames } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { useThemeStore } from '@/store/theme'

const modelValue = defineModel<string>({ default: '' })

withDefaults(defineProps<{
  preview?: boolean
  disabled?: boolean
  toolbarsExclude?: ToolbarNames[]
}>(), {
  preview: true,
  disabled: false,
  toolbarsExclude: () => ['github'] as ToolbarNames[],
})

const themeStore = useThemeStore()

const handleChange = (val: string) => {
  modelValue.value = val
}

// 销毁前清空内容，防止md-editor-v3内部MutationObserver在DOM移除后报错
onBeforeUnmount(() => {
  modelValue.value = ''
})
</script>

<style scoped>
.markdown-editor {
  width: 100%;
  min-height: 400px;
}
</style>
