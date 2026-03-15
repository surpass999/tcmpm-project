<script lang="ts" setup>
import type { DeclareProjectApi } from '#/api/declare/project';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Button, Card, message, Table, Tag } from 'ant-design-vue';

import { IconifyIcon } from '@vben/icons';

import { ACTION_ICON, TableAction } from '#/adapter/vxe-table';
import { deleteProjectProcess } from '#/api/declare/project';

import ProcessFormModal from '../modules/ProcessFormModal.vue';

interface Props {
  projectId: number;
  processList: DeclareProjectApi.ProjectProcess[];
}

const props = defineProps<Props>();

const emit = defineEmits<{
  refresh: [];
}>();

// 过程类型映射 - 使用可用的 Vben 图标
const processTypeMap: Record<number, { text: string; key: string; icon: string; color: string }> = {
  1: { text: '建设过程', key: 'construction', icon: 'ant-design:file-text-outlined', color: '#2A5C45' },
  2: { text: '年度总结', key: 'annual', icon: 'ant-design:file-text-outlined', color: '#A07852' },
  3: { text: '中期评估', key: 'midterm', icon: 'ant-design:check-circle-outlined', color: '#4A8F72' },
  4: { text: '整改记录', key: 'rectification', icon: 'ant-design:tool-outlined', color: '#FA8C16' },
  5: { text: '验收申请', key: 'acceptance', icon: 'ant-design:check-circle-outlined', color: '#9B3636' },
};

// 状态映射
const statusMap: Record<number, { text: string; color: string; bg: string; border: string }> = {
  0: { text: '草稿', color: '#6C737A', bg: '#F3F4F6', border: '#D4D9DF' },
  1: { text: '已提交', color: '#1890FF', bg: '#E6F7FF', border: '#91D5FF' },
  2: { text: '审核中', color: '#FA8C16', bg: '#FFF7E6', border: '#FFD591' },
  3: { text: '通过', color: '#52C41A', bg: '#F6FFED', border: '#B7EB8F' },
  4: { text: '退回', color: '#FF4D4F', bg: '#FFF2F0', border: '#FFCCC7' },
};

// 当前激活的子表
const activeSubTab = ref('construction');

// 加载状态
const loading = ref(false);

// 当前编辑的过程记录
const currentProcess = ref<DeclareProjectApi.ProjectProcess | null>(null);

// 过程记录表数据（按类型分组）
const groupedProcessList = computed(() => {
  const groups: Record<string, DeclareProjectApi.ProjectProcess[]> = {
    construction: [],
    annual: [],
    midterm: [],
    rectification: [],
    acceptance: [],
  };

  props.processList.forEach((process) => {
    const typeInfo = processTypeMap[process.processType || 1];
    if (typeInfo) {
      groups[typeInfo.key].push(process);
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
  return processTypeMap[getProcessTypeByKey(activeSubTab.value)] || processTypeMap[1];
});

// 表格列配置
const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '标题', dataIndex: 'processTitle', ellipsis: true },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100,
    customRender: ({ record }: { record: DeclareProjectApi.ProjectProcess }) => {
      const statusInfo = statusMap[record.status || 0];
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
  {
    title: '报告期',
    children: [
      { title: '开始', dataIndex: 'reportPeriodStart', width: 140 },
      { title: '结束', dataIndex: 'reportPeriodEnd', width: 140 },
    ],
  },
  { title: '提交时间', dataIndex: 'reportTime', width: 160 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' },
];

// 获取操作按钮
function getActions(record: DeclareProjectApi.ProjectProcess) {
  const actions = [];

  // 草稿状态：可以编辑和删除
  if (record.status === 0) {
    actions.push({
      label: '编辑',
      type: 'link' as const,
      icon: ACTION_ICON.EDIT,
      onClick: () => handleEdit(record),
    });
    actions.push({
      label: '删除',
      type: 'link' as const,
      danger: true,
      icon: ACTION_ICON.DELETE,
      popConfirm: {
        title: `确定删除 "${record.processTitle}" 吗？`,
        confirm: () => handleDelete(record),
      },
    });
  }

  // 已提交/审核中/通过状态：只能查看
  if (record.status === 1 || record.status === 2 || record.status === 3) {
    actions.push({
      label: '查看',
      type: 'link' as const,
      onClick: () => handleView(record),
    });
  }

  // 退回状态：可以重新编辑
  if (record.status === 4) {
    actions.push({
      label: '编辑',
      type: 'link' as const,
      icon: ACTION_ICON.EDIT,
      onClick: () => handleEdit(record),
    });
  }

  return actions;
}

// 新建过程记录
function handleCreate() {
  currentProcess.value = null;
  processFormModalApi.setData({
    projectId: props.projectId,
    processType: getProcessTypeByKey(activeSubTab.value),
    process: null,
  }).open();
}

// 编辑过程记录
function handleEdit(record: DeclareProjectApi.ProjectProcess) {
  currentProcess.value = record;
  processFormModalApi.setData({
    projectId: props.projectId,
    processType: record.processType,
    process: record,
  }).open();
}

// 查看过程记录（只读）
function handleView(record: DeclareProjectApi.ProjectProcess) {
  currentProcess.value = record;
  processFormModalApi.setData({
    projectId: props.projectId,
    processType: record.processType,
    process: record,
    readonly: true,
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

// 根据 key 获取过程类型
function getProcessTypeByKey(key: string): number {
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
  onConfirm: () => {
    emit('refresh');
  },
});

// 引入 h 函数
import { h } from 'vue';
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
              {{ groupedProcessList[info.key].length }}
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
      <Button type="primary" class="create-button" @click="handleCreate">
        <template #icon>
          <IconifyIcon icon="ant-design:plus-outlined" />
        </template>
        新建
      </Button>
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
        <Button type="link" class="empty-link" @click="handleCreate">
          <IconifyIcon icon="ant-design:plus-outlined" />
          点击新建
        </Button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.project-process-list {
  // Tab 导航样式 - GemDesign 风格
  .tab-navigation {
    background: linear-gradient(135deg, #2A5C45 0%, #4A8F72 100%);
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
      color: #2A5C45;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);

      .tab-badge {
        background: #2A5C45;
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
      background: #2A5C45;
      border-color: #2A5C45;

      &:hover {
        background: #4A8F72;
        border-color: #4A8F72;
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
        color: #2A5C45;
      }
    }

    .empty-text {
      color: #8c8c8c;
      font-size: 14px;
      margin-bottom: 8px;
    }

    .empty-link {
      color: #2A5C45;
      font-size: 14px;

      &:hover {
        color: #4A8F72;
      }
    }
  }
}
</style>
