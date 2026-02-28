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

// 值类型配置项
interface ConfigItem {
  key: string;
  label: string;
  type: 'text' | 'number' | 'select' | 'switch';
  placeholder?: string;
  options?: { value: string; label: string }[];
}

const emit = defineEmits(['success']);

const formData = ref<DeclareIndicatorApi.IndicatorSaveParams>();
const formRef = ref<any>();

// 当前值类型，用于控制选项定义的显示
const currentValueType = ref<number>(1);

// 是否显示选项定义（单选、多选、单选下拉、多选下拉时显示）
const showValueOptions = computed(() => {
  return [6, 7, 10, 11].includes(currentValueType.value);
});

// 是否显示最小/最大值（数字类型显示）
const showMinMaxValue = computed(() => {
  return currentValueType.value === 1;
});

// 是否显示计量单位（数字类型显示）
const showUnit = computed(() => {
  return currentValueType.value === 1;
});

// 处理值类型变化
const handleValueTypeChange = (value: number) => {
  currentValueType.value = value;
};

// 选项列表
const optionsList = reactive<OptionItem[]>([]);

// 扩展配置对象
const extraConfigData = reactive<Record<string, any>>({});

// 值类型配置显示（所有值类型1-11都显示配置区域）
const showValueTypeConfig = computed(() => {
  return currentValueType.value >= 1 && currentValueType.value <= 11;
});

// 根据值类型获取配置项
const getValueTypeConfigItems = (): ConfigItem[] => {
  switch (currentValueType.value) {
    case 1: // 数字
      return [
        { key: 'precision', label: '精度(小数位)', type: 'number', placeholder: '如：0、1、2' },
        { key: 'prefix', label: '前缀', type: 'text', placeholder: '如：¥、$' },
        { key: 'suffix', label: '后缀', type: 'text', placeholder: '如：万元、人、个' },
      ];
    case 2: // 字符串
      return [
        { key: 'maxLength', label: '最大字符数', type: 'number', placeholder: '请输入最大字符数' },
        { key: 'pattern', label: '正则校验', type: 'text', placeholder: '如：^1[3-9]\\d{9}$' },
      ];
    case 3: // 布尔
      return [
        { key: 'trueLabel', label: '是 的标签', type: 'text', placeholder: '如：是、有' },
        { key: 'falseLabel', label: '否 的标签', type: 'text', placeholder: '如：否、无' },
      ];
    case 4: // 日期
      return [
        { key: 'format', label: '日期格式', type: 'select', options: [
          { value: 'YYYY-MM-DD', label: '年-月-日' },
          { value: 'YYYY-MM-DD HH:mm', label: '年-月-日 时:分' },
          { value: 'YYYY-MM', label: '年-月' },
        ]},
      ];
    case 5: // 长文本
      return [
        { key: 'maxLength', label: '最大字符数', type: 'number', placeholder: '请输入最大字符数' },
        { key: 'richText', label: '是否富文本', type: 'switch' },
      ];
    case 6: // 单选
      return [
        { key: 'layout', label: '布局方式', type: 'select', options: [
          { value: 'vertical', label: '纵向' },
          { value: 'horizontal', label: '横向' },
        ]},
      ];
    case 7: // 多选
      return [
        { key: 'layout', label: '布局方式', type: 'select', options: [
          { value: 'vertical', label: '纵向' },
          { value: 'horizontal', label: '横向' },
        ]},
        { key: 'minSelect', label: '最少选择数', type: 'number', placeholder: '请输入最少选择数' },
        { key: 'maxSelect', label: '最多选择数', type: 'number', placeholder: '请输入最多选择数' },
      ];
    case 8: // 日期区间
      return [
        { key: 'format', label: '日期格式', type: 'select', options: [
          { value: 'YYYY-MM-DD', label: '年-月-日' },
          { value: 'YYYY-MM-DD HH:mm', label: '年-月-日 时:分' },
        ]},
        { key: 'minDays', label: '最小间隔天数', type: 'number', placeholder: '请输入最小间隔天数' },
        { key: 'maxDays', label: '最大间隔天数', type: 'number', placeholder: '请输入最大间隔天数' },
      ];
    case 9: // 文件上传
      return [
        { key: 'maxCount', label: '最大文件数', type: 'number', placeholder: '请输入最大文件数' },
        { key: 'maxSize', label: '单文件最大大小', type: 'number', placeholder: '请输入字节数，如10485760' },
        { key: 'accept', label: '允许的文件类型', type: 'text', placeholder: '如：pdf,jpg,png,doc,docx' },
      ];
    case 10: // 单选下拉
      return [
        { key: 'showSearch', label: '是否支持搜索', type: 'switch' },
      ];
    case 11: // 多选下拉
      return [
        { key: 'showSearch', label: '是否支持搜索', type: 'switch' },
        { key: 'minSelect', label: '最少选择数', type: 'number', placeholder: '请输入最少选择数' },
        { key: 'maxSelect', label: '最多选择数', type: 'number', placeholder: '请输入最多选择数' },
      ];
    default:
      return [];
  }
};

// 将扩展配置转换为JSON字符串
const convertExtraConfigToJson = () => {
  if (!formData.value) return;
  const config: Record<string, any> = {};
  const items = getValueTypeConfigItems();
  items.forEach((item) => {
    if (extraConfigData[item.key] !== undefined && extraConfigData[item.key] !== '') {
      config[item.key] = extraConfigData[item.key];
    }
  });
  formData.value.extraConfig = Object.keys(config).length > 0 ? JSON.stringify(config) : '';
};

// 将JSON字符串解析为扩展配置
const parseJsonToExtraConfig = (jsonStr: string) => {
  // 清空现有配置
  Object.keys(extraConfigData).forEach((key) => {
    delete extraConfigData[key];
  });
  if (!jsonStr) return;
  try {
    const parsed = JSON.parse(jsonStr);
    Object.assign(extraConfigData, parsed);
  } catch {
    // 解析失败，忽略
  }
};

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
    try {
      await formRef.value?.validate();
    } catch (error) {
      message.error('请完善表单信息');
      return;
    }
    modalApi.lock();
    // 转换选项为JSON
    convertOptionsToJson();
    // 转换扩展配置为JSON
    convertExtraConfigToJson();
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
        projectType: 0,
        extraConfig: '',
      };
      // 初始化值类型
      currentValueType.value = 1;
      // 清空选项列表
      optionsList.length = 0;
      // 初始化扩展配置
      formData.value.extraConfig = '';
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getIndicator(data.id);
      // 更新值类型
      currentValueType.value = formData.value?.valueType || 1;
      // 解析选项
      parseJsonToOptions(formData.value?.valueOptions || '');
      // 解析扩展配置
      parseJsonToExtraConfig(formData.value?.extraConfig || '');
    } finally {
      modalApi.unlock();
    }
  },
});

// 监听值类型变化，控制选项定义的显示
watch(
  () => currentValueType.value,
  (newVal) => {
    // 清空选项（当值类型不是单选/多选/单选下拉/多选下拉时）
    if (![6, 7, 10, 11].includes(newVal)) {
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
          :placeholder="showUnit ? '如：人、万元、次' : '用于展示前缀/后缀'"
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
      <!-- 值类型配置 - 根据不同值类型显示不同配置项 -->
      <a-form-item
        v-if="showValueTypeConfig && getValueTypeConfigItems().length > 0"
        label="值类型配置"
      >
        <div class="space-y-3 w-full">
          <div
            v-for="configItem in getValueTypeConfigItems()"
            :key="configItem.key"
            class="flex items-center"
          >
            <span class="w-28 flex-shrink-0">{{ configItem.label }}</span>
            <!-- 输入框 -->
            <a-input
              v-if="configItem.type === 'text'"
              v-model:value="extraConfigData[configItem.key]"
              :placeholder="configItem.placeholder"
              class="flex-1"
            />
            <!-- 数字输入框 -->
            <a-input-number
              v-else-if="configItem.type === 'number'"
              v-model:value="extraConfigData[configItem.key]"
              :placeholder="configItem.placeholder"
              class="flex-1"
            />
            <!-- 下拉选择 -->
            <a-select
              v-else-if="configItem.type === 'select'"
              v-model:value="extraConfigData[configItem.key]"
              placeholder="请选择"
              class="flex-1"
            >
              <a-select-option
                v-for="opt in configItem.options"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </a-select-option>
            </a-select>
            <!-- 开关 -->
            <a-switch
              v-else-if="configItem.type === 'switch'"
              v-model:checked="extraConfigData[configItem.key]"
            />
          </div>
        </div>
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
      <a-form-item v-if="showMinMaxValue" label="最小值" name="minValue">
        <a-input-number
          v-model:value="formData.minValue"
          placeholder="请输入最小值"
          class="w-full"
        />
      </a-form-item>
      <a-form-item v-if="showMinMaxValue" label="最大值" name="maxValue">
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
        >
          <a-select-option :value="0">全部项目</a-select-option>
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
          placeholder="如：202 / 201 * 100、20201 / 200 * 100"
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
