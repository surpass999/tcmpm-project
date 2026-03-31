<script lang="ts" setup>
import type { ProjectTypeApi } from '#/api/declare/project-type';
import { computed, ref } from 'vue';

import { $t } from '#/locales';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { createProjectType, getProjectType, updateProjectType } from '#/api/declare/project-type';

const emit = defineEmits(['success']);

const formData = ref<ProjectTypeApi.ProjectType | undefined>();
const formRef = ref<any>();

const getTitle = computed(() => {
  return formData.value?.id ? $t('ui.actionTitle.edit', ['项目类型']) : $t('ui.actionTitle.create', ['项目类型']);
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    try {
      await formRef.value?.validate();
    } catch {
      message.error('请完善表单信息');
      return;
    }
    modalApi.lock();
    const data = formData.value as ProjectTypeApi.ProjectType;
    try {
      if (data.id) {
        await updateProjectType({
          id: data.id,
          name: data.name,
          title: data.title,
          description: data.description,
          color: data.color,
          sort: data.sort,
          status: data.status,
        });
      } else {
        await createProjectType({
          typeCode: data.typeCode,
          typeValue: data.typeValue,
          name: data.name,
          title: data.title,
          description: data.description,
          color: data.color,
          sort: data.sort || 0,
        });
      }
      await modalApi.close();
      emit('success');
      message.success($t('ui.actionMessage.operationSuccess'));
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      return;
    }
    const data = modalApi.getData<ProjectTypeApi.ProjectType>();
    if (!data || !data.id) {
      // 新建时自动生成编码和值
      formData.value = {
        id: undefined,
        typeCode: '',
        typeValue: 0,
        name: '',
        title: '',
        description: '',
        color: '',
        sort: 0,
        status: 1,
      };
      return;
    }
    modalApi.lock();
    try {
      const detail = await getProjectType(data.id);
      formData.value = detail;
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-1/2">
    <a-form
      v-if="formData"
      ref="formRef"
      :model="formData"
      layout="vertical"
      class="mx-4"
    >
      <a-form-item label="类型编码" name="typeCode">
        <a-input
          v-model:value="formData.typeCode"
          placeholder="请输入类型编码，如 PROJECT_01"
          :disabled="!!formData.id"
        />
      </a-form-item>
      <a-form-item label="类型值" name="typeValue">
        <a-input-number
          v-model:value="formData.typeValue"
          placeholder="请输入类型值"
          :min="1"
          :max="99"
          :disabled="!!formData.id"
          class="w-full"
        />
      </a-form-item>
      <a-form-item label="类型名称" name="name">
        <a-input
          v-model:value="formData.name"
          placeholder="请输入类型名称，如 综合型"
        />
      </a-form-item>
      <a-form-item label="显示标题" name="title">
        <a-input
          v-model:value="formData.title"
          placeholder="请输入显示标题，如 综合型医院"
        />
      </a-form-item>
      <a-form-item label="描述" name="description">
        <a-textarea
          v-model:value="formData.description"
          placeholder="请输入类型描述"
          :rows="3"
        />
      </a-form-item>
      <a-form-item label="主题颜色" name="color">
        <a-input
          v-model:value="formData.color"
          placeholder="请输入颜色值，如 #1890ff"
        >
          <template #prefix>
            <span
              v-if="formData.color"
              class="color-preview"
              :style="{ backgroundColor: formData.color }"
            ></span>
          </template>
        </a-input>
      </a-form-item>
      <a-form-item label="排序" name="sort">
        <a-input-number
          v-model:value="formData.sort"
          :min="0"
          :max="9999"
          class="w-full"
        />
      </a-form-item>
      <a-form-item v-if="formData.id" label="状态" name="status">
        <a-radio-group v-model:value="formData.status">
          <a-radio :value="1">启用</a-radio>
          <a-radio :value="0">禁用</a-radio>
        </a-radio-group>
      </a-form-item>
    </a-form>
  </Modal>
</template>

<style scoped>
.color-preview {
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 4px;
  margin-right: 4px;
}
</style>
