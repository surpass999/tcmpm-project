<script lang="ts" setup>
import type { Training } from '#/api/declare/training';

import { computed, onMounted, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';

import { Input, Select, Button, RangePicker, Pagination } from 'ant-design-vue';

import dayjs from 'dayjs';

import { trainingClientApi } from '#/api/declare/training';
import { getRangePickerDefaultProps } from '#/utils';

import {
  getTrainingStatusColor,
  getTrainingTypeLabel,
  TRAINING_TYPE_OPTIONS,
} from '../training/data';
import TrainingDetailModal from './detail.vue';

const loading = ref(false);
const list = ref<Training[]>([]);

// 筛选条件
const keyword = ref('');
const selectedType = ref<number | string | undefined>(undefined);
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
  connectedComponent: TrainingDetailModal,
  destroyOnClose: true,
});

const rangePickerProps = { format: 'YYYY-MM-DD' };

/** 加载数据 */
async function loadData() {
  loading.value = true;
  try {
    const data = await trainingClientApi.getList();
    list.value = data || [];
  } finally {
    loading.value = false;
  }
}

/** 查看详情 */
function handleDetail(row: Training) {
  detailModalApi.setData(row).open();
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
  return d ? d.format('YYYY-MM-DD HH:mm') : '-';
}

/** 计算倒计时 */
function getCountdown(startTime: string | number | undefined, endTime: string | number | undefined): string {
  const start = parseTime(startTime);
  const end = parseTime(endTime);
  if (!start || !end) return '';
  const now = dayjs();
  
  // 如果当前时间在开始时间之前，显示距开始倒计时
  if (now.isBefore(start)) {
    const diff = start.diff(now, 'minute');
    if (diff < 60) return `${diff}分钟后开始`;
    const hours = Math.floor(diff / 60);
    if (hours < 24) return `${hours}小时后开始`;
    const days = Math.floor(hours / 24);
    return `${days}天后开始`;
  }
  
  // 如果当前时间在开始和结束之间，显示"进行中"
  if (now.isAfter(start) && now.isBefore(end)) {
    return '进行中';
  }
  
  // 如果当前时间在结束时间之后，显示"已结束"
  return '已结束';
}

/** 筛选后的列表 */
const filteredList = computed(() => {
  let result = list.value;

  // 关键词：名称、组织单位、主讲人
  const kw = keyword.value?.trim();
  if (kw) {
    const lower = kw.toLowerCase();
    result = result.filter(
      (item) =>
        (item.name && item.name.toLowerCase().includes(lower)) ||
        (item.organizer && item.organizer.toLowerCase().includes(lower)) ||
        (item.speaker && item.speaker.toLowerCase().includes(lower)),
    );
  }

  // 类型
  const typeVal = selectedType.value;
  if (typeVal !== undefined && typeVal !== null && typeVal !== '') {
    result = result.filter((item) => item.type === typeVal);
  }

  // 时间范围
  if (dateRange.value && dateRange.value.length === 2) {
    const [startStr, endStr] = dateRange.value;
    const start = dayjs(startStr).startOf('day');
    const end = dayjs(endStr).endOf('day');
    result = result.filter((item) => {
      const t = parseTime(item.startTime);
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
  dateRange.value = null;
  currentPage.value = 1;
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <Page auto-content-height class="training-public-page">
    <DetailModal @refresh="loadData" />

    <!-- 筛选区块 -->
    <div class="filter-block">
      <div class="filter-block__inner">
        <Input
          v-model:value="keyword"
          class="filter-block__search"
          placeholder="搜索活动名称 / 组织单位 / 主讲人"
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
          placeholder="活动类型"
          allow-clear
          :options="TRAINING_TYPE_OPTIONS.map((o) => ({ label: o.label, value: o.value }))"
        />
        <RangePicker
          v-model:value="dateRange"
          class="filter-block__range"
          v-bind="rangePickerProps"
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
        <i class="fas fa-calendar-alt text-4xl text-gray-300 mb-3"></i>
        <span class="text-gray-400">暂无培训活动</span>
      </div>

      <ul v-else class="list-block__list">
        <li
          v-for="item in paginatedList"
          :key="item.id"
          class="activity-card"
          @click="handleDetail(item)"
        >
          <div class="activity-card__poster">
            <img
              v-if="item.posterUrl"
              :src="item.posterUrl"
              :alt="item.name"
              class="activity-card__img"
            />
            <div v-else class="activity-card__img-placeholder">
              <i class="fas fa-chalkboard-teacher text-4xl text-white"></i>
            </div>
            <span
              class="activity-card__type"
              :style="{ backgroundColor: getTrainingStatusColor(item.type) }"
            >
              {{ getTrainingTypeLabel(item.type) }}
            </span>
          </div>
          <div class="activity-card__content">
            <h3 class="activity-card__title">{{ item.name }}</h3>
            <div class="activity-card__info">
              <span class="activity-card__info-item">
                <i class="fas fa-clock mr-1 text-gray-400"></i>
                {{ formatTime(item.startTime) }}
              </span>
              <span class="activity-card__info-item">
                <i class="fas fa-map-marker-alt mr-1 text-gray-400"></i>
                {{ item.location || '待定' }}
              </span>
              <span class="activity-card__info-item">
                <i class="fas fa-user-tie mr-1 text-gray-400"></i>
                {{ item.speaker || '待定' }}
              </span>
            </div>
            <div class="activity-card__footer">
              <span class="activity-card__organizer">
                <i class="fas fa-building mr-1 text-gray-400"></i>
                {{ item.organizer || '待定' }}
              </span>
              <span class="activity-card__countdown">
                {{ getCountdown(item.startTime, item.endTime) }}
              </span>
            </div>
          </div>
          <div class="activity-card__arrow">
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
.training-public-page {
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
  width: 300px;
  flex-shrink: 0;
}

.filter-block__select {
  width: 160px;
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
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.list-block__pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
}

/* 活动卡片 */
.activity-card {
  display: flex;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.activity-card:hover {
  border-color: #d9d9d9;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.activity-card__poster {
  width: 160px;
  height: 160px;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.activity-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.activity-card__img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.activity-card__type {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  font-size: 11px;
  color: #fff;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.4);
}

.activity-card__content {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.activity-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 12px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.activity-card__info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.activity-card__info-item {
  font-size: 13px;
  color: #595959;
  display: flex;
  align-items: center;
}

.activity-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.activity-card__organizer {
  font-size: 12px;
  color: #8c8c8c;
  display: flex;
  align-items: center;
}

.activity-card__countdown {
  font-size: 12px;
  color: #fa8c16;
  font-weight: 500;
}

.activity-card__arrow {
  display: flex;
  align-items: center;
  padding: 0 16px;
  color: #d9d9d9;
  font-size: 12px;
}
</style>
