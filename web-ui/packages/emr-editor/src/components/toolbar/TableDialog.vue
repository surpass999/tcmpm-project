<template>
  <teleport to="body">
    <div class="toolbar-table-dialog" @click.stop :style="stylePosition">
      <div class="table-header">
        <div class="table-title">{{ rows }} × {{ cols }}</div>
        <div class="table-close" @click="close">×</div>
      </div>

      <div class="table-panel" ref="panel" @mousemove="onMouseMove" @click="onClick">
        <div class="table-rows">
          <div v-for="r in maxRows" :key="r" class="table-row">
            <div
              v-for="c in maxCols"
              :key="c"
              class="table-cel"
              :class="{ active: r <= rows && c <= cols }"
              :data-row="r"
              :data-col="c"
            ></div>
          </div>
        </div>
      </div>

    <!-- footer buttons removed to match original compact behavior -->
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'

const props = withDefaults(defineProps<{
  visible?: boolean
  maxRows?: number
  maxCols?: number
  anchorLeft?: number
  anchorTop?: number
}>(), {
  visible: true,
  maxRows: 10,
  maxCols: 10,
  anchorLeft: undefined,
  anchorTop: undefined
})

const emit = defineEmits<{
  insert: [rows: number, cols: number]
  close: []
}>()

const rows = ref(0)
const cols = ref(0)
const maxRows = props.maxRows
const maxCols = props.maxCols
const panel = ref<HTMLElement | null>(null)

watch(() => props.visible, (v) => {
  if (v) {
    // initialize selection to 10x10 (or limited by max)
    rows.value = Math.min(10, Number(maxRows) || 10)
    cols.value = Math.min(10, Number(maxCols) || 10)
  }
})

const stylePosition = computed<any>(() => {
  if (typeof props.anchorLeft === 'number' && typeof props.anchorTop === 'number') {
    return {
      position: 'fixed',
      left: `${props.anchorLeft}px`,
      top: `${props.anchorTop}px`,
      transform: 'none'
    }
  }
  return {}
})

import { onMounted } from 'vue'
onMounted(() => {
  console.log('TableDialog mounted, anchorLeft=', props.anchorLeft, 'anchorTop=', props.anchorTop, 'visible=', props.visible)
})

const onMouseMove = (evt: MouseEvent) => {
  // Prefer panel-based calculation for reliability (teleported into body)
  if (panel.value) {
    const panelRect = panel.value.getBoundingClientRect()
    const relX = evt.clientX - panelRect.left
    const relY = evt.clientY - panelRect.top

    // measure first row/col to determine cell size and gaps
    const firstRow = panel.value.querySelector('.table-row') as HTMLElement | null
    if (!firstRow) return
    const firstCells = firstRow.querySelectorAll('.table-cel')
    if (firstCells.length === 0) return
    const firstCellRect = (firstCells[0] as HTMLElement).getBoundingClientRect()
    const cellWidth = firstCellRect.width
    const cellHeight = firstCellRect.height

    // estimate horizontal gap using second cell if present
    let hGap = 6
    if (firstCells.length > 1) {
      const secondRect = (firstCells[1] as HTMLElement).getBoundingClientRect()
      hGap = Math.max(0, secondRect.left - (firstCellRect.left + firstCellRect.width))
    }

    // measure row height and vertical gap using second row if present
    const rowsEls = panel.value.querySelectorAll('.table-row')
    let vGap = 6
    let rowHeight = cellHeight
    if (rowsEls.length > 1) {
      const firstRowRect = (rowsEls[0] as HTMLElement).getBoundingClientRect()
      const secondRowRect = (rowsEls[1] as HTMLElement).getBoundingClientRect()
      rowHeight = firstRowRect.height
      vGap = Math.max(0, secondRowRect.top - (firstRowRect.top + firstRowRect.height))
    }

    // compute column and row indices (1-based)
    const col = Math.floor(relX / (cellWidth + hGap)) + 1
    const rowIdx = Math.floor(relY / (rowHeight + vGap)) + 1

    const r = Math.min(Math.max(1, rowIdx), Number(maxRows || 10))
    const c = Math.min(Math.max(1, col), Number(maxCols || 10))

    if (r !== rows.value || c !== cols.value) {
      rows.value = r
      cols.value = c
    }
    return
  }

  // fallback: elementFromPoint
  const x = evt.clientX
  const y = evt.clientY
  const el = document.elementFromPoint(x, y) as HTMLElement | null
  if (!el) return
  const cel = el.closest('.table-cel') as HTMLElement | null
  if (!cel) return
  const r = Number(cel.dataset.row || 0)
  const c = Number(cel.dataset.col || 0)
  if (r && c) {
    rows.value = r
    cols.value = c
  }
}

const onClick = () => {
  if (rows.value > 0 && cols.value > 0) {
    emit('insert', rows.value, cols.value)
  }
}

// confirm removed (no buttons). keep close for the close icon.
const close = () => {
  emit('close')
}
</script>

<style scoped>
.toolbar-table-dialog {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 260px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  padding: 8px;
  z-index: 2000;
}
.table-header {
  display:flex;
  justify-content:space-between;
  align-items:center;
  padding:4px 4px;
  border-bottom:1px solid #f3f4f6;
}
.table-title { font-size:13px; color:#333; }
.table-close { cursor:pointer; padding:4px 6px; border-radius:4px; }
.table-panel { padding:6px 6px; }
.table-rows { display:flex; flex-direction:column; gap:6px; pointer-events:auto; }
.table-row { display:flex; gap:10px; margin-top: 0px !important;}
.table-cel { width:16px !important; height:16px; border:1px solid #e9eef3; margin-right: 0px !important; background: transparent; box-sizing:border-box; display:inline-block; }
.table-cel.active { background: rgba(73,145,242,.12); border-color: rgba(73,145,242,.2); }
.table-footer { display:none; }
.btn-confirm { display:none; }
.btn-cancel { display:none; }
</style>

