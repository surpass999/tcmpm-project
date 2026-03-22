<script lang="ts" setup>
import type { DeclareReviewApi } from '#/api/declare/review';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Tag, Card, Descriptions, DescriptionsItem } from 'ant-design-vue';

// 评审状态选项
const resultStatusOptions = [
  { label: '待接收', value: 0, color: 'orange' },
  { label: '已接收', value: 1, color: 'blue' },
  { label: '评审中', value: 2, color: 'cyan' },
  { label: '已提交', value: 3, color: 'green' },
  { label: '已拒绝', value: 5, color: 'red' },
];

function getResultStatusLabel(status: number) {
  return resultStatusOptions.find((item) => item.value === status)?.label || '未知';
}

function getResultStatusColor(status: number) {
  return resultStatusOptions.find((item) => item.value === status)?.color || 'default';
}

// 任务类型
const taskTypeOptions = [
  { label: '备案论证', value: 1 },
  { label: '中期评估', value: 2 },
  { label: '验收评审', value: 3 },
  { label: '成果审核', value: 4 },
];

function getTaskTypeLabel(type: number) {
  return taskTypeOptions.find((item) => item.value === type)?.label || '未知';
}

// 业务类型
const businessTypeOptions = [
  { label: '备案', value: 1 },
  { label: '项目', value: 2 },
  { label: '成果', value: 3 },
];

function getBusinessTypeLabel(type: number) {
  return businessTypeOptions.find((item) => item.value === type)?.label || '未知';
}

const formData = ref<DeclareReviewApi.ReviewResult>();

const [Modal, modalApi] = useVbenModal({
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      return;
    }
    const data = modalApi.getData<DeclareReviewApi.ReviewResult>();
    if (!data || !data.result) {
      return;
    }
    formData.value = data.result;
  },
});
</script>

<template>
  <Modal
    title="评审详情"
    :show-cancel-button="false"
    :show-confirm-button="false"
  >
    <div class="p-4">
      <!-- 评审任务信息 -->
      <Card title="评审任务信息" :bordered="false" class="mb-4">
        <Descriptions :column="2" bordered size="small">
          <DescriptionsItem label="业务类型">
            <Tag color="blue">{{ getBusinessTypeLabel(formData?.businessType) }}</Tag>
          </DescriptionsItem>
          <DescriptionsItem label="任务类型">
            <Tag>{{ getTaskTypeLabel(formData?.taskType) }}</Tag>
          </DescriptionsItem>
          <DescriptionsItem label="业务名称" :span="2">{{ formData?.businessName || '-' }}</DescriptionsItem>
          <DescriptionsItem label="评审状态">
            <Tag :color="getResultStatusColor(formData?.status)">
              {{ getResultStatusLabel(formData?.status) }}
            </Tag>
          </DescriptionsItem>
          <DescriptionsItem label="评分">{{ formData?.totalScore ? formData?.totalScore + '分' : '-' }}</DescriptionsItem>
          <DescriptionsItem label="评审结论" :span="2">
            <Tag :color="formData?.conclusion === '通过' ? 'green' : formData?.conclusion === '需整改' ? 'orange' : 'red'">
              {{ formData?.conclusion || '-' }}
            </Tag>
          </DescriptionsItem>
          <DescriptionsItem label="分配时间" :span="2">{{ formData?.createTime || '-' }}</DescriptionsItem>
          <DescriptionsItem label="评审意见" :span="2">{{ formData?.opinion || '无' }}</DescriptionsItem>
        </Descriptions>
      </Card>

      <!-- 专家信息 -->
      <Card title="专家信息" :bordered="false" class="mb-4">
        <Descriptions :column="2" bordered size="small">
          <DescriptionsItem label="专家姓名">{{ formData?.expertName || '-' }}</DescriptionsItem>
          <DescriptionsItem label="工作单位">{{ formData?.expertWorkUnit || '-' }}</DescriptionsItem>
          <DescriptionsItem label="联系电话">{{ formData?.expertPhone || '-' }}</DescriptionsItem>
        </Descriptions>
      </Card>
    </div>
  </Modal>
</template>
