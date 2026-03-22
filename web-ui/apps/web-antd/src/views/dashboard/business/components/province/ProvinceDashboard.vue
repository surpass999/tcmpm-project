<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { PropType } from 'vue';
import { useRouter } from 'vue-router';

import type { DashboardStats, DashboardTasks, RiskWarnings } from '#/api/declare/dashboard';
import StatisticCard from '../common/StatisticCard.vue';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import type { EchartsUIType } from '@vben/plugins/echarts';

const router = useRouter();

const props = defineProps({
  stats: Object as PropType<DashboardStats>,
  tasks: Object as PropType<DashboardTasks>,
  warnings: Object as PropType<RiskWarnings>,
});

const emit = defineEmits<{
  'go-to-bpm-tasks': [];
  'go-to-project-list': [params?: Record<string, string>];
}>();

// ECharts 完成率图表
const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

const typeCompletionData = computed(() => {
  return props.stats?.provinceStats?.projectTypeDistribution || [];
});

watch(typeCompletionData, (val) => {
  if (!val || val.length === 0) return;

  const xData = val.map((item: any) => item.typeName);
  const rateData = val.map((item: any) => item.completionRate ?? 0);
  const countData = val.map((item: any) => ({
    value: item.completedCount ?? 0,
    total: item.projectCount ?? 0
  }));

  renderEcharts({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const item = params[0];
        const data = countData[item.dataIndex];
        return `${item.name}<br/>完成率: ${item.value}%<br/>已完成: ${data.value}/${data.total}`;
      }
    },
    grid: { left: 60, right: 20, bottom: 40, top: 20 },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: { rotate: 30, fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: '{value}%' },
      max: 100
    },
    series: [{
      type: 'bar',
      data: rateData,
      itemStyle: {
        color: (params: any) => {
          const rate = params.value;
          if (rate >= 80) return '#67c23a';
          if (rate >= 50) return '#e6a23c';
          return '#f56c6c';
        }
      },
      label: { show: true, formatter: '{c}%', position: 'top', fontSize: 10 }
    }]
  });
}, { immediate: true });

function handleProvinceClick(provinceId: number) {
  emit('go-to-project-list', { province: String(provinceId) });
}

const taskColumns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', width: 180, ellipsis: true },
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', width: 280, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 100, align: 'center' as const },
];

const regionColumns = [
  { title: '地市', dataIndex: 'regionName', key: 'regionName', ellipsis: true },
  { title: '项目数', dataIndex: 'projectCount', key: 'projectCount', width: 100, align: 'center' as const },
  { title: '平均进度', dataIndex: 'progress', key: 'progress', width: 180, align: 'center' as const },
  { title: '操作', key: 'action', width: 100, align: 'center' as const },
];
</script>

<template>
  <div class="province-dashboard">
    <!-- 统计卡片 -->
    <a-row :gutter="20" class="stat-row">
      <a-col :span="6">
        <StatisticCard
          title="待审核任务"
          :value="stats?.provinceStats?.pendingReviewCount || stats?.taskCount || 0"
          icon="ant-design:file-text-outlined"
          color="#409eff"
          clickable
          @click="emit('go-to-bpm-tasks')"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="辖区项目"
          :value="stats?.provinceStats?.regionProjectCount || stats?.projectCount || 0"
          icon="ant-design:bank-outlined"
          color="#67c23a"
          clickable
          @click="emit('go-to-project-list')"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="平均进度"
          :value="stats?.provinceStats?.regionProgress || stats?.projectProgress || 0"
          suffix="%"
          icon="ant-design:line-chart-outlined"
          color="#e6a23c"
          clickable
          @click="emit('go-to-project-list')"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="高风险项目"
          :value="stats?.provinceStats?.highRiskCount || stats?.warningCount || 0"
          icon="ant-design:warning-outlined"
          :color="(stats?.provinceStats?.highRiskCount || 0) > 0 ? '#f56c6c' : '#909399'"
          clickable
          @click="emit('go-to-project-list', { warning: '1' })"
        />
      </a-col>
    </a-row>

    <!-- 待审核任务列表 -->
    <div class="task-panel">
      <div class="panel-header">
        <h3>待审核任务</h3>
        <a-button type="link" @click="emit('go-to-bpm-tasks')">查看全部</a-button>
      </div>
      <a-table
        :columns="taskColumns"
        :data-source="tasks?.bpmTasks?.items?.slice(0, 5)"
        :pagination="false"
        :row-key="(record: any) => record.taskId"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="router.push(record.jumpUrl)">
              去审核
            </a-button>
          </template>
        </template>
      </a-table>
      <div class="table-empty"><span>暂无待审核任务</span></div>
    </div>

    <!-- 辖区统计 -->
    <div class="region-stats">
      <div class="panel-header">
        <h3>辖区项目分布</h3>
        <a-button type="link" @click="emit('go-to-project-list')">查看全部</a-button>
      </div>
      <a-table
        :columns="regionColumns"
        :data-source="stats?.provinceStats?.regionDistribution || []"
        :pagination="false"
        :row-key="(record: any) => record.regionId"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'progress'">
            <div class="inline-progress">
              <div class="inline-progress-bar" :style="{ width: record.progress + '%' }"></div>
              <span>{{ record.progress }}%</span>
            </div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="handleProvinceClick(record.regionId)">
              查看
            </a-button>
          </template>
        </template>
      </a-table>
      <div class="table-empty"><span>暂无辖区数据</span></div>
    </div>

    <!-- 各项目类型完成率统计 -->
    <div class="type-completion-chart">
      <div class="panel-header">
        <h3>各项目类型完成率统计</h3>
      </div>
      <EchartsUI ref="chartRef" :style="{ height: '280px' }" />
      <div class="chart-legend">
        <span class="legend-item"><span class="dot green"></span>完成率 ≥80%</span>
        <span class="legend-item"><span class="dot yellow"></span>完成率 50-80%</span>
        <span class="legend-item"><span class="dot red"></span>完成率 &lt;50%</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.province-dashboard {
  padding: 20px;
  background-color: #f5f7fa;
}

.stat-row {
  margin-bottom: 20px;
}

.task-panel, .region-stats {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
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

.inline-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inline-progress-bar {
  height: 6px;
  flex: 1;
  max-width: 80px;
  background: linear-gradient(90deg, #409eff, #67c23a);
  border-radius: 3px;
}

.table-empty {
  text-align: center;
  padding: 16px 0;
  color: #909399;
  font-size: 14px;
}

.type-completion-chart {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 10px;
  font-size: 12px;
  color: #606266;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot.green { background: #67c23a; }
.dot.yellow { background: #e6a23c; }
.dot.red { background: #f56c6c; }
</style>
