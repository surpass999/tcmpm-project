<script lang="ts">
import type { DeclareReviewApi } from '#/api/declare/review';

import { ref, computed } from 'vue';

import { MdiIcon, Page, useVbenModal } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
  Descriptions,
  DescriptionsItem,
  Divider,
  Progress,
  Row,
  Statistic,
  Table,
  Tag,
  Textarea,
} from 'ant-design-vue';

// 评审状态选项
const resultStatusOptions = [
  { label: '待评审', value: 0, color: 'orange' },
  { label: '已接收', value: 1, color: 'blue' },
  { label: '评审中', value: 2, color: 'cyan' },
  { label: '已提交', value: 3, color: 'green' },
  { label: '超时', value: 4, color: 'red' },
];

function getResultStatusLabel(status: number) {
  return resultStatusOptions.find((item) => item.value === status)?.label || '未知';
}

function getResultStatusColor(status: number) {
  return resultStatusOptions.find((item) => item.value === status)?.color || 'default';
}

export default {
  components: {
    Button,
    Card,
    Col,
    Descriptions,
    DescriptionsItem,
    Divider,
    MdiIcon,
    Page,
    Progress,
    Row,
    Statistic,
    Table,
    Tag,
    Textarea,
  },
  props: {
    task: {
      type: Object as () => DeclareReviewApi.ReviewTask,
      default: () => ({}),
    },
  },
  setup(props: { task: DeclareReviewApi.ReviewTask }) {
    const activeKey = ref('1');

    // 专家列表
    const experts = computed(() => {
      return props.task.experts || [];
    });

    // 评审结果列表
    const results = computed(() => {
      return props.task.results || [];
    });

    // 平均分
    const averageScore = computed(() => {
      if (results.value.length === 0) return 0;
      const submittedResults = results.value.filter((r) => r.status === 3 && r.totalScore);
      if (submittedResults.length === 0) return 0;
      const sum = submittedResults.reduce((acc, r) => acc + (r.totalScore || 0), 0);
      return (sum / submittedResults.length).toFixed(2);
    });

    // 提交进度
    const progressPercent = computed(() => {
      if (results.value.length === 0) return 0;
      const submittedCount = results.value.filter((r) => r.status === 3).length;
      return Math.round((submittedCount / results.value.length) * 100);
    });

    // 表格列定义
    const columns = [
      { title: '专家姓名', dataIndex: 'expertName', key: 'expertName', width: 120 },
      { title: '工作单位', dataIndex: 'expertWorkUnit', key: 'expertWorkUnit', width: 180 },
      {
        title: '评审状态',
        dataIndex: 'status',
        key: 'status',
        width: 100,
        customRender: ({ record }: { record: DeclareReviewApi.ReviewResult }) =>
          h(Tag, { color: getResultStatusColor(record.status) }, () => getResultStatusLabel(record.status)),
      },
      { title: '评分', dataIndex: 'totalScore', key: 'totalScore', width: 80 },
      { title: '评审结论', dataIndex: 'conclusion', key: 'conclusion', width: 100 },
      { title: '提交时间', dataIndex: 'submitTime', key: 'submitTime', width: 160 },
    ];

    return {
      activeKey,
      experts,
      results,
      averageScore,
      progressPercent,
      columns,
      getResultStatusLabel,
      getResultStatusColor,
    };
  },
};
</script>

<template>
  <div class="p-4">
    <!-- 任务基本信息 -->
    <Card title="评审任务信息" :bordered="false" class="mb-4">
      <Descriptions :column="3" bordered size="small">
        <DescriptionsItem label="任务名称">{{ task.taskName }}</DescriptionsItem>
        <DescriptionsItem label="业务类型">
          <Tag color="blue">{{ task.businessType === 1 ? '备案' : task.businessType === 2 ? '项目' : '成果' }}</Tag>
        </DescriptionsItem>
        <DescriptionsItem label="业务名称">{{ task.businessName }}</DescriptionsItem>
        <DescriptionsItem label="评审要求" :span="3">{{ task.reviewRequirement || '无' }}</DescriptionsItem>
        <DescriptionsItem label="开始时间">{{ task.startTime || '-' }}</DescriptionsItem>
        <DescriptionsItem label="截止时间">{{ task.endTime || '-' }}</DescriptionsItem>
        <DescriptionsItem label="任务状态">
          <Tag :color="task.status === 2 ? 'green' : task.status === 1 ? 'blue' : 'orange'">
            {{ task.status === 0 ? '待分配' : task.status === 1 ? '评审中' : '已完成' }}
          </Tag>
        </DescriptionsItem>
      </Descriptions>
    </Card>

    <!-- 评审统计 -->
    <Row :gutter="16" class="mb-4">
      <Col :span="6">
        <Card :bordered="false">
          <Statistic title="专家数量" :value="results.length" />
        </Card>
      </Col>
      <Col :span="6">
        <Card :bordered="false">
          <Statistic title="已提交" :value="results.filter((r) => r.status === 3).length" />
        </Card>
      </Col>
      <Col :span="6">
        <Card :bordered="false">
          <Statistic title="平均评分" :value="averageScore" suffix="分" />
        </Card>
      </Col>
      <Col :span="6">
        <Card :bordered="false">
          <Statistic title="评审结论" :value="task.reviewConclusion || '-'" />
        </Card>
      </Col>
    </Row>

    <!-- 提交进度 -->
    <Card title="评审进度" :bordered="false" class="mb-4">
      <Progress :percent="progressPercent" status="active" />
      <div class="mt-2 text-sm text-gray-500">
        已提交 {{ results.filter((r) => r.status === 3).length }} / {{ results.length }} 位专家
      </div>
    </Card>

    <!-- 专家评审结果列表 -->
    <Card title="专家评审详情" :bordered="false">
      <Table
        :columns="columns"
        :data-source="results"
        :pagination="false"
        :row-key="(record: DeclareReviewApi.ReviewResult) => record.id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag :color="getResultStatusColor(record.status)">
              {{ getResultStatusLabel(record.status) }}
            </Tag>
          </template>
          <template v-if="column.key === 'totalScore'">
            {{ record.totalScore ? record.totalScore + '分' : '-' }}
          </template>
          <template v-if="column.key === 'conclusion'">
            <Tag :color="record.conclusion === '通过' ? 'green' : record.conclusion === '需整改' ? 'orange' : 'red'">
              {{ record.conclusion || '-' }}
            </Tag>
          </template>
        </template>
      </Table>
    </Card>
  </div>
</template>
