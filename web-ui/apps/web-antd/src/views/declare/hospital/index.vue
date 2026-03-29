<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareHospitalApi } from '#/api/declare/hospital';

import { ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createHospital,
  deleteHospital,
  getHospital,
  getHospitalPage,
  updateHospital,
} from '#/api/declare/hospital';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';
import ImportForm from './modules/import-form.vue';
import TagConfigModal from './modules/tag-config-modal.vue';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [ImportModal, importModalApi] = useVbenModal({
  connectedComponent: ImportForm,
  destroyOnClose: true,
});

const [TagConfigModalComp, tagConfigModalApi] = useVbenModal({
  connectedComponent: TagConfigModal,
  destroyOnClose: true,
});

/** 配置医院标签 */
function handleConfigTag(row: DeclareHospitalApi.Hospital) {
  tagConfigModalApi.setData(row).open();
}

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 创建医院 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 导入医院 */
function handleImport() {
  importModalApi.open();
}

/** 编辑医院 */
function handleEdit(row: DeclareHospitalApi.Hospital) {
  formModalApi.setData(row).open();
}

/** 删除医院 */
async function handleDelete(row: DeclareHospitalApi.Hospital) {
  await confirm($t('ui.actionMessage.deleteConfirm', [row.hospitalName]));
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.hospitalName]),
    duration: 0,
  });
  try {
    await deleteHospital(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.hospitalName]));
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
          const res = await getHospitalPage({
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
  } as VxeTableGridOptions<DeclareHospitalApi.Hospital>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <ImportModal @success="handleRefresh" />
    <TagConfigModalComp @success="handleRefresh" />

    <Grid table-title="医院列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['医院']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['declare:hospital:create'],
              onClick: handleCreate,
            },
            {
              label: $t('ui.actionTitle.import', ['医院']),
              type: 'primary',
              icon: ACTION_ICON.UPLOAD,
              auth: ['declare:hospital:import'],
              onClick: handleImport,
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
              auth: ['declare:hospital:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['declare:hospital:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.hospitalName]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
          :drop-down-actions="[
            {
              label: '配置标签',
              type: 'link',
              auth: ['declare:hospital:config-tag'],
              onClick: handleConfigTag.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
