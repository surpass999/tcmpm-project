# @vben/emr-editor

基于 Canvas Editor 的 Vue 3 医疗编辑器组件

## 特性

- ✅ **原生工具栏支持**：使用 Canvas Editor 自带的完整工具栏
- ✅ **配置驱动**：通过 Props 灵活控制功能显示和权限
- ✅ **主题定制**：支持多种预设主题和自定义样式
- ✅ **医疗专用**：内置丰富的医疗表单控件
- ✅ **TypeScript 支持**：完整的类型定义
- ✅ **权限控制**：基于角色的功能权限管理

## 安装

```bash
pnpm add @vben/emr-editor
```

## 基础用法

```vue
<template>
  <EMREditor
    :data="editorData"
    :config="config"
    @change="handleChange"
  />
</template>

<script setup>
import EMREditor from '@vben/emr-editor'

const editorData = ref({
  main: [
    { value: '欢迎使用 EMR 编辑器' }
  ]
})

const config = {
  toolbar: {
    enabled: true,
    groups: {
      basic: true,
      controls: true
    }
  }
}

const handleChange = (data) => {
  console.log('编辑器数据变化:', data)
}
</script>
```

## 预设配置

### 医生端配置（完整功能）

```typescript
import { DOCTOR_CONFIG } from '@vben/emr-editor/config'

<EMREditor :config="DOCTOR_CONFIG" :theme="'medical'" />
```

包含功能：
- 基础操作（撤销、重做、格式刷、清除格式）
- 文本格式（字体、字号、颜色、样式、对齐）
- 文档结构（标题、列表、缩进）
- 插入元素（表格、图片、链接、公式）
- 医疗控件（文本、选择、复选、单选、日期、数字）
- 视图控制（缩放、页边距、分页模式）
- 高级功能（搜索、批注、水印、目录）

### 护士端配置（简化功能）

```typescript
import { NURSE_CONFIG } from '@vben/emr-editor/config'

<EMREditor :config="NURSE_CONFIG" :theme="'compact'" />
```

主要功能：
- 基础操作
- 医疗控件（完整）
- 打印功能

### 管理员配置（只读模式）

```typescript
import { ADMIN_CONFIG } from '@vben/emr-editor/config'

<EMREditor :config="ADMIN_CONFIG" :theme="'document'" />
```

- 不显示工具栏
- 只读模式

## 自定义配置

### 使用配置生成器

```typescript
import { createConfig } from '@vben/emr-editor/config'

const customConfig = createConfig({
  role: 'doctor',  // 基于医生配置
  customFeatures: {
    print: false,  // 禁用打印
    table: false   // 禁用表格插入
  },
  customGroups: {
    advanced: false  // 隐藏高级功能组
  }
})
```

### 手动配置

```typescript
const config = {
  toolbar: {
    enabled: true,
    groups: {
      basic: true,     // 基础操作
      text: false,     // 隐藏文本格式
      controls: true   // 显示医疗控件
    }
  },
  features: {
    undo: true,
    redo: true,
    controls: {
      text: true,
      select: true,
      checkbox: true,
      radio: false,    // 禁用单选框
      date: true,
      number: true
    }
  },
  options: {
    // Canvas Editor 的原生选项
    defaultFont: '微软雅黑',
    defaultSize: 14
  }
}
```

## Props API

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| data | `IEditorData` | - | 编辑器数据 |
| config | `EditorConfig` | - | 功能配置 |
| theme | `string` | `'default'` | 主题样式 |
| height | `number \| string` | `600` | 编辑器高度 |

## 事件

| 事件名 | 参数 | 说明 |
|--------|------|------|
| change | `(data: IEditorData)` | 内容变化 |
| command | `(command: string, params?: any)` | 命令执行 |
| ready | `(editor: Editor)` | 编辑器初始化完成 |
| error | `(error: Error)` | 错误发生 |

## 主题样式

### 预设主题

- `default` - 默认主题
- `medical` - 医疗主题（蓝色调）
- `compact` - 紧凑主题（小尺寸）
- `document` - 文档主题（灰色调）

### 自定义主题

```css
.emr-editor {
  --emr-toolbar-bg: #ffffff;
  --emr-toolbar-border: #e5e7eb;
  --emr-button-color: #374151;
  --emr-primary-color: #1890ff;
}
```

## 方法

```typescript
const editorRef = ref()

// 获取编辑器实例
const editor = editorRef.value?.getEditor()

// 执行命令
editorRef.value?.executeCommand('bold')

// 获取数据
const data = editorRef.value?.getData()
```

## 权限控制

### 基于角色的权限

```typescript
// 后端返回用户角色和权限
const userPermissions = {
  role: 'nurse',
  features: {
    controls: {
      text: true,
      select: true,
      date: true
    },
    print: true
  }
}

// 前端根据权限生成配置
const config = createConfig({
  role: userPermissions.role,
  customFeatures: userPermissions.features
})
```

### 动态权限更新

```typescript
// 运行时更新权限
const updatePermissions = (newPermissions) => {
  config.value = createConfig({
    role: 'custom',
    customFeatures: newPermissions
  })
}
```

## 浏览器兼容性

- ✅ Chrome 80+
- ✅ Firefox 75+
- ✅ Safari 13+
- ✅ Edge 80+
- ✅ 主流信创浏览器

## 注意事项

1. **DOM 操作**：组件内部会操作 DOM，请勿在外部修改工具栏结构
2. **事件顺序**：`ready` 事件触发后编辑器才完全可用
3. **权限控制**：所有权限判断都在前端，敏感操作请在后端验证
4. **性能优化**：大量数据时建议使用分页或虚拟滚动

## 开发调试

```typescript
// 开发环境下的调试信息
if (import.meta.env.DEV) {
  window.emrEditor = editorRef.value?.getEditor()
}
```

## 示例

查看 `examples/usage.vue` 获取完整示例代码。

## 许可证

MIT
