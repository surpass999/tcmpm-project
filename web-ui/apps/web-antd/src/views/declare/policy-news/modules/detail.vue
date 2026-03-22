<script lang="ts" setup>
import type { DeclarePolicyNewsApi } from '#/api/declare/policy-news';

import { ref } from 'vue';

import { getDictOptions } from '@vben/hooks';
import { DICT_TYPE } from '@vben/constants';

import { useVbenModal } from '@vben/common-ui';

/** 状态选项 */
const POLICY_STATUS_OPTIONS = getDictOptions(DICT_TYPE.DECLARE_POLICY_STATUS, 'number');

/** 项目类型选项 */
const PROJECT_TYPE_OPTIONS = getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number');

/** 目标范围选项 */
const TARGET_SCOPE_OPTIONS = getDictOptions(DICT_TYPE.DECLARE_TARGET_SCOPE, 'number');

const formData = ref<DeclarePolicyNewsApi.PolicyNews>();

const [Modal, modalApi] = useVbenModal({
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      return;
    }
    const data = modalApi.getData<DeclarePolicyNewsApi.PolicyNews>();
    if (!data || !data.id) {
      return;
    }

    formData.value = data;
  },
});

/** 获取状态标签和颜色 */
function getStatusInfo(status: number) {
  const item = POLICY_STATUS_OPTIONS.find((opt) => opt.value === status);
  return { label: item?.label || '-', color: item?.color || 'default' };
}

/** 解析适用项目类型 */
function getProjectTypesText(types: string) {
  if (!types) return '-';
  const typeList = types.split(',');
  const labels = typeList.map((t) => {
    const item = PROJECT_TYPE_OPTIONS.find((opt) => opt.value === Number(t));
    return item?.label || t;
  });
  return labels.join('、');
}

/** 获取目标范围标签 */
function getTargetScopeLabel(scope: number) {
  const item = TARGET_SCOPE_OPTIONS.find((opt) => opt.value === scope);
  return item?.label || '-';
}

/** 格式化发布时间 */
function formatReleaseTime(time: number | string) {
  if (!time) return '-';
  // 如果是时间戳（数字）
  if (typeof time === 'number') {
    const date = new Date(time);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  }
  return time;
}
</script>

<template>
  <Modal
    :title="formData?.policyTitle || '政策文件详情'"
    class="w-[80%] px-5"
    :show-cancel-button="false"
    :show-confirm-button="false"
    :closable="true"
  >
    <!-- 核心信息区 -->
    <div class="mb-6 text-center border-b pb-4">
      <h1 class="text-xl font-bold text-gray-800 mb-3 px-4">
        {{ formData?.policyTitle || '政策标题' }}
      </h1>
      <div class="flex flex-wrap justify-center gap-2 mb-3">
        <span
          v-if="formData?.releaseDept"
          class="px-3 py-1 bg-[#A07852] text-white text-xs rounded-md"
        >
          {{ formData.releaseDept }}
        </span>
      </div>
      <div class="text-sm text-gray-500">
        <span v-if="formData?.releaseTime">
          发布时间：{{ formatReleaseTime(formData.releaseTime) }}
        </span>
        <span v-if="formData?.releaseTime && formData?.publisherName" class="mx-2">|</span>
        <span v-if="formData?.publisherName">
          发布人：{{ formData.publisherName }}
        </span>
      </div>
    </div>

    <!-- 基本信息 -->
    <div class="bg-gray-50 rounded-lg p-4 mb-4 text-sm">
      <div class="flex items-center mb-2">
        <span class="text-gray-500 w-20 shrink-0">目标范围：</span>
        <span class="text-gray-800">
          {{ formData?.targetScope ? getTargetScopeLabel(formData.targetScope) : '-' }}
        </span>
      </div>
      <div class="flex items-center">
        <span class="text-gray-500 w-20 shrink-0">适用项目：</span>
        <span class="text-gray-800">
          {{ formData?.targetProjectTypes ? getProjectTypesText(formData.targetProjectTypes) : '-' }}
        </span>
      </div>
    </div>

    <!-- 政策摘要 -->
    <div v-if="formData?.policySummary" class="mb-4">
      <div class="text-sm text-gray-600 mb-2 font-medium">【政策摘要】</div>
      <div class="bg-blue-50 border-l-4 border-blue-500 p-3 text-gray-700 text-sm leading-relaxed rounded-r">
        {{ formData.policySummary }}
      </div>
    </div>

    <!-- 政策正文 -->
    <div class="mb-4">
      <div class="text-sm text-gray-600 mb-2 font-medium">【正文内容】</div>
      <div class="bg-white rounded-lg p-5 text-gray-700 text-sm leading-loose">
        <div v-if="formData?.policyContent" v-html="formData.policyContent" class="policy-content"></div>
        <span v-else class="text-gray-400">暂无正文内容</span>
      </div>
    </div>

    <!-- 附件下载 -->
    <div v-if="formData?.attachmentUrls && formData.attachmentUrls.length > 0" class="mb-4">
      <div class="text-sm text-gray-600 mb-2 font-medium">【附件下载】</div>
      <div class="bg-gray-50 rounded-lg p-3">
        <div
          v-for="(url, index) in formData.attachmentUrls"
          :key="index"
          class="flex items-center gap-2 py-2 hover:bg-gray-100 rounded px-2"
        >
          <i class="fas fa-paperclip text-gray-400"></i>
          <a :href="url" target="_blank" class="text-blue-600 hover:underline truncate">
            附件{{ index + 1 }}：{{ url.split('/').pop() }}
          </a>
          <a :href="url" download class="ml-2 text-gray-500 hover:text-gray-700">
            <i class="fas fa-download"></i>
          </a>
        </div>
      </div>
    </div>

    <!-- 底部操作区 -->
    <div class="mt-6 pt-4 border-t flex justify-center gap-4">
      <button
        class="px-5 py-2 border border-gray-300 rounded-md hover:bg-gray-50 transition-colors flex items-center text-sm"
        @click="modalApi.close()"
      >
        <i class="fas fa-arrow-left mr-2 text-gray-500"></i>
        关闭
      </button>
    </div>
  </Modal>
</template>

<style scoped>
.policy-content {
  text-indent: 2em;
}
.policy-content :deep(p) {
  margin-bottom: 1em;
  line-height: 1.8;
}
.policy-content :deep(h2) {
  font-size: 1.125rem;
  font-weight: bold;
  margin-top: 1.875rem;
  margin-bottom: 0.75rem;
  padding-bottom: 0.25rem;
  border-bottom: 1px solid #2A5C45;
}
.policy-content :deep(h3) {
  font-size: 1rem;
  font-weight: bold;
  margin-top: 1.25rem;
  margin-bottom: 0.625rem;
}
.policy-content :deep(.text-indent-2) {
  text-indent: 2em;
}
.policy-content :deep(.bg-selectedBg) {
  background-color: #f0f9ff;
  padding: 1rem;
  border-radius: 0.5rem;
}
</style>
