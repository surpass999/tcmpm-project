<script lang="ts" setup>
import type { DeclareReviewApi } from '#/api/declare/review';

import { nextTick, ref, watch } from 'vue';

import { Collapse, Empty, Spin, Tag } from 'ant-design-vue';

import { getReviewSummaryByBusiness } from '#/api/declare/review';

import ReviewDetailModal from './ReviewDetailModal.vue';

interface Props {
  businessType: number;
  businessId: number;
}

const props = defineProps<Props>();

const loading = ref(false);
const reviewSummaryList = ref<DeclareReviewApi.ReviewSummary[]>([]);
const detailModal = ref<InstanceType<typeof ReviewDetailModal> | null>(null);
const hasLoaded = ref(false); // 标记是否已加载过

// 加载评审汇总数据
async function loadReviewSummary() {
  if (!props.businessId) {
    console.warn('[ProjectReviewRecords] businessId 为空，跳过加载');
    return;
  }

  loading.value = true;
  try {
    console.log('[ProjectReviewRecords] 加载评审记录: businessType=', props.businessType, 'businessId=', props.businessId);
    const data = await getReviewSummaryByBusiness(props.businessType, props.businessId);
    console.log('[ProjectReviewRecords] 评审记录数据:', data);
    reviewSummaryList.value = data || [];
    hasLoaded.value = true;
  } catch (e) {
    console.error('加载评审汇总失败', e);
    reviewSummaryList.value = [];
  } finally {
    loading.value = false;
  }
}

// 暴露加载方法供父组件调用
defineExpose({ load: loadReviewSummary });

// 监听 businessId 变化，自动加载数据
watch(
  () => props.businessId,
  async (newId) => {
    if (newId && !hasLoaded.value) {
      await nextTick();
      await loadReviewSummary();
    }
  },
  { immediate: true }
);

// 查看评审详情
function handleViewDetail(summary: DeclareReviewApi.ReviewSummary) {
  detailModal.value?.open(summary);
}

// 通过状态标签颜色
function getPassStatusColor(status?: string): string {
  if (status === '通过') return 'success';
  if (status === '未通过') return 'error';
  return 'default';
}
</script>

<template>
  <Spin :spinning="loading">
    <div v-if="reviewSummaryList.length === 0" class="py-8">
      <Empty description="暂无评审记录" />
    </div>
    <Collapse v-else accordion class="review-collapse">
      <Collapse.Panel
        v-for="summary in reviewSummaryList"
        :key="summary.processType"
        :header="summary.processTypeName"
      >
        <template #extra>
          <div class="flex items-center gap-4 text-sm">
            <span>专家：{{ summary.expertCount }}人</span>
            <span v-if="summary.averageScore">
              平均分：{{ summary.averageScore.toFixed(1) }}分
            </span>
            <Tag v-if="summary.passStatus" :color="getPassStatusColor(summary.passStatus)">
              {{ summary.passStatus }}
            </Tag>
          </div>
        </template>

        <div class="space-y-3">
          <div v-if="summary.reviewTime" class="text-gray-500 text-sm">
            评审时间：{{ summary.reviewTime }}
          </div>

          <div class="flex flex-wrap gap-2">
            <div
              v-for="result in summary.results"
              :key="result.id"
              class="review-result-card"
            >
              <div class="flex items-center justify-between">
                <div>
                  <div class="font-medium">{{ result.expertName }}</div>
                  <div v-if="result.expertWorkUnit" class="text-gray-500 text-sm">
                    {{ result.expertWorkUnit }}
                  </div>
                </div>
                <div class="text-right">
                  <div v-if="result.totalScore" class="text-lg font-semibold text-primary">
                    {{ result.totalScore }}分
                  </div>
                  <div v-if="result.conclusion" class="text-sm">
                    <Tag :color="result.conclusion === '通过' ? 'success' : 'error'">
                      {{ result.conclusion }}
                    </Tag>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="pt-2">
            <a-button type="link" @click="handleViewDetail(summary)">
              查看详情
            </a-button>
          </div>
        </div>
      </Collapse.Panel>
    </Collapse>

    <!-- 评审详情弹窗 -->
    <ReviewDetailModal ref="detailModal" />
  </Spin>
</template>

<style scoped>
.review-collapse {
  background: transparent;
}

.review-collapse :deep(.ant-collapse-header) {
  font-weight: 500;
}

.review-collapse :deep(.ant-collapse-content-box) {
  padding: 12px 16px;
}

.review-result-card {
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 8px;
  min-width: 200px;
  flex: 1;
  max-width: 280px;
}

.text-primary {
  color: var(--ant-primary-color, #1677ff);
}
</style>
