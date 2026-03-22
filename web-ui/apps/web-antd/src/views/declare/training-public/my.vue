<script lang="ts" setup>
import type { Registration } from '#/api/declare/training';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { Button, Empty, Input, Rate, Divider } from 'ant-design-vue';

import dayjs from 'dayjs';

import { registrationApi, trainingClientApi } from '#/api/declare/training';
import { getRegistrationStatusColor, getRegistrationStatusLabel } from '../training/data';

const loading = ref(false);
const list = ref<Registration[]>([]);

// 反馈表单
const feedbackForm = ref({
  registrationId: null as number | null,
  feedback: '',
  rating: 0,
});
const showFeedbackModal = ref(false);
const submitting = ref(false);

/** 加载数据 */
async function loadData() {
  loading.value = true;
  try {
    const data = await trainingClientApi.getMy({ pageNo: 1, pageSize: 100 });
    list.value = data?.list || [];
  } finally {
    loading.value = false;
  }
}

/** 格式化时间 */
function formatTime(time: string | number | undefined): string {
  if (!time) return '-';
  return dayjs(time).format('YYYY-MM-DD HH:mm');
}

/** 打开反馈弹窗 */
function openFeedback(row: Registration) {
  feedbackForm.value = {
    registrationId: row.id!,
    feedback: row.feedback || '',
    rating: row.rating || 0,
  };
  showFeedbackModal.value = true;
}

/** 提交反馈 */
async function handleSubmitFeedback() {
  submitting.value = true;
  try {
    await trainingClientApi.feedback({
      registrationId: feedbackForm.value.registrationId!,
      feedback: feedbackForm.value.feedback,
      rating: feedbackForm.value.rating,
    });
    showFeedbackModal.value = false;
    loadData();
  } finally {
    submitting.value = false;
  }
}

/** 取消报名 */
async function handleCancel(row: Registration) {
    await trainingClientApi.cancel(row.trainingId!);
  loadData();
}

/** 获取状态对应的操作 */
function getActions(row: Registration) {
  const actions = [];
  if (row.status === 1) {
    actions.push({ label: '取消报名', danger: true, click: () => handleCancel(row) });
  }
  if (row.status === 2) {
    actions.push({ label: '填写反馈', click: () => openFeedback(row) });
  }
  return actions;
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <Page auto-content-height class="my-registration-page">
    <div class="registration-list">
      <div v-if="loading" class="registration-list__loading">
        <i class="fas fa-circle-notch fa-spin text-2xl text-[#4A8F72]"></i>
        <span class="ml-3 text-gray-500">加载中...</span>
      </div>

      <Empty v-else-if="list.length === 0" description="暂无报名记录" />

      <ul v-else class="registration-list__items">
        <li v-for="item in list" :key="item.id" class="registration-card">
          <div class="registration-card__header">
            <div class="registration-card__info">
              <h3 class="registration-card__title">{{ item.trainingName }}</h3>
              <div class="registration-card__meta">
                <span>
                  <i class="fas fa-calendar-alt mr-1 text-gray-400"></i>
                  {{ formatTime(item.registerTime) }}
                </span>
                <span>
                  <i class="fas fa-building mr-1 text-gray-400"></i>
                  {{ item.organization || '-' }}
                </span>
              </div>
            </div>
            <div class="registration-card__status">
              <a-tag :color="getRegistrationStatusColor(item.status)">
                {{ getRegistrationStatusLabel(item.status) }}
              </a-tag>
            </div>
          </div>

          <div class="registration-card__body">
            <div class="registration-card__detail">
              <div class="detail-item">
                <span class="detail-item__label">报名人</span>
                <span class="detail-item__value">{{ item.userName }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-item__label">职位/职称</span>
                <span class="detail-item__value">{{ item.position || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-item__label">联系电话</span>
                <span class="detail-item__value">{{ item.phone || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-item__label">签到时间</span>
                <span class="detail-item__value">{{ formatTime(item.signInTime) }}</span>
              </div>
            </div>

            <div v-if="item.feedback" class="registration-card__feedback">
              <Divider class="my-2">我的反馈</Divider>
              <div class="feedback-content">
                <div v-if="item.rating" class="feedback-rating">
                  <span class="text-gray-500 mr-2">评分：</span>
                  <Rate :value="item.rating" disabled />
                </div>
                <p v-if="item.feedback" class="feedback-text">{{ item.feedback }}</p>
              </div>
            </div>
          </div>

          <div class="registration-card__actions">
            <template v-for="(action, idx) in getActions(item)" :key="idx">
              <Button
                :type="action.danger ? 'text' : 'link'"
                :danger="action.danger"
                size="small"
                @click="action.click"
              >
                {{ action.label }}
              </Button>
            </template>
          </div>
        </li>
      </ul>
    </div>

    <!-- 反馈弹窗 -->
    <div v-if="showFeedbackModal" class="feedback-modal">
      <div class="feedback-modal__overlay" @click="showFeedbackModal = false"></div>
      <div class="feedback-modal__content">
        <h3 class="feedback-modal__title">填写反馈</h3>
        <div class="feedback-modal__form">
          <div class="form-item">
            <label class="form-item__label">评分</label>
            <Rate v-model:value="feedbackForm.rating" />
          </div>
          <div class="form-item">
            <label class="form-item__label">反馈内容</label>
            <Input.TextArea
              v-model:value="feedbackForm.feedback"
              placeholder="请输入您的反馈意见"
              :rows="4"
            />
          </div>
          <div class="feedback-modal__actions">
            <Button @click="showFeedbackModal = false">取消</Button>
            <Button type="primary" :loading="submitting" @click="handleSubmitFeedback">
              提交
            </Button>
          </div>
        </div>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.my-registration-page {
  padding: 24px;
  background: #f5f6f8;
  min-height: 100%;
}

.registration-list {
  max-width: 900px;
  margin: 0 auto;
}

.registration-list__loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: #fff;
  border-radius: 8px;
}

.registration-list__items {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.registration-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #f0f0f0;
}

.registration-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.registration-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 8px 0;
}

.registration-card__meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #8c8c8c;
}

.registration-card__body {
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
  margin-bottom: 12px;
}

.registration-card__detail {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-item__label {
  font-size: 12px;
  color: #8c8c8c;
}

.detail-item__value {
  font-size: 14px;
  color: #262626;
}

.registration-card__feedback {
  margin-top: 12px;
}

.feedback-content {
  padding: 12px;
  background: #fff;
  border-radius: 4px;
}

.feedback-rating {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.feedback-text {
  font-size: 14px;
  color: #595959;
  margin: 0;
  line-height: 1.6;
}

.registration-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 反馈弹窗 */
.feedback-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.feedback-modal__overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
}

.feedback-modal__content {
  position: relative;
  width: 480px;
  background: #fff;
  border-radius: 8px;
  padding: 24px;
}

.feedback-modal__title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 20px 0;
}

.feedback-modal__form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item__label {
  font-size: 14px;
  color: #595959;
}

.feedback-modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}
</style>
