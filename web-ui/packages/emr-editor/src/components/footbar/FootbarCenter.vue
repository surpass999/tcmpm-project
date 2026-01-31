<template>
  <div class="footbar-center">
    <div
      class="editor-mode"
      title="编辑模式(编辑、清洁、只读、表单、打印、设计、涂鸦)"
      @click="handleEditorModeClick"
    >
      {{ currentModeName }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

export interface FootbarCenterProps {
  editorMode?: 'edit' | 'clean' | 'readonly' | 'form' | 'print' | 'design' | 'graffiti'
}

const props = withDefaults(defineProps<FootbarCenterProps>(), {
  editorMode: 'edit'
})

const emit = defineEmits<{
  editorModeChange: [mode: string]
}>()

// 编辑模式列表
const modeList = [
  { mode: 'edit', name: '编辑模式' },
  { mode: 'clean', name: '清洁模式' },
  { mode: 'readonly', name: '只读模式' },
  { mode: 'form', name: '表单模式' },
  { mode: 'print', name: '打印模式' },
  { mode: 'design', name: '设计模式' },
  { mode: 'graffiti', name: '涂鸦模式' }
]

// 当前模式索引
let modeIndex = ref(0)

// 初始化模式索引
const initModeIndex = () => {
  modeIndex.value = modeList.findIndex(item => item.mode === props.editorMode)
  if (modeIndex.value === -1) {
    modeIndex.value = 0
  }
}

// 监听 props 变化
watch(() => props.editorMode, () => {
  initModeIndex()
}, { immediate: true })

// 当前模式名称
const currentModeName = computed(() => {
  const current = modeList[modeIndex.value]
  return current?.name || '编辑模式'
})

// 处理编辑模式切换
const handleEditorModeClick = () => {
  modeIndex.value = modeIndex.value === modeList.length - 1 ? 0 : modeIndex.value + 1
  const { mode } = modeList[modeIndex.value]
  emit('editorModeChange', mode)
}
</script>

<style scoped>
.footbar-center {
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  pointer-events: none;
}

.editor-mode {
  cursor: pointer;
  user-select: none;
  font-size: 12px;
  color: #666;
  padding: 2px 8px;
  border-radius: 3px;
  transition: all 0.2s ease;
  pointer-events: auto;
}

.editor-mode:hover {
  background-color: var(--emr-button-hover-bg, #e6f7ff);
  color: var(--emr-button-color, #1890ff);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .editor-mode {
    font-size: 11px;
    padding: 1px 6px;
  }
}

@media (max-width: 768px) {
  .footbar-center {
    position: static;
    transform: none;
    order: -1;
    flex: 1;
    justify-content: center;
  }

  .editor-mode {
    font-size: 10px;
  }
}

@media (max-width: 480px) {
  .editor-mode {
    font-size: 9px;
    padding: 1px 4px;
  }
}
</style>
