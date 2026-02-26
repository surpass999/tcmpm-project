<script lang="ts" setup>
import type { DeclareIndicatorApi } from '#/api/declare/indicator';

import { computed, ref, watch } from 'vue';

import { message } from 'ant-design-vue';
import { PlusOutlined } from '@vben/icons';
import { getIndicatorPage } from '#/api/declare/indicator';

// 导入 Ant Design Vue 组件
import { Empty, List } from 'ant-design-vue';

const AEmpty = Empty;
const AList = List;
const AListItem = List.Item;
const AListItemMeta = List.Item.Meta;

import {
  COMPARE_OPERATOR_OPTIONS,
  CONDITION_OPERATOR_OPTIONS,
  FORMULA_OPERATOR_OPTIONS,
  RULE_LEVEL_OPTIONS,
  RULE_TYPE_OPTIONS,
  type RuleGroupConfig,
  type RuleItem,
} from '../types';

// 获取弹窗容器的函数
function getModalContainer() {
  return document.body;
}

// Props
const props = defineProps<{
  modelValue: string; // JSON string of RuleGroupConfig
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
    value: item.id,
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
        pageSize: 20,
      };
      if (keyword) {
        params.indicatorName = keyword;
        params.indicatorCode = keyword;
      }
      const res = await getIndicatorPage(params);
      indicatorOptions.value = res?.list || [];
      hasMore.value = indicatorOptions.value.length >= 20;
    } catch (e) {
      console.error('搜索指标失败', e);
    } finally {
      indicatorLoading.value = false;
    }
  }, 300);
}

// 加载更多指标
async function loadMoreIndicatorOptions() {
  if (indicatorLoading.value || !hasMore.value) return;

  indicatorLoading.value = true;
  try {
    currentPage.value += 1;
    const params: any = {
      pageNo: currentPage.value,
      pageSize: 20,
    };
    if (searchKeyword.value) {
      params.indicatorName = searchKeyword.value;
      params.indicatorCode = searchKeyword.value;
    }
    const res = await getIndicatorPage(params);
    const newList = res?.list || [];
    indicatorOptions.value = [...indicatorOptions.value, ...newList];
    hasMore.value = newList.length >= 20;
  } catch (e) {
    console.error('加载更多指标失败', e);
  } finally {
    indicatorLoading.value = false;
  }
}

// 初始加载指标列表
async function loadIndicatorOptions() {
  indicatorLoading.value = true;
  searchKeyword.value = '';
  currentPage.value = 1;
  hasMore.value = true;
  try {
    const res = await getIndicatorPage({ pageNo: 1, pageSize: 100 });
    indicatorOptions.value = res?.list || [];
    hasMore.value = indicatorOptions.value.length >= 100;

    // 编辑模式下，将已保存的指标 ID 转换为名称
    if (currentRule.value) {
      updateIndicatorNames(currentRule.value);
    }
  } catch (e) {
    console.error('加载指标列表失败', e);
  } finally {
    indicatorLoading.value = false;
  }
}

// 将规则中的指标 ID 转换为名称
function updateIndicatorNames(rule: any) {
  // 更新条件指标名称
  if (rule.condition?.indicatorId && rule.condition.indicatorId > 0) {
    const conditionIndicator = indicatorOptions.value.find(
      (i) => i.id === rule.condition.indicatorId
    );
    if (conditionIndicator) {
      rule.condition.indicatorName = conditionIndicator.indicatorName;
      rule.condition.indicatorCode = conditionIndicator.indicatorCode;
    }
  }

  // 更新公式中的指标名称
  if (rule.action?.formula) {
    rule.action.formula.forEach((item: any) => {
      if (item.valueType === 'indicator' && item.indicatorId) {
        const indicator = indicatorOptions.value.find(
          (i) => i.id === item.indicatorId
        );
        if (indicator) {
          item.indicatorName = indicator.indicatorName;
          item.indicatorCode = indicator.indicatorCode;
        }
      }
    });
  }

  // 更新比较指标名称
  if (rule.action?.compareIndicatorId) {
    const compareIndicator = indicatorOptions.value.find(
      (i) => i.id === rule.action.compareIndicatorId
    );
    if (compareIndicator) {
      rule.action.compareIndicatorName = compareIndicator.indicatorName;
    }
  }
}

// 解析 ruleConfig
const ruleConfig = ref<RuleGroupConfig>({
  rules: [],
});

// 当前编辑的规则索引，-1表示新增模式，>=0表示编辑模式
const currentRuleIndex = ref<number>(-1);

// 弹窗显示状态，添加或编辑时都为true
const isRuleEditorVisible = ref(false);

// 当前编辑的规则
const currentRule = ref<RuleItem>({
  name: '',
  ruleType: 1,
  action: {
    type: 'formula',
    level: 1,
    message: '',
  },
});

// 解析外部传入的值
watch(
  () => props.modelValue,
  (val) => {
    console.log('[RuleConfig] modelValue changed:', val ? val.substring(0, 100) : val);
    if (val) {
      try {
        const parsed = JSON.parse(val);
        console.log('[RuleConfig] parsed ruleConfig:', parsed);
        ruleConfig.value = parsed;
      } catch {
        console.error('[RuleConfig] parse error, using default');
        ruleConfig.value = {
          rules: [],
        };
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

// 监听条件指标变化，自动设置 indicatorName
watch(
  () => currentRule.value?.condition?.indicatorId,
  (val) => {
    if (!currentRule.value.condition) return;
    
    // indicatorId 为 0 表示"无条件"
    if (!val || val === 0) {
      currentRule.value.condition.indicatorName = '';
      currentRule.value.condition.operator = 'not_empty';
      return;
    }
    
    if (selectOptions.value.length) {
      const item = selectOptions.value.find(o => o.value === val);
      if (item) {
        currentRule.value.condition.indicatorName = item.raw.indicatorName;
        // 如果之前没有运算符，给一个默认值
        if (!currentRule.value.condition.operator) {
          currentRule.value.condition.operator = 'not_empty';
        }
      }
    }
  }
);

// 添加规则
function addRule() {
  currentRuleIndex.value = -1;
  isRuleEditorVisible.value = true;
  // 用 indicatorId = 0 表示"无条件"（始终验证）
  currentRule.value = {
    name: '',
    ruleType: 1,
    condition: {
      indicatorId: 0,
      indicatorName: '',
      operator: 'not_empty',
    },
    action: {
      type: 'formula',
      level: 1,
      message: '',
      operator: '>=',
      compareType: 'fixed',
      compareValue: 0,
      compareIndicatorId: undefined,
      compareIndicatorName: '',
      formula: [
        { valueType: 'indicator', indicatorId: undefined, indicatorName: '', indicatorCode: '', fixedValue: undefined, mathOp: '+' },
      ],
    },
  };
  loadIndicatorOptions();
}

// 编辑规则
function editRule(index: number) {
  currentRuleIndex.value = index;
  isRuleEditorVisible.value = true;
  const ruleData = JSON.parse(JSON.stringify(ruleConfig.value.rules[index]));
  
  // 兼容旧数据：转换为新的 formula 格式
  if (ruleData.action.type === 'formula') {
    // 初始化比较值相关字段
    if (!ruleData.action.compareType) {
      if (ruleData.action.right?.type === 'fixed') {
        ruleData.action.compareType = 'fixed';
        ruleData.action.compareValue = ruleData.action.right.value;
      } else if (ruleData.action.right?.items?.length) {
        ruleData.action.compareType = 'indicator';
        ruleData.action.compareIndicatorId = ruleData.action.right.items[0]?.indicatorId;
        ruleData.action.compareIndicatorName = ruleData.action.right.items[0]?.indicatorName;
      } else {
        ruleData.action.compareType = 'fixed';
        ruleData.action.compareValue = 0;
      }
    }
    
    if (!ruleData.action.formula || !ruleData.action.formula.length) {
      // 从旧的 left/right 格式转换
      const items: any[] = [];
      
      // 左侧指标
      if (ruleData.action.left?.items) {
        ruleData.action.left.items.forEach((item: any) => {
          items.push({
            valueType: 'indicator',
            indicatorId: item.indicatorId,
            indicatorName: item.indicatorName,
            indicatorCode: item.indicatorCode,
            fixedValue: undefined,
            mathOp: item.operator || '+',
          });
        });
      }
      
      ruleData.action.formula = items.length > 0 ? items : [
        { valueType: 'indicator', indicatorId: undefined, indicatorName: '', indicatorCode: '', fixedValue: undefined, mathOp: '+' },
      ];
    }
  }
  
  currentRule.value = ruleData;
  loadIndicatorOptions();
}

// 删除规则
function deleteRule(index: number) {
  ruleConfig.value.rules.splice(index, 1);
}

// 添加公式项
function addFormulaItem() {
  if (!currentRule.value.action.formula) {
    currentRule.value.action.formula = [];
  }
  currentRule.value.action.formula.push({
    valueType: 'indicator',
    indicatorId: undefined,
    indicatorName: '',
    indicatorCode: '',
    fixedValue: undefined,
    mathOp: '+',
  });
}

// 删除公式项
function removeFormulaItem(index: number) {
  if (currentRule.value.action.formula) {
    currentRule.value.action.formula.splice(index, 1);
  }
}

// 保存规则
function saveRule() {
  if (!currentRule.value.name) {
    message.error('请输入规则名称');
    return;
  }

  if (!currentRule.value.action.message) {
    message.error('请输入提示信息');
    return;
  }

  // 验证规则配置
  if (currentRule.value.ruleType === 1) {
    // 公式验证 - 计算公式
    const formulaItems = currentRule.value.action.formula || [];
    const hasValidItem = formulaItems.some(item => {
      if (item.valueType === 'indicator') {
        return item.indicatorId && item.indicatorId > 0;
      } else {
        return item.fixedValue !== undefined && item.fixedValue !== null;
      }
    });
    
    if (!hasValidItem) {
      message.error('请至少添加一个指标或固定值');
      return;
    }
    
    if (!currentRule.value.action.operator) {
      message.error('请选择比较运算符');
      return;
    }
    
    // 公式验证 - 比较值
    if (currentRule.value.action.compareType === 'fixed') {
      if (currentRule.value.action.compareValue === undefined || currentRule.value.action.compareValue === null) {
        message.error('请输入比较值');
        return;
      }
    } else {
      if (!currentRule.value.action.compareIndicatorId) {
        message.error('请选择比较指标');
      return;
      }
    }
  } else if (currentRule.value.ruleType === 2) {
    // 必填验证
    if (!currentRule.value.condition?.indicatorId) {
      message.error('请选择条件指标');
      return;
    }
    if (!currentRule.value.action.targetIndicatorId) {
      message.error('请选择目标指标');
      return;
    }
  } else if (currentRule.value.ruleType === 3) {
    // 互斥验证
    if (!currentRule.value.condition?.indicatorId) {
      message.error('请选择第一个指标');
      return;
    }
    if (!currentRule.value.action.targetIndicatorId) {
      message.error('请选择第二个指标');
      return;
    }
  }

  // 如果没有设置触发条件（indicatorId 为 0），则删除 condition
  const ruleToSave = { ...currentRule.value };
  if (ruleToSave.condition?.indicatorId === 0) {
    delete ruleToSave.condition;
  }

  if (currentRuleIndex.value === -1) {
    ruleConfig.value.rules.push(ruleToSave);
  } else {
    ruleConfig.value.rules[currentRuleIndex.value] = ruleToSave;
  }

  currentRuleIndex.value = -1;
  isRuleEditorVisible.value = false;
  message.success('规则保存成功');
}

// 取消编辑
function cancelEdit() {
  currentRuleIndex.value = -1;
  isRuleEditorVisible.value = false;
}

// 关闭弹窗
function closeEditor() {
  currentRuleIndex.value = -1;
  isRuleEditorVisible.value = false;
}

// 获取指标名称
function getIndicatorName(indicatorId: number): string {
  const indicator = indicatorOptions.value.find((i) => i.id === indicatorId);
  return indicator ? `${indicator.indicatorCode} - ${indicator.indicatorName}` : `指标${indicatorId}`;
}

// 获取规则类型名称
function getRuleTypeName(type: number): string {
  const option = RULE_TYPE_OPTIONS.find((o) => o.value === type);
  return option?.label || '未知';
}

// 初始化
loadIndicatorOptions();
</script>

<template>
  <div class="rule-config-container">
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
        <a-button type="primary" size="small" @click="addRule">添加规则</a-button>
      </template>

      <a-empty v-if="ruleConfig.rules.length === 0" description="暂无规则，请点击添加规则" />

      <a-list v-else size="small" bordered :data-source="ruleConfig.rules">
        <template #renderItem="{ item, index }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <span class="font-medium">{{ item.name }}</span>
                <a-tag :color="item.ruleType === 1 ? 'blue' : item.ruleType === 2 ? 'green' : 'orange'" class="ml-2">
                  {{ getRuleTypeName(item.ruleType) }}
                </a-tag>
              </template>
              <template #description>
                <div class="text-xs text-gray-500">
                  提示：{{ item.action.message || '未配置' }}
                </div>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-button type="link" size="small" @click="editRule(index)">编辑</a-button>
              <a-button type="link" size="small" danger @click="deleteRule(index)">删除</a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </a-card>

    <!-- 规则编辑弹窗 -->
    <a-modal
      v-model:open="isRuleEditorVisible"
      :title="currentRuleIndex === -1 ? '添加规则' : '编辑规则'"
      width="800px"
      :get-container="getModalContainer"
      :z-index="2000"
      destroy-on-close
      @ok="saveRule"
      @cancel="closeEditor"
    >
      <a-form layout="vertical">
        <!-- 基本信息 -->
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="规则名称" required>
              <a-input v-model:value="currentRule.name" placeholder="如：床位数与病房数关系验证" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="规则类型" required>
              <a-select v-model:value="currentRule.ruleType" placeholder="请选择规则类型">
                <a-select-option v-for="opt in RULE_TYPE_OPTIONS" :key="opt.value" :value="opt.value">
                  <div>
                    <div>{{ opt.label }}</div>
                    <div class="text-xs text-gray-400">{{ opt.description }}</div>
                  </div>
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="验证方式" required>
              <a-select v-model:value="currentRule.action.level">
                <a-select-option v-for="opt in RULE_LEVEL_OPTIONS" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 条件配置（可选） -->
        <a-divider>触发条件（可选）</a-divider>
        <a-alert
          type="info"
          class="mb-4"
          message="不设置触发条件时，规则将始终生效；设置后，只有当条件满足时才验证"
        />
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="条件指标">
              <a-select
                v-model:value="currentRule!.condition!.indicatorId"
                placeholder="选择指标（如：床位数）"
                :loading="indicatorLoading"
                show-search
                :filter-option="false"
                :options="selectOptions"
                @search="handleIndicatorSearch"
                allow-clear
              >
                <a-select-option v-for="opt in selectOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="运算符">
              <a-select
                v-model:value="currentRule!.condition!.operator"
                :disabled="!currentRule!.condition!.indicatorId"
                placeholder="选择条件"
              >
                <a-select-option v-for="opt in CONDITION_OPERATOR_OPTIONS" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="比较值">
              <a-input
                v-model:value="currentRule!.condition!.value"
                :disabled="!currentRule!.condition!.indicatorId"
                placeholder="可选"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 公式验证配置 -->
        <template v-if="currentRule.ruleType === 1">
          <a-divider>公式配置</a-divider>
          <a-alert type="info" class="mb-4" message="配置验证规则：指标/固定值 经过数学运算 后，与 比较值 进行比较" />
          
          <!-- 公式表达式列表 -->
          <a-form-item label="计算公式">
            <div class="space-y-3">
              <div 
                v-for="(item, idx) in currentRule.action.formula" 
                :key="idx"
                class="ant-row flex items-center gap-2"
              >
                <!-- 指标/固定值选择 -->
                <a-select
                  v-model:value="item.valueType"
                  style="width: 90px; min-height: 32px;"
                  size=""
                >
                  <a-select-option value="indicator">指标</a-select-option>
                  <a-select-option value="fixed">固定值</a-select-option>
                </a-select>
                
                <!-- 指标选择或固定值输入 -->
                  <a-select
                  v-if="item.valueType === 'indicator'"
                  v-model:value="item.indicatorId"
                    placeholder="选择指标"
                    :loading="indicatorLoading"
                    show-search
                    :filter-option="false"
                    :options="selectOptions"
                    @search="handleIndicatorSearch"
                  style="width: 200px"
                  size=""
                    @change="(val: any) => {
                    const opt = selectOptions.find(o => o.value === val);
                    if (opt) {
                      item.indicatorId = val;
                      item.indicatorName = opt.raw.indicatorName;
                      item.indicatorCode = opt.raw.indicatorCode;
                      }
                    }"
                  />
                <a-input-number
                  v-else
                  v-model:value="item.fixedValue"
                  placeholder="固定值"
                  style="width: 150px"
                  size=""
                />
                
                <!-- 数学运算符 -->
                <a-select
                  v-model:value="item.mathOp"
                  style="width: 70px"
                  size="middle"
                >
                  <a-select-option value="+">+</a-select-option>
                  <a-select-option value="-">-</a-select-option>
                  <a-select-option value="*">×</a-select-option>
                  <a-select-option value="/">÷</a-select-option>
                </a-select>
                
                <!-- 删除按钮 -->
                <a-button 
                  size="middle" 
                  @click="removeFormulaItem(idx)"
                  class="shadow-sm"
                >
                  ×
                </a-button>
              </div>
              
              <a-button type="dashed" block @click="addFormulaItem()">
                <template #icon><PlusOutlined /></template>
                添加
              </a-button>
                </div>
              </a-form-item>

          <!-- 比较运算符和比较值 -->
          <a-row :gutter="16" class="mt-4">
            <a-col :span="8">
              <a-form-item label="比较运算符">
                <a-select v-model:value="currentRule.action.operator" placeholder="选择运算符">
                  <a-select-option v-for="opt in COMPARE_OPERATOR_OPTIONS" :key="opt.value" :value="opt.value">
                    {{ opt.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="比较值类型">
                <a-select v-model:value="currentRule.action.compareType">
                  <a-select-option value="fixed">固定值</a-select-option>
                  <a-select-option value="indicator">指标</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="比较值">
                  <a-input-number
                  v-if="currentRule.action.compareType === 'fixed'"
                  v-model:value="currentRule.action.compareValue"
                  placeholder="输入值"
                  style="width: 100%"
                  />
                  <a-select
                    v-else
                  v-model:value="currentRule.action.compareIndicatorId"
                    placeholder="选择指标"
                    :loading="indicatorLoading"
                    show-search
                    :filter-option="false"
                    :options="selectOptions"
                    @search="handleIndicatorSearch"
                  style="width: 100%"
                    @change="(val: any) => {
                    const opt = selectOptions.find(o => o.value === val);
                    if (opt) {
                      currentRule.action.compareIndicatorId = val;
                      currentRule.action.compareIndicatorName = opt.raw.indicatorName;
                      }
                    }"
                  />
              </a-form-item>
            </a-col>
          </a-row>
        </template>

        <!-- 必填验证配置 -->
        <template v-if="currentRule.ruleType === 2">
          <a-divider>必填验证配置</a-divider>
          <a-alert type="info" class="mb-4">
            当条件指标有值时，目标指标变为必填项
          </a-alert>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="目标指标（必填）">
                <a-select
                  v-model:value="currentRule.action.targetIndicatorId"
                  placeholder="选择目标指标"
                  :loading="indicatorLoading"
                  show-search
                  :filter-option="false"
                  :options="selectOptions"
                  @search="handleIndicatorSearch"
                  @change="(val: any) => {
                    if (val) {
                      const item = selectOptions.find(o => o.value === val);
                      if (item) {
                        currentRule.action.targetIndicatorName = item.raw.indicatorName;
                      }
                    }
                  }"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </template>

        <!-- 互斥验证配置 -->
        <template v-if="currentRule.ruleType === 3">
          <a-divider>互斥验证配置</a-divider>
          <a-alert type="info" class="mb-4">
            两个指标不能同时填写值
          </a-alert>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="指标A">
                <a-select
                  v-model:value="currentRule.condition!.indicatorId"
                  placeholder="选择指标A"
                  :loading="indicatorLoading"
                  show-search
                  :filter-option="false"
                  :options="selectOptions"
                  @search="handleIndicatorSearch"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="指标B">
                <a-select
                  v-model:value="currentRule.action.targetIndicatorId"
                  placeholder="选择指标B"
                  :loading="indicatorLoading"
                  show-search
                  :filter-option="false"
                  :options="selectOptions"
                  @search="handleIndicatorSearch"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </template>

        <!-- 提示信息 -->
        <a-divider>提示信息</a-divider>
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="验证失败提示" required>
              <a-textarea
                v-model:value="currentRule.action.message"
                placeholder="如：床位数必须大于等于病房数×3"
                :rows="2"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.rule-config-container {
  width: 100%;
}
</style>
