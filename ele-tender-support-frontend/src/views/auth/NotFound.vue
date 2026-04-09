<template>
  <div class="not-found">
    <div class="card panel-card">
      <div class="code">404</div>
      <div class="title">页面不存在</div>
      <div class="desc">你访问的地址已失效或没有权限访问</div>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped lang="scss">
.not-found {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.card {
  width: min(460px, 100%);
  text-align: center;
  padding: 44px 28px;
}

.code {
  font-size: 92px;
  line-height: 1;
  font-weight: 800;
  color: #0f8a5f;
}

.title {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: #153526;
}

.desc {
  margin: 10px 0 24px;
  color: #678377;
}
</style>
