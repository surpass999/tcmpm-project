<script lang="ts" setup>
import { computed } from 'vue';

import { Form, Input, InputNumber, DatePicker, Select, Radio, Upload, Row, Col, Empty, message, Button } from 'ant-design-vue';

import type { FileType } from 'ant-design-vue/es/upload/interface';

const AEmpty = Empty;

import { PlusOutlined } from '@vben/icons';

import { uploadFile } from '#/api/infra/file';

import type { IndicatorFormItem } from '#/api/declare/project';

interface Props {
  modelValue: Record<string, any>;
  indicators: IndicatorFormItem[];
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

// 按排序字段排序指标
const sortedIndicators = computed(() => {
  return [...(props.indicators || [])].sort((a, b) => (a.sort || 0) - (b.sort || 0));
});

// 根据值类型返回不同的栅格宽度
const getSpan = (indicator: IndicatorFormItem) => {
  if ([3, 4, 7, 8].includes(indicator.valueType)) {
    return 8; // 单选/日期/年/季度 - 3个一行
  }
  if ([1, 2, 5].includes(indicator.valueType)) {
    return 12; // 数值/文本/多行文本 - 2个一行
  }
  return 24; // 其他类型 - 1个一行
};

// 获取绑定值的key
const getBindKey = (indicator: IndicatorFormItem) => `indicator_${indicator.indicatorId}`;

// 处理值变化
const handleValueChange = (indicator: IndicatorFormItem, value: any) => {
  const key = getBindKey(indicator);
  values.value = { ...values.value, [key]: value };
};

// 上传文件变化处理
// 上传文件
const handleFileUpload = async (indicator: IndicatorFormItem, file: FileType) => {
  try {
    const result = await uploadFile({
      file: file as any,
      directory: 'declare/indicator',
    });

    const fileId = result.id || result;
    const key = getBindKey(indicator);
    values.value = { ...values.value, [key]: fileId };
    message.success('上传成功');
  } catch (error) {
    console.error('上传失败:', error);
    message.error('上传失败');
  }
  return false; // 阻止默认上传行为
};

// 文件变化（保留兼容性，但推荐使用 before-upload）
const handleFileChange = (indicator: IndicatorFormItem, info: any) => {
  if (info.file.status === 'done') {
    const response = info.file.response;
    if (response?.code === 0) {
      const fileId = response.data?.id || response.data;
      const key = getBindKey(indicator);
      values.value = { ...values.value, [key]: fileId };
      message.success('上传成功');
    } else {
      message.error(response?.msg || '上传失败');
    }
  } else if (info.file.status === 'error') {
    message.error('上传失败');
  }
};
</script>

<template>
  <div class="indicator-form">
    <Row :gutter="[16, 0]">
      <Col
        v-for="indicator in sortedIndicators"
        :key="indicator.indicatorId"
        :span="getSpan(indicator)"
      >
        <Form.Item
          :label="indicator.indicatorName"
          :required="indicator.isRequired"
          class="indicator-form-item"
        >
          <template #label>
            {{ indicator.indicatorName }}
            <span v-if="indicator.unit" class="unit-label">({{ indicator.unit }})</span>
          </template>

          <!-- 数值类型 -->
          <InputNumber
            v-if="indicator.valueType === 1"
            :value="values[getBindKey(indicator)]"
            :placeholder="indicator.fillRequire || '请输入'"
            :min="0"
            :precision="2"
            :disabled="props.disabled"
            style="width: 100%"
            @update:value="handleValueChange(indicator, $event)"
          />

          <!-- 文本类型 -->
          <Input
            v-else-if="indicator.valueType === 2"
            :value="values[getBindKey(indicator)]"
            :placeholder="indicator.fillRequire || '请输入'"
            :disabled="props.disabled"
            @update:value="handleValueChange(indicator, $event)"
          />

          <!-- 单选类型 -->
          <Radio.Group
            v-else-if="indicator.valueType === 3"
            :value="values[getBindKey(indicator)]"
            :disabled="props.disabled"
            @update:value="handleValueChange(indicator, $event)"
          >
            <Radio :value="1">是</Radio>
            <Radio :value="0">否</Radio>
          </Radio.Group>

          <!-- 日期类型 -->
          <DatePicker
            v-else-if="indicator.valueType === 4"
            :value="values[getBindKey(indicator)]"
            valueFormat="YYYY-MM-DD"
            :placeholder="indicator.fillRequire || '请选择'"
            :disabled="props.disabled"
            style="width: 100%"
            @update:value="handleValueChange(indicator, $event)"
          />

          <!-- 多行文本 -->
          <Input.TextArea
            v-else-if="indicator.valueType === 5"
            :value="values[getBindKey(indicator)]"
            :placeholder="indicator.fillRequire || '请输入'"
            :rows="3"
            :disabled="props.disabled"
            @update:value="handleValueChange(indicator, $event)"
          />

          <!-- 多选类型 -->
          <Select
            v-else-if="indicator.valueType === 6"
            :value="values[getBindKey(indicator)]"
            mode="multiple"
            :placeholder="indicator.fillRequire || '请选择'"
            :disabled="props.disabled"
            @update:value="handleValueChange(indicator, $event)"
          >
            <Select.Option value="1">选项1</Select.Option>
            <Select.Option value="2">选项2</Select.Option>
          </Select>

          <!-- 年份类型 -->
          <DatePicker.Picker
            v-else-if="indicator.valueType === 7"
            :value="values[getBindKey(indicator)]"
            valueFormat="YYYY"
            picker="year"
            :placeholder="indicator.fillRequire || '请选择'"
            :disabled="props.disabled"
            style="width: 100%"
            @update:value="handleValueChange(indicator, $event)"
          />

          <!-- 季度类型 -->
          <Select
            v-else-if="indicator.valueType === 8"
            :value="values[getBindKey(indicator)]"
            :placeholder="indicator.fillRequire || '请选择'"
            :disabled="props.disabled"
            @update:value="handleValueChange(indicator, $event)"
          >
            <Select.Option :value="1">第一季度</Select.Option>
            <Select.Option :value="2">第二季度</Select.Option>
            <Select.Option :value="3">第三季度</Select.Option>
            <Select.Option :value="4">第四季度</Select.Option>
          </Select>

          <!-- 文件上传类型 -->
          <Upload
            v-else-if="indicator.valueType === 9"
            :before-upload="(file: FileType) => handleFileUpload(indicator, file)"
            :file-list="
              values[getBindKey(indicator)]
                ? [
                    {
                      uid: '-1',
                      name: '附件',
                      status: 'done',
                      url: values[getBindKey(indicator)],
                    },
                  ]
                : []
            "
            :disabled="props.disabled"
            :max-count="1"
          >
            <Button :disabled="props.disabled">
              <PlusOutlined />
              上传
            </Button>
          </Upload>

          <!-- 提示文字 -->
          <div v-if="indicator.fillRequire && !props.disabled" class="indicator-tip">
            {{ indicator.fillRequire }}
          </div>
        </Form.Item>
      </Col>
    </Row>

    <!-- 无指标时的提示 -->
    <AEmpty v-if="!sortedIndicators?.length" description="暂无指标配置" />
  </div>
</template>

<style lang="scss" scoped>
.indicator-form {
  padding: 4px 0;
}

.indicator-form-item {
  margin-bottom: 20px;

  :deep(.ant-form-item-label) {
    padding-bottom: 6px;

    label {
      font-weight: 600;
      color: #303133;
      font-size: 13px;
    }
  }

  // 输入框样式优化
  :deep(.ant-input),
  :deep(.ant-input-number),
  :deep(.ant-select-selector),
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

  // 单选框样式
  :deep(.ant-radio-wrapper) {
    margin-right: 16px;
  }

  .unit-label {
    font-weight: normal;
    color: #8c8c8c;
    margin-left: 4px;
    font-size: 12px;
  }
}

.indicator-tip {
  color: #8c8c8c;
  font-size: 12px;
  margin-top: 6px;
}
</style>
