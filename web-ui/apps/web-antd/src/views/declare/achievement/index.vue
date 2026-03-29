<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareAchievementApi } from '#/api/declare/achievement';

import { computed, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { message, Modal } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteAchievement,
  getAchievementPage,
  submitAchievement,
  recommendToLibrary,
} from '#/api/declare/achievement';
import { useBpmApproval } from '#/composables/useBpmApproval';
import ApprovalModal from '#/components/bpm/ApprovalModal.vue';
import ReturnModal from '#/components/bpm/ReturnModal.vue';
import UserSelectModal from '#/components/bpm/UserSelectModal.vue';
import { $t } from '#/locales';

import Form from './modules/form.vue';
import DetailModal from './modules/DetailModal.vue';

// 业务类型
const BUSINESS_TYPE_KEY = 'achievement:submit';
const BUSINESS_TABLE_NAME = 'achievement';

// 当前登录用户ID
const userStore = useUserStore();
const currentUserId = computed(() => {
  return Number(userStore.userInfo?.userId) || userStore.userInfo?.id;
});

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
  class: '!min-w-[80%] !min-h-[80vh]',
  closeOnClickModal: false,
});

// 详情弹窗
const [AchievementDetailModal, detailModalApi] = useVbenModal({
  connectedComponent: DetailModal,
  destroyOnClose: true,
});

// 成果状态选项（与 AchievementStatus 枚举一致）
const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '国家局已审核', value: 'NATIONAL_APPROVED' },
  { label: '退回', value: 'REJECTED' },
];

// 推荐状态选项
const recommendStatusOptions = [
  { label: '未推荐', value: 0 },
  { label: '已推荐至国家局', value: 1 },
  { label: '已纳入推广库', value: 2 },
];

// 成果类型选项
const achievementTypeOptions = [
  { label: '系统功能', value: 1 },
  { label: '数据集', value: 2 },
  { label: '科研成果', value: 3 },
  { label: '管理经验', value: 4 },
];

function getStatusLabel(status: string) {
  return statusOptions.find((item) => item.value === status)?.label || '未知';
}

function getRecommendStatusLabel(status: number) {
  return recommendStatusOptions.find((item) => item.value === status)?.label || '未知';
}

function getAchievementTypeLabel(type: number) {
  return achievementTypeOptions.find((item) => item.value === type)?.label || '未知';
}

// ========== BPM 审批相关 Hook ==========
const {
  rowAvailableActions,
  loadRowAvailableActions,
  handleApprovalAction,
  approvalModalRef,
  returnModalRef,
  userSelectModalRef,
  confirmApproval,
  openUserSelect,
  handleUserSelectConfirm,
  handleSelectExpert,
} = useBpmApproval({
  tableName: BUSINESS_TABLE_NAME,
  businessType: BUSINESS_TYPE_KEY,
  onRefresh: () => {
    handleRefresh();
  },
  onCustomReturn: (taskId: string) => {
    // 使用退回弹窗
    returnModalRef.value?.open({ taskId, title: '退回' });
    return true;
  },
});

/** 获取行操作按钮列表 */
function getRowActions(row: DeclareAchievementApi.Achievement) {
  const actions = rowAvailableActions.value[row.id!] || [];

  const actionButtons = [
    // 草稿状态：显示"提交审核"按钮（仅创建人）
    ...(row.status === 'DRAFT' && row.creator === currentUserId.value
      ? [
          {
            label: '提交审核',
            type: 'link' as any,
            onClick: () => handleSubmitForApproval(row),
          },
        ]
      : []),

    // 推荐遴选按钮（条件：recommendStatus = 1）
    ...(row.recommendStatus === 1
      ? [
          {
            label: '推荐遴选',
            type: 'link' as any,
            auth: ['declare:achievement:recommend'],
            onClick: () => handleRecommendToLibrary(row),
          },
        ]
      : []),

    // BPM 操作按钮（通过/驳回等）
    ...actions.map((action: any) => ({
      label: action.buttonSettings?.[action.buttonKey]?.displayName || action.taskName,
      type: 'link' as any,
      onClick: () => handleApprovalAction(action),
    })),

    // 查看详情（所有状态）
    {
      label: '查看详情',
      type: 'link' as any,
      onClick: () => handleDetail(row),
    },

    // 编辑按钮（仅草稿状态）
    ...(row.status === 'DRAFT'
      ? [
          {
            label: $t('common.edit'),
            type: 'link' as any,
            icon: ACTION_ICON.EDIT,
            auth: ['declare:achievement:update'],
            onClick: handleEdit.bind(null, row),
          },
        ]
      : []),

    // 删除按钮（仅草稿状态）
    ...(row.status === 'DRAFT'
      ? [
          {
            label: $t('common.delete'),
            type: 'link' as any,
            danger: true,
            icon: ACTION_ICON.DELETE,
            auth: ['declare:achievement:delete'],
            onClick: handleDelete.bind(null, row),
          },
        ]
      : []),
  ];

  return actionButtons;
}

/** 提交审核 */
async function handleSubmitForApproval(row: DeclareAchievementApi.Achievement) {
  Modal.confirm({
    title: '确认提交审核',
    content: `确认要提交成果「${row.achievementName}」进行审核吗？提交后将无法再次编辑。`,
    async onOk() {
      try {
        await submitAchievement(row.id!, "declare_achievement");
        message.success('提交审核成功');
        handleRefresh();
        loadRowAvailableActions([row]);
      } catch (error) {
        console.error('提交审核失败:', error);
        message.error('提交审核失败');
      }
    },
  });
}

/** 推荐遴选（推荐至推广库） */
async function handleRecommendToLibrary(row: DeclareAchievementApi.Achievement) {
  Modal.confirm({
    title: '确认推荐至推广库',
    content: `确认要将成果「${row.achievementName}」纳入推广库吗？`,
    async onOk() {
      try {
        await recommendToLibrary(row.id!);
        message.success('推荐成功');
        handleRefresh();
      } catch (error) {
        console.error('推荐失败:', error);
        message.error('推荐失败');
      }
    },
  });
}

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 创建成果信息 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑成果信息 */
function handleEdit(row: DeclareAchievementApi.Achievement) {
  formModalApi.setData(row).open();
}

/** 查看详情 */
function handleDetail(row: DeclareAchievementApi.Achievement) {
  detailModalApi.setData(row).open();
}

/** 删除成果信息 */
async function handleDelete(row: DeclareAchievementApi.Achievement) {
  try {
    await deleteAchievement(row.id!);
    message.success($t('common.deleteSuccess'));
    handleRefresh();
  } catch {
    message.error('删除失败');
  }
}

// 表格列配置
const columns = [
  { field: 'id', title: '编号', width: 80 },
  { type: 'checkbox', width: 60 },
  { field: 'achievementName', title: '成果名称', minWidth: 150 },
  { field: 'achievementType', title: '成果类型', width: 100, slots: { default: 'achievementType' } },
  { field: 'dataName', title: '数据名称', width: 120 },
  { field: 'flowType', title: '流通类型', width: 100, slots: { default: 'flowType' } },
  { field: 'status', title: '状态', width: 120, slots: { default: 'status' } },
  { field: 'recommendStatus', title: '推荐状态', width: 130, slots: { default: 'recommendStatus' } },
  { field: 'createTime', title: '创建时间', width: 160 },
  { width: 320, slots: { default: 'actions' }, title: '操作' },
];

// 流通类型选项
const flowTypeOptions = [
  { label: '内部使用', value: 1 },
  { label: '对外共享', value: 2 },
  { label: '交易', value: 3 },
];

// 查询表单配置
const formSchema = [
  { fieldName: 'achievementName', label: '成果名称', component: 'Input', componentProps: { placeholder: '请输入成果名称' } },
  { fieldName: 'achievementType', label: '成果类型', component: 'Select', componentProps: { placeholder: '请选择成果类型', options: achievementTypeOptions } },
  { fieldName: 'dataName', label: '数据名称', component: 'Input', componentProps: { placeholder: '请输入数据名称' } },
  { fieldName: 'flowType', label: '流通类型', component: 'Select', componentProps: { placeholder: '请选择流通类型', options: flowTypeOptions } },
  { fieldName: 'status', label: '状态', component: 'Select', componentProps: { placeholder: '请选择状态', options: statusOptions } },
  { fieldName: 'recommendStatus', label: '推荐状态', component: 'Select', componentProps: { placeholder: '请选择推荐状态', options: recommendStatusOptions } },
];

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: formSchema,
  },
  gridOptions: {
    columns,
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getAchievementPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
          // 异步加载每行的可用操作按钮（需要 await，确保按钮数据加载完成后再渲染表格）
          if (result?.list?.length) {
            await loadRowAvailableActions(result.list);
          }
          return result;
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
  } as VxeTableGridOptions<DeclareAchievementApi.Achievement>,
});

</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <AchievementDetailModal />
    <!-- 审批弹窗（通过/拒绝） -->
    <ApprovalModal
      ref="approvalModalRef"
      @confirm="(reason, action) => confirmApproval(reason, action)"
    />
    <!-- 退回弹窗 -->
    <ReturnModal ref="returnModalRef" @success="handleRefresh" />
    <!-- 用户选择弹窗（转办/委派/加签/抄送） -->
    <UserSelectModal
      ref="userSelectModalRef"
      @confirm="(users, reason) => handleUserSelectConfirm(users, reason)"
    />
    <Grid table-title="成果信息列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['成果']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['declare:achievement:create'],
              onClick: handleCreate,
            },
          ]"
        />
      </template>
      <!-- 成果类型插槽 -->
      <template #achievementType="{ row }">
        {{ getAchievementTypeLabel(row.achievementType) }}
      </template>
      <!-- 流通类型插槽 -->
      <template #flowType="{ row }">
        <a-tag v-if="row.flowType === 1">内部使用</a-tag>
        <a-tag v-else-if="row.flowType === 2" color="blue">对外共享</a-tag>
        <a-tag v-else-if="row.flowType === 3" color="purple">交易</a-tag>
        <span v-else>-</span>
      </template>
      <!-- 状态插槽 -->
      <template #status="{ row }">
        <a-tag :color="row.status === 'NATIONAL_APPROVED' ? 'green' : row.status === 'REJECTED' ? 'red' : row.status === 'SUBMITTED' ? 'blue' : row.status === 'DRAFT' ? 'default' : 'orange'">
          {{ getStatusLabel(row.status) }}
        </a-tag>
      </template>
      <!-- 推荐状态插槽 -->
      <template #recommendStatus="{ row }">
        <a-tag :color="row.recommendStatus === 2 ? 'purple' : row.recommendStatus === 1 ? 'blue' : 'default'">
          {{ getRecommendStatusLabel(row.recommendStatus) }}
        </a-tag>
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="getRowActions(row)"
        />
      </template>
    </Grid>
  </Page>
</template>
