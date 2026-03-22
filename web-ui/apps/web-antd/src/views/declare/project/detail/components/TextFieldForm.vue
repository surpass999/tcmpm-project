<script lang="ts" setup>
import { computed } from 'vue';

import { Empty, Form, Input, DatePicker } from 'ant-design-vue';

const AEmpty = Empty;

import type { TextFieldConfig } from '#/api/declare/project';

interface Props {
  modelValue: Record<string, any>;
  fields: TextFieldConfig[];
  disabled?: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, any>];
}>();

const values = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

// 处理值变化
const handleValueChange = (field: TextFieldConfig, value: any) => {
  values.value = { ...values.value, [field.fieldCode]: value };
};
</script>

<template>
  <div class="text-field-form">
    <Form.Item
      v-for="field in fields"
      :key="field.fieldCode"
      :label="field.fieldName"
      :required="field.isRequired"
    >
      <!-- 多行文本 -->
      <Input.TextArea
        v-if="field.fieldType === 2"
        :value="values[field.fieldCode]"
        :placeholder="`请输入${field.fieldName}`"
        :rows="4"
        :maxlength="field.maxLength"
        show-count
        :disabled="props.disabled"
        @update:value="handleValueChange(field, $event)"
      />

      <!-- 日期类型 -->
      <DatePicker
        v-else-if="field.fieldType === 3"
        :value="values[field.fieldCode]"
        valueFormat="YYYY-MM-DD"
        :placeholder="`请选择${field.fieldName}`"
        :disabled="props.disabled"
        style="width: 100%"
        @update:value="handleValueChange(field, $event)"
      />

      <!-- 单行文本（默认） -->
      <Input
        v-else
        :value="values[field.fieldCode]"
        :placeholder="`请输入${field.fieldName}`"
        :maxlength="field.maxLength"
        show-count
        :disabled="props.disabled"
        @update:value="handleValueChange(field, $event)"
      />
    </Form.Item>

    <!-- 无字段时的提示 -->
    <AEmpty v-if="!fields?.length" description="暂无文本字段配置" />
  </div>
</template>

<style lang="scss" scoped>
.text-field-form {
  padding: 4px 0;

  :deep(.ant-form-item) {
    margin-bottom: 20px;

    .ant-form-item-label {
      padding-bottom: 6px;

      label {
        font-weight: 600;
        color: #303133;
        font-size: 13px;
      }
    }
  }

  // 输入框样式优化
  :deep(.ant-input),
  :deep(.ant-picker) {
    border-radius: 6px;
    border-color: #d9d9d9;
    transition: all 0.3s;

    &:hover {
      border-color: hsl(var(--primary));
    }

    &:focus,
    &.ant-input-focused {
      border-color: hsl(var(--primary));
      box-shadow: 0 0 0 2px hsl(var(--primary) / 10%);
    }
  }

  // 文本域样式
  :deep(.ant-input-textarea) {
    .ant-input {
      border-radius: 6px;
    }
  }
}
</style>
