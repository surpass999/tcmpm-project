<script lang="ts" setup>
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { DeclareIndicatorValueApi } from '#/api/declare/indicatorValue';
import type { BpmTaskApi } from '#/api/bpm/task';
import type { BpmActionApi } from '#/api/bpm/action';

import { ref, shallowRef, computed, watch, nextTick } from 'vue';
import { getDictObj } from '@vben/hooks';
import { DICT_TYPE } from '@vben/constants';

import { Steps, Step, Spin } from 'ant-design-vue';
import { message } from 'ant-design-vue';

import { getFiling } from '#/api/declare/filing';
import { getIndicatorsByProjectType } from '#/api/declare/indicator';
import { getIndicatorValues } from '#/api/declare/indicatorValue';
import { submitBpmAction } from '#/api/bpm/action';
import { getTaskByBusiness, getProcessByBusiness } from '#/api/bpm/task';
import ExpertSelectModal from '#/components/bpm/ExpertSelectModal.vue';
import { OperationButtonType } from '#/views/bpm/components/simple-process-design/consts';

// 指标业务类型（数字）
const BUSINESS_TYPE_NUMBER = 1;

// Props - 只支持通过 tableName + businessId 获取审批详情
const props = defineProps<{
  tableName: string;  // 业务表分类，如 filing
  businessId: number;  // 业务ID
  showActions?: boolean;
}>();

// Emits
const emit = defineEmits<{
  success: [];
  action: [action: BpmActionApi.BpmAction & { taskId?: string }];
  refresh: [];
}>();

// 加载状态
const loading = ref(false);

// 监听 businessId 变化，自动加载数据
watch(
  () => props.businessId,
  (newBusinessId) => {
    if (newBusinessId && props.tableName) {
      nextTick(() => {
        loadAllData();
      });
    }
  },
  { immediate: true }
);

// 备案数据（仅用于展示基本信息）
const filingData = ref<any>(null);

// 指标列表（按分类分组）
const indicatorGroups = ref<{
  category: number;
  categoryName: string;
  indicators: DeclareIndicatorApi.Indicator[];
}[]>([]);

// 指标值映射 (indicatorId -> value)
const indicatorValuesMap = ref<Record<number, any>>({});

// 折叠面板展开的key
const activeCollapseKeys = ref<(string | number)[]>([]);

// 审批详情数据
const approvalDetail = ref<{
  status?: number;
  processInstance?: any;
  activityNodes?: any[];
  todoTask?: any;
} | null>(null);

// 审批历史（任务列表）
const approvalHistory = ref<any[]>([]);

// 可用操作按钮
const availableActions = ref<(BpmActionApi.BpmAction & { taskId?: string })[]>([]);

// 当前任务（待处理的任务）
const currentTask = shallowRef<BpmTaskApi.Task | null>(null);

// 审批表单数据
const reasonForm = ref({
  reason: '',
});

// 审批操作弹窗
const actionModalVisible = ref(false);
const actionModalLoading = ref(false);
const currentAction = shallowRef<BpmActionApi.BpmAction & { taskId?: string } | null>(null);

// 专家选择弹窗
const expertModalRef = ref<{
  open: () => void;
  close: () => void;
} | null>(null);

// 已选中的专家
const selectedExperts = ref<any[]>([]);

// 计算当前流程进度
const processNodes = computed(() => {
  if (!approvalDetail.value?.activityNodes) return [];
  return approvalDetail.value.activityNodes;
});

// 计算当前进行中的节点ID
const currentNodeId = computed(() => {
  if (!approvalDetail.value?.todoTask) return null;
  return approvalDetail.value.todoTask.taskDefinitionKey;
});

// 获取节点状态
function getNodeStatus(node: BpmProcessInstanceApi.ApprovalNodeInfo): number {
  if (approvalDetail.value?.todoTask?.taskDefinitionKey === node.id) {
    return 1; // 进行中
  }
  if (node.endTime || node.status === 2) {
    return 2; // 已完成
  }
  if (node.startTime) {
    return 1; // 进行中
  }
  return 0; // 待处理
}

// 格式化日期（完整格式）
function formatDate(date: string | Date | number | undefined): string {
  if (!date) return '-';
  // 如果是时间戳（毫秒），转换为 Date
  const d = typeof date === 'number' ? new Date(date) : new Date(date);
  return d.toLocaleString('zh-CN');
}

// 格式化日期（仅日期）
function formatDateOnly(date: string | Date | number | undefined): string {
  if (!date) return '-';
  // 如果是时间戳（毫秒），转换为 Date
  const d = typeof date === 'number' ? new Date(date) : new Date(date);
  return d.toLocaleDateString('zh-CN');
}

// 获取项目类型显示文本
function getProjectTypeText(value: number | undefined): string {
  if (!value) return '-';
  const dict = getDictObj(DICT_TYPE.DECLARE_PROJECT_TYPE, String(value));
  return dict?.label || String(value);
}

// 获取流程状态显示文本（基于 approvalDetail.status）
function getProcessStatusText(status?: number): string {
  if (status === undefined || status === null) return '未知';
  // 流程状态: -1=未开始，1=审批中，2=审批通过，3=审批不通过，4=已取消
  switch (status) {
    case -1:
      return '未开始';
    case 1:
      return '审批中';
    case 2:
      return '审批通过';
    case 3:
      return '审批不通过';
    case 4:
      return '已取消';
    default:
      return `状态${status}`;
  }
}

// 获取流程状态样式类
function getProcessStatusClass(status?: number): string {
  if (status === 2) return 'status-tag-success';
  if (status === 3 || status === 4) return 'status-tag-error';
  return 'status-tag-pending';
}

// 获取操作按钮类型
function getActionType(key: string): 'primary' | 'default' | 'dashed' | 'link' | 'text' {
  if (key.includes('pass') || key.includes('agree') || key.includes('approve')) {
    return 'primary';
  }
  if (key.includes('reject') || key.includes('refuse') || key.includes('deny')) {
    return 'default';
  }
  return 'default';
}

// 处理审批操作点击
function handleActionClick(action: BpmActionApi.BpmAction & { taskId?: string }) {
  const vars = action.vars || {};

  // 如果外部需要处理操作点击，emit 事件
  if (props.showActions === false) {
    emit('action', action);
    return;
  }

  // 判断是否是 simple 设计器的按钮格式（使用数字 key）
  const actionKey = action.key;
  const isSimpleButton = !isNaN(Number(actionKey));

  if (isSimpleButton) {
    // Simple 设计器按钮格式处理
    const buttonType = Number(actionKey);

    // 选择专家操作 - 打开专家选择弹窗
    if (buttonType === OperationButtonType.SELECT_APPROVER) {
      expertModalRef.value?.open();
      return;
    }

    // 拒绝和退回操作，需要填写审批意见
    if (buttonType === OperationButtonType.REJECT || buttonType === OperationButtonType.RETURN) {
      currentAction.value = action;
      reasonForm.value.reason = '';
      actionModalVisible.value = true;
      return;
    }

    // 其他操作直接执行
    executeAction(action, '');
  } else {
    // 原有 DSL 按钮格式处理
    if (vars.expertMin !== undefined || vars.expertMax !== undefined) {
      message.info('该操作需要选择专家，请联系管理员配置');
      return;
    }

    if (vars.reason || vars.reasonRequired) {
      currentAction.value = action;
      reasonForm.value.reason = '';
      actionModalVisible.value = true;
      return;
    }

    executeAction(action, '');
  }
}

// 执行审批操作
async function executeAction(
  action: BpmActionApi.BpmAction & { taskId?: string },
  reason: string
) {
  actionModalLoading.value = true;
  try {
    // 使用 props 中的 tableName 作为 businessType
    await submitBpmAction({
      businessType: props.tableName,
      businessId: props.businessId,
      actionKey: action.key,
      reason,
    });
    message.success('操作成功');
    actionModalVisible.value = false;
    emit('success');
    await loadAllData();
  } catch (error: any) {
    console.error('操作失败:', error);
    message.error(error.message || '操作失败');
  } finally {
    actionModalLoading.value = false;
  }
}

// 确认审批操作
async function handleActionConfirm() {
  if (!currentAction.value) return;
  const vars = currentAction.value.vars || {};
  if (vars.reasonRequired && !reasonForm.value.reason.trim()) {
    message.warning('请输入审批意见');
    return;
  }
  await executeAction(currentAction.value, reasonForm.value.reason);
}

// 专家选择确认
function handleExpertConfirm(experts: any[]) {
  selectedExperts.value = experts;
  // 选择专家后触发刷新，让父组件刷新数据
  emit('refresh');
}

// 加载所有数据 - 通过 tableName + businessId 获取
async function loadAllData() {
  const { tableName, businessId } = props;

  // 参数验证
  if (!tableName || !businessId) {
    console.error('tableName 或 businessId 不能为空', props);
    message.error('参数错误：业务表分类和业务ID不能为空');
    loading.value = false;
    return;
  }

  loading.value = true;
  try {
    // 1. 如果是 filing 类型，获取备案基本信息
    if (tableName === 'filing') {
      try {
        filingData.value = await getFiling(businessId);
      } catch (e) {
        console.warn('获取备案信息失败:', e);
      }

      // 2. 加载指标和指标值
      if (filingData.value?.projectType) {
        const indicators = await getIndicatorsByProjectType(
          filingData.value.projectType,
          'filing'
        );

        // 按分类分组
        const categoryMap = new Map<number, typeof indicatorGroups.value[0]>();
        indicators.forEach((indicator) => {
          if (!categoryMap.has(indicator.category)) {
            categoryMap.set(indicator.category, {
              category: indicator.category,
              categoryName: getCategoryName(indicator.category),
              indicators: [],
            });
          }
          categoryMap.get(indicator.category)!.indicators.push(indicator);
        });
        indicatorGroups.value = Array.from(categoryMap.values()).sort(
          (a, b) => a.category - b.category
        );

        // 默认展开所有分类
        if (indicatorGroups.value.length > 0) {
          activeCollapseKeys.value = indicatorGroups.value.map((g) => g.category);
        }

        // 获取指标值
        const values = await getIndicatorValues(BUSINESS_TYPE_NUMBER, businessId);
        const map: Record<number, any> = {};
        values.forEach((v) => {
          map[v.indicatorId] = parseIndicatorValue(v);
        });
        indicatorValuesMap.value = map;
      }
    }

    // 3. 通过 tableName + businessId 获取审批信息
    const taskStatusList = await getTaskByBusiness({
      tableName,
      businessIds: [businessId],
    });

    const taskStatus = taskStatusList?.[0];

    // 4. 通过 tableName + businessId 获取流程详情（用于流程进度）
    let processActivityNodes: any[] = [];
    try {
      const processList = await getProcessByBusiness({
        tableName,
        businessId,
      });
      // 取第一个流程实例的节点信息
      const firstProcess = processList?.[0];
      if (firstProcess) {
        processActivityNodes = firstProcess.activityNodes || [];
      }
    } catch (e) {
      console.warn('获取流程详情失败:', e);
    }

    // 5. 构建审批详情数据
    if (taskStatus) {
      // 有流程实例信息
      approvalDetail.value = {
        status: taskStatus.hasTodoTask ? 1 : (taskStatus.processInstanceId ? 2 : -1),
        processInstance: taskStatus.processInstanceId ? { id: taskStatus.processInstanceId } : null,
        activityNodes: processActivityNodes, // 使用流程详情中的节点信息
        todoTask: taskStatus.todoTasks?.[0] || null,
      };

      // 6. 转换审批历史 - 使用 processActivityNodes 显示完整的审批记录（包含所有节点的处理信息）
      // 从 activityNodes 中提取所有已完成的任务作为审批历史
      const approvalHistoryFromNodes: any[] = [];
      processActivityNodes.forEach((node: any) => {
        if (node.tasks && node.tasks.length > 0) {
          node.tasks.forEach((task: any) => {
            // 根据节点状态判断操作结果
            let approved: boolean | undefined;
            let actionLabel = '';
            switch (node.status) {
              case 2: // 审批通过
                approved = true;
                actionLabel = '通过';
                break;
              case 3: // 审批不通过
                approved = false;
                actionLabel = '拒绝';
                break;
              case 5: // 退回
                approved = undefined;
                actionLabel = '退回';
                break;
              default:
                actionLabel = node.name;
            }

            approvalHistoryFromNodes.push({
              id: task.id,
              name: node.name,
              taskDefinitionKey: node.id,
              createTime: task.endTime || node.endTime,
              endTime: node.endTime,
              reason: task.reason,
              approved,
              status: node.status === 5 ? 5 : (approved ? 2 : approved === false ? 3 : undefined),
              actionLabel,
              assignees: task.assigneeUser ? [task.assigneeUser.id] : [],
              assigneeUser: task.assigneeUser,
              ownerUser: task.assigneeUser,
            });
          });
        }
      });
      // 按时间正序排列（最早的操作在最前面）
      approvalHistoryFromNodes.sort((a, b) => (a.createTime || 0) - (b.createTime || 0));

      // 优先使用 activityNodes 构建的审批历史，否则使用 allDoneTasks
      approvalHistory.value = approvalHistoryFromNodes.length > 0
        ? approvalHistoryFromNodes
        : (taskStatus.allDoneTasks || taskStatus.doneTasks || []).map((done: any) => ({
            id: done.taskId,
            name: done.taskName,
            taskDefinitionKey: done.taskDefinitionKey,
            createTime: done.endTime,
            endTime: done.endTime,
            reason: done.reason,
            approved: done.approved,
            status: done.status,
            assignees: done.assigneeUser ? [done.assigneeUser.id] : [],
            assigneeUser: done.assigneeUser,
            ownerUser: done.assigneeUser,
          })).reverse();
    } else if (processActivityNodes.length > 0) {
      // 没有任务信息但有流程节点信息（可能是已结束的流程）
      approvalDetail.value = {
        status: 2, // 视为已完成
        processInstance: null,
        activityNodes: processActivityNodes,
        todoTask: null,
      };
      approvalHistory.value = [];
    } else {
      // 没有流程信息
      approvalDetail.value = {
        status: -1,
        processInstance: null,
        activityNodes: [],
        todoTask: null,
      };
      approvalHistory.value = [];
    }

    // 7. 加载可用操作按钮
    // 根据是否有待办任务构建操作
    if (taskStatus?.hasTodoTask && taskStatus.todoTasks?.[0]) {
      const todoTask = taskStatus.todoTasks[0];
      // 从 buttonSettings 构建操作按钮
      const buttons = todoTask.buttonSettings || {};
      availableActions.value = Object.entries(buttons)
        .filter(([_, setting]: [string, any]) => setting.enable)
        .map(([key, setting]: [string, any]) => ({
          key,
          label: setting.displayName,
          taskId: todoTask.taskId,
          vars: {
            reasonRequired: todoTask.reasonRequire,
          },
        }));
    } else {
      availableActions.value = [];
    }

  } catch (error) {
    console.error('加载审批详情失败:', error);
    message.error('加载审批详情失败');
    approvalDetail.value = null;
    approvalHistory.value = [];
  } finally {
    loading.value = false;
  }
}

// 获取分类名称
function getCategoryName(category: number): string {
  const categoryNames: Record<number, string> = {
    1: '基本信息',
    2: '业务功能',
    3: '技术实现',
    4: '安全防护',
    5: '其他',
  };
  return categoryNames[category] || `分类${category}`;
}

// 解析指标值（统一转换为可显示格式）
function parseIndicatorValue(
  item: DeclareIndicatorValueApi.IndicatorValue
): any {
  switch (item.valueType) {
    case 1: // 数字
      return item.valueNum;
    case 2: // 字符串
    case 6: // 单选
    case 10: // 单选下拉
      return item.valueStr;
    case 7: // 多选
    case 11: // 多选下拉
      return item.valueStr ? item.valueStr.split(',') : [];
    case 3: // 布尔
      return item.valueBool;
    case 4: // 日期
      return item.valueDate;
    case 5: // 长文本
      return item.valueText;
    case 8: // 日期区间
      if (item.valueDateStart || item.valueDateEnd) {
        return [item.valueDateStart, item.valueDateEnd];
      }
      return undefined;
    case 9: // 文件上传
      return item.valueStr;
    default:
      return item.valueStr;
  }
}

// 解析选项
function parseOptions(
  valueOptions: string
): Array<{ label: string; value: string }> {
  if (!valueOptions) return [];
  try {
    return JSON.parse(valueOptions);
  } catch {
    return [];
  }
}

// 获取选项数组
function getOptions(valueOptions: string): Array<{ label: string; value: string }> {
  return parseOptions(valueOptions);
}

// 解析文件列表
function parseFileList(
  value: string | undefined
): Array<{ name: string; url: string }> {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) {
      return parsed;
    }
  } catch {
    // 可能是逗号分隔的URL
    if (value.includes(',')) {
      return value.split(',').map((url) => ({
        name: url.split('/').pop() || url,
        url: url.trim(),
      }));
    }
    // 单个URL
    return [
      {
        name: value.split('/').pop() || value,
        url: value,
      },
    ];
  }
  return [];
}

// 暴露方法给外部调用
defineExpose({
  // 打开审批详情 - 直接加载数据（使用 props 中的 tableName 和 businessId）
  openWithData: async (_row?: any) => {
    // _row 参数保留兼容，但不依赖它获取数据
    await loadAllData();
  },
  // 兼容旧接口
  open: async () => {
    await loadAllData();
  },
  close: () => {
    // 重置数据
  },
});
</script>

<template>
  <!-- 直接渲染内容，不使用 Modal 包装 -->
  <!-- 外部已经有 a-modal 包裹 -->
  <div class="approval-detail-wrapper">
    <div v-if="loading" class="flex items-center justify-center py-12">
      <Spin size="large" />
    </div>

    <div v-else class="approval-detail-content">
      <!-- 备案状态标识区 -->
      <div class="filing-status-section">
        <div class="filing-status-tag" :class="getProcessStatusClass(approvalDetail?.status)">
          {{ getProcessStatusText(approvalDetail?.status) }}
        </div>
        <h2 class="filing-title">
          {{ filingData?.orgName || '项目备案' }}
        </h2>
        <div class="filing-meta">
          <span v-if="filingData?.projectType">
            <i class="fas fa-tag mr-1" />
            备案类型：{{ getProjectTypeText(filingData?.projectType) }}
          </span>
          <span v-if="filingData?.createTime">
            <i class="fas fa-clock mr-1" />
            提交时间：{{ formatDate(filingData?.createTime) }}
          </span>
        </div>
      </div>

      <!-- 流程进度 -->
      <div class="detail-card">
        <div class="detail-card-header">
          <h3 class="detail-card-title">
            <i class="fas fa-project-diagram mr-2" />
            流程进度
          </h3>
        </div>
        <div class="detail-card-content">
          <Steps
            :current="processNodes.findIndex((n) => n.id === currentNodeId)"
            size="small"
          >
            <Step
              v-for="node in processNodes"
              :key="node.id"
              :title="node.name"
              :status="
                getNodeStatus(node) === 2
                  ? 'finish'
                  : getNodeStatus(node) === 1
                    ? 'process'
                    : 'wait'
              "
            />
          </Steps>
        </div>
      </div>

      <!-- 填报信息区块 -->
      <div class="detail-card">
        <div class="detail-card-header">
          <h3 class="detail-card-title">
            <i class="fas fa-building mr-2" />
            填报信息
          </h3>
        </div>
        <div class="detail-card-content">
          <!-- Filing 表基础信息 -->
          <div class="info-section">
            <div class="info-section-title">机构信息</div>
            <div class="info-grid">
              <div class="info-item">
                <label class="info-label">统一社会信用代码：</label>
                <span class="info-value">
                  {{ filingData?.socialCreditCode || '-' }}
                </span>
              </div>
              <div class="info-item">
                <label class="info-label">执业许可证号：</label>
                <span class="info-value">
                  {{ filingData?.medicalLicenseNo || '-' }}
                </span>
              </div>
              <div class="info-item">
                <label class="info-label">机构名称：</label>
                <span class="info-value font-medium">
                  {{ filingData?.orgName || '-' }}
                </span>
              </div>
              <div class="info-item">
                <label class="info-label">项目类型：</label>
                <span class="info-value">
                  {{ getProjectTypeText(filingData?.projectType) }}
                </span>
              </div>
              <div class="info-item">
                <label class="info-label">有效期开始：</label>
                <span class="info-value">
                  {{ formatDateOnly(filingData?.validStartTime) || '-' }}
                </span>
              </div>
              <div class="info-item">
                <label class="info-label">有效期结束：</label>
                <span class="info-value">
                  {{ formatDateOnly(filingData?.validEndTime) || '-' }}
                </span>
              </div>
              <div class="info-item col-span-2">
                <label class="info-label">建设内容：</label>
                <span class="info-value">
                  {{ filingData?.constructionContent || '-' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

      <!-- 指标分类信息 - 折叠面板 -->
      <a-collapse
        v-if="indicatorGroups.length"
        v-model:activeKey="activeCollapseKeys"
        class="indicator-collapse"
      >
        <a-collapse-panel
          v-for="group in indicatorGroups"
          :key="group.category"
          :header="`${group.categoryName} (${group.indicators.length}个指标)`"
        >
          <div class="info-grid">
                <div
                  v-for="indicator in group.indicators"
                  :key="indicator.id"
                  class="info-item"
                  :class="{ 'col-span-2': indicator.valueType === 5 }"
                >
                  <label class="info-label">{{ indicator.indicatorName }}：</label>
                  <div class="info-value-wrap">
                    <!-- 数字类型 -->
                    <template v-if="indicator.valueType === 1">
                      <span class="info-value">
                        {{ indicatorValuesMap[indicator.id] || '-' }}
                      </span>
                      <span v-if="indicator.unit" class="unit">{{ indicator.unit }}</span>
                    </template>

                    <!-- 字符串类型 -->
                    <template v-else-if="indicator.valueType === 2">
                      <span class="info-value">{{ indicatorValuesMap[indicator.id] || '-' }}</span>
                    </template>

                    <!-- 布尔类型 -->
                    <template v-else-if="indicator.valueType === 3">
                      <span
                        class="status-tag"
                        :class="
                          indicatorValuesMap[indicator.id]
                            ? 'status-tag-success'
                            : 'status-tag-pending'
                        "
                      >
                        {{ indicatorValuesMap[indicator.id] ? '是' : '否' }}
                      </span>
                    </template>

                    <!-- 日期类型 -->
                    <template v-else-if="indicator.valueType === 4">
                      <span class="info-value">{{ formatDate(indicatorValuesMap[indicator.id]) || '-' }}</span>
                    </template>

                    <!-- 长文本类型 -->
                    <template v-else-if="indicator.valueType === 5">
                      <span class="info-value long-text">
                        {{ indicatorValuesMap[indicator.id] || '-' }}
                      </span>
                    </template>

                    <!-- 单选类型 - 显示全部选项，用户选中项主色标注 -->
                    <template v-else-if="indicator.valueType === 6">
                      <div class="option-list">
                        <span
                          v-for="opt in getOptions(indicator.valueOptions)"
                          :key="opt.value"
                          class="option-tag"
                          :class="{
                            'option-selected': String(opt.value) === String(indicatorValuesMap[indicator.id])
                          }"
                        >
                          {{ opt.label }}
                        </span>
                      </div>
                    </template>

                    <!-- 多选类型 - 显示全部选项，用户选中项主色标注 -->
                    <template v-else-if="indicator.valueType === 7">
                      <div class="option-list">
                        <span
                          v-for="opt in getOptions(indicator.valueOptions)"
                          :key="opt.value"
                          class="option-tag"
                          :class="{
                            'option-selected': (indicatorValuesMap[indicator.id] || []).includes(opt.value)
                          }"
                        >
                          {{ opt.label }}
                        </span>
                      </div>
                    </template>

                    <!-- 日期区间类型 -->
                    <template v-else-if="indicator.valueType === 8">
                      <span class="info-value">
                        {{
                          indicatorValuesMap[indicator.id]?.start
                            ? `${indicatorValuesMap[indicator.id].start} ~ ${indicatorValuesMap[indicator.id].end}`
                            : '-'
                        }}
                      </span>
                    </template>

                    <!-- 默认显示 -->
                    <template v-else>
                      <span class="info-value">{{ indicatorValuesMap[indicator.id] || '-' }}</span>
                    </template>
                  </div>
                </div>
              </div>
            </a-collapse-panel>
          </a-collapse>

          <!-- 无指标数据时显示提示 -->
          <div v-if="!indicatorGroups.length" class="empty-text">
            暂无指标数据
          </div>

      <!-- 审核记录区 -->
      <div class="detail-card">
        <div class="detail-card-header">
          <h3 class="detail-card-title">
            <i class="fas fa-history mr-2" />
            审核记录
          </h3>
        </div>
        <div class="detail-card-content">
          <div v-if="approvalHistory.length" class="audit-records">
            <div
              v-for="task in approvalHistory"
              :key="task.id"
              class="audit-record"
            >
              <div class="audit-record-header">
                <div class="audit-record-info">
                  <span class="audit-stage">{{ task.name }}</span>
                  <!-- 操作结果标签：通过(2)/拒绝(3)/退回(5) -->
                  <a-tag v-if="task.status === 5 || task.actionLabel === '退回'" color="orange">退回</a-tag>
                  <a-tag v-else-if="task.approved === true || task.actionLabel === '通过'" color="green">通过</a-tag>
                  <a-tag v-else-if="task.approved === false || task.actionLabel === '拒绝'" color="red">拒绝</a-tag>
                  <span class="audit-operator">
                    操作人：{{
                      task.assigneeUser?.nickname ||
                      task.ownerUser?.nickname ||
                      '-'
                    }}
                  </span>
                </div>
                <span class="audit-time">{{ formatDate(task.createTime || task.endTime) }}</span>
              </div>
              <!-- 审批意见 -->
              <div v-if="task.reason" class="audit-reason">
                <span class="reason-label">审批意见：</span>
                {{ task.reason }}
              </div>
              <div v-if="task.endTime" class="audit-completed">
                已完成 · {{ formatDate(task.endTime) }}
              </div>
            </div>
          </div>
          <div v-else class="empty-text">暂无审核记录</div>
        </div>
      </div>

      <!-- 审批操作按钮：仅当 showActions 为 true 时在组件内显示；否则由父组件（如弹窗底部）负责展示 -->
      <div v-if="props.showActions && availableActions.length" class="action-buttons">
        <a-button
          v-for="action in availableActions"
          :key="action.key"
          :type="getActionType(action.key)"
          class="action-btn"
          @click="handleActionClick(action)"
        >
          {{ action.label }}
        </a-button>
      </div>
      <div v-else-if="props.showActions && !availableActions.length" class="empty-text">
        当前无可用审批操作
      </div>

    <!-- 审批操作弹窗 -->
    <a-modal
      v-model:open="actionModalVisible"
      :title="currentAction?.label || '审批操作'"
      :confirm-loading="actionModalLoading"
      @ok="handleActionConfirm"
    >
      <a-form layout="vertical">
        <a-form-item
          :label="currentAction?.vars?.reason || '审批意见'"
          :required="currentAction?.vars?.reasonRequired"
        >
          <a-textarea
            v-model:value="reasonForm.reason"
            :placeholder="
              currentAction?.vars?.reasonRequired
                ? '请输入审批意见（必填）'
                : '请输入审批意见（可选）'
            "
            :rows="4"
          />
        </a-form-item>
        </a-form>
      </a-modal>

    <!-- 专家选择弹窗 -->
    <ExpertSelectModal
      ref="expertModalRef"
      :multiple="true"
      :expert-min="1"
      :expert-max="10"
      @confirm="handleExpertConfirm"
    />
  </div>
  </template>

<style lang="scss" scoped>
.approval-detail-content {
  max-height: 70vh;
  overflow-y: auto;
  padding: 0 4px;
}

// 备案状态标识区
.filing-status-section {
  text-align: center;
  margin-bottom: 24px;
}

.filing-status-tag {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 12px;
  background-color: #F2E4D3;
  color: var(--color-primary, #2A5C45);
}

// 流程状态样式
.status-tag-success {
  background-color: #f6ffed !important;
  color: var(--color-primary, #2A5C45);
  border: 1px solid #b7eb8f;
}

.status-tag-error {
  background-color: #fff2f0 !important;
  color: #ff4d4f !important;
  border: 1px solid #ffccc7;
}

.status-tag-pending {
  background-color: #fff7e6 !important;
  color: #fa8c16 !important;
  border: 1px solid #ffd591;
}

.filing-title {
  font-size: 20px;
  font-weight: 600;
  color: #232931;
  margin-bottom: 8px;
}

.filing-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16px;
  font-size: 14px;
  color: #6C737A;
}

// 卡片样式 - GemDesign风格
.detail-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 16px;
  overflow: hidden;
}

.detail-card-header {
  background-color: var(--color-primary-light-9, #E6EEE8);
  padding: 12px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #232931;
  margin: 0;
  display: flex;
  align-items: center;

  i {
    color: var(--color-primary, #2A5C45);
  }
}

.detail-card-content {
  padding: 20px;
}

// 信息区块
.info-section {
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
}

.info-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #232931;
  padding-bottom: 8px;
  margin-bottom: 12px;
  border-bottom: 1px solid #D4D9DF;
}

// 表单信息样式
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  padding: 6px 0;

  &.col-span-2 {
    flex-direction: row;
    grid-column: span 2;
  }
}

.info-label {
  flex-shrink: 0;
  width: 300px;
  color: #6C737A;
  font-size: 14px;
}

.info-value {
  flex: 1;
  color: #232931;
  font-size: 14px;
  word-break: break-all;

  &.font-medium {
    font-weight: 500;
  }
}

.info-value-wrap {
  // flex: 1;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.status-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
}

.status-tag-success {
  background-color: var(--color-primary-light-9, #E6EEE8);
  color: var(--color-primary, #2A5C45);
  border: 1px solid var(--color-primary, #D4E4DD);
}

.status-tag-primary {
  background-color: var(--color-primary-light-9, #E6EEE8);
  color: var(--color-primary, #2A5C45);
  border: 1px solid var(--color-primary, #2A5C45);
}

// 选项列表样式
.option-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-tag {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
  background-color: #F3F4F6;
  color: #595959;
  border: 1px solid #D4D9DF;
  transition: all 0.2s ease;

  &.option-selected {
    background-color: var(--color-primary-light-9, #E6EEE8);
    color: var(--color-primary, #2A5C45);
    
    font-weight: 500;
  }
}

.status-tag-warning {
  background-color: #FFF7E6;
  color: #D46B08;
  border: 1px solid #FFD591;
}

.status-tag-pending {
  background-color: #F3F4F6;
  color: #6C737A;
  border: 1px solid #D4D9DF;
}

.status-tag-error {
  background-color: #FFF2F0;
  color: #9B3636;
  border: 1px solid #FFCCC7;
}

// 指标样式
.indicator-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.indicator-item {
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border-secondary, #f0f0f0);

  &:last-child {
    border-bottom: none;
  }
}

.indicator-label {
  font-size: 14px;
  color: var(--color-text-quaternary, #8c8c8c);
  margin-bottom: 6px;
  display: flex;
  align-items: center;

  .required-tag {
    margin-left: 6px;
    padding: 1px 6px;
    background-color: var(--color-error-bg, #fff2f0);
    color: var(--color-error, #ff4d4f);
    font-size: 12px;
    border-radius: 2px;
  }
}

.indicator-value {
  font-size: 14px;
  color: var(--color-text, #262626);
  word-break: break-all;

  .unit {
    margin-left: 4px;
    color: var(--color-text-quaternary, #8c8c8c);
    text-align: left;
    font-size: 12px;
  }

  .long-text {
    white-space: pre-wrap;
    max-height: 100px;
    overflow-y: auto;
    background: var(--color-bg-container, #fafafa);
    padding: 8px;
    border-radius: 4px;
  }
}

// 文件列表
.file-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-item {
  display: flex;
  align-items: center;
}

.download-link {
  color: var(--color-primary, #1677ff);
  cursor: pointer;
  display: flex;
  align-items: center;
  text-decoration: none;

  &:hover {
    color: var(--color-primary-hover, #4096ff);
    text-decoration: underline;
  }
}

// 审核记录
.audit-records {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.audit-record {
  border-left: 3px solid var(--color-border, #d9d9d9);
  padding-left: 16px;
  padding-bottom: 12px;
}

.audit-record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.audit-record-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.audit-stage {
  font-weight: 500;
  color: var(--color-text, #262626);
}

.audit-operator {
  color: var(--color-primary, #1677ff);
  font-weight: 500;
}

.audit-time {
  color: var(--color-text-quaternary, #8c8c8c);
  font-size: 13px;
}

.audit-reason {
  color: var(--color-text-secondary, #595959);
  font-size: 14px;
  margin-top: 4px;
  padding: 8px 12px;
  background-color: var(--color-bg-container, #fafafa);
  border-radius: 4px;
  border-left: 3px solid var(--color-primary, #1677ff);
}

.reason-label {
  font-weight: 500;
  color: var(--color-text-secondary, #595959);
}

.audit-completed {
  color: var(--color-success, #52c41a);
  font-size: 12px;
  margin-top: 4px;
}

// 空状态
.empty-text {
  text-align: center;
  color: var(--color-text-quaternary, #bfbfbf);
  padding: 20px 0;
}

// 操作按钮
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 20px 0;
  border-top: 1px solid var(--color-border-secondary, #f0f0f0);
}

.action-btn {
  min-width: 100px;
}

// 调整ant design vue组件样式
:deep(.ant-steps) {
  padding: 0 8px;
}

:deep(.ant-collapse) {
  background: transparent;
  border: none;

  .ant-collapse-item {
    border: none;
    margin-bottom: 8px;
    background: var(--color-bg-container, white);
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    overflow: hidden;

    &:last-child {
      margin-bottom: 0;
    }

    &.ant-collapse-item-active .ant-collapse-header {
      background-color: var(--color-primary-light-9, #E6EEE8) !important; /* 浅绿色，与填报信息卡片头一致 */
    }
  }

  .ant-collapse-header {
    background-color: var(--color-primary-light-9, #E6EEE8) !important; /* 浅绿色，与填报信息卡片头一致 */
    padding: 12px 20px !important;
    padding-right: 48px !important;
    font-weight: bold;
    color: var(--color-text, #262626) !important;
    position: relative;

    // 箭头移到右侧
    .ant-collapse-expand-icon {
      position: absolute;
      right: 16px;
      left: auto !important;
    }

    .ant-collapse-header-text {
      flex: 1;
    }
  }

  .ant-collapse-content {
    border-top: none;
  }

  .ant-collapse-content-box {
    padding: 16px 20px !important;
  }
}

:deep(.ant-tabs-nav) {
  margin-bottom: 16px;

  &::before {
    border-bottom: 1px solid var(--color-border-secondary, #f0f0f0);
  }
}

:deep(.ant-tabs-tab) {
  padding: 12px 20px;
  font-size: 15px;

  &.ant-tabs-tab-active .ant-tabs-tab-btn {
    color: var(--color-primary, #1677ff) !important;
    font-weight: 500;
  }
}

:deep(.ant-tabs-ink-bar) {
  background: var(--color-primary, #1677ff);
}
</style>
