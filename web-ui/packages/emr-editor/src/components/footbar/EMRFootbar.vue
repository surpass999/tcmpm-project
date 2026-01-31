<template>
  <div class="emr-footbar" :class="footbarClasses">
    <!-- 左侧信息区域 -->
    <FootbarLeft
      :page-info="pageInfo"
      :word-count="wordCount"
      :visible-pages="visiblePages"
      :catalog-show="catalogShow"
      :page-mode="pageMode"
      @catalog-toggle="handleCatalogToggle"
      @page-mode-change="handlePageModeChange"
    />

    <!-- 中间编辑模式区域 -->
    <FootbarCenter
      :editor-mode="editorMode"
      @editor-mode-change="handleEditorModeChange"
    />

    <!-- 右侧控制区域 -->
    <FootbarRight
      :scale="scale"
      :paper-size="paperSize"
      :paper-direction="paperDirection"
      :is-fullscreen="isFullscreen"
      @scale-change="handleScaleChange"
      @scale-add="handleScaleAdd"
      @scale-minus="handleScaleMinus"
      @scale-recovery="handleScaleRecovery"
      @paper-size-change="handlePaperSizeChange"
      @paper-direction-change="handlePaperDirectionChange"
      @fullscreen-toggle="handleFullscreenToggle"
      @editor-option="handleEditorOption"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import FootbarLeft from './FootbarLeft.vue'
import FootbarCenter from './FootbarCenter.vue'
import FootbarRight from './FootbarRight.vue'

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

export interface EMRFootbarProps {
  pageInfo?: PageInfo
  wordCount?: WordCount
  visiblePages?: number[]
  scale?: number
  pageMode?: 'paging' | 'continuity'
  paperSize?: string
  paperDirection?: 'portrait' | 'landscape'
  isFullscreen?: boolean
  layout?: 'horizontal' | 'vertical'
  catalogShow?: boolean
  editorMode?: 'edit' | 'clean' | 'readonly' | 'form' | 'print' | 'design' | 'graffiti'
}

const props = withDefaults(defineProps<EMRFootbarProps>(), {
  pageInfo: () => ({ current: 1, total: 1, visiblePages: [1] }),
  wordCount: () => ({ total: 0, selected: 0, lines: 0, columns: 0 }),
  visiblePages: () => [1],
  scale: 100,
  pageMode: 'paging',
  paperSize: 'A4',
  paperDirection: 'portrait',
  isFullscreen: false,
  layout: 'horizontal',
  catalogShow: false,
  editorMode: 'edit'
})

const emit = defineEmits<{
  scaleChange: [scale: number]
  scaleAdd: []
  scaleMinus: []
  scaleRecovery: []
  pageModeChange: [mode: string]
  paperSizeChange: [size: string]
  paperDirectionChange: [direction: string]
  fullscreenToggle: []
  editorOption: []
  catalogToggle: []
  editorModeChange: [mode: string]
}>()

// 计算样式类名
const footbarClasses = computed(() => {
  return [
    'emr-footbar',
    `emr-footbar--${props.layout}`
  ]
})

// 事件处理
const handleScaleChange = (scale: number) => {
  emit('scaleChange', scale)
}

const handleScaleAdd = () => {
  emit('scaleAdd')
}

const handleScaleMinus = () => {
  emit('scaleMinus')
}

const handleScaleRecovery = () => {
  emit('scaleRecovery')
}

const handlePageModeChange = (mode: string) => {
  emit('pageModeChange', mode)
}

const handlePaperSizeChange = (size: string) => {
  emit('paperSizeChange', size)
}

const handlePaperDirectionChange = (direction: string) => {
  emit('paperDirectionChange', direction)
}

const handleFullscreenToggle = () => {
  emit('fullscreenToggle')
}

const handleEditorOption = () => {
  emit('editorOption')
}

const handleCatalogToggle = () => {
  try { console.log('[EMRFootbar] handleCatalogToggle (re-emitting)') } catch {}
  emit('catalogToggle')
}

const handleEditorModeChange = (mode: string) => {
  emit('editorModeChange', mode)
}
</script>

<style scoped>
.emr-footbar {
  /* Use layout variables (same approach as toolbar) so footer aligns with content container */
  width: var(--app-content-width, 100%);
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f2f4f7;
  z-index: 9;
  position: fixed;
  bottom: var(--app-bottom-offset, 0px);
  left: var(--app-left-offset, 0px);
  font-size: 12px;
  padding: 0 4px 0 20px;
  box-sizing: border-box;
  transition: left 0.18s ease, width 0.18s ease;
}

.emr-footbar--horizontal {
  flex-direction: row;
}

.emr-footbar--vertical {
  flex-direction: column;
  align-items: stretch;
  padding: 12px 6px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .emr-footbar {
    padding: 4px 8px;
    font-size: 11px;
  }
}

@media (max-width: 768px) {
  .emr-footbar {
    padding: 4px 6px;
    min-height: 28px;
  }

  .emr-footbar--horizontal {
    flex-wrap: wrap;
    gap: 4px;
  }
}

@media (max-width: 480px) {
  .emr-footbar {
    padding: 2px 4px;
    font-size: 10px;
  }
}
</style>
