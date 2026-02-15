<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { BpmProcessNodeDslApi } from '#/api/bpm/process-node-dsl';

import { onActivated } from 'vue';

import { DocAlert, Page, useVbenModal } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { message, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteDsl,
  getDslPage,
} from '#/api/bpm/process-node-dsl';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

defineOptions({ name: 'BpmProcessNodeDsl' });

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 新增DSL配置 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑DSL配置 */
function handleEdit(row: BpmProcessNodeDslApi.Dsl) {
  formModalApi.setData(row).open();
}

/** 删除DSL配置 */
async function handleDelete(row: BpmProcessNodeDslApi.Dsl) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.nodeName]),
    duration: 0,
  });
  try {
    await deleteDsl(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.nodeName]));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
  onSuccess: () => {
    handleRefresh();
  },
});

/** 解析DSL配置中的cap字段用于显示 */
function parseCap(dslConfig: string): string {
  if (!dslConfig) return '-';
  try {
    const config = JSON.parse(dslConfig);
    return config.cap || '-';
  } catch {
    return '-';
  }
}

/** 解析DSL配置中的actions字段用于显示 */
function parseActions(dslConfig: string): string[] {
  if (!dslConfig) return [];
  try {
    const config = JSON.parse(dslConfig);
    return (config.actions || '').split(',');
  } catch {
    return [];
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
          return await getDslPage({
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
  } as VxeTableGridOptions<BpmProcessNodeDslApi.Dsl>,
});

/** 激活时 */
onActivated(() => {
  handleRefresh();
});
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert
        title="流程节点DSL配置"
        url="https://doc.iocoder.cn/bpm/process-node-dsl/"
      />
    </template>

    <FormModal />
    <Grid table-title="流程节点DSL配置">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['DSL配置']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['bpm:process-node-dsl:create'],
              onClick: handleCreate,
            },
          ]"
        />
      </template>

      <!-- 节点能力列渲染 -->
      <template #cap-default="{ row }">
        <Tag :color="parseCap(row.dslConfig) === 'AUDIT' ? 'blue' : parseCap(row.dslConfig) === 'COUNTERSIGN' ? 'purple' : 'cyan'">
          {{ parseCap(row.dslConfig) }}
        </Tag>
      </template>

      <!-- 可用动作列渲染 -->
      <template #actions-default="{ row }">
        <span>
          <a-tag v-for="action in parseActions(row.dslConfig)" :key="action" color="default" style="margin-right: 4px">
            {{ action }}
          </a-tag>
        </span>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: $t('common.edit'),
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['bpm:process-node-dsl:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['bpm:process-node-dsl:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.nodeName]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
