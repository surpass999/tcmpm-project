<script lang="ts" setup>
import type { DeclareProjectApi } from '#/api/declare/project';

import { computed, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Form, Input, message } from 'ant-design-vue';

import { createProjectProcess, updateProjectProcess } from '#/api/declare/project';

interface Props {
  projectId?: number;
  processType?: number;
  process?: DeclareProjectApi.ProjectProcess | null;
  readonly?: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  success: [];
}>();

// 弹窗表单数据
const formData = ref<{
  id?: number;
  projectId: number;
  processType: number;
  processTitle: string;
  reportPeriodStart?: string;
  reportPeriodEnd?: string;
  indicatorValues: string;
  processData: string;
}>({
  projectId: 0,
  processType: 1,
  processTitle: '',
  indicatorValues: '{}',
  processData: '{}',
});

// 加载状态
const loading = ref(false);

// 过程类型配置
const processTypeOptions = [
  { value: 1, label: '建设过程' },
  { value: 2, label: '年度总结' },
  { value: 3, label: '中期评估' },
  { value: 4, label: '整改记录' },
  { value: 5, label: '验收申请' },
];

// 弹窗标题
const modalTitle = computed(() => {
  if (props.readonly) return '查看过程记录';
  return props.process?.id ? '编辑过程记录' : '新建过程记录';
});

// 是否可以编辑（只有草稿状态可以编辑）
const canEdit = computed(() => {
  return !props.readonly && (props.process?.status === 0 || !props.process?.id);
});

// 初始化表单数据
watch(
  () => props.process,
  (newProcess) => {
    if (newProcess) {
      formData.value = {
        id: newProcess.id,
        projectId: newProcess.projectId || props.projectId || 0,
        processType: newProcess.processType || props.processType || 1,
        processTitle: newProcess.processTitle || '',
        reportPeriodStart: newProcess.reportPeriodStart as any,
        reportPeriodEnd: newProcess.reportPeriodEnd as any,
        indicatorValues: newProcess.indicatorValues || '{}',
        processData: newProcess.processData || '{}',
      };
    } else {
      formData.value = {
        projectId: props.projectId || 0,
        processType: props.processType || 1,
        processTitle: '',
        indicatorValues: '{}',
        processData: '{}',
      };
    }
  },
  { immediate: true },
);

// 提交表单
async function handleSubmit() {
  if (!formData.value.processTitle.trim()) {
    message.warning('请输入过程标题');
    return;
  }

  loading.value = true;
  try {
    if (formData.value.id) {
      // 更新
      await updateProjectProcess(formData.value as any);
      message.success('更新成功');
    } else {
      // 创建
      await createProjectProcess(formData.value as any);
      message.success('创建成功');
    }

    // 关闭弹窗并触发刷新
    emit('success');
  } catch (e) {
    console.error('保存失败', e);
    message.error('保存失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <a-modal
    :open="true"
    :title="modalTitle"
    :confirm-loading="loading"
    :width="700"
    :footer="canEdit ? undefined : null"
    @ok="canEdit ? handleSubmit() : null"
    @cancel="() => {}"
  >
    <Form
      :model="formData"
      :disabled="!canEdit"
      layout="vertical"
      class="process-form"
    >
      <Form.Item label="过程类型" name="processType">
        <a-select
          v-model:value="formData.processType"
          :options="processTypeOptions"
          placeholder="请选择过程类型"
        />
      </Form.Item>

      <Form.Item
        label="过程标题"
        name="processTitle"
        :rules="[{ required: true, message: '请输入过程标题' }]"
      >
        <a-input
          v-model:value="formData.processTitle"
          placeholder="如：2025年上半年建设过程"
        />
      </Form.Item>

      <Form.Item label="报告周期" name="reportPeriod">
        <a-range-picker
          :value="[formData.reportPeriodStart, formData.reportPeriodEnd]"
          @update:value="(val: [string, string] | null) => {
            if (val) {
              formData.reportPeriodStart = val[0];
              formData.reportPeriodEnd = val[1];
            }
          }"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          style="width: 100%"
        />
      </Form.Item>

      <!-- TODO: 后续需要接入指标表单组件 -->
      <Form.Item label="指标值" name="indicatorValues">
        <a-textarea
          v-model:value="formData.indicatorValues"
          :rows="6"
          placeholder='请输入指标值 JSON，如：{"201": 1000, "202": 500}'
          :disabled="!canEdit"
        />
        <div class="form-tip">提示：草稿状态可编辑指标值，提交审核后将锁定</div>
      </Form.Item>

      <Form.Item label="过程数据" name="processData">
        <a-textarea
          v-model:value="formData.processData"
          :rows="4"
          placeholder='请输入过程数据 JSON'
          :disabled="!canEdit"
        />
      </Form.Item>
    </Form>
  </a-modal>
</template>

<style lang="scss" scoped>
.process-form {
  .form-tip {
    margin-top: 4px;
    font-size: 12px;
    color: #999;
  }
}
</style>
