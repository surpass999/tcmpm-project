<script lang="ts" setup>
import { ref } from 'vue';

defineOptions({ name: 'ProvincePanel' });

// 模拟数据 - 待办审核
const todoTasks = ref({
  filing: {
    title: '备案审核',
    count: 5,
    tasks: [
      { id: 1, name: 'XX中医医院综合型试点项目', unit: 'XX省中医院', date: '2023-11-15' },
      { id: 2, name: 'XX市中西医结合医院特色专科试点', unit: 'XX市中西医结合医院', date: '2023-11-14' },
      { id: 3, name: 'XX县中医院基层服务能力提升项目', unit: 'XX县中医院', date: '2023-11-12' },
    ],
  },
  midTerm: {
    title: '中期评估审核',
    count: 4,
    tasks: [
      { id: 1, name: 'XX省中医医院智慧医疗项目', unit: 'XX省中医医院', date: '2023-11-14', stage: '中期评估' },
      { id: 2, name: 'XX市中西医结合医院信息化建设', unit: 'XX市中西医结合医院', date: '2023-11-12', stage: '中期评估' },
    ],
  },
  acceptance: {
    title: '验收审核',
    count: 3,
    tasks: [
      { id: 1, name: 'XX市中医医院远程会诊中心建设', unit: 'XX市中医医院', date: '2023-11-15', type: '初步验收' },
      { id: 2, name: 'XX省中西医结合医院智慧病房项目', unit: 'XX省中西医结合医院', date: '2023-11-12', type: '正式验收' },
    ],
  },
});

// 风险预警数据
const riskWarnings = ref({
  total: 8,
  categories: [
    { name: '全部风险', active: true },
    { name: '进度滞后', active: false },
    { name: '资金执行异常', active: false },
    { name: '材料缺失', active: false },
  ],
  items: [
    {
      id: 1,
      category: '进度滞后',
      name: 'XX市中医医院智慧药房项目',
      issue: '系统功能建设进度滞后 15%',
      date: '2023-11-10',
    },
    {
      id: 2,
      category: '资金执行异常',
      name: 'XX省中西医结合医院项目',
      issue: '资金执行率低于 60% 基准线',
      date: '2023-11-08',
    },
  ],
});

// 辖区统计
const regionStats = ref([
  { name: '北京市', total: 12, building: 8, completed: 4 },
  { name: '上海市', total: 10, building: 7, completed: 3 },
  { name: '广东省', total: 15, building: 10, completed: 5 },
  { name: '江苏省', total: 8, building: 5, completed: 3 },
  { name: '浙江省', total: 9, building: 6, completed: 3 },
]);

// 统计数据
const statistics = ref([
  { label: '辖区医院总数', value: 156, icon: 'hospital' },
  { label: '在运行项目', value: 89, icon: 'project-diagram' },
  { label: '待审核备案', value: 12, icon: 'clipboard-check' },
  { label: '风险预警', value: 8, icon: 'exclamation-triangle', danger: true },
]);

// 表格列配置
const regionColumns = [
  { title: '辖区', dataIndex: 'name', key: 'name' },
  { title: '项目总数', dataIndex: 'total', key: 'total' },
  { title: '建设中', dataIndex: 'building', key: 'building' },
  { title: '已完成', dataIndex: 'completed', key: 'completed' },
  { title: '完成进度', key: 'progress', width: 200 },
];
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 页面标题区 -->
    <div class="container mx-auto px-5 py-5">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between">
        <h2 class="text-xl font-bold mb-4 md:mb-0" :style="{ color: 'var(--color-primary, #2A5C45)' }">
          省局智慧中医医院试点项目监管控制台
        </h2>
        <div class="flex items-center space-x-3">
          <a-button class="flex items-center">
            <template #icon><i class="fas fa-sync-alt mr-2"></i></template>
            刷新数据
          </a-button>
          <a-dropdown>
            <a-button class="flex items-center">
              <template #icon><i class="fas fa-filter mr-2"></i></template>
              筛选辖区
              <i class="fas fa-chevron-down ml-2 text-xs"></i>
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item key="all">全部辖区</a-menu-item>
                <a-menu-item key="beijing">北京市</a-menu-item>
                <a-menu-item key="shanghai">上海市</a-menu-item>
                <a-menu-item key="guangdong">广东省</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>
    </div>

    <!-- 核心功能区块 -->
    <div class="container mx-auto px-5 pb-10">
      <!-- 待办审核区和风险预警区 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-5 mb-5">
        <!-- 待办审核区 -->
        <div class="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
          <div
            class="px-5 py-3 border-b border-gray-200 flex items-center justify-between"
            :style="{ backgroundColor: 'var(--color-primary-light-9, #E6EEE8)' }"
          >
            <h3 class="font-bold flex items-center" :style="{ color: 'var(--color-primary, #2A5C45)' }">
              <i class="fas fa-clipboard-list mr-2"></i>
              待办审核
            </h3>
            <span class="font-medium" style="color: var(--color-error, #ff4d4f)">待办：{{ todoTasks.filing.count + todoTasks.midTerm.count + todoTasks.acceptance.count }} 件</span>
          </div>
          <div class="p-4">
            <!-- 备案审核 -->
            <div class="mb-4">
              <h4 class="font-semibold text-gray-800 flex items-center mb-3">
                <i class="fas fa-chevron-down mr-2 text-xs text-gray-400"></i>
                备案审核
                <a-tag class="ml-2" color="green">待办数：{{ todoTasks.filing.count }}</a-tag>
              </h4>
              <div class="space-y-3 max-h-48 overflow-y-auto">
                <div
                  v-for="task in todoTasks.filing.tasks"
                  :key="task.id"
                  class="p-3 bg-white border border-gray-200 rounded-md hover:bg-gray-50 transition-colors cursor-pointer"
                >
                  <div class="flex justify-between items-start">
                    <div>
                      <p class="font-medium text-gray-800">{{ task.name }}</p>
                      <p class="text-sm text-gray-500 mt-1">申请单位：{{ task.unit }}</p>
                    </div>
                    <a-button type="primary" size="small">审核</a-button>
                  </div>
                  <p class="text-sm text-gray-400 mt-2">提交时间：{{ task.date }}</p>
                </div>
              </div>
            </div>

            <!-- 中期评估审核 -->
            <div class="mb-4">
              <h4 class="font-semibold text-gray-800 flex items-center mb-3">
                <i class="fas fa-chevron-down mr-2 text-xs text-gray-400"></i>
                中期评估审核
                <a-tag class="ml-2" color="green">待办数：{{ todoTasks.midTerm.count }}</a-tag>
              </h4>
              <div class="space-y-3 max-h-48 overflow-y-auto">
                <div
                  v-for="task in todoTasks.midTerm.tasks"
                  :key="task.id"
                  class="p-3 bg-white border border-gray-200 rounded-md hover:bg-gray-50 transition-colors cursor-pointer"
                >
                  <div class="flex justify-between items-start">
                    <div>
                      <p class="font-medium text-gray-800">{{ task.name }}</p>
                      <p class="text-sm text-gray-500 mt-1">申请单位：{{ task.unit }}</p>
                    </div>
                    <a-button type="primary" size="small">审核</a-button>
                  </div>
                  <div class="flex justify-between text-sm text-gray-400 mt-2">
                    <span>评估阶段：{{ task.stage }}</span>
                    <span>提交时间：{{ task.date }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 验收审核 -->
            <div>
              <h4 class="font-semibold text-gray-800 flex items-center mb-3">
                <i class="fas fa-chevron-down mr-2 text-xs text-gray-400"></i>
                验收审核
                <a-tag class="ml-2" color="green">待办数：{{ todoTasks.acceptance.count }}</a-tag>
              </h4>
              <div class="space-y-3 max-h-48 overflow-y-auto">
                <div
                  v-for="task in todoTasks.acceptance.tasks"
                  :key="task.id"
                  class="p-3 bg-white border border-gray-200 rounded-md hover:bg-gray-50 transition-colors cursor-pointer"
                >
                  <div class="flex justify-between items-start">
                    <div>
                      <p class="font-medium text-gray-800">{{ task.name }}</p>
                      <p class="text-sm text-gray-500 mt-1">申请单位：{{ task.unit }}</p>
                    </div>
                    <a-button type="primary" size="small">审核</a-button>
                  </div>
                  <div class="flex justify-between text-sm text-gray-400 mt-2">
                    <span>验收类型：{{ task.type }}</span>
                    <span>提交时间：{{ task.date }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 风险预警区 -->
        <div class="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
          <div class="bg-red-50 px-5 py-3 border-b border-gray-200 flex items-center justify-between">
            <h3 class="font-bold flex items-center text-red-700">
              <i class="fas fa-exclamation-triangle mr-2"></i>
              风险预警项目
            </h3>
            <span class="font-medium text-red-600">预警：{{ riskWarnings.total }} 个</span>
          </div>
          <div class="p-4">
            <!-- 风险分类标签 -->
            <div class="flex space-x-2 mb-4 overflow-x-auto pb-2">
              <a-tag
                v-for="cat in riskWarnings.categories"
                :key="cat.name"
                :color="cat.active ? 'red' : 'default'"
                class="cursor-pointer"
              >
                {{ cat.name }}
              </a-tag>
            </div>

            <!-- 风险项目列表 -->
            <div class="space-y-3">
              <div
                v-for="item in riskWarnings.items"
                :key="item.id"
                class="p-3 border border-red-200 bg-red-50 rounded-md cursor-pointer hover:bg-red-100 transition-colors"
              >
                <div class="flex items-start">
                  <i class="fas fa-clock text-red-500 mt-1 mr-3"></i>
                  <div class="flex-grow">
                    <div class="flex justify-between items-start">
                      <div>
                        <p class="font-medium text-gray-800">{{ item.name }}</p>
                        <p class="text-sm text-gray-500 mt-1">{{ item.issue }}</p>
                      </div>
                      <a-tag color="red">{{ item.category }}</a-tag>
                    </div>
                    <p class="text-xs text-gray-400 mt-2">预警时间：{{ item.date }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 辖区统计 -->
      <div class="bg-white rounded-lg border border-gray-200 shadow-sm">
        <div
          class="px-5 py-3 border-b border-gray-200"
          :style="{ backgroundColor: 'var(--color-primary-light-9, #E6EEE8)' }"
        >
          <h3 class="font-bold flex items-center" :style="{ color: 'var(--color-primary, #2A5C45)' }">
            <i class="fas fa-chart-bar mr-2"></i>
            辖区统计
          </h3>
        </div>
        <div class="p-4">
          <a-table :columns="regionColumns" :data-source="regionStats" :pagination="false" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'progress'">
                <div class="w-full bg-gray-100 rounded-full h-2">
                  <div
                    class="h-2 rounded-full"
                    :style="{ width: `${(record.completed / record.total) * 100}%`, backgroundColor: 'var(--color-primary, #2A5C45)' }"
                  ></div>
                </div>
                <span class="text-xs text-gray-500">{{ record.completed }}/{{ record.total }}</span>
              </template>
            </template>
          </a-table>
        </div>
      </div>
    </div>
  </div>
</template>
