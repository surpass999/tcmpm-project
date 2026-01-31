<template>
  <div class="mr-editor-wrapper">
    <Toolbar
      @undo="onUndo"
      @redo="onRedo"
      @painter="onPainter"
      @format="onFormat"
      @underline="onUnderline"
      @size-add="onSizeAdd"
      @size-minus="onSizeMinus"
      @bold="onBold"
      @italic="onItalic"
      @font-select="onFontSelect"
      @size-select="onSizeSelect"
      @underline-style="onUnderlineStyle"
      @strikeout="onStrikeout"
      @superscript="onSuperscript"
      @subscript="onSubscript"
      @color-change="onColorChange"
      @highlight-change="onHighlightChange"
      @title-select="onTitleSelect"
      @row-flex="onRowFlex"
      @list-select="onListSelect"
      @insert-table="onInsertTable"
      @image-select="onImageSelect"
      @hyperlink="onHyperlink"
      @insert-control="onInsertControl"
      @page-scale-add="onPageScaleAdd"
      @page-scale-minus="onPageScaleMinus"
    >
      <template #font-options>
        <ul>
          <li data-family="Microsoft YaHei" style="font-family:'Microsoft YaHei';">微软雅黑</li>
          <li data-family="Arial" style="font-family:'Arial';">Arial</li>
          <li data-family="SimSun" style="font-family:'SimSun';">宋体</li>
        </ul>
      </template>
      <template #size-options>
        <ul>
          <li data-size="16">小四</li>
          <li data-size="14">五号</li>
          <li data-size="12">小五</li>
        </ul>
      </template>
      <template #underline-options>
        <ul>
          <li data-decoration-style='solid'><i></i></li>
          <li data-decoration-style='double'><i></i></li>
          <li data-decoration-style='dashed'><i></i></li>
        </ul>
      </template>
      <template #title-options>
        <ul>
          <li data-level="1">标题1</li>
          <li data-level="2">标题2</li>
          <li data-level="3">标题3</li>
          <li data-level="4">标题4</li>
          <li data-level="5">标题5</li>
          <li data-level="6">标题6</li>
          <li data-level="">正文</li>
        </ul>
      </template>
      <template #list-options>
        <ul>
          <li data-list-type="ul" data-list-style="disc">• 无序列表</li>
          <li data-list-type="ul" data-list-style="circle">○ 无序列表</li>
          <li data-list-type="ul" data-list-style="square">▪ 无序列表</li>
          <li data-list-type="ol" data-list-style="decimal">1. 有序列表</li>
          <li data-list-type="ol" data-list-style="lower-alpha">a. 有序列表</li>
          <li data-list-type="ol" data-list-style="upper-alpha">A. 有序列表</li>
        </ul>
      </template>
      <template #control-options>
        <ul>
          <li data-control="checkbox">复选框</li>
          <li data-control="radio">单选框</li>
          <li data-control="separator">分割线</li>
        </ul>
      </template>
    </Toolbar>

    <!-- header/footer/page controls -->
    

    <div class="mr-editor-container" ref="containerRef"></div>
    <!-- bottom footbar (page-width, centered over page) -->
    <div
      class="mr-footbar footer"
      :style="{
        position: 'fixed',
        left: footbarLeft + 'px',
        bottom: '0px',
        width: paperWidth + 'px',
        height: '36px',
        background: '#f7f8fa',
        borderTop: '1px solid #e9eef5',
        display: 'flex',
        alignItems: 'center',
        padding: '0 12px',
        zIndex: 2100
      }"
    >
      <div style="display:flex;align-items:center;color:#6b7280;font-size:12px;">
        <i class="icon-pages" style="width:16px;height:16px;display:inline-block;background-repeat:no-repeat;background-size:contain"></i>
        <span class="page-no-list">{{ visiblePageNoListText || '-' }}</span>
        <span>页面：{{ intersectionPageNo }} </span>
        <span> / </span>
        <span class="page-size">{{ pageSizeText || '-' }}</span>
        <span v-if="rowNo !== null && colNo !== null">行：{{ rowNo }} 列：{{ colNo }}</span>
        <span style="margin-left:8px">总页数：{{ pageCount }}</span>
        <!-- page jump removed -->
      </div>
      <div style="margin-left:auto;display:flex;align-items:center;gap:10px">
        <button class="catalog-mode" title="目录" @click="onToggleCatalog" style="background:none;border:0;cursor:pointer">
          <i></i>
        </button>

        <div class="paper-size" style="position:relative;">
          <button title="纸张大小" @click="paperSizeOpen = !paperSizeOpen" style="background:none;border:0;cursor:pointer">
            <i></i>
          </button>
          <ul v-if="paperSizeOpen" style="position:absolute;bottom:28px;right:0;background:#fff;border:1px solid #e6e9ef;padding:6px;list-style:none;margin:0;">
            <li :class="{active: currentPaperSize === '794x1123' || currentPaperSize === 'A4'}" style="padding:4px 8px;cursor:pointer" @click="setPaperSize(794,1123)">A4</li>
            <li :class="{active: currentPaperSize === '1123x1587' || currentPaperSize === 'A3'}" style="padding:4px 8px;cursor:pointer" @click="setPaperSize(1123,1587)">A3</li>
            <li :class="{active: currentPaperSize === '792x1224' || currentPaperSize === 'Letter'}" style="padding:4px 8px;cursor:pointer" @click="setPaperSize(792,1224)">Letter</li>
          </ul>
        </div>

        <button class="paper-direction" title="纸张方向" @click="togglePaperDirection" style="background:none;border:0;cursor:pointer">
          <i></i>
        </button>

        <div class="paper-margin" style="position:relative;">
          <button title="页边距" @click="marginOpen = !marginOpen" style="background:none;border:0;cursor:pointer">
            <i></i>
          </button>
          <ul v-if="marginOpen" style="position:absolute;bottom:28px;right:0;background:#fff;border:1px solid #e6e9ef;padding:6px;list-style:none;margin:0;">
            <li style="padding:4px 8px;cursor:pointer" @click="setPaperMargin([40,40,40,40])">窄</li>
            <li style="padding:4px 8px;cursor:pointer" @click="setPaperMargin([60,60,60,60])">普通</li>
            <li style="padding:4px 8px;cursor:pointer" @click="setPaperMargin([80,80,80,80])">宽</li>
          </ul>
        </div>

        <button :class="isFullscreen ? 'fullscreen exist' : 'fullscreen'" title="全屏" @click="toggleFullscreen" style="background:none;border:0;cursor:pointer">
          <i></i>
        </button>

        <div class="page-mode" style="position:relative;">
          <button title="页面模式" @click="pageModeOpen = !pageModeOpen" style="background:none;border:0;cursor:pointer">
            <i></i>
          </button>
          <ul v-if="pageModeOpen" style="position:absolute;bottom:28px;right:0;background:#fff;border:1px solid #e6e9ef;padding:6px;list-style:none;margin:0;">
            <li :class="{active: pageMode === 'paging'}" style="padding:4px 8px;cursor:pointer" @click="onPageModeSelect('paging')">分页</li>
            <li :class="{active: pageMode === 'continuity'}" style="padding:4px 8px;cursor:pointer" @click="onPageModeSelect('continuity')">连续</li>
          </ul>
        </div>

        <button class="menu-item__print" title="打印" @click="onPrint" style="background:none;border:0;cursor:pointer">
          <i></i>
        </button>
        <button class="menu-item__image-download" title="保存为图片" @click="onSaveAsImage" style="background:none;border:0;cursor:pointer">
          <i></i>
        </button>
        <!-- prev/next removed -->

        
        <button title="缩小" @click="onPageScaleMinus" style="background:none;border:0;cursor:pointer" class="page-scale-minus">
          <i></i>
        </button>
        <span style="padding:0 8px;color:#374151;cursor:pointer" @click="onPageScaleRecovery" class="page-scale-percentage">{{ pageScalePercentage }}</span>
        <button title="放大" @click="onPageScaleAdd" style="background:none;border:0;cursor:pointer" class="page-scale-add">
          <i></i>
        </button>
      </div>
    </div>
    <!-- catalog sidebar -->
    <div
      v-if="catalogOpen"
      class="mr-catalog"
      :style="{
        position: 'fixed',
      left: catalogPosition.left + 'px',
      top: catalogPosition.top + 'px',
        width: '300px',
        maxHeight: '60vh',
        overflow: 'auto',
        background: '#fff',
        border: '1px solid #e6e9ef',
        boxShadow: '0 6px 18px rgba(0,0,0,0.08)',
        zIndex: 2200,
        padding: '8px'
      }"
    >
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
        <strong>目录</strong>
        <button @click="catalogOpen = false" style="background:none;border:0;cursor:pointer">关闭</button>
      </div>
      <ul style="list-style:none;padding:0;margin:0">
        <template v-for="item in catalogItems" :key="item.id">
          <li @click="onCatalogItemClick(item)" style="cursor:pointer;padding:6px 8px;border-radius:4px" :title="item.name">
            <span :style="{display:'inline-block',width:(item.level*12)+'px'}"></span>
            <span>{{ item.name }}</span>
            <span style="float:right;color:#6b7280">{{ item.pageNo + 1 }}</span>
          </li>
          <li v-if="item.subCatalog && item.subCatalog.length" v-for="sub in item.subCatalog" :key="sub.id" @click="onCatalogItemClick(sub)" style="cursor:pointer;padding:6px 8px;border-radius:4px" :title="sub.name">
            <span :style="{display:'inline-block',width:(sub.level*12)+'px'}"></span>
            <span>{{ sub.name }}</span>
            <span style="float:right;color:#6b7280">{{ sub.pageNo + 1 }}</span>
          </li>
        </template>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import '../editor/style.css'
// import full demo styles from original canvas-editor to ensure icons and layout match demo
import '../../../../canvas-editor/src/style.css'
// @ts-ignore - allow using dynamic emits from Toolbar.vue
import Toolbar from './toolbar/Toolbar.vue'
import CanvasEditorDefault from '../editor'

const props = defineProps<{
  data?: any
  options?: any
}>()
const emit = defineEmits<{
  (e: 'change', value: any): void
}>()

const containerRef = ref<HTMLDivElement | null>(null)
let editor: InstanceType<typeof CanvasEditorDefault> | null = null
let lastRangeContext: any = null
const headerEnabled = ref(true)
const footerEnabled = ref(true)
const footerText = ref<string>('')
const headerText = ref<string>('')
const headerTop = ref<number>(0)
const showPageNumber = ref<boolean>(true)
// paper width for footbar (matches page width)
const paperWidth = ref<number>(0)
const footbarLeft = ref<number>(0)
// footbar state
const visiblePageNoListText = ref<string>('')
const intersectionPageNo = ref<number>(1)
const pageSizeText = ref<string>('')
const pageScalePercentage = ref<string>('100%')
const rowNo = ref<number | null>(null)
const colNo = ref<number | null>(null)
const paperSizeOpen = ref(false)
const marginOpen = ref(false)
const isFullscreen = ref(false)
const pageModeOpen = ref(false)
const pageMode = ref<string>('paging')
const currentPaperSize = ref<string>('')
const currentPaperDirection = ref<string>('')
const catalogOpen = ref(false)
const catalogItems = ref<any[]>([])
const pageCount = ref<number>(0)
// page jump removed per user request
const catalogTrigger = ref(0)
const catalogFixed = ref<{ left: number; top: number } | null>(null)

const catalogPosition = computed(() => {
  // depend on trigger so position recomputes on resize/scroll/page changes
  void catalogTrigger.value
  const catalogWidth = 300
  const catalogHeight = Math.min(window.innerHeight * 0.6, 600)
  const { pageContainer, pages } = getPageContainerAndPages()
  if (!pageContainer || !pages || pages.length === 0) {
    return { left: Math.max(8, footbarLeft.value - catalogWidth - 8), top: 80 }
  }
  // if a fixed catalog position was set when opened, use it so the popup won't move with content scroll
  if (catalogFixed.value) return catalogFixed.value
  try {
    // anchor to the editor container's parent (the actual page area wrapper).
    // Prefer `containerRef` because it is the component element that hosts the editor.
    const anchorEl = (containerRef.value && (containerRef.value.parentElement || containerRef.value)) as HTMLElement | null
    const containerAnchor = anchorEl || (pageContainer as HTMLElement).parentElement || pageContainer
    const rect = (containerAnchor as HTMLElement).getBoundingClientRect()
    // position relative to the page container left border with small gap (5px)
    let desiredLeft = Math.round(rect.left) + 5
    const chosenLeft = Math.min(Math.max(8, desiredLeft), Math.max(8, window.innerWidth - catalogWidth - 8))
    // vertically center around 1/3 down the container area
    const desiredCenterY = rect.top + rect.height * 0.33
    const desiredTop = Math.round(desiredCenterY - catalogHeight / 2)
    // clamp within viewport with 8px margin
    const minTop = 8
    const maxTop = Math.max(minTop, window.innerHeight - catalogHeight - 8)
    const top = Math.max(minTop, Math.min(maxTop, desiredTop))
    return { left: chosenLeft, top }
  } catch {
    return { left: Math.max(8, footbarLeft.value - catalogWidth - 8), top: 80 }
  }
})

function applyHeaderFooter() {
  if (!editor) return
  try {
    // update header/footer enabled flags
    ;(editor.command as any).updateOptions({
      header: { disabled: !headerEnabled.value },
      footer: { disabled: !footerEnabled.value }
    })

    // header options and content
    ;(editor.command as any).updateOptions({
      header: {
        disabled: !headerEnabled.value,
        top: headerTop.value
      },
      footer: {
        disabled: !footerEnabled.value
      }
    })

    // footer handling: if user wants page numbers, enable pageNumber with Chinese format
    if (showPageNumber.value) {
      const defaultFormat = '第{pageNo}页/共{pageCount}页'
      ;(editor.command as any).updateOptions({
        pageNumber: { format: defaultFormat, numberType: 'chinese', disabled: false }
      })
      ;(editor.command as any).setHTML({ footer: '' })
    } else {
      // otherwise use footerText: if contains placeholders, apply as pageNumber format (chinese)
      const ftext = footerText.value || ''
      const footerHasPlaceholder = ftext.includes('{pageNo}') || ftext.includes('{pageCount}')
      if (footerHasPlaceholder) {
        ;(editor.command as any).updateOptions({
          pageNumber: { format: ftext, numberType: 'chinese', disabled: false }
        })
        ;(editor.command as any).setHTML({ footer: '' })
      } else {
        const safeHtml = `<div>${escapeHtml(ftext)}</div>`
        ;(editor.command as any).setHTML({ footer: safeHtml })
      }
    }

    // header content handling (support placeholders similarly, but do not force numberType)
    const htext = headerText.value || ''
    const headerHasPlaceholder = htext.includes('{pageNo}') || htext.includes('{pageCount}')
    if (headerHasPlaceholder) {
      ;(editor.command as any).setHTML({ header: '' })
      ;(editor.command as any).updateOptions({
        pageNumber: { format: htext }
      })
    } else {
      const safeHeaderHtml = `<div>${escapeHtml(htext)}</div>`
      ;(editor.command as any).setHTML({ header: safeHeaderHtml })
    }
  } catch (e) {
    console.error('applyHeaderFooter failed', e)
  }
}

// apply header/footer whenever relevant controls change
watch([headerEnabled, footerEnabled, headerText, footerText, showPageNumber], () => {
  try { applyHeaderFooter() } catch {}
})
function escapeHtml(str: string) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

// toolbar state is tracked via DOM and editor listener; no local reactive needed here

onMounted(() => {
  if (!containerRef.value) return
  // Default A4 size in px (approx)
  const DEFAULT_A4_WIDTH = 794
  const DEFAULT_A4_HEIGHT = 1123
  const initialOptions = {
    ...(props.options || {}),
    width: (props.options && props.options.width) || DEFAULT_A4_WIDTH,
    height: (props.options && props.options.height) || DEFAULT_A4_HEIGHT
  }
  // initialize paperWidth to editor page width (A4 by default)
  paperWidth.value = initialOptions.width
  // keep aspect ratio for responsive resizing
  const paperAspect = initialOptions.height / initialOptions.width
  editor = new CanvasEditorDefault(
    containerRef.value,
    props.data || { main: [] },
    initialOptions
  )

  // initialize footbar values and listeners similar to original demo
  try {
    const rangeContext = (editor.command as any).getRangeContext?.()
    if (rangeContext) {
      rowNo.value = rangeContext.startRowNo != null ? rangeContext.startRowNo + 1 : null
      colNo.value = rangeContext.startColNo != null ? rangeContext.startColNo + 1 : null
    }
    const opts = (editor.command as any).getOptions?.()
    if (opts && typeof opts.scale === 'number') {
      pageScalePercentage.value = `${Math.floor(opts.scale * 100)}%`
    }
  } catch {}

  // set up listeners for footbar updates
  editor.listener.visiblePageNoListChange = function (payload: number[]) {
    visiblePageNoListText.value = payload.map(i => i + 1).join('、')
  }
  editor.listener.pageSizeChange = function (payload: string | number) {
    if (!editor) return
    pageSizeText.value = `${payload}`
    // update pageCount when pageSize changes (worker may also change pages)
    const { pages } = getPageContainerAndPages()
    pageCount.value = pages.length
    // reflect current paper size selection (payload may be width or "WxH")
    try {
      const opts = (editor.command as any).getOptions?.()
      if (opts && opts.width && opts.height) {
        currentPaperSize.value = `${opts.width}x${opts.height}`
      } else {
        currentPaperSize.value = `${payload}`
      }
    } catch {}
  }
  editor.listener.intersectionPageNoChange = function (payload: number) {
    intersectionPageNo.value = payload + 1
  }
  editor.listener.pageScaleChange = function (payload: number) {
    pageScalePercentage.value = `${Math.floor(payload * 100)}%`
  }
  editor.listener.pageModeChange = function (payload: any) {
    try {
      pageMode.value = payload
    } catch {}
  }
  // reflect initial paper options
  try {
    const opts = (editor.command as any).getOptions?.()
    if (opts) {
      currentPaperDirection.value = opts.paperDirection || ''
      if (opts.width && opts.height) currentPaperSize.value = `${opts.width}x${opts.height}`
    }
  } catch {}
  // initialize pageCount from current page container
  const { pages } = getPageContainerAndPages()
  pageCount.value = pages.length
  editor.listener.rangeStyleChange = editor.listener.rangeStyleChange || null
  editor.listener.contentChange = () => {
    try {
      const v = editor?.command.getValue()
      emit('change', v)
    } catch {}
  }
    
  // 绑定工具栏事件
  setupToolbarBindings()
  // 注册状态回显监听 + fallback subscriptions
  registerStatusEcho()
  registerStatusEchoFallbacks()
  // 响应窗口变化，调整画纸宽度
  const onResize = () => {
    if (!editor || !containerRef.value) return
    const available = Math.max(400, containerRef.value.parentElement!.clientWidth - 40)
    // If user provided explicit page width in props, keep page size; otherwise fit by scaling.
    const originalWidth = initialOptions.width || DEFAULT_A4_WIDTH
    if (props.options && props.options.width) {
      // user specified width -> keep that physical page size
      const newWidth = (props.options.width as number)
      const newHeight = Math.round(newWidth * paperAspect)
      paperWidth.value = newWidth
      // update paper size in editor to match explicit width/height
      try {
        ;(editor.command as any).executePaperSize(newWidth, newHeight)
      } catch {
        try { ;(editor.command as any).executePaperSize({ width: newWidth, height: newHeight }) } catch {}
      }
    } else {
      // fit to available width by adjusting scale
      const scale = Math.min(1, available / originalWidth)
      paperWidth.value = Math.round(originalWidth * scale)
      try {
        ;(editor.command as any).executePageScale(scale)
      } catch {}
    }
    // trigger catalog recompute after resize
    catalogTrigger.value++
  }
  window.addEventListener('resize', onResize)
  // update footbar fixed left position on scroll as well (so it stays aligned to page)
  const updateFootbarPosition = () => {
    if (!containerRef.value) return
    const rect = containerRef.value.getBoundingClientRect()
    // compute left so footbar aligns with centered page inside the editor container
    const left = rect.left + (containerRef.value.clientWidth - paperWidth.value) / 2 + window.scrollX
    footbarLeft.value = Math.max(0, Math.round(left))
    // trigger catalog recompute
    catalogTrigger.value++
  }
  const onScroll = () => updateFootbarPosition()
  window.addEventListener('scroll', onScroll, { passive: true })
  // initial compute
  updateFootbarPosition()
  // store onResize for cleanup
  ;(containerRef.value as any).__mr_onresize = onResize
  ;(containerRef.value as any).__mr_on_scroll = onScroll
})

onBeforeUnmount(() => {
  if (containerRef.value) {
    const onResizeStored = (containerRef.value as any).__mr_onresize
    if (onResizeStored) window.removeEventListener('resize', onResizeStored)
    const onScrollStored = (containerRef.value as any).__mr_on_scroll
    if (onScrollStored) window.removeEventListener('scroll', onScrollStored)
    const painterMouseup = (containerRef.value as any).__mr_painter_mouseup
    if (painterMouseup) {
      try { containerRef.value.removeEventListener('mouseup', painterMouseup) } catch {}
    }
  }
  if (editor) {
    try { editor.destroy?.() } catch {}
  }
})

function setupToolbarBindings() {
  if (!containerRef.value || !editor) return
  const root = containerRef.value.parentElement!
  try {
    // Only setup painter mouseup handler since it needs to bind to editor container
    const painterDom = root.querySelector<HTMLDivElement>('.menu-item__painter')
    if (painterDom) {
      // When user releases mouse inside editor container, if painter mode is active, apply the painter style.
      const painterMouseUpHandler = () => {
        try {
          // only apply if painter is active (visualized by .active class from rangeStyleChange)
          if (painterDom.classList.contains('active')) {
            editor!.command.executeApplyPainterStyle()
          }
        } catch {}
      }
      containerRef.value!.addEventListener('mouseup', painterMouseUpHandler)
      // store for cleanup
      ;(containerRef.value as any).__mr_painter_mouseup = painterMouseUpHandler
    }
  } catch (e) {
    // ignore missing dom parts
  }
}

function registerStatusEcho() {
  if (!editor || !containerRef.value) return
  const root = containerRef.value.parentElement!
  const fontSelectDom = root.querySelector<HTMLDivElement>('.menu-item__font .select')
  const fontOptionDom = root.querySelector<HTMLDivElement>('.menu-item__font .options')
  const sizeSelectDom = root.querySelector<HTMLDivElement>('.menu-item__size .select')
  const sizeOptionDom = root.querySelector<HTMLDivElement>('.menu-item__size .options')
  const boldDom = root.querySelector<HTMLDivElement>('.menu-item__bold')
  const italicDom = root.querySelector<HTMLDivElement>('.menu-item__italic')
  const underlineDom = root.querySelector<HTMLDivElement>('.menu-item__underline')
  const strikeoutDom = root.querySelector<HTMLDivElement>('.menu-item__strikeout')
  const undoDom = root.querySelector<HTMLDivElement>('.menu-item__undo')
  const redoDom = root.querySelector<HTMLDivElement>('.menu-item__redo')
  const painterDom = root.querySelector<HTMLDivElement>('.menu-item__painter')
  const pageScaleDom = root.querySelector<HTMLSpanElement>('.page-scale-percentage')
  const pageNoDom = root.querySelector<HTMLSpanElement>('.page-no')
  const pageSizeDom = root.querySelector<HTMLSpanElement>('.page-size')

  editor.listener.rangeStyleChange = (payload: any) => {
    try {
      // debug: log payload to help diagnose status echo
      console.debug('rangeStyleChange payload:', payload)
      // guard: if payload lacks font/size and we don't have a lastRangeContext,
      // avoid clearing UI — only update minimal flags (undo/redo/painter) if present.
      try {
        // sanitize payload: treat keys with undefined as absent (delete them)
        if (payload && typeof payload === 'object') {
          Object.keys(payload).forEach(k => {
            if (payload[k] === undefined) delete payload[k]
          })
        }
        // Instrumentation: detect payload fields explicitly set to undefined (helps trace origin)
        if (payload && typeof payload === 'object') {
          const suspiciousKeys = Object.keys(payload).filter(k => payload[k] === undefined)
          if (suspiciousKeys.length) {
            console.warn('rangeStyleChange received keys with undefined values:', suspiciousKeys, payload)
            console.warn('lastRangeContext:', lastRangeContext)
            console.trace()
          }
        }
        const hasSizeField = payload && Object.prototype.hasOwnProperty.call(payload, 'size') && payload.size !== undefined
        const hasFontField = payload && Object.prototype.hasOwnProperty.call(payload, 'font') && payload.font !== undefined
        if (!hasSizeField && !hasFontField && !lastRangeContext) {
          try {
            if (payload && Object.prototype.hasOwnProperty.call(payload, 'undo') && payload.undo !== undefined) {
              payload.undo ? undoDom?.classList.remove('no-allow') : undoDom?.classList.add('no-allow')
            }
            if (payload && Object.prototype.hasOwnProperty.call(payload, 'redo') && payload.redo !== undefined) {
              payload.redo ? redoDom?.classList.remove('no-allow') : redoDom?.classList.add('no-allow')
            }
            if (payload && Object.prototype.hasOwnProperty.call(payload, 'painter') && payload.painter !== undefined) {
              payload.painter ? painterDom?.classList.add('active') : painterDom?.classList.remove('active')
            }
          } catch {}
          return
        }
      } catch {}
      // font
      // ensure we have font/size info when payload omits them (some events emit partial payloads)
      try {
        const needFont = !payload || payload.font == null
        const needSize = !payload || payload.size == null
        if ((needFont || needSize)) {
          let rc = (editor as any).command?.getRangeContext && (editor as any).command.getRangeContext()
          if (!rc) rc = lastRangeContext
          if (rc) {
            lastRangeContext = rc
            payload = {
              ...payload,
              font: payload?.font ?? rc.font,
              size: payload?.size ?? rc.size
            }
          }
        }
      } catch {}
      if (fontOptionDom && fontSelectDom && payload.font) {
        const cur = fontOptionDom.querySelector<HTMLLIElement>(`[data-family='${payload.font}']`)
        if (cur) {
          ;(fontSelectDom as HTMLElement).textContent = cur.innerText
          ;(fontSelectDom as HTMLElement).style.fontFamily = payload.font
        }
      }
      // size
      if (sizeSelectDom) {
        const sizeMap: Record<number, string> = {
          56: '初号',
          48: '小初',
          34: '一号',
          32: '小一',
          29: '二号',
          24: '小二',
          21: '三号',
          20: '小三',
          18: '四号',
          16: '小四',
          14: '五号',
          12: '小五',
          10: '六号',
          8: '小六',
          7: '七号',
          6: '八号'
        }
        // Update size when payload contains size OR when payload contains font (follow font's update logic),
        // otherwise attempt to read from rangeContext/lastRangeContext without clearing existing UI.
        // Distinguish between "field absent", "field present but undefined", and "field explicitly null".
        // Treat undefined as "absent" (do not clear UI). Only clear when size === null.
        const hasSizeField = payload && Object.prototype.hasOwnProperty.call(payload, 'size') && payload.size !== undefined
        const hasFontField = payload && Object.prototype.hasOwnProperty.call(payload, 'font') && payload.font !== undefined

        try {
          const rc = (editor as any).command?.getRangeContext?.() || lastRangeContext

          if (hasSizeField) {
            // payload explicitly provides size (could be null to clear)
            if (payload.size !== null) {
              const label = sizeMap[payload.size] || String(payload.size)
              ;(sizeSelectDom as HTMLElement).textContent = label
              if (sizeOptionDom) {
                sizeOptionDom.querySelectorAll('li').forEach(li => li.classList.remove('active'))
                const activeLi = sizeOptionDom.querySelector<HTMLLIElement>(`[data-size='${payload.size}']`)
                if (activeLi) activeLi.classList.add('active')
              }
            } else {
              // explicit clear
              ;(sizeSelectDom as HTMLElement).textContent = ''
              if (sizeOptionDom) {
                sizeOptionDom.querySelectorAll('li').forEach(li => li.classList.remove('active'))
              }
            }
          } else if (hasFontField) {
            // font changed but size not provided -> prefer rangeContext size if available (do not clear UI)
            if (rc && rc.size != null) {
              const label = sizeMap[rc.size] || String(rc.size)
              ;(sizeSelectDom as HTMLElement).textContent = label
              if (sizeOptionDom) {
                sizeOptionDom.querySelectorAll('li').forEach(li => li.classList.remove('active'))
                const activeLi = sizeOptionDom.querySelector<HTMLLIElement>(`[data-size='${rc.size}']`)
                if (activeLi) activeLi.classList.add('active')
              }
            }
          } else {
            // neither size nor font fields present -> try to fill from rc but do not clear existing UI
            if (rc && rc.size != null) {
              const label = sizeMap[rc.size] || String(rc.size)
              ;(sizeSelectDom as HTMLElement).textContent = label
              if (sizeOptionDom) {
                sizeOptionDom.querySelectorAll('li').forEach(li => li.classList.remove('active'))
                const activeLi = sizeOptionDom.querySelector<HTMLLIElement>(`[data-size='${rc.size}']`)
                if (activeLi) activeLi.classList.add('active')
              }
            }
          }
        } catch {}
      }
      // toggles - only update when payload explicitly contains the field
      if (payload && Object.prototype.hasOwnProperty.call(payload, 'bold') && payload.bold !== undefined) {
        payload.bold ? boldDom?.classList.add('active') : boldDom?.classList.remove('active')
      }
      if (payload && Object.prototype.hasOwnProperty.call(payload, 'italic') && payload.italic !== undefined) {
        payload.italic ? italicDom?.classList.add('active') : italicDom?.classList.remove('active')
      }
      if (payload && Object.prototype.hasOwnProperty.call(payload, 'underline') && payload.underline !== undefined) {
        payload.underline ? underlineDom?.classList.add('active') : underlineDom?.classList.remove('active')
      }
      if (payload && Object.prototype.hasOwnProperty.call(payload, 'strikeout') && payload.strikeout !== undefined) {
        payload.strikeout ? strikeoutDom?.classList.add('active') : strikeoutDom?.classList.remove('active')
      }
      // color/highlight - only modify when payload explicitly includes color/highlight
      {
        const colorSpan = root.querySelector<HTMLSpanElement>('.menu-item__color span')
        const colorControl = root.querySelector<HTMLInputElement>('#color')
        if (payload && Object.prototype.hasOwnProperty.call(payload, 'color') && payload.color !== undefined) {
          if (payload.color) {
            if (colorSpan) colorSpan.style.backgroundColor = payload.color
            if (colorControl) colorControl.value = payload.color
            root.querySelector<HTMLDivElement>('.menu-item__color')?.classList.add('active')
          } else {
            if (colorSpan) colorSpan.style.backgroundColor = ''
            if (colorControl) colorControl.value = '#000000'
            root.querySelector<HTMLDivElement>('.menu-item__color')?.classList.remove('active')
          }
        }
      }
      {
        const hlSpan = root.querySelector<HTMLSpanElement>('.menu-item__highlight span')
        const hlControl = root.querySelector<HTMLInputElement>('#highlight')
        if (payload && Object.prototype.hasOwnProperty.call(payload, 'highlight') && payload.highlight !== undefined) {
          if (payload.highlight) {
            if (hlSpan) hlSpan.style.backgroundColor = payload.highlight
            if (hlControl) hlControl.value = payload.highlight
            root.querySelector<HTMLDivElement>('.menu-item__highlight')?.classList.add('active')
          } else {
            if (hlSpan) hlSpan.style.backgroundColor = ''
            if (hlControl) hlControl.value = '#ffff00'
            root.querySelector<HTMLDivElement>('.menu-item__highlight')?.classList.remove('active')
          }
        }
      }
      // undo/redo
      payload.undo ? undoDom?.classList.remove('no-allow') : undoDom?.classList.add('no-allow')
      payload.redo ? redoDom?.classList.remove('no-allow') : redoDom?.classList.add('no-allow')
    } catch (e) {
      // ignore
    }
  }

  editor.listener.pageScaleChange = (payload: number) => {
    if (pageScaleDom) pageScaleDom.innerText = `${Math.floor(payload * 10 * 10)}%`
  }
  editor.listener.intersectionPageNoChange = (payload: number) => {
    if (pageNoDom) pageNoDom.innerText = `${payload + 1}`
  }
  editor.listener.pageSizeChange = (payload: number) => {
    if (pageSizeDom) pageSizeDom.innerText = `${payload}`
  }
}

// enhance: also subscribe to eventBus and selectionchange as fallback
function registerStatusEchoFallbacks() {
  if (!editor || !containerRef.value) return
  const handler = (payload: any) => {
    try {
      // reuse existing rangeStyleChange handler
      const fn = ((editor as any).listener.rangeStyleChange as any)
      if (typeof fn === 'function') fn(payload)
    } catch {}
  }
  // subscribe eventBus if available
  try {
    if (editor && (editor as any).eventBus && (editor as any).eventBus.on) {
      ;(editor as any).eventBus.on('rangeStyleChange', handler)
    }
  } catch {}

  // selectionchange fallback: debounce and poll rangeContext
  let t: any = null
  const selectionHandler = () => {
    if (t) clearTimeout(t)
    t = setTimeout(() => {
      try {
        const rc = (editor as any).command?.getRangeContext?.()
        if (rc) {
          // remember last known range context for cases where getRangeContext later returns null
          lastRangeContext = rc
          const payload = {
            font: rc.font,
            size: rc.size,
            bold: rc.bold,
            italic: rc.italic,
            underline: rc.underline,
            strikeout: rc.strikeout,
            color: rc.color,
            highlight: rc.highlight,
            undo: (editor as any).historyManager?.isCanUndo?.() ?? false,
            redo: (editor as any).historyManager?.isCanRedo?.() ?? false
          }
          handler(payload)
        }
      } catch {}
    }, 120)
  }
  document.addEventListener('selectionchange', selectionHandler)

  // store cleanup handlers on container for unmount
  ;(containerRef.value as any).__mr_status_fallback = {
    selectionHandler
  }
}


onBeforeUnmount(() => {
  try {
    editor?.destroy && editor.destroy()
  } catch {}
  editor = null
  try {
    const onResize = (containerRef.value as any)?.__mr_onresize
    if (onResize) window.removeEventListener('resize', onResize)
  } catch {}
  try {
    const fb = (containerRef.value as any)?.__mr_status_fallback
    if (fb && fb.selectionHandler) document.removeEventListener('selectionchange', fb.selectionHandler)
  } catch {}
  try {
    const painterHandler = (containerRef.value as any)?.__mr_painter_mouseup
    if (painterHandler && containerRef.value) containerRef.value.removeEventListener('mouseup', painterHandler)
  } catch {}
})

watch(() => props.data, (nv) => {
  if (!editor || !nv) return
  try {
    editor.command.executeSetValue(nv as any)
  } catch {}
})

// toolbar DOM handlers are bound directly in setupToolbarBindings
// Toolbar event handlers (used by Toolbar.vue emits)
function onUndo() {
  editor?.command.executeUndo()
}
function onRedo() {
  editor?.command.executeRedo()
}
function onPainter() {
  editor?.command.executePainter({ isDblclick: false })
}
function onFormat() {
  editor?.command.executeFormat()
}
function onSizeAdd() {
  editor?.command.executeSizeAdd()
}
function onSizeMinus() {
  editor?.command.executeSizeMinus()
}
function onBold() {
  editor?.command.executeBold()
}
function onItalic() {
  editor?.command.executeItalic()
}
function onFontSelect(f: string) {
  editor?.command.executeFont(f)
}
function onSizeSelect(s: number) {
  editor?.command.executeSize(s)
}
function onUnderline() {
  // default to solid underline when user clicks the underline button
  editor?.command.executeUnderline({ style: 'solid' as any })
}
function getPageContainerAndPages() {
  if (!editor) return { pageContainer: null, pages: [] as HTMLCanvasElement[] }
  try {
    const container = (editor.command as any).getContainer?.()
    if (!container) return { pageContainer: null, pages: [] as HTMLCanvasElement[] }
    const pageContainer = container.querySelector?.('.ce-page-container') || null
    const pages = pageContainer ? Array.from(pageContainer.querySelectorAll('canvas')) as HTMLCanvasElement[] : []
    return { pageContainer, pages }
  } catch {
    return { pageContainer: null, pages: [] as HTMLCanvasElement[] }
  }
}

/* prev/next page controls removed */

function onPageScaleAdd() {
  try { (editor?.command as any).executePageScaleAdd() } catch {}
}

function onPageScaleMinus() {
  try { (editor?.command as any).executePageScaleMinus() } catch {}
}
function onPageScaleRecovery() {
  try { (editor?.command as any).executePageScaleRecovery() } catch {}
}
function onPrint() {
  try { editor?.command.executePrint() } catch {}
}

function onSaveAsImage() {
  try { (editor?.command as any).executeSaveAsImageElement?.() } catch {}
}
function onPageModeSelect(mode: 'paging' | 'continuity') {
  try {
    (editor?.command as any).executePageMode(mode)
    pageMode.value = mode
    pageModeOpen.value = false
  } catch {}
}
function onUnderlineStyle(style: string) {
  editor?.command.executeUnderline({ style: style as any })
}
function onStrikeout() {
  editor?.command.executeStrikeout()
}
function onSuperscript() {
  editor?.command.executeSuperscript()
}
function onSubscript() {
  editor?.command.executeSubscript()
}
function onColorChange(val: string) {
  editor?.command.executeColor(val)
}
function onHighlightChange(val: string) {
  editor?.command.executeHighlight(val)
}
function onTitleSelect(level: string | null) {
  editor?.command.executeTitle(level as any)
}
function onRowFlex(r: string) {
  editor?.command.executeRowFlex(r as any)
}
function onListSelect(payload: any) {
  editor?.command.executeList(payload?.listType as any, payload?.listStyle as any)
}
function onHyperlink() {
  if (!editor) return
  const text = editor.command.getRangeText() || ''
  const name = window.prompt('超链接文本', text) || text
  const url = window.prompt('URL', 'https://') || ''
  if (!url) return
  try {
    editor.command.executeHyperlink({
      url,
      valueList: name ? name.split('').map(ch => ({ value: ch })) : undefined
    })
  } catch (e) {
    console.error('executeHyperlink failed', e)
  }
}
function onInsertControl(type: string) {
  editor?.command.executeInsertControl({ type } as any)
}
function onImageSelect(file: File) {
  if (!editor) return
  try {
    const reader = new FileReader()
    reader.onload = () => {
      const value = reader.result as string
      const image = new Image()
      image.src = value
      image.onload = () => {
        editor!.command.executeImage({
          value,
          width: image.width,
          height: image.height
        })
      }
    }
    reader.readAsDataURL(file)
  } catch (e) {
    console.error('image insert failed', e)
  }
}
function onInsertTable(payload: { rows: number, cols: number }) {
  if (!editor) return
  try {
    const r = Math.max(1, payload.rows || 1)
    const c = Math.max(1, payload.cols || 1)
    editor.command.executeInsertTable(r, c)
  } catch (e) {
    console.error('insert table failed', e)
  }
}
function onToggleCatalog() {
  // toggle: close if already open
  if (catalogOpen.value) {
    catalogOpen.value = false
    catalogFixed.value = null
    return
  }
  if (!editor) return
  try {
    ;(editor.command as any).getCatalog().then((catalog: any) => {
      catalogItems.value = Array.isArray(catalog) ? catalog : []
      // compute a fixed catalog position at time of opening so it won't follow page scroll
      const { pageContainer, pages } = getPageContainerAndPages()
      if (pageContainer && pages && pages.length) {
        try {
          // compute left based on page container's parent so popup sits in left gap area
          const catalogWidth = 300
          const catalogHeight = Math.min(window.innerHeight * 0.6, 600)
          const anchorEl = (containerRef.value && (containerRef.value.parentElement || containerRef.value)) as HTMLElement | null
          const containerAnchor = anchorEl || (pageContainer as HTMLElement).parentElement || pageContainer
          const rect = (containerAnchor as HTMLElement).getBoundingClientRect()
          const desiredLeft = Math.round(rect.left) + 5
          const left = Math.min(Math.max(8, desiredLeft), Math.max(8, window.innerWidth - catalogWidth - 8))
          const desiredCenterY = rect.top + rect.height * 0.33
          const desiredTop = Math.round(desiredCenterY - catalogHeight / 2)
          const minTop = 8
          const maxTop = Math.max(minTop, window.innerHeight - catalogHeight - 8)
          const top = Math.max(minTop, Math.min(maxTop, desiredTop))
          catalogFixed.value = { left, top }
        } catch {
          catalogFixed.value = null
        }
      } else {
        catalogFixed.value = null
      }
      catalogOpen.value = true
    }).catch(() => {
      catalogOpen.value = false
      catalogFixed.value = null
    })
  } catch (e) {}
}

function setPaperSize(width: number, height: number) {
  if (!editor) {
    paperSizeOpen.value = false
    return
  }
  try {
    ;(editor.command as any).executePaperSize(width, height)
  } catch {
    try { ;(editor.command as any).executePaperSize({ width, height }) } catch {}
  } finally {
    paperSizeOpen.value = false
  }
}

function togglePaperDirection() {
  if (!editor) return
  try {
    // toggle between vertical and horizontal based on current option
    const opts = (editor.command as any).getOptions?.()
    const current = opts?.paperDirection || currentPaperDirection.value || 'vertical'
    const next = current === 'horizontal' ? 'vertical' : 'horizontal'
    ;(editor.command as any).executePaperDirection(next)
    // reflect immediately in UI (listener in editor may also update)
    currentPaperDirection.value = next
  } catch {}
}
// listeners update currentPaperDirection from editor; no extra watch needed

function setPaperMargin(margins: number[]) {
  if (!editor) {
    marginOpen.value = false
    return
  }
  try {
    ;(editor.command as any).executeSetPaperMargin(margins)
  } catch {}
  marginOpen.value = false
}

function toggleFullscreen() {
  const el = containerRef.value?.parentElement || containerRef.value
  if (!el) return
  if (!isFullscreen.value) {
    if ((el as any).requestFullscreen) (el as any).requestFullscreen()
    isFullscreen.value = true
  } else {
    if ((document as any).exitFullscreen) (document as any).exitFullscreen()
    isFullscreen.value = false
  }
}

/* page jump removed */

function onCatalogItemClick(item: any) {
  if (!item) return
  // if item has pageNo, jump to that page
  const pageNo = Number(item.pageNo)
  if (!isNaN(pageNo)) {
    const { pageContainer, pages } = getPageContainerAndPages()
    if (!pageContainer || !pages.length) return
    const idx = Math.max(0, Math.min(pages.length - 1, pageNo))
    const target = pages[idx] as HTMLElement
    pageContainer.scrollTop = target.offsetTop
    target.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    catalogOpen.value = false
  }
}
// expose instance API to parent via ref
defineExpose({
  getInstance: () => editor,
  executeInsertControl: (payload: any) => editor && editor.command.executeInsertControl(payload),
  executeSetValue: (val: any) => editor && editor.command.executeSetValue(val),
  executeCommand: (name: string, ...args: any[]) => {
    if (!editor) return
    const cmd = (editor.command as any)[name]
    if (typeof cmd === 'function') return cmd.apply(editor.command, args)
  }
})
</script>

<style scoped>
.mr-editor-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.mr-editor-toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
}
.mr-editor-container {
  width: 100%;
  height: 100%;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.mr-editor-container { display:flex; justify-content:center; align-items:center; min-height: calc(100vh - 120px); }

/* canvas-editor footer button styles */

/* footbar button states */
.mr-footbar button {
  padding: 4px;
  border-radius: 3px;
  transition: all 0.15s ease;
}
.mr-footbar button:hover:not(:disabled) {
  background-color: #e5e7eb;
}
.mr-footbar button:disabled {
  opacity: 0.4 !important;
  cursor: not-allowed;
}
.mr-footbar .active {
  background-color: #dbeafe;
  color: #1d4ed8;
}
.mr-footbar .paper-size li:hover,
.mr-footbar .paper-margin li:hover {
  background-color: #f3f4f6;
}

/* canvas-editor footer button styles - override canvas-editor styles */
.footer .catalog-mode i {
  background-image: url('../../../../canvas-editor/src/assets/images/catalog.svg') !important;
}
.footer .page-prev i {
  background-image: url('../editor/assets/images/arrow-left.svg') !important;
  background-repeat: no-repeat !important;
  background-size: 14px 14px !important;
  background-position: center !important;
  display: inline-block;
  width: 16px;
  height: 16px;
}
.footer .page-next i {
  background-image: url('../editor/assets/images/arrow-right.svg') !important;
  background-repeat: no-repeat !important;
  background-size: 14px 14px !important;
  background-position: center !important;
  display: inline-block;
  width: 16px;
  height: 16px;
}
.footer .menu-item__print i {
  background-image: url('../editor/assets/images/print.svg') !important;
  background-repeat: no-repeat !important;
  background-size: contain !important;
  display: inline-block;
  width: 16px;
  height: 16px;
}
.footer .menu-item__image-download i {
  background-image: url('../editor/assets/images/image-download.svg') !important;
  background-repeat: no-repeat !important;
  background-size: contain !important;
  display: inline-block;
  width: 16px;
  height: 16px;
}

/* small utility */
.mr-footbar .page-no-list { max-width: 180px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>

