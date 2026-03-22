<script setup lang="ts">
import { computed } from 'vue';
import type { PropType } from 'vue';
import { useRouter } from 'vue-router';

import type {
  DashboardStats,
  DashboardTasks,
  FundStats,
  RiskWarnings,
  TaskItem,
} from '#/api/declare/dashboard';
import StatisticCard from '../common/StatisticCard.vue';

const router = useRouter();

defineProps({
  stats: Object as PropType<DashboardStats>,
  tasks: Object as PropType<DashboardTasks>,
  fundStats: Object as PropType<FundStats>,
  warnings: Object as PropType<RiskWarnings>,
});

const emit = defineEmits<{
  'go-to-bpm-tasks': [];
  'go-to-project-list': [params?: Record<string, string>];
  'go-to-warning-projects': [];
}>();

// 合并待办任务
function getMergedTasks(tasks: DashboardTasks | null): TaskItem[] {
  if (!tasks) return [];
  const merged: TaskItem[] = [];
  if (tasks.bpmTasks?.items) merged.push(...tasks.bpmTasks.items);
  if (tasks.draftTasks?.items) merged.push(...tasks.draftTasks.items);
  return merged.sort((a, b) => {
    if (a.urgent && !b.urgent) return -1;
    if (!a.urgent && b.urgent) return 1;
    return 0;
  });
}

function getProcessTypeName(type?: number): string {
  const map: Record<number, string> = {
    1: '建设过程', 2: '半年报', 3: '年度报告', 4: '中期评估',
    5: '整改记录', 6: '验收申请', 7: '成果流通',
  };
  return type ? map[type] || '未知' : '未知';
}

function getStatusColor(status: string): string {
  const map: Record<string, string> = {
    pending: 'blue', processing: 'orange', completed: 'green',
  };
  return map[status] || 'default';
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    pending: '待处理', processing: '处理中', completed: '已完成',
  };
  return map[status] || status;
}

// 表格列配置
const taskColumns = [
  { title: '紧急', dataIndex: 'urgent', key: 'urgent', width: 60, align: 'center' as const },
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', width: 180, ellipsis: true },
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', width: 300, ellipsis: true },
  { title: '截止日期', dataIndex: 'deadline', key: 'deadline', width: 130 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90, align: 'center' as const },
  { title: '操作', key: 'action', width: 100, align: 'center' as const },
];

// 快速业务入口
const quickEntries = [
  { name: '备案管理', icon: 'file-text' },
  { name: '项目填报', icon: 'edit' },
  { name: '年度总结', icon: 'book' },
  { name: '中期评估', icon: 'bar-chart' },
  { name: '数据登记', icon: 'database' },
  { name: '验收申请', icon: 'check-circle' },
];

function goToQuickEntry(name: string) {
  const pathMap: Record<string, string> = {
    '备案管理': '/filing',
    '项目填报': '/project',
    '年度总结': '/project',
    '中期评估': '/review',
    '数据登记': '/caliber',
    '验收申请': '/review',
  };
  window.location.href = pathMap[name] || '/project';
}
</script>

<template>
  <div class="hospital-dashboard">
    <!-- 第一行：4个统计卡片 -->
    <a-row :gutter="20" class="stat-row">
      <a-col :span="6">
        <StatisticCard
          title="待办任务"
          :value="stats?.hospitalStats?.draftReportCount || stats?.taskCount || 0"
          icon="ant-design:file-text-outlined"
          color="#409eff"
          clickable
          @click="emit('go-to-bpm-tasks')"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="项目进度"
          :value="stats?.projectProgress || 0"
          suffix="%"
          icon="ant-design:line-chart-outlined"
          color="#67c23a"
          clickable
          @click="emit('go-to-project-list')"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="资金执行"
          :value="fundStats?.executionRate || stats?.fundExecutionRate || 0"
          suffix="%"
          icon="ant-design:account-book-outlined"
          color="#e6a23c"
          clickable
          @click="emit('go-to-project-list')"
        />
      </a-col>
      <a-col :span="6">
        <StatisticCard
          title="风险预警"
          :value="warnings?.totalCount || stats?.warningCount || 0"
          icon="ant-design:warning-outlined"
          :color="(warnings?.highRiskCount || 0) > 0 ? '#f56c6c' : '#909399'"
          clickable
          @click="emit('go-to-warning-projects')"
        />
      </a-col>
    </a-row>

    <!-- 第二行：我的待办 + 项目概览 -->
    <a-row :gutter="20" class="task-project-row">
      <!-- 待办任务 -->
      <a-col :span="16">
        <div class="task-panel">
          <div class="panel-header">
            <h3>我的待办</h3>
            <a-button type="link" @click="emit('go-to-bpm-tasks')">查看全部</a-button>
          </div>
          <a-table
            :columns="taskColumns"
            :data-source="getMergedTasks(tasks).slice(0, 5)"
            :pagination="false"
            :row-key="(record: TaskItem) => record.taskId"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'urgent'">
                <a-tag v-if="record.urgent" color="red" size="small">急</a-tag>
              </template>
              <template v-else-if="column.key === 'taskName'">
                <span>{{ record.taskName }}</span>
                <a-tag size="small" style="margin-left: 8px">{{ getProcessTypeName(record.processType) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" size="small" @click="router.push(record.jumpUrl)">
                  {{ record.status === 'pending' ? '去填写' : '查看' }}
                </a-button>
              </template>
            </template>
          </a-table>
          <div class="table-empty" v-if="!getMergedTasks(tasks).length">
            <span>暂无待办任务</span>
          </div>
        </div>
      </a-col>

      <!-- 项目概览 -->
      <a-col :span="8">
        <div class="project-summary">
          <div class="panel-header">
            <h3>我的项目</h3>
            <a-button type="link" @click="emit('go-to-project-list')">查看全部</a-button>
          </div>
          <div class="summary-item">
            <div class="value">{{ stats?.hospitalStats?.myProjectCount || stats?.projectCount || 0 }}</div>
            <div class="label">项目总数</div>
          </div>
          <div class="summary-item">
            <div class="value">{{ stats?.hospitalStats?.draftReportCount || 0 }}</div>
            <div class="label">草稿报告</div>
          </div>
          <div class="summary-item">
            <div class="value">{{ stats?.hospitalStats?.reviewingReportCount || 0 }}</div>
            <div class="label">审核中</div>
          </div>
          <div class="summary-item">
            <div class="value">{{ stats?.hospitalStats?.completedReportCount || 0 }}</div>
            <div class="label">已完成</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 第三行：建设进度与资金分析 -->
    <div class="progress-fund-panel">
      <div class="panel-header">
        <h3>建设进度与资金分析</h3>
      </div>
      <a-row :gutter="20">
        <!-- 左侧：建设进度 -->
        <a-col :span="12">
          <h4 class="section-title">建设进度</h4>
          <div class="progress-list">
            <div v-if="stats?.hospitalStats?.dimensionProgress" class="progress-item">
              <div class="progress-label">
                <span>系统功能建设</span>
                <span>{{ stats.hospitalStats.dimensionProgress.systemProgress || 0 }}%</span>
              </div>
              <div class="progress-bar-wrapper">
                <div class="progress-bar" :style="{ width: (stats.hospitalStats.dimensionProgress.systemProgress || 0) + '%' }"></div>
              </div>
            </div>
            <div v-else class="progress-item">
              <div class="progress-label">
                <span>系统功能建设</span>
                <span>--</span>
              </div>
              <div class="progress-bar-wrapper">
                <div class="progress-bar" style="width: 0%"></div>
              </div>
            </div>
            <div v-if="stats?.hospitalStats?.dimensionProgress" class="progress-item">
              <div class="progress-label">
                <span>高质量数据集建设</span>
                <span>{{ stats.hospitalStats.dimensionProgress.datasetProgress || 0 }}%</span>
              </div>
              <div class="progress-bar-wrapper">
                <div class="progress-bar" :style="{ width: (stats.hospitalStats.dimensionProgress.datasetProgress || 0) + '%' }"></div>
              </div>
            </div>
            <div v-else class="progress-item">
              <div class="progress-label">
                <span>高质量数据集建设</span>
                <span>--</span>
              </div>
              <div class="progress-bar-wrapper">
                <div class="progress-bar" style="width: 0%"></div>
              </div>
            </div>
            <div v-if="stats?.hospitalStats?.dimensionProgress" class="progress-item">
              <div class="progress-label">
                <span>信息安全备案</span>
                <span>{{ stats.hospitalStats.dimensionProgress.securityProgress || 0 }}%</span>
              </div>
              <div class="progress-bar-wrapper">
                <div class="progress-bar" :style="{ width: (stats.hospitalStats.dimensionProgress.securityProgress || 0) + '%' }"></div>
              </div>
            </div>
            <div v-else class="progress-item">
              <div class="progress-label">
                <span>信息安全备案</span>
                <span>--</span>
              </div>
              <div class="progress-bar-wrapper">
                <div class="progress-bar" style="width: 0%"></div>
              </div>
            </div>
            <div v-if="stats?.hospitalStats?.dimensionProgress" class="progress-item">
              <div class="progress-label">
                <span>成果转化</span>
                <span>{{ stats.hospitalStats.dimensionProgress.achievementProgress || 0 }}%</span>
              </div>
              <div class="progress-bar-wrapper">
                <div class="progress-bar" :style="{ width: (stats.hospitalStats.dimensionProgress.achievementProgress || 0) + '%' }"></div>
              </div>
            </div>
            <div v-else class="progress-item">
              <div class="progress-label">
                <span>成果转化</span>
                <span>--</span>
              </div>
              <div class="progress-bar-wrapper">
                <div class="progress-bar" style="width: 0%"></div>
              </div>
            </div>
          <div class="table-empty" v-if="!stats?.hospitalStats">
            <span>暂无进度数据</span>
          </div>
          </div>
        </a-col>

        <!-- 右侧：资金分析 -->
        <a-col :span="12">
          <h4 class="section-title">资金分析</h4>
          <div class="fund-analysis">
            <div class="fund-summary-row">
              <div class="fund-item">
                <div class="fund-label">总预算</div>
                <div class="fund-value">{{ fundStats?.totalFund ? (fundStats.totalFund / 10000).toFixed(0) + '万' : '-' }}</div>
              </div>
              <div class="fund-item">
                <div class="fund-label">已执行</div>
                <div class="fund-value">{{ fundStats?.executedFund ? (fundStats.executedFund / 10000).toFixed(0) + '万' : '-' }}</div>
              </div>
              <div class="fund-item highlight">
                <div class="fund-label">执行率</div>
                <div class="fund-value">{{ fundStats?.executionRate || stats?.fundExecutionRate || 0 }}%</div>
              </div>
            </div>
            <div class="fund-progress-wrapper">
              <div class="fund-progress-bar">
                <div
                  class="fund-progress-fill"
                  :style="{ width: (fundStats?.executionRate || stats?.fundExecutionRate || 0) + '%' }"
                ></div>
              </div>
              <div class="fund-progress-threshold">
                <div class="threshold-line" style="left: 60%"></div>
                <span class="threshold-label">60%基准线</span>
              </div>
            </div>
            <div class="fund-alert" v-if="(fundStats?.executionRate || 0) < 60">
              <span>资金执行率低于60%基准线，请加快执行进度</span>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <!-- 第四行：快速业务入口 -->
    <div class="quick-entries-panel">
      <div class="panel-header">
        <h3>快速业务入口</h3>
      </div>
      <div class="quick-entries-grid">
        <div
          v-for="entry in quickEntries"
          :key="entry.name"
          class="quick-entry-item"
          @click="goToQuickEntry(entry.name)"
        >
          <div class="entry-icon">
            <span class="icon-placeholder">{{ entry.name[0] }}</span>
          </div>
          <span class="entry-name">{{ entry.name }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hospital-dashboard {
  padding: 20px;
  background-color: #f5f7fa;
}

.stat-row,
.task-project-row {
  margin-bottom: 20px;
}

.task-panel,
.project-summary,
.progress-fund-panel,
.quick-entries-panel {
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

.project-summary {
  height: 100%;
}

.summary-item {
  text-align: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 12px;
}

.summary-item:last-child {
  margin-bottom: 0;
}

.summary-item .value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.summary-item .label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin: 0 0 16px 0;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}

.progress-list {
  padding: 0 8px;
}

.progress-item {
  margin-bottom: 16px;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #606266;
  margin-bottom: 6px;
}

.fund-analysis {
  padding: 0 8px;
}

.fund-summary-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.fund-item {
  flex: 1;
  text-align: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.fund-item.highlight {
  background: #e8f4fd;
}

.fund-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.fund-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.fund-item.highlight .fund-value {
  color: #409eff;
}

.fund-progress-wrapper {
  position: relative;
  margin-bottom: 12px;
}

.fund-progress-bar {
  height: 12px;
  background: #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}

.fund-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #67c23a);
  border-radius: 6px;
  transition: width 0.6s ease;
}

.fund-progress-threshold {
  position: relative;
  height: 16px;
  margin-top: 4px;
}

.threshold-line {
  position: absolute;
  top: 0;
  width: 1px;
  height: 100%;
  background: #f56c6c;
}

.threshold-label {
  position: absolute;
  left: 60%;
  transform: translateX(-50%);
  font-size: 10px;
  color: #f56c6c;
}

.quick-entries-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.quick-entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 8px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.quick-entry-item:hover {
  background: #ecf5ff;
  transform: translateY(-2px);
}

.entry-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #409eff;
  margin-bottom: 8px;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.2);
}

.entry-name {
  font-size: 14px;
  color: #606266;
  text-align: center;
}

.progress-bar-wrapper {
  background: #ebeef5;
  border-radius: 10px;
  height: 8px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #409eff 0%, #67c23a 100%);
  border-radius: 10px;
  transition: width 0.6s ease;
}

.table-empty {
  text-align: center;
  padding: 24px 0;
  color: #909399;
  font-size: 14px;
}

.fund-alert {
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 6px;
  padding: 10px 16px;
  color: #e6a23c;
  font-size: 14px;
}
</style>
