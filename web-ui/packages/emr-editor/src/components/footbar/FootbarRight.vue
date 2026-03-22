<template>
  <div class="footbar-right">
    <!-- 缩放控制 -->
    <div class="scale-controls">
      <div
        class="footbar-button scale-minus"
        title="缩小(Ctrl+-)"
        @click="handleScaleMinus"
      >
        <i class="icon-minus"></i>
      </div>

      <div class="scale-percentage" title="显示比例(点击可复原Ctrl+0)" @click="handleScaleRecovery">
        <span class="scale-value">{{ scale }}%</span>
      </div>

      <div
        class="footbar-button scale-add"
        title="放大(Ctrl+=)"
        @click="handleScaleAdd"
      >
        <i class="icon-plus"></i>
      </div>
    </div>

    <!-- 纸张设置 -->
    <div class="paper-controls">
      <div class="paper-size-container">
      <div
        class="footbar-button paper-size"
          title="纸张类型"
          @click="handlePaperSizeToggle"
      >
        <i class="icon-paper-size"></i>
      </div>

        <!-- 纸张大小选项 -->
        <div
          class="paper-size-options"
          :class="{ visible: showPaperSizeOptions }"
          @click="handlePaperSizeOptionClick"
        >
          <ul>
            <li :class="{ active: paperSize === 'A4' }" data-paper-size="794*1123">A4</li>
            <li :class="{ active: paperSize === 'A2' }" data-paper-size="1593*2251">A2</li>
            <li :class="{ active: paperSize === 'A3' }" data-paper-size="1125*1593">A3</li>
            <li :class="{ active: paperSize === 'A5' }" data-paper-size="565*796">A5</li>
            <li :class="{ active: paperSize === '5号信封' }" data-paper-size="412*488">5号信封</li>
            <li :class="{ active: paperSize === '6号信封' }" data-paper-size="450*866">6号信封</li>
            <li :class="{ active: paperSize === '7号信封' }" data-paper-size="609*862">7号信封</li>
            <li :class="{ active: paperSize === '9号信封' }" data-paper-size="862*1221">9号信封</li>
            <li :class="{ active: paperSize === '法律用纸' }" data-paper-size="813*1266">法律用纸</li>
            <li :class="{ active: paperSize === '信纸' }" data-paper-size="813*1054">信纸</li>
          </ul>
        </div>
      </div>

      <div class="paper-direction-container">
      <div
        class="footbar-button paper-direction"
        title="纸张方向"
          @click="handlePaperDirectionToggle"
      >
        <i class="icon-paper-direction"></i>
        </div>

        <!-- 纸张方向选项 -->
        <div
          class="paper-direction-options"
          :class="{ visible: showPaperDirectionOptions }"
          @click="handlePaperDirectionOptionClick"
        >
          <ul>
            <li :class="{ active: paperDirection === 'portrait' }" data-paper-direction="vertical">纵向</li>
            <li :class="{ active: paperDirection === 'landscape' }" data-paper-direction="horizontal">横向</li>
          </ul>
        </div>
      </div>

      <div
        class="footbar-button paper-margin"
        title="页边距"
        @click="handlePaperMarginClick"
      >
        <i class="icon-paper-margin"></i>
      </div>
    </div>

    <!-- 其他控制 -->
    <div class="other-controls">
      <div
        class="footbar-button fullscreen"
        :class="{ active: isFullscreen }"
        title="全屏显示"
        @click="handleFullscreenToggle"
      >
        <i class="icon-fullscreen"></i>
      </div>

      <div
        class="footbar-button editor-option"
        title="编辑器设置"
        @click="handleEditorOption"
      >
        <i class="icon-settings"></i>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

export interface FootbarRightProps {
  scale?: number
  paperSize?: string
  paperDirection?: 'portrait' | 'landscape'
  isFullscreen?: boolean
}

const props = withDefaults(defineProps<FootbarRightProps>(), {
  scale: 100,
  paperSize: 'A4',
  paperDirection: 'portrait',
  isFullscreen: false
})

const emit = defineEmits([
  'scaleChange',
  'scaleAdd',
  'scaleMinus',
  'scaleRecovery',
  'paperSizeChange',
  'paperDirectionChange',
  'fullscreenToggle',
  'editorOption',
  'paperMarginChange'
])

// 纸张选项显示状态
const showPaperSizeOptions = ref(false)
const showPaperDirectionOptions = ref(false)

// 缩放配置（交由 editor 侧命令处理，UI 仅触发动作）

// 纸张大小映射
const paperSizeMap: Record<string, string> = {
  '794*1123': 'A4',
  '1593*2251': 'A2',
  '1125*1593': 'A3',
  '565*796': 'A5',
  '412*488': '5号信封',
  '450*866': '6号信封',
  '609*862': '7号信封',
  '862*1221': '9号信封',
  '813*1266': '法律用纸',
  '813*1054': '信纸'
}

// 处理缩小 — 转为向上层发出减小命令，由编辑器侧执行，保持与 canvas-editor 行为一致
const handleScaleMinus = () => {
  emit('scaleMinus')
}

// 处理放大 — 发出放大命令
const handleScaleAdd = () => {
  emit('scaleAdd')
}

// 处理缩放复原 — 通知父执行恢复
const handleScaleRecovery = () => {
  emit('scaleRecovery')
}

// 处理纸张大小切换
const handlePaperSizeToggle = () => {
  showPaperSizeOptions.value = !showPaperSizeOptions.value
  showPaperDirectionOptions.value = false
}

// 处理纸张大小选项点击
const handlePaperSizeOptionClick = (event: Event) => {
  const target = event.target as HTMLElement
  const li = target.closest('li') as HTMLLIElement
  if (li && li.dataset.paperSize) {
    const sizeKey = li.dataset.paperSize
    const sizeName = paperSizeMap[sizeKey] || 'A4'
    emit('paperSizeChange', sizeName)
    showPaperSizeOptions.value = false
  }
}

// 处理纸张方向切换
const handlePaperDirectionToggle = () => {
  showPaperDirectionOptions.value = !showPaperDirectionOptions.value
  showPaperSizeOptions.value = false
}

// 处理纸张方向选项点击
const handlePaperDirectionOptionClick = (event: Event) => {
  const target = event.target as HTMLElement
  const li = target.closest('li') as HTMLLIElement
  if (li && li.dataset.paperDirection) {
    const direction = li.dataset.paperDirection === 'vertical' ? 'portrait' : 'landscape'
    emit('paperDirectionChange', direction)
    showPaperDirectionOptions.value = false
  }
}

// 处理页边距
const handlePaperMarginClick = () => {
  emit('paperMarginChange')
}

// 处理全屏切换
const handleFullscreenToggle = () => {
  emit('fullscreenToggle')
}

// 处理编辑器设置
const handleEditorOption = () => {
  emit('editorOption')
}
</script>

<style scoped>
.footbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.footbar-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
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

/* 缩放控制 */
.scale-controls {
  display: flex;
  align-items: center;
  gap: 2px;
  background-color: var(--emr-scale-bg, #f0f0f0);
  border-radius: 4px;
  padding: 2px;
}

.scale-minus,
.scale-add {
  width: 20px;
  height: 20px;
}

.scale-percentage {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: 20px;
  font-size: 11px;
  font-weight: 600;
  color: var(--emr-button-color, #666);
  background-color: var(--emr-scale-contrast-bg, #ffffff);
  border-radius: 2px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;
}

.scale-percentage:hover {
  background-color: var(--emr-button-hover-bg, #e6f7ff);
  color: var(--emr-button-color, #1890ff);
}

/* 纸张控制 */
.paper-controls {
  display: flex;
  align-items: center;
  gap: 2px;
}

.paper-size-container,
.paper-direction-container {
  position: relative;
}

.paper-size-options,
.paper-direction-options {
  width: 80px;
  position: absolute;
  right: 0;
  bottom: 25px;
  padding: 10px;
  background: #fff;
  font-size: 12px;
  box-shadow: 0 2px 12px 0 rgb(56 56 56 / 20%);
  border: 1px solid #e2e6ed;
  border-radius: 2px;
  display: none;
  z-index: 1000;
}

.paper-size-options.visible,
.paper-direction-options.visible {
  display: block;
}

.paper-size-options ul,
.paper-direction-options ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.paper-size-options li,
.paper-direction-options li {
  padding: 5px;
  margin: 5px 0;
  user-select: none;
  transition: all .3s;
  text-align: center;
  cursor: pointer;
  border-radius: 2px;
  font-size: 11px;
}

.paper-size-options li:hover,
.paper-direction-options li:hover {
  background-color: #ebecef;
}

.paper-size-options li.active,
.paper-direction-options li.active {
  background-color: #e2e6ed;
}

.paper-size,
.paper-direction,
.paper-margin {
  width: 24px;
  height: 22px;
}

/* 其他控制 */
.other-controls {
  display: flex;
  align-items: center;
  gap: 2px;
}

.fullscreen,
.editor-option {
  width: 24px;
  height: 22px;
}

/* 图标样式 */

/* 使用SVG图标 */
.footbar-right .icon-minus {
  display: inline-block;
  width: 14px;
  height: 14px;
  background-image: url('../../assets/images/page-scale-minus.svg');
  background-size: 14px 14px;
  background-repeat: no-repeat;
}

.footbar-right .icon-minus::before { content: none !important; }

.footbar-right .icon-plus {
  display: inline-block;
  width: 14px;
  height: 14px;
  background-image: url('../../assets/images/page-scale-add.svg');
  background-size: 14px 14px;
  background-repeat: no-repeat;
}

.footbar-right .icon-paper-size {
  display: inline-block;
  width: 14px;
  height: 14px;
  background-image: url('../../assets/images/paper-size.svg');
  background-size: 14px 14px;
  background-repeat: no-repeat;
}

.footbar-right .icon-paper-direction {
  display: inline-block;
  width: 14px;
  height: 14px;
  background-image: url('../../assets/images/paper-direction.svg');
  background-size: 14px 14px;
  background-repeat: no-repeat;
}

.footbar-right .icon-paper-margin {
  display: inline-block;
  width: 14px;
  height: 14px;
  background-image: url('../../assets/images/paper-margin.svg');
  background-size: 14px 14px;
  background-repeat: no-repeat;
}

.footbar-right .icon-fullscreen {
  display: inline-block;
  width: 14px;
  height: 14px;
  background-image: url('../../assets/images/request-fullscreen.svg');
  background-size: 14px 14px;
  background-repeat: no-repeat;
}

.footbar-right .icon-settings {
  display: inline-block;
  width: 14px;
  height: 14px;
  background-image: url('../../assets/images/option.svg');
  background-size: 14px 14px;
  background-repeat: no-repeat;
}


/* 兼容性样式 */
.page-scale-percentage {
  min-width: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  height: 20px;
  background: var(--emr-scale-contrast-bg, #fff);
  border-radius: 2px;
  font-weight: 600;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .footbar-right {
    gap: 6px;
  }

  .scale-percentage {
    min-width: 35px;
    font-size: 10px;
  }
}

@media (max-width: 768px) {
  .footbar-right {
    flex-wrap: wrap;
    gap: 4px;
  }

  .scale-controls {
    order: -1;
    flex: 1;
    justify-content: center;
  }

  .paper-controls,
  .other-controls {
    flex: 1;
    justify-content: center;
  }

  .footbar-button {
    width: 24px;
    height: 20px;
  }

  .scale-minus,
  .scale-add {
    width: 18px;
    height: 18px;
  }

  .scale-percentage {
    min-width: 32px;
    height: 18px;
    font-size: 9px;
  }
}

@media (max-width: 480px) {
  .footbar-right {
    gap: 2px;
  }

  .footbar-button {
    width: 22px;
    height: 18px;
  }

  .scale-percentage {
    min-width: 28px;
  }
}
</style>
