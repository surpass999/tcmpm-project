<script setup lang="ts">
import { computed } from 'vue';
import type { PropType } from 'vue';
import { useRouter } from 'vue-router';

import type { DashboardStats, DashboardTasks } from '#/api/declare/dashboard';
import StatisticCard from '../common/StatisticCard.vue';

const props = defineProps({
  stats: Object as PropType<DashboardStats>,
  tasks: Object as PropType<DashboardTasks>,
});

const router = useRouter();

const taskColumns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', ellipsis: true },
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', width: 150, ellipsis: true },
  { title: '报告类型', dataIndex: 'processTitle', key: 'processTitle', width: 150 },
  { title: '截止时间', dataIndex: 'deadline', key: 'deadline', width: 160 },
  { title: '操作', key: 'action', width: 100, align: 'center' as const },
];

// 获取待评审任务列表
const pendingTasks = computed(() => {
  if (!props.tasks) return [];
  // 优先使用 bpmTasks，如果没有则使用 draftTasks
  const items = props.tasks.bpmTasks?.items?.length > 0
    ? props.tasks.bpmTasks.items
    : props.tasks.draftTasks?.items || [];
  return items.slice(0, 10);
});
</script>

<template>
  <div class="expert-dashboard">
    <!-- 统计卡片 -->
    <a-row :gutter="20" class="stat-row">
      <a-col :span="8">
        <StatisticCard
          title="待评审任务"
          :value="stats?.expertStats?.pendingReviewCount || stats?.taskCount || 0"
          icon="ant-design:file-text-outlined"
          color="#409eff"
        />
      </a-col>
      <a-col :span="8">
        <StatisticCard
          title="已完成评审"
          :value="stats?.expertStats?.completedReviewCount || 0"
          icon="ant-design:check-circle-outlined"
          color="#67c23a"
        />
      </a-col>
      <a-col :span="8">
        <StatisticCard
          title="本月评审"
          :value="stats?.expertStats?.monthlyReviewCount || 0"
          icon="ant-design:calendar-outlined"
          color="#e6a23c"
        />
      </a-col>
    </a-row>

    <!-- 待评审任务列表 -->
    <div class="task-panel">
      <div class="panel-header">
        <h3>待评审任务</h3>
      </div>
      <a-table
        v-if="pendingTasks.length > 0"
        :columns="taskColumns"
        :data-source="pendingTasks"
        :pagination="false"
        :row-key="(record: any) => record.taskId"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-button
              type="link"
              size="small"
              @click="router.push(record.jumpUrl)"
            >
              去评审
            </a-button>
          </template>
        </template>
      </a-table>
      <div v-else class="table-empty">
        <a-empty description="暂无待评审任务" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.expert-dashboard {
  padding: 20px;
  background-color: #f5f7fa;
}

.stat-row {
  margin-bottom: 20px;
}

.task-panel {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.table-empty {
  text-align: center;
  padding: 16px 0;
  color: #909399;
  font-size: 14px;
}
</style>
