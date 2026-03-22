<script lang="ts" setup>
import type { DeclarePolicyApi } from '#/api/declare/policy';

import { ref } from 'vue';

import { useVbenForm, useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';
import type { UploadFile } from 'ant-design-vue';
import { Upload } from 'ant-design-vue';

import { createPolicy, getPolicy, updatePolicy } from '#/api/declare/policy';
import { uploadFile } from '#/api/infra/file';

import { useFormSchema } from '../data';

const emit = defineEmits<{
  success: [];
}>();

const isEdit = ref(false);
const loading = ref(false);

// 附件上传相关
const fileList = ref<UploadFile[]>([]);

// 解析存储的附件列表（attachmentUrls 是 string[]）
function parseStoredFileList(value: string[] | undefined): UploadFile[] {
  if (!value || !Array.isArray(value)) return [];
  return value.map((url, index) => ({
    name: `附件${index + 1}`,
    uid: String(index),
    status: 'done',
    url: url,
  }));
}

// 上传前处理
async function handleBeforeUpload(file: UploadFile) {
  try {
    const result = await uploadFile({
      file: file as unknown as File,
      directory: 'declare/policy',
    });
    // 直接更新当前文件的 url，不要 push 新文件（Upload 组件会自动处理）
    file.url = String(result);
    file.status = 'done';
    return false; // 阻止默认上传行为
  } catch (error) {
    console.error('上传失败:', error);
    message.error('上传失败');
    return false;
  }
}

// 删除附件
function handleRemove(file: UploadFile) {
  fileList.value = fileList.value.filter(f => f.uid !== file.uid);
}

const [Form, formApi] = useVbenForm({
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formApi.resetForm();
      fileList.value = [];
      return;
    }

    const data = modalApi.getData<Record<string, unknown>>();
    const id = data?.id as number | undefined;
    isEdit.value = !!id;

    if (id) {
      loading.value = true;
      try {
        const detail = await getPolicy(id);
        // 处理适用项目类型（逗号分隔转数组）
        if (detail.targetProjectTypes) {
          detail.targetProjectTypes = detail.targetProjectTypes.split(',').map(Number) as unknown as string;
        }
        formApi.setValues(detail);
        // 加载附件列表
        fileList.value = parseStoredFileList(detail.attachmentIds);
      } finally {
        loading.value = false;
      }
    }
  },
  async onConfirm() {
    const valid = await formApi.validate();
    if (!valid) return;

    const values = await formApi.getValues();
    // 处理适用项目类型（数组转逗号分隔）
    if (values.targetProjectTypes && Array.isArray(values.targetProjectTypes)) {
      values.targetProjectTypes = values.targetProjectTypes.join(',');
    }
    // 处理附件 - 转换为路径字符串数组
    values.attachmentUrls = fileList.value
      .filter(f => f.status === 'done' && f.url)
      .map(f => f.url);

    loading.value = true;
    try {
      if (isEdit.value) {
        await updatePolicy(values as DeclarePolicyApi.Policy);
        message.success('更新成功');
      } else {
        await createPolicy(values as DeclarePolicyApi.Policy);
        message.success('创建成功');
      }
      modalApi.close();
      emit('success');
    } finally {
      loading.value = false;
    }
  },
});
</script>

<template>
  <Modal
    :title="isEdit ? '编辑政策通知' : '新增政策通知'"
    :loading="loading"
    class="w-[80%]"
  >
    <Form class="policy-form" />

    <!-- 附件上传 -->
    <a-divider>附件上传</a-divider>
    <a-form-item label="附件" name="attachmentUrls">
      <Upload
        v-model:file-list="fileList"
        :before-upload="handleBeforeUpload"
        :remove="handleRemove"
        :max-count="10"
        multiple
      >
        <a-button>上传附件</a-button>
      </Upload>
    </a-form-item>
  </Modal>
</template>

<style scoped>
.policy-form :deep(.ant-select) {
  width: 100% !important;
}
</style>
