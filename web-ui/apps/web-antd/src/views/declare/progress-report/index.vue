<script setup lang="ts">
import { computed } from 'vue';
import { useUserStore } from '@vben/stores';

import HospitalView from './views/hospital.vue';
import ProvinceView from './views/province.vue';
import NationalView from './views/national.vue';

const userStore = useUserStore();

const currentRole = computed(() => {
  const roles = userStore.userRoles || [];
  if (roles.includes('PROVINCE')) return 'province';
  if (roles.includes('NATION')) return 'national';
  return 'hospital';
});

const currentView = computed(() => {
  switch (currentRole.value) {
    case 'province': return ProvinceView;
    case 'national': return NationalView;
    default: return HospitalView;
  }
});
</script>

<template>
  <component :is="currentView" />
</template>
