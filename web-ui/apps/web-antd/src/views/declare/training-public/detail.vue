<script lang="ts" setup>
import type { Training } from '#/api/declare/training';

import { ref } from 'vue';

import { confirm, useVbenModal } from '@vben/common-ui';

import { Button, Divider, Empty, message, Tag } from 'ant-design-vue';

import dayjs from 'dayjs';

import { get, register as registerApi } from '#/api/declare/training';

import {
  getTrainingStatusColor,
  getTrainingStatusLabel,
  getTrainingTypeLabel,
  getTargetScopeLabel,
} from '../training/data';

const emit = defineEmits<{
  refresh: [];
}>();

const detail = ref<Training | null>(null);
const loading = ref(false);

// 报名表单
const registerForm = ref({
  position: '',
  phone: '',
  email: '',
  remark: '',
});
const submitting = ref(false);

/** 格式化时间 */
function formatTime(time: string | number | undefined): string {
  if (!time) return '-';
  return dayjs(time).format('YYYY-MM-DD HH:mm');
}

const [Modal, modalApi] = useVbenModal({
  onOpenChange: async (isOpen) => {
    if (isOpen) {
      const data = modalApi.getData<Training>();
      if (data?.id) {
        loading.value = true;
        try {
          detail.value = await get(data.id);
        } finally {
          loading.value = false;
        }
      }
    }
  },
  onClosed: () => {
    detail.value = null;
    registerForm.value = { position: '', phone: '', email: '', remark: '' };
  },
});

/** 报名 - 先弹出确认框，确认后再提交 */
async function handleRegister() {
  const name = detail.value?.name || '该活动';
  const timeStr = detail.value ? formatTime(detail.value.startTime) : '';
  const locationStr = detail.value?.location || '待定';
  try {
    await confirm({
      title: '确认报名',
      content: `确定要报名参加「${name}」活动吗？\n\n时间：${timeStr}\n地点：${locationStr}`,
    });
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await registerApi({
      trainingId: detail.value!.id!,
      position: registerForm.value.position,
      phone: registerForm.value.phone,
      email: registerForm.value.email,
      remark: registerForm.value.remark,
    });
    message.success('报名成功');
    modalApi.close();
    emit('refresh');
  } finally {
    submitting.value = false;
  }
}

</script>

<template>
  <Modal
    :title="detail?.name || '培训活动详情'"
    class="w-[80%] px-5"
    :show-cancel-button="false"
    :show-confirm-button="false"
    :closable="true"
  >
    <div v-if="loading" class="flex items-center justify-center py-12">
      <i class="fas fa-circle-notch fa-spin text-2xl text-[#4A8F72]"></i>
    </div>

    <div v-else-if="detail" class="detail-content">
      <!-- 头部信息 -->
      <div class="detail-header">
        <div class="detail-poster">
          <img
            v-if="detail.posterUrl"
            :src="detail.posterUrl"
            :alt="detail.name"
            class="detail-poster__img"
          />
          <div v-else class="detail-poster__placeholder">
            <i class="fas fa-chalkboard-teacher text-5xl text-white"></i>
          </div>
        </div>
        <div class="detail-info">
          <div class="detail-info__row">
            <Tag :color="getTrainingStatusColor(detail.status)">
              {{ getTrainingStatusLabel(detail.status) }}
            </Tag>
            <Tag>{{ getTrainingTypeLabel(detail.type) }}</Tag>
            <Tag>{{ getTargetScopeLabel(detail.targetScope) }}</Tag>
          </div>
          <h2 class="detail-info__name">{{ detail.name }}</h2>
          <div class="detail-info__stats">
            <div class="detail-info__stat">
              <i class="fas fa-users mr-2 text-gray-400"></i>
              {{ detail.currentParticipants || 0 }}
              <span class="text-gray-400 text-sm ml-1">
                / {{ detail.maxParticipants || '不限' }} 人
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 活动详情 -->
      <Divider>基本信息</Divider>

      <div class="detail-grid">
        <div class="detail-item">
          <span class="detail-item__label">组织单位</span>
          <span class="detail-item__value">{{ detail.organizer || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-item__label">主讲人</span>
          <span class="detail-item__value">{{ detail.speaker || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-item__label">开始时间</span>
          <span class="detail-item__value">{{ formatTime(detail.startTime) }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-item__label">结束时间</span>
          <span class="detail-item__value">{{ formatTime(detail.endTime) }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-item__label">活动地点</span>
          <span class="detail-item__value">{{ detail.location || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-item__label">报名截止</span>
          <span class="detail-item__value">{{ formatTime(detail.registrationDeadline) }}</span>
        </div>
      </div>

      <Divider>活动介绍</Divider>
      <div
        v-if="detail.content"
        class="detail-content__html prose max-w-none"
        v-html="detail.content"
      />
      <Empty v-else description="暂无活动介绍" />

      <Divider>会议资料</Divider>
      <div v-if="detail.meetingMaterials" v-html="detail.meetingMaterials" />
      <Empty v-else description="暂无会议资料" />

      <!-- 操作按钮 -->
      <div class="detail-actions">
        <Button
          v-if="detail.status === 2"
          type="primary"
          size="large"
          block
          @click="handleRegister"
        >
          立即报名
        </Button>
        <Button
          v-else-if="detail.status === 4"
          size="large"
          block
          disabled
        >
          已结束
        </Button>
      </div>

      <!-- 底部返回按钮 -->
      <div class="mt-6 pt-4 border-t flex justify-center gap-4">
        <button
          class="px-5 py-2 border border-gray-300 rounded-md hover:bg-gray-50 transition-colors flex items-center text-sm"
          @click="modalApi.close()"
        >
          <i class="fas fa-arrow-left mr-2 text-gray-500"></i>
          返回列表
        </button>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.detail-header {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.detail-poster {
  width: 200px;
  height: 150px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.detail-poster__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-poster__placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-info {
  flex: 1;
  min-width: 0;
}

.detail-info__row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.detail-info__name {
  font-size: 20px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 16px 0;
  line-height: 1.4;
}

.detail-info__stats {
  display: flex;
  gap: 24px;
}

.detail-info__stat {
  font-size: 14px;
  color: #595959;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item__label {
  font-size: 12px;
  color: #8c8c8c;
}

.detail-item__value {
  font-size: 14px;
  color: #262626;
}

.detail-content__html :deep(p) {
  margin: 0 0 12px 0;
}

.detail-actions {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}
</style>
