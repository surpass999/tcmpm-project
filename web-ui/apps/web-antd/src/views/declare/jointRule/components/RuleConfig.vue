<script lang="ts" setup>
import type { DeclareIndicatorApi } from '#/api/declare/indicator';

import { computed, ref, watch } from 'vue';

import { message } from 'ant-design-vue';
import { Empty } from 'ant-design-vue';
import { PlusOutlined, DeleteOutlined, DragOutlined } from '@vben/icons';
import { getIndicatorPage } from '#/api/declare/indicator';

import {
  VALUE_TYPE_OPTIONS,
  COMPARE_MODE_OPTIONS,
  MULTI_SELECT_COMPARE_TYPE_OPTIONS,
  type PositiveRuleItem,
  type PositiveRuleConfig,
  type PositiveRuleOption,
  ValueType,
  CompareMode,
  MultiSelectCompareType,
} from '../types';

// Props
const props = defineProps<{
  modelValue: string;
  projectType?: number;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

// 指标选项
const indicatorOptions = ref<DeclareIndicatorApi.Indicator[]>([]);
const indicatorLoading = ref(false);
const currentPage = ref(1);
const hasMore = ref(true);
const searchKeyword = ref('');
let searchTimer: ReturnType<typeof setTimeout> | null = null;

// 转换 options 格式供 a-select 使用
const selectOptions = computed(() => {
  return indicatorOptions.value.map((item) => ({
    value: item.indicatorCode,
    label: `${item.indicatorCode} - ${item.indicatorName}`,
    raw: item,
  }));
});

// 远程搜索指标
async function handleIndicatorSearch(keyword: string) {
  if (searchTimer) clearTimeout(searchTimer);

  searchTimer = setTimeout(async () => {
    searchKeyword.value = keyword;
    currentPage.value = 1;
    hasMore.value = true;
    indicatorLoading.value = true;
    try {
      const params: any = {
        pageNo: 1,
        pageSize: 200,
      };
      if (keyword) {
        params.indicatorCode = keyword;
      }
      // 按项目类型筛选
      if (props.projectType && props.projectType !== 0) {
        params.projectType = props.projectType;
      }
      const res = await getIndicatorPage(params);
      indicatorOptions.value = res?.list || [];
      hasMore.value = indicatorOptions.value.length >= 200;
    } catch (e) {
      console.error('搜索指标失败', e);
    } finally {
      indicatorLoading.value = false;
    }
  }, 300);
}

// 初始加载指标列表
async function loadIndicatorOptions() {
  indicatorLoading.value = true;
  searchKeyword.value = '';
  currentPage.value = 1;
  hasMore.value = true;
  try {
    const params: any = { pageNo: 1, pageSize: 200 };
    // 按项目类型筛选
    if (props.projectType && props.projectType !== 0) {
      params.projectType = props.projectType;
    }
    const res = await getIndicatorPage(params);
    indicatorOptions.value = res?.list || [];
    hasMore.value = indicatorOptions.value.length >= 200;
  } catch (e) {
    console.error('加载指标列表失败', e);
  } finally {
    indicatorLoading.value = false;
  }
}

// 解析 ruleConfig
const ruleConfig = ref<PositiveRuleConfig>({
  rules: [],
});

// 当前编辑的规则索引
const currentRuleIndex = ref<number>(-1);

// 弹窗显示状态
const isRuleEditorVisible = ref(false);

// 当前编辑的规则
const currentRule = ref<PositiveRuleItem>({
  name: '',
  indicatorCode: '',
  valueType: ValueType.NUMBER,
  compareMode: CompareMode.POSITIVE,
});

// 选项编辑相关
const isOptionEditorVisible = ref(false);
const currentOptionIndex = ref<number>(-1);
const currentOption = ref<PositiveRuleOption>({
  value: '',
  label: '',
});

// 解析外部传入的值
watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      try {
        const parsed = JSON.parse(val);
        ruleConfig.value = parsed;
      } catch {
        ruleConfig.value = { rules: [] };
      }
    }
  },
  { immediate: true }
);

// 监听变化，emit 到父组件
watch(
  ruleConfig,
  (val) => {
    emit('update:modelValue', JSON.stringify(val));
  },
  { deep: true }
);

// 监听指标变化，自动设置规则名称、值类型和选项
watch(
  () => currentRule.value.indicatorCode,
  async (code) => {
    if (code) {
      let indicator = indicatorOptions.value.find((i) => i.indicatorCode === code);

      // 如果在已加载的列表中找不到，通过 API 获取详情
      if (!indicator) {
        try {
          const indicatorList = await getIndicatorPage({
            pageNo: 1,
            pageSize: 1,
            indicatorCode: code,
          });
          if (indicatorList?.list?.length > 0) {
            indicator = indicatorList.list[0];
            const existIdx = indicatorOptions.value.findIndex(
              (i) => i.indicatorCode === code
            );
            if (existIdx >= 0) {
              indicatorOptions.value[existIdx] = indicator;
            } else {
              indicatorOptions.value.push(indicator);
            }
          }
        } catch (e) {
          console.error('获取指标详情失败:', e);
        }
      }

      if (indicator) {
        // 自动设置规则名称
        if (!currentRule.value.name) {
          currentRule.value.name = indicator.indicatorName;
        }
        // 自动设置值类型
        currentRule.value.valueType = indicator.valueType;
        // 自动填充选项（单选/多选）
        if (
          (indicator.valueType === ValueType.RADIO ||
            indicator.valueType === ValueType.MULTI_SELECT) &&
          indicator.valueOptions
        ) {
          try {
            const options = JSON.parse(indicator.valueOptions);
            if (Array.isArray(options) && options.length > 0) {
              currentRule.value.options = options.map((opt: any) => {
                if (typeof opt === 'string') {
                  return { value: opt, label: opt };
                }
                return {
                  value: opt.value || opt,
                  label: opt.label || opt,
                };
              });
              // 单选默认 compareType 为 radio
              if (indicator.valueType === ValueType.RADIO) {
                currentRule.value.compareType = 'radio';
              }
            }
          } catch (e) {
            console.error('解析指标选项失败:', e);
          }
        }
      }
    }
  }
);

// 监听项目类型变化，重新加载指标列表
watch(
  () => props.projectType,
  () => {
    loadIndicatorOptions();
  }
);

// 添加规则
function addRule() {
  currentRuleIndex.value = -1;
  isRuleEditorVisible.value = true;
  currentRule.value = {
    name: '',
    indicatorCode: '',
    valueType: ValueType.NUMBER,
    compareMode: CompareMode.POSITIVE,
    compareType: 'number',
    options: [],
    excludeOptions: [],
    minNewCount: 1,
  };
  loadIndicatorOptions();
}

// 编辑规则
function editRule(index: number) {
  currentRuleIndex.value = index;
  isRuleEditorVisible.value = true;
  const ruleData = JSON.parse(JSON.stringify(ruleConfig.value.rules[index]));
  currentRule.value = ruleData;
  loadIndicatorOptions();
}

// 删除规则
function deleteRule(index: number) {
  ruleConfig.value.rules.splice(index, 1);
  message.success('删除成功');
}

// 保存规则
function saveRule() {
  if (!currentRule.value.indicatorCode) {
    message.error('请选择指标');
    return;
  }

  if (!currentRule.value.name) {
    message.error('请输入规则名称');
    return;
  }

  // 单选/多选必须有选项
  if (
    (currentRule.value.valueType === ValueType.RADIO ||
      currentRule.value.valueType === ValueType.MULTI_SELECT) &&
    (!currentRule.value.options || currentRule.value.options.length === 0)
  ) {
    message.error('单选/多选类型必须配置选项');
    return;
  }

  // 验证选项值唯一
  if (currentRule.value.options) {
    const values = currentRule.value.options.map((o) => o.value);
    if (new Set(values).size !== values.length) {
      message.error('选项值不能重复');
      return;
    }
  }

  const ruleToSave = { ...currentRule.value };

  if (currentRuleIndex.value === -1) {
    ruleConfig.value.rules.push(ruleToSave);
  } else {
    ruleConfig.value.rules[currentRuleIndex.value] = ruleToSave;
  }

  isRuleEditorVisible.value = false;
  message.success('规则保存成功');
}

// 取消编辑
function cancelEdit() {
  isRuleEditorVisible.value = false;
}

// 关闭弹窗
function closeEditor() {
  isRuleEditorVisible.value = false;
}

// 获取值类型名称
function getValueTypeName(valueType: number): string {
  const option = VALUE_TYPE_OPTIONS.find((o) => o.value === valueType);
  return option?.label || '未知';
}

// 获取比较模式名称
function getCompareModeName(compareMode: string): string {
  const option = COMPARE_MODE_OPTIONS.find((o) => o.value === compareMode);
  return option?.label || '未知';
}

// 获取比较类型名称（包含所有类型）
function getCompareTypeName(compareType: string | undefined, valueType?: number): string {
  if (!compareType) return '-';
  // 根据值类型显示不同的名称
  if (valueType === ValueType.NUMBER) {
    return '数值比较';
  }
  if (valueType === ValueType.RADIO) {
    return '单选比较';
  }
  // 多选类型
  const option = MULTI_SELECT_COMPARE_TYPE_OPTIONS.find((o) => o.value === compareType);
  return option?.label || compareType;
}

// 选项编辑
function openOptionEditor(index: number = -1) {
  currentOptionIndex.value = index;
  if (index === -1) {
    currentOption.value = { value: '', label: '' };
  } else {
    currentOption.value = { ...currentRule.value.options![index] };
  }
  isOptionEditorVisible.value = true;
}

function saveOption() {
  if (!currentOption.value.value || !currentOption.value.label) {
    message.error('请输入选项值和标签');
    return;
  }

  if (!currentRule.value.options) {
    currentRule.value.options = [];
  }

  if (currentOptionIndex.value === -1) {
    currentRule.value.options.push({ ...currentOption.value });
  } else {
    currentRule.value.options[currentOptionIndex.value] = { ...currentOption.value };
  }

  isOptionEditorVisible.value = false;
  message.success('选项保存成功');
}

function deleteOption(index: number) {
  if (currentRule.value.options) {
    currentRule.value.options.splice(index, 1);
  }
}

// 拖拽排序相关
const draggedIndex = ref<number | null>(null);

function onDragStart(index: number) {
  draggedIndex.value = index;
}

function onDragOver(e: DragEvent, index: number) {
  e.preventDefault();
}

function onDrop(index: number) {
  if (draggedIndex.value === null || draggedIndex.value === index) {
    return;
  }

  const options = currentRule.value.options;
  if (!options) return;

  const draggedItem = options[draggedIndex.value];
  options.splice(draggedIndex.value, 1);
  options.splice(index, 0, draggedItem);

  draggedIndex.value = null;
}

// 初始化
loadIndicatorOptions();
</script>

<template>
  <div class="positive-rule-config-container">
    <!-- 规则基本信息 -->
    <a-card size="small" class="mb-4">
      <a-form layout="inline">
        <a-form-item label="规则数量">
          <a-tag color="blue">{{ ruleConfig.rules.length }} 条</a-tag>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 规则列表 -->
    <a-card size="small" title="规则列表">
      <template #extra>
        <a-button type="primary" size="small" @click="addRule">
          <template #icon><PlusOutlined /></template>
          添加规则
        </a-button>
      </template>

      <Empty v-if="ruleConfig.rules.length === 0" description="暂无规则，请点击添加规则" />

      <a-table v-else :data-source="ruleConfig.rules" :pagination="false" size="small">
        <a-table-column title="规则名称" data-index="name" key="name" />
        <a-table-column title="指标编码" data-index="indicatorCode" key="indicatorCode" />
        <a-table-column title="值类型" key="valueType">
          <template #default="{ record }">
            <a-tag :color="record.valueType === 1 ? 'blue' : record.valueType === 2 ? 'green' : 'orange'">
              {{ getValueTypeName(record.valueType) }}
            </a-tag>
          </template>
        </a-table-column>
        <a-table-column title="比较模式" key="compareMode">
          <template #default="{ record }">
            {{ getCompareModeName(record.compareMode) }}
          </template>
        </a-table-column>
        <a-table-column title="比较类型" key="compareType">
          <template #default="{ record }">
            {{ getCompareTypeName(record.compareType, record.valueType) }}
          </template>
        </a-table-column>
        <a-table-column title="操作" key="action" width="150">
          <template #default="{ record, index }">
            <a-button type="link" size="small" @click="editRule(index)">编辑</a-button>
            <a-button type="link" size="small" danger @click="deleteRule(index)">删除</a-button>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <!-- 规则编辑弹窗 -->
    <a-modal
      v-model:open="isRuleEditorVisible"
      :title="currentRuleIndex === -1 ? '添加规则' : '编辑规则'"
      width="700px"
      destroy-on-close
      @ok="saveRule"
      @cancel="closeEditor"
    >
      <a-form layout="vertical">
        <!-- 指标选择 -->
        <a-form-item label="选择指标" required>
          <a-select
            v-model:value="currentRule.indicatorCode"
            placeholder="请搜索并选择指标"
            :loading="indicatorLoading"
            show-search
            :filter-option="false"
            :options="selectOptions"
            @search="handleIndicatorSearch"
            style="width: 100%"
          />
        </a-form-item>

        <!-- 规则名称 -->
        <a-form-item label="规则名称" required>
          <a-input v-model:value="currentRule.name" placeholder="如：项目进度上期对比" />
        </a-form-item>

        <!-- 值类型 -->
        <a-form-item label="值类型" required>
          <a-radio-group v-model:value="currentRule.valueType">
            <a-radio-button
              v-for="opt in VALUE_TYPE_OPTIONS"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </a-radio-button>
          </a-radio-group>
          <div class="text-xs text-gray-500 mt-1">
            {{ VALUE_TYPE_OPTIONS.find(o => o.value === currentRule.valueType)?.description }}
          </div>
        </a-form-item>

        <!-- 比较模式（数值和单选始终显示，多选时排除选项数量、新增数量、上期必须保持） -->
        <a-form-item
          v-if="currentRule.valueType !== ValueType.MULTI_SELECT || !['count', 'new_count', 'keep_required'].includes(currentRule.compareType)"
          label="比较模式"
          required
        >
          <a-radio-group v-model:value="currentRule.compareMode">
            <a-radio-button
              v-for="opt in COMPARE_MODE_OPTIONS"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </a-radio-button>
          </a-radio-group>
          <a-alert
            v-if="currentRule.valueType === ValueType.NUMBER"
            :message="currentRule.compareMode === 'positive' ? '数值必须 >= 上期值（只能增大）' : '数值必须 <= 上期值（只能减小）'"
            type="info"
            show-icon
            class="mt-1"
          />
          <a-alert
            v-else-if="currentRule.valueType === ValueType.RADIO"
            :message="currentRule.compareMode === 'positive' ? '所选选项等级必须 >= 上期等级（只能升级，不能降级）' : '所选选项等级必须 <= 上期等级（只能降级，不能升级）'"
            type="info"
            show-icon
            class="mt-1"
          />
          <a-alert
            v-else-if="!['count', 'new_count', 'keep_required'].includes(currentRule.compareType)"
            type="info"
            show-icon
            class="mt-1"
            :message="COMPARE_MODE_OPTIONS.find(o => o.value === currentRule.compareMode)?.description || ''"
          />
        </a-form-item>

        <!-- 比较类型（仅多选显示） -->
        <a-form-item
          v-if="currentRule.valueType === ValueType.MULTI_SELECT"
          label="多选比较类型"
          required
        >
          <a-select
            v-model:value="currentRule.compareType"
            placeholder="请选择比较类型"
            style="width: 100%"
          >
            <a-select-option
              v-for="opt in MULTI_SELECT_COMPARE_TYPE_OPTIONS"
              :key="opt.value"
              :value="opt.value"
            >
              <div>
                <div>{{ opt.label }}</div>
                <div class="text-xs text-gray-400">{{ opt.description }}</div>
              </div>
            </a-select-option>
          </a-select>
        </a-form-item>

        <!-- 选项配置（单选/多选） -->
        <template v-if="currentRule.valueType === ValueType.RADIO || currentRule.valueType === ValueType.MULTI_SELECT">
          <a-divider>选项配置</a-divider>

          <!-- 仅等级相关类型显示提示 -->
          <template v-if="!['count', 'new_count', 'keep_required'].includes(currentRule.compareType)">
            <a-alert
              type="info"
              class="mb-4"
              :message="currentRule.compareMode === 'positive'
                ? '正向模式：拖拽调整选项顺序，列表上方=低等级（只能升级：等级数值只能增大）'
                : '负向模式：拖拽调整选项顺序，列表上方=低等级（只能降级：等级数值只能减小）'"
              show-icon
            />

            <a-alert
              type="warning"
              class="mb-4"
              :message="currentRule.compareMode === 'positive'
                ? '提示：列表上方等级低（如等级1），下方等级高（如等级4）。正向模式本期等级必须 >= 上期等级。'
                : '提示：列表上方等级低（如等级1），下方等级高（如等级4）。负向模式本期等级必须 <= 上期等级。'"
              show-icon
            />
          </template>

          <div class="mb-4">
            <a-table
              :data-source="currentRule.options || []"
              :pagination="false"
              size="small"
              row-key="value"
            >
              <a-table-column title="选项值" data-index="value" />
              <a-table-column title="选项标签" data-index="label" />
              <a-table-column title="等级" width="80">
                <template #default="{ index }">
                  {{ index + 1 }}
                </template>
              </a-table-column>
              <a-table-column title="操作" width="120">
                <template #default="{ record, index }">
                  <a-button type="link" size="small" @click="openOptionEditor(index)">编辑</a-button>
                  <a-button type="link" size="small" danger @click="deleteOption(index)">删除</a-button>
                </template>
              </a-table-column>
            </a-table>

            <!-- 拖拽层 - 仅等级相关类型显示 -->
            <div v-if="!['count', 'new_count', 'keep_required'].includes(currentRule.compareType)" class="mt-2">
              <template v-for="(option, idx) in currentRule.options || []" :key="option.value">
                <div
                  class="drag-handle"
                  draggable="true"
                  @dragstart="onDragStart(idx)"
                  @dragover="(e) => onDragOver(e, idx)"
                  @drop="onDrop(idx)"
                  @dragend="draggedIndex = null"
                >
                  <span class="mr-2"><DragOutlined /></span>
                  <span>{{ option.label }}</span>
                </div>
              </template>
            </div>

            <a-button type="dashed" block class="mt-2" @click="openOptionEditor(-1)">
              <template #icon><PlusOutlined /></template>
              添加选项
            </a-button>
          </div>
        </template>

        <!-- 排除选项（多选） -->
        <template v-if="currentRule.valueType === ValueType.MULTI_SELECT">
          <a-divider>排除选项</a-divider>
          <a-alert
            type="info"
            class="mb-4"
            message="被排除的选项不参与比较校验"
          />
          <a-select
            v-model:value="currentRule.excludeOptions"
            mode="multiple"
            placeholder="选择不参与比较的选项"
            style="width: 100%"
          >
            <a-select-option
              v-for="opt in currentRule.options"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </a-select-option>
          </a-select>
        </template>

        <!-- 最小新增数量（new_count） -->
        <template v-if="currentRule.compareType === MultiSelectCompareType.NEW_COUNT">
          <a-divider>新增数量配置</a-divider>
          <a-form-item label="最小新增数量">
            <a-input-number
              v-model:value="currentRule.minNewCount"
              :min="1"
              :max="100"
            />
          </a-form-item>
        </template>
      </a-form>
    </a-modal>

    <!-- 选项编辑弹窗 -->
    <a-modal
      v-model:open="isOptionEditorVisible"
      :title="currentOptionIndex === -1 ? '添加选项' : '编辑选项'"
      width="400px"
      destroy-on-close
      @ok="saveOption"
      @cancel="isOptionEditorVisible = false"
    >
      <a-form layout="vertical">
        <a-form-item label="选项值" required>
          <a-input v-model:value="currentOption.value" placeholder="如：level_1" />
          <div class="text-xs text-gray-500 mt-1">选项的唯一标识，用于比较</div>
        </a-form-item>
        <a-form-item label="选项标签" required>
          <a-input v-model:value="currentOption.label" placeholder="如：启动阶段" />
          <div class="text-xs text-gray-500 mt-1">显示给用户的文本</div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.positive-rule-config-container {
  width: 100%;
}

.cursor-move {
  cursor: move;
}

.drag-handle {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  margin-bottom: 4px;
  background: #f5f5f5;
  border-radius: 4px;
  cursor: move;
  user-select: none;
  transition: background-color 0.2s;
}

.drag-handle:hover {
  background: #e6e6e6;
}

.drag-handle:active {
  background: #d9d9d9;
}
</style>
