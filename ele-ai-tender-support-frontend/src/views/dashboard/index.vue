<template>
  <div class="page-shell dashboard-page">
    <div class="page-header-row">
      <div>
        <div class="page-title">运营总览</div>
        <div class="page-subtitle">聚焦当前系统状态与常用操作入口</div>
      </div>
      <el-tag type="success" effect="dark" round>系统正常</el-tag>
    </div>

    <div class="stats-grid">
      <el-card v-for="item in statCards" :key="item.label" shadow="never" class="panel-card stat-card">
        <div class="stat-head">
          <div class="icon-box" :class="item.className">
            <el-icon :size="20"><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-meta">
            <div class="meta-label">{{ item.label }}</div>
            <div class="meta-value">{{ item.value }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <div class="bottom-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <span>快捷入口</span>
        </template>
        <div class="quick-grid">
          <button class="quick-tile" @click="$router.push('/system/user')">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </button>
          <button class="quick-tile" @click="$router.push('/system/role')">
            <el-icon><UserFilled /></el-icon>
            <span>角色管理</span>
          </button>
          <button class="quick-tile" @click="$router.push('/external')">
            <el-icon><Connection /></el-icon>
            <span>接入系统</span>
          </button>
          <button class="quick-tile" @click="$router.push('/version')">
            <el-icon><Files /></el-icon>
            <span>版本管理</span>
          </button>
        </div>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <span>系统信息</span>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="系统名称">EleAITender 支撑中心管理系统</el-descriptions-item>
          <el-descriptions-item label="系统版本">v1.0.0</el-descriptions-item>
          <el-descriptions-item label="后端服务">Spring Boot 3.2</el-descriptions-item>
          <el-descriptions-item label="前端框架">Vue 3 + Element Plus</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { User, UserFilled, Connection, Files } from '@element-plus/icons-vue'

const stats = reactive({
  userCount: 128,
  roleCount: 8,
  systemCount: 15,
  versionCount: 23,
})

const statCards = computed(() => [
  { label: '用户总数', value: stats.userCount, icon: User, className: 'users' },
  { label: '角色总数', value: stats.roleCount, icon: UserFilled, className: 'roles' },
  { label: '接入系统', value: stats.systemCount, icon: Connection, className: 'systems' },
  { label: '版本总数', value: stats.versionCount, icon: Files, className: 'versions' },
])
</script>

<style scoped lang="scss">
.dashboard-page {
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
    margin-bottom: 14px;
  }

  .stat-card {
    border: none;
  }

  .stat-head {
    display: flex;
    gap: 12px;
    align-items: center;
  }

  .icon-box {
    width: 42px;
    height: 42px;
    border-radius: 12px;
    display: grid;
    place-items: center;
    color: #fff;

    &.users {
      background: linear-gradient(130deg, #197b55, #0f8a5f);
    }

    &.roles {
      background: linear-gradient(130deg, #2f9068, #16806a);
    }

    &.systems {
      background: linear-gradient(130deg, #aa7a2d, #c17814);
    }

    &.versions {
      background: linear-gradient(130deg, #8d5f25, #a96f12);
    }
  }

  .meta-label {
    font-size: 12px;
    color: var(--et-text-weak);
  }

  .meta-value {
    margin-top: 2px;
    font-size: 28px;
    font-weight: 700;
    color: #173528;
  }

  .bottom-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;
  }

  .quick-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .quick-tile {
    border: 1px solid #d3e1da;
    border-radius: 12px;
    background: #f7fcf9;
    color: #1c392b;
    height: 54px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    cursor: pointer;
    transition: 0.2s ease;

    &:hover {
      border-color: #8fbca7;
      background: #eef8f3;
      transform: translateY(-1px);
    }
  }
}

@media (max-width: 1100px) {
  .dashboard-page {
    .stats-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .bottom-grid {
      grid-template-columns: 1fr;
    }
  }
}

@media (max-width: 640px) {
  .dashboard-page .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
