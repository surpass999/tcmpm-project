<script setup lang="ts">
import type {
  DeclareProgressReport,
  IndicatorCondition,
  IndicatorConditionGroup,
  NationalSearchParams,
} from '#/api/declare/progress-report';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';

import { computed, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { nationalSearch, OPERATORS } from '#/api/declare/progress-report';
import { getIndicatorsByBusinessType } from '#/api/declare/indicator';

const emit = defineEmits<{
  (e: 'search', data: DeclareProgressReport[], params: NationalSearchParams): void;
}>();

/** 项目类型选项 */
const PROJECT_TYPE_OPTIONS = [
  { label: '综合型', value: 1 },
  { label: '中医电子病历型', value: 2 },
  { label: '智慧中药房型', value: 3 },
  { label: '名老中医传承型', value: 4 },
  { label: '中医临床科研型', value: 5 },
  { label: '中医智慧医共体型', value: 6 },
];

/** 填报批次选项 */
const BATCH_OPTIONS = [
  { label: '第1期', value: 1 },
  { label: '第2期', value: 2 },
  { label: '第3期', value: 3 },
  { label: '第4期', value: 4 },
  { label: '第5期', value: 5 },
  { label: '第6期', value: 6 },
  { label: '第7期', value: 7 },
  { label: '第8期', value: 8 },
  { label: '第9期', value: 9 },
  { label: '第10期', value: 10 },
];

/** 填报状态选项 */
const REPORT_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已保存', value: 'SAVED' },
  { label: '待审批', value: 'SUBMITTED' },
  // { label: '省级审核中', value: 'HOSPITAL_AUDITING' },
  // { label: '省级通过', value: 'PROVINCE_APPROVED' },
  // { label: '省级驳回', value: 'PROVINCE_REJECTED' },
  // { label: '省级退回', value: 'PROVINCE_RETURNED' },
  // { label: '国家局审批中', value: 'NATION_AUDITING' },
  { label: '国家局通过', value: 'NATION_APPROVED' },
  { label: '国家局驳回', value: 'NATION_REJECTED' },
  // { label: '国家局退回', value: 'NATION_RETURNED' },
];

/** 省级审核状态选项 */
const PROVINCE_STATUS_OPTIONS = [
  { label: '未提交', value: 0 },
  { label: '审核中', value: 1 },
  { label: '已通过', value: 2 },
  { label: '已驳回', value: 3 },
];

/** 国家局上报状态选项 */
const NATIONAL_REPORT_STATUS_OPTIONS = [
  { label: '未上报', value: 0 },
  { label: '国家局审批中', value: 1 },
  { label: '已上报', value: 2 },
];

/** 年度选项 */
const YEAR_OPTIONS = [
  { label: '2028', value: 2028 },
  { label: '2027', value: 2027 },
  { label: '2026', value: 2026 },
  { label: '2025', value: 2025 },
  { label: '2024', value: 2024 },
];

/** 动态容器子字段类型 */
const FIELD_TYPES = [
  { label: '数字', value: 'number' },
  { label: '文本', value: 'text' },
  { label: '长文本', value: 'textarea' },
  { label: '单选', value: 'radio' },
  { label: '下拉', value: 'select' },
  { label: '多选', value: 'checkbox' },
  { label: '多选下拉', value: 'multiSelect' },
  { label: '日期', value: 'date' },
  { label: '日期区间', value: 'dateRange' },
];

// ========== 上次搜索状态（用于再次打开时回显） ==========
const lastSearchParams = ref<NationalSearchParams | null>(null);

// ========== 基本信息区（保持 ref，关闭时存入 lastSearchParams） ==========
const hospitalName = ref('');
const reportYear = ref<number | undefined>();
const reportBatch = ref<number | undefined>();
const reportStatus = ref<string | undefined>();
const provinceStatus = ref<number | undefined>();
const nationalReportStatus = ref<number | undefined>();
const projectType = ref<number | undefined>();

// ========== 指标条件区 ==========
const indicatorGroups = ref<IndicatorConditionGroup[]>([]);
const indicatorList = ref<DeclareIndicatorApi.Indicator[]>([]);
const indicatorListLoading = ref(false);

// 监听项目类型变化，加载指标列表
watch(projectType, async (newVal) => {
  if (newVal) {
    await loadIndicators(newVal);
    indicatorGroups.value = indicatorGroups.value.filter((g) => g.conditions.length > 0);
  } else {
    indicatorList.value = [];
    indicatorGroups.value = [];
  }
});

async function loadIndicators(type: number) {
  indicatorListLoading.value = true;
  try {
    const list = await getIndicatorsByBusinessType('process', type);
    // 过滤掉 type=9（文件上传）不可搜索的类型
    indicatorList.value = list.filter((ind) => ind.valueType !== 9);
  } catch {
    indicatorList.value = [];
  } finally {
    indicatorListLoading.value = false;
  }
}

const indicatorOptions = computed(() => {
  return indicatorList.value.map((ind) => ({
    label: `${ind.indicatorCode} - ${ind.indicatorName}`,
    value: ind.indicatorCode,
    valueType: ind.valueType,
    valueOptions: ind.valueOptions,
  }));
});

function getOperatorsForType(valueType: number) {
  return OPERATORS[valueType] || OPERATORS[2];
}

function getOperatorsByFieldType(fieldType: string) {
  const mapping: Record<string, number> = {
    number: 1,
    text: 2,
    textarea: 5,
    radio: 3,
    select: 6,
    checkbox: 7,
    multiSelect: 7,
    date: 4,
    dateRange: 8,
  };
  const type = mapping[fieldType] || 2;
  return OPERATORS[type] || OPERATORS[2];
}

function parseValueOptions(optionsStr: string): Array<{ label: string; value: string }> {
  if (!optionsStr) return [];
  try {
    const parsed = JSON.parse(optionsStr);
    if (Array.isArray(parsed)) {
      return parsed.map((item) => {
        if (typeof item === 'string') return { label: item, value: item };
        if (typeof item === 'object' && item !== null) {
          return { label: item.label || item.name || String(item.value), value: String(item.value) };
        }
        return { label: String(item), value: String(item) };
      });
    }
  } catch {
    // 如果不是 JSON，尝试按换行或逗号分隔
    return optionsStr.split(/[,\n]/).filter(Boolean).map((v) => {
      const trimmed = v.trim();
      return { label: trimmed, value: trimmed };
    });
  }
  return [];
}

/**
 * 解析容器指标的子字段列表（适配新结构 { mode, link, fields }）
 */
function parseContainerFields(optionsStr: string): Array<{ label: string; value: string; fieldType: string; options?: any }> {
  if (!optionsStr) return [];
  try {
    const parsed = JSON.parse(optionsStr);
    // 新对象格式：{ mode, link?, fields: [...] }
    if (parsed && typeof parsed === 'object' && Array.isArray(parsed.fields)) {
      return parsed.fields.map((f: any) => ({
        label: `${f.fieldLabel || f.fieldCode} (${f.fieldType})`,
        value: f.fieldCode,
      }));
    }
    // 旧数组格式（兼容）
    if (Array.isArray(parsed)) {
      return parsed.map((f: any) => ({
        label: `${f.fieldLabel || f.fieldCode} (${f.fieldType})`,
        value: f.fieldCode,
      }));
    }
  } catch { /* ignore */ }
  return [];
}

function getContainerFieldOptions(optionsStr: string) {
  return parseContainerFields(optionsStr);
}

function addConditionGroup() {
  indicatorGroups.value.push({
    innerLogic: 'OR',
    conditions: [],
  });
}

function removeConditionGroup(index: number) {
  indicatorGroups.value.splice(index, 1);
}

function addCondition(groupIndex: number) {
  const group = indicatorGroups.value[groupIndex];
  if (!group) return;
  group.conditions.push({
    indicatorId: 0,
    indicatorCode: '',
    valueType: 2,
    operator: 'eq',
    value: '',
    value2: '',
    fieldCode: '',
    fieldType: '',
    indicatorOptions: '',
  });
}

function removeCondition(groupIndex: number, condIndex: number) {
  const group = indicatorGroups.value[groupIndex];
  if (!group) return;
  group.conditions.splice(condIndex, 1);
}

function onIndicatorChange(groupIndex: number, condIndex: number, indicatorCode: string) {
  const group = indicatorGroups.value[groupIndex];
  if (!group) return;
  const cond = group.conditions[condIndex];
  if (!cond) return;

  const indicator = indicatorList.value.find((ind) => ind.indicatorCode === indicatorCode);
  if (indicator) {
    cond.indicatorId = indicator.id || 0;
    cond.valueType = indicator.valueType;
    cond.indicatorOptions = indicator.valueOptions;
    cond.fieldCode = '';
    cond.fieldType = '';
    cond.operator = '';
    cond.value = '';
    cond.value2 = '';

    // 动态容器：自动设置默认操作符
    if (indicator.valueType === 12) {
      cond.operator = 'contains';
    } else {
      const ops = getOperatorsForType(indicator.valueType);
      if (ops.length > 0) {
        cond.operator = ops[0].value;
      }
    }
  }
}

function onFieldTypeChange(groupIndex: number, condIndex: number, fieldType: string) {
  const group = indicatorGroups.value[groupIndex];
  if (!group) return;
  const cond = group.conditions[condIndex];
  if (!cond) return;

  cond.fieldType = fieldType;
  cond.operator = '';
  cond.value = '';
  cond.value2 = '';

  // 设置默认值操作符
  const ops = getOperatorsByFieldType(fieldType);
  if (ops.length > 0) {
    cond.operator = ops[0].value;
  }
}

function onContainerFieldChange(groupIndex: number, condIndex: number, fieldCode: string) {
  const group = indicatorGroups.value[groupIndex];
  if (!group) return;
  const cond = group.conditions[condIndex];
  if (!cond) return;

  // 根据 fieldCode 查找对应字段定义
  const fields = parseContainerFields(cond.indicatorOptions || '');
  const field = fields.find((f) => f.value === fieldCode);
  if (field) {
    cond.fieldType = field.fieldType;
    // 根据字段类型设置操作符
    const ops = getOperatorsByFieldType(field.fieldType);
    if (ops.length > 0) {
      cond.operator = ops[0].value;
    }
    cond.value = '';
    cond.value2 = '';
  }
}

function needsValueInput(operator: string): boolean {
  return operator !== 'is_empty' && operator !== 'is_not_empty';
}

function needsValue2(operator: string, valueType: number, fieldType?: string): boolean {
  if (operator === 'between' || operator === 'overlaps' || operator === 'contains') {
    return true;
  }
  return false;
}

function isMultiSelect(operator: string, valueType: number, fieldType?: string): boolean {
  if (operator === 'has_any' || operator === 'has_all') {
    return true;
  }
  if ((valueType === 7 || valueType === 11 || fieldType === 'checkbox' || fieldType === 'multiSelect')) {
    return true;
  }
  return false;
}

function isBooleanType(valueType: number): boolean {
  return valueType === 3;
}

function isDateType(valueType: number): boolean {
  return valueType === 4;
}

function isDateRangeType(valueType: number): boolean {
  return valueType === 8;
}

function isSelectType(valueType: number): boolean {
  return valueType === 6 || valueType === 10;
}

function isDynamicContainer(valueType: number): boolean {
  return valueType === 12;
}

function isNumberType(valueType: number): boolean {
  return valueType === 1;
}

function isTextType(valueType: number): boolean {
  return valueType === 2 || valueType === 5;
}

async function handleSearch() {
  // 校验必填项
  if (!projectType.value) {
    message.warning('请选择项目类型（必选）');
    return;
  }

  // 构建搜索参数
  const params: NationalSearchParams = {
    projectType: projectType.value!,
  };

  if (hospitalName.value) params.hospitalName = hospitalName.value;
  if (reportYear.value) params.reportYear = reportYear.value;
  if (reportBatch.value) params.reportBatch = reportBatch.value;
  if (reportStatus.value) params.reportStatus = reportStatus.value;
  if (provinceStatus.value !== undefined) params.provinceStatus = provinceStatus.value;
  if (nationalReportStatus.value !== undefined) params.nationalReportStatus = nationalReportStatus.value;

  // 过滤有效的指标条件
  const validGroups: IndicatorConditionGroup[] = [];
  for (const group of indicatorGroups.value) {
    const validConds: IndicatorCondition[] = [];
    for (const cond of group.conditions) {
      if (!cond.indicatorCode) continue;
      if (!cond.operator) continue;
      if (needsValueInput(cond.operator) && !cond.value) continue;
      validConds.push({ ...cond });
    }
    if (validConds.length > 0) {
      validGroups.push({
        innerLogic: group.innerLogic,
        conditions: validConds,
      });
    }
  }

  if (validGroups.length > 0) {
    params.indicatorGroups = validGroups;
  }

  try {
    const result = await nationalSearch(params);
    // 保存搜索参数，下次打开时回显
    lastSearchParams.value = { ...params };
    emit('search', result, params);
    close();
  } catch (e: any) {
    message.error(e?.message || '搜索失败');
  }
}

function handleReset() {
  hospitalName.value = '';
  reportYear.value = undefined;
  reportBatch.value = undefined;
  reportStatus.value = undefined;
  provinceStatus.value = undefined;
  nationalReportStatus.value = undefined;
  projectType.value = undefined;
  indicatorGroups.value = [];
  indicatorList.value = [];
}

const [Modal, { open, close }] = useVbenModal({
  title: '国家局高级搜索',
  width: 900,
  showClose: true,
  onOpenChange: async (isOpen) => {
    if (isOpen) {
      // 打开时：从上次搜索参数恢复表单状态
      if (lastSearchParams.value) {
        const p = lastSearchParams.value;
        hospitalName.value = p.hospitalName ?? '';
        reportYear.value = p.reportYear;
        reportBatch.value = p.reportBatch;
        reportStatus.value = p.reportStatus;
        provinceStatus.value = p.provinceStatus;
        nationalReportStatus.value = p.nationalReportStatus;
        projectType.value = p.projectType;
        if (p.projectType) {
          await loadIndicators(p.projectType);
        }
      }
    }
    // 关闭时不重置，保留搜索条件供下次回显
  },
});

defineExpose({ open });
</script>

<template>
  <Modal class="national-advanced-search-modal">
    <a-form layout="vertical" class="search-form">
      <!-- 基本信息区 -->
      <div class="form-section">
        <div class="section-title">基本信息</div>
        <div class="form-grid">
          <a-form-item label="医院名称" class="form-item"
            :label-col="{ span: 24 }"
            :wrapper-col="{ span: 24 }">
            <a-input v-model:value="hospitalName" placeholder="请输入医院名称（模糊匹配）" />
          </a-form-item>

          <a-form-item label="填报年度" class="form-item"
            :label-col="{ span: 24 }"
            :wrapper-col="{ span: 24 }">
            <a-select
              v-model:value="reportYear"
              placeholder="请选择年度"
              :options="YEAR_OPTIONS"
              allowClear
            />
          </a-form-item>

          <a-form-item label="填报批次" class="form-item"
            :label-col="{ span: 24 }"
            :wrapper-col="{ span: 24 }">
            <a-select
              v-model:value="reportBatch"
              placeholder="请选择批次"
              :options="BATCH_OPTIONS"
              allowClear
            />
          </a-form-item>

          <a-form-item label="填报状态" class="form-item"
            :label-col="{ span: 24 }"
            :wrapper-col="{ span: 24 }">
            <a-select
              v-model:value="reportStatus"
              placeholder="请选择状态"
              :options="REPORT_STATUS_OPTIONS"
              allowClear
            />
          </a-form-item>

          <a-form-item label="省级审核" class="form-item"
            :label-col="{ span: 24 }"
            :wrapper-col="{ span: 24 }">
            <a-select
              v-model:value="provinceStatus"
              placeholder="请选择"
              :options="PROVINCE_STATUS_OPTIONS"
              allowClear
            />
          </a-form-item>

          <a-form-item label="上报状态" class="form-item"
            :label-col="{ span: 24 }"
            :wrapper-col="{ span: 24 }">
            <a-select
              v-model:value="nationalReportStatus"
              placeholder="请选择"
              :options="NATIONAL_REPORT_STATUS_OPTIONS"
              allowClear
            />
          </a-form-item>

          <a-form-item label="项目类型" class="form-item required"
            :label-col="{ span: 24 }"
            :wrapper-col="{ span: 24 }">
            <a-select
              v-model:value="projectType"
              placeholder="请选择项目类型（必选）"
              :options="PROJECT_TYPE_OPTIONS"
              :loading="indicatorListLoading"
            />
          </a-form-item>
        </div>
      </div>

      <!-- 指标条件区 -->
      <div class="form-section">
        <div class="section-title">
          <span>指标条件</span>
          <a-button type="primary" size="small" @click="addConditionGroup" class="add-group-btn">
            <template #icon>
              <span>+</span>
            </template>
            添加条件组
          </a-button>
        </div>

        <div v-if="indicatorGroups.length === 0" class="empty-hint">
          请先选择项目类型，然后点击"添加条件组"开始添加指标条件
        </div>

        <div v-for="(group, gIdx) in indicatorGroups" :key="gIdx" class="condition-group">
          <div class="group-header">
            <span class="group-label">条件组 {{ gIdx + 1 }}</span>
            <a-radio-group
              v-model:value="group.innerLogic"
              size="small"
              class="inner-logic"
            >
              <a-radio-button value="OR">OR</a-radio-button>
              <a-radio-button value="AND">AND</a-radio-button>
            </a-radio-group>
            <a-button
              type="link"
              danger
              size="small"
              @click="removeConditionGroup(gIdx)"
              class="remove-group-btn"
            >
              删除组
            </a-button>
          </div>

          <div class="conditions-list">
            <div
              v-for="(cond, cIdx) in group.conditions"
              :key="cIdx"
              class="condition-row"
            >
              <div class="condition-content">
                <!-- 指标选择 -->
                <a-select
                  v-model:value="cond.indicatorCode"
                  placeholder="选择指标"
                  :options="indicatorOptions"
                  show-search
                  :filter-option="(input: string, option: any) =>
                    (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                  "
                  class="indicator-select"
                  @change="(val: string) => onIndicatorChange(gIdx, cIdx, val)"
                />

                <!-- 动态容器子字段选择 -->
                <template v-if="isDynamicContainer(cond.valueType)">
                  <a-select
                    v-model:value="cond.fieldCode"
                    placeholder="选择子字段"
                    class="field-select"
                    :options="getContainerFieldOptions(cond.indicatorOptions || '')"
                    allow-clear
                    @change="(val: string) => onContainerFieldChange(gIdx, cIdx, val)"
                  />

                  <a-select
                    v-if="cond.fieldCode"
                    v-model:value="cond.fieldType"
                    placeholder="子字段类型"
                    :options="FIELD_TYPES"
                    class="field-type-select"
                    @change="(val: string) => onFieldTypeChange(gIdx, cIdx, val)"
                  />
                </template>

                <!-- 操作符选择 -->
                <a-select
                  v-model:value="cond.operator"
                  placeholder="操作符"
                  :options="
                    isDynamicContainer(cond.valueType) && cond.fieldType
                      ? getOperatorsByFieldType(cond.fieldType)
                      : getOperatorsForType(cond.valueType)
                  "
                  class="operator-select"
                />

                <!-- 值输入 -->
                <template v-if="needsValueInput(cond.operator)">
                  <!-- 布尔类型 -->
                  <a-select
                    v-if="isBooleanType(cond.valueType)"
                    v-model:value="cond.value"
                    placeholder="选择值"
                    class="value-input"
                  >
                    <a-select-option value="1">是</a-select-option>
                    <a-select-option value="0">否</a-select-option>
                  </a-select>

                  <!-- 数字类型 -->
                  <a-input-number
                    v-else-if="isNumberType(cond.valueType)"
                    v-model:value="cond.value"
                    placeholder="输入数值"
                    class="value-input"
                  />

                  <!-- 单选/下拉类型 -->
                  <a-select
                    v-else-if="isSelectType(cond.valueType)"
                    v-model:value="cond.value"
                    placeholder="选择值"
                    class="value-input"
                    :options="parseValueOptions(cond.indicatorOptions || '')"
                  />

                  <!-- 多选类型 -->
                  <a-select
                    v-else-if="isMultiSelect(cond.operator, cond.valueType, cond.fieldType)"
                    v-model:value="cond.value"
                    placeholder="选择值（多选）"
                    class="value-input"
                    mode="multiple"
                    :options="parseValueOptions(cond.indicatorOptions || '')"
                  />

                  <!-- 日期类型 -->
                  <a-date-picker
                    v-else-if="isDateType(cond.valueType) && !needsValue2(cond.operator, cond.valueType, cond.fieldType)"
                    v-model:value="cond.value"
                    placeholder="选择日期"
                    class="value-input"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                  />

                  <!-- 日期范围类型 -->
                  <a-range-picker
                    v-else-if="isDateRangeType(cond.valueType)"
                    class="value-input range-input"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    @change="(dates: any, dateStrings: string[]) => { cond.value = dateStrings[0]; cond.value2 = dateStrings[1]; }"
                  />

                  <!-- 日期区间（between 操作符） -->
                  <template v-else-if="needsValue2(cond.operator, cond.valueType, cond.fieldType) && isDateType(cond.valueType)">
                    <a-date-picker
                      v-model:value="cond.value"
                      placeholder="开始日期"
                      class="value-input"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                    />
                    <span class="range-separator">至</span>
                    <a-date-picker
                      v-model:value="cond.value2"
                      placeholder="结束日期"
                      class="value-input"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                    />
                  </template>

                  <!-- 文本/长文本类型 -->
                  <a-textarea
                    v-else-if="isTextType(cond.valueType)"
                    v-model:value="cond.value"
                    placeholder="输入文本"
                    class="value-input"
                    :rows="1"
                  />

                  <!-- 默认输入框 -->
                  <a-input
                    v-else
                    v-model:value="cond.value"
                    placeholder="输入值"
                    class="value-input"
                  />
                </template>

                <!-- 区间第二值（仅日期类型的 between） -->
                <template v-if="needsValue2(cond.operator, cond.valueType, cond.fieldType) && isDynamicContainer(cond.valueType) && cond.fieldType === 'date'">
                  <span class="range-separator">至</span>
                  <a-input
                    v-model:value="cond.value2"
                    placeholder="结束日期"
                    class="value-input"
                  />
                </template>

                <!-- 删除条件按钮 -->
                <a-button
                  type="link"
                  danger
                  size="small"
                  @click="removeCondition(gIdx, cIdx)"
                  class="remove-cond-btn"
                >
                  删除
                </a-button>
              </div>
            </div>
          </div>

          <a-button
            type="dashed"
            size="small"
            @click="addCondition(gIdx)"
            class="add-condition-btn"
          >
            + 添加条件
          </a-button>
        </div>
      </div>
    </a-form>

    <template #footer>
      <a-button @click="handleReset">重置</a-button>
      <a-button type="primary" @click="handleSearch">开始搜索</a-button>
    </template>
  </Modal>
</template>

<style scoped>
.national-advanced-search-modal :deep(.ant-modal-body) {
  padding: 16px 24px;
  max-height: 70vh;
  overflow-y: auto;
}

.search-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-section {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.form-item {
  margin-bottom: 0;
}

.form-item.required :deep(.ant-form-item-label)::after {
  content: '*';
  color: #ff4d4f;
  margin-left: 4px;
}

.empty-hint {
  color: #999;
  font-size: 13px;
  text-align: center;
  padding: 20px;
}

.condition-group {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
  background: #fff;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.group-label {
  font-weight: 500;
  color: #333;
}

.inner-logic {
  margin-left: auto;
}

.remove-group-btn {
  padding: 0;
}

.conditions-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.condition-row {
  display: flex;
  align-items: flex-start;
}

.condition-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1;
}

.indicator-select {
  width: 280px;
  min-width: 200px;
}

.field-select {
  width: 140px;
}

.field-type-select {
  width: 120px;
}

.operator-select {
  width: 100px;
}

.value-input {
  width: 160px;
}

.range-input {
  width: 260px !important;
}

.range-separator {
  color: #999;
  flex-shrink: 0;
}

.remove-cond-btn {
  flex-shrink: 0;
  padding: 0;
}

.add-condition-btn {
  width: 100%;
  margin-top: 8px;
}

.add-group-btn {
  font-size: 13px;
}

:deep(.ant-form-item) {
  margin-bottom: 12px;
}

:deep(.ant-form-item-label) {
  padding-bottom: 4px;
}

:deep(.ant-form-item-label > label) {
  font-size: 13px;
  height: auto;
}
</style>
