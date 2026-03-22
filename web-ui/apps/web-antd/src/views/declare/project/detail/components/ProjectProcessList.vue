<script lang="ts" setup>
import type { DeclareProjectApi } from '#/api/declare/project';
import type { DeclareExpertApi } from '#/api/declare/expert';

import { computed, h, onMounted, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { Button, message, Table, Tag } from 'ant-design-vue';

import { confirm } from '@vben/common-ui';

import { IconifyIcon } from '@vben/icons';

import { getDictOptions } from '@vben/hooks';
import { DICT_TYPE } from '@vben/constants';

import { ACTION_ICON, TableAction } from '#/adapter/vxe-table';
import { deleteProjectProcess, submitProcessReview } from '#/api/declare/project';
import { selectExpert } from '#/api/bpm/task';
import { useBpmApproval } from '#/composables/useBpmApproval';

import ProcessFormModal from './ProcessFormModal.vue';
import ProcessDetailModal from './ProcessDetailModal.vue';
import ApprovalModal from '#/components/bpm/ApprovalModal.vue';
import ReturnModal from '#/components/bpm/ReturnModal.vue';
import UserSelectModal from '#/components/bpm/UserSelectModal.vue';
import ExpertSelectModalCmp from '#/components/bpm/ExpertSelectModal.vue';

interface Props {
  projectId: number;
  processList: DeclareProjectApi.ProjectProcess[];
}

const props = defineProps<Props>();

const emit = defineEmits<{
  refresh: [];
}>();

// 当前登录用户ID
const userStore = useUserStore();
const currentUserId = computed(() => {
  return Number(userStore.userInfo?.userId) || userStore.userInfo?.id;
});

// 过程类型配置 - icon 和 color 从映射表获取
const processTypeStyleMap: Record<number, { icon: string; color: string }> = {
  1: { icon: 'ant-design:file-text-outlined', color: 'hsl(var(--primary))' },
  2: { icon: 'ant-design:file-text-outlined', color: '#A07852' },
  3: { icon: 'ant-design:check-circle-outlined', color: 'hsl(var(--primary-hover, var(--primary)))' },
  4: { icon: 'ant-design:tool-outlined', color: '#FA8C16' },
  5: { icon: 'ant-design:check-circle-outlined', color: '#9B3636' },
  6: { icon: 'ant-design:trophy-outlined', color: '#722ED1' },
  7: { icon: 'ant-design:rise-outlined', color: '#13C2C2' },
};

// 过程类型映射 - 从字典获取
const processTypeMap = ref<
  Record<number, { text: string; key: string; icon: string; color: string }>
>({});

// 加载字典数据
async function loadProcessTypeMap() {
  const dictOptions = getDictOptions(DICT_TYPE.DECLARE_PROCESS_TYPE, 'number');

  const map: Record<number, { text: string; key: string; icon: string; color: string }> = {};
  (dictOptions as any[]).forEach((item: { value: number; label: string }) => {
    // 隐藏第5项（整改记录）- 不在过程记录Tab显示
    if (item.value === 5) return;
    // 隐藏第7项（成果与流通）
    if (item.value === 7) return;

    const style = processTypeStyleMap[item.value] || {
      icon: 'ant-design:file-text-outlined',
      color: '#1890FF',
    };
    map[item.value] = {
      text: item.label,
      key: `process_${item.value}`,
      icon: style.icon,
      color: style.color,
    };
  });

  processTypeMap.value = map;
}

// 页面加载时获取字典数据
onMounted(() => {
  loadProcessTypeMap();
  loadStatusMap();
});

// 状态映射 - 从字典获取
const statusMap = ref<Record<string, { text: string; color: string; bg: string; border: string }>>({});

// 状态颜色映射（基于字典数据的样式）
const statusColorMap: Record<string, { color: string; bg: string; border: string }> = {
  DRAFT: { color: '#6C737A', bg: '#F3F4F6', border: '#D4D9DF' },
  SAVED: { color: '#722ED1', bg: '#F9F0FF', border: '#D3ADF7' },
  SUBMITTED: { color: '#1890FF', bg: '#E6F7FF', border: '#91D5FF' },
  AUDITING: { color: '#FA8C16', bg: '#FFF7E6', border: '#FFD591' },
  APPROVED: { color: '#52C41A', bg: '#F6FFED', border: '#B7EB8F' },
  REJECTED: { color: '#FF4D4F', bg: '#FFF2F0', border: '#FFCCC7' },
};

// 加载状态字典数据
async function loadStatusMap() {
  const dictOptions = getDictOptions(DICT_TYPE.DECLARE_PROJECT_STATUS, 'string');

  const map: Record<string, { text: string; color: string; bg: string; border: string }> = {};
  (dictOptions as any[]).forEach((item: { value: string; label: string }) => {
    const style = statusColorMap[item.value] || {
      color: '#6C737A',
      bg: '#F3F4F6',
      border: '#D4D9DF',
    };
    map[item.value] = {
      text: item.label,
      ...style,
    };
  });
  statusMap.value = map;
}

// 当前激活的子表
const activeSubTab = ref('process_1');

// 根据当前激活的 Tab 生成 businessType
const currentBusinessType = computed(() => {
  // activeSubTab 格式：process_1, process_2, ...
  const match = activeSubTab.value.match(/^process_(\d+)$/);
  if (match?.[1]) {
    const type = match[1];
    return `project_process:type:${type}`;
  }
  return 'project_process:type:1';
});

// 当前编辑的过程记录
const currentProcess = ref<DeclareProjectApi.ProjectProcess | null>(null);

// 过程记录表数据（按类型分组）
const groupedProcessList = computed(() => {
  const groups: Record<string, DeclareProjectApi.ProjectProcess[]> = {};

  // 使用 processTypeMap 的 key 初始化分组
  Object.values(processTypeMap.value).forEach((typeInfo) => {
    groups[typeInfo.key] = [];
  });

  // 确保至少有一个分组（防止字典未加载时报错）
  if (Object.keys(groups).length === 0) {
    groups['process_1'] = [];
  }

  props.processList.forEach((process) => {
    const typeInfo = processTypeMap.value[process.processType || 1];
    if (typeInfo && typeInfo.key) {
      const key = typeInfo.key as keyof typeof groups;
      if (groups[key]) {
        groups[key].push(process);
      }
    }
  });

  return groups;
});

// 当前Tab的过程记录
const currentProcesses = computed(() => {
  return groupedProcessList.value[activeSubTab.value] || [];
});

// 获取当前Tab的样式信息
const currentTabInfo = computed(() => {
  const info = processTypeMap.value[getProcessTypeByKey(activeSubTab.value)] || processTypeMap.value[1];
  // 提供默认值以防字典未加载
  return info || {
    text: '建设过程',
    key: 'process_1',
    icon: 'ant-design:file-text-outlined',
    color: 'hsl(var(--primary))',
  };
});

// 表格列配置
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const columns: any[] = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '标题', dataIndex: 'processTitle', ellipsis: true , align: 'center' },
  {
    title: '状态',
    dataIndex: 'status',
    width: 140,
    align: 'center',
    customRender: ({ record }: { record: DeclareProjectApi.ProjectProcess }) => {
      const statusInfo = statusMap.value[record.status || 'DRAFT'];
      if (!statusInfo) {
        return h(Tag, { color: '#F3F4F6' }, () => '未知');
      }
      return h(
        Tag,
        {
          color: statusInfo.bg,
          style: {
            color: statusInfo.color,
            borderColor: statusInfo.border,
          },
        },
        () => statusInfo?.text || '-'
      );
    },
  },
  { title: '报告期-开始时间', dataIndex: 'reportPeriodStart', width: 160, align: 'center' },
  { title: '报告期-结束时间', dataIndex: 'reportPeriodEnd', width: 160, align: 'center' },
  // {
  //   title: '报告期',
  //   children: [
  //     { title: '开始', dataIndex: 'reportPeriodStart', width: 140 },
  //     { title: '结束', dataIndex: 'reportPeriodEnd', width: 140 },
  //   ],
  // },
  { title: '提交时间', dataIndex: 'reportTime', width: 160 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: '20%', align: 'center' },
];

// 获取操作按钮
function getActions(record: DeclareProjectApi.ProjectProcess) {
  const actions = [];
  const statusVal = record.status as unknown;

  // 判断是否为创建人（使用宽松比较，处理数字和字符串类型）
  const isCreator = record.creatorId != null && Number(record.creatorId) === Number(currentUserId.value);

  // 状态判断常量
  const isEditable =
    statusVal != null &&
    (['DRAFT', 'SAVED', 'RECTIFICATION_SUBMIT'].includes(statusVal as string) || (statusVal as string).includes('RETURNED'));
  const isDeletable =
    statusVal != null &&
    (['DRAFT', 'SAVED', 'RECTIFICATION_SUBMIT'].includes(statusVal as string) || (statusVal as string).includes('RETURNED'));

  // 检查该记录是否有待办任务（BPM 返回的审批按钮）
  const hasApprovalActions = (rowAvailableActions.value[record.id!] || []).length > 0;

  // 发起流程：仅 SAVED 状态可以发起，且没有待办任务时才显示
  const canStartProcess = statusVal === 'SAVED' && !hasApprovalActions;

  // DRAFT/SAVED/RECTIFICATION_SUBMIT/包含RETURNED的状态：创建人可操作（没有待办任务时显示）
  if (isEditable && isCreator && !hasApprovalActions) {
    // 编辑按钮（DRAFT/SAVED/RECTIFICATION_SUBMIT/包含RETURNED的状态可见，没有待办任务时显示）
    actions.push({
      label: '编辑',
      type: 'link' as const,
      auth: ['declare:project-process:update'],
      icon: ACTION_ICON.EDIT,
      onClick: () => handleEdit(record),
    });
  }

  // 删除按钮（仅 DRAFT/SAVED/RECTIFICATION_SUBMIT/包含RETURNED的状态的创建人可见，没有待办任务时显示）
  if (isDeletable && isCreator && !hasApprovalActions) {
    actions.push({
      label: '删除',
      type: 'link' as const,
      auth: ['declare:project-process:delete'],
      danger: true,
      icon: ACTION_ICON.DELETE,
      popConfirm: {
        title: `确定删除 "${record.processTitle}" 吗？`,
        confirm: () => handleDelete(record),
      },
    });
  }

  // 发起流程按钮（仅 SAVED 状态可见，且没有待办任务时显示）
  if (canStartProcess) {
    actions.push({
      label: '发起流程',
      type: 'link' as const,
      auth: ['declare:project-process:create'],
      icon: 'ant-design:send-outlined',
      onClick: () => handleStartProcess(record),
    });
  }

  // 查看按钮（所有状态都可见）
  actions.push({
    label: '查看',
    type: 'link' as const,
    onClick: () => handleView(record),
  });

  // 审批操作（如果有待办任务）
  const approvalActions = rowAvailableActions.value[record.id!] || [];
  console.log('[DEBUG] approvalActions for record', record.id, ':', approvalActions);
  for (const action of approvalActions) {
    const btnKey = action.buttonKey;
    const btnConfig = btnKey ? action.buttonSettings?.[btnKey] : undefined;
    console.log('[DEBUG] action:', action, 'btnKey:', btnKey, 'btnConfig:', btnConfig);
    if (btnKey && btnConfig?.enable) {
      // RECTIFICATION_SUBMIT 状态需要显示编辑按钮
      if (btnConfig.bizStatus === 'RECTIFICATION_SUBMIT') {
        actions.push({
          label: '编辑',
          type: 'link' as const,
          auth: ['declare:project-process:update'],
          icon: ACTION_ICON.EDIT,
          onClick: () => handleEdit(record, btnConfig.bizStatus),
        });
      }

      actions.push({
        label: btnConfig.displayName,
        type: 'link' as const,
        onClick: () => handleApprovalAction(action),
      });
    }
  }

  return actions;
}

// 新建过程记录
function handleCreate() {
  // 打开弹窗，传递当前 Tab 的过程类型
  processFormModalApi.setData({
    process: null,
    processType: getProcessTypeByKey(activeSubTab.value),
  });
  processFormModalApi.open();
}

// 编辑过程记录
function handleEdit(record: DeclareProjectApi.ProjectProcess, bizStatus?: string) {
  console.log('[DEBUG handleEdit] record:', record, 'bizStatus:', bizStatus);
  currentProcess.value = record;
  processFormModalApi.setData({ process: record, bizStatus });
  processFormModalApi.open();
}

// 查看过程记录（打开详情弹窗）
function handleView(record: DeclareProjectApi.ProjectProcess) {
  processDetailModalApi.setData({
    processId: record.id,
    projectId: record.projectId,
    processType: record.processType,
  }).open();
}

// 删除过程记录
async function handleDelete(record: DeclareProjectApi.ProjectProcess) {
  try {
    await deleteProjectProcess(record.id!);
    message.success('删除成功');
    emit('refresh');
  } catch (e) {
    console.error('删除失败', e);
    message.error('删除失败');
  }
}

// 发起流程（提交审核）
async function handleStartProcess(record: DeclareProjectApi.ProjectProcess) {
  try {
    // 获取过程类型名称
    const processTypeInfo = processTypeMap.value[record.processType || 1];
    const processTypeName = processTypeInfo?.text || '该过程记录';
    const title = record.processTitle || processTypeName;

    // REJECTED 状态需要提示重新提交
    const statusVal = record.status as unknown;
    const confirmMessage = statusVal === 'REJECTED'
      ? `当前状态为「退回」，确定要重新修改并提交「${title}」进行审核吗？`
      : `确定要提交「${title}」进行审核吗？提交后将进入审批流程。`;

    // 确认弹窗 - 用户取消会抛出一个包含 "cancel" 的错误
    await confirm(confirmMessage);

    // 确认后执行提交（状态更新由 DeclareProcessStartedListener 通过事件驱动）
    const processType = record.processType || 1;
    const businessType = `project_process:type:${processType}`;
    await submitProcessReview(record.id!, businessType);

    message.success('提交审核成功');
    emit('refresh');
  } catch (e: any) {
    // 用户取消时不处理
    if (e?.message?.includes?.('cancel') || e === 'cancel' || e === 'esc') {
      return;
    }
    // 其他错误（提交失败）
    console.error('提交审核失败', e);
    message.error('提交审核失败，请重试');
  }
}

// 根据 key 获取过程类型
function getProcessTypeByKey(key: string): number {
  // key 格式为 process_1, process_2 等
  const match = key.match(/^process_(\d+)$/);
  if (match?.[1]) {
    return parseInt(match[1], 10);
  }
  // 兼容旧的 key 格式
  const reverseMap: Record<string, number> = {
    construction: 1,
    annual: 2,
    midterm: 3,
    rectification: 4,
    acceptance: 5,
  };
  return reverseMap[key] || 1;
}

// 格式化日期
function formatDateTime(value: string | number | undefined) {
  if (!value) return '-';
  const date = new Date(value);
  return date.toLocaleString('zh-CN');
}

// 过程记录表单弹窗
const [processFormModal, processFormModalApi] = useVbenModal({
  connectedComponent: ProcessFormModal,
  destroyOnClose: true,
  onCancel() {
    // 取消时刷新列表
    emit('refresh');
  },
});

// 过程记录详情弹窗
const [processDetailModal, processDetailModalApi] = useVbenModal({
  connectedComponent: ProcessDetailModal,
  destroyOnClose: true,
  onCancel() {
    // 取消时刷新列表
    emit('refresh');
  },
});

// ========== 审批相关 Hook ==========
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
  tableName: 'project',
  businessType: currentBusinessType.value,
  onRefresh: () => {
    emit('refresh');
  },
  onCustomReturn: (taskId: string) => {
    // 自定义退回处理
    returnModalRef.value?.open({ taskId, title: '退回' });
    return true;
  },
  onOpenExpertSelect: (taskId: string) => {
    // 使用自定义的专家选择弹窗
    openExpertSelectModal(taskId);
    return true;
  },
});

// ========== 专家选择弹窗相关 ==========
const expertSelectVisible = ref(false);
const currentExpertTaskId = ref<string>('');

// 打开专家选择弹窗
function openExpertSelectModal(taskId: string) {
  currentExpertTaskId.value = taskId;
  expertSelectVisible.value = true;
}

// 处理专家选择确认
async function handleExpertSelectConfirm(experts: DeclareExpertApi.Expert[]) {
  if (!currentExpertTaskId.value) {
    return;
  }

  // 提取专家关联的系统用户ID
  const expertUserIds = experts
    .map((e) => e.userId)
    .filter((userId): userId is number => userId !== undefined && userId !== null);

  if (expertUserIds.length === 0) {
    message.error('所选专家未关联系统用户，无法提交');
    return;
  }

  // 调用选择专家API
  try {
    await selectExpert({
      id: currentExpertTaskId.value,
      userIds: expertUserIds,
    });
    message.success('已选择专家');
    emit('refresh');
  } catch (error) {
    console.error('选择专家失败:', error);
    message.error('选择专家失败');
  }
}

// 监听 businessType 变化，更新审批加载
watch(
  () => currentBusinessType.value,
  async (newVal, oldVal) => {
    console.log('[DEBUG] ProjectProcessList businessType changed:', oldVal, '->', newVal);
    // 当 businessType 变化时，重新加载审批操作
    if (props.processList && props.processList.length > 0) {
      const rowsWithId = props.processList.filter((item): item is { id: number } => !!item.id);
      if (rowsWithId.length > 0) {
        console.log('[DEBUG] Loading actions for businessType:', newVal);
        await loadRowAvailableActions(rowsWithId, newVal);
      }
    }
  },
);

// 监听 processList 变化，自动加载审批操作
watch(
  () => props.processList,
  async (newList) => {
    if (newList && newList.length > 0) {
      const rowsWithId = newList.filter((item): item is { id: number } => !!item.id);
      if (rowsWithId.length > 0) {
        await loadRowAvailableActions(rowsWithId, currentBusinessType.value);
      }
    }
  },
  { immediate: true, deep: true }
);
</script>

<template>
  <div class="project-process-list">
    <!-- Tab 导航 - GemDesign 风格 -->
    <div class="tab-navigation">
      <div class="tab-scroll-container">
        <div class="tab-scroll-content">
          <button
            v-for="(info, key) in processTypeMap"
            :key="key"
            class="tab-button"
            :class="{ 'tab-button-active': activeSubTab === info.key }"
            @click="activeSubTab = info.key"
          >
            <IconifyIcon :icon="info.icon" class="tab-icon" />
            <span class="tab-text">{{ info.text }}</span>
            <span
              v-if="groupedProcessList[info.key]?.length"
              class="tab-badge"
            >
              {{ groupedProcessList[info.key]?.length }}
            </span>
          </button>
        </div>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="table-toolbar">
      <div class="toolbar-left">
        <h3 class="toolbar-title">
          <IconifyIcon :icon="currentTabInfo.icon" class="toolbar-icon" :style="{ color: currentTabInfo.color }" />
          {{ currentTabInfo.text }}
        </h3>
      </div>
      <TableAction
        :actions="[
          {
            label: '新建',
            type: 'primary',
            auth: ['declare:project-process:create'],
            onClick: handleCreate,
          },
        ]"
      >
        <template #button>
          <Button type="primary" class="create-button">
            <IconifyIcon icon="ant-design:plus-outlined" />
            新建
          </Button>
        </template>
      </TableAction>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <Table
        v-if="currentProcesses.length > 0"
        :columns="columns"
        :data-source="currentProcesses"
        :pagination="false"
        :scroll="{ x: 1000 }"
        row-key="id"
        class="process-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <TableAction :actions="getActions(record)" />
          </template>
          <template v-else-if="column.dataIndex === 'reportTime' || column.dataIndex === 'createTime'">
            <span class="date-cell">{{ formatDateTime(record[column.dataIndex as keyof DeclareProjectApi.ProjectProcess] as any) }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'reportPeriodStart' || column.dataIndex === 'reportPeriodEnd'">
            <span class="date-cell">{{ formatDateTime(record[column.dataIndex as keyof DeclareProjectApi.ProjectProcess] as any) }}</span>
          </template>
        </template>
      </Table>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <div class="empty-icon-wrapper">
          <IconifyIcon :icon="currentTabInfo.icon" class="empty-icon" />
        </div>
        <p class="empty-text">暂无{{ currentTabInfo.text }}数据</p>
        <TableAction
          :actions="[
            {
              label: '点击新建',
              type: 'link',
              auth: ['declare:project-process:create'],
              onClick: handleCreate,
            },
          ]"
        >
          <template #button>
            <Button type="link" class="empty-link">
              <IconifyIcon :icon="currentTabInfo.icon" />
              点击新建
            </Button>
          </template>
        </TableAction>
      </div>
    </div>

    <!-- 过程记录表单弹窗 -->
    <component
      :is="processFormModal"
      :project-id="projectId"
      :process-type="getProcessTypeByKey(activeSubTab)"
      :process="currentProcess as any"
      @refresh="emit('refresh')"
    />

    <!-- 审批弹窗（通过/拒绝） -->
    <ApprovalModal
      ref="approvalModalRef"
      @confirm="(reason, action) => confirmApproval(reason, action)"
    />

    <!-- 退回弹窗 -->
    <ReturnModal ref="returnModalRef" @success="emit('refresh')" />

    <!-- 过程记录详情弹窗 -->
    <component :is="processDetailModal" />

    <!-- 用户选择弹窗（转办/委派/加签/抄送/选择专家） -->
    <UserSelectModal
      ref="userSelectModalRef"
      :width="'60%'"
      @confirm="handleUserSelectConfirm"
    />

    <!-- 专家选择弹窗 -->
    <ExpertSelectModalCmp
      v-model:open="expertSelectVisible"
      @confirm="handleExpertSelectConfirm"
    />
  </div>
</template>

<style lang="scss" scoped>
.project-process-list {
  // Tab 导航样式 - GemDesign 风格
  .tab-navigation {
    background: linear-gradient(135deg, hsl(var(--primary)) 0%, hsl(var(--primary-hover, var(--primary))) 100%);
    border-radius: 12px;
    padding: 4px;
    margin-bottom: 20px;
    box-shadow: 0 4px 12px rgba(42, 92, 69, 0.15);
  }

  .tab-scroll-container {
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  .tab-scroll-content {
    display: flex;
    gap: 4px;
    min-width: max-content;
  }

  .tab-button {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 20px;
    border: none;
    background: transparent;
    color: rgba(255, 255, 255, 0.8);
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    border-radius: 8px;
    transition: all 0.3s ease;
    white-space: nowrap;

    .tab-icon {
      font-size: 14px;
    }

    .tab-badge {
      background: rgba(255, 255, 255, 0.2);
      padding: 2px 8px;
      border-radius: 10px;
      font-size: 12px;
    }

    &:hover {
      background: rgba(255, 255, 255, 0.1);
      color: #fff;
    }

    &.tab-button-active {
      background: #fff;
      color: hsl(var(--primary));
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);

      .tab-badge {
        background: hsl(var(--primary));
        color: #fff;
      }
    }
  }

  // 操作栏样式
  .table-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 12px 16px;
    background: #fafafa;
    border-radius: 8px;
    border: 1px solid #e8e8e8;

    .toolbar-left {
      display: flex;
      align-items: center;
    }

    .toolbar-title {
      margin: 0;
      font-size: 15px;
      font-weight: 600;
      color: #262626;
      display: flex;
      align-items: center;
      gap: 8px;

      .toolbar-icon {
        font-size: 16px;
      }
    }

    .create-button {
      background: hsl(var(--primary));
      border-color: hsl(var(--primary));

      &:hover {
        background: hsl(var(--primary-hover, var(--primary)));
        border-color: hsl(var(--primary-hover, var(--primary)));
      }
    }
  }

  // 表格容器样式
  .table-container {
    background: #fff;
    border-radius: 8px;
    border: 1px solid #e8e8e8;
    overflow: hidden;
  }

  .process-table {
    :deep(.ant-table) {
      .ant-table-thead > tr > th {
        background: #fafafa;
        font-weight: 600;
        color: #262626;
      }

      .ant-table-tbody > tr:hover > td {
        background: #f6ffed;
      }
    }

    .date-cell {
      color: #595959;
      font-size: 13px;
    }
  }

  // 空状态样式
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 20px;
    background: #fafafa;

    .empty-icon-wrapper {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: linear-gradient(135deg, #E6EEE8 0%, #D4E4DD 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 16px;

      .empty-icon {
        font-size: 32px;
        color: hsl(var(--primary));
      }
    }

    .empty-text {
      color: #8c8c8c;
      font-size: 14px;
      margin-bottom: 8px;
    }

    .empty-link {
      color: hsl(var(--primary));
      font-size: 14px;

      &:hover {
        color: hsl(var(--primary-hover, var(--primary)));
      }
    }
  }
}
</style>
