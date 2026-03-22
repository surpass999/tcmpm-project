<script lang="ts" setup>
import { ref, shallowRef } from 'vue';

import { message } from 'ant-design-vue';

import { getTaskListByReturn, returnTask } from '#/api/bpm/task';

interface ReturnNode {
  taskDefinitionKey: string;
  name: string;
}

interface Props {
  title?: string;
  reasonRequired?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  title: '退回',
  reasonRequired: true,
});

const emit = defineEmits<{
  'update:open': [open: boolean];
  success: [];
  cancel: [];
}>();

// 弹窗显示状态
const open = ref(false);

// 加载状态
const loading = ref(false);
// 可退回节点列表
const returnNodes = shallowRef<ReturnNode[]>([]);
// 已选中的节点（选中后是 taskDefinitionKey 字符串）
const selectedNode = ref<string | null>(null);
// 退回原因
const reason = ref('');

// 记录当前的 taskId
const currentTaskId = ref<string>('');

// 加载可退回节点
async function loadReturnNodes(taskId: string) {
  loading.value = true;
  try {
    const result = await getTaskListByReturn(taskId);
    returnNodes.value = result || [];
  } catch (error) {
    console.error('加载可退回节点失败:', error);
    returnNodes.value = [];
  } finally {
    loading.value = false;
  }
}

// 打开弹窗
function handleOpen(isOpen: boolean) {
  if (isOpen) {
    // 重置状态
    selectedNode.value = null;
    reason.value = '';

    // 加载可退回节点
    if (currentTaskId.value) {
      loadReturnNodes(currentTaskId.value);
    }
  }
}

// 暴露打开弹窗的方法
function openModal(data: { taskId: string; title?: string }) {
  currentTaskId.value = data.taskId;
  open.value = true;
}

// 暴露关闭弹窗的方法
function closeModal() {
  open.value = false;
}

// 确认退回
async function handleConfirm() {
  if (!currentTaskId.value) {
    message.error('任务ID不存在');
    return;
  }

  if (!selectedNode.value) {
    message.warning('请选择退回节点');
    return;
  }
  if (props.reasonRequired && !reason.value?.trim()) {
    message.warning('请输入退回原因');
    return;
  }

  loading.value = true;
  try {
    await returnTask({
      id: currentTaskId.value,
      targetTaskDefinitionKey: selectedNode.value,
      reason: reason.value,
    });
    message.success('退回成功');
    open.value = false;
    emit('success');
  } catch (error: any) {
    console.error('退回失败:', error);
    message.error(error.message || '退回失败');
  } finally {
    loading.value = false;
  }
}

// 监听 open 变化
import { watch } from 'vue';
watch(() => open.value, handleOpen);

defineExpose({
  open: openModal,
  close: closeModal,
});
</script>

<template>
  <a-modal
    v-model:open="open"
    :title="props.title"
    :confirm-loading="loading"
    class="w-[500px]"
    @ok="handleConfirm"
    @cancel="emit('cancel')"
  >
    <div class="return-modal-container">
      <!-- 选择退回节点 -->
      <a-form-item label="退回节点" required>
        <a-select
          v-model:value="selectedNode"
          placeholder="请选择退回节点"
          :loading="loading"
          :field-names="{ label: 'name', value: 'taskDefinitionKey' }"
          :options="returnNodes"
        />
      </a-form-item>

      <!-- 退回原因 -->
      <a-form-item label="退回原因" :required="props.reasonRequired">
        <a-textarea
          v-model:value="reason"
          placeholder="请输入退回原因"
          :rows="4"
          :maxlength="500"
          show-count
        />
      </a-form-item>
    </div>
  </a-modal>
</template>

<style scoped>
.return-modal-container {
  padding: 16px;
}
</style>
