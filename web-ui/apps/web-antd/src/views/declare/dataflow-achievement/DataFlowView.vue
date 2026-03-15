<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareDataFlowApi } from '#/api/declare/dataflow';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteDataFlow,
  getDataFlowPage,
  submitDataFlow,
} from '#/api/declare/dataflow';
import { getTaskByBusiness, approveTask, rejectTask } from '#/api/bpm/task';
import { $t } from '#/locales';

import Form from '../dataflow/modules/form.vue';

const BUSINESS_TABLE_NAME = 'data_flow';

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

const statusOptions = [
  { label: '草稿', value: 0 },
  { label: '已提交', value: 1 },
  { label: '审核中', value: 2 },
  { label: '已通过', value: 3 },
  { label: '退回', value: 4 },
];

const flowTypeOptions = [
  { label: '内部使用', value: 1 },
  { label: '对外共享', value: 2 },
  { label: '交易', value: 3 },
];

function getStatusLabel(status: number) {
  return statusOptions.find((item) => item.value === status)?.label || '未知';
}

function getFlowTypeLabel(flowType: number) {
  return flowTypeOptions.find((item) => item.value === flowType)?.label || '未知';
}

async function loadRowAvailableActions(rows: DeclareDataFlowApi.DataFlow[]) {
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
    console.error('[BPM] 加载数据流通操作失败:', e);
  }
  rowAvailableActions.value = { ...results };
}

function getRowActions(row: DeclareDataFlowApi.DataFlow) {
  const actions = rowAvailableActions.value[row.id!] || [];
  return [
    ...(row.status === 0 && row.creator === currentUserId.value
      ? [{ label: '提交审核', type: 'link' as any, onClick: () => handleSubmit(row) }]
      : []),
    ...actions.map((action: any) => ({
      label: action.label,
      type: 'link' as any,
      onClick: () => handleBpmAction(row, action),
    })),
    ...(row.status === 0
      ? [
          { label: $t('common.edit'), type: 'link' as any, icon: ACTION_ICON.EDIT, auth: ['declare:data-flow:update'], onClick: () => handleEdit(row) },
          { label: $t('common.delete'), type: 'link' as any, danger: true, icon: ACTION_ICON.DELETE, auth: ['declare:data-flow:delete'], onClick: () => handleDelete(row) },
        ]
      : []),
  ];
}

async function handleSubmit(row: DeclareDataFlowApi.DataFlow) {
  try {
    await submitDataFlow(row.id!);
    message.success('提交审核成功');
    gridApi.query();
    loadRowAvailableActions([row]);
  } catch {
    message.error('提交审核失败');
  }
}

async function handleBpmAction(row: DeclareDataFlowApi.DataFlow, action: any) {
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

function handleEdit(row: DeclareDataFlowApi.DataFlow) {
  formModalApi.setData(row).open();
}

async function handleDelete(row: DeclareDataFlowApi.DataFlow) {
  try {
    await deleteDataFlow(row.id!);
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
  { field: 'dataName', title: '数据名称', minWidth: 150 },
  { field: 'dataType', title: '数据类型', width: 120 },
  { field: 'flowType', title: '流通类型', width: 100, slots: { default: 'flowType' } },
  { field: 'projectName', title: '关联项目', width: 150 },
  { field: 'status', title: '状态', width: 100, slots: { default: 'status' } },
  { field: 'createTime', title: '创建时间', width: 160 },
  { field: 'creator', title: '创建人', width: 80 },
  { width: 250, slots: { default: 'actions' }, title: '操作' },
];

const formSchema = [
  { field: 'dataName', label: '数据名称', component: 'Input', componentProps: { placeholder: '请输入数据名称' } },
  { field: 'dataType', label: '数据类型', component: 'Input', componentProps: { placeholder: '请输入数据类型' } },
  { field: 'flowType', label: '流通类型', component: 'Select', componentProps: { placeholder: '请选择流通类型', options: flowTypeOptions } },
  { field: 'status', label: '状态', component: 'Select', componentProps: { placeholder: '请选择状态', options: statusOptions } },
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
          const result = await getDataFlowPage({
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
  } as VxeTableGridOptions<DeclareDataFlowApi.DataFlow>,
});
</script>

<template>
  <div>
    <FormModal @success="handleRefresh" />
    <Grid table-title="数据流通记录列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['数据流通']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['declare:data-flow:create'],
              onClick: handleCreate,
            },
          ]"
        />
      </template>
      <template #flowType="{ row }">
        {{ getFlowTypeLabel(row.flowType) }}
      </template>
      <template #status="{ row }">
        <a-tag :color="row.status === 3 ? 'green' : row.status === 4 ? 'red' : row.status === 1 ? 'blue' : 'default'">
          {{ getStatusLabel(row.status) }}
        </a-tag>
      </template>
      <template #actions="{ row }">
        <TableAction :actions="getRowActions(row)" />
      </template>
    </Grid>
  </div>
</template>
