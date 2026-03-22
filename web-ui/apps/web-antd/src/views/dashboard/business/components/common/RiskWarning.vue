<script setup lang="ts">
import type { RiskWarnings } from '#/api/declare/dashboard';

defineProps<{
  warnings?: RiskWarnings | null;
}>();

const emit = defineEmits<{
  'go-to-warning-projects': [];
}>();

function getLevelColor(level: string): string {
  switch (level) {
    case 'high': return '#f56c6c';
    case 'medium': return '#e6a23c';
    case 'low': return '#67c23a';
    default: return '#909399';
  }
}

function getLevelText(level: string): string {
  switch (level) {
    case 'high': return '高';
    case 'medium': return '中';
    case 'low': return '低';
    default: return '未知';
  }
}

function getTypeIcon(type: string): string {
  switch (type) {
    case 'progress': return '⏱';
    case 'fund': return '💰';
    case 'quality': return '⚠';
    default: return 'ℹ';
  }
}
</script>

<template>
  <div class="risk-warning">
    <div class="panel-header">
      <h3>风险预警</h3>
      <a-button type="link" @click="emit('go-to-warning-projects')">
        查看全部
      </a-button>
    </div>

    <div class="warning-summary">
      <div class="summary-item high">
        <span class="count">{{ warnings?.highRiskCount || 0 }}</span>
        <span class="label">高风险</span>
      </div>
      <div class="summary-item medium">
        <span class="count">{{ warnings?.mediumRiskCount || 0 }}</span>
        <span class="label">中风险</span>
      </div>
      <div class="summary-item low">
        <span class="count">{{ warnings?.lowRiskCount || 0 }}</span>
        <span class="label">低风险</span>
      </div>
    </div>

    <div class="warning-list" v-if="warnings?.warnings?.length">
      <div
        v-for="item in (warnings.warnings || []).slice(0, 5)"
        :key="item.id"
        class="warning-item"
      >
        <div class="warning-icon" :style="{ color: getLevelColor(item.level) }">
          <span style="font-size: 18px">{{ getTypeIcon(item.warningType) }}</span>
        </div>
        <div class="warning-content">
          <div class="warning-title">
            <a-tag :color="getLevelColor(item.level)" style="color: #fff">
              {{ getLevelText(item.level) }}
            </a-tag>
            <span>{{ item.title }}</span>
          </div>
          <div class="warning-desc">{{ item.description }}</div>
          <div class="warning-meta">
            <span class="meta-icon">[{{ item.projectName }}]</span>
            <span class="meta-icon">{{ item.warningTime }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <span>暂无风险预警</span>
    </div>
  </div>
</template>

<style scoped>
.risk-warning {
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
  display: flex;
  align-items: center;
  gap: 6px;
}

.panel-header h3 .icon {
  color: #e6a23c;
  font-size: 18px;
}

.warning-summary {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.summary-item {
  flex: 1;
  text-align: center;
}

.summary-item .count {
  display: block;
  font-size: 24px;
  font-weight: 600;
}

.summary-item.high .count { color: #f56c6c; }
.summary-item.medium .count { color: #e6a23c; }
.summary-item.low .count { color: #67c23a; }

.summary-item .label {
  font-size: 12px;
  color: #909399;
}

.warning-list {
  max-height: 300px;
  overflow-y: auto;
}

.warning-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.warning-item:last-child {
  border-bottom: none;
}

.warning-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-right: 12px;
  flex-shrink: 0;
}

.warning-content {
  flex: 1;
  min-width: 0;
}

.warning-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.warning-desc {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.warning-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.meta-icon {
  margin-right: 4px;
}

.empty-state {
  text-align: center;
  padding: 32px 0;
  color: #909399;
  font-size: 14px;
}
</style>
