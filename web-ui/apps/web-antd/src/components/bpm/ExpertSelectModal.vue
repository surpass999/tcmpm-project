<script lang="ts" setup>
import type { DeclareExpertApi } from '#/api/declare/expert';

import { ref, shallowRef } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { getDictLabel } from '@vben/hooks';

import { message } from 'ant-design-vue';

import { getExpertSelectList } from '#/api/declare/expert';
import { DICT_TYPE } from '@vben/constants';

interface Props {
  // 是否多选
  multiple?: boolean;
  // 已选中的专家ID列表
  selectedIds?: number[];
  // 专家类型筛选
  expertType?: number;
  // 当前部门ID（用于判断回避）
  currentDeptId?: number;
  // 最少选择专家数量
  expertMin?: number;
  // 最多选择专家数量
  expertMax?: number;
}

const props = withDefaults(defineProps<Props>(), {
  multiple: true,
  selectedIds: () => [],
  expertType: undefined,
  currentDeptId: undefined,
  expertMin: undefined,
  expertMax: undefined,
});

const emit = defineEmits<{
  confirm: [experts: DeclareExpertApi.Expert[]];
}>();

// 表格列定义
const columns = [
  {
    title: '专家姓名',
    key: 'expertName',
    dataIndex: 'expertName',
    width: 120,
  },
  {
    title: '专家类型',
    key: 'expertType',
    dataIndex: 'expertType',
    width: 100,
  },
  {
    title: '工作单位',
    key: 'workUnit',
    dataIndex: 'workUnit',
    width: 180,
  },
  {
    title: '联系电话',
    key: 'phone',
    dataIndex: 'phone',
    width: 120,
  },
  {
    title: '回避',
    key: 'isAvoid',
    dataIndex: 'isAvoid',
    width: 80,
  },
  {
    title: '状态',
    key: 'status',
    dataIndex: 'status',
    width: 80,
  },
];

// 加载状态
const loading = ref(false);
// 专家列表数据
const expertList = shallowRef<DeclareExpertApi.Expert[]>([]);
// 已选中的专家
const selectedExperts = ref<DeclareExpertApi.Expert[]>([]);
// 当前页码
const currentPage = ref(1);
// 每页条数
const pageSize = ref(10);
// 总数
const total = ref(0);
// 搜索关键词
const searchKeyword = ref('');

// 加载专家列表
async function loadExpertList() {
  loading.value = true;
  try {
    const result = await getExpertSelectList({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      expertName: searchKeyword.value || undefined,
      expertType: props.expertType,
      currentDeptId: props.currentDeptId,
    });
    expertList.value = result.list || [];
    total.value = result.total || 0;

    // 如果有已选择的ID列表，选中对应的专家
    if (props.selectedIds && props.selectedIds.length > 0) {
      const selected = expertList.value.filter((e) =>
        props.selectedIds!.includes(e.id!),
      );
      selectedExperts.value = [...selected];
    }
  } catch (error) {
    console.error('加载专家列表失败:', error);
    expertList.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

// 处理选择变化
function handleSelectionChange(selected: DeclareExpertApi.Expert[]) {
  selectedExperts.value = selected;
}

// 分页变化
function handlePageChange(page: number, size: number) {
  currentPage.value = page;
  pageSize.value = size;
  loadExpertList();
}

// 搜索
function handleSearch() {
  currentPage.value = 1;
  loadExpertList();
}

// 使用 Vben Modal
const [Modal, modalApi] = useVbenModal({
  onOpenChange: async (isOpen: boolean) => {
    if (!isOpen) {
      return;
    }
    // 重置状态
    selectedExperts.value = [];
    processSelectedCount.value = 0;
    currentPage.value = 1;
    searchKeyword.value = '';

    // 获取传递的参数（专家数量限制和流程中已选择的数量）
    const data = modalApi.getData<{
      expertMin?: number;
      expertMax?: number;
      processSelectedCount?: number;
    }>();
    // 设置专家数量限制
    expertMin.value = data?.expertMin;
    expertMax.value = data?.expertMax;

    // 设置流程中已选择的专家数量（即使为0也要设置）
    processSelectedCount.value = data?.processSelectedCount || 0;

    await loadExpertList();
  },
  onConfirm: async () => {
    // 本次操作中，在弹窗内选择的专家数量
    const selectedCount = selectedExperts.value.length;

    // 验证专家数量限制
    if (expertMin.value !== undefined && selectedCount < expertMin.value) {
      message.warning(`请至少选择 ${expertMin.value} 位专家，当前已选 ${selectedCount} 位`);
      return false;
    }
    if (expertMax.value !== undefined && selectedCount > expertMax.value) {
      message.warning(`最多只能选择 ${expertMax.value} 位专家，当前已选 ${selectedCount} 位`);
      return false;
    }

    if (selectedCount === 0 && processSelectedCount.value === 0) {
      message.warning('请至少选择一个专家');
      return false;
    }
    emit('confirm', selectedExperts.value);
    return true;
  },
});

// 获取状态标签
function getStatusLabel(status: number) {
  return getDictLabel(DICT_TYPE.DECLARE_EXPERT_STATUS, status) || '未知';
}

// 获取专家类型标签
function getExpertTypeLabel(expertType: number) {
  return getDictLabel(DICT_TYPE.DECLARE_EXPERT_TYPE, expertType) || '未知';
}

// 获取行唯一键
function getRowKey(record: DeclareExpertApi.Expert) {
  return record.id!;
}

// 流程中已选择的专家数量（来自后端记录）
const processSelectedCount = ref(0);
// 专家数量限制
const expertMin = ref<number | undefined>(undefined);
const expertMax = ref<number | undefined>(undefined);

// 暴露打开弹窗的方法
defineExpose({
  open: (data?: { expertMin?: number; expertMax?: number; processSelectedCount?: number }) => {
    // 先设置数据，再打开弹窗
    if (data) {
      modalApi.setData(data);
    }
    modalApi.open();
  },
  close: () => modalApi.close(),
});
</script>

<template>
  <Modal
    :title="multiple ? '选择专家' : '选择专家（单选）'"
    :class="['w-[900px]', 'h-[600px]']"
  >
    <template #prepend-footer>
      <span v-if="selectedExperts.length > 0" class="mr-4 text-sm text-gray-600">
        已选择 <span class="font-medium text-primary">{{ selectedExperts.length }}</span> 位专家
        <span v-if="expertMin !== undefined || expertMax !== undefined" class="ml-2 text-orange-500">
          （审批专家数
          <template v-if="expertMin !== undefined && expertMax !== undefined">{{ expertMin }}-{{ expertMax }} 人</template>
          <template v-else-if="expertMin !== undefined">{{ expertMin }}人起</template>
          <template v-else-if="expertMax !== undefined">最多{{ expertMax }}人</template>
          ）
        </span>
      </span>
      <span v-else-if="processSelectedCount > 0" class="mr-4 text-sm text-gray-500">
        流程中已选择 <span class="font-medium">{{ processSelectedCount }}</span> 位专家
        <span v-if="expertMin !== undefined || expertMax !== undefined" class="ml-2 text-orange-500">
          （审批专家数
          <template v-if="expertMin !== undefined && expertMax !== undefined">{{ expertMin }}-{{ expertMax }}人</template>
          <template v-else-if="expertMin !== undefined">{{ expertMin }}人起</template>
          <template v-else-if="expertMax !== undefined">最多{{ expertMax }}人</template>
          ）
        </span>
      </span>
      <span v-else-if="expertMin !== undefined || expertMax !== undefined" class="mr-4 text-sm text-gray-500">
        审批专家数
        <span class="font-medium text-orange-500">
          <template v-if="expertMin !== undefined && expertMax !== undefined">{{ expertMin }}-{{ expertMax }} 人</template>
          <template v-else-if="expertMin !== undefined">{{ expertMin }}人起</template>
          <template v-else-if="expertMax !== undefined">最多{{ expertMax }}人</template>
        </span>
      </span>
    </template>
    <div class="expert-select-container">
      <!-- 搜索栏 -->
      <div class="mb-4 flex gap-2">
        <a-input
          v-model:value="searchKeyword"
          placeholder="请输入专家姓名搜索"
          class="w-48"
          @press-enter="handleSearch"
        />
        <a-button type="primary" :loading="loading" @click="handleSearch">
          搜索
        </a-button>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="expertList"
        :loading="loading"
        :row-key="getRowKey"
        :row-selection="{
          selectedRowKeys: selectedExperts.map((e) => e.id!),
          onChange: (_, selectedRows) =>
            handleSelectionChange(multiple ? selectedRows : selectedRows.slice(0, 1)),
        }"
        :pagination="{
          current: currentPage,
          pageSize: pageSize,
          total: total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total: number) => `共 ${total} 条`,
          onChange: handlePageChange,
        }"
        :scroll="{ y: 400 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'expertName'">
            <div class="font-medium">{{ record.expertName }}</div>
          </template>
          <template v-else-if="column.key === 'expertType'">
            <a-tag :color="record.expertType === 1 ? 'blue' : record.expertType === 2 ? 'green' : 'orange'">
              {{ getExpertTypeLabel(record.expertType) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'workUnit'">
            {{ record.workUnit || '-' }}
          </template>
          <template v-else-if="column.key === 'phone'">
            {{ record.phone || '-' }}
          </template>
          <template v-else-if="column.key === 'isAvoid'">
            <a-tag v-if="record.isAvoid" color="red">回避</a-tag>
            <span v-else class="text-gray-400">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'orange'">
              {{ getStatusLabel(record.status) }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </div>
  </Modal>
</template>

<style scoped>
.expert-select-container {
  padding: 16px;
}
</style>
