<script setup lang="ts">
/**
 * 文件上传输入组件
 * 使用 Vue 3 v-model 语法
 */

import { computed } from 'vue';
import { IconifyIcon } from '@vben/icons';
import { Upload } from 'ant-design-vue';
import type { UploadFile } from 'ant-design-vue/es/upload/interface';

interface Props {
  modelValue?: UploadFile[];
  disabled?: boolean;
  accept?: string;
  maxCount?: number;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  disabled: false,
  maxCount: 5,
});

const emit = defineEmits<{
  'update:modelValue': [value: UploadFile[]];
}>();

const fileList = computed(() => props.modelValue || []);

function handleRemove(file: UploadFile) {
  const newList = fileList.value.filter((f) => f.uid !== file.uid);
  emit('update:modelValue', newList);
}
</script>

<template>
  <div class="file-upload-wrapper">
    <div v-if="fileList.length > 0" class="file-list">
      <div
        v-for="(file, index) in fileList"
        :key="index"
        class="file-item"
      >
        <IconifyIcon icon="lucide:file-text" class="file-icon" />
        <span class="file-name" :title="file.name">{{ file.name }}</span>
        <button
          v-if="!disabled"
          type="button"
          class="file-delete-btn"
          @click="handleRemove(file)"
        >
          <IconifyIcon icon="lucide:x" />
        </button>
      </div>
    </div>
    <Upload
      v-if="!disabled"
      :before-upload="() => false"
      :show-upload-list="false"
      :disabled="fileList.length >= maxCount"
      :accept="accept ? '.' + accept.replace(/,/g, ',.') : undefined"
      multiple
    >
      <a-button type="dashed" :disabled="fileList.length >= maxCount">
        <IconifyIcon icon="lucide:plus" />
        上传文件
      </a-button>
    </Upload>
    <div v-if="!disabled" class="upload-hint">
      <span v-if="accept">支持 {{ accept }}</span>
      <span v-if="accept && maxCount">，</span>
      <span v-if="maxCount">最多 {{ maxCount }} 个</span>
      <span class="upload-count">({{ fileList.length }}/{{ maxCount }})</span>
    </div>
  </div>
</template>

<style scoped>
.file-upload-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-hint {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0;
}

.upload-count {
  color: hsl(var(--primary));
  font-weight: 500;
}

.file-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  max-width: 200px;
}

.file-icon {
  flex-shrink: 0;
  color: hsl(var(--primary));
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: hsl(var(--foreground));
}

.file-delete-btn {
  flex-shrink: 0;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  color: hsl(var(--destructive));
  display: flex;
  align-items: center;
  opacity: 0.7;
  transition: opacity 0.15s;
}

.file-delete-btn:hover {
  opacity: 1;
}

.w-full {
  width: 100%;
}
</style>
