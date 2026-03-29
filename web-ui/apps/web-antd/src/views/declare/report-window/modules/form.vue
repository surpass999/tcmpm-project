<script lang="ts" setup>
import type { ReportWindow } from '#/api/declare/report-window';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenForm } from '#/adapter/form';
import {
  createReportWindow,
  getReportWindowList,
  updateReportWindow,
} from '#/api/declare/report-window';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<Partial<ReportWindow>>();

const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['时间窗口'])
    : $t('ui.actionTitle.create', ['时间窗口']);
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
    const data: any = await formApi.getValues();
    if (data.reportYear) {
      data.reportYear = parseInt(data.reportYear, 10);
    }
    try {
      if (formData.value?.id) {
        await updateReportWindow(formData.value.id, data);
      } else {
        await createReportWindow(data);
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
    const data = modalApi.getData<ReportWindow>();

    if (!data || !data.id) {
      return;
    }

    modalApi.lock();
    try {
      const list = await getReportWindowList();
      const win = list.find((item) => item.id === data.id);
      if (win) {
        formData.value = win;
        await formApi.setValues({
          ...win,
          reportYear: String(win.reportYear),
          windowStart: win.windowStart
            ? dayjs(win.windowStart).format('YYYY-MM-DD HH:mm:ss')
            : null,
          windowEnd: win.windowEnd
            ? dayjs(win.windowEnd).format('YYYY-MM-DD HH:mm:ss')
            : null,
        });
      }
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-[600px]">
    <Form class="mx-4" />
  </Modal>
</template>
