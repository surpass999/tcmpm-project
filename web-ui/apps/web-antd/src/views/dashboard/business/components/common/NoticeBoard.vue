<script setup lang="ts">
import { ref } from 'vue';

const notices = ref([
  { id: 1, title: '关于开展2024年度项目验收工作的通知', time: '2024-12-01', type: 'notice' },
  { id: 2, title: '智慧中医医院试点项目中期评估工作启动', time: '2024-11-25', type: 'news' },
  { id: 3, title: '2024年度项目资金执行情况通报', time: '2024-11-20', type: 'report' },
]);

function getTypeLabel(type: string): string {
  switch (type) {
    case 'notice': return '通知';
    case 'news': return '动态';
    case 'report': return '通报';
    default: return '公告';
  }
}

function getTypeColor(type: string): string {
  switch (type) {
    case 'notice': return '#409eff';
    case 'news': return '#67c23a';
    case 'report': return '#e6a23c';
    default: return '#909399';
  }
}
</script>

<template>
  <div class="notice-board">
    <div class="panel-header">
      <h3><bell theme="filled" class="icon" /> 通知公告</h3>
      <a-button type="link">查看全部</a-button>
    </div>

    <div class="notice-list">
      <div v-for="item in notices" :key="item.id" class="notice-item">
        <div class="notice-tag">
          <a-tag size="small" :color="getTypeColor(item.type)" style="color: #fff">
            {{ getTypeLabel(item.type) }}
          </a-tag>
        </div>
        <div class="notice-content">
          <span class="notice-title">{{ item.title }}</span>
          <span class="notice-time">{{ item.time }}</span>
        </div>
      </div>
    </div>

    <div v-if="!notices.length" class="empty-state">
      <span>暂无通知公告</span>
    </div>
  </div>
</template>

<style scoped>
.notice-board {
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
  color: #409eff;
  font-size: 18px;
}

.notice-list {
  max-height: 300px;
  overflow-y: auto;
}

.notice-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
}

.notice-item:hover {
  background: #f5f7fa;
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-tag {
  margin-right: 12px;
  flex-shrink: 0;
}

.notice-content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-width: 0;
}

.notice-title {
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.notice-time {
  font-size: 12px;
  color: #909399;
  margin-left: 16px;
  flex-shrink: 0;
}

.empty-state {
  text-align: center;
  padding: 32px 0;
  color: #909399;
  font-size: 14px;
}
</style>
