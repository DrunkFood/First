<template>
  <div class="qualification-list">
    <div
      v-for="(item, index) in items"
      :key="index"
      class="qualification-item"
    >
      <span class="item-index">{{ index + 1 }}</span>
      <el-input
        :model-value="item"
        @update:model-value="updateItem(index, $event)"
        placeholder="请输入资格条件"
        class="item-input"
      />
      <el-button
        :icon="Delete"
        circle
        size="small"
        type="danger"
        :disabled="items.length <= minItems"
        @click="removeItem(index)"
      />
    </div>
    <el-button
      type="primary"
      :icon="Plus"
      @click="addItem"
      class="add-btn"
    >
      添加资格条件
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  modelValue: string[]
  minItems?: number
}>(), {
  minItems: 1,
})

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const items = ref<string[]>([...props.modelValue])

watch(() => props.modelValue, (val) => {
  items.value = [...val]
}, { deep: true })

function updateItem(index: number, value: string) {
  const newItems = [...items.value]
  newItems[index] = value
  items.value = newItems
  emit('update:modelValue', newItems)
}

function addItem() {
  const newItems = [...items.value, '']
  items.value = newItems
  emit('update:modelValue', newItems)
}

function removeItem(index: number) {
  if (items.value.length <= props.minItems) return
  const newItems = items.value.filter((_, i) => i !== index)
  items.value = newItems
  emit('update:modelValue', newItems)
}
</script>

<style scoped>
.qualification-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.qualification-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-index {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--app-brand-color);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.item-input {
  flex: 1;
}

.add-btn {
  align-self: flex-start;
  margin-top: 4px;
}
</style>
