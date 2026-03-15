<script lang="ts" setup>
import { computed, ref } from 'vue';

import { useUserStore } from '@vben/stores';

import HospitalPanel from './components/HospitalPanel.vue';
import ProvincePanel from './components/ProvincePanel.vue';
import NationalPanel from './components/NationalPanel.vue';
import ExpertPanel from './components/ExpertPanel.vue';

const userStore = useUserStore();

// 角色类型定义
type RoleType = 'hospital' | 'province' | 'national' | 'expert';

// 角色配置
const roleConfig: Record<RoleType, { label: string; value: RoleType; roles: string[] }> = {
  hospital: {
    label: '医院',
    value: 'hospital',
    roles: ['hospital'], // 对应的系统角色标识
  },
  province: {
    label: '省级',
    value: 'province',
    roles: ['province', 'province_admin'],
  },
  national: {
    label: '国家局',
    value: 'national',
    roles: ['national', 'national_admin', 'super_admin'],
  },
  expert: {
    label: '专家',
    value: 'expert',
    roles: ['expert'],
  },
};

// 当前选中的角色类型
const currentRoleType = ref<RoleType>('hospital');

// 获取用户可用的角色列表
const availableRoles = computed(() => {
  const userRoles = userStore.userRoles || [];
  const available: RoleType[] = [];

  // 检查每个角色类型是否匹配用户的系统角色
  for (const [type, config] of Object.entries(roleConfig)) {
    const hasRole = config.roles.some((role) => userRoles.includes(role));
    if (hasRole) {
      available.push(type as RoleType);
    }
  }

  // 如果没有匹配的角色，默认显示医院面板
  if (available.length === 0) {
    available.push('hospital');
  }

  return available;
});

// 当前应该显示的面板组件
const currentPanel = computed(() => {
  switch (currentRoleType.value) {
    case 'hospital':
      return HospitalPanel;
    case 'province':
      return ProvincePanel;
    case 'national':
      return NationalPanel;
    case 'expert':
      return ExpertPanel;
    default:
      return HospitalPanel;
  }
});

// 初始化：根据用户角色自动选择面板
const initRole = () => {
  if (availableRoles.value.length > 0) {
    currentRoleType.value = availableRoles.value[0];
  }
};

// 角色切换
const handleRoleChange = (role: RoleType) => {
  currentRoleType.value = role;
};

// 页面加载时初始化
initRole();
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 角色切换标签 -->
    <div class="bg-white px-5 pt-4">
      <a-tabs v-model:activeKey="currentRoleType" @change="handleRoleChange">
        <a-tab-pane
          v-for="role in availableRoles"
          :key="role"
          :tab="roleConfig[role].label"
        />
      </a-tabs>
    </div>

    <!-- 动态面板内容 -->
    <component :is="currentPanel" :key="currentRoleType" />
  </div>
</template>
