<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { Registration } from '#/api/declare/training';

import { ref } from 'vue';

import { Page, confirm } from '@vben/common-ui';

import { message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { signIn, getRegistrationPage as getPage } from '#/api/declare/training';

import {
  getRegistrationStatusColor,
  getRegistrationStatusLabel,
  useRegistrationGridColumns,
  useRegistrationFormSchema,
} from '../data';

const checkedIds = ref<number[]>([]);

/** 处理复选框变化 */
function handleRowCheckboxChange({ records }: { records: Registration[] }) {
  checkedIds.value = records.map((item) => item.id!);
}

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 签到 */
async function handleSignIn(row: Registration) {
  try {
    await confirm('确定要为该用户签到吗？');
    await signIn(row.id!);
    message.success('签到成功');
    handleRefresh();
  } catch {
    // 用户取消不处理
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useRegistrationFormSchema(),
  },
  gridOptions: {
    columns: useRegistrationGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getPage({
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
      zoom: true,
    },
  } as VxeTableGridOptions<Registration>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="报名管理">
      <template #statusLabel_default="{ row }">
        <a-tag :color="getRegistrationStatusColor(row.status)">
          {{ getRegistrationStatusLabel(row.status) }}
        </a-tag>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '签到',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              ifShow: row.status === 1,
              onClick: handleSignIn.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
