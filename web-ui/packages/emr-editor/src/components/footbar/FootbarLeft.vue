<template>
  <div class="footbar-left">
    <!-- 目录按钮 -->
    <div
      class="footbar-button catalog-mode"
      :class="{ active: catalogShow }"
      title="目录"
      @click="handleCatalogToggle"
    >
      <i class="icon-catalog"></i>
    </div>

    <!-- 页面模式 -->
    <div class="page-mode-container">
      <div
        class="footbar-button page-mode"
        title="页面模式(分页、连页)"
        @click="handlePageModeToggle"
      >
        <i class="icon-page-mode"></i>
      </div>

      <!-- 页面模式选项 -->
      <div
        class="page-mode-options"
        :class="{ visible: showPageModeOptions }"
        @click="handlePageModeOptionClick"
      >
        <ul>
          <li
            :class="{ active: pageMode === 'paging' }"
            data-page-mode="paging"
          >
            分页
          </li>
          <li
            :class="{ active: pageMode === 'continuity' }"
            data-page-mode="continuity"
          >
            连页
          </li>
        </ul>
      </div>
    </div>

    <!-- 页面信息 -->
    <div class="page-info">
      <span class="page-info-label">可见页码：</span>
      <span class="page-info-value">
        {{ visiblePagesText || '1' }}
      </span>
    </div>

    <!-- 页面和字数统计 -->
    <div class="page-info">
      <span class="page-info-label">页面：</span>
      <span class="page-info-value">{{ pageInfo.current }}/{{ pageInfo.total }}</span>
    </div>

    <div class="page-info">
      <span class="page-info-label">字数：</span>
      <span class="page-info-value">{{ wordCount.total }}</span>
    </div>

    <div class="page-info">
      <span class="page-info-label">行：</span>
      <span class="page-info-value">{{ wordCount.lines }}</span>
    </div>

    <div class="page-info">
      <span class="page-info-label">列：</span>
      <span class="page-info-value">{{ wordCount.columns }}</span>
    </div>

    <div v-if="wordCount.selected > 0" class="page-info page-info-selected">
      <span class="page-info-label">已选中：</span>
      <span class="page-info-value">{{ wordCount.selected }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onMounted } from 'vue'

export interface PageInfo {
  current: number
  total: number
  visiblePages: number[]
}

export interface WordCount {
  total: number
  selected: number
  lines: number
  columns: number
}

export interface FootbarLeftProps {
  pageInfo?: PageInfo
  wordCount?: WordCount
  visiblePages?: number[]
  catalogShow?: boolean
  pageMode?: 'paging' | 'continuity'
}

const props = withDefaults(defineProps<FootbarLeftProps>(), {
  pageInfo: () => ({ current: 1, total: 1, visiblePages: [1] }),
  wordCount: () => ({ total: 0, selected: 0, lines: 0, columns: 0 }),
  visiblePages: () => [1],
  catalogShow: false,
  pageMode: 'paging'
})

const emit = defineEmits<{
  catalogToggle: []
  pageModeChange: [mode: string]
}>()

// 页面模式选项显示状态
const showPageModeOptions = ref(false)

// 计算可见页码文本
const visiblePagesText = computed(() => {
  if (!props.visiblePages || props.visiblePages.length === 0) {
    return null
  }

  // 如果只有一个页面，直接显示
  if (props.visiblePages.length === 1) {
    return props.visiblePages[0].toString()
  }

  // 如果是连续的页面，用连字符表示
  const sorted = [...props.visiblePages].sort((a, b) => a - b)
  const ranges: string[] = []
  let start = sorted[0]
  let prev = sorted[0]

  for (let i = 1; i < sorted.length; i++) {
    if (sorted[i] === prev + 1) {
      prev = sorted[i]
    } else {
      ranges.push(start === prev ? start.toString() : `${start}-${prev}`)
      start = sorted[i]
      prev = sorted[i]
    }
  }
  ranges.push(start === prev ? start.toString() : `${start}-${prev}`)

  return ranges.join(', ')
})

// 处理目录切换
const handleCatalogToggle = () => {
  // debug: emit and log
  try { console.log('[FootbarLeft] catalog button clicked') } catch {}
  emit('catalogToggle')
}

onMounted(() => {
  try { console.log('[FootbarLeft] mounted') } catch {}
})

// 处理页面模式切换按钮点击
const handlePageModeToggle = () => {
  showPageModeOptions.value = !showPageModeOptions.value
}

// 处理页面模式选项点击
const handlePageModeOptionClick = (event: Event) => {
  const target = event.target as HTMLElement
  const li = target.closest('li') as HTMLLIElement
  if (li && li.dataset.pageMode) {
    const mode = li.dataset.pageMode
    emit('pageModeChange', mode)
    showPageModeOptions.value = false
  }
}
</script>

<style scoped>
.footbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0; /* 允许内容收缩 */
}

.footbar-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 3px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.footbar-button:hover {
  background-color: var(--emr-button-hover-bg, #e6f7ff);
  color: var(--emr-button-color, #1890ff);
}

.footbar-button.active {
  background-color: var(--emr-button-active-bg, #bae7ff);
  color: var(--emr-button-color, #1890ff);
}

.footbar-button i {
  font-size: 12px;
  font-weight: bold;
}

.page-mode-container {
  position: relative;
}

.page-mode-options {
  width: 70px;
  position: absolute;
  left: 0;
  bottom: 25px;
  padding: 10px;
  background: #fff;
  font-size: 14px;
  box-shadow: 0 2px 12px 0 rgb(56 56 56 / 20%);
  border: 1px solid #e2e6ed;
  border-radius: 2px;
  display: none;
  z-index: 1000;
}

.page-mode-options.visible {
  display: block;
}

.page-mode-options ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.page-mode-options li {
  padding: 5px;
  margin: 5px 0;
  user-select: none;
  transition: all .3s;
  text-align: center;
  cursor: pointer;
  border-radius: 2px;
}

.page-mode-options li:hover {
  background-color: #ebecef;
}

.page-mode-options li.active {
  background-color: #e2e6ed;
}

.page-info {
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
  margin-right: 5px;
  letter-spacing: 1px;
}

.page-info-label {
  color: #999;
  font-weight: 500;
  font-size: 12px;
}

.page-info-value {
  color: #666;
  font-weight: 600;
  font-size: 12px;
}

.page-info-selected {
  color: #1890ff;
}

.page-info-selected .page-info-value {
  color: #1890ff;
}

/* 图标样式 */
.icon-catalog {
  display: inline-block;
  width: 16px;
  height: 16px;
  background-image: url('../../assets/images/catalog.svg');
  background-size: 16px 16px;
  background-repeat: no-repeat;
}

.icon-page-mode {
  display: inline-block;
  width: 16px;
  height: 16px;
  background-image: url('../../assets/images/page-mode.svg');
  background-size: 16px 16px;
  background-repeat: no-repeat;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .footbar-left {
    gap: 6px;
  }

  .page-info {
    font-size: 11px;
    margin-right: 3px;
  }
}

@media (max-width: 768px) {
  .footbar-left {
    flex-wrap: wrap;
    gap: 4px;
  }

  .page-info {
    font-size: 10px;
    margin-right: 2px;
  }

  .page-mode-options {
    width: 60px;
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .footbar-left {
    gap: 2px;
  }

  .page-info {
    font-size: 9px;
    margin-right: 1px;
  }

  .footbar-button {
    width: 20px;
    height: 20px;
  }

  .footbar-button i {
    font-size: 10px;
  }
}

/* 数字格式化 */
.page-info-value {
  font-variant-numeric: tabular-nums; /* 等宽数字 */
}
</style>
