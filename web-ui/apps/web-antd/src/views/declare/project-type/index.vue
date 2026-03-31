<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ProjectTypeApi } from '#/api/declare/project-type';

import { Page, useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';

import { $t } from '#/locales';
import {
  deleteProjectType,
  getProjectTypePage,
} from '#/api/declare/project-type';

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

/** 创建项目类型 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑项目类型 */
function handleEdit(row: ProjectTypeApi.ProjectType) {
  formModalApi.setData(row).open();
}

/** 删除项目类型 */
async function handleDelete(row: ProjectTypeApi.ProjectType) {
  try {
    await deleteProjectType(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.name]));
    handleRefresh();
  } catch {
    message.error('删除失败');
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
            const res = await getProjectTypePage({
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
  } as VxeTableGridOptions<ProjectTypeApi.ProjectType>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="项目类型管理">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['项目类型']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['declare:project-type:create'],
              onClick: handleCreate,
            },
          ]"
        />
      </template>
      <template #color="{ row }">
        <span v-if="row.color" class="color-dot" :style="{ backgroundColor: row.color }"></span>
        {{ row.color || '-' }}
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: $t('common.edit'),
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['declare:project-type:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['declare:project-type:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.name]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>

<style scoped>
.color-dot {
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  margin-right: 8px;
  vertical-align: middle;
}
</style>
