<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareDataFlowApi } from '#/api/declare/dataflow';

import { computed, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
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

import Form from './modules/form.vue';

// 业务类型
const BUSINESS_TYPE_KEY = 'declare:dataflow:submit';
const BUSINESS_TABLE_NAME = 'data_flow';

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

// 状态选项
const statusOptions = [
  { label: '草稿', value: 0 },
  { label: '已提交', value: 1 },
  { label: '审核中', value: 2 },
  { label: '已通过', value: 3 },
  { label: '退回', value: 4 },
];

// 流通类型选项
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

/** 加载每行的可用操作按钮 */
async function loadRowAvailableActions(rows: DeclareDataFlowApi.DataFlow[]) {
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
function getRowActions(row: DeclareDataFlowApi.DataFlow) {
  const actions = rowAvailableActions.value[row.id!] || [];

  const actionButtons = [
    // 草稿状态：显示"提交审核"按钮
    ...(row.status === 0 && row.creator === currentUserId.value
      ? [
          {
            label: '提交审核',
            type: 'link' as any,
            onClick: () => handleSubmitForApproval(row),
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
    ...(row.status === 0
      ? [
          {
            label: $t('common.edit'),
            type: 'link' as any,
            icon: ACTION_ICON.EDIT,
            auth: ['declare:data-flow:update'],
            onClick: handleEdit.bind(null, row),
          },
        ]
      : []),

    // 删除按钮（仅草稿状态）
    ...(row.status === 0
      ? [
          {
            label: $t('common.delete'),
            type: 'link' as any,
            danger: true,
            icon: ACTION_ICON.DELETE,
            auth: ['declare:data-flow:delete'],
            onClick: handleDelete.bind(null, row),
          },
        ]
      : []),
  ];

  return actionButtons;
}

/** 提交审核 */
async function handleSubmitForApproval(row: DeclareDataFlowApi.DataFlow) {
  try {
    await submitDataFlow(row.id!);
    message.success('提交审核成功');
    handleRefresh();
    loadRowAvailableActions([row]);
  } catch (error) {
    console.error('提交审核失败:', error);
    message.error('提交审核失败');
  }
}

/** 处理BPM操作 */
async function handleBpmAction(row: DeclareDataFlowApi.DataFlow, action: any) {
  const { taskId, key, reasonRequire } = action;
  const buttonId = Number(key);

  try {
    if (buttonId === 1) {
      // 通过
      if (reasonRequire) {
        // TODO: 打开审批意见弹窗
        await approveTask({ id: taskId, buttonId, reason: '' });
      } else {
        await approveTask({ id: taskId, buttonId: 1, reason: '' });
      }
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

/** 创建数据流通记录 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑数据流通记录 */
function handleEdit(row: DeclareDataFlowApi.DataFlow) {
  formModalApi.setData(row).open();
}

/** 删除数据流通记录 */
async function handleDelete(row: DeclareDataFlowApi.DataFlow) {
  try {
    await deleteDataFlow(row.id!);
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
  { field: 'dataName', title: '数据名称', minWidth: 150 },
  { field: 'dataType', title: '数据类型', width: 120 },
  { field: 'flowType', title: '流通类型', width: 100, slots: { default: 'flowType' } },
  { field: 'projectName', title: '关联项目', width: 150 },
  { field: 'status', title: '状态', width: 100, slots: { default: 'status' } },
  { field: 'createTime', title: '创建时间', width: 160 },
  { field: 'creator', title: '创建人', width: 80 },
  { width: 250, slots: { default: 'actions' }, title: '操作' },
];

// 查询表单配置
const formSchema = [
  { field: 'dataName', label: '数据名称', component: 'Input', componentProps: { placeholder: '请输入数据名称' } },
  { field: 'dataType', label: '数据类型', component: 'Input', componentProps: { placeholder: '请输入数据类型' } },
  { field: 'flowType', label: '流通类型', component: 'Select', componentProps: { placeholder: '请选择流通类型', options: flowTypeOptions } },
  { field: 'status', label: '状态', component: 'Select', componentProps: { placeholder: '请选择状态', options: statusOptions } },
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
          const result = await getDataFlowPage({
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
  } as VxeTableGridOptions<DeclareDataFlowApi.DataFlow>,
});

</script>

<template>
  <Page auto-content-height>
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
      <!-- 流通类型插槽 -->
      <template #flowType="{ row }">
        {{ getFlowTypeLabel(row.flowType) }}
      </template>
      <!-- 状态插槽 -->
      <template #status="{ row }">
        <a-tag :color="row.status === 3 ? 'green' : row.status === 4 ? 'red' : row.status === 1 ? 'blue' : 'default'">
          {{ getStatusLabel(row.status) }}
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
