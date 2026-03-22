# EMR Editor 工具栏组件

这是一套全新的 Vue 3 工具栏组件，用于替代原有的 HTML 字符串生成方式。

## 组件结构

```
toolbar/
├── EMRToolbar.vue          # 主工具栏组件
├── ToolbarGroup.vue        # 工具栏组容器
├── ToolbarItem.vue         # 单个工具栏项
├── ToolbarDropdown.vue     # 下拉菜单组件
├── ToolbarConfig.ts        # 配置和类型定义
└── index.ts               # 导出文件
```

## 基础用法

### 1. 完整工具栏

```vue
<template>
  <EMRToolbar
    :config="toolbarConfig"
    :toolbar-state="toolbarState"
    :disabled-commands="disabledCommands"
    @command="handleCommand"
  />
</template>

<script setup>
import { EMRToolbar } from '@/components/toolbar'

const toolbarConfig = {
  enabled: true,
  groups: {
    basic: true,
    text: true,
    controls: true,
    insert: true,
    view: true
  },
  layout: 'horizontal'
}

const toolbarState = {
  bold: { active: true },
  font: { value: 'Microsoft YaHei' },
  size: { value: 16 }
}

const disabledCommands = ['undo', 'redo']

const handleCommand = (command, value) => {
  console.log('Toolbar command:', command, value)
  // 处理工具栏命令
}
</script>
```

### 2. 自定义配置

```vue
<template>
  <EMRToolbar
    :config="customConfig"
    @command="handleCommand"
  />
</template>

<script setup>
const customConfig = {
  enabled: true,
  groups: {
    basic: true,      // 只显示基础操作
    text: false,      // 隐藏文本格式
    controls: true,   // 显示医疗控件
    insert: false,    // 隐藏插入功能
    view: true        // 显示视图控制
  }
}
</script>
```

### 3. 单个工具栏项

```vue
<template>
  <ToolbarItem
    type="button"
    command="bold"
    title="粗体"
    :active="isBold"
    :disabled="isDisabled"
    @click="handleClick"
  />
</template>

<script setup>
import { ToolbarItem } from '@/components/toolbar'
</script>
```

### 4. 工具栏组

```vue
<template>
  <ToolbarGroup title="基础操作" :show-divider="true">
    <ToolbarItem command="undo" title="撤销" />
    <ToolbarItem command="redo" title="重做" />
  </ToolbarGroup>
</template>

<script setup>
import { ToolbarGroup, ToolbarItem } from '@/components/toolbar'
</script>
```

## 配置选项

### ToolbarConfig

```typescript
interface EMRToolbarProps {
  config?: {
    enabled?: boolean           // 是否启用工具栏
    groups?: {                  // 启用的工具栏组
      basic?: boolean          // 基础操作
      text?: boolean           // 文本格式
      controls?: boolean       // 医疗控件
      insert?: boolean         // 插入元素
      view?: boolean           // 视图控制
    }
    layout?: 'horizontal' | 'vertical'  // 布局方式
  }
  toolbarState?: Record<string, any>    // 工具栏状态
  disabledCommands?: string[]           // 禁用的命令
}
```

### 工具栏状态

```typescript
const toolbarState = {
  // 按钮状态
  bold: { active: true },
  italic: { active: false },

  // 下拉选择状态
  font: { value: 'Microsoft YaHei' },
  size: { value: 16 },

  // 颜色状态
  color: { value: '#000000' }
}
```

## 命令列表

### 基础操作
- `undo` - 撤销
- `redo` - 重做
- `painter` - 格式刷
- `format` - 清除格式

### 文本格式
- `font` - 字体选择
- `size` - 字号选择
- `bold` - 粗体
- `italic` - 斜体
- `underline` - 下划线
- `color` - 字体颜色
- `highlight` - 背景色
- `align-left` - 左对齐
- `align-center` - 居中
- `align-right` - 右对齐

### 医疗控件
- `control-text` - 文本控件
- `control-select` - 选择控件
- `control-checkbox` - 复选框
- `control-radio` - 单选框
- `control-date` - 日期控件
- `control-number` - 数字控件

### 插入元素
- `table` - 插入表格
- `image` - 插入图片
- `link` - 插入链接
- `formula` - 数学公式
- `separator` - 分隔符

### 视图控制
- `zoom-out` - 缩小
- `zoom-in` - 放大
- `page-mode` - 分页模式

## 样式定制

组件使用 scoped CSS，可以通过以下方式定制样式：

```vue
<style scoped>
.emr-toolbar {
  /* 自定义工具栏样式 */
}

.toolbar-item {
  /* 自定义工具栏项样式 */
}

.toolbar-item.active {
  /* 自定义激活状态样式 */
}
</style>
```

## 迁移指南

### 从 HTML 字符串迁移

**原方式：**
```vue
<div v-html="toolbarHTML"></div>
```

**新方式：**
```vue
<EMRToolbar
  :config="config?.toolbar"
  :toolbar-state="toolbarState"
  @command="handleCommand"
/>
```

### 状态管理

需要添加状态管理：

```typescript
const toolbarState = reactive({
  bold: { active: false },
  italic: { active: false },
  font: { value: 'Microsoft YaHei' },
  size: { value: 16 }
})

// 监听编辑器状态变化，更新工具栏状态
editor.listener.on('contentChange', (payload) => {
  updateToolbarState(payload)
})
```

## 注意事项

1. **性能优化**: 大量工具栏项时考虑使用虚拟滚动
2. **响应式**: 移动端可能需要调整布局
3. **国际化**: 工具栏文本需要支持多语言
4. **键盘快捷键**: 需要与编辑器的快捷键系统集成
5. **无障碍**: 添加 ARIA 属性支持屏幕阅读器
