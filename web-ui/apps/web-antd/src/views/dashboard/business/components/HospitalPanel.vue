<script lang="ts" setup>
import { ref } from 'vue';

// 模拟数据
const todoList = ref([
  {
    id: 1,
    title: '待填报 2025 年上半年项目绩效考核指标调查表（智慧中药房型）',
    deadline: '2025-07-25',
    priority: 'urgent',
    icon: 'file-alt',
  },
  {
    id: 2,
    title: '待发起新增备案申请',
    deadline: '2025-08-10',
    priority: 'normal',
    icon: 'file-signature',
  },
  {
    id: 3,
    title: '待填写项目建设过程数据',
    deadline: '2025-07-30',
    priority: 'urgent',
    icon: 'chart-line',
  },
  {
    id: 4,
    title: '待确认中期评估整改通知',
    deadline: '2025-07-20',
    priority: 'normal',
    icon: 'check-circle',
  },
]);

const projectList = ref([
  {
    id: 1,
    name: '智慧中药房型',
    status: '建设中',
    statusColor: 'primary',
    progress: 72,
    progressLabel: '中药调剂效率',
  },
  {
    id: 2,
    name: '名老中医传承型',
    status: '待中期评估',
    statusColor: 'secondary',
    progress: 0,
    progressLabel: '覆盖工作室',
  },
  {
    id: 3,
    name: '中医电子病历型',
    status: '验收准备中',
    statusColor: 'secondary',
    progress: 0,
    progressLabel: '四诊仪采集覆盖率',
  },
]);

const buildProgress = ref([
  { name: '系统功能建设', progress: 82 },
  { name: '高质量数据集建设', progress: 68 },
  { name: '信息安全备案', progress: 95 },
  { name: '成果转化', progress: 55 },
]);

const fundData = ref([
  { label: '中央转移支付到位率', value: '100%', sub: '200 万元' },
  { label: '预算执行率', value: '76%', sub: '304/400 万元' },
  { label: '转移支付执行率', value: '72%', sub: '144/200 万元' },
]);

const notices = ref([
  {
    id: 1,
    title: '《2025 年下半年填报工作通知》',
    date: '2025-07-15',
    source: '国家中医药管理局',
  },
  {
    id: 2,
    title: '《中医药高质量数据集建设规范（2025 修订版）》',
    date: '2025-07-10',
    source: '国家局',
  },
  {
    id: 3,
    title: '《智慧中药房系统功能补充要求》',
    date: '2025-07-05',
    source: '省级主管部门',
  },
]);

const warnings = ref([
  {
    id: 1,
    type: 'danger',
    title: '智慧中药房资金执行率 58%（低于 60%）',
    project: '智慧中药房型',
    icon: 'exclamation-circle',
  },
  {
    id: 2,
    type: 'warning',
    title: '高质量数据集未完成清洗标注',
    project: '名老中医传承型',
    icon: 'exclamation-triangle',
  },
]);

const quickEntries = ref([
  { name: '备案管理', icon: 'file-alt' },
  { name: '项目填报', icon: 'pen-alt' },
  { name: '年度总结', icon: 'book-open' },
  { name: '中期评估', icon: 'chart-line' },
  { name: '数据登记', icon: 'database' },
  { name: '验收申请', icon: 'check-circle' },
]);

const getPriorityClass = (priority: string) => {
  return priority === 'urgent'
    ? 'bg-red-500 text-white'
    : 'bg-amber-100 text-amber-700';
};

const getStatusClass = (color: string) => {
  return color === 'primary' ? 'text-[var(--color-primary)]' : 'text-amber-600';
};
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 第一行：我的待办 + 项目概览 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 px-5 pt-5">
      <!-- 我的待办 -->
      <div class="md:col-span-2 bg-white rounded-lg border border-gray-200 shadow-sm">
        <div
          class="px-5 py-3 border-b border-gray-200 rounded-t-lg flex items-center justify-between"
          :style="{ backgroundColor: 'var(--color-primary-light-9, #E6EEE8)' }"
        >
          <h3 class="font-bold flex items-center" :style="{ color: 'var(--color-primary, #2A5C45)' }">
            <i class="fas fa-clipboard-list mr-2"></i>
            我的待办
          </h3>
        </div>
        <div class="p-4 space-y-3">
          <div
            v-for="item in todoList"
            :key="item.id"
            class="flex items-start p-3 border border-gray-200 rounded-md hover:bg-gray-50 transition-colors cursor-pointer"
          >
            <input type="checkbox" class="mt-1 mr-3 rounded border-gray-300" />
            <div class="flex-grow">
              <div class="flex items-center">
                <i :class="`fas fa-${item.icon} mr-2`" :style="{ color: 'var(--color-primary, #2A5C45)' }"></i>
                <span class="text-gray-800">{{ item.title }}</span>
              </div>
              <div class="flex items-center mt-1 text-sm">
                <span class="text-gray-500 mr-3">截止时间：{{ item.deadline }}</span>
                <span :class="getPriorityClass(item.priority)" class="text-xs px-2 py-1 rounded-full">
                  {{ item.priority === 'urgent' ? '紧急' : '一般' }}
                </span>
              </div>
            </div>
          </div>
        </div>
        <div class="p-3 text-right border-t border-gray-100">
          <a href="javascript:void(0);" class="hover:underline text-sm" :style="{ color: 'var(--color-primary, #2A5C45)' }">
            查看全部待办
          </a>
        </div>
      </div>

      <!-- 项目概览 -->
      <div class="bg-white rounded-lg border border-gray-200 shadow-sm">
        <div
          class="px-5 py-3 border-b border-gray-200 rounded-t-lg flex items-center justify-between"
          :style="{ backgroundColor: 'var(--color-primary-light-9, #E6EEE8)' }"
        >
          <h3 class="font-bold flex items-center" :style="{ color: 'var(--color-primary, #2A5C45)' }">
            <i class="fas fa-project-diagram mr-2"></i>
            项目概览
          </h3>
        </div>
        <div class="p-4 space-y-4">
          <div
            v-for="project in projectList"
            :key="project.id"
            class="p-3 border border-gray-200 rounded-md hover:shadow-md transition-shadow cursor-pointer"
          >
            <div class="flex justify-between items-start mb-2">
              <h4 class="font-medium text-gray-800">{{ project.name }}</h4>
              <span :class="getStatusClass(project.statusColor)" class="text-sm font-medium">
                {{ project.status }}
              </span>
            </div>
            <div class="space-y-1 text-sm">
              <p class="text-gray-500">
                {{ project.progressLabel }}：
                <span class="font-medium text-gray-700">{{ project.progress || '0' }}{{ project.progress ? '%' : '个' }}</span>
              </p>
            </div>
            <div v-if="project.progress > 0" class="mt-2">
              <div class="w-full bg-gray-100 rounded-full h-2">
                <div class="h-2 rounded-full" :style="{ width: `${project.progress}%`, backgroundColor: 'var(--color-primary, #2A5C45)' }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 第二行：建设进度与资金分析 -->
    <div class="px-5 pt-5">
      <div class="bg-white rounded-lg border border-gray-200 shadow-sm p-5">
        <h3 class="font-bold text-lg mb-6" :style="{ color: 'var(--color-primary, #2A5C45)' }">
          建设进度与资金分析
        </h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <!-- 左侧：建设进度 -->
          <div>
            <h4 class="text-gray-800 font-medium mb-4">建设进度</h4>
            <div class="space-y-4">
              <div v-for="item in buildProgress" :key="item.name">
                <div class="flex justify-between mb-1">
                  <span class="text-sm text-gray-600">{{ item.name }}</span>
                  <span class="text-sm font-medium text-gray-700">{{ item.progress }}%</span>
                </div>
                <div class="w-full bg-gray-100 rounded-full h-2">
                  <div
                    class="h-2 rounded-full"
                    :style="{ width: `${item.progress}%`, backgroundColor: 'var(--color-primary, #2A5C45)' }"
                  ></div>
                </div>
              </div>
            </div>
          </div>
          <!-- 右侧：资金分析 -->
          <div>
            <h4 class="text-gray-800 font-medium mb-4">资金分析</h4>
            <div class="grid grid-cols-2 gap-3 mb-4">
              <div v-for="fund in fundData" :key="fund.label" class="p-3 border border-gray-200 rounded-md">
                <p class="text-xs text-gray-500 mb-1">{{ fund.label }}</p>
                <p class="text-xl font-bold text-gray-800">{{ fund.value }}</p>
                <p class="text-xs text-gray-400">{{ fund.sub }}</p>
              </div>
            </div>
            <!-- 图表占位 -->
            <div class="h-40 border border-gray-100 rounded-lg flex items-center justify-center bg-gray-50">
              <span class="text-gray-400">资金趋势图表区域</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 第三行：最新通知 + 风险预警 -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 px-5 pt-5">
      <!-- 最新通知 -->
      <div class="bg-white rounded-lg border border-gray-200 shadow-sm">
        <div
          class="px-5 py-3 border-b border-gray-200 rounded-t-lg"
          :style="{ backgroundColor: 'var(--color-primary-light-9, #E6EEE8)' }"
        >
          <h3 class="font-bold flex items-center" :style="{ color: 'var(--color-primary, #2A5C45)' }">
            <i class="fas fa-bell mr-2"></i>
            最新通知
          </h3>
        </div>
        <div class="p-4 max-h-64 overflow-y-auto">
          <div v-for="notice in notices" :key="notice.id" class="pb-3 mb-3 border-b border-gray-100 last:border-0">
            <div class="flex justify-between items-start mb-1">
              <h4 class="text-gray-700 font-medium hover:opacity-80 cursor-pointer" :style="{ color: 'var(--color-primary, #2A5C45)' }">
                {{ notice.title }}
              </h4>
              <a href="javascript:void(0);" class="text-sm hover:underline" :style="{ color: 'var(--color-primary, #2A5C45)' }">
                查看详情
              </a>
            </div>
            <div class="flex items-center text-xs text-gray-400">
              <span>{{ notice.date }}</span>
              <span class="mx-2">|</span>
              <span>{{ notice.source }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 风险预警 -->
      <div class="bg-white rounded-lg border border-gray-200 shadow-sm">
        <div class="bg-red-50 px-5 py-3 border-b border-gray-200 rounded-t-lg">
          <h3 class="font-bold flex items-center text-red-700">
            <i class="fas fa-exclamation-triangle mr-2"></i>
            风险预警
          </h3>
        </div>
        <div class="p-4 space-y-3">
          <div
            v-for="warning in warnings"
            :key="warning.id"
            :class="warning.type === 'danger' ? 'bg-red-50 border-red-200' : 'bg-amber-50 border-amber-200'"
            class="p-3 border rounded-md flex items-start"
          >
            <i :class="`fas fa-${warning.icon} mt-1 mr-3 ${warning.type === 'danger' ? 'text-red-600' : 'text-amber-600'}`"></i>
            <div class="flex-grow">
              <p class="text-gray-700 text-sm">{{ warning.title }}</p>
              <p class="text-gray-500 text-xs mt-1">关联项目：{{ warning.project }}</p>
            </div>
            <button class="ml-2 text-sm hover:underline" :style="{ color: 'var(--color-primary, #2A5C45)' }">
              查看整改指引
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 第四行：快速业务入口 -->
    <div class="px-5 pt-5 pb-5">
      <div class="bg-white rounded-lg border border-gray-200 shadow-sm p-5">
        <h3 class="font-bold text-lg mb-6" :style="{ color: 'var(--color-primary, #2A5C45)' }">
          快速业务入口
        </h3>
        <div class="grid grid-cols-3 md:grid-cols-6 gap-4">
          <a
            v-for="entry in quickEntries"
            :key="entry.name"
            href="javascript:void(0);"
            class="flex flex-col items-center justify-center p-4 bg-gray-50 rounded-lg hover:scale-105 transition-all text-center"
          >
            <div
              class="w-14 h-14 rounded-full flex items-center justify-center mb-3"
              :style="{ backgroundColor: 'var(--color-primary-light-9, #E6EEE8)', color: 'var(--color-primary, #2A5C45)' }"
            >
              <i :class="`fas fa-${entry.icon} text-xl`"></i>
            </div>
            <span class="text-sm text-gray-700">{{ entry.name }}</span>
          </a>
        </div>
      </div>
    </div>
  </div>
</template>
