<script lang="ts" setup>
import type { DeclareAchievementApi } from '#/api/declare/achievement';

import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';

import { Button, message, Spin, Tabs, Tag, Timeline } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

import { getAchievement } from '#/api/declare/achievement';
import { getTaskByBusiness, getProcessByBusiness } from '#/api/bpm/task';
import { submitBpmAction } from '#/api/bpm/action';
import { OperationButtonType } from '#/views/bpm/components/simple-process-design/consts';

import AchievementBasicInfo from './components/AchievementBasicInfo.vue';
import ApprovalModal from '#/components/bpm/ApprovalModal.vue';

const route = useRoute();
const router = useRouter();

// 成果类型
const ACHIEVEMENT_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '系统功能', color: '#4A8F72' },
  2: { label: '数据集', color: '#1890ff' },
  3: { label: '科研成果', color: '#722ed1' },
  4: { label: '管理经验', color: '#fa8c16' },
};

// 推荐状态
const RECOMMEND_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '未推荐', color: 'default' },
  1: { label: '已推荐至国家局', color: 'blue' },
  2: { label: '已纳入推广库', color: 'purple' },
};

// BPM 相关
const BUSINESS_TABLE_NAME = 'achievement';
const loading = ref(false);
const detail = ref<DeclareAchievementApi.Achievement | null>(null);
const activeTab = ref('basic');

// BPM 审批相关状态
const approvalLoading = ref(false);
const approvalDetail = ref<any>(null);
const approvalHistory = ref<any[]>([]);
const availableActions = ref<any[]>([]);
const approvalModalRef = ref<any>(null);
const currentAction = ref<any>(null);

const achievementId = computed(() => Number(route.params.id));

const achievementTypeInfo = computed(() => {
  return ACHIEVEMENT_TYPE_MAP[detail.value?.achievementType || 0] || { label: '未知', color: '#999' };
});

const recommendStatusInfo = computed(() => {
  return RECOMMEND_STATUS_MAP[detail.value?.recommendStatus || 0] || { label: '未知', color: 'default' };
});

function formatTime(time: string | undefined): string {
  if (!time) return '-';
  return time.substring(0, 19).replace('T', ' ');
}

// 格式化日期
function formatDateTime(value: string | number | undefined): string {
  if (!value) return '-';
  const date = new Date(value);
  if (isNaN(date.getTime())) return '-';
  return date.toLocaleString('zh-CN');
}

// 加载成果数据
async function loadData() {
  if (!achievementId.value) return;
  loading.value = true;
  try {
    detail.value = await getAchievement(achievementId.value);
  } finally {
    loading.value = false;
  }
}

// 加载 BPM 审批数据和审批历史
async function loadBpmData() {
  if (!achievementId.value) return;
  approvalLoading.value = true;
  try {
    const [taskStatusList, processList] = await Promise.all([
      getTaskByBusiness({
        tableName: BUSINESS_TABLE_NAME,
        businessIds: [achievementId.value],
      }),
      getProcessByBusiness({
        tableName: BUSINESS_TABLE_NAME,
        businessId: achievementId.value,
      }),
    ]);

    const taskStatus = taskStatusList?.[0];
    approvalDetail.value = {
      status: taskStatus?.hasTodoTask ? 1 : (taskStatus?.processInstanceId ? 2 : -1),
      processInstance: taskStatus?.processInstanceId ? { id: taskStatus.processInstanceId } : null,
      todoTask: taskStatus?.todoTasks?.[0] || null,
    };

    // 提取可用操作
    const todoTask = taskStatus?.todoTasks?.[0];
    if (todoTask?.buttonSettings) {
      const actions: any[] = [];
      Object.entries(todoTask.buttonSettings).forEach(([key, config]: [string, any]) => {
        if (config?.enable) {
          actions.push({
            key,
            label: config.displayName || key,
            vars: { rectifyProcessDefinitionKey: config.rectifyProcessDefinitionKey },
            taskId: todoTask.taskId,
          });
        }
      });
      availableActions.value = actions;
    } else {
      availableActions.value = [];
    }

    // 构建审批历史
    const processData = processList?.[0];
    if (processData?.activityNodes) {
      const history: any[] = [];
      processData.activityNodes.forEach((node: any) => {
        if (node.tasks && node.tasks.length > 0) {
          node.tasks.forEach((task: any) => {
            let approved: boolean | undefined;
            let actionLabel = '';
            switch (node.status) {
              case 2:
                approved = true;
                actionLabel = '通过';
                break;
              case 3:
                approved = false;
                actionLabel = '拒绝';
                break;
              case 5:
                actionLabel = '退回';
                break;
              default:
                actionLabel = node.name;
            }
            history.push({
              id: task.id || node.id,
              name: node.name,
              actionLabel,
              assigneeUser: task.assigneeUser,
              reason: task.reason,
              startTime: node.startTime,
              endTime: task.endTime || node.endTime,
              approved,
              children: [], // 预留子流程展开
            });
          });
        }
      });
      // 按结束时间排序
      approvalHistory.value = history.sort((a, b) => {
        const aTime = a.endTime ? new Date(a.endTime).getTime() : 0;
        const bTime = b.endTime ? new Date(b.endTime).getTime() : 0;
        return aTime - bTime;
      });
    } else {
      approvalHistory.value = [];
    }
  } catch (e) {
    console.error('加载BPM审批数据失败', e);
  } finally {
    approvalLoading.value = false;
  }
}

// 获取操作按钮类型
function getActionButtonType(key: string): 'primary' | 'default' | undefined {
  if (key === '1') return 'primary';
  if (key === '2') return 'primary'; // 拒绝也用 primary，在样式上区分
  return 'default';
}

// 获取操作图标
function getActionIcon(key: string): string {
  const iconMap: Record<string, string> = {
    '1': 'ep:circle-check',
    '2': 'ep:circle-close',
    '3': 'ep:right',
    '4': 'ep:user',
    '5': 'ep:document-add',
    '6': 'ep:back',
    '8': 'ep:user',
    '9': 'ep:document',
  };
  return iconMap[key] || 'ep:more-filled';
}

// 获取历史项颜色
function getTimelineColor(approved: boolean | undefined): string {
  if (approved === true) return 'green';
  if (approved === false) return 'red';
  return 'blue';
}

// 处理审批操作点击
function handleActionClick(action: any) {
  const buttonType = Number(action.key);

  // 通过/发起整改 - 打开审批弹窗
  if (buttonType === OperationButtonType.APPROVE || buttonType === OperationButtonType.RECTIFY) {
    currentAction.value = action;
    approvalModalRef.value?.open({
      action: 'approve',
      title: action.label || '通过',
      reason: '',
    });
    return;
  }

  // 拒绝 - 打开审批弹窗
  if (buttonType === OperationButtonType.REJECT) {
    currentAction.value = action;
    approvalModalRef.value?.open({
      action: 'reject',
      title: action.label || '拒绝',
      reason: '',
    });
    return;
  }

  message.warn('该操作暂不支持');
}

// 审批确认
async function handleApprovalConfirm(reason: string, _action: 'approve' | 'reject') {
  if (!currentAction.value) {
    message.error('操作信息丢失');
    return;
  }
  const vars = currentAction.value.vars || {};
  try {
    await submitBpmAction({
      businessType: BUSINESS_TABLE_NAME,
      businessId: achievementId.value,
      actionKey: currentAction.value.key,
      reason,
      taskId: approvalDetail.value?.todoTask?.taskId,
      variables: vars.rectifyProcessDefinitionKey
        ? { rectifyProcessDefinitionKey: vars.rectifyProcessDefinitionKey }
        : undefined,
    });
    message.success('操作成功');
    currentAction.value = null;
    await loadBpmData();
  } catch (e: any) {
    message.error(e.message || '操作失败');
  }
}

function handleBack() {
  router.push('/declare/achievement');
}

onMounted(async () => {
  await loadData();
  await loadBpmData();
});

// 监听 Tab 切换，切换到审批历史时重新加载
watch(activeTab, async (newTab) => {
  if (newTab === 'approval' && approvalHistory.value.length === 0) {
    await loadBpmData();
  }
});
</script>

<template>
  <Page auto-content-height class="achievement-detail-page">
    <!-- 顶部导航栏 -->
    <div class="detail-nav">
      <Button type="link" class="back-btn" @click="handleBack">
        <i class="fas fa-arrow-left mr-2"></i>
        返回列表
      </Button>
      <span class="detail-nav__title">成果详情</span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="detail-loading">
      <Spin size="large" />
    </div>

    <!-- 详情内容 -->
    <div v-else-if="detail" class="detail-content">
      <!-- 头部卡片 -->
      <div class="detail-header-card">
        <div class="detail-header-card__icon">
          <div
            class="detail-header-card__icon-inner"
            :style="{ backgroundColor: achievementTypeInfo.color }"
          >
            <i class="fas fa-trophy text-2xl text-white"></i>
          </div>
        </div>
        <div class="detail-header-card__info">
          <div class="detail-header-card__tags">
            <Tag :color="achievementTypeInfo.color">
              {{ achievementTypeInfo.label }}
            </Tag>
            <Tag :color="recommendStatusInfo.color">
              {{ recommendStatusInfo.label }}
            </Tag>
          </div>
          <h1 class="detail-header-card__name">{{ detail.achievementName }}</h1>
          <div class="detail-header-card__meta">
            <span v-if="detail.projectName">
              <i class="fas fa-hospital mr-1 text-gray-400"></i>
              {{ detail.projectName }}
            </span>
            <span v-if="detail.createTime">
              <i class="fas fa-calendar-alt ml-4 mr-1 text-gray-400"></i>
              {{ formatTime(detail.createTime) }}
            </span>
          </div>
        </div>
      </div>

      <!-- Tab 切换 -->
      <div class="detail-tabs">
        <Tabs v-model:activeKey="activeTab">
          <Tabs.TabPane key="basic" tab="基本信息">
            <AchievementBasicInfo :detail="detail" />
          </Tabs.TabPane>
          <Tabs.TabPane key="approval" tab="审批历史">
            <div class="approval-history-container">
              <Spin v-if="approvalLoading" />
              <template v-else>
                <!-- 审批操作按钮 -->
                <div v-if="availableActions.length" class="action-buttons mb-4">
                  <Button
                    v-for="action in availableActions"
                    :key="action.key"
                    :type="getActionButtonType(action.key)"
                    size="large"
                    class="action-btn"
                    @click="handleActionClick(action)"
                  >
                    <template #icon>
                      <IconifyIcon :icon="getActionIcon(action.key)" />
                    </template>
                    {{ action.label }}
                  </Button>
                </div>
                <div v-else-if="approvalDetail?.status === -1" class="empty-text mb-4">
                  当前成果暂无审批流程
                </div>

                <!-- 审批历史时间线 -->
                <div v-if="approvalHistory.length > 0" class="approval-timeline">
                  <Timeline>
                    <Timeline.Item
                      v-for="item in approvalHistory"
                      :key="item.id"
                      :color="getTimelineColor(item.approved)"
                    >
                      <div class="timeline-item">
                        <div class="timeline-header">
                          <span class="timeline-node-name">{{ item.name }}</span>
                          <span class="timeline-action-label">{{ item.actionLabel }}</span>
                        </div>
                        <div class="timeline-meta">
                          <span v-if="item.assigneeUser?.nickname">
                            <IconifyIcon icon="ep:user" class="mr-1" />
                            {{ item.assigneeUser.nickname }}
                          </span>
                          <span v-if="item.endTime">
                            <IconifyIcon icon="ep:clock" class="ml-3 mr-1" />
                            {{ formatDateTime(item.endTime) }}
                          </span>
                        </div>
                        <div v-if="item.reason" class="timeline-reason">
                          <IconifyIcon icon="ep:document" class="mr-1" />
                          {{ item.reason }}
                        </div>
                      </div>
                    </Timeline.Item>
                  </Timeline>
                </div>
                <div v-else class="empty-text">
                  暂无审批记录
                </div>
              </template>
            </div>
          </Tabs.TabPane>
        </Tabs>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="detail-empty">
      <p class="text-gray-400">未找到成果详情</p>
      <Button type="primary" class="mt-4" @click="handleBack">返回列表</Button>
    </div>
    <!-- 审批弹窗（通过/拒绝/发起整改） -->
    <ApprovalModal
      ref="approvalModalRef"
      @confirm="(reason, action) => handleApprovalConfirm(reason, action)"
    />
  </Page>
</template>

<style scoped>
.achievement-detail-page {
  display: flex;
  flex-direction: column;
  background: #f5f6f8;
  padding: 0;
}

.detail-nav {
  background: #fff;
  padding: 12px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  padding: 4px 8px;
  color: #666;
  font-size: 14px;
}

.back-btn:hover {
  color: #4A8F72;
}

.detail-nav__title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.detail-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 80px;
}

.detail-content {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-empty {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 80px;
}

/* 头部卡片 */
.detail-header-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.detail-header-card__icon {
  flex-shrink: 0;
}

.detail-header-card__icon-inner {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-header-card__info {
  flex: 1;
  min-width: 0;
}

.detail-header-card__tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.detail-header-card__name {
  font-size: 22px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 12px 0;
  line-height: 1.4;
}

.detail-header-card__meta {
  font-size: 14px;
  color: #595959;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

/* Tab 区域 */
.detail-tabs {
  background: #fff;
  border-radius: 8px;
  padding: 0 24px 24px;
}

/* 审批历史 */
.approval-history-container {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.action-btn {
  min-width: 100px;
}

.empty-text {
  color: #999;
  text-align: center;
  padding: 24px;
}

.approval-timeline {
  padding: 8px 0;
}

.timeline-item {
  padding: 4px 0;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.timeline-node-name {
  font-weight: 600;
  font-size: 14px;
  color: #262626;
}

.timeline-action-label {
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 4px;
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}

.timeline-meta {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.timeline-reason {
  font-size: 13px;
  color: #595959;
  background: #fafafa;
  padding: 6px 10px;
  border-radius: 4px;
  border-left: 3px solid #d9d9d9;
}
</style>
