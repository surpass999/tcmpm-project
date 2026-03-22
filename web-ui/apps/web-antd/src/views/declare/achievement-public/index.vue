<script lang="ts" setup>
import type { AchievementClientApi } from '#/api/declare/achievement-client';

import { computed, onMounted, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Input, Select, Button, RangePicker, Pagination } from 'ant-design-vue';

import dayjs from 'dayjs';

import { getPublishedAchievementList } from '#/api/declare/achievement-client';

import AchievementDetailModal from './detail.vue';

// 成果类型选项
const ACHIEVEMENT_TYPE_OPTIONS = [
  { label: '全部类型', value: undefined },
  { label: '系统功能', value: 1 },
  { label: '数据集', value: 2 },
  { label: '科研成果', value: 3 },
  { label: '管理经验', value: 4 },
];

// 推广范围选项
const PROMOTION_SCOPE_OPTIONS = [
  { label: '全部范围', value: undefined },
  { label: '院内', value: 1 },
  { label: '省级', value: 2 },
  { label: '全国', value: 3 },
];

// 转化类型选项
const TRANSFORM_TYPE_OPTIONS = [
  { label: '全部', value: undefined },
  { label: '标准规范', value: 1 },
  { label: '创新模式', value: 2 },
  { label: '典型案例', value: 3 },
];

// 成果类型颜色
const ACHIEVEMENT_TYPE_COLORS: Record<number, string> = {
  1: '#4A8F72',
  2: '#1890ff',
  3: '#722ed1',
  4: '#fa8c16',
};

// 成果类型图标 (Iconify 图标)
const ACHIEVEMENT_TYPE_ICONS: Record<number, string> = {
  1: 'carbon:function',
  2: 'carbon:chart-bar',
  3: 'carbon:science',
  4: 'carbon:task-complete',
};

/** 移除 HTML 标签，保留纯文本 */
function stripHtml(html: string | undefined | null): string {
  if (!html) return '';
  return html.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();
}

const loading = ref(false);
const list = ref<AchievementClientApi.AchievementSummary[]>([]);

// 筛选条件
const keyword = ref('');
const selectedType = ref<number | undefined>(undefined);
const selectedScope = ref<number | undefined>(undefined);
const selectedTransform = ref<number | undefined>(undefined);
const dateRange = ref<[string, string] | null>(null);

// 分页
const currentPage = ref(1);
const pageSize = ref(10);
const total = computed(() => filteredList.value.length);

/** 当前页数据 */
const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredList.value.slice(start, end);
});

/** 切换页码 */
function handlePageChange(page: number) {
  currentPage.value = page;
}

/** 切换每页条数 */
function handlePageSizeChange(size: number) {
  pageSize.value = size;
  currentPage.value = 1;
}

// 详情弹窗
const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: AchievementDetailModal,
  destroyOnClose: true,
});

/** 加载数据 */
async function loadData() {
  loading.value = true;
  try {
    const data = await getPublishedAchievementList();
    list.value = data || [];
  } finally {
    loading.value = false;
  }
}

/** 查看详情 */
function handleDetail(row: AchievementClientApi.AchievementSummary) {
  detailModalApi.setData(row).open();
}

/** 获取成果类型标签 */
function getAchievementTypeLabel(type?: number): string {
  return ACHIEVEMENT_TYPE_OPTIONS.find((o) => o.value === type)?.label || '未知';
}

/** 获取成果类型颜色 */
function getAchievementTypeColor(type?: number): string {
  return ACHIEVEMENT_TYPE_COLORS[type || 0] || '#999';
}

/** 获取成果类型图标 */
function getAchievementTypeIcon(type?: number): string {
  return ACHIEVEMENT_TYPE_ICONS[type || 0] || 'carbon:trophy';
}

/** 获取推广范围标签 */
function getPromotionScopeLabel(scope?: number): string {
  return PROMOTION_SCOPE_OPTIONS.find((o) => o.value === scope)?.label || '-';
}

/** 获取转化类型标签 */
function getTransformTypeLabel(transform?: number): string {
  return TRANSFORM_TYPE_OPTIONS.find((o) => o.value === transform)?.label || '-';
}

/** 解析时间为 dayjs */
function parseTime(time: string | number | undefined): dayjs.Dayjs | null {
  if (time == null) return null;
  if (typeof time === 'number') return dayjs(time);
  return dayjs(time).isValid() ? dayjs(time) : null;
}

/** 格式化时间 */
function formatTime(time: string | number | undefined): string {
  const d = parseTime(time);
  return d ? d.format('YYYY-MM-DD') : '-';
}

/** 筛选后的列表 */
const filteredList = computed(() => {
  let result = list.value;

  // 关键词：成果名称、应用领域、项目名称、描述
  const kw = keyword.value?.trim();
  if (kw) {
    const lower = kw.toLowerCase();
    result = result.filter(
      (item) =>
        (item.achievementName && item.achievementName.toLowerCase().includes(lower)) ||
        (item.applicationField && item.applicationField.toLowerCase().includes(lower)) ||
        (item.projectName && item.projectName.toLowerCase().includes(lower)) ||
        (item.description && item.description.toLowerCase().includes(lower)),
    );
  }

  // 成果类型
  const typeVal = selectedType.value;
  if (typeVal !== undefined && typeVal !== null) {
    result = result.filter((item) => item.achievementType === typeVal);
  }

  // 推广范围
  const scopeVal = selectedScope.value;
  if (scopeVal !== undefined && scopeVal !== null) {
    result = result.filter((item) => item.promotionScope === scopeVal);
  }

  // 转化类型
  const transformVal = selectedTransform.value;
  if (transformVal !== undefined && transformVal !== null) {
    result = result.filter((item) => item.transformType === transformVal);
  }

  // 时间范围（按创建时间）
  if (dateRange.value && dateRange.value.length === 2) {
    const [startStr, endStr] = dateRange.value;
    const start = dayjs(startStr).startOf('day');
    const end = dayjs(endStr).endOf('day');
    result = result.filter((item) => {
      const t = parseTime(item.createTime);
      if (!t) return false;
      return !t.isBefore(start) && !t.isAfter(end);
    });
  }

  return result;
});

/** 筛选 */
function handleSearch() {
  currentPage.value = 1;
}

/** 重置 */
function handleReset() {
  keyword.value = '';
  selectedType.value = undefined;
  selectedScope.value = undefined;
  selectedTransform.value = undefined;
  dateRange.value = null;
  currentPage.value = 1;
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <Page auto-content-height class="achievement-public-page">
    <DetailModal @refresh="loadData" />

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">成果与推广</h1>
      <p class="page-subtitle">智慧中医医院试点项目成果展示与经验分享平台</p>
    </div>

    <!-- 筛选区块 -->
    <div class="filter-block">
      <div class="filter-block__inner">
        <Input
          v-model:value="keyword"
          class="filter-block__search"
          placeholder="搜索成果名称 / 应用领域 / 项目名称"
          allow-clear
          @press-enter="handleSearch"
        >
          <template #prefix>
            <i class="fas fa-search text-gray-400"></i>
          </template>
        </Input>
        <Select
          v-model:value="selectedType"
          class="filter-block__select"
          placeholder="成果类型"
          allow-clear
          :options="ACHIEVEMENT_TYPE_OPTIONS"
        />
        <Select
          v-model:value="selectedScope"
          class="filter-block__select"
          placeholder="推广范围"
          allow-clear
          :options="PROMOTION_SCOPE_OPTIONS"
        />
        <Select
          v-model:value="selectedTransform"
          class="filter-block__select"
          placeholder="转化类型"
          allow-clear
          :options="TRANSFORM_TYPE_OPTIONS"
        />
        <RangePicker
          v-model:value="dateRange"
          class="filter-block__range"
          format="YYYY-MM-DD"
          allow-clear
        />
        <Button type="primary" class="filter-block__btn" @click="handleSearch">
          筛选
        </Button>
        <Button class="filter-block__btn" @click="handleReset">
          重置
        </Button>
      </div>
    </div>

    <!-- 列表区块 -->
    <div class="list-block">
      <div v-if="loading" class="list-block__loading">
        <i class="fas fa-circle-notch fa-spin text-2xl text-[#4A8F72] mb-3"></i>
        <span class="list-block__loading-text">加载中...</span>
      </div>

      <div v-else-if="filteredList.length === 0" class="list-block__empty">
        <i class="fas fa-trophy text-4xl text-gray-300 mb-3"></i>
        <span class="text-gray-400">暂无成果信息</span>
      </div>

      <ul v-else class="list-block__list">
        <li
          v-for="item in paginatedList"
          :key="item.id"
          class="achievement-card"
          @click="handleDetail(item)"
        >
          <!-- 左侧：图标区域 -->
          <div class="achievement-card__icon">
            <div
              class="achievement-card__icon-inner"
              :style="{ backgroundColor: getAchievementTypeColor(item.achievementType) }"
            >
              <IconifyIcon
                class="achievement-card__icon-svg"
                :icon="getAchievementTypeIcon(item.achievementType)"
              />
            </div>
            <span
              class="achievement-card__type"
              :style="{ backgroundColor: getAchievementTypeColor(item.achievementType) }"
            >
              {{ getAchievementTypeLabel(item.achievementType) }}
            </span>
          </div>

          <!-- 中间：内容区域 -->
          <div class="achievement-card__content">
            <h3 class="achievement-card__title">{{ item.achievementName || '-' }}</h3>

            <!-- 项目信息 -->
            <div class="achievement-card__project">
              <i class="fas fa-hospital text-gray-400 mr-1"></i>
              <span>{{ item.projectName || '未关联项目' }}</span>
            </div>

            <!-- 应用领域 -->
            <div v-if="item.applicationField" class="achievement-card__field">
              <span class="achievement-card__field-tag">
                <i class="fas fa-tag mr-1"></i>
                {{ item.applicationField }}
              </span>
            </div>

            <!-- 描述 -->
            <p v-if="stripHtml(item.description)" class="achievement-card__desc">
              {{ stripHtml(item.description).length > 150 ? stripHtml(item.description).substring(0, 150) + '...' : stripHtml(item.description) }}
            </p>

            <!-- 标签 -->
            <div class="achievement-card__tags">
              <span class="achievement-card__tag achievement-card__tag--scope">
                <i class="fas fa-globe mr-1"></i>
                {{ getPromotionScopeLabel(item.promotionScope) }}
              </span>
              <span v-if="item.transformType" class="achievement-card__tag achievement-card__tag--transform">
                <i class="fas fa-sync mr-1"></i>
                {{ getTransformTypeLabel(item.transformType) }}
              </span>
              <span v-if="item.dataName" class="achievement-card__tag achievement-card__tag--data">
                <i class="fas fa-database mr-1"></i>
                数据集：{{ item.dataName }}
              </span>
            </div>
          </div>

          <!-- 右侧：信息区域 -->
          <div class="achievement-card__meta">
            <div class="achievement-card__meta-item">
              <i class="fas fa-calendar-alt text-gray-400 mr-1"></i>
              <span>{{ formatTime(item.createTime) }}</span>
            </div>
            <div v-if="item.promotionCount" class="achievement-card__meta-item">
              <i class="fas fa-share-alt text-gray-400 mr-1"></i>
              <span>推广 {{ item.promotionCount }} 次</span>
            </div>
          </div>

          <!-- 箭头 -->
          <div class="achievement-card__arrow">
            <i class="fas fa-chevron-right"></i>
          </div>
        </li>
      </ul>

      <!-- 分页 -->
      <div class="list-block__pagination">
        <Pagination
          v-model:current="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :show-size-changer="true"
          :show-quick-jumper="true"
          :page-size-options="['6', '12', '24']"
          @change="handlePageChange"
          @showSizeChange="handlePageSizeChange"
        />
      </div>
    </div>
  </Page>
</template>

<style scoped>
.achievement-public-page {
  padding: 0;
  background: #f5f6f8;
}

/* 页面标题 */
.page-header {
  background: linear-gradient(135deg, #4A8F72 0%, #6db396 100%);
  padding: 32px 24px;
  text-align: center;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

/* 筛选区块 */
.filter-block {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  padding: 16px 24px;
}

.filter-block__inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-block__search {
  width: 300px;
  flex-shrink: 0;
}

.filter-block__select {
  width: 140px;
  flex-shrink: 0;
}

.filter-block__range {
  width: 280px;
  flex-shrink: 0;
}

.filter-block__btn {
  flex-shrink: 0;
}

/* 列表区块 */
.list-block {
  flex: 1;
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.list-block__loading,
.list-block__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.list-block__loading-text {
  margin-top: 12px;
  font-size: 14px;
  color: #999;
}

.list-block__empty {
  color: #999;
  font-size: 14px;
}

.list-block__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.list-block__pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
}

/* 成果卡片 */
.achievement-card {
  display: flex;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.achievement-card:hover {
  border-color: #4A8F72;
  box-shadow: 0 4px 12px rgba(74, 143, 114, 0.15);
}

/* 左侧图标区域 */
.achievement-card__icon {
  width: 110px;
  flex-shrink: 0;
  background: linear-gradient(160deg, #f8faf9 0%, #f0f4f2 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 16px;
  gap: 12px;
  position: relative;
}

.achievement-card__icon::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, transparent, rgba(74, 143, 114, 0.3), transparent);
}

.achievement-card__icon-inner {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.achievement-card:hover .achievement-card__icon-inner {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.achievement-card__icon-svg {
  font-size: 28px;
  color: #fff;
}

.achievement-card__type {
  font-size: 11px;
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
}

/* 中间内容区域 */
.achievement-card__content {
  flex: 1;
  padding: 16px 20px;
  min-width: 0;
}

.achievement-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.achievement-card__project {
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.achievement-card__field {
  margin-bottom: 8px;
}

.achievement-card__field-tag {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  color: #4A8F72;
  background: rgba(74, 143, 114, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
}

.achievement-card__desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0 0 12px 0;
  line-height: 1.6;
}

.achievement-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.achievement-card__tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  display: flex;
  align-items: center;
}

.achievement-card__tag--scope {
  background: rgba(24, 144, 255, 0.1);
  color: #1890ff;
}

.achievement-card__tag--transform {
  background: rgba(114, 46, 209, 0.1);
  color: #722ed1;
}

.achievement-card__tag--data {
  background: rgba(250, 140, 22, 0.1);
  color: #fa8c16;
}

/* 右侧信息区域 */
.achievement-card__meta {
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 120px;
}

.achievement-card__meta-item {
  font-size: 12px;
  color: #8c8c8c;
  display: flex;
  align-items: center;
}

/* 箭头 */
.achievement-card__arrow {
  display: flex;
  align-items: center;
  padding: 0 16px;
  color: #d9d9d9;
  font-size: 12px;
}

.achievement-card:hover .achievement-card__arrow {
  color: #4A8F72;
}

/* 响应式 */
@media (max-width: 768px) {
  .achievement-card__icon {
    display: none;
  }

  .achievement-card__meta {
    display: none;
  }
}
</style>
