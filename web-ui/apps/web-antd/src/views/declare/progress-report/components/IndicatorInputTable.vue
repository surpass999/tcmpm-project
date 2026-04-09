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
          :data-indicator-id="'in_' + indicator.id"
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
              <!-- 数字类型：使用 SafeNumberInput 支持后缀显示，blur 时只报错不校准 -->
              <SafeNumberInput
                v-if="indicator.valueType === 1"
                v-model="formValues[indicator.indicatorCode]"
                :disabled="readonly || isComputedIndicator(indicator)"
                :placeholder="isComputedIndicator(indicator) ? '自动计算' : `请输入数字`"
                :min="indicator.minValue ?? 0"
                :max="indicator.maxValue"
                :precision="getNumberPrecision(indicator)"
                :suffix="indicator.unit || parseExtraConfig(indicator.extraConfig).suffix"
                class="w-full number-input-auto-width"
                @blur="(e: Event) => handleNumberBlur(indicator, e)"
              />

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
                :value="formValues[indicator.indicatorCode] || []"
                :disabled="readonly"
                class="flex flex-wrap gap-x-4 gap-y-2"
                @change="(vals: string[]) => handleMultiSelectChange(indicator, vals)"
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
                :value="formValues[indicator.indicatorCode] || []"
                :disabled="readonly"
                :placeholder="`请选择${indicator.indicatorName}`"
                mode="multiple"
                class="w-full"
                :show-search="getShowSearch(indicator)"
                allow-clear
                @change="(vals: string[]) => handleMultiSelectChange(indicator, vals)"
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
                <div v-if="!readonly" class="upload-hint">
                  <span v-if="getAcceptTypes(indicator)">支持 {{ getAcceptTypes(indicator) }}</span>
                  <span v-if="getAcceptTypes(indicator) && getMaxFileCount(indicator)">，</span>
                  <span v-if="getMaxFileCount(indicator)">最多 {{ getMaxFileCount(indicator) }} 个</span>
                  <span class="upload-count">({{ getFileList(indicator.indicatorCode).length }}/{{ getMaxFileCount(indicator) }})</span>
                </div>
              </div>

              <!-- 动态容器类型（普通容器 vs 条件容器 vs 自动条目容器） -->
              <div v-else-if="indicator.valueType === 12">
                <!-- 条件容器：无 header，无添加/删除按钮 -->
                <div v-show="getContainerType(indicator.valueOptions) === 'conditional'" class="conditional-container-form">
                  <div
                    v-for="entry in getContainerEntries(indicator.indicatorCode)"
                    :key="entry.rowKey"
                    class="dynamic-entry-row mb-4"
                  >
                    <div class="entry-fields space-y-3">
                      <div
                        v-for="field in getVisibleFields(indicator.indicatorCode, indicator.valueOptions, entry)"
                        :key="field.fieldCode"
                        class="dynamic-field-item"
                        :data-container-field-key="getContainerFieldFullErrorKey(entry, indicator.indicatorCode, field.fieldCode)"
                      >
                        <span class="dynamic-field-label">
                          {{ field.fieldLabel }}
                          <span v-if="field.required" class="text-red-500">*</span>
                        </span>
                        <a-input
                          v-if="field.fieldType === 'text'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
                          :maxlength="toNumber(field.maxLength)"
                          :show-count="field.maxLength != null"
                          class="w-full"
                          @blur="(e: FocusEvent) => onFieldBlur(indicator, entry, field, e)"
                        />
                        <SafeNumberInput
                          v-else-if="field.fieldType === 'number'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :min="field.minValue ?? 0"
                          :precision="field.precision"
                          :prefix="field.prefix"
                          :suffix="field.suffix"
                          class="number-input-auto-width"
                          @blur="(e: FocusEvent) => onFieldBlur(indicator, entry, field, e)"
                        />
                        <a-textarea
                          v-else-if="field.fieldType === 'textarea'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
                          :rows="field.rows || 3"
                          :maxlength="toNumber(field.maxLength)"
                          :show-count="field.maxLength != null"
                          class="w-full"
                          @blur="(e: FocusEvent) => onFieldBlur(indicator, entry, field, e)"
                        />
                        <a-radio-group
                          v-else-if="field.fieldType === 'radio'"
                          :value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          :disabled="readonly"
                          class="flex flex-wrap gap-x-4 gap-y-2"
                          @change="(val: any) => {
                            console.log('[radio @change]', { val, valType: typeof val, optionVals: field.options?.map((o: any) => ({ v: o.value, t: typeof o.value })) });
                            setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val);
                            onContainerFieldChange(indicator, entry, field);
                          }"
                        >
                        >
                          <a-radio v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio>
                        </a-radio-group>
                        <a-checkbox-group
                          v-else-if="field.fieldType === 'checkbox'"
                          :value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, true)"
                          :disabled="readonly"
                          class="flex flex-wrap gap-x-4 gap-y-2"
                          @change="(vals: string[]) => { setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, vals); onContainerFieldChange(indicator, entry, field); }"
                        >
                          <a-checkbox v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-checkbox>
                        </a-checkbox-group>
                        <a-select
                          v-else-if="field.fieldType === 'select'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :placeholder="`请选择${field.fieldLabel}`"
                          :show-search="field.showSearch"
                          allow-clear
                          class="w-full"
                          @change="() => onContainerFieldChange(indicator, entry, field)"
                        >
                          <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                        </a-select>
                        <a-select
                          v-else-if="field.fieldType === 'multiSelect'"
                          :value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, true)"
                          :disabled="readonly"
                          :placeholder="`请选择${field.fieldLabel}`"
                          mode="multiple"
                          allow-clear
                          class="w-full"
                          @change="(vals: string[]) => { setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, vals); onContainerFieldChange(indicator, entry, field); }"
                        >
                          <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                        </a-select>
                        <a-date-picker
                          v-else-if="field.fieldType === 'date'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :show-time="field.format?.includes('HH')"
                          :format="field.format || 'YYYY-MM-DD'"
                          class="w-full"
                          @blur="(e: FocusEvent) => onFieldBlur(indicator, entry, field, e)"
                        />
                        <a-range-picker
                          v-else-if="field.fieldType === 'dateRange'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :show-time="field.format?.includes('HH')"
                          :format="field.format || 'YYYY-MM-DD'"
                          class="w-full"
                          @blur="(e: FocusEvent) => onFieldBlur(indicator, entry, field, e)"
                        />
                        <a-switch
                          v-else-if="field.fieldType === 'boolean'"
                          :checked="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:checked="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          @change="() => onContainerFieldChange(indicator, entry, field)"
                        />
                        <span v-else class="text-gray-400 text-sm">不支持的类型: {{ field.fieldType }}</span>
                        <!-- 统一错误提示 -->
                        <div
                          v-if="getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode) && containerFieldDirty[getContainerFieldFullErrorKey(entry, indicator.indicatorCode, field.fieldCode)]"
                          class="indicator-error"
                        >
                          {{ getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode) }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 自动条目容器：有 header，有来源说明，无添加/删除按钮 -->
                <div v-show="getContainerType(indicator.valueOptions) === 'autoEntry' && isAutoEntryVisible(indicator)" class="auto-entry-container-form">
                  <div
                    v-for="entry in getContainerEntries(indicator.indicatorCode)"
                    :key="entry.rowKey"
                    class="dynamic-entry-row mb-4"
                  >
                    <div class="entry-header flex items-center justify-between mb-2">
                      <span class="entry-label text-gray-500 text-sm font-medium">
                        {{ entry.rowKey }} {{ indicator.indicatorName }}
                      </span>
                    </div>
                    <div class="entry-fields space-y-3">
                      <div
                        v-for="field in getVisibleFields(indicator.indicatorCode, indicator.valueOptions, entry)"
                        :key="field.fieldCode"
                        class="dynamic-field-item"
                        :data-container-field-key="getContainerFieldFullErrorKey(entry, indicator.indicatorCode, field.fieldCode)"
                      >
                        <span class="dynamic-field-label">
                          {{ field.fieldLabel }}
                          <span v-if="field.required" class="text-red-500">*</span>
                        </span>
                        <a-input
                          v-if="field.fieldType === 'text'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
                          :maxlength="toNumber(field.maxLength)"
                          :show-count="field.maxLength != null"
                          class="w-full"
                          @blur="(e: FocusEvent) => onFieldBlur(indicator, entry, field, e)"
                        />
                        <SafeNumberInput
                          v-else-if="field.fieldType === 'number'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :min="field.minValue ?? 0"
                          :precision="field.precision"
                          :prefix="field.prefix"
                          :suffix="field.suffix"
                          class="number-input-auto-width"
                          @blur="(e: FocusEvent) => onFieldBlur(indicator, entry, field, e)"
                        />
                        <a-textarea
                          v-else-if="field.fieldType === 'textarea'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
                          :rows="field.rows || 3"
                          :maxlength="toNumber(field.maxLength)"
                          :show-count="field.maxLength != null"
                          class="w-full"
                          @blur="(e: FocusEvent) => onFieldBlur(indicator, entry, field, e)"
                        />
                        <a-radio-group
                          v-else-if="field.fieldType === 'radio'"
                          :value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          :disabled="readonly"
                          class="flex flex-wrap gap-x-4 gap-y-2"
                          @change="(val: any) => {
                            console.log('[radio @change]', { val, valType: typeof val, optionVals: field.options?.map((o: any) => ({ v: o.value, t: typeof o.value })) });
                            setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val);
                            onContainerFieldChange(indicator, entry, field);
                          }"
                        >
                        >
                          <a-radio v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio>
                        </a-radio-group>
                        <a-checkbox-group
                          v-else-if="field.fieldType === 'checkbox'"
                          :value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, true)"
                          :disabled="readonly"
                          class="flex flex-wrap gap-x-4 gap-y-2"
                          @change="(vals: string[]) => { setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, vals); onContainerFieldChange(indicator, entry, field); }"
                        >
                          <a-checkbox v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-checkbox>
                        </a-checkbox-group>
                        <a-select
                          v-else-if="field.fieldType === 'select'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :placeholder="`请选择${field.fieldLabel}`"
                          :show-search="field.showSearch"
                          allow-clear
                          class="w-full"
                          @change="() => onContainerFieldChange(indicator, entry, field)"
                        >
                          <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                        </a-select>
                        <a-select
                          v-else-if="field.fieldType === 'multiSelect'"
                          :value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, true)"
                          :disabled="readonly"
                          :placeholder="`请选择${field.fieldLabel}`"
                          mode="multiple"
                          allow-clear
                          class="w-full"
                          @change="(vals: string[]) => { setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, vals); onContainerFieldChange(indicator, entry, field); }"
                        >
                          <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                        </a-select>
                        <a-date-picker
                          v-else-if="field.fieldType === 'date'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :show-time="field.format?.includes('HH')"
                          :format="field.format || 'YYYY-MM-DD'"
                          class="w-full"
                          @change="() => onContainerFieldChange(indicator, entry, field)"
                        />
                        <a-range-picker
                          v-else-if="field.fieldType === 'dateRange'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :show-time="field.format?.includes('HH')"
                          :format="field.format || 'YYYY-MM-DD'"
                          class="w-full"
                          @change="() => onContainerFieldChange(indicator, entry, field)"
                        />
                        <span v-else class="text-gray-400 text-sm">不支持的类型: {{ field.fieldType }}</span>

                        <!-- 字段错误提示 -->
                        <div
                          v-if="getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode) && containerFieldDirty[getContainerFieldFullErrorKey(entry, indicator.indicatorCode, field.fieldCode)]"
                          class="indicator-error"
                        >
                          {{ getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode) }}
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 底部来源说明 -->
                  <div class="text-gray-400 text-xs mt-2">
                    （由「{{ getLinkedIndicatorName(indicator) }}」指标自动生成，数量不可调整）
                  </div>
                </div>

                <!-- 普通动态容器：有 header，有添加/删除按钮 -->
                <div v-show="getContainerType(indicator.valueOptions) === 'normal'" class="dynamic-container-form">
                  <div
                    v-for="entry in getContainerEntries(indicator.indicatorCode)"
                    :key="entry.rowKey"
                    class="dynamic-entry-row mb-4"
                  >
                    <div class="entry-header flex items-center justify-between mb-2">
                      <span class="entry-label text-gray-500 text-sm font-medium">
                        {{ entry.rowKey }} {{ indicator.indicatorName }}
                        <span class="text-gray-500 text-xs font-italic">
                          {{ indicator.isRequired ? '必须填写此项内容' : '如无特殊情况，请勿删除条目' }}
                        </span>
                      </span>
                      <a-button
                        type="text"
                        danger
                        size="small"
                        :disabled="extractEntryIndex(entry.rowKey) === 1 && indicator.isRequired"
                        :title="extractEntryIndex(entry.rowKey) === 1 && indicator.isRequired ? '必填项，第一条不可删除' : ''"
                        @click="handleRemoveEntry(indicator.indicatorCode, entry.rowKey)"
                      >
                        <template #icon><IconifyIcon icon="lucide:trash-2" /></template>
                        删除条目
                      </a-button>
                    </div>
                    <div class="entry-fields space-y-3">
                      <div
                        v-for="field in getVisibleFields(indicator.indicatorCode, indicator.valueOptions, entry)"
                        :key="field.fieldCode"
                        class="dynamic-field-item"
                        :data-container-field-key="getContainerFieldFullErrorKey(entry, indicator.indicatorCode, field.fieldCode)"
                      >
                        <span class="dynamic-field-label">
                          {{ field.fieldLabel }}
                          <span v-if="field.required" class="text-red-500">*</span>
                        </span>
                        <a-input
                          v-if="field.fieldType === 'text'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
                          :maxlength="toNumber(field.maxLength)"
                          :show-count="field.maxLength != null"
                          class="w-full"
                          @blur="(e: FocusEvent) => onFieldBlur(indicator, entry, field, e)"
                        />
                        <SafeNumberInput
                          v-else-if="field.fieldType === 'number'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :min="field.minValue ?? 0"
                          :precision="field.precision"
                          :prefix="field.prefix"
                          :suffix="field.suffix"
                          class="number-input-auto-width"
                          @blur="(e: FocusEvent) => onFieldBlur(indicator, entry, field, e)"
                        />
                        <a-textarea
                          v-else-if="field.fieldType === 'textarea'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
                          :rows="field.rows || 3"
                          :maxlength="toNumber(field.maxLength)"
                          :show-count="field.maxLength != null"
                          class="w-full"
                          @blur="(e: FocusEvent) => onFieldBlur(indicator, entry, field, e)"
                        />
                        <a-radio-group
                          v-else-if="field.fieldType === 'radio'"
                          :value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          :disabled="readonly"
                          class="flex flex-wrap gap-x-4 gap-y-2"
                          @change="(val: any) => {
                            console.log('[radio @change]', { val, valType: typeof val, optionVals: field.options?.map((o: any) => ({ v: o.value, t: typeof o.value })) });
                            setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val);
                            onContainerFieldChange(indicator, entry, field);
                          }"
                        >
                        >
                          <a-radio v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio>
                        </a-radio-group>
                        <a-checkbox-group
                          v-else-if="field.fieldType === 'checkbox'"
                          :value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, true)"
                          :disabled="readonly"
                          class="flex flex-wrap gap-x-4 gap-y-2"
                          @change="(vals: string[]) => { setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, vals); onContainerFieldChange(indicator, entry, field); }"
                        >
                          <a-checkbox v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-checkbox>
                        </a-checkbox-group>
                        <a-select
                          v-else-if="field.fieldType === 'select'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :placeholder="`请选择${field.fieldLabel}`"
                          :show-search="field.showSearch"
                          allow-clear
                          class="w-full"
                          @change="() => onContainerFieldChange(indicator, entry, field)"
                        >
                          <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                        </a-select>
                        <a-select
                          v-else-if="field.fieldType === 'multiSelect'"
                          :value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, true)"
                          :disabled="readonly"
                          :placeholder="`请选择${field.fieldLabel}`"
                          mode="multiple"
                          allow-clear
                          class="w-full"
                          @change="(vals: string[]) => { setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, vals); onContainerFieldChange(indicator, entry, field); }"
                        >
                          <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                        </a-select>
                        <a-date-picker
                          v-else-if="field.fieldType === 'date'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :show-time="field.format?.includes('HH')"
                          :format="field.format || 'YYYY-MM-DD'"
                          class="w-full"
                          @blur="(e: FocusEvent) => onFieldBlur(indicator, entry, field, e)"
                        />
                        <a-range-picker
                          v-else-if="field.fieldType === 'dateRange'"
                          :model-value="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:model-value="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          :show-time="field.format?.includes('HH')"
                          :format="field.format || 'YYYY-MM-DD'"
                          class="w-full"
                          @blur="(e: FocusEvent) => onFieldBlur(indicator, entry, field, e)"
                        />
                        <a-switch
                          v-else-if="field.fieldType === 'boolean'"
                          :checked="getEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode)"
                          @update:checked="(val: any) => setEntryFieldValue(entry, indicator.indicatorCode, field.fieldCode, val)"
                          :disabled="readonly"
                          @change="() => onContainerFieldChange(indicator, entry, field)"
                        />
                        <span v-else class="text-gray-400 text-sm">不支持的类型: {{ field.fieldType }}</span>
                        <!-- 统一错误提示 -->
                        <div
                          v-if="getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode) && containerFieldDirty[getContainerFieldFullErrorKey(entry, indicator.indicatorCode, field.fieldCode)]"
                          class="indicator-error"
                        >
                          {{ getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode) }}
                        </div>
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
                    共 {{ getContainerEntries(indicator.indicatorCode).length }} 个条目（上限 {{ MAX_CONTAINER_ENTRIES }}）
                  </div>
                </div>
              </div>

              <!-- 未知类型 -->
              <span v-else class="text-gray-400 text-sm">暂不支持该类型</span>

              <!-- 错误提示：必填/精度/格式/范围错误 -->
              <div
                v-if="indicator.valueType !== 12 && indicator.id && getTopLevelError(indicator) && topLevelFieldDirty['in_' + indicator.id]"
                class="indicator-error"
              >
                {{ getTopLevelError(indicator) }}
              </div>
            </div>
          </div>

          <!-- 右侧：上期值 -->
          <div
            v-if="lastPeriodValues[indicator.indicatorCode]"
            class="indicator-last-value"
          >
            <div class="last-value-label">上期值</div>
            <div class="last-value-content">
              {{ lastPeriodValues[indicator.indicatorCode] }}
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
import { SafeNumberInput } from '#/components/safe-number-input';

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
  minValue?: number;
  maxValue?: number;
  showCondition?: ShowCondition;
  defaultValue?: any;
  logicRule?: string; // 容器字段的逻辑规则
  noRepeat?: boolean; // 文本/多行文本不可重复
}

/** 容器类型枚举 */
type ContainerType = 'normal' | 'conditional' | 'autoEntry';

/** 容器配置（解析后的 valueOptions） */
interface ContainerConfig {
  mode: ContainerType;
  link?: string;        // 自动条目容器：关联的指标代码
  fields: DynamicField[];
}

/** 动态容器值 Map（key = indicatorCode, value = rowKey -> entry 的映射，entry 自身为 reactive 确保响应式追踪） */
const containerValues = reactive<Record<string, Record<string, any>>>({});

/** 容器字段脏标记 Map（key = `${code}:${entryIndex}:${fieldCode}`，用户交互后才显示错误） */
const containerFieldDirty = reactive<Record<string, boolean>>({});

/** 顶级指标脏标记 Map（key = 'in_' + indicator.id，全局唯一，避免跨 group 同 code 互相覆盖） */
const topLevelFieldDirty = reactive<Record<string, boolean>>({});

/** 标记顶级指标为脏（key = 'in_' + indicator.id，全局唯一，避免跨 group 同 code 互相覆盖） */
function markTopLevelDirty(indicatorId: number | undefined) {
  if (indicatorId !== undefined) topLevelFieldDirty['in_' + indicatorId] = true;
}

/** 获取动态容器条目数组（返回空数组兜底，避免模板中 TS 报错） */
function getContainerEntries(indicatorCode: string): any[] {
  const entriesMap = containerValues[indicatorCode] || {};
  const entries = Object.values(entriesMap);
  const containerType = entries.length
    ? getContainerType(indicators.value.find(i => i.indicatorCode === indicatorCode)?.valueOptions || '')
    : undefined;
  if (containerType === 'conditional') {
    console.debug('[条件容器 getContainerEntries]', { indicatorCode, containerType, entries, entryKeys: entries.map(e => Object.keys(e)) });
  }
  return entries;
}

/** 获取容器条目中指定字段的值（用于模板绑定）
 * @param entry 条目对象
 * @param indicatorCode 容器指标编码
 * @param fieldCode 后台配置的字段编码
 * @param isArray 是否为数组类型（复选框/多选），返回 [] 兜底
 */
function getEntryFieldValue(entry: any, indicatorCode: string, fieldCode: string, isArray = false): any {
  const fullKey = generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
  const val = entry?.[fullKey];
  if (isArray) return val || [];
  return val;
}

/** 设置容器条目中指定字段的值（用于模板绑定）
 * @param entry 条目对象
 * @param indicatorCode 容器指标编码
 * @param fieldCode 后台配置的字段编码
 * @param value 要设置的值
 */
function setEntryFieldValue(entry: any, indicatorCode: string, fieldCode: string, value: any) {
  const fullKey = generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
  console.log('[条件容器 setEntryFieldValue]', { fullKey, value, rowKey: entry.rowKey, fieldCode, indicatorCode });
  entry[fullKey] = value;
}

/** 获取容器字段的错误/脏标记 key（使用完整 key 格式）
 * @param entry 条目对象（包含 rowKey）
 * @param indicatorCode 容器指标编码
 * @param fieldCode 后台配置的字段编码
 */
function getContainerFieldFullErrorKey(entry: any, indicatorCode: string, fieldCode: string): string {
  return generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
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
function parseOptions(valueOptions: string): Array<{ value: string; label: string; exclusive?: boolean }> {
  if (!valueOptions) return [];
  try {
    const parsed = JSON.parse(valueOptions);
    return Array.isArray(parsed)
      ? parsed.map((item: any) => ({
          value: String(item.value),
          label: item.label ?? item.value,
          exclusive: item.exclusive == true, // 用 == 兼容 "true"、1、true
        }))
      : [];
  } catch {
    return [];
  }
}

/**
 * 多选指标变化处理（支持排他选项）
 * 在 @change handler 入口处立即读取旧值（此时 Ant Design 尚未修改 formValues），
 * 再 diff 出本次操作新增的选项，决定互斥行为
 */
function handleMultiSelectChange(indicator: any, selectedValues: string[]) {
  // 在 Ant Design 更新 formValues 之前立即捕获旧值
  const prevValues = Array.isArray(formValues[indicator.indicatorCode])
    ? [...formValues[indicator.indicatorCode]]
    : [];

  const options = parseOptions(indicator.valueOptions);
  const exclusiveValues = new Set(
    options.filter((o) => o.exclusive).map((o) => o.value),
  );

  if (exclusiveValues.size === 0) {
    formValues[indicator.indicatorCode] = selectedValues;
    onIndicatorChange(indicator);
    return;
  }

  // 找出本次操作新增的选项（用于判断用户点击的是哪个）
  const prevSet = new Set(prevValues);
  const addedValues = selectedValues.filter((v) => !prevSet.has(v));

  let result: string[];

  if (addedValues.some((v) => exclusiveValues.has(v))) {
    // 选中了互斥项 → 清除其他，只保留该互斥项
    const exclusiveAdded = addedValues.find((v) => exclusiveValues.has(v));
    result = exclusiveAdded !== undefined ? [exclusiveAdded] : [...selectedValues];
  } else if (addedValues.length > 0) {
    // 选中了普通项 → 清除所有互斥项
    result = selectedValues.filter((v) => !exclusiveValues.has(v));
  } else {
    // 取消勾选（addedValues 为空）→ 直接用 selectedValues
    result = [...selectedValues];
  }

  formValues[indicator.indicatorCode] = result;
  onIndicatorChange(indicator);
}

/** 动态容器最大条目数量限制 */
const MAX_CONTAINER_ENTRIES = 99;

/** 为普通动态容器/自动条目容器生成带序号的 rowKey */
function generateContainerRowKey(indicatorCode: string, index: number): string {
  const paddedIndex = String(index).padStart(2, '0');
  return `${indicatorCode}${paddedIndex}`;
}

/** 为条件容器生成 rowKey（直接使用 indicatorCode） */
function generateConditionalRowKey(indicatorCode: string): string {
  return indicatorCode;
}

/** 获取当前容器中已有的最大序号 */
function getMaxEntryIndex(entries: any[]): number {
  let maxIndex = 0;
  for (const entry of entries) {
    const match = entry.rowKey.match(/(\d+)$/);
    if (match) {
      const idx = parseInt(match[1]!, 10);
      if (idx > maxIndex) maxIndex = idx;
    }
  }
  return maxIndex;
}

/** 为容器生成下一个可用的 rowKey
 * @returns 生成的新 rowKey，如果已达上限则返回 undefined
 */
function generateNextContainerRowKey(indicatorCode: string): string | undefined {
  const entriesMap = containerValues[indicatorCode] || {};
  const entries = Object.values(entriesMap);
  const maxIndex = getMaxEntryIndex(entries);
  const nextIndex = maxIndex + 1;
  if (nextIndex > MAX_CONTAINER_ENTRIES) {
    return undefined;
  }
  return generateContainerRowKey(indicatorCode, nextIndex);
}

/** 重新编排容器内所有条目的 rowKey（删除后调用） */
function renumberContainerEntries(indicatorCode: string) {
  const entriesMap = containerValues[indicatorCode] || {};
  const keys = Object.keys(entriesMap);
  keys.forEach((oldKey, idx) => {
    const entry = entriesMap[oldKey];
    const newRowKey = generateContainerRowKey(indicatorCode, idx + 1);
    if (oldKey !== newRowKey) {
      // 创建新条目并删除旧条目（reactive 属性无法修改 key，只能重建）
      const newEntry = reactive({ rowKey: newRowKey, ...entry });
      delete entriesMap[oldKey];
      entriesMap[newRowKey] = newEntry;
    }
  });
}

/** 从 rowKey 提取序号数字（如 'code01' -> 1） */
function extractEntryIndex(rowKey: string): number {
  const match = rowKey.match(/(\d+)$/);
  return match ? parseInt(match[1]!, 10) : 1;
}

/** 生成容器字段的完整 key（容器code + 条目索引 + 字段code）
 * rowKey 已包含 indicatorCode + 序号，所以直接拼接 rowKey + fieldCode
 * 例如：容器 502，条目 rowKey=50201，字段 code=01 -> 5020101
 */
function generateContainerFieldKey(
  _indicatorCode: string,
  rowKey: string,
  fieldCode: string
): string {
  // rowKey 已经是 indicatorCode + 序号，直接拼接 fieldCode
  const key = `${rowKey}${fieldCode}`;
  return key;
}

/** 将旧格式 rowKey 转换为新格式（用于编辑已有数据时） */
function migrateRowKeyToNewFormat(indicatorCode: string, entryIndex: number): string {
  return generateContainerRowKey(indicatorCode, entryIndex + 1);
}

/** 添加条目 */
function handleAddEntry(indicatorCode: string) {
  const indicator = indicators.value.find(i => i.indicatorCode === indicatorCode);
  if (!indicator) return;

  const containerType = getContainerType(indicator.valueOptions);

  // 条件容器不允许手动添加
  if (containerType === 'conditional') {
    return;
  }

  if (!containerValues[indicatorCode]) {
    containerValues[indicatorCode] = {};
  }

  // 普通/自动条目容器：使用带序号的 rowKey
  const newRowKey = generateNextContainerRowKey(indicatorCode);
  if (!newRowKey) {
    message.warning(`容器 ${indicatorCode} 的条目数量已达到上限（${MAX_CONTAINER_ENTRIES}）`);
    return;
  }
  // 用 reactive() 包裹 entry，使 entry[fullKey] 的修改能被 Vue 追踪
  containerValues[indicatorCode][newRowKey] = reactive({ rowKey: newRowKey });
}

/** 删除条目 */
function handleRemoveEntry(indicatorCode: string, rowKey: string) {
  const entriesMap = containerValues[indicatorCode] || {};
  if (!(rowKey in entriesMap)) return;

  // 清理该条目相关的脏标记和错误
  const entry = entriesMap[rowKey];
  const indicator = indicators.value.find(i => i.indicatorCode === indicatorCode);
  if (indicator && entry) {
    const fields = parseDynamicFields(indicator.valueOptions);
    for (const field of fields) {
      const key = generateContainerFieldKey(indicatorCode, entry.rowKey, field.fieldCode);
      delete containerFieldDirty[key];
      delete jointRuleErrors[key];
      delete logicRuleErrors[key];
    }
    // 清理容器指标本身的 logicRule 错误（key = containerCode，无 entryIndex）
    delete logicRuleErrors[indicatorCode];
  }
  // 从 map 中删除该 entry
  delete containerValues[indicatorCode]![rowKey];
  // 删除后重新编排序号
  renumberContainerEntries(indicatorCode);
  // 触发同步和校验
  onIndicatorChange({ indicatorCode, valueType: 12 } as any);
}

/** 解析动态容器子字段定义 JSON */
function parseContainerConfig(valueOptions: string): ContainerConfig {
  if (!valueOptions) return { mode: 'normal', fields: [] };
  try {
    const parsed = JSON.parse(valueOptions);
    // 数组格式（旧数据兼容）→ 默认普通容器
    if (Array.isArray(parsed)) {
      return { mode: 'normal', fields: parsed };
    }
    // 对象格式（新数据）
    return {
      mode: (parsed.mode as ContainerType) || 'normal',
      link: parsed.link,
      fields: parsed.fields || [],
    };
  } catch {
    return { mode: 'normal', fields: [] };
  }
}

/** 获取容器类型 */
function getContainerType(valueOptions: string): ContainerType {
  const config = parseContainerConfig(valueOptions);
  return config.mode;
}

/** 获取自动条目容器的关联指标代码 */
function getAutoEntryLink(valueOptions: string): string | undefined {
  const config = parseContainerConfig(valueOptions);
  return config.link;
}

/** 获取容器内的所有子字段 */
function parseDynamicFields(valueOptions: string): DynamicField[] {
  const config = parseContainerConfig(valueOptions);
  return config.fields;
}

// 判断是否为自动条目容器（供外部使用）
// 注意：前端使用 getContainerType() === 'autoEntry'，此函数预留

/** 同步自动条目容器的条目数量 */
function syncAutoEntryContainerCount(
  containerCode: string,
  linkedIndicatorCode: string
) {
  const linkedValue = formValues[linkedIndicatorCode];
  const targetCount = Math.max(0, Math.floor(Number(linkedValue)) || 0);

  // 超过上限时提示用户，只取上限值
  if (targetCount > MAX_CONTAINER_ENTRIES) {
    message.warning(`自动条目容器最多支持 ${MAX_CONTAINER_ENTRIES} 个条目，已截断`);
  }

  // 目标数量为 0 时，不做任何操作（保留已有条目，等用户输入关联指标值）
  if (targetCount <= 0) return;

  if (!containerValues[containerCode]) {
    containerValues[containerCode] = {};
  }

  const entriesMap = containerValues[containerCode];
  const effectiveTarget = Math.min(targetCount, MAX_CONTAINER_ENTRIES);
  const currentKeys = Object.keys(entriesMap).sort();

  // 超出时：清理末尾条目的错误状态并删除
  if (currentKeys.length > effectiveTarget) {
    const toDelete = currentKeys.slice(effectiveTarget);
    const indicator = indicators.value.find(i => i.indicatorCode === containerCode);
    if (indicator) {
      const fields = parseDynamicFields(indicator.valueOptions);
      for (const oldKey of toDelete) {
        const entry = entriesMap[oldKey];
        for (const field of fields) {
          const key = generateContainerFieldKey(containerCode, entry.rowKey, field.fieldCode);
          delete containerFieldDirty[key];
          delete jointRuleErrors[key];
          delete logicRuleErrors[key];
        }
        delete entriesMap[oldKey];
      }
    }
  }

  // 不足时：创建新条目
  if (currentKeys.length < effectiveTarget) {
    const startIndex = currentKeys.length + 1;
    for (let i = startIndex; i <= effectiveTarget; i++) {
      const rowKey = generateContainerRowKey(containerCode, i);
      entriesMap[rowKey] = reactive({ rowKey });
    }
  }

  // 重新编排序号（key 变化后自动完成）
  renumberContainerEntries(containerCode);
}

/** 根据关联指标变化，同步所有自动条目容器 */
function checkAndSyncLinkedAutoContainers(changedCode: string) {
  for (const indicator of indicators.value) {
    if (indicator.valueType !== 12) continue;

    const link = getAutoEntryLink(indicator.valueOptions);
    if (link === changedCode) {
      syncAutoEntryContainerCount(indicator.indicatorCode, changedCode);
    }
  }
}

/** 初始化所有自动条目容器 */
function initializeAutoEntryContainers() {
  for (const indicator of indicators.value) {
    if (indicator.valueType !== 12) continue;

    const link = getAutoEntryLink(indicator.valueOptions);
    if (link) {
      syncAutoEntryContainerCount(indicator.indicatorCode, link);
    }
  }
}

/** 获取自动条目容器关联的指标名称 */
function getLinkedIndicatorName(indicator: any): string {
  const link = getAutoEntryLink(indicator.valueOptions);
  if (!link) return '';
  const linkedIndicator = indicators.value.find(i => i.indicatorCode === link);
  return linkedIndicator?.indicatorName || link;
}

/** 判断自动条目容器是否应该显示（关联指标有正数值时才显示） */
function isAutoEntryVisible(indicator: any): boolean {
  const link = getAutoEntryLink(indicator.valueOptions);
  if (!link) return false;
  const linkedValue = formValues[link];
  return Math.max(0, Math.floor(Number(linkedValue))) > 0;
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

/** 将动态容器条目中的日期/日期区间字段转换为 dayjs 对象（供 ADatePicker / ARangePicker 使用） */
function convertContainerEntryDates(
  valueOptions: string,
  item: Record<string, any>,
  indicatorCode?: string,
  rowKey?: string,
): Record<string, any> {
  const fields = parseDynamicFields(valueOptions);
  const result: Record<string, any> = { ...item };

  // 如果提供了 indicatorCode 和 rowKey，说明是新格式，直接处理 fullKey
  if (indicatorCode && rowKey) {
    for (const field of fields) {
      const fullKey = generateContainerFieldKey(indicatorCode, rowKey, field.fieldCode);
      if (field.fieldType === 'date' && item[fullKey]) {
        const d = dayjs(item[fullKey]);
        if (d.isValid()) result[fullKey] = d;
      } else if (field.fieldType === 'dateRange') {
        const [start, end] = Array.isArray(item[fullKey]) ? item[fullKey] : [null, null];
        const ds = start ? dayjs(start) : null;
        const de = end ? dayjs(end) : null;
        result[fullKey] = [ds, de];
      }
    }
  } else {
    // 旧格式兼容：直接使用原始 fieldCode（仅用于旧数据迁移时调用）
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
  }

  return result;
}

/** 迁移旧格式容器条目为新格式（fieldCode -> fullKey）
 * 旧格式：{ rowKey: "60201", name: "张三", age: "25" }
 * 新格式：{ rowKey: "60201", "6020101": "张三", "6020201": "张三" } 等
 * @param item 原始条目
 * @param valueOptions valueOptions JSON
 * @param indicatorCode 容器指标编码
 * @param rowKey 条目 rowKey
 */
function migrateContainerEntryToFullKey(
  item: Record<string, any>,
  valueOptions: string,
  indicatorCode: string,
  rowKey: string,
): Record<string, any> {
  const fields = parseDynamicFields(valueOptions);
  console.log('[migrateContainerEntryToFullKey] item:', JSON.stringify(item), 'rowKey:', rowKey, 'fields:', fields.map((f: any) => ({ code: f.fieldCode, type: f.fieldType })));
  const result: Record<string, any> = { rowKey: item.rowKey };

  for (const field of fields) {
    const fullKey = generateContainerFieldKey(indicatorCode, rowKey, field.fieldCode);
    // 优先从 fullKey 路径读取（已存储为 fullKey 格式的数据）
    if (item[fullKey] !== undefined) {
      result[fullKey] = item[fullKey];
    }
    // 降级兼容：如果 fullKey 无值，尝试 raw fieldCode（旧格式数据）
    else if (item[field.fieldCode] !== undefined) {
      result[fullKey] = item[field.fieldCode];
    }
  }
  console.log('[migrateContainerEntryToFullKey] result:', JSON.stringify(result));
  return result;
}

/** 判断同条目中某字段是否可见
 * @param entry 条目对象（包含 rowKey）
 * @param indicatorCode 容器指标编码
 * @param field 当前字段
 * @param allFields 所有字段列表
 */
function isFieldVisible(entry: any, indicatorCode: string, field: DynamicField, allFields: DynamicField[]): boolean {
  if (!field.showCondition) return true;
  const cond = field.showCondition;
  const watchFieldFullKey = generateContainerFieldKey(indicatorCode, entry.rowKey, cond.watchField);
  const watchVal = entry?.[watchFieldFullKey];
  const { operator, value } = cond;
  const watchedField = allFields.find(f => f.fieldCode === cond.watchField);
  const isBooleanWatch = watchedField?.fieldType === 'boolean';
  let result: boolean;
  switch (operator) {
    case 'eq':
      if (isBooleanWatch) {
        const boolVal = value === 'true' || value === '1' || value === true;
        result = watchVal === boolVal;
      } else {
        result = watchVal === value;
      }
      break;
    case 'neq':
      if (isBooleanWatch) {
        const boolVal = value === 'true' || value === '1' || value === true;
        result = watchVal !== boolVal;
      } else {
        result = watchVal !== value;
      }
      break;
    case 'gt':       result = Number(watchVal) > Number(value); break;
    case 'gte':      result = Number(watchVal) >= Number(value); break;
    case 'lt':       result = Number(watchVal) < Number(value); break;
    case 'lte':      result = Number(watchVal) <= Number(value); break;
    case 'in':       result = Array.isArray(value) && value.includes(watchVal); break;
    case 'notEmpty': result = watchVal !== undefined && watchVal !== null && watchVal !== ''; break;
    case 'isEmpty':  result = watchVal === undefined || watchVal === null || watchVal === ''; break;
    default:         result = true;
  }
  console.debug('[isFieldVisible]', { fieldCode: field.fieldCode, hasShowCondition: !!field.showCondition, cond, watchVal, result });
  return result;
}

/** 获取某条目中可见的字段列表（用于条件容器和动态容器两分支）
 * @param indicatorCode 容器指标编码
 * @param valueOptions valueOptions JSON 字符串
 * @param entry 条目对象（包含 rowKey）
 */
function getVisibleFields(indicatorCode: string, valueOptions: string, entry: any): DynamicField[] {
  const fields = parseDynamicFields(valueOptions);
  const visible = fields.filter(f => isFieldVisible(entry, indicatorCode, f, fields));
  console.log('[getVisibleFields]', { indicatorCode, rowKey: entry.rowKey, totalFields: fields.length, visibleFields: visible.map(f => ({ code: f.fieldCode, type: f.fieldType })) });
  return visible;
}

/** 获取数字精度 */
function getNumberPrecision(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  const extraConfig = parseExtraConfig(indicator.extraConfig);
  return extraConfig.precision !== undefined ? Number(extraConfig.precision) : undefined;
}

// ============================================================
// 校验基础设施
// ============================================================

/** 校验错误单元 */
interface ValidationError {
  indicatorId?: number;
  indicatorCode: string;
  message: string;
  /** 容器字段错误 key，格式：容器code + 条目序号 + 字段code（如 6020101） */
  containerFieldKey?: string;
}

/** 容器字段校验上下文 */
interface ContainerFieldContext {
  indicatorId: number;
  containerCode: string;
  entryIndex: number;
  entry: Record<string, any>;
  field: DynamicField;
  fieldCode: string;
  fieldValue: any;
  fieldLabel: string;
  indicatorName: string;
  errors: ValidationError[];
}

/** 空值判断（统一处理 null/undefined/''/'[]'/'[ ]'） */
function isEmpty(value: any): boolean {
  if (value === undefined || value === null || value === '') return true;
  if (typeof value === 'string') {
    const t = value.trim();
    if (t === '' || t === '[]' || t === '[ ]') return true;
  }
  return false;
}

/** 必填校验：返回错误消息或 null */
function checkRequired(value: any, isRequired: boolean): string | null {
  if (isRequired && isEmpty(value)) return '此项为必填';
  return null;
}

/** 范围校验：仅对数字值生效，返回错误消息或 null */
function checkRange(value: number, min: number | null, max: number | null): string | null {
  if (min != null && value < min) return `不能小于 ${min}`;
  if (max != null && value > max) return `不能大于 ${max}`;
  return null;
}

/** 精度校验：仅对数字值生效，返回错误消息或 null */
function checkPrecision(value: number, precision: number | undefined): string | null {
  if (precision === undefined) return null;
  if (value !== Number(value.toFixed(precision))) return `小数位数不能超过 ${precision} 位`;
  return null;
}

/** 多选数量校验：仅对 checkbox 生效，返回错误消息或 null */
function checkSelectCount(value: any, minSelect?: number, maxSelect?: number): string | null {
  const arr = Array.isArray(value) ? value : [];
  if (minSelect != null && arr.length < minSelect) return `至少选择 ${minSelect} 项`;
  if (maxSelect != null && arr.length > maxSelect) return `最多选择 ${maxSelect} 项`;
  return null;
}

/** 统一写入 errors 数组 */
function pushError(errors: ValidationError[], code: string, message: string, id?: number, containerFieldKey?: string) {
  errors.push({
    indicatorId: id,
    indicatorCode: code,
    message,
    ...(containerFieldKey ? { containerFieldKey } : {}),
  });
}

/**
 * 容器字段通用校验（必填 + 调用 extraRules）
 * 所有容器内字段共用此入口
 * 注意：容器字段错误由 containerFieldErrors computed 自动计算，此处仅生成 ValidationError[] 供 validateAll 使用
 */
function validateContainerField(
  ctx: ContainerFieldContext,
  extraRules?: Array<(ctx: ContainerFieldContext) => string | null>,
) {
  const { field, fieldValue, errors, indicatorId, containerCode, entryIndex, fieldCode, fieldLabel, indicatorName, entry } = ctx;
  const entryLabel = `第${entryIndex + 1}个条目`;
  const containerFieldKey = generateContainerFieldKey(containerCode, entry.rowKey, fieldCode);

  // 1. 必填校验（所有字段类型，优先校验）
  if (field.required) {
    const err = checkRequired(fieldValue, true);
    if (err) {
      pushError(errors, containerCode, `${indicatorName} ${entryLabel}「${fieldLabel}」为必填`, indicatorId, containerFieldKey);
      return; // 必填失败直接返回，不继续
    }
  }

  // 2. 非空时，执行类型相关额外规则（遇到第一个错误就返回）
  if (!isEmpty(fieldValue) && extraRules) {
    for (const rule of extraRules) {
      const err = rule(ctx);
      if (err) {
        pushError(errors, containerFieldKey, `${indicatorName} ${entryLabel}「${fieldLabel}」：${err}`, indicatorId, containerFieldKey);
        return; // 遇到第一个错误就返回
      }
    }
  }
}

// ============================================================
// 按 valueType 拆分的校验方法
// ============================================================

/** valueType=1 数字：必填 → 非数字 → 范围 → 精度 */
function validateType1_Number(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const { indicatorCode: code, id, isRequired, minValue, maxValue } = indicator;
  const value = formValues[code];

  // 必填校验（失败则返回；自动计算指标跳过，由公式决定值）
  const isComputed = isComputedIndicator(indicator);
  const reqErr = checkRequired(value, !!isRequired && !isComputed);
  if (reqErr) { pushError(errors, code, `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`, id); return errors; }

  // 非空时才校验格式/范围/精度（必填未填时在上面已返回）
  if (!isEmpty(value)) {
    const numVal = Number(value);
    if (isNaN(numVal)) { pushError(errors, code, `${indicator.indicatorCode} - ${indicator.indicatorName}：请输入有效数字`, id); return errors; }
    const rangeErr = checkRange(numVal, minValue ?? null, maxValue ?? null);
    if (rangeErr) pushError(errors, code, `${indicator.indicatorCode} - ${indicator.indicatorName}：${rangeErr}`, id);
    const cfg = parseExtraConfig(indicator.extraConfig);
    const precErr = checkPrecision(numVal, cfg.precision);
    if (precErr) pushError(errors, code, `${indicator.indicatorCode} - ${indicator.indicatorName}：${precErr}`, id);
  }
  return errors;
}

/** valueType=2 文本：必填 + 纯数字校验 */
function validateType2_Text(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const value = formValues[indicator.indicatorCode];
  const isEmpty = value === undefined || value === null || value === '';

  // 必填校验
  const reqErr = checkRequired(value, !!indicator.isRequired);
  if (reqErr) { pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`, indicator.id); return errors; }

  // 非空时：纯数字校验
  if (!isEmpty) {
    const trimmed = String(value).trim();
    if (/^-?\d+(\.\d+)?$/.test(trimmed)) {
      pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：不能输入纯数字`, indicator.id);
    }
  }

  return errors;
}

/** valueType=3 布尔：无需校验 */
function validateType3_Boolean(_: DeclareIndicatorApi.Indicator): ValidationError[] { return []; }

/** valueType=4 日期：必填 */
function validateType4_Date(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const reqErr = checkRequired(formValues[indicator.indicatorCode], !!indicator.isRequired);
  if (reqErr) pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`, indicator.id);
  return errors;
}

/** valueType=5 富文本：同文本 */
function validateType5_RichText(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  return validateType2_Text(indicator);
}

/** valueType=6 下拉选择：必填 */
function validateType6_Select(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const reqErr = checkRequired(formValues[indicator.indicatorCode], !!indicator.isRequired);
  if (reqErr) pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`, indicator.id);
  return errors;
}

/** valueType=7 多选：必填（数组长度判断） */
function validateType7_MultiSelect(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const value = formValues[indicator.indicatorCode];
  const arr = Array.isArray(value) ? value : [];
  if (indicator.isRequired && arr.length === 0) {
    pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：此项为必填`, indicator.id);
  }
  return errors;
}

/** valueType=8 日期区间：必填 */
function validateType8_DateRange(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const reqErr = checkRequired(formValues[indicator.indicatorCode], !!indicator.isRequired);
  if (reqErr) pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`, indicator.id);
  return errors;
}

/** valueType=9 文件上传：必填（'[]'/'[ ]' 视为空） */
function validateType9_File(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const value = formValues[indicator.indicatorCode];
  const isFileEmpty = isEmpty(value) || value === '[]' || value === '[ ]';
  if (indicator.isRequired && isFileEmpty) {
    pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：此项为必填`, indicator.id);
  }
  return errors;
}

/** valueType=10 部门选择：必填 */
function validateType10_Dept(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const reqErr = checkRequired(formValues[indicator.indicatorCode], !!indicator.isRequired);
  if (reqErr) pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`, indicator.id);
  return errors;
}

/** valueType=11 用户选择：必填 */
function validateType11_User(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const reqErr = checkRequired(formValues[indicator.indicatorCode], !!indicator.isRequired);
  if (reqErr) pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`, indicator.id);
  return errors;
}

/**
 * valueType=12 动态容器
 * 遍历每个条目 → 每个可见字段 → validateContainerField
 * 容器内字段：required(所有) ✅ / precision(number) ✅ / selectCount(checkbox) ✅ / range(number) ⚠️ 待后端扩展
 */
function validateType12_Container(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const { indicatorCode: code, id, indicatorName } = indicator;
  const entriesMap = containerValues[code] || {};
  const entries = Object.values(entriesMap);
  const fields = parseDynamicFields(indicator.valueOptions);

  let entryIndex = 0;
  for (const entry of entries) {
    for (const field of fields) {
      if (!isFieldVisible(entry, code, field, fields)) continue;

      const fullKey = generateContainerFieldKey(code, entry.rowKey, field.fieldCode);
      const fieldValue = entry[fullKey];
      const ctx: ContainerFieldContext = {
        indicatorId: id!,
        containerCode: code,
        entryIndex,
        entry,
        field,
        fieldCode: field.fieldCode,
        fieldValue,
        fieldLabel: field.fieldLabel,
        indicatorName,
        errors,
      };

      let extraRules: Array<(ctx: ContainerFieldContext) => string | null> | undefined;

      if (field.fieldType === 'number') {
        extraRules = [(ctx) => {
          const numVal = Number(ctx.fieldValue);
          if (isNaN(numVal)) return '请输入有效数字';
          const precErr = checkPrecision(numVal, ctx.field.precision);
          return precErr;
        }];
      }

      if (field.fieldType === 'checkbox') {
        extraRules = [(ctx) => checkSelectCount(ctx.fieldValue, ctx.field.minSelect, ctx.field.maxSelect)];
      }

      if (field.fieldType === 'text' || field.fieldType === 'textarea') {
        extraRules = [(ctx) => {
          const trimmed = String(ctx.fieldValue).trim();
          if (ctx.field.fieldType === 'text' && /^-?\d+(\.\d+)?$/.test(trimmed)) {
            return '不能输入纯数字';
          }
          if (ctx.field.noRepeat && trimmed) {
            const allEntries = Object.values(containerValues[ctx.containerCode] || {});
            for (let j = 0; j < allEntries.length; j++) {
              if (j === ctx.entryIndex) continue;
              const otherFullKey = generateContainerFieldKey(ctx.containerCode, allEntries[j].rowKey, ctx.fieldCode);
              const otherVal = allEntries[j]?.[otherFullKey];
              if (String(otherVal ?? '').trim() === trimmed) {
                return `该值与「第${j + 1}个条目」重复`;
              }
            }
          }
          return null;
        }];
      }

      validateContainerField(ctx, extraRules);

      // 容器字段 logicRule 校验
      if (field.logicRule?.trim()) {
        const containerLogicRuleErrors = validateContainerFieldLogicRule(
          indicator, field, entryIndex, entry
        );
        errors.push(...containerLogicRuleErrors);
      }
    }
    entryIndex++;
  }

  return errors;
}

// ============================================================
// 主调度器 & 逻辑规则
// ============================================================

/** 校验容器内字段的 logicRule */
function validateContainerFieldLogicRule(
  containerIndicator: DeclareIndicatorApi.Indicator,
  field: DynamicField,
  _entryIndex: number,
  entry: any
): ValidationError[] {
  const errors: ValidationError[] = [];
  const containerCode = containerIndicator.indicatorCode;
  const fieldCode = field.fieldCode;
  const fullKey = generateContainerFieldKey(containerCode, entry.rowKey, fieldCode);
  const fieldValue = entry[fullKey];

  // 解析 logicRule 中涉及的所有字段代码
  const involvedFieldCodes = new Set<string>();
  for (const m of field.logicRule!.matchAll(/\[([^\]]+)\]/g)) {
    involvedFieldCodes.add(m[1]!.trim());
  }

  // 构建当前条目的字段值映射（仅限当前条目内，使用完整 key）
  const fieldValueMap: Record<string, any> = {};
  for (const fc of involvedFieldCodes) {
    const fcFullKey = generateContainerFieldKey(containerCode, entry.rowKey, fc);
    fieldValueMap[fc] = entry[fcFullKey];
  }
  fieldValueMap[fieldCode] = fieldValue;

  // 将 logicRule 中的 [fieldCode] 替换为实际值
  let processedRule = field.logicRule!;
  for (const fc of involvedFieldCodes) {
    const fcFullKey = generateContainerFieldKey(containerCode, entry.rowKey, fc);
    const val = entry[fcFullKey];
    if (val !== undefined && val !== null && val !== '') {
      processedRule = processedRule.replace(
        new RegExp(`\\[${fc.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\]`, 'g'),
        String(val)
      );
    }
  }

  // 安全过滤：只保留数字和运算符
  const safeExpr = processedRule.replace(/[^0-9+\-*/.()%<>=!&|]/g, '');
  if (!safeExpr || safeExpr.trim() === '') return errors;

  try {
    const result = new Function(`return ${safeExpr}`)();
    if (typeof result === 'boolean' && !result) {
      // 校验失败，生成错误
      const msg = buildContainerFieldLogicRuleMsg(field.logicRule ?? '', containerIndicator);
      errors.push({
        indicatorId: containerIndicator.id,
        indicatorCode: containerCode,
        message: `${field.fieldLabel}：${msg}`,
        containerFieldKey: fullKey,
      });
    }
  } catch {
    // 解析错误，静默跳过
  }

  return errors;
}

/** 生成容器字段 logicRule 的错误提示 */
function buildContainerFieldLogicRuleMsg(
  logicRule: string,
  _containerIndicator: DeclareIndicatorApi.Indicator
): string {
  if (!logicRule) return '校验失败';

  const match = logicRule.trim().match(/^(.+?)\s*(>=|<=|>|<|==|!=)\s*(.+)$/);
  if (!match) return '校验失败';

  const leftRaw = match[1]!.trim();
  const operator = match[2]!;
  const rightRaw = match[3]!.trim();

  const opText: Record<string, string> = { '>=': '应大于等于', '<=': '应小于等于', '>': '应大于', '<': '应小于', '==': '应等于', '!=': '不应等于' };

  return `${leftRaw.replace(/\[([^\]]+)\]/g, '[$1]')} ${opText[operator] || operator} ${rightRaw.replace(/\[([^\]]+)\]/g, '[$1]')}`;
}

/** 主校验入口（返回 ValidationError[]，供 ValidationSummaryModal 消费）
 * 同时将错误同步到 jointRuleErrors 和 containerFieldDirty，使行内错误能够显示
 */
function validateAll(indicatorsToValidate: DeclareIndicatorApi.Indicator[]): ValidationError[] {
  const errors: ValidationError[] = [];

  // 清空旧的 jointRuleErrors（顶级指标），保留容器字段错误（由下方的循环重新设置）
  Object.keys(jointRuleErrors).forEach(key => {
    if (!key.includes(':')) delete jointRuleErrors[key];
  });

  const validators: Record<number, (ind: DeclareIndicatorApi.Indicator) => ValidationError[]> = {
    1: validateType1_Number, 2: validateType2_Text, 3: validateType3_Boolean,
    4: validateType4_Date, 5: validateType5_RichText, 6: validateType6_Select,
    7: validateType7_MultiSelect, 8: validateType8_DateRange, 9: validateType9_File,
    10: validateType10_Dept, 11: validateType11_User, 12: validateType12_Container,
  };

  for (const indicator of indicatorsToValidate) {
    const v = validators[indicator.valueType];
    if (v) errors.push(...v(indicator));
  }

  errors.push(...validateLogicRules(indicatorsToValidate));

  // 标记所有容器字段为脏，使行内错误提示能够显示（即使用户没有与该字段交互）
  for (const error of errors) {
    if (error.containerFieldKey) {
      containerFieldDirty[error.containerFieldKey] = true;
    } else if (error.indicatorId !== undefined) {
      topLevelFieldDirty['in_' + error.indicatorId] = true;
    }
  }

  return errors;
}

/** 逻辑规则校验（遍历所有指标，检查各自 logicRule） */
function validateLogicRules(allIndicators: DeclareIndicatorApi.Indicator[]): ValidationError[] {
  const errors: ValidationError[] = [];

  const codeToIndicator = new Map<string, DeclareIndicatorApi.Indicator>();
  for (const ind of allIndicators) {
    codeToIndicator.set(ind.indicatorCode, ind);
  }

  const codeValueMap: Record<string, any> = {};
  for (const ind of allIndicators) {
    codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];
  }

  for (const indicator of allIndicators) {
    if (!indicator.logicRule?.trim()) continue;

    const rules = parseLogicRule(indicator.logicRule);
    if (rules.length === 0) continue;

    const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });
    const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, codeValueMap);

    if (results.length === 0) {
      // 校验通过：清除所有涉及指标的旧 logicRule 错误
      // 注意：一个 code 在不同 group 可能重复存在，必须遍历所有匹配 code 的指标，
      //      用精确的 id key 清除错误，避免跨组错误残留或错误清除失败
      const involvedCodes = new Set<string>();
      for (const m of indicator.logicRule.matchAll(/\[([^\]]+)\]/g)) {
        involvedCodes.add(m[1]!.trim());
      }
      for (const code of involvedCodes) {
        for (const ind of allIndicators) {
          if (ind.indicatorCode === code && ind.id !== undefined) {
            delete logicRuleErrors[String(ind.id)];
          }
        }
        delete logicRuleErrors[code];
      }
      continue;
    }

    // 校验失败：设置当前指标的 logicRule 错误（只用 id key，避免跨组 code 冲突）
    errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: errMsg });
    if (indicator.id !== undefined) logicRuleErrors[String(indicator.id)] = errMsg;
  }

  return errors;
}

/**
 * 实时逻辑规则（blur / change 时调用）
 * 只校验涉及当前字段的那些 logicRule，而非全部
 */
function validateLogicRuleForBlur(changedIndicator: DeclareIndicatorApi.Indicator) {
  const allIndicators = indicators.value;
  const changedCode = changedIndicator.indicatorCode;

  const codeToIndicator = new Map<string, DeclareIndicatorApi.Indicator>();
  for (const ind of allIndicators) {
    codeToIndicator.set(ind.indicatorCode, ind);
  }

  const codeValueMap: Record<string, any> = {};
  for (const ind of allIndicators) {
    codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];
  }

  for (const indicator of allIndicators) {
    if (!indicator.logicRule?.trim()) continue;

    const involvedCodes = new Set<string>();
    for (const m of indicator.logicRule.matchAll(/\[([^\]]+)\]/g)) {
      involvedCodes.add(m[1]!.trim());
    }
    if (!involvedCodes.has(changedCode)) continue;

    // 清除所有涉及指标的旧 logicRule 错误（旧状态）
    for (const code of involvedCodes) {
      // 遍历所有匹配 code 的指标，精确清除对应 id 的错误，避免跨组残留
      for (const ind of allIndicators) {
        if (ind.indicatorCode === code && ind.id !== undefined) {
          delete logicRuleErrors[String(ind.id)];
        }
      }
      delete logicRuleErrors[code];
    }
    // 标记当前指标为脏（由 handleNumberBlur/onIndicatorChange 已标记，但 validateLogicRuleForBlur 可能在首次渲染后调用，提前标记）
    markTopLevelDirty(indicator.id);

    const rules = parseLogicRule(indicator.logicRule);
    const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });
    if (results.length === 0) continue;

    // 校验失败：设置当前拥有 logicRule 的指标的 logicRule 错误（只用 id key，避免跨组 code 冲突）
    const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, codeValueMap);
    if (indicator.id !== undefined) logicRuleErrors[String(indicator.id)] = errMsg;
  }
}

/** 获取容器字段行内错误
 * 优先级：1. 字段级 logicRuleErrors > 2. containerFieldDirty 脏标记 > 3. containerFieldErrors
 * 注意：容器指标本身的 logicRule 错误不在此处返回（否则会导致所有字段行同时显示同一个错误，
 *       即使该行已填写有效值）。容器级 logicRule 错误应由容器头部单独展示，不干扰字段级错误。
 * @param entry 条目对象（包含 rowKey）
 * @param indicatorCode 容器指标编码
 * @param fieldCode 后台配置的字段编码
 */
function getContainerFieldError(entry: any, indicatorCode: string, fieldCode: string): string | undefined {
  const key = generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);

  // 1. 字段级 logicRule 错误（精确匹配到 entry + field）
  if (logicRuleErrors[key]) return logicRuleErrors[key];

  // 2. 只有脏标记为 true 时才返回 computed 计算的错误（避免页面加载时误显示）
  if (!containerFieldDirty[key]) return undefined;

  // 3. 返回实时 computed 错误（必填/格式/精度）
  return containerFieldErrors.value[key];
}

/** 获取顶级指标行内错误（优先级：logicRuleErrors > jointRuleErrors > indicatorErrors）
 * 优先用 indicator.id 作为 key（全局唯一，避免跨 group 同 code 互相覆盖），
 * 后备用 indicatorCode（code 可能重复，但与 id 共存时以 id 为准）
 */
function getTopLevelError(indicator: DeclareIndicatorApi.Indicator): string | undefined {
  // 全部用 id key（全局唯一），避免跨 group 同 code 互相覆盖
  if (indicator.id === undefined) return undefined;
  const idKey = String(indicator.id);
  if (logicRuleErrors[idKey]) return logicRuleErrors[idKey];
  if (jointRuleErrors[idKey]) return jointRuleErrors[idKey];
  if (indicatorErrors.value[idKey]) return indicatorErrors.value[idKey];
  return undefined;
}

/** 数字类型 blur 事件 */
function handleNumberBlur(indicator: DeclareIndicatorApi.Indicator, _event: Event) {
  markTopLevelDirty(indicator.id);
  nextTick(() => {
    recalculateComputedIndicators();
    validateLogicRuleForBlur(indicator);
  });
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

/** 防御性转数字，解决数据库已存字符串值的问题 */
function toNumber(val: any): number | undefined {
  if (val == null || val === '') return undefined;
  const n = Number(val);
  return isNaN(n) ? undefined : n;
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
  markTopLevelDirty(indicator.id);

  // 如果是动态容器，将子字段值同步到 formValues
  if (indicator.valueType === 12) {
    const entriesMap = containerValues[indicator.indicatorCode] || {};
    formValues[indicator.indicatorCode] = JSON.stringify(Object.values(entriesMap));
  }

  // 触发关联的自动条目容器同步
  checkAndSyncLinkedAutoContainers(indicator.indicatorCode);

  // 触发逻辑规则校验
  nextTick(() => {
    validateLogicRuleForBlur(indicator);
  });
}

/** 容器字段失焦验证：标记脏 + 触发容器逻辑规则 + 触发所有涉及指标的联合规则
 * 注意：对于 radio/checkbox/select 等选择类字段，blur 时 DOM value 为空，
 * 值由 @change 事件通过 setEntryFieldValue 写入，所以跳过 blur 校验避免误判必填。
 */
function onFieldBlur(indicator: DeclareIndicatorApi.Indicator, entry: any, field: DynamicField, event: FocusEvent) {
  // radio / checkbox / select / cascader 等选择类字段的值由 @change 事件写入，blur 时 DOM value 为空，跳过
  if (field.fieldType === 'radio' || field.fieldType === 'checkbox' || field.fieldType === 'select' || field.fieldType === 'cascader' || field.fieldType === 'treeSelect') {
    return;
  }

  const key = generateContainerFieldKey(indicator.indicatorCode, entry.rowKey, field.fieldCode);

  // 从 DOM 原生 input 元素读取当前值（解决 a-input 在某些情况下 @update:model-value 与 @blur 时序问题）
  const target = event.target as HTMLInputElement;
  const rawValue = target?.value ?? '';

  // 按字段类型归一化值：number 转数字，其他保留字符串
  let normalizedValue: any;
  if (field.fieldType === 'number') {
    const trimmed = rawValue.trim();
    normalizedValue = trimmed === '' ? null : Number(trimmed);
  } else {
    normalizedValue = rawValue;
  }

  // 脏值比较：只有真正变化了才写入（减少不必要的响应式触发）
  if (normalizedValue !== entry[key]) {
    entry[key] = normalizedValue;
  }

  // 标记为脏
  containerFieldDirty[key] = true;
  nextTick(() => {
    recalculateComputedIndicators();
    validateLogicRuleForBlur(indicator);
  });
}

/** 容器字段值变化验证（单选/复选/下拉等选择类型） */
function onContainerFieldChange(indicator: DeclareIndicatorApi.Indicator, entry: any, field: DynamicField) {
  const key = generateContainerFieldKey(indicator.indicatorCode, entry.rowKey, field.fieldCode);
  containerFieldDirty[key] = true;
  nextTick(() => {
    recalculateComputedIndicators();
    validateLogicRuleForBlur(indicator);
  });
}

/** 顶级指标错误（computed 直接计算，不存储） */
const indicatorErrors = computed(() => {
  const errors: Record<string, string> = {};
  const allIndicators = indicators.value;

  for (const ind of allIndicators) {
    const code = ind.indicatorCode;
    const value = formValues[code];
    const isEmpty = value === undefined || value === null || value === '';
    const isComputed = isComputedIndicator(ind);

    // 优先用 id key（全局唯一，避免跨 group 同 code 互相覆盖）；无 id 时用 code key兜底
    const idKey = ind.id !== undefined ? String(ind.id) : code;

    // 1. 必填校验（自动计算指标跳过，由公式决定值）
    if (ind.isRequired && isEmpty && !isComputed) {
      errors[idKey] = '此项为必填';
      continue;
    }

    // 2. 非空时才校验格式
    if (!isEmpty) {
      // 文本类型：不能输入纯数字
      if (ind.valueType === 2) {
        const trimmed = String(value).trim();
        if (/^-?\d+(\.\d+)?$/.test(trimmed)) {
          errors[idKey] = '不能输入纯数字';
          continue;
        }
      }
      // 数字类型额外校验
      if (ind.valueType === 1) {
        const numVal = Number(value);
        if (isNaN(numVal)) {
          errors[idKey] = '请输入有效数字';
          continue;
        }
        // 精度校验
        const cfg = parseExtraConfig(ind.extraConfig);
        const precErr = checkPrecision(numVal, cfg.precision);
        if (precErr) {
          errors[idKey] = precErr;
          continue;
        }
        // 范围校验
        const rangeErr = checkRange(numVal, ind.minValue ?? null, ind.maxValue ?? null);
        if (rangeErr) {
          errors[idKey] = rangeErr;
        }
      }
    }
  }

  return errors;
});

/** 容器字段错误（computed 直接计算） */
const containerFieldErrors = computed(() => {
  const errors: Record<string, string> = {};

  for (const indicator of indicators.value) {
    if (indicator.valueType !== 12) continue; // 仅处理容器类型

    const code = indicator.indicatorCode;
    const entriesMap = containerValues[code] || {};
    const entries = Object.values(entriesMap);
    const fields = parseDynamicFields(indicator.valueOptions);

    for (let i = 0; i < entries.length; i++) {
      const entry = entries[i];
      for (const field of fields) {
        if (!isFieldVisible(entry, code, field, fields)) continue;

        const fullKey = generateContainerFieldKey(code, entry.rowKey, field.fieldCode);
        const fieldValue = entry[fullKey];
        const isEmpty = fieldValue === undefined || fieldValue === null || fieldValue === '';

        // 1. 必填校验
        if (field.required && isEmpty) {
          errors[fullKey] = '此项为必填';
          continue;
        }

        // 2. 非空时校验格式
        if (!isEmpty) {
          // 文本类型 / 多行文本：不能输入纯数字
          if (field.fieldType === 'text' || field.fieldType === 'textarea') {
            const trimmed = String(fieldValue).trim();
            if (field.fieldType === 'text' && /^-?\d+(\.\d+)?$/.test(trimmed)) {
              errors[fullKey] = '不能输入纯数字';
              continue;
            }
            // 不可重复校验（文本和多行文本都支持）
            if (field.noRepeat && trimmed) {
              for (let j = 0; j < entries.length; j++) {
                if (j === i) continue;
                const otherFullKey = generateContainerFieldKey(code, entries[j].rowKey, field.fieldCode);
                const otherVal = entries[j]?.[otherFullKey];
                if (String(otherVal ?? '').trim() === trimmed) {
                  errors[fullKey] = `该值与「第${j + 1}个条目」重复`;
                  break;
                }
              }
            }
          }
          if (field.fieldType === 'number') {
            const numVal = Number(fieldValue);
            if (isNaN(numVal)) {
              errors[fullKey] = '请输入有效数字';
              continue;
            }
            const precErr = checkPrecision(numVal, field.precision);
            if (precErr) {
              errors[fullKey] = precErr;
              continue;
            }
            // 范围校验：未配置 minValue 时默认 0（没有配置入口时容器内数字不可为负）
            const rangeErr = checkRange(numVal, field.minValue ?? 0, field.maxValue ?? null);
            if (rangeErr) {
              errors[fullKey] = rangeErr;
            }
          }
          if (field.fieldType === 'checkbox') {
            const countErr = checkSelectCount(fieldValue, field.minSelect, field.maxSelect);
            if (countErr) {
              errors[fullKey] = countErr;
            }
          }
        }
      }
    }
  }

  return errors;
});

/** 联合校验错误 Map（validateAll 将 ValidationError 同步至此，供 getContainerFieldError 读取）
 * key 格式：
 * - 顶级指标：`indicatorCode` 或 `indicatorId`
 * - 容器字段：`容器code + 条目序号 + 字段code`（如 6020101）
 */
const jointRuleErrors = reactive<Record<string, string>>({});

/** 逻辑规则错误 Map（仅用于多指标联合验证，需要跨指标共享错误） */
const logicRuleErrors = reactive<Record<string, string>>({});

/** 根据 logicRule 生成含填报要求的提示语 */
function buildLogicRuleMsg(logicRule: string, allIndicators: typeof indicators.value, codeValueMap: Record<string, any>): string {
  if (!logicRule) return '校验失败';

  const match = logicRule.trim().match(/^(.+?)\s*(>=|<=|>|<|==|!=)\s*(.+)$/);
  if (!match) return '校验失败';

  const leftRaw = match[1]!.trim();
  const operator = match[2]!;
  const rightRaw = match[3]!.trim();

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
      const calculatedValue = calculateIndicatorValue(ind);
      if (calculatedValue !== undefined) {
        let finalValue: number = calculatedValue;

        // 精度截断（静默，不报错）
        const precision = getNumberPrecision(ind);
        if (precision !== undefined) {
          finalValue = Number(calculatedValue.toFixed(precision));
        }

        formValues[ind.indicatorCode] = finalValue;

        // 范围校验由 indicatorErrors computed 自动处理，此处不需要手动设置错误
      }
    }
  }

  // 计算完成后，直接操作 DOM 刷新 disabled input 的显示值
  // （v-model 对 disabled 无效，Vue 不追踪响应式变化到原生 DOM）
  nextTick(() => {
    for (const ind of computedOnes) {
      if (formValues[ind.indicatorCode] !== undefined) {
        // SafeNumberInput（.safe-number-input-inner）和原版 a-input（.ant-input input）都兼容
        const el = document.querySelector(
          `[data-indicator-code="${ind.indicatorCode}"] .safe-number-input-inner,` +
          `[data-indicator-code="${ind.indicatorCode}"] .ant-input input`
        ) as HTMLInputElement | null;
        if (el) {
          el.value = String(formValues[ind.indicatorCode]);
        }
      }
    }
    // 标记所有计算指标为脏，使范围错误能正确显示
    for (const ind of computedOnes) {
      markTopLevelDirty(ind.id);
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
          const containerType = ind ? getContainerType(ind.valueOptions) : 'normal';
          const raw = value;
          const indicatorCode = record.indicatorCode;
          console.log('[容器初始化] indicatorCode:', indicatorCode, 'containerType:', containerType, 'raw:', JSON.stringify(raw));
          containerValues[indicatorCode] = {};
          if (Array.isArray(raw)) {
            raw.forEach((item: any, idx: number) => {
              const rowKey = item.rowKey || migrateRowKeyToNewFormat(indicatorCode, idx);
              const entryWithFullKey = migrateContainerEntryToFullKey(item, ind!.valueOptions, indicatorCode, rowKey);
              const dates = convertContainerEntryDates(ind!.valueOptions, entryWithFullKey, indicatorCode, rowKey);
              const entryData = { rowKey, ...item, ...dates };
              containerValues[indicatorCode]![rowKey] = reactive(entryData);
            });
          } else if (raw && typeof raw === 'object') {
            // 旧格式兼容：单个对象转为一行
            if (containerType === 'conditional') {
              const rowKey = generateConditionalRowKey(indicatorCode);
              const entryWithFullKey = migrateContainerEntryToFullKey(raw, ind!.valueOptions, indicatorCode, rowKey);
              const dates = convertContainerEntryDates(ind!.valueOptions, entryWithFullKey, indicatorCode, rowKey);
              containerValues[indicatorCode]![rowKey] = reactive({ rowKey, ...raw, ...dates });
            } else {
              const rowKey = generateContainerRowKey(indicatorCode, 1);
              const entryWithFullKey = migrateContainerEntryToFullKey(raw, ind!.valueOptions, indicatorCode, rowKey);
              const dates = convertContainerEntryDates(ind!.valueOptions, entryWithFullKey, indicatorCode, rowKey);
              containerValues[indicatorCode]![rowKey] = reactive({ rowKey, ...raw, ...dates });
            }
          } else {
            if (containerType === 'conditional') {
              containerValues[indicatorCode]![generateConditionalRowKey(indicatorCode)] = reactive({ rowKey: generateConditionalRowKey(indicatorCode) });
            } else {
              containerValues[indicatorCode]![generateContainerRowKey(indicatorCode, 1)] = reactive({ rowKey: generateContainerRowKey(indicatorCode, 1) });
            }
          }
        }
      }
    }

    // 3.5 初始化自动条目容器（根据关联指标值生成条目）
    initializeAutoEntryContainers();

    // 3.6 为所有非自动条目容器指标初始化空值（至少保证有一行）
    for (const ind of indicatorData) {
      if (ind.valueType === 12) {
        const link = getAutoEntryLink(ind.valueOptions);
        // 自动条目容器：由 initializeAutoEntryContainers() 控制，不覆盖
        if (link) continue;
      }
      if (ind.valueType === 12 && !containerValues[ind.indicatorCode]) {
        const containerType = getContainerType(ind.valueOptions);
        if (containerType === 'conditional') {
          // 条件容器：rowKey 直接使用 indicatorCode
          const rowKey = generateConditionalRowKey(ind.indicatorCode);
          containerValues[ind.indicatorCode] = {};
          containerValues[ind.indicatorCode]![rowKey] = reactive({ rowKey });
        } else {
          // 普通容器：使用带序号的 rowKey
          const rowKey = generateContainerRowKey(ind.indicatorCode, 1);
          containerValues[ind.indicatorCode] = {};
          containerValues[ind.indicatorCode]![rowKey] = reactive({ rowKey });
        }
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

    // 6. 初始化逻辑校验
    validateLogicRules(indicators.value);
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
        if (!ind) {
          // 指标已从系统删除，忽略该孤立值记录（后端保留审计数据，不级联删除）
          console.warn(`[IndicatorInputTable] 指标 ${record.indicatorId} (code=${record.indicatorCode}) 已不存在，跳过加载`);
          continue;
        }
        const vt = record.valueType ?? ind.valueType ?? 1;
        const value = extractValue(record, vt);
        formValues[record.indicatorCode!] = value;

        if (vt === 9 && value && record.indicatorCode) {
          fileListMap[record.indicatorCode] = parseStoredFileList(value);
        }
        if (vt === 12 && record.indicatorCode) {
          const containerType = getContainerType(ind.valueOptions);
          const raw = value;
          const indicatorCode = record.indicatorCode;
          containerValues[indicatorCode] = {};
          if (Array.isArray(raw)) {
            raw.forEach((item: any, idx: number) => {
              const rowKey = item.rowKey || migrateRowKeyToNewFormat(indicatorCode, idx);
              const entryWithFullKey = migrateContainerEntryToFullKey(item, ind.valueOptions, indicatorCode, rowKey);
              const dates = convertContainerEntryDates(ind.valueOptions, entryWithFullKey, indicatorCode, rowKey);
              const entryData = { rowKey, ...item, ...dates };
              containerValues[indicatorCode]![rowKey] = reactive(entryData);
            });
          } else if (raw && typeof raw === 'object') {
            if (containerType === 'conditional') {
              const rowKey = generateConditionalRowKey(indicatorCode);
              const entryWithFullKey = migrateContainerEntryToFullKey(raw, ind.valueOptions, indicatorCode, rowKey);
              const dates = convertContainerEntryDates(ind.valueOptions, entryWithFullKey, indicatorCode, rowKey);
              containerValues[indicatorCode]![rowKey] = reactive({ rowKey, ...raw, ...dates });
            } else {
              const rowKey = generateContainerRowKey(indicatorCode, 1);
              const entryWithFullKey = migrateContainerEntryToFullKey(raw, ind.valueOptions, indicatorCode, rowKey);
              const dates = convertContainerEntryDates(ind.valueOptions, entryWithFullKey, indicatorCode, rowKey);
              containerValues[indicatorCode]![rowKey] = reactive({ rowKey, ...raw, ...dates });
            }
          } else {
            if (containerType === 'conditional') {
              containerValues[indicatorCode]![generateConditionalRowKey(indicatorCode)] = reactive({ rowKey: generateConditionalRowKey(indicatorCode) });
            } else {
              containerValues[indicatorCode]![generateContainerRowKey(indicatorCode, 1)] = reactive({ rowKey: generateContainerRowKey(indicatorCode, 1) });
            }
          }
        }
      }
    }

    // 3.5 初始化自动条目容器（根据关联指标值生成条目）
    initializeAutoEntryContainers();

    // 3.6 为所有非自动条目容器指标初始化空值（至少保证有一行）
    for (const ind of indicatorData) {
      if (ind.valueType === 12) {
        const link = getAutoEntryLink(ind.valueOptions);
        // 自动条目容器：由 initializeAutoEntryContainers() 控制，不覆盖
        if (link) continue;
      }
      if (ind.valueType === 12 && !containerValues[ind.indicatorCode]) {
        const containerType = getContainerType(ind.valueOptions);
        if (containerType === 'conditional') {
          // 条件容器：rowKey 直接使用 indicatorCode
          const rowKey = generateConditionalRowKey(ind.indicatorCode);
          containerValues[ind.indicatorCode] = {};
          containerValues[ind.indicatorCode]![rowKey] = reactive({ rowKey });
        } else {
          // 普通容器：使用带序号的 rowKey
          const rowKey = generateContainerRowKey(ind.indicatorCode, 1);
          containerValues[ind.indicatorCode] = {};
          containerValues[ind.indicatorCode]![rowKey] = reactive({ rowKey });
        }
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

    // 7. 初始化逻辑校验
    validateLogicRules(indicators.value);
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

/** 监听 formValues 变化，同步关联的自动条目容器 */
watch(
  () => formValues,
  () => {
    // 遍历所有自动条目容器，检查其关联指标是否变化
    for (const indicator of indicators.value) {
      if (indicator.valueType !== 12) continue;
      const link = getAutoEntryLink(indicator.valueOptions);
      if (!link) continue;
      const linkValue = formValues[link];
      if (linkValue !== undefined) {
        syncAutoEntryContainerCount(indicator.indicatorCode, link);
      }
    }
  },
  { deep: true },
);

/** 获取所有动态容器值（供父组件保存时调用） */
function getContainerValues(): Record<string, string> {
  const result: Record<string, string> = {};
  for (const ind of indicators.value) {
    if (ind.valueType === 12) {
      result[ind.indicatorCode] = JSON.stringify(Object.values(containerValues[ind.indicatorCode] || {}));
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
      item.valueStr = JSON.stringify(Object.values(containerValues[code] || {}));
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

/** 同步动态容器值到 formValues（直接使用模块级 formValues reactive） */
function syncContainerValuesToForm() {
  for (const ind of indicators.value) {
    if (ind.valueType === 12) {
      formValues[ind.indicatorCode] = JSON.stringify(Object.values(containerValues[ind.indicatorCode] || {}));
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
  display: flex;
  align-items: flex-start;
  gap: 0;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 10px;
  padding: 14px 16px;
  transition: border-color 0.2s, box-shadow 0.2s;
  position: relative;
  overflow: visible;
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

.indicator-last-value {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-left: 1px solid hsl(var(--border));
  padding-left: 16px;
  gap: 4px;
}

/* 仅开关行：主区域按内容宽度，不占满整行 */
.indicator-main--inline {
  flex: none;
  align-self: center;
  width: 100%;
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

/* 开关同行显示，不撑满宽度 */
.input-row--inline {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
}

.input-row--inline :deep(.ant-switch) {
  width: auto !important;
  min-width: 0 !important;
}

/* 去掉 ant-radio-wrapper 默认的 margin-right，改为 gap 间距 */
.input-row--inline :deep(.ant-radio-wrapper),
.input-row--inline :deep(.ant-checkbox-wrapper) {
  margin-inline-end: 0 !important;
  white-space: normal !important;
  word-break: break-word !important;
}

.w-full {
  width: 100%;
}

/* 数字输入框：内容撑开，最大 8 个字符宽度 */
.number-input-auto-width {
  width: auto;
  max-width: 8ch;
  min-width: 160px;
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
  min-width: 160px;
  width: fit-content;
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

.upload-hint {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0;
}

.upload-count {
  color: hsl(var(--primary));
  font-weight: 500;
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
  min-width: 220px;
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

/* 容器内数字输入框也限制最大 8 个字符宽度 */
.entry-fields .number-input-auto-width {
  width: auto !important;
  max-width: 16ch;
  min-width: 160px;
}

.text-red-500 {
  color: hsl(var(--destructive));
  font-weight: 600;
}
</style>

<!-- 全局覆盖 ant checkbox/radio 换行问题（非 scoped 可穿透 CSS-in-JS） -->
<style>
.indicator-input-row.input-row--inline .ant-checkbox-group,
.indicator-input-row.input-row--inline .ant-radio-group {
  display: flex !important;
  flex-wrap: wrap !important;
  width: 100% !important;
  gap: 8px 16px !important;
  line-height: 1.57 !important;
}

.indicator-input-row.input-row--inline .ant-checkbox-group .ant-checkbox-wrapper,
.indicator-input-row.input-row--inline .ant-radio-group .ant-radio-wrapper {
  display: flex !important;
  width: auto !important;
  min-width: 0 !important;
  white-space: normal !important;
  line-height: 1.57 !important;
  font-size: 14px !important;
  list-style: none !important;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol', 'Noto Color Emoji' !important;
}
</style>
