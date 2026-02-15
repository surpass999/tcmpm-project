<script lang="ts" setup>
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import { computed, reactive, ref, watch } from 'vue';
import { PlusOutlined } from '@vben/icons';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import {
  createIndicator,
  getIndicator,
  updateIndicator,
} from '#/api/declare/indicator';
import { $t } from '#/locales';
import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

// 选项定义项
interface OptionItem {
  value: string;
  label: string;
  key: string;
}

const emit = defineEmits(['success']);

const formData = ref<DeclareIndicatorApi.IndicatorSaveParams>();
const formRef = ref<any>();

// 当前值类型，用于控制选项定义的显示
const currentValueType = ref<number>(1);

// 是否显示选项定义（单选或多选时显示）
const showValueOptions = computed(() => {
  return currentValueType.value === 6 || currentValueType.value === 7 || currentValueType.value === 10;
});

// 处理值类型变化
const handleValueTypeChange = (value: number) => {
  currentValueType.value = value;
};

// 选项列表
const optionsList = reactive<OptionItem[]>([]);

const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['指标'])
    : $t('ui.actionTitle.create', ['指标']);
});

// 添加选项
const handleAddOption = () => {
  const key = `option_${Date.now()}`;
  optionsList.push({ value: '', label: '', key });
};

// 删除选项
const handleRemoveOption = (key: string) => {
  const index = optionsList.findIndex((item) => item.key === key);
  if (index > -1) {
    optionsList.splice(index, 1);
  }
};

// 将选项列表转换为JSON字符串
const convertOptionsToJson = () => {
  if (optionsList.length === 0) {
    formData.value!.valueOptions = '';
    return;
  }
  const validOptions = optionsList.filter(
    (opt) => opt.value && opt.label,
  );
  formData.value!.valueOptions = JSON.stringify(validOptions);
};

// 将JSON字符串解析为选项列表
const parseJsonToOptions = (jsonStr: string) => {
  optionsList.length = 0;
  if (!jsonStr) return;
  try {
    const parsed = JSON.parse(jsonStr);
    if (Array.isArray(parsed)) {
      parsed.forEach((item) => {
        optionsList.push({
          value: String(item.value),
          label: item.label,
          key: `option_${Date.now()}_${Math.random()}`,
        });
      });
    }
  } catch {
    // 解析失败，忽略
  }
};

// 表单字段配置
const formRules = {
  indicatorCode: [{ required: true, message: '请输入指标代号' }],
  indicatorName: [{ required: true, message: '请输入指标名称' }],
  category: [{ required: true, message: '请选择指标分类' }],
  valueType: [{ required: true, message: '请选择值类型' }],
};

// 表单布局
const formLayout = {
  labelCol: { span: 6 },
  wrapperCol: { span: 16 },
};

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formRef.value?.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    // 转换选项为JSON
    convertOptionsToJson();
    // 提交表单
    const data = formData.value as DeclareIndicatorApi.IndicatorSaveParams;
    try {
      await (formData.value?.id ? updateIndicator(data) : createIndicator(data));
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
    const data = modalApi.getData<DeclareIndicatorApi.IndicatorSaveParams>();
    if (!data || !data.id) {
      formData.value = {
        id: undefined,
        indicatorCode: '',
        indicatorName: '',
        unit: '',
        category: 1,
        valueType: 1,
        valueOptions: '',
        isRequired: false,
        sort: 0,
        showInList: true,
        businessType: '',
        logicRule: '',
        calculationRule: '',
        childrenIndicatorCodes: '',
      };
      // 初始化值类型
      currentValueType.value = 1;
      // 清空选项列表
      optionsList.length = 0;
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getIndicator(data.id);
      // 更新值类型
      currentValueType.value = formData.value?.valueType || 1;
      // 解析选项
      parseJsonToOptions(formData.value?.valueOptions || '');
    } finally {
      modalApi.unlock();
    }
  },
});

// 监听值类型变化，控制选项定义的显示
watch(
  () => currentValueType.value,
  (newVal) => {
    // 清空选项（当值类型不是单选或多选时）
    if (newVal !== 6 && newVal !== 7) {
      optionsList.length = 0;
    }
  },
);
</script>

<template>
  <Modal :title="getTitle" class="w-2/3">
    <a-form
      v-if="formData"
      ref="formRef"
      :model="formData"
      :rules="formRules"
      v-bind="formLayout"
      class="mx-4"
    >
      <a-form-item label="指标代号" name="indicatorCode">
        <a-input
          v-model:value="formData.indicatorCode"
          placeholder="请输入指标代号，如 101、20101"
        />
      </a-form-item>
      <a-form-item label="指标名称" name="indicatorName">
        <a-input
          v-model:value="formData.indicatorName"
          placeholder="请输入指标名称"
        />
      </a-form-item>
      <a-form-item label="计量单位" name="unit">
        <a-input
          v-model:value="formData.unit"
          placeholder="如：人、万元、次"
        />
      </a-form-item>
      <a-form-item label="指标分类" name="category">
        <a-select
          v-model:value="formData.category"
          placeholder="请选择指标分类"
        >
          <a-select-option
            v-for="item in getDictOptions(DICT_TYPE.DECLARE_INDICATOR_CATEGORY, 'number')"
            :key="item.value"
            :value="Number(item.value)"
          >
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="值类型" name="valueType">
        <a-select
          v-model:value="formData.valueType"
          placeholder="请选择值类型"
          @change="handleValueTypeChange"
        >
          <a-select-option
            v-for="item in getDictOptions(DICT_TYPE.DECLARE_INDICATOR_VALUE_TYPE, 'number')"
            :key="item.value"
            :value="Number(item.value)"
          >
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <!-- 选项定义 - 直接在值类型下方 -->
      <a-form-item v-if="showValueOptions" label="选项定义">
        <div class="space-y-2 w-full">
          <div
            v-for="option in optionsList"
            :key="option.key"
            class="flex items-center gap-2"
          >
            <a-input
              v-model:value="option.value"
              placeholder="值"
              class="w-32"
            />
            <a-input
              v-model:value="option.label"
              placeholder="标签"
              class="flex-1"
            />
            <a-button
              type="text"
              danger
              @click="handleRemoveOption(option.key)"
            >
              删除
            </a-button>
          </div>
          <a-button type="dashed" class="w-full" @click="handleAddOption">
            <PlusOutlined />
            添加选项
          </a-button>
        </div>
      </a-form-item>
      <a-form-item label="是否必填" name="isRequired">
        <a-radio-group v-model:value="formData.isRequired">
          <a-radio :value="true">是</a-radio>
          <a-radio :value="false">否</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="最小值" name="minValue">
        <a-input-number
          v-model:value="formData.minValue"
          placeholder="请输入最小值"
          class="w-full"
        />
      </a-form-item>
      <a-form-item label="最大值" name="maxValue">
        <a-input-number
          v-model:value="formData.maxValue"
          placeholder="请输入最大值"
          class="w-full"
        />
      </a-form-item>
      <a-form-item label="适用项目类型" name="projectType">
        <a-select
          v-model:value="formData.projectType"
          placeholder="请选择适用项目类型"
          allow-clear
        >
          <a-select-option
            v-for="item in getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number')"
            :key="item.value"
            :value="Number(item.value)"
          >
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="适用业务类型" name="businessType">
        <a-input
          v-model:value="formData.businessType"
          placeholder="如：filing、project、process"
        />
      </a-form-item>
      <a-form-item label="逻辑校验关系" name="logicRule">
        <a-textarea
          v-model:value="formData.logicRule"
          placeholder="如：201>=20101、802>=80201+80202"
          :rows="2"
        />
      </a-form-item>
      <a-form-item label="计算公式" name="calculationRule">
        <a-input
          v-model:value="formData.calculationRule"
          placeholder="如：401=系统覆盖名老中医工作室数"
        />
      </a-form-item>
      <a-form-item label="列表显示" name="showInList">
        <a-radio-group v-model:value="formData.showInList">
          <a-radio :value="true">是</a-radio>
          <a-radio :value="false">否</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="排序" name="sort">
        <a-input-number
          v-model:value="formData.sort"
          placeholder="请输入排序"
          :min="0"
          :max="9999"
          class="w-full"
        />
      </a-form-item>
    </a-form>
  </Modal>
</template>
