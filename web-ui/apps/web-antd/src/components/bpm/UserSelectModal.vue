<script lang="ts" setup>
import { ref, shallowRef, watch, onMounted } from 'vue';

import { message } from 'ant-design-vue';

import { getSimpleUserList } from '#/api/system/user';

interface User {
  id: number;
  username: string;
  nickname: string;
  deptId?: number;
  deptName?: string;
}

interface Props {
  // 是否多选
  multiple?: boolean;
  // 标题
  title?: string;
  // 已选中的用户ID列表
  selectedIds?: number[];
  // 是否显示原因输入框
  showReason?: boolean;
  // 原因输入框标签
  reasonLabel?: string;
  // 原因是否必填
  reasonRequired?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  multiple: false,
  title: '选择用户',
  selectedIds: () => [],
  showReason: false,
  reasonLabel: '操作原因',
  reasonRequired: false,
});

const emit = defineEmits<{
  'update:open': [open: boolean];
  confirm: [users: User[], reason: string];
}>();

// 弹窗显示状态
const open = ref(false);

// 加载状态
const loading = ref(false);
// 用户列表数据
const userList = shallowRef<User[]>([]);
// 已选中的用户
const selectedUsers = ref<User[]>([]);
// 原因输入
const reason = ref('');

// 监听 open 变化，重置状态
watch(
  () => open.value,
  (isOpen) => {
    if (isOpen) {
      // 打开时重置状态并加载数据
      selectedUsers.value = [];
      reason.value = '';
      loadUserList();
    }
  },
);

// 加载用户列表
async function loadUserList() {
  loading.value = true;
  try {
    const result = await getSimpleUserList();
    userList.value = result || [];

    // 如果有已选择的ID列表，选中对应的用户
    if (props.selectedIds && props.selectedIds.length > 0) {
      const selected = userList.value.filter((u) =>
        props.selectedIds!.includes(u.id),
      );
      selectedUsers.value = [...selected];
    }
  } catch (error) {
    console.error('加载用户列表失败:', error);
    userList.value = [];
  } finally {
    loading.value = false;
  }
}

// 处理选择变化
function handleSelectionChange(selected: User[]) {
  // 如果是单选，只保留最后一个
  if (!props.multiple && selected.length > 0) {
    selectedUsers.value = [selected[selected.length - 1]];
  } else {
    selectedUsers.value = [...selected];
  }
}

// 获取行唯一键
function getRowKey(record: User) {
  return record.id;
}

// 表格列定义
const columns = [
  {
    title: '用户名',
    key: 'username',
    dataIndex: 'username',
    width: 150,
  },
  {
    title: '昵称',
    key: 'nickname',
    dataIndex: 'nickname',
    width: 150,
  },
  {
    title: '部门',
    key: 'deptName',
    dataIndex: 'deptName',
    width: 150,
  },
];

// 打开弹窗
function openModal(data?: {
  title?: string;
  multiple?: boolean;
  selectedIds?: number[];
  showReason?: boolean;
  reasonLabel?: string;
  reasonRequired?: boolean;
}) {
  // 设置传入的参数（这里简单处理，实际可以存到组件内部状态）
  if (data) {
    // 可以在这里处理传入的配置
  }
  open.value = true;
}

// 关闭弹窗
function closeModal() {
  open.value = false;
}

// 确认
function handleConfirm() {
  if (selectedUsers.value.length === 0) {
    message.warning('请至少选择一个用户');
    return;
  }
  // 如果需要输入原因
  if (props.showReason && props.reasonRequired && !reason.value?.trim()) {
    message.warning(`请输入${props.reasonLabel}`);
    return;
  }
  emit('confirm', selectedUsers.value, reason.value);
  open.value = false;
}

// 暴露方法
defineExpose({
  open: openModal,
  close: closeModal,
});
</script>

<template>
  <a-modal
    v-model:open="open"
    :title="props.title"
    :class="['w-[700px]']"
    @ok="handleConfirm"
  >
    <div class="user-select-container">
      <!-- 提示信息 -->
      <div v-if="selectedUsers.length > 0" class="mb-4 text-sm text-gray-600">
        已选择 <span class="font-medium text-primary">{{ selectedUsers.length }}</span> 位用户
      </div>

      <!-- 用户表格 -->
      <a-table
        :columns="columns"
        :data-source="userList"
        :loading="loading"
        :row-key="getRowKey"
        :row-selection="{
          selectedRowKeys: selectedUsers.map((u) => u.id),
          onChange: (_, selectedRows) => handleSelectionChange(selectedRows),
          type: props.multiple ? 'checkbox' : 'radio',
        }"
        :pagination="{ pageSize: 20 }"
        :scroll="{ y: 300 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'username'">
            <div class="font-medium">{{ record.username }}</div>
          </template>
          <template v-else-if="column.key === 'nickname'">
            {{ record.nickname || '-' }}
          </template>
          <template v-else-if="column.key === 'deptName'">
            {{ record.deptName || '-' }}
          </template>
        </template>
      </a-table>

      <!-- 原因输入框 -->
      <div v-if="props.showReason" class="mt-4">
        <div class="mb-2 text-sm text-gray-600">
          {{ props.reasonLabel }}
          <span v-if="props.reasonRequired" class="text-red-500">*</span>
        </div>
        <a-textarea
          v-model:value="reason"
          :placeholder="`请输入${props.reasonLabel}`"
          :rows="3"
        />
      </div>
    </div>
  </a-modal>
</template>

<style scoped>
.user-select-container {
  padding: 16px;
}
</style>
