<script lang="ts" setup>
import type { DataComparisonRespVO } from '#/api/mall/statistics/common';
import type { MallTradeStatisticsApi } from '#/api/mall/statistics/trade';

import { onMounted, ref } from 'vue';

import { DocAlert, Page } from '@vben/common-ui';
import { fenToYuan } from '@vben/utils';

import { ElCol, ElRow } from 'element-plus';

import { getOrderComparison } from '#/api/mall/statistics/trade';

import ComparisonCard from './modules/comparison-card.vue';
import OperationDataCard from './modules/operation-data-card.vue';
import ShortcutCard from './modules/shortcut-card.vue';
import TradeTrendCard from './modules/trade-trend-card.vue';

/** 商城首页 */
defineOptions({ name: 'MallHome' });

const loading = ref(true); // 加载中
const orderComparison =
  ref<DataComparisonRespVO<MallTradeStatisticsApi.TradeOrderSummaryRespVO>>(); // 交易对照数据

/** 查询交易对照卡片数据 */
async function loadOrderComparison() {
  orderComparison.value = await getOrderComparison();
}

/** 初始化 */
onMounted(async () => {
  loading.value = true;
  await loadOrderComparison();
  loading.value = false;
});
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert
        title="商城手册（功能开启）"
        url="https://doc.iocoder.cn/mall/build/"
      />
    </template>

    <div class="flex flex-col gap-4">
      <!-- 数据对照 -->
      <ElRow :gutter="16">
        <ElCol :md="8" :sm="12" :xs="24">
          <ComparisonCard
            tag="今日"
            title="销售额"
            prefix="￥"
            :decimals="2"
            :value="fenToYuan(orderComparison?.value?.orderPayPrice || 0)"
            :reference="
              fenToYuan(orderComparison?.reference?.orderPayPrice || 0)
            "
          />
        </ElCol>
        <ElCol :md="8" :sm="12" :xs="24">
          <ComparisonCard
            tag="今日"
            title="订单量"
            :value="orderComparison?.value?.orderPayCount || 0"
            :reference="orderComparison?.reference?.orderPayCount || 0"
          />
        </ElCol>
        <ElCol :md="8" :sm="12" :xs="24">
          <ComparisonCard
            tag="今日"
            title="支付金额"
            prefix="￥"
            :decimals="2"
            :value="fenToYuan(orderComparison?.value?.orderPayPrice || 0)"
            :reference="
              fenToYuan(orderComparison?.reference?.orderPayPrice || 0)
            "
          />
        </ElCol>
      </ElRow>
      <!-- 快捷入口和运营数据 -->
      <ElRow :gutter="16">
        <ElCol :md="12" :xs="24">
          <ShortcutCard />
        </ElCol>
        <ElCol :md="12" :xs="24">
          <OperationDataCard />
        </ElCol>
      </ElRow>
      <!-- 交易量趋势 -->
      <TradeTrendCard />
    </div>
  </Page>
</template>
