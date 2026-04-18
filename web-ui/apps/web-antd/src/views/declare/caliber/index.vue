<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareIndicatorCaliberApi } from '#/api/declare/caliber';

import { Page, useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';

import { $t } from '#/locales';
import {
  deleteCaliber,
  getCaliberPage,
  updateCaliber,
  updateCaliberStatus,
} from '#/api/declare/caliber';

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

/** 创建口径 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑口径 */
function handleEdit(row: DeclareIndicatorCaliberApi.Caliber) {
  formModalApi.setData(row).open();
}

/** 删除口径 */
async function handleDelete(row: DeclareIndicatorCaliberApi.Caliber) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.id]),
    duration: 0,
  });
  try {
    await deleteCaliber(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.id]));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

/** 切换口径状态 */
async function handleToggleStatus(row: DeclareIndicatorCaliberApi.Caliber) {
  const newStatus = row.status === 1 ? 0 : 1;
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.saving', []),
    duration: 0,
  });
  try {
    await updateCaliberStatus(row.id!, newStatus);
    message.success($t('ui.actionMessage.operationSuccess'));
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
            const res = await getCaliberPage({
              pageNo: page.currentPage,
              pageSize: page.pageSize,
              ...formValues,
            });
            return {
              list: res?.list ?? res ?? [],
              total: res?.total ?? 0,
            };
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
  } as VxeTableGridOptions<DeclareIndicatorCaliberApi.Caliber>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="指标口径管理">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['指标口径']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['declare:caliber:create'],
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
              auth: ['declare:caliber:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: row.status === 1 ? '禁用' : '启用',
              type: 'link',
              auth: ['declare:caliber:update'],
              onClick: handleToggleStatus.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['declare:caliber:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.id]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
      <template #status="{ row }">
        <a-tag :color="row.status === 1 ? 'green' : 'red'">
          {{ row.status === 1 ? '启用' : '禁用' }}
        </a-tag>
      </template>
    </Grid>
  </Page>
</template>
