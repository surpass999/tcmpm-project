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
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

// 业务类型：1=备案
const BUSINESS_TYPE = 'filing';

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
  // contentClass: '!min-w-[90%] !min-h-[80vh]'
});

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
  // 处理日期区间
  if (typeof value === 'object' && value !== null && 'start' in value && 'end' in value) {
    return `${value.start} ~ ${value.end}`;
  }
  // 处理布尔值
  if (typeof value === 'boolean') {
    return value ? '是' : '否';
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
          return await getFilingPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
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
  </Page>
</template>