<script lang="ts" setup>
import type { DeclareFilingApi } from '#/api/declare/filing';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { DeclareIndicatorJointRuleApi } from '#/api/declare/jointRule';

import dayjs from 'dayjs';
import { computed, reactive, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';
import type { FileType } from 'ant-design-vue/es/upload/interface';
import { Steps, Step, Upload } from 'ant-design-vue';
import {
  getIndicatorsByProjectType,
} from '#/api/declare/indicator';
import {
  createFiling,
  getFiling,
  updateFiling,
} from '#/api/declare/filing';
import {
  getIndicatorValues,
} from '#/api/declare/indicatorValue';
import {
  getEnabledJointRules,
} from '#/api/declare/jointRule';
import { uploadFile } from '#/api/infra/file';
import { validate } from '#/utils/indicatorValidator';
import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import { $t } from '#/locales';

interface IndicatorGroup {
  category: number;
  categoryName: string;
  indicators: DeclareIndicatorApi.Indicator[];
}

// 业务类型枚举
const BUSINESS_TYPE = {
  FILING: 1, // 备案
} as const;

const emit = defineEmits(['success']);

// 当前步骤
const currentStep = ref(1);

// 项目类型
const projectType = ref<number | undefined>(undefined);

// 指标分组数据
const indicatorGroups = ref<IndicatorGroup[]>([]);

// 联合验证规则
const jointRules = ref<DeclareIndicatorJointRuleApi.JointRule[]>([]);

// 指标值存储 - 使用 reactive 对象
const indicatorValuesMap = reactive<Record<number, any>>({});

// 指标验证错误状态: key 为指标ID, value 为错误消息
const indicatorErrors = reactive<Record<number, string>>({});

// 获取有验证错误的指标ID列表
const errorIndicatorIds = computed(() => {
  return Object.keys(indicatorErrors)
    .filter((key) => indicatorErrors[Number(key)])
    .map(Number);
});

// 折叠面板展开的key
const activeCollapseKeys = ref<(string | number)[]>([]);

// 表单ref
const formRef = ref<any>(null);

// 判断指标是否有口径
function hasIndicatorSpec(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(
    indicator.definition ||
    indicator.statisticScope ||
    indicator.dataSource ||
    indicator.fillRequire ||
    indicator.calculationExample
  );
}

const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['项目备案核心信息'])
    : $t('ui.actionTitle.create', ['项目备案核心信息']);
});

// 指标分类字典
const indicatorCategories = computed(() =>
  getDictOptions(DICT_TYPE.DECLARE_INDICATOR_CATEGORY, 'number')
);

// 获取分类名称
function getCategoryName(category: number) {
  const found = indicatorCategories.value.find(
    (item) => Number(item.value) === category
  );
  return found?.label || `分类${category}`;
}

// 指标值变化时触发验证（实时验证）
// eslint-disable-next-line @typescript-eslint/no-unused-vars
function onIndicatorChange(_indicatorId: number, indicator?: DeclareIndicatorApi.Indicator) {
  // 收集所有有错误的指标ID（用于后续对比）
  const previousErrorIds = new Set(Object.keys(indicatorErrors).map(Number));

  // 收集本次验证失败的指标ID
  const currentErrorIds = new Set<number>();

  // 执行 FILL 类型规则的实时验证
  const errors = validate(
    jointRules.value.filter((r) => r.id) as any,
    indicatorValuesMap,
    { triggerTiming: 'FILL' }
  );

  // 遍历所有错误，为每个涉及的指标设置错误状态和消息
  errors.forEach((error) => {
    // 收集所有涉及的指标ID
    const involvedIds = (error.involvedIndicatorIds || [error.indicatorId].filter(Boolean)) as number[];

    // 每个涉及的指标都显示错误消息
    involvedIds.forEach((id) => {
      if (id) {
        indicatorErrors[id] = error.message;
        currentErrorIds.add(id);
      }
    });
  });

  // 清除已通过验证的指标错误（本次验证没有失败的指标）
  previousErrorIds.forEach((id) => {
    if (!currentErrorIds.has(id)) {
      delete indicatorErrors[id];
    }
  });

  // 如果传入了指标对象，同时进行必填验证
  if (indicator) {
    validateRequired(indicator);
  }
  
  // 重新计算所有计算指标
  recalculateAllComputedIndicators();
}

// 验证必填项
function validateRequired(indicator: DeclareIndicatorApi.Indicator) {
  if (!indicator.isRequired) return;

  let value = indicatorValuesMap[indicator.id!];
  let isEmpty = false;

  // 处理不同值类型的空值判断
  if (value === undefined || value === null) {
    isEmpty = true;
  } else if (typeof value === 'string') {
    isEmpty = value.trim() === '';
  } else if (Array.isArray(value)) {
    isEmpty = value.length === 0;
  } else if (indicator.valueType === 9) {
    // 文件上传类型：value 是 JSON 字符串
    try {
      const fileList = JSON.parse(value);
      isEmpty = !Array.isArray(fileList) || fileList.length === 0;
    } catch {
      isEmpty = true;
    }
  }

  const indicatorId = indicator.id!;

  if (isEmpty) {
    // 字段为空，设置必填错误
    // 如果已经有逻辑验证错误，只在后面追加必填提示
    const existingError = indicatorErrors[indicatorId];
    if (existingError) {
      // 如果现有错误不是必填错误，追加必填提示
      if (!existingError.includes('不能为空')) {
        indicatorErrors[indicatorId] = `${existingError}；${indicator.indicatorName} 不能为空，请填写`;
      }
    } else {
      indicatorErrors[indicatorId] = `${indicator.indicatorName} 不能为空，请填写`;
    }
  } else {
    // 字段有值，只清除必填错误，保留逻辑验证错误
    const existingError = indicatorErrors[indicatorId];
    if (existingError && existingError.includes('不能为空')) {
      // 如果只有必填错误，直接清除
      if (!existingError.includes('；')) {
        delete indicatorErrors[indicatorId];
      } else {
        // 如果有多个错误（逻辑验证错误 + 必填错误），只清除必填错误部分
        // 错误格式：逻辑错误；指标名 不能为空，请填写
        const parts = existingError.split('；');
        const logicError = parts.find(p => !p.includes('不能为空'));
        if (logicError) {
          indicatorErrors[indicatorId] = logicError;
        } else {
          delete indicatorErrors[indicatorId];
        }
      }
    }
  }
}

// 验证所有必填指标
function validateAllRequired(): boolean {
  console.log('validateAllRequired 被调用，indicatorGroups 长度:', indicatorGroups.value.length);
  
  // 先清空所有必填错误
  Object.keys(indicatorErrors).forEach((key) => {
    const id = Number(key);
    const error = indicatorErrors[id];
    if (error && error.includes('不能为空')) {
      delete indicatorErrors[id];
    }
  });

  // 遍历所有指标进行必填验证
  let hasError = false;
  indicatorGroups.value.forEach((group) => {
    console.log('处理分组:', group.categoryName, '指标数量:', group.indicators.length);
    group.indicators.forEach((indicator) => {
      console.log('验证指标:', indicator.indicatorName, 'isRequired:', indicator.isRequired, 'valueType:', indicator.valueType, 'value:', indicatorValuesMap[indicator.id!]);
      if (indicator.isRequired) {
        validateRequired(indicator);
        // 检查是否还有错误
        if (indicatorErrors[indicator.id!]) {
          console.log('指标有错误:', indicator.indicatorName, indicatorErrors[indicator.id!]);
          hasError = true;
        }
      }
    });
  });

  console.log('验证完成，hasError:', hasError, 'errors:', indicatorErrors);
  return !hasError;
}

// 文件上传处理
interface UploadFileItem {
  name: string;
  url: string;
  uid?: string;
}

const fileListMap = reactive<Record<number, UploadFileItem[]>>({});

function getFileList(indicatorId: number): UploadFileItem[] {
  return fileListMap[indicatorId] || [];
}

// 获取文件上传最大数量（优先使用 extraConfig.maxCount，其次使用 maxValue 字段，默认5个）
function getMaxFileCount(indicator: DeclareIndicatorApi.Indicator): number {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  if (extraConfig.maxCount !== undefined) {
    return Number(extraConfig.maxCount);
  }
  return indicator.maxValue ? Number(indicator.maxValue) : 5;
}

// 获取文件上传最大大小（字节）
function getMaxFileSize(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.maxSize ? Number(extraConfig.maxSize) : undefined;
}

// 获取允许的文件类型
function getAcceptTypes(indicator: DeclareIndicatorApi.Indicator): string {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.accept || '';
}

// 获取数字精度（小数位数）
function getNumberPrecision(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.precision !== undefined ? Number(extraConfig.precision) : undefined;
}

// 获取数字前缀
function getNumberPrefix(indicator: DeclareIndicatorApi.Indicator): string {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.prefix || '';
}

// 获取数字后缀
function getNumberSuffix(indicator: DeclareIndicatorApi.Indicator): string {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.suffix || indicator.unit || '';
}

// 获取日期格式
function getDateFormat(indicator: DeclareIndicatorApi.Indicator): string {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.format || 'YYYY-MM-DD';
}

// 获取布尔类型的标签
function getBooleanLabels(indicator: DeclareIndicatorApi.Indicator): { true: string; false: string } {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return {
    true: extraConfig.trueLabel || '是',
    false: extraConfig.falseLabel || '否',
  };
}

// 获取字符串/文本的最大长度
function getMaxLength(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.maxLength !== undefined ? Number(extraConfig.maxLength) : undefined;
}

// 是否富文本
function isRichText(indicator: DeclareIndicatorApi.Indicator): boolean {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.richText === true;
}

// 获取布局方式
function getLayout(indicator: DeclareIndicatorApi.Indicator): string {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  // 默认改为水平显示，超出自动折行
  return extraConfig.layout || 'horizontal';
}

// 是否显示搜索
function getShowSearch(indicator: DeclareIndicatorApi.Indicator): boolean {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.showSearch === true;
}

// 解析存储的文件列表
function parseStoredFileList(value: string | undefined): UploadFileItem[] {
  if (!value) return [];
  try {
    // 尝试解析 JSON 数组
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) {
      return parsed;
    }
  } catch {
    // 兼容单个URL格式
    if (value.includes(',')) {
      // 多个URL用逗号分隔
      return value.split(',').map((url) => ({
        name: url.split('/').pop() || url,
        url: url.trim(),
      }));
    }
    // 单个URL
    return [{
      name: value.split('/').pop() || value,
      url: value,
    }];
  }
  return [];
}

// 初始化文件列表（加载已有数据时调用）
function initFileList(indicatorId: number, value: string | undefined) {
  fileListMap[indicatorId] = parseStoredFileList(value);
}

async function handleFileUpload(file: FileType, indicator: DeclareIndicatorApi.Indicator) {
  const indicatorId = indicator.id!;
  const maxCount = getMaxFileCount(indicator);
  const maxSize = getMaxFileSize(indicator);
  const acceptTypes = getAcceptTypes(indicator);

  // 检查文件类型
  if (acceptTypes) {
    const fileExt = file.name.split('.').pop()?.toLowerCase() || '';
    const allowedExts = acceptTypes.split(',').map((ext) => ext.trim().toLowerCase());
    if (!allowedExts.includes(fileExt)) {
      message.warning(`仅支持上传以下类型：${acceptTypes}`);
      return false;
    }
  }

  // 检查文件大小
  if (maxSize && file.size > maxSize) {
    const maxSizeMB = (maxSize / 1024 / 1024).toFixed(2);
    message.warning(`文件大小不能超过${maxSizeMB}MB`);
    return false;
  }

  try {
    const result = await uploadFile({
      file: file as any,
      directory: 'declare/indicator',
    });

    const fileUrl = result.url || result;
    const fileName = file.name;
    
    // 获取当前文件列表
    const currentList = fileListMap[indicatorId] || [];
    
    // 检查是否已上传过同名文件
    if (currentList.some(f => f.name === fileName)) {
      message.warning('文件已存在');
      return false;
    }
    
    // 检查文件数量限制
    if (currentList.length >= maxCount) {
      message.warning(`最多上传${maxCount}个文件`);
      return false;
    }
    
    // 添加新文件
    const newFile: UploadFileItem = {
      name: fileName,
      url: fileUrl,
      uid: Date.now().toString(),
    };
    
    fileListMap[indicatorId] = [...currentList, newFile];
    
    // 更新存储值（JSON 数组）
    indicatorValuesMap[indicatorId] = JSON.stringify(fileListMap[indicatorId]);

    // 触发验证（包括必填验证）
    onIndicatorChange(indicatorId);
    validateRequired(indicator);
    message.success('文件上传成功');
  } catch (error) {
    console.error('文件上传错误:', error);
    message.error('文件上传失败');
  }
  
  // 阻止默认上传行为
  return false;
}

// 删除文件
function handleFileRemove(file: UploadFileItem, indicatorId: number, indicator?: DeclareIndicatorApi.Indicator) {
  const currentList = fileListMap[indicatorId] || [];
  fileListMap[indicatorId] = currentList.filter(f => f.uid !== file.uid && f.name !== file.name);

  // 更新存储值
  indicatorValuesMap[indicatorId] = JSON.stringify(fileListMap[indicatorId]);

  // 触发验证（包括必填验证）
  onIndicatorChange(indicatorId);
  if (indicator) {
    validateRequired(indicator);
  }
  message.success('文件已删除');
}

// 项目类型改变时加载指标
async function loadIndicators(type: number) {
  if (!type) return;
  try {
    // 并行加载指标和验证规则
    const [indicators, rules] = await Promise.all([
      getIndicatorsByProjectType(type, 'filing'),
      getEnabledJointRules({ projectType: type, triggerTiming: 'FILL' }),
    ]);

    // 保存验证规则
    jointRules.value = rules;
    console.log('加载验证规则:', rules);

    // 按分类分组
    const groups: IndicatorGroup[] = [];
    const categoryMap = new Map<number, IndicatorGroup>();

    indicators.forEach((indicator) => {
      if (!categoryMap.has(indicator.category)) {
        const group: IndicatorGroup = {
          category: indicator.category,
          categoryName: getCategoryName(indicator.category),
          indicators: [],
        };
        categoryMap.set(indicator.category, group);
        groups.push(group);
      }
      categoryMap.get(indicator.category)!.indicators.push(indicator);
    });

    // 按分类排序
    groups.sort((a, b) => a.category - b.category);
    indicatorGroups.value = groups;

    // 默认展开第一个分类
    const firstGroup = groups[0];
    if (firstGroup) {
      activeCollapseKeys.value = [firstGroup.category];
    }

    // 加载已保存的指标值（编辑模式）
    if (formData.value?.id) {
      await loadIndicatorValues();
    }
  } catch (error) {
    console.error('加载指标失败:', error);
    message.error('加载指标失败');
  }
}

// 加载已保存的指标值
async function loadIndicatorValues() {
  if (!formData.value?.id) return;
  try {
    const values = await getIndicatorValues(
      BUSINESS_TYPE.FILING,
      formData.value.id
    );
    // 将值填充到 indicatorValuesMap
    values.forEach((v) => {
      let value: any;
      switch (v.valueType) {
        case 1: // 数字
          value = v.valueNum ? Number(v.valueNum) : undefined;
          break;
        case 2: // 字符串
        case 6: // 单选
          // 单选直接使用字符串值
          value = v.valueStr || undefined;
          break;
        case 7: // 多选
          // 多选需要将逗号分隔的字符串转换为数组
          if (v.valueStr) {
            value = v.valueStr.split(',');
          } else {
            value = undefined;
          }
          break;
        case 10: // 单选下拉
        case 11: // 多选下拉
          // 多选下拉也需要将逗号分隔的字符串转换为数组
          if (v.valueStr && v.valueType === 11) {
            value = v.valueStr.split(',');
          } else {
            value = v.valueStr || undefined;
          }
          break;
        case 3: // 布尔
          value = v.valueBool;
          break;
        case 4: // 日期
          value = v.valueDate || undefined;
          break;
        case 5: // 长文本
          value = v.valueText || undefined;
          break;
        case 8: // 日期区间
          // 日期区间需要特殊处理
          if (v.valueDateStart || v.valueDateEnd) {
            value = [v.valueDateStart, v.valueDateEnd];
          }
          break;
        case 9: // 文件上传
          value = v.valueStr || undefined;
          // 初始化文件列表
          if (value && v.indicatorId) {
            initFileList(v.indicatorId, value);
          }
          break;
        default:
          value = v.valueStr || undefined;
      }
      indicatorValuesMap[v.indicatorId] = value;
    });
  } catch (error) {
    console.error('加载指标值失败:', error);
  }
}

// 解析选项
function parseOptions(valueOptions: string): Array<{ label: string; value: string }> {
  if (!valueOptions) return [];
  try {
    return JSON.parse(valueOptions);
  } catch {
    return [];
  }
}

// 解析扩展配置
function parseExtraConfig(extraConfig: string | undefined): Record<string, any> {
  if (!extraConfig) return {};
  try {
    return JSON.parse(extraConfig);
  } catch {
    return {};
  }
}

// 判断指标是否为计算指标（有计算公式）
function isComputedIndicator(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(indicator.calculationRule && indicator.calculationRule.trim());
}

// 获取指标值（支持通过 indicatorCode 获取）
function getIndicatorValue(indicatorCodeOrId: string | number): number | undefined {
  // 先通过 ID 查找
  if (typeof indicatorCodeOrId === 'number') {
    return indicatorValuesMap[indicatorCodeOrId];
  }
  
  // 通过 code 查找
  let value: number | undefined;
  indicatorGroups.value.forEach((group) => {
    group.indicators.forEach((indicator) => {
      if (indicator.indicatorCode === indicatorCodeOrId) {
        const val = indicatorValuesMap[indicator.id!];
        if (val !== undefined && val !== null && val !== '') {
          value = Number(val);
        }
      }
    });
  });
  return value;
}

// 计算指标值
function calculateIndicatorValue(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  if (!indicator.calculationRule) return undefined;
  
  try {
    // 替换公式中的指标 CODE 为实际值
    // 使用 [CODE] 格式来标识指标引用，避免与数字混淆
    let formula = indicator.calculationRule;
    
    // 匹配 [CODE] 格式的指标引用
    const indicatorMatches = formula.match(/\[[^\]]+\]/g) || [];
    
    // 尝试将每个匹配的指标替换为对应的值
    let hasAllValues = true;
    let processedFormula = formula;
    
    for (const match of indicatorMatches) {
      // 提取 CODE
      const code = match.replace(/[\[\]]/g, '');
      const value = getIndicatorValue(code);
      
      if (value === undefined) {
        console.log(`指标 ${indicator.indicatorCode} 计算公式缺少依赖指标: ${code}`);
        hasAllValues = false;
        break;
      }
      // 替换所有出现的该指标引用
      processedFormula = processedFormula.replace(match, String(value));
    }
    
    if (!hasAllValues) {
      return undefined;
    }
    
    // 安全地计算表达式（只允许数字和基本运算符）
    // 移除所有非数字、非运算符、非括号的字符
    const safeFormula = processedFormula.replace(/[^0-9+\-*/.()%]/g, '');
    
    if (!safeFormula || safeFormula.trim() === '') {
      console.log(`指标 ${indicator.indicatorCode} 计算公式无效: ${indicator.calculationRule}`);
      return undefined;
    }
    
    // 使用 Function 计算结果（比 eval 更安全，但仍需注意）
    const result = new Function(`return ${safeFormula}`)();
    
    if (isNaN(result) || !isFinite(result)) {
      console.log(`指标 ${indicator.indicatorCode} 计算结果无效: ${result}`);
      return undefined;
    }
    
    console.log(`指标 ${indicator.indicatorCode} 计算成功: ${formula} = ${processedFormula} = ${result}`);
    return result;
  } catch (error) {
    console.error(`指标 ${indicator.indicatorCode} 计算失败:`, error);
    return undefined;
  }
}

// 重新计算所有计算指标
function recalculateAllComputedIndicators() {
  indicatorGroups.value.forEach((group) => {
    group.indicators.forEach((indicator) => {
      if (isComputedIndicator(indicator)) {
        const calculatedValue = calculateIndicatorValue(indicator);
        if (calculatedValue !== undefined) {
          indicatorValuesMap[indicator.id!] = calculatedValue;
        }
      }
    });
  });
}

// 初始化表单数据
const step1FormData = ref({
  socialCreditCode: '',
  medicalLicenseNo: '',
  orgName: '',
  projectType: undefined as number | undefined,
  validStartTime: undefined as any,
  validEndTime: undefined as any,
  constructionContent: '',
});

// 表单校验规则
const formRules = {
  socialCreditCode: [
    { required: true, message: '请输入社会信用代码', trigger: 'blur' },
  ],
  medicalLicenseNo: [
    { required: true, message: '请输入执业许可证号', trigger: 'blur' },
  ],
  orgName: [
    { required: true, message: '请输入机构名称', trigger: 'blur' },
  ],
  projectType: [
    { required: true, message: '请选择项目类型', trigger: 'change' },
  ],
  validStartTime: [
    { required: true, message: '请选择有效期开始时间', trigger: 'change' },
  ],
  validEndTime: [
    { required: true, message: '请选择有效期结束时间', trigger: 'change' },
  ],
  constructionContent: [
    { required: true, message: '请输入建设内容', trigger: 'blur' },
  ],
};

// 校验第一步表单
async function validateStep1() {
  try {
    await formRef.value?.validate();
    return true;
  } catch (err) {
    console.error('验证失败:', err);
    return false;
  }
}

// 构建指标值数据
function buildIndicatorValues(): Array<{
  indicatorId: number;
  indicatorCode: string;
  valueType: number;
  valueNum?: string;
  valueStr?: string;
  valueBool?: boolean;
  valueDate?: string;
  valueDateStart?: string;
  valueDateEnd?: string;
  valueText?: string;
}> {
  const result: any[] = [];

  // 遍历所有指标组
  indicatorGroups.value.forEach((group) => {
    group.indicators.forEach((indicator) => {
      const value = indicatorValuesMap[indicator.id!];
      if (value === undefined || value === null || value === '') {
        return; // 跳过空值
      }

      const item: any = {
        indicatorId: indicator.id!,
        indicatorCode: indicator.indicatorCode,
        valueType: indicator.valueType,
      };

      // 根据值类型处理
      switch (indicator.valueType) {
        case 1: // 数字
          item.valueNum = String(value);
          break;
        case 2: // 字符串
          item.valueStr = String(value);
          break;
        case 6: // 单选
          item.valueStr = String(value);
          break;
        case 7: // 多选
          // 需要将数组转换为逗号分隔的字符串
          if (Array.isArray(value)) {
            item.valueStr = value.join(',');
          } else {
            item.valueStr = String(value);
          }
          break;
        case 10: // 单选下拉
          item.valueStr = String(value);
          break;
        case 11: // 多选下拉
          // 需要将数组转换为逗号分隔的字符串
          if (Array.isArray(value)) {
            item.valueStr = value.join(',');
          } else {
            item.valueStr = String(value);
          }
          break;
        case 9: // 文件上传
          item.valueStr = String(value);
          break;
        case 3: // 布尔
          item.valueBool = value;
          break;
        case 4: // 日期
          // 处理 dayjs 对象
          if (value && typeof value === 'object' && value.format) {
            item.valueDate = value.format('YYYY-MM-DD HH:mm:ss');
          } else {
            item.valueDate = value;
          }
          break;
        case 5: // 长文本
          item.valueText = String(value);
          break;
        case 8: // 日期区间
          if (Array.isArray(value) && value.length === 2) {
            // 处理 dayjs 对象
            if (value[0] && value[0].format) {
              item.valueDateStart = value[0].format('YYYY-MM-DD HH:mm:ss');
              item.valueDateEnd = value[1].format('YYYY-MM-DD HH:mm:ss');
            } else {
              item.valueDateStart = value[0];
              item.valueDateEnd = value[1];
            }
          }
          break;
      }

      result.push(item);
    });
  });

  return result;
}

// 保存
async function handleSubmit() {
  const valid = await validateStep1();
  if (!valid) {
    message.error('请完善基本信息');
    return;
  }

  // 验证所有必填指标
  const requiredValid = validateAllRequired();
  console.log('必填验证结果:', requiredValid, '错误列表:', indicatorErrors);
  if (!requiredValid) {
    // 获取第一个错误的指标名称
    const firstErrorId = Object.keys(indicatorErrors)[0];
    const errorMsg = indicatorErrors[Number(firstErrorId)];
    console.log('显示错误:', errorMsg);
    message.error(errorMsg || '请填写所有必填项');
    return;
  }

  // 验证联合规则
  const rules = jointRules.value.filter((r) => r.id);
  console.log('验证联合规则，规则数量:', rules.length, '指标值:', indicatorValuesMap);
  const validationErrors = validate(
    jointRules.value.filter((r) => r.id) as any,
    indicatorValuesMap,
    { triggerTiming: 'FILL' }
  );
  if (validationErrors.length > 0) {
    message.error('验证失败：' + validationErrors.map((e) => e.message).join('；'));
    return;
  }

  modalApi.lock();

  try {
    // 格式化日期函数：将 dayjs 对象转换为后端需要的时间戳格式
    const formatDate = (date: any): number | undefined => {
      if (!date) return undefined;
      // 如果是数字（时间戳），直接返回
      if (typeof date === 'number') return date;
      // 如果是字符串，尝试解析
      if (typeof date === 'string') {
        const parsed = dayjs(date);
        if (parsed.isValid()) {
          return parsed.valueOf();
        }
        return undefined;
      }
      // 如果是 dayjs 对象，转换为时间戳
      if (date && typeof date.valueOf === 'function') {
        const result = date.valueOf();
        console.log('格式化日期(时间戳):', date.format('YYYY-MM-DD HH:mm:ss'), '->', result);
        return result;
      }
      console.log('未知日期格式:', date);
      return undefined;
    };

    // 1. 构建备案数据（包含指标值）
    const indicatorValues = buildIndicatorValues();
    const filingData: DeclareFilingApi.Filing = {
      id: formData.value?.id || 0,
      socialCreditCode: step1FormData.value.socialCreditCode || '',
      medicalLicenseNo: step1FormData.value.medicalLicenseNo || '',
      orgName: step1FormData.value.orgName || '',
      projectType: step1FormData.value.projectType || 1,
      validStartTime: formatDate(step1FormData.value.validStartTime),
      validEndTime: formatDate(step1FormData.value.validEndTime),
      planEndTime: formatDate(step1FormData.value.planEndTime),
      constructionContent: step1FormData.value.constructionContent || '',
      filingStatus: 0,
      provinceReviewOpinion: '',
      provinceReviewTime: '' as any,
      provinceReviewerId: 0,
      expertReviewOpinion: '',
      expertReviewerIds: '',
      filingArchiveTime: '' as any,
      // 包含指标值
      indicatorValues,
    };
    console.log('提交数据:', JSON.stringify(filingData));

    // 2. 一次性提交备案信息和指标值
    if (formData.value?.id) {
      await updateFiling(filingData);
    } else {
      await createFiling(filingData);
    }
    console.log('保存成功，备案ID:', filingData.id);

    message.success('保存成功');
    await modalApi.close();
    emit('success');
  } catch (error) {
    console.error('保存失败:', error);
    message.error('保存失败');
  } finally {
    modalApi.unlock();
  }
}

// 表单数据（用于编辑模式加载）
const formData = ref<DeclareFilingApi.Filing>();

const [Modal, modalApi] = useVbenModal({
  destroyOnClose: true,
  footer: true,
  // 根据步骤动态设置按钮文字
  confirmText: '下一步',
  showCancelButton: true,
  cancelText: '取消',
  async onConfirm() {
    // 第一步：下一步；第二步：提交
    if (currentStep.value === 1) {
      const valid = await validateStep1();
      if (!valid) {
        message.error('请完善基本信息');
        return false; // 阻止关闭
      }
      // 进入第二步
      currentStep.value = 2;
      // 更新底部按钮文字
      modalApi.setState({
        confirmText: '提交',
        cancelText: '上一步',
      });
      // 加载指标数据
      await loadIndicators(step1FormData.value.projectType!);
      return false; // 阻止关闭，保持弹窗打开
    } else {
      // 第二步：提交
      await handleSubmit();
      return false;
    }
  },
  onCancel() {
    // 上一步
    if (currentStep.value === 2) {
      currentStep.value = 1;
      // 更新底部按钮文字
      modalApi.setState({
        confirmText: '下一步',
        cancelText: '取消',
      });
      return;
    }
    // 第一步点击取消，直接关闭
    modalApi.close();
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      currentStep.value = 1;
      indicatorGroups.value = [];
      activeCollapseKeys.value = [];
      Object.keys(indicatorValuesMap).forEach(key => delete indicatorValuesMap[Number(key)]);
      // 重置表单
      step1FormData.value = {
        socialCreditCode: '',
        medicalLicenseNo: '',
        orgName: '',
        projectType: undefined,
        validStartTime: undefined,
        validEndTime: undefined,
        constructionContent: '',
      };
      return;
    }

    const data = modalApi.getData<DeclareFilingApi.Filing>();
    if (!data || !data.id) {
      // 新增模式
      return;
    }

    // 编辑模式：加载数据
    modalApi.lock();
    try {
      formData.value = await getFiling(data.id);
      const filing = formData.value!;

      // 填充基本信息
      console.log('加载到的日期:', filing.validStartTime, filing.validEndTime);
      step1FormData.value = {
        socialCreditCode: filing.socialCreditCode || '',
        medicalLicenseNo: filing.medicalLicenseNo || '',
        orgName: filing.orgName || '',
        projectType: filing.projectType,
        // 处理日期类型：将后端返回的字符串转换为 dayjs 对象
        validStartTime: filing.validStartTime ? dayjs(filing.validStartTime) : undefined,
        validEndTime: filing.validEndTime ? dayjs(filing.validEndTime) : undefined,
        constructionContent: filing.constructionContent || '',
      };
      console.log('转换后的日期:', step1FormData.value.validStartTime, step1FormData.value.validEndTime);

      // 加载指标数据
      await loadIndicators(step1FormData.value.projectType!);
    } catch (error) {
      console.error('加载数据失败:', error);
      message.error('加载数据失败');
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="p-4">
      <!-- 步骤条 -->
      <div class="mb-6">
        <Steps :current="currentStep - 1" size="small">
          <Step title="基本信息" />
          <Step title="指标填报" />
        </Steps>
      </div>

      <!-- 第一步：基本信息 -->
      <div v-show="currentStep === 1">
        <a-form
          ref="formRef"
          :model="step1FormData"
          :rules="formRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item
            label="社会信用代码"
            name="socialCreditCode"
          >
            <a-input
              v-model:value="step1FormData.socialCreditCode"
              placeholder="请输入社会信用代码"
            />
          </a-form-item>

          <a-form-item
            label="执业许可证号"
            name="medicalLicenseNo"
          >
            <a-input
              v-model:value="step1FormData.medicalLicenseNo"
              placeholder="请输入执业许可证号"
            />
          </a-form-item>

          <a-form-item label="机构名称" name="orgName">
            <a-input
              v-model:value="step1FormData.orgName"
              placeholder="请输入机构名称"
            />
          </a-form-item>

          <a-form-item label="项目类型" name="projectType">
            <a-select
              v-model:value="step1FormData.projectType"
              placeholder="请选择项目类型"
              @change="(val: number) => (projectType = val)"
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

          <a-form-item
            label="有效期开始时间"
            name="validStartTime"
          >
            <a-date-picker
              v-model:value="step1FormData.validStartTime"
              show-time
              format="YYYY-MM-DD"
              class="w-full"
            />
          </a-form-item>

          <a-form-item label="有效期结束时间" name="validEndTime">
            <a-date-picker
              v-model:value="step1FormData.validEndTime"
              show-time
              format="YYYY-MM-DD"
              class="w-full"
            />
          </a-form-item>

          <a-form-item
            label="建设内容"
            name="constructionContent"
          >
            <a-textarea
              v-model:value="step1FormData.constructionContent"
              placeholder="请输入建设内容"
              :rows="4"
            />
          </a-form-item>
        </a-form>
      </div>

      <!-- 第二步：指标填报 -->
      <div v-show="currentStep === 2">
        <div v-if="indicatorGroups.length === 0" class="py-8 text-center text-gray-500">
          暂无指标数据，请确认项目类型选择正确
        </div>

        <a-collapse v-else v-model:activeKey="activeCollapseKeys">
          <a-collapse-panel
            v-for="group in indicatorGroups"
            :key="group.category"
            :header="`${group.categoryName} (${group.indicators.length}个指标)`"
          >
            <div class="grid grid-cols-2 gap-4">
              <a-form-item
                v-for="indicator in group.indicators"
                :key="indicator.id"
                :required="indicator.isRequired"
                :label-col="{ span: 8 }"
                :wrapper-col="{ span: 16 }"
              >
                <!-- 指标名称 + 问号图标 -->
                <template #label>
                  <div class="flex items-center justify-end max-w-full" style="max-width: 90%;">
                    <span class="truncate" :title="indicator.indicatorName" >
                      {{ indicator.indicatorName }}
                    </span>
                    <!-- 计算指标显示计算公式提示 -->
                    <a-tag v-if="isComputedIndicator(indicator)" color="orange" class="ml-1 text-xs flex-shrink-1">
                      自动计算
                    </a-tag>
                    <!-- 仅当有口径时才显示问号图标 -->
                    <a-popover
                      v-if="hasIndicatorSpec(indicator)"
                      placement="right"
                      trigger="click"
                      :overlayStyle="{ maxWidth: '400px' }"
                    >
                      <template #content>
                        <div class="space-y-3">
                          <!-- 指标定义 -->
                          <div v-if="indicator.definition">
                            <h4 class="font-medium text-gray-900 text-sm mb-1">📌 {{ indicator.indicatorCode}} - 指标定义</h4>
                            <p class="text-gray-600 text-xs">{{ indicator.definition }}</p>
                          </div>

                          <!-- 统计范围 -->
                          <div v-if="indicator.statisticScope">
                            <h4 class="font-medium text-gray-900 text-sm mb-1">📏 统计范围</h4>
                            <p class="text-gray-600 text-xs">{{ indicator.statisticScope }}</p>
                          </div>

                          <!-- 数据来源 -->
                          <div v-if="indicator.dataSource">
                            <h4 class="font-medium text-gray-900 text-sm mb-1">📁 数据来源</h4>
                            <p class="text-gray-600 text-xs">{{ indicator.dataSource }}</p>
                          </div>

                          <!-- 填报要求 -->
                          <div v-if="indicator.fillRequire">
                            <h4 class="font-medium text-gray-900 text-sm mb-1">📝 填报要求</h4>
                            <p class="text-gray-600 text-xs whitespace-pre-wrap">{{ indicator.fillRequire }}</p>
                          </div>

                          <!-- 计算示例 -->
                          <div v-if="indicator.calculationExample">
                            <h4 class="font-medium text-gray-900 text-sm mb-1">🧮 计算示例</h4>
                            <div class="bg-gray-50 p-2 rounded text-gray-600 text-xs whitespace-pre-wrap">
                              {{ indicator.calculationExample }}
                            </div>
                          </div>
                        </div>
                      </template>
                      <IconifyIcon
                        icon="lucide:help-circle"
                        class="ml-1 cursor-pointer text-blue-500 hover:text-blue-600 flex-shrink-0"
                        style="width: 14px; height: 14px;"
                      />
                    </a-popover>
                  </div>
                </template>
                <!-- 数字类型 -->
                <a-input-number
                  v-if="indicator.valueType === 1"
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  class="w-full"
                  :disabled="isComputedIndicator(indicator)"
                  :status="errorIndicatorIds.includes(indicator.id!) ? 'error' : ''"
                  :placeholder="isComputedIndicator(indicator) ? '自动计算' : `请输入${indicator.indicatorName}`"
                  :min="indicator.minValue"
                  :max="indicator.maxValue"
                  :precision="getNumberPrecision(indicator)"
                  @change="onIndicatorChange(indicator.id!, indicator)"
                  @blur="validateRequired(indicator)"
                >
                  <template #addonBefore v-if="getNumberPrefix(indicator)">
                    {{ getNumberPrefix(indicator) }}
                  </template>
                  <template #addonAfter v-if="getNumberSuffix(indicator)">
                    {{ getNumberSuffix(indicator) }}
                  </template>
                </a-input-number>

                <!-- 字符串类型 -->
                <a-input
                  v-else-if="indicator.valueType === 2"
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  :status="errorIndicatorIds.includes(indicator.id!) ? 'error' : ''"
                  :placeholder="`请输入${indicator.indicatorName}`"
                  :maxlength="getMaxLength(indicator)"
                  show-count
                  @change="onIndicatorChange(indicator.id!, indicator)"
                  @blur="validateRequired(indicator)"
                />

                <!-- 布尔类型 -->
                <a-switch
                  v-else-if="indicator.valueType === 3"
                  v-model:checked="indicatorValuesMap[indicator.id!]"
                  :checked-children="getBooleanLabels(indicator).true"
                  :un-checked-children="getBooleanLabels(indicator).false"
                  @change="onIndicatorChange(indicator.id!, indicator)"
                />

                <!-- 日期类型 -->
                <a-date-picker
                  v-else-if="indicator.valueType === 4"
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  :show-time="getDateFormat(indicator).includes('HH')"
                  :format="getDateFormat(indicator)"
                  class="w-full"
                  :status="errorIndicatorIds.includes(indicator.id!) ? 'error' : ''"
                  @change="onIndicatorChange(indicator.id!, indicator)"
                  @blur="validateRequired(indicator)"
                />

                <!-- 长文本类型 -->
                <a-textarea
                  v-else-if="indicator.valueType === 5"
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  :status="errorIndicatorIds.includes(indicator.id!) ? 'error' : ''"
                  :placeholder="`请输入${indicator.indicatorName}`"
                  :rows="isRichText(indicator) ? 6 : 2"
                  :maxlength="getMaxLength(indicator)"
                  :show-count="!!getMaxLength(indicator)"
                  @change="onIndicatorChange(indicator.id!, indicator)"
                  @blur="validateRequired(indicator)"
                />

                <!-- 单选类型 - 单选框 -->
                <a-radio-group
                  v-else-if="indicator.valueType === 6"
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  class="flex flex-wrap gap-x-4 gap-y-2"
                  @change="onIndicatorChange(indicator.id!, indicator)"
                >
                  <a-radio
                    v-for="opt in parseOptions(indicator.valueOptions)"
                    :key="opt.value"
                    :value="opt.value"
                  >
                    {{ opt.label }}
                  </a-radio>
                </a-radio-group>

                <!-- 多选类型 - 复选框 -->
                <a-checkbox-group
                  v-else-if="indicator.valueType === 7"
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  class="flex flex-wrap gap-x-4 gap-y-2"
                  @change="onIndicatorChange(indicator.id!, indicator)"
                >
                  <a-checkbox
                    v-for="opt in parseOptions(indicator.valueOptions)"
                    :key="opt.value"
                    :value="opt.value"
                  >
                    {{ opt.label }}
                  </a-checkbox>
                </a-checkbox-group>

                <!-- 单选下拉类型 -->
                <a-select
                  v-else-if="indicator.valueType === 10"
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  :placeholder="`请选择${indicator.indicatorName}`"
                  class="w-full"
                  :status="errorIndicatorIds.includes(indicator.id!) ? 'error' : ''"
                  :show-search="getShowSearch(indicator)"
                  allow-clear
                  @change="onIndicatorChange(indicator.id!, indicator)"
                >
                  <a-select-option
                    v-for="opt in parseOptions(indicator.valueOptions)"
                    :key="opt.value"
                    :value="opt.value"
                  >
                    {{ opt.label }}
                  </a-select-option>
                </a-select>

                <!-- 多选下拉类型 -->
                <a-select
                  v-else-if="indicator.valueType === 11"
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  :placeholder="`请选择${indicator.indicatorName}`"
                  mode="multiple"
                  class="w-full"
                  :status="errorIndicatorIds.includes(indicator.id!) ? 'error' : ''"
                  :show-search="getShowSearch(indicator)"
                  allow-clear
                  @change="onIndicatorChange(indicator.id!, indicator)"
                >
                  <a-select-option
                    v-for="opt in parseOptions(indicator.valueOptions)"
                    :key="opt.value"
                    :value="opt.value"
                  >
                    {{ opt.label }}
                  </a-select-option>
                </a-select>

                <!-- 日期区间类型 -->
                <a-range-picker
                  v-else-if="indicator.valueType === 8"
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  :show-time="getDateFormat(indicator).includes('HH')"
                  :format="getDateFormat(indicator)"
                  class="w-full"
                  @change="onIndicatorChange(indicator.id!, indicator)"
                />

                <!-- 文件上传类型（多文件） -->
                <div v-else-if="indicator.valueType === 9" class="w-full">
                  <div class="flex flex-wrap items-center gap-2">
                    <!-- 已上传文件列表（显示在上传按钮左侧） -->
                    <div v-if="getFileList(indicator.id!).length > 0" class="flex flex-wrap gap-2">
                      <div
                        v-for="(file, index) in getFileList(indicator.id!)"
                        :key="index"
                        class="relative flex flex-col items-center justify-center w-24 h-24 border-2 border-dashed border-blue-400 rounded-lg bg-blue-50 group hover:border-red-400 transition-colors"
                      >
                        <IconifyIcon icon="lucide:file-text" class="text-xl text-blue-500" />
                        <div class="mt-1 text-xs text-gray-600 truncate w-20 text-center" :title="file.name">
                          {{ file.name }}
                        </div>
                        <button
                          type="button"
                          class="absolute -top-2 -right-2 w-5 h-5 bg-red-500 text-white rounded-full opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center"
                          @click.stop="handleFileRemove(file, indicator.id!, indicator)"
                        >
                          <IconifyIcon icon="lucide:x" class="text-xs" />
                        </button>
                      </div>
                    </div>
                    
                    <!-- 上传按钮 -->
                    <Upload
                      :before-upload="(file: FileType) => handleFileUpload(file, indicator)"
                      :show-upload-list="false"
                      :disabled="getFileList(indicator.id!).length >= getMaxFileCount(indicator)"
                      :accept="getAcceptTypes(indicator) ? '.' + getAcceptTypes(indicator).replace(/,/g, ',.') : undefined"
                      multiple
                    >
                      <div
                        class="flex flex-col items-center justify-center w-24 h-24 border-2 border-dashed rounded-lg cursor-pointer hover:border-blue-400 transition-colors"
                        :class="getFileList(indicator.id!).length >= getMaxFileCount(indicator) ? 'opacity-50 cursor-not-allowed' : ''"
                      >
                        <IconifyIcon icon="lucide:plus" class="text-2xl text-gray-400" />
                        <div class="mt-1 text-xs text-gray-400">点击上传</div>
                        <div class="text-xs text-gray-300">最多{{ getMaxFileCount(indicator) }}个</div>
                      </div>
                    </Upload>
                  </div>
                </div>

                <!-- 统一的错误提示：每个指标只显示一次 -->
                <div v-if="indicatorErrors[indicator.id!]" class="text-red-500 text-xs mt-1">
                  {{ indicatorErrors[indicator.id!] }}
                </div>
              </a-form-item>
            </div>
          </a-collapse-panel>
        </a-collapse>
      </div>
    </div>
  </Modal>
</template>
