<script setup lang="ts">
/**
 * 指标行组件
 * 负责单个指标行的渲染
 */

import { computed } from 'vue';
import { IconifyIcon } from '@vben/icons';

import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { DynamicField } from '../../types';

// 输入组件
import NumberInput from './input-components/NumberInput.vue';
import TextInput from './input-components/TextInput.vue';
import SwitchInput from './input-components/SwitchInput.vue';
import DatePickerInput from './input-components/DatePickerInput.vue';
import TextareaInput from './input-components/TextareaInput.vue';
import RadioInput from './input-components/RadioInput.vue';
import CheckboxInput from './input-components/CheckboxInput.vue';
import SelectInput from './input-components/SelectInput.vue';
import MultiSelectInput from './input-components/MultiSelectInput.vue';
import DateRangeInput from './input-components/DateRangeInput.vue';

interface Props {
  indicator: DeclareIndicatorApi.Indicator;
  modelValue: any;
  disabled?: boolean;
  lastValue?: string;
  error?: string;
  fieldError?: string;
  showFieldError?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
});

const emit = defineEmits<{
  'update:modelValue': [value: any];
  'blur': [event: any];
  'change': [indicator: DeclareIndicatorApi.Indicator];
}>();

/** 判断指标是否有口径 */
const hasIndicatorSpec = computed(() => {
  const ind = props.indicator;
  return !!(
    ind.definition ||
    ind.statisticScope ||
    ind.dataSource ||
    ind.fillRequire ||
    ind.calculationExample
  );
});

/** 判断是否为计算指标 */
const isComputedIndicator = computed(() => {
  return !!(props.indicator.calculationRule && props.indicator.calculationRule.trim());
});

/** 解析 valueOptions */
function parseOptions(valueOptions: string): Array<{ value: string; label: string }> {
  if (!valueOptions) return [];
  try {
    const parsed = JSON.parse(valueOptions);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

/** 解析扩展配置 */
function parseExtraConfig(extraConfig: string | undefined): Record<string, any> {
  if (!extraConfig) return {};
  try {
    return JSON.parse(extraConfig);
  } catch {
    return {};
  }
}

/** 获取数字精度 */
const precision = computed(() => {
  const cfg = parseExtraConfig(props.indicator.extraConfig);
  return cfg.precision !== undefined ? Number(cfg.precision) : undefined;
});

/** 获取日期格式 */
const dateFormat = computed(() => {
  const cfg = parseExtraConfig(props.indicator.extraConfig);
  return cfg.format || 'YYYY-MM-DD';
});

/** 获取布尔类型标签 */
const booleanLabels = computed(() => {
  const cfg = parseExtraConfig(props.indicator.extraConfig);
  return {
    true: cfg.trueLabel || '是',
    false: cfg.falseLabel || '否',
  };
});

/** 获取最大长度 */
const maxLength = computed(() => {
  const cfg = parseExtraConfig(props.indicator.extraConfig);
  return cfg.maxLength !== undefined ? Number(cfg.maxLength) : undefined;
});

/** 是否显示搜索 */
const showSearch = computed(() => {
  const cfg = parseExtraConfig(props.indicator.extraConfig);
  return cfg.showSearch === true;
});

/** 是否富文本 */
const isRichText = computed(() => {
  const cfg = parseExtraConfig(props.indicator.extraConfig);
  return cfg.richText === true;
});

/** 是否内联显示（开关、同行选择） */
const isInlineDisplay = computed(() => {
  return props.indicator.valueType === 3 || props.indicator.valueType === 6 || props.indicator.valueType === 7;
});

/** 是否为数字类型 */
const isNumberType = computed(() => props.indicator.valueType === 1);

/** 是否为开关类型 */
const isSwitchType = computed(() => props.indicator.valueType === 3);

function handleUpdate(value: any) {
  emit('update:modelValue', value);
}

function handleChange() {
  emit('change', props.indicator);
}

function handleBlur(e: any) {
  emit('blur', e);
}
</script>

<template>
  <div
    class="indicator-row"
    :class="{
      'indicator-row--switch': isSwitchType,
    }"
    :data-indicator-id="'in_' + indicator.id"
    :data-indicator-code="indicator.indicatorCode"
  >
    <!-- 左侧：指标名称 + 输入组件 -->
    <div
      class="indicator-main"
      :class="{ 'indicator-main--inline': isInlineDisplay }"
    >
      <!-- 指标标签行 -->
      <div class="indicator-label-row">
        <!-- 有口径时用 Popover 展示 -->
        <div class="flex items-center gap-1 flex-wrap">
          <a-popover
            v-if="hasIndicatorSpec"
            placement="right"
            trigger="click"
            :overlay-style="{ maxWidth: '420px' }"
          >
            <template #content>
              <div class="caliber-popover-content">
                <div v-if="indicator.definition" class="caliber-item">
                  <div class="caliber-label">指标定义</div>
                  <div class="caliber-value">{{ indicator.definition }}</div>
                </div>
                <div v-if="indicator.statisticScope" class="caliber-item">
                  <div class="caliber-label">统计范围</div>
                  <div class="caliber-value">{{ indicator.statisticScope }}</div>
                </div>
                <div v-if="indicator.dataSource" class="caliber-item">
                  <div class="caliber-label">数据来源</div>
                  <div class="caliber-value">{{ indicator.dataSource }}</div>
                </div>
                <div v-if="indicator.fillRequire" class="caliber-item">
                  <div class="caliber-label">填报要求</div>
                  <div class="caliber-value">{{ indicator.fillRequire }}</div>
                </div>
                <div v-if="indicator.calculationExample" class="caliber-item">
                  <div class="caliber-label">计算示例</div>
                  <div class="caliber-value">{{ indicator.calculationExample }}</div>
                </div>
              </div>
            </template>
            <span class="label-text has-caliber" :title="indicator.indicatorName">
              {{ indicator.indicatorCode }} - {{ indicator.indicatorName }}
              <IconifyIcon icon="lucide:help-circle" class="caliber-icon" />
            </span>
          </a-popover>
          <!-- 无口径时只显示名称 -->
          <span v-else class="label-text" :title="indicator.indicatorName">
            {{ indicator.indicatorCode }} - {{ indicator.indicatorName }}
          </span>
        </div>
        <a-tag v-if="isComputedIndicator" color="orange" class="computed-tag">自动计算</a-tag>
      </div>

      <!-- 指标输入组件行 -->
      <div
        class="indicator-input-row"
        :class="{
          'input-row--inline': isInlineDisplay,
        }"
      >
        <!-- 数字类型 -->
        <NumberInput
          v-if="indicator.valueType === 1"
          :model-value="modelValue"
          :disabled="disabled || isComputedIndicator"
          :placeholder="isComputedIndicator ? '自动计算' : '请输入数字'"
          :min="indicator.minValue ?? 0"
          :max="indicator.maxValue"
          :precision="precision"
          :suffix="indicator.unit || parseExtraConfig(indicator.extraConfig).suffix"
          @update:model-value="handleUpdate"
          @blur="handleBlur"
        />

        <!-- 字符串类型 -->
        <TextInput
          v-else-if="indicator.valueType === 2"
          :model-value="modelValue"
          :disabled="disabled"
          :placeholder="`请输入${indicator.indicatorName}`"
          :maxlength="maxLength"
          @update:model-value="handleUpdate"
          @change="handleChange"
        />

        <!-- 布尔类型（开关） -->
        <SwitchInput
          v-else-if="indicator.valueType === 3"
          :model-value="modelValue"
          :disabled="disabled"
          :true-label="booleanLabels.true"
          :false-label="booleanLabels.false"
          @update:model-value="handleUpdate"
          @change="handleChange"
        />

        <!-- 日期类型 -->
        <DatePickerInput
          v-else-if="indicator.valueType === 4"
          :model-value="modelValue"
          :disabled="disabled"
          :format="dateFormat"
          @update:model-value="handleUpdate"
          @change="handleChange"
        />

        <!-- 长文本类型 -->
        <TextareaInput
          v-else-if="indicator.valueType === 5"
          :model-value="modelValue"
          :disabled="disabled"
          :placeholder="`请输入${indicator.indicatorName}`"
          :rows="isRichText ? 4 : 2"
          :maxlength="maxLength"
          :show-count="!!maxLength"
          @update:model-value="handleUpdate"
          @change="handleChange"
        />

        <!-- 单选类型 -->
        <RadioInput
          v-else-if="indicator.valueType === 6"
          :model-value="modelValue"
          :disabled="disabled"
          :options="parseOptions(indicator.valueOptions)"
          @update:model-value="handleUpdate"
          @change="handleChange"
        />

        <!-- 多选类型 -->
        <CheckboxInput
          v-else-if="indicator.valueType === 7"
          :model-value="modelValue || []"
          :disabled="disabled"
          :options="parseOptions(indicator.valueOptions)"
          @update:model-value="handleUpdate"
          @change="handleChange"
        />

        <!-- 单选下拉类型 -->
        <SelectInput
          v-else-if="indicator.valueType === 10"
          :model-value="modelValue"
          :disabled="disabled"
          :placeholder="`请选择${indicator.indicatorName}`"
          :options="parseOptions(indicator.valueOptions)"
          :show-search="showSearch"
          @update:model-value="handleUpdate"
          @change="handleChange"
        />

        <!-- 多选下拉类型 -->
        <MultiSelectInput
          v-else-if="indicator.valueType === 11"
          :model-value="modelValue || []"
          :disabled="disabled"
          :placeholder="`请选择${indicator.indicatorName}`"
          :options="parseOptions(indicator.valueOptions)"
          :show-search="showSearch"
          @update:model-value="handleUpdate"
          @change="handleChange"
        />

        <!-- 日期区间类型 -->
        <DateRangeInput
          v-else-if="indicator.valueType === 8"
          :model-value="modelValue"
          :disabled="disabled"
          :format="dateFormat"
          @update:model-value="handleUpdate"
          @change="handleChange"
        />

        <!-- 文件上传类型（暂时保持原样，由父组件处理） -->
        <slot v-else-if="indicator.valueType === 9" name="file-upload" />

        <!-- 动态容器（暂时保持原样，由父组件处理） -->
        <slot v-else-if="indicator.valueType === 12" name="container" />

        <!-- 未知类型 -->
        <span v-else class="text-gray-400 text-sm">暂不支持该类型</span>

        <!-- 错误提示 -->
        <div
          v-if="error && indicator.valueType !== 12"
          class="indicator-error"
        >
          {{ error }}
        </div>
      </div>
    </div>

    <!-- 右侧：上期值 -->
    <div
      v-if="lastValue"
      class="indicator-last-value"
    >
      <div class="last-value-label">上期值</div>
      <div class="last-value-content">
        {{ lastValue }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.indicator-row {
  display: flex;
  align-items: flex-start;
  gap: 0;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 10px;
  padding: 14px 16px;
  transition: border-color 0.2s, box-shadow 0.2s;
  position: relative;
  overflow: visible;
}

.indicator-row::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: hsl(var(--primary));
  border-radius: 10px 0 0 10px;
  opacity: 0;
  transition: opacity 0.2s;
}

.indicator-row:hover {
  border-color: hsl(var(--primary) / 0.4);
  box-shadow: 0 2px 12px hsl(var(--primary) / 0.08);
}

.indicator-row:hover::before {
  opacity: 1;
}

.indicator-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 30px;
  flex: 1;
  min-width: 0;
}

.indicator-main--inline {
  flex: none;
  align-self: center;
  width: 100%;
}

.indicator-label-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.label-text {
  font-size: 14px;
  font-weight: 500;
  color: hsl(var(--foreground));
  line-height: 1.4;
}

.label-text.has-caliber {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-weight: 600;
  transition: color 0.15s;
}

.label-text.has-caliber:hover {
  color: hsl(var(--primary-dark));
}

.caliber-icon {
  font-size: 13px;
  flex-shrink: 0;
  opacity: 0.7;
}

.caliber-popover-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 400px;
  overflow-y: auto;
  padding: 4px 0;
}

.caliber-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.caliber-label {
  font-size: 12px;
  font-weight: 600;
  color: hsl(var(--muted-foreground));
  line-height: 1.4;
}

.caliber-value {
  font-size: 13px;
  color: hsl(var(--foreground));
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.computed-tag {
  font-size: 11px;
  padding: 0 6px;
  height: 20px;
  line-height: 18px;
  border-radius: 4px;
  font-weight: 600;
  background: hsl(var(--warning) / 0.1);
  color: hsl(var(--warning));
  border-color: hsl(var(--warning) / 0.2);
}

.indicator-input-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.input-row--inline {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
}

.w-full {
  width: 100%;
}

.indicator-error {
  display: flex;
  align-items: center;
  gap: 6px;
  color: hsl(var(--destructive));
  font-size: 12.5px;
  margin-top: 6px;
  padding: 6px 10px;
  background: hsl(var(--destructive) / 0.06);
  border: 1px solid hsl(var(--destructive) / 0.2);
  border-radius: 6px;
  line-height: 1.4;
  min-width: 160px;
  width: fit-content;
}

.indicator-last-value {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-left: 1px solid hsl(var(--border));
  padding-left: 16px;
  gap: 4px;
}

.last-value-label {
  font-size: 11px;
  color: hsl(var(--muted-foreground));
  font-weight: 500;
  letter-spacing: 0.3px;
  text-transform: uppercase;
}

.last-value-content {
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--success));
  line-height: 1.2;
}

.text-gray-400 {
  color: hsl(var(--muted-foreground));
}
</style>
