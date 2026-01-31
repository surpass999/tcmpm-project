<template>
  <div
    class="emr-editor"
    :class="containerClasses"
    :style="containerStyle"
  >
    <!-- 工具栏容器 -->
    <!-- 新版本：使用 Vue 组件（推荐） -->
    <EMRToolbar
      v-if="mergedConfig?.toolbar?.enabled"
      :config="mergedConfig?.toolbar"
      :toolbar-state="toolbarState"
      :disabled-commands="disabledCommands"
      @command="handleToolbarCommand"
    />
    <!-- TableDialog is managed by Toolbar; EMREditor executes insert commands -->

    <!-- 当前版本：HTML 字符串生成（已废弃） -->
    <!-- 工具栏已完全迁移到 Vue 组件 -->

    <!-- 编辑器容器 -->
    <div
      ref="editorContainer"
      class="canvas-editor-container"
   
    ></div>

    <!-- Footbar 容器 -->
    <div v-if="config?.footbar?.enabled" class="emr-footbar-container">
      <!-- 新版本：Vue 组件方式 -->
      <EMRFootbar
        :page-info="footbarState.pageInfo"
        :word-count="footbarState.wordCount"
        :visible-pages="footbarState.visiblePages"
        :scale="footbarState.scale"
        :page-mode="footbarState.pageMode"
        :paper-size="footbarState.paperSize"
        :paper-direction="footbarState.paperDirection"
        :is-fullscreen="footbarState.isFullscreen"
        :editor-mode="footbarState.editorMode"
        @scale-change="handleFootbarScaleChange"
        @scale-add="handleFootbarScaleAdd"
        @scale-minus="handleFootbarScaleMinus"
        @scale-recovery="handleFootbarScaleRecovery"
        @catalog-toggle="handleCatalogToggle"
        @page-mode-change="handleFootbarPageModeChange"
        @paper-size-change="handleFootbarPaperSizeChange"
        @paper-direction-change="handleFootbarPaperDirectionChange"
        @fullscreen-toggle="handleFootbarFullscreenToggle"
        @editor-option="handleFootbarEditorOption"
        @editor-mode-change="handleFootbarEditorModeChange"
      />

      <!-- 调试信息（暂时显示 footbar 状态） -->
      <div style="display: none">{{ footbarState.pageInfo.current }}</div>

      <!-- 当前版本：HTML 方式（已废弃） -->
      <!--
      <div class="footer">
        <div class="footer-left">
          <div class="page-info">
            <span class="page-no-list">可见页码: </span>
            <span class="page-no">-</span>
          </div>
          <div class="word-info">
            可见页码: <span class="page-no-list">1, 2</span> 页面: <span class="page-no">1/2</span> 字数: <span class="word-count">0</span> 行: <span class="row-no">0</span> 列: <span class="col-no">0</span>
          </div>
        </div>
        <div class="footer-right">
          <div class="page-mode" title="分页模式"><i></i></div>
          <div class="page-scale-minus" title="缩小"><i></i></div>
          <div class="page-scale-percentage" title="页面缩放"><span class="page-scale-percentage">100%</span></div>
          <div class="page-scale-add" title="放大"><i></i></div>
          <div class="page-size" title="纸张大小"><i></i></div>
          <div class="paper-direction" title="纸张方向"><i></i></div>
          <div class="paper-margin" title="页边距"><i></i></div>
          <div class="fullscreen" title="全屏"><i></i></div>
          <div class="editor-option" title="编辑器设置"><i></i></div>
        </div>
      </div>
      -->
    </div>
    <!-- 目录面板（与 canvas-editor 结构对齐） -->
    <div ref="catalogDom" class="catalog" editor-component="catalog" style="display:none;">
      <div class="catalog__header">
        <span>目录</span>
        <div class="catalog__header__close" @click="handleCatalogToggle">
          <i></i>
        </div>
      </div>
      <div class="catalog__main"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onBeforeUnmount, nextTick, toRaw } from 'vue'
import { TitleLevel } from '../editor/dataset/enum/Title'
import { RowFlex } from '../editor/dataset/enum/Row'
import { ListType, ListStyle } from '../editor/dataset/enum/List'
import { ElementType } from '../editor/dataset/enum/Element'
import { BlockType } from '../editor/dataset/enum/Block'
import { splitText } from '../editor/utils'
import Editor, {
  type IEditorData,
  type IEditorOption,
  type IBlock
} from '../editor/index'
import { SCALE_CONFIG } from './footbar/FootbarConfig'
import { ref as vueRef } from 'vue'

// 新版本工具栏组件（暂时注释）
// import { EMRToolbar } from './toolbar'

// 新版本工具栏和 footbar 组件
import { EMRToolbar } from './toolbar'
import { EMRFootbar } from './footbar'

// 示例：如何使用新的 Vue 组件（在需要时取消注释）
// <EMRToolbar
//   :config="{ enabled: true, groups: { basic: true, text: true } }"
//   @command="(cmd) => console.log('Toolbar command:', cmd)"
// />
//
// <EMRFootbar
//   :page-info="{ current: 1, total: 3, visiblePages: [1, 2] }"
//   :word-count="{ total: 1250, lines: 45, columns: 12 }"
//   :scale="100"
//   @scale-change="(scale) => console.log('Scale changed:', scale)"
// />

// 类型定义
interface ToolbarGroups {
  basic?: boolean
  text?: boolean
  structure?: boolean
  insert?: boolean
  controls?: boolean
  view?: boolean
  advanced?: boolean
}

interface EmrEditorProps {
  data?: IEditorData
  config?: {
    toolbar?: {
      enabled?: boolean
      groups?: ToolbarGroups
      layout?: 'horizontal' | 'vertical'
    }
    footbar?: {
      enabled?: boolean
      layout?: 'horizontal' | 'vertical'
    }
    features?: FeatureFlags
    options?: Partial<IEditorOption>
  }
  theme?: 'default' | 'medical' | 'compact' | 'document'
  height?: number | string
  onChange?: (data: IEditorData) => void
  onCommand?: (command: string, params?: any) => void
  onError?: (error: Error) => void
}


interface FeatureFlags {
  undo?: boolean
  redo?: boolean
  painter?: boolean
  format?: boolean
  font?: boolean
  size?: boolean
  color?: boolean
  highlight?: boolean
  bold?: boolean
  italic?: boolean
  underline?: boolean
  strikeout?: boolean
  superscript?: boolean
  subscript?: boolean
  align?: boolean
  title?: boolean
  list?: boolean
  indent?: boolean
  table?: boolean
  image?: boolean
  link?: boolean
  formula?: boolean
  separator?: boolean
  pageBreak?: boolean
  date?: boolean
  search?: boolean
  comment?: boolean
  print?: boolean
  controls?: {
    text?: boolean
    select?: boolean
    checkbox?: boolean
    radio?: boolean
    date?: boolean
    number?: boolean
  }
}

// Props定义
const props = withDefaults(defineProps<EmrEditorProps>(), {
  config: () => ({
    toolbar: {
      enabled: true,
      groups: {
        basic: true,
        text: true,
        controls: true,
        insert: true,
        view: true
      },
      layout: 'horizontal',
      /* sticky behavior removed */
    },
    footbar: {
      enabled: true,
      layout: 'horizontal'
    }
  }),
  theme: 'default',
  height: 600
})

// 事件定义
const emit = defineEmits<{
  change: [data: IEditorData]
  command: [command: string, params?: any]
  error: [error: Error]
}>()

// 响应式数据
const editorContainer = ref<HTMLDivElement>()
const editorInstance = ref<Editor | null>(null)
const isReady = ref(false)
const isInitializing = ref(false)

// No toolbar-local table dialog state here; toolbar manages UI and emits insert command.

// Toolbar 状态
const toolbarState = reactive<Record<string, any>>({
  // 按钮状态
  bold: { active: false },
  italic: { active: false },
  underline: { active: false },
  strikeout: { active: false },
  superscript: { active: false },
  subscript: { active: false },
  color: { value: '#000000' },
  highlight: { value: '#ffffff' },
  'align-left': { active: true },
  'align-center': { active: false },
  'align-right': { active: false },

  // 下拉选择状态
  font: { value: 'Microsoft YaHei' },
  size: { value: 16 },

  // 其他状态
  painter: { active: false },
  format: { active: false }
  ,
  // search result state
  searchResult: null
})

// Footbar 状态
const footbarState = reactive({
  pageInfo: {
    current: 1,
    total: 1,
    visiblePages: [1]
  },
  wordCount: {
    total: 0,
    selected: 0,
    lines: 0,
    columns: 0
  },
  visiblePages: [1],
  scale: 100,
  pageMode: 'paging' as 'paging' | 'continuity',
  paperSize: 'A4' as string,
  paperDirection: 'portrait' as 'portrait' | 'landscape',
  isFullscreen: false,
  // editor mode for center display: 'edit' | 'clean' | 'readonly' | 'form' | 'print' | 'design' | 'graffiti'
  editorMode: 'edit' as 'edit' | 'clean' | 'readonly' | 'form' | 'print' | 'design' | 'graffiti'
})

// 计算属性
const containerClasses = computed(() => [
  'emr-editor',
  `emr-theme-${props.theme}`,
  {
    'toolbar-enabled': props.config?.toolbar?.enabled,
    'ready': isReady.value
  }
])

// disabled commands derived from toolbarState
const disabledCommands = computed(() => {
  return Object.keys(toolbarState).filter(k => (toolbarState as any)[k]?.disabled)
})

const containerStyle = computed(() => ({
  width: '100%',
  position: 'relative' as const
}))

const editorHeight = computed(() => {
  if (typeof props.height === 'number') {
    return `${props.height}px`
  }
  return props.height || '600px'
})


// 默认配置
const DEFAULT_CONFIG = {
  toolbar: {
    enabled: true,
    groups: {
      basic: true,
      text: true,
      structure: true,
      insert: true,
      controls: true,
      view: true,
      advanced: true
    }
  },
  features: {
    undo: true,
    redo: true,
    painter: true,
    format: true,
    font: true,
    size: true,
    color: true,
    highlight: true,
    bold: true,
    italic: true,
    underline: true,
    strikeout: true,
    superscript: true,
    subscript: true,
    align: true,
    title: true,
    list: true,
    indent: true,
    table: true,
    image: true,
    link: true,
    formula: true,
    separator: true,
    pageBreak: true,
    date: true,
    search: true,
    comment: true,
    print: true,
    controls: {
      text: true,
      select: true,
      checkbox: true,
      radio: true,
      date: true,
      number: true
    }
  }
}

// 配置合并
const mergeConfig = (userConfig?: EmrEditorProps['config']) => {
  return deepMerge(DEFAULT_CONFIG, userConfig || {})
}

// 深度合并对象
const deepMerge = (target: any, source: any): any => {
  const result = { ...target }

  Object.keys(source).forEach(key => {
    if (source[key] && typeof source[key] === 'object' && !Array.isArray(source[key])) {
      result[key] = deepMerge(target[key] || {}, source[key])
    } else {
      result[key] = source[key]
    }
  })

  return result
}

// 获取默认数据
const getDefaultData = () => ({
  main: [
    { value: '欢迎使用EMR编辑器', size: 16, bold: true },
    { value: '\n\n' },
    { value: '这是一个基于Canvas Editor的医疗专用编辑器。' }
  ]
})

// 生成工具栏HTML

// 防抖函数
const debounce = (fn: Function, delay: number) => {
  let timeoutId: ReturnType<typeof setTimeout>
  return (...args: any[]) => {
    clearTimeout(timeoutId)
    timeoutId = setTimeout(() => fn.apply(null, args), delay)
  }
}

// 生命周期
onMounted(async () => {
  await initializeEditor()
})

onBeforeUnmount(() => {
  destroyEditor()
})

// 统一合并配置供模板与初始化使用
const mergedConfig = computed(() => mergeConfig(props.config))

// 初始化编辑器
const initializeEditor = async () => {
  if (isInitializing.value || !editorContainer.value) return

  isInitializing.value = true

  try {
    // 等待DOM更新完成
    await nextTick()

    // 合并配置（使用统一 mergedConfig）
    const finalConfig = mergedConfig.value

    // 创建编辑器实例
    editorInstance.value = new Editor(
      editorContainer.value,
      toRaw(props.data || getDefaultData()),
      finalConfig.options || {}
    )

    // 设置事件监听
    if (editorInstance.value) {
      setupEventListeners(editorInstance.value)
    }

    // 初始化完成后立即执行一次统计
    if (editorInstance.value) {
      await performInitialStatistics(editorInstance.value)
    }

    // 应用权限控制
    if (editorInstance.value) {
      applyPermissions(editorInstance.value, finalConfig)
    }

    isReady.value = true

  } catch (error) {
    console.error('编辑器初始化失败:', error)
    emit('error', error as Error)
  } finally {
    isInitializing.value = false
  }
}

// 执行初始化统计（字数等）
const performInitialStatistics = async (editor: any) => {
  try {
    // 执行字数统计
    if (editor.command && typeof editor.command.getWordCount === 'function') {
      const wc = await editor.command.getWordCount()
      footbarState.wordCount.total = wc || 0
    }

    // 初始化其他统计数据（如果canvas-editor提供的话）
    // 注意：行数、列数等统计通常在contentChange事件中提供
    // 这里可以设置一些合理的默认值或尝试从编辑器获取
  } catch (e) {
    console.warn('初始化统计失败:', e)
  }
}

// 销毁编辑器
const destroyEditor = () => {
  if (editorInstance.value) {
    editorInstance.value.destroy?.()
    editorInstance.value = null
  }
  isReady.value = false
}

// 设置事件监听
const setupEventListeners = (editor: any) => {
  // 内容变化事件（防抖处理）
  editor.listener.contentChange = debounce(async (data: any) => {
    emit('change', data)
    // 更新 footbar 状态（来自 listener payload）
    updateFootbarFromEditor(data)
    // 部分统计数据通过命令接口获取（确保兼容不同实现）
    try {
      if (editor.command && typeof editor.command.getWordCount === 'function') {
        const wc = await editor.command.getWordCount()
        footbarState.wordCount.total = wc || 0
      }
    } catch (e) {
      // ignore
    }
  }, 300)

  // 选择变化事件
  editor.listener.rangeStyleChange = debounce(async (data: any) => {
    // 更新 toolbar 状态
    updateToolbarFromEditor(data)
    // 更新 range text 以便 toolbar 的 link dialog 可以预填显示文本
    try {
      if (editor.command && typeof editor.command.getRangeText === 'function') {
        const rt = editor.command.getRangeText()
        toolbarState.rangeText = rt || ''
      }
      // also capture the current range object so we can restore it after toolbar interactions
      if (editor.command && typeof editor.command.getRange === 'function') {
        try {
          const rg = editor.command.getRange()
          toolbarState.range = rg || null
        } catch {}
      }
    } catch (e) {
      // ignore
    }
  }, 100)

  // 页面变化事件
  editor.listener.pageSizeChange = debounce((data: any) => {
    updateFootbarPageInfo(data)
  }, 100)

  // 缩放变化事件：遵循 canvas-editor 原始逻辑，直接使用 floor(percent)
  editor.listener.pageScaleChange = debounce((scale: number) => {
    try {
      // canvas-editor 使用 Math.floor(payload * 100) 来显示百分比
      footbarState.scale = Math.floor(scale * 100)
    } catch (e) {
      footbarState.scale = Math.round(scale * 100)
    }
  }, 100)
 
  // 可见页码列表变化（draw 层会以 0-based 索引数组回调）
  editor.listener.visiblePageNoListChange = debounce((payload: number[]) => {
    try {
      const pages = Array.isArray(payload) ? payload.map(i => i + 1) : []
      footbarState.visiblePages = pages
      footbarState.pageInfo.visiblePages = pages
    } catch (e) {}
  }, 100)

  // 当前可见页面索引变化（0-based）
  editor.listener.intersectionPageNoChange = debounce((payload: number) => {
    try {
      footbarState.pageInfo.current = typeof payload === 'number' ? payload + 1 : footbarState.pageInfo.current
    } catch (e) {}
  }, 100)
}

// 从编辑器更新 toolbar 状态
const updateToolbarFromEditor = (data: any) => {
  if (!data) return

  // 更新控件类型状态
  if (data.type !== undefined) {
    updateToolbarState('subscript', { active: data.type === ElementType.SUBSCRIPT })
    updateToolbarState('superscript', { active: data.type === ElementType.SUPERSCRIPT })
    updateToolbarState('separator', { active: data.type === ElementType.SEPARATOR, value: data.dashArray?.join(',') || '0,0' })
  }

  // 更新文本格式状态
  if (data.bold !== undefined) {
    updateToolbarState('bold', { active: data.bold })
  }
  if (data.italic !== undefined) {
    updateToolbarState('italic', { active: data.italic })
  }
  if (data.underline !== undefined) {
    updateToolbarState('underline', { active: data.underline })
  }
  if (data.strikeout !== undefined) {
    updateToolbarState('strikeout', { active: data.strikeout })
  }
  if (data.type !== undefined) {
    updateToolbarState('superscript', { active: data.type === ElementType.SUPERSCRIPT })
    updateToolbarState('subscript', { active: data.type === ElementType.SUBSCRIPT })
  }
  if (data.font) {
    updateToolbarState('font', { value: data.font })
  }
  if (data.size) {
    updateToolbarState('size', { value: data.size })
  }
  if (data.color !== undefined) {
    updateToolbarState('color', { value: data.color || '#000000' })
  }
  if (data.highlight !== undefined) {
    updateToolbarState('highlight', { value: data.highlight || '#ffffff' })
  }

  // 更新对齐状态 (使用rowFlex而不是align)
  if (data.rowFlex) {
    const alignmentMap: { [key: string]: string } = {
      'left': 'align-left',
      'center': 'align-center',
      'right': 'align-right',
      'alignment': 'alignment',
      'justify': 'justify'
    }
    const alignmentKey = alignmentMap[data.rowFlex] || 'align-left'
    updateAlignmentState(alignmentKey)
  } else {
    // 默认左对齐
    updateAlignmentState('align-left')
  }

  // 更新行间距状态
  if (data.rowMargin !== undefined) {
    updateToolbarState('row-margin', { value: data.rowMargin })
  }

  // 更新功能状态
  if (data.undo !== undefined) {
    updateToolbarState('undo', { disabled: !data.undo })
  }
  if (data.redo !== undefined) {
    updateToolbarState('redo', { disabled: !data.redo })
  }
  if (data.painter !== undefined) {
    updateToolbarState('painter', { active: data.painter })
  }

  // 更新标题级别状态
  if (data.level !== undefined) {
    updateToolbarState('title', { value: data.level || null })
  }

  // 更新列表状态
  if (data.listType !== undefined) {
    updateToolbarState('list', {
      active: !!data.listType,
      value: data.listType ? { type: data.listType, style: data.listStyle } : null
    })
  }
}

// 从编辑器更新 footbar 状态
const updateFootbarFromEditor = (data: any) => {
  if (!data) return

  // 更新字数统计
  if (data.wordCount !== undefined) {
    footbarState.wordCount.total = data.wordCount
  }
  if (data.selectedWordCount !== undefined) {
    footbarState.wordCount.selected = data.selectedWordCount
  }
  if (data.lineCount !== undefined) {
    footbarState.wordCount.lines = data.lineCount
  }
  if (data.columnCount !== undefined) {
    footbarState.wordCount.columns = data.columnCount
  }
}

// 更新 footbar 页面信息
const updateFootbarPageInfo = (data: any) => {
  if (!data) return

  if (data.currentPage !== undefined) {
    footbarState.pageInfo.current = data.currentPage
  }
  if (data.totalPages !== undefined) {
    footbarState.pageInfo.total = data.totalPages
  }
  if (data.visiblePages !== undefined) {
    footbarState.pageInfo.visiblePages = data.visiblePages
    footbarState.visiblePages = data.visiblePages
  }
}

// 应用权限控制
const applyPermissions = (_editor: any, config?: EmrEditorProps['config']) => {
  if (!config?.features) return

  // 权限控制通过CSS类和配置选项实现
  // 在初始化时通过HTML生成来控制显示
  const features = config.features

  // 可以通过editor选项来控制某些功能
  if (features.font === false) {
    // 可以通过CSS隐藏字体选择器
    hideToolbarElement('font')
  }

  if (features.size === false) {
    hideToolbarElement('size')
  }

  // 医疗控件权限控制
  if (features.controls) {
    Object.entries(features.controls).forEach(([controlType, enabled]) => {
      if (enabled === false) {
        hideControlElement(controlType)
      }
    })
  }
}

// 隐藏工具栏元素（通过添加CSS类）
const hideToolbarElement = (_featureName: string) => {
  // 暂时实现为通过HTML生成时控制显示
  // 后续可以通过动态样式或重新生成HTML来实现
}

// 隐藏控件元素
const hideControlElement = (_controlType: string) => {
  // 暂时实现为通过HTML生成时控制显示
  // 后续可以通过动态样式或重新生成HTML来实现
}


// 暴露方法给父组件
defineExpose({
  getEditor: () => editorInstance.value,
  executeCommand: (cmd: string, params?: any) => {
    const command = editorInstance.value?.command[cmd as keyof typeof editorInstance.value.command] as any
    command?.(params)
  },
  getData: () => editorInstance.value?.command.getValue?.()
})

// 更新工具栏状态
const updateToolbarState = (command: string, state: any) => {
  toolbarState[command] = { ...toolbarState[command], ...state }
}

// 更新对齐状态（互斥）
const updateAlignmentState = (activeCommand: string) => {
  const alignments = ['align-left', 'align-center', 'align-right', 'alignment', 'justify']
  alignments.forEach(cmd => {
    updateToolbarState(cmd, { active: cmd === activeCommand })
  })
}

// Toolbar 事件处理函数（新版本使用）
const handleToolbarCommand = (command: string, value?: any) => {
  console.log('Toolbar command:', command, value)

  // 执行编辑器命令
  if (editorInstance.value) {
    try {
      switch (command) {
        // 基础操作
        case 'undo':
          editorInstance.value.command.executeUndo()
          break
        case 'redo':
          editorInstance.value.command.executeRedo()
          break
        case 'painter':
          editorInstance.value.command.executePainter()
          break
        case 'format':
          editorInstance.value.command.executeFormat()
          break

        // 文本格式
        case 'bold':
          editorInstance.value.command.executeBold()
          updateToolbarState('bold', { active: !toolbarState.bold?.active })
          break
        case 'italic':
          editorInstance.value.command.executeItalic()
          updateToolbarState('italic', { active: !toolbarState.italic?.active })
          break
        case 'underline':
          editorInstance.value.command.executeUnderline()
          updateToolbarState('underline', { active: !toolbarState.underline?.active })
          break
        case 'font':
          if (value) {
            editorInstance.value.command.executeFont(value.toString())
            updateToolbarState('font', { value })
          }
          break
        case 'size':
          if (value) {
            editorInstance.value.command.executeSize(Number(value))
            updateToolbarState('size', { value })
          }
          break
        case 'size-add':
          editorInstance.value.command.executeSizeAdd()
          break
        case 'size-minus':
          editorInstance.value.command.executeSizeMinus()
          break
        case 'strikeout':
          editorInstance.value.command.executeStrikeout()
          updateToolbarState('strikeout', { active: !toolbarState.strikeout?.active })
          break
        case 'superscript':
          editorInstance.value.command.executeSuperscript()
          updateToolbarState('superscript', { active: !toolbarState.superscript?.active })
          break
        case 'subscript':
          editorInstance.value.command.executeSubscript()
          updateToolbarState('subscript', { active: !toolbarState.subscript?.active })
          break
        case 'color':
          if (value && typeof value === 'string') {
            try {
              editorInstance.value.command.executeColor(value)
              updateToolbarState('color', { value })
            } catch (error) {
              console.error('Execute color failed:', error)
              emit('error', error as Error)
            }
          }
          break
        case 'highlight':
          if (value && typeof value === 'string') {
            try {
              editorInstance.value.command.executeHighlight(value)
              updateToolbarState('highlight', { value })
            } catch (error) {
              console.error('Execute highlight failed:', error)
              emit('error', error as Error)
            }
          }
          break
        case 'align-left':
          try {
            editorInstance.value?.command.executeRowFlex(RowFlex.LEFT)
            updateAlignmentState('align-left')
          } catch (error) {
            console.error('Execute align-left failed:', error)
            emit('error', error as Error)
          }
          break
        case 'align-center':
          try {
            editorInstance.value?.command.executeRowFlex(RowFlex.CENTER)
            updateAlignmentState('align-center')
          } catch (error) {
            console.error('Execute align-center failed:', error)
            emit('error', error as Error)
          }
          break
        case 'align-right':
          try {
            editorInstance.value?.command.executeRowFlex(RowFlex.RIGHT)
            updateAlignmentState('align-right')
          } catch (error) {
            console.error('Execute align-right failed:', error)
            emit('error', error as Error)
          }
          break
        case 'alignment':
          try {
            editorInstance.value?.command.executeRowFlex(RowFlex.ALIGNMENT)
            updateAlignmentState('alignment')
          } catch (error) {
            console.error('Execute alignment failed:', error)
            emit('error', error as Error)
          }
          break

        // 插入元素
        case 'table':
          // toolbar sends table insert params {rows, cols}; EMREditor executes insertion
          if (value && typeof value === 'object' && 'rows' in (value as any) && 'cols' in (value as any)) {
            try {
              const rows = Number((value as any).rows)
              const cols = Number((value as any).cols)
              editorInstance.value?.command.executeInsertTable(rows, cols)
            } catch (error) {
              console.error('Insert table failed:', error)
              emit('error', error as Error)
            }
          }
          break
        case 'separator':
          // value may be a string like '1,1' or an array of numbers
          if (value) {
            let payload: number[] = []
            if (Array.isArray(value)) {
              payload = value.map(v => Number(v))
            } else if (typeof value === 'string') {
              payload = value.split(',').map(s => Number(s))
            }
            editorInstance.value.command.executeSeparator(payload)
          } else {
            editorInstance.value.command.executeSeparator()
          }
          break
        case 'title':
          // value: 'normal' | 'first' | 'second' | ...
          try {
            if (!value || value === 'normal') {
              editorInstance.value.command.executeTitle(null)
              updateToolbarState('title', { value: 'normal' })
            } else if (typeof value === 'string') {
              const map: Record<string, TitleLevel> = {
                first: TitleLevel.FIRST,
                second: TitleLevel.SECOND,
                third: TitleLevel.THIRD,
                fourth: TitleLevel.FOURTH,
                fifth: TitleLevel.FIFTH,
                sixth: TitleLevel.SIXTH
              }
              const level = map[value]
              editorInstance.value.command.executeTitle(level)
              updateToolbarState('title', { value })
            }
          } catch (error) {
            console.error('Execute title failed:', error)
            emit('error', error as Error)
          }
          break
        case 'justify':
          try {
            editorInstance.value?.command.executeRowFlex(RowFlex.JUSTIFY)
            updateAlignmentState('justify')
          } catch (error) {
            console.error('Execute justify failed:', error)
            emit('error', error as Error)
          }
          break
        case 'row-margin':
          try {
            if (value != null) {
              const v = Number(value)
              editorInstance.value?.command.executeRowMargin(v)
              updateToolbarState('row-margin', { value: v })
            }
          } catch (error) {
            console.error('Execute row-margin failed:', error)
            emit('error', error as Error)
          }
          break
        case 'list':
          try {
            if (!value || value === 'none') {
              editorInstance.value?.command.executeList(null)
              updateToolbarState('list', { value: 'none' })
            } else if (typeof value === 'string') {
              // map value strings to ListType/ListStyle
              if (value === 'ol') {
                editorInstance.value?.command.executeList(ListType.OL)
                updateToolbarState('list', { value })
              } else if (value.startsWith('ul')) {
                const style = value.split('-')[1] || 'disc'
                let listStyle: ListStyle | undefined
                switch (style) {
                  case 'checkbox':
                    listStyle = ListStyle.CHECKBOX
                    break
                  case 'disc':
                    listStyle = ListStyle.DISC
                    break
                  case 'circle':
                    listStyle = ListStyle.CIRCLE
                    break
                  case 'square':
                    listStyle = ListStyle.SQUARE
                    break
                }
                editorInstance.value?.command.executeList(ListType.UL, listStyle)
                updateToolbarState('list', { value })
              }
            }
          } catch (error) {
            console.error('Execute list failed:', error)
            emit('error', error as Error)
          }
          break
        case 'block':
          try {
            // value may be an object from toolbar dialog: { type, width, height, src, srcdoc }
            if (value && typeof value === 'object') {
              const typeStr = (value as any).type
              const width = (value as any).width
              const height = (value as any).height
              const src = (value as any).src
              const srcdoc = (value as any).srcdoc
              if (!height) return
              const block: any = { type: typeStr === 'video' ? BlockType.VIDEO : BlockType.IFRAME }
              if (block.type === BlockType.IFRAME) {
                if (!src && !srcdoc) return
                block.iframeBlock = { src, srcdoc }
              } else if (block.type === BlockType.VIDEO) {
                if (!src) return
                block.videoBlock = { src }
              }
              const blockElement: any = {
                type: ElementType.BLOCK,
                value: '',
                height: Number(height),
                block
              }
              if (width) {
                blockElement.width = Number(width)
              }
              editorInstance.value?.command.executeInsertElementList([blockElement])
            } else {
              // fallback: insert placeholder iframe block
              const blockElement: any = {
                type: ElementType.BLOCK,
                value: '',
                block: { type: BlockType.IFRAME },
                width: 400,
                height: 200
              }
              editorInstance.value?.command.executeInsertElementList([blockElement])
            }
          } catch (error) {
            console.error('Insert block failed:', error)
            emit('error', error as Error)
          }
          break
        case 'page-break':
          try {
            editorInstance.value?.command.executePageBreak()
          } catch (error) {
            console.error('Insert page-break failed:', error)
            emit('error', error as Error)
          }
          break
        case 'control-checkbox':
        case 'control-radio':
          try {
            if (value && typeof value === 'object') {
              // Distinguish raw element insertion vs. configured control insertion.
              // If payload is an actual element (has checkbox/radio property), treat as raw element.
              const isRawElement =
                (value.type === ElementType.CHECKBOX && (value.checkbox !== undefined || value.checkbox === undefined && value.value !== undefined)) ||
                (value.type === ElementType.RADIO && (value.radio !== undefined || value.radio === undefined && value.value !== undefined))
              if (isRawElement) {
                // payload is already an element, insert directly
                editorInstance.value?.command.executeInsertElementList([value])
              } else {
                // Configured control insertion (from dialog) -> normalize into CONTROL element
                try {
                  editorInstance.value?.command.executeFocus && editorInstance.value?.command.executeFocus()
                } catch {}
                const controlType = value.type || (command === 'control-checkbox' ? 'checkbox' : 'radio')
                const controlObj: any = { type: controlType }
                if (value.code) controlObj.code = value.code
                if (value.valueSets) controlObj.valueSets = value.valueSets
                if (value.value) controlObj.value = value.value ? [{ value: value.value }] : null
                if (value.placeholder) controlObj.placeholder = value.placeholder
                const element = {
                  type: ElementType.CONTROL,
                  value: '',
                  control: controlObj
                }
                try { console.log('[EMREditor] insertControl element (checkbox/radio):', element) } catch {}
                editorInstance.value?.command.executeInsertControl(element)
              }
            } else {
              // Quick insert (no dialog): insert a default checkbox/radio element
              const isCheckbox = command === 'control-checkbox'
              const el: any = {
                type: isCheckbox ? ElementType.CHECKBOX : ElementType.RADIO,
                checkbox: { value: false },
                value: ''
              }
              editorInstance.value?.command.executeInsertElementList([el])
            }
          } catch (error) {
            console.error('Insert checkbox/radio failed:', error)
            emit('error', error as Error)
          }
          break
        // // debug fallback for control commands
        // default:
        //   try { console.log('[EMREditor] unhandled toolbar command:', command, value) } catch {}
        //   break
        case 'control-text':
        case 'control-select':
        case 'control-date':
        case 'control-number':
          try {
            if (value && typeof value === 'object') {
              // Normalize dialog payload into element structure expected by executeInsertControl
              const controlType = value.type || (command === 'control-text' ? 'text' : command === 'control-select' ? 'select' : command === 'control-date' ? 'date' : 'number')
              const controlObj: any = { type: controlType }
              // common fields
              if (value.placeholder) controlObj.placeholder = value.placeholder
              if (value.font) controlObj.font = value.font
              if (value.size) controlObj.size = value.size
              if (value.code) controlObj.code = value.code
              // value / default
              if (value.value) {
                controlObj.value = [{ value: value.value }]
              } else {
                controlObj.value = null
              }
              // select/checkbox/radio valueSets (already parsed in dialog)
              if (value.valueSets) controlObj.valueSets = value.valueSets
              if (value.dateFormat) controlObj.dateFormat = value.dateFormat

              // ensure editor focused and restore range if available
              try {
                if (toolbarState.range && editorInstance.value?.command?.executeFocus) {
                  try {
                    editorInstance.value.command.executeFocus({ range: toolbarState.range })
                  } catch {
                    editorInstance.value.command.executeFocus && editorInstance.value.command.executeFocus()
                  }
                } else {
                  editorInstance.value?.command.executeFocus && editorInstance.value.command.executeFocus()
                }
              } catch {}

              const element = {
                type: ElementType.CONTROL,
                value: '',
                control: controlObj
              }
              try { console.log('[EMREditor] insertControl element:', element) } catch {}
              editorInstance.value?.command.executeInsertControl(element)
            }
          } catch (error) {
            console.error('Insert control failed:', error)
            emit('error', error as Error)
          }
          break
        case 'link':
          try {
            if (value && typeof value === 'object' && 'url' in value) {
              const name = value.name || ''
              const url = value.url
              // ensure editor focused
              try {
            // Try to restore the selection/range saved earlier before opening link dialog
            if (toolbarState.range && editorInstance.value?.command?.executeFocus) {
              try {
                editorInstance.value.command.executeFocus({ range: toolbarState.range })
              } catch {
                editorInstance.value.command.executeFocus && editorInstance.value.command.executeFocus()
              }
            } else {
              editorInstance.value?.command.executeFocus && editorInstance.value.command.executeFocus()
            }
              } catch {}
              const valueList = splitText(String(name || url)).map(n => ({
                value: n,
                size: 16
              }))
              console.log('ExecuteHyperlink payload:', { url, valueList })
              const res = editorInstance.value?.command.executeHyperlink({
                url,
                valueList
              })
              console.log('executeHyperlink returned:', res)
            }
          } catch (error) {
            console.error('Insert link failed:', error)
            emit('error', error as Error)
          }
          break
        case 'link-prepare':
          try {
            if (editorInstance.value && editorInstance.value.command) {
              try {
                toolbarState.rangeText = editorInstance.value.command.getRangeText() || ''
              } catch {}
              try {
                const rg = editorInstance.value.command.getRange()
                toolbarState.range = rg || null
              } catch {}
            }
          } catch {}
          break
        case 'control-prepare':
          try {
            if (editorInstance.value && editorInstance.value.command) {
              try {
                toolbarState.rangeText = editorInstance.value.command.getRangeText() || ''
              } catch {}
              try {
                const rg = editorInstance.value.command.getRange()
                toolbarState.range = rg || null
              } catch {}
              // prepare an initial payload for dialog: if selection is a control element, prefill its control
              try {
                const sel = editorInstance.value.command.getRange()
                // best-effort: leave null; toolbar will use rangeText for placeholder
                toolbarState.controlInitialPayload = null
              } catch {}
            }
          } catch {}
          break
        case 'image':
          try {
            if (!value) {
              console.log('No image data provided')
              break
            }
            // value may be a data URL string or a File
            const handleDataUrl = (dataUrl: string) => {
              // load image to get natural width/height, then call executeImage with object
              const img = new Image()
              img.onload = () => {
                const payload: any = {
                  value: dataUrl,
                  width: img.naturalWidth,
                  height: img.naturalHeight
                }
                try {
                  editorInstance.value?.command.executeImage(payload)
                } catch (err) {
                  console.error('executeImage failed, fallback to insertElementList', err)
                  try {
                    editorInstance.value?.command.executeInsertElementList([{ value: dataUrl }])
                  } catch (e) {
                    console.error('fallback insert failed', e)
                  }
                }
              }
              img.onerror = () => {
                // if image fails to load, still try with dataUrl
                try {
                  editorInstance.value?.command.executeImage({ value: dataUrl })
                } catch (err) {
                  console.error('executeImage fallback failed:', err)
                }
              }
              img.src = dataUrl
            }

            // ensure editor has focus/active range before inserting
            try {
              editorInstance.value?.command.executeFocus && editorInstance.value?.command.executeFocus()
            } catch (e) {
              // ignore
            }

            if (typeof value === 'string') {
              handleDataUrl(value)
            } else if (value instanceof File) {
              const reader = new FileReader()
              reader.onload = () => {
                const data = reader.result as string
                handleDataUrl(data)
              }
              reader.readAsDataURL(value)
            } else {
              // unknown type, try to stringify and handle
              handleDataUrl(String(value))
            }
          } catch (error) {
            console.error('Insert image failed:', error)
            emit('error', error as Error)
          }
          break
        case 'date':
          if (typeof value === 'string') {
            const fmt = value
            const now = new Date()
            const pad = (n: number) => String(n).padStart(2, '0')
            const formatted = fmt
              .replace('yyyy', String(now.getFullYear()))
              .replace('MM', pad(now.getMonth() + 1))
              .replace('dd', pad(now.getDate()))
              .replace('hh', pad(now.getHours()))
              .replace('mm', pad(now.getMinutes()))
              .replace('ss', pad(now.getSeconds()))
            // insert as DATE element so clicking it later opens the date picker
            try {
              editorInstance.value.command.executeInsertElementList([
                {
                  type: ElementType.DATE,
                  value: '',
                  dateFormat: fmt,
                  valueList: [
                    {
                      value: formatted
                    }
                  ]
                }
              ])
            } catch (err) {
              console.error('Insert date failed:', err)
            }
          }
          break

        // 视图控制
        case 'zoom-out':
          try { editorInstance.value?.command.executePageScaleMinus && editorInstance.value.command.executePageScaleMinus() } catch {}
          break
        case 'zoom-in':
          try { editorInstance.value?.command.executePageScaleAdd && editorInstance.value.command.executePageScaleAdd() } catch {}
          break
        case 'page-mode':
          handleFootbarPageModeChange(footbarState.pageMode === 'paging' ? 'continuity' : 'paging')
          break

        // 搜索功能
        case 'search':
          if (value && typeof value === 'object' && 'keyword' in value) {
            try {
              const { keyword, options } = value
              editorInstance.value.command.executeSearch(keyword, options)
              // update toolbar search result state
              try {
                const res = editorInstance.value.command.getSearchNavigateInfo && editorInstance.value.command.getSearchNavigateInfo()
                if (res) {
                  updateToolbarState('searchResult', res)
                } else {
                  toolbarState.searchResult = null
                }
              } catch {}
            } catch (error) {
              console.error('Search failed:', error)
              emit('error', error as Error)
            }
          }
          break
        case 'search-prev':
          try {
            editorInstance.value.command.executeSearchNavigatePre()
            try {
              const resPrev = editorInstance.value.command.getSearchNavigateInfo && editorInstance.value.command.getSearchNavigateInfo()
              if (resPrev) {
                updateToolbarState('searchResult', resPrev)
              } else {
                toolbarState.searchResult = null
              }
            } catch {}
          } catch (error) {
            console.error('Search navigate prev failed:', error)
            emit('error', error as Error)
          }
          break
        case 'search-next':
          try {
            editorInstance.value.command.executeSearchNavigateNext()
            try {
              const resNext = editorInstance.value.command.getSearchNavigateInfo && editorInstance.value.command.getSearchNavigateInfo()
              if (resNext) {
                updateToolbarState('searchResult', resNext)
              } else {
                toolbarState.searchResult = null
              }
            } catch {}
          } catch (error) {
            console.error('Search navigate next failed:', error)
            emit('error', error as Error)
          }
          break
        case 'search-replace':
          if (value && typeof value === 'object' && value.keyword) {
            try {
              const { keyword, replaceText, options } = value
              // 先执行搜索找到位置，然后替换
              editorInstance.value.command.executeSearch(keyword, options)
              // 执行替换
              editorInstance.value.command.executeReplace(replaceText)
            } catch (error) {
              console.error('Search and replace failed:', error)
              emit('error', error as Error)
            }
          }
          break
        case 'search-replace-all':
          if (value && typeof value === 'object' && value.keyword) {
            try {
              const { keyword, replaceText, options } = value
              // 执行全部替换
              editorInstance.value.command.executeReplace(replaceText, {
                isReplaceAll: true,
                searchKeyword: keyword,
                searchOptions: options
              })
            } catch (error) {
              console.error('Search and replace all failed:', error)
              emit('error', error as Error)
            }
          }
          break

        // 打印功能
        case 'print':
          try {
            editorInstance.value.command.executePrint()
          } catch (error) {
            console.error('Print failed:', error)
            emit('error', error as Error)
          }
          break

        // 代码块插入
        case 'codeblock':
          if (value && typeof value === 'object' && value.code) {
            try {
              const { language, code } = value
              // Insert codeblock as plain text with language info
              const codeLines = code.split('\n').map((line: string) => ({
                value: line || '\n',
                size: 14,
                font: 'monospace'
              }))
              // Add language indicator at the beginning
              codeLines.unshift({
                value: `${language}\n`,
                size: 12,
                color: '#666666',
                bold: true
              })
              editorInstance.value.command.executeInsertElementList(codeLines)
            } catch (error) {
              console.error('Insert codeblock failed:', error)
              emit('error', error as Error)
            }
          }
          break

        // 水印功能
        case 'watermark-add':
          if (value && typeof value === 'object') {
            try {
              // normalize payload to IWatermark expected by CommandAdapt.addWatermark
              const payload = {
                data: (value.text as string) || (value.data as string) || '',
                type: (value.type as any) || undefined,
                width: value.width,
                height: value.height,
                color: value.color,
                opacity: value.opacity,
                size: value.size,
                font: value.font,
                repeat: value.repeat,
                gap: value.gap,
                numberType: value.numberType
              }
              // debug log to help verify watermark payload and rendering
              try { console.log('[EMREditor] addWatermark payload:', payload) } catch {}
              editorInstance.value.command.executeAddWatermark(payload)
            } catch (error) {
              console.error('Add watermark failed:', error)
              emit('error', error as Error)
            }
          }
          break
        case 'watermark-delete':
          try {
            editorInstance.value.command.executeDeleteWatermark()
          } catch (error) {
            console.error('Delete watermark failed:', error)
            emit('error', error as Error)
          }
          break

        // LaTeX公式插入
        case 'latex':
        case 'formula':
          if (value && typeof value === 'object' && value.latex) {
            try {
              const { latex, size } = value
              // Insert LaTeX as an image element that will be rendered by the LaTeX particle
              editorInstance.value.command.executeInsertElementList([{
                type: ElementType.LATEX,
                value: latex,
                size: size || 16
              }])
            } catch (error) {
              console.error('Insert LaTeX failed:', error)
              emit('error', error as Error)
            }
          }
          break

        default:
          console.warn('Unknown toolbar command:', command)
      }
    } catch (error) {
      console.error('Toolbar command execution failed:', error)
      emit('error', error as Error)
    }
  }
}

// Footbar 事件处理函数（新版本使用）
const handleFootbarScaleChange = (scale: number) => {
  footbarState.scale = scale
  if (editorInstance.value) {
    try {
      // 通知编辑器实例缩放
      editorInstance.value.command.executePageScale(scale / 100)
      console.log('Footbar scale change:', scale)
    } catch (error) {
      console.error('Scale change failed:', error)
      emit('error', error as Error)
    }
  }
}

const handleFootbarPageModeChange = (mode: string) => {
  footbarState.pageMode = mode as 'paging' | 'continuity'
  if (editorInstance.value) {
    try {
      // 通知编辑器实例切换分页模式
      editorInstance.value.command.executePageMode(mode)
      console.log('Footbar page mode change:', mode)
    } catch (error) {
      console.error('Page mode change failed:', error)
      emit('error', error as Error)
    }
  }
}

const handleFootbarPaperSizeChange = (size: string) => {
  // size may be either a name like 'A4' or a dimension string '794*1123'
  footbarState.paperSize = size
  if (editorInstance.value) {
    try {
      let width: number | null = null
      let height: number | null = null
      if (typeof size === 'string' && size.includes('*')) {
        const parts = size.split('*').map(s => Number(s))
        if (parts.length === 2) {
          const w = parts[0]
          const h = parts[1]
          if (!Number.isNaN(w) && !Number.isNaN(h)) {
            width = w as number
            height = h as number
          }
        }
      } else {
        // map common paper names to default pixel dimensions (matches canvas-editor defaults)
        const map: Record<string, [number, number]> = {
          A4: [794, 1123],
          A3: [1125, 1593],
          A2: [1593, 2251],
          A5: [565, 796],
          '5号信封': [412, 488],
          '6号信封': [450, 866],
          '7号信封': [609, 862],
          '9号信封': [862, 1221],
          '法律用纸': [813, 1266],
          '信纸': [813, 1054],
          Letter: [216, 279], // fallback (mm-like) but prefer defaults above
          Legal: [813, 1266]
        }
        const found = map[size]
        if (found) {
          width = found[0]
          height = found[1]
        }
      }

      if (width != null && height != null) {
        editorInstance.value.command.executePaperSize(width, height)
        console.log('Footbar paper size change:', width, height)
      } else {
        console.warn('Unsupported paper size payload:', size)
      }
    } catch (error) {
      console.error('Paper size change failed:', error)
      emit('error', error as Error)
    }
  }
}

const handleFootbarPaperDirectionChange = (direction: string) => {
  // convert 'portrait'|'landscape' to editor's PaperDirection 'vertical'|'horizontal'
  const pd = direction === 'portrait' ? 'vertical' : 'horizontal'
  footbarState.paperDirection = direction as 'portrait' | 'landscape'
  if (editorInstance.value) {
    try {
      editorInstance.value.command.executePaperDirection(pd)
      console.log('Footbar paper direction change:', pd)
    } catch (error) {
      console.error('Paper direction change failed:', error)
      emit('error', error as Error)
    }
  }
}

const handleFootbarFullscreenToggle = () => {
  footbarState.isFullscreen = !footbarState.isFullscreen
  // TODO: 实现全屏模式切换
  console.log('Footbar fullscreen toggle:', footbarState.isFullscreen)

  // 这里可以添加全屏切换逻辑
  if (footbarState.isFullscreen) {
    // 进入全屏
    const element = editorContainer.value?.parentElement
    if (element && element.requestFullscreen) {
      element.requestFullscreen()
    }
  } else {
    // 退出全屏
    if (document.exitFullscreen) {
      document.exitFullscreen()
    }
  }
}

const handleFootbarEditorOption = () => {
  // TODO: 打开编辑器设置面板
  console.log('Footbar editor option clicked')

  // 这里可以打开设置对话框
  // 例如：打开纸张设置、页边距设置等
}

// 处理编辑模式切换（来自 footbar 中央的模式循环）
const handleFootbarEditorModeChange = (mode: string) => {
  footbarState.editorMode = mode as any
  if (editorInstance.value) {
    try {
      // 调用编辑器命令切换模式
      editorInstance.value.command.executeMode && editorInstance.value.command.executeMode(mode)
      console.log('Footbar editor mode change:', mode)
      // 根据只读模式等可更新菜单状态（与 canvas-editor 行为一致）
      const isReadonly = mode === 'readonly'
      // 禁用某些菜单项（通过 toolbarState），示例：禁用除 search/print 外的菜单
      const enableMenuList = ['search', 'print']
      Object.keys(toolbarState).forEach(key => {
        // 这里简单映射：若为菜单控制项则按 isReadonly 处理（具体映射可扩展）
        if (isReadonly && !enableMenuList.includes(key)) {
          updateToolbarState(key, { disabled: true })
        } else {
          updateToolbarState(key, { disabled: false })
        }
      })
    } catch (error) {
      console.error('Editor mode change failed:', error)
      emit('error', error as Error)
    }
  }
}

// 处理来自 footbar 的放大/缩小/恢复命令（直接调用 editor 的对应命令）
const handleFootbarScaleAdd = () => {
  try { editorInstance.value?.command.executePageScaleAdd && editorInstance.value.command.executePageScaleAdd() } catch {}
}

const handleFootbarScaleMinus = () => {
  try { editorInstance.value?.command.executePageScaleMinus && editorInstance.value.command.executePageScaleMinus() } catch {}
}

const handleFootbarScaleRecovery = () => {
  try { editorInstance.value?.command.executePageScaleRecovery && editorInstance.value.command.executePageScaleRecovery() } catch {}
}

// 目录面板控制
const catalogDom = vueRef<HTMLElement | null>(null)
const catalogVisible = vueRef(false)

const clearCatalog = () => {
  if (!catalogDom.value) return
  const main = catalogDom.value.querySelector('.catalog__main') as HTMLElement | null
  if (main) main.innerHTML = ''
}

const appendCatalog = (parent: HTMLElement, items: any[]) => {
  for (let c = 0; c < items.length; c++) {
    const catalogItem = items[c]
    const catalogItemDom = document.createElement('div')
    catalogItemDom.classList.add('catalog-item')
    const catalogItemContentDom = document.createElement('div')
    catalogItemContentDom.classList.add('catalog-item__content')
    const catalogItemContentSpanDom = document.createElement('span')
    catalogItemContentSpanDom.innerText = catalogItem.name || catalogItem.title || ''
    catalogItemContentDom.append(catalogItemContentSpanDom)
    catalogItemContentDom.onclick = () => {
      try {
        editorInstance.value?.command.executeLocationCatalog &&
          editorInstance.value.command.executeLocationCatalog(catalogItem.id)
      } catch {}
    }
    catalogItemDom.append(catalogItemContentDom)
    if (catalogItem.subCatalog && catalogItem.subCatalog.length) {
      appendCatalog(catalogItemDom, catalogItem.subCatalog)
    }
    parent.append(catalogItemDom)
  }
}

const handleCatalogToggle = async () => {
  try { console.log('[EMREditor] handleCatalogToggle invoked, catalogVisible=', catalogVisible.value) } catch {}
  if (!catalogDom.value) {
    try { console.warn('[EMREditor] catalogDom is null') } catch {}
    return
  }
  catalogVisible.value = !catalogVisible.value
  catalogDom.value.style.display = catalogVisible.value ? 'block' : 'none'
  if (!catalogVisible.value) {
    clearCatalog()
    return
  }
  // show and populate
  try {
    const catalogMainDom = catalogDom.value.querySelector('.catalog__main') as HTMLElement
    if (!catalogMainDom) return
    catalogMainDom.innerHTML = ''
    if (!editorInstance.value || !editorInstance.value.command || typeof editorInstance.value.command.getCatalog !== 'function') {
      try { console.warn('[EMREditor] editorInstance or getCatalog is not available') } catch {}
      return
    }
    try {
      const catalog = await editorInstance.value.command.getCatalog()
      try { console.log('[EMREditor] getCatalog result:', catalog) } catch {}
      if (catalog && Array.isArray(catalog)) {
        appendCatalog(catalogMainDom, catalog)
      } else {
        // show empty placeholder so user sees the panel
        catalogMainDom.innerHTML = '<div class="catalog-empty">无目录项</div>'
      }
    } catch (e) {
      console.error('[EMREditor] getCatalog call failed:', e)
      catalogMainDom.innerHTML = '<div class="catalog-empty">加载目录失败</div>'
    }
  } catch (e) {
    console.error('Load catalog failed:', e)
  }
}
</script>

<style scoped>
/* 基础主题变量 */
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

/* 工具栏样式重写 */
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
  border: 1px solid var(--emr-editor-border);
  border-top: none;
  background: var(--emr-editor-bg);
  min-height: 400px;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content:flex-start;
  align-items:center;
}

/* 响应式设计（仅考虑大屏显示器） */
@media (max-width: 1440px) {
  .emr-editor .canvas-toolbar {
    padding: 0 12px;
  }

  .emr-editor .menu-item {
    gap: 1px;
  }

  .emr-editor .menu-divider {
    margin: 0 8px;
  }
}

/* 目录面板样式（固定定位，在编辑器内部左边悬浮） */
.emr-editor .catalog {
  width: 220px;
  height: 400px;
  position: fixed;
  left: calc(var(--app-left-offset, 0px) + 20px);
  top: calc(var(--app-top-offset, 0px) + 250px);
  padding: 0 15px 20px 15px;
  z-index: 1200;
  background: #ffffff9c;
  /* opacity: 0.8; */
  border: 1px solid #f2f2f2;
  border-bottom: none;
  border-right: none;
  border-left: none;
  /* border-radius: 4px; */
  /* box-shadow: 0 2px 12px 0 rgb(56 56 56 / 20%); */
}

.emr-editor .catalog .catalog__header {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e6ed;
}

.emr-editor .catalog .catalog__header span {
  color: #3d4757;
  font-size: 14px;
  font-weight: bold;
}

.emr-editor .catalog .catalog__header i {
  width: 16px;
  height: 16px;
  cursor: pointer;
  display: inline-block;
  background: url('../assets/images/close.svg') no-repeat;
  transition: all .2s;
}

.emr-editor .catalog .catalog__header>div:hover {
  background: rgba(235, 238, 241);
}

.catalog__main {
  height: 320px;
  padding: 10px 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.emr-editor .catalog__main .catalog-item {
  width: 100%;
  padding-left: 10px;
  box-sizing: border-box;
}

.emr-editor .catalog__main>.catalog-item {
  padding-left: 0;
}

.emr-editor .catalog__main .catalog-item .catalog-item__content {
  width: 100%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.emr-editor .catalog__main .catalog-item .catalog-item__content:hover>span {
  color: #4991f2;
}

.emr-editor .catalog__main .catalog-item .catalog-item__content span {
  color: #3d4757;
  line-height: 30px;
  font-size: 12px;
  white-space: nowrap;
  cursor: pointer;
  user-select: none;
}

.emr-editor .catalog-empty {
  color: #9aa2ad;
  padding: 12px;
  font-size: 13px;
}
</style>
