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
          :class="{ 'indicator-row--switch': indicator.valueType === 3 }"
        >
          <!-- 左侧：指标名称 + 输入组件 -->
          <div
            class="indicator-main"
            :class="{ 'indicator-main--inline': indicator.valueType === 3 || indicator.valueType === 6 || indicator.valueType === 7 }"
          >
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
              <a-tag v-if="isComputedIndicator(indicator)" color="orange" class="computed-tag">自动计算</a-tag>
            </div>

            <!-- 指标输入组件行 -->
            <div
              class="indicator-input-row"
              :class="{
                'input-row--inline': indicator.valueType === 3 || indicator.valueType === 6 || indicator.valueType === 7,
              }"
            >
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
                @blur="(e: Event) => handleNumberBlur(indicator, e)"
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
                  class="switch-auto-width"
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
                  :accept="getAcceptTypes(indicator) ? '.' + getAcceptTypes(indicator).replace(/,/g, ',.') : undefined"
                  multiple
                >
                  <a-button type="dashed" :disabled="getFileList(indicator.indicatorCode).length >= getMaxFileCount(indicator)">
                    <IconifyIcon icon="lucide:plus" />
                    上传文件
                  </a-button>
                </Upload>
              </div>

              <!-- 动态容器类型（条件容器 vs 动态容器） -->
              <div v-else-if="indicator.valueType === 12">
                <!-- 条件容器：无 header，无添加按钮 -->
                <div v-show="isConditionalContainer(indicator.valueOptions)" class="conditional-container-form">
                  <div
                    v-for="(entry, entryIndex) in getContainerEntries(indicator.indicatorCode)"
                    :key="entry.rowKey"
                    class="dynamic-entry-row mb-4"
                  >
                    <div class="entry-fields space-y-3">
                      <div
                        v-for="field in getVisibleFields(indicator.valueOptions, entry)"
                        :key="field.fieldCode"
                        class="dynamic-field-item"
                      >
                        <span class="dynamic-field-label">
                          {{ field.fieldLabel }}
                          <span v-if="field.required" class="text-red-500">*</span>
                        </span>
                        <a-input
                          v-if="field.fieldType === 'text'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
                          :maxlength="field.maxLength"
                          :show-count="!!field.maxLength"
                          class="w-full"
                          @change="onIndicatorChange(indicator)"
                        />
                        <a-input-number
                          v-else-if="field.fieldType === 'number'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          :precision="field.precision"
                          :min="0"
                          class="w-full"
                          @change="onIndicatorChange(indicator)"
                        >
                          <template #addonBefore v-if="field.prefix">{{ field.prefix }}</template>
                          <template #addonAfter v-if="field.suffix">{{ field.suffix }}</template>
                        </a-input-number>
                        <a-textarea
                          v-else-if="field.fieldType === 'textarea'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
                          :rows="field.rows || 3"
                          :maxlength="field.maxLength"
                          :show-count="!!field.maxLength"
                          class="w-full"
                          @change="onIndicatorChange(indicator)"
                        />
                        <a-radio-group
                          v-else-if="field.fieldType === 'radio'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          class="flex flex-wrap gap-x-4 gap-y-2"
                          @change="onIndicatorChange(indicator)"
                        >
                          <a-radio v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio>
                        </a-radio-group>
                        <a-checkbox-group
                          v-else-if="field.fieldType === 'checkbox'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          class="flex flex-wrap gap-x-4 gap-y-2"
                          @change="onIndicatorChange(indicator)"
                        >
                          <a-checkbox v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-checkbox>
                        </a-checkbox-group>
                        <a-select
                          v-else-if="field.fieldType === 'select'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          :placeholder="`请选择${field.fieldLabel}`"
                          :show-search="field.showSearch"
                          allow-clear
                          class="w-full"
                          @change="onIndicatorChange(indicator)"
                        >
                          <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                        </a-select>
                        <a-select
                          v-else-if="field.fieldType === 'multiSelect'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          :placeholder="`请选择${field.fieldLabel}`"
                          mode="multiple"
                          allow-clear
                          class="w-full"
                          @change="onIndicatorChange(indicator)"
                        >
                          <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                        </a-select>
                        <a-date-picker
                          v-else-if="field.fieldType === 'date'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          :show-time="field.format?.includes('HH')"
                          :format="field.format || 'YYYY-MM-DD'"
                          class="w-full"
                          @change="onIndicatorChange(indicator)"
                        />
                        <a-range-picker
                          v-else-if="field.fieldType === 'dateRange'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          :show-time="field.format?.includes('HH')"
                          :format="field.format || 'YYYY-MM-DD'"
                          class="w-full"
                          @change="onIndicatorChange(indicator)"
                        />
                        <a-switch
                          v-else-if="field.fieldType === 'boolean'"
                          v-model:checked="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          @change="onIndicatorChange(indicator)"
                        />
                        <span v-else class="text-gray-400 text-sm">不支持的类型: {{ field.fieldType }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                <!-- 动态容器：有 header，有添加按钮 -->
                <div v-show="!isConditionalContainer(indicator.valueOptions)" class="dynamic-container-form">
                  <div
                    v-for="(entry, entryIndex) in getContainerEntries(indicator.indicatorCode)"
                    :key="entry.rowKey"
                    class="dynamic-entry-row mb-4"
                  >
                    <div class="entry-header flex items-center justify-between mb-2">
                      <span class="entry-label text-gray-500 text-sm font-medium">
                        条目 {{ entryIndex + 1 }}
                      </span>
                      <a-button
                        type="text"
                        danger
                        size="small"
                        @click="handleRemoveEntry(indicator.indicatorCode, entry.rowKey)"
                      >
                        <template #icon><IconifyIcon icon="lucide:trash-2" /></template>
                        删除条目
                      </a-button>
                    </div>
                    <div class="entry-fields space-y-3">
                      <div
                        v-for="field in getVisibleFields(indicator.valueOptions, entry)"
                        :key="field.fieldCode"
                        class="dynamic-field-item"
                      >
                        <span class="dynamic-field-label">
                          {{ field.fieldLabel }}
                          <span v-if="field.required" class="text-red-500">*</span>
                        </span>
                        <a-input
                          v-if="field.fieldType === 'text'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
                          :maxlength="field.maxLength"
                          :show-count="!!field.maxLength"
                          class="w-full"
                          @change="onIndicatorChange(indicator)"
                        />
                        <a-input-number
                          v-else-if="field.fieldType === 'number'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          :precision="field.precision"
                          :min="0"
                          class="w-full"
                          @change="onIndicatorChange(indicator)"
                        >
                          <template #addonBefore v-if="field.prefix">{{ field.prefix }}</template>
                          <template #addonAfter v-if="field.suffix">{{ field.suffix }}</template>
                        </a-input-number>
                        <a-textarea
                          v-else-if="field.fieldType === 'textarea'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
                          :rows="field.rows || 3"
                          :maxlength="field.maxLength"
                          :show-count="!!field.maxLength"
                          class="w-full"
                          @change="onIndicatorChange(indicator)"
                        />
                        <a-radio-group
                          v-else-if="field.fieldType === 'radio'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          class="flex flex-wrap gap-x-4 gap-y-2"
                          @change="onIndicatorChange(indicator)"
                        >
                          <a-radio v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio>
                        </a-radio-group>
                        <a-checkbox-group
                          v-else-if="field.fieldType === 'checkbox'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          class="flex flex-wrap gap-x-4 gap-y-2"
                          @change="onIndicatorChange(indicator)"
                        >
                          <a-checkbox v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-checkbox>
                        </a-checkbox-group>
                        <a-select
                          v-else-if="field.fieldType === 'select'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          :placeholder="`请选择${field.fieldLabel}`"
                          :show-search="field.showSearch"
                          allow-clear
                          class="w-full"
                          @change="onIndicatorChange(indicator)"
                        >
                          <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                        </a-select>
                        <a-select
                          v-else-if="field.fieldType === 'multiSelect'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          :placeholder="`请选择${field.fieldLabel}`"
                          mode="multiple"
                          allow-clear
                          class="w-full"
                          @change="onIndicatorChange(indicator)"
                        >
                          <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                        </a-select>
                        <a-date-picker
                          v-else-if="field.fieldType === 'date'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          :show-time="field.format?.includes('HH')"
                          :format="field.format || 'YYYY-MM-DD'"
                          class="w-full"
                          @change="onIndicatorChange(indicator)"
                        />
                        <a-range-picker
                          v-else-if="field.fieldType === 'dateRange'"
                          v-model:value="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          :show-time="field.format?.includes('HH')"
                          :format="field.format || 'YYYY-MM-DD'"
                          class="w-full"
                          @change="onIndicatorChange(indicator)"
                        />
                        <a-switch
                          v-else-if="field.fieldType === 'boolean'"
                          v-model:checked="getEntryField(indicator.indicatorCode, entryIndex)[field.fieldCode]"
                          :disabled="readonly"
                          @change="onIndicatorChange(indicator)"
                        />
                        <span v-else class="text-gray-400 text-sm">不支持的类型: {{ field.fieldType }}</span>
                      </div>
                    </div>
                  </div>
                  <a-button
                    type="dashed"
                    class="w-full mt-2"
                    :disabled="readonly"
                    @click="handleAddEntry(indicator.indicatorCode)"
                  >
                    <template #icon><PlusOutlined /></template>
                    添加条目
                  </a-button>
                  <div
                    v-if="getContainerEntries(indicator.indicatorCode).length > 0"
                    class="text-gray-400 text-xs mt-1 text-right"
                  >
                    共 {{ getContainerEntries(indicator.indicatorCode).length }} 个条目
                  </div>
                </div>
              </div>

              <!-- 未知类型 -->
              <span v-else class="text-gray-400 text-sm">暂不支持该类型</span>

              <!-- 错误提示 -->
              <div
              v-if="indicator.id && (jointRuleErrors[String(indicator.id)] || jointRuleErrors[indicator.indicatorCode])"
              class="indicator-error"
            >
              {{ jointRuleErrors[String(indicator.id)] || jointRuleErrors[indicator.indicatorCode] }}
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

import { IconifyIcon, PlusOutlined } from '@vben/icons';
import { message } from 'ant-design-vue';
import type { UploadFile } from 'ant-design-vue/es/upload/interface';
import { Upload } from 'ant-design-vue';

import {
  getIndicatorsByBusinessType,
  getLastPeriodValues,
  getProgressReportIndicatorValues,
} from '#/api/declare/indicator';
import { getIndicatorGroupTreeByProjectType } from '#/api/declare/indicator-group';
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

/** 是否已修改（任意指标有值即为 dirty，用于父组件关闭确认） */
const isDirty = ref(false);

/** 标记为已修改 */
function markDirty() {
  isDirty.value = true;
}

/** 重置 dirty（父组件在保存/重置后调用） */
function resetDirty() {
  isDirty.value = false;
}

/** 文件列表 Map（key = indicatorCode） */
const fileListMap = reactive<Record<string, UploadFile[]>>({});

/** 动态容器子字段条件显示配置 */
interface ShowCondition {
  watchField: string;
  operator: 'eq' | 'neq' | 'gt' | 'gte' | 'lt' | 'lte' | 'in' | 'notEmpty' | 'isEmpty';
  value?: any;
}

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
  showCondition?: ShowCondition;
  defaultValue?: any;
}

/** 动态容器值 Map（key = indicatorCode, value = 条目数组，每项 = { rowKey: string, [fieldCode]: value }） */
const containerValues = reactive<Record<string, any[]>>({});

/** 获取动态容器条目数组（返回空数组兜底，避免模板中 TS 报错） */
function getContainerEntries(indicatorCode: string): any[] {
  return containerValues[indicatorCode] || [];
}

/** 安全获取指定条目的字段对象（用于模板 v-model 绑定，避免 TS 报错） */
function getEntryField(indicatorCode: string, entryIndex: number): any {
  return containerValues[indicatorCode]?.[entryIndex] || {};
}

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

/** 生成唯一 rowKey（时间戳 + 随机数） */
function generateRowKey(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

/** 添加条目 */
function handleAddEntry(indicatorCode: string) {
  if (!containerValues[indicatorCode]) {
    containerValues[indicatorCode] = [];
  }
  const newRowKey = generateRowKey();
  containerValues[indicatorCode].push({ rowKey: newRowKey });
}

/** 删除条目 */
function handleRemoveEntry(indicatorCode: string, rowKey: string) {
  const entries = containerValues[indicatorCode] || [];
  const index = entries.findIndex((e: any) => e.rowKey === rowKey);
  if (index !== -1) {
    containerValues[indicatorCode]!.splice(index, 1);
  }
  // 触发同步和校验
  onIndicatorChange({ indicatorCode, valueType: 12 } as any);
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

/** 判断是否为条件容器（至少有一条 showCondition → 条件容器；否则 → 动态容器） */
function isConditionalContainer(valueOptions: string): boolean {
  const fields = parseDynamicFields(valueOptions);
  return fields.some(f => f.showCondition != null);
}

/** 将动态容器条目中的日期/日期区间字段转换为 dayjs 对象（供 ADatePicker / ARangePicker 使用） */
function convertContainerEntryDates(
  valueOptions: string,
  item: Record<string, any>,
): Record<string, any> {
  const fields = parseDynamicFields(valueOptions);
  const result: Record<string, any> = { ...item };
  for (const field of fields) {
    if (field.fieldType === 'date' && item[field.fieldCode]) {
      const d = dayjs(item[field.fieldCode]);
      if (d.isValid()) result[field.fieldCode] = d;
    } else if (field.fieldType === 'dateRange') {
      const [start, end] = Array.isArray(item[field.fieldCode]) ? item[field.fieldCode] : [null, null];
      const ds = start ? dayjs(start) : null;
      const de = end ? dayjs(end) : null;
      result[field.fieldCode] = [ds, de];
    }
  }
  return result;
}

/** 判断同条目中某字段是否可见 */
function isFieldVisible(entry: any, field: DynamicField, allFields: DynamicField[]): boolean {
  if (!field.showCondition) return true;
  const cond = field.showCondition;
  const watchVal = entry?.[cond.watchField];
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

/** 获取某条目中可见的字段列表（用于条件容器和动态容器两分支） */
function getVisibleFields(valueOptions: string, entry: any): DynamicField[] {
  const fields = parseDynamicFields(valueOptions);
  return fields.filter(f => isFieldVisible(entry, f, fields));
}

/** 获取数字精度 */
function getNumberPrecision(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.precision !== undefined ? Number(extraConfig.precision) : undefined;
}

/** 处理数字输入框失焦：范围校验（不再由 a-input-number 自动修值） */
function handleNumberBlur(indicator: DeclareIndicatorApi.Indicator, event: Event) {
  const target = event.target as HTMLInputElement;
  const rawVal = target.value;
  if (rawVal === '' || rawVal === undefined || rawVal === null) return;

  const numVal = Number(rawVal);
  if (isNaN(numVal)) return;

  let hasError = false;

  // 范围校验
  if (indicator.maxValue != null && numVal > indicator.maxValue) {
    formValues[indicator.indicatorCode] = indicator.maxValue;
    const errMsg = `不能大于 ${indicator.maxValue}`;
    if (indicator.id) jointRuleErrors[String(indicator.id)] = errMsg;
    jointRuleErrors[indicator.indicatorCode] = errMsg;
    hasError = true;
  }
  if (indicator.minValue != null && numVal < indicator.minValue) {
    formValues[indicator.indicatorCode] = indicator.minValue;
    const errMsg = `不能小于 ${indicator.minValue}`;
    if (indicator.id) jointRuleErrors[String(indicator.id)] = errMsg;
    jointRuleErrors[indicator.indicatorCode] = errMsg;
    hasError = true;
  }

  // 精度校验
  const precision = getNumberPrecision(indicator);
  if (precision !== undefined) {
    const rounded = Number(numVal.toFixed(precision));
    if (numVal !== rounded) {
      formValues[indicator.indicatorCode] = rounded;
      if (!hasError) {
        const errMsg = `小数位数不能超过 ${precision} 位`;
        if (indicator.id) jointRuleErrors[String(indicator.id)] = errMsg;
        jointRuleErrors[indicator.indicatorCode] = errMsg;
      }
    }
  }

  // 如果修正后触发 onIndicatorChange 以更新联合规则校验
  if (hasError || precision !== undefined) {
    onIndicatorChange(indicator);
  }
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

/** 获取指标允许的上传文件类型（来自 extraConfig.accept，如 "pdf,jpg,png"） */
function getAcceptTypes(indicator: DeclareIndicatorApi.Indicator): string {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.accept || '';
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
  // 只清除当前变化的指标的联合规则错误（不要清除所有指标！）
  if (indicator.id) {
    delete jointRuleErrors[String(indicator.id)];
  }
  delete jointRuleErrors[indicator.indicatorCode];

  // 如果是动态容器，将子字段值同步到 formValues
  if (indicator.valueType === 12) {
    const entries = containerValues[indicator.indicatorCode] || [];
    formValues[indicator.indicatorCode] = JSON.stringify(entries);
  }

  // 任何指标值变化，都触发所有计算指标全量重算（支持依赖链）
  recalculateComputedIndicators();

  // 实时逻辑校验（双重 nextTick 确保 form 值已更新到 formValues）
  nextTick(() => {
    nextTick(() => {
      runLogicRuleValidation();
      console.log('[onIndicatorChange after runLogicRuleValidation] jointRuleErrors:', JSON.stringify(jointRuleErrors));
    });
  });

  // 实时联合规则校验
  nextTick(() => {
    nextTick(() => {
      runJointValidation();
    });
  });
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
/** 运行逻辑规则校验并更新 jointRuleErrors */
function runLogicRuleValidation() {
  const { idToCode, codeToId } = buildIndicatorMaps();

  // 构建 code → value 映射
  const codeValueMap: Record<string, any> = {};
  for (const ind of indicators.value) {
    codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];
  }

  for (const indicator of indicators.value) {
    if (!indicator.logicRule?.trim()) continue;

    const rules = parseLogicRule(indicator.logicRule);
    if (rules.length === 0) {
      console.log('[调试] 指标', indicator.indicatorCode, 'logicRule:', indicator.logicRule, '解析结果为空');
      continue;
    }

    const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL', idToCode });
    console.log('[调试] 指标', indicator.indicatorCode, 'logicRule:', indicator.logicRule, 'results.length:', results.length);

    // 从 logicRule 字符串解析出所有涉及的指标 code（左侧和右侧的都包含）
    const ruleCodes = new Set<string>();
    const codeMatches = [...indicator.logicRule.matchAll(/\[([^\]]+)\]/g)];
    for (const m of codeMatches) ruleCodes.add(m[1]!.trim());

    if (results.length === 0) {
      // 验证通过：只清除当前指标的 code/id key（不要清除其他指标的错误！）
      const curId = indicator.id !== undefined ? String(indicator.id) : undefined;
      if (curId !== undefined) delete jointRuleErrors[curId];
      delete jointRuleErrors[indicator.indicatorCode];
      continue;
    }

    // 验证失败：只向当前指标的 code/id 写入错误（不在 ruleCodes 循环里跨指标写！）
    const errMsg = buildLogicRuleMsg(indicator.logicRule, ruleCodes, indicators.value, codeValueMap);
    const curId = indicator.id !== undefined ? String(indicator.id) : undefined;
    if (curId !== undefined) jointRuleErrors[curId] = errMsg;
    jointRuleErrors[indicator.indicatorCode] = errMsg;
    console.log('[调试] 验证失败，indicator:', indicator.indicatorCode, 'errMsg:', errMsg, '| jointRuleErrors:', JSON.stringify(jointRuleErrors));
  }
}

/** 根据 logicRule 和涉及的指标代码生成含填报要求的提示语 */
function buildLogicRuleMsg(logicRule: string, involvedCodes: Set<string>, allIndicators: typeof indicators.value, codeValueMap: Record<string, any>): string {
  if (!logicRule) return '校验失败';

  const match = logicRule.trim().match(/^(.+?)\s*(>=|<=|>|<|==|!=)\s*(.+)$/);
  if (!match) return '校验失败';

  const leftRaw = match[1]!.trim();
  const operator = match[2]!;
  const rightRaw = match[3]!.trim();
  const leftCodes = [...leftRaw.matchAll(/\[([^\]]+)\]/g)].map(m => m[1]!.trim());
  const rightCodes = [...rightRaw.matchAll(/\[([^\]]+)\]/g)].map(m => m[1]!.trim());
  const allRuleCodes = [...leftCodes, ...rightCodes];

  // 建立 code → 指标名 的映射（优先用 allIndicators，没有的用 code 本身）
  const codeMap = new Map<string, string>();
  for (const ind of allIndicators) {
    if (!codeMap.has(ind.indicatorCode)) {
      codeMap.set(ind.indicatorCode, ind.indicatorName || ind.indicatorCode);
    }
  }

  const opText: Record<string, string> = { '>=': '应大于等于', '<=': '应小于等于', '>': '应大于', '<': '应小于', '==': '应等于', '!=': '不应等于' };

  // 替换 [xxx] → "指标名(实际值)"，没有实际值则显示 "指标名(未填)"
  const replaceCode = (code: string): string => {
    const name = codeMap.get(code) || code;
    const val = codeValueMap[code];
    const valText = val !== undefined && val !== null && val !== '' ? String(val) : '未填';
    return `${name}(${valText})`;
  };

  const msgLeft = leftRaw.replace(/\[([^\]]+)\]/g, (_, c) => replaceCode(c.trim()));
  const msgRight = rightRaw.replace(/\[([^\]]+)\]/g, (_, c) => replaceCode(c.trim()));

  return `${msgLeft} ${opText[operator] || operator} ${msgRight}`;
}

/** 运行联合验证并更新 jointRuleErrors（供 onIndicatorChange 实时调用） */
async function runJointValidation() {
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
      delete jointRuleErrors[String(id)];
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
          jointRuleErrors[String(id)] = result.message;
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
    delete jointRuleErrors[String(id)];
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
      if (record.valueBool !== undefined && record.valueBool !== null) {
        return record.valueBool;
      }
      if (record.valueStr === 'true') return true;
      if (record.valueStr === 'false') return false;
      return undefined;
    case 4:
      return record.valueDate ? dayjs(record.valueDate) : undefined;
    case 5:
      return record.valueText || undefined;
    case 7:
      return record.valueStr ? record.valueStr.split(',') : undefined;
    case 8: {
      const start = record.valueDateStart ? dayjs(record.valueDateStart) : null;
      const end = record.valueDateEnd ? dayjs(record.valueDateEnd) : null;
      return start || end ? [start, end] : undefined;
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

  // 前端格式校验（指标配置中的 accept 字段）
  const acceptTypes = getAcceptTypes(indicator);
  if (acceptTypes) {
    const fileExt = file.name.split('.').pop()?.toLowerCase() || '';
    const allowedExts = acceptTypes.split(',').map((ext: string) => ext.trim().toLowerCase());
    if (!allowedExts.includes(fileExt)) {
      message.warning(`仅支持上传以下格式：${acceptTypes}（由管理员在指标配置中设置）`);
      return false;
    }
  }

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

  // projectType 未就绪时跳过加载，等待 watch 触发
  if (props.projectType === undefined) {
    return;
  }

  emit('loadingChange', true);

  try {
    // 1. 加载分组信息映射（包含 parentId 和 groupLevel）
    const groupTree = await getIndicatorGroupTreeByProjectType(props.projectType);
    groupInfoMap.value = {};
    groupTree.forEach((item) => {
      if (item.id) {
        groupInfoMap.value[item.id] = {
          groupName: item.groupName,
          groupPrefix: item.groupPrefix || '',
          parentId: item.parentId ?? 0,
          groupLevel: 1,
        };
        (item.children || []).forEach((child: any) => {
          if (child.id) {
            groupInfoMap.value[child.id] = {
              groupName: child.groupName,
              groupPrefix: child.groupPrefix || '',
              parentId: child.parentId ?? item.id,
              groupLevel: 2,
            };
          }
        });
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

        // 初始化动态容器值：兼容旧格式（对象）和新格式（数组）
        if (vt === 12 && record.indicatorCode) {
          const raw = value;
          if (Array.isArray(raw)) {
            containerValues[record.indicatorCode] = raw.map((item: any) => ({
              rowKey: item.rowKey || generateRowKey(),
              ...convertContainerEntryDates(ind!.valueOptions, item),
            }));
          } else if (raw && typeof raw === 'object') {
            // 旧格式兼容：单个对象转为一行
            containerValues[record.indicatorCode] = [
              { rowKey: generateRowKey(), ...convertContainerEntryDates(ind!.valueOptions, raw) },
            ];
          } else {
            containerValues[record.indicatorCode] = [{ rowKey: generateRowKey() }];
          }
        }
      }
    }

    // 3.5 为所有动态容器指标初始化空值（至少保证有一行）
    for (const ind of indicatorData) {
      if (ind.valueType === 12 && !containerValues[ind.indicatorCode]) {
        containerValues[ind.indicatorCode] = [{ rowKey: generateRowKey() }];
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

    // 6. 初始化逻辑校验和联合校验
    runLogicRuleValidation();
    runJointValidation();
  } catch (error) {
    console.error('[IndicatorInputTable] 加载指标数据失败:', error);
    message.error('加载指标数据失败');
  } finally {
    emit('loadingChange', false);
  }
});

/** 监听 projectType，当其变为有效值时加载完整指标数据（处理异步加载场景） */
watch(() => props.projectType, async (newProjectType) => {
  if (newProjectType === undefined) return;
  emit('loadingChange', true);

  try {
    // 1. 加载分组信息映射（包含 parentId 和 groupLevel）
    const groupTree = await getIndicatorGroupTreeByProjectType(newProjectType);
    groupInfoMap.value = {};
    groupTree.forEach((item) => {
      if (item.id) {
        groupInfoMap.value[item.id] = {
          groupName: item.groupName,
          groupPrefix: item.groupPrefix || '',
          parentId: item.parentId ?? 0,
          groupLevel: 1,
        };
        (item.children || []).forEach((child: any) => {
          if (child.id) {
            groupInfoMap.value[child.id] = {
              groupName: child.groupName,
              groupPrefix: child.groupPrefix || '',
              parentId: child.parentId ?? item.id,
              groupLevel: 2,
            };
          }
        });
      }
    });

    // 2. 加载指标列表（按项目类型过滤）
    const indicatorData = await getIndicatorsByBusinessType('process', newProjectType);
    indicators.value = indicatorData;

    // 3. 加载已有填报值（编辑时）
    Object.keys(formValues).forEach(key => delete formValues[key]);
    if (props.reportId) {
      const savedValues = await getProgressReportIndicatorValues(props.reportId);
      for (const record of savedValues) {
        const ind = indicatorData.find((i) => i.id === record.indicatorId);
        const vt = record.valueType ?? ind?.valueType ?? 1;
        const value = extractValue(record, vt);
        formValues[record.indicatorCode!] = value;

        if (vt === 9 && value && record.indicatorCode) {
          fileListMap[record.indicatorCode] = parseStoredFileList(value);
        }
        if (vt === 12 && record.indicatorCode) {
          const raw = value;
          if (Array.isArray(raw)) {
            containerValues[record.indicatorCode] = raw.map((item: any) => ({
              rowKey: item.rowKey || generateRowKey(),
              ...convertContainerEntryDates(ind!.valueOptions, item),
            }));
          } else if (raw && typeof raw === 'object') {
            containerValues[record.indicatorCode] = [
              { rowKey: generateRowKey(), ...convertContainerEntryDates(ind!.valueOptions, raw) },
            ];
          } else {
            containerValues[record.indicatorCode] = [{ rowKey: generateRowKey() }];
          }
        }
      }
    }

    // 3.5 为所有动态容器指标初始化空值（至少保证有一行）
    for (const ind of indicatorData) {
      if (ind.valueType === 12 && !containerValues[ind.indicatorCode]) {
        containerValues[ind.indicatorCode] = [{ rowKey: generateRowKey() }];
      }
    }

    // 4. 加载上期参考值
    if (props.hospitalId && props.reportYear !== undefined && props.reportBatch !== undefined) {
      const lastValues = await getLastPeriodValues(props.hospitalId, props.reportYear, props.reportBatch);
      lastPeriodValues.value = lastValues || {};
    }

    // 5. 加载联合规则（如果尚未加载）
    if (jointRules.value.length === 0) {
      try {
        const rules = await getEnabledJointRules({
          projectType: newProjectType,
          triggerTiming: 'FILL',
        });
        jointRules.value = rules || [];
      } catch (error) {
        console.error('[IndicatorInputTable] 加载联合规则失败:', error);
      }
    }

    // 6. 重新计算所有计算指标
    recalculateComputedIndicators();

    // 7. 初始化逻辑校验和联合校验
    runLogicRuleValidation();
    runJointValidation();
  } catch (error) {
    console.error('[IndicatorInputTable] watch 加载指标数据失败:', error);
    message.error('加载指标数据失败');
  } finally {
    emit('loadingChange', false);
  }
}, { immediate: false });

/** 监听 formValues 变化，只要任意指标有值就标记 dirty */
watch(
  () => formValues,
  () => {
    const hasAnyValue = Object.values(formValues).some(
      (v) => v !== undefined && v !== null && v !== '',
    );
    if (hasAnyValue) {
      markDirty();
    }
  },
  { deep: true },
);

/** 获取所有动态容器值（供父组件保存时调用） */
function getContainerValues(): Record<string, string> {
  const result: Record<string, string> = {};
  for (const ind of indicators.value) {
    if (ind.valueType === 12) {
      result[ind.indicatorCode] = JSON.stringify(containerValues[ind.indicatorCode] || []);
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
          jointRuleErrors[String(indicator.id)] = '此项为必填';
          jointRuleErrors[indicator.indicatorCode] = '此项为必填';
          errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorCode} - ${indicator.indicatorName} 为必填项` });
        }
        continue;
      }
    }

    // 数字范围校验
    if (vt === 1 && rawValue !== undefined && rawValue !== null && rawValue !== '') {
      const numVal = Number(rawValue);
      if (!isNaN(numVal)) {
        if ((indicator.minValue != null) && numVal < indicator.minValue) {
          jointRuleErrors[indicator.indicatorCode] = `不能小于 ${indicator.minValue}`;
          errors.push({ indicatorId: indicator.id!, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorCode} - ${indicator.indicatorName} 不能小于 ${indicator.minValue}` });
        }
        if ((indicator.maxValue != null) && numVal > indicator.maxValue) {
          jointRuleErrors[indicator.indicatorCode] = `不能大于 ${indicator.maxValue}`;
          errors.push({ indicatorId: indicator.id!, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorCode} - ${indicator.indicatorName} 不能大于 ${indicator.maxValue}` });
        }
      }
    }

    // 动态容器内部字段校验：遍历条目数组，验证每行的必填字段
    if (vt === 12 && Array.isArray(containerValues[code])) {
      const fields = parseDynamicFields(indicator.valueOptions);
      for (let i = 0; i < containerValues[code].length; i++) {
        const entry = containerValues[code][i];
        for (const field of fields) {
          if (field.required && isFieldVisible(entry, field, fields) && !entry[field.fieldCode]) {
            errors.push({
              indicatorId: indicator.id!,
              indicatorCode: indicator.indicatorCode,
              message: `${indicator.indicatorName} 第${i + 1}个条目的「${field.fieldLabel}」为必填`,
            });
          }
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
              jointRuleErrors[String(id)] = result.message;
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
      item.valueStr = JSON.stringify(containerValues[code] || []);
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
      formValues[ind.indicatorCode] = JSON.stringify(containerValues[ind.indicatorCode] || []);
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
  isDirty,
  resetDirty,
});
</script>

<style scoped>
.indicator-form-wrapper {
  padding: 4px 0;
}

.indicator-category-section {
  margin-bottom: 32px;
}

.indicator-category-section:last-child {
  margin-bottom: 0;
}

.category-title {
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--foreground));
  margin-bottom: 14px;
  padding-bottom: 10px;
  padding-left: 12px;
  border-bottom: 2px solid hsl(var(--primary));
  border-left: 4px solid hsl(var(--primary));
  letter-spacing: 0.3px;
}

.category-title--level2 {
  font-size: 14px;
  color: hsl(var(--muted-foreground));
  border-bottom-color: hsl(var(--primary-light) / 0.5);
  border-left-color: hsl(var(--primary-light));
}

.indicator-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.indicator-row {
  display: grid;
  grid-template-columns: 1fr 20%;
  gap: 0;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 10px;
  padding: 14px 16px;
  transition: border-color 0.2s, box-shadow 0.2s;
  position: relative;
  overflow: hidden;
}

.indicator-row::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: hsl(var(--primary));
  border-radius: 10px 0 0 10px;
  opacity: 0;
  transition: opacity 0.2s;
}

.indicator-row::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: hsl(var(--primary));
  border-radius: 10px 0 0 10px;
  opacity: 0;
  transition: opacity 0.2s;
}

.indicator-row::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: hsl(var(--primary));
  border-radius: 10px 0 0 10px;
  opacity: 0;
  transition: opacity 0.2s;
}

.indicator-row:hover {
  border-color: hsl(var(--primary) / 0.4);
  box-shadow: 0 2px 12px hsl(var(--primary) / 0.08);
}

.indicator-row:hover::before {
  opacity: 1;
}

.indicator-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 30px;
  flex: 1;
  min-width: 0;
}

.indicator-main--inline {
  flex: none;
  align-self: center;
}

.indicator-last-value {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-left: 1px solid hsl(var(--border));
  padding-left: 16px;
  gap: 4px;
}

/* 开关/radio/checkbox 行：让 grid 列收缩到内容宽度 */
.indicator-main--inline {
  flex: none;
  align-self: center;
}

.indicator-label-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.label-text {
  font-size: 14px;
  font-weight: 500;
  color: hsl(var(--foreground));
  line-height: 1.4;
}

.label-text.has-caliber {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: hsl(var(--primary));
  cursor: pointer;
  font-weight: 600;
  transition: color 0.15s;
}

.label-text.has-caliber:hover {
  color: hsl(var(--primary-dark));
}

.caliber-icon {
  font-size: 13px;
  flex-shrink: 0;
  opacity: 0.7;
}

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
  color: hsl(var(--muted-foreground));
  line-height: 1.4;
}

.caliber-value {
  font-size: 13px;
  color: hsl(var(--foreground));
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.required-tag {
  font-size: 11px;
  padding: 0 6px;
  height: 20px;
  line-height: 18px;
  border-radius: 4px;
  font-weight: 600;
  background: hsl(var(--destructive) / 0.1);
  color: hsl(var(--destructive));
  border-color: hsl(var(--destructive) / 0.2);
}

.computed-tag {
  font-size: 11px;
  padding: 0 6px;
  height: 20px;
  line-height: 18px;
  border-radius: 4px;
  font-weight: 600;
  background: hsl(var(--warning) / 0.1);
  color: hsl(var(--warning));
  border-color: hsl(var(--warning) / 0.2);
}

.switch-wrapper {
  display: inline-flex;
  align-items: center;
}

.switch-auto-width {
  width: auto !important;
}

.indicator-input-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

/* 开关/radio/checkbox 同行显示，不撑满宽度 */
.input-row--inline {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.input-row--inline :deep(.ant-switch),
.input-row--inline :deep(.ant-radio-group),
.input-row--inline :deep(.ant-checkbox-group) {
  width: auto !important;
  min-width: 0 !important;
}

.w-full {
  width: 100%;
}

.indicator-error {
  display: flex;
  align-items: center;
  gap: 6px;
  color: hsl(var(--destructive));
  font-size: 12.5px;
  margin-top: 6px;
  padding: 6px 10px;
  background: hsl(var(--destructive) / 0.06);
  border: 1px solid hsl(var(--destructive) / 0.2);
  border-radius: 6px;
  line-height: 1.4;
}

.indicator-last-value {
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-left: 1px solid hsl(var(--border));
  padding-left: 16px;
  gap: 4px;
}

.last-value-label {
  font-size: 11px;
  color: hsl(var(--muted-foreground));
  font-weight: 500;
  letter-spacing: 0.3px;
  text-transform: uppercase;
}

.last-value-content {
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--success));
  line-height: 1.2;
}

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
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  max-width: 200px;
}

.file-icon {
  flex-shrink: 0;
  color: hsl(var(--primary));
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: hsl(var(--foreground));
}

.file-delete-btn {
  flex-shrink: 0;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  color: hsl(var(--destructive));
  display: flex;
  align-items: center;
  opacity: 0.7;
  transition: opacity 0.15s;
}

.file-delete-btn:hover {
  opacity: 1;
}

.dynamic-container-form {
  display: flex;
  flex-direction: column;
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
  color: hsl(var(--foreground));
  flex-shrink: 0;
}

.dynamic-field-item .w-full {
  flex: 1;
}

.dynamic-entry-row {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
  padding: 16px;
}

.entry-header {
  border-bottom: 1px solid hsl(var(--border));
  padding-bottom: 8px;
  margin-bottom: 12px;
}

.entry-label {
  font-weight: 600;
  color: hsl(var(--muted-foreground));
}

.entry-fields .dynamic-field-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.entry-fields .dynamic-field-label {
  min-width: unset;
  line-height: 1.5;
}

.entry-fields .dynamic-field-item .w-full {
  width: 100%;
  flex: unset;
}

.text-red-500 {
  color: hsl(var(--destructive));
  font-weight: 600;
}
</style>
