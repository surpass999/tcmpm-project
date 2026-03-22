<script lang="ts" setup>
import type { BpmProcessInstanceApi } from '#/api/bpm/processInstance';

import { nextTick, ref } from 'vue';

import { Card, Descriptions, message, Modal, Spin, Steps, Tag, Timeline, TimelineItem } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

import { getApprovalDetail } from '#/api/bpm/processInstance';

// 定义 Props 接口
interface Props {
  childProcessInstanceId?: string;
  processDefinitionKey?: string;
  processDefinitionName?: string;
  processId?: number;
  projectId?: number;
}

defineProps<Props>();

// 本地响应式状态（用于存储传入的数据）
const localData = ref<Props>({});

// 弹窗是否可见
const visible = ref(false);

// 加载状态
const loading = ref(false);

// 流程实例详情
const processInstanceDetail = ref<BpmProcessInstanceApi.ApprovalDetailRespVO | null>(null);

// 流程进度节点
const processNodes = ref<{ id: string; name: string; status: number }[]>([]);

// 审批历史
const approvalHistory = ref<any[]>([]);

// 当前流程节点索引
const currentNodeIndex = ref(0);

// 弹窗标题
const modalTitle = ref('子流程详情');

// 暴露 open 方法供外部调用
function open(data: Partial<Props>) {
  // 使用本地状态存储数据
  localData.value = {
    childProcessInstanceId: data.childProcessInstanceId,
    processDefinitionKey: data.processDefinitionKey,
    processDefinitionName: data.processDefinitionName,
    processId: data.processId,
    projectId: data.projectId,
  };

  // 设置标题
  if (data.processDefinitionName) {
    modalTitle.value = data.processDefinitionName;
  } else {
    modalTitle.value = '子流程详情';
  }

  // 重置数据
  resetData();
  // 显示弹窗
  visible.value = true;
  // 加载数据
  nextTick(() => {
    loadChildProcessDetail();
  });
}

// 关闭弹窗
function handleClose() {
  visible.value = false;
  resetData();
}

// 重置数据
function resetData() {
  processInstanceDetail.value = null;
  processNodes.value = [];
  approvalHistory.value = [];
  currentNodeIndex.value = 0;
}

// 加载子流程详情
async function loadChildProcessDetail() {
  if (!localData.value.childProcessInstanceId) return;

  loading.value = true;
  try {
    const result = await getApprovalDetail({
      processInstanceId: localData.value.childProcessInstanceId,
    });
    processInstanceDetail.value = result;
    parseApprovalDetail(result);
  } catch (e) {
    console.error('加载子流程详情失败', e);
    message.error('加载子流程详情失败');
  } finally {
    loading.value = false;
  }
}

// 解析审批详情
function parseApprovalDetail(detail: BpmProcessInstanceApi.ApprovalDetailRespVO) {
  if (!detail?.activityNodes?.length) return;

  // 构建流程节点
  const nodes: { id: string; name: string; status: number }[] = [];
  detail.activityNodes.forEach((node: BpmProcessInstanceApi.ApprovalNodeInfo) => {
    let status = 0;
    if (node.status === 2 || node.endTime) {
      status = 2; // 已完成
    } else if (node.status === 1 || node.startTime) {
      status = 1; // 进行中
    } else if (node.status === 3) {
      status = 3; // 拒绝
    } else if (node.status === 5) {
      status = 3; // 退回
    }
    nodes.push({ id: node.id, name: node.name, status });
  });
  processNodes.value = nodes;

  // 计算当前节点索引
  const idx = nodes.findIndex((n) => n.status === 1);
  currentNodeIndex.value = idx >= 0 ? idx : nodes.length - 1;

  // 构建审批历史
  const history: any[] = [];
  detail.activityNodes.forEach((node: BpmProcessInstanceApi.ApprovalNodeInfo) => {
    if (node.tasks && node.tasks.length > 0) {
      node.tasks.forEach((task: BpmProcessInstanceApi.ApprovalTaskInfo) => {
        let actionStatus = '审核';
        if (node.status === 2) actionStatus = '通过';
        else if (node.status === 3) actionStatus = '拒绝';
        else if (node.status === 5) actionStatus = '退回';

        history.push({
          name: node.name,
          status: actionStatus,
          opinion: task.reason,
          reviewerName:
            task.assigneeUser?.nickname || task.assigneeUser?.name || '-',
          reviewTime: task.endTime || node.endTime,
        });
      });
    }
  });

  // 按时间正序排列
  history.sort((a, b) => {
    const timeA = a.reviewTime || '';
    const timeB = b.reviewTime || '';
    return String(timeA).localeCompare(String(timeB));
  });
  approvalHistory.value = history;
}

// 格式化日期
function formatDateTime(value: string | number | Date | undefined) {
  if (!value) return '-';
  const date = new Date(value);
  if (isNaN(date.getTime())) return '-';
  return date.toLocaleString('zh-CN');
}

// 获取流程状态
function getProcessStatusText(status: number | undefined) {
  if (!status) return '-';
  if (status === 2) return '已结束';
  if (status === 1) return '进行中';
  return '未知';
}

// 获取流程状态颜色
function getProcessStatusColor(status: number | undefined) {
  if (status === 2) return 'green';
  if (status === 1) return 'processing';
  return 'default';
}

defineExpose({ open });
</script>

<template>
  <!-- 使用 Teleport 将弹窗传送到 body，避免嵌套在父弹窗内部导致的问题 -->
  <Teleport to="body">
    <Modal
      v-model:open="visible"
      :title="processInstanceDetail?.processInstance?.name || modalTitle"
      :width="700"
      :footer="null"
      :maskClosable="true"
      :destroyOnClose="true"
      @cancel="handleClose"
    >
      <Spin :spinning="loading">
        <div class="child-process-detail-content">
          <!-- 流程基本信息 -->
          <Card title="流程信息" :bordered="false" class="mb-4">
            <Descriptions :column="2" bordered size="small">
              <Descriptions.Item label="流程名称">
                {{ processInstanceDetail?.processInstance?.name || '-' }}
              </Descriptions.Item>
              <Descriptions.Item label="流程状态">
                <Tag :color="getProcessStatusColor(processInstanceDetail?.status)">
                  {{ getProcessStatusText(processInstanceDetail?.status) }}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="开始时间">
                {{ formatDateTime(processInstanceDetail?.processInstance?.startTime) }}
              </Descriptions.Item>
              <Descriptions.Item label="结束时间">
                {{ formatDateTime(processInstanceDetail?.processInstance?.endTime) }}
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <!-- 流程进度 -->
          <Card v-if="processNodes.length > 0" title="流程进度" :bordered="false" class="mb-4">
            <Steps :current="currentNodeIndex" size="small">
              <Steps.Step
                v-for="node in processNodes"
                :key="node.id"
                :title="node.name"
                :status="node.status === 2 ? 'finish' : node.status === 1 ? 'process' : node.status === 3 ? 'error' : 'wait'"
              />
            </Steps>
          </Card>

          <!-- 审批历史 -->
          <Card v-if="approvalHistory.length > 0" title="审批历史" :bordered="false" class="mb-4">
            <Timeline>
              <TimelineItem
                v-for="(item, index) in approvalHistory"
                :key="index"
                :color="item.status === '通过' ? 'green' : item.status === '退回' || item.status === '拒绝' ? 'red' : 'blue'"
              >
                <div class="approval-history-item">
                  <div class="approval-history-header">
                    <span class="approval-type">{{ item.name }}</span>
                    <Tag
                      :color="item.status === '通过' ? 'green' : item.status === '退回' || item.status === '拒绝' ? 'red' : 'orange'"
                    >
                      {{ item.status }}
                    </Tag>
                  </div>
                  <div v-if="item.reviewerName" class="approval-reviewer">
                    审批人：{{ item.reviewerName }}
                  </div>
                  <div v-if="item.opinion" class="approval-opinion">
                    审批意见：{{ item.opinion }}
                  </div>
                  <div class="approval-time">
                    {{ formatDateTime(item.reviewTime) }}
                  </div>
                </div>
              </TimelineItem>
            </Timeline>
          </Card>

          <!-- 空状态 -->
          <div
            v-if="!loading && approvalHistory.length === 0 && processNodes.length === 0"
            class="empty-state"
          >
            <div class="empty-icon-wrapper">
              <IconifyIcon icon="ant-design:branches-outlined" class="empty-icon" />
            </div>
            <p class="empty-text">暂无审批记录</p>
          </div>
        </div>
      </Spin>
    </Modal>
  </Teleport>
</template>

<style lang="scss" scoped>
.child-process-detail-content {
  max-height: 70vh;
  overflow-y: auto;
  padding: 4px;
}

.mb-4 {
  margin-bottom: 16px;
}

:deep(.ant-card) {
  border-radius: 8px;
  box-shadow: 0 1px 2px -1px hsl(var(--primary) / 0.1),
    0 1px 3px -1px hsl(var(--primary) / 0.1);
  border: 1px solid hsl(var(--border));

  .ant-card-head {
    border-bottom-color: hsl(var(--border));
    background: linear-gradient(
      135deg,
      hsl(var(--primary) / 0.02) 0%,
      hsl(var(--background)) 100%
    );

    .ant-card-head-title {
      font-weight: 600;
      color: hsl(var(--foreground));
    }
  }

  .ant-card-body {
    padding: 16px;
  }
}

:deep(.ant-descriptions) {
  .ant-descriptions-item-label {
    color: hsl(var(--muted-foreground));
    background: hsl(var(--secondary) / 0.3);
  }

  .ant-descriptions-item-content {
    color: hsl(var(--foreground));
    font-weight: 500;
  }
}

:deep(.ant-steps) {
  padding: 16px 0;

  .ant-steps-item-process .ant-steps-item-icon {
    background: hsl(var(--primary));
    border-color: hsl(var(--primary));
  }

  .ant-steps-item-finish .ant-steps-item-icon {
    background: hsl(var(--primary) / 0.1);
    border-color: hsl(var(--primary));

    .ant-steps-icon {
      color: hsl(var(--primary));
    }
  }

  .ant-steps-item-wait .ant-steps-item-icon {
    background: hsl(var(--secondary));
    border-color: hsl(var(--border));
  }
}

:deep(.ant-timeline) {
  padding: 8px 0;

  .ant-timeline-item-tail {
    border-left-color: hsl(var(--primary) / 0.3);
  }
}

.approval-history-item {
  .approval-history-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 6px;
  }

  .approval-type {
    font-weight: 600;
    color: hsl(var(--foreground));
    font-size: 14px;
  }

  .approval-reviewer {
    color: hsl(var(--primary));
    font-weight: 500;
    margin-bottom: 4px;
    padding-left: 4px;
  }

  .approval-opinion {
    color: hsl(var(--muted-foreground));
    margin-bottom: 6px;
    padding: 8px 12px;
    background: hsl(var(--secondary) / 0.3);
    border-radius: 6px;
    font-size: 13px;
    line-height: 1.5;
  }

  .approval-time {
    color: hsl(var(--muted-foreground));
    font-size: 12px;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;

  .empty-icon-wrapper {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: linear-gradient(135deg, #e6eee8 0%, #d4e4dd 100%);
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
</style>
