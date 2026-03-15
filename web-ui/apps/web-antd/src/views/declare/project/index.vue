<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareProjectApi } from '#/api/declare/project';

import { ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';

import { message, Progress } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteProject,
  deleteProjectList,
  exportProject,
  getProjectPage,
} from '#/api/declare/project';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import ProjectDetailModal from './detail/ProjectDetailModal.vue';

// 选中的项目ID列表
const checkedIds = ref<number[]>([]);

// 详情弹窗
const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: ProjectDetailModal,
  destroyOnClose: true,
});

/** 处理复选框变化 */
function handleRowCheckboxChange({
  records,
}: {
  records: DeclareProjectApi.Project[];
}) {
  checkedIds.value = records.map((item) => item.id!);
}

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 批量删除项目 */
async function handleDeleteBatch() {
  if (isEmpty(checkedIds.value)) {
    message.warning('请选择要删除的项目');
    return;
  }

  try {
    await import('@vben/common-ui').then(({ confirm }) =>
      confirm(`确定要删除选中的 ${checkedIds.value.length} 个项目吗？`),
    );

    const hideLoading = message.loading({
      content: '批量删除中...',
      duration: 0,
    });

    try {
      await deleteProjectList(checkedIds.value);
      checkedIds.value = [];
      message.success('批量删除成功');
      handleRefresh();
    } finally {
      hideLoading();
    }
  } catch (e) {
    // 用户取消不处理
  }
}

/** 导出项目数据 */
async function handleExport() {
  const data = await exportProject(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '项目列表.xls', source: data });
}

// 项目状态映射到颜色
const statusColors: Record<number, string> = {
  0: 'blue',   // 立项审批中
  1: 'orange', // 建设中
  2: 'purple', // 中期评估中
  3: 'red',    // 整改中
  4: 'cyan',   // 验收中
  5: 'green',  // 已验收
  6: 'default', // 已终止
};

// 进度条颜色
const progressColors: Record<string, string> = {
  low: 'exception',
  normal: 'normal',
  high: 'success',
};

// 获取进度状态
function getProgressStatus(value: number): string {
  if (value < 30) return 'exception';
  if (value < 70) return 'normal';
  return 'success';
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  showSearchForm: false,
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getProjectPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
          return result;
        },
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<DeclareProjectApi.Project>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="项目列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.export'),
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['declare:project:export'],
              onClick: handleExport,
            },
            {
              label: $t('ui.actionTitle.deleteBatch'),
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['declare:project:delete'],
              disabled: isEmpty(checkedIds),
              onClick: handleDeleteBatch,
            },
          ]"
        />
      </template>

      <!-- 进度条插槽 -->
      <template #progress="{ row }">
        <Progress
          :percent="row.actualProgress || 0"
          :status="getProgressStatus(row.actualProgress || 0)"
          :stroke-color="
            row.actualProgress && row.actualProgress >= 70 ? '#52c41a' : '#1890ff'
          "
          :show-info="false"
          size="small"
          style="width: 100%"
        />
      </template>

      <!-- 操作列插槽 -->
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '详情',
              type: 'link',
              auth: ['declare:project:detail'],
              onClick: () => {
                detailModalApi.setData({ 
                  projectId: row.id,
                  class: 'min-w-[800px]'
                }).open();
              },
            },
            {
              label: $t('common.edit'),
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['declare:project:update'],
              onClick: () => {
                // TODO: 编辑项目
                console.log('编辑项目', row.id);
              },
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['declare:project:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.id]),
                confirm: async () => {
                  try {
                    await deleteProject(row.id!);
                    message.success($t('ui.actionMessage.deleteSuccess', [row.id]));
                    handleRefresh();
                  } catch (e) {
                    console.error('删除失败', e);
                  }
                },
              },
            },
          ]"
        />
      </template>
    </Grid>

    <!-- 项目详情弹窗 -->
    <DetailModal />
  </Page>
</template>

<style lang="scss" scoped>
/* 自定义样式 */
</style>
