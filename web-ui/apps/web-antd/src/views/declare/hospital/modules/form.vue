<script lang="ts" setup>
import type { DeclareHospitalApi } from '#/api/declare/hospital';

import { computed, ref } from 'vue';

import dayjs from 'dayjs';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createHospital, getHospital, updateHospital } from '#/api/declare/hospital';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<DeclareHospitalApi.Hospital>();
const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['医院'])
    : $t('ui.actionTitle.create', ['医院']);
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
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    const data = (await formApi.getValues()) as DeclareHospitalApi.Hospital;
    try {
      await (formData.value?.id ? updateHospital(data) : createHospital(data));
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
    const data = modalApi.getData<DeclareHospitalApi.Hospital>();
    if (!data || !data.id) {
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getHospital(data.id);
      // 许可证有效期是字符串，需转为 dayjs 对象才能被 ADatePicker 正确渲染
      const formValues = { ...formData.value };
      if (formValues.medicalLicenseExpire) {
        formValues.medicalLicenseExpire = dayjs(formValues.medicalLicenseExpire);
      }
      await formApi.setValues(formValues);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-[800px]">
    <Form class="mx-4" />
  </Modal>
</template>
