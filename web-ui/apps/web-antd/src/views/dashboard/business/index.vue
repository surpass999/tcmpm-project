<script lang="ts" setup>
import { computed, ref } from 'vue';
import { onMounted } from 'vue';

import {
  getDashboardStats,
  getDashboardTasks,
  getRiskWarnings,
  getFundStats,
  type DashboardStats,
  type DashboardTasks,
  type RiskWarnings,
  type FundStats,
} from '#/api/declare/dashboard';

import HospitalDashboard from './components/hospital/HospitalDashboard.vue';
import ProvinceDashboard from './components/province/ProvinceDashboard.vue';
import NationalDashboard from './components/national/NationalDashboard.vue';
import ExpertDashboard from './components/expert/ExpertDashboard.vue';

type RoleType = 'hospital' | 'province' | 'nation' | 'expert';

const roleConfig: Record<RoleType, { label: string; title: string }> = {
  hospital: { label: '医院', title: '医院驾驶舱' },
  province: { label: '省级', title: '省级驾驶舱' },
  nation: { label: '国家局', title: '国家局驾驶舱' },
  expert: { label: '专家', title: '专家驾驶舱' },
};

const loading = ref(false);
const currentRole = ref<RoleType>('hospital');
const stats = ref<DashboardStats | null>(null);
const tasks = ref<DashboardTasks | null>(null);
const warnings = ref<RiskWarnings | null>(null);
const fundStats = ref<FundStats | null>(null);

const currentPanel = computed(() => {
  switch (currentRole.value) {
    case 'hospital': return HospitalDashboard;
    case 'province': return ProvinceDashboard;
  case 'nation': return NationalDashboard;
    case 'expert': return ExpertDashboard;
    default: return HospitalDashboard;
  }
});

const roleTitle = computed(() => roleConfig[currentRole.value]?.title || '业务控制台');

async function loadData() {
  loading.value = true;
  try {
    const [statsData, tasksData, warningsData, fundData] = await Promise.all([
      getDashboardStats(),
      getDashboardTasks(),
      getRiskWarnings(),
      getFundStats(),
    ]);
    stats.value = statsData;
    tasks.value = tasksData;
    warnings.value = warningsData;
    fundStats.value = fundData;

    // 后端返回 userRole，以此为准决定展示哪个驾驶舱
    console.log('[Dashboard] 后端返回 statsData:', statsData);
    console.log('[Dashboard] userRole:', statsData?.userRole);
    if (
      statsData?.userRole
      && ['hospital', 'province', 'nation', 'expert'].includes(statsData.userRole)
    ) {
      currentRole.value = statsData.userRole as RoleType;
      console.log('[Dashboard] 设置 currentRole:', currentRole.value);
    } else {
      console.warn('[Dashboard] userRole 未匹配，userRole=', statsData?.userRole, '，保持 currentRole=', currentRole.value);
    }
  } catch (error) {
    console.error('加载驾驶舱数据失败:', error);
  } finally {
    loading.value = false;
  }
}

async function handleRefresh() {
  await loadData();
}

function goToBpmTasks() {
  window.location.href = '/bpm/task/todo-page';
}

function goToProjectList(params?: Record<string, string>) {
  const query = params ? '?' + new URLSearchParams(params).toString() : '';
  window.location.href = `/project${query}`;
}

function goToWarningProjects() {
  goToProjectList({ warning: '1' });
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <div class="business-console" v-loading="loading">
    <div class="console-header">
      <div class="welcome">
        <h2>{{ roleTitle }}</h2>
        <span class="role-badge">{{ roleConfig[currentRole]?.label }}</span>
        <span class="date">{{ new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) }}</span>
      </div>
      <div class="header-actions">
        <a-button @click="handleRefresh" :loading="loading">刷新</a-button>
      </div>
    </div>

    <div class="console-content">
      <component
        :is="currentPanel"
        :stats="stats"
        :tasks="tasks"
        :warnings="warnings"
        :fundStats="fundStats"
        @go-to-bpm-tasks="goToBpmTasks"
        @go-to-project-list="goToProjectList"
        @go-to-warning-projects="goToWarningProjects"
      />
    </div>
  </div>
</template>

<style scoped>
.business-console {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.console-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.welcome {
  display: flex;
  align-items: center;
  gap: 12px;
}

.welcome h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.role-badge {
  padding: 2px 10px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  border-radius: 12px;
}

.welcome .date {
  color: #909399;
  font-size: 14px;
  padding-left: 12px;
  border-left: 1px solid #dcdfe6;
}

.console-content {
  min-height: 400px;
}
</style>
