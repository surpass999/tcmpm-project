<script lang="ts" setup>
import type { DeclareHospitalApi } from '#/api/declare/hospital';

import { ref, computed } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { getHospitalTagList, getHospitalTags, assignHospitalTags } from '#/api/declare/hospital-tag';
import type { HospitalTag } from '#/api/declare/hospital-tag';
import { $t } from '#/locales';

const emit = defineEmits(['success']);

const hospital = ref<DeclareHospitalApi.Hospital>();
const allTags = ref<HospitalTag[]>([]);
const selectedTagIds = ref<number[]>([]);
const loading = ref(false);

const groupedTags = computed(() => {
  const groups: Record<string, HospitalTag[]> = {
    region: [],
    level: [],
    feature: [],
    attribute: [],
  };
  for (const tag of allTags.value) {
    const category = tag.tagCategory || 'attribute';
    if (!groups[category]) {
      groups[category] = [];
    }
    groups[category].push(tag);
  }
  return groups;
});

const categoryLabels: Record<string, string> = {
  region: '区域',
  level: '等级',
  feature: '特征',
  attribute: '属性',
};

const [Modal, modalApi] = useVbenModal({
  onOpenChange(isOpen) {
    if (!isOpen) {
      selectedTagIds.value = [];
      allTags.value = [];
      hospital.value = undefined;
      return;
    }
    const data = modalApi.getData<DeclareHospitalApi.Hospital>();
    hospital.value = data;
    loadTags(data.hospitalCode!);
  },
  async onConfirm() {
    if (!hospital.value?.hospitalCode) {
      message.warning('缺少医院编码，无法保存');
      return;
    }
    modalApi.lock();
    try {
      await assignHospitalTags({
        hospitalCode: hospital.value.hospitalCode,
        tagIds: selectedTagIds.value,
      });
      await modalApi.close();
      emit('success');
      message.success($t('ui.actionMessage.operationSuccess'));
    } catch (err: any) {
      console.error('[TagConfigModal] 保存标签失败:', err);
      message.error(err?.message || '保存失败，请重试');
    } finally {
      modalApi.unlock();
    }
  },
});

async function loadTags(hospitalCode: string) {
  loading.value = true;
  try {
    const [all, current] = await Promise.all([
      getHospitalTagList(),
      getHospitalTags(hospitalCode),
    ]);
    allTags.value = all || [];
    selectedTagIds.value = (current || []).map((t) => t.id);
  } catch {
    message.error('加载标签失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <Modal :title="`配置标签 - ${hospital?.hospitalName || ''}`" class="w-[600px]">
    <div v-if="loading" class="py-8 text-center text-gray-400">加载中...</div>
    <div v-else class="max-h-[400px] overflow-y-auto px-2">
      <template v-for="(tags, category) in groupedTags" :key="category">
        <div v-if="tags.length > 0">
          <div class="mb-2 mt-4 text-sm font-medium text-gray-600 first:mt-0">
            {{ categoryLabels[category] || category }}
          </div>
          <div class="flex flex-wrap gap-2">
            <div
              v-for="tag in tags"
              :key="tag.id"
              class="cursor-pointer rounded-full border px-3 py-1 text-sm transition-colors"
              :class="
                selectedTagIds.includes(tag.id)
                  ? 'border-blue-500 bg-blue-50 text-blue-600'
                  : 'border-gray-300 bg-white text-gray-600 hover:border-blue-300 hover:bg-blue-50'
              "
              @click="
                selectedTagIds.includes(tag.id)
                  ? (selectedTagIds = selectedTagIds.filter((id) => id !== tag.id))
                  : selectedTagIds.push(tag.id)
              "
            >
              {{ tag.tagName }}
            </div>
          </div>
        </div>
      </template>
      <div
        v-if="allTags.length === 0"
        class="py-8 text-center text-sm text-gray-400"
      >
        暂无可用标签，请先在「医院标签管理」中添加标签
      </div>
    </div>
  </Modal>
</template>
