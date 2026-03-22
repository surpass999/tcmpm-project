<script lang="ts" setup>
import type { DeclareAchievementApi } from '#/api/declare/achievement';

import { nextTick, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Spin, Statistic, Tabs, Tag } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { Descriptions, DescriptionsItem } from 'ant-design-vue';

// 成果类型
const ACHIEVEMENT_TYPE_MAP: Record<number, { label: string; color: string; icon: string }> = {
  1: { label: '系统功能', color: '#4A8F72', icon: 'ep:monitor' },
  2: { label: '数据集', color: '#1890ff', icon: 'ep:database' },
  3: { label: '科研成果', color: '#722ed1', icon: 'ep:magic-stick' },
  4: { label: '管理经验', color: '#fa8c16', icon: 'ep:document' },
};

// 流通类型
const FLOW_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '内部使用', color: 'blue' },
  2: { label: '对外共享', color: 'green' },
  3: { label: '交易', color: 'orange' },
};

// 推广范围
const PROMOTION_SCOPE_MAP: Record<number, string> = {
  1: '院内',
  2: '省级',
  3: '全国',
};

// 可复制性
const REPLICATION_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '高', color: 'green' },
  2: { label: '中', color: 'orange' },
  3: { label: '低', color: 'red' },
};

// 共享范围
const SHARE_SCOPE_MAP: Record<number, string> = {
  1: '院内',
  2: '省级',
  3: '全国',
};

// 数据质量
const DATA_QUALITY_MAP: Record<number, string> = {
  1: '优',
  2: '良',
  3: '中',
  4: '差',
};

// 转化类型
const TRANSFORM_TYPE_MAP: Record<number, string> = {
  1: '标准规范',
  2: '创新模式',
  3: '典型案例',
};

// 状态映射
const STATUS_MAP: Record<string, { text: string; color: string; bg: string }> = {
  DRAFT: { text: '草稿', color: '#8c8c8c', bg: '#f5f5f5' },
  SUBMITTED: { text: '已提交', color: '#1890ff', bg: '#e6f4ff' },
  NATIONAL_APPROVED: { text: '国家局已审核', color: '#52c41a', bg: '#f6ffed' },
  REJECTED: { text: '退回', color: '#ff4d4f', bg: '#fff2f0' },
};

// 推荐状态
const RECOMMEND_STATUS_MAP: Record<number, { text: string; color: string; bg: string }> = {
  0: { text: '未推荐', color: '#8c8c8c', bg: '#f5f5f5' },
  1: { text: '已推荐至国家局', color: '#1890ff', bg: '#e6f4ff' },
  2: { text: '已纳入推广库', color: '#722ed1', bg: '#f9f0ff' },
};

const detail = ref<DeclareAchievementApi.Achievement | null>(null);
const activeTab = ref('basic');

// 弹窗
const [Modal, modalApi] = useVbenModal({
  destroyOnClose: true,
  showCancelButton: false,
  confirmText: '关闭',
  onConfirm() {
    modalApi.close();
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      await nextTick();
      detail.value = null;
      return;
    }
    const data = modalApi.getData<DeclareAchievementApi.Achievement>();
    if (data?.id) {
      detail.value = data;
    }
  },
});

function getAchievementTypeInfo(type?: number) {
  return ACHIEVEMENT_TYPE_MAP[type || 0] || { label: '-', color: '#999', icon: 'ep:info' };
}

function getFlowTypeInfo(value?: number) {
  return FLOW_TYPE_MAP[value || 0] || { label: '-', color: '' };
}

function getReplicationInfo(value?: number) {
  return REPLICATION_MAP[value || 0] || { label: '-', color: '' };
}

function getStatusInfo(status?: string) {
  return STATUS_MAP[status || ''] || { text: '-', color: '#999', bg: '#f5f5f5' };
}

function getRecommendStatusInfo(status?: number) {
  return RECOMMEND_STATUS_MAP[status || 0] || { text: '-', color: '#999', bg: '#f5f5f5' };
}

function formatDateTime(value: string | undefined) {
  if (!value) return '-';
  return value.substring(0, 19).replace('T', ' ');
}

function formatDate(value: string | undefined) {
  if (!value) return '-';
  return value.substring(0, 10);
}

function hasContent(text: string | undefined | null): boolean {
  return !!text && text.trim().length > 0;
}

// 获取附件下载 URL
function getAttachmentUrl(attachmentId: string): string {
  if (!attachmentId) return '#';
  // 如果已经是完整 URL，直接返回
  if (attachmentId.startsWith('http://') || attachmentId.startsWith('https://')) {
    return attachmentId;
  }
  // 如果是文件 ID 格式（数字），需要构造访问 URL
  // 格式: /infra/file/{configId}/get/{path}
  // 这里假设 attachmentIds 存储的是文件的完整 URL 或 path
  // 如果是 path，需要拼接 baseURL
  if (import.meta.env.VITE_API_BASEPATH) {
    return `${import.meta.env.VITE_API_BASEPATH}${attachmentId}`;
  }
  return attachmentId;
}

// 获取附件名称
function getAttachmentName(attachmentId: string, index: number): string {
  if (!attachmentId) return `附件${index + 1}`;
  try {
    // 如果是 URL，提取文件名
    if (attachmentId.includes('/')) {
      const filename = attachmentId.split('/').pop() || '';
      return decodeURIComponent(filename) || `附件${index + 1}`;
    }
  } catch {
    // 忽略解码错误
  }
  return `附件${index + 1}`;
}
</script>

<template>
  <Modal
    class="achievement-modal w-[80%]"
    :body-style="{ padding: 0 }"
  >
    <Spin v-if="!detail" class="loading-center" />

    <div v-else class="detail-wrapper">
      <!-- 顶部标题区 -->
      <div class="detail-header">
        <div class="header-left">
          <div class="achievement-icon" :style="{ background: getAchievementTypeInfo(detail.achievementType).color + '15', border: `1px solid ${getAchievementTypeInfo(detail.achievementType).color}30` }">
            <IconifyIcon :icon="getAchievementTypeInfo(detail.achievementType).icon" :style="{ color: getAchievementTypeInfo(detail.achievementType).color }" />
          </div>
          <div class="header-info">
            <h2 class="achievement-title">{{ detail.achievementName }}</h2>
            <div class="header-tags">
              <Tag :color="getAchievementTypeInfo(detail.achievementType).color" :style="{ borderRadius: '4px' }">
                {{ getAchievementTypeInfo(detail.achievementType).label }}
              </Tag>
              <span class="status-badge" :style="{ color: getStatusInfo(detail.status).color, background: getStatusInfo(detail.status).bg }">
                <span class="status-dot" :style="{ background: getStatusInfo(detail.status).color }"></span>
                {{ getStatusInfo(detail.status).text }}
              </span>
              <span v-if="detail.recommendStatus && detail.recommendStatus > 0" class="status-badge" :style="{ color: getRecommendStatusInfo(detail.recommendStatus).color, background: getRecommendStatusInfo(detail.recommendStatus).bg }">
                <span class="status-dot" :style="{ background: getRecommendStatusInfo(detail.recommendStatus).color }"></span>
                {{ getRecommendStatusInfo(detail.recommendStatus).text }}
              </span>
            </div>
          </div>
        </div>
        <div class="header-right">
          <Statistic
            v-if="detail.promotionCount != null"
            title="推广次数"
            :value="detail.promotionCount"
            :value-style="{ color: '#fa8c16', fontSize: '28px', fontWeight: '600' }"
          />
          <div class="header-meta">
            <div class="meta-item">
              <IconifyIcon icon="ep:user" />
              <span>{{ detail.creatorName || detail.creator || '-' }}</span>
            </div>
            <div class="meta-item">
              <IconifyIcon icon="ep:clock" />
              <span>{{ formatDateTime(detail.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab 切换区 -->
      <div class="detail-body">
        <Tabs v-model:activeKey="activeTab" class="achievement-tabs">
          <Tabs.TabPane key="basic" tab="基本信息">
            <div class="tab-content">
              <!-- 成果基本信息 -->
              <div class="info-card">
                <div class="info-card-header">
                  <IconifyIcon icon="ep:postcard" class="card-icon" />
                  <span>成果基本信息</span>
                </div>
                <div class="info-card-body">
                  <Descriptions :column="2" bordered size="small">
                    <DescriptionsItem label="应用领域">
                      <span class="field-value">{{ detail.applicationField || '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="可复制性">
                      <Tag
                        v-if="detail.replicationValue"
                        :color="getReplicationInfo(detail.replicationValue).color"
                        style="border-radius: 4px;"
                      >
                        {{ getReplicationInfo(detail.replicationValue).label }}
                      </Tag>
                      <span v-else>-</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="推广范围">
                      <span class="field-value">{{ detail.promotionScope ? PROMOTION_SCOPE_MAP[detail.promotionScope] || '-' : '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="转化类型">
                      <span class="field-value">{{ detail.transformType ? TRANSFORM_TYPE_MAP[detail.transformType] || '-' : '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="关联项目">
                      <span class="field-value highlight">{{ detail.projectName || '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="所属部门">
                      <span class="field-value">{{ detail.deptId || '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="成果描述" :span="2">
                      <div class="field-content" v-html="hasContent(detail.description) ? detail.description : '<span class=\'empty-hint\'>暂无</span>'"></div>
                    </DescriptionsItem>
                    <DescriptionsItem label="应用效果" :span="2">
                      <div class="field-content" v-html="hasContent(detail.effectDescription) ? detail.effectDescription : '<span class=\'empty-hint\'>暂无</span>'"></div>
                    </DescriptionsItem>
                  </Descriptions>
                </div>
              </div>

              <!-- 数据集信息 -->
              <div v-if="detail.dataName || detail.dataType" class="info-card">
                <div class="info-card-header">
                  <IconifyIcon icon="ep:database" class="card-icon" />
                  <span>数据集信息</span>
                </div>
                <div class="info-card-body">
                  <Descriptions :column="2" bordered size="small">
                    <DescriptionsItem label="数据名称">
                      <span class="field-value">{{ detail.dataName || '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="数据类型">
                      <span class="field-value">{{ detail.dataType || '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="数据来源">
                      <span class="field-value">{{ detail.dataSource || '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="数据规模">
                      <span class="field-value">{{ detail.dataVolume || '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="数据质量">
                      <span class="field-value">{{ detail.dataQuality ? DATA_QUALITY_MAP[detail.dataQuality] || '-' : '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="共享范围">
                      <span class="field-value">{{ detail.shareScope ? SHARE_SCOPE_MAP[detail.shareScope] || '-' : '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="数据描述" :span="2">
                      <div class="field-content" v-html="hasContent(detail.dataDescription) ? detail.dataDescription : '<span class=\'empty-hint\'>暂无</span>'"></div>
                    </DescriptionsItem>
                  </Descriptions>
                </div>
              </div>

              <!-- 数据流通信息 -->
              <div v-if="detail.flowType" class="info-card">
                <div class="info-card-header">
                  <IconifyIcon icon="ep:share" class="card-icon" />
                  <span>数据流通信息</span>
                </div>
                <div class="info-card-body">
                  <Descriptions :column="2" bordered size="small">
                    <DescriptionsItem label="流通类型">
                      <Tag :color="getFlowTypeInfo(detail.flowType).color" style="border-radius: 4px;">
                        {{ getFlowTypeInfo(detail.flowType).label }}
                      </Tag>
                    </DescriptionsItem>
                    <DescriptionsItem label="流通对象">
                      <span class="field-value">{{ detail.flowObject || '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="流通开始时间">
                      <span class="field-value">{{ formatDate(detail.startTime) }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="流通结束时间">
                      <span class="field-value">{{ formatDate(detail.endTime) }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="安全备案编号">
                      <span class="field-value">{{ detail.securityFilingNo || '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="流通目的" :span="2">
                      <div class="field-content" v-html="hasContent(detail.flowPurpose) ? detail.flowPurpose : '<span class=\'empty-hint\'>暂无</span>'"></div>
                    </DescriptionsItem>
                  </Descriptions>
                </div>
              </div>

              <!-- 证书与交易信息 -->
              <div v-if="detail.certificateCount || detail.propertyCount || detail.transactionCount" class="info-card">
                <div class="info-card-header">
                  <IconifyIcon icon="ep:trophy" class="card-icon" />
                  <span>证书与交易信息</span>
                </div>
                <div class="info-card-body">
                  <div class="stat-grid">
                    <div v-if="detail.certificateCount != null" class="stat-card">
                      <div class="stat-value">{{ detail.certificateCount }}</div>
                      <div class="stat-label">数据产品证书数</div>
                    </div>
                    <div v-if="detail.propertyCount != null" class="stat-card">
                      <div class="stat-value">{{ detail.propertyCount }}</div>
                      <div class="stat-label">数据产权登记证数</div>
                    </div>
                    <div v-if="detail.transactionAmount != null" class="stat-card highlight">
                      <div class="stat-value">{{ detail.transactionAmount }}<span class="stat-unit">万元</span></div>
                      <div class="stat-label">累计交易金额</div>
                    </div>
                    <div v-if="detail.transactionCount != null" class="stat-card">
                      <div class="stat-value">{{ detail.transactionCount }}</div>
                      <div class="stat-label">完成交易产品数</div>
                    </div>
                  </div>
                  <Descriptions v-if="detail.transactionObject || detail.transactionTime" :column="2" bordered size="small" style="margin-top: 16px;">
                    <DescriptionsItem label="交易对象">
                      <span class="field-value">{{ detail.transactionObject || '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="交易完成时间">
                      <span class="field-value">{{ formatDate(detail.transactionTime) }}</span>
                    </DescriptionsItem>
                  </Descriptions>
                </div>
              </div>

              <!-- 推荐信息 -->
              <div v-if="detail.recommendStatus && detail.recommendStatus > 0" class="info-card recommend-card">
                <div class="info-card-header">
                  <IconifyIcon icon="ep:medal" class="card-icon" />
                  <span>推荐信息</span>
                </div>
                <div class="info-card-body">
                  <Descriptions :column="2" bordered size="small">
                    <DescriptionsItem label="推荐状态">
                      <Tag :color="detail.recommendStatus === 2 ? 'purple' : 'blue'" style="border-radius: 4px;">
                        {{ detail.recommendStatus === 2 ? '已纳入推广库' : '已推荐至国家局' }}
                      </Tag>
                    </DescriptionsItem>
                    <DescriptionsItem label="推荐时间">
                      <span class="field-value">{{ formatDateTime(detail.recommendTime) }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="推荐意见" :span="2">
                      <div class="field-content" v-html="hasContent(detail.recommendOpinion) ? detail.recommendOpinion : '<span class=\'empty-hint\'>暂无</span>'"></div>
                    </DescriptionsItem>
                  </Descriptions>
                </div>
              </div>

              <!-- 审核信息 -->
              <div v-if="detail.auditStatus || detail.auditOpinion" class="info-card">
                <div class="info-card-header">
                  <IconifyIcon icon="ep:stamp" class="card-icon" />
                  <span>审核信息</span>
                </div>
                <div class="info-card-body">
                  <Descriptions :column="2" bordered size="small">
                    <DescriptionsItem label="审核状态">
                      <span class="field-value">{{ detail.auditStatus || '-' }}</span>
                    </DescriptionsItem>
                    <DescriptionsItem label="审核意见" :span="2">
                      <div class="field-content" v-html="hasContent(detail.auditOpinion) ? detail.auditOpinion : '<span class=\'empty-hint\'>暂无</span>'"></div>
                    </DescriptionsItem>
                  </Descriptions>
                </div>
              </div>
            </div>
          </Tabs.TabPane>

          <!-- 附件 -->
          <Tabs.TabPane key="attachment" tab="附件">
            <div class="tab-content">
              <div v-if="detail.attachmentIds" class="attachment-grid">
                <a
                  v-for="(attachmentId, index) in detail.attachmentIds.split(',').filter(Boolean)"
                  :key="index"
                  :href="getAttachmentUrl(attachmentId)"
                  target="_blank"
                  class="attachment-card"
                >
                  <div class="attachment-icon">
                    <IconifyIcon icon="ep:folder" />
                  </div>
                  <div class="attachment-info">
                    <div class="attachment-name">{{ getAttachmentName(attachmentId, index) }}</div>
                    <div class="attachment-action">
                      <IconifyIcon icon="ep:download" />
                      <span>点击下载</span>
                    </div>
                  </div>
                </a>
              </div>
              <div v-else class="empty-state">
                <IconifyIcon icon="ep:files" class="empty-icon" />
                <div class="empty-text">暂无附件</div>
              </div>
            </div>
          </Tabs.TabPane>
        </Tabs>
      </div>
    </div>
  </Modal>
</template>

<style lang="scss" scoped>
.loading-center {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.detail-wrapper {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ========== 顶部标题区 ========== */
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: linear-gradient(135deg, hsl(var(--primary) / 0.04) 0%, hsl(var(--background)) 100%);
  border-bottom: 1px solid hsl(var(--border));
  gap: 16px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.achievement-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  :deep(svg) {
    width: 28px;
    height: 28px;
  }
}

.header-info {
  flex: 1;
  min-width: 0;
}

.achievement-title {
  font-size: 18px;
  font-weight: 700;
  color: hsl(var(--foreground));
  margin: 0 0 8px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-shrink: 0;
}

.header-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: hsl(var(--muted-foreground));

  :deep(svg) {
    width: 14px;
    height: 14px;
    color: hsl(var(--muted-foreground) / 0.7);
  }
}

/* ========== 主体区 ========== */
.detail-body {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.achievement-tabs {
  :deep(.ant-tabs-nav) {
    margin: 0;
    padding: 0 24px;
    background: hsl(var(--background));

    &::before {
      border-bottom-color: hsl(var(--border));
    }
  }

  :deep(.ant-tabs-tab) {
    padding: 12px 0;
    font-size: 14px;

    &:hover {
      color: hsl(var(--primary));
    }

    &.ant-tabs-tab-active .ant-tabs-tab-btn {
      color: hsl(var(--primary));
      font-weight: 600;
    }
  }

  :deep(.ant-tabs-ink-bar) {
    background: hsl(var(--primary));
  }

  :deep(.ant-tabs-content) {
    padding: 0;
  }
}

.tab-content {
  padding: 20px 24px 24px;
}

/* ========== 信息卡片 ========== */
.info-card {
  background: hsl(var(--background));
  border: 1px solid hsl(var(--border));
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 16px;
  transition: box-shadow 0.2s ease;

  &:hover {
    box-shadow: 0 2px 8px hsl(var(--primary) / 0.08);
  }

  &:last-child {
    margin-bottom: 0;
  }
}

.info-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: hsl(var(--primary) / 0.03);
  border-bottom: 1px solid hsl(var(--border));
  font-size: 14px;
  font-weight: 600;
  color: hsl(var(--foreground));

  .card-icon {
    width: 16px;
    height: 16px;
    color: hsl(var(--primary));
  }
}

.info-card-body {
  padding: 16px;
}

/* ========== 数字统计网格 ========== */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-card {
  background: hsl(var(--secondary) / 0.2);
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
  padding: 14px 16px;
  text-align: center;
  transition: all 0.2s ease;

  &:hover {
    background: hsl(var(--secondary) / 0.3);
    transform: translateY(-1px);
  }

  &.highlight {
    background: hsl(var(--primary) / 0.06);
    border-color: hsl(var(--primary) / 0.2);

    .stat-value {
      color: hsl(var(--primary));
    }
  }
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: hsl(var(--foreground));
  line-height: 1.2;
}

.stat-unit {
  font-size: 13px;
  font-weight: 400;
  margin-left: 2px;
}

.stat-label {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  margin-top: 4px;
}

/* ========== Descriptions 样式 ========== */
:deep(.ant-descriptions) {
  .ant-descriptions-item-label {
    color: hsl(var(--muted-foreground));
    background: hsl(var(--secondary) / 0.25);
    min-width: 110px;
    font-size: 13px;
    padding: 10px 14px;
    border-color: hsl(var(--border) / 0.6);
  }

  .ant-descriptions-item-content {
    color: hsl(var(--foreground));
    font-size: 13px;
    padding: 10px 14px;
    border-color: hsl(var(--border) / 0.6);
  }

  .ant-descriptions-item {
    &:last-child {
      .ant-descriptions-item-content {
        border-right: 1px solid hsl(var(--border) / 0.6);
      }
    }
  }
}

/* ========== 字段值 ========== */
.field-value {
  color: hsl(var(--foreground));
  font-weight: 500;

  &.highlight {
    color: hsl(var(--primary));
    font-weight: 600;
  }
}

.empty-hint {
  color: hsl(var(--muted-foreground) / 0.6);
  font-style: italic;
}

/* ========== 富文本内容 ========== */
.field-content {
  line-height: 1.9;
  color: hsl(var(--foreground));

  :deep(p) {
    margin: 0 0 0.5em;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(div) {
    margin-bottom: 0.3em;
  }

  :deep(ul), :deep(ol) {
    margin: 0.3em 0;
    padding-left: 1.5em;
  }

  :deep(li) {
    margin: 0.2em 0;
  }
}

/* ========== 附件 ========== */
.attachment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.attachment-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: hsl(var(--background));
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.2s ease;
  cursor: pointer;

  &:hover {
    border-color: hsl(var(--primary) / 0.4);
    background: hsl(var(--primary) / 0.03);
    box-shadow: 0 2px 8px hsl(var(--primary) / 0.1);
    transform: translateY(-1px);
    text-decoration: none;
  }
}

.attachment-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: hsl(var(--primary) / 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  :deep(svg) {
    width: 20px;
    height: 20px;
    color: hsl(var(--primary));
  }
}

.attachment-info {
  flex: 1;
  min-width: 0;
}

.attachment-name {
  font-size: 13px;
  font-weight: 500;
  color: hsl(var(--foreground));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 3px;
}

.attachment-action {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: hsl(var(--primary));

  :deep(svg) {
    width: 12px;
    height: 12px;
  }
}

/* ========== 空状态 ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  gap: 12px;

  .empty-icon {
    width: 48px;
    height: 48px;
    color: hsl(var(--muted-foreground) / 0.4);
  }

  .empty-text {
    font-size: 14px;
    color: hsl(var(--muted-foreground) / 0.7);
  }
}
</style>
