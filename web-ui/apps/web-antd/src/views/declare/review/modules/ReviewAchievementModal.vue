<script lang="ts" setup>
import type { DeclareReviewApi } from '#/api/declare/review';
import type { DeclareAchievementApi } from '#/api/declare/achievement';

import { ref, computed } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Button, Card, Descriptions, DescriptionsItem, Divider, Spin, Tabs, Tag } from 'ant-design-vue';
import { message } from 'ant-design-vue';

import {
  applyAvoidReview,
  getReviewResultDetail,
  submitReview,
} from '#/api/declare/review';
import { getTaskBatchStatusByProcessInstanceIds } from '#/api/bpm/task';

import { getAchievement } from '#/api/declare/achievement';

// 评审结论选项
const conclusionOptions = [
  { label: '通过', value: '通过' },
  { label: '需整改', value: '需整改' },
  { label: '未通过', value: '未通过' },
];

// 成果类型
const ACHIEVEMENT_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '系统功能', color: '#4A8F72' },
  2: { label: '数据集', color: '#1890ff' },
  3: { label: '科研成果', color: '#722ed1' },
  4: { label: '管理经验', color: '#fa8c16' },
};

// 流通类型
const FLOW_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '内部使用', color: 'blue' },
  2: { label: '对外共享', color: 'green' },
  3: { label: '交易', color: 'orange' },
};

// 可复制性
const REPLICATION_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '高', color: 'green' },
  2: { label: '中', color: 'orange' },
  3: { label: '低', color: 'red' },
};

// 推广范围
const PROMOTION_SCOPE_MAP: Record<number, string> = {
  1: '院内',
  2: '省级',
  3: '全国',
};

// 数据质量
const DATA_QUALITY_MAP: Record<number, string> = {
  1: '优',
  2: '良',
  3: '中',
  4: '差',
};

// 转化类型
const TRANSFORM_TYPE_MAP: Record<number, string> = {
  1: '标准规范',
  2: '创新模式',
  3: '典型案例',
};

const emit = defineEmits(['success']);

const [Modal, modalApi] = useVbenModal({
  onOpenChange: async (isOpen: boolean) => {
    if (!isOpen) {
      reviewResult.value = null;
      achievementData.value = null;
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
});

const loading = ref(false);
const submitting = ref(false);
const currentResultId = ref(0);
const reviewResult = ref<DeclareReviewApi.ReviewResult | null>(null);
const achievementData = ref<DeclareAchievementApi.Achievement | null>(null);
const activeTab = ref('basic');

const formData = ref({
  opinion: '',
  conclusion: '',
});

// 按钮配置
const buttonSettings = ref<Record<string, any>>({});
const currentButtonId = ref<number | undefined>(undefined);

const totalScore = computed(() => {
  return 0;
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

    // 加载成果信息
    if (result?.businessId) {
      try {
        achievementData.value = await getAchievement(result.businessId);
      } catch (e) {
        console.warn('加载成果信息失败:', e);
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

function formatDateTime(value: string | Date | undefined | null) {
  if (!value) return '-';
  // 如果是 Date 对象
  if (value instanceof Date) {
    return value.toLocaleString('zh-CN');
  }
  // 如果是时间戳（数字）
  if (typeof value === 'number') {
    return new Date(value).toLocaleString('zh-CN');
  }
  // 如果是字符串
  if (typeof value === 'string') {
    if (value.includes('T')) {
      return value.substring(0, 19).replace('T', ' ');
    }
    return value.substring(0, 19);
  }
  return String(value);
}

function formatDate(value: string | Date | undefined | null) {
  if (!value) return '-';
  // 如果是 Date 对象
  if (value instanceof Date) {
    return value.toLocaleDateString('zh-CN');
  }
  // 如果是时间戳（数字）
  if (typeof value === 'number') {
    return new Date(value).toLocaleDateString('zh-CN');
  }
  // 如果是字符串
  if (typeof value === 'string' && !value.includes('T')) {
    return value.substring(0, 10);
  }
  try {
    return value.substring(0, 10);
  } catch {
    return String(value);
  }
}

function hasContent(text: string | undefined | null): boolean {
  return !!text && text.trim().length > 0;
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
    title="成果审核评审"
    :show-cancel-button="true"
    :show-confirm-button="true"
    :confirm-loading="submitting"
    cancel-text="关闭"
    confirm-text="提交评审"
    class="!min-w-[90%] !max-w-[1200px]"
    @confirm="handleSubmit"
  >
    <Spin :spinning="loading">
      <div v-if="reviewResult">
        <!-- 任务状态提示 -->
        <Card :bordered="false" class="mb-4">
          <a-alert
            v-if="reviewResult.status === 1"
            message="任务已接收"
            description="请查阅成果信息后完成评审意见和结论。"
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
        <div class="mb-4 text-center">
          <Button danger size="small" @click="handleAvoid">
            申请回避
          </Button>
        </div>

        <!-- 成果信息 -->
        <Card :bordered="false" class="mb-4">
          <template #title>
            <span class="flex items-center gap-2">
              <span class="achievement-type-badge" :style="{ background: ACHIEVEMENT_TYPE_MAP[achievementData?.achievementType || 0]?.color + '20', color: ACHIEVEMENT_TYPE_MAP[achievementData?.achievementType || 0]?.color }">
                {{ ACHIEVEMENT_TYPE_MAP[achievementData?.achievementType || 0]?.label || '-' }}
              </span>
              {{ achievementData?.achievementName || '成果详情' }}
            </span>
          </template>

          <Descriptions :column="2" bordered size="small">
            <Descriptions.Item label="应用领域">
              {{ achievementData?.applicationField || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="可复制性">
              <Tag
                v-if="achievementData?.replicationValue"
                :color="REPLICATION_MAP[achievementData.replicationValue]?.color"
                style="border-radius: 4px;"
              >
                {{ REPLICATION_MAP[achievementData.replicationValue]?.label }}
              </Tag>
              <span v-else>-</span>
            </Descriptions.Item>
            <Descriptions.Item label="推广范围">
              {{ achievementData?.promotionScope ? PROMOTION_SCOPE_MAP[achievementData.promotionScope] || '-' : '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="转化类型">
              {{ achievementData?.transformType ? TRANSFORM_TYPE_MAP[achievementData.transformType] || '-' : '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="关联项目">
              <span class="text-primary">{{ achievementData?.projectName || '-' }}</span>
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {{ formatDateTime(achievementData?.createTime) }}
            </Descriptions.Item>
            <Descriptions.Item label="成果描述" :span="2">
              <div class="field-content">
                <span v-if="hasContent(achievementData?.description)">{{ achievementData?.description }}</span>
                <span v-else class="text-gray-400">暂无</span>
              </div>
            </Descriptions.Item>
            <Descriptions.Item label="应用效果" :span="2">
              <div class="field-content">
                <span v-if="hasContent(achievementData?.effectDescription)">{{ achievementData?.effectDescription }}</span>
                <span v-else class="text-gray-400">暂无</span>
              </div>
            </Descriptions.Item>
          </Descriptions>
        </Card>

        <!-- 数据集信息 -->
        <Card v-if="achievementData?.dataName || achievementData?.dataType" title="数据集信息" :bordered="false" class="mb-4">
          <Descriptions :column="2" bordered size="small">
            <Descriptions.Item label="数据名称">
              {{ achievementData?.dataName || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="数据类型">
              {{ achievementData?.dataType || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="数据来源">
              {{ achievementData?.dataSource || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="数据规模">
              {{ achievementData?.dataVolume || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="数据质量">
              {{ achievementData?.dataQuality ? DATA_QUALITY_MAP[achievementData.dataQuality] || '-' : '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="安全备案编号">
              {{ achievementData?.securityFilingNo || '-' }}
            </Descriptions.Item>
          </Descriptions>
        </Card>

        <!-- 流通信息 -->
        <Card v-if="achievementData?.flowType" title="流通信息" :bordered="false" class="mb-4">
          <Descriptions :column="2" bordered size="small">
            <Descriptions.Item label="流通类型">
              <Tag :color="FLOW_TYPE_MAP[achievementData.flowType]?.color" style="border-radius: 4px;">
                {{ FLOW_TYPE_MAP[achievementData.flowType]?.label }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="流通对象">
              {{ achievementData?.flowObject || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="流通开始时间">
              {{ formatDate(achievementData?.startTime) }}
            </Descriptions.Item>
            <Descriptions.Item label="流通结束时间">
              {{ formatDate(achievementData?.endTime) }}
            </Descriptions.Item>
            <Descriptions.Item label="流通目的" :span="2">
              <div class="field-content">
                <span v-if="hasContent(achievementData?.flowPurpose)">{{ achievementData?.flowPurpose }}</span>
                <span v-else class="text-gray-400">暂无</span>
              </div>
            </Descriptions.Item>
          </Descriptions>
        </Card>

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
.achievement-type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.field-content {
  line-height: 1.8;
  white-space: pre-wrap;
}

.text-primary {
  color: #1890ff;
  font-weight: 500;
}

.text-gray-400 {
  color: #999;
  font-style: italic;
}
</style>
