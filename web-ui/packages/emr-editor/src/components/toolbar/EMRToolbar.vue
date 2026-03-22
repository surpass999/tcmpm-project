<template>
  <div class="emr-toolbar" :class="toolbarClasses">
    <ToolbarGroup
      v-for="group in visibleGroups"
      :key="group.key"
      :title="group.title"
      :show-divider="group.showDivider"
      :layout="props.config?.layout"
    >
      <ToolbarItem
        v-for="item in group.items"
        :key="item.command"
        :type="item.type"
        :command="item.command"
        :icon="item.icon"
        :text="item.text"
        :title="item.title"
        :active="isItemActive(item.command)"
        :disabled="isItemDisabled(item.command)"
        :dropdown-options="item.dropdownOptions"
        :current-value="getItemCurrentValue(item.command)"
        :display-text="item.displayText"
        @click="(cmd, target) => handleItemClick(cmd as any, group.key, target)"
        @select="(cmd, value) => handleItemSelect(cmd as any, value, group.key)"
      />
    </ToolbarGroup>

    <TableDialog
      v-if="showTableDialog"
      :visible="showTableDialog"
      :anchor-left="dialogPosition.left"
      :anchor-top="dialogPosition.top"
      :max-rows="10"
      :max-cols="10"
      @insert="handleTableInsert"
      @close="closeTable"
    />

    <LinkDialog
      v-if="showLinkDialog"
      :visible="showLinkDialog"
      :anchor-left="dialogPosition.left"
      :anchor-top="dialogPosition.top"
      :initial-name="props.toolbarState?.rangeText || ''"
      @insert="handleLinkInsert"
    @close="closeLinkDialog"
    />

  <FormulaDialog
    v-if="showFormulaDialog"
    :visible="showFormulaDialog"
    :anchor-left="dialogPosition.left"
    :anchor-top="dialogPosition.top"
    :initial-latex="props.toolbarState?.rangeText || ''"
    :initial-size="16"
    @insert="handleFormulaInsert"
    @close="closeFormulaDialog"
  />
  <CodeBlockDialog
    v-if="showCodeBlockDialog"
    :visible="showCodeBlockDialog"
    :anchor-left="dialogPosition.left"
    :anchor-top="dialogPosition.top"
    :initial-code="props.toolbarState?.rangeText || ''"
    :initial-language="'javascript'"
    @insert="handleCodeBlockInsert"
    @close="closeCodeBlockDialog"
  />
  <WatermarkDialog
    v-if="showWatermarkDialog"
    :visible="showWatermarkDialog"
    :anchor-left="dialogPosition.left"
    :anchor-top="dialogPosition.top"
    :initial-text="props.toolbarState?.rangeText || ''"
    @insert="handleWatermarkInsert"
    @close="closeWatermarkDialog"
  />
  <ControlDialog
    v-if="showControlDialog"
    :visible="showControlDialog"
    :type="controlDialogType"
    :initial-name="props.toolbarState?.rangeText || ''"
    :initial-payload="controlDialogInitialPayload"
    @insert="handleControlDialogInsert"
    @close="closeControlDialog"
  />

    <SearchCollapse
      v-if="showSearchCollapse"
      :visible="showSearchCollapse"
      :initial-result="props.toolbarState?.searchResult"
      :anchor-left="searchDialogPosition.left"
      :anchor-top="searchDialogPosition.top"
      @search="payload => emit('command', 'search', payload)"
      @replace="payload => emit('command', 'search-replace', payload)"
      @prev="() => emit('command', 'search-prev')"
      @next="() => emit('command', 'search-next')"
      @close="closeSearch"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onBeforeUnmount } from 'vue'
import ToolbarGroup from './ToolbarGroup.vue'
import ToolbarItem from './ToolbarItem.vue'
import TableDialog from './TableDialog.vue'
import SearchCollapse from './SearchCollapse.vue'
import LinkDialog from './LinkDialog.vue'
import FormulaDialog from './FormulaDialog.vue'
import CodeBlockDialog from './CodeBlockDialog.vue'
import WatermarkDialog from './WatermarkDialog.vue'
import ControlDialog from './ControlDialog.vue'
// control dialog state
const showControlDialog = ref(false)
const controlDialogType = ref<string>('text')
const controlDialogInitialPayload = ref<any>(null)
import { Dialog } from '../dialog/Dialog'
import { ElementType } from '../../editor/dataset/enum/Element'
import { DEFAULT_TOOLBAR_CONFIG, getVisibleGroups } from './ToolbarConfig'
import type { ToolbarCommand } from './ToolbarConfig'

export interface EMRToolbarProps {
  key?: string | number
  config?: {
    enabled?: boolean
    groups?: Record<string, boolean> | any
    layout?: 'horizontal' | 'vertical'
  }
  toolbarState?: Record<string, any>
  disabledCommands?: string[]
}

const props = withDefaults(defineProps<EMRToolbarProps>(), {
  config: () => ({
    enabled: true,
    groups: {
      basic: true,
      text: true,
      controls: true,
      insert: true,
      view: true
    },
    layout: 'horizontal'
  }),
  toolbarState: () => ({}),
  disabledCommands: () => []
})

// Use broader command typing (string) so toolbar can emit framework-specific commands
const emit = defineEmits<{
  command: [command: string, value?: any]
}>()

const showTableDialog = ref(false)
const showSearchCollapse = ref(false)
const showLinkDialog = ref(false)
const showFormulaDialog = ref(false)
const showCodeBlockDialog = ref(false)
const showWatermarkDialog = ref(false)
const dialogPosition = ref({ left: 0, top: 0 })
const searchDialogPosition = ref({ left: 0, top: 48 })

const closeTable = () => {
  showTableDialog.value = false
  // remove scroll/resize listeners when dialog closed
  removeScrollListeners()
}
const closeSearch = () => {
  showSearchCollapse.value = false
  emit('command', 'search', { keyword: null, options: {} })
}

const getScrollableParents = (el: HTMLElement | null): Array<EventTarget> => {
  const parents: Array<EventTarget> = []
  let node: HTMLElement | null = el?.parentElement || null
  while (node) {
    try {
      const style = window.getComputedStyle(node)
      const overflowY = style.overflowY
      const isScrollable = (overflowY === 'auto' || overflowY === 'scroll' || overflowY === 'overlay') && node.scrollHeight > node.clientHeight
      if (isScrollable) parents.push(node)
      node = node.parentElement
    } catch {
      break
    }
  }
  parents.push(window)
  return parents
}

let scrollParents: Array<EventTarget> = []
let activeDialogType: 'table' | 'link' | 'formula' | null = null

// scroll handler for scrollable parents: only update vertical position (top)
const onParentScroll = () => {
  if (activeDialogType === 'link') {
    updateLinkPositionFromButton(/* onlyTop */ true)
  } else if (activeDialogType === 'formula') {
    updateFormulaPositionFromButton(/* onlyTop */ true)
  } else {
    updateDialogPositionFromButton(/* onlyTop */ true)
  }
}

// resize handler for window: update full position (left + top)
const onWindowResize = () => {
  if (activeDialogType === 'link') {
    updateLinkPositionFromButton()
  } else if (activeDialogType === 'formula') {
    updateFormulaPositionFromButton()
  } else {
    updateDialogPositionFromButton()
  }
}

const addScrollListenersForButton = (type: 'table' | 'link' | 'formula' | 'codeblock' | 'search' = 'table') => {
  const selector =
    type === 'table'
      ? '.toolbar-item__table'
      : type === 'link'
      ? '.toolbar-item__link'
      : type === 'formula'
      ? '.toolbar-item__latex, .toolbar-item__formula'
      : type === 'codeblock'
      ? '.toolbar-item__codeblock'
      : type === 'search'
      ? '.toolbar-item__search'
      : '.toolbar-item__watermark'
  const btn = (document.querySelector('.emr-toolbar') as HTMLElement | null)
    ?.querySelector(selector) as HTMLElement | null
  if (!btn) return
  removeScrollListeners()
  activeDialogType = type
  scrollParents = getScrollableParents(btn)
  scrollParents.forEach(p => {
    try { p.addEventListener('scroll', onParentScroll, { passive: true }) } catch {}
  })
  try { window.addEventListener('resize', onWindowResize) } catch {}
}
const removeScrollListeners = () => {
  scrollParents.forEach(p => {
    try { p.removeEventListener('scroll', onParentScroll) } catch {}
  })
  scrollParents = []
  try { window.removeEventListener('resize', onWindowResize) } catch {}
  activeDialogType = null
}

const updateDialogPositionFromButton = (onlyTop?: boolean) => {
  const btn = (document.querySelector('.emr-toolbar') as HTMLElement | null)
    ?.querySelector('.toolbar-item__table') as HTMLElement | null
  if (!btn) return
  const rect = btn.getBoundingClientRect()
  // Use client coordinates so dialog (position: fixed) aligns with toolbar and follows viewport scroll
  if (!onlyTop) dialogPosition.value.left = Math.round(rect.left)
  dialogPosition.value.top = Math.round(rect.bottom + 8)
}

const updateSearchPositionFromButton = () => {
  const btn = (document.querySelector('.emr-toolbar') as HTMLElement | null)
    ?.querySelector('.toolbar-item__search') as HTMLElement | null
  if (!btn) return
  const rect = btn.getBoundingClientRect()
  searchDialogPosition.value.left = Math.round(rect.left)
  searchDialogPosition.value.top = Math.round(rect.bottom + 8)
}

const updateLinkPositionFromButton = (onlyTop?: boolean) => {
  const btn = (document.querySelector('.emr-toolbar') as HTMLElement | null)
    ?.querySelector('.toolbar-item__link') as HTMLElement | null
  if (!btn) return
  const rect = btn.getBoundingClientRect()
  if (!onlyTop) dialogPosition.value.left = Math.round(rect.left)
  dialogPosition.value.top = Math.round(rect.bottom + 8)
}

const updateFormulaPositionFromButton = () => {
  const btn = (document.querySelector('.emr-toolbar') as HTMLElement | null)
    ?.querySelector('.toolbar-item__latex, .toolbar-item__formula') as HTMLElement | null
  if (!btn) return
  const rect = btn.getBoundingClientRect()
  if (!(arguments.length && arguments[0] === true)) dialogPosition.value.left = Math.round(rect.left)
  dialogPosition.value.top = Math.round(rect.bottom + 8)
}

const updateCodeBlockPositionFromButton = (onlyTop?: boolean) => {
  const btn = (document.querySelector('.emr-toolbar') as HTMLElement | null)
    ?.querySelector('.toolbar-item__codeblock') as HTMLElement | null
  if (!btn) return
  const rect = btn.getBoundingClientRect()
  if (!onlyTop) dialogPosition.value.left = Math.round(rect.left)
  dialogPosition.value.top = Math.round(rect.bottom + 8)
}

const updateWatermarkPositionFromButton = (onlyTop?: boolean) => {
  const btn = (document.querySelector('.emr-toolbar') as HTMLElement | null)
    ?.querySelector('.toolbar-item__watermark') as HTMLElement | null
  if (!btn) return
  const rect = btn.getBoundingClientRect()
  if (!onlyTop) dialogPosition.value.left = Math.round(rect.left)
  dialogPosition.value.top = Math.round(rect.bottom + 8)
}

const visibleGroups = computed(() => {
  if (!props.config?.enabled) return []
  const enabledGroups = props.config.groups || {}
  return getVisibleGroups(DEFAULT_TOOLBAR_CONFIG, enabledGroups as Record<string, boolean>)
})

const toolbarClasses = computed(() => {
  const classes = ['emr-toolbar']
  if (props.config?.layout) classes.push(`emr-toolbar--${props.config.layout}`)
  return classes
})

const isItemActive = (command: string): boolean => {
  const state = props.toolbarState[command]
  return state?.active === true
}
const isItemDisabled = (command: string): boolean => {
  return props.disabledCommands.includes(command)
}
const getItemCurrentValue = (command: string): string | number | undefined => {
  const state = props.toolbarState[command]
  return state?.value
}

const handleItemClick = (command: ToolbarCommand, _groupKey: string, target?: HTMLElement) => {
  // special-case painter: support single-click (apply once) and double-click (continuous)
  if (command === 'painter') {
    // use module-level temp vars
    if (!(handleItemClick as any)._isFirstClick) {
      ;(handleItemClick as any)._isFirstClick = true
    }
    if (!(handleItemClick as any)._painterTimeout) {
      ;(handleItemClick as any)._painterTimeout = 0
    }
    if ((handleItemClick as any)._isFirstClick) {
      ;(handleItemClick as any)._isFirstClick = false
      ;(handleItemClick as any)._painterTimeout = window.setTimeout(() => {
        // single click action
        emit('command', 'painter', { isDblclick: false })
        ;(handleItemClick as any)._isFirstClick = true
      }, 200)
    } else {
      // second click within 200ms -> treat as double click
      try { window.clearTimeout((handleItemClick as any)._painterTimeout) } catch {}
      ;(handleItemClick as any)._isFirstClick = true
      emit('command', 'painter', { isDblclick: true })
    }
    return
  }

  // Open table dialog locally (special-case) to compute position and add scroll listeners
  if (command === 'table') {
    showTableDialog.value = true
    // compute position and attach scroll/resize listeners to keep dialog aligned
    updateDialogPositionFromButton()
    addScrollListenersForButton('table')
    return
  }

  // Open link dialog (special-case)
  if (command === 'link') {
    // ask parent to prepare (capture current range/rangeText) before opening dialog
    emit('command', 'link-prepare')
    // parent handler should run synchronously and update props.toolbarState
    showLinkDialog.value = true
    updateLinkPositionFromButton()
    addScrollListenersForButton('link')
    return
  }
 
  // Open formula dialog (special-case)
  if (command === 'latex' || command === 'formula') {
    emit('command', 'link-prepare')
    showFormulaDialog.value = true
    updateFormulaPositionFromButton()
    addScrollListenersForButton('formula')
    return
  }
  // Open codeblock dialog
  if (command === 'codeblock') {
    emit('command', 'link-prepare')
    showCodeBlockDialog.value = true
    updateCodeBlockPositionFromButton()
    addScrollListenersForButton('codeblock')
    return
  }
  // Open watermark dialog
  if (command === 'watermark') {
    emit('command', 'link-prepare')
    showWatermarkDialog.value = true
    updateWatermarkPositionFromButton()
    addScrollListenersForButton('watermark')
    return
  }

  // Open search panel (special-case)
  if (command === 'search') {
    // show local search collapse panel
    showSearchCollapse.value = true
    updateSearchPositionFromButton()
    addScrollListenersForButton('search')
    return
  }

  // Open block dialog (special-case) - uses framework Dialog to collect block params
  if (command === 'block') {
    const dialog = new Dialog({
      title: '内容块',
      data: [
        { type: 'select', label: '类型', name: 'type', value: 'iframe', options: [{ label: 'iframe', value: 'iframe' }, { label: 'video', value: 'video' }] },
        { type: 'text', label: '宽度 (px)', name: 'width', placeholder: '请输入宽度' },
        { type: 'text', label: '高度 (px)', name: 'height', placeholder: '请输入高度' },
        { type: 'text', label: 'src', name: 'src', placeholder: 'iframe/src 地址' },
        { type: 'textarea', label: 'srcdoc', name: 'srcdoc', placeholder: 'iframe srcdoc 内容' }
      ],
      onConfirm: payload => {
        const typeVal = payload.find((p: any) => p.name === 'type')?.value
        const width = payload.find((p: any) => p.name === 'width')?.value
        const height = payload.find((p: any) => p.name === 'height')?.value
        const src = payload.find((p: any) => p.name === 'src')?.value
        const srcdoc = payload.find((p: any) => p.name === 'srcdoc')?.value
        emit('command', 'block' as any, { type: typeVal, width, height, src, srcdoc })
      }
    })
    return
  }

  // simple emit for most commands; parent will handle execution
  emit('command', command as any)
}

const handleItemSelect = (command: ToolbarCommand, value: any, _groupKey: string) => {
  // special-case control dropdown: open centered dialog for configuration
  if (command === 'control') {
    const t = String(value || 'text')
    emit('command', 'control-prepare')
    controlDialogType.value = t
    // pass initial payload/name to dialog via props (toolbarState is reactive)
    controlDialogInitialPayload.value = props.toolbarState?.controlInitialPayload || null
    showControlDialog.value = true
    return
  }
  emit('command', command as any, value)
}

const handleControlDialogInsert = (payload: any) => {
  try {
    // Ensure payload is an object and include explicit type
    const p = payload && typeof payload === 'object' ? { ...payload } : {}
    p.type = p.type || controlDialogType.value
    // if valueSets is a string (from dialog), try parse
    if (p.valueSets && typeof p.valueSets === 'string') {
      try { p.valueSets = JSON.parse(p.valueSets) } catch {}
    }
    try { console.log('[EMRToolbar] control insert emit:', `control-${controlDialogType.value}`, p) } catch {}
    emit('command', `control-${controlDialogType.value}`, p)
  } catch (e) {
    try { console.error('[EMRToolbar] handleControlDialogInsert error', e) } catch {}
  }
  showControlDialog.value = false
  removeScrollListeners()
}

const closeControlDialog = () => {
  showControlDialog.value = false
  removeScrollListeners()
}

const handleTableInsert = (rowsOrPayload: any, cols?: number) => {
  // TableDialog emits (rows, cols). Normalize to { rows, cols } object.
  let payload: any = rowsOrPayload
  if (typeof rowsOrPayload === 'number' && typeof cols === 'number') {
    payload = { rows: Number(rowsOrPayload), cols: Number(cols) }
  } else if (rowsOrPayload && typeof rowsOrPayload === 'object' && 'rows' in rowsOrPayload && 'cols' in rowsOrPayload) {
    payload = { rows: Number(rowsOrPayload.rows), cols: Number(rowsOrPayload.cols) }
  } else {
    // fallback: ignore
    closeTable()
    return
  }
  // unify with EMREditor: emit 'table' with payload { rows, cols }
  emit('command', 'table' as any, payload)
  closeTable()
}

const handleLinkInsert = (payload: any) => {
  // payload: { name?: string, url: string }
  if (!payload || typeof payload !== 'object' || !payload.url) {
    showLinkDialog.value = false
    removeScrollListeners()
    return
  }
  // Emit 'link' with { url, name }
  emit('command', 'link' as any, { url: String(payload.url), name: payload.name || '' })
  showLinkDialog.value = false
  removeScrollListeners()
}

// Close handler for link dialog (used in template)
const closeLinkDialog = () => {
  showLinkDialog.value = false
  removeScrollListeners()
}

const handleFormulaInsert = (payload: any) => {
  // payload: { latex: string, size?: number }
  if (!payload || typeof payload !== 'object' || !payload.latex) {
    showFormulaDialog.value = false
    removeScrollListeners()
    return
  }
  emit('command', 'latex' as any, { latex: String(payload.latex), size: Number(payload.size || 16) })
  showFormulaDialog.value = false
  removeScrollListeners()
}

const closeFormulaDialog = () => {
  showFormulaDialog.value = false
  removeScrollListeners()
}

const handleCodeBlockInsert = (payload: any) => {
  // payload: { language: string, code: string }
  if (!payload || typeof payload !== 'object' || !payload.code) {
    showCodeBlockDialog.value = false
    removeScrollListeners()
    return
  }
  emit('command', 'codeblock' as any, { language: String(payload.language || 'plain'), code: String(payload.code) })
  showCodeBlockDialog.value = false
  removeScrollListeners()
}

const closeCodeBlockDialog = () => {
  showCodeBlockDialog.value = false
  removeScrollListeners()
}
const handleWatermarkInsert = (payload: any) => {
  // payload: { text, opacity, size, color }
  if (!payload || typeof payload !== 'object' || !payload.text) {
    showWatermarkDialog.value = false
    removeScrollListeners()
    return
  }
  emit('command', 'watermark-add' as any, {
    text: String(payload.text),
    opacity: Number(payload.opacity || 0.12),
    size: Number(payload.size || 48),
    color: String(payload.color || '#000000'),
    repeat: !!payload.repeat,
    gap: Array.isArray(payload.gap) ? [Number(payload.gap[0] || 10), Number(payload.gap[1] || 10)] : undefined
  })
  showWatermarkDialog.value = false
  removeScrollListeners()
}

const closeWatermarkDialog = () => {
  showWatermarkDialog.value = false
  removeScrollListeners()
}

onBeforeUnmount(() => {
  removeScrollListeners()
  // clear painter timeout if any
  try {
    const t = (handleItemClick as any)._painterTimeout
    if (t) window.clearTimeout(t)
  } catch {}
})
</script>

<style scoped>
.emr-toolbar {
  padding: 6px;
  display: flex;
  justify-content: center;
  gap: 6px;
  align-items: center;
  /* Use editor theme variables for background/border/shadow to match original project */
  background: var(--emr-toolbar-bg, #ffffff);
  /* border: 1px solid var(--emr-toolbar-border, #e5e7eb); */
  box-shadow: var(--emr-toolbar-shadow, 0 1px 3px rgba(0,0,0,0.1));
  height: var(--emr-toolbar-height, 40px);
  /* Fixed positioning to align with framework offsets */
  position: fixed;
  top: var(--app-top-offset, 0px);
  left: var(--app-left-offset, 0px);
  width: var(--app-content-width, 100%);
  z-index: var(--app-z, 900);
  transition: transform 0.18s ease;
}
</style>
