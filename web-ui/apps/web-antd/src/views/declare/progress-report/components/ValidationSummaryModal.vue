<template>
  <a-modal
    v-model:open="visible"
    title="请修正以下问题"
    :width="640"
    :footer="null"
    :maskClosable="false"
  >
    <!-- 错误统计 -->
    <div class="error-summary">
      <div class="total-count">
        共 <span class="count-number">{{ errors.length }}</span> 个问题
      </div>
      <div class="type-counts">
        <span
          v-for="(count, type) in errorCounts"
          :key="type"
          class="type-badge"
          :class="`type-badge--${type}`"
        >
          {{ typeLabelMap[type] || type }}: {{ count }}
        </span>
      </div>
    </div>

    <!-- 错误列表（按类型分组） -->
    <div class="validation-summary">
      <a-collapse v-model:activeKey="activeKeys" ghost>
        <a-collapse-panel
          v-for="(group, type) in groupedErrors"
          :key="type"
          :header="`${typeLabelMap[type] || type} (${group.length})`"
        >
          <div class="error-list">
            <div
              v-for="(error, index) in group"
              :key="getErrorKey(error, index)"
              class="error-item"
              :class="{ 'error-item--container': !!error.containerFieldKey }"
              @click="handleJump(error)"
            >
              <span class="error-icon">
                <IconifyIcon icon="lucide:alert-circle" />
              </span>
              <div class="error-content">
                <span v-if="error.indicatorCode" class="error-code">{{ error.indicatorCode }}</span>
                <span v-if="error.indicatorName" class="error-name">{{ error.indicatorName }}</span>
                <span class="error-message">{{ error.message }}</span>
                <span v-if="error.fieldLabel" class="error-field">字段: {{ error.fieldLabel }}</span>
              </div>
              <span class="jump-hint">
                <IconifyIcon icon="lucide:target" />
                定位
              </span>
            </div>
          </div>
        </a-collapse-panel>
      </a-collapse>

      <!-- 无错误时的提示 -->
      <div v-if="errors.length === 0" class="no-errors">
        <IconifyIcon icon="lucide:check-circle" class="no-errors-icon" />
        <span>所有校验已通过</span>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { IconifyIcon } from '@vben/icons';

interface ErrorItem {
  indicatorId?: number;
  indicatorCode: string;
  message: string;
  containerFieldKey?: string;
  errorType?: 'required' | 'format' | 'range' | 'logic' | 'joint';
  indicatorName?: string;
  fieldLabel?: string;
}

const visible = ref(false);
const errors = ref<ErrorItem[]>([]);
const activeKeys = ref<string[]>([]);

const typeLabelMap: Record<string, string> = {
  required: '必填错误',
  format: '格式错误',
  range: '范围错误',
  logic: '逻辑规则错误',
  joint: '联合规则错误',
};

/** 按错误类型分组 */
const groupedErrors = computed(() => {
  const groups: Record<string, ErrorItem[]> = {
    required: [],
    format: [],
    range: [],
    logic: [],
    joint: [],
  };

  for (const error of errors.value) {
    const type = error.errorType || 'format';
    if (groups[type]) {
      groups[type].push(error);
    } else {
      groups.format.push(error);
    }
  }

  // 过滤空分组
  return Object.fromEntries(
    Object.entries(groups).filter(([, list]) => list.length > 0),
  );
});

/** 错误统计 */
const errorCounts = computed(() => {
  const counts: Record<string, number> = {};
  for (const error of errors.value) {
    const type = error.errorType || 'format';
    counts[type] = (counts[type] || 0) + 1;
  }
  return counts;
});

/** 生成错误唯一 key */
function getErrorKey(error: ErrorItem, index: number): string {
  return error.containerFieldKey || `${error.indicatorId}-${index}`;
}

/** 显示错误弹窗 */
function show(errorList: ErrorItem[]) {
  errors.value = errorList;
  // 默认展开所有分组
  activeKeys.value = Object.keys(groupedErrors.value);
  visible.value = true;
}

/** 关闭弹窗 */
function close() {
  visible.value = false;
}

function handleJump(error: ErrorItem) {
  // 优先通过 containerFieldKey 定位（容器字段）
  if (error.containerFieldKey) {
    const fieldEl = document.querySelector(
      `[data-container-field-key="${error.containerFieldKey}"]`,
    );
    if (fieldEl) {
      const closestScroll = fieldEl.closest('.indicator-area') as HTMLElement | null;
      (closestScroll || fieldEl).scrollIntoView({ behavior: 'smooth', block: 'center' });
      addHighlight(fieldEl);
      closeModalAfterScroll();
      return;
    }
  }

  // 顶级指标或容器行统一通过 indicatorCode 定位（最稳定）
  const indicatorCode = error.indicatorCode;
  if (indicatorCode) {
    const rowEl = document.querySelector(
      `[data-indicator-code="${indicatorCode}"]`,
    );
    if (rowEl) {
      const closestScroll = rowEl.closest('.indicator-area') as HTMLElement | null;
      (closestScroll || rowEl).scrollIntoView({ behavior: 'smooth', block: 'center' });
      addHighlight(rowEl);
      closeModalAfterScroll();
      return;
    }
  }

  // 完全找不到对应元素时直接关闭弹窗
  visible.value = false;
}

/** 延迟关闭弹窗，确保滚动动画完成后再关闭 */
function closeModalAfterScroll() {
  setTimeout(() => {
    visible.value = false;
  }, 500);
}

function addHighlight(el: Element) {
  el.classList.add('indicator-highlight');
  setTimeout(() => {
    el.classList.remove('indicator-highlight');
  }, 2000);
}

defineExpose({ show, close });
</script>

<style scoped>
.error-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: hsl(var(--destructive) / 0.05);
  border-radius: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.total-count {
  font-size: 14px;
  color: hsl(var(--foreground));
}

.count-number {
  font-size: 18px;
  font-weight: 700;
  color: hsl(var(--destructive));
  margin: 0 4px;
}

.type-counts {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.type-badge {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.type-badge--required {
  background: hsl(var(--destructive) / 0.15);
  color: hsl(var(--destructive));
}

.type-badge--format {
  background: hsl(var(--warning) / 0.15);
  color: hsl(var(--warning));
}

.type-badge--range {
  background: hsl(var(--warning) / 0.15);
  color: hsl(var(--warning));
}

.type-badge--logic {
  background: hsl(var(--primary) / 0.15);
  color: hsl(var(--primary));
}

.type-badge--joint {
  background: hsl(var(--primary) / 0.15);
  color: hsl(var(--primary));
}

.validation-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 500px;
  overflow-y: auto;
  padding: 4px 0;
}

:deep(.ant-collapse-header) {
  padding: 10px 0 !important;
  font-weight: 600;
}

:deep(.ant-collapse-content-box) {
  padding: 0 !important;
}

.error-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 8px;
}

.error-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.error-item:hover {
  background: hsl(var(--destructive) / 0.05);
  border-color: hsl(var(--destructive) / 0.3);
  transform: translateX(2px);
}

.error-item--container {
  background: hsl(var(--primary) / 0.03);
  border-color: hsl(var(--primary) / 0.2);
}

.error-item--container:hover {
  background: hsl(var(--primary) / 0.06);
  border-color: hsl(var(--primary) / 0.4);
}

.error-icon {
  flex-shrink: 0;
  font-size: 16px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: hsl(var(--destructive) / 0.1);
  border-radius: 50%;
  color: hsl(var(--destructive));
}

.error-item--container .error-icon {
  background: hsl(var(--primary) / 0.1);
  color: hsl(var(--primary));
}

.error-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.error-code {
  font-size: 11px;
  color: hsl(var(--muted-foreground));
  font-weight: 600;
  font-family: monospace;
}

.error-name {
  font-size: 12px;
  color: hsl(var(--foreground));
  font-weight: 500;
}

.error-message {
  font-size: 13px;
  color: hsl(var(--destructive));
  font-weight: 500;
  line-height: 1.4;
}

.error-item--container .error-message {
  color: hsl(var(--foreground));
}

.error-field {
  font-size: 11px;
  color: hsl(var(--muted-foreground));
}

.jump-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  flex-shrink: 0;
  padding: 4px 10px;
  background: hsl(var(--border));
  border-radius: 6px;
  transition: all 0.2s;
}

.error-item:hover .jump-hint {
  background: hsl(var(--primary) / 0.1);
  color: hsl(var(--primary));
}

.no-errors {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px;
  color: hsl(var(--muted-foreground));
}

.no-errors-icon {
  font-size: 48px;
  color: hsl(var(--success));
}
</style>

<style>
/* 高亮闪烁动画 */
@keyframes indicator-flash {
  0%, 100% { background-color: transparent; }
  50% { background-color: rgba(239, 68, 68, 0.15); }
}

.indicator-highlight {
  animation: indicator-flash 0.5s ease-in-out 3;
}
</style>
