<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareAchievementApi } from '#/api/declare/achievement';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
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

import Form from '../achievement/modules/form.vue';

const BUSINESS_TABLE_NAME = 'achievement';

const userStore = useUserStore();
const currentUserId = computed(() => {
  return Number(userStore.userInfo?.userId) || userStore.userInfo?.id;
});

const rowAvailableActions = ref<Record<number, any[]>>({});

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
  class: '!min-w-[90%] !min-h-[80vh]',
  closeOnClickModal: false,
});

const auditStatusOptions = [
  { label: '待审核', value: 0 },
  { label: '省级通过/待国家局审核', value: 1 },
  { label: '国家局审核中', value: 2 },
  { label: '已认定推广', value: 3 },
  { label: '退回', value: 4 },
];

const recommendStatusOptions = [
  { label: '未推荐', value: 0 },
  { label: '已推荐至国家局', value: 1 },
  { label: '已纳入推广库', value: 2 },
];

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

async function loadRowAvailableActions(rows: DeclareAchievementApi.Achievement[]) {
  const results: Record<number, any[]> = { ...rowAvailableActions.value };
  const validRows = rows.filter((row) => row.id);
  if (validRows.length === 0) return;

  const businessIds = validRows.map((row) => row.id!);
  try {
    const taskStatusList = await getTaskByBusiness({
      tableName: BUSINESS_TABLE_NAME,
      businessIds,
    });
    for (const taskStatus of taskStatusList) {
      const businessId = taskStatus.businessId;
      if (taskStatus.hasTodoTask && taskStatus.todoTasks?.length) {
        const todoTask = taskStatus.todoTasks[0];
        const buttonsSetting = todoTask.buttonSettings || {};
        const actions: any[] = [];
        Object.entries(buttonsSetting).forEach(([type, config]: [string, any]) => {
          if (config?.enable) {
            actions.push({
              key: type,
              label: config.displayName || type,
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
    console.error('[BPM] 加载成果操作失败:', e);
  }
  rowAvailableActions.value = { ...results };
}

function getRowActions(row: DeclareAchievementApi.Achievement) {
  const actions = rowAvailableActions.value[row.id!] || [];
  return [
    ...(row.auditStatus === 0 && row.creator === currentUserId.value
      ? [{ label: '提交审核', type: 'link' as any, onClick: () => handleSubmit(row) }]
      : []),
    ...(row.auditStatus === 3 && row.recommendStatus === 0
      ? [{ label: '推荐至国家局', type: 'link' as any, onClick: () => handleRecommend(row) }]
      : []),
    ...actions.map((action: any) => ({
      label: action.label,
      type: 'link' as any,
      onClick: () => handleBpmAction(row, action),
    })),
    ...(row.auditStatus === 0
      ? [
          { label: $t('common.edit'), type: 'link' as any, icon: ACTION_ICON.EDIT, auth: ['declare:achievement:update'], onClick: () => handleEdit(row) },
          { label: $t('common.delete'), type: 'link' as any, danger: true, icon: ACTION_ICON.DELETE, auth: ['declare:achievement:delete'], onClick: () => handleDelete(row) },
        ]
      : []),
  ];
}

async function handleSubmit(row: DeclareAchievementApi.Achievement) {
  try {
    await submitAchievement(row.id!);
    message.success('提交审核成功');
    gridApi.query();
    loadRowAvailableActions([row]);
  } catch {
    message.error('提交审核失败');
  }
}

async function handleRecommend(row: DeclareAchievementApi.Achievement) {
  try {
    await recommendToNation(row.id!, '推荐该成果至国家局');
    message.success('推荐成功');
    gridApi.query();
  } catch {
    message.error('推荐失败');
  }
}

async function handleBpmAction(row: DeclareAchievementApi.Achievement, action: any) {
  const { taskId, key } = action;
  const buttonId = Number(key);
  try {
    if (buttonId === 1) {
      await approveTask({ id: taskId, buttonId: 1, reason: '' });
      message.success('审批通过');
    } else if (buttonId === 2) {
      await rejectTask({ id: taskId, reason: '' });
      message.success('已驳回');
    }
    loadRowAvailableActions([row]);
    gridApi.query();
  } catch {
    message.error('操作失败');
  }
}

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData(null).open();
}

function handleEdit(row: DeclareAchievementApi.Achievement) {
  formModalApi.setData(row).open();
}

async function handleDelete(row: DeclareAchievementApi.Achievement) {
  try {
    await deleteAchievement(row.id!);
    message.success($t('common.deleteSuccess'));
    gridApi.query();
  } catch {
    message.error('删除失败');
  }
}

defineExpose({ handleRefresh });

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

const formSchema = [
  { field: 'achievementName', label: '成果名称', component: 'Input', componentProps: { placeholder: '请输入成果名称' } },
  { field: 'achievementType', label: '成果类型', component: 'Select', componentProps: { placeholder: '请选择成果类型', options: achievementTypeOptions } },
  { field: 'auditStatus', label: '审核状态', component: 'Select', componentProps: { placeholder: '请选择审核状态', options: auditStatusOptions } },
  { field: 'recommendStatus', label: '推荐状态', component: 'Select', componentProps: { placeholder: '请选择推荐状态', options: recommendStatusOptions } },
];

const [Grid, gridApi] = useVbenVxeGrid({
  // 禁用表单搜索功能，避免 vee-validate 在 KeepAlive 下的问题
  formOptions: false,
  gridOptions: {
    columns,
    // 使用固定高度，避免在 KeepAlive 缓存的动态组件中出现无限变高的问题
    maxHeight: 'calc(100vh - 300px)',
    minHeight: '500px',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getAchievementPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
          if (result?.list?.length) loadRowAvailableActions(result.list);
          return result;
        },
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<DeclareAchievementApi.Achievement>,
});
</script>

<template>
  <div>
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
      <template #achievementType="{ row }">
        {{ getAchievementTypeLabel(row.achievementType) }}
      </template>
      <template #auditStatus="{ row }">
        <a-tag :color="row.auditStatus === 3 ? 'green' : row.auditStatus === 4 ? 'red' : row.auditStatus === 1 ? 'blue' : 'default'">
          {{ getAuditStatusLabel(row.auditStatus) }}
        </a-tag>
      </template>
      <template #recommendStatus="{ row }">
        <a-tag :color="row.recommendStatus === 2 ? 'purple' : row.recommendStatus === 1 ? 'blue' : 'default'">
          {{ getRecommendStatusLabel(row.recommendStatus) }}
        </a-tag>
      </template>
      <template #actions="{ row }">
        <TableAction :actions="getRowActions(row)" />
      </template>
    </Grid>
  </div>
</template>
