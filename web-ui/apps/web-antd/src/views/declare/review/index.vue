<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareReviewApi } from '#/api/declare/review';

import { Page, useVbenModal } from '@vben/common-ui';

import { Modal } from 'ant-design-vue';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getMyReviewTasks,
  getReviewResultDetail,
  receiveReviewTask,
  rejectReviewTask,
  startReview,
} from '#/api/declare/review';

import { useGridColumns, useGridFormSchema } from './data';
import ReviewDetailModal from './modules/ReviewDetailModal.vue';
import ReviewFilingModal from './modules/ReviewFilingModal.vue';
import ReviewProjectModal from './modules/ReviewProjectModal.vue';
import ReviewAchievementModal from './modules/ReviewAchievementModal.vue';

// 评审详情弹窗
const [ReviewDetailModalInstance, reviewDetailModalApi] = useVbenModal({
  connectedComponent: ReviewDetailModal,
  destroyOnClose: true,
  class: '!min-w-[80%] !min-h-[70vh]',
});

// 备案评审弹窗
const [ReviewFilingModalInstance, reviewFilingModalApi] = useVbenModal({
  connectedComponent: ReviewFilingModal,
  destroyOnClose: true,
  class: '!min-w-[90%] !max-w-[1200px]',
});

// 项目/过程评审弹窗
const [ReviewProjectModalInstance, reviewProjectModalApi] = useVbenModal({
  connectedComponent: ReviewProjectModal,
  destroyOnClose: true,
  class: '!min-w-[90%] !max-w-[1200px]',
});

// 成果评审弹窗
const [ReviewAchievementModalInstance, reviewAchievementModalApi] = useVbenModal({
  connectedComponent: ReviewAchievementModal,
  destroyOnClose: true,
  class: '!min-w-[90%] !max-w-[1200px]',
});

// 表格列定义
const gridColumns = useGridColumns();

// 评审状态选项（用于标签颜色）
const reviewStatusOptions = [
  { label: '待接收', value: 0, color: 'orange' },
  { label: '已接收', value: 1, color: 'blue' },
  { label: '评审中', value: 2, color: 'processing' },
  { label: '已提交', value: 3, color: 'green' },
  { label: '已拒绝', value: 5, color: 'red' },
];

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

function getTaskTypeLabel(type: number) {
  return taskTypeOptions.find((item) => item.value === type)?.label || '未知';
}

function getBusinessTypeLabel(type: number) {
  return businessTypeOptions.find((item) => item.value === type)?.label || '未知';
}

function getReviewStatusLabel(status: number) {
  const option = reviewStatusOptions.find((item) => item.value === status);
  return option?.label || '未知';
}

function getReviewStatusColor(status: number) {
  const option = reviewStatusOptions.find((item) => item.value === status);
  return option?.color || 'default';
}

/** 格式化分配时间（时间戳 → YYYY-MM-DD HH:mm） */
function formatCreateTime(time: string | number | undefined) {
  if (!time) return '-';
  const ts = typeof time === 'number' ? time : Number(time);
  if (Number.isNaN(ts)) return '-';
  return dayjs(ts).format('YYYY-MM-DD HH:mm');
}

// 表格配置
const gridOptions: VxeTableGridOptions = {
  columns: gridColumns,
  height: 'auto',
  keepSource: true,
  pagerConfig: {
    pageSize: 10,
    pageSizes: [10, 20, 50],
  },
  proxyConfig: {
    ajax: {
      query: async ({ page }, formValues) => {
        return await getMyReviewTasks({
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

// 刷新表格
function handleRefresh() {
  gridApi.query();
}

// 领取任务
async function handleReceive(row: DeclareReviewApi.ReviewResult) {
  Modal.confirm({
    title: '确认领取',
    content: `确定要领取该评审任务吗？领取后将开始计时，请在规定时间内完成评审。`,
    okText: '确认领取',
    cancelText: '取消',
    async onOk() {
      await receiveReviewTask(row.id!);
      message.success('领取成功');
      gridApi.query();
    },
  });
}

// 拒绝任务
async function handleReject(row: DeclareReviewApi.ReviewResult) {
  await rejectReviewTask(row.id!, '专家主动拒绝');
  message.success('已拒绝任务');
  gridApi.query();
}

// 查看详情/开始评审
async function handleViewDetail(row: DeclareReviewApi.ReviewResult) {
  // 已接收/评审中状态，根据业务类型打开不同的评审弹窗
  if (row.status === 1 || row.status === 2) {
    // 如果是已接收状态，需要先调用开始评审接口
    if (row.status === 1) {
      try {
        await startReview(row.id!);
      } catch (e) {
        // 开始评审失败，继续打开弹窗
      }
    }

    // 根据业务类型打开不同的评审弹窗
    if (row.businessType === 1) {
      // 备案评审
      reviewFilingModalApi.setData({ resultId: row.id }).open();
    } else if (row.businessType === 2) {
      // 项目/过程评审
      reviewProjectModalApi.setData({ resultId: row.id }).open();
    } else if (row.businessType === 3) {
      // 成果评审
      reviewAchievementModalApi.setData({ resultId: row.id }).open();
    }
    return;
  }

  // 其他状态（已提交/已拒绝），显示详情弹窗
  const detail = await getReviewResultDetail(row.id!);
  reviewDetailModalApi.setData({ result: detail }).open();
}

// 判断操作按钮显示
function getActions(row: DeclareReviewApi.ReviewResult) {
  const actions: any[] = [];

  // 待接收状态：显示领取/拒绝按钮
  if (row.status === 0) {
    actions.push({
      label: '领取',
      type: 'primary',
      onClick: handleReceive.bind(null, row),
    });
    actions.push({
      label: '拒绝',
      danger: true,
      onClick: handleReject.bind(null, row),
    });
  }

  // 已接收/评审中状态：显示开始评审/继续评审
  if (row.status === 1 || row.status === 2) {
    actions.push({
      label: row.status === 1 ? '开始评审' : '继续评审',
      type: 'primary',
      onClick: handleViewDetail.bind(null, row),
    });
  }

  // 已提交状态：显示查看结果
  if (row.status === 3) {
    actions.push({
      label: '查看结果',
      onClick: handleViewDetail.bind(null, row),
    });
  }

  // 已拒绝状态：只显示查看
  if (row.status === 5) {
    actions.push({
      label: '查看',
      onClick: handleViewDetail.bind(null, row),
    });
  }

  return actions;
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
        <a-tag :color="getReviewStatusColor(row.status)">
          {{ getReviewStatusLabel(row.status) }}
        </a-tag>
      </template>
      <template #createTime_default="{ row }">
        {{ formatCreateTime(row.createTime) }}
      </template>
      <template #action_default="{ row }">
        <TableAction :actions="getActions(row)" />
      </template>
    </Grid>

    <ReviewDetailModalInstance @success="handleRefresh" />
    <ReviewFilingModalInstance @success="handleRefresh" />
    <ReviewProjectModalInstance @success="handleRefresh" />
    <ReviewAchievementModalInstance @success="handleRefresh" />
  </Page>
</template>
