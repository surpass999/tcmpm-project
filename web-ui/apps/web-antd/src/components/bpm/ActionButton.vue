<template>
    <div class="bpm-action-button">
      <template v-for="action in availableActions" :key="action.key">
        <!-- 退回按钮：发起节点不显示退回按钮 -->
        <a-button
          v-if="isReturnAction(action) && !action.vars?.isStartNode"
          :type="getActionType(action.key)"
          @click="handleActionClick(action)"
        >
          <template #icon>
            <IconifyIcon icon="ep:back" />
          </template>
          {{ getActionLabel(action) }}
        </a-button>

        <!-- 其他按钮 -->
        <a-button
          v-else-if="!isReturnAction(action)"
          :type="getActionType(action.key)"
          @click="handleActionClick(action)"
        >
          <template #icon>
            <IconifyIcon v-if="action.key.includes('pass') || action.key.includes('agree')" icon="ep:circle-check" />
            <IconifyIcon v-else-if="action.key.includes('reject') || action.key.includes('refuse')" icon="ep:circle-close" />
            <IconifyIcon v-else-if="action.key.includes('transfer')" icon="ep:right" />
            <IconifyIcon v-else-if="action.key.includes('selectExpert') || action.key.includes('expert')" icon="ep:user" />
            <IconifyIcon v-else icon="ep:minus" />
          </template>
          {{ getActionLabel(action) }}
        </a-button>
      </template>
  
      <!-- 审批操作弹窗（填写审批意见） -->
      <a-modal
        v-model:open="actionModalVisible"
        :title="currentAction?.label || '审批操作'"
        :confirm-loading="actionLoading"
        @ok="handleActionConfirm"
      >
        <a-form :model="actionForm" layout="vertical">
          <a-form-item :label="currentAction?.vars?.reason || '审批意见'" :required="currentAction?.vars?.reasonRequired">
            <a-textarea
              v-model:value="actionForm.reason"
              :placeholder="currentAction?.vars?.reason ? `请输入${currentAction.vars.reason}` : '请输入审批意见'"
              :rows="4"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </template>

  <script setup lang="ts">
  import { ref, watch } from 'vue';
  import { message } from 'ant-design-vue';
  import { IconifyIcon } from '@vben/icons';
  import { submitBpmAction, type SubmitActionParams } from '#/api/bpm/action';

  interface BpmAction {
    key: string;
    label?: string;
    bizStatus?: string;
    bizStatusLabel?: string;
    bpmAction?: string;
    taskId?: string;
    vars?: {
      reason?: string;
      reasonRequired?: boolean;
      expertMin?: number;
      expertMax?: number;
      isReturned?: boolean;
      isStartNode?: boolean;
      backStrategy?: string;
      [key: string]: any;
    };
  }
  
  interface Props {
    businessType: string;
    businessId: number;
    actions?: BpmAction[];
    taskId?: string;
    reload?: () => void;
  }

  const props = withDefaults(defineProps<Props>(), {
    actions: () => [],
  });
  
const emit = defineEmits<{
  success: [];
  'update:actions': [actions: BpmAction[]];
  refresh: [];
  /** 退回操作（需父组件打开 ReturnModal） */
  return: [action: BpmAction];
}>();

const availableActions = ref<BpmAction[]>([]);
const actionModalVisible = ref(false);
const actionLoading = ref(false);
const currentAction = ref<BpmAction | null>(null);
const actionForm = ref({
  reason: '',
});

/** 判断是否为退回按钮 */
function isReturnAction(action: BpmAction): boolean {
  return (
    action.bizStatus === 'PROVINCE_RETURNED' ||
    action.key === '6' ||
    action.key.toLowerCase() === 'back'
  );
}

  // 监听外部传入的 actions
  watch(
    () => props.actions,
    (newActions) => {
      // 过滤掉 _PROCESS_RUNNING_ 标记（这是流程进行中但无操作权限的标记）
      availableActions.value = (newActions || []).filter((a: any) => a.key !== '_PROCESS_RUNNING_');
    },
    { immediate: true, deep: true }
  );
  
  // 获取按钮类型
  function getActionType(key: string): 'primary' | 'default' | 'dashed' | 'link' | 'text' {
    if (key === '1' || key.includes('pass') || key.includes('agree') || key.includes('approve')) {
      return 'primary';
    }
    if (key === '2' || key.includes('reject') || key.includes('refuse') || key.includes('deny')) {
      return 'default';
    }
    if (key === '6' || key.toLowerCase() === 'back') {
      return 'default';
    }
    return 'default';
  }

  // 获取按钮显示文案
  function getActionLabel(action: BpmAction): string {
    const vars = action.vars || {};
    const key = action.key;

    // 退回状态或发起节点：submit 改为"重新提交"
    if ((vars.isReturned || vars.isStartNode) && (key === 'submit' || key === 'resubmit')) {
      return '重新提交';
    }

    // 退回按钮显示文案
    if (isReturnAction(action)) {
      if (vars.backStrategy === 'TO_START') {
        return '退回发起人';
      }
      if (vars.backStrategy === 'TO_PREV') {
        return '退回上一级';
      }
      return '退回';
    }

    return action.label || key;
  }

  // 处理操作点击
  function handleActionClick(action: BpmAction) {
    // 退回按钮：emit 给父组件，由父组件打开 ReturnModal
    if (isReturnAction(action)) {
      emit('return', action);
      return;
    }

    const vars = action.vars || {};
    currentAction.value = action;
    actionForm.value.reason = '';

    // 如果需要填写理由，打开弹窗
    if (vars.reason || vars.reasonRequired) {
      actionModalVisible.value = true;
      return;
    }

    // 直接执行
    handleActionConfirm();
  }

  // 确认操作
  async function handleActionConfirm() {
    if (!currentAction.value) {
      return;
    }

    actionLoading.value = true;
    try {
      const params: SubmitActionParams = {
        businessType: props.businessType,
        businessId: props.businessId,
        actionKey: currentAction.value.key,
        reason: actionForm.value.reason,
        taskId: props.taskId || currentAction.value.taskId,
      };
  
      // 退回操作应走 ReturnModal，这里只是防御性检查
      if (isReturnAction(currentAction.value)) {
        message.error('退回操作请通过退回弹窗进行');
        return;
      }
  
      await submitBpmAction(params);
      message.success('操作成功');
  
      actionModalVisible.value = false;

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