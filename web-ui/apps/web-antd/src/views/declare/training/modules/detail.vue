<script lang="ts" setup>
import type { Training } from '#/api/declare/training';

import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  Card,
  Descriptions,
  DescriptionsItem,
  Divider,
  Empty,
  Row,
  Space,
  Spin,
  Tag,
} from 'ant-design-vue';

import {
  getTrainingStatusColor,
  getTrainingStatusLabel,
  getTrainingTypeLabel,
  getTargetScopeLabel,
} from '../data';

const loading = ref(false);
const detail = ref<Training | null>(null);

const [Modal, modalApi] = useVbenModal({
  showCancelButton: false,
  showConfirmButton: false,
  onOpenChange: async (isOpen) => {
    if (isOpen) {
      const data = modalApi.getData<Training>();
      if (data?.id) {
        detail.value = data;
      }
    }
  },
  onClosed: () => {
    detail.value = null;
  },
});
</script>

<template>
  <Modal
    title="活动详情"
    class="detail-modal w-[80%]"
    :centered="true"
  >
    <Spin :spinning="loading">
      <Descriptions :column="2" bordered size="small" class="mb-4">
        <DescriptionsItem :span="2" :label="'活动名称'">
          {{ detail?.name || '-' }}
        </DescriptionsItem>
        <DescriptionsItem :label="'活动类型'">
          {{ getTrainingTypeLabel(detail?.type) }}
        </DescriptionsItem>
        <DescriptionsItem :label="'状态'">
          <Tag :color="getTrainingStatusColor(detail?.status)">
            {{ getTrainingStatusLabel(detail?.status) }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem :label="'组织单位'">
          {{ detail?.organizer || '-' }}
        </DescriptionsItem>
        <DescriptionsItem :label="'主讲人/嘉宾'">
          {{ detail?.speaker || '-' }}
        </DescriptionsItem>
        <DescriptionsItem :label="'开始时间'">
          {{ detail?.startTime || '-' }}
        </DescriptionsItem>
        <DescriptionsItem :label="'结束时间'">
          {{ detail?.endTime || '-' }}
        </DescriptionsItem>
        <DescriptionsItem :label="'活动地点'">
          {{ detail?.location || '-' }}
        </DescriptionsItem>
        <DescriptionsItem :label="'线上链接'">
          <a v-if="detail?.onlineLink" :href="detail.onlineLink" target="_blank">
            {{ detail.onlineLink }}
          </a>
          <span v-else>-</span>
        </DescriptionsItem>
        <DescriptionsItem :label="'目标范围'">
          {{ getTargetScopeLabel(detail?.targetScope) }}
        </DescriptionsItem>
        <DescriptionsItem :label="'目标省份'">
          {{ detail?.targetProvinces || '-' }}
        </DescriptionsItem>
        <DescriptionsItem :label="'报名截止时间'">
          {{ detail?.registrationDeadline || '-' }}
        </DescriptionsItem>
        <DescriptionsItem :label="'最大人数'">
          {{ detail?.maxParticipants || '不限' }}
        </DescriptionsItem>
        <DescriptionsItem :label="'当前报名'">
          {{ detail?.currentParticipants || 0 }} 人
        </DescriptionsItem>
        <DescriptionsItem :label="'发布时间'">
          {{ detail?.publishTime || '-' }}
        </DescriptionsItem>
        <DescriptionsItem :label="'发布人'">
          {{ detail?.publisherName || '-' }}
        </DescriptionsItem>
      </Descriptions>

      <Divider>活动详情</Divider>
      <div v-if="detail?.content" class="prose max-w-none" v-html="detail.content" />
      <Empty v-else description="暂无活动详情" />

      <Divider>会议资料</Divider>
      <div v-if="detail?.meetingMaterials" v-html="detail.meetingMaterials" />
      <Empty v-else description="暂无会议资料" />

      <template v-if="detail?.posterUrls?.trim()">
        <Divider>活动海报</Divider>
        <Space wrap>
          <img
            v-for="(url, index) in detail.posterUrls.split(',').filter(Boolean)"
            :key="index"
            :src="url"
            class="poster-image"
          />
        </Space>
      </template>

      <template v-if="detail?.attachmentUrls?.trim()">
        <Divider>附件</Divider>
        <Space direction="vertical">
          <a
            v-for="(url, index) in detail.attachmentUrls.split(',').filter(Boolean)"
            :key="index"
            :href="url"
            target="_blank"
          >
            附件 {{ index + 1 }}
          </a>
        </Space>
      </template>
    </Spin>
  </Modal>
</template>

<style scoped>
.mb-4 {
  margin-bottom: 16px;
}

.poster-image {
  width: 200px;
  height: auto;
  border-radius: 8px;
  object-fit: cover;
}

.detail-modal {
  width: 80% !important;
  max-width: 1400px;
}

.detail-modal :deep(.ant-modal) {
  width: 80% !important;
  max-width: 1400px;
}
</style>
