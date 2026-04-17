/**
 * useLinkage - 联动状态管理
 */

import { watch } from 'vue';
import { evaluateAllLinkages } from '../utils/linkageEvaluator';
import type { LinkageEvaluationResult } from '../types';
import { formValues } from './useFormValues';
import { indicators } from './useIndicatorData';
import { linkageResults } from './useLinkageState';

const timers: ReturnType<typeof setTimeout>[] = [];
let initialized = false;
let isRecalculating = false;

function clearIndicatorValue(indicatorCode: string) {
  // // #region agent debug log
  // fetch('http://127.0.0.1:7550/ingest/3f60b161-8a14-4e30-8c88-b76dc9cc5103',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'952378'},body:JSON.stringify({sessionId:'952378',location:'useLinkage.ts:clearIndicatorValue',message:'联动清除值',data:{indicatorCode,currentValue:formValues[indicatorCode]},timestamp:Date.now()})}).catch(()=>{});
  // // #endregion
  if (formValues[indicatorCode] !== undefined) formValues[indicatorCode] = undefined;
}

function recalculateLinkage() {
  // // #region agent debug log
  // fetch('http://127.0.0.1:7550/ingest/3f60b161-8a14-4e30-8c88-b76dc9cc5103',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'952378'},body:JSON.stringify({sessionId:'952378',location:'useLinkage.ts:recalculateLinkage',message:'开始重新计算联动',data:{indicatorsCount:indicators.value.length},timestamp:Date.now()})}).catch(()=>{});
  // // #endregion
  if (indicators.value.length === 0) return;
  if (isRecalculating) return;
  isRecalculating = true;
  const results = evaluateAllLinkages(indicators.value, formValues);
  linkageResults.value = results;
  isRecalculating = false;
}

function getIndicatorLinkageState(indicatorCode: string): LinkageEvaluationResult | undefined {
  return linkageResults.value.get(indicatorCode);
}

export function isIndicatorVisible(indicatorCode: string): boolean {
  const state = getIndicatorLinkageState(indicatorCode);
  if (state?.type === 'show') {
    const visible = state.enabled;
    if (!visible && formValues[indicatorCode] !== undefined) clearIndicatorValue(indicatorCode);
    return visible;
  }
  return true;
}

export function isIndicatorDisabled(indicatorCode: string, readonly: boolean): boolean {
  const state = getIndicatorLinkageState(indicatorCode);
  if (state?.type === 'disabled') return !state.enabled;
  return readonly;
}

export function isIndicatorRequired(indicatorCode: string, isRequired: boolean): boolean {
  if (isRequired) return true;
  const state = getIndicatorLinkageState(indicatorCode);
  if (state?.type === 'required') return state.enabled;
  return false;
}

watch(
  () => indicators.value.length,
  (newLen, oldLen) => {
    if (newLen > 0 && (oldLen === 0 || oldLen === undefined)) {
      initialized = true;
      recalculateLinkage();
    }
  },
);

watch(
  () => ({ ...formValues }),
  () => {
    if (!initialized) return;
    if (isRecalculating) return;
    const timer = setTimeout(() => recalculateLinkage(), 100);
    timers.push(timer);
  },
  { deep: true },
);

export {
  linkageResults,
  recalculateLinkage,
  getIndicatorLinkageState,
  clearIndicatorValue,
};
