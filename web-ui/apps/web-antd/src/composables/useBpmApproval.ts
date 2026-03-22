import { ref } from 'vue';

import { message } from 'ant-design-vue';

import {
  approveTask,
  copyTask,
  delegateTask,
  getTaskByBusiness,
  rejectTask,
  returnTask,
  selectExpert,
  signCreateTask,
  transferTask,
} from '#/api/bpm/task';

export interface BpmAction {
  /** 任务ID */
  taskId: string;
  /** 任务名称 */
  taskName: string;
  /** 任务定义Key */
  taskDefinitionKey: string;
  /** 按钮设置 */
  buttonSettings?: Record<string, { displayName: string; enable: boolean; bizStatus?: string; rectifyProcessDefinitionKey?: string }>;
  /** 签名设置 */
  signEnable?: boolean;
  /** 原因设置 */
  reasonRequire?: boolean;
  /** 节点类型 */
  nodeType?: string;
  /** 按钮Key */
  buttonKey?: string;
}

export interface UseBpmApprovalOptions {
  /** 业务表名 */
  tableName: string;
  /** 业务类型（可选） */
  businessType?: string;
  /** 刷新回调 */
  onRefresh?: () => void;
  /** 自定义处理退回操作（返回true表示自定义处理） */
  onCustomReturn?: (taskId: string) => boolean | void;
  /** 打开专家选择弹窗（返回true表示自定义处理） */
  onOpenExpertSelect?: (taskId: string) => boolean | void;
  /** 自定义处理选择专家确认操作 */
  onSelectExpert?: (taskId: string, experts: any[]) => void | Promise<void>;
  /** 自定义处理用户选择操作（转办/委派/加签/抄送）（返回true表示自定义处理） */
  onCustomUserSelect?: (type: string, taskId: string) => boolean | void;
}

export function useBpmApproval(options: UseBpmApprovalOptions) {
  const { tableName, businessType: _businessType, onRefresh, onCustomReturn, onOpenExpertSelect, onSelectExpert, onCustomUserSelect } = options;

  // 每行业务ID的可用操作缓存
  const rowAvailableActions = ref<Record<number, BpmAction[]>>({});

  // ========== 弹窗组件引用 ==========

  // 当前操作的任务ID和类型
  const currentTaskId = ref('');
  const currentActionType = ref<'approve' | 'reject'>('approve');
  const currentButtonKey = ref<string>('');
  // 当前操作的完整 action（用于获取 rectifyProcessDefinitionKey 等扩展字段）
  const currentActionRef = ref<BpmAction | null>(null);

  // 审批弹窗（通过/拒绝）
  const approvalModalRef = ref<{
    open: (data: { taskId: string; action: 'approve' | 'reject'; title?: string }) => void;
    close: () => void;
  } | null>(null);

  // 退回弹窗
  const returnModalRef = ref<{
    open: (data: { taskId: string; title?: string }) => void;
    close: () => void;
  } | null>(null);

  // 用户选择弹窗（转办/委派/加签/抄送）
  const userSelectModalRef = ref<{
    open: (data: {
      title: string;
      multiple: boolean;
      showReason?: boolean;
      reasonLabel?: string;
      reasonRequired?: boolean;
    }) => void;
    close: () => void;
  } | null>(null);

  // 用户选择弹窗配置
  const userSelectConfig = ref<{
    action: 'transfer' | 'delegate' | 'addSign' | 'copy' | 'selectExpert';
    taskId: string;
    multiple: boolean;
    showReason: boolean;
    reasonLabel: string;
    reasonRequired: boolean;
  }>({
    action: 'transfer',
    taskId: '',
    multiple: false,
    showReason: false,
    reasonLabel: '操作原因',
    reasonRequired: false,
  });

  // ========== 方法 ==========

  /**
   * 加载一批业务数据的可用操作
   * @param rows 业务数据行
   * @param businessType 可选的动态业务类型（优先级高于初始化时的businessType）
   */
  async function loadRowAvailableActions(rows: { id: number }[], businessType?: string) {
    if (!rows || rows.length === 0) {
      return;
    }

    const businessIds = rows.map((r) => r.id).filter((id) => id);
    if (businessIds.length === 0) {
      return;
    }

    // 优先使用传入的 businessType，否则使用初始化时的
    const effectiveBusinessType = businessType || _businessType;

    try {
      const result = await getTaskByBusiness({
        tableName,
        businessType: effectiveBusinessType,
        businessIds,
      });

      // 处理返回结果，构建每行的可用操作
      const actionMap: Record<number, BpmAction[]> = {};

      for (const item of result) {
        const todoTasks = item.todoTasks || [];
        // 取第一个待办任务作为可用操作（简化处理）
        if (todoTasks.length > 0) {
          const task = todoTasks[0];
          if (!task) continue;

          const actions: BpmAction[] = [];

          // 从 buttonSettings 解析可用按钮
          const buttonSettings = task.buttonSettings || {};
          for (const [key, config] of Object.entries(buttonSettings)) {
            if (config?.enable) {
              actions.push({
                taskId: task.taskId,
                taskName: task.taskName,
                taskDefinitionKey: task.taskDefinitionKey,
                buttonSettings,
                signEnable: task.signEnable,
                reasonRequire: task.reasonRequire,
                nodeType: task.nodeType,
                buttonKey: key,
              });
            }
          }

          actionMap[item.businessId] = actions;
        }
      }

      rowAvailableActions.value = actionMap;
    } catch (error) {
      console.error('[useBpmApproval] 加载可用操作失败:', error);
    }
  }

  /**
   * 获取指定行的可用操作
   */
  function getRowActions(businessId: number): BpmAction[] {
    return rowAvailableActions.value[businessId] || [];
  }

  /**
   * 是否有待办任务
   */
  function hasTodoTask(businessId: number): boolean {
    const actions = rowAvailableActions.value[businessId];
    return !!(actions && actions.length > 0);
  }

  /**
   * 打开审批弹窗（通过/拒绝）
   */
  function openApprovalModal(taskId: string, action: 'approve' | 'reject', buttonKey?: string) {
    currentTaskId.value = taskId;
    currentActionType.value = action;
    currentButtonKey.value = buttonKey || '';
    approvalModalRef.value?.open({ taskId, action });
  }

  /**
   * 执行审批确认
   */
  async function confirmApproval(reason: string, action: 'approve' | 'reject') {
    const taskId = currentTaskId.value;
    const buttonKey = currentButtonKey.value;
    // buttonKey 转换为 buttonId (buttonKey 是 string，如 "1", "2")
    const buttonId = buttonKey ? parseInt(buttonKey, 10) : undefined;
    // 获取 rectifyProcessDefinitionKey（用于发起整改按钮）
    const rectifyProcessKey = currentActionRef.value?.buttonSettings?.[buttonKey || '']?.rectifyProcessDefinitionKey;
    if (action === 'approve') {
      await handleApprove(taskId, reason, buttonId, rectifyProcessKey);
    } else {
      await handleReject(taskId, reason, buttonId);
    }
  }

  /**
   * 打开用户选择弹窗
   */
  function openUserSelect(
    action: 'transfer' | 'delegate' | 'addSign' | 'copy' | 'selectExpert',
    taskId: string,
    config: {
      multiple?: boolean;
      showReason?: boolean;
      reasonLabel?: string;
      reasonRequired?: boolean;
    } = {}
  ) {
    const actionConfig = {
      transfer: { title: '转办', multiple: false, showReason: false },
      delegate: { title: '委派', multiple: false, showReason: false },
      addSign: { title: '加签', multiple: true, showReason: false },
      copy: { title: '抄送', multiple: true, showReason: false },
      selectExpert: { title: '选择专家', multiple: true, showReason: false },
    };

    userSelectConfig.value = {
      action,
      taskId,
      multiple: config.multiple ?? actionConfig[action].multiple,
      showReason: config.showReason ?? actionConfig[action].showReason,
      reasonLabel: config.reasonLabel ?? '操作原因',
      reasonRequired: config.reasonRequired ?? actionConfig[action].showReason,
    };

    userSelectModalRef.value?.open({
      title: actionConfig[action].title,
      multiple: userSelectConfig.value.multiple,
      showReason: userSelectConfig.value.showReason,
      reasonLabel: userSelectConfig.value.reasonLabel,
      reasonRequired: userSelectConfig.value.reasonRequired,
    });
  }

  /**
   * 处理用户选择确认
   */
  async function handleUserSelectConfirm(users: any[], reason: string) {
    const { action, taskId } = userSelectConfig.value;
    const userIds = users.map((u) => u.id);

    try {
      switch (action) {
        case 'transfer':
          await handleTransfer(taskId, userIds[0]);
          break;
        case 'delegate':
          await handleDelegate(taskId, userIds[0]);
          break;
        case 'addSign':
          await handleAddSign(taskId, userIds);
          break;
        case 'copy':
          await handleCopy(taskId, userIds);
          break;
        case 'selectExpert':
          await handleSelectExpert(taskId, users);
          break;
      }
    } catch (error) {
      console.error(`处理${action}操作失败:`, error);
    }
  }

  // ========== 具体操作方法 ==========

  /**
   * 审批通过
   */
  async function handleApprove(taskId: string, reason?: string, buttonId?: number, rectifyProcessKey?: string) {
    try {
      await approveTask({
        id: taskId,
        reason,
        buttonId,
        variables: rectifyProcessKey
          ? { rectifyProcessDefinitionKey: rectifyProcessKey }
          : undefined,
      });
      message.success('审批通过');
      onRefresh?.();
    } catch (error) {
      console.error('审批通过失败:', error);
      message.error('审批通过失败');
      throw error;
    }
  }

  /**
   * 审批拒绝
   */
  async function handleReject(taskId: string, reason: string, buttonId?: number) {
    try {
      await rejectTask({ id: taskId, reason, buttonId });
      message.success('审批拒绝');
      onRefresh?.();
    } catch (error) {
      console.error('审批拒绝失败:', error);
      message.error('审批拒绝失败');
      throw error;
    }
  }

  /**
   * 审批拒绝（带确认）
   */
  async function handleRejectWithConfirm(taskId: string, reason: string, buttonId?: number) {
    await handleReject(taskId, reason, buttonId);
  }

  /**
   * 转办
   */
  async function handleTransfer(taskId: string, userId: number) {
    try {
      await transferTask({ id: taskId, assigneeUserId: userId });
      message.success('转办成功');
      onRefresh?.();
    } catch (error) {
      console.error('转办失败:', error);
      message.error('转办失败');
      throw error;
    }
  }

  /**
   * 委派
   */
  async function handleDelegate(taskId: string, userId: number) {
    try {
      await delegateTask({ id: taskId, delegateUserId: userId });
      message.success('委派成功');
      onRefresh?.();
    } catch (error) {
      console.error('委派失败:', error);
      message.error('委派失败');
      throw error;
    }
  }

  /**
   * 加签
   */
  async function handleAddSign(taskId: string, userIds: number[]) {
    try {
      await signCreateTask({ id: taskId, userIds, type: 'after', reason: '加签' });
      message.success('加签成功');
      onRefresh?.();
    } catch (error) {
      console.error('加签失败:', error);
      message.error('加签失败');
      throw error;
    }
  }

  /**
   * 退回
   */
  async function handleReturn(taskId: string, targetTaskDefinitionKey?: string, reason?: string) {
    try {
      await returnTask({ id: taskId, targetTaskDefinitionKey, reason });
      message.success('退回成功');
      onRefresh?.();
    } catch (error) {
      console.error('退回失败:', error);
      message.error('退回失败');
      throw error;
    }
  }

  /**
   * 抄送
   */
  async function handleCopy(taskId: string, userIds: number[]) {
    try {
      await copyTask({ id: taskId, copyUserIds: userIds });
      message.success('抄送成功');
      onRefresh?.();
    } catch (error) {
      console.error('抄送失败:', error);
      message.error('抄送失败');
      throw error;
    }
  }

  /**
   * 选择专家
   */
  async function handleSelectExpert(taskId: string, experts: any[]) {
    try {
      // 提取专家关联的系统用户ID
      const expertUserIds = experts
        .map((e) => e.userId)
        .filter((userId): userId is number => userId !== undefined && userId !== null);

      if (expertUserIds.length === 0) {
        message.error('所选专家未关联系统用户');
        return;
      }

      await selectExpert({
        id: taskId,
        userIds: expertUserIds,
      });
      message.success('选择专家成功');
      onRefresh?.();
    } catch (error) {
      console.error('选择专家失败:', error);
      message.error('选择专家失败');
      throw error;
    }
  }

  /**
   * 处理审批操作
   */
  async function handleApprovalAction(action: BpmAction) {
    console.log('[DEBUG] handleApprovalAction called with:', action);
    const { taskId, buttonKey } = action;
    console.log('[DEBUG] taskId:', taskId, 'buttonKey:', buttonKey);

    // 存储当前操作供 confirmApproval 使用
    currentActionRef.value = action;

    switch (buttonKey) {
      case '1': // 通过
        openApprovalModal(taskId, 'approve', buttonKey);
        break;
      case '2': // 拒绝
        openApprovalModal(taskId, 'reject', buttonKey);
        break;
      case '3': // 转办
        if (onCustomUserSelect?.('transfer', taskId)) {
          // 自定义处理
        } else {
          openUserSelect('transfer', taskId);
        }
        break;
      case '4': // 委派
        if (onCustomUserSelect?.('delegate', taskId)) {
          // 自定义处理
        } else {
          openUserSelect('delegate', taskId);
        }
        break;
      case '5': // 加签
        if (onCustomUserSelect?.('addSign', taskId)) {
          // 自定义处理
        } else {
          openUserSelect('addSign', taskId);
        }
        break;
      case '6': // 退回
        // 如果有自定义退回处理，则调用；否则使用退回弹窗
        if (onCustomReturn?.(taskId)) {
          // 自定义处理
        } else {
          returnModalRef.value?.open({ taskId, title: '退回' });
        }
        break;
      case '7': // 抄送
        if (onCustomUserSelect?.('copy', taskId)) {
          // 自定义处理
        } else {
          openUserSelect('copy', taskId);
        }
        break;
      case '8': // 选择专家
        // 调用自定义打开专家选择弹窗处理
        console.log('[DEBUG] case 8: onOpenExpertSelect =', onOpenExpertSelect);
        onOpenExpertSelect?.(taskId);
        break;
      case '9': // 发起整改 - 打开审批弹窗（填写整改意见）
        openApprovalModal(taskId, 'approve', buttonKey);
        break;
      default:
        console.warn('未知的操作类型:', buttonKey);
    }
  }

  /**
   * 刷新指定行的操作
   */
  async function refreshRowActions(row: { id: number }) {
    await loadRowAvailableActions([row]);
  }

  return {
    // 状态
    rowAvailableActions,

    // 弹窗组件引用
    approvalModalRef,
    returnModalRef,
    userSelectModalRef,

    // 方法
    loadRowAvailableActions,
    getRowActions,
    hasTodoTask,
    handleApprovalAction,
    openApprovalModal,
    confirmApproval,
    openUserSelect,
    handleUserSelectConfirm,
    refreshRowActions,

    // 操作方法（供外部直接调用）
    handleApprove,
    handleReject,
    handleRejectWithConfirm,
    handleTransfer,
    handleDelegate,
    handleAddSign,
    handleReturn,
    handleCopy,
    handleSelectExpert,
  };
}
