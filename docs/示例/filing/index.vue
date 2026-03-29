<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareFilingApi } from '#/api/declare/filing';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { DeclareExpertApi } from '#/api/declare/expert';
import type { BpmActionApi } from '#/api/bpm/action';

import { computed, nextTick, onMounted, ref, shallowRef } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';

import { message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteFiling,
  deleteFilingList,
  exportFiling,
  getFilingPage,
} from '#/api/declare/filing';
import { getIndicatorsForListDisplay } from '#/api/declare/indicator';
import { getAvailableActions, getAvailableActionsBatch } from '#/api/bpm/action';
import { $t } from '#/locales';

import ExpertSelectModalCmp from '#/components/bpm/ExpertSelectModal.vue';

import { useBpmAction } from '#/composables/useBpmAction';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

import ApprovalDetailCmp from '#/views/bpm/components/approval-detail.vue';
import ActionButtonCmp from '#/components/bpm/ActionButton.vue';

// 业务类型
const BUSINESS_TYPE_KEY = 'declare:filing:create';

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

/** 加载一批备案行的可用操作按钮（批量请求优化） */
async function loadRowAvailableActions(rows: DeclareFilingApi.Filing[]) {
  const results: Record<number, any[]> = { ...rowAvailableActions.value };
  const ids = rows.filter((row) => row.id).map((row) => row.id!);

  if (ids.length === 0) {
    return;
  }

  try {
    // 一次批量请求获取所有行的可用操作
    const actionsMap = await getAvailableActionsBatch(BUSINESS_TYPE_KEY, ids);
    console.log('[BPM] batch actions result:', actionsMap);

    // 按 businessId 分配结果
    ids.forEach((id) => {
      results[id] = actionsMap[id] || [];
    });
  } catch (e) {
    console.error('[BPM] 批量加载操作失败:', e);
    // 失败时设为空
    ids.forEach((id) => {
      results[id] = [];
    });
  }

  console.log('[BPM] rowAvailableActions updated:', results);
  rowAvailableActions.value = { ...results };
}

/** 获取行操作按钮列表（处理退回状态的按钮文案） */
function getRowActions(row: DeclareFilingApi.Filing) {
  const actions = rowAvailableActions.value[row.id!] || [];
  return [
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

/** 执行 DSL 按钮操作 */
async function handleBpmAction(row: DeclareFilingApi.Filing, action: any) {
  currentSubmitRow.value = row;
  currentSubmitAction.value = action;
  const vars = action.vars || {};

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

/** 专家选择弹窗引用 */
const [ExpertSelectModal, expertSelectModalApi] = useVbenModal({
  connectedComponent: ExpertSelectModalCmp,
  destroyOnClose: true,
});

/** 审批详情弹窗 - 使用 a-modal 直接控制宽度 */
const approvalDetailVisible = ref(false);
const approvalDetailRef = ref<InstanceType<typeof ApprovalDetailCmp> | null>(null);

/** 审批操作相关状态 */
const currentApprovalActions = ref<(BpmActionApi.BpmAction & { taskId?: string })[]>([]);
const currentApprovalBusinessId = ref<number | null>(null);
const currentApprovalRow = ref<DeclareFilingApi.Filing | null>(null);

/** 打开审批详情弹窗 */
async function handleViewApprovalDetail(row: DeclareFilingApi.Filing) {
  approvalDetailVisible.value = true;
  // 保存当前行数据
  currentApprovalRow.value = row;
  // 保存当前业务 ID
  currentApprovalBusinessId.value = row.id ?? null;
  // 传递数据给组件
    nextTick(async () => {
    approvalDetailRef.value?.openWithData(row);
    // 使用和列表页相同的方式获取可用操作
    try {
      const actions = await getAvailableActions(BUSINESS_TYPE_KEY, row.id!);
      // 过滤掉特殊标记：
      // _PROCESS_RUNNING_: 流程进行中但无操作权限
      // _PROCESS_FINISHED_: 流程已结束
      currentApprovalActions.value = (actions || []).filter(
        (a: any) => a.key !== '_PROCESS_RUNNING_' && a.key !== '_PROCESS_FINISHED_'
      );
    } catch (e) {
      console.error('获取审批操作失败:', e);
      currentApprovalActions.value = [];
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

    <!-- 审批详情弹窗 - 使用 a-modal 直接控制宽度 -->
    <a-modal
      v-model:open="approvalDetailVisible"
      :title="`审批详情`"
      :footer="null"
      width="90%"
      :body-style="{ minHeight: '80vh', padding: '16px', paddingBottom: '80px', position: 'relative' }"
      :z-index="1000"
      destroy-on-close
    >
      <ApprovalDetailCmp
        ref="approvalDetailRef"
        class="approval-detail-content-wrapper"
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