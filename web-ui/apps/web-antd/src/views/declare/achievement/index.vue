<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareAchievementApi } from '#/api/declare/achievement';

import { computed, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteAchievement,
  getAchievementPage,
  submitAchievement,
  recommendToNation,
} from '#/api/declare/achievement';
import { getTaskByBusiness, approveTask, rejectTask } from '#/api/bpm/task';
import { $t } from '#/locales';

import Form from './modules/form.vue';

// 业务类型
const BUSINESS_TYPE_KEY = 'declare:achievement:submit';
const BUSINESS_TABLE_NAME = 'achievement';

// 当前登录用户ID
const userStore = useUserStore();
const currentUserId = computed(() => {
  return Number(userStore.userInfo?.userId) || userStore.userInfo?.id;
});

// 每行的可用操作缓存
const rowAvailableActions = ref<Record<number, any[]>>({});

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
  class: '!min-w-[90%] !min-h-[80vh]',
  closeOnClickModal: false,
});

// 审核状态选项
const auditStatusOptions = [
  { label: '待审核', value: 0 },
  { label: '省级通过/待国家局审核', value: 1 },
  { label: '国家局审核中', value: 2 },
  { label: '已认定推广', value: 3 },
  { label: '退回', value: 4 },
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

function getAuditStatusLabel(status: number) {
  return auditStatusOptions.find((item) => item.value === status)?.label || '未知';
}

function getRecommendStatusLabel(status: number) {
  return recommendStatusOptions.find((item) => item.value === status)?.label || '未知';
}

function getAchievementTypeLabel(type: number) {
  return achievementTypeOptions.find((item) => item.value === type)?.label || '未知';
}

/** 加载每行的可用操作按钮 */
async function loadRowAvailableActions(rows: DeclareAchievementApi.Achievement[]) {
  const results: Record<number, any[]> = { ...rowAvailableActions.value };

  const validRows = rows.filter((row) => row.id);
  if (validRows.length === 0) {
    return;
  }

  const businessIds = validRows.map((row) => row.id!);

  try {
    const taskStatusList = await getTaskByBusiness({
      tableName: BUSINESS_TABLE_NAME,
      businessIds,
    });

    for (const taskStatus of taskStatusList) {
      const businessId = taskStatus.businessId;

      if (taskStatus.hasTodoTask && taskStatus.todoTasks && taskStatus.todoTasks.length > 0) {
        const todoTask = taskStatus.todoTasks[0];
        const buttonsSetting = todoTask.buttonSettings || {};

        const actions: any[] = [];
        Object.entries(buttonsSetting).forEach(([type, config]: [string, any]) => {
          if (config && config.enable) {
            actions.push({
              key: type,
              label: config.displayName || type,
              vars: {},
              taskId: todoTask.taskId,
              processInstanceId: taskStatus.processInstanceId,
              taskDefinitionKey: todoTask.taskDefinitionKey,
              signEnable: todoTask.signEnable,
              reasonRequire: todoTask.reasonRequire,
            });
          }
        });

        results[businessId] = actions;
      } else {
        results[businessId] = [];
      }
    }
  } catch (e) {
    console.error('[BPM] 加载操作失败:', e);
  }

  rowAvailableActions.value = { ...results };
}

/** 获取行操作按钮列表 */
function getRowActions(row: DeclareAchievementApi.Achievement) {
  const actions = rowAvailableActions.value[row.id!] || [];

  const actionButtons = [
    // 草稿状态：显示"提交审核"按钮
    ...(row.auditStatus === 0 && row.creator === currentUserId.value
      ? [
          {
            label: '提交审核',
            type: 'link' as any,
            onClick: () => handleSubmitForApproval(row),
          },
        ]
      : []),

    // 已通过状态：显示"推荐至国家局"按钮（省局角色）
    ...(row.auditStatus === 3 && row.recommendStatus === 0
      ? [
          {
            label: '推荐至国家局',
            type: 'link' as any,
            onClick: () => handleRecommend(row),
          },
        ]
      : []),

    // BPM 操作按钮（通过/驳回等）
    ...actions.map((action: any) => ({
      label: action.label,
      type: 'link' as any,
      onClick: () => handleBpmAction(row, action),
    })),

    // 编辑按钮（仅草稿状态）
    ...(row.auditStatus === 0
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
    ...(row.auditStatus === 0
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
  try {
    await submitAchievement(row.id!);
    message.success('提交审核成功');
    handleRefresh();
    loadRowAvailableActions([row]);
  } catch (error) {
    console.error('提交审核失败:', error);
    message.error('提交审核失败');
  }
}

/** 推荐至国家局 */
async function handleRecommend(row: DeclareAchievementApi.Achievement) {
  try {
    await recommendToNation(row.id!, '推荐该成果至国家局');
    message.success('推荐成功');
    handleRefresh();
  } catch (error) {
    console.error('推荐失败:', error);
    message.error('推荐失败');
  }
}

/** 处理BPM操作 */
async function handleBpmAction(row: DeclareAchievementApi.Achievement, action: any) {
  const { taskId, key, reasonRequire } = action;
  const buttonId = Number(key);

  try {
    if (buttonId === 1) {
      // 通过
      await approveTask({ id: taskId, buttonId: 1, reason: '' });
      message.success('审批通过');
    } else if (buttonId === 2) {
      // 驳回
      await rejectTask({ id: taskId, reason: '' });
      message.success('已驳回');
    }

    await loadRowAvailableActions([row]);
    handleRefresh();
  } catch (error) {
    console.error('操作失败:', error);
    message.error('操作失败');
  }
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
  { type: 'seq', width: 60 },
  { type: 'checkbox', width: 60 },
  { field: 'achievementName', title: '成果名称', minWidth: 150 },
  { field: 'achievementType', title: '成果类型', width: 100, slots: { default: 'achievementType' } },
  { field: 'projectName', title: '关联项目', width: 150 },
  { field: 'auditStatus', title: '审核状态', width: 140, slots: { default: 'auditStatus' } },
  { field: 'recommendStatus', title: '推荐状态', width: 120, slots: { default: 'recommendStatus' } },
  { field: 'createTime', title: '创建时间', width: 160 },
  { width: 280, slots: { default: 'actions' }, title: '操作' },
];

// 查询表单配置
const formSchema = [
  { field: 'achievementName', label: '成果名称', component: 'Input', componentProps: { placeholder: '请输入成果名称' } },
  { field: 'achievementType', label: '成果类型', component: 'Select', componentProps: { placeholder: '请选择成果类型', options: achievementTypeOptions } },
  { field: 'auditStatus', label: '审核状态', component: 'Select', componentProps: { placeholder: '请选择审核状态', options: auditStatusOptions } },
  { field: 'recommendStatus', label: '推荐状态', component: 'Select', componentProps: { placeholder: '请选择推荐状态', options: recommendStatusOptions } },
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
          // 异步加载每行的可用操作按钮
          if (result?.list?.length) {
            loadRowAvailableActions(result.list);
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
      <!-- 审核状态插槽 -->
      <template #auditStatus="{ row }">
        <a-tag :color="row.auditStatus === 3 ? 'green' : row.auditStatus === 4 ? 'red' : row.auditStatus === 1 ? 'blue' : 'default'">
          {{ getAuditStatusLabel(row.auditStatus) }}
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
