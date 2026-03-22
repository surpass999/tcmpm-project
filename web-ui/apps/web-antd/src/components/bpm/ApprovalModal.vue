<script lang="ts" setup>
import { ref, watch } from 'vue';

interface Props {
  title?: string;
  action?: 'approve' | 'reject';
  reasonRequired?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  title: '审批',
  action: 'approve',
  reasonRequired: true,
});

const emit = defineEmits<{
  'update:open': [open: boolean];
  confirm: [reason: string, action: 'approve' | 'reject'];
  cancel: [];
}>();

// 弹窗显示状态
const open = ref(false);

// 原因输入
const reason = ref('');

// 当前的 action
const currentAction = ref<'approve' | 'reject'>('approve');

// 监听 open 变化
watch(
  () => open.value,
  (isOpen) => {
    if (!isOpen) {
      // 关闭时重置
      reason.value = '';
    }
  },
);

// 打开弹窗
function openModal(data: { action?: 'approve' | 'reject'; title?: string; reason?: string }) {
  currentAction.value = data.action || 'approve';
  if (data.reason) {
    reason.value = data.reason;
  }
  open.value = true;
}

// 关闭弹窗
function closeModal() {
  open.value = false;
}

// 确认
function handleConfirm() {
  if (props.reasonRequired && currentAction.value === 'reject' && !reason.value?.trim()) {
    return;
  }
  emit('confirm', reason.value, currentAction.value);
  open.value = false;
}

// 获取弹窗标题
const modalTitle = ref('');
watch(
  [() => props.title, currentAction],
  ([title, action]) => {
    if (action === 'approve') {
      modalTitle.value = title || '审批通过';
    } else {
      modalTitle.value = '审批拒绝';
    }
  },
  { immediate: true },
);

defineExpose({
  open: openModal,
  close: closeModal,
});
</script>

<template>
  <a-modal
    v-model:open="open"
    :title="modalTitle"
    @ok="handleConfirm"
    @cancel="emit('cancel')"
  >
    <a-form layout="vertical">
      <a-form-item
        :label="currentAction === 'approve' ? '审批意见' : '拒绝原因'"
        :required="currentAction === 'reject' && reasonRequired"
      >
        <a-textarea
          v-model:value="reason"
          :placeholder="currentAction === 'approve' ? '请输入审批意见（可选）' : '请输入拒绝原因'"
          :rows="4"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
