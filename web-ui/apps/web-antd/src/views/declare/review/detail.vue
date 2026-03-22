<script lang="ts">
import type { DeclareReviewApi } from '#/api/declare/review';

import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';

import { Page } from '@vben/common-ui';

import {
  Avatar,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  DescriptionsItem,
  Divider,
  Empty,
  Form,
  FormItem,
  Input,
  message,
  Progress,
  Radio,
  RadioGroup,
  Rate,
  Row,
  Select,
  SelectOption,
  Space,
  Spin,
  Statistic,
  Steps,
  Step,
  Tag,
  Textarea,
  Timeline,
  Tooltip,
} from 'ant-design-vue';

import {
  applyAvoidReview,
  getReviewResultDetail,
  receiveReviewTask,
  startReview,
  submitReview,
} from '#/api/declare/review';

import { getProcessIndicatorConfigListByProcessType } from '#/api/declare/process-indicator-config';
import { getProject } from '#/api/declare/project';

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
  color: string;
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
  { label: '通过', value: '通过', color: 'success' },
  { label: '需整改', value: '需整改', color: 'warning' },
  { label: '未通过', value: '未通过', color: 'error' },
];

const statusOptions = [
  { label: '待接收', value: 0, color: 'default', stepStatus: 'wait' },
  { label: '已接收', value: 1, color: 'blue', stepStatus: 'process' },
  { label: '评审中', value: 2, color: 'cyan', stepStatus: 'process' },
  { label: '已提交', value: 3, color: 'green', stepStatus: 'finish' },
];

const taskTypeOptions = [
  { label: '备案论证', value: 1 },
  { label: '中期评估', value: 2 },
  { label: '验收评审', value: 3 },
  { label: '成果审核', value: 4 },
];

const businessTypeOptions = [
  { label: '备案', value: 1 },
  { label: '项目', value: 2 },
  { label: '成果', value: 3 },
];

function getStatusLabel(status: number) {
  return statusOptions.find((item) => item.value === status)?.label || '未知';
}
function getStatusColor(status: number) {
  return statusOptions.find((item) => item.value === status)?.color || 'default';
}
function getStatusStep(status: number) {
  return statusOptions.find((item) => item.value === status)?.stepStatus || 'wait';
}
function getTaskTypeLabel(type?: number) {
  return taskTypeOptions.find((item) => item.value === type)?.label || '-';
}
function getBusinessTypeLabel(type?: number) {
  return businessTypeOptions.find((item) => item.value === type)?.label || '-';
}
function getConclusionInfo(conclusion?: string) {
  return conclusionOptions.find((item) => item.value === conclusion) || { label: '-', color: 'default' };
}

function formatTime(time: string | undefined | null): string {
  if (!time) return '-';
  if (/^\d{13}$/.test(time)) {
    return new Date(Number(time)).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  }
  const cleaned = time.replace('T', ' ').substring(0, 19);
  return cleaned || '-';
}

function formatDate(time: string | undefined | null): string {
  if (!time) return '-';
  if (/^\d{13}$/.test(time)) {
    return new Date(Number(time)).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });
  }
  return time.substring(0, 10) || '-';
}

export default {
  components: {
    Avatar,
    Badge,
    Button,
    Card,
    Col,
    Descriptions,
    DescriptionsItem,
    Divider,
    Empty,
    Form,
    FormItem,
    Input,
    Page,
    Progress,
    Radio,
    RadioGroup,
    Rate,
    Row,
    Select,
    SelectOption,
    Space,
    Spin,
    Statistic,
    Steps,
    Step,
    Tag,
    Textarea,
    Timeline,
    Tooltip,
  },
  props: {
    resultId: {
      type: Number,
      default: 0,
    },
  },
  setup(props: { resultId: number }) {
    const route = useRoute();
    const loading = ref(false);
    const submitting = ref(false);

    const currentResultId = computed(() => {
      if (props.resultId) return props.resultId;
      const id = route.query.id;
      return id ? Number(id) : 0;
    });

    const reviewResult = ref<DeclareReviewApi.ReviewResult | null>(null);
    const indicatorConfigs = ref<any[]>([]);
    const formData = ref({
      scores: [] as IndicatorScoreData[],
      conclusion: '',
      opinion: '',
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
      } finally {
        loading.value = false;
      }
    };

    const loadIndicatorConfigs = async () => {
      if (!reviewResult.value) return;
      try {
        let processType = reviewResult.value.processType;
        if (!processType && !reviewResult.value.processType && reviewResult.value.businessId && reviewResult.value.businessType === 2) {
          try {
            const projectDetail = await getProject(reviewResult.value.businessId);
            if (projectDetail) processType = projectDetail.processType;
          } catch (e) { /* ignore */ }
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
      } catch (e) { /* ignore */ }
    };

    const scoreLevelColors: Record<string, string> = {
      SATISFIED: '#52c41a',
      BASIC: '#1890ff',
      PARTIAL: '#faad14',
      UNSATISFIED: '#ff4d4f',
    };

    const generateScoreOptions = (maxScore: number, ratios: {
      satisfied?: number; basic?: number; partial?: number; unsatisfied?: number;
    }): ScoreOption[] => {
      const defaultRatios = { satisfied: 1, basic: 0.75, partial: 0.5, unsatisfied: 0.25 };
      const r = { ...defaultRatios, ...ratios };
      return [
        { label: '满足', value: 'SATISFIED', ratio: r.satisfied, score: maxScore * r.satisfied, color: scoreLevelColors.SATISFIED },
        { label: '基本满足', value: 'BASIC', ratio: r.basic, score: maxScore * r.basic, color: scoreLevelColors.BASIC },
        { label: '部分满足', value: 'PARTIAL', ratio: r.partial, score: maxScore * r.partial, color: scoreLevelColors.PARTIAL },
        { label: '不满足', value: 'UNSATISFIED', ratio: r.unsatisfied, score: maxScore * r.unsatisfied, color: scoreLevelColors.UNSATISFIED },
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

    const totalScore = computed(() => {
      return formData.value.scores.reduce((sum, item) => sum + item.score, 0);
    });

    const maxTotalScore = computed(() => {
      return formData.value.scores.reduce((sum, item) => sum + item.maxScore, 0);
    });

    const scoreProgress = computed(() => {
      if (maxTotalScore.value === 0) return 0;
      return Math.round((totalScore.value / maxTotalScore.value) * 100);
    });

    const handleReceive = async () => {
      try {
        await receiveReviewTask(currentResultId.value);
        message.success('已接收任务');
        await loadDetail();
      } catch { message.error('接收失败'); }
    };

    const handleStart = async () => {
      try {
        await startReview(currentResultId.value);
        message.success('已开始评审');
        await loadDetail();
      } catch { message.error('开始失败'); }
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
        });
        message.success('提交成功');
        await loadDetail();
      } catch (e) { message.error('提交失败'); }
      finally { submitting.value = false; }
    };

    const handleAvoid = async () => {
      const reason = window.prompt('请输入回避原因：');
      if (!reason) return;
      try {
        await applyAvoidReview(currentResultId.value, reason);
        message.success('已提交回避申请');
        await loadDetail();
      } catch { message.error('申请失败'); }
    };

    onMounted(() => { loadDetail(); });
    watch(() => route.query.id, () => { loadDetail(); });

    return {
      route, loading, submitting, reviewResult, formData,
      currentResultId, conclusionOptions, totalScore, maxTotalScore, scoreProgress,
      getStatusLabel, getStatusColor, getStatusStep, getTaskTypeLabel, getBusinessTypeLabel,
      getConclusionInfo, getScoreOptionsForIndicator, handleScoreLevelChange,
      handleReceive, handleStart, handleSubmit, handleAvoid,
      formatTime, formatDate,
      scoreLevelColors,
      taskTypeOptions,
    };
  },
};
</script>

<template>
  <Page title="评审详情">
    <Spin :spinning="loading">
      <div v-if="reviewResult">

        <!-- 顶部操作区 + 状态进度条 -->
        <Card :bordered="false" class="mb-4 status-card">
          <Row :gutter="24" align="middle">
            <Col :span="18">
              <Steps :current="reviewResult.status ?? 0" size="small" class="review-steps">
                <Step v-for="(opt, idx) in statusOptions" :key="idx"
                  :title="opt.label"
                  :status="getStatusStep(reviewResult.status ?? 0) as any" />
              </Steps>
            </Col>
            <Col :span="6" class="text-right">
              <template v-if="reviewResult.status === 0">
                <Button type="primary" size="large" class="action-btn" @click="handleReceive">接收任务</Button>
                <Button size="large" class="ml-2 action-btn" @click="handleAvoid">申请回避</Button>
              </template>
              <template v-else-if="reviewResult.status === 1">
                <Button type="primary" size="large" class="action-btn" @click="handleStart">开始评审</Button>
              </template>
              <template v-else-if="reviewResult.status === 2">
                <Button type="primary" size="large" class="action-btn" :loading="submitting" @click="handleSubmit">
                  提交评审
                </Button>
              </template>
              <template v-else-if="reviewResult.status === 3">
                <Tag color="green" class="completed-tag">评审已完成</Tag>
              </template>
            </Col>
          </Row>
        </Card>

        <!-- 专家信息 + 业务信息 -->
        <Row :gutter="16" class="mb-4">
          <!-- 专家信息卡片 -->
          <Col :span="8">
            <Card title="评审专家" :bordered="false" class="info-card">
              <div class="expert-info">
                <Avatar :size="56" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                  {{ reviewResult.expertName?.charAt(0) || 'E' }}
                </Avatar>
                <div class="expert-detail">
                  <div class="expert-name">{{ reviewResult.expertName || '未知专家' }}</div>
                  <div class="expert-unit">{{ reviewResult.expertWorkUnit || '-' }}</div>
                  <div class="expert-phone">{{ reviewResult.expertPhone || '-' }}</div>
                </div>
              </div>
              <Divider dashed class="my-3" />
              <div class="expert-meta">
                <div class="meta-item">
                  <span class="meta-label">专家ID</span>
                  <span class="meta-value">{{ reviewResult.expertId }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">状态</span>
                  <Tag :color="getStatusColor(reviewResult.status ?? 0)">
                    {{ getStatusLabel(reviewResult.status ?? 0) }}
                  </Tag>
                </div>
                <div class="meta-item">
                  <span class="meta-label">接收时间</span>
                  <span class="meta-value">{{ formatTime(reviewResult.receiveTime) }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">提交时间</span>
                  <span class="meta-value">{{ formatTime(reviewResult.submitTime) }}</span>
                </div>
                <div v-if="reviewResult.isAvoid" class="meta-item">
                  <span class="meta-label">回避状态</span>
                  <Tag color="orange">已申请回避</Tag>
                </div>
              </div>
            </Card>
          </Col>

          <!-- 业务信息卡片 -->
          <Col :span="16">
            <Card title="业务信息" :bordered="false" class="info-card">
              <Descriptions :column="2" bordered size="small" class="biz-descriptions">
                <DescriptionsItem label="业务类型">
                  <Tag color="blue">{{ getBusinessTypeLabel(reviewResult.businessType) }}</Tag>
                </DescriptionsItem>
                <DescriptionsItem label="任务类型">
                  <span>{{ getTaskTypeLabel(reviewResult.taskType) }}</span>
                </DescriptionsItem>
                <DescriptionsItem label="业务ID">
                  <span class="mono-text">{{ reviewResult.businessId }}</span>
                </DescriptionsItem>
                <DescriptionsItem label="评审任务ID">
                  <span class="mono-text">{{ reviewResult.taskId }}</span>
                </DescriptionsItem>
                <DescriptionsItem label="流程实例ID" :span="2">
                  <span class="mono-text">{{ reviewResult.processInstanceId || '-' }}</span>
                </DescriptionsItem>
                <DescriptionsItem label="Flowable任务ID" :span="2">
                  <span class="mono-text">{{ reviewResult.flowableTaskId || '-' }}</span>
                </DescriptionsItem>
                <DescriptionsItem v-if="reviewResult.businessName" label="业务名称" :span="2">
                  {{ reviewResult.businessName }}
                </DescriptionsItem>
                <DescriptionsItem label="创建时间">
                  {{ formatTime(reviewResult.createTime) }}
                </DescriptionsItem>
                <DescriptionsItem label="总分">
                  <span class="score-highlight" v-if="reviewResult.totalScore != null">
                    {{ reviewResult.totalScore }} 分
                  </span>
                  <span v-else>-</span>
                </DescriptionsItem>
                <DescriptionsItem v-if="reviewResult.conclusion" label="评审结论" :span="2">
                  <Tag :color="getConclusionInfo(reviewResult.conclusion).color">
                    {{ reviewResult.conclusion }}
                  </Tag>
                </DescriptionsItem>
              </Descriptions>
            </Card>
          </Col>
        </Row>

        <!-- 评分卡片 -->
        <Card
          title="指标评分"
          :bordered="false"
          class="mb-4 score-card"
          :disabled="reviewResult.status !== 2"
        >
          <template #extra>
            <div class="score-summary">
              <span class="score-current">{{ totalScore.toFixed(1) }}</span>
              <span class="score-sep">/</span>
              <span class="score-max">{{ maxTotalScore.toFixed(1) }} 分</span>
              <Progress
                :percent="scoreProgress"
                :stroke-color="scoreProgress >= 75 ? '#52c41a' : scoreProgress >= 50 ? '#faad14' : '#ff4d4f'"
                size="small"
                :show-info="false"
                class="score-progress"
              />
            </div>
          </template>

          <div v-if="formData.scores.length === 0" class="text-center py-8">
            <Empty description="暂无评分数据" />
          </div>

          <div v-else class="score-list">
            <Card
              v-for="(item, index) in formData.scores"
              :key="index"
              size="small"
              class="score-item-card"
              :body-style="{ padding: '16px' }"
            >
              <Row align="middle" :gutter="16">
                <Col :span="12">
                  <div class="indicator-name">
                    <span class="indicator-index">{{ index + 1 }}</span>
                    <span class="indicator-title">{{ item.indicatorName || item.indicatorCode }}</span>
                    <Tag v-if="item.indicatorCode" color="default" class="ml-2">{{ item.indicatorCode }}</Tag>
                  </div>
                  <div class="indicator-meta">
                    满分 <span class="max-score">{{ item.maxScore }}</span> 分
                  </div>
                </Col>
                <Col :span="12">
                  <RadioGroup
                    :value="item.scoreLevel"
                    @change="() => handleScoreLevelChange(item)"
                    :disabled="reviewResult.status !== 2"
                    class="score-radio-group"
                  >
                    <Space :size="[8, 8]" wrap>
                      <Radio
                        v-for="opt in getScoreOptionsForIndicator(item.indicatorId)"
                        :key="opt.value"
                        :value="opt.value"
                        class="score-radio-item"
                      >
                        <Tooltip :title="`${opt.score.toFixed(1)} 分`">
                          <Tag
                            :color="item.scoreLevel === opt.value ? opt.color : 'default'"
                            class="score-tag"
                          >
                            {{ opt.label }}
                            <span class="score-tag-score">({{ opt.score.toFixed(1) }})</span>
                          </Tag>
                        </Tooltip>
                      </Radio>
                    </Space>
                  </RadioGroup>
                </Col>
              </Row>

              <div v-if="item.scoreLevel" class="score-result">
                <span class="score-result-label">当前得分：</span>
                <span class="score-result-value" :style="{ color: scoreLevelColors[item.scoreLevel] }">
                  {{ item.score.toFixed(1) }} 分
                </span>
                <Tag
                  :color="scoreLevelColors[item.scoreLevel]"
                  class="ml-2"
                >
                  {{ SCORE_LEVEL[item.scoreLevel] || item.scoreLevel }}
                </Tag>
              </div>

              <div class="mt-3">
                <Input
                  v-model:value="item.comment"
                  placeholder="评分说明（可选）"
                  :disabled="reviewResult.status !== 2"
                  size="small"
                />
              </div>
            </Card>
          </div>
        </Card>

        <!-- 评审意见卡片 -->
        <Card
          title="评审意见"
          :bordered="false"
          class="opinion-card"
          :disabled="reviewResult.status !== 2"
        >
          <Form layout="vertical">
            <Row :gutter="16">
              <Col :span="8">
                <FormItem label="评审结论" required>
                  <Select
                    v-model:value="formData.conclusion"
                    placeholder="请选择评审结论"
                    :disabled="reviewResult.status !== 2"
                    size="large"
                  >
                    <SelectOption v-for="opt in conclusionOptions" :key="opt.value" :value="opt.value">
                      <Tag :color="opt.color">{{ opt.label }}</Tag>
                    </SelectOption>
                  </Select>
                </FormItem>
              </Col>
              <Col :span="16">
                <FormItem label="总分（自动计算）">
                  <Statistic
                    :value="totalScore"
                    :suffix="`/ ${maxTotalScore.toFixed(1)}`"
                    :value-style="{ color: scoreProgress >= 75 ? '#52c41a' : scoreProgress >= 50 ? '#faad14' : '#ff4d4f', fontSize: '28px', fontWeight: 'bold' }"
                  />
                </FormItem>
              </Col>
            </Row>
            <FormItem label="评审意见（包含核心结论、具体依据、整改建议）" required>
              <Textarea
                v-model:value="formData.opinion"
                :rows="5"
                placeholder="请填写详细的评审意见，包括：&#10;1. 总体评价&#10;2. 主要优点&#10;3. 存在问题&#10;4. 整改建议"
                :disabled="reviewResult.status !== 2"
                size="large"
              />
            </FormItem>
          </Form>
        </Card>

      </div>
    </Spin>
  </Page>
</template>

<style scoped>
.status-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf2 100%);
  border-radius: 12px;
}

.review-steps {
  padding: 4px 0;
}

.action-btn {
  border-radius: 8px;
  font-weight: 500;
}

.completed-tag {
  font-size: 16px;
  padding: 4px 16px;
  border-radius: 8px;
}

.info-card {
  border-radius: 12px;
  height: 100%;
}

.info-card :deep(.ant-card-head) {
  border-radius: 12px 12px 0 0;
  font-weight: 600;
}

.expert-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}

.expert-detail {
  flex: 1;
}

.expert-name {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.4;
}

.expert-unit {
  font-size: 13px;
  color: #6b7280;
  margin-top: 2px;
}

.expert-phone {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.expert-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.meta-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.meta-label {
  color: #6b7280;
}

.meta-value {
  color: #374151;
  font-weight: 500;
}

.biz-descriptions :deep(.ant-descriptions-item-label) {
  background: #f9fafb;
  color: #6b7280;
  font-weight: 500;
}

.biz-descriptions :deep(.ant-descriptions-item-content) {
  color: #1f2937;
}

.mono-text {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  color: #4b5563;
}

.score-highlight {
  color: #3b82f6;
  font-weight: 600;
  font-size: 15px;
}

.score-card {
  border-radius: 12px;
}

.score-card :deep(.ant-card-head) {
  border-radius: 12px 12px 0 0;
  font-weight: 600;
}

.score-summary {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
}

.score-current {
  font-size: 28px;
  font-weight: 800;
  color: #1f2937;
  line-height: 1;
}

.score-sep {
  font-size: 18px;
  color: #9ca3af;
}

.score-max {
  font-size: 16px;
  color: #6b7280;
}

.score-progress {
  width: 120px;
  margin-left: 8px;
}

.score-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.score-item-card {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  transition: box-shadow 0.2s;
}

.score-item-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.indicator-name {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.indicator-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #667eea;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.indicator-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.max-score {
  color: #667eea;
  font-weight: 700;
}

.indicator-meta {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
  margin-left: 30px;
}

.score-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.score-radio-item :deep(.ant-radio-wrapper) {
  align-items: center;
}

.score-tag {
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  border: none;
}

.score-tag-score {
  opacity: 0.7;
  font-size: 11px;
  margin-left: 2px;
}

.score-result {
  margin-top: 10px;
  padding: 6px 12px;
  background: #f9fafb;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.score-result-label {
  font-size: 13px;
  color: #6b7280;
}

.score-result-value {
  font-size: 18px;
  font-weight: 800;
}

.opinion-card {
  border-radius: 12px;
}

.opinion-card :deep(.ant-card-head) {
  border-radius: 12px 12px 0 0;
  font-weight: 600;
}

.mb-4 { margin-bottom: 16px; }
.mb-2 { margin-bottom: 8px; }
.mt-3 { margin-top: 12px; }
.ml-2 { margin-left: 8px; }
.my-3 { margin-top: 12px; margin-bottom: 12px; }
.py-8 { padding-top: 32px; padding-bottom: 32px; }
.text-center { text-align: center; }
.text-right { text-align: right; }
.mr-4 { margin-right: 16px; }
</style>
