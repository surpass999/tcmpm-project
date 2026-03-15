<script lang="ts" setup>
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { ProcessIndicatorConfigApi } from '#/api/declare/process-indicator-config';

import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import {
  createProcessIndicatorConfig,
  getIndicatorsByBusinessType,
  updateProcessIndicatorConfig,
} from '#/api/declare/process-indicator-config';
import { $t } from '#/locales';

/** 过程类型选项 */
const PROCESS_TYPE_OPTIONS = [
  { label: '建设过程', value: 1 },
  { label: '半年报', value: 2 },
  { label: '年度总结', value: 3 },
  { label: '中期评估', value: 4 },
  { label: '整改记录', value: 5 },
  { label: '验收申请', value: 6 },
];

/** 项目类型选项 */
const PROJECT_TYPE_OPTIONS = [
  { label: '通用类型', value: 0 },
  { label: '综合型', value: 1 },
  { label: '中医电子病历型', value: 2 },
  { label: '智慧中药房型', value: 3 },
  { label: '名老中医传承型', value: 4 },
  { label: '中医临床科研型', value: 5 },
  { label: '中医智慧医共体型', value: 6 },
];

const emit = defineEmits(['success']);

const formData = ref<ProcessIndicatorConfigApi.ConfigSaveParams>({
  processType: undefined,
  projectType: 0,
  indicatorId: undefined,
  isRequired: false,
  sort: 0,
});
const loading = ref(false);
const indicatorList = ref<DeclareIndicatorApi.Indicator[]>([]);

// 业务类型映射
const businessTypeMap: Record<number, string> = {
  1: 'process_construction',
  2: 'process_half_year',
  3: 'process_annual',
  4: 'process_midterm',
  5: 'process_rectification',
  6: 'process_acceptance',
};

// 根据过程类型获取可选指标列表
async function loadIndicators(processType: number) {
  const businessType = businessTypeMap[processType];
  if (!businessType) {
    indicatorList.value = [];
    return;
  }

  try {
    const res = await getIndicatorsByBusinessType(businessType);
    indicatorList.value = res || [];
  } catch (error) {
    console.error('加载指标列表失败:', error);
    indicatorList.value = [];
  }
}

// 过程类型变化时，加载对应的指标列表
async function handleProcessTypeChange(value: number) {
  formData.value.indicatorId = undefined;
  await loadIndicators(value);
}

const [Modal, modalApi] = useVbenModal({
  // eslint-disable-next-line vue/require-explicit-defaults
  async onOpen() {
    const data = modalApi.getData<ProcessIndicatorConfigApi.ConfigResp>();
    if (data?.id) {
      // 编辑模式
      formData.value = {
        id: data.id,
        processType: data.processType,
        projectType: data.projectType,
        indicatorId: data.indicatorId,
        isRequired: data.isRequired,
        sort: data.sort,
      };
      await loadIndicators(data.processType);
    } else {
      // 新增模式
      formData.value = {
        processType: undefined,
        projectType: 0,
        indicatorId: undefined,
        isRequired: false,
        sort: 0,
      };
      indicatorList.value = [];
    }
  },
});

async function handleSubmit() {
  if (!formData.value.processType) {
    message.warning('请选择过程类型');
    return;
  }
  if (formData.value.projectType === undefined || formData.value.projectType === null) {
    message.warning('请选择项目类型');
    return;
  }
  if (!formData.value.indicatorId) {
    message.warning('请选择指标');
    return;
  }

  loading.value = true;
  try {
    if (formData.value.id) {
      await updateProcessIndicatorConfig(formData.value);
      message.success($t('common.saveSuccess'));
    } else {
      await createProcessIndicatorConfig(formData.value);
      message.success($t('common.saveSuccess'));
    }
    emit('success');
    modalApi.close();
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <Modal
    :loading="loading"
    :title="formData.id ? '编辑过程指标配置' : '创建过程指标配置'"
    @close="modalApi.close"
    @submit="handleSubmit"
  >
    <a-form
      ref="formRef"
      :label-col="{ span: 6 }"
      :model="formData"
      autocomplete="off"
    >
      <a-form-item
        label="过程类型"
        name="processType"
        :rules="[{ required: true, message: '请选择过程类型' }]"
      >
        <a-select
          v-model:value="formData.processType"
          :options="PROCESS_TYPE_OPTIONS"
          placeholder="请选择过程类型"
          @change="handleProcessTypeChange"
        />
      </a-form-item>

      <a-form-item
        label="项目类型"
        name="projectType"
        :rules="[{ required: true, message: '请选择项目类型' }]"
      >
        <a-select
          v-model:value="formData.projectType"
          :options="PROJECT_TYPE_OPTIONS"
          placeholder="请选择项目类型"
        />
      </a-form-item>

      <a-form-item
        label="指标"
        name="indicatorId"
        :rules="[{ required: true, message: '请选择指标' }]"
      >
        <a-select
          v-model:value="formData.indicatorId"
          :options="indicatorList.map(item => ({
            label: `${item.indicatorCode} - ${item.indicatorName}`,
            value: item.id,
          }))"
          :disabled="!formData.processType"
          placeholder="请先选择过程类型"
          show-search
          :filter-option="(input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())"
        />
      </a-form-item>

      <a-form-item label="是否必填" name="isRequired">
        <a-switch v-model:checked="formData.isRequired" />
      </a-form-item>

      <a-form-item label="排序" name="sort">
        <a-input-number v-model:value="formData.sort" :min="0" />
      </a-form-item>
    </a-form>
  </Modal>
</template>
