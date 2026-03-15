<script lang="ts" setup>
import type { DeclareAchievementApi } from '#/api/declare/achievement';

import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';
import {
  createAchievement,
  getAchievement,
  updateAchievement,
} from '#/api/declare/achievement';

const emit = defineEmits(['success']);

// 表单数据
const formData = ref<DeclareAchievementApi.Achievement>({});

// 成果类型选项
const achievementTypeOptions = [
  { label: '系统功能', value: 1 },
  { label: '数据集', value: 2 },
  { label: '科研成果', value: 3 },
  { label: '管理经验', value: 4 },
];

// 表单校验规则
const formRules = {
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  achievementName: [{ required: true, message: '请输入成果名称', trigger: 'blur' }],
  achievementType: [{ required: true, message: '请选择成果类型', trigger: 'change' }],
  applicationField: [{ required: true, message: '请输入应用领域', trigger: 'blur' }],
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
        await updateAchievement(data);
        message.success('更新成功');
      } else {
        await createAchievement(data);
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

    const data = modalApi.getData<DeclareAchievementApi.Achievement>();
    if (data?.id) {
      // 编辑模式：加载数据
      try {
        const result = await getAchievement(data.id);
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
  <Modal :title="formData.id ? '编辑成果' : '新增成果'">
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

      <a-form-item label="成果名称" name="achievementName">
        <a-input v-model:value="formData.achievementName" placeholder="请输入成果名称" />
      </a-form-item>

      <a-form-item label="成果类型" name="achievementType">
        <a-select v-model:value="formData.achievementType" placeholder="请选择成果类型">
          <a-select-option v-for="item in achievementTypeOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="应用领域" name="applicationField">
        <a-input v-model:value="formData.applicationField" placeholder="请输入应用领域" />
      </a-form-item>

      <a-form-item label="成果描述" name="description">
        <a-textarea v-model:value="formData.description" placeholder="请输入成果描述" :rows="4" />
      </a-form-item>
    </a-form>
  </Modal>
</template>
