<template>
  <div class="mr-editor-page">
    <div class="page-header">
      <h2>EMR Editor 测试 (新组件)</h2>
      <div class="header-actions">
        <Button @click="switchToDoctorMode" type="primary">医生模式</Button>
        <Button @click="switchToNurseMode">护士模式</Button>
        <Button @click="switchToAdminMode">管理员模式</Button>
        <Button @click="switchTheme">切换主题</Button>
        <Button @click="testExecuteCommand">测试命令</Button>
        <Button @click="getEditorData">获取数据</Button>
      </div>
    </div>

    <div class="config-info">
      <p><strong>当前配置:</strong> {{ currentMode }}</p>
      <p><strong>当前主题:</strong> {{ currentTheme }}</p>
    </div>

    <div class="editor-content">
      <!-- 使用新的 EMREditor 组件 -->
      <EMREditor
        ref="editorRef"
        :data="editorData"
        :config="currentConfig"
        :theme="currentTheme"
        :height="600"
        @change="onEditorChange"
        @command="onCommand"
        @error="onError"
      />
    </div>

    <!-- <div class="data-preview">
      <h3>编辑器数据预览</h3>
      <pre>{{ editorDataJson }}</pre>
    </div> -->
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Button, message } from 'ant-design-vue'

// 导入新的 EMREditor 组件
import { EMREditor } from '@vben/emr-editor/EMREditor'

// 导入配置
import { DOCTOR_CONFIG, NURSE_CONFIG, ADMIN_CONFIG } from '@vben/emr-editor/config'

// 响应式数据
const editorRef = ref()
const currentMode = ref('医生模式')
const currentTheme = ref<'default' | 'medical' | 'compact' | 'document'>('medical' as 'default' | 'medical' | 'compact' | 'document')


// 编辑器数据 - 使用reactive保持响应式（标题使用 valueList 格式以便被 formatElementList 展开并生成 titleId）
const editorData = reactive({
  main: [
    // 一级标题（valueList 内置文本节点）
    { type: 'title', valueList: [{ value: 'EMR编辑器使用指南', size: 18, bold: true }], level: 1 },
    { value: '\n\n' },

    { type: 'title', value: 'EMR编辑器使用指南22', valueList: [{ value: 'EMR编辑器使用指南11', size: 18, bold: true }], level: 1 },
    { value: '\n\n' },

    // 二级标题
    { type: 'title', valueList: [{ value: '概述', size: 16, bold: true }], level: 2 },
    { value: '\n\n' },
    { value: '欢迎使用EMR编辑器 (新组件)', size: 16, bold: true },
    { value: '\n\n' },
    { value: '这是一个基于 Canvas Editor 的全新Vue组件封装，支持完整的医疗编辑功能。', size: 14 },
    { value: '\n\n' },

    // 二级标题
    { type: 'title', valueList: [{ value: '主要特性', size: 16, bold: true }], level: 2 },
    { value: '\n\n' },
    { value: '主要特性：', size: 14, bold: true },
    { value: '\n• 配置驱动的权限控制' },
    { value: '\n• 完整的医疗表单控件' },
    { value: '\n• 多种主题风格支持' },
    { value: '\n• Vue 3 Composition API' },
    { value: '\n\n' },

    // 三级标题
    { type: 'title', valueList: [{ value: '权限控制', size: 14, bold: true }], level: 3 },
    { value: '\n\n' },
    { value: '支持医生、护士、管理员三种角色权限控制。', size: 14 },
    { value: '\n\n' },

    // 三级标题
    { type: 'title', valueList: [{ value: '表单控件', size: 14, bold: true }], level: 3 },
    { value: '\n\n' },
    { value: '提供丰富的医疗专用表单控件，包括病历模板、检查项目等。', size: 14 },
    { value: '\n\n' },

    // 二级标题
    { type: 'title', valueList: [{ value: '使用说明', size: 14, bold: true }], level: 1 },
    { value: '\n\n' },
    { value: '当前显示所有功能，请使用上方按钮切换不同模式。', size: 12, color: '#666666' }
  ]
})

// 当前配置 - 使用computed根据模式动态切换，并启用全部功能
const currentConfig = computed(() => {
  let baseConfig

  switch (currentMode.value) {
    case '医生模式':
      baseConfig = { ...DOCTOR_CONFIG }
      break
    case '护士模式':
      baseConfig = { ...NURSE_CONFIG }
      break
    case '管理员模式':
      baseConfig = { ...ADMIN_CONFIG }
      break
    default:
      baseConfig = { ...DOCTOR_CONFIG }
  }

  // 强制启用工具栏和 footbar，并显示全部功能
  return {
    ...baseConfig,
    toolbar: {
      enabled: true,
      groups: {
        basic: true,      // 基础操作
        text: true,       // 文本格式
        controls: true,   // 医疗控件
        insert: true,     // 插入元素
        view: true,       // 视图控制
        structure: true,  // 文档结构
        advanced: true    // 高级功能
      },
      layout: 'horizontal' as const
    },
    footbar: {
      enabled: true,
      layout: 'horizontal' as const
    }
  }
})

// 计算属性：格式化的数据预览
const editorDataJson = computed(() => {
  try {
    return JSON.stringify(editorData, null, 2)
  } catch {
    return '数据格式化失败'
  }
})


// 事件处理函数
const onEditorChange = (data: any) => {
  console.log('编辑器数据变化:', data)
  // 更新本地数据
  Object.assign(editorData, data)
}

const onCommand = (command: string, params?: any) => {
  console.log('执行命令:', command, params)
  message.info(`执行命令: ${command}`)
}

const onError = (error: Error) => {
  console.error('编辑器错误:', error)
  message.error(`编辑器错误: ${error.message}`)
}

/* ===========================================================================
   Sticky offset provider
   ---------------------------------------------------------------------------
   为了让编辑器工具栏能够纯 CSS 吸顶（避免复杂 JS 计算），我们在框架层
   计算 header + tab wrapper 的实际底部偏移，并将其写入一个全局 CSS 变量
   `--app-top-offset`。EMRToolbar 会使用这个变量作为 `top` 偏移，从而稳健地
   吸附在 tab 栏下面。

   说明（注释请勿删除）：
   - 选择器 '_scroll__fixed_' 为当前页面包含 header + tab 的 wrapper 的 class，
     如果框架中该 class 可能更改，请替换为更稳定的选择器或在框架中添加
     一个语义化的 class（例如 `.app-header-wrapper`）。
   - 我们使用 ResizeObserver + window.resize 来在布局变化时刷新变量值，并
     在组件卸载时清理观察器。
  =========================================================================== */
let __layout_ro: ResizeObserver | null = null
const updateOffset = () => {
  try {
    // 首选使用框架提供的 wrapper class
    const wrapper = document.querySelector('._scroll__fixed_') as HTMLElement | null
    let offset = 0
    if (wrapper) {
      const rect = wrapper.getBoundingClientRect()
      offset = Math.round(rect.bottom)
    } else {
      // 回退：累计 header 与 tab 的高度（更通用但不如 wrapper 精确）
      const header = document.querySelector('header') as HTMLElement | null
      const tab = document.querySelector('section.flex.w-full.border-b') as HTMLElement | null
      const h = header ? Math.round(header.getBoundingClientRect().height) : 0
      const t = tab ? Math.round(tab.getBoundingClientRect().height) : 0
      offset = h + t
    }
    document.documentElement.style.setProperty('--app-top-offset', `${offset}px`)
    // also set left/width so toolbar can align with content when fixed
    if (wrapper) {
      document.documentElement.style.setProperty('--app-left-offset', `${Math.round(wrapper.getBoundingClientRect().left)}px`)
      document.documentElement.style.setProperty('--app-content-width', `${Math.round(wrapper.getBoundingClientRect().width)}px`)
    } else {
      document.documentElement.style.setProperty('--app-left-offset', `0px`)
      document.documentElement.style.setProperty('--app-content-width', `100%`)
    }
    // attempt to set editor container padding to avoid overlap (if present)
    const editorContainer = document.querySelector('.canvas-editor-container') as HTMLElement | null
    if (editorContainer) {
      editorContainer.style.paddingTop = `${offset}px`
    }
    // 开发模式下打印，便于调试（使用 Vite 的环境变量）
    if (typeof import.meta !== 'undefined' && (import.meta as any).env && (import.meta as any).env.DEV) {
      // eslint-disable-next-line no-console
      console.log('[Layout] set --app-top-offset =', offset)
    }
  } catch (e) {
    // ignore
  }
}

onMounted(async () => {
  await nextTick()
  updateOffset()
  try {
    __layout_ro = new ResizeObserver(updateOffset)
    __layout_ro.observe(document.documentElement)
    const wrapperNode = document.querySelector('._scroll__fixed_') as Element | null
    if (wrapperNode) __layout_ro.observe(wrapperNode)
  } catch (e) {
    // ignore if ResizeObserver not supported
  }
  window.addEventListener('resize', updateOffset, { passive: true })
})

onBeforeUnmount(() => {
  try { __layout_ro && __layout_ro.disconnect() } catch {}
  try { window.removeEventListener('resize', updateOffset) } catch {}
})

// 模式切换函数
const switchToDoctorMode = () => {
  currentMode.value = '医生模式'
  message.success('已切换到医生模式 - 显示全部功能')
}

const switchToNurseMode = () => {
  currentMode.value = '护士模式'
  message.success('已切换到护士模式 - 简化功能')
}

const switchToAdminMode = () => {
  currentMode.value = '管理员模式'
  message.success('已切换到管理员模式 - 只读模式')
}

// 主题切换
const switchTheme = () => {
  const themes: ('default' | 'medical' | 'compact' | 'document')[] = ['default', 'medical', 'compact', 'document']
  const currentIndex = themes.indexOf(currentTheme.value)
  const nextIndex = (currentIndex + 1) % themes.length
  currentTheme.value = themes[nextIndex] as 'default' | 'medical' | 'compact' | 'document'
  message.success(`已切换到 ${currentTheme.value} 主题`)
}

// 测试命令执行
const testExecuteCommand = () => {
  if (editorRef.value) {
    try {
      editorRef.value.executeCommand('bold')
    message.success('已执行加粗命令')
    } catch (error) {
      message.error('命令执行失败')
    }
  } else {
    message.warning('编辑器实例未就绪')
  }
}

// 获取编辑器数据
const getEditorData = () => {
  if (editorRef.value) {
    try {
      const data = editorRef.value.getData()
      console.log('获取到的编辑器数据:', data)
    message.success('数据已输出到控制台')
    } catch (error) {
      message.error('获取数据失败')
    }
  } else {
    message.warning('编辑器实例未就绪')
  }
}
</script>

<style scoped>
.mr-editor-page {
  padding: 20px;
  background: #f5f5f5;
  /* min-height: 100vh; */
  overflow: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.page-header h2 {
  margin: 0;
  color: #262626;
  font-size: 18px;
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.config-info {
  background: white;
  padding: 12px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
  border-left: 4px solid #1890ff;
}

.config-info p {
  margin: 4px 0;
  font-size: 14px;
  color: #666;
}

.config-info strong {
  color: #262626;
}

.editor-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  /* Allow scrolling when editor content grows (e.g. when zoomed to 300%) */
  overflow-x: auto;
  overflow-y: auto;
  margin-bottom: 20px;
}

/* EMREditor组件会自己处理容器样式 */

.data-preview {
  background: white;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.data-preview h3 {
  margin-top: 0;
  color: #262626;
  border-bottom: 2px solid #1890ff;
  padding-bottom: 8px;
}

.data-preview pre {
  background: #f6f8fa;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 12px;
  color: #24292f;
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #e5e7eb;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .mr-editor-page {
    padding: 10px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .header-actions {
    width: 100%;
    justify-content: center;
  }

  .config-info {
    font-size: 13px;
  }
}

/* EMR Editor 样式 */
.emr-editor {
  --emr-toolbar-height: 40px;
  --emr-toolbar-bg: #ffffff;
  --emr-toolbar-border: #e5e7eb;
  --emr-toolbar-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);

  --emr-button-size: 32px;
  --emr-button-radius: 4px;
  --emr-button-color: #374151;
  --emr-button-hover-bg: #f9fafb;
  --emr-button-active-bg: #f3f4f6;

  --emr-editor-bg: #ffffff;
  --emr-editor-border: #e5e7eb;

  --emr-menu-divider: #e5e7eb;
}

/* 医疗主题 */
.emr-theme-medical {
  --emr-toolbar-bg: #f8f9fa;
  --emr-toolbar-border: #bae7ff;
  --emr-button-color: #1890ff;
  --emr-button-hover-bg: #e6f7ff;
  --emr-button-active-bg: #bae7ff;
}

/* 紧凑主题 */
.emr-theme-compact {
  --emr-toolbar-height: 36px;
  --emr-button-size: 28px;
  --emr-button-radius: 3px;
}

/* 文档主题 */
.emr-theme-document {
  --emr-toolbar-bg: #fafbfc;
  --emr-toolbar-border: #d1d9e0;
  --emr-button-color: #24292f;
}

/* 工具栏样式 */
.emr-editor .canvas-toolbar {
  height: var(--emr-toolbar-height);
  background: var(--emr-toolbar-bg);
  border: 1px solid var(--emr-toolbar-border);
  border-bottom: none;
  box-shadow: var(--emr-toolbar-shadow);
  display: flex;
  align-items: center;
  padding: 0 16px;
  overflow-x: auto;
  overflow-y: hidden;
  font-size: 14px;
}

.emr-editor .menu-item {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.emr-editor .menu-item > div {
  width: var(--emr-button-size);
  height: var(--emr-button-size);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--emr-button-radius);
  cursor: pointer;
  color: var(--emr-button-color);
  transition: all 0.2s ease;
  user-select: none;
}

.emr-editor .menu-item > div:hover {
  background: var(--emr-button-hover-bg);
}

.emr-editor .menu-item > div:active,
.emr-editor .menu-item > div.active {
  background: var(--emr-button-active-bg);
}

.emr-editor .menu-item > div[disabled] {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

/* 下拉选择器样式 */
.emr-editor .select {
  position: relative;
  padding: 4px 8px;
  min-width: 80px;
  cursor: pointer;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: white;
}

.emr-editor .options {
  display: none;
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #d1d5db;
  border-top: none;
  border-radius: 0 0 4px 4px;
  z-index: 1000;
  max-height: 200px;
  overflow-y: auto;
}

.emr-editor .options ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.emr-editor .options li {
  padding: 6px 8px;
  cursor: pointer;
  border-bottom: 1px solid #f3f4f6;
}

.emr-editor .options li:hover {
  background: #f9fafb;
}

.emr-editor .options li:last-child {
  border-bottom: none;
}

/* 分隔符样式 */
.emr-editor .menu-divider {
  width: 1px;
  height: 24px;
  background: var(--emr-menu-divider);
  margin: 0 12px;
  flex-shrink: 0;
}

/* 编辑器容器样式 */
.emr-editor .canvas-editor-container {
  flex: 1;
  justify-items: center;
  border: 1px solid var(--emr-editor-border);
  border-top: none;
  background: var(--emr-editor-bg);
  height: auto;
}

/* 加载状态样式 */
.editor-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: #666;
  font-size: 14px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* end added sticky styles */
</style>
