<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclarePolicyApi } from '#/api/declare/policy';

import { ref } from 'vue';

import { Page, useVbenModal, confirm } from '@vben/common-ui';

import { message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deletePolicy,
  getPolicyPage,
  publishPolicy,
  unpublishPolicy,
} from '#/api/declare/policy';

import PolicyDetailModal from './modules/detail.vue';
import PolicyFormModal from './modules/form.vue';

import {
  getPolicyTypeLabel,
  getStatusColor,
  getStatusLabel,
  getTargetProjectTypesLabel,
  getTargetScopeLabel,
  useGridColumns,
  useGridFormSchema,
} from './data';

const checkedIds = ref<number[]>([]);

// 详情弹窗
const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: PolicyDetailModal,
  destroyOnClose: true,
});

// 表单弹窗
const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: PolicyFormModal,
  destroyOnClose: true,
});

/** 处理复选框变化 */
function handleRowCheckboxChange({
  records,
}: {
  records: DeclarePolicyApi.Policy[];
}) {
  checkedIds.value = records.map((item) => item.id!);
}

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 新增政策通知 */
function handleAdd() {
  formModalApi.setData(null).open();
}

/** 编辑政策通知 */
function handleEdit(row: DeclarePolicyApi.Policy) {
  formModalApi.setData(row).open();
}

/** 查看详情 */
function handleDetail(row: DeclarePolicyApi.Policy) {
  detailModalApi.setData(row).open();
}

/** 删除政策通知 */
async function handleDelete(row: DeclarePolicyApi.Policy) {
  try {
    await confirm('确定要删除该政策通知吗？');
    await deletePolicy(row.id!);
    message.success('删除成功');
    handleRefresh();
  } catch (e) {
    // 用户取消不处理
  }
}

/** 发布政策通知 */
async function handlePublish(row: DeclarePolicyApi.Policy) {
  try {
    await confirm('确定要发布该政策通知吗？');
    await publishPolicy(row.id!);
    message.success('发布成功');
    handleRefresh();
  } catch (e) {
    // 用户取消不处理
  }
}

/** 下架政策通知 */
async function handleUnpublish(row: DeclarePolicyApi.Policy) {
  try {
    await confirm('确定要下架该政策通知吗？');
    await unpublishPolicy(row.id!);
    message.success('下架成功');
    handleRefresh();
  } catch (e) {
    // 用户取消不处理
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
            const res = await getPolicyPage({
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
      zoom: true,
    },
  } as VxeTableGridOptions<DeclarePolicyApi.Policy>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <DetailModal @success="handleRefresh" />
    <FormModal @success="handleRefresh" />
    <Grid table-title="政策通知管理">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              onClick: handleAdd,
            },
          ]"
        />
      </template>

      <template #policyTitle_default="{ row }">
        <div class="flex items-center gap-2 flex-wrap">
          <a-tag
            v-if="row.releaseDept"
            color="#A07852"
            class="m-0"
          >
            {{ row.releaseDept }}
          </a-tag>
          <span class="truncate">{{ row.policyTitle }}</span>
        </div>
      </template>

      <template #policyType_default="{ row }">
        {{ getPolicyTypeLabel(row.policyType) }}
      </template>

      <template #targetProjectTypes_default="{ row }">
        {{ getTargetProjectTypesLabel(row.targetProjectTypes) }}
      </template>

      <template #releaseDept_default="{ row }">
        {{ row.releaseDept }}
      </template>

      <template #targetScope_default="{ row }">
        {{ getTargetScopeLabel(row.targetScope) }}
      </template>

      <template #status_default="{ row }">
        <a-tag :color="getStatusColor(row.status)">
          {{ getStatusLabel(row.status) }}
        </a-tag>
      </template>

      <template #actions_default="{ row }">
        <TableAction
          :actions="[
            {
              label: '查看',
              type: 'link',
              icon: ACTION_ICON.VIEW,
              onClick: handleDetail.bind(null, row),
            },
            {
              label: '编辑',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              onClick: handleEdit.bind(null, row),
            },
            {
              label: '发布',
              type: 'link',
              ifShow: row.status !== 1,
              icon: ACTION_ICON.ADD,
              onClick: handlePublish.bind(null, row),
            },
            {
              label: '下架',
              type: 'link',
              ifShow: row.status === 1,
              onClick: handleUnpublish.bind(null, row),
            },
            {
              label: '删除',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              onClick: handleDelete.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
