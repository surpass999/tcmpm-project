/**
 * useLinkage - 联动状态管理
 *
 * 职责：
 * - 管理联动结果状态
 * - 监听表单值变化，重新计算联动
 * - 提供指标联动状态查询
 *
 * 设计原则：
 * - 只导入状态（formValues），不导入其他 composables 的函数
 * - 避免循环依赖
 */

import { ref, watch, computed } from 'vue';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import {
  evaluateAllLinkages,
  evaluateLinkage,
  parseLinkageConfig,
} from '../utils/linkageEvaluator';
import type { LinkageEvaluationResult } from '../types';
import { formValues } from './useFormValues';
import { indicators } from './useIndicatorData';

// ==================== 状态 ====================

/** 联动结果缓存：key = indicatorCode */
const linkageResults = ref<Map<string, LinkageEvaluationResult>>(new Map());

/** 定时器集合（用于清理） */
const timers: ReturnType<typeof setTimeout>[] = [];

/** 是否已初始化完成 */
let initialized = false;

/** 防止重入的锁：recalculateLinkage 执行期间为 true */
let isRecalculating = false;

// ==================== 计算属性 ====================

/**
 * 计算所有被监听的指标编码集合
 * 用于精准监听，只监听联动配置中涉及的触发指标
 */
const watchedIndicators = computed(() => {
  const watched = new Set<string>();
  for (const indicator of indicators.value) {
    const config = parseLinkageConfig(indicator);
    if (config?.enabled && config.trigger?.indicatorCode) {
      watched.add(config.trigger.indicatorCode);
    }
  }
  return watched;
});

/**
 * 是否有任意联动配置
 */
const hasAnyLinkage = computed(() => {
  for (const indicator of indicators.value) {
    const config = parseLinkageConfig(indicator);
    if (config?.enabled) {
      return true;
    }
  }
  return false;
});

// ==================== 清除值 ====================

/**
 * 清除指标的表单值
 * 当指标被隐藏时调用，避免脏数据提交
 */
function clearIndicatorValue(indicatorCode: string) {
  // 清除主值
  if (formValues[indicatorCode] !== undefined) {
    console.log('[DEBUG useLinkage clearIndicatorValue]', { indicatorCode, oldValue: formValues[indicatorCode] });
    formValues[indicatorCode] = undefined;
  }
}

// ==================== 联动计算 ====================

/**
 * 重新计算所有联动
 */
function recalculateLinkage() {
  if (indicators.value.length === 0) return;
  if (isRecalculating) return; // 防止重入
  isRecalculating = true;
  console.log('[DEBUG useLinkage recalculateLinkage] START');
  // 打印当前 formValues 中所有联动指标的值
  const linkedCodes = ['702', '70201'];
  for (const code of linkedCodes) {
    console.log('[DEBUG useLinkage recalculateLinkage] before eval formValues', { code, value: formValues[code] });
  }
  const results = evaluateAllLinkages(indicators.value, formValues);
  // 打印所有联动结果
  for (const [code, result] of results) {
    console.log('[DEBUG useLinkage recalculateLinkage] linkage result', { code, type: result.type, enabled: result.enabled });
  }
  linkageResults.value = results;
  console.log('[DEBUG useLinkage recalculateLinkage] END');
  isRecalculating = false;
}

/**
 * 获取指标的联动状态
 */
function getIndicatorLinkageState(
  indicatorCode: string,
): LinkageEvaluationResult | undefined {
  return linkageResults.value.get(indicatorCode);
}

// ==================== 状态查询 ====================

/**
 * 判断指标是否应该显示
 * 优先级：
 * 1. 有 show 类型联动 → 按联动结果
 * 2. 无联动 → 默认显示
 *
 * 副作用：隐藏时自动清除值
 */
function isIndicatorVisible(indicatorCode: string): boolean {
  const state = getIndicatorLinkageState(indicatorCode);
  // 调试特定指标
  if (indicatorCode === '702' || indicatorCode === '70201') {
    console.log('[DEBUG isIndicatorVisible RENDER]', { indicatorCode, state, linkageResultsSize: linkageResults.value.size, formValues702: formValues['702'] });
  }

  if (state?.type === 'show') {
    const visible = state.enabled;
    console.log('[DEBUG isIndicatorVisible]', { indicatorCode, visible, type: state.type, enabled: state.enabled });

    // 如果即将变为隐藏，清除值
    if (!visible && formValues[indicatorCode] !== undefined) {
      clearIndicatorValue(indicatorCode);
    }

    return visible;
  }

  return true;
}

/**
 * 判断指标是否应该禁用
 * 优先级：
 * 1. 有 disabled 类型联动 → 按联动结果
 * 2. 外部传入的 readonly
 */
function isIndicatorDisabled(
  indicatorCode: string,
  readonly: boolean,
): boolean {
  const state = getIndicatorLinkageState(indicatorCode);
  if (state?.type === 'disabled') {
    // enabled = true → 可用，enabled = false → 禁用
    return !state.enabled;
  }
  return readonly;
}

/**
 * 判断指标是否应该必填
 * 优先级：
 * 1. 有 required 类型联动 → 按联动结果
 * 2. 指标本身的 isRequired
 */
function isIndicatorRequired(
  indicatorCode: string,
  isRequired: boolean,
): boolean {
  if (isRequired) return true;
  const state = getIndicatorLinkageState(indicatorCode);
  if (state?.type === 'required') {
    return state.enabled;
  }
  return false;
}

// ==================== 监听设置 ====================

// 监听指标加载，首次加载时初始化联动
watch(
  () => indicators.value.length,
  (newLen, oldLen) => {
    if (newLen > 0 && (oldLen === 0 || oldLen === undefined)) {
      // 首次加载指标（formValues 已在 loadIndicatorData 中通过 await savedValues 填充完毕）
      initialized = true;
      recalculateLinkage();
      // 验证 linkageResults 是否正确
      console.log('[DEBUG useLinkage watch] after recalculateLinkage linkageResults.get(702):', linkageResults.value.get('702'));
    }
  },
);

// 监听被监听的指标值变化（精准监听 + 防抖）
watch(
  () => {
    // 只返回被监听的指标的值
    const result: Record<string, any> = {};
    for (const code of watchedIndicators.value) {
      result[code] = formValues[code];
    }
    return result;
  },
  () => {
    if (!initialized) return;
    if (watchedIndicators.value.size === 0) return;
    if (isRecalculating) return; // recalculateLinkage 正在执行，跳过

    // 防抖处理
    const timer = setTimeout(() => {
      recalculateLinkage();
    }, 100);
    timers.push(timer);
  },
  { deep: true },
);

// ==================== 导出 ====================

/**
 * 重置联动状态（供外部在需要时调用，如组件卸载）
 */
function resetLinkageState() {
  // 清理定时器
  for (const timer of timers) {
    clearTimeout(timer);
  }
  timers.length = 0;

  // 重置状态
  linkageResults.value = new Map();
  initialized = false;
  isRecalculating = false;
}

export {
  linkageResults,
  recalculateLinkage,
  getIndicatorLinkageState,
  isIndicatorVisible,
  isIndicatorDisabled,
  isIndicatorRequired,
  hasAnyLinkage,
  clearIndicatorValue,
  resetLinkageState,
};
