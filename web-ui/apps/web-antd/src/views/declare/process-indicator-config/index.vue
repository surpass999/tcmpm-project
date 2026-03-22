<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ProcessIndicatorConfigApi } from '#/api/declare/process-indicator-config';

import { computed, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';

import { $t } from '#/locales';
import {
  deleteProcessIndicatorConfig,
  getProcessIndicatorConfigPage,
} from '#/api/declare/process-indicator-config';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

// 表格数据引用
const tableData = ref<ProcessIndicatorConfigApi.ConfigResp[]>([]);

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 创建配置 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑配置 */
function handleEdit(row: ProcessIndicatorConfigApi.ConfigResp) {
  formModalApi.setData(row).open();
}

/** 删除配置 */
async function handleDelete(row: ProcessIndicatorConfigApi.ConfigResp) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.indicatorName]),
    duration: 0,
  });
  try {
    await deleteProcessIndicatorConfig(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.indicatorName]));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

// 表格列定义
const gridColumns = useGridColumns();

// 计算当前页总分
const totalMaxScore = computed(() => {
  if (!tableData.value || tableData.value.length === 0) {
    return 0;
  }
  return tableData.value.reduce((sum, item) => {
    return sum + (Number(item.maxScore) || 0);
  }, 0);
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    keepSource: true,
    pagerConfig: {
      slots: {
        home: 'pager-center',
      },
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const res = await getProcessIndicatorConfigPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
          // 分页接口返回 { list, total }，同步到 tableData 供底部统计使用
          const list = (res as any)?.list ?? (Array.isArray(res) ? res : []);
          tableData.value = list;
          return res;
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
  } as VxeTableGridOptions<ProcessIndicatorConfigApi.ConfigResp>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="过程指标配置">
      <template #maxScoreDefault="{ row }">
        <span>{{ row.maxScore ? `${row.maxScore}分` : '-' }}</span>
      </template>
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['配置']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['declare:process-indicator-config:create'],
              onClick: handleCreate,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: $t('common.edit'),
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['declare:process-indicator-config:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['declare:process-indicator-config:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.indicatorName]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
      <!-- 使用 pager 左侧插槽在分页行左侧插入本页总分 -->
      <template #pager-center>
        <div class="pager-left-info">
          本页总分：<span class="total-score">{{ totalMaxScore }}分</span>
        </div>
      </template>
    </Grid>
  </Page>
</template>

<style scoped>
.pager-left-info {
  margin: 0 auto;
  margin-right: 20px;
  align-items: center;
  font-weight: 500;
  color: #666;
  height: 16px;
  line-height: 16px;
}

.total-score {
  font-weight: 700;
  color: #1677ff;
  font-size: 16px;
  margin-left: 4px;
}
</style>
