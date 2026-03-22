<script lang="ts" setup>
import type { DeclareAchievementApi } from '#/api/declare/achievement';

import { Descriptions, DescriptionsItem, Tag } from 'ant-design-vue';

// 成果类型
const ACHIEVEMENT_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '系统功能', color: '#4A8F72' },
  2: { label: '数据集', color: '#1890ff' },
  3: { label: '科研成果', color: '#722ed1' },
  4: { label: '管理经验', color: '#fa8c16' },
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

defineProps<{
  detail: DeclareAchievementApi.Achievement;
}>();

function getAchievementTypeInfo(type?: number) {
  return ACHIEVEMENT_TYPE_MAP[type || 0] || { label: '-', color: '#999' };
}

function getFlowTypeInfo(value?: number) {
  return FLOW_TYPE_MAP[value || 0] || { label: '-', color: '' };
}

function getReplicationInfo(value?: number) {
  return REPLICATION_MAP[value || 0] || { label: '-', color: '' };
}

function formatTime(time: string | undefined): string {
  if (!time) return '-';
  return time.substring(0, 19).replace('T', ' ');
}

function formatDate(time: string | undefined): string {
  if (!time) return '-';
  return time.substring(0, 10);
}

function formatMoney(amount?: number): string {
  if (amount == null) return '-';
  return `${amount} 万元`;
}
</script>

<template>
  <div class="basic-info">
    <!-- 成果基本信息 -->
    <Descriptions :column="3" bordered size="small" class="info-descriptions">
      <DescriptionsItem label="成果名称" :span="3">
        {{ detail.achievementName || '-' }}
      </DescriptionsItem>
      <DescriptionsItem label="成果类型">
        <Tag :color="getAchievementTypeInfo(detail.achievementType).color">
          {{ getAchievementTypeInfo(detail.achievementType).label }}
        </Tag>
      </DescriptionsItem>
      <DescriptionsItem label="应用领域">
        {{ detail.applicationField || '-' }}
      </DescriptionsItem>
      <DescriptionsItem label="可复制性">
        <Tag v-if="detail.replicationValue" :color="getReplicationInfo(detail.replicationValue).color">
          {{ getReplicationInfo(detail.replicationValue).label }}
        </Tag>
        <span v-else>-</span>
      </DescriptionsItem>
      <DescriptionsItem label="推广范围">
        {{ detail.promotionScope ? PROMOTION_SCOPE_MAP[detail.promotionScope] || '-' : '-' }}
      </DescriptionsItem>
      <DescriptionsItem label="推广次数">
        {{ detail.promotionCount != null ? detail.promotionCount + ' 次' : '-' }}
      </DescriptionsItem>
      <DescriptionsItem label="转化类型">
        {{ detail.transformType ? TRANSFORM_TYPE_MAP[detail.transformType] || '-' : '-' }}
      </DescriptionsItem>
      <DescriptionsItem label="关联项目">
        {{ detail.projectName || '-' }}
      </DescriptionsItem>
      <DescriptionsItem label="创建时间">
        {{ formatTime(detail.createTime) }}
      </DescriptionsItem>
      <DescriptionsItem label="成果描述" :span="3">
        <div class="field-content" v-html="detail.description || '暂无'"></div>
      </DescriptionsItem>
      <DescriptionsItem label="应用效果" :span="3">
        <div class="field-content" v-html="detail.effectDescription || '暂无'"></div>
      </DescriptionsItem>
    </Descriptions>

    <!-- 数据集信息 -->
    <template v-if="detail.dataName || detail.dataType">
      <h4 class="section-title">数据集信息</h4>
      <Descriptions :column="3" bordered size="small" class="info-descriptions">
        <DescriptionsItem label="数据名称">
          {{ detail.dataName || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="数据类型">
          {{ detail.dataType || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="数据来源">
          {{ detail.dataSource || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="数据规模">
          {{ detail.dataVolume || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="数据质量">
          {{ detail.dataQuality ? DATA_QUALITY_MAP[detail.dataQuality] || '-' : '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="共享范围">
          {{ detail.shareScope ? SHARE_SCOPE_MAP[detail.shareScope] || '-' : '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="数据描述" :span="3">
          {{ detail.dataDescription || '暂无' }}
        </DescriptionsItem>
      </Descriptions>
    </template>

    <!-- 数据流通信息 -->
    <template v-if="detail.flowType">
      <h4 class="section-title">数据流通信息</h4>
      <Descriptions :column="3" bordered size="small" class="info-descriptions">
        <DescriptionsItem label="流通类型">
          <Tag v-if="detail.flowType" :color="getFlowTypeInfo(detail.flowType).color">
            {{ getFlowTypeInfo(detail.flowType).label }}
          </Tag>
          <span v-else>-</span>
        </DescriptionsItem>
        <DescriptionsItem label="流通对象">
          {{ detail.flowObject || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="流通目的">
          {{ detail.flowPurpose || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="流通开始时间">
          {{ formatDate(detail.startTime) }}
        </DescriptionsItem>
        <DescriptionsItem label="流通结束时间">
          {{ formatDate(detail.endTime) }}
        </DescriptionsItem>
        <DescriptionsItem label="安全备案编号">
          {{ detail.securityFilingNo || '-' }}
        </DescriptionsItem>
      </Descriptions>
    </template>

    <!-- 证书与交易信息 -->
    <template v-if="detail.certificateCount || detail.propertyCount || detail.transactionCount">
      <h4 class="section-title">证书与交易信息</h4>
      <Descriptions :column="3" bordered size="small" class="info-descriptions">
        <DescriptionsItem label="数据产品证书数">
          {{ detail.certificateCount ?? '-' }} 个
        </DescriptionsItem>
        <DescriptionsItem label="数据产权登记证数">
          {{ detail.propertyCount ?? '-' }} 个
        </DescriptionsItem>
        <DescriptionsItem label="累计交易金额">
          {{ formatMoney(detail.transactionAmount) }}
        </DescriptionsItem>
        <DescriptionsItem label="完成交易的产品数">
          {{ detail.transactionCount ?? '-' }} 个
        </DescriptionsItem>
        <DescriptionsItem label="交易对象">
          {{ detail.transactionObject || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="交易完成时间">
          {{ formatDate(detail.transactionTime) }}
        </DescriptionsItem>
      </Descriptions>
    </template>

    <!-- 推荐信息 -->
    <template v-if="detail.recommendStatus && detail.recommendStatus > 0">
      <h4 class="section-title">推荐信息</h4>
      <Descriptions :column="3" bordered size="small" class="info-descriptions">
        <DescriptionsItem label="推荐状态">
          <Tag :color="detail.recommendStatus === 2 ? 'purple' : 'blue'">
            {{ detail.recommendStatus === 2 ? '已纳入推广库' : '已推荐至国家局' }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="推荐时间">
          {{ formatTime(detail.recommendTime) }}
        </DescriptionsItem>
        <DescriptionsItem label="推荐意见">
          {{ detail.recommendOpinion || '-' }}
        </DescriptionsItem>
      </Descriptions>
    </template>
  </div>
</template>

<style scoped>
.basic-info {
  padding: 8px 0;
}

.info-descriptions {
  margin-bottom: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin: 16px 0 12px;
  padding-left: 8px;
  border-left: 3px solid #4A8F72;
}

.field-content {
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
