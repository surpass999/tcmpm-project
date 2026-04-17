<script setup lang="ts">
/**
 * IndicatorContainer - 容器封装组件
 *
 * VBenForm schema 中的字段表现形式：一个 fieldName → JSON string
 * 内部管理三种容器类型的条目渲染
 */
import { computed, ref, watch } from 'vue';
import { Button, message } from 'ant-design-vue';
import { PlusOutlined } from '@vben/icons';
import type { DynamicField, ContainerType } from '../../types';
import ContainerFieldRenderer from './container-fields/ContainerFieldRenderer.vue';

interface Entry { rowKey: string; [key: string]: any }

interface ContainerEntryLastValue { rowKey: string; fieldValues: Record<string, string>; }

interface Props {
  value?: string | null;
  valueOptions?: string;
  disabled?: boolean;
  lastPeriodValues?: ContainerEntryLastValue[];
  indicatorCode?: string;
}

const props = withDefaults(defineProps<Props>(), {
  value: null,
  valueOptions: '',
  disabled: false,
  lastPeriodValues: () => [],
  indicatorCode: '',
});

const emit = defineEmits<{
  'update:value': [value: string];
  change: [value: string];
}>();

const MAX_ENTRIES = 99;

const containerType = computed<ContainerType>(() => {
  if (!props.valueOptions) return 'normal';
  try {
    const parsed = JSON.parse(props.valueOptions);
    if (Array.isArray(parsed)) return 'normal';
    return (parsed.mode as ContainerType) || 'normal';
  } catch { return 'normal'; }
});

const fields = computed<DynamicField[]>(() => {
  if (!props.valueOptions) return [];
  try {
    const parsed = JSON.parse(props.valueOptions);
    if (Array.isArray(parsed)) return parsed;
    return parsed.fields || [];
  } catch { return []; }
});

/** 内部条目状态 */
const entries = ref<Entry[]>([]);

/** 从外部 value（JSON 字符串）初始化条目 */
watch(
  () => props.value,
  (val) => {
    if (!val) { entries.value = [makeEntry(1)]; return; }
    try {
      const parsed = JSON.parse(val);
      if (Array.isArray(parsed) && parsed.length > 0) {
        entries.value = parsed;
      } else {
        entries.value = [makeEntry(1)];
      }
    } catch { entries.value = [makeEntry(1)]; }
  },
  { immediate: true },
);

function makeEntry(index: number): Entry {
  const rowKey = `${props.indicatorCode || 'C'}${String(index).padStart(2, '0')}`;
  return { rowKey };
}

function makeNextEntry(): Entry | null {
  const nextIndex = entries.value.length + 1;
  if (nextIndex > MAX_ENTRIES) { message.warning(`容器条目数量已达到上限（${MAX_ENTRIES}）`); return null; }
  const rowKey = `${props.indicatorCode || 'C'}${String(nextIndex).padStart(2, '0')}`;
  return { rowKey };
}

function syncToParent() {
  const val = JSON.stringify(entries.value);
  emit('update:value', val);
  emit('change', val);
}

function handleAddEntry() {
  if (containerType.value !== 'normal') return;
  const newEntry = makeNextEntry();
  if (!newEntry) return;
  entries.value.push(newEntry);
  syncToParent();
}

function handleRemoveEntry(index: number) {
  if (containerType.value !== 'normal') return;
  if (entries.value.length === 1 && props.indicatorCode) {
    message.warning('至少需要保留一条记录');
    return;
  }
  entries.value.splice(index, 1);
  renumberEntries();
  syncToParent();
}

function renumberEntries() {
  entries.value.forEach((entry, idx) => {
    entry.rowKey = `${props.indicatorCode || 'C'}${String(idx + 1).padStart(2, '0')}`;
  });
}

function handleFieldUpdate(entryIndex: number, _field: DynamicField) {
  syncToParent();
}

function handleFieldBlur(entryIndex: number, _field: DynamicField) {
  syncToParent();
}
</script>

<template>
  <div class="indicator-container" :class="`container-${containerType}`">
    <!-- 条件容器 / 自动条目容器 -->
    <template v-if="containerType !== 'normal'">
      <div v-for="(entry, entryIdx) in entries" :key="entry.rowKey" class="dynamic-entry-row mb-4">
        <div class="entry-header" v-if="containerType === 'autoEntry'">
          <span class="entry-label text-sm font-medium text-gray-500">{{ entry.rowKey }}</span>
        </div>
        <div class="entry-fields space-y-3">
          <ContainerFieldRenderer
            v-for="field in fields"
            :key="field.fieldCode"
            :entry="entry"
            :indicator-code="indicatorCode || ''"
            :row-key="entry.rowKey"
            :field="field"
            :disabled="disabled"
            :entry-index="entryIdx"
            :container-last-values="lastPeriodValues"
            @update="() => handleFieldUpdate(entryIdx, field)"
            @blur="() => handleFieldBlur(entryIdx, field)"
            @field-change="() => handleFieldUpdate(entryIdx, field)"
          />
        </div>
      </div>
      <div v-if="containerType === 'autoEntry'" class="text-xs text-gray-400 mt-2">
        （由关联指标自动生成，数量不可调整）
      </div>
    </template>

    <!-- 普通容器 -->
    <template v-else>
      <div v-for="(entry, entryIdx) in entries" :key="entry.rowKey" class="dynamic-entry-row mb-4">
        <div class="entry-header flex items-center justify-between mb-2">
          <span class="entry-label text-sm font-medium text-gray-500">
            {{ entry.rowKey }}
          </span>
          <Button
            type="text"
            danger
            size="small"
            :disabled="entries.length === 1"
            @click="handleRemoveEntry(entryIdx)"
          >
            <template #icon><span>删除条目</span></template>
          </Button>
        </div>
        <div class="entry-fields space-y-3">
          <ContainerFieldRenderer
            v-for="field in fields"
            :key="field.fieldCode"
            :entry="entry"
            :indicator-code="indicatorCode || ''"
            :row-key="entry.rowKey"
            :field="field"
            :disabled="disabled"
            :entry-index="entryIdx"
            :container-last-values="lastPeriodValues"
            @update="() => handleFieldUpdate(entryIdx, field)"
            @blur="() => handleFieldBlur(entryIdx, field)"
            @field-change="() => handleFieldUpdate(entryIdx, field)"
          />
        </div>
      </div>
      <Button type="dashed" class="w-full mt-2" :disabled="disabled" @click="handleAddEntry">
        <template #icon><PlusOutlined /></template>
        添加条目
      </Button>
      <div v-if="entries.length > 0" class="text-xs text-gray-400 mt-1 text-right">
        共 {{ entries.length }} 个条目（上限 {{ MAX_ENTRIES }}）
      </div>
    </template>
  </div>
</template>

<style scoped>
.indicator-container { width: 100%; }
.dynamic-entry-row {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
  padding: 16px;
}
.entry-fields { display: flex; flex-direction: column; gap: 16px; }
.entry-header { border-bottom: 1px solid hsl(var(--border)); padding-bottom: 8px; margin-bottom: 12px; }
.mb-4 { margin-bottom: 16px; }
.w-full { width: 100%; }
.mt-2 { margin-top: 8px; }
.mt-1 { margin-top: 4px; }
.text-right { text-align: right; }
</style>
