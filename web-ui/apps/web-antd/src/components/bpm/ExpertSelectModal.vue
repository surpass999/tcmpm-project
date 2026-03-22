<script lang="ts" setup>
import type { DeclareExpertApi } from '#/api/declare/expert';

import { ref, shallowRef, watch, computed } from 'vue';

import { Modal } from 'ant-design-vue';

import { getDictLabel } from '@vben/hooks';

import { message } from 'ant-design-vue';

import { getExpertSelectList } from '#/api/declare/expert';
import { DICT_TYPE } from '@vben/constants';

interface Props {
  // 是否显示弹窗
  open?: boolean;
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
  // 流程中已选择的专家数量（来自后端记录）
  processSelectedCount?: number;
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
  'update:open': [open: boolean];
  'confirm': [experts: DeclareExpertApi.Expert[]];
}>();

// 弹窗显示状态 - 优先使用外部传入的 open，如果没有则使用内部状态
const internalModalVisible = ref(false);

// 已选中的专家列表
const selectedExperts = ref<DeclareExpertApi.Expert[]>([]);

// 流程中已选择的专家数量
const processSelectedCount = ref(0);

// 当前页码
const currentPage = ref(1);

// 搜索关键字
const searchKeyword = ref('');

// 加载状态
const loading = ref(false);

// 专家列表
const expertList = ref<DeclareExpertApi.Expert[]>([]);

// 总数
const total = ref(0);

// 每页数量
const pageSize = ref(10);

const modalVisible = computed({
  get: () => (props.open !== undefined ? !!props.open : internalModalVisible.value),
  set: (val) => {
    if (props.open !== undefined) {
      emit('update:open', val);
    } else {
      internalModalVisible.value = val;
    }
  },
});

// 监听外部 open 变化，同步到内部状态
watch(() => props.open, (val) => {
  if (props.open !== undefined) {
    // 如果外部传入了 open 属性，监听变化
    // 首次打开时重置状态
    if (val) {
      resetModalState();
    }
  }
});

// 监听内部状态变化，emit 到外部（仅当没有外部 open 时）
watch(internalModalVisible, (val) => {
  if (props.open === undefined && !val) {
    emit('update:open', false);
  }
});

// 重置弹窗状态
function resetModalState() {
  selectedExperts.value = [];
  // 从 props 获取流程中已选择的专家数量
  processSelectedCount.value = props.processSelectedCount ?? 0;
  currentPage.value = 1;
  searchKeyword.value = '';
}

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

// 监听弹窗打开，重置状态并加载数据
watch(modalVisible, (val) => {
  if (val) {
    resetModalState();
    loadExpertList();
  }
});

// 处理弹窗关闭
function handleClose() {
  modalVisible.value = false;
}

// 打开弹窗
function openModal() {
  modalVisible.value = true;
}

// 关闭弹窗（别名）
function closeModal() {
  modalVisible.value = false;
}

// 处理确认
function handleConfirm() {
  const selectedCount = selectedExperts.value.length;

  // 验证专家数量限制
  if (props.expertMin !== undefined && selectedCount < props.expertMin) {
    message.warning(`请至少选择 ${props.expertMin} 位专家，当前已选 ${selectedCount} 位`);
    return;
  }
  if (props.expertMax !== undefined && selectedCount > props.expertMax) {
    message.warning(`最多只能选择 ${props.expertMax} 位专家，当前已选 ${selectedCount} 位`);
    return;
  }

  if (selectedCount === 0 && processSelectedCount.value === 0) {
    message.warning('请至少选择一个专家');
    return;
  }

  emit('confirm', selectedExperts.value);
  // 延迟关闭弹窗，确保事件先被处理
  setTimeout(() => {
    handleClose();
  }, 100);
}

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

// 暴露方法供外部调用
defineExpose({
  open: openModal,
  close: closeModal,
});
</script>

<template>
  <Modal
    v-model:open="modalVisible"
    :title="multiple ? '选择专家' : '选择专家（单选）'"
    width="60%"
    height="900"
    class="expert-modal"
  >
    <!-- 选择提示信息 -->
    <div class="mb-4 flex items-center gap-4 text-sm">
      <span v-if="selectedExperts.length > 0" class="text-gray-600">
        已选择 <span class="font-medium text-primary">{{ selectedExperts.length }}</span> 位专家
        <span v-if="expertMin !== undefined || expertMax !== undefined" class="ml-2 text-orange-500">
          （审批专家数
          <template v-if="expertMin !== undefined && expertMax !== undefined">{{ expertMin }}-{{ expertMax }} 人</template>
          <template v-else-if="expertMin !== undefined">{{ expertMin }}人起</template>
          <template v-else-if="expertMax !== undefined">最多{{ expertMax }}人</template>
          ）
        </span>
      </span>
      <span v-else-if="processSelectedCount > 0" class="text-gray-500">
        流程中已选择 <span class="font-medium">{{ processSelectedCount }}</span> 位专家
        <span v-if="expertMin !== undefined || expertMax !== undefined" class="ml-2 text-orange-500">
          （审批专家数
          <template v-if="expertMin !== undefined && expertMax !== undefined">{{ expertMin }}-{{ expertMax }}人</template>
          <template v-else-if="expertMin !== undefined">{{ expertMin }}人起</template>
          <template v-else-if="expertMax !== undefined">最多{{ expertMax }}人</template>
          ）
        </span>
      </span>
      <span v-else-if="expertMin !== undefined || expertMax !== undefined" class="text-gray-500">
        审批专家数
        <span class="font-medium text-orange-500">
          <template v-if="expertMin !== undefined && expertMax !== undefined">{{ expertMin }}-{{ expertMax }} 人</template>
          <template v-else-if="expertMin !== undefined">{{ expertMin }}人起</template>
          <template v-else-if="expertMax !== undefined">最多{{ expertMax }}人</template>
        </span>
      </span>
    </div>
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
          onChange: (_: string[], selectedRows: DeclareExpertApi.Expert[]) =>
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
    <template #footer>
      <a-button @click="handleClose">取消</a-button>
      <a-button type="primary" @click="handleConfirm">确定</a-button>
    </template>
  </Modal>
</template>

<style scoped>
.expert-select-container {
  padding: 16px;
}
</style>
