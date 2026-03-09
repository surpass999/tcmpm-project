<script lang="ts" setup>
import type { DeclareFilingApi } from '#/api/declare/filing';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { DeclareIndicatorValueApi } from '#/api/declare/indicatorValue';
import type { BpmProcessInstanceApi } from '#/api/bpm/processInstance';
import type { BpmTaskApi } from '#/api/bpm/task';
import type { BpmActionApi } from '#/api/bpm/action';

import { ref, shallowRef, computed } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Steps, Step, Tabs, TabPane, Tag, Button, Spin } from 'ant-design-vue';
import { message } from 'ant-design-vue';

import { getFiling } from '#/api/declare/filing';
import { getIndicatorsByProjectType } from '#/api/declare/indicator';
import { getIndicatorValues } from '#/api/declare/indicatorValue';
import { getApprovalDetail } from '#/api/bpm/processInstance';
import { getTaskListByProcessInstanceId } from '#/api/bpm/task';
import { getAvailableActions, submitBpmAction } from '#/api/bpm/action';

// 业务类型
const BUSINESS_TYPE = 'declare:filing:create';
// 指标业务类型（数字）
const BUSINESS_TYPE_NUMBER = 1;

// 使用 Vben Modal - 直接使用，不通过 connectedComponent
const [Modal, modalApi] = useVbenModal({
  destroyOnClose: true,
  showCancelButton: false,
  showConfirmButton: false,
  onOpenChange: async (isOpen: boolean) => {
    if (!isOpen) {
      return;
    }
    await loadAllData();
  },
});

// 存储最终的businessId和businessType
const finalBusinessId = ref<number>(0);
const finalBusinessType = ref<string>(BUSINESS_TYPE);

// 加载状态
const loading = ref(false);

// 备案数据
const filingData = ref<DeclareFilingApi.Filing | null>(null);

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

// 审批详情数据（包含流程节点）
const approvalDetail = ref<BpmProcessInstanceApi.ApprovalDetailRespVO | null>(null);

// 审批历史（任务列表）
const approvalHistory = ref<BpmTaskApi.Task[]>([]);

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

// 格式化日期
function formatDate(date: string | Date | undefined): string {
  if (!date) return '-';
  const d = new Date(date);
  return d.toLocaleString('zh-CN');
}

// 获取状态样式类
function getStatusClass(status?: string): string {
  if (status === 'APPROVED' || status === '已完成') return 'status-tag-success';
  if (status === 'REJECTED' || status === '已退回') return 'status-tag-error';
  return 'status-tag-pending';
}

// 获取状态显示文本
function getStatusText(status?: string): string {
  if (status === 'APPROVED' || status === '已完成') return '审核通过';
  if (status === 'REJECTED' || status === '已退回') return '审核退回';
  return '审核中';
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

// 执行审批操作
async function executeAction(
  action: BpmActionApi.BpmAction & { taskId?: string },
  reason: string
) {
  actionModalLoading.value = true;
  try {
    await submitBpmAction({
      businessType: finalBusinessType.value,
      businessId: finalBusinessId.value,
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

// 加载所有数据
async function loadAllData() {
  // 从 modalApi 获取数据
  const data = modalApi.getData<{
    businessId?: number;
    businessType?: string;
  }>();

  const id = data?.businessId;
  const type = data?.businessType || BUSINESS_TYPE;

  // 存储到 ref 供其他方法使用
  finalBusinessId.value = id as number;
  finalBusinessType.value = type;

  // 参数验证
  if (!id) {
    console.error('businessId 不能为空，data:', data);
    message.error('参数错误：业务ID不能为空');
    loading.value = false;
    return;
  }

  loading.value = true;
  try {
    // 1. 加载备案基本信息
    filingData.value = await getFiling(id);

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

      // 默认展开第一个分类
      if (indicatorGroups.value.length > 0) {
        activeCollapseKeys.value = [indicatorGroups.value[0].category];
      }

      // 获取指标值
      const values = await getIndicatorValues(BUSINESS_TYPE_NUMBER, id);
      const map: Record<number, any> = {};
      values.forEach((v) => {
        map[v.indicatorId] = parseIndicatorValue(v);
      });
      indicatorValuesMap.value = map;
    }

    // 3. 加载可用操作按钮（包含 taskId 信息）
    const actions = await getAvailableActions(type, id);
    availableActions.value = actions;

    // 4. 尝试获取流程实例信息
    let taskId: string | undefined;
    if (actions.length > 0 && actions[0].taskId) {
      taskId = actions[0].taskId;
    }

    // 5. 如果有 taskId，加载审批详情和审批历史
    if (taskId) {
      approvalDetail.value = await getApprovalDetail({ taskId });

      if (approvalDetail.value?.processInstance) {
        const processInstanceId = approvalDetail.value.processInstance.id;
        const tasks = await getTaskListByProcessInstanceId(processInstanceId);
        approvalHistory.value = (tasks || []).reverse();
      }
    } else {
      approvalDetail.value = null;
      approvalHistory.value = [];
    }
  } catch (error) {
    console.error('加载审批详情失败:', error);
    message.error('加载审批详情失败');
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

// 获取选项标签
function getOptionLabel(valueOptions: string, value: string): string {
  const options = parseOptions(valueOptions);
  const option = options.find((opt) => opt.value === value);
  return option?.label || value;
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

// 定义 emits
const emit = defineEmits<{
  success: [];
}>();

// 暴露打开弹窗的方法
defineExpose({
  open: () => modalApi.open(),
  close: () => modalApi.close(),
});
</script>

<template>
  <Modal
    :title="`审批详情 - ${filingData?.orgName || ''}`"
    class="approval-detail-modal"
    width="90%"
    :footer="null"
  >
    <Spin v-if="loading" size="large" class="flex items-center justify-center py-12" />

    <div v-else class="approval-detail-content">
      <!-- 备案状态 -->
      <div class="status-section">
        <span
          class="status-tag"
          :class="getStatusClass(filingData?.filingStatus)"
        >
          {{ getStatusText(filingData?.filingStatus) }}
        </span>
      </div>

      <!-- 流程进度 -->
      <div class="info-block">
        <div class="info-block-header">
          <i class="fas fa-project-diagram mr-2" />
          流程进度
        </div>
        <div class="info-block-content">
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

      <!-- Tab切换：基本信息 | 指标数据 -->
      <a-tabs default-active-key="form" class="mb-6">
        <a-tab-pane key="form" tab="基本信息">
          <!-- 基本信息区块 -->
          <div class="info-block">
            <div class="info-block-header">
              <i class="fas fa-building mr-2" />
              备案基础信息
            </div>
            <div class="info-block-content">
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
                    {{ filingData?.projectType || '-' }}
                  </span>
                </div>
                <div class="info-item">
                  <label class="info-label">有效期开始时间：</label>
                  <span class="info-value">
                    {{ filingData?.validStartTime || '-' }}
                  </span>
                </div>
                <div class="info-item">
                  <label class="info-label">有效期结束时间：</label>
                  <span class="info-value">
                    {{ filingData?.validEndTime || '-' }}
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
        </a-tab-pane>

        <!-- 指标数据Tab -->
        <a-tab-pane key="indicators" tab="指标数据">
          <a-collapse
            v-if="indicatorGroups.length"
            v-model:activeKey="activeCollapseKeys"
          >
            <a-collapse-panel
              v-for="group in indicatorGroups"
              :key="group.category"
              :header="`${group.categoryName} (${group.indicators.length}个指标)`"
            >
              <div class="indicator-grid">
                <div
                  v-for="indicator in group.indicators"
                  :key="indicator.id"
                  class="indicator-item"
                >
                  <div class="indicator-label">
                    {{ indicator.indicatorName }}
                    <span v-if="indicator.isRequired" class="required-tag">必填</span>
                  </div>
                  <div class="indicator-value">
                    <!-- 数字类型 -->
                    <template v-if="indicator.valueType === 1">
                      <span>{{ indicatorValuesMap[indicator.id] || '-' }}</span>
                      <span v-if="indicator.unit" class="unit">
                        {{ indicator.unit }}
                      </span>
                    </template>

                    <!-- 字符串类型 -->
                    <template v-else-if="indicator.valueType === 2">
                      {{ indicatorValuesMap[indicator.id] || '-' }}
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
                      {{ indicatorValuesMap[indicator.id] || '-' }}
                    </template>

                    <!-- 长文本类型 -->
                    <template v-else-if="indicator.valueType === 5">
                      <div class="long-text">
                        {{ indicatorValuesMap[indicator.id] || '-' }}
                      </div>
                    </template>

                    <!-- 单选类型 -->
                    <template v-else-if="indicator.valueType === 6">
                      <span class="status-tag status-tag-warning">
                        {{
                          getOptionLabel(
                            indicator.valueOptions,
                            indicatorValuesMap[indicator.id]
                          )
                        }}
                      </span>
                    </template>

                    <!-- 多选类型 -->
                    <template v-else-if="indicator.valueType === 7">
                      <span
                        v-for="val in indicatorValuesMap[indicator.id] || []"
                        :key="val"
                        class="status-tag status-tag-warning mr-1"
                      >
                        {{ getOptionLabel(indicator.valueOptions, val) }}
                      </span>
                      <span
                        v-if="!indicatorValuesMap[indicator.id]?.length"
                        >-</span
                      >
                    </template>

                    <!-- 日期区间类型 -->
                    <template v-else-if="indicator.valueType === 8">
                      <template
                        v-if="indicatorValuesMap[indicator.id]?.length === 2"
                      >
                        {{
                          indicatorValuesMap[indicator.id][0]
                        }}
                        ~
                        {{
                          indicatorValuesMap[indicator.id][1]
                        }}
                      </template>
                      <template v-else>-</template>
                    </template>

                    <!-- 文件上传类型 -->
                    <template v-else-if="indicator.valueType === 9">
                      <div
                        v-if="
                          parseFileList(indicatorValuesMap[indicator.id]).length
                        "
                        class="file-list"
                      >
                        <div
                          v-for="file in parseFileList(
                            indicatorValuesMap[indicator.id]
                          )"
                          :key="file.url"
                          class="file-item"
                        >
                          <a
                            :href="file.url"
                            target="_blank"
                            class="download-link"
                          >
                            <i class="fas fa-file-alt mr-1" />
                            {{ file.name }}
                          </a>
                        </div>
                      </div>
                      <span v-else>-</span>
                    </template>

                    <!-- 单选下拉类型 -->
                    <template v-else-if="indicator.valueType === 10">
                      <span class="status-tag status-tag-warning">
                        {{
                          getOptionLabel(
                            indicator.valueOptions,
                            indicatorValuesMap[indicator.id]
                          )
                        }}
                      </span>
                    </template>

                    <!-- 多选下拉类型 -->
                    <template v-else-if="indicator.valueType === 11">
                      <span
                        v-for="val in indicatorValuesMap[indicator.id] || []"
                        :key="val"
                        class="status-tag status-tag-warning mr-1"
                      >
                        {{ getOptionLabel(indicator.valueOptions, val) }}
                      </span>
                      <span
                        v-if="!indicatorValuesMap[indicator.id]?.length"
                        >-</span
                      >
                    </template>

                    <!-- 未知类型 -->
                    <template v-else>
                      {{ indicatorValuesMap[indicator.id] || '-' }}
                    </template>
                  </div>
                </div>
              </div>
            </a-collapse-panel>
          </a-collapse>
          <div v-else class="empty-text">暂无指标数据</div>
        </a-tab-pane>
      </a-tabs>

      <!-- 审核记录区 -->
      <div class="info-block">
        <div class="info-block-header">
          <i class="fas fa-history mr-2" />
          审核记录
        </div>
        <div class="info-block-content">
          <div v-if="approvalHistory.length" class="audit-records">
            <div
              v-for="task in approvalHistory"
              :key="task.id"
              class="audit-record"
            >
              <div class="audit-record-header">
                <div class="audit-record-info">
                  <span class="audit-stage">{{ task.name }}</span>
                  <span class="audit-operator">
                    操作人：{{
                      task.assigneeUser?.nickname ||
                      task.ownerUser?.nickname ||
                      '-'
                    }}
                  </span>
                </div>
                <span class="audit-time">{{ formatDate(task.createTime) }}</span>
              </div>
              <div v-if="task.reason" class="audit-reason">
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

      <!-- 审批操作按钮 -->
      <div v-if="availableActions.length" class="action-buttons">
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
      <div v-else class="empty-text">
        当前无可用审批操作
      </div>
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
  </Modal>
</template>

<style lang="scss" scoped>
.approval-detail-content {
  max-height: 70vh;
  overflow-y: auto;
  padding: 0 4px;
}

// 状态标签 - 使用CSS变量实现主题适配
.status-section {
  text-align: center;
  margin-bottom: 20px;
}

.status-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
}

.status-tag-success {
  background-color: var(--color-success-bg, #f6ffed);
  color: var(--color-success, #52c41a);
  border: 1px solid var(--color-success-border, #b7eb8f);
}

.status-tag-warning {
  background-color: var(--color-warning-bg, #fff7e6);
  color: var(--color-warning-text, #d46b08);
  border: 1px solid var(--color-warning-border, #ffd591);
}

.status-tag-pending {
  background-color: var(--color-bg-container, #fafafa);
  color: var(--color-text-quaternary, #bfbfbf);
  border: 1px solid var(--color-border, #d9d9d9);
}

.status-tag-error {
  background-color: var(--color-error-bg, #fff2f0);
  color: var(--color-error, #ff4d4f);
  border: 1px solid var(--color-error-border, #ffccc7);
}

// 区块样式 - 使用CSS变量
.info-block {
  background: var(--color-bg-container, white);
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 16px;
  overflow: hidden;
}

.info-block-header {
  background-color: var(--color-primary-light-9, #e6f7ff);
  padding: 12px 20px;
  font-size: 16px;
  font-weight: bold;
  color: var(--color-text, #232931);
  display: flex;
  align-items: center;
  cursor: pointer;

  i {
    color: var(--color-primary, #1677ff);
  }
}

.info-block-content {
  padding: 20px;
  background-color: var(--color-bg-container, white);
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
  padding: 8px 0;

  &.col-span-2 {
    flex-direction: column;
    grid-column: span 2;
  }
}

.info-label {
  flex-shrink: 0;
  width: 140px;
  color: var(--color-text-quaternary, #8c8c8c);
  font-size: 14px;
}

.info-value {
  flex: 1;
  color: var(--color-text, #262626);
  font-size: 14px;
  word-break: break-all;

  &.font-medium {
    font-weight: 500;
  }
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
  }

  .ant-collapse-header {
    background-color: var(--color-primary-light-9, #e6f7ff) !important;
    padding: 12px 20px !important;
    font-weight: bold;
    color: var(--color-text, #262626) !important;
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
