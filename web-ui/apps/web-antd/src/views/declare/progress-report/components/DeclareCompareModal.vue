<template>
  <a-modal
    v-model:open="visible"
    title="数据对比"
    width="85%"
    :footer="null"
    :destroy-on-close="true"
  >
    <!-- 弹窗标题区: 两条记录基本信息 -->
    <div class="flex items-center gap-4 mb-6 px-1">
      <div class="flex-1 p-3 bg-blue-50 rounded border border-blue-200">
        <div class="font-medium text-blue-800">{{ data.reportA?.hospitalName }}</div>
        <div class="text-sm text-blue-600 mt-1">
          {{ data.reportA?.projectTypeName }} | {{ data.reportA?.reportYear }}年度第{{ data.reportA?.reportBatch }}批
        </div>
      </div>
      <div class="text-gray-400 font-bold">VS</div>
      <div class="flex-1 p-3 bg-green-50 rounded border border-green-200">
        <div class="font-medium text-green-800">{{ data.reportB?.hospitalName }}</div>
        <div class="text-sm text-green-600 mt-1">
          {{ data.reportB?.projectTypeName }} | {{ data.reportB?.reportYear }}年度第{{ data.reportB?.reportBatch }}批
        </div>
      </div>
    </div>

    <Spin :spinning="loading">
      <!-- 基础信息对比区（只展示，无差异高亮） -->
      <div class="mb-6">
        <div class="text-sm font-medium text-gray-500 mb-3 flex items-center gap-2">
          <IconifyIcon icon="lucide:info" class="text-base" />
          基础信息对比
        </div>
        <a-table
          :columns="infoColumns"
          :data-source="infoRows"
          :pagination="false"
          bordered
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'field'">
              <span class="text-gray-500">{{ record.label }}</span>
            </template>
            <template v-else-if="column.key === 'valueA'">
              <span>{{ record.valueA || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'valueB'">
              <span>{{ record.valueB || '-' }}</span>
            </template>
          </template>
        </a-table>
      </div>

      <!-- 指标数据对比区（差异高亮） -->
      <div>
        <div class="text-sm font-medium text-gray-500 mb-3 flex items-center gap-2">
          <IconifyIcon icon="lucide:bar-chart-2" class="text-base" />
          指标数据对比
        </div>
        <a-table
          :columns="indicatorColumns"
          :data-source="paginatedIndicators"
          :pagination="indicatorPagination"
          bordered
          size="small"
          :scroll="{ y: 500 }"
        >
          <template #bodyCell="{ column, record }">
            <!-- 一级分组标题行 -->
            <template v-if="record._isGroup && record._isLevel1">
              <template v-if="column.key === 'indicatorName'">
                <span class="text-base font-bold text-blue-700 border-b border-blue-300 pb-1">{{ record._groupName }}</span>
              </template>
            </template>
            <!-- 二级分组标题行 -->
            <template v-else-if="record._isGroup && record._isLevel2">
              <template v-if="column.key === 'indicatorName'">
                <span class="text-sm font-medium text-blue-700 border-b border-blue-300 pb-0.5 ml-4">{{ record._groupName }}</span>
              </template>
            </template>
            <!-- 指标数据行 -->
            <template v-else-if="column.key === 'indicatorName'">
              <span class="font-medium">{{ record.indicatorName }}</span>
              <span class="text-gray-400 text-xs ml-1">[{{ record.indicatorCode }}]</span>
              <span class="text-gray-400 text-xs ml-1">{{ record.unit }}</span>
            </template>
            <!-- 申报A值 -->
            <template v-else-if="column.key === 'valueA'">
              <pre v-if="record.valueType === 12" :class="['whitespace-pre-wrap text-left', getDiffClass(record, 'A')]">{{ formatValue(record.valueA, record, 'A') }}</pre>
              <span v-else :class="getDiffClass(record, 'A')">{{ formatValue(record.valueA, record, 'A') }}</span>
            </template>
            <!-- 申报B值 -->
            <template v-else-if="column.key === 'valueB'">
              <pre v-if="record.valueType === 12" :class="['whitespace-pre-wrap text-left', getDiffClass(record, 'B')]">{{ formatValue(record.valueB, record, 'B') }}</pre>
              <span v-else :class="getDiffClass(record, 'B')">{{ formatValue(record.valueB, record, 'B') }}</span>
            </template>
            <!-- 对比结果 -->
            <template v-else-if="column.key === 'diff'">
              <!-- 如果任一方被隐藏，显示 "-" 表示无法比较 -->
              <span v-if="!isSideVisible(record, 'A') || !isSideVisible(record, 'B')" class="text-gray-300">- 无法比较</span>
              <span v-else-if="record.diffType === 'up'" class="text-green-600 font-bold">↑ 上升</span>
              <span v-else-if="record.diffType === 'down'" class="text-red-500 font-bold">↓ 下降</span>
              <span v-else-if="record.diffType === 'different'" class="text-red-500 font-bold">≠ 不一致</span>
              <span v-else-if="record.diffType === 'equal'" class="text-gray-400">= 相同</span>
              <span v-else class="text-gray-300">- 无数据</span>
            </template>
          </template>
        </a-table>
      </div>
    </Spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue';
import { message } from 'ant-design-vue';
import { Spin } from 'ant-design-vue';
import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import { getCompareData, type CompareDataResponse, type CompareIndicatorRow } from '#/api/declare/progress-report';
import { IconifyIcon } from '@vben/icons';
import {
  evaluateLinkageVisibility,
  isIndicatorVisibleByLinkage,
} from './IndicatorInputTable/utils/linkageVisibility';
import type { LinkageEvaluationResult } from './IndicatorInputTable/types';

const visible = ref(false);
const loading = ref(false);
const data = reactive<CompareDataResponse>({ reportA: null as any, reportB: null as any, indicators: [] });

// 指标对比表格分页：按一级分组整组翻页，避免分组标题和内容被切断
const indicatorPage = reactive({ current: 1, pageSize: 20 });

// 分页配置
const indicatorPagination = computed(() => ({
  current: indicatorPage.current,
  pageSize: indicatorPage.pageSize,
  total: indicatorPageGroups.value.length,
  onChange: (page: number, size: number) => onIndicatorPageChange(page, size),
}));

// 联动可见性 Map（分别记录 A 和 B 的联动状态）
const reportALinkageMap = ref<Map<string, LinkageEvaluationResult>>(new Map());
const reportBLinkageMap = ref<Map<string, LinkageEvaluationResult>>(new Map());

/** 通过字典获取填报状态中文名称 */
function getReportStatusName(status: string | number | null | undefined): string {
  if (!status) return '-';
  const options = getDictOptions(DICT_TYPE.DECLARE_PROJECT_STATUS);
  const found = options.find((item) => String(item.value) === String(status));
  return found?.label || String(status);
}

async function open(reportIdA: number, reportIdB: number) {
  visible.value = true;
  loading.value = true;
  indicatorPage.current = 1;
  data.indicators = [];
  data.reportA = null as any;
  data.reportB = null as any;
  reportALinkageMap.value = new Map();
  reportBLinkageMap.value = new Map();
  try {
    const res = await getCompareData(reportIdA, reportIdB);
    data.reportA = res.reportA;
    data.reportB = res.reportB;
    data.indicators = res.indicators || [];

    // 评估联动可见性
    if (data.indicators.length) {
      // 收集 A 的触发值
      const triggerValuesA: Record<string, any> = {};
      for (const ind of data.indicators) {
        if (ind.valueA !== null && ind.valueA !== undefined) {
          triggerValuesA[ind.indicatorCode] = ind.valueA;
        }
      }
      reportALinkageMap.value = evaluateLinkageVisibility(
        data.indicators.map(i => ({ indicatorCode: i.indicatorCode, extraConfig: i.extraConfig })),
        triggerValuesA,
      );

      // 收集 B 的触发值
      const triggerValuesB: Record<string, any> = {};
      for (const ind of data.indicators) {
        if (ind.valueB !== null && ind.valueB !== undefined) {
          triggerValuesB[ind.indicatorCode] = ind.valueB;
        }
      }
      reportBLinkageMap.value = evaluateLinkageVisibility(
        data.indicators.map(i => ({ indicatorCode: i.indicatorCode, extraConfig: i.extraConfig })),
        triggerValuesB,
      );
    }
  } catch (e: any) {
    message.error(e?.message || '加载对比数据失败');
    visible.value = false;
  } finally {
    loading.value = false;
  }
}

// 基础信息表格列
const infoColumns = [
  { title: '字段', key: 'field', width: 160 },
  { title: '申报记录 A', key: 'valueA' },
  { title: '申报记录 B', key: 'valueB' },
];

// 指标对比表格列
const indicatorColumns = [
  { title: '指标名称', key: 'indicatorName', fixed: 'left', width: 260 },
  { title: '申报记录 A', key: 'valueA', align: 'center' as const, width: 180 },
  { title: '申报记录 B', key: 'valueB', align: 'center' as const, width: 180 },
  { title: '对比结果', key: 'diff', align: 'center' as const, width: 120 },
];

// 基础信息行数据(只展示，不做差异比较)
const infoRows = computed(() => [
  { label: '统一社会信用代码', valueA: data.reportA?.unifiedSocialCreditCode, valueB: data.reportB?.unifiedSocialCreditCode },
  { label: '医疗机构许可证号', valueA: data.reportA?.medicalLicenseNo, valueB: data.reportB?.medicalLicenseNo },
  { label: '项目类型', valueA: data.reportA?.projectTypeName, valueB: data.reportB?.projectTypeName },
  { label: '填报年度', valueA: `${data.reportA?.reportYear}年`, valueB: `${data.reportB?.reportYear}年` },
  { label: '填报批次', valueA: `第${data.reportA?.reportBatch}批`, valueB: `第${data.reportB?.reportBatch}批` },
  { label: '填报状态', valueA: getReportStatusName(data.reportA?.reportStatus), valueB: getReportStatusName(data.reportB?.reportStatus) },
  { label: '省级审核状态', valueA: data.reportA?.provinceStatusName, valueB: data.reportB?.provinceStatusName },
  { label: '国家上报状态', valueA: data.reportA?.nationalReportStatusName, valueB: data.reportB?.nationalReportStatusName },
]);

/** 省级审核状态名称 */
function getProvinceStatusName(status: number | null | undefined): string {
  const names: Record<number, string> = { 0: '未提交', 1: '审核中', 2: '已通过', 3: '已驳回' };
  return names[Number(status)] || '未知';
}

/** 国家局上报状态名称 */
function getNationalReportStatusName(status: number | null | undefined): string {
  if (Number(status) === 1) return '国家局审批中';
  if (Number(status) === 2) return '已上报';
  return '未上报';
}

// 按指标分类（两级分组）分组的对比数据
const groupedIndicators = computed(() => {
  const result: any[] = [];
  let currentLevel1 = '';
  let currentLevel2 = '';

  for (const row of data.indicators) {
    // groupName 是一级标题（parentGroupName 不存在时）或二级标题（parentGroupName 存在时）
    if (row.groupName && row.groupName !== currentLevel1) {
      // 一级分组变化：groupName 本身作为一级标题
      currentLevel1 = row.groupName;
      currentLevel2 = '';
      result.push({
        _isGroup: true,
        _isLevel1: true,
        _isLevel2: false,
        _groupName: currentLevel1,
      });
      // parentGroupName 存在时，作为二级标题追加在同级内
      if (row.parentGroupName && row.parentGroupName !== currentLevel2) {
        currentLevel2 = row.parentGroupName;
        result.push({
          _isGroup: true,
          _isLevel1: false,
          _isLevel2: true,
          _groupName: currentLevel2,
        });
      }
    } else if (row.parentGroupName && row.parentGroupName !== currentLevel2) {
      // 同组内 parentGroupName 变化，插入二级标题
      currentLevel2 = row.parentGroupName;
      result.push({
        _isGroup: true,
        _isLevel1: false,
        _isLevel2: true,
        _groupName: currentLevel2,
      });
    }
    result.push(row);
  }
  return result;
});

// 将分组后的数据按一级分组切分成多页（每页 = 一个一级分组及其下所有内容）
const indicatorPageGroups = computed(() => {
  const groups: any[][] = [];
  let currentGroup: any[] = [];

  for (const row of groupedIndicators.value) {
    if (row._isLevel1) {
      // 新一级分组：先保存旧分组，再开启新分组
      if (currentGroup.length > 0) {
        groups.push(currentGroup);
      }
      currentGroup = [row];
    } else {
      currentGroup.push(row);
    }
  }
  if (currentGroup.length > 0) {
    groups.push(currentGroup);
  }
  return groups;
});

// 当前页显示的分组内容
const paginatedIndicators = computed(() => {
  const start = (indicatorPage.current - 1) * indicatorPage.pageSize;
  return indicatorPageGroups.value.slice(start, start + indicatorPage.pageSize).flat();
});

// 分页变化处理
function onIndicatorPageChange(page: number, size: number) {
  indicatorPage.current = page;
  indicatorPage.pageSize = size;
}

/** 判断指定侧的指标是否可见（用于 diff 列的判断） */
function isSideVisible(row: any, side: 'A' | 'B'): boolean {
  const linkageMap = side === 'A' ? reportALinkageMap.value : reportBLinkageMap.value;
  return isIndicatorVisibleByLinkage(row.indicatorCode, linkageMap);
}

// 值格式化
function formatValue(val: any, row: any, side?: 'A' | 'B') {
  if (row._isGroup) return '';

  // 检查联动可见性：隐藏的指标显示空值
  if (side) {
    const linkageMap = side === 'A' ? reportALinkageMap.value : reportBLinkageMap.value;
    const isVisible = isIndicatorVisibleByLinkage(row.indicatorCode, linkageMap);
    if (!isVisible) return '-';
  }

  if (val === null || val === undefined) return '-';
  switch (row.valueType) {
    case 1: {
      const num = Number(val);
      return isNaN(num) ? val : num.toFixed(2);
    }
    case 3: return val ? '是' : '否';
    case 4: return val ? String(val).substring(0, 10) : '-';
    case 8:
      if (typeof val === 'object' && val !== null) {
        const s = val.start ? String(val.start).substring(0, 10) : '-';
        const e = val.end ? String(val.end).substring(0, 10) : '-';
        return `${s} ~ ${e}`;
      }
      return String(val);
    case 6:
    case 10: {
      // 单选：处理输入型选项
      if (typeof val === 'string' && val.includes('∵')) {
        const deserialized = deserializeInputTypeValue(val);
        const label = resolveOptionLabel(deserialized.value, row.valueOptions);
        return deserialized.input ? `${label}（${deserialized.input}）` : label;
      }
      return resolveOptionLabel(val, row.valueOptions);
    }
    case 7:
    case 11: {
      // 多选：处理输入型选项
      if (val && row.valueOptions) {
        try {
          const rawSelected = String(val).split(',');
          const options = JSON.parse(row.valueOptions);
          const displayParts: string[] = [];
          for (const v of rawSelected) {
            if (v.includes('∵')) {
              const deserialized = deserializeInputTypeValue(v);
              const label = options.find((o: any) => String(o.value) === String(deserialized.value))?.label || deserialized.value;
              displayParts.push(deserialized.input ? `${label}（${deserialized.input}）` : label);
            } else {
              const label = options.find((o: any) => String(o.value) === String(v))?.label || v;
              displayParts.push(label);
            }
          }
          return displayParts.join('、');
        } catch {
          return String(val);
        }
      }
      return String(val);
    }
    case 9: {
      // 文件上传：显示文件名列表（换行分隔），每个文件名可点击下载
      let files: any[];
      if (typeof val === 'string') {
        try { files = JSON.parse(val || '[]'); } catch { return '-'; }
      } else if (Array.isArray(val)) {
        files = val;
      } else {
        return '-';
      }
      if (!files.length) return '-';
      return files.map(f => f.name || f.fileName || '未命名文件').join('\n');
    }
    case 12:
      return renderContainerValue(val, row.valueOptions);
    default:
      return String(val);
  }
}

// 将选项代号映射为选项文本
function resolveOptionLabel(value: any, valueOptions?: string) {
  if (!value) return '-';
  if (!valueOptions) return String(value);
  try {
    const options = JSON.parse(valueOptions);
    const found = options.find((o: any) => String(o.value) === String(value));
    return found ? found.label : String(value);
  } catch { return String(value); }
}

// 反序列化输入型选项值
function deserializeInputTypeValue(optionValue: string): { value: string; input: string } {
  const idx = optionValue.indexOf('∵');
  if (idx === -1) {
    return { value: optionValue, input: '' };
  }
  return {
    value: optionValue.substring(0, idx),
    input: optionValue.substring(idx + 1),
  };
}

/** 容器子字段条件显示配置 */
interface ShowCondition {
  watchField: string;
  operator: 'eq' | 'neq' | 'gt' | 'gte' | 'lt' | 'lte' | 'in' | 'notEmpty' | 'isEmpty';
  value?: any;
}

/** 容器字段定义 */
interface ContainerField {
  fieldCode: string;
  fieldLabel: string;
  fieldType: string;
  required?: boolean;
  options?: { value: string; label: string }[];
  showCondition?: ShowCondition;
  precision?: number;
  prefix?: string;
  suffix?: string;
  format?: string;
  maxLength?: number;
  rows?: number;
  defaultValue?: any;
}

/** 容器配置（统一解析格式） */
interface ContainerConfig {
  mode: 'normal' | 'conditional' | 'autoEntry';
  link?: string;
  fields: ContainerField[];
}

/** 统一解析容器配置（兼容三种容器类型） */
function parseContainerConfig(valueOptions: string): ContainerConfig {
  if (!valueOptions) return { mode: 'normal', fields: [] };
  try {
    const parsed = JSON.parse(valueOptions);
    if (Array.isArray(parsed)) {
      return { mode: 'normal', fields: parsed as ContainerField[] };
    }
    return {
      mode: (parsed.mode as ContainerConfig['mode']) || 'normal',
      link: parsed.link,
      fields: parsed.fields || [],
    };
  } catch {
    return { mode: 'normal', fields: [] };
  }
}

/** 获取容器的所有字段定义（统一入口） */
function getContainerFields(valueOptions: string): ContainerField[] {
  return parseContainerConfig(valueOptions).fields;
}

/** 获取容器类型 */
function getContainerType(valueOptions: string): ContainerConfig['mode'] {
  return parseContainerConfig(valueOptions).mode;
}

/** 获取自动条目容器的关联指标名称 */
function getContainerLink(valueOptions: string): string | undefined {
  return parseContainerConfig(valueOptions).link;
}

/** 按精度格式化数字值（与 approval-detail.vue 一致） */
function formatContainerNumber(val: any, precision?: number): string {
  if (val === null || val === undefined || val === '') return '-';
  const num = Number(val);
  if (isNaN(num)) return String(val);
  if (precision !== undefined && precision !== null) return num.toFixed(Number(precision));
  return Number.isInteger(num) ? String(num) : num.toFixed(2);
}

/** 格式化日期值 */
function formatContainerDate(val: any): string {
  if (!val) return '-';
  const s = String(val).substring(0, 10);
  return s || '-';
}

/** 判断同条目中某字段是否可见（与 approval-detail.vue 一致） */
function isFieldVisible(entry: any, field: ContainerField, allFields: ContainerField[]): boolean {
  if (!field.showCondition) return true;
  const cond = field.showCondition;
  // 条件字段的 key 也是复合格式
  const watchVal = entry?.[entry.rowKey + cond.watchField];
  const { operator, value } = cond;
  const watchedField = allFields.find(f => f.fieldCode === cond.watchField);
  const isBooleanWatch = watchedField?.fieldType === 'boolean';
  switch (operator) {
    case 'eq':
      if (isBooleanWatch) {
        const boolVal = value === 'true' || value === '1' || value === true;
        return watchVal === boolVal;
      }
      return watchVal === value;
    case 'neq':
      if (isBooleanWatch) {
        const boolVal = value === 'true' || value === '1' || value === true;
        return watchVal !== boolVal;
      }
      return watchVal !== value;
    case 'gt':       return Number(watchVal) > Number(value);
    case 'gte':      return Number(watchVal) >= Number(value);
    case 'lt':       return Number(watchVal) < Number(value);
    case 'lte':      return Number(watchVal) <= Number(value);
    case 'in':       return Array.isArray(value) && value.includes(watchVal);
    case 'notEmpty': return watchVal !== undefined && watchVal !== null && watchVal !== '';
    case 'isEmpty':  return watchVal === undefined || watchVal === null || watchVal === '';
    default:         return true;
  }
}

/** 渲染单个容器条目的字段列表（用于对比视图） */
function renderContainerEntry(entry: any, fields: ContainerField[], showIndex: boolean, entryIndex: number): string {
  const parts: string[] = [];
  if (showIndex) {
    parts.push(`【条目${entryIndex + 1}】`);
  }
  for (const field of fields) {
    if (!isFieldVisible(entry, field, fields)) continue;
    // 字段值的 key 也是复合格式
    const val = entry?.[entry.rowKey + field.fieldCode];
    let displayVal: string;
    if (val === undefined || val === null || val === '') {
      displayVal = '-';
    } else if (field.fieldType === 'boolean') {
      displayVal = val ? '是' : '否';
    } else if (field.fieldType === 'select' || field.fieldType === 'radio') {
      displayVal = (field.options?.find(o => String(o.value) === String(val))?.label) || String(val);
    } else if (field.fieldType === 'multiSelect' || field.fieldType === 'checkbox') {
      const selected = Array.isArray(val) ? val : String(val).split(',');
      displayVal = (selected as string[])
        .map(v => field.options?.find(o => String(o.value) === String(v))?.label || v)
        .join('、') || '-';
    } else if (field.fieldType === 'number') {
      displayVal = formatContainerNumber(val, field.precision);
    } else if (field.fieldType === 'date') {
      displayVal = formatContainerDate(val);
    } else if (field.fieldType === 'dateRange') {
      if (Array.isArray(val)) {
        displayVal = `${formatContainerDate(val[0])} ~ ${formatContainerDate(val[1])}`;
      } else {
        displayVal = '-';
      }
    } else {
      displayVal = String(val);
    }
    parts.push(`${field.fieldLabel}: ${displayVal}`);
  }
  return parts.join('\n');
}

/** 渲染完整容器值字符串（用于对比表格单元格） */
function renderContainerValue(val: any, valueOptions: string | undefined): string {
  let entries: any[];
  if (typeof val === 'string') {
    try { entries = JSON.parse(val || '[]'); } catch { return '-'; }
  } else if (Array.isArray(val)) {
    entries = val;
  } else {
    return '-';
  }
  if (!entries.length) return '-';
  const fields = valueOptions ? getContainerFields(valueOptions) : [];
  const containerType = getContainerType(valueOptions || '');
  const showIndex = containerType !== 'conditional';
  let result = entries
    .map((entry, idx) => renderContainerEntry(entry, fields, showIndex, idx))
    .join('\n');
  if (containerType === 'autoEntry') {
    const link = getContainerLink(valueOptions || '');
    if (link) result = `[由「${link}」指标自动生成]\n${result}`;
  }
  return result;
}

// 差异样式类
function getDiffClass(record: any, side: 'A' | 'B') {
  if (record._isGroup) return '';
  if (record.diffType === 'none') return 'text-gray-300';
  if (record.diffType === 'equal') return 'text-gray-600';
  if (side === 'B') {
    if (record.diffType === 'up') return 'text-green-600 font-semibold';
    if (record.diffType === 'down') return 'text-red-500 font-semibold';
  }
  if (record.diffType === 'different') return 'text-red-400';
  return '';
}

defineExpose({ open });
</script>

<style scoped>
/* 容器值在对比表格中的展示 — 不设 max-height，条目数量不同也能完整显示 */
.container-compare-cell {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
  color: hsl(var(--foreground));
}
</style>
