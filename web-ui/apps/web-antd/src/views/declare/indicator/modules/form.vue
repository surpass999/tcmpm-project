<script lang="ts" setup>
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import { computed, reactive, ref, watch, nextTick } from 'vue';
import { PlusOutlined } from '@vben/icons';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import {
  createIndicator,
  getIndicator,
  updateIndicator,
} from '#/api/declare/indicator';
import { getIndicatorGroupTreeByProjectType } from '#/api/declare/indicator-group';
import { getProjectTypeSimpleList } from '#/api/declare/project-type';
import { $t } from '#/locales';

// 选项定义项
interface OptionItem {
  value: string;
  label: string;
  key: string;
}

// 动态容器子字段条件配置
interface ShowCondition {
  watchField?: string;
  operator: string;
  value?: any;
}

// 动态容器子字段定义
interface DynamicField {
  key: string;
  fieldCode: string;
  fieldLabel: string;
  fieldType: string;
  required: boolean;
  sort: number;
  options: { value: string; label: string }[];
  maxLength?: number;
  placeholder?: string;
  showSearch?: boolean;
  minSelect?: number;
  maxSelect?: number;
  format?: string;
  layout?: string;
  rows?: number;
  precision?: number;
  prefix?: string;
  suffix?: string;
  showCondition?: ShowCondition;
  defaultValue?: any;
}

// 子字段类型选项
const fieldTypeOptions = [
  { value: 'text', label: '文本输入' },
  { value: 'number', label: '数字输入' },
  { value: 'textarea', label: '多行文本' },
  { value: 'radio', label: '单选' },
  { value: 'checkbox', label: '多选' },
  { value: 'select', label: '下拉单选' },
  { value: 'multiSelect', label: '下拉多选' },
  { value: 'date', label: '日期' },
  { value: 'dateRange', label: '日期区间' },
  { value: 'boolean', label: '开关' },
];

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

// 是否为动态容器类型
const isDynamicContainer = computed(() => {
  return currentValueType.value === 12;
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

// 动态容器子字段列表
const dynamicFields = reactive<DynamicField[]>([]);

// 扩展配置对象
const extraConfigData = reactive<Record<string, any>>({});

// 分组扁平时钟数据（treeDataSimpleMode 更可靠，避免嵌套结构更新时的渲染问题）
const flattenGroupTreeData = ref<{ id: number; pId: number; label: string; value: number }[]>([]);

// 用于强制刷新 tree-select 的 key（解决 groupId 在 treeData 加载完成前已有值导致不回显的问题）
const treeSelectKey = ref(0);

// 项目类型选项（从 API 加载）
const projectTypeOptions = ref<{ label: string; value: number }[]>([]);

// 加载项目类型选项
const loadProjectTypeOptions = async () => {
  try {
    const list = await getProjectTypeSimpleList();
    projectTypeOptions.value = list.map((item) => ({
      label: item.title || item.name,
      value: item.typeValue,
    }));
  } catch {
    projectTypeOptions.value = [];
  }
};

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

// 添加子字段
const handleAddField = () => {
  dynamicFields.push({
    key: `field_${Date.now()}`,
    fieldCode: '',
    fieldLabel: '',
    fieldType: 'text',
    required: false,
    sort: dynamicFields.length + 1,
    options: [],
  });
};

// 删除子字段
const handleRemoveField = (key: string) => {
  const index = dynamicFields.findIndex((item) => item.key === key);
  if (index > -1) {
    dynamicFields.splice(index, 1);
  }
};

// 返回同容器中除自身外的其他字段（用于选择监听目标）
const otherFields = (field: DynamicField): DynamicField[] => {
  return dynamicFields.filter((f) => f.key !== field.key);
};

// 添加条件配置
const handleAddShowCondition = (field: DynamicField) => {
  field.showCondition = { watchField: '', operator: 'eq', value: '' };
};

// 移除条件配置
const handleRemoveShowCondition = (field: DynamicField) => {
  delete field.showCondition;
};

// 根据被监听字段类型返回合适的占位提示
const getConditionValuePlaceholder = (field: DynamicField): string => {
  const watchedField = dynamicFields.find((f) => f.fieldCode === field.showCondition?.watchField);
  if (!watchedField) return '输入比较值';
  switch (watchedField.fieldType) {
    case 'radio':
    case 'select':
      return watchedField.options?.[0]?.value ?? '输入选项值';
    case 'multiSelect':
    case 'checkbox':
      return '输入选项值，多个用逗号分隔';
    case 'number':
      return '输入数字，如 100';
    case 'boolean':
      return '输入 1（开启）或 0（关闭）';
    default:
      return '输入比较值';
  }
};

// 判断被监听字段是否排在当前字段之后（顺序错误会导致运行时拿不到值）
const isWatchFieldAfterCurrent = (field: DynamicField): boolean => {
  if (!field.showCondition?.watchField) return false;
  const watchedIdx = dynamicFields.findIndex((f) => f.fieldCode === field.showCondition?.watchField);
  const currentIdx = dynamicFields.findIndex((f) => f.key === field.key);
  return watchedIdx > currentIdx;
};

// 添加子字段选项
const handleAddFieldOption = (fieldKey: string) => {
  const field = dynamicFields.find((item) => item.key === fieldKey);
  if (field) {
    field.options.push({ value: '', label: '' });
  }
};

// 删除子字段选项
const handleRemoveFieldOption = (fieldKey: string, optionIndex: number) => {
  const field = dynamicFields.find((item) => item.key === fieldKey);
  if (field && optionIndex >= 0 && optionIndex < field.options.length) {
    field.options.splice(optionIndex, 1);
  }
};

// 将选项列表转换为JSON字符串
const convertOptionsToJson = () => {
  // 动态容器类型：序列化子字段定义
  if (currentValueType.value === 12) {
    if (dynamicFields.length === 0) {
      formData.value!.valueOptions = '';
      return;
    }
    const validFields = dynamicFields
      .filter((field) => field.fieldCode && field.fieldLabel && field.fieldType)
      .map((field) => ({
        fieldCode: field.fieldCode,
        fieldLabel: field.fieldLabel,
        fieldType: field.fieldType,
        required: field.required ?? false,
        sort: field.sort ?? 0,
        options: field.options?.filter((opt) => opt.value && opt.label) ?? [],
        maxLength: field.maxLength,
        placeholder: field.placeholder,
        showSearch: field.showSearch,
        minSelect: field.minSelect,
        maxSelect: field.maxSelect,
        format: field.format,
        layout: field.layout,
        rows: field.rows,
        precision: field.precision,
        prefix: field.prefix,
        suffix: field.suffix,
        showCondition: field.showCondition?.watchField
          ? Object.fromEntries(Object.entries({ ...field.showCondition }).filter(([, v]) => v !== undefined))
          : undefined,
        defaultValue: field.defaultValue,
      }));
    formData.value!.valueOptions = JSON.stringify(validFields);
    return;
  }

  // 普通选项类型
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
  dynamicFields.length = 0;
  if (!jsonStr) return;
  try {
    const parsed = JSON.parse(jsonStr);
    if (!Array.isArray(parsed)) return;

    // 判断是动态容器子字段还是普通选项（通过是否有 fieldCode 判断）
    const firstItem = parsed[0];
    if (firstItem && 'fieldCode' in firstItem) {
      // 动态容器子字段定义
      parsed.forEach((item: any) => {
        dynamicFields.push({
          key: `field_${Date.now()}_${Math.random()}`,
          fieldCode: item.fieldCode ?? '',
          fieldLabel: item.fieldLabel ?? '',
          fieldType: item.fieldType ?? 'text',
          required: item.required ?? false,
          sort: item.sort ?? 0,
          options: item.options ?? [],
          maxLength: item.maxLength,
          placeholder: item.placeholder,
          showSearch: item.showSearch,
          minSelect: item.minSelect,
          maxSelect: item.maxSelect,
          format: item.format,
          layout: item.layout,
          rows: item.rows,
          precision: item.precision,
          prefix: item.prefix,
          suffix: item.suffix,
          showCondition: item.showCondition,
          defaultValue: item.defaultValue,
        });
      });
    } else {
      // 普通选项
      parsed.forEach((item: any) => {
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

// 加载分组数据（扁平时钟格式，避免嵌套结构更新时的渲染问题）
const loadGroupTreeData = async (projectType?: number) => {
  try {
    const tree = await getIndicatorGroupTreeByProjectType(projectType);
    // 转换为扁平时钟格式，id/pId/value 都用 number
    const flatten: { id: number; pId: number; label: string; value: number }[] = [];
    tree.forEach((item) => {
      flatten.push({
        id: Number(item.id),
        pId: Number(item.parentId) || 0,
        label: item.groupName,
        value: Number(item.id),
      });
      (item.children || []).forEach((child: any) => {
        flatten.push({
          id: Number(child.id),
          pId: Number(item.id),
          label: child.groupName,
          value: Number(child.id),
        });
      });
    });
    flattenGroupTreeData.value = flatten;
  } catch {
    flattenGroupTreeData.value = [];
  }
};

// 表单字段配置
const formRules = {
  indicatorCode: [{ required: true, message: '请输入指标代号' }],
  indicatorName: [{ required: true, message: '请输入指标名称' }],
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
    // 加载项目类型选项
    await loadProjectTypeOptions();
    const data = modalApi.getData<DeclareIndicatorApi.IndicatorSaveParams>();
    if (!data || !data.id) {
      formData.value = {
        id: undefined,
        indicatorCode: '',
        indicatorName: '',
        unit: '',
        valueType: 1,
        valueOptions: '',
        isRequired: false,
        sort: 0,
        showInList: true,
        businessType: '',
        logicRule: '',
        calculationRule: '',
        childrenIndicatorCodes: '',
        projectType: undefined,
        extraConfig: '',
      };
      currentValueType.value = 1;
      optionsList.length = 0;
      dynamicFields.length = 0;
      formData.value.extraConfig = '';
      // 新建模式下先加载全部分组树
      await loadGroupTreeData();
      return;
    }
    modalApi.lock();
    try {
      // 1. 先获取指标详情（含正确的 projectType 和 groupId）
      const res = await getIndicator(data.id);
      formData.value = res;
      currentValueType.value = res?.valueType || 1;
      parseJsonToOptions(formData.value?.valueOptions || '');
      parseJsonToExtraConfig(formData.value?.extraConfig || '');
      // 2. 按指标的 projectType 加载分组树
      await loadGroupTreeData(res?.projectType);
      // 3. 树加载完成后等待 DOM 更新，再递增 key 强制 tree-select 重渲染
      if (res?.groupId) {
        await nextTick();
        treeSelectKey.value++;
      }
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
    // 清空动态容器子字段（当值类型不是动态容器时）
    if (newVal !== 12) {
      dynamicFields.length = 0;
    }
  },
);

// 监听适用项目类型变化，重新加载分组
watch(
  () => formData.value?.projectType,
  (newProjectType, oldProjectType) => {
    // 只在 projectType 从一个有值变为另一个有值时才清空 groupId（用户切换项目类型）
    // 初始加载编辑数据时 oldProjectType 是 undefined，此时不应清空 groupId
    if (
      newProjectType !== undefined &&
      oldProjectType !== undefined &&
      newProjectType !== oldProjectType &&
      formData.value
    ) {
      formData.value.groupId = undefined;
    }
    loadGroupTreeData(newProjectType);
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
      <!-- 选项定义（单选、多选、单选下拉、多选下拉时显示） -->
      <a-form-item v-if="showValueOptions" label="选项定义">
        <div class="w-full">
          <div v-if="optionsList.length === 0" class="text-gray-400 text-sm mb-2">
            请点击下方按钮添加选项
          </div>
          <div
            v-for="opt in optionsList"
            :key="opt.key"
            class="flex items-center gap-2 mb-2"
          >
            <a-input
              v-model:value="opt.value"
              placeholder="选项值"
              class="w-32"
            />
            <a-input
              v-model:value="opt.label"
              placeholder="选项标签"
              class="flex-1"
            />
            <a-button
              type="text"
              danger
              @click="handleRemoveOption(opt.key)"
            >
              删除选项
            </a-button>
          </div>
          <a-button type="dashed" class="w-full" @click="handleAddOption">
            <PlusOutlined />
            添加选项
          </a-button>
        </div>
      </a-form-item>
      <!-- 动态容器子字段定义 -->
      <a-form-item v-if="isDynamicContainer" label="子字段定义">
        <div class="dynamic-container w-full">
          <div v-if="dynamicFields.length === 0" class="text-gray-400 text-sm mb-2">
            请点击下方按钮添加子字段
          </div>
          <div
            v-for="field in dynamicFields"
            :key="field.key"
            class="dynamic-field-card mb-4"
          >
            <!-- 子字段基本信息行 -->
            <div class="flex items-start gap-2 mb-2">
              <a-input
                v-model:value="field.fieldCode"
                placeholder="字段编码"
                class="w-34"
              />
              <a-input
                v-model:value="field.fieldLabel"
                placeholder="字段名称"
                class="flex-1 min-w-48"
              />
              <a-select
                v-model:value="field.fieldType"
                placeholder="字段类型"
                class="w-14"
              >
                <a-select-option
                  v-for="opt in fieldTypeOptions"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </a-select-option>
              </a-select>
              <a-switch v-model:checked="field.required" />
              <span class="text-gray-400 text-xs whitespace-nowrap">必填</span>
              <a-button
                type="text"
                danger
                @click="handleRemoveField(field.key)"
              >
                删除字段
              </a-button>
            </div>

            <!-- 条件显示配置（点击"添加显示条件"后出现） -->
            <div v-if="field.showCondition" class="mb-2">
              <div class="text-gray-500 text-xs mb-1 font-medium">字段显示条件</div>
              <div class="flex items-center gap-2 flex-wrap">
                <span class="text-gray-400 text-xs">当</span>
                <a-select
                  v-model:value="field.showCondition.watchField"
                  placeholder="请选择被监听的字段"
                  class="w-40"
                  allow-clear
                >
                  <a-select-option
                    v-for="f in otherFields(field)"
                    :key="f.fieldCode"
                    :value="f.fieldCode"
                  >
                    {{ f.fieldLabel }}
                  </a-select-option>
                </a-select>
                <span class="text-red-400 text-xs" v-if="isWatchFieldAfterCurrent(field)">
                  ⚠ 被监听字段应在当前字段之前
                </span>
                <a-select v-model:value="field.showCondition.operator" class="w-24" placeholder="选择条件">
                  <a-select-option value="eq">等于</a-select-option>
                  <a-select-option value="neq">不等于</a-select-option>
                  <a-select-option value="notEmpty">有值</a-select-option>
                  <a-select-option value="isEmpty">无值</a-select-option>
                  <a-select-option value="gt">大于</a-select-option>
                  <a-select-option value="gte">大于等于</a-select-option>
                  <a-select-option value="lt">小于</a-select-option>
                  <a-select-option value="lte">小于等于</a-select-option>
                  <a-select-option value="in">包含</a-select-option>
                </a-select>
                <a-input
                  v-if="!['notEmpty','isEmpty'].includes(field.showCondition.operator)"
                  v-model:value="field.showCondition.value"
                  :placeholder="getConditionValuePlaceholder(field)"
                  class="flex-1"
                />
                <a-button type="text" danger size="small" @click="handleRemoveShowCondition(field)">
                  移除条件
                </a-button>
              </div>
              <div class="text-gray-400 text-xs mt-1">
                示例：监听字段="性别"，运算符="等于"，比较值="男" → 当用户填写"性别=男"时此字段才显示<br/>
                无条件=始终可见；有条件=默认隐藏，满足条件才显示
              </div>
            </div>
            <!-- "添加显示条件"按钮（无条件配置时显示） -->
            <div v-else class="mb-2">
              <a-button type="link" size="small" @click="handleAddShowCondition(field)">
                + 添加显示条件
              </a-button>
            </div>

            <!-- 根据子字段类型显示不同配置 -->
            <div class="pl-2 border-l-2 border-blue-200 space-y-2">
              <!-- 单选/多选/下拉类型：需要配置选项 -->
              <div
                v-if="['radio', 'checkbox', 'select', 'multiSelect'].includes(field.fieldType)"
                class="space-y-2"
              >
                <div class="text-gray-500 text-xs">选项列表</div>
                <div
                  v-for="(opt, optIndex) in field.options"
                  :key="optIndex"
                  class="flex items-center gap-2"
                >
                  <a-input
                    v-model:value="opt.value"
                    placeholder="值"
                    class="w-24"
                  />
                  <a-input
                    v-model:value="opt.label"
                    placeholder="标签"
                    class="flex-1"
                  />
                  <a-button
                    type="text"
                    danger
                    size="small"
                    @click="handleRemoveFieldOption(field.key, optIndex)"
                  >
                    ×
                  </a-button>
                </div>
                <a-button
                  type="dashed"
                  size="small"
                  @click="handleAddFieldOption(field.key)"
                >
                  <PlusOutlined />
                  添加选项
                </a-button>
              </div>

              <!-- 文本/多行文本：可配置最大长度、占位符 -->
              <div
                v-if="field.fieldType === 'text' || field.fieldType === 'textarea'"
                class="flex items-center gap-2"
              >
                <a-input
                  v-model:value="field.maxLength"
                  placeholder="最大字符数"
                  type="number"
                  class="w-32"
                />
                <a-input
                  v-model:value="field.placeholder"
                  placeholder="占位提示文字"
                  class="flex-1"
                />
                <a-input-number
                  v-if="field.fieldType === 'textarea'"
                  v-model:value="field.rows"
                  placeholder="行数"
                  :min="2"
                  :max="10"
                  class="w-20"
                />
              </div>

              <!-- 数字类型：可配置精度、前缀、后缀 -->
              <div
                v-if="field.fieldType === 'number'"
                class="flex items-center gap-2"
              >
                <a-input-number
                  v-model:value="field.precision"
                  placeholder="精度"
                  :min="0"
                  :max="6"
                  class="w-20"
                />
                <a-input
                  v-model:value="field.prefix"
                  placeholder="前缀"
                  class="w-20"
                />
                <a-input
                  v-model:value="field.suffix"
                  placeholder="后缀"
                  class="w-20"
                />
              </div>

              <!-- 日期类型：可配置格式 -->
              <div
                v-if="field.fieldType === 'date' || field.fieldType === 'dateRange'"
                class="flex items-center gap-2"
              >
                <a-select
                  v-model:value="field.format"
                  placeholder="日期格式"
                  class="flex-1"
                  allow-clear
                >
                  <a-select-option value="YYYY-MM-DD">年-月-日</a-select-option>
                  <a-select-option value="YYYY-MM-DD HH:mm">年-月-日 时:分</a-select-option>
                  <a-select-option value="YYYY-MM">年-月</a-select-option>
                  <a-select-option value="YYYY">年</a-select-option>
                </a-select>
              </div>

              <!-- 下拉单选：可配置是否显示搜索 -->
              <div
                v-if="field.fieldType === 'select'"
                class="flex items-center gap-2"
              >
                <a-switch v-model:checked="field.showSearch" />
                <span class="text-gray-400 text-xs">支持搜索</span>
              </div>

              <!-- 多选/下拉多选：可配置最少/最多选择数 -->
              <div
                v-if="field.fieldType === 'checkbox' || field.fieldType === 'multiSelect'"
                class="flex items-center gap-2"
              >
                <a-input-number
                  v-model:value="field.minSelect"
                  placeholder="最少选"
                  :min="0"
                  class="w-24"
                />
                <a-input-number
                  v-model:value="field.maxSelect"
                  placeholder="最多选"
                  :min="1"
                  class="w-24"
                />
              </div>
            </div>
          </div>
          <a-button type="dashed" class="w-full" @click="handleAddField">
            <PlusOutlined />
            添加子字段
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
          <a-select-option
            v-for="option in projectTypeOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="指标分组" name="groupId">
        <a-tree-select
          v-model:value="formData.groupId"
          :treeData="flattenGroupTreeData"
          :key="treeSelectKey"
          treeDataSimpleMode
          :placeholder="
            !formData.projectType
              ? '请先选择适用项目类型'
              : '请选择指标分组'
          "
          allowClear
          style="width: 100%"
          :disabled="!formData.projectType"
        />
        <div
          v-if="!formData.projectType"
          class="text-gray-400 text-xs mt-1"
        >
          请先选择适用项目类型以加载可选的指标分组
        </div>
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
          placeholder="如：[202] / [201] * 100、[202] * 0.5 + [301] * 0.5"
        />
        <div class="text-gray-400 text-xs mt-1">
          使用 [指标CODE] 格式引用其他指标，如 [202] 表示指标代码为202的值
        </div>
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

<style scoped>
/* 动态容器子字段样式 */
.dynamic-container {
  width: 100%;
}

.dynamic-field-card {
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 12px;
}

.dynamic-field-card:hover {
  border-color: #1890ff;
}
</style>
