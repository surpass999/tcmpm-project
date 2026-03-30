<script setup lang="ts">
import type { DashboardStats, DashboardTasks, ProjectTypeItem } from '#/api/declare/dashboard';
import type { DashboardStats as DashboardStatsType } from '#/api/declare/dashboard';
import type { Hospital } from '#/api/declare/hospital';
import { getHospitalPage } from '#/api/declare/hospital';
import StatisticCard from '../common/StatisticCard.vue';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import type { EchartsUIType } from '@vben/plugins/echarts';
import { PROJECT_TYPE_OPTIONS } from '#/api/declare/hospital';
import { onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const props = defineProps({
  stats: Object as PropType<DashboardStats>,
  tasks: Object as PropType<DashboardTasks>,
});

const emit = defineEmits<{
  'go-to-bpm-tasks': [];
  'go-to-project-list': [params?: Record<string, string>];
}>();

// ===== 辖区医院列表 =====
const hospitalList = ref<Hospital[]>([]);
const hospitalLoading = ref(false);
const hospitalTotal = ref(0);

async function loadHospitalList(provinceCode?: string) {
  hospitalLoading.value = true;
  try {
    const data = await getHospitalPage({
      provinceCode,
      status: 1,
      pageNo: 1,
      pageSize: 20,
    });
    hospitalList.value = data.list || [];
    hospitalTotal.value = data.total || 0;
  } catch {
    hospitalList.value = [];
  } finally {
    hospitalLoading.value = false;
  }
}

watch(
  () => props.stats?.provinceStats?.provinceCode,
  (code) => {
    if (code) {
      loadHospitalList(code);
    }
  },
  { immediate: true },
);

const hospitalColumns = [
  { title: '医院名称', dataIndex: 'hospitalName', key: 'hospitalName', ellipsis: true },
  { title: '项目类型', dataIndex: 'projectTypeName', key: 'projectTypeName', width: 460 },
  { title: '医院等级', dataIndex: 'hospitalLevel', key: 'hospitalLevel', width: 220 },
  { title: '城市', dataIndex: 'cityName', key: 'cityName', width: 400 },
  { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson', width: 100 },
];

// ===== 底部柱状图（辖区项目分型统计）=====
const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

const chartData = ref<ProjectTypeItem[]>([]);

// 确保6种类型都存在，缺失的补0
function buildChartData(items: ProjectTypeItem[]): ProjectTypeItem[] {
  return PROJECT_TYPE_OPTIONS.map((opt) => {
    const found = items.find((item) => item.typeValue === opt.value);
    return {
      typeName: opt.label,
      typeValue: opt.value,
      projectCount: found?.projectCount ?? 0,
      percentage: found?.percentage ?? 0,
      totalInvestment: 0,
      arrivedFund: 0,
    };
  });
}

watch(
  () => props.stats?.provinceStats?.projectTypeDistribution,
  (val) => {
    chartData.value = buildChartData(val && val.length > 0 ? val : []);
    if (chartRef.value) {
      renderChart(chartData.value);
    }
  },
  { immediate: true },
);

onMounted(() => {
  if (chartData.value.length > 0) {
    renderChart(chartData.value);
  }
});

function renderChart(data: ProjectTypeItem[]) {
  const xData = data.map((d) => d.typeName);
  const countData = data.map((d) => d.projectCount);

  renderEcharts({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const item = params[0];
        return `${item.name}<br/>医院数量：${item.value} 家`;
      },
    },
    grid: { left: 60, right: 20, bottom: 40, top: 20 },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: { rotate: 20, fontSize: 10 },
    },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: '{value}' },
    },
    series: [
      {
        type: 'bar',
        data: countData,
        itemStyle: { color: '#409eff' },
        label: { show: true, position: 'top', fontSize: 10 },
      },
    ],
  });
}
</script>

<template>
  <div class="province-dashboard">
    <!-- 统计卡片（3张） -->
    <a-row :gutter="20" class="stat-row">
      <a-col :span="8">
        <StatisticCard
          title="待审核任务"
          :value="stats?.provinceStats?.pendingReviewCount || 0"
          icon="ant-design:file-text-outlined"
          color="#409eff"
          clickable
          @click="router.push('/progress-report')"
        />
      </a-col>
      <a-col :span="8">
        <StatisticCard
          title="辖区项目"
          :value="stats?.provinceStats?.regionProjectCount || 0"
          icon="ant-design:bank-outlined"
          color="#67c23a"
          clickable
          @click="router.push('/progress-report')"
        />
      </a-col>
      <a-col :span="8">
        <StatisticCard
          title="已填报数"
          :value="stats?.provinceStats?.reportedHospitalCount || 0"
          icon="ant-design:check-circle-outlined"
          color="#e6a23c"
          clickable
          @click="router.push('/progress-report')"
        />
      </a-col>
    </a-row>

    <!-- 辖区医院列表 -->
    <div class="hospital-list-panel">
      <div class="panel-header">
        <h3>辖区医院</h3>
        <a-button type="link" @click="emit('go-to-project-list')">查看全部</a-button>
      </div>
      <a-table
        :columns="hospitalColumns"
        :data-source="hospitalList"
        :loading="hospitalLoading"
        :pagination="{ pageSize: 10, total: hospitalTotal }"
        :row-key="(record: Hospital) => record.id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'hospitalName'">
            <span class="hospital-name">{{ record.hospitalName }}</span>
          </template>
          <template v-else-if="column.key === 'projectTypeName'">
            <a-tag :color="'blue'">{{ record.projectTypeName || '未分型' }}</a-tag>
          </template>
        </template>
      </a-table>
      <div v-if="!hospitalLoading && hospitalList.length === 0" class="table-empty">
        <span>暂无辖区医院数据</span>
      </div>
    </div>

    <!-- 辖区项目分型统计 -->
    <div class="type-chart-panel">
      <div class="panel-header">
        <h3>辖区项目分型统计</h3>
      </div>
      <EchartsUI ref="chartRef" :style="{ height: '300px' }" />
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

.hospital-list-panel,
.type-chart-panel {
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

.table-empty {
  text-align: center;
  padding: 16px 0;
  color: #909399;
  font-size: 14px;
}

.hospital-name {
  font-weight: 500;
  color: #303133;
}
</style>
