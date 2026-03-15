<script lang="ts" setup>
import { ref } from 'vue';

defineOptions({ name: 'ExpertPanel' });

// 统计数据
const statistics = ref([
  { label: '待接收任务', value: 12, icon: 'inbox', color: 'danger', sub: '含 3 个紧急任务' },
  { label: '处理中任务', value: 8, icon: 'clock', color: 'warning', sub: '距截止时间≤3 天：2 个' },
  { label: '已完成任务', value: 25, icon: 'check-circle', color: 'success', sub: '本月完成：10 个' },
]);

// 待评审任务
const pendingTasks = ref({
  filing: {
    title: '备案方案论证',
    count: 5,
    tasks: [
      {
        id: 1,
        name: 'XX县中医医院智慧中药房型试点项目备案论证',
        province: '浙江省',
        type: '智慧中药房',
        unit: 'XX县中医医院',
        deadline: '2025-10-25',
        urgent: true,
      },
      {
        id: 2,
        name: 'XX市中医医院综合型试点项目备案论证',
        province: '江苏省',
        type: '综合型',
        unit: 'XX市中医医院',
        deadline: '2025-10-30',
        urgent: false,
      },
      {
        id: 3,
        name: 'XX省中西医结合医院智慧医疗型试点项目备案论证',
        province: '广东省',
        type: '智慧医疗',
        unit: 'XX省中西医结合医院',
        deadline: '2025-11-05',
        urgent: false,
      },
    ],
  },
  midTerm: {
    title: '中期评估评审',
    count: 4,
    tasks: [
      {
        id: 1,
        name: 'XX省中医药大学附属医院名老中医传承型试点项目中期评估',
        province: '浙江省',
        type: '名老中医传承型',
        unit: 'XX省中医药大学附属医院',
        deadline: '2025-10-30',
        urgent: false,
      },
      {
        id: 2,
        name: 'XX市第一中医医院智慧中药房试点项目中期评估',
        province: '江苏省',
        type: '智慧中药房',
        unit: 'XX市第一中医医院',
        deadline: '2025-10-22',
        urgent: true,
      },
    ],
  },
  acceptance: {
    title: '验收评审',
    count: 3,
    tasks: [
      {
        id: 1,
        name: 'XX县中医医院智慧医疗型试点项目验收评审',
        province: '广东省',
        type: '智慧医疗型',
        unit: 'XX县中医医院',
        deadline: '2025-10-20',
        urgent: true,
      },
    ],
  },
});

// 个人信息
const expertInfo = ref({
  name: '李四',
  fields: ['中医电子病历', '智慧中药房'],
  unit: 'XX中医药大学',
  level: '国家级',
});

// 评审偏好
const preferences = ref({
  displayMode: '按模块分组',
  template: '备案论证模板',
  notification: '实时提醒',
});

// 通知公告
const notices = ref([
  {
    id: 1,
    title: '关于 2025 年智慧中医医院试点项目中期评估评审标准更新的通知',
    date: '2025-10-18',
    type: 'info',
  },
  {
    id: 2,
    title: '评审意见填写规范提醒 —— 需明确标注核心指标打分依据',
    date: '2025-10-15',
    type: 'info',
  },
  {
    id: 3,
    title: '系统升级提示：10 月 20 日 22:00-24:00 暂停评审功能，请勿在此期间提交评审结果',
    date: '2025-10-10',
    type: 'warning',
  },
  {
    id: 4,
    title: '关于举办"智慧中医医院试点项目评审要点"线上培训的通知',
    date: '2025-10-05',
    type: 'info',
  },
]);
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 页面标题区 -->
    <div class="container mx-auto px-5 py-5">
      <div class="flex flex-col md:flex-row md:items-center justify-between">
        <h2 class="text-xl font-bold text-gray-800 mb-4 md:mb-0">专家评审工作控制台</h2>
        <div class="flex space-x-3">
          <a-button class="flex items-center">
            <template #icon><i class="fas fa-sync-alt mr-1"></i></template>
            刷新任务
          </a-button>
          <a-button class="flex items-center">
            <template #icon><i class="fas fa-filter mr-1"></i></template>
            筛选任务
          </a-button>
          <a-button class="flex items-center">
            <template #icon><i class="fas fa-cog mr-1"></i></template>
            个人设置
          </a-button>
        </div>
      </div>
    </div>

    <!-- 核心功能区块 -->
    <div class="container mx-auto px-5 pb-10">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
        <!-- 待评审任务区 -->
        <div class="bg-white rounded-lg border border-gray-200 shadow-sm">
          <div
            class="px-5 py-3 border-b border-gray-200 flex items-center justify-between"
            :style="{ backgroundColor: 'var(--color-primary-light-9, #E6EEE8)' }"
          >
            <h3 class="font-bold flex items-center" :style="{ color: 'var(--color-primary, #2A5C45)' }">
              <i class="fas fa-tasks mr-2"></i>
              待评审任务
            </h3>
            <span class="font-medium" style="color: var(--color-error, #ff4d4f)">紧急任务：3 个</span>
          </div>
          <div class="p-4">
            <!-- 备案方案论证 -->
            <div class="mb-6">
              <h4 class="font-bold text-gray-800 mb-3">
                备案方案论证（待评审数：{{ pendingTasks.filing.count }}）
              </h4>
              <div class="space-y-3 max-h-64 overflow-y-auto pr-2">
                <div
                  v-for="task in pendingTasks.filing.tasks"
                  :key="task.id"
                  class="border border-gray-200 rounded-lg p-4 hover:bg-gray-50 transition-colors cursor-pointer"
                >
                  <div class="mb-2">
                    <h5 class="font-medium text-gray-800">{{ task.name }}</h5>
                  </div>
                  <div class="grid grid-cols-2 gap-2 text-sm text-gray-500 mb-3">
                    <div>
                      <i class="fas fa-map-marker-alt mr-1"></i>
                      {{ task.province }}
                    </div>
                    <div>
                      <i class="fas fa-tag mr-1"></i>
                      {{ task.type }}
                    </div>
                    <div>
                      <i class="fas fa-building mr-1"></i>
                      {{ task.unit }}
                    </div>
                    <div :class="task.urgent ? 'text-red-500' : ''">
                      <i class="fas fa-clock mr-1"></i>
                      {{ task.deadline }}
                      <span v-if="task.urgent" class="text-red-500">(紧急)</span>
                    </div>
                  </div>
                  <div class="flex space-x-2">
                    <a-button size="small">接收任务</a-button>
                    <a-button size="small">查看材料</a-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 中期评估评审 -->
            <div class="mb-6">
              <h4 class="font-bold text-gray-800 mb-3">
                中期评估评审（待评审数：{{ pendingTasks.midTerm.count }}）
              </h4>
              <div class="space-y-3 max-h-64 overflow-y-auto pr-2">
                <div
                  v-for="task in pendingTasks.midTerm.tasks"
                  :key="task.id"
                  class="border border-gray-200 rounded-lg p-4 hover:bg-gray-50 transition-colors cursor-pointer"
                >
                  <div class="mb-2">
                    <h5 class="font-medium text-gray-800">{{ task.name }}</h5>
                  </div>
                  <div class="grid grid-cols-2 gap-2 text-sm text-gray-500 mb-3">
                    <div>
                      <i class="fas fa-project-diagram mr-1"></i>
                      {{ task.type }}
                    </div>
                    <div>
                      <i class="fas fa-sitemap mr-1"></i>
                      中期评估
                    </div>
                    <div>
                      <i class="fas fa-building mr-1"></i>
                      {{ task.unit }}
                    </div>
                    <div :class="task.urgent ? 'text-red-500' : ''">
                      <i class="fas fa-clock mr-1"></i>
                      {{ task.deadline }}
                      <span v-if="task.urgent" class="text-red-500">(紧急)</span>
                    </div>
                  </div>
                  <div class="flex space-x-2">
                    <a-button size="small">接收任务</a-button>
                    <a-button size="small">查看材料</a-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 验收评审 -->
            <div>
              <h4 class="font-bold text-gray-800 mb-3">
                验收评审（待评审数：{{ pendingTasks.acceptance.count }}）
              </h4>
              <div class="space-y-3 max-h-64 overflow-y-auto pr-2">
                <div
                  v-for="task in pendingTasks.acceptance.tasks"
                  :key="task.id"
                  class="border border-gray-200 rounded-lg p-4 hover:bg-gray-50 transition-colors cursor-pointer"
                >
                  <div class="mb-2">
                    <h5 class="font-medium text-gray-800">{{ task.name }}</h5>
                  </div>
                  <div class="grid grid-cols-2 gap-2 text-sm text-gray-500 mb-3">
                    <div>
                      <i class="fas fa-project-diagram mr-1"></i>
                      {{ task.type }}
                    </div>
                    <div>
                      <i class="fas fa-check-square mr-1"></i>
                      正式验收
                    </div>
                    <div>
                      <i class="fas fa-building mr-1"></i>
                      {{ task.unit }}
                    </div>
                    <div :class="task.urgent ? 'text-red-500' : ''">
                      <i class="fas fa-clock mr-1"></i>
                      {{ task.deadline }}
                      <span v-if="task.urgent" class="text-red-500">(紧急)</span>
                    </div>
                  </div>
                  <div class="flex space-x-2">
                    <a-button size="small">接收任务</a-button>
                    <a-button size="small">查看材料</a-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧区域 -->
        <div class="space-y-5">
          <!-- 评审进度统计 -->
          <div class="bg-white rounded-lg border border-gray-200 shadow-sm p-5">
            <h3 class="font-bold text-lg mb-4 flex items-center" :style="{ color: 'var(--color-primary, #2A5C45)' }">
              <i class="fas fa-chart-pie mr-2"></i>
              评审进度概览
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div
                v-for="stat in statistics"
                :key="stat.label"
                class="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow cursor-pointer"
              >
                <div
                  class="text-3xl mb-2"
                  :style="{ color: stat.color === 'danger' ? 'var(--color-error, #ff4d4f)' : stat.color === 'warning' ? 'var(--color-warning, #faad14)' : 'var(--color-success, #52c41a)' }"
                >
                  {{ stat.icon === 'inbox' ? '📥' : stat.icon === 'clock' ? '⏳' : '✅' }}
                </div>
                <div class="text-2xl font-bold text-gray-800 mb-1">{{ stat.value }} 个</div>
                <div class="text-sm text-gray-500">{{ stat.label }}</div>
                <div class="text-xs text-gray-400 mt-1">{{ stat.sub }}</div>
              </div>
            </div>
          </div>

          <!-- 个人信息与评审偏好 -->
          <div class="bg-white rounded-lg border border-gray-200 shadow-sm p-5">
            <h3 class="font-bold text-lg mb-4 flex items-center" :style="{ color: 'var(--color-primary, #2A5C45)' }">
              <i class="fas fa-user-circle mr-2"></i>
              个人信息与评审设置
            </h3>

            <!-- 个人基础信息 -->
            <div class="mb-6">
              <h4 class="font-bold text-gray-800 mb-4">个人基础信息</h4>
              <div class="space-y-3">
                <div class="flex">
                  <div class="w-24 text-gray-500">专家姓名</div>
                  <div class="text-gray-800">{{ expertInfo.name }}</div>
                </div>
                <div class="flex">
                  <div class="w-24 text-gray-500">专业领域</div>
                  <div class="flex flex-wrap gap-2">
                    <a-tag v-for="field in expertInfo.fields" :key="field" color="green">
                      {{ field }}
                    </a-tag>
                  </div>
                </div>
                <div class="flex">
                  <div class="w-24 text-gray-500">所属单位</div>
                  <div class="text-gray-800">{{ expertInfo.unit }}</div>
                </div>
                <div class="flex">
                  <div class="w-24 text-gray-500">评审资质等级</div>
                  <div class="text-gray-800">{{ expertInfo.level }}</div>
                </div>
              </div>
            </div>

            <!-- 评审偏好设置 -->
            <div>
              <h4 class="font-bold text-gray-800 mb-4">评审偏好设置</h4>
              <div class="space-y-4">
                <div class="flex items-center">
                  <div class="w-32 text-gray-500">评审材料展示方式</div>
                  <a-select :value="preferences.displayMode" class="w-48">
                    <a-select-option value="按模块分组">按模块分组</a-select-option>
                    <a-select-option value="按时间排序">按时间排序</a-select-option>
                  </a-select>
                </div>
                <div class="flex items-center">
                  <div class="w-32 text-gray-500">意见填写模板</div>
                  <a-select :value="preferences.template" class="w-48">
                    <a-select-option value="备案论证模板">备案论证模板</a-select-option>
                    <a-select-option value="中期评估模板">中期评估模板</a-select-option>
                    <a-select-option value="验收评审模板">验收评审模板</a-select-option>
                  </a-select>
                </div>
                <div class="flex items-center">
                  <div class="w-32 text-gray-500">通知提醒频率</div>
                  <a-select :value="preferences.notification" class="w-48">
                    <a-select-option value="实时提醒">实时提醒</a-select-option>
                    <a-select-option value="每日汇总">每日汇总</a-select-option>
                    <a-select-option value="不提醒">不提醒</a-select-option>
                  </a-select>
                </div>
              </div>
            </div>
          </div>

          <!-- 通知公告 -->
          <div class="bg-white rounded-lg border border-gray-200 shadow-sm p-5">
            <h3 class="font-bold text-lg mb-4 flex items-center" :style="{ color: 'var(--color-primary, #2A5C45)' }">
              <i class="fas fa-bell mr-2"></i>
              评审通知与政策提示
            </h3>
            <div class="space-y-4 max-h-80 overflow-y-auto pr-2">
              <div v-for="notice in notices" :key="notice.id" class="border-b border-gray-100 pb-4 last:border-0">
                <div class="flex justify-between items-start mb-2">
                  <h4 :class="`font-medium ${notice.type === 'warning' ? 'text-red-600' : 'text-gray-800'}`">
                    {{ notice.title }}
                  </h4>
                  <span class="text-xs text-gray-400">{{ notice.date }}</span>
                </div>
                <a-button type="link" size="small" class="p-0" :style="{ color: 'var(--color-primary, #2A5C45)' }">
                  {{ notice.type === 'warning' ? '已阅读' : '查看详情' }}
                </a-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部快捷操作 -->
    <div class="bg-white border-t border-gray-200 py-4">
      <div class="container mx-auto px-5">
        <div class="flex flex-wrap justify-center gap-4">
          <a-button>查看历史评审</a-button>
          <a-button>修改评审偏好</a-button>
          <a-button>联系管理员</a-button>
        </div>
      </div>
    </div>
  </div>
</template>

