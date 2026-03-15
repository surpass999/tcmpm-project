<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ProcessIndicatorConfigApi } from '#/api/declare/process-indicator-config';

import { Page, useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';

import { $t } from '#/locales';
import {
  deleteProcessIndicatorConfig,
  getProcessIndicatorConfigPage,
} from '#/api/declare/process-indicator-config';

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

/** 创建配置 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑配置 */
function handleEdit(row: ProcessIndicatorConfigApi.ConfigResp) {
  formModalApi.setData(row).open();
}

/** 删除配置 */
async function handleDelete(row: ProcessIndicatorConfigApi.ConfigResp) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.indicatorName]),
    duration: 0,
  });
  try {
    await deleteProcessIndicatorConfig(row.id!);
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
          return await getProcessIndicatorConfigPage({
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
  } as VxeTableGridOptions<ProcessIndicatorConfigApi.ConfigResp>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="过程指标配置">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['配置']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['declare:process-indicator-config:create'],
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
              auth: ['declare:process-indicator-config:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['declare:process-indicator-config:delete'],
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
