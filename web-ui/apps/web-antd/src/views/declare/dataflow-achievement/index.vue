<script lang="ts" setup>
import { computed, defineAsyncComponent, ref } from 'vue';

import { Page } from '@vben/common-ui';

// 当前视图类型
type ViewType = 'dataflow' | 'achievement';
const activeView = ref<ViewType>('dataflow');

// 动态导入组件
const DataFlowView = defineAsyncComponent(() => import('./DataFlowView.vue'));
const AchievementView = defineAsyncComponent(() => import('./AchievementView.vue'));

// 根据当前视图获取对应的组件
const currentComponent = computed(() => {
  return activeView.value === 'dataflow' ? DataFlowView : AchievementView;
});

// 视图切换时传递刷新方法
function handleViewChange() {
  // 强制重新渲染当前组件
}
</script>

<template>
  <Page auto-content-height>
    <!-- 视图切换 -->
    <div class="mb-4 flex items-center gap-4">
      <span class="text-base font-medium">选择视图：</span>
      <a-radio-group v-model:value="activeView" button-style="solid">
        <a-radio-button value="dataflow">数据流通</a-radio-button>
        <a-radio-button value="achievement">成果转化</a-radio-button>
      </a-radio-group>
    </div>

    <!-- 动态组件：只渲染当前选中的视图 -->
    <component :is="currentComponent" :key="activeView" />
  </Page>
</template>
