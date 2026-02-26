<script lang="ts" setup>
import type { DeclareFilingApi } from '#/api/declare/filing';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { DeclareIndicatorJointRuleApi } from '#/api/declare/jointRule';

import { computed, reactive, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';
import {
  getIndicatorsByProjectType,
} from '#/api/declare/indicator';
import {
  createFiling,
  getFiling,
  updateFiling,
} from '#/api/declare/filing';
import {
  getIndicatorValues,
  saveIndicatorValues,
} from '#/api/declare/indicatorValue';
import {
  getEnabledJointRules,
} from '#/api/declare/jointRule';
import { validate } from '#/utils/indicatorValidator';
import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import { $t } from '#/locales';

interface IndicatorGroup {
  category: number;
  categoryName: string;
  indicators: DeclareIndicatorApi.Indicator[];
}

// 业务类型枚举
const BUSINESS_TYPE = {
  FILING: 1, // 备案
} as const;

const emit = defineEmits(['success']);

// 当前步骤
const currentStep = ref(1);

// 项目类型
const projectType = ref<number | undefined>(undefined);

// 指标分组数据
const indicatorGroups = ref<IndicatorGroup[]>([]);

// 联合验证规则
const jointRules = ref<DeclareIndicatorJointRuleApi.JointRule[]>([]);

// 指标值存储 - 使用 reactive 对象
const indicatorValuesMap = reactive<Record<number, any>>({});

// 折叠面板展开的key
const activeCollapseKeys = ref<(string | number)[]>([]);

// 表单ref
const formRef = ref<any>(null);

// 判断指标是否有口径
function hasIndicatorSpec(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(
    indicator.definition ||
    indicator.statisticScope ||
    indicator.dataSource ||
    indicator.fillRequire ||
    indicator.calculationExample
  );
}

const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['项目备案核心信息'])
    : $t('ui.actionTitle.create', ['项目备案核心信息']);
});

// 指标分类字典
const indicatorCategories = computed(() =>
  getDictOptions(DICT_TYPE.DECLARE_INDICATOR_CATEGORY, 'number')
);

// 获取分类名称
function getCategoryName(category: number) {
  const found = indicatorCategories.value.find(
    (item) => Number(item.value) === category
  );
  return found?.label || `分类${category}`;
}

// 指标值变化时触发验证
function onIndicatorChange(indicatorId: number) {
  // 执行 FILL 类型规则的实时验证
  const errors = validate(jointRules.value, indicatorValuesMap, {
    triggerTiming: 'FILL',
    changedIndicatorId: indicatorId,
  });

  if (errors.length > 0) {
    // 显示第一个错误消息
    message.warning(errors[0].message);
  }
}

// 项目类型改变时加载指标
async function loadIndicators(type: number) {
  if (!type) return;
  try {
    // 并行加载指标和验证规则
    const [indicators, rules] = await Promise.all([
      getIndicatorsByProjectType(type, 'filing'),
      getEnabledJointRules({ projectType: type, triggerTiming: 'FILL' }),
    ]);

    // 保存验证规则
    jointRules.value = rules;
    console.log('加载验证规则:', rules);

    // 按分类分组
    const groups: IndicatorGroup[] = [];
    const categoryMap = new Map<number, IndicatorGroup>();

    indicators.forEach((indicator) => {
      if (!categoryMap.has(indicator.category)) {
        const group: IndicatorGroup = {
          category: indicator.category,
          categoryName: getCategoryName(indicator.category),
          indicators: [],
        };
        categoryMap.set(indicator.category, group);
        groups.push(group);
      }
      categoryMap.get(indicator.category)!.indicators.push(indicator);
    });

    // 按分类排序
    groups.sort((a, b) => a.category - b.category);
    indicatorGroups.value = groups;

    // 默认展开第一个分类
    const firstGroup = groups[0];
    if (firstGroup) {
      activeCollapseKeys.value = [firstGroup.category];
    }

    // 加载已保存的指标值（编辑模式）
    if (formData.value?.id) {
      await loadIndicatorValues();
    }
  } catch (error) {
    console.error('加载指标失败:', error);
    message.error('加载指标失败');
  }
}

// 加载已保存的指标值
async function loadIndicatorValues() {
  if (!formData.value?.id) return;
  try {
    const values = await getIndicatorValues(
      BUSINESS_TYPE.FILING,
      formData.value.id
    );
    // 将值填充到 indicatorValuesMap
    values.forEach((v) => {
      let value: any;
      switch (v.valueType) {
        case 1: // 数字
          value = v.valueNum ? Number(v.valueNum) : undefined;
          break;
        case 2: // 字符串
        case 6: // 单选
        case 7: // 多选
        case 10: // 单选下拉
        case 11: // 多选下拉
          value = v.valueStr || undefined;
          break;
        case 3: // 布尔
          value = v.valueBool;
          break;
        case 4: // 日期
          value = v.valueDate || undefined;
          break;
        case 5: // 长文本
          value = v.valueText || undefined;
          break;
        case 8: // 日期区间
          // 日期区间需要特殊处理
          if (v.valueDateStart || v.valueDateEnd) {
            value = [v.valueDateStart, v.valueDateEnd];
          }
          break;
        default:
          value = v.valueStr || undefined;
      }
      indicatorValuesMap[v.indicatorId] = value;
    });
  } catch (error) {
    console.error('加载指标值失败:', error);
  }
}

// 解析选项
function parseOptions(valueOptions: string): Array<{ label: string; value: string }> {
  if (!valueOptions) return [];
  try {
    return JSON.parse(valueOptions);
  } catch {
    return [];
  }
}

// 初始化表单数据
const step1FormData = ref({
  socialCreditCode: '',
  medicalLicenseNo: '',
  orgName: '',
  projectType: undefined as number | undefined,
  validStartTime: undefined as any,
  validEndTime: undefined as any,
  constructionContent: '',
});

// 表单校验规则
const formRules = {
  socialCreditCode: [
    { required: true, message: '请输入社会信用代码', trigger: 'blur' },
  ],
  medicalLicenseNo: [
    { required: true, message: '请输入执业许可证号', trigger: 'blur' },
  ],
  orgName: [
    { required: true, message: '请输入机构名称', trigger: 'blur' },
  ],
  projectType: [
    { required: true, message: '请选择项目类型', trigger: 'change' },
  ],
  validStartTime: [
    { required: true, message: '请选择有效期开始时间', trigger: 'change' },
  ],
  validEndTime: [
    { required: true, message: '请选择有效期结束时间', trigger: 'change' },
  ],
  constructionContent: [
    { required: true, message: '请输入建设内容', trigger: 'blur' },
  ],
};

// 校验第一步表单
async function validateStep1() {
  try {
    await formRef.value?.validate();
    return true;
  } catch (err) {
    console.error('验证失败:', err);
    return false;
  }
}

// 构建指标值数据
function buildIndicatorValues(
  _businessId: number
): Array<{
  indicatorId: number;
  indicatorCode: string;
  valueType: number;
  valueNum?: string;
  valueStr?: string;
  valueBool?: boolean;
  valueDate?: string;
  valueDateStart?: string;
  valueDateEnd?: string;
  valueText?: string;
}> {
  const result: any[] = [];

  // 遍历所有指标组
  indicatorGroups.value.forEach((group) => {
    group.indicators.forEach((indicator) => {
      const value = indicatorValuesMap[indicator.id!];
      if (value === undefined || value === null || value === '') {
        return; // 跳过空值
      }

      const item: any = {
        indicatorId: indicator.id!,
        indicatorCode: indicator.indicatorCode,
        valueType: indicator.valueType,
      };

      // 根据值类型处理
      switch (indicator.valueType) {
        case 1: // 数字
          item.valueNum = String(value);
          break;
        case 2: // 字符串
        case 6: // 单选
        case 7: // 多选
        case 10: // 单选下拉
        case 11: // 多选下拉
          item.valueStr = String(value);
          break;
        case 3: // 布尔
          item.valueBool = value;
          break;
        case 4: // 日期
          // 处理 dayjs 对象
          if (value && typeof value === 'object' && value.format) {
            item.valueDate = value.format('YYYY-MM-DD HH:mm:ss');
          } else {
            item.valueDate = value;
          }
          break;
        case 5: // 长文本
          item.valueText = String(value);
          break;
        case 8: // 日期区间
          if (Array.isArray(value) && value.length === 2) {
            // 处理 dayjs 对象
            if (value[0] && value[0].format) {
              item.valueDateStart = value[0].format('YYYY-MM-DD HH:mm:ss');
              item.valueDateEnd = value[1].format('YYYY-MM-DD HH:mm:ss');
            } else {
              item.valueDateStart = value[0];
              item.valueDateEnd = value[1];
            }
          }
          break;
      }

      result.push(item);
    });
  });

  return result;
}

// 保存
async function handleSubmit() {
  const valid = await validateStep1();
  if (!valid) {
    message.error('请完善基本信息');
    return;
  }

  // 验证联合规则
  const validationErrors = validate(jointRules.value, indicatorValuesMap, {
    triggerTiming: 'FILL',
  });
  if (validationErrors.length > 0) {
    message.error('验证失败：' + validationErrors.map((e) => e.message).join('；'));
    return;
  }

  modalApi.lock();

  try {
    // 1. 先保存备案基本信息
    const filingData: DeclareFilingApi.Filing = {
      id: formData.value?.id || 0,
      socialCreditCode: step1FormData.value.socialCreditCode || '',
      medicalLicenseNo: step1FormData.value.medicalLicenseNo || '',
      orgName: step1FormData.value.orgName || '',
      projectType: step1FormData.value.projectType || 1,
      validStartTime: step1FormData.value.validStartTime as any,
      validEndTime: step1FormData.value.validEndTime as any,
      constructionContent: step1FormData.value.constructionContent || '',
      filingStatus: 0,
      provinceReviewOpinion: '',
      provinceReviewTime: '' as any,
      provinceReviewerId: 0,
      expertReviewOpinion: '',
      expertReviewerIds: '',
      filingArchiveTime: '' as any,
    };

    let filingId: number;
    if (formData.value?.id) {
      await updateFiling(filingData);
      filingId = formData.value.id;
    } else {
      filingId = await createFiling(filingData);
    }

    // 2. 保存指标值到后端
    const indicatorValues = buildIndicatorValues(filingId);
    if (indicatorValues.length > 0) {
      await saveIndicatorValues({
        businessType: BUSINESS_TYPE.FILING,
        businessId: filingId,
        values: indicatorValues,
      });
    }
    console.log('保存成功，备案ID:', filingId, '指标值:', indicatorValuesMap);

    await modalApi.close();
    emit('success');
    message.success($t('ui.actionMessage.operationSuccess'));
  } catch (error) {
    console.error('保存失败:', error);
    message.error('保存失败');
  } finally {
    modalApi.unlock();
  }
}

// 表单数据（用于编辑模式加载）
const formData = ref<DeclareFilingApi.Filing>();

const [Modal, modalApi] = useVbenModal({
  destroyOnClose: true,
  footer: true,
  // 根据步骤动态设置按钮文字
  confirmText: '下一步',
  showCancelButton: true,
  cancelText: '取消',
  async onConfirm() {
    // 第一步：下一步；第二步：提交
    if (currentStep.value === 1) {
      const valid = await validateStep1();
      if (!valid) {
        message.error('请完善基本信息');
        return false; // 阻止关闭
      }
      // 进入第二步
      currentStep.value = 2;
      // 更新底部按钮文字
      modalApi.setState({
        confirmText: '提交',
        cancelText: '上一步',
      });
      // 加载指标数据
      await loadIndicators(step1FormData.value.projectType!);
      return false; // 阻止关闭，保持弹窗打开
    } else {
      // 第二步：提交
      await handleSubmit();
      return false;
    }
  },
  onCancel() {
    // 上一步
    if (currentStep.value === 2) {
      currentStep.value = 1;
      // 更新底部按钮文字
      modalApi.setState({
        confirmText: '下一步',
        cancelText: '取消',
      });
      return false; // 阻止关闭
    }
    // 取消关闭
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      currentStep.value = 1;
      indicatorGroups.value = [];
      activeCollapseKeys.value = [];
      Object.keys(indicatorValuesMap).forEach(key => delete indicatorValuesMap[Number(key)]);
      // 重置表单
      step1FormData.value = {
        socialCreditCode: '',
        medicalLicenseNo: '',
        orgName: '',
        projectType: undefined,
        validStartTime: undefined,
        validEndTime: undefined,
        constructionContent: '',
      };
      return;
    }

    const data = modalApi.getData<DeclareFilingApi.Filing>();
    if (!data || !data.id) {
      // 新增模式
      return;
    }

    // 编辑模式：加载数据
    modalApi.lock();
    try {
      formData.value = await getFiling(data.id);
      const filing = formData.value!;

      // 填充基本信息
      step1FormData.value = {
        socialCreditCode: filing.socialCreditCode || '',
        medicalLicenseNo: filing.medicalLicenseNo || '',
        orgName: filing.orgName || '',
        projectType: filing.projectType,
        validStartTime: filing.validStartTime,
        validEndTime: filing.validEndTime,
        constructionContent: filing.constructionContent || '',
      };

      // 加载指标数据
      await loadIndicators(step1FormData.value.projectType!);
    } catch (error) {
      console.error('加载数据失败:', error);
      message.error('加载数据失败');
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="p-4">
      <!-- 步骤条 -->
      <div class="mb-6">
        <a-steps :current="currentStep - 1" size="small">
          <a-step title="基本信息" />
          <a-step title="指标填报" />
        </a-steps>
      </div>

      <!-- 第一步：基本信息 -->
      <div v-show="currentStep === 1">
        <a-form
          ref="formRef"
          :model="step1FormData"
          :rules="formRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item
            label="社会信用代码"
            name="socialCreditCode"
          >
            <a-input
              v-model:value="step1FormData.socialCreditCode"
              placeholder="请输入社会信用代码"
            />
          </a-form-item>

          <a-form-item
            label="执业许可证号"
            name="medicalLicenseNo"
          >
            <a-input
              v-model:value="step1FormData.medicalLicenseNo"
              placeholder="请输入执业许可证号"
            />
          </a-form-item>

          <a-form-item label="机构名称" name="orgName">
            <a-input
              v-model:value="step1FormData.orgName"
              placeholder="请输入机构名称"
            />
          </a-form-item>

          <a-form-item label="项目类型" name="projectType">
            <a-select
              v-model:value="step1FormData.projectType"
              placeholder="请选择项目类型"
              @change="(val: number) => (projectType = val)"
            >
              <a-select-option
                v-for="item in getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number')"
                :key="item.value"
                :value="Number(item.value)"
              >
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item
            label="有效期开始时间"
            name="validStartTime"
          >
            <a-date-picker
              v-model:value="step1FormData.validStartTime"
              show-time
              format="YYYY-MM-DD"
              class="w-full"
            />
          </a-form-item>

          <a-form-item label="有效期结束时间" name="validEndTime">
            <a-date-picker
              v-model:value="step1FormData.validEndTime"
              show-time
              format="YYYY-MM-DD"
              class="w-full"
            />
          </a-form-item>

          <a-form-item
            label="建设内容"
            name="constructionContent"
          >
            <a-textarea
              v-model:value="step1FormData.constructionContent"
              placeholder="请输入建设内容"
              :rows="4"
            />
          </a-form-item>
        </a-form>
      </div>

      <!-- 第二步：指标填报 -->
      <div v-show="currentStep === 2">
        <div v-if="indicatorGroups.length === 0" class="py-8 text-center text-gray-500">
          暂无指标数据，请确认项目类型选择正确
        </div>

        <a-collapse v-else v-model:activeKey="activeCollapseKeys">
          <a-collapse-panel
            v-for="group in indicatorGroups"
            :key="group.category"
            :header="`${group.categoryName} (${group.indicators.length}个指标)`"
          >
            <div class="grid grid-cols-2 gap-4">
              <a-form-item
                v-for="indicator in group.indicators"
                :key="indicator.id"
                :required="indicator.isRequired"
                :label-col="{ span: 8 }"
                :wrapper-col="{ span: 16 }"
              >
                <!-- 指标名称 + 问号图标 -->
                <template #label>
                  <div class="flex items-center">
                    {{ indicator.indicatorName }}
                    <!-- 仅当有口径时才显示问号图标 -->
                    <a-popover
                      v-if="hasIndicatorSpec(indicator)"
                      placement="right"
                      trigger="click"
                      :overlayStyle="{ maxWidth: '400px' }"
                    >
                      <template #content>
                        <div class="space-y-3">
                          <!-- 指标定义 -->
                          <div v-if="indicator.definition">
                            <h4 class="font-medium text-gray-900 text-sm mb-1">📌 指标定义</h4>
                            <p class="text-gray-600 text-xs">{{ indicator.definition }}</p>
                          </div>

                          <!-- 统计范围 -->
                          <div v-if="indicator.statisticScope">
                            <h4 class="font-medium text-gray-900 text-sm mb-1">📏 统计范围</h4>
                            <p class="text-gray-600 text-xs">{{ indicator.statisticScope }}</p>
                          </div>

                          <!-- 数据来源 -->
                          <div v-if="indicator.dataSource">
                            <h4 class="font-medium text-gray-900 text-sm mb-1">📁 数据来源</h4>
                            <p class="text-gray-600 text-xs">{{ indicator.dataSource }}</p>
                          </div>

                          <!-- 填报要求 -->
                          <div v-if="indicator.fillRequire">
                            <h4 class="font-medium text-gray-900 text-sm mb-1">📝 填报要求</h4>
                            <p class="text-gray-600 text-xs whitespace-pre-wrap">{{ indicator.fillRequire }}</p>
                          </div>

                          <!-- 计算示例 -->
                          <div v-if="indicator.calculationExample">
                            <h4 class="font-medium text-gray-900 text-sm mb-1">🧮 计算示例</h4>
                            <div class="bg-gray-50 p-2 rounded text-gray-600 text-xs whitespace-pre-wrap">
                              {{ indicator.calculationExample }}
                            </div>
                          </div>
                        </div>
                      </template>
                      <IconifyIcon
                        icon="lucide:help-circle"
                        class="ml-1 cursor-pointer text-blue-500 hover:text-blue-600"
                        style="width: 14px; height: 14px;"
                      />
                    </a-popover>
                  </div>
                </template>
                <!-- 数字类型 -->
                <a-input-number
                  v-if="indicator.valueType === 1"
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  class="w-full"
                  :placeholder="`请输入${indicator.indicatorName}`"
                  :min="indicator.minValue"
                  :max="indicator.maxValue"
                  @change="onIndicatorChange(indicator.id!)"
                >
                  <template #addonAfter v-if="indicator.unit">
                    {{ indicator.unit }}
                  </template>
                </a-input-number>

                <!-- 字符串类型 -->
                <a-input
                  v-else-if="indicator.valueType === 2"
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  :placeholder="`请输入${indicator.indicatorName}`"
                  @change="onIndicatorChange(indicator.id!)"
                />

                <!-- 布尔类型 -->
                <a-switch
                  v-else-if="indicator.valueType === 3"
                  v-model:checked="indicatorValuesMap[indicator.id!]"
                  checked-children="是"
                  un-checked-children="否"
                  @change="onIndicatorChange(indicator.id!)"
                />

                <!-- 日期类型 -->
                <a-date-picker
                  v-else-if="indicator.valueType === 4"
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  show-time
                  format="YYYY-MM-DD"
                  class="w-full"
                  @change="onIndicatorChange(indicator.id!)"
                />

                <!-- 长文本类型 -->
                <a-textarea
                  v-else-if="indicator.valueType === 5"
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  :placeholder="`请输入${indicator.indicatorName}`"
                  :rows="2"
                  @change="onIndicatorChange(indicator.id!)"
                />

                <!-- 单选类型 - 单选框 -->
                <a-radio-group
                  v-else-if="indicator.valueType === 6"
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  @change="onIndicatorChange(indicator.id!)"
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
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  @change="onIndicatorChange(indicator.id!)"
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
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  :placeholder="`请选择${indicator.indicatorName}`"
                  class="w-full"
                  allow-clear
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
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  :placeholder="`请选择${indicator.indicatorName}`"
                  mode="multiple"
                  class="w-full"
                  allow-clear
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
                  v-model:value="indicatorValuesMap[indicator.id!]"
                  show-time
                  format="YYYY-MM-DD"
                  class="w-full"
                />
              </a-form-item>
            </div>
          </a-collapse-panel>
        </a-collapse>
      </div>
    </div>
  </Modal>
</template>
