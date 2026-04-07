<script setup lang="ts">
import { IconifyIcon } from '@vben/icons';
import { computed, onMounted, ref, watch } from 'vue';
import type { PropType } from 'vue';
import { useRouter } from 'vue-router';

import type { DashboardStats, DashboardTasks, RiskWarnings, ReportWindowStatsVO } from '#/api/declare/dashboard';
import { getNationalStats, getReportWindowStats } from '#/api/declare/dashboard';
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

// 全国统计数据
const localNationalStats = ref<any>(null);
const nationalStatsLoading = ref(false);

// 填报窗口统计数据
const windowStats = ref<ReportWindowStatsVO | null>(null);

async function loadNationalStats() {
  nationalStatsLoading.value = true;
  try {
    const data = await getNationalStats();
    localNationalStats.value = data;
  } catch (error) {
    console.error('加载全国统计数据失败:', error);
  } finally {
    nationalStatsLoading.value = false;
  }
}

async function loadWindowStats() {
  try {
    const data = await getReportWindowStats();
    console.log('[Dashboard] 填报窗口 stats 原始数据:', data);
    windowStats.value = data;
    console.log('[Dashboard] windowStats.value:', windowStats.value);
    console.log('[Dashboard] hasOpenWindow:', windowStats.value?.hasOpenWindow);
  } catch (error) {
    console.error('加载填报窗口统计失败:', error);
  }
}

onMounted(() => {
  loadNationalStats();
  loadWindowStats();
});

const bpmTasksItems = computed(() => props.tasks?.bpmTasks?.items ?? []);
const hasBpmTasks = computed(() => bpmTasksItems.value.length > 0);

// 6种项目类型网格数据
const typeItems = computed(() => {
  return localNationalStats.value?.projectTypeDistribution
    || (props.stats?.nationalStats as any)?.projectTypeDistribution
    || [];
});

// 各省分布数据
const provinceData = computed(() => {
  return localNationalStats.value?.provinceDistribution
    || (props.stats?.nationalStats as any)?.provinceDistribution
    || [];
});

// 动态计算省分布图高度
const provinceChartHeight = computed(() => {
  const count = provinceData.value.length;
  return Math.max(200, Math.min(count * 35, 500)) + 'px';
});

// ========== 各省医院分布图表 ==========
const provinceChartRef = ref<EchartsUIType>();
const { renderEcharts: renderProvinceChart } = useEcharts(provinceChartRef);

watch(provinceData, (val) => {
  if (!val || val.length === 0) return;

  const sortedData = [...val].sort((a: any, b: any) => b.projectCount - a.projectCount);
  const yData = sortedData.map((item: any) => item.provinceName);
  const countData = sortedData.map((item: any) => item.projectCount ?? 0);

  renderProvinceChart({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const idx = params[0].dataIndex;
        const item = sortedData[idx];
        return `${item.provinceName}<br/>医院数: ${item.projectCount}`;
      }
    },
    grid: { left: 80, right: 80, bottom: 20, top: 10 },
    xAxis: {
      type: 'value',
      name: '医院数'
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
          color: '#409eff'
        },
        label: {
          show: true,
          position: 'right',
          formatter: (params: any) => `${params.value}家`,
          fontSize: 11
        }
      }
    ]
  });
}, { immediate: true });

// ========== 各项目类型医院数量分布图表 ==========
const typeChartRef = ref<EchartsUIType>();
const { renderEcharts: renderTypeChart } = useEcharts(typeChartRef);

const typeChartData = computed(() => {
  return localNationalStats.value?.projectTypeDistribution
    || (props.stats?.nationalStats as any)?.projectTypeDistribution
    || [];
});

watch(typeChartData, (val) => {
  if (!val || val.length === 0) return;

  const xData = val.map((item: any) => item.typeName);
  const countData = val.map((item: any) => item.projectCount ?? 0);
  const percentageData = val.map((item: any) => item.percentage ?? 0);

  // 按数量降序确定颜色
  const sortedCounts = [...countData].sort((a: number, b: number) => b - a);
  const maxCount = sortedCounts[0] || 1;
  const colors = countData.map((count: number) => {
    if (count >= maxCount * 0.8) return '#67c23a';
    if (count >= maxCount * 0.5) return '#e6a23c';
    return '#f56c6c';
  });

  renderTypeChart({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const idx = params[0].dataIndex;
        const typeItem = val[idx];
        return `${params[0].name}<br/>医院数: ${params[0].value}<br/>占比: ${typeItem.percentage ?? 0}%`;
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
      axisLabel: { formatter: '{value}家' }
    },
    series: [{
      type: 'bar',
      data: countData,
      itemStyle: {
        color: (params: any) => colors[params.dataIndex]
      },
      label: {
        show: true,
        formatter: '{c}',
        position: 'top',
        fontSize: 10
      }
    }]
  });
}, { immediate: true });

// ========== 计算属性 ==========

// 全国医院总数
const totalHospitalCount = computed(() => {
  return localNationalStats.value?.totalProjectCount
    || (props.stats?.nationalStats as any)?.totalProjectCount
    || props.stats?.projectCount
    || 0;
});

// 填报进度（已填报/全国医院）
const reportProgress = computed(() => {
  const reported = windowStats.value?.reportedHospitalCount ?? 0;
  const total = windowStats.value?.totalHospitalCount ?? totalHospitalCount.value;
  if (!total) return 0;
  return Math.round(reported * 100 / total);
});

// 填报窗口状态描述
const windowStatusText = computed(() => {
  if (windowStats.value?.hasOpenWindow) {
    const { startDate, endDate } = windowStats.value;
    // 支持两种格式：ISO字符串 或 时间戳数字
    const fmt = (d: string | number | undefined | null) => {
      if (d == null) return '';
      if (typeof d === 'string') return d.substring(0, 10);
      if (typeof d === 'number') return new Date(d).toISOString().substring(0, 10);
      return '';
    };
    return `${fmt(startDate)} ~ ${fmt(endDate)}`;
  }
  return '已关闭';
});

// 填报批次描述
const batchText = computed(() => {
  if (!windowStats.value?.hasOpenWindow) return '暂无开放窗口';
  return `第${windowStats.value?.currentBatch}批 · ${windowStats.value?.reportYear}年`;
});

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
          title="全国医院数"
          :value="totalHospitalCount"
          icon="ant-design:bank-outlined"
          color="#67c23a"
          clickable
          @click="router.push('/declare/hospital')"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="填报进度"
          :value="reportProgress"
          suffix="%"
          icon="ant-design:line-chart-outlined"
          color="#e6a23c"
          clickable
          @click="router.push('/progress-report')"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          :title="windowStats?.hasOpenWindow ? '填报窗口状态' : '填报窗口状态'"
          :value="windowStats?.hasOpenWindow ? '开放中' : '已关闭'"
          icon="ant-design:calendar-outlined"
          :color="windowStats?.hasOpenWindow ? '#67c23a' : '#f56c6c'"
          :extra="windowStats?.hasOpenWindow ? windowStatusText : ''"
          @click="router.push('/declare/report-window')"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="填报批次"
          :value="batchText"
          icon="ant-design:form-outlined"
          color="#409eff"
          @click="router.push('/declare/report-window')"
        />
      </a-col>
    </a-row>

    <!-- 第二行：4 个统计卡片 -->
    <a-row :gutter="20" class="stat-row-2">
      <a-col :span="6">
        <StatisticCard
          title="已填报医院"
          :value="`${windowStats?.reportedHospitalCount ?? 0} / ${windowStats?.totalHospitalCount ?? 0}`"
          icon="ant-design:check-circle-outlined"
          color="#67c23a"
          clickable
          @click="router.push('/progress-report')"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="总填报记录"
          :value="localNationalStats?.midtermProjectCount ?? 0"
          icon="ant-design:file-text-outlined"
          color="#409eff"
          clickable
          @click="router.push('/progress-report')"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="待审核"
          :value="localNationalStats?.acceptedProjectCount ?? 0"
          icon="ant-design:audit-outlined"
          color="#e6a23c"
          clickable
          @click="router.push('/progress-report')"
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
              <span class="type-count">{{ item.projectCount ?? 0 }}家</span>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 待审批任务列表 -->
    <!-- <div class="task-panel">
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
    </div> -->


    <!-- 各项目类型医院数量分布图表 -->
    <!-- <div class="type-completion-chart" v-loading="nationalStatsLoading">
      <div class="panel-header">
        <h3>各项目类型医院数量分布</h3>
      </div>
      <EchartsUI ref="typeChartRef" :style="{ height: '320px' }" />
      <div class="chart-legend">
        <span class="legend-item"><span class="dot green"></span>数量较多</span>
        <span class="legend-item"><span class="dot yellow"></span>数量中等</span>
        <span class="legend-item"><span class="dot red"></span>数量较少</span>
      </div>
    </div> -->

    <!-- 各省医院分布图表 -->   <!-- <a-button type="link" @click="emit('go-to-project-list')">查看全部</a-button> -->
    <!-- <div class="national-stats" v-loading="nationalStatsLoading">
      <div class="panel-header">
        <h3>各省医院分布（按医院数量排序）</h3>
       
      </div>
      <EchartsUI ref="provinceChartRef" :style="{ height: provinceChartHeight }" />
    </div> -->

    
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
