<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareReviewApi } from '#/api/declare/review';

import { Page, useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';
import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteReviewTask,
  getReviewTaskPage,
  getReviewTaskDetail,
} from '#/api/declare/review';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import ReviewDetailModal from './modules/ReviewDetailModal.vue';

// 业务类型
const BUSINESS_TABLE_NAME = 'review_task';

// 任务类型选项
const taskTypeOptions = [
  { label: '备案论证', value: 1 },
  { label: '中期评估', value: 2 },
  { label: '验收评审', value: 3 },
  { label: '成果审核', value: 4 },
];

// 业务类型选项
const businessTypeOptions = [
  { label: '备案', value: 1 },
  { label: '项目', value: 2 },
  { label: '成果', value: 3 },
];

// 评审状态选项
const reviewStatusOptions = [
  { label: '待分配', value: 0 },
  { label: '评审中', value: 1 },
  { label: '已完成', value: 2 },
];

function getTaskTypeLabel(type: number) {
  return taskTypeOptions.find((item) => item.value === type)?.label || '未知';
}

function getBusinessTypeLabel(type: number) {
  return businessTypeOptions.find((item) => item.value === type)?.label || '未知';
}

function getReviewStatusLabel(status: number) {
  return reviewStatusOptions.find((item) => item.value === status)?.label || '未知';
}

// 评审详情弹窗
const [ReviewDetailModalInstance, reviewDetailModalApi] = useVbenModal({
  connectedComponent: ReviewDetailModal,
  destroyOnClose: true,
  class: '!min-w-[80%] !min-h-[70vh]',
});

// 表格列定义
const gridColumns = useGridColumns();

// 表格配置
const gridOptions: VxeTableGridOptions = {
  columns: gridColumns,
  pagerConfig: {
    enabled: true,
    pageSize: 10,
    pageSizes: [10, 20, 50],
  },
  proxyConfig: {
    ajax: {
      query: async ({ page }, formValues) => {
        const { current = 1, pageSize = 10 } = page || {};
        const res = await getReviewTaskPage({
          current,
          pageSize,
          ...formValues,
        });
        return {
          items: res.items || [],
          total: res.total || 0,
        };
      },
    },
  },
  toolbarConfig: {
    custom: true,
    refresh: true,
    search: true,
  },
};

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions,
});

// 查看详情
async function handleViewDetail(row: DeclareReviewApi.ReviewTask) {
  const detail = await getReviewTaskDetail(row.id!);
  reviewDetailModalApi.setData({
    task: detail,
  });
  reviewDetailModalApi.open();
}

// 删除任务
async function handleDelete(row: DeclareReviewApi.ReviewTask) {
  await deleteReviewTask(row.id!);
  message.success('删除成功');
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="评审任务列表">
      <template #taskType_default="{ row }">
        {{ getTaskTypeLabel(row.taskType) }}
      </template>
      <template #businessType_default="{ row }">
        {{ getBusinessTypeLabel(row.businessType) }}
      </template>
      <template #status_default="{ row }">
        <a-tag :color="row.status === 2 ? 'green' : row.status === 1 ? 'blue' : 'orange'">
          {{ getReviewStatusLabel(row.status) }}
        </a-tag>
      </template>
      <template #action_default="{ row }">
        <TableAction
          :actions="[
            {
              label: '查看详情',
              onClick: handleViewDetail.bind(null, row),
            },
            {
              label: '删除',
              danger: true,
              popconfirm: {
                title: '确认删除该评审任务吗？',
                onConfirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>

    <ReviewDetailModalInstance />
  </Page>
</template>
