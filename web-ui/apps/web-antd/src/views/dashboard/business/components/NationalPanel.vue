<script lang="ts" setup>
import { ref } from 'vue';

defineOptions({ name: 'NationalPanel' });

// 统计数据
const statistics = ref([
  { label: '全国医院总数', value: 1256, icon: 'hospital', color: 'primary' },
  { label: '在运行项目', value: 892, icon: 'project-diagram', color: 'primary' },
  { label: '待国家审批', value: 45, icon: 'clock', color: 'danger' },
  { label: '专家论证中', value: 18, icon: 'users', color: 'warning' },
]);

// 全国统计图表数据
const nationalStats = ref([
  { name: '北京市', total: 45, excellent: 12, good: 28, pass: 5 },
  { name: '上海市', total: 38, excellent: 10, good: 22, pass: 6 },
  { name: '广东省', total: 52, excellent: 15, good: 30, pass: 7 },
  { name: '江苏省', total: 41, excellent: 11, good: 25, pass: 5 },
  { name: '浙江省', total: 35, excellent: 9, good: 20, pass: 6 },
  { name: '四川省', total: 48, excellent: 13, good: 28, pass: 7 },
]);

// 待国家局审批任务
const pendingTasks = ref({
  filing: {
    title: '备案审批',
    count: 18,
    tasks: [
      { id: 1, name: 'XX省中医医院综合型试点项目', province: '北京市', date: '2023-11-15', priority: 'high' },
      { id: 2, name: 'XX市中西医结合医院特色专科试点', province: '上海市', date: '2023-11-14', priority: 'normal' },
      { id: 3, name: 'XX县中医院基层服务能力提升项目', province: '广东省', date: '2023-11-12', priority: 'normal' },
    ],
  },
  expert: {
    title: '专家论证',
    count: 15,
    tasks: [
      { id: 1, name: 'XX省中医药大学附属医院项目论证', province: '江苏省', date: '2023-11-15', expertCount: 5 },
      { id: 2, name: 'XX市中医医院智慧药房项目论证', province: '浙江省', date: '2023-11-14', expertCount: 3 },
    ],
  },
  finalAcceptance: {
    title: '最终验收',
    count: 12,
    tasks: [
      { id: 1, name: 'XX省中西医结合医院项目验收', province: '四川省', date: '2023-11-15', type: '正式验收' },
    ],
  },
});

// 风险预警
const riskWarnings = ref([
  {
    id: 1,
    province: '广东省',
    project: 'XX市中医医院项目',
    issue: '省级审批超时 5 天',
    level: 'warning',
  },
  {
    id: 2,
    province: '江苏省',
    project: 'XX县中医院项目',
    issue: '专家论证意见分歧较大',
    level: 'danger',
  },
]);

// 项目类型分布
const projectTypeDist = ref([
  { name: '智慧中药房型', value: 320, percent: 28 },
  { name: '综合型', value: 280, percent: 24 },
  { name: '智慧医疗型', value: 256, percent: 22 },
  { name: '名老中医传承型', value: 180, percent: 16 },
  { name: '其他', value: 112, percent: 10 },
]);

// 表格列配置
const provinceColumns = [
  { title: '省份', dataIndex: 'name', key: 'name' },
  { title: '项目总数', dataIndex: 'total', key: 'total' },
  { title: '优秀', dataIndex: 'excellent', key: 'excellent' },
  { title: '良好', dataIndex: 'good', key: 'good' },
  { title: '合格', dataIndex: 'pass', key: 'pass' },
  { title: '优秀率', key: 'rating' },
];
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 页面标题区 -->
    <div class="container mx-auto px-5 py-5">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between">
        <h2 class="text-xl font-bold mb-4 md:mb-0" :style="{ color: 'var(--color-primary, #2A5C45)' }">
          国家局智慧中医医院试点项目监管控制台
        </h2>
        <div class="flex items-center space-x-3">
          <a-button class="flex items-center">
            <template #icon><i class="fas fa-sync-alt mr-2"></i></template>
            刷新数据
          </a-button>
          <a-range-picker class="w-60" />
        </div>
      </div>
    </div>

    <!-- 核心功能区块 -->
    <div class="container mx-auto px-5 pb-10">
      <!-- 统计卡片 -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-5">
        <div
          v-for="stat in statistics"
          :key="stat.label"
          class="bg-white rounded-lg border border-gray-200 shadow-sm p-4 hover:shadow-md transition-shadow cursor-pointer"
        >
          <div class="flex items-center justify-between mb-2">
            <div
              class="w-12 h-12 rounded-lg flex items-center justify-center"
              :style="{ backgroundColor: stat.color === 'danger' ? 'var(--color-error-light-9, #fff2f0)' : stat.color === 'warning' ? 'var(--color-warning-light-9, #fffbe6)' : 'var(--color-primary-light-9, #E6EEE8)' }"
            >
              <i
                :class="`fas fa-${stat.icon} text-xl`"
                :style="{ color: stat.color === 'danger' ? 'var(--color-error, #ff4d4f)' : stat.color === 'warning' ? 'var(--color-warning, #faad14)' : 'var(--color-primary, #2A5C45)' }"
              ></i>
            </div>
          </div>
          <div class="text-2xl font-bold text-gray-800 mb-1">{{ stat.value }}</div>
          <div class="text-sm text-gray-500">{{ stat.label }}</div>
        </div>
      </div>

      <!-- 待审批任务区和风险预警区 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-5 mb-5">
        <!-- 待国家局审批 -->
        <div class="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
          <div
            class="px-5 py-3 border-b border-gray-200 flex items-center justify-between"
            :style="{ backgroundColor: 'var(--color-primary-light-9, #E6EEE8)' }"
          >
            <h3 class="font-bold flex items-center" :style="{ color: 'var(--color-primary, #2A5C45)' }">
              <i class="fas fa-gavel mr-2"></i>
              待国家局审批
            </h3>
            <span class="font-medium" style="color: var(--color-error, #ff4d4f)">待办：45 件</span>
          </div>
          <div class="p-4">
            <!-- 备案审批 -->
            <div class="mb-4">
              <h4 class="font-semibold text-gray-800 flex items-center mb-3">
                <i class="fas fa-file-alt mr-2" :style="{ color: 'var(--color-primary, #2A5C45)' }"></i>
                备案审批
                <a-tag class="ml-2" color="green">待办数：18</a-tag>
              </h4>
              <div class="space-y-3 max-h-48 overflow-y-auto">
                <div
                  v-for="task in pendingTasks.filing.tasks"
                  :key="task.id"
                  class="p-3 bg-white border border-gray-200 rounded-md hover:bg-gray-50 transition-colors cursor-pointer"
                >
                  <div class="flex justify-between items-start">
                    <div>
                      <p class="font-medium text-gray-800">{{ task.name }}</p>
                      <p class="text-sm text-gray-500 mt-1">申报省份：{{ task.province }}</p>
                    </div>
                    <a-button type="primary" size="small">审批</a-button>
                  </div>
                  <div class="flex justify-between text-sm text-gray-400 mt-2">
                    <span>提交时间：{{ task.date }}</span>
                    <a-tag :color="task.priority === 'high' ? 'red' : 'default'" size="small">
                      {{ task.priority === 'high' ? '紧急' : '普通' }}
                    </a-tag>
                  </div>
                </div>
              </div>
            </div>

            <!-- 专家论证 -->
            <div class="mb-4">
              <h4 class="font-semibold text-gray-800 flex items-center mb-3">
                <i class="fas fa-users mr-2" :style="{ color: 'var(--color-primary, #2A5C45)' }"></i>
                专家论证
                <a-tag class="ml-2" color="orange">待办数：15</a-tag>
              </h4>
              <div class="space-y-3 max-h-48 overflow-y-auto">
                <div
                  v-for="task in pendingTasks.expert.tasks"
                  :key="task.id"
                  class="p-3 bg-white border border-gray-200 rounded-md hover:bg-gray-50 transition-colors cursor-pointer"
                >
                  <div class="flex justify-between items-start">
                    <div>
                      <p class="font-medium text-gray-800">{{ task.name }}</p>
                      <p class="text-sm text-gray-500 mt-1">申报省份：{{ task.province }}</p>
                    </div>
                    <a-button type="primary" size="small">查看</a-button>
                  </div>
                  <div class="flex justify-between text-sm text-gray-400 mt-2">
                    <span>提交时间：{{ task.date }}</span>
                    <span>专家数：{{ task.expertCount }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 最终验收 -->
            <div>
              <h4 class="font-semibold text-gray-800 flex items-center mb-3">
                <i class="fas fa-check-circle mr-2" :style="{ color: 'var(--color-primary, #2A5C45)' }"></i>
                最终验收
                <a-tag class="ml-2" color="green">待办数：12</a-tag>
              </h4>
              <div class="space-y-3 max-h-48 overflow-y-auto">
                <div
                  v-for="task in pendingTasks.finalAcceptance.tasks"
                  :key="task.id"
                  class="p-3 bg-white border border-gray-200 rounded-md hover:bg-gray-50 transition-colors cursor-pointer"
                >
                  <div class="flex justify-between items-start">
                    <div>
                      <p class="font-medium text-gray-800">{{ task.name }}</p>
                      <p class="text-sm text-gray-500 mt-1">申报省份：{{ task.province }}</p>
                    </div>
                    <a-button type="primary" size="small">验收</a-button>
                  </div>
                  <div class="flex justify-between text-sm text-gray-400 mt-2">
                    <span>提交时间：{{ task.date }}</span>
                    <span>验收类型：{{ task.type }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 风险预警区 -->
        <div class="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
          <div class="bg-red-50 px-5 py-3 border-b border-gray-200">
            <h3 class="font-bold flex items-center text-red-700">
              <i class="fas fa-exclamation-triangle mr-2"></i>
              全国风险预警
            </h3>
          </div>
          <div class="p-4">
            <div class="space-y-3">
              <div
                v-for="warning in riskWarnings"
                :key="warning.id"
                :class="warning.level === 'danger' ? 'bg-red-50 border-red-200' : 'bg-amber-50 border-amber-200'"
                class="p-3 border rounded-md"
              >
                <div class="flex items-start">
                  <i :class="`fas fa-exclamation-circle mt-1 mr-3 ${warning.level === 'danger' ? 'text-red-600' : 'text-amber-600'}`"></i>
                  <div class="flex-grow">
                    <p class="font-medium text-gray-800">{{ warning.project }}</p>
                    <p class="text-sm text-gray-500 mt-1">{{ warning.issue }}</p>
                    <p class="text-xs text-gray-400 mt-1">所属省份：{{ warning.province }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 全国统计和项目类型分布 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
        <!-- 各省项目统计 -->
        <div class="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
          <div
            class="px-5 py-3 border-b border-gray-200"
            :style="{ backgroundColor: 'var(--color-primary-light-9, #E6EEE8)' }"
          >
            <h3 class="font-bold flex items-center" :style="{ color: 'var(--color-primary, #2A5C45)' }">
              <i class="fas fa-chart-bar mr-2"></i>
              各省项目统计
            </h3>
          </div>
          <div class="p-4">
            <a-table :columns="provinceColumns" :data-source="nationalStats" :pagination="false" size="small">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'rating'">
                  <a-progress :percent="Math.round((record.excellent / record.total) * 100)" size="small" />
                </template>
              </template>
            </a-table>
          </div>
        </div>

        <!-- 项目类型分布 -->
        <div class="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
          <div
            class="px-5 py-3 border-b border-gray-200"
            :style="{ backgroundColor: 'var(--color-primary-light-9, #E6EEE8)' }"
          >
            <h3 class="font-bold flex items-center" :style="{ color: 'var(--color-primary, #2A5C45)' }">
              <i class="fas fa-chart-pie mr-2"></i>
              项目类型分布
            </h3>
          </div>
          <div class="p-4">
            <div class="space-y-4">
              <div v-for="type in projectTypeDist" :key="type.name">
                <div class="flex justify-between mb-1">
                  <span class="text-sm text-gray-600">{{ type.name }}</span>
                  <span class="text-sm font-medium text-gray-800">{{ type.value }} ({{ type.percent }}%)</span>
                </div>
                <div class="w-full bg-gray-100 rounded-full h-2">
                  <div
                    class="h-2 rounded-full"
                    :style="{ width: `${type.percent}%`, backgroundColor: 'var(--color-primary, #2A5C45)' }"
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

