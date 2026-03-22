<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { Training } from '#/api/declare/training';

import { ref } from 'vue';

import { Page, useVbenModal, confirm } from '@vben/common-ui';

import { message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  remove as deleteTraining,
  publish,
  unpublish,
  getPage,
} from '#/api/declare/training';

import TrainingDetailModal from './modules/detail.vue';
import TrainingFormModal from './modules/form.vue';

import {
  getTrainingStatusColor,
  getTrainingStatusLabel,
  useGridColumns,
  useGridFormSchema,
} from './data';

const checkedIds = ref<number[]>([]);

// 详情弹窗
const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: TrainingDetailModal,
  destroyOnClose: true,
});

// 表单弹窗
const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: TrainingFormModal,
  destroyOnClose: true,
});

/** 处理复选框变化 */
function handleRowCheckboxChange({ records }: { records: Training[] }) {
  checkedIds.value = records.map((item) => item.id!);
}

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 新增活动 */
function handleAdd() {
  formModalApi.setData(null).open();
}

/** 编辑活动 */
function handleEdit(row: Training) {
  formModalApi.setData(row).open();
}

/** 查看详情 */
function handleDetail(row: Training) {
  detailModalApi.setData(row).open();
}

/** 删除活动 */
async function handleDelete(row: Training) {
  try {
    await confirm('确定要删除该活动吗？');
    await deleteTraining(row.id!);
    message.success('删除成功');
    handleRefresh();
  } catch {
    // 用户取消不处理
  }
}

/** 发布活动 */
async function handlePublish(row: Training) {
  try {
    await confirm('确定要发布该活动吗？');
    await publish(row.id!);
    message.success('发布成功');
    handleRefresh();
  } catch {
    // 用户取消不处理
  }
}

/** 取消发布活动 */
async function handleUnpublish(row: Training) {
  try {
    await confirm('确定要取消发布该活动吗？');
    await unpublish(row.id!);
    message.success('取消发布成功');
    handleRefresh();
  } catch {
    // 用户取消不处理
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
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
      zoom: true,
    },
  } as VxeTableGridOptions<Training>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <DetailModal @success="handleRefresh" />
    <FormModal @success="handleRefresh" />
    <Grid table-title="培训活动管理">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              onClick: handleAdd,
            },
          ]"
        />
      </template>

      <template #name_default="{ row }">
        <div class="flex items-center gap-2 flex-wrap">
          <a-tag v-if="row.typeLabel" class="m-0">
            {{ row.typeLabel }}
          </a-tag>
          <span class="truncate max-w-[300px]">{{ row.name }}</span>
        </div>
      </template>

      <template #statusLabel_default="{ row }">
        <a-tag :color="getTrainingStatusColor(row.status)">
          {{ getTrainingStatusLabel(row.status) }}
        </a-tag>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '查看',
              type: 'link',
              icon: ACTION_ICON.VIEW,
              onClick: handleDetail.bind(null, row),
            },
            {
              label: '编辑',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              onClick: handleEdit.bind(null, row),
            },
            {
              label: '发布',
              type: 'link',
              ifShow: row.status !== 2,
              icon: ACTION_ICON.ADD,
              onClick: handlePublish.bind(null, row),
            },
            {
              label: '取消发布',
              type: 'link',
              ifShow: row.status === 2,
              onClick: handleUnpublish.bind(null, row),
            },
            {
              label: '删除',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              onClick: handleDelete.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
