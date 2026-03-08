<template>
  <div class="bpm-action-button">
    <template v-for="action in availableActions" :key="action.key">
      <!-- 提交审核按钮 -->
      <a-button
        v-if="action.key === 'submit'"
        type="primary"
        @click="handleSubmit(action)"
      >
        {{ action.label || '提交审核' }}
      </a-button>

      <!-- 其他操作按钮（暂时只支持提交） -->
      <a-button
        v-else
        @click="handleAction(action)"
      >
        {{ action.label || action.key }}
      </a-button>
    </template>

    <!-- 提交审核弹窗 -->
    <a-modal
      v-model:open="submitModalVisible"
      title="提交审核"
      @ok="handleSubmitConfirm"
      :confirm-loading="submitLoading"
    >
      <a-form :model="submitForm" layout="vertical">
        <a-form-item label="审批意见">
          <a-textarea
            v-model:value="submitForm.reason"
            placeholder="请输入审批意见（可选）"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { message } from 'ant-design-vue';
import { getAvailableActions, submitBpmAction, type SubmitActionParams } from '#/api/bpm/action';

interface BpmAction {
  key: string;
  label?: string;
  bizStatus?: string;
  bizStatusLabel?: string;
  bpmAction?: string;
  taskId?: string;
}

interface Props {
  businessType: string;
  businessId: number;
  reload?: () => void;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  success: [];
}>();

const availableActions = ref<BpmAction[]>([]);
const submitModalVisible = ref(false);
const submitLoading = ref(false);
const currentAction = ref<BpmAction | null>(null);
const submitForm = ref({
  reason: '',
});

// 加载可用操作
async function loadActions() {
  if (!props.businessType || !props.businessId) {
    availableActions.value = [];
    return;
  }

  try {
    const actions = await getAvailableActions(props.businessType, props.businessId);
    availableActions.value = actions || [];
  } catch (error) {
    console.error('[BpmActionButton] 加载操作失败:', error);
    availableActions.value = [];
  }
}

// 提交审核
function handleSubmit(action: BpmAction) {
  currentAction.value = action;
  submitForm.value.reason = '';
  submitModalVisible.value = true;
}

// 确认提交
async function handleSubmitConfirm() {
  if (!currentAction.value) {
    return;
  }

  submitLoading.value = true;
  try {
    const params: SubmitActionParams = {
      businessType: props.businessType,
      businessId: props.businessId,
      actionKey: currentAction.value.key,
      reason: submitForm.value.reason,
    };

    await submitBpmAction(params);
    message.success('提交成功');

    submitModalVisible.value = false;

    // 刷新操作列表
    await loadActions();

    // 触发成功回调
    emit('success');

    // 如果有外部刷新函数，也调用
    if (props.reload) {
      props.reload();
    }
  } catch (error: any) {
    console.error('[BpmActionButton] 提交失败:', error);
    message.error(error.message || '提交失败');
  } finally {
    submitLoading.value = false;
  }
}

// 其他操作（暂未实现）
function handleAction(action: BpmAction) {
  message.info(`暂不支持操作: ${action.label || action.key}`);
}

// 监听业务ID变化，重新加载
watch(
  () => [props.businessType, props.businessId],
  () => {
    loadActions();
  },
  { immediate: true }
);

onMounted(() => {
  loadActions();
});

// 暴露方法供外部调用
defineExpose({
  loadActions,
});
</script>

<style scoped>
.bpm-action-button {
  display: inline-flex;
  gap: 8px;
}
</style>
