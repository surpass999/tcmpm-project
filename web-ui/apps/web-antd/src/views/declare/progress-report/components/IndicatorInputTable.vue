<template>
  <div class="indicator-form-wrapper">
    <!-- 按分组显示指标 -->
    <div
      v-for="group in indicatorGroups"
      :key="group.groupId"
      class="indicator-category-section"
    >
      <div
        class="category-title"
        :class="{
          'category-title--level2': group.groupLevel === 2,
        }"
      >
        {{ group.groupName }}
      </div>
      <div class="indicator-list">
        <div
          v-for="indicator in group.indicators"
          :key="indicator.id"
          class="indicator-row"
        >
          <!-- 左侧：指标名称 + 输入组件 -->
          <div class="indicator-main">
            <!-- 指标标签行 -->
            <div class="indicator-label-row">
              <span class="label-text" :title="indicator.indicatorName">
                {{ indicator.indicatorName }}
              </span>
              <a-tag v-if="indicator.isRequired" color="red" class="required-tag">必填</a-tag>
              <a-tag v-if="isComputedIndicator(indicator)" color="orange" class="computed-tag">自动计算</a-tag>
            </div>

            <!-- 指标输入组件行 -->
            <div class="indicator-input-row">
              <!-- 数字类型 -->
              <a-input-number
                v-if="indicator.valueType === 1"
                v-model:value="formValues[indicator.indicatorCode]"
                :disabled="readonly || isComputedIndicator(indicator)"
                :placeholder="isComputedIndicator(indicator) ? '自动计算' : `请输入${indicator.indicatorName}`"
                :min="indicator.minValue"
                :max="indicator.maxValue"
                :precision="getNumberPrecision(indicator)"
                class="w-full"
                @change="onIndicatorChange(indicator)"
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
                v-model:value="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                :placeholder="`请输入${indicator.indicatorName}`"
                :maxlength="getMaxLength(indicator)"
                show-count
                class="w-full"
                @change="onIndicatorChange(indicator)"
              />

              <!-- 布尔类型 -->
              <a-switch
                v-else-if="indicator.valueType === 3"
                v-model:checked="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                :checked-children="getBooleanLabels(indicator).true"
                :un-checked-children="getBooleanLabels(indicator).false"
                @change="onIndicatorChange(indicator)"
              />

              <!-- 日期类型 -->
              <a-date-picker
                v-else-if="indicator.valueType === 4"
                v-model:value="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                :show-time="getDateFormat(indicator).includes('HH')"
                :format="getDateFormat(indicator)"
                class="w-full"
                @change="onIndicatorChange(indicator)"
              />

              <!-- 长文本类型 -->
              <a-textarea
                v-else-if="indicator.valueType === 5"
                v-model:value="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                :placeholder="`请输入${indicator.indicatorName}`"
                :rows="isRichText(indicator) ? 4 : 2"
                :maxlength="getMaxLength(indicator)"
                :show-count="!!getMaxLength(indicator)"
                class="w-full"
                @change="onIndicatorChange(indicator)"
              />

              <!-- 单选类型 - 单选框 -->
              <a-radio-group
                v-else-if="indicator.valueType === 6"
                v-model:value="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                class="flex flex-wrap gap-x-4 gap-y-2"
                @change="onIndicatorChange(indicator)"
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
                v-model:value="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                class="flex flex-wrap gap-x-4 gap-y-2"
                @change="onIndicatorChange(indicator)"
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
                v-model:value="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                :placeholder="`请选择${indicator.indicatorName}`"
                class="w-full"
                :show-search="getShowSearch(indicator)"
                allow-clear
                @change="onIndicatorChange(indicator)"
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
                v-model:value="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                :placeholder="`请选择${indicator.indicatorName}`"
                mode="multiple"
                class="w-full"
                :show-search="getShowSearch(indicator)"
                allow-clear
                @change="onIndicatorChange(indicator)"
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
                v-model:value="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                :show-time="getDateFormat(indicator).includes('HH')"
                :format="getDateFormat(indicator)"
                class="w-full"
                @change="onIndicatorChange(indicator)"
              />

              <!-- 文件上传类型 -->
              <div v-else-if="indicator.valueType === 9" class="file-upload-wrapper">
                <div v-if="getFileList(indicator.indicatorCode).length > 0" class="file-list">
                  <div
                    v-for="(file, index) in getFileList(indicator.indicatorCode)"
                    :key="index"
                    class="file-item"
                  >
                    <IconifyIcon icon="lucide:file-text" class="file-icon" />
                    <span class="file-name" :title="file.name">{{ file.name }}</span>
                    <button
                      v-if="!readonly"
                      type="button"
                      class="file-delete-btn"
                      @click="handleFileRemove(indicator.indicatorCode, file)"
                    >
                      <IconifyIcon icon="lucide:x" />
                    </button>
                  </div>
                </div>
                <Upload
                  v-if="!readonly"
                  :before-upload="(file: any) => handleFileUpload(file, indicator)"
                  :show-upload-list="false"
                  :disabled="getFileList(indicator.indicatorCode).length >= getMaxFileCount(indicator)"
                  multiple
                >
                  <a-button type="dashed" :disabled="getFileList(indicator.indicatorCode).length >= getMaxFileCount(indicator)">
                    <IconifyIcon icon="lucide:plus" />
                    上传文件
                  </a-button>
                </Upload>
              </div>

              <!-- 动态容器类型 -->
              <div v-else-if="indicator.valueType === 12" class="dynamic-container-form">
                <div
                  v-for="field in parseDynamicFields(indicator.valueOptions)"
                  :key="field.fieldCode"
                  class="dynamic-field-item"
                >
                  <span class="dynamic-field-label">
                    {{ field.fieldLabel }}
                    <span v-if="field.required" class="text-red-500">*</span>
                  </span>
                  <!-- 文本输入 -->
                  <a-input
                    v-if="field.fieldType === 'text'"
                    v-model:value="containerValues[indicator.indicatorCode][field.fieldCode]"
                    :disabled="readonly"
                    :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
                    :maxlength="field.maxLength"
                    :show-count="!!field.maxLength"
                    @change="onIndicatorChange(indicator)"
                  />
                  <!-- 数字输入 -->
                  <a-input-number
                    v-else-if="field.fieldType === 'number'"
                    v-model:value="containerValues[indicator.indicatorCode][field.fieldCode]"
                    :disabled="readonly"
                    :precision="field.precision"
                    :min="0"
                    class="w-full"
                    @change="onIndicatorChange(indicator)"
                  >
                    <template #addonBefore v-if="field.prefix">{{ field.prefix }}</template>
                    <template #addonAfter v-if="field.suffix">{{ field.suffix }}</template>
                  </a-input-number>
                  <!-- 多行文本 -->
                  <a-textarea
                    v-else-if="field.fieldType === 'textarea'"
                    v-model:value="containerValues[indicator.indicatorCode][field.fieldCode]"
                    :disabled="readonly"
                    :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
                    :rows="field.rows || 3"
                    :maxlength="field.maxLength"
                    :show-count="!!field.maxLength"
                    @change="onIndicatorChange(indicator)"
                  />
                  <!-- 单选 -->
                  <a-radio-group
                    v-else-if="field.fieldType === 'radio'"
                    v-model:value="containerValues[indicator.indicatorCode][field.fieldCode]"
                    :disabled="readonly"
                    @change="onIndicatorChange(indicator)"
                  >
                    <a-radio
                      v-for="opt in field.options"
                      :key="opt.value"
                      :value="opt.value"
                    >
                      {{ opt.label }}
                    </a-radio>
                  </a-radio-group>
                  <!-- 多选 -->
                  <a-checkbox-group
                    v-else-if="field.fieldType === 'checkbox'"
                    v-model:value="containerValues[indicator.indicatorCode][field.fieldCode]"
                    :disabled="readonly"
                    @change="onIndicatorChange(indicator)"
                  >
                    <a-checkbox
                      v-for="opt in field.options"
                      :key="opt.value"
                      :value="opt.value"
                    >
                      {{ opt.label }}
                    </a-checkbox>
                  </a-checkbox-group>
                  <!-- 下拉单选 -->
                  <a-select
                    v-else-if="field.fieldType === 'select'"
                    v-model:value="containerValues[indicator.indicatorCode][field.fieldCode]"
                    :disabled="readonly"
                    :placeholder="`请选择${field.fieldLabel}`"
                    :show-search="field.showSearch"
                    allow-clear
                    class="w-full"
                    @change="onIndicatorChange(indicator)"
                  >
                    <a-select-option
                      v-for="opt in field.options"
                      :key="opt.value"
                      :value="opt.value"
                    >
                      {{ opt.label }}
                    </a-select-option>
                  </a-select>
                  <!-- 下拉多选 -->
                  <a-select
                    v-else-if="field.fieldType === 'multiSelect'"
                    v-model:value="containerValues[indicator.indicatorCode][field.fieldCode]"
                    :disabled="readonly"
                    :placeholder="`请选择${field.fieldLabel}`"
                    mode="multiple"
                    allow-clear
                    class="w-full"
                    @change="onIndicatorChange(indicator)"
                  >
                    <a-select-option
                      v-for="opt in field.options"
                      :key="opt.value"
                      :value="opt.value"
                    >
                      {{ opt.label }}
                    </a-select-option>
                  </a-select>
                  <!-- 日期 -->
                  <a-date-picker
                    v-else-if="field.fieldType === 'date'"
                    v-model:value="containerValues[indicator.indicatorCode][field.fieldCode]"
                    :disabled="readonly"
                    :show-time="field.format?.includes('HH')"
                    :format="field.format || 'YYYY-MM-DD'"
                    class="w-full"
                    @change="onIndicatorChange(indicator)"
                  />
                  <!-- 日期区间 -->
                  <a-range-picker
                    v-else-if="field.fieldType === 'dateRange'"
                    v-model:value="containerValues[indicator.indicatorCode][field.fieldCode]"
                    :disabled="readonly"
                    :show-time="field.format?.includes('HH')"
                    :format="field.format || 'YYYY-MM-DD'"
                    class="w-full"
                    @change="onIndicatorChange(indicator)"
                  />
                  <!-- 未知子字段类型 -->
                  <span v-else class="text-gray-400 text-sm">不支持的类型: {{ field.fieldType }}</span>
                </div>
              </div>

              <!-- 未知类型 -->
              <span v-else class="text-gray-400 text-sm">暂不支持该类型</span>

              <!-- 错误提示 -->
              <div v-if="indicator.id && indicatorErrors[indicator.id]" class="indicator-error">
                {{ indicatorErrors[indicator.id] }}
              </div>
            </div>
          </div>

          <!-- 右侧：上期值 -->
          <div class="indicator-last-value">
            <div class="last-value-label">上期值</div>
            <div class="last-value-content">
              {{ lastPeriodValues[indicator.indicatorCode] || '-' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 暂无指标数据时的空状态 -->
    <div
      v-if="mounted && indicatorGroups.length === 0"
      style="text-align: center; color: #999; margin-top: 40px; font-size: 14px"
    >
      暂无指标数据，请检查指标配置
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { DeclareIndicatorApi } from '#/api/declare/indicator';

import { reactive, ref, computed, onMounted } from 'vue';
import dayjs from 'dayjs';

import { IconifyIcon } from '@vben/icons';
import { message } from 'ant-design-vue';
import type { UploadFile } from 'ant-design-vue/es/upload/interface';
import { Upload } from 'ant-design-vue';

import {
  getIndicatorsByBusinessType,
  getLastPeriodValues,
  getProgressReportIndicatorValues,
} from '#/api/declare/indicator';
import { getIndicatorGroupList } from '#/api/declare/indicator-group';
import { uploadFile } from '#/api/infra/file';

interface IndicatorGroup {
  groupId: number;
  groupName: string;
  parentId: number;
  groupLevel: number;
  indicators: DeclareIndicatorApi.Indicator[];
  children: IndicatorGroup[];
}


const props = withDefaults(
  defineProps<{
    hospitalId: number;
    reportId?: number;
    reportYear?: number;
    reportBatch?: number;
    readonly?: boolean;
  }>(),
  {
    readonly: false,
  },
);

/** 组件是否已挂载（用于区分加载中与真的无数据） */
const mounted = ref(false);

const emit = defineEmits<{
  loadingChange: [loading: boolean];
}>();

/** 分组信息映射（key = groupId），包含 parentId 和 groupLevel */
const groupInfoMap = ref<Record<number, { groupName: string; parentId: number; groupLevel: number }>>({});

/** 指标列表 */
const indicators = ref<DeclareIndicatorApi.Indicator[]>([]);

/** 填报值 Map（key = indicatorCode） */
const formValues = reactive<Record<string, any>>({});

/** 指标验证错误 Map（key = indicatorId） */
const indicatorErrors = reactive<Record<number, string>>({});

/** 文件列表 Map（key = indicatorCode） */
const fileListMap = reactive<Record<string, UploadFile[]>>({});

/** 动态容器子字段定义类型 */
interface DynamicField {
  fieldCode: string;
  fieldLabel: string;
  fieldType: string;
  required?: boolean;
  sort?: number;
  options?: { value: string; label: string }[];
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
}

/** 动态容器值 Map（key = indicatorCode, value = { fieldCode: value }） */
const containerValues = reactive<Record<string, Record<string, any>>>({});

/** 获取动态容器值（通过 computed 包装，模板中使用 containerValuesRef.xxx 避免 TS undefined 检查） */
const containerValuesRef = computed(() => containerValues);

/** 上期值 Map（key = indicatorCode） */
const lastPeriodValues = ref<Record<string, string>>({});

/** 指标分组（树形结构：一级 → 二级 → 指标） */
const indicatorGroups = computed<IndicatorGroup[]>(() => {
  // 1. 初始化一级分组 Map（parentId = 0）
  const levelOneMap = new Map<number, IndicatorGroup>();
  // 2. 初始化二级分组 Map（parentId != 0），key = groupId
  const levelTwoMap = new Map<number, IndicatorGroup>();

  // 先按 parentId 分类填充分组信息
  for (const ind of indicators.value) {
    const gid = ind.groupId || 0;
    // 确保分组信息已注册（从 groupInfoMap 中获取）
    const info = groupInfoMap.value[gid];
    if (!info) continue;

    if (info.parentId === 0) {
      // 一级分组
      if (!levelOneMap.has(gid)) {
        levelOneMap.set(gid, {
          groupId: gid,
          groupName: info.groupName,
          parentId: 0,
          groupLevel: 1,
          indicators: [],
          children: [],
        });
      }
      levelOneMap.get(gid)!.indicators.push(ind);
    } else {
      // 二级分组
      if (!levelTwoMap.has(gid)) {
        levelTwoMap.set(gid, {
          groupId: gid,
          groupName: info.groupName,
          parentId: info.parentId,
          groupLevel: 2,
          indicators: [],
          children: [],
        });
      }
      levelTwoMap.get(gid)!.indicators.push(ind);
    }
  }

  // 3. 将二级分组挂到对应一级分组下
  for (const [, lvl2] of levelTwoMap) {
    const parent = levelOneMap.get(lvl2.parentId);
    if (parent) {
      parent.children.push(lvl2);
    }
  }

  // 4. 组装结果：一级分组在前，二级分组按 sort 排序后追加
  const result: IndicatorGroup[] = [];
  // 指标 sort 函数
  const sortInds = (g: IndicatorGroup) => {
    g.indicators.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));
    g.children.forEach(sortInds);
  };
  // 将二级分组展开为独立条目插在一级分组之后
  for (const [, lvl1] of levelOneMap) {
    sortInds(lvl1);
    result.push(lvl1);
    // 二级分组按 groupId 排序追加
    const sortedLvl2 = Array.from(lvl1.children.values()).sort((a, b) => a.groupId - b.groupId);
    result.push(...sortedLvl2);
  }

  return result;
});

/** 判断指标是否为计算指标 */
function isComputedIndicator(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(indicator.calculationRule && indicator.calculationRule.trim());
}

/** 解析 valueOptions JSON */
function parseOptions(valueOptions: string): Array<{ value: string; label: string }> {
  if (!valueOptions) return [];
  try {
    const parsed = JSON.parse(valueOptions);
    return Array.isArray(parsed)
      ? parsed.map((item: any) => ({
          value: String(item.value),
          label: item.label ?? item.value,
        }))
      : [];
  } catch {
    return [];
  }
}

/** 解析动态容器子字段定义 JSON */
function parseDynamicFields(valueOptions: string): DynamicField[] {
  if (!valueOptions) return [];
  try {
    const parsed = JSON.parse(valueOptions);
    if (!Array.isArray(parsed)) return [];
    // 判断是子字段定义（通过是否有 fieldCode 判断）
    const firstItem = parsed[0];
    if (firstItem && 'fieldCode' in firstItem) {
      return parsed as DynamicField[];
    }
    return [];
  } catch {
    return [];
  }
}

/** 解析扩展配置 */
function parseExtraConfig(extraConfig: string | undefined): Record<string, any> {
  if (!extraConfig) return {};
  try {
    return JSON.parse(extraConfig);
  } catch {
    return {};
  }
}

/** 获取数字精度 */
function getNumberPrecision(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.precision !== undefined ? Number(extraConfig.precision) : undefined;
}

/** 获取数字前缀 */
function getNumberPrefix(indicator: DeclareIndicatorApi.Indicator): string {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.prefix || '';
}

/** 获取数字后缀 */
function getNumberSuffix(indicator: DeclareIndicatorApi.Indicator): string {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.suffix || indicator.unit || '';
}

/** 获取日期格式 */
function getDateFormat(indicator: DeclareIndicatorApi.Indicator): string {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.format || 'YYYY-MM-DD';
}

/** 获取布尔类型的标签 */
function getBooleanLabels(indicator: DeclareIndicatorApi.Indicator): { true: string; false: string } {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return {
    true: extraConfig.trueLabel || '是',
    false: extraConfig.falseLabel || '否',
  };
}

/** 获取字符串最大长度 */
function getMaxLength(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.maxLength !== undefined ? Number(extraConfig.maxLength) : undefined;
}

/** 是否富文本 */
function isRichText(indicator: DeclareIndicatorApi.Indicator): boolean {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.richText === true;
}

/** 是否显示搜索 */
function getShowSearch(indicator: DeclareIndicatorApi.Indicator): boolean {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.showSearch === true;
}

/** 获取文件上传最大数量 */
function getMaxFileCount(indicator: DeclareIndicatorApi.Indicator): number {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  if (extraConfig.maxCount !== undefined) {
    return Number(extraConfig.maxCount);
  }
  return indicator.maxValue ? Number(indicator.maxValue) : 5;
}

/** 获取文件列表 */
function getFileList(indicatorCode: string): UploadFile[] {
  return fileListMap[indicatorCode] || [];
}

/** 从指标值记录中提取动态容器值 */
function extractContainerValue(record: any): Record<string, any> {
  if (!record || !record.valueStr) return {};
  try {
    const parsed = JSON.parse(record.valueStr);
    return typeof parsed === 'object' && parsed !== null ? parsed : {};
  } catch {
    return {};
  }
}

/** 解析已存储的文件列表 */
function parseStoredFileList(value: string | undefined): UploadFile[] {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) {
      return parsed.map((item: any, index: number) => ({
        uid: String(index),
        name: item.name || item.url?.split('/').pop() || '文件',
        url: item.url,
        status: 'done' as const,
      }));
    }
  } catch {
    // 兼容单个URL格式
    if (value.includes(',')) {
      return value.split(',').map((url, index) => ({
        uid: String(index),
        name: url.split('/').pop() || url,
        url: url.trim(),
        status: 'done' as const,
      }));
    }
    if (value.startsWith('http') || value.startsWith('/')) {
      return [{
        uid: '0',
        name: value.split('/').pop() || value,
        url: value,
        status: 'done' as const,
      }];
    }
  }
  return [];
}

/** 指标值变化处理 */
function onIndicatorChange(indicator: DeclareIndicatorApi.Indicator) {
  // 清除错误
  if (indicator.id) {
    delete indicatorErrors[indicator.id];
  }

  // 如果是动态容器，将子字段值同步到 formValues
  if (indicator.valueType === 12) {
    formValues[indicator.indicatorCode] = JSON.stringify(containerValues[indicator.indicatorCode] || {});
  }

  // 如果是计算指标，重新计算
  if (isComputedIndicator(indicator)) {
    recalculateComputedIndicators();
  }
}

/** 获取指标值 */
function getIndicatorValue(indicatorCode: string): number | undefined {
  const value = formValues[indicatorCode];
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  return Number(value);
}

/** 计算指标值 */
function calculateIndicatorValue(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  if (!indicator.calculationRule) return undefined;

  try {
    let formula = indicator.calculationRule;
    const indicatorMatches = formula.match(/\[[^\]]+\]/g) || [];

    let hasAllValues = true;
    let processedFormula = formula;

    for (const match of indicatorMatches) {
      const code = match.replace(/[\[\]]/g, '');
      const value = getIndicatorValue(code);

      if (value === undefined) {
        hasAllValues = false;
        break;
      }
      processedFormula = processedFormula.replace(match, String(value));
    }

    if (!hasAllValues) {
      return undefined;
    }

    const safeFormula = processedFormula.replace(/[^0-9+\-*/.()%]/g, '');
    if (!safeFormula || safeFormula.trim() === '') {
      return undefined;
    }

    const result = new Function(`return ${safeFormula}`)();
    if (isNaN(result) || !isFinite(result)) {
      return undefined;
    }

    return result;
  } catch {
    return undefined;
  }
}

/** 重新计算所有计算指标 */
function recalculateComputedIndicators() {
  for (const ind of indicators.value) {
    if (isComputedIndicator(ind)) {
      const calculatedValue = calculateIndicatorValue(ind);
      if (calculatedValue !== undefined) {
        formValues[ind.indicatorCode] = calculatedValue;
      }
    }
  }
}

/** 从后端返回的指标值中提取展示值 */
function extractValue(record: any, valueType: number): any {
  if (!record) return undefined;
  switch (valueType) {
    case 1:
      return record.valueNum !== null && record.valueNum !== undefined
        ? Number(record.valueNum)
        : undefined;
    case 2:
    case 6:
      return record.valueStr || undefined;
    case 3:
      return record.valueStr || undefined;
    case 4:
      return record.valueDate || undefined;
    case 5:
      return record.valueText || undefined;
    case 7:
      return record.valueStr ? record.valueStr.split(',') : undefined;
    case 8: {
      if (record.valueDateStart || record.valueDateEnd) {
        return [record.valueDateStart || null, record.valueDateEnd || null];
      }
      return undefined;
    }
    case 9:
      return record.valueStr || undefined;
    case 10:
      return record.valueStr || undefined;
    case 11:
      return record.valueStr ? record.valueStr.split(',') : undefined;
    case 12:
      return extractContainerValue(record);
    default:
      return record.valueStr || undefined;
  }
}

/** 文件上传处理 */
async function handleFileUpload(file: File, indicator: DeclareIndicatorApi.Indicator) {
  const indicatorCode = indicator.indicatorCode;
  const maxCount = getMaxFileCount(indicator);

  try {
    const result = await uploadFile({
      file: file as any,
      directory: 'declare/indicator',
    });

    const fileUrl = result.url || result;
    const fileName = file.name;

    const currentList = fileListMap[indicatorCode] || [];

    if (currentList.some(f => f.name === fileName)) {
      message.warning('文件已存在');
      return false;
    }

    if (currentList.length >= maxCount) {
      message.warning(`最多上传${maxCount}个文件`);
      return false;
    }

    const newFile: UploadFile = {
      uid: Date.now().toString(),
      name: fileName,
      url: fileUrl,
      status: 'done',
    };

    fileListMap[indicatorCode] = [...currentList, newFile];
    formValues[indicatorCode] = JSON.stringify(fileListMap[indicatorCode]);

    message.success('文件上传成功');
  } catch (error) {
    console.error('文件上传错误:', error);
    message.error('文件上传失败');
  }

  return false;
}

/** 删除文件 */
function handleFileRemove(indicatorCode: string, file: UploadFile) {
  const currentList = fileListMap[indicatorCode] || [];
  fileListMap[indicatorCode] = currentList.filter(f => f.uid !== file.uid);
  formValues[indicatorCode] = JSON.stringify(fileListMap[indicatorCode]);
  message.success('文件已删除');
}

onMounted(async () => {
  mounted.value = true;
  emit('loadingChange', true);

  try {
    // 1. 加载分组信息映射（包含 parentId 和 groupLevel）
    const groupList = await getIndicatorGroupList();
    groupList.forEach((g) => {
      if (g.id) {
        groupInfoMap.value[g.id] = {
          groupName: g.groupName,
          parentId: g.parentId ?? 0,
          groupLevel: g.groupLevel ?? 1,
        };
      }
    });

    // 2. 加载指标列表
    const indicatorData = await getIndicatorsByBusinessType('process');
    indicators.value = indicatorData;

    // 3. 加载已有填报值（编辑时）
    if (props.reportId) {
      const savedValues = await getProgressReportIndicatorValues(props.reportId);
      for (const record of savedValues) {
        const ind = indicatorData.find((i) => i.id === record.indicatorId);
        const vt = record.valueType ?? ind?.valueType ?? 1;
        const value = extractValue(record, vt);
        formValues[record.indicatorCode!] = value;

        // 初始化文件列表
        if (vt === 9 && value && record.indicatorCode) {
          fileListMap[record.indicatorCode] = parseStoredFileList(value);
        }

        // 初始化动态容器值
        if (vt === 12 && record.indicatorCode) {
          containerValues[record.indicatorCode] = value || {};
        }
      }
    }

    // 3.5 为所有动态容器指标初始化空值
    for (const ind of indicatorData) {
      if (ind.valueType === 12 && !containerValues[ind.indicatorCode]) {
        containerValues[ind.indicatorCode] = {};
      }
    }

    // 4. 加载上期参考值
    if (
      props.hospitalId &&
      props.reportYear !== undefined &&
      props.reportBatch !== undefined
    ) {
      const lastValues = await getLastPeriodValues(
        props.hospitalId,
        props.reportYear,
        props.reportBatch,
      );
      lastPeriodValues.value = lastValues || {};
    }

    // 5. 重新计算所有计算指标
    recalculateComputedIndicators();
  } catch (error) {
    console.error('[IndicatorInputTable] 加载指标数据失败:', error);
    message.error('加载指标数据失败');
  } finally {
    emit('loadingChange', false);
  }
});

/** 获取所有动态容器值（供父组件保存时调用） */
function getContainerValues(): Record<string, string> {
  const result: Record<string, string> = {};
  for (const ind of indicators.value) {
    if (ind.valueType === 12) {
      result[ind.indicatorCode] = JSON.stringify(containerValues[ind.indicatorCode] || {});
    }
  }
  return result;
}

/** 获取所有指标值（用于保存到后端），包含动态容器的完整 valueStr */
function getAllIndicatorValues(): Array<{
  indicatorId: number;
  indicatorCode: string;
  valueType: number;
  valueStr?: string;
  valueNum?: string;
  valueText?: string;
  valueBool?: boolean;
  valueDate?: string;
}> {
  const result: Array<{
    indicatorId: number;
    indicatorCode: string;
    valueType: number;
    valueStr?: string;
    valueNum?: string;
    valueText?: string;
    valueBool?: boolean;
    valueDate?: string;
  }> = [];

  for (const ind of indicators.value) {
    const code = ind.indicatorCode;
    const vt = ind.valueType;
    const rawValue = formValues[code];

    // 跳过无值
    if (rawValue === undefined || rawValue === null || rawValue === '') continue;

    const item: any = {
      indicatorId: ind.id!,
      indicatorCode: code,
      valueType: vt,
    };

    // 动态容器：JSON 字符串作为 valueStr
    if (vt === 12) {
      item.valueStr = JSON.stringify(containerValues[code] || {});
    } else if (vt === 1) {
      item.valueNum = String(rawValue);
    } else if (vt === 2 || vt === 6 || vt === 9 || vt === 10) {
      item.valueStr = String(rawValue);
    } else if (vt === 3) {
      item.valueBool = !!rawValue;
    } else if (vt === 4) {
      item.valueDate = dayjs.isDayjs(rawValue)
        ? rawValue.format('YYYY-MM-DD HH:mm:ss')
        : String(rawValue);
    } else if (vt === 5) {
      item.valueText = String(rawValue);
    } else if (vt === 7 || vt === 11) {
      item.valueStr = Array.isArray(rawValue) ? rawValue.join(',') : String(rawValue);
    } else if (vt === 8) {
      // 日期区间：rawValue 是 [start, end] 数组
      if (Array.isArray(rawValue)) {
        item.valueDateStart = dayjs.isDayjs(rawValue[0])
          ? rawValue[0].format('YYYY-MM-DD HH:mm:ss')
          : String(rawValue[0] || '');
        item.valueDateEnd = dayjs.isDayjs(rawValue[1])
          ? rawValue[1].format('YYYY-MM-DD HH:mm:ss')
          : String(rawValue[1] || '');
      }
    }

    result.push(item);
  }

  return result;
}

/** 同步动态容器值到 formValues */
function syncContainerValuesToForm(formValues: Record<string, any>) {
  for (const ind of indicators.value) {
    if (ind.valueType === 12) {
      formValues[ind.indicatorCode] = JSON.stringify(containerValues[ind.indicatorCode] || {});
    }
  }
}

defineExpose({ getContainerValues, getAllIndicatorValues, syncContainerValuesToForm, containerValues });
</script>

<style scoped>
.indicator-form-wrapper {
  padding: 16px 0;
}

.indicator-category-section {
  margin-bottom: 24px;
}

.indicator-category-section:last-child {
  margin-bottom: 0;
}

.category-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #1890ff;
}

.category-title--level2 {
  font-size: 15px;
  font-weight: 600;
  color: #555;
  border-bottom: 1px dashed #ccc;
  margin-top: 16px;
}

.indicator-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.indicator-row {
  display: grid;
  grid-template-columns: 1fr 20%;
  gap: 16px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px 16px;
  transition: border-color 0.2s;
  min-width: 0;
}

.indicator-row:hover {
  border-color: #1890ff;
}

.indicator-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.indicator-label-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label-text {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.required-tag {
  font-size: 12px;
  padding: 0 4px;
  height: 20px;
  line-height: 18px;
}

.computed-tag {
  font-size: 12px;
  padding: 0 4px;
  height: 20px;
  line-height: 18px;
}

.indicator-input-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.w-full {
  width: 100%;
}

.indicator-error {
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 4px;
}

.text-gray-400 {
  color: #999;
  font-size: 13px;
}

/* 右侧上期值样式 */
.indicator-last-value {
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-left: 1px solid #e8e8e8;
  padding-left: 16px;
}

.last-value-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.last-value-content {
  font-size: 16px;
  font-weight: 500;
  color: #666;
}

/* 文件上传样式 */
.file-upload-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  max-width: 200px;
}

.file-icon {
  flex-shrink: 0;
  color: #1890ff;
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}

.file-delete-btn {
  flex-shrink: 0;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  color: #ff4d4f;
  display: flex;
  align-items: center;
}

.file-delete-btn:hover {
  color: #ff7875;
}

/* 动态容器填报样式 */
.dynamic-container-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dynamic-field-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.dynamic-field-label {
  min-width: 120px;
  line-height: 32px;
  font-size: 14px;
  color: #333;
  flex-shrink: 0;
}

.dynamic-field-item .w-full {
  flex: 1;
}

.text-red-500 {
  color: #ff4d4f;
}
</style>
