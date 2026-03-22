<script lang="ts" setup>
import type { DeclareReviewApi } from '#/api/declare/review';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Button, Card, Descriptions, DescriptionsItem, Spin } from 'ant-design-vue';
import { message } from 'ant-design-vue';

import {
  applyAvoidReview,
  getReviewResultDetail,
  submitReview,
} from '#/api/declare/review';
import { getTaskBatchStatusByProcessInstanceIds } from '#/api/bpm/task';

import { getFiling } from '#/api/declare/filing';
import { getIndicatorsByProjectType } from '#/api/declare/indicator';
import { getIndicatorValues } from '#/api/declare/indicatorValue';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';

// 评审结论选项
const conclusionOptions = [
  { label: '通过', value: '通过' },
  { label: '未通过', value: '未通过' },
];
// { label: '需整改', value: '需整改' },

const emit = defineEmits(['success']);

const [Modal, modalApi] = useVbenModal({
  onOpenChange: async (isOpen: boolean) => {
    if (!isOpen) {
      reviewResult.value = null;
      filingData.value = null;
      indicatorGroups.value = [];
      indicatorValuesMap.value = {};
      activeCollapseKeys.value = [];
      formData.value = { opinion: '', conclusion: '' };
      buttonSettings.value = {};
      currentButtonId.value = undefined;
      return;
    }
    const data = modalApi.getData<{ resultId: number }>();
    if (!data?.resultId) return;
    currentResultId.value = data.resultId;
    await loadAllData();
  },
  async onConfirm() {
    await handleSubmit();
  },
});

const loading = ref(false);
const submitting = ref(false);
const currentResultId = ref(0);
const reviewResult = ref<DeclareReviewApi.ReviewResult | null>(null);
const filingData = ref<any>(null);

// 按钮配置
const buttonSettings = ref<Record<string, any>>({});
const currentButtonId = ref<number | undefined>(undefined);

// 指标相关
const indicatorGroups = ref<{
  category: number;
  categoryName: string;
  indicators: DeclareIndicatorApi.Indicator[];
}[]>([]);
const indicatorValuesMap = ref<Record<number, any>>({});
const activeCollapseKeys = ref<(string | number)[]>([]);

const formData = ref({
  opinion: '',
  conclusion: '',
});

const totalScore = computed(() => {
  return formData.value.conclusion === '通过' ? 100 : 0;
});

async function loadAllData() {
  loading.value = true;
  try {
    // 加载评审结果
    const result = await getReviewResultDetail(currentResultId.value);
    reviewResult.value = result;
    if (result?.opinion) {
      formData.value.opinion = result.opinion;
    }
    if (result?.conclusion) {
      formData.value.conclusion = result.conclusion;
    }

    // 加载备案信息
    if (result?.businessId) {
      try {
        filingData.value = await getFiling(result.businessId);
        await loadIndicators();
      } catch (e) {
        console.warn('加载备案信息失败:', e);
      }
    }

    // 通过 processInstanceId 获取按钮配置
    if (result?.processInstanceId) {
      try {
        const taskStatusList = await getTaskBatchStatusByProcessInstanceIds({
          processInstanceIds: [result.processInstanceId],
        });
        const taskStatus = taskStatusList?.[0];
        if (taskStatus?.todoTasks?.[0]?.buttonSettings) {
          buttonSettings.value = taskStatus.todoTasks[0].buttonSettings;
          // 默认选中第一个启用的按钮
          const firstEnabled = Object.entries(buttonSettings.value)
            .find(([key, config]: [string, any]) => config?.enable);
          if (firstEnabled) {
            currentButtonId.value = parseInt(firstEnabled[0], 10);
          }
        }
      } catch (e) {
        console.warn('加载按钮配置失败:', e);
      }
    }
  } finally {
    loading.value = false;
  }
}

async function loadIndicators() {
  if (!filingData.value?.projectType) return;

  try {
    const indicators = await getIndicatorsByProjectType(
      filingData.value.projectType,
      'filing'
    );

    // 按分类分组
    const categoryMap = new Map<number, typeof indicatorGroups.value[0]>();
    indicators.forEach((indicator) => {
      if (!categoryMap.has(indicator.category)) {
        categoryMap.set(indicator.category, {
          category: indicator.category,
          categoryName: getCategoryName(indicator.category),
          indicators: [],
        });
      }
      categoryMap.get(indicator.category)!.indicators.push(indicator);
    });
    indicatorGroups.value = Array.from(categoryMap.values()).sort(
      (a, b) => a.category - b.category
    );

    // 默认展开所有分类
    activeCollapseKeys.value = indicatorGroups.value.map((g) => g.category);

    // 获取指标值
    if (filingData.value?.id) {
      const values = await getIndicatorValues(1, filingData.value.id);
      const map: Record<number, any> = {};
      values.forEach((v) => {
        map[v.indicatorId] = parseIndicatorValue(v);
      });
      indicatorValuesMap.value = map;
    }
  } catch (e) {
    console.error('加载指标失败:', e);
  }
}

function getCategoryName(category: number): string {
  const categoryNames: Record<number, string> = {
    1: '基本信息',
    2: '业务功能',
    3: '技术实现',
    4: '安全防护',
    5: '其他',
  };
  return categoryNames[category] || `分类${category}`;
}

function parseIndicatorValue(item: any): any {
  switch (item.valueType) {
    case 1:
      return item.valueNum;
    case 2:
    case 6:
    case 10:
      return item.valueStr;
    case 7:
    case 11:
      return item.valueStr ? item.valueStr.split(',') : [];
    case 3:
      return item.valueBool;
    case 4:
      return item.valueDate;
    case 5:
      return item.valueText;
    case 8:
      if (item.valueDateStart || item.valueDateEnd) {
        return [item.valueDateStart, item.valueDateEnd];
      }
      return undefined;
    case 9:
      return item.valueStr;
    default:
      return item.valueStr;
  }
}

function parseOptions(valueOptions: string): Array<{ label: string; value: string }> {
  if (!valueOptions) return [];
  try {
    return JSON.parse(valueOptions);
  } catch {
    return [];
  }
}

function formatDate(date: string | Date | undefined | null): string {
  if (!date) return '-';
  // 如果是 Date 对象，转换为字符串
  if (date instanceof Date) {
    return date.toLocaleDateString('zh-CN');
  }
  // 如果是时间戳（数字）
  if (typeof date === 'number') {
    return new Date(date).toLocaleDateString('zh-CN');
  }
  // 如果是字符串但没有 T，直接返回前10位
  if (typeof date === 'string' && !date.includes('T')) {
    return date.substring(0, 10);
  }
  // 如果是 ISO 格式字符串
  try {
    return date.substring(0, 10);
  } catch {
    return String(date);
  }
}

function formatDateTime(date: string | Date | undefined | null): string {
  if (!date) return '-';
  // 如果是 Date 对象，转换为字符串
  if (date instanceof Date) {
    return date.toLocaleString('zh-CN');
  }
  // 如果是时间戳（数字）
  if (typeof date === 'number') {
    return new Date(date).toLocaleString('zh-CN');
  }
  // 如果是字符串
  if (typeof date === 'string') {
    // 尝试处理 ISO 格式
    if (date.includes('T')) {
      return date.substring(0, 19).replace('T', ' ');
    }
    return date.substring(0, 19);
  }
  return String(date);
}

function getProjectTypeText(value: number | undefined): string {
  const projectTypeMap: Record<number, string> = {
    1: '综合型',
    2: '中医电子病历型',
    3: '智慧中药房型',
    4: '名老中医传承型',
    5: '中医临床科研型',
    6: '中医智慧医共体型',
  };
  return projectTypeMap[value || 0] || '-';
}

const handleSubmit = async () => {
  if (!formData.value.conclusion) {
    message.warning('请选择评审结论');
    return;
  }
  if (!formData.value.opinion) {
    message.warning('请填写评审意见');
    return;
  }

  submitting.value = true;
  try {
    await submitReview(currentResultId.value, {
      taskId: reviewResult.value?.taskId || 0,
      totalScore: totalScore.value,
      conclusion: formData.value.conclusion,
      opinion: formData.value.opinion,
      buttonId: currentButtonId.value,
    });
    message.success('提交成功');
    emit('success');
    modalApi.close();
  } catch (e) {
    message.error('提交失败');
  } finally {
    submitting.value = false;
  }
};

const handleAvoid = async () => {
  const reason = window.prompt('请输入回避原因：');
  if (!reason) return;

  try {
    await applyAvoidReview(currentResultId.value, reason);
    message.success('已提交回避申请');
    emit('success');
    modalApi.close();
  } catch (e) {
    message.error('申请失败');
  }
};
</script>

<template>
  <Modal
    title="备案论证评审"
    :show-cancel-button="true"
    :show-confirm-button="true"
    :confirm-loading="submitting"
    cancel-text="关闭"
    confirm-text="提交评审"
    class="!min-w-[90%] !max-w-[1200px]"
  >
    <Spin :spinning="loading">
      <div v-if="reviewResult">
        <!-- 任务状态提示 -->
        <Card :bordered="false" class="mb-4">
          <a-alert
            v-if="reviewResult.status === 1"
            message="任务已接收"
            description="请查阅备案信息后完成评审意见和结论。"
            type="info"
            show-icon
          />
          <a-alert
            v-else-if="reviewResult.status === 2"
            message="评审进行中"
            description="请完成评审意见后提交。"
            type="success"
            show-icon
          />
        </Card>

        <!-- 操作按钮 -->
        <!-- <div class="mb-4 text-center">
          <Button danger size="small" @click="handleAvoid">
            申请回避
          </Button>
        </div> -->

        <!-- 备案信息 -->
        <Card title="备案信息" :bordered="false" class="mb-4">
          <Descriptions :column="2" bordered size="small">
            <Descriptions.Item label="机构名称" :span="2">
              {{ filingData?.orgName || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="统一社会信用代码">
              {{ filingData?.socialCreditCode || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="项目类型">
              {{ getProjectTypeText(filingData?.projectType) }}
            </Descriptions.Item>
            <Descriptions.Item label="执业许可证号">
              {{ filingData?.medicalLicenseNo || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="有效期">
              {{ formatDate(filingData?.validStartTime) }} ~ {{ formatDate(filingData?.validEndTime) }}
            </Descriptions.Item>
            <Descriptions.Item label="建设内容" :span="2">
              {{ filingData?.constructionContent || '-' }}
            </Descriptions.Item>
          </Descriptions>
        </Card>

        <!-- 备案指标信息 -->
        <a-collapse
          v-if="indicatorGroups.length"
          v-model:activeKey="activeCollapseKeys"
          class="mb-4"
        >
          <a-collapse-panel
            v-for="group in indicatorGroups"
            :key="group.category"
            :header="`${group.categoryName} (${group.indicators.length}个指标)`"
          >
            <div class="info-grid">
              <div
                v-for="indicator in group.indicators"
                :key="indicator.id"
                class="info-item"
                :class="{ 'col-span-2': indicator.valueType === 5 }"
              >
                <label class="info-label">{{ indicator.indicatorName }}：</label>
                <div class="info-value-wrap">
                  <!-- 数字类型 -->
                  <template v-if="indicator.valueType === 1">
                    <span class="info-value">
                      {{ indicatorValuesMap[indicator.id] || '-' }}
                    </span>
                    <span v-if="indicator.unit" class="unit">{{ indicator.unit }}</span>
                  </template>

                  <!-- 字符串类型 -->
                  <template v-else-if="indicator.valueType === 2">
                    <span class="info-value">{{ indicatorValuesMap[indicator.id] || '-' }}</span>
                  </template>

                  <!-- 布尔类型 -->
                  <template v-else-if="indicator.valueType === 3">
                    <span
                      class="status-tag"
                      :class="indicatorValuesMap[indicator.id] ? 'status-tag-success' : 'status-tag-pending'"
                    >
                      {{ indicatorValuesMap[indicator.id] ? '是' : '否' }}
                    </span>
                  </template>

                  <!-- 日期类型 -->
                  <template v-else-if="indicator.valueType === 4">
                    <span class="info-value">{{ formatDate(indicatorValuesMap[indicator.id]) || '-' }}</span>
                  </template>

                  <!-- 长文本类型 -->
                  <template v-else-if="indicator.valueType === 5">
                    <span class="info-value long-text">
                      {{ indicatorValuesMap[indicator.id] || '-' }}
                    </span>
                  </template>

                  <!-- 单选类型 -->
                  <template v-else-if="indicator.valueType === 6">
                    <div class="option-list">
                      <span
                        v-for="opt in parseOptions(indicator.valueOptions)"
                        :key="opt.value"
                        class="option-tag"
                        :class="{
                          'option-selected': String(opt.value) === String(indicatorValuesMap[indicator.id])
                        }"
                      >
                        {{ opt.label }}
                      </span>
                    </div>
                  </template>

                  <!-- 多选类型 -->
                  <template v-else-if="indicator.valueType === 7">
                    <div class="option-list">
                      <span
                        v-for="opt in parseOptions(indicator.valueOptions)"
                        :key="opt.value"
                        class="option-tag"
                        :class="{
                          'option-selected': (indicatorValuesMap[indicator.id] || []).includes(opt.value)
                        }"
                      >
                        {{ opt.label }}
                      </span>
                    </div>
                  </template>

                  <!-- 默认显示 -->
                  <template v-else>
                    <span class="info-value">{{ indicatorValuesMap[indicator.id] || '-' }}</span>
                  </template>
                </div>
              </div>
            </div>
          </a-collapse-panel>
        </a-collapse>

        <!-- 无指标时显示提示 -->
        <a-alert
          v-else-if="!loading"
          message="暂无指标数据"
          type="warning"
          class="mb-4"
        />

        <!-- 评审意见 -->
        <Card title="评审意见" :bordered="false">
          <a-form layout="vertical">
            <a-form-item label="评审结论" required>
              <a-select
                v-model:value="formData.conclusion"
                placeholder="请选择评审结论"
              >
                <a-select-option v-for="opt in conclusionOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="评审意见（包含核心结论、具体依据、整改建议）" required>
              <a-textarea
                v-model:value="formData.opinion"
                :rows="6"
                placeholder="请填写评审意见"
              />
            </a-form-item>
          </a-form>
        </Card>
      </div>
    </Spin>
  </Modal>
</template>

<style lang="scss" scoped>
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  padding: 6px 0;

  &.col-span-2 {
    flex-direction: row;
    grid-column: span 2;
  }
}

.info-label {
  flex-shrink: 0;
  width: 150px;
  color: #6C737A;
  font-size: 14px;
}

.info-value {
  flex: 1;
  color: #232931;
  font-size: 14px;
  word-break: break-all;

  &.long-text {
    white-space: pre-wrap;
    background: #fafafa;
    padding: 8px;
    border-radius: 4px;
  }
}

.info-value-wrap {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.status-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
}

.status-tag-success {
  background-color: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.status-tag-pending {
  background-color: #fff7e6;
  color: #fa8c16;
  border: 1px solid #ffd591;
}

.option-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-tag {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
  background-color: #F3F4F6;
  color: #595959;
  border: 1px solid #D4D9DF;

  &.option-selected {
    background-color: #E6EEE8;
    color: #2A5C45;
    font-weight: 500;
  }
}

:deep(.ant-collapse) {
  background: white;
  border: 1px solid #d9d9d9;
  border-radius: 8px;

  .ant-collapse-item {
    border: none;
    margin-bottom: 8px;
    background: white;
    border-radius: 8px;
    overflow: hidden;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .ant-collapse-header {
    background-color: #E6EEE8;
    padding: 12px 16px;
    font-weight: 600;
  }

  .ant-collapse-content-box {
    padding: 16px;
  }
}
</style>
