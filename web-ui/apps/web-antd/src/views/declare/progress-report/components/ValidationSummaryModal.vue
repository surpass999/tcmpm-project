<template>
  <a-modal
    v-model:open="visible"
    title="请修正以下问题"
    :width="560"
    :footer="null"
    :maskClosable="false"
  >
    <div class="validation-summary">
      <div
        v-for="(error, index) in errors"
        :key="`${error.indicatorId}-${index}`"
        class="error-item"
        @click="handleJump(error)"
      >
        <span class="error-icon">❌</span>
        <span class="error-message">{{ error.message }}</span>
        <span class="jump-hint">点击定位</span>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue';

interface ErrorItem {
  indicatorId: number;
  message: string;
}

const visible = ref(false);
const errors = ref<ErrorItem[]>([]);

function show(errorList: ErrorItem[]) {
  errors.value = errorList;
  visible.value = true;
}

function handleJump(error: ErrorItem) {
  const el = document.querySelector(`[data-indicator-id="${error.indicatorId}"]`);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    // 高亮闪烁效果
    el.classList.add('indicator-highlight');
    setTimeout(() => el.classList.remove('indicator-highlight'), 1500);
  }
  visible.value = false;
}

defineExpose({ show });
</script>

<style scoped>
.validation-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
  padding: 4px 0;
}

.error-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: hsl(var(--destructive) / 0.04);
  border: 1px solid hsl(var(--destructive) / 0.15);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, transform 0.15s;
}

.error-item:hover {
  background: hsl(var(--destructive) / 0.08);
  border-color: hsl(var(--destructive) / 0.3);
  transform: translateX(2px);
}

.error-icon {
  flex-shrink: 0;
  font-size: 14px;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: hsl(var(--destructive) / 0.1);
  border-radius: 50%;
}

.error-message {
  flex: 1;
  font-size: 13px;
  color: hsl(var(--destructive));
  font-weight: 500;
  line-height: 1.4;
}

.jump-hint {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  flex-shrink: 0;
  padding: 2px 8px;
  background: hsl(var(--border));
  border-radius: 4px;
  transition: background 0.2s;
}

.error-item:hover .jump-hint {
  background: hsl(var(--destructive) / 0.1);
  color: hsl(var(--destructive));
}
</style>

<style>
/* 高亮闪烁动画（全局） */
@keyframes indicator-flash {
  0%, 100% { background-color: transparent; }
  50% { background-color: rgba(255, 77, 79, 0.15); }
}

.indicator-highlight {
  animation: indicator-flash 0.5s ease-in-out 3;
}
</style>
