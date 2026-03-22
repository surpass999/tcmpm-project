<script lang="ts" setup>
import type { DeclareIndicatorCaliberApi } from '#/api/declare/caliber';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';
import {
  createCaliber,
  getCaliber,
  updateCaliber,
} from '#/api/declare/caliber';
import { getIndicatorPage } from '#/api/declare/indicator';
import { $t } from '#/locales';

const emit = defineEmits(['success']);

const formData = ref<{
  id?: number;
  indicatorId?: number;
  definition?: string;
  statisticScope?: string;
  dataSource?: string;
  fillRequire?: string;
  calculationExample?: string;
}>({});

const selectedIndicatorId = ref<number>();
const indicatorOptions = ref<DeclareIndicatorApi.Indicator[]>([]);
const indicatorLoading = ref(false);
const currentPage = ref(1);
const hasMore = ref(true);
const searchKeyword = ref('');
let searchTimer: ReturnType<typeof setTimeout> | null = null;
let scrollTimer: ReturnType<typeof setTimeout> | null = null;

// 使用 ref 存储 options
const selectOptions = ref<{ value: number; label: string; raw: DeclareIndicatorApi.Indicator }[]>([]);

// 手动更新 selectOptions
function updateSelectOptions() {
  selectOptions.value = indicatorOptions.value
    .filter(item => item.id)
    .map((item) => ({
      value: item.id!,
    label: `${item.indicatorCode} - ${item.indicatorName}`,
    raw: item,
  }));
}

// 远程搜索指标
async function handleIndicatorSearch(keyword: string) {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    currentPage.value = 1;
    indicatorOptions.value = [];
    loadIndicatorOptions(keyword);
  }, 300);
}

// 加载指标选项
async function loadIndicatorOptions(keyword = '') {
  if (keyword && currentPage.value > 1) {
    return;
  }
  if (indicatorLoading.value || (!keyword && !hasMore.value)) return;

    indicatorLoading.value = true;
    try {
      const params: any = {
      pageNo: currentPage.value,
        pageSize: 20,
      };
      if (keyword) {
        params.indicatorName = keyword;
        params.indicatorCode = keyword;
      }
      const res = await getIndicatorPage(params);
      indicatorOptions.value = res?.list || [];
    updateSelectOptions();
      hasMore.value = indicatorOptions.value.length >= 20;
    } catch (e) {
      console.error('搜索指标失败', e);
    } finally {
      indicatorLoading.value = false;
    }
}

// 加载更多指标
async function loadMoreIndicatorOptions() {
  if (indicatorLoading.value || !hasMore.value) return;

  indicatorLoading.value = true;
  try {
    currentPage.value += 1;
    const params: any = {
      pageNo: currentPage.value,
      pageSize: 20,
    };
    if (searchKeyword.value) {
      params.indicatorName = searchKeyword.value;
      params.indicatorCode = searchKeyword.value;
    }
    const res = await getIndicatorPage(params);
    const newList = res?.list || [];
    indicatorOptions.value = [...indicatorOptions.value, ...newList];
    updateSelectOptions();
    hasMore.value = newList.length >= 20;
  } catch (e) {
    console.error('加载更多指标失败', e);
  } finally {
    indicatorLoading.value = false;
  }
}

// 滚动事件处理
function handlePopupScroll(event: Event) {
  if (scrollTimer) clearTimeout(scrollTimer);
  scrollTimer = setTimeout(() => {
    const target = event.target as HTMLElement;
    const { scrollTop, scrollHeight, clientHeight } = target;
    if (scrollHeight - scrollTop - clientHeight < 50) {
      loadMoreIndicatorOptions();
    }
  }, 100);
}

// 指标选择变化处理
function handleIndicatorChange(value: number) {
  selectedIndicatorId.value = value;
}

// 初始加载指标列表
async function initLoadIndicatorOptions(extraIndicatorId?: number) {
  indicatorLoading.value = true;
  searchKeyword.value = '';
  currentPage.value = 1;
  hasMore.value = true;
  try {
    const res = await getIndicatorPage({ pageNo: 1, pageSize: 20 });
    let list = res?.list || [];
    
    // 如果有额外需要显示的指标，且不在列表中，则添加到列表最前面
    if (extraIndicatorId && !list.find(item => item.id === extraIndicatorId)) {
      // 尝试获取该指标的详情
      const indicatorApi = await import('#/api/declare/indicator');
      try {
        const indicatorRes = await indicatorApi.getIndicator(extraIndicatorId);
        if (indicatorRes) {
          list = [indicatorRes, ...list];
        }
      } catch (e) {
        console.error('获取指标详情失败', e);
      }
    }
    
    indicatorOptions.value = list;
    updateSelectOptions();
    hasMore.value = indicatorOptions.value.length >= 20;
  } catch (e) {
    console.error('加载指标列表失败', e);
  } finally {
    indicatorLoading.value = false;
  }
}

const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['指标口径'])
    : $t('ui.actionTitle.create', ['指标口径']);
});

const [Modal, modalApi] = useVbenModal({
  async onOpenChange(isOpen: boolean) {
    if (isOpen) {
      const data = modalApi.getData<DeclareIndicatorCaliberApi.Caliber>();
      
      // 判断是否有有效数据
      const hasData = data && Object.keys(data).length > 0 && data.id;
      
      if (hasData) {
        modalApi.lock();
        try {
          formData.value = await getCaliber(data.id!);
          selectedIndicatorId.value = formData.value?.indicatorId;
          // 编辑模式：先获取已选中指标的详情，确保它显示在列表中
          await initLoadIndicatorOptions(formData.value.indicatorId);
        } finally {
          modalApi.unlock();
        }
      } else {
        // 新增模式
        formData.value = {
          indicatorId: undefined,
          definition: '',
          statisticScope: '',
          dataSource: '',
          fillRequire: '',
          calculationExample: '',
        };
        selectedIndicatorId.value = undefined;
        await initLoadIndicatorOptions();
      }
    } else {
      formData.value = {};
}
  },
  async onConfirm() {
    const indicatorId = selectedIndicatorId.value;

    if (!indicatorId && indicatorId !== 0) {
      message.error('请选择指标');
      return;
    }

    if (!formData.value.definition) {
      message.error('请输入指标解释');
      return;
    }

    const data = {
      ...formData.value,
      indicatorId,
    } as DeclareIndicatorCaliberApi.Caliber;

    modalApi.lock();
    try {
      if (data.id) {
        await updateCaliber(data);
      } else {
        await createCaliber(data);
      }
      await modalApi.close();
      emit('success');
      message.success($t('ui.actionMessage.operationSuccess'));
    } catch (error) {
      console.error('保存失败:', error);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-1/2">
    <a-form 
      class="mx-4" 
      layout="horizontal" 
      :label-col="{ span: 6 }" 
      :wrapper-col="{ span: 16 }"
    >
      <!-- 指标选择 -->
      <a-form-item label="选择指标" required>
        <a-select
          :value="selectedIndicatorId"
          placeholder="请输入关键词搜索指标"
          :loading="indicatorLoading && indicatorOptions.length > 0"
          show-search
          allow-clear
          :filter-option="false"
          :options="selectOptions"
          dropdown-match-select-width
          class="w-full"
          @update:value="handleIndicatorChange"
          @search="handleIndicatorSearch"
          @popup-scroll="handlePopupScroll"
        />
      </a-form-item>
      
      <!-- 其他字段 -->
      <a-form-item label="指标解释" required>
        <a-textarea v-model:value="formData.definition" :rows="2" placeholder="请输入指标解释" />
      </a-form-item>
      <a-form-item label="统计范围">
        <a-textarea v-model:value="formData.statisticScope" :rows="2" placeholder="请输入统计范围" />
      </a-form-item>
      <a-form-item label="数据来源">
        <a-textarea v-model:value="formData.dataSource" :rows="2" placeholder="请输入数据来源" />
      </a-form-item>
      <a-form-item label="填报要求">
        <a-textarea v-model:value="formData.fillRequire" :rows="2" placeholder="请输入填报要求" />
      </a-form-item>
      <a-form-item label="计算公式">
        <a-textarea v-model:value="formData.calculationExample" :rows="2" placeholder="请输入计算公式" />
      </a-form-item>
    </a-form>
  </Modal>
</template>
