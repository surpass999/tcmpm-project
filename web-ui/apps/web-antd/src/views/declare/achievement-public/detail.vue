<script lang="ts" setup>
import type { AchievementClientApi } from '#/api/declare/achievement-client';

import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Empty, Tag } from 'ant-design-vue';

import dayjs from 'dayjs';

import { getPublishedAchievement } from '#/api/declare/achievement-client';

// 成果类型选项
const ACHIEVEMENT_TYPE_MAP: Record<number, { label: string; color: string; icon: string }> = {
  1: { label: '系统功能', color: '#4A8F72', icon: 'carbon:function' },
  2: { label: '数据集', color: '#1890ff', icon: 'carbon:chart-bar' },
  3: { label: '科研成果', color: '#722ed1', icon: 'carbon:science' },
  4: { label: '管理经验', color: '#fa8c16', icon: 'carbon:task-complete' },
};

/** 移除 HTML 标签，保留纯文本 */
function stripHtml(html: string | undefined | null): string {
  if (!html) return '';
  return html.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();
}

// 推广范围
const PROMOTION_SCOPE_MAP: Record<number, string> = {
  1: '院内',
  2: '省级',
  3: '全国',
};

// 转化类型
const TRANSFORM_TYPE_MAP: Record<number, string> = {
  1: '标准规范',
  2: '创新模式',
  3: '典型案例',
};

// 可复制性
const REPLICATION_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '高', color: 'green' },
  2: { label: '中', color: 'orange' },
  3: { label: '低', color: 'red' },
};

// 流通类型
const FLOW_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '内部使用', color: 'blue' },
  2: { label: '对外共享', color: 'purple' },
  3: { label: '交易', color: 'orange' },
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

const detail = ref<AchievementClientApi.AchievementDetail | null>(null);
const loading = ref(false);
const attachmentList = ref<{ name: string; url: string }[]>([]);

const [Modal, modalApi] = useVbenModal({
  onOpenChange: async (isOpen) => {
    if (isOpen) {
      const data = modalApi.getData<AchievementClientApi.AchievementSummary>();
      if (data?.id) {
        loading.value = true;
        try {
          detail.value = await getPublishedAchievement(data.id);

          // 解析附件列表
          const attachmentUrls = detail.value?.attachmentIds;
          if (attachmentUrls) {
            const urls = attachmentUrls.split(',').filter(Boolean);
            attachmentList.value = urls.map((url, index) => {
              // 从 URL 中提取文件名
              const fileName = url.substring(url.lastIndexOf('/') + 1);
              return { name: fileName || `附件${index + 1}`, url };
            });
          } else {
            attachmentList.value = [];
          }
        } finally {
          loading.value = false;
        }
      }
    }
  },
  onClosed: () => {
    detail.value = null;
    attachmentList.value = [];
  },
});

/** 格式化时间 */
function formatTime(time: string | number | undefined): string {
  if (!time) return '-';
  return dayjs(time).format('YYYY-MM-DD HH:mm');
}

function formatDate(time: string | number | undefined): string {
  if (!time) return '-';
  return dayjs(time).format('YYYY-MM-DD');
}

function formatMoney(amount: number | undefined): string {
  if (amount == null) return '-';
  return `${amount} 万元`;
}

function getAchievementTypeInfo(type?: number) {
  return ACHIEVEMENT_TYPE_MAP[type || 0] || { label: '未知', color: '#999', icon: 'carbon:trophy' };
}

function getReplicationInfo(value?: number) {
  return REPLICATION_MAP[value || 0] || { label: '-', color: '' };
}

function getFlowTypeInfo(value?: number) {
  return FLOW_TYPE_MAP[value || 0] || { label: '-', color: '' };
}
</script>

<template>
  <Modal
    title="成果详情"
    class="achievement-detail-modal w-[90%]"
    :show-cancel-button="false"
    :show-confirm-button="false"
    :closable="true"
  >
    <div v-if="loading" class="flex items-center justify-center py-16">
      <IconifyIcon icon="eos-icons:bubble-loading" class="text-3xl text-[#4A8F72]" />
    </div>

    <div v-else-if="detail" class="detail-content">
      <!-- 头部信息 -->
      <div
        class="detail-header"
        :style="{
          background: `linear-gradient(135deg, ${getAchievementTypeInfo(detail.achievementType).color}ee 0%, ${getAchievementTypeInfo(detail.achievementType).color}99 50%, ${getAchievementTypeInfo(detail.achievementType).color}66 100%)`,
        }"
      >
        <div class="detail-header__icon">
          <div
            class="detail-header__icon-inner"
            :style="{ backgroundColor: getAchievementTypeInfo(detail.achievementType).color }"
          >
            <IconifyIcon
              :icon="getAchievementTypeInfo(detail.achievementType).icon"
              class="detail-header__icon-svg"
            />
          </div>
        </div>
        <div class="detail-header__info">
          <div class="detail-header__tags">
            <Tag :color="getAchievementTypeInfo(detail.achievementType).color" class="detail-header__tag">
              {{ getAchievementTypeInfo(detail.achievementType).label }}
            </Tag>
            <Tag v-if="detail.promotionScope" class="detail-header__tag detail-header__tag--white">
              <IconifyIcon icon="ep:location" class="mr-1" />
              {{ PROMOTION_SCOPE_MAP[detail.promotionScope] || '-' }}
            </Tag>
            <Tag v-if="detail.transformType" class="detail-header__tag detail-header__tag--white">
              <IconifyIcon icon="carbon:data-structured" class="mr-1" />
              {{ TRANSFORM_TYPE_MAP[detail.transformType] || '-' }}
            </Tag>
          </div>
          <h2 class="detail-header__name">{{ detail.achievementName }}</h2>
          <div class="detail-header__meta">
            <span v-if="detail.projectName">
              <IconifyIcon icon="ep:office-building" class="mr-1" />
              {{ detail.projectName }}
            </span>
            <span v-if="detail.applicationField">
              <IconifyIcon icon="ep:price-tag" class="mr-1" />
              {{ detail.applicationField }}
            </span>
          </div>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="detail-stats">
        <div class="detail-stats__card">
          <div class="detail-stats__icon" style="background: rgba(24, 144, 255, 0.1);">
            <IconifyIcon icon="ph:share-network" style="color: #1890ff;" />
          </div>
          <div class="detail-stats__content">
            <div class="detail-stats__value">{{ detail.promotionCount ?? 0 }}</div>
            <div class="detail-stats__label">推广次数</div>
          </div>
        </div>
        <div class="detail-stats__card">
          <div class="detail-stats__icon" style="background: rgba(114, 46, 209, 0.1);">
            <IconifyIcon icon="carbon:certificate" style="color: #722ed1;" />
          </div>
          <div class="detail-stats__content">
            <div class="detail-stats__value">{{ detail.certificateCount ?? 0 }}</div>
            <div class="detail-stats__label">数据产品证书数</div>
          </div>
        </div>
        <div class="detail-stats__card">
          <div class="detail-stats__icon" style="background: rgba(250, 140, 22, 0.1);">
            <IconifyIcon icon="carbon:task-complete" style="color: #fa8c16;" />
          </div>
          <div class="detail-stats__content">
            <div class="detail-stats__value">{{ detail.propertyCount ?? 0 }}</div>
            <div class="detail-stats__label">数据产权登记证数</div>
          </div>
        </div>
        <div class="detail-stats__card">
          <div class="detail-stats__icon" style="background: rgba(74, 143, 114, 0.1);">
            <IconifyIcon icon="carbon:money" style="color: #4A8F72;" />
          </div>
          <div class="detail-stats__content">
            <div class="detail-stats__value">{{ formatMoney(detail.transactionAmount) }}</div>
            <div class="detail-stats__label">累计交易金额</div>
          </div>
        </div>
      </div>

      <!-- 成果描述卡片 -->
      <div v-if="detail.description" class="detail-section">
        <div class="detail-section__header">
          <IconifyIcon icon="ep-document" class="detail-section__icon" />
          <span>成果描述</span>
        </div>
        <div class="detail-section__body" v-html="detail.description || '暂无描述'"></div>
      </div>

      <!-- 应用效果卡片 -->
      <div v-if="detail.effectDescription" class="detail-section">
        <div class="detail-section__header">
          <IconifyIcon icon="ep-data-analysis" class="detail-section__icon" />
          <span>应用效果</span>
        </div>
        <div class="detail-section__body" v-html="detail.effectDescription || '暂无效果描述'"></div>
      </div>

      <!-- 基本信息卡片 -->
      <div class="detail-section">
        <div class="detail-section__header">
          <IconifyIcon icon="ep-info-filled" class="detail-section__icon" />
          <span>基本信息</span>
        </div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-item__label">可复制性</span>
            <span class="detail-item__value">
              <Tag v-if="detail.replicationValue" :color="getReplicationInfo(detail.replicationValue).color">
                {{ getReplicationInfo(detail.replicationValue).label }}
              </Tag>
              <span v-else>-</span>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">发布时间</span>
            <span class="detail-item__value">{{ formatDate(detail.createTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 数据集信息卡片 -->
      <template v-if="detail.dataName || detail.dataType || detail.dataVolume">
        <div class="detail-section detail-section--data">
          <div class="detail-section__header">
            <IconifyIcon icon="carbon:chart-bar" class="detail-section__icon" />
            <span>数据集信息</span>
          </div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-item__label">数据名称</span>
              <span class="detail-item__value">{{ detail.dataName || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">数据类型</span>
              <span class="detail-item__value">{{ detail.dataType || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">数据来源</span>
              <span class="detail-item__value">{{ detail.dataSource || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">数据规模</span>
              <span class="detail-item__value">{{ detail.dataVolume || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">数据质量</span>
              <span class="detail-item__value">
                {{ detail.dataQuality ? DATA_QUALITY_MAP[detail.dataQuality] || '-' : '-' }}
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">共享范围</span>
              <span class="detail-item__value">
                {{ detail.shareScope ? SHARE_SCOPE_MAP[detail.shareScope] || '-' : '-' }}
              </span>
            </div>
          </div>
          <div v-if="detail.dataDescription" class="detail-field">
            <span class="detail-field__label">数据描述</span>
            <div class="detail-field__content">{{ detail.dataDescription }}</div>
          </div>
        </div>
      </template>

      <!-- 数据流通信息卡片 -->
      <template v-if="detail.flowType">
        <div class="detail-section">
          <div class="detail-section__header">
            <IconifyIcon icon="carbon:flow" class="detail-section__icon" />
            <span>数据流通信息</span>
          </div>
          <div class="detail-grid detail-grid--flow">
            <div class="detail-item">
              <span class="detail-item__label">流通类型</span>
              <span class="detail-item__value">
                <Tag v-if="detail.flowType" :color="getFlowTypeInfo(detail.flowType).color">
                  {{ getFlowTypeInfo(detail.flowType).label }}
                </Tag>
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">流通对象</span>
              <span class="detail-item__value">{{ detail.flowObject || '-' }}</span>
            </div>
            <!-- 流通目的：整行展示，支持富文本 HTML -->
            <div class="detail-item detail-item--flow-purpose">
              <span class="detail-item__label">流通目的</span>
              <div
                v-if="detail.flowPurpose"
                class="detail-flow-purpose"
                v-html="detail.flowPurpose"
              />
              <div v-else class="detail-flow-purpose detail-flow-purpose--empty">-</div>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">流通开始时间</span>
              <span class="detail-item__value">{{ formatDate(detail.startTime) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">流通结束时间</span>
              <span class="detail-item__value">{{ formatDate(detail.endTime) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">安全备案编号</span>
              <span class="detail-item__value">{{ detail.securityFilingNo || '-' }}</span>
            </div>
          </div>
        </div>
      </template>

      <!-- 附件材料卡片 -->
      <template v-if="attachmentList.length > 0">
        <div class="detail-section">
          <div class="detail-section__header">
            <IconifyIcon icon="ep-paperclip" class="detail-section__icon" />
            <span>附件材料</span>
          </div>
          <div class="attachment-grid">
            <a
              v-for="(file, index) in attachmentList"
              :key="index"
              :href="file.url"
              target="_blank"
              class="attachment-item"
            >
              <IconifyIcon icon="carbon:document" class="attachment-item__icon" />
              <span class="attachment-item__name">{{ file.name }}</span>
              <IconifyIcon icon="ep-download" class="attachment-item__download" />
            </a>
          </div>
        </div>
      </template>

      <!-- 推荐意见卡片 -->
      <template v-if="detail.recommendOpinion">
        <div class="detail-section detail-section--recommend">
          <div class="detail-section__header">
            <IconifyIcon icon="ep-magic-stick" class="detail-section__icon" />
            <span>推荐意见</span>
          </div>
          <div class="recommend-content">
            <IconifyIcon icon="ep-quotation" class="recommend-content__quote" />
            {{ detail.recommendOpinion }}
          </div>
        </div>
      </template>

      <!-- 底部返回按钮 -->
      <div class="detail-footer">
        <button class="detail-footer__btn" @click="modalApi.close()">
          <IconifyIcon icon="ep-arrow-left" class="mr-2" />
          返回列表
        </button>
      </div>
    </div>

    <div v-else class="flex items-center justify-center py-16">
      <Empty description="未找到成果详情" />
    </div>
  </Modal>
</template>

<style scoped>
/* 头部区域 */
.detail-header {
  display: flex;
  gap: 24px;
  padding: 28px;
  border-radius: 12px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.detail-header::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -10%;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.detail-header::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: 20%;
  width: 150px;
  height: 150px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

.detail-header__icon {
  flex-shrink: 0;
  z-index: 1;
}

.detail-header__icon-inner {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.detail-header__icon-svg {
  font-size: 36px;
  color: #fff;
}

.detail-header__info {
  flex: 1;
  min-width: 0;
  z-index: 1;
}

.detail-header__tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.detail-header__tag {
  border: none;
  font-weight: 500;
}

.detail-header__tag--white {
  background: rgba(255, 255, 255, 0.95) !important;
  color: #595959 !important;
}

.detail-header__name {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px 0;
  line-height: 1.3;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.detail-header__meta {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.detail-header__meta span {
  display: inline-flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.15);
  padding: 4px 12px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
}

/* 统计卡片 */
.detail-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.detail-stats__card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s;
}

.detail-stats__card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.detail-stats__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.detail-stats__content {
  flex: 1;
  min-width: 0;
}

.detail-stats__value {
  font-size: 20px;
  font-weight: 700;
  color: #262626;
  line-height: 1.2;
}

.detail-stats__label {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

/* 通用内容区块 */
.detail-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.detail-section__header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-section__icon {
  font-size: 18px;
  color: #4A8F72;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 12px;
}

/* 数据流通：流通目的独占一行 */
.detail-grid--flow .detail-item--flow-purpose {
  grid-column: 1 / -1;
}

.detail-flow-purpose {
  font-size: 14px;
  color: #595959;
  line-height: 1.8;
  padding: 14px 16px;
  background: #fafafa;
  border-radius: 8px;
  border-left: 3px solid #4a8f72;
  min-height: 1em;
  word-break: break-word;
}

.detail-flow-purpose--empty {
  color: #8c8c8c;
  font-weight: 400;
}

/* v-html 富文本内部样式（scoped 需 :deep） */
.detail-flow-purpose :deep(h1),
.detail-flow-purpose :deep(h2),
.detail-flow-purpose :deep(h3) {
  margin: 0.75em 0 0.5em;
  font-size: 1.05em;
  font-weight: 600;
  color: #262626;
}

.detail-flow-purpose :deep(h1:first-child),
.detail-flow-purpose :deep(h2:first-child),
.detail-flow-purpose :deep(h3:first-child) {
  margin-top: 0;
}

.detail-flow-purpose :deep(p) {
  margin: 0 0 0.65em;
}

.detail-flow-purpose :deep(p:last-child) {
  margin-bottom: 0;
}

.detail-flow-purpose :deep(ul),
.detail-flow-purpose :deep(ol) {
  margin: 0.5em 0;
  padding-left: 1.25em;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item__label {
  font-size: 12px;
  color: #8c8c8c;
}

.detail-item__value {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.detail-field {
  margin-top: 12px;
}

.detail-field__label {
  font-size: 12px;
  color: #8c8c8c;
  display: block;
  margin-bottom: 8px;
}

.detail-field__content {
  font-size: 14px;
  color: #595959;
  line-height: 1.8;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  border-left: 3px solid #4A8F72;
}

/* 推荐意见特殊样式 */
.detail-section--recommend .detail-field__content {
  border: none;
  background: linear-gradient(135deg, #f0f9f5 0%, #e8f5ee 100%);
}

.recommend-content {
  position: relative;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f0f9f5 0%, #e8f5ee 100%);
  border-radius: 10px;
  font-size: 14px;
  color: #595959;
  line-height: 1.8;
  border-left: 4px solid #4A8F72;
}

.recommend-content__quote {
  position: absolute;
  top: -8px;
  left: 12px;
  font-size: 40px;
  color: #4A8F72;
  opacity: 0.3;
}

/* 附件网格 */
.attachment-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 8px;
  color: #262626;
  text-decoration: none;
  font-size: 13px;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.attachment-item:hover {
  background: #f0f9f5;
  border-color: #4A8F72;
  color: #4A8F72;
}

.attachment-item__icon {
  font-size: 20px;
  color: #4A8F72;
  flex-shrink: 0;
}

.attachment-item__name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-item__download {
  font-size: 14px;
  color: #8c8c8c;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.attachment-item:hover .attachment-item__download {
  opacity: 1;
  color: #4A8F72;
}

/* 底部按钮 */
.detail-footer {
  display: flex;
  justify-content: center;
  padding-top: 16px;
}

.detail-footer__btn {
  display: inline-flex;
  align-items: center;
  padding: 10px 28px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.detail-footer__btn:hover {
  background: #4A8F72;
  border-color: #4A8F72;
  color: #fff;
}

/* 响应式 */
@media (max-width: 768px) {
  .detail-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .detail-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .detail-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .attachment-grid {
    grid-template-columns: 1fr;
  }
}
</style>
