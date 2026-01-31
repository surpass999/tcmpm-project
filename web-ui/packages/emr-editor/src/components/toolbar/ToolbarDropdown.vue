<template>
  <div class="toolbar-dropdown" :class="dropdownClasses">
    <div class="dropdown-trigger" @click="toggleDropdown">
      <span class="dropdown-label">{{ label }}</span>
      <i class="dropdown-arrow" :class="{ expanded: showDropdown }"></i>
    </div>

    <!-- 下拉面板 -->
    <div v-if="showDropdown" class="dropdown-panel" @click.stop>
      <!-- 颜色选择器 -->
      <div v-if="type === 'color'" class="color-picker">
        <div class="color-grid">
          <div
            v-for="color in colorOptions"
            :key="color"
            class="color-item"
            :style="{ backgroundColor: color }"
            @click="selectColor(color)"
          ></div>
        </div>
        <div class="color-input">
          <input
            v-model="customColor"
            type="text"
            placeholder="#000000"
            @input="handleColorInput"
            @keydown.enter="applyCustomColor"
          />
          <button @click="applyCustomColor">确定</button>
        </div>
      </div>

      <!-- 字体选择器：增加字体预览样式以接近原项目 -->
      <div v-else-if="type === 'font'" class="font-picker">
        <div class="font-list">
          <div
            v-for="font in fontOptions"
            :key="String(font.value)"
            class="font-item"
            :class="{ active: String(font.value) === String(currentValue) }"
            @click="selectFont(String(font.value))"
          >
            <!-- 如果当前为选中项，显示一个带背景的预览块 -->
            <div
              class="font-sample"
              v-if="String(font.value) === String(currentValue)"
              :style="{ fontFamily: String(font.value) }"
            >
              {{ font.label }}
            </div>

            <!-- 始终显示字体名称，名称使用该字体渲染以便预览 -->
            <div class="font-name" :style="{ fontFamily: String(font.value) }">
              {{ font.label }}
            </div>
          </div>
        </div>
      </div>

      <!-- 标题选择器（正文/标题1/2/3），每项可带 font-size inline style -->
      <div v-else-if="type === 'title'" class="title-picker">
        <div class="title-list">
          <div
            v-for="opt in options"
            :key="String(opt.value)"
            class="title-item"
            :class="{ active: String(opt.value) === String(currentValue) }"
            :style="opt.style"
            @click="selectOption(opt.value)"
          >
            {{ opt.label }}
          </div>
        </div>
      </div>

      <!-- 字号选择器 -->
      <div v-else-if="type === 'size'" class="size-picker">
        <div class="size-list">
          <div
            v-for="size in sizeOptions"
            :key="String(size.value)"
            class="size-item"
            :class="{ active: String(size.value) === String(currentValue) }"
            @click="selectSize(Number(size.value))"
          >
            {{ size.label }}
          </div>
        </div>
      </div>

      <!-- 行间距选择器（带预览） -->
      <div v-else-if="type === 'row-margin'" class="row-margin-picker">
        <div class="row-margin-list">
          <div
            v-for="opt in options || sizeOptions"
            :key="String(opt.value)"
            class="row-margin-item"
            :class="{ active: String(opt.value) === String(currentValue) }"
            @click="selectOption(opt.value)"
          >
            <div class="row-margin-preview" :style="{ lineHeight: String(opt.value) }">行间距</div>
            <div class="row-margin-label">{{ opt.label }}</div>
          </div>
        </div>
      </div>

      <!-- 列表样式选择器（带样例图标） -->
      <div v-else-if="type === 'list'" class="list-picker">
        <div class="list-list">
          <div
            v-for="opt in options"
            :key="String(opt.value)"
            class="list-item"
            :class="{ active: String(opt.value) === String(currentValue) }"
            @click="selectOption(opt.value)"
          >
            <div class="list-preview">
              <template v-if="String(opt.value).startsWith('ol')">1. 项目</template>
              <template v-else-if="String(opt.value).includes('checkbox')">☑  项目</template>
              <template v-else-if="String(opt.value).includes('disc')">• 项目</template>
              <template v-else-if="String(opt.value).includes('circle')">◦ 项目</template>
              <template v-else-if="String(opt.value).includes('square')">▪ 项目</template>
              <template v-else>• 项目</template>
            </div>
            <div class="list-label">{{ opt.label }}</div>
          </div>
        </div>
      </div>

      <!-- 通用选项列表 -->
      <div v-else class="option-list">
        <div
          v-for="option in options"
          :key="option.value"
          class="option-item"
          :class="{ active: option.value === currentValue }"
          @click="selectOption(option.value)"
        >
          <template v-if="option.html" v-html="option.html"></template>
          <template v-else>{{ option.label }}</template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

export interface DropdownOption {
  value: string | number
  label: string
  icon?: string
  html?: string
  style?: Record<string, string | number>
}

export interface ToolbarDropdownProps {
  type: 'color' | 'font' | 'size' | 'list' | 'title' | 'row-margin'
  label: string
  currentValue?: string | number
  options?: DropdownOption[]
  disabled?: boolean
}

const props = withDefaults(defineProps<ToolbarDropdownProps>(), {
  disabled: false
})

const emit = defineEmits<{
  select: [value: string | number, type: string]
}>()

const showDropdown = ref(false)
const customColor = ref('')

// 预定义的颜色选项
const colorOptions = [
  '#000000', '#333333', '#666666', '#999999', '#CCCCCC', '#FFFFFF',
  '#FF0000', '#00FF00', '#0000FF', '#FFFF00', '#FF00FF', '#00FFFF',
  '#FFA500', '#800080', '#008000', '#000080', '#800000', '#808000'
]

// 字体选项（参考原 HTML 列表）
const fontOptions: DropdownOption[] = [
  { value: "'Microsoft YaHei', '微软雅黑', sans-serif", label: '微软雅黑' },
  { value: "'华文宋体', 'STSong', 'Microsoft YaHei', sans-serif", label: '华文宋体' },
  { value: "'华文黑体', 'STHeiti', 'Microsoft YaHei', sans-serif", label: '华文黑体' },
  { value: "'华文仿宋', 'FangSong', 'Microsoft YaHei', sans-serif", label: '华文仿宋' },
  { value: "'华文楷体', 'STKaiti', 'Microsoft YaHei', sans-serif", label: '华文楷体' },
  { value: "'华文琥珀', 'HuaWenHuPo', 'Microsoft YaHei', sans-serif", label: '华文琥珀' },
  { value: "'华文隶书', 'STLiti', 'Microsoft YaHei', sans-serif", label: '华文隶书' },
  { value: "'华文新魏', 'STXinwei', 'Microsoft YaHei', sans-serif", label: '华文新魏' },
  { value: "'华文行楷', 'STXingkai', 'Microsoft YaHei', sans-serif", label: '华文行楷' },
  { value: "'华文中宋', 'STZhongsong', 'Microsoft YaHei', sans-serif", label: '华文中宋' },
  { value: "'华文彩云', 'STCaiyun', 'Microsoft YaHei', sans-serif", label: '华文彩云' },
  { value: "Arial, 'Helvetica Neue', Helvetica, sans-serif", label: 'Arial' },
  { value: "'Segoe UI', Tahoma, Geneva, sans-serif", label: 'Segoe UI' },
  { value: "'Ink Free', cursive", label: 'Ink Free' },
  { value: "Fantasy, 'Impact', Charcoal, sans-serif", label: 'Fantasy' }
]

// 字号选项（参考原 HTML 列表）
const sizeOptions: DropdownOption[] = [
  { value: 56, label: '初号' },
  { value: 48, label: '小初' },
  { value: 34, label: '一号' },
  { value: 32, label: '小一' },
  { value: 29, label: '二号' },
  { value: 24, label: '小二' },
  { value: 21, label: '三号' },
  { value: 20, label: '小三' },
  { value: 18, label: '四号' },
  { value: 16, label: '小四' },
  { value: 14, label: '五号' },
  { value: 12, label: '小五' },
  { value: 10, label: '六号' },
  { value: 8, label: '小六' },
  { value: 7, label: '七号' },
  { value: 6, label: '八号' }
]

// 计算样式类名
const dropdownClasses = computed(() => {
  return [
    'toolbar-dropdown',
    `toolbar-dropdown--${props.type}`,
    { disabled: props.disabled }
  ]
})

// 切换下拉菜单
const toggleDropdown = () => {
  if (props.disabled) return
  showDropdown.value = !showDropdown.value
}

// 选择颜色
const selectColor = (color: string) => {
  showDropdown.value = false
  emit('select', color, 'color')
}

// 自定义颜色输入
const handleColorInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  customColor.value = target.value
}

// 应用自定义颜色
const applyCustomColor = () => {
  if (customColor.value) {
    selectColor(customColor.value)
    customColor.value = ''
  }
}

// 选择字体
const selectFont = (font: string) => {
  showDropdown.value = false
  emit('select', font, 'font')
}

// 选择字号
const selectSize = (size: number) => {
  showDropdown.value = false
  emit('select', size, 'size')
}

// 选择通用选项
const selectOption = (value: string | number) => {
  showDropdown.value = false
  emit('select', value, props.type)
}

// 点击外部关闭下拉菜单
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.toolbar-dropdown')) {
    showDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.toolbar-dropdown {
  position: relative;
  display: inline-block;
}

.dropdown-trigger {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  min-width: 80px;
  font-size: 12px;
  color: #666;
  transition: border-color 0.2s ease;
}

.dropdown-trigger:hover:not(.disabled) {
  border-color: #1890ff;
}

.dropdown-trigger.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.dropdown-label {
  flex: 1;
  text-align: left;
}

.dropdown-arrow {
  width: 0;
  height: 0;
  border: 4px solid transparent;
  border-top-color: #666;
  margin-left: 4px;
  transition: transform 0.2s ease;
}

.dropdown-arrow.expanded {
  transform: rotate(180deg);
}

/* 下拉面板 */
.dropdown-panel {
  position: absolute;
  top: 100%;
  left: 0;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  margin-top: 2px;
  min-width: 200px;
  max-width: 300px;
}

/* 颜色选择器 */
.color-picker {
  padding: 12px;
}

.color-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 4px;
  margin-bottom: 8px;
}

.color-item {
  width: 20px;
  height: 20px;
  border-radius: 2px;
  cursor: pointer;
  border: 1px solid #ddd;
  transition: transform 0.2s ease;
}

.color-item:hover {
  transform: scale(1.1);
}

.color-input {
  display: flex;
  gap: 4px;
}

.color-input input {
  flex: 1;
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 3px;
  font-size: 12px;
}

.color-input button {
  padding: 4px 8px;
  border: 1px solid #ddd;
  background: #f5f5f5;
  border-radius: 3px;
  cursor: pointer;
  font-size: 12px;
}

/* 字体选择器 */
.font-picker {
  padding: 8px 0;
}

.font-list {
  max-height: 200px;
  overflow-y: auto;
}

.font-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.15s ease;
}

.font-item:hover {
  background-color: #fafafa;
}

.font-item.active {
  background-color: transparent;
}

.font-sample {
  background: #f3f4f6;
  border-radius: 6px;
  padding: 8px 12px;
  box-shadow: 0 1px 0 rgba(0,0,0,0.04);
  font-size: 16px;
  color: #111827;
  min-width: 140px;
  text-align: left;
}

.font-name {
  flex: 1;
  font-size: 14px;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 字号选择器 */
.size-picker {
  padding: 8px 0;
}

.size-list {
  max-height: 200px;
  overflow-y: auto;
}

.size-item {
  padding: 8px 12px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.size-item:hover {
  background-color: #f5f5f5;
}

.size-item.active {
  background-color: #e6f7ff;
  color: #1890ff;
}

/* 通用选项列表 */
.option-list {
  padding: 8px 0;
}

/* row-margin picker */
.row-margin-list { max-height: 200px; overflow-y: auto; }
.row-margin-item { display:flex; align-items:center; gap:8px; padding:6px 10px; cursor:pointer; }
.row-margin-preview { font-size:12px; color:#666; padding:4px 8px; background:#fafafa; border-radius:4px; }
.row-margin-label { color:#666; }
.row-margin-item.active { background:#e6f7ff; color:#1890ff }

/* list picker */
.list-list { max-height: 200px; overflow-y: auto; }
.list-item { display:flex; align-items:center; gap:8px; padding:6px 10px; cursor:pointer; }
.list-preview { color:#666; width:70px; }
.list-label { color:#666; flex:1; text-align:left; }
.list-item.active { background:#e6f7ff; color:#1890ff }


.option-item {
  padding: 8px 12px;
  cursor: pointer;
  font-size: 12px;
  color: #666;
  transition: background-color 0.2s ease;
}

.option-item:hover {
  background-color: #f5f5f5;
}

.option-item.active {
  background-color: #e6f7ff;
  color: #1890ff;
}

/* 标题选择器样式 */
.title-list {
  max-height: 220px;
  overflow-y: auto;
}
.title-item {
  padding: 6px 8px;
  cursor: pointer;
}
.title-item:hover {
  background: #f5f5f5;
}
.title-item.active {
  background: #e6f7ff;
  color: #1890ff;
  font-weight: 500;
}

/* title-name font weight: only first item normal, others bold */
.title-item .title-name {
  font-weight: 500;
}
.title-item:first-child .title-name {
  font-weight: normal;
}
</style>
