<template>
  <el-alert
    v-if="visible"
    title="AI服务暂时不可用"
    type="warning"
    :closable="false"
    show-icon
    class="ai-unavailable-alert"
  >
    <template #default>
      <p class="alert-desc">AI服务暂时不可用，您可以选择重试或跳过此步骤进行手动操作。</p>
      <div class="alert-actions">
        <el-button size="small" type="primary" @click="$emit('retry')">
          重试
        </el-button>
        <el-button size="small" @click="$emit('skip')">
          跳过，手动操作
        </el-button>
      </div>
    </template>
  </el-alert>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean
}>()

defineEmits<{
  retry: []
  skip: []
}>()
</script>

<style scoped>
.ai-unavailable-alert {
  margin-bottom: 16px;
}

.alert-desc {
  margin: 4px 0 12px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.alert-actions {
  display: flex;
  gap: 8px;
}
</style>
