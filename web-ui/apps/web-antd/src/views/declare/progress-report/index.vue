<script lang="ts" setup>
import type { DeclareProgressReport } from '#/api/declare/progress-report';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { computed, nextTick, onMounted, ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

import { message, Modal } from 'ant-design-vue';
import { useUserStore } from '@vben/stores';

import {
  batchNationalReport,
  checkReportWindow,
  deleteProgressReport,
  getHospitalReportList,
  submitProgressReport,
} from '#/api/declare/progress-report';
import { getAvailableActionsBatch, submitBpmAction } from '#/api/bpm/action';
import { IconifyIcon } from '@vben/icons';
import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';

import Form from './modules/form.vue';
import ApprovalDetail from './modules/approval-detail.vue';
import ReturnModal from '#/components/bpm/ReturnModal.vue';
import DeclareCompareModal from './components/DeclareCompareModal.vue';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
  closeOnClickModal: false,
});

/** 审批详情组件 ref */
const approvalDetailRef = ref<InstanceType<typeof ApprovalDetail> | null>(null);

/** 退回弹窗组件 ref */
const returnModalRef = ref<InstanceType<typeof ReturnModal> | null>(null);

/** 对比弹窗组件 ref */
const compareModalRef = ref<InstanceType<typeof DeclareCompareModal> | null>(null);

/** 列表已选记录（用于数据对比） */
const selectedRows = ref<DeclareProgressReport[]>([]);

/** 列表行 BPM 操作：意见弹窗（不在页面顶部再渲染一套按钮，避免重复与无弹窗） */
const listBpmModalVisible = ref(false);
const listBpmLoading = ref(false);
const listBpmRow = ref<DeclareProgressReport | null>(null);
const listBpmCurrentAction = ref<any>(null);
const listBpmReason = ref('');

const userStore = useUserStore();

const BUSINESS_TYPE_KEY = 'progress_report:submit';

/** 当前用户ID */
const currentUserId = computed(() => {
  return Number(userStore.userInfo?.userId) || userStore.userInfo?.id;
});

/** 当前用户所属医院ID，从 deptId 获取 */
const hospitalId = computed(() => Number(userStore.userInfo?.deptId) || 0);


/** 是否有开放的填报窗口 */
const canCreate = ref(false);

/** 每行记录的 BPM 可用操作 { businessId: actions } */
const rowBpmActions = ref<Record<number, any[]>>({});

/** 加载时检查 */
onMounted(async () => {
  if (hospitalId.value) {
    try {
      canCreate.value = await checkReportWindow(hospitalId.value);
    } catch {
      canCreate.value = false;
    }
  }
});

/** 刷新表格 */
async function handleRefresh() {
  await nextTick();
  await nextTick();
  gridApi.query?.();
  if (hospitalId.value) {
    checkReportWindow(hospitalId.value)
      .then((res) => { canCreate.value = res; })
      .catch(() => { canCreate.value = false; });
  }
}

/** 对比按钮禁用状态：必须恰好选中2条且项目类型一致 */
const compareDisabled = computed(() => {
  if (selectedRows.value.length !== 2) return true;
  const types = new Set(selectedRows.value.map((r) => r.projectType));
  return types.size > 1;
});

/** 对比按钮 tooltip 提示 */
const compareTooltip = computed(() => {
  if (selectedRows.value.length < 2) return '请先选择2条申报记录';
  if (selectedRows.value.length > 2) return '只能选择2条记录进行对比';
  if (selectedRows.value[0]?.projectType !== selectedRows.value[1]?.projectType)
    return '所选记录项目类型不同，无法对比';
  return '';
});

/** 打开数据对比弹窗 */
function handleOpenCompare() {
  const [a, b] = selectedRows.value;
  if (a && b) {
    compareModalRef.value?.open(a.id, b.id);
  }
}

/** 监听复选框变化，更新已选行 */
function handleRowCheckboxChange(params: { records: DeclareProgressReport[] }) {
  selectedRows.value = [...params.records];
}
function handleCreate() {
  formModalApi.setData({ hospitalId: hospitalId.value }).open();
}

/** 编辑填报 */
function handleEdit(row: DeclareProgressReport) {
  formModalApi.setData(row).open();
}

/** 提交审核 */
async function handleSubmit(row: DeclareProgressReport) {
  await confirm(
    `确认提交"${row.hospitalName}"的${row.reportYear}年第${row.reportBatch}期填报？`,
  );
  const hideLoading = message.loading({ content: '提交中...', duration: 0 });
  try {
    await submitProgressReport(row.id);
    message.success('提交成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

/** 删除填报 */
async function handleDelete(row: DeclareProgressReport) {
  await confirm(
    `确认删除"${row.hospitalName}"的${row.reportYear}年第${row.reportBatch}期填报？删除后不可恢复。`,
  );
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteProgressReport(row.id);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

/** 批量上报国家局 */
async function handleBatchReport() {
  const selectedRows = (gridApi.grid?.getCheckboxRecords() || []) as DeclareProgressReport[];
  // 筛选出省级审核通过且未上报的记录
  const reportableRows = selectedRows.filter(
    (row) => row.provinceStatus === 2 && row.nationalReportStatus !== 1,
  );
  if (reportableRows.length === 0) {
    message.warning('请勾选省级审核通过且未上报的记录');
    return;
  }

  const names = reportableRows
    .map((r) => `${r.hospitalName} ${r.reportYear}年第${r.reportBatch}期`)
    .join('、');

  Modal.confirm({
    title: '确认批量上报国家局',
    content: `确认将以下 ${reportableRows.length} 条记录上报至国家局？\n${names}`,
    async onOk() {
      const hideLoading = message.loading({ content: '上报中...', duration: 0 });
      try {
        await batchNationalReport(reportableRows.map((r) => r.id));
        message.success('上报成功');
        handleRefresh();
      } catch (error: any) {
        message.error(error?.message || '上报失败');
      } finally {
        hideLoading();
      }
    },
  });
}

/** 单条记录上报国家局 */
async function handleSingleReport(row: DeclareProgressReport) {
  Modal.confirm({
    title: '确认上报国家局',
    content: `确认将「${row.hospitalName}」${row.reportYear}年第${row.reportBatch}期上报至国家局？`,
    async onOk() {
      const hideLoading = message.loading({ content: '上报中...', duration: 0 });
      try {
        await batchNationalReport([row.id]);
        message.success('上报成功');
        handleRefresh();
      } catch (error: any) {
        message.error(error?.message || '上报失败');
      } finally {
        hideLoading();
      }
    },
  });
}

/** 打开审批详情弹窗 */
function handleViewApprovalDetail(row: DeclareProgressReport) {
  approvalDetailRef.value?.openWithData({
    processInstanceId: row.hospitalProcessInstanceId,
    reportId: row.id,
    hospitalId: row.hospitalId,
    deptId: row.deptId,
    hospitalName: row.hospitalName,
    reportYear: row.reportYear!,
    reportBatch: row.reportBatch!,
    projectType: row.projectType,
    projectTypeName: row.projectTypeName,
    reportStatus: row.reportStatus,
  });
}

/** 快速执行 BPM 审批操作（从列表行直接触发：退回走 ReturnModal，其余走意见弹窗 + submitBpmAction） */
function handleQuickBpmAction(row: DeclareProgressReport, action: any) {
  // 退回按钮：bizStatus 为 PROVINCE_RETURNED，或按钮 key 为 "6"（后端定义）
  const isBack =
    action.bizStatus === 'PROVINCE_RETURNED' ||
    action.key === '6' ||
    (action.key as string)?.toLowerCase() === 'back';

  if (isBack) {
    returnModalRef.value?.open({ taskId: action.taskId, buttonId: action.key });
    return;
  }
  listBpmRow.value = row;
  listBpmCurrentAction.value = action;
  listBpmReason.value = '';
  // 非退回操作：一律打开意见弹窗（用户需要填写审批意见）
  listBpmModalVisible.value = true;
}

async function submitListBpmAction(reason: string) {
  const row = listBpmRow.value;
  const action = listBpmCurrentAction.value;
  if (!row?.id || !action) return;

  const vars = action.vars || {};
  if (vars.reasonRequired && !reason?.trim()) {
    message.warning('请输入审批意见');
    return;
  }

  listBpmLoading.value = true;
  try {
    await submitBpmAction({
      businessType: BUSINESS_TYPE_KEY,
      businessId: row.id,
      actionKey: String(action.key),
      reason,
      taskId: action.taskId,
    });
    message.success('操作成功');
    listBpmModalVisible.value = false;
    listBpmRow.value = null;
    listBpmCurrentAction.value = null;
    await handleRefresh();
  } catch (e: any) {
    message.error(e?.message || '操作失败');
  } finally {
    listBpmLoading.value = false;
  }
}

function handleListBpmModalOk() {
  void submitListBpmAction(listBpmReason.value);
}

/** 退回弹窗成功回调 */
function handleReturnSuccess() {
  message.success('退回成功');
  handleRefresh();
}

/** 判断填报状态是否可编辑 */
function canEditStatus(status: string): boolean {
  if (status === 'DRAFT' || status === 'SAVED') return true;
  return status.endsWith('RETURNED');
}

/** 判断填报状态是否可提交 */
function canSubmitStatus(status: string): boolean {
  return status === 'SAVED';
}

/** 获取状态颜色 */
function getStatusColor(status: string | number): string {
  const colors: Record<string, string> = {
    DRAFT: 'default',
    SAVED: 'processing',
    SUBMITTED: 'warning',
    HOSPITAL_AUDITING: 'warning',
    HOSPITAL_APPROVED: 'success',
    HOSPITAL_REJECTED: 'error',
    HOSPITAL_RETURNED: 'warning',
    PROVINCE_APPROVED: 'success',
    PROVINCE_REJECTED: 'error',
    PROVINCE_RETURNED: 'warning',
    // 省级审核数字状态
    0: 'default',   // 未提交
    1: 'warning',   // 审核中
    2: 'success',   // 已通过
    3: 'error',     // 已驳回
  };
  return colors[String(status)] || 'default';
}

/** 获取状态图标 */
function getStatusIcon(status: string | number): string {
  const icons: Record<string, string> = {
    DRAFT: 'lucide:file-edit',
    SAVED: 'lucide:save',
    SUBMITTED: 'lucide:send',
    HOSPITAL_AUDITING: 'lucide:search',
    HOSPITAL_APPROVED: 'lucide:check-circle',
    HOSPITAL_REJECTED: 'lucide:x-circle',
    HOSPITAL_RETURNED: 'lucide:corner-up-left',
    PROVINCE_APPROVED: 'lucide:check-circle',
    PROVINCE_REJECTED: 'lucide:x-circle',
    PROVINCE_RETURNED: 'lucide:corner-up-left',
    // 省级审核数字状态
    0: 'lucide:minus',          // 未提交
    1: 'lucide:clock',          // 审核中
    2: 'lucide:check-circle',  // 已通过
    3: 'lucide:x-circle',       // 已驳回
  };
  return icons[String(status)] || 'lucide:circle';
}

/** 获取填报状态名称 */
function getReportStatusName(status: string | number): string {
  const options = getDictOptions(DICT_TYPE.DECLARE_PROJECT_STATUS);
  const found = options.find((item) => String(item.value) === String(status));
  return found?.label || String(status);
}

/** 获取省级审核状态名称 */
function getProvinceStatusName(status: number | null | undefined): string {
  const names: Record<number, string> = {
    0: '未提交',
    1: '审核中',
    2: '已通过',
    3: '已驳回',
  };
  return names[Number(status)] || '未知';
}

/** 获取国家局上报状态名称 */
function getNationalReportStatusName(status: number | null | undefined): string {
  return Number(status) === 1 ? '已上报' : '未上报';
}

/** 根据行数据构建操作按钮列表 */
function getRowActions(row: DeclareProgressReport) {
  const actions = rowBpmActions.value[row.id] || [];
  // BPM 操作按钮：auth: [] 绕过 TableAction 权限校验（BPM 按钮不需要权限码）
  const actionButtons = actions.map((action) => ({
    ...action,
    type: 'link' as const,
    auth: [],
    onClick: () => handleQuickBpmAction(row, action),
  }));

  // 只有创建者才能看到编辑/删除按钮（其他人只能看到审批详情）
  const isCreator = String(row.creator) === String(currentUserId.value);
  const alwaysButtons = [
    {
      label: '查看详情',
      type: 'link' as const,
      icon: 'lucide:history',
      onClick: () => handleViewApprovalDetail(row),
    }
  ];

  
  if (isCreator && row.deptId === hospitalId.value && canSubmitStatus(row.reportStatus) && row.provinceStatus !== 3) {
    alwaysButtons.push({
      label: '提交',
      type: 'link' as const,
      icon: 'lucide:send',
      onClick: () => handleSubmit(row),
    });
  }

  // 省级通过且未上报：显示上报按钮
  if (row.provinceStatus === 2 && row.nationalReportStatus !== 1) {
    alwaysButtons.push({
      label: '上报国家局',
      type: 'link' as const,
      icon: 'lucide:upload',
      auth: ['declare:national-report:batch-report'],
      onClick: () => handleSingleReport(row),
    } as any);
  }
  if (isCreator && canEditStatus(row.reportStatus)) {
    alwaysButtons.push({
      label: '编辑',
      type: 'link' as const,
      icon: 'lucide:pencil',
      auth: ['declare:progress-report:update'],
      onClick: () => handleEdit(row),
    } as any);

    alwaysButtons.push({
      label: '删除',
      type: 'link' as const,
      danger: true,
      icon: 'lucide:trash-2',
      auth: ['declare:progress-report:update'],
      onClick: () => handleDelete(row),
    } as any);
  }

  return [...actionButtons, ...alwaysButtons];
}

/** 加载每行的 BPM 可用操作 */
async function loadRowBpmActions(rows: DeclareProgressReport[]) {
  const ids = rows.map((r) => r.id).filter(Boolean);
  if (!ids.length) return;
  try {
    const actions = await getAvailableActionsBatch(BUSINESS_TYPE_KEY, ids as number[], 'progress');
    const grouped: Record<number, any[]> = {};
    for (const action of actions) {
      // 兼容 businessId 可能被序列化为 undefined 的情况
      const bid = (action.businessId as number) || action.businessId?.toString?.();
      if (bid && !Number.isNaN(Number(bid))) {
        const key = Number(bid);
        if (!grouped[key]) grouped[key] = [];
        grouped[key].push(action);
      }
    }
    ids.forEach((id) => {
      rowBpmActions.value[id] = grouped[id] || [];
    });
  } catch {
    ids.forEach((id) => { rowBpmActions.value[id] = []; });
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: [
      {
        fieldName: 'reportYear',
        label: '填报年度',
        component: 'Select',
        componentProps: {
          placeholder: '请选择年度',
          allowClear: true,
          options: [
            { label: '2028', value: 2028 },
            { label: '2027', value: 2027 },
            { label: '2026', value: 2026 },
            { label: '2025', value: 2025 },
            { label: '2024', value: 2024 },
          ],
        },
      },
    ],
  },
  gridOptions: {
    columns: [
      { type: 'checkbox', width: 50 },
      {
        field: 'seq',
        title: '序号',
        width: 70,
        slots: { default: 'seq' },
      },
      { field: 'hospitalName', title: '医院名称', minWidth: 200, showOverflow: true },
      { field: 'reportYear', title: '填报年度', width: 100 },
      {
        field: 'reportBatch',
        title: '填报批次',
        width: 100,
        slots: { default: 'reportBatch' },
      },
      {
        field: 'reportStatus',
        title: '填报状态',
        width: 120,
        slots: { default: 'reportStatus' },
      },
      {
        field: 'provinceStatus',
        title: '省级审核',
        width: 120,
        slots: { default: 'provinceStatus' },
      },
      {
        field: 'nationalReportStatus',
        title: '国家局上报',
        width: 120,
        slots: { default: 'nationalReportStatus' },
      },
      {
        field: 'createTime',
        title: '创建时间',
        width: 180,
        formatter: 'formatDateTime',
      },
      {
        title: '操作',
        width: 300,
        fixed: 'right',
        slots: { default: 'actions' },
      },
    ],
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async () => {
          const list = await getHospitalReportList(hospitalId.value);
          if (list?.length) {
            await loadRowBpmActions(list);
          }
          return { list, total: list.length };
        },
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    checkboxConfig: {
      trigger: 'row',
      highlight: true,
      reserve: true,
      checkMethod: ({ row }: { row: DeclareProgressReport }) => {
        // 用 == 宽松比较，兼容 provinceStatus 返回值为字符串或数字的场景
        return true;   //Number(row.provinceStatus) === 2 && Number(row.nationalReportStatus) !== 1;
      },
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
    pagerConfig: {
      enabled: true,
      pageSize: 10,
    },
  },
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
} as VxeTableGridOptions<DeclareProgressReport> as any,
);
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <ApprovalDetail ref="approvalDetailRef" @success="handleRefresh" />
    <ReturnModal ref="returnModalRef" @success="handleReturnSuccess" />
    <DeclareCompareModal ref="compareModalRef" />

    <a-modal
      v-model:open="listBpmModalVisible"
      :title="listBpmCurrentAction?.label || '审批操作'"
      :confirm-loading="listBpmLoading"
      @ok="handleListBpmModalOk"
    >
      <a-form layout="vertical">
        <a-form-item
          :label="listBpmCurrentAction?.vars?.reason || '审批意见'"
          :required="listBpmCurrentAction?.vars?.reasonRequired"
        >
          <a-textarea
            v-model:value="listBpmReason"
            :placeholder="
              listBpmCurrentAction?.vars?.reasonRequired
                ? '请输入审批意见（必填）'
                : '请输入审批意见（可选）'
            "
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <Grid table-title="进度填报列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: canCreate ? '新建填报' : '暂无可用窗口',
              type: 'primary',
              icon: 'lucide:plus',
              auth: ['declare:progress-report:create'],
              disabled: !canCreate,
              tooltip: canCreate ? '' : '当前没有开放的填报时间窗口',
              onClick: handleCreate,
            },
            {
              label: '数据对比',
              auth: ['declare/progress-report/compare-data'],
              icon: 'lucide:git-compare',
              disabled: compareDisabled,
              tooltip: compareTooltip,
              onClick: handleOpenCompare,
            },
            {
              label: '批量上报国家局',
              icon: 'lucide:upload',
              auth: ['declare:national-report:batch-report'],
              onClick: handleBatchReport,
            },
          ]"
        />
      </template>

      <!-- 序号列 -->
      <template #seq="{ $rowIndex }">
        <span class="seq-cell">{{ $rowIndex + 1 }}</span>
      </template>

      <!-- 填报批次列 -->
      <template #reportBatch="{ row }">
        <span class="batch-badge">
          <IconifyIcon icon="lucide:layers" class="batch-icon" />
          第{{ row.reportBatch }}期
        </span>
      </template>

      <!-- 填报状态列 -->
      <template #reportStatus="{ row }">
        <a-tag
          :color="getStatusColor(row.reportStatus)"
          class="status-tag"
        >
          <IconifyIcon :icon="getStatusIcon(row.reportStatus)" class="tag-icon" />
          {{ getReportStatusName(row.reportStatus) }}
        </a-tag>
      </template>

      <!-- 省级审核列 -->
      <template #provinceStatus="{ row }">
        <a-tag
          :color="getStatusColor(row.provinceStatus)"
          class="status-tag"
        >
          <IconifyIcon :icon="getStatusIcon(row.provinceStatus)" class="tag-icon" />
          {{ getProvinceStatusName(row.provinceStatus) }}
        </a-tag>
      </template>

      <!-- 国家局上报列 -->
      <template #nationalReportStatus="{ row }">
        <a-tag
          :color="row.nationalReportStatus === 1 ? 'success' : 'default'"
          class="status-tag"
        >
          <IconifyIcon :icon="row.nationalReportStatus === 1 ? 'lucide:check-circle' : 'lucide:minus'" class="tag-icon" />
          {{ getNationalReportStatusName(row.nationalReportStatus) }}
        </a-tag>
      </template>

      <!-- 操作列 -->
      <template #actions="{ row }">
        <TableAction
          :actions="getRowActions(row)"
        />
      </template>
    </Grid>
  </Page>
</template>

<style scoped>
/* 序号 */
.seq-cell {
  color: hsl(var(--muted-foreground));
  font-size: 13px;
}

/* 批次徽章 */
.batch-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: hsl(var(--primary) / 0.08);
  color: hsl(var(--primary));
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
}

/* 状态标签 */
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border-radius: 6px;
  font-weight: 500;
}

.tag-icon {
  width: 12px;
  height: 12px;
}

.batch-icon {
  width: 12px;
  height: 12px;
}
</style>
