<script lang="ts" setup>
import type { DeclareIndicatorJointRuleApi } from '#/api/declare/jointRule';

import { computed, reactive, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';
import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import {
  createJointRule,
  getJointRule,
  updateJointRule,
} from '#/api/declare/jointRule';
import { $t } from '#/locales';

import RuleConfig from '../components/RuleConfig.vue';

const emit = defineEmits(['success']);

// 获取父组件传递的模态框 API
const [Modal, modalApi] = useVbenModal({
  // 弹窗打开状态变化时触发
  async onOpenChange(isOpen: boolean) {
    if (isOpen) {
      // 获取传入的数据
      const data = modalApi.getData<DeclareIndicatorJointRuleApi.JointRule>();
      console.log('[form.vue] onOpenChange, data:', data);
      
      if (data?.id) {
        // 编辑模式
        modalApi.lock();
        try {
          formData.value = await getJointRule(data.id);
          Object.assign(form, {
            ruleName: formData.value.ruleName || '',
            projectType: formData.value.projectType,
            triggerTiming: formData.value.triggerTiming || 'FILL',
            processNode: formData.value.processNode || '',
            ruleConfig: formData.value.ruleConfig || defaultRuleConfig,
            status: formData.value.status ?? 1,
          });
        } catch (error) {
          console.error('加载数据失败:', error);
          message.error('加载数据失败');
        } finally {
          modalApi.unlock();
        }
      } else {
        // 新增模式
        console.log('[form.vue] 新增模式');
        formData.value = {
          id: undefined,
          ruleName: '',
          projectType: undefined,
          triggerTiming: 'FILL',
          processNode: '',
          ruleConfig: defaultRuleConfig,
          status: 1,
        };
        Object.assign(form, {
          ruleName: '',
          projectType: undefined,
          triggerTiming: 'FILL',
          processNode: '',
          ruleConfig: defaultRuleConfig,
          status: 1,
        });
      }
    }
  },
  // 提交确认回调
  async onConfirm() {
    await handleSubmit();
  },
});

const formData = ref<DeclareIndicatorJointRuleApi.JointRule>();

// 表单 ref
const formRef = ref<any>();

// 默认规则配置
const defaultRuleConfig = '{"rules":[]}';

// 表单字段配置
const formRules = {
  ruleName: [{ required: true, message: '请输入规则名称' }],
};

// 表单布局
const formLayout = {
  labelCol: { span: 6 },
  wrapperCol: { span: 16 },
};

// 表单数据
const form = reactive({
  ruleName: '',
  projectType: undefined as number | undefined,
  triggerTiming: 'FILL',
  processNode: '',
  ruleConfig: defaultRuleConfig,
  status: 1,
});

const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['联合规则'])
    : $t('ui.actionTitle.create', ['联合规则']);
});

// 规则配置变化
function handleRuleConfigChange(value: string) {
  form.ruleConfig = value;
}

// 监听触发时机变化：当从流程提交切换到填报时，清空流程节点
watch(
  () => form.triggerTiming,
  (newVal, oldVal) => {
    if (oldVal === 'PROCESS_SUBMIT' && newVal === 'FILL') {
      form.processNode = '';
    }
  }
);

// 提交表单
async function handleSubmit() {
  try {
    await formRef.value?.validate();
  } catch (error) {
    message.error('请完善表单信息');
      return;
    }

    // 验证规则配置
  if (form.ruleConfig) {
      try {
      const config = JSON.parse(form.ruleConfig);
        if (!config.rules || config.rules.length === 0) {
          message.error('请至少配置一条规则');
          return;
        }
      } catch {
        message.error('规则配置格式错误');
        return;
      }
    } else {
      message.error('请配置规则');
      return;
    }

    modalApi.lock();
    try {
    const data: DeclareIndicatorJointRuleApi.JointRule = {
      ...form,
      id: formData.value?.id,
    };

      if (data.id) {
        await updateJointRule(data);
      } else {
        await createJointRule(data);
      }
      await modalApi.close();
      emit('success');
      message.success($t('ui.actionMessage.operationSuccess'));
    } catch (error) {
      message.error('保存失败');
    } finally {
      modalApi.unlock();
    }
}

// 旧的监听逻辑已移至 useVbenModal 的 onOpen 回调中
</script>

<template>
  <Modal :title="getTitle" class="w-1/2">
    <a-form
      ref="formRef"
      :model="form"
      :rules="formRules"
      v-bind="formLayout"
      class="mx-4"
    >
      <a-form-item label="规则名称" name="ruleName">
        <a-input
          v-model:value="form.ruleName"
          placeholder="请输入规则名称"
        />
      </a-form-item>

      <a-form-item label="项目类型" name="projectType">
        <a-select
          v-model:value="form.projectType"
          placeholder="请选择项目类型"
          allow-clear
        >
          <a-select-option :value="0">全部项目</a-select-option>
          <a-select-option
            v-for="item in getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number')"
            :key="item.value"
            :value="Number(item.value)"
          >
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="触发时机" name="triggerTiming">
        <a-select
          v-model:value="form.triggerTiming"
          placeholder="请选择触发时机"
        >
          <a-select-option value="FILL">填报时（实时验证）</a-select-option>
          <a-select-option value="PROCESS_SUBMIT">流程提交时</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="流程节点" name="processNode">
        <a-input
          v-model:value="form.processNode"
          placeholder="请输入流程节点（如：province_audit）"
          :disabled="form.triggerTiming === 'FILL'"
        />
      </a-form-item>

      <a-form-item label="规则配置" name="ruleConfig">
      <RuleConfig
          :model-value="form.ruleConfig"
          @update:model-value="handleRuleConfigChange"
      />
      </a-form-item>

      <a-form-item label="状态" name="status">
        <a-radio-group v-model:value="form.status">
          <a-radio :value="1">启用</a-radio>
          <a-radio :value="0">禁用</a-radio>
        </a-radio-group>
      </a-form-item>
    </a-form>
  </Modal>
</template>
