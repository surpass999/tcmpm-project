<script lang="ts" setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';

import dayjs from 'dayjs';

import { useVbenModal } from '@vben/common-ui';

import type { FileType } from 'ant-design-vue/es/upload/interface';

import {
  Button,
  Card,
  Collapse,
  CollapsePanel,
  Descriptions,
  Empty,
  Form,
  Input,
  message,
  Select,
  Space,
  Spin,
  Tabs,
  Tag,
  Upload,
} from 'ant-design-vue';

import { getDictOptions } from '@vben/hooks';
import { DICT_TYPE } from '@vben/constants';

import { PlusOutlined } from '@vben/icons';

import {
  createProjectProcess,
  getProcessFormConfig,
  getProject,
  getProjectProcess,
  saveProcessFormData,
  syncProcessIndicator,
  updateProjectProcess,
  validateProcessFormData,
} from '#/api/declare/project';
import { uploadFile } from '#/api/infra/file';
import { getProcessIndicatorConfigListByProcessType } from '#/api/declare/process-indicator-config';
import type { IndicatorFormItem, TextFieldConfig } from '#/api/declare/project';

import IndicatorForm from './IndicatorForm.vue';
import TextFieldForm from './TextFieldForm.vue';

// 弹窗 API - 用于获取 Modal 组件和 modalApi
const [Modal, modalApi] = useVbenModal({
  destroyOnClose: true,
  showConfirmButton: false,
  showCancelButton: false,
  async onOpenChange(isOpen: boolean) {
    if (isOpen) {
      const data = getModalData();
      console.log('[DEBUG onOpenChange] modalData:', data);
      const processType = data?.process?.processType || data?.processType || props.processType || 1;
      console.log('[DEBUG onOpenChange] bizStatus from data:', data?.bizStatus);

      // 更新当前编辑的业务状态
      currentBizStatus.value = data?.bizStatus;

      if (props.projectId && processType) {
        indicatorValues.value = {};
        textFieldValues.value = {};
        attachmentIds.value = '';
        formConfig.value = null;
        formData.value.id = undefined;
        formData.value.processType = processType;

        if (data?.process?.id) {
          // 编辑模式：直接用 loadFormConfig 一次性加载完整配置（含 textFields 和已有数据）
          await loadFormConfig(data.process.id);
          formData.value.id = data.process.id;
          formData.value.processType = formConfig.value?.processType || processType;
        } else {
          // 新建模式：加载项目信息和指标配置
          await loadProjectAndIndicators(processType);
        }
      }
    } else {
      // 弹窗关闭时清理数据
      await nextTick();
      indicatorValues.value = {};
      textFieldValues.value = {};
      attachmentIds.value = '';
      formConfig.value = null;
      currentBizStatus.value = undefined;
    }
  },
});

// 从弹窗参数中获取数据
const getModalData = () => {
  return (modalApi.getData() as any) || {};
};

// 当前编辑的业务状态（RECTIFICATION_SUBMIT 时可编辑）
const currentBizStatus = ref<string | undefined>(undefined);

const AEmpty = Empty;

interface Props {
  // 项目ID
  projectId: number;
  // 过程类型
  processType?: number;
  // 是否是新增模式
  isNew?: boolean;
  // 过程记录数据
  process?: {
    id?: number;
    projectId?: number;
    processType?: number;
    processTitle?: string;
    processData?: string;
    indicatorValues?: string;
    status?: number;
    reportPeriodStart?: string;
    reportPeriodEnd?: string;
  } | null;
  // 是否只读
  readonly?: boolean;
  // 业务状态（RECTIFICATION_SUBMIT 时可编辑）
  bizStatus?: string;
  // 刷新回调
  onRefresh?: () => void;
}

const props = defineProps<Props>();

const emit = defineEmits(['success', 'close']);

// 是否是新增模式 - 根据 formData.id 判断（数据加载后 formData.id 有值）
const isNewMode = computed(() => !formData.value.id);

// 保存从弹窗参数传入的 processType
const passedProcessType = ref<number | undefined>(undefined);

// 弹窗表单数据（基本信息）
const formData = ref<{
  id?: number;
  projectId: number;
  processType: number;
  processTitle: string;
  reportPeriodStart?: string;
  reportPeriodEnd?: string;
  indicatorValues: string;
  processData: string;
}>({
  projectId: 0,
  processType: 1,
  processTitle: '',
  indicatorValues: '{}',
  processData: '{}',
});

// 填报数据
const indicatorValues = ref<Record<string, any>>({});
const textFieldValues = ref<Record<string, any>>({});
const attachmentIds = ref('');

// 安全解析 JSON
function safeParseJSON(str: any, defaultValue: any = {}) {
  if (!str) return defaultValue;
  try {
    return typeof str === 'object' ? str : JSON.parse(str);
  } catch (e) {
    console.error('JSON解析失败:', e, str);
    return defaultValue;
  }
}

/** 将后端返回的日期规范为 YYYY-MM-DD，供 RangePicker valueFormat 使用，避免 Invalid Date */
function normalizeDateStr(v: any): string | undefined {
  if (v == null || v === '') return undefined;
  const d = dayjs(v);
  return d.isValid() ? d.format('YYYY-MM-DD') : undefined;
}

// 加载过程数据
async function loadProcessData(processId: number) {
  if (!processId) return;

  try {
    const processData = await getProjectProcess(processId);
    if (!processData) {
      console.warn('[loadProcessData] 数据为空');
      return;
    }

    // 规范日期为 YYYY-MM-DD，避免 RangePicker 出现 Invalid Date
    const startStr = normalizeDateStr(processData.reportPeriodStart);
    const endStr = normalizeDateStr(processData.reportPeriodEnd);

    // 附件 ID：后端 VO 可能有 attachmentIds 或 fileIds
    const attIds = (processData as any).attachmentIds ?? (processData as any).fileIds ?? '';

    // 设置 formData 基本信息
    formData.value = {
      ...formData.value,
      id: processData.id,
      projectId: processData.projectId,
      processType: processData.processType,
      processTitle: processData.processTitle || '',
      reportPeriodStart: startStr,
      reportPeriodEnd: endStr,
      indicatorValues: processData.indicatorValues || '{}',
      processData: processData.processData || '{}',
    };

    // 加载已保存的填报数据（后端已返回 processData、indicatorValues）
    indicatorValues.value = safeParseJSON(processData.indicatorValues);
    textFieldValues.value = safeParseJSON(processData.processData);
    attachmentIds.value = attIds;

    // 更新 formConfig 中的数据
    if (formConfig.value) {
      formConfig.value.processId = processData.id;
      formConfig.value.processData = processData.processData || '{}';
      formConfig.value.indicatorValues = processData.indicatorValues || '{}';
      formConfig.value.attachmentIds = attIds;
      formConfig.value.status = processData.status;
      formConfig.value.processTitle = processData.processTitle;
      formConfig.value.reportPeriodStart = startStr;
      formConfig.value.reportPeriodEnd = endStr;
      formConfig.value.createName = (processData as any).createName;
      formConfig.value.createTime = (processData as any).createTime;
    }

    formData.value.id = processData.id;
    formData.value.processTitle = processData.processTitle || '';
    formData.value.reportPeriodStart = startStr;
    formData.value.reportPeriodEnd = endStr;
  } catch (e) {
    console.error('[loadProcessData] 请求过程数据失败:', e);
    message.error('加载过程数据失败');
  }
}

// 表单配置
const formConfig = ref<{
  processId: number;
  processType: number;
  processTypeName: string;
  processTitle: string;
  projectId: number;
  projectName: string;
  projectType: number;
  projectTypeName: string;
  projectLeader: string;
  constructionPeriod: string;
  projectStatus: string;
  projectStatusName: string;
  indicators: IndicatorFormItem[];
  textFields: TextFieldConfig[];
  status: string;
  statusName: string;
  createName: string;
  createTime: string;
  processData: string;
  indicatorValues: string;
  attachmentIds: string;
  indicatorVersion: number;
} | null>(null);

// 加载状态
const configLoading = ref(false);
const loadingProjectAndIndicators = ref(false);
const submitting = ref(false);

// 组件挂载时，如果已经有 props 则加载数据
onMounted(() => {
  // 由于 destroyOnClose: true，每次打开弹窗都是新实例
  // 数据加载在 onOpenChange 中处理，这里不需要额外处理
});

// 当前激活的Tab
const activeTab = ref('indicator');

// 指标分类字典
const indicatorCategories = computed(() =>
  getDictOptions(DICT_TYPE.DECLARE_INDICATOR_CATEGORY, 'number')
);

// 获取分类名称
function getCategoryName(category: number): string {
  const found = indicatorCategories.value.find(
    (item: any) => Number(item.value) === category
  );
  return found?.label || `分类${category}`;
}

// 指标分类分组
interface IndicatorGroup {
  category: number;
  categoryName: string;
  indicators: IndicatorFormItem[];
}

// 默认展开所有分类
const indicatorGroups = computed<IndicatorGroup[]>(() => {
  const indicators = formConfig.value?.indicators || [];
  const groups: IndicatorGroup[] = [];
  const categoryMap = new Map<number, IndicatorGroup>();

  indicators.forEach((indicator) => {
    if (!categoryMap.has(indicator.category)) {
      const categoryName = getCategoryName(indicator.category);
      const group: IndicatorGroup = {
        category: indicator.category,
        categoryName,
        indicators: [],
      };
      categoryMap.set(indicator.category, group);
      groups.push(group);
    }
    categoryMap.get(indicator.category)!.indicators.push(indicator);
  });

  // 按分类排序
  groups.sort((a, b) => a.category - b.category);
  return groups;
});

// 折叠面板默认展开所有分类
const activeCollapseKeys = ref<number[]>([]);

// 初始化折叠面板展开所有分类
watch(
  () => indicatorGroups.value,
  (groups) => {
    if (groups.length > 0) {
      activeCollapseKeys.value = groups.map((g) => g.category);
    }
  },
  { immediate: true }
);

// 过程类型配置（从字典获取，过滤掉第7项成果与流通）
const processTypeOptions = (
  getDictOptions(DICT_TYPE.DECLARE_PROCESS_TYPE, 'number') as any[]
).filter((item: { value: number }) => item.value !== 7);

// 报告周期 RangePicker 的 value：仅在有合法 YYYY-MM-DD 时传数组，否则传 undefined 避免 Invalid Date
const reportPeriodRange = computed(() => {
  const s = formData.value.reportPeriodStart;
  const e = formData.value.reportPeriodEnd;
  if (!s && !e) return undefined;
  return [s || undefined, e || undefined] as [string | undefined, string | undefined];
});

// 弹窗标题 - 包含过程类型
const modalTitle = computed(() => {
  const processTypeName = getProcessTypeName(props.processType || 0);
  if (props.readonly) return `查看填报${processTypeName ? '-' + processTypeName : ''}`;
  // DRAFT/SAVED/包含RETURNED的状态显示编辑填报
  const status = formConfig.value?.status;
  if (!status || ['DRAFT', 'SAVED'].includes(status) || status.includes('RETURNED')) {
    return `编辑填报${processTypeName ? '-' + processTypeName : ''}`;
  }
  return `填报${processTypeName ? '-' + processTypeName : ''}`;
});

// 是否可以编辑（DRAFT、SAVED、包含 RETURNED 的状态、RECTIFICATION_SUBMIT 可以编辑）
const canEdit = computed(() => {
  if (props.readonly) return false;
  // 新建过程记录 或者 已有记录是 DRAFT/SAVED/包含RETURNED的状态
  if (!formData.value.id) return true;

  const status = formConfig.value?.status;
  console.log('[DEBUG canEdit] currentBizStatus:', currentBizStatus.value, 'status:', status, 'formData.id:', formData.value.id);
  // RECTIFICATION_SUBMIT 状态可以编辑（整改提交后的编辑）
  if (status === 'RECTIFICATION_SUBMIT' || currentBizStatus.value === 'RECTIFICATION_SUBMIT') {
    return true;
  }
  return !status || ['DRAFT', 'SAVED'].includes(status) || status.includes('RETURNED');
});

// 是否有指标配置
const hasIndicators = computed(() => {
  return formConfig.value?.indicators && formConfig.value.indicators.length > 0;
});

// 是否有文本字段配置
const hasTextFields = computed(() => {
  return formConfig.value?.textFields && formConfig.value.textFields.length > 0;
});

// 项目状态颜色映射
const getProjectStatusColor = (status: string) => {
  const colorMap: Record<string, string> = {
    FILING: 'blue',
    APPROVED: 'green',
    REJECTED: 'red',
    ARCHIVED: 'default',
  };
  return colorMap[status] || 'default';
};

// 填报状态颜色映射
const getProcessStatusColor = (status: string) => {
  const colorMap: Record<string, string> = {
    DRAFT: 'default',
    SAVED: 'processing',
    SUBMITTED: 'processing',
    AUDITING: 'orange',
    APPROVED: 'success',
    REJECTED: 'error',
  };
  return colorMap[status] || 'default';
};

// 加载表单配置
async function loadFormConfig(processId: number) {
  configLoading.value = true;
  try {
    const data = await getProcessFormConfig(processId);
    formConfig.value = data;

    // 直接构建完整的 formData（和 loadProcessData 保持一致）
    formData.value = {
      id: data.processId,
      projectId: data.projectId || 0,
      processType: data.processType || 1,
      processTitle: data.processTitle || '',
      reportPeriodStart: normalizeDateStr(data.reportPeriodStart),
      reportPeriodEnd: normalizeDateStr(data.reportPeriodEnd),
      indicatorValues: data.indicatorValues || '{}',
      processData: data.processData || '{}',
    };
    
    // 回显指标值
    if (data.indicatorValues) {
      try {
        indicatorValues.value = JSON.parse(data.indicatorValues);
      } catch {
        indicatorValues.value = {};
      }
    }

    // 回显文本字段值
    if (data.processData) {
      try {
        textFieldValues.value = JSON.parse(data.processData);
      } catch {
        textFieldValues.value = {};
      }
    }

    // 回显附件
    attachmentIds.value = data.attachmentIds || '';
  } catch (e) {
    console.error('加载表单配置失败', e);
    message.error('加载表单配置失败');
  } finally {
    configLoading.value = false;
  }
}

// 加载项目信息和指标配置（新建时使用）
async function loadProjectAndIndicators(processType: number) {
  console.log('[loadProjectAndIndicators] 开始执行, processType:', processType, 'projectId:', props.projectId);
  if (!props.projectId) {
    console.warn('[loadProjectAndIndicators] projectId 为空，直接返回');
    return;
  }
  // 防止 watch 多次触发导致并发调用，加载锁
  if (loadingProjectAndIndicators.value) {
    console.log('[loadProjectAndIndicators] 正在加载中，跳过此次调用');
    return;
  }
  loadingProjectAndIndicators.value = true;

  configLoading.value = true;
  try {
    // 获取项目信息（包含项目类型）
    const projectInfo = await getProject(props.projectId);
    console.log('[loadProjectAndIndicators] 项目信息:', projectInfo);

    // 根据项目类型和过程类型获取已配置的指标列表
    const configs = await getProcessIndicatorConfigListByProcessType(
      processType,
      projectInfo.projectType,
    );
    console.log('[loadProjectAndIndicators] 指标配置:', configs);

    // 转换为 IndicatorFormItem 格式
    const indicators: IndicatorFormItem[] = configs.map((config: any) => ({
      indicatorId: config.indicatorId,
      indicatorCode: config.indicatorCode,
      indicatorName: config.indicatorName,
      unit: config.unit,
      category: config.category,
      valueType: config.valueType,
      isRequired: config.isRequired,
      sort: config.sort,
    }));

    // 构建表单配置
    formConfig.value = {
      processId: 0,
      processType: processType,
      processTypeName: getProcessTypeName(processType),
      processTitle: '',
      projectId: projectInfo.id,
      projectName: projectInfo.projectName,
      projectType: projectInfo.projectType,
      projectTypeName: projectInfo.projectTypeName || getProjectTypeName(projectInfo.projectType),
      projectLeader: projectInfo.projectLeader,
      constructionPeriod: projectInfo.constructionPeriodStart && projectInfo.constructionPeriodEnd
        ? `${projectInfo.constructionPeriodStart} ~ ${projectInfo.constructionPeriodEnd}`
        : '',
      projectStatus: projectInfo.status,
      projectStatusName: projectInfo.statusName,
      indicators,
      textFields: getTextFieldConfigs(processType),
      status: '',
      statusName: '',
      createName: '',
      createTime: '',
      processData: '{}',
      indicatorValues: '{}',
      attachmentIds: '',
      indicatorVersion: 0,
    };

    // 同步更新 formData 中的 projectId
    formData.value.projectId = projectInfo.id;
    console.log('[loadProjectAndIndicators] formConfig 设置完成, indicators.length:', indicators.length);
  } catch (e) {
    console.error('加载项目信息失败', e);
    message.error('加载项目信息失败');
  } finally {
    configLoading.value = false;
    loadingProjectAndIndicators.value = false;
  }
}

// 获取过程类型名称（从字典选项中获取）
function getProcessTypeName(processType: number): string {
  const options = processTypeOptions;
  const found = options.find((item: any) => item.value === processType);
  return found?.label || '';
}

// 获取项目类型名称
function getProjectTypeName(projectType: number): string {
  const projectTypeMap: Record<number, string> = {
    0: '通用类型',
    1: '综合型',
    2: '中医电子病历型',
    3: '智慧中药房型',
    4: '名老中医传承型',
    5: '中医临床科研型',
    6: '中医智慧医共体型',
  };
  return projectTypeMap[projectType] || '';
}

// 获取文本字段配置（与后端 ProcessFormServiceImpl.getTextFieldConfigs 保持一致）
function getTextFieldConfigs(processType: number) {
  const configs: any[] = [
    // 通用字段：建设进展
    {
      fieldCode: 'progress',
      fieldName: '建设进展',
      isRequired: false,
      maxLength: 2000,
      fieldType: 2,
    },
    // 通用字段：存在问题
    {
      fieldCode: 'problems',
      fieldName: '存在问题',
      isRequired: false,
      maxLength: 1000,
      fieldType: 2,
    },
    // 通用字段：下一步计划
    {
      fieldCode: 'nextPlan',
      fieldName: '下一步计划',
      isRequired: false,
      maxLength: 1000,
      fieldType: 2,
    },
  ];

  // 根据过程类型添加特定字段
  if (processType) {
    switch (processType) {
      case 4: // 中期评估
        configs.push(
          { fieldCode: 'selfEvaluation', fieldName: '自评结论', isRequired: true, maxLength: 2000, fieldType: 2 },
          { fieldCode: 'rectificationPlan', fieldName: '整改措施', isRequired: false, maxLength: 1000, fieldType: 2 }
        );
        break;
      case 5: // 整改记录
        configs.push(
          { fieldCode: 'rectifyContent', fieldName: '整改内容', isRequired: true, maxLength: 1000, fieldType: 2 },
          { fieldCode: 'rectifyMeasures', fieldName: '整改措施', isRequired: true, maxLength: 1000, fieldType: 2 },
          { fieldCode: 'rectifyResult', fieldName: '整改结果', isRequired: false, maxLength: 1000, fieldType: 2 }
        );
        break;
      case 6: // 验收申请
        configs.push(
          { fieldCode: 'acceptanceSummary', fieldName: '建设总结', isRequired: true, maxLength: 5000, fieldType: 2 },
          { fieldCode: 'achievementDesc', fieldName: '建设成果说明', isRequired: false, maxLength: 2000, fieldType: 2 }
        );
        break;
    }
  }

  return configs;
}

// 保存草稿
async function handleSaveDraft() {
  await saveForm(1);
}

// 提交
async function handleSubmit() {
  // 先验证（通过 formData.value.id 判断是否编辑模式）
  if (formData.value.id) {
    try {
      const validateRes = await validateProcessFormData({
        processId: formData.value.id,
        formData: textFieldValues.value,
        indicatorValues: indicatorValues.value,
      });

      if (!validateRes.valid) {
        message.warning(validateRes.errors?.join('，') || '验证失败');
        return;
      }
    } catch (e: any) {
      // 验证接口可能不存在，静默处理
      console.log('验证跳过', e);
    }
  }

  // 保存表单
  await saveForm(2);
}

// 保存表单
async function saveForm(operateType: number) {
  // 先保存基本信息（创建或更新过程记录）
  if (!formData.value.processTitle.trim()) {
    message.warning('请输入过程标题');
    return;
  }

  submitting.value = true;
  try {
    let processId = formData.value.id;

    if (!processId) {
      // 创建过程记录（不传status，由后端默认设置为DRAFT）
      const createRes = await createProjectProcess({
        projectId: formData.value.projectId,
        processType: formData.value.processType,
        processTitle: formData.value.processTitle,
        reportPeriodStart: formData.value.reportPeriodStart,
        reportPeriodEnd: formData.value.reportPeriodEnd,
      } as any);
      processId = createRes;
    } else {
      // 更新过程记录基本信息
      await updateProjectProcess({
        id: processId,
        processTitle: formData.value.processTitle,
        reportPeriodStart: formData.value.reportPeriodStart,
        reportPeriodEnd: formData.value.reportPeriodEnd,
      } as any);
    }

    // 保存填报数据
    await saveProcessFormData({
      processId,
      formData: textFieldValues.value,
      indicatorValues: indicatorValues.value,
      attachmentIds: attachmentIds.value,
      // operateType: 1=草稿(DRAFT), 2=提交(SUBMITTED)
      status: operateType === 1 ? 'DRAFT' : 'SAVED',
    });

    message.success(operateType === 1 ? '保存草稿成功' : '保存成功');
    emit('success');
    // 调用刷新回调
    if (props.onRefresh) {
      props.onRefresh();
    }
    emit('close');
    await modalApi.close();
  } catch (e: any) {
    console.error('保存失败', e);
    message.error(e.message || '保存失败');
  } finally {
    submitting.value = false;
  }
}

// 手动同步指标
async function handleSyncIndicator() {
  const id = formData.value.id;
  if (!id) return;

  try {
    await syncProcessIndicator(id);
    message.success('同步成功');
    await loadFormConfig(id);
  } catch (e: any) {
    message.error(e.message || '同步失败');
  }
}

// 附件上传变化处理
// 上传佐证材料
async function handleAttachmentUpload(file: FileType) {
  try {
    const result = await uploadFile({
      file: file as any,
      directory: 'declare/process',
    });

    // uploadFile 返回的是文件URL字符串（如 http://localhost:48080/...）
    attachmentIds.value = result as string;
    message.success('上传成功');
  } catch (error) {
    console.error('上传失败:', error);
    message.error('上传失败');
  }
  return false; // 阻止默认上传行为
}

// 监听过程类型变化，重新加载指标配置
watch(
  () => formData.value.processType,
  async (newProcessType, oldProcessType) => {
    console.log('[watch] formData.processType 变化:', { newProcessType, oldProcessType, isNewMode: isNewMode.value, processId: formData.value.id });
    // 新建模式 + processType 有实际变化 + projectId 存在，则重新加载指标配置
    if (!formData.value.id && oldProcessType != null && newProcessType !== oldProcessType && props.projectId) {
      console.log('[watch] 触发重新加载指标');
      await loadProjectAndIndicators(newProcessType);
    }
  },
);
</script>

<template>
  <Modal
    class="w-[85%]"
    :title="modalTitle"
    :confirm-loading="submitting"
    :footer="null"
    :max-width="1600"
    :body-style="{ padding: '24px' }"
  >
    <Spin :spinning="configLoading">
      <!-- 项目信息头部 -->
      <Card v-if="formConfig" :bordered="false" class="project-info-card">
        <Descriptions :column="3" bordered size="small">
          <Descriptions.Item label="项目名称">
            <span class="project-name">{{ formConfig.projectName }}</span>
          </Descriptions.Item>
          <Descriptions.Item label="项目类型">
            <Tag color="blue">{{ formConfig.projectTypeName }}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="项目负责人">
            {{ formConfig.projectLeader }}
          </Descriptions.Item>
          <Descriptions.Item label="建设周期">
            {{ formConfig.constructionPeriod }}
          </Descriptions.Item>
          <Descriptions.Item label="项目状态">
            <Tag :color="getProjectStatusColor(formConfig.projectStatus)">
              {{ formConfig.projectStatusName }}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="填报状态">
            <Tag :color="getProcessStatusColor(formConfig.status)">
              {{ formConfig.statusName }}
            </Tag>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <!-- 填报表单 -->
      <Form
        :model="formData"
        :disabled="!canEdit"
        layout="vertical"
        class="process-form"
      >
        <!-- 基本信息 -->
        <template v-if="isNewMode">
          <!-- 新增模式：显示标题、报告周期、过程类型选择 -->
          <Form.Item
            label="过程标题"
            name="processTitle"
            :rules="[{ required: true, message: '请输入过程标题' }]"
          >
            <Input
              v-model:value="formData.processTitle"
              placeholder="如：2025年上半年建设过程"
            />
          </Form.Item>

          <Form.Item label="报告周期" name="reportPeriod">
            <a-range-picker
              :value="reportPeriodRange"
              @update:value="(val: [string, string] | null) => {
                if (val) {
                  formData.reportPeriodStart = val[0];
                  formData.reportPeriodEnd = val[1];
                }
              }"
              value-format="YYYY-MM-DD"
              format="YYYY-MM-DD"
              style="width: 100%"
            />
          </Form.Item>

          <!-- <Form.Item label="过程类型" name="processType">
            <Select
              v-model:value="formData.processType"
              :options="processTypeOptions"
              placeholder="请选择过程类型"
            />
          </Form.Item> -->
        </template>
        <template v-else>
          <!-- 编辑模式：显示标题和报告周期（不可修改过程类型） -->
          <Form.Item label="过程标题">
            <Input v-model:value="formData.processTitle" :disabled="true" />
          </Form.Item>

          <Form.Item label="报告周期">
            <a-range-picker
              :value="reportPeriodRange"
              @update:value="(val: [string, string] | null) => {
                if (val) {
                  formData.reportPeriodStart = val[0];
                  formData.reportPeriodEnd = val[1];
                }
              }"
              value-format="YYYY-MM-DD"
              format="YYYY-MM-DD"
              style="width: 100%"
              :disabled="!canEdit"
            />
          </Form.Item>
        </template>

        <!-- 填报Tab区域（新建或编辑时都显示，只要有配置） -->
        <template v-if="formConfig && (hasIndicators || hasTextFields)">
          <Tabs v-model:activeKey="activeTab" type="card">
            <!-- 指标填报 -->
            <Tabs.TabPane key="indicator" tab="指标填报" :forceRender="true">
              <!-- 使用折叠面板按分类显示指标 -->
              <a-collapse
                v-if="hasIndicators"
                v-model:activeKey="activeCollapseKeys"
              >
                <a-collapse-panel
                  v-for="group in indicatorGroups"
                  :key="group.category"
                  :header="`${group.categoryName} (${group.indicators.length}个指标)`"
                >
                  <IndicatorForm
                    v-model="indicatorValues"
                    :indicators="group.indicators"
                    :disabled="!canEdit"
                  />
                </a-collapse-panel>
              </a-collapse>
              <AEmpty v-else description="暂无指标配置" />
            </Tabs.TabPane>

            <!-- 文本填报 - 只有在有文本字段时才显示 -->
            <Tabs.TabPane v-if="hasTextFields" key="text" tab="文本填报" :forceRender="true">
              <TextFieldForm
                v-model="textFieldValues"
                :fields="formConfig!.textFields"
                :disabled="!canEdit"
              />
            </Tabs.TabPane>

            <!-- 佐证材料 -->
            <Tabs.TabPane key="attachment" tab="佐证材料">
              <div class="attachment-upload">
                <Upload
                  :before-upload="handleAttachmentUpload"
                  :file-list="
                    attachmentIds
                      ? [
                          {
                            uid: '-1',
                            name: '附件',
                            status: 'done',
                            url: attachmentIds,
                          },
                        ]
                      : []
                  "
                  :max-count="1"
                  :disabled="!canEdit"
                >
                  <Button :disabled="!canEdit">
                    <PlusOutlined />
                    上传佐证材料
                  </Button>
                </Upload>
                <div class="attachment-tip">
                  请上传相关佐证材料，如：报告文件、图片等
                </div>
              </div>

              <!-- 指标同步信息 -->
              <div v-if="formConfig?.indicatorVersion" class="sync-info">
                <Space>
                  <span>已同步次数：{{ formConfig.indicatorVersion }}</span>
                  <Button
                    v-if="canEdit"
                    type="link"
                    size="small"
                    @click="handleSyncIndicator"
                  >
                    手动同步
                  </Button>
                </Space>
              </div>
            </Tabs.TabPane>
          </Tabs>
        </template>
      </Form>

      <!-- 底部操作栏 -->
      <div v-if="canEdit" class="modal-footer">
        <Space>
          <Button @click="() => modalApi.close()">取消</Button>
          <Button  :loading="submitting" @click="handleSaveDraft">保存草稿</Button>
          <Button type="primary" :loading="submitting" @click="handleSubmit">
            保存记录
          </Button>
        </Space>
      </div>
    </Spin>
  </Modal>
</template>

<style lang="scss" scoped>
// 项目信息卡片 - 使用主题色 CSS 变量
.project-info-card {
  margin-bottom: 20px;
  border-radius: 8px;
  overflow: hidden;

  :deep(.ant-card-body) {
    padding: 0;
    background: linear-gradient(90deg, hsl(var(--primary)) 0%, hsl(var(--primary-hover, var(--primary))) 100%);
  }

  :deep(.ant-descriptions) {
    background: transparent;

    .ant-descriptions-item-label {
      color: rgba(255, 255, 255, 0.85);
      font-weight: 500;
      background: transparent;
    }

    .ant-descriptions-item-content {
      color: #fff;
      background: transparent;

      .project-name {
        font-weight: 600;
        font-size: 14px;
      }
    }

    .ant-descriptions-bordered .ant-descriptions-item-label,
    .ant-descriptions-bordered .ant-descriptions-item-content {
      background: rgba(255, 255, 255, 0.1);
      border-color: rgba(255, 255, 255, 0.2);
    }

    .ant-tag {
      border: none;
    }
  }
}

// 填报表单区域
.process-form {
  min-height: 300px;

  // 表单项样式优化
  :deep(.ant-form-item) {
    margin-bottom: 20px;

    .ant-form-item-label {
      padding-bottom: 8px;

      label {
        font-weight: 600;
        color: #303133;
      }
    }
  }

  // 输入框样式
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
}

// Tab 样式优化
:deep(.ant-tabs) {
  .ant-tabs-nav {
    margin-bottom: 20px;

    &::before {
      border-bottom: 2px solid #f0f0f0;
    }

    .ant-tabs-tab {
      padding: 12px 20px;
      font-size: 14px;
      font-weight: 500;
      color: #606266;
      transition: all 0.3s;

      &:hover {
        color: hsl(var(--primary));
      }

      &.ant-tabs-tab-active {
        .ant-tabs-tab-btn {
          color: hsl(var(--primary));
        }
      }
    }

    .ant-tabs-ink-bar {
      background: hsl(var(--primary));
      height: 3px;
      border-radius: 2px;
    }
  }
}

// 折叠面板样式优化 - 使用主题色
:deep(.ant-collapse) {
  border: none;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;

  .ant-collapse-item {
    border: none;
    margin-bottom: 12px;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    &:last-child {
      margin-bottom: 0;
    }

    .ant-collapse-header {
      padding: 14px 16px !important;
      font-size: 14px;
      font-weight: 600;
      color: #303133;
      background: #fafafa;
      border-bottom: 1px solid #f0f0f0;
      transition: all 0.3s;

      &:hover {
        background: #f0f0f0;
      }

      .ant-collapse-arrow {
        color: hsl(var(--primary));
      }
    }

    .ant-collapse-content {
      border-top: none;

      .ant-collapse-content-box {
        padding: 16px;
      }
    }
  }
}

// 附件上传区域
.attachment-upload {
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px dashed #d9d9d9;
  transition: all 0.3s;

  &:hover {
    border-color: hsl(var(--primary));
    background: #f0f7ff;
  }

  :deep(.ant-upload) {
    .ant-btn {
      border-radius: 6px;
      height: 36px;
      padding: 0 20px;
      background: hsl(var(--primary));
      border: none;
      color: #fff;
      font-weight: 500;
      transition: all 0.3s;

      &:hover {
        background: hsl(var(--primary-hover, var(--primary)));
      }

      &:disabled {
        background: #d9d9d9;
      }
    }
  }

  .attachment-tip {
    margin-top: 12px;
    color: #8c8c8c;
    font-size: 13px;
  }
}

// 同步信息
.sync-info {
  margin-top: 16px;
  padding: 12px 16px;
  background: #f0f7ff;
  border-radius: 6px;
  font-size: 13px;
  color: hsl(var(--primary));
  border: 1px solid hsl(var(--primary) / 30%);
}

// 底部操作栏
.modal-footer {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  text-align: right;

  :deep(.ant-btn) {
    height: 36px;
    padding: 0 24px;
    border-radius: 6px;
    font-weight: 500;
    transition: all 0.3s;

    &:not(.ant-btn-primary) {
      border-color: #d9d9d9;
      color: #595959;

      &:hover {
        color: hsl(var(--primary));
        border-color: hsl(var(--primary));
      }
    }

    &.ant-btn-primary {
      background: hsl(var(--primary));
      border: none;

      &:hover {
        background: hsl(var(--primary-hover, var(--primary)));
      }
    }
  }
}

// 空状态样式
:deep(.ant-empty-description) {
  color: #8c8c8c;
}
</style>
