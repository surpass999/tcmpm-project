<script lang="ts" setup>
import type { DeclareProgressReport } from '#/api/declare/progress-report';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { computed, onActivated , nextTick, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

import { message, Modal, Tabs, TabPane } from 'ant-design-vue';
import { useUserStore } from '@vben/stores';

import {
  batchNationalReport,
  getProvinceReportListByDept,
} from '#/api/declare/progress-report';
import { getAvailableActionsBatch, submitBpmAction } from '#/api/bpm/action';
import { IconifyIcon } from '@vben/icons';
import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';

import ApprovalDetail from '../modules/approval-detail.vue';
import ReturnModal from '#/components/bpm/ReturnModal.vue';
import DeclareCompareModal from '../components/DeclareCompareModal.vue';

const BUSINESS_TYPE_KEY = 'progress_report:submit';

const userStore = useUserStore();
const deptId = computed(() => Number(userStore.userInfo?.deptId) || 0);
const currentUserId = computed(() => {
  return Number(userStore.userInfo?.userId) || userStore.userInfo?.id;
});

/** 审批详情组件 ref */
const approvalDetailRef = ref<InstanceType<typeof ApprovalDetail> | null>(null);

/** 退回弹窗组件 ref */
const returnModalRef = ref<InstanceType<typeof ReturnModal> | null>(null);

/** 对比弹窗组件 ref */
const compareModalRef = ref<InstanceType<typeof DeclareCompareModal> | null>(null);

/** 列表已选记录（用于数据对比） */
const selectedRows = ref<DeclareProgressReport[]>([]);


/** 每行记录的 BPM 可用操作 */
const rowBpmActions = ref<Record<number, any[]>>({});

/** 列表行 BPM 操作：意见弹窗 */
const listBpmModalVisible = ref(false);
const listBpmLoading = ref(false);
const listBpmRow = ref<DeclareProgressReport | null>(null);
const listBpmCurrentAction = ref<any>(null);
const listBpmReason = ref('');


onActivated(() => {
  console.log('onActivated');
  handleRefresh(); // 重新加载列表 + 窗口状态
});

async function handleRefresh() {
  await nextTick();
  await nextTick();
  gridApi.query?.();
}

const compareDisabled = computed(() => {
  if (selectedRows.value.length !== 2) return true;
  const types = new Set(selectedRows.value.map((r) => r.projectType));
  return types.size > 1;
});

const compareTooltip = computed(() => {
  if (selectedRows.value.length < 2) return '请先选择2条申报记录';
  if (selectedRows.value.length > 2) return '只能选择2条记录进行对比';
  if (selectedRows.value[0]?.projectType !== selectedRows.value[1]?.projectType)
    return '所选记录项目类型不同，无法对比';
  return '';
});

function handleOpenCompare() {
  const [a, b] = selectedRows.value;
  if (a && b) {
    compareModalRef.value?.open(a.id, b.id);
  }
}

function handleRowCheckboxChange(params: { records: DeclareProgressReport[] }) {
  selectedRows.value = [...params.records];
}

async function handleBatchReport() {
  const selectedRows = (gridApi.grid?.getCheckboxRecords() || []) as DeclareProgressReport[];
  const reportableRows = selectedRows.filter(
    (row) => row.provinceStatus === 2 && row.nationalReportStatus !== 2,
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

function handleQuickBpmAction(row: DeclareProgressReport, action: any) {
  const isBack =
    action.bizStatus === 'PROVINCE_RETURNED'
    || action.key === '6'
    || (action.key as string)?.toLowerCase() === 'back';

  if (isBack) {
    returnModalRef.value?.open({ taskId: action.taskId, buttonId: action.key });
    return;
  }
  listBpmRow.value = row;
  listBpmCurrentAction.value = action;
  listBpmReason.value = '';
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

function handleReturnSuccess() {
  message.success('退回成功');
  handleRefresh();
}

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
    0: 'default',
    1: 'warning',
    2: 'success',
    3: 'error',
  };
  return colors[String(status)] || 'default';
}

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
    0: 'lucide:minus',
    1: 'lucide:clock',
    2: 'lucide:check-circle',
    3: 'lucide:x-circle',
  };
  return icons[String(status)] || 'lucide:circle';
}

function getReportStatusName(status: string | number): string {
  const options = getDictOptions(DICT_TYPE.DECLARE_PROJECT_STATUS);
  const found = options.find((item) => String(item.value) === String(status));
  return found?.label || String(status);
}

function getProvinceStatusName(status: number | null | undefined): string {
  const names: Record<number, string> = {
    0: '未提交',
    1: '审核中',
    2: '已通过',
    3: '已驳回',
  };
  return names[Number(status)] || '未知';
}

function getNationalReportStatusName(status: number | null | undefined): string {
  if (Number(status) === 1) return '国家局审批中';
  if (Number(status) === 2) return '已上报';
  return '未上报';
}

function getRowActions(row: DeclareProgressReport) {
  const buttons: any[] = [];

  const actions = rowBpmActions.value[row.id] || [];
  for (const action of actions) {
    buttons.push({
      ...action,
      type: 'link' as const,
      auth: [],
      onClick: () => handleQuickBpmAction(row, action),
    });
  }

  buttons.push({
    label: '查看详情',
    type: 'link' as const,
    icon: 'lucide:history',
    onClick: () => handleViewApprovalDetail(row),
  });

  // 【注释】医院提交后直接到国家局，不再需要省局上报
  // if (row.provinceStatus === 2 && row.nationalReportStatus !== 1) {
  //   buttons.push({
  //     label: '上报国家局',
  //     type: 'link' as const,
  //     icon: 'lucide:upload',
  //     auth: ['declare:national-report:batch-report'],
  //     onClick: () => handleSingleReport(row),
  //   });
  // }

  return buttons;
}

async function loadRowBpmActions(rows: DeclareProgressReport[]) {
  const ids = rows.map((r) => r.id).filter(Boolean);
  if (!ids.length) return;
  try {
    const actions = await getAvailableActionsBatch(
      BUSINESS_TYPE_KEY,
      ids as number[],
      'progress',
    );
    const grouped: Record<number, any[]> = {};
    for (const action of actions) {
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
      // 【注释】医院提交后直接到国家局，不再需要省局审核
      // {
      //   field: 'provinceStatus',
      //   title: '省级审核',
      //   width: 120,
      //   slots: { default: 'provinceStatus' },
      // },
      {
        field: 'nationalReportStatus',
        title: '国家局上报',
        width: 120,
        slots: { default: 'nationalReportStatus' },
      },
      {
        field: 'reportUserName',
        title: '医院填报人',
        width: 100,
      },
      {
        field: 'auditUserName',
        title: '医院审核人',
        width: 100,
      },
      {
        field: 'createTime',
        title: '创建时间',
        width: 180,
        formatter: 'formatDateTime',
      },
      {
        title: '操作',
        width: 280,
        fixed: 'right',
        slots: { default: 'actions' },
      },
    ],
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async (_params: any, form: any) => {
          const allList = await getProvinceReportListByDept(form?.reportYear);
          const list = allList || [];
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

    <!-- <Tabs v-model:activeKey="activeKey" @change="onTabChange" class="mb-4">
      <TabPane key="all" tab="全部记录" />
      <TabPane key="pending" tab="待审核" />
    </Tabs> -->

    <Grid table-title="进度填报列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '数据对比',
              icon: 'lucide:git-compare',
              auth: ['declare/progress-report/compare-data'],
              disabled: compareDisabled,
              tooltip: compareTooltip,
              onClick: handleOpenCompare,
            },
            // 【注释】医院提交后直接到国家局，不再需要省局上报
            // {
            //   label: '批量上报国家局',
            //   icon: 'lucide:upload',
            //   auth: ['declare:national-report:batch-report'],
            //   onClick: handleBatchReport,
            // },
          ]"
        />
      </template>

      <template #seq="{ $rowIndex }">
        <span class="seq-cell">{{ $rowIndex + 1 }}</span>
      </template>

      <template #reportBatch="{ row }">
        <span class="batch-badge">
          <IconifyIcon icon="lucide:layers" class="batch-icon" />
          第{{ row.reportBatch }}期
        </span>
      </template>

      <template #reportStatus="{ row }">
        <a-tag :color="getStatusColor(row.reportStatus)" class="status-tag">
          <IconifyIcon :icon="getStatusIcon(row.reportStatus)" class="tag-icon" />
          {{ getReportStatusName(row.reportStatus) }}
        </a-tag>
      </template>

      // 【注释】医院提交后直接到国家局，不再需要省局审核
      <!-- <template #provinceStatus="{ row }">
        <a-tag :color="getStatusColor(row.provinceStatus)" class="status-tag">
          <IconifyIcon :icon="getStatusIcon(row.provinceStatus)" class="tag-icon" />
          {{ getProvinceStatusName(row.provinceStatus) }}
        </a-tag>
      </template> -->

      <template #nationalReportStatus="{ row }">
        <a-tag
          :color="row.nationalReportStatus === 2 ? 'success' : 'default'"
          class="status-tag"
        >
          <IconifyIcon
            :icon="row.nationalReportStatus === 2 ? 'lucide:check-circle' : 'lucide:minus'"
            class="tag-icon"
          />
          {{ getNationalReportStatusName(row.nationalReportStatus) }}
        </a-tag>
      </template>

      <template #actions="{ row }">
        <TableAction :actions="getRowActions(row)" />
      </template>
    </Grid>
  </Page>
</template>

<style scoped>
.seq-cell {
  color: hsl(var(--muted-foreground));
  font-size: 13px;
}

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

:deep(.ant-tabs-content) {
  height: 100%;
}

:deep(.ant-tabs-tabpane) {
  height: 100%;
  overflow-y: auto;
}
</style>
