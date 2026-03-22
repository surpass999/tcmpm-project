<template>
  <div
    :class="itemClasses"
  :title="title"
  @click="handleClick($event)"
    @mousedown.prevent
  >
    <i v-if="icon && !text" :class="`icon-${icon}`"></i>
    <span v-else-if="text">{{ text }}</span>
    <span v-else :style="displayTextStyle">{{ currentDisplayText }}</span>

    <!-- Color underline for color and highlight commands -->
    <span
      v-if="props.command === 'color' || props.command === 'highlight'"
      class="color-underline"
      :style="{ backgroundColor: currentColorValue }"
    ></span>

    <!-- 下拉箭头 -->
    <i v-if="hasDropdown" class="dropdown-arrow"></i>

    <!-- 下拉菜单 -->
    <div v-if="hasDropdown && showDropdown" class="dropdown-menu" @click.stop>
      <ul>
        <li
          v-for="option in dropdownOptions"
          :key="String(option.value)"
          :class="{ active: String(option.value) === String(currentValue) }"
          @click="handleOptionSelect(option)"
          v-bind="props.command === 'underline' ? { 'data-decoration-style': option.value } : (props.command === 'separator' ? { 'data-separator': option.value } : {})"
        >
          <!-- underline special rendering (show pattern only) -->
          <template v-if="props.command === 'underline'">
            <!-- li uses data-decoration-style background images via CSS -->
            <span class="sr-only" v-if="false">{{ option.label }}</span>
          </template>

          <!-- title preview -->
          <template v-if="props.command === 'title'">
            <div class="title-name" :style="option.style">
              {{ option.label }}
            </div>
          </template>

          <!-- font preview -->
          <template v-else-if="props.command === 'font'">
            <div
              class="font-sample"
              v-if="String(option.value) === String(currentValue)"
              :style="{ fontFamily: String(option.value) }"
            >
              {{ option.label }}
            </div>
            <div class="font-name" :style="{ fontFamily: String(option.value) }">
              {{ option.label }}
            </div>
          </template>

          <!-- size (show label only for better UX) -->
          <template v-else-if="props.command === 'size'">
            <div class="size-name">{{ option.label }}</div>
          </template>

          <!-- list preview (when list dropdown is rendered via ToolbarItem) -->
          <template v-else-if="props.command === 'list'">
            <div class="list-preview">
              <template v-if="String(option.value).startsWith('ol')">1. 项目</template>
              <template v-else-if="String(option.value).includes('checkbox')">☑  项目</template>
              <template v-else-if="String(option.value).includes('disc')">• 项目</template>
              <template v-else-if="String(option.value).includes('circle')">◦ 项目</template>
              <template v-else-if="String(option.value).includes('square')">▪ 项目</template>
              <template v-else>• 项目</template>
            </div>
            <div class="list-label">{{ option.label }}</div>
          </template>

          <!-- date preview: render current date/time according to option.value format -->
          <template v-else-if="props.command === 'date'">
            <div class="date-label">{{ formatDate(String(option.value)) }}</div>
          </template>

          <!-- icon / html / default -->
          <template v-else-if="option.icon">
            <i :class="`icon-${option.icon}`"></i>
            <span v-if="option.label" class="option-label">{{ option.label }}</span>
          </template>
          <template v-else-if="option.html" v-html="option.html"></template>
          <template v-else>
            {{ option.label }}
          </template>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'

const formatDate = (fmt: string) => {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return fmt
    .replace('yyyy', String(now.getFullYear()))
    .replace('MM', pad(now.getMonth() + 1))
    .replace('dd', pad(now.getDate()))
    .replace('hh', pad(now.getHours()))
    .replace('mm', pad(now.getMinutes()))
    .replace('ss', pad(now.getSeconds()))
}

export interface ToolbarItemOption {
  value: string | number
  label: string
  icon?: string
  html?: string
  style?: Record<string, string | number>
}

export interface ToolbarItemProps {
  type: 'button' | 'dropdown' | 'select'
  command: string
  icon?: string
  text?: string
  title: string
  active?: boolean
  disabled?: boolean
  dropdownOptions?: ToolbarItemOption[]
  currentValue?: string | number
  displayText?: string
}

const props = withDefaults(defineProps<ToolbarItemProps>(), {
  active: false,
  disabled: false
})

const emit = defineEmits<{
  click: [command: string, target?: HTMLElement]
  select: [command: string, value: string | number]
}>()

const showDropdown = ref(false)

// Close this dropdown when another dropdown opens (global event)
const onGlobalDropdownOpen = (evt: Event) => {
  const ce = evt as CustomEvent
  const other = ce?.detail?.command
  if (other === undefined) return
  // if other is null -> close all
  if (other === null) {
    showDropdown.value = false
    return
  }
  if (String(other) !== String(props.command)) {
    showDropdown.value = false
  }
}

// 计算样式类名
const itemClasses = computed(() => {
  const classes = ['toolbar-item', `toolbar-item__${props.command}`]

  if (props.active) classes.push('active')
  if (props.disabled) classes.push('disabled')
  if (hasDropdown.value) classes.push('has-dropdown')

  return classes
})

// 是否有下拉菜单
const hasDropdown = computed(() => {
  return props.type === 'dropdown' || props.type === 'select'
})

// 当前显示文本（动态根据currentValue变化）
const currentDisplayText = computed(() => {
  if (!hasDropdown.value) {
    return props.displayText || props.title
  }

  if (props.currentValue !== undefined && props.dropdownOptions) {
    const option = props.dropdownOptions.find(opt => String(opt.value) === String(props.currentValue))
    if (option) {
      return option.label
    }
  }

  return props.displayText || props.title
})

// 显示文本样式（用于字体预览）
const displayTextStyle = computed(() => {
  if (props.command === 'font' && typeof props.currentValue === 'string') {
    return { fontFamily: props.currentValue }
  }
  return {}
})

// 当前颜色值（用于颜色下划线）
const currentColorValue = computed(() => {
  if ((props.command === 'color' || props.command === 'highlight') && props.currentValue) {
    return String(props.currentValue)
  }
  // 默认值
  return props.command === 'color' ? '#000000' : '#ffff00'
})

// 点击处理
const handleClick = (evt: MouseEvent) => {
  if (props.disabled) return

  if (hasDropdown.value) {
    const willOpen = !showDropdown.value
    showDropdown.value = willOpen
    if (willOpen) {
      // notify others to close
      document.dispatchEvent(new CustomEvent('emr-dropdown-open', { detail: { command: props.command } }))
    } else {
      // closed, notify close-all
      document.dispatchEvent(new CustomEvent('emr-dropdown-open', { detail: { command: null } }))
    }
  } else {
    // clicking a non-dropdown should close any open dropdowns
    document.dispatchEvent(new CustomEvent('emr-dropdown-open', { detail: { command: null } }))
    // special-case color/highlight: open native color picker
    if (props.command === 'color' || props.command === 'highlight') {
      // Prevent recursive calls
      if ((evt?.target as HTMLElement)?.tagName === 'INPUT' && (evt?.target as HTMLInputElement)?.type === 'color') {
        return
      }

      // Get the button element
      const buttonElement = (evt?.target as HTMLElement)?.closest('.toolbar-item') as HTMLElement
      if (!buttonElement) return

      // Create color input and temporarily add it to the button
      const colorInput = document.createElement('input')
      colorInput.type = 'color'
      colorInput.style.position = 'absolute'
      colorInput.style.left = '0'
      colorInput.style.top = '0'
      colorInput.style.width = '100%'
      colorInput.style.height = '100%'
      colorInput.style.opacity = '0'
      colorInput.style.border = 'none'
      colorInput.style.padding = '0'
      colorInput.style.margin = '0'
      colorInput.style.cursor = 'pointer'

      // Set up event listeners - input for real-time updates, change for cleanup
      const handleInput = (e: Event) => {
        e.stopPropagation()
        const v = (e.target as HTMLInputElement).value
        emit('select', props.command, v)
      }

      const handleChange = (e: Event) => {
        e.stopPropagation()
        // Remove input when user closes color picker
        colorInput.removeEventListener('input', handleInput)
        colorInput.removeEventListener('change', handleChange)
        if (colorInput.parentNode) {
          colorInput.parentNode.removeChild(colorInput)
        }
      }

      colorInput.addEventListener('input', handleInput)
      colorInput.addEventListener('change', handleChange)

      // Add input to button and trigger click
      buttonElement.style.position = buttonElement.style.position || 'relative'
      buttonElement.appendChild(colorInput)

      // Trigger click with a small delay to prevent immediate recursion
      setTimeout(() => {
        colorInput.click()
      }, 10)
      return
    }
    if (props.command === 'image') {
      const input = document.createElement('input')
      input.type = 'file'
      input.accept = '.png,.jpg,.jpeg,.gif,.svg'
      input.style.position = 'fixed'
      input.style.left = '-9999px'
      document.body.appendChild(input)
      input.addEventListener('change', (e: Event) => {
        const f = (e.target as HTMLInputElement).files?.[0]
        if (!f) {
          document.body.removeChild(input)
          return
        }
        const reader = new FileReader()
        reader.onload = () => {
          const data = reader.result as string
          emit('select', props.command, data)
          document.body.removeChild(input)
        }
        reader.onerror = () => {
          document.body.removeChild(input)
        }
        reader.readAsDataURL(f)
      }, { once: true })
      input.click()
      return
    }
    emit('click', props.command, evt.currentTarget as HTMLElement)
  }
}

// 选项选择
const handleOptionSelect = (option: ToolbarItemOption) => {
  showDropdown.value = false
  emit('select', props.command, option.value)
}

// 点击其他地方关闭下拉菜单
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.toolbar-item')) {
    showDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('emr-dropdown-open', onGlobalDropdownOpen as EventListener)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('emr-dropdown-open', onGlobalDropdownOpen as EventListener)
})
</script>

<style scoped>
.toolbar-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 26px;
  padding: 0 6px;
  margin: 0 1px;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.15s ease, color 0.15s ease, transform 0.12s ease;
  font-size: 12px;
  color: var(--emr-button-color, #666);
}

.toolbar-item:hover:not(.disabled) {
  background-color: var(--emr-button-hover-bg, #f5f5f5);
  color: var(--emr-button-color, #333);
}

.toolbar-item.active {
  background-color: var(--emr-button-active-bg, #e6f7ff);
  color: var(--emr-button-color, #1890ff);
}

.toolbar-item.disabled {
  cursor: not-allowed;
  opacity: 0.4;
  color: #ccc;
}

.toolbar-item i {
  font-size: 13px;
  width: 14px;
  height: 14px;
  background-size: 14px 14px;
  background-repeat: no-repeat;
  background-position: center;
  display: inline-block;
}

/* 防止全局 iconfont 的 ::before 覆盖背景图片图标（有些库会使用伪元素插入字体图标） */
.toolbar-item i::before {
  display: none !important;
  content: "" !important;
  line-height: 0 !important;
}

/* 针对可能来自全局 iconfont 的伪元素做更高优先级的覆盖 */
.toolbar-item [class^="icon-"]::before,
.toolbar-item [class*=" icon-"]::before {
  display: none !important;
  content: "" !important;
  font-family: initial !important;
  width: 0 !important;
  height: 0 !important;
  line-height: 0 !important;
  overflow: hidden !important;
}

/* 图标样式 - 基础操作 */
.toolbar-item .icon-undo {
  background-image: url('../../assets/images/undo.svg');
}

.toolbar-item .icon-redo {
  background-image: url('../../assets/images/redo.svg');
}

.toolbar-item .icon-paint-brush,
.toolbar-item .icon-painter {
  background-image: url('../../assets/images/painter.svg');
}

.toolbar-item .icon-eraser,
.toolbar-item .icon-format {
  background-image: url('../../assets/images/format.svg');
}

/* 文本格式图标 */
.toolbar-item .icon-bold {
  background-image: url('../../assets/images/bold.svg');
}

.toolbar-item .icon-italic {
  background-image: url('../../assets/images/italic.svg');
}

.toolbar-item .icon-underline {
  background-image: url('../../assets/images/underline.svg');
}

.toolbar-item .icon-strikeout {
  background-image: url('../../assets/images/strikeout.svg');
}

.toolbar-item .icon-superscript {
  background-image: url('../../assets/images/superscript.svg');
}

.toolbar-item .icon-subscript {
  background-image: url('../../assets/images/subscript.svg');
}

.toolbar-item .icon-color {
  background-image: url('../../assets/images/color.svg');
}

.toolbar-item .icon-highlight {
  background-image: url('../../assets/images/highlight.svg');
}

/* 对齐图标 */
.toolbar-item .icon-align-left,
.toolbar-item .icon-left {
  background-image: url('../../assets/images/left.svg');
}

.toolbar-item .icon-align-center,
.toolbar-item .icon-center {
  background-image: url('../../assets/images/center.svg');
}

.toolbar-item .icon-align-right,
.toolbar-item .icon-right {
  background-image: url('../../assets/images/right.svg');
}

.toolbar-item .icon-alignment {
  background-image: url('../../assets/images/alignment.svg');
}
.toolbar-item .icon-justify {
  background-image: url('../../assets/images/justify.svg');
}

/* 其他图标 */
.toolbar-item .icon-list {
  background-image: url('../../assets/images/list.svg');
}

.toolbar-item .icon-table {
  background-image: url('../../assets/images/table.svg');
}

.toolbar-item .icon-image {
  background-image: url('../../assets/images/image.svg');
}

.toolbar-item .icon-link,
.toolbar-item .icon-hyperlink {
  background-image: url('../../assets/images/hyperlink.svg');
}

.toolbar-item .icon-formula,
.toolbar-item .icon-latex {
  background-image: url('../../assets/images/latex.svg');
}

.toolbar-item .icon-separator {
  background-image: url('../../assets/images/separator.svg');
}

.toolbar-item .icon-zoom-in,
.toolbar-item .icon-size-add {
  background-image: url('../../assets/images/size-add.svg');
}

.toolbar-item .icon-zoom-out,
.toolbar-item .icon-size-minus {
  background-image: url('../../assets/images/size-minus.svg');
}

.toolbar-item .icon-page-mode {
  background-image: url('../../assets/images/page-mode.svg');
}

.toolbar-item .icon-row-margin {
  background-image: url('../../assets/images/row-margin.svg');
}

/* 线型图标（用于下划线样式） */
.toolbar-item .icon-line-single { background-image: url('../../assets/images/line-single.svg'); }
.toolbar-item .icon-line-double { background-image: url('../../assets/images/line-double.svg'); }
.toolbar-item .icon-line-dash { background-image: url('../../assets/images/line-dash-small-gap.svg'); }
.toolbar-item .icon-line-dot { background-image: url('../../assets/images/line-dot.svg'); }
.toolbar-item .icon-line-wavy { background-image: url('../../assets/images/line-wavy.svg'); }

.toolbar-item .icon-fullscreen,
.toolbar-item .icon-request-fullscreen {
  background-image: url('../../assets/images/request-fullscreen.svg');
}

.toolbar-item .icon-exit-fullscreen {
  background-image: url('../../assets/images/exit-fullscreen.svg');
}

.toolbar-item .icon-paper-size {
  background-image: url('../../assets/images/paper-size.svg');
}

.toolbar-item .icon-paper-direction {
  background-image: url('../../assets/images/paper-direction.svg');
}

.toolbar-item .icon-paper-margin {
  background-image: url('../../assets/images/paper-margin.svg');
}

.toolbar-item .icon-editor-option,
.toolbar-item .icon-option {
  background-image: url('../../assets/images/option.svg');
}

/* 医疗控件图标 */
.toolbar-item .icon-control-text,
.toolbar-item .icon-text {
  background-image: url('../../assets/images/control.svg');
}

.toolbar-item .icon-control-select,
.toolbar-item .icon-select {
  background-image: url('../../assets/images/control.svg');
}

.toolbar-item .icon-control-checkbox,
.toolbar-item .icon-checkbox {
  background-image: url('../../assets/images/checkbox.svg');
}

.toolbar-item .icon-control-radio,
.toolbar-item .icon-radio {
  background-image: url('../../assets/images/radio.svg');
}

.toolbar-item .icon-control-date,
.toolbar-item .icon-date {
  background-image: url('../../assets/images/date.svg');
}

.toolbar-item .icon-control-number,
.toolbar-item .icon-number {
  background-image: url('../../assets/images/control.svg');
}

/* 排除 dropdown-arrow 不被通用 i 样式影响（triangle 用 border 绘制） */
.toolbar-item .dropdown-arrow {
  width: 0;
  height: 0;
  background: none;
  border: 4px solid transparent;
  border-top-color: #666;
  margin-left: 4px;
  transform: rotate(0deg);
  transition: transform 0.15s ease, border-top-color 0.15s ease;
  display: inline-block;
}

.toolbar-item.has-dropdown:hover .dropdown-arrow {
  border-top-color: #333;
}

/* 兼容别名：有些配置使用 palette/highlighter/file-alt 等命名，添加别名类指向实际 svg */
.toolbar-item .icon-palette { background-image: url('../../assets/images/color.svg'); }
.toolbar-item .icon-highlighter { background-image: url('../../assets/images/highlight.svg'); }
.toolbar-item .icon-file-alt { background-image: url('../../assets/images/page-mode.svg'); }
.toolbar-item .icon-codeblock { background-image: url('../../assets/images/codeblock.svg'); }
.toolbar-item .icon-page-break { background-image: url('../../assets/images/page-break.svg'); }
/* 搜索 / 打印 图标 */
.toolbar-item .icon-search { background-image: url('../../assets/images/search.svg'); }
.toolbar-item .icon-print { background-image: url('../../assets/images/print.svg'); }
/* 补全别名：control/watermark/block 指向现有 svg */
.toolbar-item .icon-control { background-image: url('../../assets/images/control.svg'); }
.toolbar-item .icon-watermark { background-image: url('../../assets/images/watermark.svg'); }
.toolbar-item .icon-block { background-image: url('../../assets/images/block.svg'); }

.toolbar-item.has-dropdown .dropdown-arrow {
  margin-left: 4px;
  border: 3px solid transparent;
  border-top-color: #666;
  transform: rotate(0deg);
  transition: transform 0.2s ease;
}

.toolbar-item.has-dropdown:hover .dropdown-arrow {
  border-top-color: #333;
}

/* 下拉菜单 */
.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  min-width: 120px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  margin-top: 2px;
}

.dropdown-menu ul {
  list-style: none;
  margin: 0;
  padding: 4px 0;
}

.dropdown-menu li {
  padding: 6px 6px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: background-color 0.2s ease;
  display: flex;
  align-items: center;
  gap: 2px;
}

.dropdown-menu li i {
  width: 18px;
  height: 14px;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  display: inline-block;
}

.dropdown-menu li .option-label {
  display: inline-block;
}

.dropdown-menu li:hover {
  background-color: #f5f5f5;
}

.dropdown-menu li.active {
  background-color: #e6f7ff;
  color: #1890ff;
  font-weight: 500;
}

/* font preview styles in dropdown (duplicate of ToolbarDropdown for items rendered here) */
.dropdown-menu li .font-sample {
  background: #f3f4f6;
  border-radius: 6px;
  padding: 8px 12px;
  box-shadow: 0 1px 0 rgba(0,0,0,0.04);
  font-size: 16px;
  color: #111827;
  min-width: 140px;
  text-align: left;
}

.dropdown-menu li .font-name {
  flex: 1;
  font-size: 14px;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-menu li .size-name {
  font-size: 14px;
  color: #111827;
}

.dropdown-menu li .date-label {
  font-size: 14px;
  color: #111827;
}
/* ensure date option stays on single line and dropdown wide enough */
.toolbar-item__date .dropdown-menu {
  min-width: 160px;
}

.toolbar-item__size .dropdown-menu {
  min-width: 60px;
}

.toolbar-item__font .dropdown-menu {
  min-width: 100px;
}

.toolbar-item__title .dropdown-menu {
  min-width: 100px;
}

.toolbar-item__list .dropdown-menu {
  min-width: 140px;
}

.toolbar-item__date .dropdown-menu li .date-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* underline dropdown styles to match original project */
.toolbar-item__underline .dropdown-menu {
  width: 128px;
  padding: 0;
}
.toolbar-item__underline .dropdown-menu ul {
  padding: 0;
}
.toolbar-item__underline .dropdown-menu li {
  padding: 1px 5px;
  height: 22px;
  background-repeat: no-repeat;
  background-position: center left;
  background-size: contain;
}
.toolbar-item__underline .dropdown-menu li[data-decoration-style="solid"] {
  background-image: url('../../assets/images/line-single.svg');
}
.toolbar-item__underline .dropdown-menu li[data-decoration-style="double"] {
  background-image: url('../../assets/images/line-double.svg');
}
.toolbar-item__underline .dropdown-menu li[data-decoration-style="dashed"] {
  background-image: url('../../assets/images/line-dash-small-gap.svg');
}
.toolbar-item__underline .dropdown-menu li[data-decoration-style="dotted"] {
  background-image: url('../../assets/images/line-dot.svg');
}
.toolbar-item__underline .dropdown-menu li[data-decoration-style="wavy"] {
  background-image: url('../../assets/images/line-wavy.svg');
}

/* title preview in ToolbarItem dropdown */
.toolbar-item__title .dropdown-menu li .title-name {
  padding: 0px 8px;
  font-weight: 500;
  color: #111827;
}
.toolbar-item__title .dropdown-menu li:first-child .title-name {
  font-weight: normal;
}

/* restore bold for active font items (keep title-name normal) */
.dropdown-menu li.active .font-name,
.dropdown-menu li.active .font-sample {
  font-weight: 500;
}

/* Color underline for color and highlight buttons */
.color-underline {
  width: 16px;
  height: 3px;
  display: inline-block;
  border: 1px solid #e2e6ed;
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%);
  border-radius: 1px;
}

.toolbar-item__color .color-underline {
  background-color: #000000;
}

.toolbar-item__highlight .color-underline {
  background-color: #ffff00;
}

</style>
