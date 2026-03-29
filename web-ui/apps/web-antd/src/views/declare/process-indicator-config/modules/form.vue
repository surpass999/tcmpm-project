<script lang="ts" setup>
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { ProcessIndicatorConfigApi } from '#/api/declare/process-indicator-config';

import { ref } from 'vue';

import { getDictOptions } from '@vben/hooks';
import { DICT_TYPE } from '@vben/constants';

import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import {
  createProcessIndicatorConfig,
  updateProcessIndicatorConfig,
} from '#/api/declare/process-indicator-config';
import { getIndicatorPage } from '#/api/declare/indicator';
import { getProjectTypeSimpleList } from '#/api/declare/project-type';
import { $t } from '#/locales';

/** 过程类型选项 - 从字典获取 */
const processTypeOptions = getDictOptions(DICT_TYPE.DECLARE_PROCESS_TYPE, 'number');

/** 项目类型选项 - 从 API 加载 */
const projectTypeOptions = ref<{ label: string; value: number }[]>([]);

/** 加载项目类型选项 */
const loadProjectTypeOptions = async () => {
  try {
    const list = await getProjectTypeSimpleList();
    projectTypeOptions.value = list.map((item) => ({
      label: item.title || item.name,
      value: item.typeValue,
    }));
  } catch {
    projectTypeOptions.value = [];
  }
};

const emit = defineEmits(['success']);

const formData = ref<ProcessIndicatorConfigApi.ConfigSaveParams>({
  processType: undefined,
  projectType: undefined,
  indicatorId: undefined,
  isRequired: false,
  sort: 0,
  maxScore: 100,
  scoreRatioSatisfied: 1.0,
  scoreRatioBasic: 0.75,
  scoreRatioPartial: 0.5,
  scoreRatioUnsatisfied: 0.25,
});
const loading = ref(false);
const indicatorList = ref<DeclareIndicatorApi.Indicator[]>([]);

// 加载所有指标（根据项目类型筛选）
async function loadIndicators(projectType: number) {
  try {
    // 根据项目类型查询指标
    const res = await getIndicatorPage({
      pageNo: 1,
      pageSize: 200,
      projectType: projectType,
    });
    indicatorList.value = res?.list || [];
  } catch (error) {
    console.error('加载指标列表失败:', error);
    indicatorList.value = [];
  }
}

// 项目类型变化时，重新加载指标列表
async function handleProjectTypeChange(value: number) {
  formData.value.indicatorId = undefined;
  await loadIndicators(value);
}

// 过程类型变化时，重置指标选择
function handleProcessTypeChange() {
  formData.value.indicatorId = undefined;
}

const [Modal, modalApi] = useVbenModal({
  destroyOnClose: true,
  async onConfirm() {
    return await handleSubmit();
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) return;

    // 加载项目类型选项
    await loadProjectTypeOptions();

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
        maxScore: data.maxScore ?? 100,
        scoreRatioSatisfied: data.scoreRatioSatisfied ?? 1.0,
        scoreRatioBasic: data.scoreRatioBasic ?? 0.75,
        scoreRatioPartial: data.scoreRatioPartial ?? 0.5,
        scoreRatioUnsatisfied: data.scoreRatioUnsatisfied ?? 0.25,
      };
      // 编辑时根据项目类型加载指标
      await loadIndicators(data.projectType);
    } else {
      // 新增模式
      formData.value = {
        processType: undefined,
        projectType: undefined,
        indicatorId: undefined,
        isRequired: false,
        sort: 0,
        maxScore: 100,
        scoreRatioSatisfied: 1.0,
        scoreRatioBasic: 0.75,
        scoreRatioPartial: 0.5,
        scoreRatioUnsatisfied: 0.25,
      };
      // 新增时加载通用类型的指标
      await loadIndicators(0);
    }
  },
});

async function handleSubmit() {
  if (!formData.value.processType) {
    message.warning('请选择过程类型');
    return false;
  }
  if (formData.value.projectType === undefined || formData.value.projectType === null) {
    message.warning('请选择项目类型');
    return false;
  }
  if (!formData.value.indicatorId) {
    message.warning('请选择指标');
    return false;
  }

  loading.value = true;
  try {
    if (formData.value.id) {
      await updateProcessIndicatorConfig(formData.value);
      message.success($t('ui.actionMessage.operationSuccess'));
    } else {
      await createProcessIndicatorConfig(formData.value);
      message.success($t('ui.actionMessage.operationSuccess'));
    }
    emit('success');
    return true;
  } catch (error) {
    console.error('保存失败:', error);
    return false;
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <Modal
    :loading="loading"
    :title="formData.id ? '编辑过程指标配置' : '创建过程指标配置'"
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
          :options="processTypeOptions"
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
          :options="projectTypeOptions"
          placeholder="请选择项目类型"
          @change="handleProjectTypeChange"
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
          placeholder="请选择指标"
          show-search
          :filter-option="(input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())"
        />
      </a-form-item>

      <a-form-item label="是否必填" name="isRequired">
        <a-switch v-model:checked="formData.isRequired" />
      </a-form-item>

      <a-divider>评分配置</a-divider>

      <a-form-item label="满分值" name="maxScore">
        <a-input-number v-model:value="formData.maxScore" :min="0" :max="100" />
      </a-form-item>

      <a-form-item label="满足(100%)比例" name="scoreRatioSatisfied">
        <a-input-number
          v-model:value="formData.scoreRatioSatisfied"
          :min="0"
          :max="1"
          :step="0.05"
          :precision="4"
        />
      </a-form-item>

      <a-form-item label="基本满足(75%)比例" name="scoreRatioBasic">
        <a-input-number
          v-model:value="formData.scoreRatioBasic"
          :min="0"
          :max="1"
          :step="0.05"
          :precision="4"
        />
      </a-form-item>

      <a-form-item label="部分满足(50%)比例" name="scoreRatioPartial">
        <a-input-number
          v-model:value="formData.scoreRatioPartial"
          :min="0"
          :max="1"
          :step="0.05"
          :precision="4"
        />
      </a-form-item>

      <a-form-item label="不满足(25%)比例" name="scoreRatioUnsatisfied">
        <a-input-number
          v-model:value="formData.scoreRatioUnsatisfied"
          :min="0"
          :max="1"
          :step="0.05"
          :precision="4"
        />
      </a-form-item>

      <a-form-item label="排序" name="sort">
        <a-input-number v-model:value="formData.sort" :min="0" />
      </a-form-item>
    </a-form>
  </Modal>
</template>
