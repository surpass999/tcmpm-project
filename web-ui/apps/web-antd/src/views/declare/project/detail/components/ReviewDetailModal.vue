<script lang="ts" setup>
import type { DeclareReviewApi } from '#/api/declare/review';

import { computed, ref } from 'vue';

import { Button, Empty, Modal } from 'ant-design-vue';

// 本地状态保存评审数据
const localSummary = ref<DeclareReviewApi.ReviewSummary | null>(null);

const currentExpertIndex = ref(0);

// 弹窗是否可见
const visible = ref(false);

// 弹窗标题
const modalTitle = ref('评审详情');

// 格式化时间
function formatTime(time?: string | any): string {
  if (!time) return '-';
  // 如果是字符串
  if (typeof time === 'string') {
    return time.substring(0, 10);
  }
  // 如果是 Date 对象
  if (time instanceof Date) {
    return time.toISOString().substring(0, 10);
  }
  // 如果是 LocalDateTime 序列化后的对象 { year, monthValue, dayOfMonth, ... }
  if (time.year) {
    const year = time.year;
    const month = String(time.monthValue || time.month || 1).padStart(2, '0');
    const day = String(time.dayOfMonth || time.day || 1).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
  // 尝试转为字符串
  return String(time).substring(0, 10);
}

// 当前展示的专家结果
const currentResult = computed(() => {
  if (!localSummary.value?.results?.length) return null;
  return localSummary.value.results[currentExpertIndex.value];
});

// 是否有多个专家
const hasMultipleExperts = computed(() => {
  return (localSummary.value?.results?.length || 0) > 1;
});

// 获取评分等级文本
function getScoreLevelText(level?: string): string {
  const map: Record<string, string> = {
    satisfied: '满足',
    basic: '基本满足',
    partial: '部分满足',
    unsatisfied: '不满足',
  };
  return map[level || ''] || level || '-';
}

// 获取评分等级对应的颜色
function getScoreLevelColor(level?: string): string {
  const map: Record<string, string> = {
    satisfied: 'green',
    basic: 'blue',
    partial: 'orange',
    unsatisfied: 'red',
  };
  return map[level || ''] || 'default';
}

// 打开弹窗
function open(summary: DeclareReviewApi.ReviewSummary) {
  currentExpertIndex.value = 0;
  localSummary.value = summary;
  modalTitle.value = `${summary.processTypeName} - 评审详情`;
  visible.value = true;
}

// 关闭弹窗
function handleClose() {
  visible.value = false;
}

// 上一位专家
function prevExpert() {
  if (currentExpertIndex.value > 0) {
    currentExpertIndex.value--;
  }
}

// 下一位专家
function nextExpert() {
  if (currentExpertIndex.value < (localSummary.value?.results?.length || 0) - 1) {
    currentExpertIndex.value++;
  }
}

defineExpose({ open });
</script>

<template>
  <!-- 使用 Teleport 将弹窗传送到 body，避免嵌套在父弹窗内部导致的问题 -->
  <Teleport to="body">
    <Modal
      v-model:open="visible"
      :title="modalTitle"
      :width="900"
      :footer="null"
      :maskClosable="true"
      :destroyOnClose="false"
      class="review-detail-modal"
      @cancel="handleClose"
    >
      <div v-if="localSummary && currentResult" class="review-detail">
        <!-- 专家切换 -->
        <div v-if="hasMultipleExperts" class="expert-nav">
          <Button :disabled="currentExpertIndex === 0" @click="prevExpert">
            上一位
          </Button>
          <span class="expert-counter">
            专家 {{ currentExpertIndex + 1 }} / {{ localSummary.results.length }}
          </span>
          <Button
            :disabled="currentExpertIndex === localSummary.results.length - 1"
            @click="nextExpert"
          >
            下一位
          </Button>
        </div>

        <!-- 专家基本信息 -->
        <div class="expert-info">
          <div class="expert-header">
            <div class="expert-name">{{ currentResult.expertName }}</div>
            <div v-if="currentResult.totalScore" class="expert-score">
              {{ currentResult.totalScore }}分
            </div>
          </div>
          <div class="expert-meta">
            <span v-if="currentResult.expertWorkUnit">{{ currentResult.expertWorkUnit }}</span>
            <span v-if="currentResult.submitTime">
              评审时间：{{ formatTime(currentResult.submitTime) }}
            </span>
          </div>
          <div v-if="currentResult.conclusion" class="expert-conclusion">
            <a-tag :color="currentResult.conclusion === '通过' ? 'success' : 'error'">
              {{ currentResult.conclusion }}
            </a-tag>
          </div>
        </div>

        <!-- 评分明细 -->
        <div v-if="currentResult.indicatorScores?.length" class="score-detail">
          <div class="section-title">评分明细</div>
          <div class="indicator-list">
            <div
              v-for="indicator in currentResult.indicatorScores"
              :key="indicator.indicatorId"
              class="indicator-item"
            >
              <div class="indicator-header">
                <span class="indicator-name">{{ indicator.indicatorCode }}</span>
                <span v-if="indicator.maxScore" class="indicator-max-score">
                  ({{ indicator.maxScore }}分)
                </span>
              </div>
              <div class="indicator-score">
                <a-tag :color="getScoreLevelColor(indicator.scoreLevel)">
                  {{ getScoreLevelText(indicator.scoreLevel) }}
                </a-tag>
                <span v-if="indicator.score" class="score-value">
                  {{ indicator.score }}分
                </span>
              </div>
              <div v-if="indicator.comment" class="indicator-comment">
                {{ indicator.comment }}
              </div>
            </div>
          </div>
        </div>

      <!-- 评审意见 -->
      <div v-if="currentResult.opinion" class="review-opinion">
        <div class="section-title">评审意见</div>
        <div class="opinion-content">{{ currentResult.opinion }}</div>
      </div>
    </div>
      <Empty v-else description="暂无评审数据" />
    </Modal>
  </Teleport>
</template>

<style scoped>
.review-detail {
  /* max-height: 760px; 已由 ant-modal-body 控制 */
}

/* 控制弹窗整体高度 */
:global(.review-detail-modal .ant-modal-content) {
  max-height: 800px;
  display: flex;
  flex-direction: column;
}

:global(.review-detail-modal .ant-modal-body) {
  max-height: 800px;
  overflow-y: auto;
  flex: 1;
}

.expert-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 8px 16px;
  background: #f5f5f5;
  border-radius: 6px;
}

.expert-counter {
  font-weight: 500;
}

.expert-info {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.expert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.expert-name {
  font-size: 16px;
  font-weight: 600;
}

.expert-score {
  font-size: 24px;
  font-weight: 700;
  color: #1677ff;
}

.expert-meta {
  color: #666;
  font-size: 14px;
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.expert-conclusion {
  margin-top: 8px;
}

.section-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 12px;
  color: #333;
}

.score-detail {
  margin-bottom: 16px;
}

.indicator-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.indicator-item {
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
  border-left: 3px solid #1677ff;
}

.indicator-header {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.indicator-name {
  font-weight: 500;
}

.indicator-max-score {
  color: #999;
  font-size: 12px;
}

.indicator-score {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.score-value {
  font-weight: 600;
  color: #1677ff;
}

.indicator-comment {
  font-size: 13px;
  color: #666;
  margin-top: 4px;
  padding: 8px;
  background: #fff;
  border-radius: 4px;
}

.review-opinion {
  margin-top: 16px;
}

.opinion-content {
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>
