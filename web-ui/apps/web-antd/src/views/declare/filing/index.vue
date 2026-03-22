<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareFilingApi } from '#/api/declare/filing';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';

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
import { getTaskByBusiness } from '#/api/bpm/task';
import { submitBpmAction } from '#/api/bpm/action';
import { $t } from '#/locales';

import ExpertSelectModal from '#/components/bpm/ExpertSelectModal.vue';
import UserSelectModalCmp from '#/components/bpm/UserSelectModal.vue';
import ApprovalModal from '#/components/bpm/ApprovalModal.vue';
import ReturnModal from '#/components/bpm/ReturnModal.vue';
import { IconifyIcon } from '@vben/icons';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

import ApprovalDetailCmp from './modules/ApprovalDetail.vue';
import { OperationButtonType } from '#/views/bpm/components/simple-process-design/consts';
import type { DeclareExpertApi } from '#/api/declare/expert';

// 业务类型
const BUSINESS_TYPE = 'filing';

// 当前登录用户ID
const userStore = useUserStore();
const currentUserId = computed(() => {
  return Number(userStore.userInfo?.userId) || userStore.userInfo?.id;
});

// 每行的可用操作缓存
const rowAvailableActions = ref<Record<number, any[]>>({});

// 需要在列表显示的指标
const listDisplayIndicators = shallowRef<DeclareIndicatorApi.Indicator[]>([]);

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
    const cols = allColumns.value;
    nextTick(async () => {
      if (gridApi?.setGridOptions) {
        await gridApi.setGridOptions({ columns: cols });
      }
      await gridApi?.query();
    });
  } catch (error) {
    console.error('加载列表显示指标失败:', error);
    listDisplayIndicators.value = [];
  }
}

// 获取动态列
function getDynamicColumns() {
  if (!listDisplayIndicators.value || listDisplayIndicators.value.length === 0) {
    return [];
  }

  return listDisplayIndicators.value.map((indicator) => ({
    field: `indicator_${indicator.indicatorCode}`,
    title: indicator.indicatorName,
    minWidth: 120,
    slots: { default: 'indicator_value' },
    params: { indicatorCode: indicator.indicatorCode },
  }));
}

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
  class: '!min-w-[90%] !min-h-[80vh]',
  closeOnClickModal: false,
});

// 加载一批备案行的可用操作按钮
async function loadRowAvailableActions(rows: DeclareFilingApi.Filing[]) {
  const results: Record<number, any[]> = { ...rowAvailableActions.value };

  const validRows = rows.filter((row) => row.id);
  if (validRows.length === 0) {
    return;
  }

  const businessIds = validRows.map((row) => row.id!);

  try {
    const taskStatusList = await getTaskByBusiness({
      tableName: BUSINESS_TYPE,
      businessIds,
    });

    for (const taskStatus of taskStatusList) {
      const businessId = taskStatus.businessId;

      if (taskStatus.hasTodoTask && taskStatus.todoTasks && taskStatus.todoTasks.length > 0) {
        const todoTask = taskStatus.todoTasks[0];
        if (!todoTask) {
          results[businessId] = [];
          continue;
        }
        const buttonsSetting = todoTask.buttonSettings || {};

        const actions: any[] = [];
        Object.entries(buttonsSetting).forEach(([type, config]: [string, any]) => {
          if (config && config.enable) {
            actions.push({
              key: type,
              label: config.displayName || type,
              vars: {},
              taskId: todoTask.taskId,
              processInstanceId: taskStatus.processInstanceId,
              taskDefinitionKey: todoTask.taskDefinitionKey,
            });
          }
        });

        results[businessId] = actions;
      } else {
        results[businessId] = [];
      }
    }
  } catch (e) {
    console.error('[BPM] 加载操作失败:', e);
  }

  rowAvailableActions.value = { ...results };
}

// 获取行操作按钮列表
function getRowActions(row: DeclareFilingApi.Filing) {
  const actions = rowAvailableActions.value[row.id!] || [];

  const actionButtons = [
    // DRAFT 状态：显示"提交审核"按钮
    ...(row.filingStatus === 'DRAFT' && row.creator === currentUserId.value
      ? [
          {
            label: '提交审核',
            type: 'link' as any,
            onClick: () => handleSubmitForApproval(row),
          },
        ]
      : []),
    // 流程操作按钮 - 点击直接执行操作
    ...actions.map((action: any) => ({
      label: action.label,
      type: 'link' as any,
      onClick: () => handleRowActionClick(row, action),
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

// 处理列表行操作按钮点击 - 直接执行操作
function handleRowActionClick(row: DeclareFilingApi.Filing, action: any) {
  // 设置当前操作相关的状态
  currentApprovalRow.value = row;
  currentApprovalBusinessId.value = row.id ?? null;
  currentApprovalTaskInfo.value = {
    taskId: action.taskId,
    processInstanceId: action.processInstanceId,
  };
  currentApprovalAction.value = action;

  const taskId = action.taskId;
  const buttonType = Number(action.key);

  // 根据操作类型打开对应的弹窗
  if (buttonType === OperationButtonType.APPROVE) {
    approvalModalRef.value?.open({
      action: 'approve',
      title: action.label || '通过',
    });
    return;
  }

  if (buttonType === OperationButtonType.REJECT) {
    approvalModalRef.value?.open({
      action: 'reject',
      title: action.label || '拒绝',
    });
    return;
  }

  if (buttonType === OperationButtonType.RETURN) {
    if (taskId) {
      returnModalRef.value?.open({
        taskId,
        title: action.label || '退回',
      });
    }
    return;
  }

  if (
    buttonType === OperationButtonType.TRANSFER ||
    buttonType === OperationButtonType.DELEGATE ||
    buttonType === OperationButtonType.ADD_SIGN
  ) {
    userSelectModalRef.value?.open();
    return;
  }

  if (buttonType === OperationButtonType.SELECT_APPROVER) {
    openExpertSelectModal();
    return;
  }

  message.info('该操作暂不支持');
}

// ========== 审批详情弹窗相关 ==========

const approvalDetailVisible = ref(false);
const approvalDetailRef = ref<InstanceType<typeof ApprovalDetailCmp> | null>(null);

const currentApprovalActions = ref<any[]>([]);
const currentApprovalTaskInfo = ref<{ taskId?: string; processInstanceId?: string }>({});
const currentApprovalBusinessId = ref<number | null>(null);
const currentApprovalRow = ref<DeclareFilingApi.Filing | null>(null);
const currentApprovalAction = shallowRef<any>(null);

// 弹窗 refs
const approvalModalRef = ref<{
  open: (data: { action?: 'approve' | 'reject'; title?: string; reason?: string }) => void;
} | null>(null);

const returnModalRef = ref<{
  open: (data: { taskId: string; title?: string }) => void;
} | null>(null);

const userSelectModalRef = ref<{
  open: () => void;
} | null>(null);

// ========== 专家选择弹窗相关 ==========
const expertSelectVisible = ref(false);

// 打开专家选择弹窗
function openExpertSelectModal() {
  expertSelectVisible.value = true;
}

// 专家选择确认
async function handleExpertConfirm(experts: DeclareExpertApi.Expert[]) {
  if (!currentApprovalAction.value || !currentApprovalBusinessId.value) return;

  // 注意：需要传 userId（系统用户ID），不是 expert 的 id（专家表主键）
  const expertUserIds = experts.map(e => e.userId);
  const taskId = currentApprovalTaskInfo.value.taskId;

  try {
    await submitBpmAction({
      businessType: BUSINESS_TYPE,
      businessId: currentApprovalBusinessId.value,
      actionKey: currentApprovalAction.value.key,
      expertUserIds,
      taskId,
    });
    message.success(`已选择 ${experts.length} 位专家`);
    expertSelectVisible.value = false;
    handleApprovalActionSuccess();
    await loadRowAvailableActions([currentApprovalRow.value!]);
  } catch (error: any) {
    console.error('选择专家失败:', error);
    message.error(error.message || '选择专家失败');
  }
}

// 打开审批详情弹窗
async function handleViewApprovalDetail(row: DeclareFilingApi.Filing) {
  approvalDetailVisible.value = true;
  currentApprovalRow.value = row;
  currentApprovalBusinessId.value = row.id ?? null;

  nextTick(async () => {
    try {
      const taskStatusList = await getTaskByBusiness({
        tableName: BUSINESS_TYPE,
        businessIds: [row.id!],
      });

      const taskStatus = taskStatusList?.[0];
      if (taskStatus?.hasTodoTask && taskStatus.todoTasks?.[0]) {
        const todoTask = taskStatus.todoTasks[0];
        const buttonsSetting = todoTask.buttonSettings || {};

        const actions: any[] = [];
        Object.entries(buttonsSetting).forEach(([type, config]: [string, any]) => {
          if (config && config.enable) {
            actions.push({
              key: type,
              label: config.displayName,
              vars: {},
              taskId: todoTask.taskId,
              processInstanceId: taskStatus.processInstanceId,
              taskDefinitionKey: todoTask.taskDefinitionKey,
            });
          }
        });

        currentApprovalActions.value = actions;
        currentApprovalTaskInfo.value = {
          taskId: todoTask.taskId,
          processInstanceId: taskStatus.processInstanceId,
        };
      } else {
        currentApprovalActions.value = [];
        currentApprovalTaskInfo.value = {};
      }
    } catch (e) {
      console.error('获取审批操作失败:', e);
      currentApprovalActions.value = [];
      currentApprovalTaskInfo.value = {};
    }
  });
}

// 审批操作成功
function handleApprovalActionSuccess() {
  approvalDetailVisible.value = false;
  handleRefresh();
}

// 审批操作刷新
function handleApprovalActionRefresh() {
  if (approvalDetailRef.value) {
    approvalDetailRef.value.openWithData(currentApprovalRow.value as DeclareFilingApi.Filing);
  }
  if (currentApprovalRow.value) {
    loadRowAvailableActions([currentApprovalRow.value]);
  }
}

// 获取操作按钮类型
function getActionBtnType(key: string): 'primary' | 'default' | 'danger' {
  if (key === '1') return 'primary';
  if (key === '2') return 'danger';
  return 'default';
}

// 获取操作按钮图标
function getActionBtnIcon(key: string): string {
  const iconMap: Record<string, string> = {
    '1': 'ep:circle-check',
    '2': 'ep:circle-close',
    '3': 'ep:right',
    '4': 'ep:user',
    '5': 'ep:document-add',
    '6': 'ep:back',
    '8': 'ep:user',
  };
  return iconMap[key] || 'ep:more-filled';
}

// 处理审批操作点击
function handleApprovalActionClick(action: any) {
  const taskId = currentApprovalTaskInfo.value.taskId;
  const buttonType = Number(action.key);

  // 通过
  if (buttonType === OperationButtonType.APPROVE) {
    currentApprovalAction.value = action;
    approvalModalRef.value?.open({
      action: 'approve',
      title: action.label || '通过',
    });
    return;
  }

  // 拒绝
  if (buttonType === OperationButtonType.REJECT) {
    currentApprovalAction.value = action;
    approvalModalRef.value?.open({
      action: 'reject',
      title: action.label || '拒绝',
    });
    return;
  }

  // 退回
  if (buttonType === OperationButtonType.RETURN) {
    if (taskId) {
      returnModalRef.value?.open({
        taskId,
        title: action.label || '退回',
      });
    }
    return;
  }

  // 转办、委派、加签
  if (
    buttonType === OperationButtonType.TRANSFER ||
    buttonType === OperationButtonType.DELEGATE ||
    buttonType === OperationButtonType.ADD_SIGN
  ) {
    currentApprovalAction.value = action;
    userSelectModalRef.value?.open();
    return;
  }

  // 选择专家
  if (buttonType === OperationButtonType.SELECT_APPROVER) {
    openExpertSelectModal();
    return;
  }

  message.info('该操作暂不支持');
}

// 审批弹窗确认
async function handleApprovalModalConfirm(reason: string, _action: 'approve' | 'reject') {
  if (!currentApprovalAction.value || !currentApprovalBusinessId.value) return;

  try {
    await submitBpmAction({
      businessType: BUSINESS_TYPE,
      businessId: currentApprovalBusinessId.value,
      actionKey: currentApprovalAction.value.key,
      reason,
      taskId: currentApprovalTaskInfo.value.taskId,
    });
    message.success('操作成功');
    handleApprovalActionSuccess();
    await loadRowAvailableActions([currentApprovalRow.value!]);
  } catch (error: any) {
    console.error('操作失败:', error);
    message.error(error.message || '操作失败');
  }
}

// 用户选择确认
async function handleApprovalUserSelectConfirm(_users: any[], reason: string) {
  if (!currentApprovalAction.value || !currentApprovalBusinessId.value) return;

  try {
    await submitBpmAction({
      businessType: BUSINESS_TYPE,
      businessId: currentApprovalBusinessId.value,
      actionKey: currentApprovalAction.value.key,
      reason,
      taskId: currentApprovalTaskInfo.value.taskId,
    });
    message.success('操作成功');
    handleApprovalActionSuccess();
    await loadRowAvailableActions([currentApprovalRow.value!]);
  } catch (error: any) {
    console.error('操作失败:', error);
    message.error(error.message || '操作失败');
  }
}

// 退回确认
function handleApprovalReturnConfirm() {
  handleApprovalActionRefresh();
}

// ========== 其他功能 ==========

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData(null).open();
}

function handleEdit(row: DeclareFilingApi.Filing) {
  formModalApi.setData(row).open();
}

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

  const indicator = listDisplayIndicators.value.find(
    (ind) => ind.indicatorCode === indicatorCode,
  );

  // 日期区间
  if (typeof value === 'object' && value !== null && 'start' in value && 'end' in value) {
    return `${value.start} ~ ${value.end}`;
  }

  // 布尔值
  if (typeof value === 'boolean') {
    return value ? '是' : '否';
  }

  // 单选(6)和多选(7)类型
  if (indicator && (indicator.valueType === 6 || indicator.valueType === 7)) {
    const valueOptions = indicator.valueOptions;
    if (valueOptions) {
      try {
        let options: Array<{ value: string; label: string }> = [];
        if (typeof valueOptions === 'string') {
          options = JSON.parse(valueOptions);
        } else if (Array.isArray(valueOptions)) {
          options = valueOptions;
        }

        const valueToLabelMap = new Map<string, string>();
        options.forEach((opt) => {
          valueToLabelMap.set(String(opt.value), opt.label);
        });

        if (indicator.valueType === 6) {
          return valueToLabelMap.get(String(value)) || String(value);
        }

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

async function handleExport() {
  const data = await exportFiling(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '项目备案核心信息.xls', source: data });
}

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

    <!-- 审批详情弹窗 -->
    <a-modal
      v-model:open="approvalDetailVisible"
      title="备案详情"
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

      <!-- 审批操作按钮 -->
      <div class="approval-action-bar">
        <template v-if="currentApprovalBusinessId && currentApprovalActions.length">
          <a-button
            v-for="action in currentApprovalActions"
            :key="action.key"
            :type="getActionBtnType(action.key)"
            size="large"
            @click="handleApprovalActionClick(action)"
          >
            <template #icon>
              <IconifyIcon :icon="getActionBtnIcon(action.key)" />
            </template>
            {{ action.label }}
          </a-button>
        </template>
        <span v-else class="no-action-tip">当前无可用审批操作</span>
      </div>
    </a-modal>

    <!-- 审批弹窗（通过/拒绝） -->
    <ApprovalModal
      ref="approvalModalRef"
      @confirm="handleApprovalModalConfirm"
    />

    <!-- 退回弹窗 -->
    <ReturnModal
      ref="returnModalRef"
      @success="handleApprovalReturnConfirm"
    />

    <!-- 用户选择弹窗（转办/委派/加签） -->
    <UserSelectModalCmp
      ref="userSelectModalRef"
      :show-reason="true"
      reason-label="操作原因"
      @confirm="handleApprovalUserSelectConfirm"
    />

    <!-- 专家选择弹窗 -->
    <ExpertSelectModal
      v-model:open="expertSelectVisible"
      :multiple="true"
      :expert-min="1"
      :expert-max="10"
      @confirm="(...args: any[]) => handleExpertConfirm(args[0])"
    />
  </Page>
</template>

<style lang="scss" scoped>
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

.no-action-tip {
  color: var(--color-text-quaternary, #bfbfbf);
  font-size: 14px;
}
</style>
