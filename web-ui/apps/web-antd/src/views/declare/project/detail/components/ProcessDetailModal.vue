<script lang="ts" setup>
import type { DeclareProjectApi, IndicatorFormItem } from '#/api/declare/project';

import { computed, nextTick, onMounted, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Button, Card, Descriptions, message, Modal, Spin, Steps, Tag, Timeline, TimelineItem } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

import { getDictOptions } from '@vben/hooks';
import { DICT_TYPE } from '@vben/constants';

import { useBpmApproval } from '#/composables/useBpmApproval';
import { getProject, getProjectProcess, submitProcessReview } from '#/api/declare/project';
import { getProcessIndicatorConfigListByProcessType } from '#/api/declare/process-indicator-config';
import { getProcessByBusiness, selectExpert } from '#/api/bpm/task';

import IndicatorForm from './IndicatorForm.vue';
import ApprovalModal from '#/components/bpm/ApprovalModal.vue';
import ReturnModal from '#/components/bpm/ReturnModal.vue';
import ExpertSelectModalCmp from '#/components/bpm/ExpertSelectModal.vue';
import type { DeclareExpertApi } from '#/api/declare/expert';

interface Props {
  processId?: number;
  projectId?: number;
  processType?: number;
}

// 弹窗 API
const [Modal, modalApi] = useVbenModal({
  destroyOnClose: true,
  showCancelButton: false,
  confirmText: '关闭',
  onConfirm() {
    modalApi.close();
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      await nextTick();
      processDetail.value = null;
      projectInfo.value = null;
      return;
    }
    await loadProcessDetail();
  },
});

// 暴露 setData 方法供外部调用
defineExpose({ setData: modalApi.setData });

// 安全解析 JSON
function safeParseJSON(str: string | undefined | null): Record<string, any> {
  if (!str) return {};
  try {
    return JSON.parse(str);
  } catch {
    return {};
  }
}

// 加载状态
const loading = ref(false);

// 过程记录详情数据
const processDetail = ref<DeclareProjectApi.ProjectProcess | null>(null);

// 项目基本信息
const projectInfo = ref<DeclareProjectApi.Project | null>(null);

// 指标配置列表
const indicatorList = ref<IndicatorFormItem[]>([]);

// 指标值（从 indicatorValues JSON 解析）
const indicatorValues = ref<Record<string, any>>({});

// 文本字段值（从 processData JSON 解析）
const textFieldValues = ref<Record<string, any>>({});

// 附件列表
const attachmentList = ref<{ name: string; url: string }[]>([]);

// 流程进度节点
const processNodes = ref<{ id: string; name: string; status: number }[]>([]);

// 审批历史
const approvalHistory = ref<any[]>([]);

// 过程类型映射
// 过程类型映射 - 从字典获取
const processTypeMap = ref<Record<number, string>>({});

// 状态映射 - 从字典获取
const statusMap = ref<Record<string, { text: string; color: string }>>({});

// 状态颜色映射（固定样式）
const statusColorMap: Record<string, string> = {
  DRAFT: 'default',
  SUBMITTED: 'blue',
  REVIEWING: 'orange',
  APPROVED: 'green',
  REJECTED: 'red',
};

// 加载字典数据
function loadDictData() {
  // 加载过程类型字典
  const processTypeOptions = getDictOptions(DICT_TYPE.DECLARE_PROCESS_TYPE, 'number') as any[];
  const typeMap: Record<number, string> = {};
  processTypeOptions.forEach((item: { value: number; label: string }) => {
    typeMap[item.value] = item.label;
  });
  processTypeMap.value = typeMap;

  // 加载状态字典
  const statusOptions = getDictOptions(DICT_TYPE.DECLARE_PROJECT_STATUS, 'string') as any[];
  const statMap: Record<string, { text: string; color: string }> = {};
  statusOptions.forEach((item: { value: string; label: string }) => {
    statMap[item.value] = {
      text: item.label,
      color: statusColorMap[item.value] || 'default',
    };
  });
  statusMap.value = statMap;
}

// 页面加载时获取字典数据
onMounted(() => {
  loadDictData();
});

// 当前 businessType
const currentBusinessType = computed(() => {
  // 优先使用 processDetail.processType，其次尝试从 processData 中解析
  let type = processDetail.value?.processType;
  
  // 如果 processType 为空，尝试从 processData JSON 中解析
  if (!type) {
    try {
      const processData = processDetail.value?.processData;
      if (processData) {
        const parsed = JSON.parse(processData);
        type = parsed.processType || parsed.type;
      }
    } catch (e) {
      // 忽略解析错误
    }
  }
  
  // 最终默认值
  type = type || 1;
  console.log('[DEBUG] currentBusinessType computed:', { 
    processType: processDetail.value?.processType, 
    type, 
    processData: processDetail.value?.processData,
    result: `project_process:type:${type}` 
  });
  return `project_process:type:${type}`;
});

// 当前流程节点索引
const currentNodeIndex = computed(() => {
  const nodes = processNodes.value;
  if (!nodes.length) return 0;
  const currentIdx = nodes.findIndex((n) => n.status === 1); // 1=进行中
  return currentIdx >= 0 ? currentIdx : nodes.length - 1;
});

// 审批相关 Hook
const {
  rowAvailableActions,
  loadRowAvailableActions,
  handleApprovalAction,
  approvalModalRef,
  returnModalRef,
  confirmApproval,
} = useBpmApproval({
  tableName: 'project',
  businessType: currentBusinessType.value,
  onRefresh: () => {
    loadProcessDetail();
  },
  onCustomReturn: (taskId: string) => {
    // 使用自定义的退回弹窗（带节点选择）
    returnModalRef.value?.open({ taskId, title: '退回' });
    return true;
  },
  onOpenExpertSelect: (taskId: string) => {
    console.log('[DEBUG] onOpenExpertSelect called with taskId:', taskId);
    // 使用自定义的专家选择弹窗
    openExpertSelectModal(taskId);
    return true;
  },
});

// 监听 businessType 变化，重新加载审批操作
watch(
  () => currentBusinessType.value,
  async (newVal, oldVal) => {
    console.log('[DEBUG] businessType changed:', oldVal, '->', newVal);
    if (processDetail.value?.id) {
      await loadRowAvailableActions([{ id: processDetail.value.id }], currentBusinessType.value);
    }
  },
);

// 当前过程的审批操作
const currentActions = computed(() => {
  if (!processDetail.value?.id) return [];
  return rowAvailableActions.value[processDetail.value.id] || [];
});

// 加载过程记录详情
async function loadProcessDetail() {
  const data = modalApi.getData<Props>();
  if (!data?.processId) return;

  loading.value = true;
  try {
    // 1. 获取过程记录详情
    const result = await getProjectProcess(data.processId);
    // 强制转换为 any 以确保可以访问任何属性
    const resultAny = result as any;
    console.log('[DEBUG] getProjectProcess result:', JSON.stringify(resultAny));
    console.log('[DEBUG] getProjectProcess result keys:', Object.keys(resultAny), 'processType:', resultAny.processType, 'type:', resultAny.type);
    processDetail.value = result;

    // 2. 获取项目基本信息
    if (result?.projectId) {
      const project = await getProject(result.projectId);
      projectInfo.value = project;
    }

    // 3. 解析指标值 JSON
    indicatorValues.value = safeParseJSON(result?.indicatorValues);

    // 4. 解析文本字段值 JSON
    textFieldValues.value = safeParseJSON(result?.processData);

    // 5. 解析附件列表（现在存储的是URL地址，逗号分隔）
    const attachmentUrls = result?.attachmentIds; // attachmentIds 字段现在存储的是URL
    if (attachmentUrls) {
      const urls = attachmentUrls.split(',').filter(Boolean);
      attachmentList.value = urls.map((url, index) => {
        // 从URL中提取文件名
        const fileName = decodeURIComponent(url.split('/').pop() || `附件${index + 1}`);
        return {
          name: fileName,
          url: url.trim(),
        };
      });
    } else {
      attachmentList.value = [];
    }
    const processType = result?.processType || data.processType || 1;
    const configs = await getProcessIndicatorConfigListByProcessType(processType, projectInfo.value?.projectType);
    indicatorList.value = (configs as any[]).map((config: any) => ({
      configId: config.id,
      indicatorId: config.indicatorId,
      indicatorCode: config.indicatorCode,
      indicatorName: config.indicatorName,
      valueType: config.valueType,
      unit: config.unit || '',
      isRequired: config.isRequired,
      sort: config.sort,
      fillRequire: '',
      currentValue: null,
    }));

    // 7. 加载流程进度和审批历史
    await loadProcessFlow();

    // 8. 加载审批操作（传入当前 businessType）
    if (processDetail.value?.id) {
      await loadRowAvailableActions([{ id: processDetail.value.id }], currentBusinessType.value);
    }
  } catch (e) {
    console.error('加载过程记录详情失败', e);
  } finally {
    loading.value = false;
  }
}

// 加载流程进度和审批历史（从BPM系统获取）
async function loadProcessFlow() {
  if (!processDetail.value?.id) return;

  try {
    // 从 BPM 系统获取流程信息
    const processList = await getProcessByBusiness({
      tableName: 'project',
      businessId: processDetail.value.id,
    });

    // 过滤出主流程和子流程
    // 主流程通常是 construction_process，子流程是 declare_rectification
    const mainProcess = processList?.find(
      (p: any) => p.processDefinitionKey === 'construction_process'
        || p.processDefinitionKey?.startsWith('construction_process')
    );
    const rectificationProcesses = processList?.filter(
      (p: any) => p.processDefinitionKey === 'declare_rectification'
        || p.processDefinitionKey?.startsWith('declare_rectification')
    ) || [];

    // 优先使用主流程，如果没有则使用第一个流程
    const firstProcess = mainProcess || processList?.[0];

    if (firstProcess?.activityNodes) {
      // 构建流程节点（以主流程为主）
      const nodes: { id: string; name: string; status: number }[] = [];
      firstProcess.activityNodes.forEach((node: any) => {
        let status = 0; // 待处理
        if (node.status === 2 || node.endTime) {
          status = 2; // 已完成
        } else if (node.status === 1 || node.startTime) {
          status = 1; // 进行中
        }
        nodes.push({
          id: node.id,
          name: node.name,
          status,
        });
      });
      processNodes.value = nodes;

      // 构建审批历史（合并主流程和子流程的审批记录）
      const history: any[] = [];

      // 添加主流程的审批记录
      firstProcess.activityNodes.forEach((node: any) => {
        if (node.tasks && node.tasks.length > 0) {
          node.tasks.forEach((task: any) => {
            let actionStatus = '审核';
            if (node.status === 2) actionStatus = '通过';
            else if (node.status === 3) actionStatus = '拒绝';
            else if (node.status === 5) actionStatus = '退回';

            history.push({
              name: node.name,
              processDefinitionKey: firstProcess.processDefinitionKey,
              status: actionStatus,
              opinion: task.reviewReason || task.reason,
              reviewerId: task.assigneeUser?.id,
              reviewerName: task.assigneeUser?.nickname || task.assigneeUser?.name || '-',
              reviewTime: task.endTime || node.endTime,
            });
          });
        }
      });

      // 添加子流程的审批记录
      rectificationProcesses.forEach((subProcess: any) => {
        if (subProcess.activityNodes) {
          subProcess.activityNodes.forEach((node: any) => {
            if (node.tasks && node.tasks.length > 0) {
              node.tasks.forEach((task: any) => {
                let actionStatus = '审核';
                if (node.status === 2) actionStatus = '通过';
                else if (node.status === 3) actionStatus = '拒绝';
                else if (node.status === 5) actionStatus = '退回';

                history.push({
                  name: `[整改] ${node.name}`,
                  processDefinitionKey: subProcess.processDefinitionKey,
                  status: actionStatus,
                  opinion: task.reviewReason || task.reason,
                  reviewerId: task.assigneeUser?.id,
                  reviewerName: task.assigneeUser?.nickname || task.assigneeUser?.name || '-',
                  reviewTime: task.endTime || node.endTime,
                });
              });
            }
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
    } else {
      // 无流程信息时，显示当前过程记录状态作为节点
      const processType = processDetail.value.processType || 1;
      processNodes.value = [{
        id: `process_${processDetail.value.id}`,
        name: processTypeMap.value[processType] || '过程记录',
        status: processDetail.value.status === 'DRAFT' ? 0 :
                processDetail.value.status === 'APPROVED' ? 2 :
                processDetail.value.status === 'REJECTED' ? 3 : 1,
      }];

      // 如果有审核记录，添加到历史
      if (processDetail.value.reviewOpinion || processDetail.value.reviewerId) {
        approvalHistory.value = [{
          name: processTypeMap.value[processType] || '过程记录',
          status: processDetail.value.status === 'APPROVED' ? '通过' :
                  processDetail.value.status === 'REJECTED' ? '退回' : '审核',
          opinion: processDetail.value.reviewOpinion,
          reviewTime: processDetail.value.reviewTime,
        }];
      } else {
        approvalHistory.value = [];
      }
    }
  } catch (e) {
    console.error('加载流程进度失败', e);
    // 即使失败，也显示当前状态
    const processType = processDetail.value.processType || 1;
    processNodes.value = [{
      id: `process_${processDetail.value.id}`,
      name: processTypeMap.value[processType] || '过程记录',
      status: processDetail.value.status === 'DRAFT' ? 0 :
              processDetail.value.status === 'APPROVED' ? 2 :
              processDetail.value.status === 'REJECTED' ? 3 : 1,
    }];
  }
}

// 提交审核
async function handleSubmitReview() {
  if (!processDetail.value?.id) return;

  try {
    const title = processDetail.value.processTitle || '该过程记录';
    await import('@vben/common-ui').then((m) =>
      m.confirm(`确定要提交「${title}」进行审核吗？提交后将进入审批流程。`)
    );

    // 状态更新由 DeclareProcessStartedListener 通过事件驱动
    const processType = processDetail.value.processType || 1;
    const businessType = `project_process:type:${processType}`;
    await submitProcessReview(processDetail.value.id, businessType);

    modalApi.setData({ processId: processDetail.value.id, projectId: processDetail.value.projectId, processType });
    await loadProcessDetail();
    message.success('提交审核成功');
  } catch (e: any) {
    if (e?.message?.includes?.('cancel') || e === 'cancel' || e === 'esc') {
      return;
    }
    console.error('提交审核失败', e);
    message.error('提交审核失败，请重试');
  }
}

// 审批操作
async function handleApproval(action: any) {
  console.log('[DEBUG] handleApproval called with:', action);
  await handleApprovalAction(action);
}

// 格式化日期
function formatDateTime(value: string | number | any | undefined) {
  if (!value) return '-';
  if (typeof value === 'object' && value.format) {
    return value.format('YYYY-MM-DD HH:mm:ss');
  }
  const date = new Date(value);
  if (isNaN(date.getTime())) return '-';
  return date.toLocaleString('zh-CN');
}

// 获取状态信息
function getStatusInfo(status: string | number | undefined) {
  if (!status) return { text: '-', color: 'default' };
  const key = String(status).toUpperCase();
  return statusMap.value[key] || { text: String(status), color: 'default' };
}

// 获取审批按钮显示文字（兼容 buttonKey + buttonSettings.displayName 或 label）
function getActionLabel(action: any): string {
  if (action.label) return action.label;
  const key = action.buttonKey || action.key;
  if (key && action.buttonSettings?.[key]?.displayName) {
    return action.buttonSettings[key].displayName;
  }
  const firstKey = action.buttonSettings && Object.keys(action.buttonSettings)[0];
  if (firstKey) return action.buttonSettings[firstKey]?.displayName || key || '操作';
  return key || '操作';
}

// 审批按钮类型：1=通过(primary)，2=拒绝(danger)，其余=default(辅色)
function getActionButtonType(action: any): 'primary' | 'default' | 'danger' {
  const key = String(action.buttonKey ?? action.key ?? '');
  if (key === '1') return 'primary';
  if (key === '2') return 'danger';
  return 'default';
}

// 获取按钮图标
function getActionIcon(action: any): string {
  const key = String(action.buttonKey ?? action.key ?? '');
  const iconMap: Record<string, string> = {
    '1': 'ep:circle-check',     // 通过
    '2': 'ep:circle-close',    // 拒绝
    '3': 'ep:right',           // 转办
    '4': 'ep:user',            // 委派
    '5': 'ep:document-add',   // 加签
    '6': 'ep:back',            // 退回
    '8': 'ep:user',            // 选择专家
  };
  return iconMap[key] || 'ep:more-filled';
}

// 监听 businessType 变化
watch(
  () => currentBusinessType.value,
  async (newVal, oldVal) => {
    console.log('[DEBUG] businessType changed (2):', oldVal, '->', newVal);
    if (processDetail.value?.id) {
      await loadRowAvailableActions([{ id: processDetail.value.id }], currentBusinessType.value);
    }
  },
);

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
    loadProcessDetail();
  } catch (error) {
    console.error('选择专家失败:', error);
    message.error('选择专家失败');
  }
}

// ========== 专家选择弹窗相关 ==========
</script>


<template>
  <Modal
    class="w-[90%]"
    :title="`${processTypeMap[processDetail?.processType || 1]}详情 - ${processDetail?.processTitle || ''}`"
  >
    <Spin :spinning="loading">
      <div class="process-detail-content">
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


        <!-- 项目基本信息 --> 
        <Card title="项目基本信息" :bordered="false" class="mb-4">
          <Descriptions :column="2" bordered size="small">
            <Descriptions.Item label="项目名称" :span="2">
              {{ projectInfo?.projectName || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="机构名称">
              {{ projectInfo?.orgName || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="项目类型">
              {{ projectInfo?.projectType ? processTypeMap[projectInfo.projectType] : '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="项目负责人">
              {{ projectInfo?.leaderName || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="负责人电话">
              {{ projectInfo?.leaderMobile || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="立项时间" :span="2">
              {{ formatDateTime(projectInfo?.startTime) }}
            </Descriptions.Item>
            <Descriptions.Item label="计划完成时间" :span="2">
              {{ formatDateTime(projectInfo?.planEndTime) }}
            </Descriptions.Item>
          </Descriptions>
        </Card>


        <!-- 基本信息 -->
        <Card title="基本信息" :bordered="false" class="mb-4">
          <Descriptions :column="2" bordered size="small">
            <Descriptions.Item label="过程标题" :span="2">
              <span class="font-medium">{{ processDetail?.processTitle || '-' }}</span>
            </Descriptions.Item>
            <Descriptions.Item label="过程类型">
              <Tag :color="processDetail?.processType === 1 ? 'blue' : processDetail?.processType === 2 ? 'orange' : processDetail?.processType === 3 ? 'purple' : 'green'">
                {{ processTypeMap[processDetail?.processType || 1] }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag :color="getStatusInfo(processDetail?.status).color">
                {{ getStatusInfo(processDetail?.status).text }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="报告周期">
              {{ formatDateTime(processDetail?.reportPeriodStart) }} ~
              {{ formatDateTime(processDetail?.reportPeriodEnd) }}
            </Descriptions.Item>
            <Descriptions.Item label="报告提交时间">
              {{ formatDateTime(processDetail?.reportTime) }}
            </Descriptions.Item>
            <Descriptions.Item label="审核意见" :span="2">
              {{ processDetail?.reviewOpinion || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间" :span="2">
              {{ formatDateTime(processDetail?.createTime) }}
            </Descriptions.Item>
          </Descriptions>
        </Card>

       

        <!-- 填报信息 - 文本字段 -->
        <Card
          v-if="Object.keys(textFieldValues).length > 0"
          title="填报信息"
          :bordered="false"
          class="mb-4"
        >
          <Descriptions :column="1" bordered size="small">
            <Descriptions.Item
              v-for="(value, key) in textFieldValues"
              :key="key"
              :label="String(key)"
              :span="1"
            >
              {{ value || '-' }}
            </Descriptions.Item>
          </Descriptions>
        </Card>

        <!-- 指标数据 -->
        <Card
          v-if="indicatorList.length > 0"
          title="指标数据"
          :bordered="false"
          class="mb-4"
        >
          <IndicatorForm
            v-model="indicatorValues"
            :indicators="indicatorList"
            :disabled="true"
          />
        </Card>

        <!-- 附件 -->
        <Card
          v-if="attachmentList.length > 0"
          title="附件"
          :bordered="false"
          class="mb-4"
        >
          <div class="attachment-list">
            <div
              v-for="(file, index) in attachmentList"
              :key="index"
              class="attachment-item"
            >
              <a :href="file.url" target="_blank" class="attachment-link">
                <i class="fa fa-paperclip mr-1" />
                {{ file.name }}
              </a>
            </div>
          </div>
        </Card>

        <!-- 审批历史 -->
        <Card
          v-if="approvalHistory.length > 0"
          title="审批历史"
          :bordered="false"
          class="mb-4"
        >
          <Timeline>
            <TimelineItem
              v-for="(item, index) in approvalHistory"
              :key="index"
              :color="item.status === '通过' ? 'green' : item.status === '退回' ? 'red' : 'blue'"
            >
              <div class="approval-history-item">
                <div class="approval-history-header">
                  <span class="approval-type">{{ item.name }}</span>
                  <Tag :color="item.status === '通过' ? 'green' : item.status === '退回' ? 'red' : 'orange'">
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
                  {{ formatDateTime(item.reviewTime || item.createTime) }}
                </div>
              </div>
            </TimelineItem>
          </Timeline>
        </Card>

        <!-- 操作按钮组 -->
        <div class="action-buttons">
          <!-- 草稿状态且没有待办任务：显示提交审核按钮 -->
          <Button
            v-if="processDetail?.status === 'SAVED' && !currentActions.length"
            type="primary"
            size="large"
            @click="handleSubmitReview"
          >
            <template #icon>
              <IconifyIcon icon="ep:promotion" class="btn-icon" />
            </template>
            提交审核
          </Button>

          <!-- 审批操作按钮（非草稿状态，且有待办任务时显示） -->
          <template v-if="processDetail?.status !== 'DRAFT' && currentActions.length">
            <Button
              v-for="action in (currentActions as any[])"
              :key="action.buttonKey || action.key"
              :type="getActionButtonType(action)"
              size="large"
              class="approval-action-btn"
              @click="handleApproval(action)"
            >
              <template #icon>
                <IconifyIcon :icon="getActionIcon(action)" />
              </template>
              {{ getActionLabel(action) }}
            </Button>
          </template>
        </div>
      </div>
    </Spin>

    <!-- 专家选择弹窗 -->
    <ExpertSelectModalCmp
      v-model:open="expertSelectVisible"
      @confirm="(...args: any[]) => handleExpertSelectConfirm(args[0])"
    />

    <!-- 审批弹窗（通过/拒绝） -->
    <ApprovalModal
      ref="approvalModalRef"
      @confirm="(reason, action) => confirmApproval(reason, action)"
    />

    <!-- 退回弹窗 -->
    <ReturnModal ref="returnModalRef" @success="loadProcessDetail" />
  </Modal>
</template>

<style lang="scss" scoped>
.process-detail-content {
  max-height: 70vh;
  overflow-y: auto;
  padding: 4px;
}

.mb-4 {
  margin-bottom: 16px;
}

:deep(.ant-card) {
  border-radius: 8px;
  box-shadow: 0 1px 2px -1px hsl(var(--primary) / 0.1), 0 1px 3px -1px hsl(var(--primary) / 0.1);
  border: 1px solid hsl(var(--border));

  .ant-card-head {
    border-bottom-color: hsl(var(--border));
    background: linear-gradient(135deg, hsl(var(--primary) / 0.02) 0%, hsl(var(--background)) 100%);

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

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attachment-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: hsl(var(--primary));
  text-decoration: none;
  padding: 8px 12px;
  border-radius: 6px;
  background: hsl(var(--primary) / 0.05);
  border: 1px solid hsl(var(--primary) / 0.2);
  transition: all 0.2s ease;

  &:hover {
    background: hsl(var(--primary) / 0.1);
    border-color: hsl(var(--primary) / 0.4);
    text-decoration: none;
    transform: translateX(2px);
  }

  i {
    font-size: 14px;
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

.action-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
  padding: 24px 0 16px;
  border-top: 1px solid hsl(var(--border));
  background: linear-gradient(180deg, hsl(var(--secondary) / 0.1) 0%, hsl(var(--background)) 100%);
  margin-top: 8px;

  :deep(.ant-btn) {
    min-width: 100px;
    height: 40px;
    font-weight: 500;
    border-radius: 8px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    transition: all 0.2s ease;

    .btn-icon {
      color: inherit;
    }

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px -1px hsl(var(--primary) / 0.3);
    }

    &.ant-btn-primary {
      background: hsl(var(--primary));
      border-color: hsl(var(--primary));
      color: hsl(var(--primary-foreground));

      &:hover {
        background: hsl(var(--primary) / 0.9);
        border-color: hsl(var(--primary) / 0.9);
      }
    }

    &.ant-btn-danger {
      background: hsl(var(--destructive));
      border-color: hsl(var(--destructive));
      color: hsl(var(--destructive-foreground));

      &:hover {
        background: hsl(var(--destructive) / 0.9);
        border-color: hsl(var(--destructive) / 0.9);
      }
    }

    &:not(.ant-btn-primary):not(.ant-btn-danger) {
      background: hsl(var(--secondary));
      border-color: hsl(var(--border));
      color: hsl(var(--foreground));

      &:hover {
        background: hsl(var(--secondary) / 0.8);
        border-color: hsl(var(--primary) / 0.5);
        color: hsl(var(--primary));
      }
    }
  }
}
</style>
