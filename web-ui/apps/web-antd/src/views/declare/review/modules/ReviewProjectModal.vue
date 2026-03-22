<script lang="ts" setup>
import type { DeclareReviewApi } from '#/api/declare/review';

import { ref, computed, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
  Descriptions,
  DescriptionsItem,
  Divider,
  Form,
  FormItem,
  Input,
  message,
  Radio,
  RadioGroup,
  Rate,
  Row,
  Select,
  SelectOption,
  Space,
  Spin,
  Textarea,
} from 'ant-design-vue';

import {
  applyAvoidReview,
  getReviewResultDetail,
  submitReview,
} from '#/api/declare/review';
import { getTaskBatchStatusByProcessInstanceIds } from '#/api/bpm/task';

import { getProcessIndicatorConfigListByProcessType } from '#/api/declare/process-indicator-config';

// 评分等级常量
const SCORE_LEVEL = {
  SATISFIED: '满足',
  BASIC: '基本满足',
  PARTIAL: '部分满足',
  UNSATISFIED: '不满足',
} as const;

type ScoreLevelKey = keyof typeof SCORE_LEVEL;

interface ScoreOption {
  label: string;
  value: ScoreLevelKey;
  ratio: number;
  score: number;
}

interface IndicatorScoreData {
  indicatorId: number;
  indicatorCode: string;
  indicatorName: string;
  maxScore: number;
  score: number;
  scoreLevel: ScoreLevelKey | undefined;
  scoreRatio: number;
  comment: string;
}

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
      formData.value = { scores: [], conclusion: '', opinion: '' };
      buttonSettings.value = {};
      currentButtonId.value = undefined;
      return;
    }
    const data = modalApi.getData<{ resultId: number }>();
    if (!data?.resultId) return;
    currentResultId.value = data.resultId;
    await loadDetail();
  },
  async onConfirm() {
    await handleSubmit();
  },
});

const loading = ref(false);
const submitting = ref(false);
const currentResultId = ref(0);
const reviewResult = ref<DeclareReviewApi.ReviewResult | null>(null);
const indicatorConfigs = ref<any[]>([]);
const formData = ref({
  scores: [] as IndicatorScoreData[],
  conclusion: '',
  opinion: '',
});

// 按钮配置
const buttonSettings = ref<Record<string, any>>({});
const currentButtonId = ref<number | undefined>(undefined);

const totalScore = computed(() => {
  return formData.value.scores.reduce((sum, item) => sum + item.score, 0);
});

const loadDetail = async () => {
  if (!currentResultId.value) return;
  loading.value = true;
  try {
    const res = await getReviewResultDetail(currentResultId.value);
    reviewResult.value = res;

    if (res.indicatorScores && res.indicatorScores.length > 0) {
      formData.value.scores = res.indicatorScores.map((s: any) => ({
        indicatorId: s.indicatorId,
        indicatorCode: s.indicatorCode || '',
        indicatorName: s.indicatorName || '',
        maxScore: s.maxScore || 0,
        score: s.score || 0,
        scoreLevel: s.scoreLevel as ScoreLevelKey,
        scoreRatio: s.scoreRatio || 0,
        comment: s.comment || '',
      }));
    } else if (res.scoreData) {
      try {
        const parsedScores = JSON.parse(res.scoreData);
        formData.value.scores = parsedScores.map((s: any) => ({
          indicatorId: s.indicatorId,
          indicatorCode: s.indicatorCode || '',
          indicatorName: '',
          maxScore: 10,
          score: s.score || 0,
          scoreLevel: undefined,
          scoreRatio: 0,
          comment: s.comment || '',
        }));
      } catch {
        formData.value.scores = [];
      }
    }
    if (res.conclusion) formData.value.conclusion = res.conclusion;
    if (res.opinion) formData.value.opinion = res.opinion;

    await loadIndicatorConfigs();

    // 通过 processInstanceId 获取按钮配置
    if (res.processInstanceId) {
      try {
        const taskStatusList = await getTaskBatchStatusByProcessInstanceIds({
          processInstanceIds: [res.processInstanceId],
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
};

const loadIndicatorConfigs = async () => {
  if (!reviewResult.value) return;
  try {
    let processType = reviewResult.value.processType;
    if (!processType && reviewResult.value.businessId && reviewResult.value.businessType === 2) {
      const { getProject } = await import('#/api/declare/project');
      try {
        const projectDetail = await getProject(reviewResult.value.businessId);
        if (projectDetail) processType = projectDetail.processType;
      } catch {}
    }
    processType = processType || 1;
    const projectType = reviewResult.value.projectType;
    const configs = await getProcessIndicatorConfigListByProcessType(processType, projectType);
    indicatorConfigs.value = configs || [];

    if (formData.value.scores.length === 0 && configs.length > 0) {
      formData.value.scores = configs.map((config: any) => ({
        indicatorId: config.indicatorId,
        indicatorCode: config.indicatorCode || '',
        indicatorName: config.indicatorName || '',
        maxScore: config.maxScore || 10,
        score: 0,
        scoreLevel: undefined,
        scoreRatio: 0,
        comment: '',
      }));
    } else if (formData.value.scores.length > 0) {
      formData.value.scores = formData.value.scores.map((saved) => {
        const config = configs.find((c: any) => c.indicatorId === saved.indicatorId);
        if (config) {
          return {
            ...saved,
            indicatorCode: config.indicatorCode || saved.indicatorCode,
            indicatorName: config.indicatorName || saved.indicatorName,
            maxScore: config.maxScore || saved.maxScore,
          };
        }
        return saved;
      });
    }
  } catch (e) {
    console.error('加载指标配置失败', e);
  }
};

const generateScoreOptions = (maxScore: number, ratios: {
  satisfied?: number;
  basic?: number;
  partial?: number;
  unsatisfied?: number;
}): ScoreOption[] => {
  const defaultRatios = { satisfied: 1, basic: 0.75, partial: 0.5, unsatisfied: 0.25 };
  const r = { ...defaultRatios, ...ratios };
  return [
    { label: `${(maxScore * r.satisfied).toFixed(1)}分 - 满足`, value: 'SATISFIED', ratio: r.satisfied, score: maxScore * r.satisfied },
    { label: `${(maxScore * r.basic).toFixed(1)}分 - 基本满足`, value: 'BASIC', ratio: r.basic, score: maxScore * r.basic },
    { label: `${(maxScore * r.partial).toFixed(1)}分 - 部分满足`, value: 'PARTIAL', ratio: r.partial, score: maxScore * r.partial },
    { label: `${(maxScore * r.unsatisfied).toFixed(1)}分 - 不满足`, value: 'UNSATISFIED', ratio: r.unsatisfied, score: maxScore * r.unsatisfied },
  ];
};

const getScoreOptionsForIndicator = (indicatorId: number): ScoreOption[] => {
  const config = indicatorConfigs.value.find((c: any) => c.indicatorId === indicatorId);
  if (config) {
    return generateScoreOptions(config.maxScore || 10, {
      satisfied: config.scoreRatioSatisfied,
      basic: config.scoreRatioBasic,
      partial: config.scoreRatioPartial,
      unsatisfied: config.scoreRatioUnsatisfied,
    });
  }
  return generateScoreOptions(10, {});
};

const handleScoreLevelChange = (scoreItem: IndicatorScoreData) => {
  if (scoreItem.scoreLevel) {
    const options = getScoreOptionsForIndicator(scoreItem.indicatorId);
    const selectedOption = options.find((opt) => opt.value === scoreItem.scoreLevel);
    if (selectedOption) {
      scoreItem.score = selectedOption.score;
      scoreItem.scoreRatio = selectedOption.ratio;
    }
  } else {
    scoreItem.score = 0;
    scoreItem.scoreRatio = 0;
  }
};

const handleSubmit = async () => {
  if (formData.value.scores.some((s) => !s.scoreLevel)) {
    message.warning('请完成所有指标的评分');
    return;
  }
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
    const indicatorScores = formData.value.scores.map((s) => ({
      indicatorId: s.indicatorId,
      indicatorCode: s.indicatorCode,
      maxScore: s.maxScore,
      score: s.score,
      scoreLevel: s.scoreLevel,
      scoreRatio: s.scoreRatio,
      comment: s.comment,
    }));

    await submitReview(currentResultId.value, {
      taskId: reviewResult.value?.taskId || 0,
      scoreData: JSON.stringify(formData.value.scores),
      totalScore: totalScore.value,
      conclusion: formData.value.conclusion,
      opinion: formData.value.opinion,
      indicatorScores,
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
    title="评审详情"
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
            description="请完成评分后提交评审。如有利益冲突可申请回避。"
            type="info"
            show-icon
          />
          <a-alert
            v-else-if="reviewResult.status === 2"
            message="评审进行中"
            description="请完成评分和评审意见后提交评审。"
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

        <!-- 业务信息 -->
        <Card title="业务信息" :bordered="false" class="mb-4">
          <Descriptions :column="2" bordered size="small">
            <Descriptions.Item label="业务类型">
              {{ reviewResult.businessType === 1 ? '备案' : reviewResult.businessType === 2 ? '项目' : '成果' }}
            </Descriptions.Item>
            <Descriptions.Item label="业务名称">
              {{ reviewResult.businessName || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="任务类型">
              {{ reviewResult.taskType === 1 ? '备案论证' : reviewResult.taskType === 2 ? '中期评估' : reviewResult.taskType === 3 ? '验收评审' : '成果审核' }}
            </Descriptions.Item>
            <Descriptions.Item label="评审状态">
              <a-tag :color="reviewResult.status === 3 ? 'green' : reviewResult.status === 2 ? 'blue' : 'orange'">
                {{ reviewResult.status === 0 ? '待接收' : reviewResult.status === 1 ? '已接收' : reviewResult.status === 2 ? '评审中' : reviewResult.status === 3 ? '已提交' : '未知' }}
              </a-tag>
            </Descriptions.Item>
          </Descriptions>
        </Card>

        <!-- 评分组件 -->
        <Card
          title="评分"
          :bordered="false"
          class="mb-4"
        >
          <div v-for="(item, index) in formData.scores" :key="index" class="mb-4 p-4 bg-gray-50 rounded">
            <div class="mb-2 flex justify-between items-center">
              <div>
                <strong>{{ item.indicatorName || item.indicatorCode }}</strong>
                <span class="text-gray-500 ml-2">(指标{{ item.indicatorId }})</span>
              </div>
              <div class="text-blue-600 font-bold">
                满分：{{ item.maxScore }}分
              </div>
            </div>
            <a-radio-group v-model:value="item.scoreLevel" @change="handleScoreLevelChange(item)">
              <Space direction="vertical" :size="8">
                <a-radio v-for="opt in getScoreOptionsForIndicator(item.indicatorId)" :key="opt.value" :value="opt.value">
                  <span>
                    {{ opt.label }}
                    <span v-if="item.scoreLevel === opt.value" class="text-green-600 ml-2">
                      (得分: {{ item.score.toFixed(1) }}分)
                    </span>
                  </span>
                </a-radio>
              </Space>
            </a-radio-group>
            <div class="mt-2">
              <a-input
                v-model:value="item.comment"
                placeholder="请输入评分说明（可选）"
              />
            </div>
          </div>

          <a-divider />

          <div class="text-right text-lg">
            <strong>总分：{{ totalScore.toFixed(1) }}分</strong>
          </div>
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
