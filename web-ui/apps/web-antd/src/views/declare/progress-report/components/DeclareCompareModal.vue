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

    <a-spin :spinning="loading">
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
          :data-source="groupedIndicators"
          :pagination="{ pageSize: 20 }"
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
                <span class="text-sm font-medium text-gray-600 border-b border-dashed border-gray-300 pb-0.5 ml-4">{{ record._groupName }}</span>
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
              <span :class="getDiffClass(record, 'A')">{{ formatValue(record.valueA, record) }}</span>
            </template>
            <!-- 申报B值 -->
            <template v-else-if="column.key === 'valueB'">
              <span :class="getDiffClass(record, 'B')">{{ formatValue(record.valueB, record) }}</span>
            </template>
            <!-- 对比结果 -->
            <template v-else-if="column.key === 'diff'">
              <span v-if="record.diffType === 'up'" class="text-green-600 font-bold">↑ 上升</span>
              <span v-else-if="record.diffType === 'down'" class="text-red-500 font-bold">↓ 下降</span>
              <span v-else-if="record.diffType === 'different'" class="text-red-500 font-bold">≠ 不一致</span>
              <span v-else-if="record.diffType === 'equal'" class="text-gray-400">= 相同</span>
              <span v-else class="text-gray-300">- 无数据</span>
            </template>
          </template>
        </a-table>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue';
import { getCompareData, type CompareDataResponse, type CompareIndicatorRow } from '#/api/declare/progress-report';
import { IconifyIcon } from '@vben/icons';

const visible = ref(false);
const loading = ref(false);
const data = reactive<CompareDataResponse>({ reportA: null as any, reportB: null as any, indicators: [] });

async function open(reportIdA: number, reportIdB: number) {
  visible.value = true;
  loading.value = true;
  data.indicators = [];
  data.reportA = null as any;
  data.reportB = null as any;
  try {
    const res = await getCompareData(reportIdA, reportIdB);
    data.reportA = res.reportA;
    data.reportB = res.reportB;
    data.indicators = res.indicators || [];
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
  { label: '填报状态', valueA: data.reportA?.reportStatusName, valueB: data.reportB?.reportStatusName },
  { label: '省级审核状态', valueA: data.reportA?.provinceStatusName, valueB: data.reportB?.provinceStatusName },
  { label: '国家上报状态', valueA: data.reportA?.nationalReportStatusName, valueB: data.reportB?.nationalReportStatusName },
]);

// 按指标分类（两级分组）分组的对比数据
const groupedIndicators = computed(() => {
  const result: any[] = [];
  let currentLevel1 = '';
  let currentLevel2 = '';

  for (const row of data.indicators) {
    // 如果遇到新的一级分组，插入分类标题行
    if (row.parentGroupName && row.parentGroupName !== currentLevel1) {
      currentLevel1 = row.parentGroupName;
      currentLevel2 = '';
      result.push({
        _isGroup: true,
        _isLevel1: true,
        _isLevel2: false,
        _groupName: currentLevel1,
        _subGroupName: row.groupName,
      });
      // 如果一级分组变化，二级分组也需要重置并插入
      if (row.groupName && row.groupName !== currentLevel2) {
        currentLevel2 = row.groupName;
        result.push({
          _isGroup: true,
          _isLevel1: false,
          _isLevel2: true,
          _groupName: currentLevel2,
          _subGroupName: row.groupName,
        });
      }
    } else if (row.groupName && row.groupName !== currentLevel2) {
      // 新二级分组，插入子标题行
      currentLevel2 = row.groupName;
      result.push({
        _isGroup: true,
        _isLevel1: false,
        _isLevel2: true,
        _groupName: currentLevel2,
        _subGroupName: row.groupName,
      });
    }
    result.push(row);
  }
  return result;
});

// 值格式化
function formatValue(val: any, row: any) {
  if (row._isGroup) return '';
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
    case 10:
      return resolveOptionLabel(val, row.valueOptions);
    case 7:
    case 11:
      if (val && row.valueOptions) {
        try {
          const selected = String(val).split(',');
          const options = JSON.parse(row.valueOptions);
          return selected.map((v: string) => options.find((o: any) => o.value == v)?.label || v).join('、');
        } catch {
          return val;
        }
      }
      return val;
    default:
      return String(val);
  }
}

// 将选项代号映射为选项文本
function resolveOptionLabel(value: any, valueOptions?: string) {
  if (!value || !valueOptions) return value;
  try {
    const options = JSON.parse(valueOptions);
    const found = options.find((o: any) => o.value == value);
    return found ? found.label : value;
  } catch { return value; }
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
