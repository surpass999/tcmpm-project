<script lang="ts" setup>
import type { BpmBusinessTypeApi } from '#/api/bpm/business-type';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createBusinessType,
  getBusinessType,
  updateBusinessType,
} from '#/api/bpm/business-type';
import { $t } from '#/locales';

import { useModalFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<BpmBusinessTypeApi.BusinessTypeSaveParams>();
const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['业务类型'])
    : $t('ui.actionTitle.create', ['业务类型']);
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    formItemClass: 'col-span-2',
    labelWidth: 100,
  },
  layout: 'horizontal',
  schema: useModalFormSchema(),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    // 提交表单
    const data = (await formApi.getValues()) as BpmBusinessTypeApi.BusinessTypeSaveParams;
    try {
      await (formData.value?.id ? updateBusinessType(data) : createBusinessType(data));
      // 关闭并提示
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
    // 加载数据
    const data = modalApi.getData<BpmBusinessTypeApi.BusinessTypeSaveParams>();
    if (!data || !data.id) {
      formData.value = {
        id: undefined,
        businessType: '',
        businessName: '',
        processDefinitionKey: '',
        processCategory: '',
        description: '',
        enabled: 1,
        sort: 0,
      };
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getBusinessType(data.id);
      // 设置到 values
      await formApi.setValues(formData.value);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-1/2">
    <Form class="mx-4" />
  </Modal>
</template>
