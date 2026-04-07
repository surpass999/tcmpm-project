<script setup lang="ts">
import { computed, ref, watch } from 'vue';

interface Props {
  /** v-model 绑定值 */
  modelValue?: number | null;
  /** 最小值 */
  min?: number;
  /** 最大值 */
  max?: number;
  /** 精度（小数位数） */
  precision?: number;
  /** 是否禁用 */
  disabled?: boolean;
  /** 占位符 */
  placeholder?: string;
  /** 前缀 */
  prefix?: string;
  /** 后缀（单位等） */
  suffix?: string;
  /** 外部传入的错误信息 */
  errorMsg?: string;
  /** 样式类名 */
  class?: string;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  min: undefined,
  precision: undefined,
  disabled: false,
  placeholder: '请输入数字',
  prefix: undefined,
  suffix: undefined,
  errorMsg: undefined,
  class: '',
});

const emit = defineEmits<{
  /**
   * 更新值（blur 时始终同步，让父组件统一做必填+精度+范围校验）
   * 与 a-input-number 行为一致：blur 时将值同步到父组件，父组件负责所有校验
   */
  'update:modelValue': [value: number | null];
  /** 失焦事件 */
  blur: [event: FocusEvent];
  /** 输入事件 */
  input: [value: string];
}>();

/** 内部显示的字符串（保留用户输入的原始格式） */
const displayValue = ref('');

/** 是否有验证错误（用于边框变红） */
const hasError = ref(false);

/**
 * 标记用户是否正在编辑中。
 * 编辑期间跳过 watch，防止父组件回写 modelValue 时覆盖用户正在输入的内容。
 * blur 后延迟清除（给父组件一轮响应时间），避免循环触发。
 */
let isUserEditing = false;

/** 初始化 / 外部清空时重置显示 */
watch(
  () => props.modelValue,
  (val) => {
    if (isUserEditing) return;
    if (val === null || val === undefined) {
      displayValue.value = '';
    }
  },
  { immediate: true },
);

/** 解析输入值，返回有效数字或 null */
function parseInputValue(input: string): number | null {
  const trimmed = input.trim();
  if (trimmed === '') return null;
  const num = Number(trimmed);
  return isNaN(num) ? null : num;
}

/** 处理输入 */
function handleInput(event: Event) {
  const target = event.target as HTMLInputElement;
  displayValue.value = target.value;
  isUserEditing = true;
  if (hasError.value) {
    hasError.value = false;
  }
  emit('input', target.value);
}

/**
 * 处理失焦：始终同步值，让父组件统一处理必填+精度+范围校验
 * 与 a-input-number 行为完全一致：blur 时将值（有效数字或 null）emit 到父组件，
 * 不校准、不显示组件内错误，所有校验逻辑走父组件。
 */
function handleBlur(event: FocusEvent) {
  const trimmed = displayValue.value.trim();
  const numVal = parseInputValue(trimmed);

  // 始终同步值到父组件（关键修复：不再仅有效值才 emit，与 a-input-number 行为一致）
  emit('update:modelValue', numVal);

  // 保留原始输入用于显示，不校准
  displayValue.value = trimmed;

  // 仅记录"输入了无效字符"状态用于边框变红，不由组件内显示错误
  hasError.value = trimmed !== '' && numVal === null;

  // 延迟清除编辑标记，等父组件一轮响应后再允许 watch 覆盖
  setTimeout(() => {
    isUserEditing = false;
  }, 50);

  emit('blur', event);
}

/** 处理聚焦 */
function handleFocus(_event: FocusEvent) {}

/** 获取最终显示的占位符 */
const computedPlaceholder = computed(() => {
  if (props.placeholder) return props.placeholder;
  if (props.precision !== undefined && props.precision !== null) {
    return `请输入数字（最多${props.precision}位小数）`;
  }
  return '请输入数字';
});

/** 是否有前缀 */
const hasPrefix = computed(() => !!props.prefix);

/** 是否有后缀 */
const hasSuffix = computed(() => !!props.suffix);

/** 是否有错误（组件内校验 or 父组件传入） */
const isInErrorState = computed(() => hasError.value || !!props.errorMsg);

/** 根节点：纵向排列输入行 + 下方错误区（父组件统一展示时占位） */
const rootClass = computed(() => ['safe-number-input-root', props.class]);

/** 输入行容器（外观对齐 ant InputNumber 单框 + 内置后缀） */
const containerClass = computed(() => {
  return [
    'safe-number-input',
    {
      'is-disabled': props.disabled,
      'has-error': isInErrorState.value,
      'has-prefix': hasPrefix.value,
      'has-suffix': hasSuffix.value,
    },
  ];
});
</script>

<template>
  <div :class="rootClass">
    <div :class="containerClass">
      <span v-if="prefix" class="safe-number-input-prefix">{{ prefix }}</span>
      <input
        type="text"
        class="safe-number-input-inner"
        :value="displayValue"
        :disabled="disabled"
        :placeholder="computedPlaceholder"
        inputmode="decimal"
        @input="handleInput"
        @blur="handleBlur"
        @focus="handleFocus"
      />
      <span v-if="suffix" class="safe-number-input-suffix">{{ suffix }}</span>
    </div>
    <!-- 父组件通过 errorMsg prop 传入错误时显示，与页面其它 .indicator-error 一致 -->
    <div
      v-if="errorMsg"
      class="safe-number-input-explain"
      role="alert"
    >
      {{ errorMsg }}
    </div>
  </div>
</template>

<style scoped>
/*
 * 所有颜色使用 hsl(var(--xxx)) 形式，确保切换主题（暗色/亮色）时自动跟随。
 * 不写死任何固定颜色值。
 */

.safe-number-input-root {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;
}

/* ============================================================
   基础输入框样式（对齐 ant-design-vue InputNumber）
   ============================================================ */
.safe-number-input {
  display: inline-flex;
  align-items: center;
  width: 100%;
  height: 32px;
  padding: 0;
  font-size: 14px;
  line-height: 1.5;
  color: hsl(var(--foreground));
  background-color: hsl(var(--input-background));
  background-image: none;
  border: 1px solid hsl(var(--input));
  border-radius: 6px;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
  /* 圆角由外层裁剪，后缀贴齐右缘，与 ant InputNumber group-addon 一致 */
  overflow: hidden;
}

/* 无前后缀时：内边距在容器上（等同 ant 仅 input） */
.safe-number-input:not(.has-prefix):not(.has-suffix) {
  padding: 0 11px;
}

/* 有 addon 时：中间输入区用内边距，addon 条带贴左右内缘 */
.safe-number-input.has-prefix,
.safe-number-input.has-suffix {
  align-items: stretch;
}

/* ============================================================
   聚焦样式
   ============================================================ */
/* 正常状态：聚焦 → 主题色边框 + 阴影 */
.safe-number-input:focus-within:not(.is-disabled):not(.has-error) {
  border-color: hsl(var(--primary));
  box-shadow: 0 0 0 2px hsl(var(--primary) / 0.1);
}

/* 错误状态：聚焦 → 主题色（错误）边框 + 阴影（不被 primary 覆盖） */
.safe-number-input.has-error:focus-within {
  border-color: hsl(var(--destructive)) !important;
  box-shadow: 0 0 0 2px hsl(var(--destructive) / 0.1) !important;
}

/* ============================================================
   Hover 样式
   ============================================================ */
/* 正常状态：hover → 主题色边框 */
.safe-number-input:hover:not(.is-disabled):not(.has-error) {
  border-color: hsl(var(--primary));
}

/* 错误状态：hover → 主题色（错误）边框（不被 primary 覆盖） */
.safe-number-input.has-error:hover {
  border-color: hsl(var(--destructive)) !important;
}

/* ============================================================
   禁用状态
   ============================================================ */
.safe-number-input.is-disabled {
  color: hsl(var(--muted-foreground));
  background-color: hsl(var(--muted));
  border-color: hsl(var(--border));
  cursor: not-allowed;
}

/* ============================================================
   内部输入框
   ============================================================ */
.safe-number-input-inner {
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 0;
  font-size: inherit;
  line-height: inherit;
  color: inherit;
  background: transparent;
  border: none;
  outline: none;
  box-shadow: none;
}

.safe-number-input.has-prefix .safe-number-input-inner,
.safe-number-input.has-suffix .safe-number-input-inner {
  align-self: center;
  height: auto;
  min-height: 0;
  padding: 0 11px;
}

.safe-number-input-inner::placeholder {
  color: hsl(var(--muted-foreground));
}

.safe-number-input-inner:disabled {
  cursor: not-allowed;
}

/* ============================================================
   前缀 / 后缀（对齐 ant-design-vue Input group-addon：colorFillAlter + colorText + colorBorder）
   参考 ant es/input/style genInputGroupStyle 中 &-addon
   ============================================================ */
.safe-number-input-prefix {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  padding: 0 11px;
  font-size: inherit;
  line-height: 1;
  font-weight: normal;
  text-align: center;
  color: hsl(var(--foreground));
  background-color: hsl(var(--accent));
  border-right: 1px solid hsl(var(--input));
  white-space: nowrap;
}

.safe-number-input-suffix {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  padding: 0 11px;
  font-size: inherit;
  line-height: 1;
  font-weight: normal;
  text-align: center;
  color: hsl(var(--foreground));
  background-color: hsl(var(--accent));
  border-left: 1px solid hsl(var(--input));
  white-space: nowrap;
}

/* 错误态：分隔线与外框同为错误色 */
.safe-number-input.has-error .safe-number-input-prefix {
  border-right-color: hsl(var(--destructive));
}

.safe-number-input.has-error .safe-number-input-suffix {
  border-left-color: hsl(var(--destructive));
}

/* 禁用：与 ant disabled addon 一致 */
.safe-number-input.is-disabled .safe-number-input-prefix,
.safe-number-input.is-disabled .safe-number-input-suffix {
  background-color: hsl(var(--muted));
  color: hsl(var(--muted-foreground));
  border-color: hsl(var(--border));
}

/* ============================================================
   错误提示块（与 IndicatorInputTable .indicator-error 视觉一致，使用主题变量）
   ============================================================ */
.safe-number-input-explain {
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
}
</style>
