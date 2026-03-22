<template>
  <div class="menu-wrapper" ref="wrapper">
    <div ref="placeholder" style="display:none;height:0"></div>
    <div class="menu" editor-component="menu" ref="root">
      <BasicButtons
        @undo="$emit('undo')"
        @redo="$emit('redo')"
        @painter="$emit('painter')"
        @format="$emit('format')"
      />
      <div class="menu-divider"></div>
      <TableInserter @insert-table="$emit('insert-table', $event)" />
      <div class="menu-divider"></div>
      <FontSelector
        @font-select="$emit('font-select', $event)"
        @size-select="$emit('size-select', $event)"
        @size-add="$emit('size-add')"
        @size-minus="$emit('size-minus')"
      >
        <template #font-options>
          <slot name="font-options" />
        </template>
        <template #size-options>
          <slot name="size-options" />
        </template>
      </FontSelector>
      <div class="menu-divider"></div>
      <TextFormatButtons
        @bold="$emit('bold')"
        @italic="$emit('italic')"
        @underline="$emit('underline')"
        @underline-style="$emit('underline-style', $event)"
        @strikeout="$emit('strikeout')"
        @superscript="$emit('superscript')"
        @subscript="$emit('subscript')"
      >
        <template #underline-options>
          <slot name="underline-options" />
        </template>
      </TextFormatButtons>
      <div class="menu-divider"></div>
      <ImageInserter @image-select="$emit('image-select', $event)" />
      <ColorPickers
        @color-change="$emit('color-change', $event)"
        @highlight-change="$emit('highlight-change', $event)"
      />
      <div class="menu-divider"></div>
      <AdvancedButtons
        @hyperlink="$emit('hyperlink')"
        @insert-control="$emit('insert-control', $event)"
        @title-select="$emit('title-select', $event)"
        @row-flex="$emit('row-flex', $event)"
        @list-select="$emit('list-select', $event)"
      >
        <template #title-options>
          <slot name="title-options" />
        </template>
        <template #list-options>
          <slot name="list-options" />
        </template>
        <template #control-options>
          <slot name="control-options" />
        </template>
      </AdvancedButtons>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import BasicButtons from './components/BasicButtons.vue'
import FontSelector from './components/FontSelector.vue'
import TextFormatButtons from './components/TextFormatButtons.vue'
import ColorPickers from './components/ColorPickers.vue'
import TableInserter from './components/TableInserter.vue'
import ImageInserter from './components/ImageInserter.vue'
import AdvancedButtons from './components/AdvancedButtons.vue'

const emit = defineEmits([
  'undo',
  'redo',
  'painter',
  'format',
  'font-select',
  'size-select',
  'size-add',
  'size-minus',
  'bold',
  'italic',
  'underline',
  'underline-style',
  'strikeout',
  'superscript',
  'subscript',
  'color-change',
  'highlight-change',
  'title-select',
  'row-flex',
  'list-select',
  'image-select',
  'insert-table',
  'hyperlink',
  'insert-control',
  'page-prev',
  'page-next',
  'page-scale-add',
  'page-scale-minus'
])

const root = ref<HTMLElement | null>(null)
const wrapper = ref<HTMLElement | null>(null)
const placeholder = ref<HTMLElement | null>(null)
let scrollHandler: (() => void) | null = null
let resizeHandler: (() => void) | null = null
let closeOptionsHandler: (() => void) | null = null

onMounted(() => {
  if (!root.value) return

  // sticky/fixed toolbar behavior
  const getHeaderHeight = () => {
    const v = getComputedStyle(document.documentElement).getPropertyValue('--app-header-height')
    const parsed = parseInt(v)
    return Number.isFinite(parsed) ? parsed : 56
  }
  const headerHeight = getHeaderHeight()

  const updateSticky = () => {
    if (!root.value || !wrapper.value || !placeholder.value) return
    const wrapRect = wrapper.value.getBoundingClientRect()
    if (wrapRect.top <= headerHeight) {
      root.value.classList.add('menu--fixed')
      root.value.style.left = wrapRect.left + 'px'
      root.value.style.width = wrapRect.width + 'px'
      placeholder.value.style.display = 'block'
      placeholder.value.style.height = wrapRect.height + 'px'
    } else {
      root.value.classList.remove('menu--fixed')
      root.value.style.left = ''
      root.value.style.width = ''
      placeholder.value.style.display = 'none'
      placeholder.value.style.height = '0'
    }
  }

  scrollHandler = () => updateSticky()
  resizeHandler = () => updateSticky()
  window.addEventListener('scroll', scrollHandler, { passive: true })
  window.addEventListener('resize', resizeHandler)
  // initial
  updateSticky()

  // close options on document click
  closeOptionsHandler = () => {
    root.value!.querySelectorAll<HTMLElement>('.menu-item .options.visible').forEach(el => el.classList.remove('visible'))
  }
  document.addEventListener('click', closeOptionsHandler)
  ;(root.value as any).__mr_close_options = closeOptionsHandler
})

onBeforeUnmount(() => {
  // cleanup document event listener
  if ((root.value as any).__mr_close_options) {
    document.removeEventListener('click', (root.value as any).__mr_close_options)
  }
  if (scrollHandler) window.removeEventListener('scroll', scrollHandler)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
})
</script>

<style scoped>
.mr-toolbar { display:flex; justify-content:space-between; align-items:center; padding:8px 16px; background:#f6f8fa; border-bottom:1px solid #e9eef5; }
.toolbar-center { display:flex; gap:8px; align-items:center; }
.toolbar-left, .toolbar-right { display:flex; gap:8px; align-items:center; }

/* ensure options are hidden by default and shown when .visible is added */
/* table panel spacing & grid tweaks to leave breathing room at bottom */
.menu-item__table__collapse .table-panel {
  cursor: pointer;
  padding: 8px;
  padding-bottom: 18px; /* give extra space at bottom so last row isn't flush to popup edge */
  box-sizing: border-box;
  width: 100%;
  height: calc(100% - 20px); /* leave room for title area inside collapse */
}
.menu-item__table__collapse .table-panel .table-row {
  display: flex;
  flex-wrap: nowrap;
  margin-top: 10px;
  pointer-events: none;
}
.menu-item__table__collapse .table-panel .table-cel {
  width: 18px;
  height: 15px;
  box-sizing: border-box;
  border: 1px solid #e2e6ed;
  background: #fff;
  position: relative;
  margin-right: 6px;
  pointer-events: none;
}
.menu-item__table__collapse .table-panel .table-cel.active {
  border: 1px solid rgba(73, 145, 242, .2);
  background: rgba(73, 145, 242, .15);
}
.menu-item__table__collapse .table-panel .table-row .table-cel:last-child {
  margin-right: 0;
}

/* make toolbar sticky: stays at its place but sticks under app header when scrolled */
.menu {
  position: sticky;
  top: var(--app-header-height, 56px);
  z-index: 2200;
  background: #fff;
}
.menu { box-shadow: 0 1px 0 rgba(0,0,0,0.03); }
/* fixed state applied via JS when sticky should pin under header */
.menu--fixed {
  position: fixed !important;
  top: var(--app-header-height, 56px) !important;
  z-index: 2300 !important;
  box-shadow: 0 6px 18px rgba(0,0,0,0.08);
  background: #fff;
}

/* position the collapse panel like the original demo to avoid large top gap / bottom overflow */
.menu-item__table__collapse {
  width: 270px;
  height: 310px;
  background: #fff;
  box-shadow: 0 2px 12px 0 rgb(56 56 56 / 20%);
  border: 1px solid #e2e6ed;
  box-sizing: border-box;
  border-radius: 2px;
  position: absolute;
  display: none;
  z-index: 99;
  top: 25px;
  left: 0;
  padding: 14px 27px; /* original demo padding */
  cursor: auto;
}
.menu-item__table__collapse .table-panel {
  width: 100%; /* calc(100% - 54px); /* account for left/right padding */
  height: calc(100% - 35px); /* account for title and padding */
  margin-top: 0;
  overflow: hidden;
}
</style>

<style>
/* Global (non-scoped) table-panel styles copied from canvas-editor to apply to dynamically created TR/TD */
.menu-item__table__collapse .table-panel {
  cursor: pointer;
}
.menu-item__table__collapse .table-panel .table-row {
  display: flex;
  flex-wrap: nowrap;
  margin-top: 10px;
  pointer-events: none;
}
.menu-item__table__collapse .table-panel .table-cel {
  width: 16px;
  height: 16px;
  box-sizing: border-box;
  border: 1px solid #e2e6ed;
  background: #fff;
  position: relative;
  margin-right: 6px;
  pointer-events: none;
}
.menu-item__table__collapse .table-panel .table-cel.active {
  border: 1px solid rgba(73, 145, 242, .2);
  background: rgba(73, 145, 242, .15);
}
.menu-item__table__collapse .table-panel .table-row .table-cel:last-child {
  margin-right: 0;
}

/* Global dropdown styles for toolbar components */
.menu-item {
  position: relative;
}
.menu-item .options {
  display: none;
  position: absolute;
  top: 100%;
  left: 0;
  background: #fff;
  border: 1px solid #e2e6ed;
  border-radius: 2px;
  box-shadow: 0 2px 12px 0 rgb(56 56 56 / 20%);
  z-index: 99;
  min-width: 100px;
  white-space: nowrap;
}

/* specific widths for different dropdowns */
.menu-item__control .options {
  min-width: 140px;
}
.menu-item__list .options {
  min-width: 140px;
}

.menu-item .options.visible {
  display: block;
}
.menu-item .options li {
  padding: 6px 12px;
  cursor: pointer;
  user-select: none;
}
.menu-item .options li:hover {
  background-color: #f5f5f5;
}

/* special hover for underline options to preserve background-image */
.menu-item .menu-item__underline .options li:hover {
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 2px;
}

/* restore original canvas-editor underline dropdown visuals using background images */
.menu-item .menu-item__underline .options {
  width: 128px;
}
.menu-item .menu-item__underline li {
  padding: 1px 5px;
  background-repeat: no-repeat;
  background-position: left center;
  background-size: 100% 16px;
}
.menu-item__underline li[data-decoration-style=\"solid\"] {
  background-image: url('./editor/assets/images/line-single.svg');
}
.menu-item__underline li[data-decoration-style=\"double\"] {
  background-image: url('./editor/assets/images/line-double.svg');
}
.menu-item__underline li[data-decoration-style=\"dashed\"] {
  background-image: url('./editor/assets/images/line-dash-small-gap.svg');
}
.menu-item__underline li[data-decoration-style=\"dotted\"] {
  background-image: url('./editor/assets/images/line-dot.svg');
}
.menu-item__underline li[data-decoration-style=\"wavy\"] {
  background-image: url('./editor/assets/images/line-wavy.svg');
}

/* Control/List select sizing and no-wrap */
.menu-item__control,
.menu-item__list {
  min-width: 55px;
}
.menu-item__control .select,
.menu-item__list .select {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  max-width: 140px;
  min-width: 50px;
  height: 24px;
  line-height: 24px;
  color: #3d4757;
  padding: 0 6px;
}
.menu-item .menu-item__control .options,
.menu-item__list .options {
  min-width: 220px !important;
  width: auto !important;
  white-space: nowrap;
}

/* widen specific dropdowns that need more room */
.menu-item__control .options,
.menu-item__list .options,
.menu-item__title .options {
  min-width: 160px;
}
/* ensure list items don't add their own underline — only inner <i> draws the line */
.menu-item .options li {
  border-bottom: none !important;
}
/* double-line pseudo-elements */
.menu-item .options li[data-decoration-style='double'] i::before,
.menu-item .options li[data-decoration-style='double'] i::after {
  content: \"\";
  position: absolute;
  left: 0;
  right: 0;
  height: 0;
  border-bottom: 2px solid rgba(0,0,0,0.85);
  pointer-events: none;
}
.menu-item .options li[data-decoration-style='double'] i::before { top: -4px; }
.menu-item .options li[data-decoration-style='double'] i::after { top: 4px; }

/* small CE icon helpers using canvas-editor assets */
.ce-icon {
  width: 16px;
  height: 16px;
  display: inline-block;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
  vertical-align: middle;
}
.ce-icon-catalog { background-image: url('./editor/assets/images/catalog.svg'); }
.ce-icon-paper-size { background-image: url('./editor/assets/images/paper-size.svg'); }
.ce-icon-orient { background-image: url('./editor/assets/images/paper-direction.svg'); }
.ce-icon-margin { background-image: url('./editor/assets/images/paper-margin.svg'); }
.ce-icon-full { background-image: url('./editor/assets/images/request-fullscreen.svg'); }
.ce-icon-exit-full { background-image: url('./editor/assets/images/exit-fullscreen.svg'); }
.ce-icon-page-mode { background-image: url('./editor/assets/images/page-mode.svg'); }
.ce-icon-prev-page { background-image: url('./editor/assets/images/arrow-left.svg'); }
.ce-icon-next-page { background-image: url('./editor/assets/images/arrow-right.svg'); }
.ce-icon-zoom-in { background-image: url('./editor/assets/images/zoom-in.svg'); }
.ce-icon-zoom-out { background-image: url('./editor/assets/images/zoom-out.svg'); }
</style>
