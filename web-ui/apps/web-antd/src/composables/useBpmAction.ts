import { ref, shallowRef } from 'vue';
import { message } from 'ant-design-vue';
import { submitBpmAction } from '#/api/bpm/action';

interface BpmActionVars {
  reason?: string;
  reasonRequired?: boolean;
  expertMin?: number;
  expertMax?: number;
  [key: string]: any;
}

interface BpmAction {
  key: string;
  label: string;
  vars?: BpmActionVars;
  taskId?: string;
  [key: string]: any;
}

interface UseBpmActionOptions {
  businessType: string;
  businessIdGetter: () => number | undefined;
  onSuccess?: () => void;
}

export function useBpmAction(options: UseBpmActionOptions) {
  const { businessType, businessIdGetter, onSuccess } = options;

  // 弹窗状态
  const submitModalVisible = ref(false);
  const submitLoading = ref(false);

  // 弹窗配置
  const submitModalTitle = ref('提交审核');
  const submitReasonLabel = ref('审核意见');
  const submitReasonRequired = ref(false);

  // 表单数据
  const submitForm = ref({
    reason: '',
  });

  // 当前操作（包含 row 和 action）
  const currentRow = shallowRef<any>(null);
  const currentAction = shallowRef<BpmAction | null>(null);

  /**
   * 打开 reason 弹窗
   */
  function openReasonModal(action: BpmAction, row?: any) {
    const vars = action.vars || {};

    currentRow.value = row;
    currentAction.value = action;
    submitForm.value.reason = '';

    // 弹窗标题：默认用 action.label
    submitModalTitle.value = action.label;
    // 输入框标签
    submitReasonLabel.value = vars.reason || '审核意见';
    // 是否必填
    submitReasonRequired.value = vars.reasonRequired === true;

    submitModalVisible.value = true;
  }

  /**
   * 关闭弹窗
   */
  function closeSubmitModal() {
    submitModalVisible.value = false;
    currentRow.value = null;
    currentAction.value = null;
  }

  /**
   * 执行 BPM 动作（通用方法）
   * @param row 业务数据行
   * @param action BPM 动作配置
   * @param extraParams 额外参数（如专家ID数组）
   */
  async function executeBpmAction(
    row: any,
    action: BpmAction,
    extraParams?: Record<string, any>
  ) {
    const businessId = row?.id ?? businessIdGetter();
    if (!businessId) {
      message.error('业务ID不存在');
      return;
    }

    try {
      await submitBpmAction({
        businessType,
        businessId,
        actionKey: action.key,
        reason: extraParams?.reason ?? '',
        taskId: action.taskId, // 传递任务ID
        ...extraParams,
      } as any);
      message.success(`操作 "${action.label}" 执行成功`);
      onSuccess?.();
    } catch (error: any) {
      console.error('[BPM] 执行操作失败:', error);
      message.error(error.message || '操作失败');
    }
  }

  /**
   * 处理 DSL 按钮操作（智能路由）
   * @param row 业务数据行
   * @param action BPM 动作配置
   */
  async function handleBpmAction(
    row: any,
    action: BpmAction
  ) {
    const vars = action.vars || {};

    // 专家选择弹窗：返回 true 表示需要调用方处理
    if (vars.expertMin !== undefined || vars.expertMax !== undefined) {
      // 调用方负责处理专家选择
      return true;
    }

    // reason 弹窗：有 reason 字段就显示弹窗
    if (vars.reason) {
      openReasonModal(action, row);
      return true;
    }

    // 不需要弹窗，直接执行
    await executeBpmAction(row, action);
    return false;
  }

  /**
   * 确认 reason 弹窗提交
   */
  async function handleSubmitConfirm() {
    if (!currentAction.value) {
      return;
    }

    // 必填校验
    if (submitReasonRequired.value && !submitForm.value.reason?.trim()) {
      message.warning(`请输入${submitReasonLabel.value}`);
      return;
    }

    const row = currentRow.value;
    const action = currentAction.value;

    submitLoading.value = true;
    try {
      await submitBpmAction({
        businessType,
        businessId: row?.id ?? businessIdGetter(),
        actionKey: action.key,
        reason: submitForm.value.reason,
        taskId: action.taskId, // 传递任务ID
      } as any);
      message.success('提交成功');

      closeSubmitModal();
      onSuccess?.();
    } catch (error: any) {
      console.error('[BPM] 提交失败:', error);
      message.error(error.message || '提交失败');
    } finally {
      submitLoading.value = false;
    }
  }

  return {
    // 状态
    submitModalVisible,
    submitLoading,
    submitModalTitle,
    submitReasonLabel,
    submitReasonRequired,
    submitForm,
    currentRow,
    currentAction,

    // 方法
    openReasonModal,
    closeSubmitModal,
    executeBpmAction,
    handleBpmAction,
    handleSubmitConfirm,
  };
}
