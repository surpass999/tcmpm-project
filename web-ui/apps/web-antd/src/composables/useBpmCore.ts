import { computed, ref } from 'vue';
import type { BusinessType } from '@/constants/bpm/business-type';
import { getProcessDefinitionKey } from '@/constants/bpm/process-definition';
import { BpmCoreApi } from '@/api/bpm/core';
import type { BpmAction } from '@/api/bpm/action';

/**
 * BPM 审批操作 Composable
 *
 * @example
 * ```ts
 * const { loading, actions, loadActions, hasAction, submitProcess } = useBpmApproval({
 *   businessType: BUSINESS_TYPE.PROJECT_HALF_YEAR,
 *   businessIdGetter: () => processId.value,
 * });
 *
 * // 加载操作
 * await loadActions();
 *
 * // 判断是否有指定操作
 * if (hasAction('approve')) {
 *   // ...
 * }
 * ```
 */
export function useBpmApproval(options: {
  businessType: BusinessType;
  businessIdGetter: () => number | undefined;
}) {
  const { businessType, businessIdGetter } = options;

  const loading = ref(false);
  const actions = ref<BpmAction[]>([]);
  const processDefinitionKey = computed(() =>
    getProcessDefinitionKey(businessType)
  );

  /**
   * 加载可用操作
   */
  async function loadActions() {
    const id = businessIdGetter();
    if (!id) {
      actions.value = [];
      return;
    }

    loading.value = true;
    try {
      actions.value = await BpmCoreApi.getActions(businessType, id);
    } finally {
      loading.value = false;
    }
  }

  /**
   * 判断是否有指定操作
   */
  function hasAction(actionCode: string): boolean {
    return actions.value.some(a => a.key === actionCode);
  }

  /**
   * 发起流程
   */
  async function submitProcess() {
    const id = businessIdGetter();
    if (!id) return;

    loading.value = true;
    try {
      await BpmCoreApi.startProcess({
        businessId: id,
        businessType,
        processDefinitionKey: processDefinitionKey.value!,
      });
    } finally {
      loading.value = false;
    }
  }

  return {
    loading,
    actions,
    processDefinitionKey,
    loadActions,
    hasAction,
    submitProcess,
  };
}
