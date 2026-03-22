<template>
  <div class="menu-item">
    <div class="menu-item__table" title="插入表格" @click="toggleTablePanel">
      <i></i>
    </div>
    <div class="menu-item__table__collapse" ref="tablePanelContainer">
      <div class="table-close" @click="recoveryTable">×</div>
      <div class="table-title">
        <span class="table-select">{{ tableTitle }}</span>
        <span>表格</span>
      </div>
      <div class="table-panel" ref="tablePanel" @mousemove="onPanelMouseMove" @click="onTableInsert"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

const tablePanelContainer = ref<HTMLElement | null>(null)
const tablePanel = ref<HTMLElement | null>(null)
const tableTitle = ref('插入')

let tableBuilt = false
let tableCellList: HTMLDivElement[][] = []
let colIndex = 0
let rowIndex = 0

function toggleTablePanel() {
  if (tablePanelContainer.value) {
    tablePanelContainer.value.style.display = 'block'
    if (!tableBuilt) buildTableGrid()
  }
}

function recoveryTable() {
  removeAllTableCellSelect()
  tableTitle.value = '插入'
  colIndex = 0
  rowIndex = 0
  if (tablePanelContainer.value) {
    tablePanelContainer.value.style.display = 'none'
  }
}

function removeAllTableCellSelect() {
  tableCellList.forEach(tr => tr.forEach(td => td.classList.remove('active')))
}

function buildTableGrid() {
  if (!tablePanel.value) return

  // follow original canvas-editor: create table rows (tr/td) with fixed cell size + margins
  tablePanel.value.innerHTML = ''
  tableCellList = []
  for (let i = 0; i < 10; i++) {
    const tr = document.createElement('tr')
    tr.classList.add('table-row')
    const trCellList: HTMLDivElement[] = []
    for (let j = 0; j < 10; j++) {
      const td = document.createElement('td')
      td.classList.add('table-cel')
      tr.append(td)
      trCellList.push(td)
    }
    tablePanel.value.append(tr)
    tableCellList.push(trCellList)
  }
  tableBuilt = true
}

function onPanelMouseMove(evt: MouseEvent) {
  if (!tableBuilt || !tablePanel.value) return

  // Robust hit-testing: use element bounding boxes to determine which cell the pointer is over.
  // This avoids arithmetic errors from padding/border/gap differences or fractional pixels.
  const clientX = evt.clientX
  const clientY = evt.clientY
  let foundCol = 0
  let foundRow = 0

  try {
    // determine column by scanning first row's cells
    const firstRow = tableCellList[0]
    if (firstRow && firstRow.length) {
      for (let c = 0; c < firstRow.length; c++) {
        const td = firstRow[c]
        const r = td.getBoundingClientRect()
        if (clientX >= r.left) {
          foundCol = c + 1
        }
      }
    }
    // determine row by scanning first column's cells
    for (let r = 0; r < tableCellList.length; r++) {
      const td = tableCellList[r][0]
      if (!td) continue
      const rect = td.getBoundingClientRect()
      if (clientY >= rect.top) {
        foundRow = r + 1
      }
    }
  } catch (e) {
    // fallback to simple offset math if anything fails
    const panelRect = tablePanel.value.getBoundingClientRect()
    const offsetX = Math.max(0, (evt as any).offsetX || evt.clientX - panelRect.left)
    const offsetY = Math.max(0, (evt as any).offsetY || evt.clientY - panelRect.top)
    const celSize = 16
    const rowMarginTop = 10
    const celMarginRight = 6
    foundCol = Math.ceil((offsetX) / (celSize + celMarginRight)) || 1
    foundRow = Math.ceil((offsetY) / (celSize + rowMarginTop)) || 1
  }

  // clamp within [1,10]
  colIndex = Math.min(10, Math.max(1, foundCol))
  rowIndex = Math.min(10, Math.max(1, foundRow))
  removeAllTableCellSelect()
  tableCellList.forEach((tr, trIndex) => {
    tr.forEach((td, tdIndex) => {
      if (tdIndex < colIndex && trIndex < rowIndex) td.classList.add('active')
    })
  })
  tableTitle.value = `${rowIndex}×${colIndex}`
}

function onTableInsert() {
  if (!tableBuilt) return
  emit('insert-table', { rows: rowIndex, cols: colIndex })
  recoveryTable()
}

const emit = defineEmits<{
  'insert-table': [table: { rows: number; cols: number }]
}>()
</script>
