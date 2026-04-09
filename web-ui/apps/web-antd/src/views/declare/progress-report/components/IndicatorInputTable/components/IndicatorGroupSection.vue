<script setup lang="ts">
/**
 * 分组区块组件
 * 负责渲染指标分组（一级/二级分组 + 指标列表）
 */

import type { IndicatorGroup } from '../types';

interface Props {
  group: IndicatorGroup;
}

defineProps<Props>();
</script>

<template>
  <div class="indicator-category-section">
    <!-- 分组标题 -->
    <div
      class="category-title"
      :class="{
        'category-title--level2': group.groupLevel === 2,
      }"
    >
      {{ group.groupPrefix ? group.groupPrefix + ' ' : '' }}{{ group.groupName }}
    </div>

    <!-- 指标列表 -->
    <div class="indicator-list">
      <slot
        v-for="indicator in group.indicators"
        :key="indicator.id"
        :indicator="indicator"
        name="indicator"
      />
    </div>

    <!-- 二级分组（递归） -->
    <template v-for="child in group.children" :key="child.groupId">
      <div class="indicator-category-section">
        <div class="category-title category-title--level2">
          {{ child.groupPrefix ? child.groupPrefix + ' ' : '' }}{{ child.groupName }}
        </div>
        <div class="indicator-list">
          <slot
            v-for="indicator in child.indicators"
            :key="indicator.id"
            :indicator="indicator"
            name="indicator"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.indicator-category-section {
  margin-bottom: 32px;
}

.indicator-category-section:last-child {
  margin-bottom: 0;
}

.category-title {
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--foreground));
  margin-bottom: 14px;
  padding-bottom: 10px;
  padding-left: 12px;
  border-bottom: 2px solid hsl(var(--primary));
  border-left: 4px solid hsl(var(--primary));
  letter-spacing: 0.3px;
}

.category-title--level2 {
  font-size: 14px;
  color: hsl(var(--muted-foreground));
  border-bottom-color: hsl(var(--primary-light) / 0.5);
  border-left-color: hsl(var(--primary-light));
}

.indicator-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
