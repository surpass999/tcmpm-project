<script lang="ts" setup>
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { DeclareIndicatorValueApi } from '#/api/declare/indicatorValue';
import type { DeclareHospitalApi } from '#/api/declare/hospital';
import type { BpmTaskApi } from '#/api/bpm/task';
import type { BpmActionApi } from '#/api/bpm/action';

import { nextTick, ref, computed } from 'vue';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import { useUserStore } from '@vben/stores';

import { Spin, Steps, Step, Modal } from 'ant-design-vue';
import { message } from 'ant-design-vue';

import { getProgressReportIndicatorValues } from '#/api/declare/indicator';
import { submitProgressReport } from '#/api/declare/progress-report';
import { getIndicatorGroupList } from '#/api/declare/indicator-group';
import { getHospital } from '#/api/declare/hospital';
import {
  getProcessByBusiness,
  getTaskByBusiness,
} from '#/api/bpm/task';
import { getAvailableActions } from '#/api/bpm/action';

import ActionButton from '#/components/bpm/ActionButton.vue';
import ReturnModal from '#/components/bpm/ReturnModal.vue';

const userStore = useUserStore();

const BUSINESS_TYPE = 'progress_report:submit';
const BUSINESS_TYPE_NUMBER = 3; // 进度填报

// 弹窗数据载荷类型
interface ApprovalDetailPayload {
  processInstanceId?: string;
  reportId: number;
  hospitalId?: number;
  deptId: number;
  hospitalName: string;
  reportYear: number;
  reportBatch: number;
  projectType?: number;
  projectTypeName?: string;
  reportStatus?: string;
}

// 弹窗数据载荷（由父组件的 openWithData 传入）
const payload = ref<ApprovalDetailPayload | null>(null);

const emit = defineEmits<{
  success: [];
}>();

// 弹窗是否可见
const visible = ref(false);

// 弹窗标题
const modalTitle = computed(() => {
  if (!payload.value) return '审批详情';
  return `审批详情 - ${payload.value.hospitalName || ''} - ${payload.value.reportYear}年第${payload.value.reportBatch}期`;
});

// 加载状态
const loading = ref(false);

// 医院信息
const hospitalInfo = ref<DeclareHospitalApi.Hospital | null>(null);

// 分组信息映射
const groupInfoMap = ref<Record<number, { groupName: string; parentId: number; groupLevel: number; groupPrefix: string }>>({});

// 指标分组（树形结构：一级 + 二级）
interface IndicatorGroup {
  groupId: number;
  groupName: string;
  parentId: number;
  groupLevel: number;
  groupPrefix: string;
  indicators: DeclareIndicatorApi.Indicator[];
  children: IndicatorGroup[];
}

const indicatorGroups = ref<IndicatorGroup[]>([]);

// 指标值 (indicatorId -> value)
const indicatorValuesMap = ref<Record<number, any>>({});

// 流程信息（来自 getProcessByBusiness）
const processInfo = ref<{
  processInstanceId: string;
  processDefinitionKey: string;
  processDefinitionName: string;
  status: string;
  startTime?: string;
  endTime?: string;
  activityNodes: any[];
} | null>(null);

// 审批历史（来自 getTaskByBusiness）
const approvalHistory = ref<BpmTaskApi.Task[]>([]);

// 可用操作
const availableActions = ref<(BpmActionApi.BpmAction & { taskId?: string })[]>([]);

// 任务信息（从 getAvailableActions 返回）
const taskInfo = ref<{ taskId?: string; processInstanceId?: string; reasonRequire?: boolean } | null>(null);

// 退回弹窗 ref
const returnModalRef = ref<InstanceType<typeof ReturnModal> | null>(null);

// 流程节点
const processNodes = computed(() => processInfo.value?.activityNodes || []);

const currentNodeId = computed(() => {
  if (!processInfo.value) return null;
  // 找到进行中的节点
  const running = processInfo.value.activityNodes.find(
    (n: any) => n.status === 1 || (n.startTime && !n.endTime),
  );
  return running?.id || null;
});

/** 当前用户所属医院ID */
const currentHospitalId = computed(() => Number(userStore.userInfo?.deptId) || 0);

/** 当前填报是否为当前用户所在医院，且状态为 SAVED，可提交审核 */
const canSubmitAudit = computed(
  () =>
    payload.value?.deptId === currentHospitalId.value
    && (payload.value?.reportStatus === 'SAVED' || payload.value?.reportStatus === 'DRAFT'),
);

/** 提交审核 */
async function handleSubmitAudit() {
  if (!payload.value?.reportId) return;

  const hospitalName = payload.value?.hospitalName || '';
  const year = payload.value?.reportYear || '';
  const batch = payload.value?.reportBatch || '';

  Modal.confirm({
    title: '确认提交审核',
    content: `确认提交「${hospitalName}」${year}年第${batch}期进度填报？\n提交后将进入审批流程，提交后不可再编辑。`,
    okText: '确认提交',
    cancelText: '取消',
    okButtonProps: { danger: true },
    async onOk() {
      try {
        await submitProgressReport(payload.value!.reportId);
        message.success('提交成功');
        emit('success');
        await loadAllData();
      } catch (error: any) {
        message.error(error.message || '提交失败');
      }
    },
  });
}

function getNodeStatus(node: any): number {
  if (node.endTime || node.status === 2) return 2;
  if (node.startTime) return 1;
  return 0;
}

function formatDate(date: string | Date | number | undefined): string {
  if (!date && date !== 0) return '-';
  const d = new Date(typeof date === 'number' ? date : date);
  if (isNaN(d.getTime())) return '-';
  return d.toLocaleString('zh-CN');
}

function getProjectTypeText(): string {
  if (payload.value?.projectTypeName) return payload.value.projectTypeName;
  if (payload.value?.projectType !== undefined) {
    const options = getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number');
    const found = options.find((item) => Number(item.value) === payload.value!.projectType);
    if (found?.label) return found.label;
  }
  return '-';
}

function getProcessStatusText(): string {
  const status = processInfo.value?.status;
  if (!status) return '未知';
  switch (status) {
    case 'RUNNING': return '审批中';
    case 'ENDED': return '已结束';
    default: return status;
  }
}

function getProcessStatusClass(): string {
  const status = processInfo.value?.status;
  if (status === 'ENDED') return 'status-tag-success';
  if (status === 'RUNNING') return 'status-tag-pending';
  return 'status-tag-pending';
}

function getActionType(key: string): string {
  if (key.includes('pass') || key.includes('agree') || key.includes('approve') || key.includes('通过')) return 'primary';
  if (key.includes('reject') || key.includes('refuse') || key.includes('驳回') || key.includes('拒绝')) return 'default';
  return 'default';
}

function resetState() {
  payload.value = null;
  hospitalInfo.value = null;
  indicatorGroups.value = [];
  indicatorValuesMap.value = {};
  processInfo.value = null;
  approvalHistory.value = [];
  availableActions.value = [];
  taskInfo.value = null;
  groupInfoMap.value = {};
}

function close() {
  visible.value = false;
  resetState();
}

async function loadAllData() {
  if (!payload.value?.reportId) {
    console.warn('[approval-detail] 缺少 reportId，无法加载数据');
    return;
  }

  loading.value = true;
  try {
    // 0. 加载医院信息（用于统一社会信用代码、执业许可证号）
    if (payload.value.hospitalId) {
      try {
        hospitalInfo.value = await getHospital(payload.value.hospitalId);
      } catch (e) {
        console.warn('[approval-detail] 加载医院信息失败:', e);
        hospitalInfo.value = null;
      }
    }

    // 1. 加载分组信息
    try {
      const groupList = await getIndicatorGroupList();
      const map: Record<number, { groupName: string; parentId: number; groupLevel: number; groupPrefix: string }> = {};
      for (const g of groupList) {
        if (g.id) {
          map[g.id] = {
            groupName: g.groupName,
            parentId: g.parentId ?? 0,
            groupLevel: g.groupLevel ?? 1,
            groupPrefix: g.groupPrefix ?? '',
          };
        }
      }
      groupInfoMap.value = map;
    } catch (e) {
      console.warn('[approval-detail] 加载分组信息失败:', e);
      groupInfoMap.value = {};
    }

    // 2. 加载指标数据（businessType 为 'process'）
    const { getIndicatorsByBusinessType } = await import('#/api/declare/indicator');
    const indicators = await getIndicatorsByBusinessType('process', payload.value!.projectType!);

    // 3. 构建两级分组
    buildIndicatorGroups(indicators);

    // 4. 加载指标值
    const values = await getProgressReportIndicatorValues(payload.value!.reportId, BUSINESS_TYPE_NUMBER);
    const valuesMap: Record<number, any> = {};
    for (const v of values) {
      valuesMap[v.indicatorId] = parseIndicatorValue(v);
    }
    indicatorValuesMap.value = valuesMap;

    // 以下 BPM 相关数据仅在有 reportId 时加载
    if (!payload.value?.reportId) {
      processInfo.value = null;
      approvalHistory.value = [];
      availableActions.value = [];
      return;
    }

    // 5. 通过业务ID查询流程信息（getProcessByBusiness = /bpm/declare/task-status/process-query-by-business）
    const processList = await getProcessByBusiness({
      tableName: 'progress',
      businessId: payload.value.reportId,
    });
    if (processList && processList.length > 0) {
      // 取最新的流程实例
      processInfo.value = processList[0] as any;
    } else {
      processInfo.value = null;
    }

    // 6. 通过业务ID查询任务状态（getTaskByBusiness = /bpm/declare/task-status/query-by-business）
    const taskResult = await getTaskByBusiness({
      tableName: 'progress',
      businessIds: [payload.value.reportId],
    });
    const taskStatus = taskResult?.[0];

    // 7. 获取审批历史（已完成的任务）
    approvalHistory.value = (taskStatus?.allDoneTasks || [])
      .sort((a: any, b: any) => String(a.endTime || '').localeCompare(String(b.endTime || ''))) as any;

    // 8. 获取可用操作（基于待办任务）
    const rawActions = await getAvailableActions(BUSINESS_TYPE, payload.value!.reportId, 'progress');
    const buttons = !Array.isArray(rawActions) && rawActions?.actions ? rawActions.actions : {};
    taskInfo.value = !Array.isArray(rawActions) && rawActions?.taskInfo ? rawActions.taskInfo : null;
    availableActions.value = Object.entries(buttons)
      .filter(([, setting]: [string, any]) => setting.enable)
      .map(([key, setting]: [string, any]) => ({
        key,
        label: setting.displayName,
        bizStatus: setting.bizStatus,
        taskId: taskInfo.value?.taskId,
        vars: {
          reasonRequired: !!taskInfo.value?.reasonRequire,
          rectifyProcessDefinitionKey: setting.rectifyProcessDefinitionKey,
        },
      }));
  } catch (error) {
    console.error('[approval-detail] 加载数据失败:', error);
    message.error('加载数据失败');
  } finally {
    loading.value = false;
  }
}

function buildIndicatorGroups(indicators: DeclareIndicatorApi.Indicator[]) {
  const levelOneMap = new Map<number, IndicatorGroup>();
  const levelTwoMap = new Map<number, IndicatorGroup>();

  // 注册所有一级分组
  for (const [gid, info] of Object.entries(groupInfoMap.value)) {
    if (info.parentId === 0) {
      levelOneMap.set(Number(gid), {
        groupId: Number(gid),
        groupName: info.groupPrefix ? `${info.groupPrefix} ${info.groupName}` : info.groupName,
        groupPrefix: info.groupPrefix ?? '',
        parentId: 0,
        groupLevel: 1,
        indicators: [],
        children: [],
      });
    }
  }

  // 分配指标到分组
  for (const ind of indicators) {
    const gid = ind.groupId || 0;
    const info = groupInfoMap.value[gid];
    if (!info) continue;

    if (info.parentId === 0) {
      const g = levelOneMap.get(gid);
      if (g) g.indicators.push(ind);
    } else {
      if (!levelTwoMap.has(gid)) {
        levelTwoMap.set(gid, {
          groupId: gid,
          groupName: info.groupPrefix ? `${info.groupPrefix} ${info.groupName}` : info.groupName,
          groupPrefix: info.groupPrefix ?? '',
          parentId: info.parentId,
          groupLevel: 2,
          indicators: [],
          children: [],
        });
      }
      levelTwoMap.get(gid)!.indicators.push(ind);
    }
  }

  // 将二级分组挂到对应一级分组下
  for (const [, lvl2] of levelTwoMap) {
    const parent = levelOneMap.get(lvl2.parentId);
    if (parent) {
      parent.children.push(lvl2);
    }
  }

  // 排序并组装结果：只保留有指标的一级分组，二级分组跟随其父级输出
  const result: IndicatorGroup[] = [];
  const sortInds = (g: IndicatorGroup) => {
    g.indicators.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));
    g.children.forEach(sortInds);
  };
  for (const [, lvl1] of levelOneMap) {
    sortInds(lvl1);
    // 只有当一级分组自身有指标，或其子分组有指标时才输出
    const hasIndicator = lvl1.indicators.length > 0;
    const hasChildIndicator = lvl1.children.some((c) => c.indicators.length > 0);
    if (hasIndicator || hasChildIndicator) {
      result.push(lvl1);
      const sortedLvl2 = [...lvl1.children].sort((a, b) => a.groupId - b.groupId);
      result.push(...sortedLvl2);
    }
  }

  indicatorGroups.value = result;
}

function parseIndicatorValue(item: DeclareIndicatorValueApi.IndicatorValue | any): any {
  if (!item) return undefined;
  switch (item.valueType) {
    case 1: return item.valueNum;
    case 2:
    case 6:
    case 10: return item.valueStr;
    case 7:
    case 11: return item.valueStr ? item.valueStr.split(',') : [];
    case 3: return item.valueBool;
    case 4: return item.valueDate;
    case 5: return item.valueText;
    case 8: return item.valueDateStart ? [item.valueDateStart, item.valueDateEnd] : undefined;
    default: return item.valueStr;
  }
}

function parseOptions(valueOptions: string) {
  if (!valueOptions) return [];
  try { return JSON.parse(valueOptions); } catch { return []; }
}

function getIndicatorId(id?: number): number {
  return id ?? 0;
}

// 暴露给父组件调用的方法
defineExpose({
  async openWithData(data: ApprovalDetailPayload) {
    if (!data) return;
    payload.value = data;
    visible.value = true;
    await nextTick();
    await loadAllData();
  },
  close,
});
</script>

<template>
  <Modal
    v-model:open="visible"
    :title="modalTitle"
    class="approval-detail-modal"
    width="90%"
    :footer="null"
    @cancel="close"
  >
    <Spin v-if="loading" class="flex items-center justify-center py-12" />
    <div v-else class="approval-detail-content">

      <!-- 填报状态标识区 -->
      <div class="filing-status-section">
        <div class="filing-status-tag" :class="getProcessStatusClass()">
          {{ getProcessStatusText() }}
        </div>
        <h2 class="filing-title">{{ payload?.hospitalName || '进度填报审批' }}</h2>
        <div class="filing-meta">
          <span v-if="payload?.projectType">
            <i class="fas fa-tag mr-1" />
            项目类型：{{ getProjectTypeText() }}
          </span>
          <span>
            <i class="fas fa-calendar mr-1" />
            {{ payload?.reportYear }}年第{{ payload?.reportBatch }}期
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
          <div class="info-section">
            <div class="info-grid">
              <div class="info-item">
                <label class="info-label">统一社会信用代码：</label>
                <span class="info-value">{{ hospitalInfo?.unifiedSocialCreditCode || '-' }}</span>
              </div>
              <div class="info-item">
                <label class="info-label">医疗机构执业许可证登记号：</label>
                <span class="info-value">{{ hospitalInfo?.medicalLicenseNo || '-' }}</span>
              </div>
              <div class="info-item">
                <label class="info-label">机构名称：</label>
                <span class="info-value font-medium">{{ payload?.hospitalName || '-' }}</span>
              </div>
              <div class="info-item">
                <label class="info-label">项目类型：</label>
                <span class="info-value">{{ getProjectTypeText() }}</span>
              </div>
              <div class="info-item">
                <label class="info-label">填报年度：</label>
                <span class="info-value">{{ payload?.reportYear }}年第{{ payload?.reportBatch }}期</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 指标数据 - 静态卡片（支持两级分组） -->
      <template v-if="indicatorGroups.length">
        <template v-for="group in indicatorGroups" :key="group.groupId">
          <!-- 一级分组卡片 -->
          <div v-if="group.groupLevel === 1" class="detail-card">
            <div class="detail-card-header">
              <h3 class="detail-card-title">
                <i class="fas fa-layer-group mr-2" />
                {{ group.groupName }} ({{ group.indicators.length + group.children.reduce((sum, c) => sum + c.indicators.length, 0) }}个指标)
              </h3>
            </div>
            <div v-if="group.indicators.length" class="detail-card-content">
              <div class="info-grid">
                <div
                  v-for="indicator in group.indicators"
                  :key="getIndicatorId(indicator.id)"
                  class="info-item"
                  :class="{ 'col-span-2': indicator.valueType === 5 }"
                >
                  <label class="info-label">{{ indicator.indicatorName }}：</label>
                  <div class="info-value-wrap">
                    <template v-if="indicator.valueType === 1">
                      <span class="info-value">{{ indicatorValuesMap[getIndicatorId(indicator.id)] || '-' }}</span>
                      <span v-if="indicator.unit" class="unit">{{ indicator.unit }}</span>
                    </template>
                    <template v-else-if="indicator.valueType === 2">
                      <span class="info-value">{{ indicatorValuesMap[getIndicatorId(indicator.id)] || '-' }}</span>
                    </template>
                    <template v-else-if="indicator.valueType === 3">
                      <span class="status-tag" :class="indicatorValuesMap[getIndicatorId(indicator.id)] ? 'status-tag-success' : 'status-tag-pending'">
                        {{ indicatorValuesMap[getIndicatorId(indicator.id)] ? '是' : '否' }}
                      </span>
                    </template>
                    <template v-else-if="indicator.valueType === 4">
                      <span class="info-value">{{ formatDate(indicatorValuesMap[getIndicatorId(indicator.id)]) || '-' }}</span>
                    </template>
                    <template v-else-if="indicator.valueType === 5">
                      <span class="info-value long-text">{{ indicatorValuesMap[getIndicatorId(indicator.id)] || '-' }}</span>
                    </template>
                    <template v-else-if="indicator.valueType === 6">
                      <div class="option-list">
                        <span
                          v-for="opt in parseOptions(indicator.valueOptions)"
                          :key="opt.value"
                          class="option-tag"
                          :class="{ 'option-selected': String(opt.value) === String(indicatorValuesMap[getIndicatorId(indicator.id)]) }"
                        >{{ opt.label }}</span>
                      </div>
                    </template>
                    <template v-else-if="indicator.valueType === 7">
                      <div class="option-list">
                        <span
                          v-for="opt in parseOptions(indicator.valueOptions)"
                          :key="opt.value"
                          class="option-tag"
                          :class="{ 'option-selected': (indicatorValuesMap[getIndicatorId(indicator.id)] || []).includes(opt.value) }"
                        >{{ opt.label }}</span>
                      </div>
                    </template>
                    <template v-else-if="indicator.valueType === 8">
                      <span class="info-value">
                        {{ indicatorValuesMap[getIndicatorId(indicator.id)]?.start ? `${indicatorValuesMap[getIndicatorId(indicator.id)].start} ~ ${indicatorValuesMap[getIndicatorId(indicator.id)].end}` : '-' }}
                      </span>
                    </template>
                    <template v-else>
                      <span class="info-value">{{ indicatorValuesMap[getIndicatorId(indicator.id)] || '-' }}</span>
                    </template>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <!-- 二级分组卡片（必须与一级互斥：一级也有 indicators，不能用 v-if="indicators.length"） -->
          <div v-else-if="group.groupLevel === 2 && group.indicators.length" class="detail-card group-level-2">
            <div class="detail-card-header">
              <h3 class="detail-card-title">
                <i class="fas fa-cube mr-2" />
                　└ {{ group.groupName }} ({{ group.indicators.length }}个指标)
              </h3>
            </div>
            <div class="detail-card-content">
              <div class="info-grid">
                <div
                  v-for="indicator in group.indicators"
                  :key="getIndicatorId(indicator.id)"
                  class="info-item"
                  :class="{ 'col-span-2': indicator.valueType === 5 }"
                >
                  <label class="info-label">{{ indicator.indicatorName }}：</label>
                  <div class="info-value-wrap">
                    <template v-if="indicator.valueType === 1">
                      <span class="info-value">{{ indicatorValuesMap[getIndicatorId(indicator.id)] || '-' }}</span>
                      <span v-if="indicator.unit" class="unit">{{ indicator.unit }}</span>
                    </template>
                    <template v-else-if="indicator.valueType === 2">
                      <span class="info-value">{{ indicatorValuesMap[getIndicatorId(indicator.id)] || '-' }}</span>
                    </template>
                    <template v-else-if="indicator.valueType === 3">
                      <span class="status-tag" :class="indicatorValuesMap[getIndicatorId(indicator.id)] ? 'status-tag-success' : 'status-tag-pending'">
                        {{ indicatorValuesMap[getIndicatorId(indicator.id)] ? '是' : '否' }}
                      </span>
                    </template>
                    <template v-else-if="indicator.valueType === 4">
                      <span class="info-value">{{ formatDate(indicatorValuesMap[getIndicatorId(indicator.id)]) || '-' }}</span>
                    </template>
                    <template v-else-if="indicator.valueType === 5">
                      <span class="info-value long-text">{{ indicatorValuesMap[getIndicatorId(indicator.id)] || '-' }}</span>
                    </template>
                    <template v-else-if="indicator.valueType === 6">
                      <div class="option-list">
                        <span
                          v-for="opt in parseOptions(indicator.valueOptions)"
                          :key="opt.value"
                          class="option-tag"
                          :class="{ 'option-selected': String(opt.value) === String(indicatorValuesMap[getIndicatorId(indicator.id)]) }"
                        >{{ opt.label }}</span>
                      </div>
                    </template>
                    <template v-else-if="indicator.valueType === 7">
                      <div class="option-list">
                        <span
                          v-for="opt in parseOptions(indicator.valueOptions)"
                          :key="opt.value"
                          class="option-tag"
                          :class="{ 'option-selected': (indicatorValuesMap[getIndicatorId(indicator.id)] || []).includes(opt.value) }"
                        >{{ opt.label }}</span>
                      </div>
                    </template>
                    <template v-else-if="indicator.valueType === 8">
                      <span class="info-value">
                        {{ indicatorValuesMap[getIndicatorId(indicator.id)]?.start ? `${indicatorValuesMap[getIndicatorId(indicator.id)].start} ~ ${indicatorValuesMap[getIndicatorId(indicator.id)].end}` : '-' }}
                      </span>
                    </template>
                    <template v-else>
                      <span class="info-value">{{ indicatorValuesMap[getIndicatorId(indicator.id)] || '-' }}</span>
                    </template>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </template>
      <div v-else-if="!loading" class="empty-text">暂无指标数据</div>

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
              :key="task.taskId"
              class="audit-record"
            >
              <div class="audit-record-header">
                <div class="audit-record-info">
                  <span class="audit-stage">{{ task.taskName }}</span>
                  <span class="audit-operator">
                    操作人：{{ task.assigneeUser?.nickname || '-' }}
                  </span>
                </div>
                <span class="audit-time">{{ formatDate(task.endTime) }}</span>
              </div>
              <div v-if="task.reason" class="audit-reason">{{ task.reason }}</div>
              <div v-if="task.endTime" class="audit-completed">已完成 · {{ formatDate(task.endTime) }}</div>
            </div>
          </div>
          <div v-else class="empty-text">暂无审核记录</div>
        </div>
      </div>

      <!-- 草稿/已保存状态：显示提交审核按钮 -->
      <div v-if="canSubmitAudit && !loading" class="action-buttons">
        <a-button type="primary" class="action-btn" @click="handleSubmitAudit">
          <i class="fas fa-paper-plane mr-1" />
          提交审核
        </a-button>
      </div>
      <!-- 审批中/审批完成：显示 BPM 工作流操作按钮 -->
      <div v-else-if="availableActions.length" class="action-buttons">
        <ActionButton
          :business-type="BUSINESS_TYPE"
          :business-id="payload?.reportId ?? 0"
          :actions="availableActions"
          :task-id="taskInfo?.taskId"
          @success="async () => { emit('success'); await loadAllData(); }"
          @refresh="loadAllData"
          @return="(action) => returnModalRef?.open({ taskId: action.taskId || taskInfo?.taskId || '', buttonId: action.key })"
        />
      </div>
      <div v-else-if="!loading" class="empty-text">当前无可用审批操作</div>

      <!-- 退回弹窗 -->
      <ReturnModal ref="returnModalRef" @success="async () => { emit('success'); await loadAllData(); }" />
    </div>
  </Modal>
</template>

<style lang="scss" scoped>
/* 与 Vben 主题 / Ant Design 变量一致：使用 hsl(var(--token)) 随主题切换 */
.approval-detail-content {
  max-height: 70vh;
  overflow-y: auto;
  padding: 0 4px;
}

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
  background: hsl(var(--primary) / 0.12);
  color: hsl(var(--primary));
}

.filing-title {
  font-size: 20px;
  font-weight: 600;
  color: hsl(var(--foreground));
  margin-bottom: 8px;
}

.filing-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16px;
  font-size: 14px;
  color: hsl(var(--muted-foreground));
}

.detail-card {
  background: hsl(var(--card));
  border-radius: 8px;
  box-shadow: 0 1px 3px hsl(var(--foreground) / 0.08);
  margin-bottom: 16px;
  overflow: hidden;
  border: 1px solid hsl(var(--border));
}

.detail-card-header {
  background: hsl(var(--primary) / 0.1);
  padding: 12px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-card-title {
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--foreground));
  margin: 0;
  display: flex;
  align-items: center;

  i {
    color: hsl(var(--primary));
  }
}

.detail-card-content {
  padding: 20px;
}

.info-section {
  margin-bottom: 20px;
  &:last-child { margin-bottom: 0; }
}

.info-section-title {
  font-size: 14px;
  font-weight: 600;
  color: hsl(var(--foreground));
  padding-bottom: 8px;
  margin-bottom: 12px;
  border-bottom: 1px solid hsl(var(--border));
}

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
  color: hsl(var(--muted-foreground));
  font-size: 14px;
}

.info-value {
  flex: 1;
  color: hsl(var(--foreground));
  font-size: 14px;
  word-break: break-all;

  &.font-medium { font-weight: 500; }
}

.info-value-wrap {
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
  background: hsl(var(--primary) / 0.12);
  color: hsl(var(--primary));
  border: 1px solid hsl(var(--primary) / 0.35);
}

.status-tag-error {
  background: hsl(var(--destructive) / 0.1);
  color: hsl(var(--destructive));
  border: 1px solid hsl(var(--destructive) / 0.35);
}

.status-tag-pending {
  background: hsl(var(--warning) / 0.18);
  color: hsl(var(--warning));
  border: 1px solid hsl(var(--warning) / 0.45);
}

.option-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-tag {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
  background: hsl(var(--muted));
  color: hsl(var(--muted-foreground));
  border: 1px solid hsl(var(--border));

  &.option-selected {
    background: hsl(var(--primary) / 0.12);
    color: hsl(var(--primary));
    font-weight: 500;
    border-color: hsl(var(--primary) / 0.35);
  }
}

.long-text {
  white-space: pre-wrap;
  max-height: 100px;
  overflow-y: auto;
  background: hsl(var(--muted));
  padding: 8px;
  border-radius: 4px;
}

.audit-records {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.audit-record {
  border-left: 3px solid hsl(var(--border));
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
  color: hsl(var(--foreground));
}

.audit-operator {
  color: hsl(var(--primary));
  font-weight: 500;
}

.audit-time {
  color: hsl(var(--muted-foreground));
  font-size: 13px;
}

.audit-reason {
  color: hsl(var(--muted-foreground));
  font-size: 14px;
  margin-top: 4px;
}

.audit-completed {
  color: hsl(var(--success));
  font-size: 12px;
  margin-top: 4px;
}

.empty-text {
  text-align: center;
  color: hsl(var(--muted-foreground));
  padding: 20px 0;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 20px 0;
  border-top: 1px solid hsl(var(--border));
}

.action-btn {
  min-width: 100px;
}

.detail-card.group-level-2 {
  margin-top: 4px;
  border-left: 3px solid hsl(var(--primary));

  .detail-card-header {
    background: hsl(var(--primary) / 0.06);
    padding: 8px 20px;
  }

  .detail-card-title {
    font-weight: 600;
    font-size: 14px;
    color: hsl(var(--foreground));
  }
}

:deep(.ant-steps) {
  padding: 0 8px;
}
</style>
