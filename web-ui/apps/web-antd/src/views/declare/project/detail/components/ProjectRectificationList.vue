<script lang="ts" setup>
import type { RectificationRecord } from '#/api/declare/project';

import { computed, h, onMounted, ref } from 'vue';

import { Button, message, Table, Tag } from 'ant-design-vue';

import { IconifyIcon } from '@vben/icons';

import { getDictOptions } from '@vben/hooks';
import { DICT_TYPE } from '@vben/constants';

import { ACTION_ICON, TableAction } from '#/adapter/vxe-table';
import { getRectificationRecordList } from '#/api/declare/project';

import ChildProcessDetailModal from './ChildProcessDetailModal.vue';

interface Props {
  projectId: number;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  refresh: [];
}>();

// 加载状态
const loading = ref(false);

// 整改记录列表数据
const rectificationList = ref<RectificationRecord[]>([]);

// 状态映射
const statusMap = ref<Record<string, { text: string; color: string; bg: string; border: string }>>({});

// 加载状态字典数据
async function loadStatusMap() {
  const dictOptions = getDictOptions(DICT_TYPE.DECLARE_PROJECT_STATUS, 'string');

  const map: Record<string, { text: string; color: string; bg: string; border: string }> = {};
  const colorMap: Record<string, { color: string; bg: string; border: string }> = {
    DRAFT: { color: '#6C737A', bg: '#F3F4F6', border: '#D4D9DF' },
    SAVED: { color: '#722ED1', bg: '#F9F0FF', border: '#D3ADF7' },
    SUBMITTED: { color: '#1890FF', bg: '#E6F7FF', border: '#91D5FF' },
    AUDITING: { color: '#FA8C16', bg: '#FFF7E6', border: '#FFD591' },
    APPROVED: { color: '#52C41A', bg: '#F6FFED', border: '#B7EB8F' },
    REJECTED: { color: '#FF4D4F', bg: '#FFF2F0', border: '#FFCCC7' },
    RUNNING: { color: '#1890FF', bg: '#E6F7FF', border: '#91D5FF' },
    COMPLETED: { color: '#52C41A', bg: '#F6FFED', border: '#B7EB8F' },
  };

  (dictOptions as any[]).forEach((item: { value: string; label: string }) => {
    const style = colorMap[item.value] || {
      color: '#6C737A',
      bg: '#F3F4F6',
      border: '#D4D9DF',
    };
    map[item.value] = {
      text: item.label,
      ...style,
    };
  });

  // 添加子流程状态映射
  map['RUNNING'] = { text: '进行中', color: '#1890FF', bg: '#E6F7FF', border: '#91D5FF' };
  map['COMPLETED'] = { text: '已完成', color: '#52C41A', bg: '#F6FFED', border: '#B7EB8F' };

  statusMap.value = map;
}

// 加载整改记录列表
async function loadRectificationList() {
  if (!props.projectId) return;

  loading.value = true;
  try {
    const data = await getRectificationRecordList(props.projectId);
    rectificationList.value = data || [];
  } catch (e) {
    console.error('加载整改记录失败', e);
    message.error('加载整改记录失败');
  } finally {
    loading.value = false;
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadStatusMap();
  loadRectificationList();
});

// 过滤子流程记录（recordType=2）
const childProcessRecords = computed(() => {
  return rectificationList.value.filter(item => item.recordType === 2);
});

// 子流程表格列配置
const childProcessColumns = [
  { title: '子流程ID', dataIndex: 'childProcessInstanceId', width: 220, ellipsis: true },
  { title: '子流程名称', dataIndex: 'title', ellipsis: true, align: 'center' as const },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100,
    align: 'center' as const,
    customRender: ({ record }: { record: RectificationRecord }) => {
      const statusInfo = statusMap.value[record.status || ''];
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
  { title: '发起人', dataIndex: 'startUserName', width: 120 },
  { title: '发起时间', dataIndex: 'createTime', width: 160 },
  { title: '结束时间', dataIndex: 'endTime', width: 160 },
  { title: '操作', key: 'action', width: '15%', align: 'center' as const },
];

// 获取子流程操作按钮
function getChildProcessActions(record: RectificationRecord) {
  const actions = [];

  // 查看按钮
  actions.push({
    label: '查看',
    type: 'link' as const,
    icon: ACTION_ICON.VIEW,
    onClick: () => handleViewChildProcess(record),
  });

  return actions;
}

// 子流程详情弹窗 ref
const childProcessDetailModalRef = ref<InstanceType<typeof ChildProcessDetailModal> | null>(null);

// 查看子流程详情
function handleViewChildProcess(record: RectificationRecord) {
  if (record.childProcessInstanceId) {
    childProcessDetailModalRef.value?.open({
      childProcessInstanceId: record.childProcessInstanceId,
      processDefinitionKey: record.processDefinitionKey,
      processDefinitionName: record.processDefinitionName,
      processId: record.processId,
      projectId: props.projectId,
    });
  }
}

// 格式化日期
function formatDateTime(value: string | number | undefined) {
  if (!value) return '-';
  const date = new Date(value);
  return date.toLocaleString('zh-CN');
}
</script>

<template>
  <div class="project-rectification-list">
    <!-- 操作栏 -->
    <div class="table-toolbar">
      <div class="toolbar-left">
        <h3 class="toolbar-title">
          <IconifyIcon icon="ant-design:branches-outlined" class="toolbar-icon" style="color: #FA8C16" />
          整改记录
        </h3>
      </div>
      <div class="toolbar-right">
        <Button @click="loadRectificationList" :loading="loading">
          <IconifyIcon icon="ant-design:reload-outlined" />
          刷新
        </Button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <Table
        v-if="childProcessRecords.length > 0"
        :columns="childProcessColumns"
        :data-source="childProcessRecords"
        :pagination="false"
        :scroll="{ x: 1200 }"
        row-key="childProcessInstanceId"
        class="process-table"
        :loading="loading"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <TableAction :actions="getChildProcessActions(record as RectificationRecord)" />
          </template>
          <template v-else-if="column.dataIndex === 'createTime' || column.dataIndex === 'endTime'">
            <span class="date-cell">{{ formatDateTime(record[column.dataIndex as keyof RectificationRecord] as any) }}</span>
          </template>
        </template>
      </Table>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <div class="empty-icon-wrapper">
          <IconifyIcon icon="ant-design:branches-outlined" class="empty-icon" />
        </div>
        <p class="empty-text">暂无整改记录</p>
      </div>
    </div>

    <!-- 子流程详情弹窗 -->
    <ChildProcessDetailModal ref="childProcessDetailModalRef" />
  </div>
</template>

<style lang="scss" scoped>
.project-rectification-list {
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

    .toolbar-right {
      display: flex;
      gap: 8px;
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
  }
}
</style>
