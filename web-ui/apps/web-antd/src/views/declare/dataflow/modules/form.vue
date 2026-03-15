<script lang="ts" setup>
import type { DeclareDataFlowApi } from '#/api/declare/dataflow';

import { ref, reactive } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';
import {
  createDataFlow,
  getDataFlow,
  updateDataFlow,
} from '#/api/declare/dataflow';

const emit = defineEmits(['success']);

// 表单数据
const formData = ref<DeclareDataFlowApi.DataFlow>({});

// 流通类型选项
const flowTypeOptions = [
  { label: '内部使用', value: 1 },
  { label: '对外共享', value: 2 },
  { label: '交易', value: 3 },
];

// 数据质量选项
const dataQualityOptions = [
  { label: '优', value: 1 },
  { label: '良', value: 2 },
  { label: '中', value: 3 },
  { label: '差', value: 4 },
];

// 共享范围选项
const shareScopeOptions = [
  { label: '院内', value: 1 },
  { label: '省级', value: 2 },
  { label: '全国', value: 3 },
];

// 表单校验规则
const formRules = {
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  dataName: [{ required: true, message: '请输入数据名称', trigger: 'blur' }],
  dataType: [{ required: true, message: '请输入数据类型', trigger: 'blur' }],
  flowType: [{ required: true, message: '请选择流通类型', trigger: 'change' }],
};

const [Modal, modalApi] = useVbenModal({
  destroyOnClose: true,
  footer: true,
  confirmText: '保存',
  showCancelButton: true,
  async onConfirm() {
    const form = await modalApi.getForm();
    if (!form) return;
    
    try {
      await form.validate();
    } catch {
      message.error('请完善表单信息');
      return false;
    }

    modalApi.lock();
    try {
      const data = formData.value;
      if (data.id) {
        await updateDataFlow(data);
        message.success('更新成功');
      } else {
        await createDataFlow(data);
        message.success('创建成功');
      }
      await modalApi.close();
      emit('success');
    } catch (error) {
      console.error('保存失败:', error);
      message.error('保存失败');
    } finally {
      modalApi.unlock();
    }
    return false;
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = {};
      return;
    }

    const data = modalApi.getData<DeclareDataFlowApi.DataFlow>();
    if (data?.id) {
      // 编辑模式：加载数据
      try {
        const result = await getDataFlow(data.id);
        formData.value = result || {};
      } catch (error) {
        console.error('加载数据失败:', error);
        message.error('加载数据失败');
      }
    }
  },
});

defineExpose({ setData: modalApi.setData });
</script>

<template>
  <Modal :title="formData.id ? '编辑数据流通' : '新增数据流通'">
    <a-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
    >
      <a-form-item label="关联项目" name="projectId">
        <a-select v-model:value="formData.projectId" placeholder="请选择关联项目">
          <!-- 项目选择器需要后续接入项目列表API -->
          <a-select-option :value="1">示例项目1</a-select-option>
          <a-select-option :value="2">示例项目2</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="数据名称" name="dataName">
        <a-input v-model:value="formData.dataName" placeholder="请输入数据名称" />
      </a-form-item>

      <a-form-item label="数据类型" name="dataType">
        <a-input v-model:value="formData.dataType" placeholder="如：患者数据、诊疗数据" />
      </a-form-item>

      <a-form-item label="流通类型" name="flowType">
        <a-select v-model:value="formData.flowType" placeholder="请选择流通类型">
          <a-select-option v-for="item in flowTypeOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="数据来源" name="dataSource">
        <a-input v-model:value="formData.dataSource" placeholder="请输入数据来源" />
      </a-form-item>

      <a-form-item label="流通对象" name="flowObject">
        <a-input v-model:value="formData.flowObject" placeholder="请输入流通对象" />
      </a-form-item>

      <a-form-item label="流通目的" name="flowPurpose">
        <a-textarea v-model:value="formData.flowPurpose" placeholder="请输入流通目的" :rows="2" />
      </a-form-item>

      <a-form-item label="安全备案编号" name="securityFilingNo">
        <a-input v-model:value="formData.securityFilingNo" placeholder="请输入安全备案编号" />
      </a-form-item>

      <a-form-item label="数据产品证书数" name="certificateCount">
        <a-input-number v-model:value="formData.certificateCount" :min="0" class="w-full" />
      </a-form-item>

      <a-form-item label="产权登记证数" name="propertyCount">
        <a-input-number v-model:value="formData.propertyCount" :min="0" class="w-full" />
      </a-form-item>

      <a-form-item label="数据描述" name="dataDescription">
        <a-textarea v-model:value="formData.dataDescription" placeholder="请输入数据描述" :rows="3" />
      </a-form-item>
    </a-form>
  </Modal>
</template>
