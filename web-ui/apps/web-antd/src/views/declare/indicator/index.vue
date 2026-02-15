<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';

import { Page, useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';

import { $t } from '#/locales';
import {
  deleteIndicator,
  getIndicatorPage,
} from '#/api/declare/indicator';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 创建指标 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑指标 */
function handleEdit(row: DeclareIndicatorApi.Indicator) {
  formModalApi.setData(row).open();
}

/** 删除指标 */
async function handleDelete(row: DeclareIndicatorApi.Indicator) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.indicatorName]),
    duration: 0,
  });
  try {
    await deleteIndicator(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.indicatorName]));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getIndicatorPage({
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
  } as VxeTableGridOptions<DeclareIndicatorApi.Indicator>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="指标管理">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['指标']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['declare:indicator:create'],
              onClick: handleCreate,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: $t('common.edit'),
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['declare:indicator:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['declare:indicator:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.indicatorName]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
