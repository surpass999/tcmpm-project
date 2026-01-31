# EMR Editor Footbar 组件

底部状态栏组件，用于显示页面信息、缩放控制、纸张设置等功能。

## 组件结构

```
footbar/
├── EMRFootbar.vue          # 主 footbar 组件
├── FootbarLeft.vue         # 左侧信息显示
├── FootbarRight.vue        # 右侧控制按钮
├── FootbarConfig.ts        # 配置和工具函数
└── index.ts               # 导出文件
```

## 基础用法

### 完整 footbar

```vue
<template>
  <EMRFootbar
    :page-info="pageInfo"
    :word-count="wordCount"
    :visible-pages="visiblePages"
    :scale="scale"
    :page-mode="pageMode"
    :paper-size="paperSize"
    :paper-direction="paperDirection"
    :is-fullscreen="isFullscreen"
    @scale-change="handleScaleChange"
    @page-mode-change="handlePageModeChange"
    @paper-size-change="handlePaperSizeChange"
    @fullscreen-toggle="handleFullscreenToggle"
  />
</template>

<script setup>
import { EMRFootbar } from '@/components/footbar'

const pageInfo = {
  current: 1,
  total: 3,
  visiblePages: [1, 2]
}

const wordCount = {
  total: 1250,
  selected: 0,
  lines: 45,
  columns: 12
}

const scale = 100
const pageMode = 'paging'
const paperSize = 'A4'
const paperDirection = 'portrait'
const isFullscreen = false

const handleScaleChange = (newScale) => {
  scale.value = newScale
  // 应用缩放
}

const handlePageModeChange = (mode) => {
  pageMode.value = mode
  // 切换分页模式
}
</script>
```

### 部分功能

```vue
<template>
  <!-- 只显示信息 -->
  <FootbarLeft
    :page-info="pageInfo"
    :word-count="wordCount"
    :visible-pages="visiblePages"
  />

  <!-- 只显示控制 -->
  <FootbarRight
    :scale="scale"
    @scale-change="handleScaleChange"
  />
</template>

<script setup>
import { FootbarLeft, FootbarRight } from '@/components/footbar'
</script>
```

## 配置选项

### EMRFootbar Props

```typescript
interface EMRFootbarProps {
  // 页面信息
  pageInfo?: {
    current: number      // 当前页
    total: number        // 总页数
    visiblePages: number[] // 可见页码
  }

  // 字数统计
  wordCount?: {
    total: number        // 总字数
    selected: number     // 选中字数
    lines: number        // 行数
    columns: number      // 列数
  }

  // 可见页码（简化版本）
  visiblePages?: number[]

  // 缩放比例
  scale?: number

  // 分页模式
  pageMode?: 'paging' | 'continuous'

  // 纸张大小
  paperSize?: 'A4' | 'A3' | 'Letter' | 'Legal'

  // 纸张方向
  paperDirection?: 'portrait' | 'landscape'

  // 全屏状态
  isFullscreen?: boolean

  // 布局方式
  layout?: 'horizontal' | 'vertical'
}
```

### 事件

```typescript
interface EMRFootbarEmits {
  scaleChange: [scale: number]              // 缩放变化
  pageModeChange: [mode: string]            // 分页模式变化
  paperSizeChange: [size: string]           // 纸张大小变化
  paperDirectionChange: [direction: string] // 纸张方向变化
  fullscreenToggle: []                      // 全屏切换
  editorOption: []                          // 编辑器设置
}
```

## 功能特性

### 📊 信息显示

#### 页面信息
- **可见页码**: 智能显示页码范围（如 "1-3, 5"）
- **当前页面**: "1/3" 格式显示
- **字数统计**: 总字数、行数、列数
- **选中状态**: 高亮显示选中文本统计

#### 智能格式化
```javascript
// 输入: [1, 2, 3, 5, 6, 7, 9]
// 输出: "1-3, 5-7, 9"
formatVisiblePages([1, 2, 3, 5, 6, 7, 9]) // "1-3, 5-7, 9"
```

### 🎛️ 控制功能

#### 缩放控制
- **步进缩放**: ±10% 步进
- **预设值**: 50%, 75%, 100%, 125%, 150%, 200%
- **范围限制**: 50%-200%
- **智能匹配**: 自动匹配最近的预设值

#### 纸张设置
- **纸张大小**: A4, A3, Letter, Legal 循环切换
- **纸张方向**: 纵向/横向切换
- **页边距**: 普通/窄边距/宽边距

#### 显示控制
- **分页模式**: 分页/连续模式切换
- **全屏模式**: 全屏切换
- **编辑器设置**: 打开设置面板

## 样式定制

组件使用 scoped CSS，可以通过以下方式定制：

```vue
<style scoped>
.emr-footbar {
  /* 自定义 footbar 样式 */
}

.footbar-left {
  /* 自定义左侧信息样式 */
}

.footbar-right {
  /* 自定义右侧控制样式 */
}

.footbar-button:hover {
  /* 自定义按钮悬停样式 */
}
</style>
```

## 响应式设计

组件内置响应式支持：

- **桌面端**: 完整功能显示
- **平板端**: 调整间距和布局
- **手机端**: 折行显示，精简信息

```css
/* 移动端自动调整 */
@media (max-width: 768px) {
  .footbar-right {
    flex-wrap: wrap;
    gap: 4px;
  }
}
```

## 数据同步

### 与编辑器的集成

```typescript
// 监听编辑器状态变化
editor.listener.on('contentChange', (payload) => {
  // 更新页面信息
  pageInfo.value = {
    current: payload.currentPage,
    total: payload.totalPages,
    visiblePages: payload.visiblePages
  }

  // 更新字数统计
  wordCount.value = {
    total: payload.totalWords,
    selected: payload.selectedWords,
    lines: payload.totalLines,
    columns: payload.currentColumn
  }
})

// 监听缩放变化
editor.listener.on('scaleChange', (scale) => {
  scale.value = scale
})
```

### 状态管理

```typescript
const footbarState = reactive({
  pageInfo: { current: 1, total: 1, visiblePages: [1] },
  wordCount: { total: 0, selected: 0, lines: 0, columns: 0 },
  scale: 100,
  pageMode: 'paging',
  paperSize: 'A4',
  paperDirection: 'portrait',
  isFullscreen: false
})
```

## 工具函数

### 格式化函数

```typescript
import { formatVisiblePages, clampScale, getNearestScalePreset } from '@/components/footbar'

// 格式化可见页码
formatVisiblePages([1, 2, 3, 5]) // "1-3, 5"

// 限制缩放范围
clampScale(250) // 200 (最大值)

// 获取最近的预设值
getNearestScalePreset(95) // 100
```

### 配置常量

```typescript
import {
  SCALE_CONFIG,
  PAPER_SIZE_CONFIG,
  PAPER_DIRECTION_CONFIG
} from '@/components/footbar'

// 缩放配置
SCALE_CONFIG.MIN_SCALE // 50
SCALE_CONFIG.PRESETS   // [50, 75, 100, 125, 150, 200]

// 纸张大小选项
PAPER_SIZE_CONFIG.options // [{ value: 'A4', label: 'A4', ... }]
```

## 注意事项

1. **性能优化**: 避免频繁更新大量数据
2. **状态同步**: 确保与编辑器状态保持同步
3. **用户体验**: 提供适当的视觉反馈
4. **无障碍**: 添加适当的 ARIA 属性
5. **国际化**: 支持多语言标签

## 迁移指南

### 从 HTML 迁移

**原方式：**
```vue
<div class="footer">
  <div class="footer-left">
    <span>可见页码: {{ visiblePages }}</span>
    <span>字数: {{ wordCount }}</span>
  </div>
  <div class="footer-right">
    <button @click="zoomOut">−</button>
    <span>{{ scale }}%</span>
    <button @click="zoomIn">+</button>
  </div>
</div>
```

**新方式：**
```vue
<EMRFootbar
  :page-info="pageInfo"
  :word-count="wordCount"
  :scale="scale"
  @scale-change="handleScaleChange"
/>
```
