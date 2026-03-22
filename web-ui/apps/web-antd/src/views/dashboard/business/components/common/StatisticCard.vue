<script setup lang="ts">
import { IconifyIcon } from '@vben/icons';

defineProps<{
  title: string;
  value: string | number;
  /** Iconify 图标名，如 ant-design:file-text-outlined */
  icon?: string;
  color?: string;
  suffix?: string;
  loading?: boolean;
  clickable?: boolean;
}>();

const emit = defineEmits<{
  click: [];
}>();

function handleClick() {
  emit('click');
}
</script>

<template>
  <div
    class="statistic-card"
    :class="{ clickable }"
    @click="clickable ? handleClick() : null"
  >
    <div class="card-icon" :style="{ backgroundColor: color || '#409eff' }">
      <IconifyIcon v-if="icon" :icon="icon" class="card-icon-svg" />
      <span v-else class="card-icon-fallback">📊</span>
    </div>
    <div class="card-content">
      <div class="card-value" v-if="loading">
        <span class="loading-spinner"></span>
      </div>
      <div class="card-value" v-else>
        <span>{{ value }}</span>
        <span v-if="suffix" class="suffix">{{ suffix }}</span>
      </div>
      <div class="card-title">{{ title }}</div>
    </div>
  </div>
</template>

<style scoped>
.statistic-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.statistic-card.clickable {
  cursor: pointer;
}

.statistic-card.clickable:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.card-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 28px;
  color: #fff;
  flex-shrink: 0;
}

.card-icon-svg {
  font-size: 28px;
  color: #fff;
}

.card-icon-fallback {
  font-size: 26px;
  line-height: 1;
}

.card-content {
  flex: 1;
  min-width: 0;
}

.card-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.card-value .suffix {
  font-size: 16px;
  font-weight: normal;
  color: #909399;
  margin-left: 4px;
}

.card-title {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.loading-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid #e4e7ed;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
