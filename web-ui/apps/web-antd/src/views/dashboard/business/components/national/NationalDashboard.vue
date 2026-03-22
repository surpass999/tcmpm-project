<script setup lang="ts">
import { IconifyIcon } from '@vben/icons';
import { computed, onMounted, ref, watch } from 'vue';
import type { PropType } from 'vue';
import { useRouter } from 'vue-router';

import type { DashboardStats, DashboardTasks, RiskWarnings } from '#/api/declare/dashboard';
import { getNationalStats } from '#/api/declare/dashboard';
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

// 全国统计数据（独立调用接口获取全量数据）
const localNationalStats = ref<any>(null);
const nationalStatsLoading = ref(false);

async function loadNationalStats() {
  nationalStatsLoading.value = true;
  try {
    const data = await getNationalStats();
    localNationalStats.value = data;
    console.log('[NationalDashboard] 全国统计数据:', data);
  } catch (error) {
    console.error('加载全国统计数据失败:', error);
  } finally {
    nationalStatsLoading.value = false;
  }
}

onMounted(() => {
  loadNationalStats();
});



const bpmTasksItems = computed(() => props.tasks?.bpmTasks?.items ?? []);
const hasBpmTasks = computed(() => bpmTasksItems.value.length > 0);

const typeItems = computed(() => {
  return localNationalStats.value?.projectTypeDistribution
    || (props.stats?.nationalStats as any)?.projectTypeDistribution
    || [];
});

// ECharts 完成率图表
const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

const typeCompletionData = computed(() => {
  return localNationalStats.value?.projectTypeDistribution
    || (props.stats?.nationalStats as any)?.projectTypeDistribution
    || [];
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
    grid: { left: 60, right: 20, bottom: 40, top: 30 },
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
      label: {
        show: true,
        formatter: '{c}%',
        position: 'top',
        fontSize: 10
      }
    }]
  });
}, { immediate: true });

// ECharts 各省项目分布图表（新增）
const provinceChartRef = ref<EchartsUIType>();
const { renderEcharts: renderProvinceChart } = useEcharts(provinceChartRef);

const provinceData = computed(() => {
  return localNationalStats.value?.provinceDistribution
    || (props.stats?.nationalStats as any)?.provinceDistribution
    || [];
});

// 动态计算图表高度（每个省份约 35px）
const provinceChartHeight = computed(() => {
  const count = provinceData.value.length;
  return Math.max(200, Math.min(count * 35, 500)) + 'px';
});

watch(provinceData, (val) => {
  if (!val || val.length === 0) return;

  // 按项目数量降序排序
  const sortedData = [...val].sort((a, b) => b.projectCount - a.projectCount);
  const yData = sortedData.map((item: any) => item.provinceName);
  const countData = sortedData.map((item: any) => item.projectCount ?? 0);
  const progressData = sortedData.map((item: any) => item.progress ?? 0);

  renderProvinceChart({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const idx = params[0].dataIndex;
        const item = sortedData[idx];
        return `${item.provinceName}<br/>项目数: ${item.projectCount}<br/>平均进度: ${item.progress ?? 0}%`;
      }
    },
    grid: { left: 80, right: 80, bottom: 20, top: 10 },
    xAxis: {
      type: 'value',
      name: '项目数'
    },
    yAxis: {
      type: 'category',
      data: yData,
      axisLabel: { fontSize: 11 }
    },
    series: [
      {
        type: 'bar',
        data: countData,
        barWidth: 18,
        itemStyle: {
          color: (params: any) => {
            const progress = progressData[params.dataIndex];
            if (progress >= 80) return '#67c23a';
            if (progress >= 50) return '#e6a23c';
            return '#f56c6c';
          }
        },
        label: {
          show: true,
          position: 'right',
          formatter: (params: any) => {
            const progress = progressData[params.dataIndex];
            return `${params.value} | 进度${progress}%`;
          },
          fontSize: 11
        }
      }
    ]
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
</script>

<template>
  <div class="national-dashboard">
    <!-- 第一行：4 个统计卡片 -->
    <a-row :gutter="20" class="stat-row">
      <a-col :span="6">
        <StatisticCard
          title="全国项目"
          :value="localNationalStats?.totalProjectCount || stats?.nationalStats?.totalProjectCount || stats?.projectCount || 0"
          icon="ant-design:bank-outlined"
          color="#67c23a"
          clickable
          @click="emit('go-to-project-list')"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="全国平均进度"
          :value="localNationalStats?.averageProgress || stats?.nationalStats?.nationalProgress || stats?.projectProgress || 0"
          suffix="%"
          icon="ant-design:line-chart-outlined"
          color="#e6a23c"
          clickable
          @click="emit('go-to-project-list')"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="资金执行率"
          :value="localNationalStats?.totalFundRate || stats?.nationalStats?.totalFundRate || stats?.fundExecutionRate || 0"
          suffix="%"
          icon="ant-design:account-book-outlined"
          color="#9c27b0"
          clickable
          @click="emit('go-to-project-list')"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="中央转移资金三年总额"
          :value="((localNationalStats?.centralFundThreeYearTotal || (stats?.nationalStats as any)?.centralFundThreeYearTotal) || 0) / 10000"
          icon="ant-design:credit-card-outlined"
          suffix="亿"
          :precision="2"
          color="#1d39c4"
        />
      </a-col>
    </a-row>

    <!-- 第二行：4 个统计卡片 -->
    <a-row :gutter="20" class="stat-row-2">
      <a-col :span="6">
        <StatisticCard
          title="已中期评估"
          :value="localNationalStats?.midtermProjectCount || (stats?.nationalStats as any)?.midtermProjectCount || 0"
          icon="ant-design:audit-outlined"
          color="#909399"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="已验收项目"
          :value="localNationalStats?.acceptedProjectCount || (stats?.nationalStats as any)?.acceptedProjectCount || 0"
          icon="ant-design:check-circle-outlined"
          color="#67c23a"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="到账资金总额"
          :value="((localNationalStats?.centralFundArriveTotal || (stats?.nationalStats as any)?.centralFundArriveTotal) || 0) / 10000"
          suffix="亿"
          :precision="2"
          icon="ant-design:account-book-outlined"
          color="#c8960c"
        />
      </a-col>
      <a-col :span="6">
        <div class="type-grid-wrap">

          <div class="type-grid-header">
            <IconifyIcon icon="ant-design:pie-chart-outlined" class="header-icon" />          
          </div>
          <div class="type-grid">
            <div
              v-for="item in typeItems"
              :key="item.typeValue"
              class="type-item"
            >
              <span class="type-name">{{ item.typeName }}</span>
              <span class="type-count">{{ item.projectCount ?? 0 }}</span>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 待审批任务列表 -->
    <div class="task-panel">
      <div class="panel-header">
        <h3>待审批任务</h3>
        <a-button type="link" @click="emit('go-to-bpm-tasks')">查看全部</a-button>
      </div>
      <a-table
        :columns="taskColumns"
        :data-source="props.tasks?.bpmTasks?.items?.slice(0, 5)"
        :pagination="false"
        :row-key="(record: any) => record.taskId"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="router.push(record.jumpUrl)">
              去审批
            </a-button>
          </template>
        </template>
      </a-table>
      <div v-if="!hasBpmTasks" class="table-empty"><span>暂无待审批任务</span></div>
    </div>

    <!-- 全国项目分布（图表展示） -->
    <div class="national-stats" v-loading="nationalStatsLoading">
      <div class="panel-header">
        <h3>全国项目分布（按项目数量排序）</h3>
        <a-button type="link" @click="emit('go-to-project-list')">查看全部</a-button>
      </div>
      <EchartsUI ref="provinceChartRef" :style="{ height: provinceChartHeight }" />
      <div class="province-chart-legend">
        <span class="legend-item"><span class="dot green"></span>进度 ≥80%</span>
        <span class="legend-item"><span class="dot yellow"></span>进度 50-80%</span>
        <span class="legend-item"><span class="dot red"></span>进度 &lt;50%</span>
      </div>
    </div>

    <!-- 各项目类型完成率统计 -->
    <div class="type-completion-chart" v-loading="nationalStatsLoading">
      <div class="panel-header">
        <h3>各项目类型完成率统计</h3>
      </div>
      <EchartsUI ref="chartRef" :style="{ height: '320px' }" />
      <div class="chart-legend">
        <span class="legend-item"><span class="dot green"></span>完成率 ≥80%</span>
        <span class="legend-item"><span class="dot yellow"></span>完成率 50-80%</span>
        <span class="legend-item"><span class="dot red"></span>完成率 &lt;50%</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.national-dashboard {
  padding: 20px;
  background-color: #f5f7fa;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-row-2 {
  margin-bottom: 20px;
}

.type-grid-wrap {
  display: flex;
  flex-direction: row;
  justify-content: start;
  align-items: center;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  height: 100%;
}

.type-grid-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 16px;
  font-size: 14px;
  color: #909399;
  font-weight: 600;
}

.header-icon {
  width: 60px;
  height: 60px;
  font-size: 16px;
  color: #f5222d;
}

.type-grid-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 16px;
  font-size: 14px;
  color: #909399;
  font-weight: 600;
}

.header-icon {
  width: 60px;
  height: 60px;
  font-size: 16px;
  color: #f5222d;
}

.type-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 12px;
}

.type-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  /* background: #fafafa; */
  /* border-radius: 4px; */
  /* padding: 8px 12px; */
  font-size: 10px;
}

.type-name {
  color: #606266;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  margin-right: 8px;
}

.type-count {
  font-weight: 600;
  color: #303133;
  flex-shrink: 0;
}

.task-panel, .national-stats {
  min-height: 300px;
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

.province-chart-legend {
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
