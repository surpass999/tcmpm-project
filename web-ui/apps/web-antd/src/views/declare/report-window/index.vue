<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ReportWindow } from '#/api/declare/report-window';

import { ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createReportWindow,
  deleteReportWindow,
  getReportWindowList,
  updateReportWindow,
} from '#/api/declare/report-window';
import { $t } from '#/locales';

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

/** 创建时间窗口 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑时间窗口 */
function handleEdit(row: ReportWindow) {
  formModalApi.setData(row).open();
}

/** 删除时间窗口 */
async function handleDelete(row: ReportWindow) {
  await confirm($t('ui.actionMessage.deleteConfirm', [`${row.reportYear}年第${row.reportBatch}批`]));
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [`${row.reportYear}年第${row.reportBatch}批`]),
    duration: 0,
  });
  try {
    await deleteReportWindow(row.id);
    message.success($t('ui.actionMessage.deleteSuccess'));
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
    pagerConfig: {
      enabled: false,
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const list = await getReportWindowList(formValues.reportYear, formValues.status);
          return { list, total: list.length };
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
  } as VxeTableGridOptions<ReportWindow>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />

    <Grid table-title="填报时间窗口列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['时间窗口']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['declare:report-window:create'],
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
              auth: ['declare:report-window:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['declare:report-window:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [`${row.reportYear}年第${row.reportBatch}批`]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
