<template>
  <div class="markdown-editor">
    <MdEditor
      v-model="modelValue"
      :theme="'light'"
      :preview="true"
      :toolbarsExclude="['github']"
      @onChange="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

const modelValue = defineModel<string>({ default: '' })

const handleChange = (val: string) => {
  modelValue.value = val
}
</script>

<style scoped>
.markdown-editor {
  width: 100%;
  min-height: 400px;
}
</style>
