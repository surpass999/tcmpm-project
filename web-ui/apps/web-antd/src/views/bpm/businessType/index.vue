<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { BpmBusinessTypeApi } from '#/api/bpm/business-type';

import { Page, useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { $t } from '#/locales';

import {
  deleteBusinessType,
  getBusinessTypePage,
} from '#/api/bpm/business-type';

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

/** 创建业务类型 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑业务类型 */
function handleEdit(row: BpmBusinessTypeApi.BusinessType) {
  formModalApi.setData(row).open();
}

/** 删除业务类型 */
async function handleDelete(row: BpmBusinessTypeApi.BusinessType) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.businessName]),
    duration: 0,
  });
  try {
    await deleteBusinessType(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.businessName]));
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
          return await getBusinessTypePage({
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
  } as VxeTableGridOptions<BpmBusinessTypeApi.BusinessType>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="业务类型管理">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['业务类型']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['bpm:business-type:create'],
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
              auth: ['bpm:business-type:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['bpm:business-type:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.businessName]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
