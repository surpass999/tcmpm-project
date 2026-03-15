<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareFilingApi } from '#/api/declare/filing';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { DeclareExpertApi } from '#/api/declare/expert';
import type { BpmActionApi } from '#/api/bpm/action';

import { computed, nextTick, onMounted, ref, shallowRef } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';
import { useUserStore } from '@vben/stores';

import { message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteFiling,
  deleteFilingList,
  exportFiling,
  submitFiling,
  getFilingPage,
} from '#/api/declare/filing';
import { getIndicatorsForListDisplay } from '#/api/declare/indicator';
import { getTaskListByProcessInstanceId, approveTask, rejectTask, transferTask, delegateTask, signCreateTask, returnTask, copyTask, setNextAssignees, getTaskListByReturn } from '#/api/bpm/task';
import { getTaskByBusiness } from '#/api/bpm/task';
import { getAvailableActions } from '#/api/bpm/action';
import { getSimpleUserList } from '#/api/system/user';
import { $t } from '#/locales';

import ExpertSelectModalCmp from '#/components/bpm/ExpertSelectModal.vue';
import UserSelectModalCmp from '#/components/bpm/UserSelectModal.vue';

import { useBpmAction } from '#/composables/useBpmAction';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

import ApprovalDetailCmp from '#/views/bpm/components/approval-detail.vue';
import ActionButtonCmp from '#/components/bpm/ActionButton.vue';

// 业务类型
const BUSINESS_TYPE_KEY = 'declare:filing:create';

// 当前登录用户ID
const userStore = useUserStore();
const currentUserId = computed(() => {
  return Number(userStore.userInfo?.userId) || userStore.userInfo?.id;
});

// 每行的可用操作缓存 { [id]: BpmAction[] }
const rowAvailableActions = ref<Record<number, any[]>>({});

// 需要在列表显示的指标
const listDisplayIndicators = shallowRef<DeclareIndicatorApi.Indicator[]>([]);

// 指标列表业务类型（用于加载列表显示指标）
const BUSINESS_TYPE = 'filing';

// 基础列配置
const baseColumns = useGridColumns() || [];

// 计算所有列（包含动态指标列）
const allColumns = computed(() => {
  const cols = baseColumns as any[];
  const indicatorColumns = getDynamicColumns();
  if (indicatorColumns.length === 0) {
    return cols;
  }
  return [...cols.slice(0, -1), ...indicatorColumns, cols[cols.length - 1]];
});

// 加载列表显示指标
async function loadListDisplayIndicators() {
  try {
    const data = await getIndicatorsForListDisplay(BUSINESS_TYPE);
    listDisplayIndicators.value = data || [];
    // 更新表格列配置
    const cols = allColumns.value;
    // 使用 setGridOptions 更新列配置
    nextTick(async () => {
      if (gridApi?.setGridOptions) {
        await gridApi.setGridOptions({ columns: cols });
      }
      // 刷新表格数据
      await gridApi?.query();
    });
  } catch (error) {
    console.error('加载列表显示指标失败:', error);
    listDisplayIndicators.value = [];
  }
}

// 获取动态列
function getDynamicColumns() {
  // 如果还没有加载指标，返回空数组
  if (!listDisplayIndicators.value || listDisplayIndicators.value.length === 0) {
    return [];
  }

  const indicatorColumns = listDisplayIndicators.value.map((indicator) => ({
    field: `indicator_${indicator.indicatorCode}`, // 使用唯一字段名
    title: indicator.indicatorName,
    minWidth: 120,
    slots: { default: 'indicator_value' },
    params: { indicatorCode: indicator.indicatorCode },
  }));
  return indicatorColumns;
}

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
  // fullscreen: true,
  class: '!min-w-[90%] !min-h-[80vh]',
  closeOnClickModal: false, // 点击遮罩不关闭弹窗
  // contentClass: '!min-w-[90%] !min-h-[80vh]'
});

/** 加载一批备案行的可用操作按钮（从 BPM 获取） */
async function loadRowAvailableActions(rows: DeclareFilingApi.Filing[]) {
  const results: Record<number, any[]> = { ...rowAvailableActions.value };

  // 过滤有效的业务ID
  const validRows = rows.filter((row) => row.id);
  if (validRows.length === 0) {
    return;
  }

  const businessIds = validRows.map((row) => row.id!);

  try {
    // 使用新的批量查询接口，根据 tableName + businessIds 批量查询
    const taskStatusList = await getTaskByBusiness({
      tableName: 'filing',
      businessIds,
    });

    console.log('[BPM] taskStatusList for filing:', taskStatusList);

    // 遍历结果，解析待办任务中的按钮配置
    for (const taskStatus of taskStatusList) {
      const businessId = taskStatus.businessId;

      if (taskStatus.hasTodoTask && taskStatus.todoTasks && taskStatus.todoTasks.length > 0) {
        // 取第一个待办任务
        const todoTask = taskStatus.todoTasks[0];
        const buttonsSetting = todoTask.buttonSettings || {};

        // 解析按钮配置
        // 后端返回的是 Map<Integer, ButtonSetting>，key 是按钮类型枚举值
        const actions: any[] = [];
        Object.entries(buttonsSetting).forEach(([type, config]: [string, any]) => {
          if (config && config.enable) {
            actions.push({
              key: type, // 使用按钮类型作为 key
              label: config.displayName || type,
              vars: {},
              taskId: todoTask.taskId,
              processInstanceId: taskStatus.processInstanceId,
              taskDefinitionKey: todoTask.taskDefinitionKey,
              // 额外的配置信息
              signEnable: todoTask.signEnable,
              reasonRequire: todoTask.reasonRequire,
            });
          }
        });

        results[businessId] = actions;
        console.log('[BPM] actions for filing:', businessId, actions);
      } else {
        // 没有待办任务，说明流程已结束或没有进行中的任务
        results[businessId] = [];
      }
    }
  } catch (e) {
    console.error('[BPM] 加载操作失败:', e);
  }

  console.log('[BPM] rowAvailableActions updated:', results);
  rowAvailableActions.value = { ...results };
}

/** 获取行操作按钮列表（处理退回状态的按钮文案） */
function getRowActions(row: DeclareFilingApi.Filing) {
  const actions = rowAvailableActions.value[row.id!] || [];

  const actionButtons = [
    // DRAFT 状态：显示"提交审核"按钮（只有创建者才能提交）
    ...(row.filingStatus === 'DRAFT' && row.creator === currentUserId.value
      ? [
          {
            label: '提交审核',
            type: 'link' as any,
            onClick: () => handleSubmitForApproval(row),
          },
        ]
      : []),
      
    // DSL 定义的流程操作按钮（如：提交、通过、退回等）
    ...actions.map((action: any) => ({
    // 退回状态：submit 改为"重新提交"（只有被退回后才显示"重新提交"，新提交显示"提交审核"）
    // isReturned=true 表示被退回后重新提交，isStartNode=true 表示发起节点（新提交）
    label: action.vars?.isReturned && (action.key === 'submit' || action.key === 'resubmit')
      ? '重新提交'
      : action.label,
      type: 'link' as any,
      onClick: () => handleBpmAction(row, action),
    })),
    {
      label: '审批详情',
      type: 'link' as any,
      onClick: () => handleViewApprovalDetail(row),
    },
    {
      label: $t('common.edit'),
      type: 'link' as any,
      icon: ACTION_ICON.EDIT,
      auth: ['declare:filing:update'],
      onClick: handleEdit.bind(null, row),
    },
    {
      label: $t('common.delete'),
      type: 'link' as any,
      danger: true,
      icon: ACTION_ICON.DELETE,
      auth: ['declare:filing:delete'],
      popConfirm: {
        title: $t('ui.actionMessage.deleteConfirm', [row.id]),
        confirm: handleDelete.bind(null, row),
      },
    },
  ];

  return actionButtons;
}

// 使用 BPM 操作 Hook
const {
  submitModalVisible,
  submitLoading,
  submitModalTitle,
  submitReasonLabel,
  submitReasonRequired,
  submitForm,
  handleBpmAction: handleBpmActionBase,
  executeBpmAction,
  handleSubmitConfirm: handleSubmitConfirmBase,
} = useBpmAction({
  businessType: BUSINESS_TYPE_KEY,
  businessIdGetter: () => currentSubmitRow.value?.id,
  onSuccess: () => {
    handleRefresh();
    if (currentSubmitRow.value) {
      loadRowAvailableActions([currentSubmitRow.value]);
    }
  },
});

// 当前操作的行数据
const currentSubmitRow = ref<DeclareFilingApi.Filing | null>(null);
const currentSubmitAction = ref<any>(null);

/** 执行 BPM 按钮操作 */
async function handleBpmAction(row: DeclareFilingApi.Filing, action: any) {
  currentSubmitRow.value = row;
  currentSubmitAction.value = action;
  const vars = action.vars || {};

  // BPM 原生按钮：approve/reject
  if (action.taskId) {
    await handleNativeBpmAction(row, action);
    return;
  }

  // 专家选择弹窗：先打开弹窗
  if (vars.expertMin !== undefined || vars.expertMax !== undefined) {
    const selectedCount = row.expertReviewerIds
      ? row.expertReviewerIds.split(',').filter(Boolean).length
      : 0;
    expertSelectModalApi.setData({
      expertMin: vars.expertMin,
      expertMax: vars.expertMax,
      processSelectedCount: selectedCount,
    });
    expertSelectModalApi.open();
    return;
  }

  // 其他操作（reason 弹窗或直接执行）由 Hook 处理
  await handleBpmActionBase(row, action);
}

/** 处理 BPM 原生按钮（approve/reject/transfer/delegate/addSign/return/copy/selectApprover 等） */
async function handleNativeBpmAction(row: DeclareFilingApi.Filing, action: any) {
  const { taskId, key, reasonRequire } = action;
  const buttonId = Number(key); // 将 key 转换为数字（1-8）

  // 解析 vars 中的额外配置
  const vars = action.vars || {};

  try {
    // 根据按钮ID处理不同的操作
    switch (buttonId) {
      case 1: // 通过 (APPROVE)
        await handleApproveAction(row, action);
        break;
      case 2: // 拒绝 (REJECT)
        await handleRejectAction(row, action);
        break;
      case 3: // 转办 (TRANSFER)
        await handleTransferAction(row, action);
        break;
      case 4: // 委派 (DELEGATE)
        await handleDelegateAction(row, action);
        break;
      case 5: // 加签 (ADD_SIGN)
        await handleAddSignAction(row, action);
        break;
      case 6: // 退回 (RETURN)
        await handleReturnAction(row, action);
        break;
      case 7: // 抄送 (COPY)
        await handleCopyAction(row, action);
        break;
      case 8: // 选择专家 (SELECT_APPROVER) - 使用专家选择弹窗
        await handleSelectApproverAction(row, action);
        break;
      default:
        message.warning(`未知的操作: ${buttonId}`);
    }
  } catch (e: any) {
    console.error('[BPM] 执行操作失败:', e);
    message.error(e.message || '操作失败');
  }
}

/** 处理通过操作 */
async function handleApproveAction(row: DeclareFilingApi.Filing, action: any) {
  const { taskId, reasonRequire } = action;
  const vars = action.vars || {};

  // 如果需要输入审批意见，打开弹窗
  if (reasonRequire) {
    // 使用弹窗让用户输入审批意见
    approvalFormData.value.taskId = taskId;
    approvalFormData.value.buttonId = 1;
    approvalFormData.value.reason = '';
    approvalFormData.value.title = vars.approveName || '通过';
    approvalFormData.value.reasonLabel = vars.reason || '审批意见';
    approvalFormData.value.reasonRequired = reasonRequire;
    approvalFormVisible.value = true;
    return;
  }

  // 直接提交
  await approveTask({ id: taskId, buttonId: 1, reason: '' });
  message.success('审批通过');
  await loadRowAvailableActions([row]);
}

/** 处理拒绝操作 */
async function handleRejectAction(row: DeclareFilingApi.Filing, action: any) {
  const { taskId, reasonRequire } = action;
  const vars = action.vars || {};

  // 拒绝操作通常需要输入原因
  approvalFormData.value.taskId = taskId;
  approvalFormData.value.buttonId = 2;
  approvalFormData.value.reason = '';
  approvalFormData.value.title = vars.rejectName || '拒绝';
  approvalFormData.value.reasonLabel = vars.reason || '拒绝原因';
  approvalFormData.value.reasonRequired = reasonRequire !== false; // 拒绝原因默认必填
  approvalFormVisible.value = true;
}

/** 处理转办操作 */
async function handleTransferAction(row: DeclareFilingApi.Filing, action: any) {
  const { taskId, reasonRequire } = action;
  const vars = action.vars || {};

  // 打开用户选择弹窗
  userSelectData.value.taskId = taskId;
  userSelectData.value.buttonId = 3;
  userSelectData.value.title = vars.transferName || '转办';
  userSelectData.value.multiple = false;
  userSelectData.value.reasonRequired = reasonRequire !== false;
  userSelectModalApi.setData({
    title: vars.transferName || '选择转办人',
    multiple: false,
  }).open();
}

/** 处理委派操作 */
async function handleDelegateAction(row: DeclareFilingApi.Filing, action: any) {
  const { taskId, reasonRequire } = action;
  const vars = action.vars || {};

  // 打开用户选择弹窗
  userSelectData.value.taskId = taskId;
  userSelectData.value.buttonId = 4;
  userSelectData.value.title = vars.delegateName || '委派';
  userSelectData.value.multiple = false;
  userSelectData.value.reasonRequired = reasonRequire !== false;
  userSelectModalApi.setData({
    title: vars.delegateName || '选择被委派人',
    multiple: false,
  }).open();
}

/** 处理加签操作 */
async function handleAddSignAction(row: DeclareFilingApi.Filing, action: any) {
  const { taskId, reasonRequire } = action;
  const vars = action.vars || {};

  // 打开用户选择弹窗（加签可以多人）
  userSelectData.value.taskId = taskId;
  userSelectData.value.buttonId = 5;
  userSelectData.value.title = vars.addSignName || '加签';
  userSelectData.value.multiple = true;
  userSelectData.value.reasonRequired = reasonRequire !== false;
  userSelectModalApi.setData({
    title: vars.addSignName || '选择加签人',
    multiple: true,
  }).open();
}

/** 处理退回操作 */
async function handleReturnAction(row: DeclareFilingApi.Filing, action: any) {
  const { taskId, reasonRequire } = action;
  const vars = action.vars || {};

  // 先获取可退回的节点列表
  try {
    const returnNodes = await getTaskListByReturn(taskId);
    if (!returnNodes || returnNodes.length === 0) {
      message.warning('没有可退回的节点');
      return;
    }

    // 打开退回弹窗
    returnData.value.taskId = taskId;
    returnData.value.buttonId = 6;
    returnData.value.title = vars.returnName || '退回';
    returnData.value.reasonRequired = reasonRequire !== false;
    returnData.value.nodes = returnNodes.map((node: any) => ({
      key: node.taskDefinitionKey,
      label: node.name,
    }));
    returnModalVisible.value = true;
  } catch (e: any) {
    console.error('[BPM] 获取可退回节点失败:', e);
    // 根据错误类型给出不同提示
    if (e.response?.status === 403 || e.response?.data?.code === 403) {
      message.error('您没有执行此操作的权限，请联系管理员');
    } else {
      message.error('获取可退回节点失败');
    }
  }
}

/** 处理抄送操作 */
async function handleCopyAction(row: DeclareFilingApi.Filing, action: any) {
  const { taskId, reasonRequire } = action;
  const vars = action.vars || {};

  // 打开用户选择弹窗（抄送可以多人）
  userSelectData.value.taskId = taskId;
  userSelectData.value.buttonId = 7;
  userSelectData.value.title = vars.copyName || '抄送';
  userSelectData.value.multiple = true;
  userSelectData.value.reasonRequired = reasonRequire;
  userSelectModalApi.setData({
    title: vars.copyName || '选择抄送人',
    multiple: true,
  }).open();
}

/** 处理选择专家操作 */
async function handleSelectApproverAction(row: DeclareFilingApi.Filing, action: any) {
  const { taskId } = action;
  const vars = action.vars || {};

  // 使用已有的专家选择弹窗
  const selectedCount = row.expertReviewerIds
    ? row.expertReviewerIds.split(',').filter(Boolean).length
    : 0;
  expertSelectModalApi.setData({
    expertMin: vars.expertMin,
    expertMax: vars.expertMax,
    processSelectedCount: selectedCount,
  }).open();
}

/** 专家选择弹窗引用 */
const [ExpertSelectModal, expertSelectModalApi] = useVbenModal({
  connectedComponent: ExpertSelectModalCmp,
  destroyOnClose: true,
});

/** 用户选择弹窗引用 */
const [UserSelectModal, userSelectModalApi] = useVbenModal({
  connectedComponent: UserSelectModalCmp,
  destroyOnClose: true,
  onConfirm: async () => {
    // UserSelectModal 自己处理确认，这里不做额外处理
    return false;
  },
});

/** 审批表单数据类型 */
interface ApprovalFormData {
  taskId: string;
  buttonId: number;
  reason: string;
  title: string;
  reasonLabel: string;
  reasonRequired: boolean;
}

/** 审批表单数据 */
const approvalFormData = ref<ApprovalFormData>({
  taskId: '',
  buttonId: 0,
  reason: '',
  title: '审批',
  reasonLabel: '审批意见',
  reasonRequired: false,
});
const approvalFormVisible = ref(false);

/** 退回弹窗显示状态 */
const returnModalVisible = ref(false);

/** 审批表单确认处理 */
async function handleApprovalFormConfirm() {
  const { taskId, buttonId, reason } = approvalFormData.value;

  if (approvalFormData.value.reasonRequired && !reason?.trim()) {
    message.warning(`请输入${approvalFormData.value.reasonLabel}`);
    return;
  }

  try {
    if (buttonId === 1) {
      // 通过
      await approveTask({ id: taskId, buttonId, reason });
      message.success('审批通过');
    } else if (buttonId === 2) {
      // 拒绝
      await rejectTask({ id: taskId, reason });
      message.success('已拒绝');
    }
    approvalFormVisible.value = false;
    // 刷新操作按钮
    if (currentSubmitRow.value) {
      await loadRowAvailableActions([currentSubmitRow.value]);
    }
  } catch (e: any) {
    console.error('[BPM] 审批操作失败:', e);
    message.error(e.message || '操作失败');
  }
}

/** 用户选择弹窗数据类型 */
interface UserSelectData {
  taskId: string;
  buttonId: number;
  title: string;
  multiple: boolean;
  reasonRequired: boolean;
}

/** 用户选择弹窗数据 */
const userSelectData = ref<UserSelectData>({
  taskId: '',
  buttonId: 0,
  title: '选择用户',
  multiple: false,
  reasonRequired: false,
});

/** 确认用户选择后处理 */
async function handleUserSelectConfirm(users: any[], reason: string) {
  const { taskId, buttonId, reasonRequired } = userSelectData.value;

  if (reasonRequired && !reason?.trim()) {
    message.warning('请输入操作原因');
    return;
  }

  if (users.length === 0) {
    message.warning('请选择用户');
    return;
  }

  try {
    switch (buttonId) {
      case 3: // 转办
        await transferTask({
          id: taskId,
          assigneeUserId: users[0].id,
          reason,
        });
        message.success('转办成功');
        break;
      case 4: // 委派
        await delegateTask({
          id: taskId,
          delegateUserId: users[0].id,
          reason,
        });
        message.success('委派成功');
        break;
      case 5: // 加签
        await signCreateTask({
          id: taskId,
          userIds: users.map((u) => u.id),
          type: 'after', // 默认后加签
          reason,
        });
        message.success('加签成功');
        break;
      case 7: // 抄送
        await copyTask({
          id: taskId,
          copyUserIds: users.map((u) => u.id),
          reason,
        });
        message.success('抄送成功');
        break;
      default:
        message.warning(`未知的操作: ${buttonId}`);
        return;
    }

    // 刷新操作按钮
    if (currentSubmitRow.value) {
      await loadRowAvailableActions([currentSubmitRow.value]);
    }
  } catch (e: any) {
    console.error('[BPM] 操作失败:', e);
    message.error(e.message || '操作失败');
  }
}

/** 退回弹窗数据类型 */
interface ReturnData {
  taskId: string;
  buttonId: number;
  title: string;
  reasonRequired: boolean;
  targetTaskDefinitionKey: string;
  reason: string;
  nodes: { key: string; label: string }[];
}

/** 退回弹窗数据 */
const returnData = ref<ReturnData>({
  taskId: '',
  buttonId: 0,
  title: '退回',
  reasonRequired: false,
  targetTaskDefinitionKey: '',
  reason: '',
  nodes: [],
});

/** 用户选择弹窗确认处理 */
async function handleUserSelectModalConfirm(users: any[], reason: string) {
  const { taskId, buttonId } = userSelectData.value;

  try {
    switch (buttonId) {
      case 3: // 转办
        await transferTask({
          id: taskId,
          assigneeUserId: users[0].id,
          reason,
        });
        message.success('转办成功');
        break;
      case 4: // 委派
        await delegateTask({
          id: taskId,
          delegateUserId: users[0].id,
          reason,
        });
        message.success('委派成功');
        break;
      case 5: // 加签
        await signCreateTask({
          id: taskId,
          userIds: users.map((u) => u.id),
          type: 'after', // 默认后加签
          reason,
        });
        message.success('加签成功');
        break;
      case 7: // 抄送
        await copyTask({
          id: taskId,
          copyUserIds: users.map((u) => u.id),
          reason,
        });
        message.success('抄送成功');
        break;
      default:
        message.warning(`未知的操作: ${buttonId}`);
        return;
    }

    // 刷新操作按钮
    if (currentSubmitRow.value) {
      await loadRowAvailableActions([currentSubmitRow.value]);
    }
  } catch (e: any) {
    console.error('[BPM] 操作失败:', e);
    message.error(e.message || '操作失败');
  }
}

/** 退回确认处理 */
async function handleReturnConfirm() {
  const { taskId, targetTaskDefinitionKey, reason, reasonRequired } = returnData.value;

  if (reasonRequired && !reason?.trim()) {
    message.warning('请输入退回原因');
    return;
  }

  if (!targetTaskDefinitionKey) {
    message.warning('请选择退回节点');
    return;
  }

  try {
    await returnTask({
      id: taskId,
      targetTaskDefinitionKey,
      reason,
    });
    message.success('退回成功');

    // 关闭弹窗
    returnModalVisible.value = false;

    // 刷新表格数据
    handleRefresh();

    // 刷新操作按钮
    if (currentSubmitRow.value) {
      await loadRowAvailableActions([currentSubmitRow.value]);
    }
  } catch (e: any) {
    console.error('[BPM] 退回失败:', e);
    message.error(e.message || '操作失败');
  }
}

/** 审批详情弹窗 - 使用 a-modal 直接控制宽度 */
const approvalDetailVisible = ref(false);
const approvalDetailRef = ref<InstanceType<typeof ApprovalDetailCmp> | null>(null);

/** 审批操作相关状态 */
const currentApprovalActions = ref<any[]>([]);
const currentApprovalTaskInfo = ref<{ taskId?: string; processInstanceId?: string }>({});
const currentApprovalBusinessId = ref<number | null>(null);
const currentApprovalRow = ref<DeclareFilingApi.Filing | null>(null);

/** 打开审批详情弹窗 */
async function handleViewApprovalDetail(row: DeclareFilingApi.Filing) {
  approvalDetailVisible.value = true;
  // 保存当前行数据
  currentApprovalRow.value = row;
  // 保存当前业务 ID
  currentApprovalBusinessId.value = row.id ?? null;
  // 组件现在从 props 读取 tableName 和 businessId，自动加载数据
  // 仍然需要获取操作按钮供 ActionButtonCmp 使用
  nextTick(async () => {
    // 使用和列表页相同的方式获取可用操作
    try {
      const result = await getAvailableActions(BUSINESS_TYPE_KEY, row.id!);
      // 新接口返回格式：{ actions: {...}, taskInfo: {...} }
      if (result && typeof result === 'object' && 'actions' in result) {
        currentApprovalActions.value = result.actions;
        currentApprovalTaskInfo.value = result.taskInfo || {};
      } else {
        // 兼容旧格式（数组）
        currentApprovalActions.value = (result || []).filter(
          (a: any) => a.key !== '_PROCESS_RUNNING_' && a.key !== '_PROCESS_FINISHED_'
        );
        currentApprovalTaskInfo.value = {};
      }
    } catch (e) {
      console.error('获取审批操作失败:', e);
      currentApprovalActions.value = [];
      currentApprovalTaskInfo.value = {};
    }
  });
}

/** 处理审批操作成功 */
function handleApprovalActionSuccess() {
  approvalDetailVisible.value = false;
  handleRefresh();
}

/** 处理审批操作刷新（选择专家后刷新数据） */
function handleApprovalActionRefresh() {
  // 刷新审批详情数据，但不关闭弹窗
  if (approvalDetailRef.value) {
    approvalDetailRef.value.openWithData(currentApprovalRow.value as DeclareFilingApi.Filing);
  }
  // 刷新列表中的操作按钮
  if (currentApprovalRow.value) {
    loadRowAvailableActions([currentApprovalRow.value]);
  }
}

/** 处理专家选择确认 */
async function handleExpertSelectConfirm(experts: DeclareExpertApi.Expert[]) {
  if (!currentSubmitRow.value || !currentSubmitAction.value) {
    return;
  }

  // 提取专家关联的系统用户ID
  const expertUserIds = experts
    .map((e) => e.userId)
    .filter((userId): userId is number => userId !== undefined && userId !== null);

  if (expertUserIds.length === 0) {
    message.error('所选专家未关联系统用户，无法提交');
    return;
  }

  // 使用 Hook 的 executeBpmAction 方法提交
  await executeBpmAction(currentSubmitRow.value, currentSubmitAction.value, {
    expertUserIds,
    reason: '',
  });
}

// executeBpmAction 已经在 Hook 中定义并导出，这里不需要再定义

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 创建项目备案核心信息 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑项目备案核心信息 */
function handleEdit(row: DeclareFilingApi.Filing) {
  formModalApi.setData(row).open();
}

/** 删除项目备案核心信息 */
async function handleDelete(row: DeclareFilingApi.Filing) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.id]),
    duration: 0,
  });
  try {
    await deleteFiling(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.id]));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

/** 提交审核 */
async function handleSubmitForApproval(row: DeclareFilingApi.Filing) {
  try {
    await confirm('确定要提交审核吗？提交后将进入审批流程。');
    const hideLoading = message.loading({
      content: '提交审核中...',
      duration: 0,
    });
    try {
      await submitFiling(row.id!);
      message.success('提交审核成功');
      handleRefresh();
    } finally {
      hideLoading();
    }
  } catch (e) {
    // 用户取消不处理
  }
}

/** 批量删除项目备案核心信息 */
async function handleDeleteBatch() {
  await confirm($t('ui.actionMessage.deleteBatchConfirm'));
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deletingBatch'),
    duration: 0,
  });
  try {
    await deleteFilingList(checkedIds.value);
    checkedIds.value = [];
    message.success($t('ui.actionMessage.deleteSuccess'));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({
  records,
}: {
  records: DeclareFilingApi.Filing[];
}) {
  checkedIds.value = records.map((item) => item.id!);
}

// 格式化指标值显示
function formatIndicatorValue(row: DeclareFilingApi.Filing, indicatorCode: string) {
  const indicatorValues = row.indicatorValues || {};
  const value = indicatorValues[indicatorCode];
  if (value === null || value === undefined || value === '') {
    return '无';
  }

  // 查找对应的指标配置
  const indicator = listDisplayIndicators.value.find(
    (ind) => ind.indicatorCode === indicatorCode,
  );

  // 处理日期区间
  if (typeof value === 'object' && value !== null && 'start' in value && 'end' in value) {
    return `${value.start} ~ ${value.end}`;
  }

  // 处理布尔值
  if (typeof value === 'boolean') {
    return value ? '是' : '否';
  }

  // 处理单选(6)和多选(7)类型，需要将值转换为标签
  if (indicator && (indicator.valueType === 6 || indicator.valueType === 7)) {
    const valueOptions = indicator.valueOptions;
    if (valueOptions) {
      try {
        // 解析 valueOptions：可能是 JSON 字符串或对象
        let options: Array<{ value: string; label: string }> = [];
        if (typeof valueOptions === 'string') {
          options = JSON.parse(valueOptions);
        } else if (Array.isArray(valueOptions)) {
          options = valueOptions;
        }

        // 创建值到标签的映射
        const valueToLabelMap = new Map<string, string>();
        options.forEach((opt) => {
          valueToLabelMap.set(String(opt.value), opt.label);
        });

        // 单选类型：直接转换单个值
        if (indicator.valueType === 6) {
          return valueToLabelMap.get(String(value)) || String(value);
        }

        // 多选类型：转换逗号分隔的多个值
        if (indicator.valueType === 7) {
          const valueArray = String(value).split(',');
          const labels = valueArray
            .map((v) => valueToLabelMap.get(v.trim()) || v.trim())
            .join(', ');
          return labels || String(value);
        }
      } catch (e) {
        console.error('解析 valueOptions 失败:', e);
      }
    }
  }

  return String(value);
}

/** 导出表格 */
async function handleExport() {
  const data = await exportFiling(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '项目备案核心信息.xls', source: data });
}

// 页面加载时获取列表显示指标
onMounted(() => {
  loadListDisplayIndicators();
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  showSearchForm: false,
  gridOptions: {
    columns: allColumns.value,
    height: 'auto',
    keepSource: true,
    proxyConfig: {
        ajax: {
          query: async ({ page }, formValues) => {
            const result = await getFilingPage({
              pageNo: page.currentPage,
              pageSize: page.pageSize,
              ...formValues,
            });
            // 异步加载每行的可用操作按钮，不阻塞列表渲染
            if (result?.list?.length) {
              loadRowAvailableActions(result.list);
            }
            return result;
          },
        },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<DeclareFilingApi.Filing>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});

</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="项目备案核心信息列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['项目备案核心信息']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['declare:filing:create'],
              onClick: handleCreate,
            },
            {
              label: $t('ui.actionTitle.export'),
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['declare:filing:export'],
              onClick: handleExport,
            },
            {
              label: $t('ui.actionTitle.deleteBatch'),
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['declare:filing:delete'],
              disabled: isEmpty(checkedIds),
              onClick: handleDeleteBatch,
            },
          ]"
        />
      </template>
      <!-- 指标值插槽 -->
      <template #indicator_value="{ row, column }">
        {{ formatIndicatorValue(row, column.params?.indicatorCode) }}
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="getRowActions(row)"
        />
      </template>
    </Grid>

    <!-- 提交审核弹窗 -->
    <a-modal
      v-model:open="submitModalVisible"
      :title="submitModalTitle"
      :confirm-loading="submitLoading"
      @ok="handleSubmitConfirmBase"
    >
      <a-form layout="vertical">
        <a-form-item :label="submitReasonLabel" :required="submitReasonRequired">
          <a-textarea
            v-model:value="submitForm.reason"
            :placeholder="submitReasonRequired ? `请输入${submitReasonLabel}` : `请输入${submitReasonLabel}（可选）`"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 选择专家弹窗 -->
    <ExpertSelectModal @confirm="handleExpertSelectConfirm" />

    <!-- 用户选择弹窗（用于转办、委派、加签、抄送） -->
    <UserSelectModal @confirm="handleUserSelectModalConfirm" />

    <!-- 审批表单弹窗（用于通过/拒绝） -->
    <a-modal
      v-model:open="approvalFormVisible"
      :title="approvalFormData.title"
      width="500px"
      @ok="handleApprovalFormConfirm"
    >
      <a-form layout="vertical">
        <a-form-item
          :label="approvalFormData.reasonLabel"
          :required="approvalFormData.reasonRequired"
        >
          <a-textarea
            v-model:value="approvalFormData.reason"
            :placeholder="`请输入${approvalFormData.reasonLabel}`"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 退回弹窗 -->
    <a-modal
      v-model:open="returnModalVisible"
      :title="returnData.title"
      width="500px"
      @ok="handleReturnConfirm"
    >
      <a-form layout="vertical">
        <a-form-item label="退回节点" required>
          <a-select
            v-model:value="returnData.targetTaskDefinitionKey"
            placeholder="请选择退回节点"
          >
            <a-select-option
              v-for="node in returnData.nodes"
              :key="node.key"
              :value="node.key"
            >
              {{ node.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="returnData.reasonRequired ? '退回原因' : '退回原因（可选）'">
          <a-textarea
            v-model:value="returnData.reason"
            placeholder="请输入退回原因"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 审批详情弹窗 - 使用 a-modal 直接控制宽度 -->
    <a-modal
      v-model:open="approvalDetailVisible"
      :title="`备案详情`"
      :footer="null"
      width="90%"
      :body-style="{ minHeight: '80vh', padding: '16px', paddingBottom: '80px', position: 'relative' }"
      :z-index="1000"
      destroy-on-close
    >
      <ApprovalDetailCmp
        ref="approvalDetailRef"
        class="approval-detail-content-wrapper"
        :table-name="BUSINESS_TYPE"
        :business-id="currentApprovalBusinessId"
        :show-actions="false"
        @success="handleRefresh"
      />

      <!-- 审批操作按钮 - 使用通用组件 -->
      <div class="approval-action-bar">
        <ActionButtonCmp
          v-if="currentApprovalBusinessId"
          :business-type="BUSINESS_TYPE_KEY"
          :business-id="currentApprovalBusinessId"
          :actions="currentApprovalActions"
          :task-info="currentApprovalTaskInfo"
          @success="handleApprovalActionSuccess"
          @refresh="handleApprovalActionRefresh"
        />
        <span v-else class="no-action-tip">当前无可用审批操作</span>
      </div>
    </a-modal>
  </Page>
</template>

<style lang="scss" scoped>
/* 审批详情弹窗底部操作栏 */
.approval-action-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  background: white;
  border-top: 1px solid var(--color-border, #d9d9d9);
  z-index: 10;
}

.approval-action-btn {
  min-width: 100px;
  height: 36px;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 6px;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }

  :deep(.anticon) {
    font-size: 14px;
  }
}

.no-action-tip {
  color: var(--color-text-quaternary, #bfbfbf);
  font-size: 14px;
}
</style>