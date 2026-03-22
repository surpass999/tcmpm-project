<script lang="ts" setup>
import type { DeclarePolicyNewsApi } from '#/api/declare/policy-news';

import { computed, onMounted, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';

import { Input, Select, Button, RangePicker, Pagination } from 'ant-design-vue';

import dayjs from 'dayjs';

import { getPolicyNewsList } from '#/api/declare/policy-news';
import { getRangePickerDefaultProps } from '#/utils';

import {
  formatTime,
  getPolicyTypeColor,
  getPolicyTypeLabel,
  POLICY_TYPE_OPTIONS,
} from './data';
import PolicyNewsDetailModal from './modules/detail.vue';

const loading = ref(false);
const list = ref<DeclarePolicyNewsApi.PolicyNews[]>([]);

// 筛选条件
const keyword = ref('');
const selectedType = ref<number | string | undefined>(undefined);
const dateRange = ref<[string, string] | null>(null);
const releaseDept = ref('');

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
  connectedComponent: PolicyNewsDetailModal,
  destroyOnClose: true,
});

const rangePickerProps = getRangePickerDefaultProps();

/** 加载数据 */
async function loadData() {
  loading.value = true;
  try {
    const data = await getPolicyNewsList();
    list.value = data || [];
  } finally {
    loading.value = false;
  }
}

/** 查看详情 */
function handleDetail(row: DeclarePolicyNewsApi.PolicyNews) {
  detailModalApi.setData(row).open();
}

/** 解析时间为 dayjs（支持时间戳或字符串） */
function parseReleaseTime(time: string | number | undefined): dayjs.Dayjs | null {
  if (time == null) return null;
  if (typeof time === 'number') return dayjs(time);
  return dayjs(time).isValid() ? dayjs(time) : null;
}

/** 筛选后的列表（前端过滤） */
const filteredList = computed(() => {
  let result = list.value;

  // 关键词：标题、摘要
  const kw = keyword.value?.trim();
  if (kw) {
    const lower = kw.toLowerCase();
    result = result.filter(
      (item) =>
        (item.policyTitle && item.policyTitle.toLowerCase().includes(lower)) ||
        (item.policySummary && item.policySummary.toLowerCase().includes(lower)),
    );
  }

  // 类型
  const typeVal = selectedType.value;
  if (typeVal !== undefined && typeVal !== null && typeVal !== '') {
    result = result.filter((item) => item.policyType === typeVal);
  }

  // 发布时间范围
  if (dateRange.value && dateRange.value.length === 2) {
    const [startStr, endStr] = dateRange.value;
    const start = dayjs(startStr).startOf('day');
    const end = dayjs(endStr).endOf('day');
    result = result.filter((item) => {
      const t = parseReleaseTime(item.releaseTime);
      if (!t) return false;
      return !t.isBefore(start) && !t.isAfter(end);
    });
  }

  // 发布单位
  const dept = releaseDept.value?.trim();
  if (dept) {
    result = result.filter(
      (item) => item.releaseDept && item.releaseDept.includes(dept),
    );
  }

  return result;
});

/** 筛选：当前已通过表单条件过滤，无需额外请求 */
function handleSearch() {
  // 筛选结果由 computed filteredList 实时响应，无需操作
}

/** 重置筛选条件 */
function handleReset() {
  keyword.value = '';
  selectedType.value = undefined;
  dateRange.value = null;
  releaseDept.value = '';
  currentPage.value = 1;
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <Page auto-content-height class="policy-news-page">
    <DetailModal />

    <!-- 筛选区块 -->
    <div class="filter-block">
      <div class="filter-block__inner">
        <Input
          v-model:value="keyword"
          class="filter-block__search"
          placeholder="搜索政策 / 通知标题 / 关键词"
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
          placeholder="全部"
          allow-clear
          :options="POLICY_TYPE_OPTIONS.map((o) => ({ label: o.label, value: o.value }))"
        />
        <RangePicker
          v-model:value="dateRange"
          class="filter-block__range"
          v-bind="rangePickerProps"
          allow-clear
        />
        <Input
          v-model:value="releaseDept"
          class="filter-block__input"
          placeholder="发布单位"
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
        <i class="fas fa-inbox text-4xl text-gray-300 mb-3"></i>
        <span class="text-gray-400">暂无政策资讯</span>
      </div>

      <ul v-else class="list-block__list">
        <li
          v-for="item in paginatedList"
          :key="item.id"
          class="news-card"
          @click="handleDetail(item)"
        >
          <span
            class="news-card__tag"
            :style="{ backgroundColor: getPolicyTypeColor(item.policyType) }"
          >
            {{ getPolicyTypeLabel(item.policyType) }}
          </span>
          <div class="news-card__body">
            <h3 class="news-card__title">{{ item.policyTitle }}</h3>
            <p v-if="item.policySummary" class="news-card__summary">
              {{ item.policySummary }}
            </p>
            <div class="news-card__meta">
              <span v-if="item.releaseDept" class="news-card__meta-item">
                <i class="fas fa-building mr-1 text-gray-400"></i>
                {{ item.releaseDept }}
              </span>
              <span v-if="item.releaseTime" class="news-card__meta-item">
                <i class="fas fa-calendar-alt mr-1 text-gray-400"></i>
                {{ formatTime(item.releaseTime) }}
              </span>
            </div>
          </div>
          <div class="news-card__arrow">
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
          :page-size-options="['5', '10', '20', '50']"
          @change="handlePageChange"
          @showSizeChange="handlePageSizeChange"
        />
      </div>
    </div>
  </Page>
</template>

<style scoped>
.policy-news-page {
  padding: 0;
  background: #f5f6f8;
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
  width: 280px;
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

.filter-block__input {
  width: 160px;
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
  gap: 12px;
}

.list-block__pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
}

.news-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s;
}

.news-card:hover {
  border-color: #d9d9d9;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.news-card__tag {
  flex-shrink: 0;
  padding: 4px 10px;
  font-size: 12px;
  color: #fff;
  border-radius: 4px;
}

.news-card__body {
  flex: 1;
  min-width: 0;
}

.news-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-card__summary {
  font-size: 14px;
  color: #8c8c8c;
  line-height: 1.6;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-card__meta {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 12px;
  color: #bfbfbf;
}

.news-card__meta-item {
  display: inline-flex;
  align-items: center;
}

.news-card__arrow {
  flex-shrink: 0;
  color: #d9d9d9;
  font-size: 12px;
  padding-top: 2px;
}
</style>
