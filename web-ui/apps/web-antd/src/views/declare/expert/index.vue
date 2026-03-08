<script lang="ts" setup>
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { getDictLabel } from '@vben/hooks';

import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteExpert,
  getExpertPage,
  updateExpertStatus,
} from '#/api/declare/expert';
import { $t } from '#/locales';
import { DICT_TYPE } from '@vben/constants';

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

/** 创建专家 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑专家 */
function handleEdit(row: any) {
  formModalApi.setData(row).open();
}

/** 删除专家 */
async function handleDelete(row: any) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.expertName]),
    duration: 0,
  });
  try {
    await deleteExpert(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.expertName]));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

/** 更新专家状态 */
async function handleStatusChange(newStatus: number, row: any) {
  const statusLabel = getDictLabel(DICT_TYPE.DECLARE_EXPERT_STATUS, newStatus) || '未知';
  await confirm(
    `确定要将专家【${row.expertName}】的状态修改为【${statusLabel}】吗？`,
  );
  await updateExpertStatus(row.id!, newStatus);
  message.success($t('ui.actionMessage.operationSuccess'));
  handleRefresh();
}

// 表格配置
const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  showSearchForm: true,
  gridOptions: {
    id: 'expert',
    columns: useGridColumns(handleEdit, handleDelete, handleStatusChange),
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 10,
      pageSizes: [10, 20, 50],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }: any, formData: any) => {
          const params = {
            ...formData,
            pageNo: page.currentPage,
            pageSize: page.pageSize,
          };
          const result = await getExpertPage(params);
          return result;
        },
      },
    },
    rowConfig: {
      keyField: 'id',
    },
    checkboxConfig: {
      trigger: 'row',
      highlight: true,
    },
    toolbarConfig: {
      custom: true,
      refresh: true,
      zoom: true,
    },
  },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid>
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['专家']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              onClick: handleCreate,
            },
          ]"
        />
      </template>

      <template #status="{ row }">
        <a-tag :color="row.status === 1 ? 'green' : (row.status === 2 ? 'orange' : 'red')">
          {{ getDictLabel(DICT_TYPE.DECLARE_EXPERT_STATUS, row.status) || '未知' }}
        </a-tag>
      </template>

      <template #expertType="{ row }">
        {{ getDictLabel(DICT_TYPE.DECLARE_EXPERT_TYPE, row.expertType) || '未知' }}
      </template>

      <template #action="{ row }">
        <TableAction
          :actions="[
            {
              label: '编辑',
              icon: ACTION_ICON.EDIT,
              onClick: handleEdit.bind(null, row),
            },
            {
              label: '删除',
              icon: ACTION_ICON.DELETE,
              danger: true,
              popConfirm: {
                title: `确定删除专家【${row.expertName}】吗？`,
                confirm: handleDelete.bind(null, row),
              },
              onClick: handleDelete.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
