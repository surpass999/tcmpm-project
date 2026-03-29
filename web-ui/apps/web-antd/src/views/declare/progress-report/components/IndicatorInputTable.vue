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
        {{ group.groupPrefix ? group.groupPrefix + ' ' : '' }}{{ group.groupName }}
      </div>
      <div class="indicator-list">
        <div
          v-for="indicator in group.indicators"
          :key="indicator.id"
          :data-indicator-id="indicator.id"
          :data-indicator-code="indicator.indicatorCode"
          class="indicator-row"
        >
          <!-- 左侧：指标名称 + 输入组件 -->
          <div class="indicator-main">
            <!-- 指标标签行 -->
            <div class="indicator-label-row">
              <!-- 指标名称 + 口径提示图标 -->
              <div class="flex items-center gap-1 flex-wrap">
                <!-- 有口径时用 Popover 展示 -->
                <a-popover
                  v-if="hasIndicatorSpec(indicator)"
                  placement="right"
                  trigger="click"
                  :overlay-style="{ maxWidth: '420px' }"
                >
                  <template #content>
                    <div class="caliber-popover-content">
                      <div v-if="indicator.definition" class="caliber-item">
                        <div class="caliber-label">📌 指标定义</div>
                        <div class="caliber-value">{{ indicator.definition }}</div>
                      </div>
                      <div v-if="indicator.statisticScope" class="caliber-item">
                        <div class="caliber-label">📏 统计范围</div>
                        <div class="caliber-value">{{ indicator.statisticScope }}</div>
                      </div>
                      <div v-if="indicator.dataSource" class="caliber-item">
                        <div class="caliber-label">📁 数据来源</div>
                        <div class="caliber-value">{{ indicator.dataSource }}</div>
                      </div>
                      <div v-if="indicator.fillRequire" class="caliber-item">
                        <div class="caliber-label">📝 填报要求</div>
                        <div class="caliber-value">{{ indicator.fillRequire }}</div>
                      </div>
                      <div v-if="indicator.calculationExample" class="caliber-item">
                        <div class="caliber-label">🧮 计算示例</div>
                        <div class="caliber-value">{{ indicator.calculationExample }}</div>
                      </div>
                    </div>
                  </template>
                  <span class="label-text has-caliber" :title="indicator.indicatorName">
                    {{ indicator.indicatorCode }} - {{ indicator.indicatorName }}
                    <IconifyIcon icon="lucide:help-circle" class="caliber-icon" />
                  </span>
                </a-popover>
                <!-- 无口径时只显示名称 -->
                <span v-else class="label-text" :title="indicator.indicatorName">
                  {{ indicator.indicatorCode }} - {{ indicator.indicatorName }}
                </span>
              </div>
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

              <!-- 错误提示（优先显示联合规则错误，其次显示必填/范围错误） -->
              <div
                v-if="indicator.id && (jointRuleErrors[indicator.id] || jointRuleErrors[indicator.indicatorCode] || indicatorErrors[indicator.id] || indicatorErrors[indicator.indicatorCode])"
                class="indicator-error"
              >
                {{ jointRuleErrors[indicator.id] || jointRuleErrors[indicator.indicatorCode] || indicatorErrors[indicator.id] || indicatorErrors[indicator.indicatorCode] }}
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

import { reactive, ref, computed, onMounted, watch, nextTick } from 'vue';
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
import {
  getEnabledJointRules,
  type DeclareIndicatorJointRuleApi,
} from '#/api/declare/jointRule';
import { uploadFile } from '#/api/infra/file';
import { validate as validateJointRule, parseLogicRule } from '#/utils/indicatorValidator';

interface IndicatorGroup {
  groupId: number;
  groupName: string;
  groupPrefix: string;
  parentId: number;
  groupLevel: number;
  indicators: DeclareIndicatorApi.Indicator[];
  children: IndicatorGroup[];
}


const props = withDefaults(
  defineProps<{
    hospitalId: number;
    projectType?: number;
    reportId?: number;
    reportYear?: number;
    reportBatch?: number;
    readonly?: boolean;
  }>(),
  {
    projectType: undefined,
    readonly: false,
  },
);

/** 组件是否已挂载（用于区分加载中与真的无数据） */
const mounted = ref(false);

const emit = defineEmits<{
  loadingChange: [loading: boolean];
}>();

/** 分组信息映射（key = groupId），包含 parentId、groupLevel 和 groupPrefix */
const groupInfoMap = ref<Record<number, { groupName: string; groupPrefix: string; parentId: number; groupLevel: number }>>({});

/** 指标列表 */
const indicators = ref<DeclareIndicatorApi.Indicator[]>([]);

/** 填报值 Map（key = indicatorCode） */
const formValues = reactive<Record<string, any>>({});

/** 指标验证错误 Map（key = indicatorId 数字 或 indicatorCode 字符串） */
const indicatorErrors = reactive<Record<string, string>>({});

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

/** 联合验证规则（按 projectType 加载） */
const jointRules = ref<DeclareIndicatorJointRuleApi.JointRule[]>([]);

/** 指标分组（树形结构：一级 → 二级 → 指标） */
const indicatorGroups = computed<IndicatorGroup[]>(() => {
  // 1. 先把所有分组注册到 map（一级、二级都要）
  const levelOneMap = new Map<number, IndicatorGroup>();
  const levelTwoMap = new Map<number, IndicatorGroup>();

  // 注册所有一级分组
  for (const [gid, info] of Object.entries(groupInfoMap.value)) {
    if (info.parentId === 0) {
      levelOneMap.set(Number(gid), {
        groupId: Number(gid),
        groupName: info.groupName,
        groupPrefix: info.groupPrefix,
        parentId: 0,
        groupLevel: 1,
        indicators: [],
        children: [],
      });
    }
  }

  // 注册所有二级分组并挂到父分组下
  for (const [gid, info] of Object.entries(groupInfoMap.value)) {
    if (info.parentId !== 0) {
      const lvl2: IndicatorGroup = {
        groupId: Number(gid),
        groupName: info.groupName,
        groupPrefix: info.groupPrefix,
        parentId: info.parentId,
        groupLevel: 2,
        indicators: [],
        children: [],
      };
      levelTwoMap.set(Number(gid), lvl2);
      const parent = levelOneMap.get(info.parentId);
      if (parent) {
        parent.children.push(lvl2);
      }
    }
  }

  // 2. 将指标分配到对应分组
  for (const ind of indicators.value) {
    const gid = ind.groupId || 0;
    const info = groupInfoMap.value[gid];
    if (!info) continue;

    if (info.parentId === 0) {
      // 一级分组下的指标
      const lvl1 = levelOneMap.get(gid);
      if (lvl1) {
        lvl1.indicators.push(ind);
      }
    } else {
      // 二级分组下的指标
      const lvl2 = levelTwoMap.get(gid);
      if (lvl2) {
        lvl2.indicators.push(ind);
      }
    }
  }

  // 3. 排序
  const sortInds = (g: IndicatorGroup) => {
    g.indicators.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));
    g.children.forEach(sortInds);
  };
  for (const [, lvl1] of levelOneMap) {
    sortInds(lvl1);
    lvl1.children.sort((a, b) => a.groupId - b.groupId);
  }

  // 4. 组装结果：一级分组在前，其二级子分组依次跟在后面
  const result: IndicatorGroup[] = [];
  for (const [, lvl1] of levelOneMap) {
    result.push(lvl1);
    result.push(...lvl1.children);
  }

  return result;
});

/** 判断指标是否为计算指标 */
function isComputedIndicator(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(indicator.calculationRule && indicator.calculationRule.trim());
}

/** 判断指标是否有口径 */
function hasIndicatorSpec(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(
    indicator.definition ||
    indicator.statisticScope ||
    indicator.dataSource ||
    indicator.fillRequire ||
    indicator.calculationExample
  );
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
  // 清除当前字段的错误（同时清除 id 和 code 两种 key）
  if (indicator.id) {
    delete indicatorErrors[indicator.id];
    delete jointRuleErrors[indicator.id];
  }
  delete indicatorErrors[indicator.indicatorCode];
  delete jointRuleErrors[indicator.indicatorCode];

  // 如果是动态容器，将子字段值同步到 formValues
  if (indicator.valueType === 12) {
    formValues[indicator.indicatorCode] = JSON.stringify(containerValues[indicator.indicatorCode] || {});
  }

  // 任何指标值变化，都触发所有计算指标全量重算（支持依赖链）
  recalculateComputedIndicators();

  // 实时逻辑校验（指标自身的 logicRule）
  runLogicRuleValidation();

  // 实时联合规则校验
  nextTick(() => runJointValidation());
}

/** 联合验证错误 Map（独立追踪，与必填等独立错误分开） */
const jointRuleErrors = reactive<Record<string, string>>({});

/** 构建 id → code 和 code → id 的双向查找表 */
function buildIndicatorMaps() {
  const allIndicators = indicators.value;
  const idToCode = new Map<number, string>();
  const codeToId = new Map<string, number>();
  for (const ind of allIndicators) {
    if (ind.id !== undefined) {
      idToCode.set(ind.id, ind.indicatorCode);
      codeToId.set(ind.indicatorCode, ind.id);
    }
  }
  return { idToCode, codeToId };
}

/** 运行指标自身的逻辑校验（logicRule 字符串，如 201>=20101）并更新 indicatorErrors */
function runLogicRuleValidation() {
  const { idToCode, codeToId } = buildIndicatorMaps();

  for (const indicator of indicators.value) {
    if (!indicator.logicRule?.trim()) continue;

    const rules = parseLogicRule(indicator.logicRule);
    if (rules.length === 0) continue;

    // 构建 code → value 映射
    const codeValueMap: Record<string, any> = {};
    for (const ind of indicators.value) {
      codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];
    }

    const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL', idToCode });

    const errKey = indicator.indicatorCode;

    if (results.length === 0) {
      // 验证通过：清除旧错误
      delete indicatorErrors[errKey];
      if (indicator.id) delete indicatorErrors[indicator.id];
      continue;
    }

    // 验证失败：取第一条错误信息
    const first = results[0]!;
    const errMsg = first.message || '校验失败';

    indicatorErrors[errKey] = errMsg;
    if (indicator.id) indicatorErrors[indicator.id] = errMsg;

    // 同时清除相关指标的联合规则旧错误
    const codes: string[] = [...(first.involvedIndicatorCodes || [])];
    if (first.involvedIndicatorIds) {
      for (const id of first.involvedIndicatorIds) {
        const code = idToCode.get(id);
        if (code && !codes.includes(code)) codes.push(code);
      }
    }
    for (const code of codes) {
      delete jointRuleErrors[code];
      const id = codeToId.get(code);
      if (id !== undefined) delete jointRuleErrors[id];
    }
  }
}

/** 运行联合验证并更新 jointRuleErrors（供 onIndicatorChange 实时调用） */
function runJointValidation() {
  if (jointRules.value.length === 0) return;

  const { idToCode, codeToId } = buildIndicatorMaps();

  // 构建 code → value 映射（引擎用 code 字符串查值）
  const codeValueMap: Record<string, any> = {};
  for (const ind of indicators.value) {
    codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];
  }

  const results = validateJointRule(jointRules.value as any, codeValueMap, { triggerTiming: 'FILL', idToCode });

  // 先收集所有规则涉及的全部指标 code/id（用于清除旧错误）
  const allInvolvedCodes = new Set<string>();
  const allInvolvedIds = new Set<number>();
  for (const rule of jointRules.value) {
    try {
      const config = JSON.parse(rule.ruleConfig);
      for (const item of config.rules || []) {
        const action = item.action || {};
        if (action.indicatorCode) allInvolvedCodes.add(action.indicatorCode);
        if (action.compareIndicatorCode) allInvolvedCodes.add(action.compareIndicatorCode);
        if (action.indicatorId !== undefined) allInvolvedIds.add(action.indicatorId);
        if (action.compareIndicatorId !== undefined) allInvolvedIds.add(action.compareIndicatorId);
        if (action.formula) {
          for (const f of action.formula) {
            if (f.indicatorCode) allInvolvedCodes.add(f.indicatorCode);
            if (f.indicatorId !== undefined) allInvolvedIds.add(f.indicatorId);
          }
        }
        if (action.compareFormula) {
          for (const f of action.compareFormula) {
            if (f.indicatorCode) allInvolvedCodes.add(f.indicatorCode);
            if (f.indicatorId !== undefined) allInvolvedIds.add(f.indicatorId);
          }
        }
      }
    } catch {}
  }

  if (results.length === 0) {
    // 验证全部通过：清除涉及的指标爆红
    for (const code of allInvolvedCodes) {
      delete jointRuleErrors[code];
    }
    for (const id of allInvolvedIds) {
      delete jointRuleErrors[id];
    }
    return;
  }

  for (const result of results) {
    if (!result.valid) {
      const codes: string[] = [...(result.involvedIndicatorCodes || [])];
      // 把 involvedIndicatorIds 也转成 code
      if (result.involvedIndicatorIds) {
        for (const id of result.involvedIndicatorIds) {
          const code = idToCode.get(id);
          if (code && !codes.includes(code)) {
            codes.push(code);
          }
        }
      }
      console.log('[runJointValidation] 验证失败，involvedCodes:', codes, 'message:', result.message);
      if (codes.length === 0 && result.indicatorCode) {
        codes.push(result.indicatorCode);
      }
      for (const code of codes) {
        jointRuleErrors[code] = result.message;
        const id = codeToId.get(code);
        if (id !== undefined) {
          jointRuleErrors[id] = result.message;
        }
      }
      // 从清除集合中移除已报错的指标（保留其错误状态）
      for (const code of codes) {
        allInvolvedCodes.delete(code);
      }
    }
  }
  // 清除未报错的涉及指标的旧错误
  for (const code of allInvolvedCodes) {
    delete jointRuleErrors[code];
  }
  for (const id of allInvolvedIds) {
    delete jointRuleErrors[id];
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

    if (!hasAllValues) return undefined;

    const safeFormula = processedFormula.replace(/[^0-9+\-*/.()%]/g, '');
    if (!safeFormula || safeFormula.trim() === '') return undefined;

    const result = new Function(`return ${safeFormula}`)();
    if (isNaN(result) || !isFinite(result)) return undefined;

    return result;
  } catch {
    return undefined;
  }
}

/**
 * 解析指标公式中的依赖指标代码列表
 */
function parseCalcDependencies(calcRule: string): string[] {
  if (!calcRule) return [];
  const matches = calcRule.match(/\[[^\]]+\]/g) || [];
  return matches.map(m => m.replace(/[\[\]]/g, ''));
}

/**
 * 拓扑排序：按依赖关系确定计算顺序，避免 A = B+C 而 B 还未算出
 */
function sortIndicatorsTopological(computedOnes: DeclareIndicatorApi.Indicator[]): DeclareIndicatorApi.Indicator[] {
  const codeToInd = new Map<string, DeclareIndicatorApi.Indicator>();
  computedOnes.forEach(ind => codeToInd.set(ind.indicatorCode, ind));

  const inDegree = new Map<string, number>();
  const deps = new Map<string, string[]>();

  computedOnes.forEach(ind => {
    const depsCodes = parseCalcDependencies(ind.calculationRule || '');
    inDegree.set(ind.indicatorCode, 0);
    deps.set(ind.indicatorCode, []);
    depsCodes.forEach(depCode => {
      if (codeToInd.has(depCode)) {
        deps.get(ind.indicatorCode)!.push(depCode);
        inDegree.set(ind.indicatorCode, (inDegree.get(ind.indicatorCode) || 0) + 1);
      }
    });
  });

  const queue: string[] = [];
  inDegree.forEach((deg, code) => {
    if (deg === 0) queue.push(code);
  });

  const sorted: DeclareIndicatorApi.Indicator[] = [];
  while (queue.length > 0) {
    const code = queue.shift()!;
    const ind = codeToInd.get(code)!;
    sorted.push(ind);
    const myDeps = deps.get(code) || [];
    myDeps.forEach(depCode => {
      const newDeg = (inDegree.get(depCode) || 1) - 1;
      inDegree.set(depCode, newDeg);
      if (newDeg === 0) queue.push(depCode);
    });
  }

  // 兜底：未排进来的（循环依赖或异常）按原始顺序追加
  computedOnes.forEach(ind => {
    if (!sorted.includes(ind)) sorted.push(ind);
  });

  return sorted;
}

/** 重新计算所有计算指标（拓扑排序版，支持依赖链） */
function recalculateComputedIndicators() {
  const computedOnes = indicators.value.filter(ind => isComputedIndicator(ind));
  if (computedOnes.length === 0) return;

  // 最多迭代 MAX_PASSES 轮，防止循环引用死循环
  const MAX_PASSES = computedOnes.length + 1;
  let changed = true;
  let pass = 0;

  while (changed && pass < MAX_PASSES) {
    changed = false;
    pass++;

    // 每次按拓扑序重算（已算出值的指标可作为下一轮的依赖）
    const sorted = sortIndicatorsTopological(computedOnes);
    for (const ind of sorted) {
      const prev = formValues[ind.indicatorCode];
      const calculatedValue = calculateIndicatorValue(ind);
      if (calculatedValue !== undefined) {
        formValues[ind.indicatorCode] = calculatedValue;
        if (formValues[ind.indicatorCode] !== prev) {
          changed = true;
        }
      }
    }
  }

  // 计算完成后，直接操作 DOM 刷新 disabled input 的显示值
  // （v-model 对 disabled 无效，Vue 不追踪响应式变化到原生 DOM）
  nextTick(() => {
    for (const ind of computedOnes) {
      if (formValues[ind.indicatorCode] !== undefined) {
        const el = document.querySelector(
          `[data-indicator-code="${ind.indicatorCode}"] .ant-input-number input`
        ) as HTMLInputElement | null;
        if (el) {
          el.value = String(formValues[ind.indicatorCode]);
        }
      }
    }
  });
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
          groupPrefix: g.groupPrefix || '',
          parentId: g.parentId ?? 0,
          groupLevel: g.groupLevel ?? 1,
        };
      }
    });

    // 2. 加载指标列表（按项目类型过滤）
    const indicatorData = await getIndicatorsByBusinessType('process', props.projectType);
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

/** 监听 projectType，当其变为有效值时加载联合验证规则（处理异步加载场景） */
watch(() => props.projectType, async (newProjectType) => {
  if (newProjectType === undefined || jointRules.value.length > 0) return;
  try {
    const rules = await getEnabledJointRules({
      projectType: newProjectType,
      triggerTiming: 'FILL',
    });
    jointRules.value = rules || [];
  } catch (error) {
    console.error('[IndicatorInputTable] watch 加载联合规则失败:', error);
  }
}, { immediate: false });

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

/** 验证所有必填指标，返回错误列表 */
function validateAll(indicatorsToValidate: DeclareIndicatorApi.Indicator[]): Array<{ indicatorId: number; message: string }> {
  const errors: Array<{ indicatorId: number; indicatorCode: string; message: string }> = [];

  // 构建 id → indicator 的快速查找
  const idToIndicator = new Map<number, DeclareIndicatorApi.Indicator>();
  // 构建 code → id 的反向查找（联合验证用 code 标记错误，需要 id 来构建错误对象）
  const codeToId = new Map<string, number>();
  for (const ind of indicatorsToValidate) {
    if (ind.id !== undefined) {
      idToIndicator.set(ind.id, ind);
      codeToId.set(ind.indicatorCode, ind.id);
    }
  }

  // 构建 id → value 映射（供联合验证引擎使用）
  // 同时用 numeric id 和 string indicatorCode 两种 key，方便引擎按两种方式查值
  const indicatorIdValueMap: Record<string | number, any> = {};
  for (const ind of indicatorsToValidate) {
    if (ind.id !== undefined) {
      indicatorIdValueMap[ind.id] = formValues[ind.indicatorCode];
    }
    if (ind.indicatorCode !== undefined) {
      indicatorIdValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];
    }
  }

  for (const indicator of indicatorsToValidate) {
    const code = indicator.indicatorCode;
    const vt = indicator.valueType;
    const rawValue = formValues[code];

    // 必填校验
    if (indicator.isRequired) {
      const isEmpty = rawValue === undefined || rawValue === null || rawValue === '';
      if (isEmpty) {
        if (indicator.id) {
          indicatorErrors[indicator.id] = '此项为必填';
          indicatorErrors[indicator.indicatorCode] = '此项为必填';
          errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorCode} - ${indicator.indicatorName} 为必填项` });
        }
        continue;
      }
    }

    // 数字范围校验
    if (vt === 1 && rawValue !== undefined && rawValue !== null && rawValue !== '') {
      const numVal = Number(rawValue);
      if (!isNaN(numVal)) {
        if (indicator.minValue !== undefined && numVal < indicator.minValue) {
          indicatorErrors[indicator.indicatorCode] = `不能小于 ${indicator.minValue}`;
          errors.push({ indicatorId: indicator.id!, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorCode} - ${indicator.indicatorName} 不能小于 ${indicator.minValue}` });
        }
        if (indicator.maxValue !== undefined && numVal > indicator.maxValue) {
          indicatorErrors[indicator.indicatorCode] = `不能大于 ${indicator.maxValue}`;
          errors.push({ indicatorId: indicator.id!, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorCode} - ${indicator.indicatorName} 不能大于 ${indicator.maxValue}` });
        }
      }
    }

    // 动态容器内部字段校验
    if (vt === 12 && containerValues[code]) {
      const fields = parseDynamicFields(indicator.valueOptions);
      for (const field of fields) {
        if (field.required && !containerValues[code][field.fieldCode]) {
          errors.push({ indicatorId: indicator.id!, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorCode} - ${indicator.indicatorName} 中的「${field.fieldLabel}」为必填` });
        }
      }
    }
  }

  // 联合规则校验：把 involvedIndicatorCodes 里所有涉及字段都标记为错误
  if (jointRules.value.length > 0) {
    const jointResults = validateJointRule(
      jointRules.value as any,
      indicatorIdValueMap,
      { triggerTiming: 'FILL' }
    );
    for (const result of jointResults) {
      if (!result.valid) {
        const codes = result.involvedIndicatorCodes || [];
        // 如果引擎没返回 involvedCodes，用 result.indicatorCode兜底
        if (codes.length === 0 && result.indicatorCode) {
          codes.push(result.indicatorCode);
        }

        for (const code of codes) {
          // 标记联合验证错误（写到 jointRuleErrors，与必填等独立错误分开）
          if (jointRuleErrors[code] === undefined) {
            jointRuleErrors[code] = result.message;
            const id = codeToId.get(code);
            if (id !== undefined) {
              jointRuleErrors[id] = result.message;
            }
          }
          // 避免重复 push 到 errors 数组
          const existing = errors.find(e => e.indicatorCode === code);
          if (!existing) {
            errors.push({
              indicatorId: codeToId.get(code) ?? 0,
              indicatorCode: code,
              message: result.message,
            });
          }
        }
      }
    }
  }

  return errors;
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

defineExpose({
  getContainerValues,
  getAllIndicatorValues,
  getAllIndicators: () => indicators.value,
  validateAll,
  recalculateComputedIndicators,
  syncContainerValuesToForm,
  containerValues,
});
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

.label-text.has-caliber {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #1890ff;
  cursor: pointer;
}

.caliber-icon {
  font-size: 14px;
  flex-shrink: 0;
}

/* 口径 Popover 内容 */
.caliber-popover-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 400px;
  overflow-y: auto;
  padding: 4px 0;
}

.caliber-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.caliber-label {
  font-size: 12px;
  font-weight: 600;
  color: #555;
  line-height: 1.4;
}

.caliber-value {
  font-size: 13px;
  color: #333;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
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
