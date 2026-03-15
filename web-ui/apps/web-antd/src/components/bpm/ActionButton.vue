<template>
  <div class="bpm-action-button">
    <template v-for="action in availableActions" :key="action.id">
      <!-- 显示已启用的按钮 -->
      <a-button
        v-if="action.enable"
        :type="getActionType(action.id)"
        @click="handleActionClick(action)"
      >
        <template #icon>
          <IconifyIcon v-if="action.id === OperationButtonType.APPROVE" icon="ep:circle-check" />
          <IconifyIcon v-else-if="action.id === OperationButtonType.REJECT" icon="ep:circle-close" />
          <IconifyIcon v-else-if="action.id === OperationButtonType.TRANSFER" icon="ep:right" />
          <IconifyIcon v-else-if="action.id === OperationButtonType.DELEGATE" icon="ep:user" />
          <IconifyIcon v-else-if="action.id === OperationButtonType.ADD_SIGN" icon="ep:document-add" />
          <IconifyIcon v-else-if="action.id === OperationButtonType.RETURN" icon="ep:back" />
          <IconifyIcon v-else-if="action.id === OperationButtonType.COPY" icon="ep:copy-document" />
          <IconifyIcon v-else-if="action.id === OperationButtonType.SELECT_APPROVER" icon="ep:user" />
          <IconifyIcon v-else icon="ep:minus" />
        </template>
        {{ action.displayName }}
      </a-button>
    </template>

    <!-- 审批操作弹窗（填写审批意见） -->
    <a-modal
      v-model:open="actionModalVisible"
      :title="currentAction?.displayName || '审批操作'"
      :confirm-loading="actionLoading"
      @ok="handleActionConfirm"
    >
      <a-form :model="actionForm" layout="vertical">
        <a-form-item label="审批意见" :required="true">
          <a-textarea
            v-model:value="actionForm.reason"
            placeholder="请输入审批意见"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 专家选择弹窗 -->
    <ExpertSelectModal
      ref="expertModalRef"
      :multiple="true"
      :expert-min="1"
      :expert-max="10"
      @confirm="handleExpertConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { submitBpmAction, type SubmitActionParams } from '#/api/bpm/action';
import ExpertSelectModal from './ExpertSelectModal.vue';
import type { DeclareExpertApi } from '#/api/declare/expert';
import { OperationButtonType } from '#/views/bpm/components/simple-process-design/consts';

/**
 * Simple 设计器按钮配置格式 - 数组格式
 */
interface ButtonSetting {
  id: OperationButtonType;
  displayName: string;
  enable: boolean;
}

/**
 * 后端返回的按钮配置格式 - 对象格式
 * key 是按钮类型数字（如 1=通过, 2=拒绝, 6=退回, 8=选择专家）
 */
interface ButtonSettingMap {
  [key: number]: {
    displayName: string;
    enable: boolean;
  };
}

/**
 * 任务信息
 */
interface TaskInfo {
  taskId?: string;
  processInstanceId?: string;
}

interface Props {
  businessType: string;
  businessId: number;
  actions?: ButtonSetting[] | ButtonSettingMap;
  taskInfo?: TaskInfo;
  reload?: () => void;
}

const props = withDefaults(defineProps<Props>(), {
  actions: () => [],
  taskInfo: () => ({}),
});

const emit = defineEmits<{
  success: [];
  'update:actions': [actions: ButtonSetting[]];
  refresh: [];
}>();

const availableActions = ref<ButtonSetting[]>([]);
const actionModalVisible = ref(false);
const actionLoading = ref(false);
const currentAction = ref<ButtonSetting | null>(null);
const actionForm = ref({
  reason: '',
});

// 专家选择弹窗
const expertModalRef = ref<{
  open: () => void;
  close: () => void;
} | null>(null);

// 已选中的专家（用于提交时传递 IDs）
const selectedExperts = ref<DeclareExpertApi.Expert[]>([]);

// 监听外部传入的 actions
watch(
  () => props.actions,
  (newActions) => {
    if (!newActions || newActions.length === 0) {
      availableActions.value = [];
      return;
    }

    // 判断是数组格式还是对象格式
    if (Array.isArray(newActions)) {
      // 数组格式：[{ id: 1, displayName: '通过', enable: true }, ...]
      availableActions.value = newActions.filter((a) => a.enable);
    } else {
      // 对象格式：{ 1: { displayName: '通过', enable: true }, 2: { displayName: '拒绝', enable: true }, ... }
      const actions: ButtonSetting[] = [];
      Object.entries(newActions).forEach(([key, config]: [string, any]) => {
        if (config && config.enable) {
          actions.push({
            id: parseInt(key),
            displayName: config.displayName || key,
            enable: true,
          });
        }
      });
      availableActions.value = actions;
    }
  },
  { immediate: true, deep: true }
);

// 获取按钮类型
function getActionType(id: number): 'primary' | 'default' | 'dashed' | 'link' | 'text' {
  if (id === OperationButtonType.APPROVE) {
    return 'primary';
  }
  if (id === OperationButtonType.REJECT) {
    return 'default';
  }
  return 'default';
}

// 处理操作点击
function handleActionClick(action: ButtonSetting) {
  currentAction.value = action;
  actionForm.value.reason = '';

  // 选择专家操作 - 打开专家选择弹窗
  if (action.id === OperationButtonType.SELECT_APPROVER) {
    expertModalRef.value?.open();
    return;
  }

  // 拒绝和退回操作，需要填写审批意见
  if (action.id === OperationButtonType.REJECT || action.id === OperationButtonType.RETURN) {
    actionModalVisible.value = true;
    return;
  }

  // 其他操作直接执行（通过、转办、委派、加签等）
  handleActionConfirm();
}

// 专家选择确认
function handleExpertConfirm(experts: DeclareExpertApi.Expert[]) {
  selectedExperts.value = experts;
  // 选择专家后不直接提交，而是触发刷新事件，让父组件刷新数据
  // ExpertSelectModal 使用 useVbenModal，onConfirm 返回 true 后会自动关闭弹窗
  emit('refresh');
}

// 获取按钮对应的 actionKey
function getActionKey(id: number): string {
  const actionKeyMap: Record<number, string> = {
    [OperationButtonType.APPROVE]: 'approve',
    [OperationButtonType.REJECT]: 'reject',
    [OperationButtonType.TRANSFER]: 'transfer',
    [OperationButtonType.DELEGATE]: 'delegate',
    [OperationButtonType.ADD_SIGN]: 'addSign',
    [OperationButtonType.RETURN]: 'return',
    [OperationButtonType.COPY]: 'copy',
    [OperationButtonType.SELECT_APPROVER]: 'selectApprover',
  };
  return actionKeyMap[id] || 'approve';
}

// 确认操作
async function handleActionConfirm() {
  if (!currentAction.value) {
    return;
  }

  actionLoading.value = true;
  try {
    // 获取专家用户 IDs
    const expertUserIds = selectedExperts.value.length > 0
      ? selectedExperts.value.map(e => e.userId).filter((id): id is number => id !== undefined)
      : undefined;

    const params: SubmitActionParams = {
      businessType: props.businessType,
      businessId: props.businessId,
      actionKey: getActionKey(currentAction.value.id),
      reason: actionForm.value.reason,
      expertUserIds,
      taskId: props.taskInfo?.taskId,
    };

    await submitBpmAction(params);
    message.success('操作成功');

    actionModalVisible.value = false;
    selectedExperts.value = [];

    // 触发成功回调
    emit('success');

    // 如果有外部刷新函数，也调用
    if (props.reload) {
      props.reload();
    }
  } catch (error: any) {
    console.error('[BpmActionButton] 操作失败:', error);
    message.error(error.message || '操作失败');
  } finally {
    actionLoading.value = false;
  }
}

// 暴露方法供外部调用
defineExpose({
  loadActions: () => {
    // 外部调用时不做任何操作，由父组件控制 actions
  },
});
</script>

<style scoped>
.bpm-action-button {
  display: inline-flex;
  gap: 8px;
}
</style>
