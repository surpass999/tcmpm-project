<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareFilingApi } from '#/api/declare/filing';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';

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
import { getAvailableActions, getAvailableActionsBatch, submitBpmAction } from '#/api/bpm/action';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

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

/** 提交弹窗状态 */
const submitModalVisible = ref(false);
const submitLoading = ref(false);
const currentSubmitRow = ref<DeclareFilingApi.Filing | null>(null);
const currentSubmitAction = ref<any>(null);
const submitForm = ref({
  reason: '',
});

/** 执行 DSL 按钮操作 */
async function handleBpmAction(row: DeclareFilingApi.Filing, action: any) {
  // 如果是 submit 类型，显示弹窗让用户填写审批意见
  if (action.key === 'submit') {
    currentSubmitRow.value = row;
    currentSubmitAction.value = action;
    submitForm.value.reason = '';
    submitModalVisible.value = true;
    return;
  }

  // 其他操作直接执行
  await executeBpmAction(row, action);
}

/** 执行 BPM 实际操作 */
async function executeBpmAction(row: DeclareFilingApi.Filing, action: any) {
  try {
    await submitBpmAction({
      businessType: BUSINESS_TYPE_KEY,
      businessId: row.id!,
      actionKey: action.key,
      reason: '',
    });
    message.success(`操作 "${action.label}" 执行成功`);
    // 刷新表格
    handleRefresh();
    // 重新加载该行的可用操作
    loadRowAvailableActions([row]);
  } catch (error: any) {
    console.error('[BPM] 执行操作失败:', error);
    message.error(error.message || '操作失败');
  }
}

/** 确认提交 */
async function handleSubmitConfirm() {
  if (!currentSubmitRow.value || !currentSubmitAction.value) {
    return;
  }

  submitLoading.value = true;
  try {
    await submitBpmAction({
      businessType: BUSINESS_TYPE_KEY,
      businessId: currentSubmitRow.value.id!,
      actionKey: currentSubmitAction.value.key,
      reason: submitForm.value.reason,
    });
    message.success('提交成功');

    submitModalVisible.value = false;

    // 刷新表格
    handleRefresh();
    // 重新加载该行的可用操作
    loadRowAvailableActions([currentSubmitRow.value]);
  } catch (error: any) {
    console.error('[BPM] 提交失败:', error);
    message.error(error.message || '提交失败');
  } finally {
    submitLoading.value = false;
  }
}

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
          :actions="[
            // DSL 定义的流程操作按钮（如：提交、通过、退回等）
            ...(rowAvailableActions[row.id] || []).map((action) => ({
              label: action.label,
              type: 'link' as const,
              onClick: () => handleBpmAction(row, action),
            })),
            {
              label: $t('common.edit'),
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['declare:filing:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['declare:filing:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.id]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>

    <!-- 提交审核弹窗 -->
    <a-modal
      v-model:open="submitModalVisible"
      title="提交审核"
      :confirm-loading="submitLoading"
      @ok="handleSubmitConfirm"
    >
      <a-form layout="vertical">
        <a-form-item label="审批意见">
          <a-textarea
            v-model:value="submitForm.reason"
            placeholder="请输入审批意见（可选）"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </Page>
</template>