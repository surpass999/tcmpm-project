<template>
  <teleport to="body">
    <div
      class="menu-item__search__collapse"
      v-show="visible"
      ref="root"
      :style="stylePosition"
    >
    <div class="menu-item__search__collapse__header">
      <input
        class="search-input"
        type="text"
        v-model="keyword"
        placeholder="查找"
        @input="onInput"
        @keydown.enter="onSearch"
      />
      <span class="search-result">{{ resultText }}</span>
      <div class="arrow-left" @click="onPrev">‹</div>
      <div class="arrow-right" @click="onNext">›</div>
      <div class="search-close" @click="onClose">✕</div>
    </div>

    <div class="menu-item__search__collapse__replace">
      <input
        type="text"
        v-model="replaceText"
        placeholder="替换为"
        @keydown.enter="onReplace"
      />
      <button @click="onReplace">替换</button>
    </div>

    <div class="menu-item__search__collapse__options">
    <label><input type="checkbox" v-model="isReg" /> 正则</label>
    <label><input type="checkbox" v-model="isCase" /> 忽略大小写</label>
      <label><input type="checkbox" v-model="isSelection" /> 选定内容内查找</label>
    </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue'

const props = defineProps<{
  visible: boolean
  initialKeyword?: string
  initialReplace?: string
  initialResult?: { index: number; count: number } | null
  anchorLeft?: number
  anchorTop?: number
}>()

const emit = defineEmits<{
  (e: 'search', payload: any): void
  (e: 'replace', payload: any): void
  (e: 'replaceAll', payload: any): void
  (e: 'prev'): void
  (e: 'next'): void
  (e: 'close'): void
}>()

const keyword = ref(props.initialKeyword || '')
const replaceText = ref(props.initialReplace || '')
const isReg = ref(false)
const isCase = ref(false)
const isSelection = ref(false)
const visible = ref(!!props.visible)
const result = ref(props.initialResult || null)
const root = ref<HTMLElement | null>(null)

import { nextTick } from 'vue'

const stylePosition = ref<any>({})

const adjustPosition = async () => {
  await nextTick()
  const panel = root.value
  if (!panel) return
  const bodyWidth = document.body.getBoundingClientRect().width
  const rect = panel.getBoundingClientRect()
  let left: number | undefined = typeof props.anchorLeft === 'number' ? props.anchorLeft : undefined
  let top: number | undefined = typeof props.anchorTop === 'number' ? props.anchorTop : undefined
  // fallback to querying toolbar button if anchor not provided or zero-ish
  if (left == null || isNaN(left) || left <= 0 || top == null || isNaN(top)) {
    const btn = document.querySelector('.emr-toolbar')?.querySelector('.toolbar-item__search') as HTMLElement | null
    if (btn) {
      const brect = btn.getBoundingClientRect()
      left = brect.left
      top = brect.bottom + 8
    } else {
      left = 8
      top = 48
    }
  }
  const width = rect.width || 360
  if (left + width > bodyWidth - 8) {
    left = Math.max(8, bodyWidth - width - 8)
  }
  panel.style.position = 'fixed'
  panel.style.left = `${left}px`
  panel.style.top = `${top}px`
  // expose as stylePosition for SSR/reactivity (fallback)
  stylePosition.value = { position: 'fixed', left: `${left}px`, top: `${top}px` }
}

watch(() => props.visible, v => (visible.value = !!v))

const resultText = computed(() => {
  if (!result.value) return ''
  return `${result.value.index}/${result.value.count}`
})

onMounted(() => {
  // adjust initial position when mounted
  adjustPosition()
})

watch(() => props.anchorLeft, () => adjustPosition())
watch(() => props.anchorTop, () => adjustPosition())
watch(() => props.visible, (v) => {
  if (v) {
    adjustPosition()
    // trigger initial search when panel opens
    onInput()
  }
})

// trigger search when options change
watch(isReg, () => {
  if (visible.value) onInput()
})
watch(isCase, () => {
  if (visible.value) onInput()
})
watch(isSelection, () => {
  if (visible.value) onInput()
})

// reflect external search result updates
watch(() => props.initialResult, v => {
  result.value = v || null
})

function onInput() {
  emit('search', {
    keyword: keyword.value || null,
    options: {
      isRegEnable: isReg.value,
      isIgnoreCase: isCase.value,
      isLimitSelection: isSelection.value
    }
  })
}

function onSearch() {
  onInput()
}

function onReplace() {
  emit('replace', {
    keyword: keyword.value,
    replaceText: replaceText.value,
    options: {
      isRegEnable: isReg.value,
      isIgnoreCase: isCase.value,
      isLimitSelection: isSelection.value
    }
  })
}

function onPrev() {
  emit('prev')
}

function onNext() {
  emit('next')
}

function onClose() {
  emit('close')
}

onMounted(() => {
  // autofocus handled by parent when opening
})
</script>

<style scoped>
.menu-item__search__collapse {
  position: fixed;
  top: 48px;
  left: 8px;
  background: #fff;
  border: 1px solid #e6eaef;
  padding: 8px;
  border-radius: 6px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  z-index: 1200;
  width: 360px;
}
.menu-item__search__collapse__header {
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-input {
  flex: 1;
  height: 30px;
  padding: 4px 8px;
  border: 1px solid #ebebeb;
  border-radius: 4px;
}
.search-result {
  min-width: 44px;
  text-align: center;
  color: #6b7280;
}
.arrow-left, .arrow-right {
  width: 28px;
  height: 28px;
  display:flex;
  align-items:center;
  justify-content:center;
  border-left: 1px solid #e2e6ed;
  cursor:pointer;
}
.search-close {
  margin:0px 6px;
  cursor: pointer;
}
.menu-item__search__collapse__replace {
  margin-top: 8px;
  display:flex;
  align-items:center;
}
.menu-item__search__collapse__replace input {
  flex:1;
  height:28px;
  padding:4px 6px;
  border:1px solid #ebebeb;
  border-radius:4px;
}
.menu-item__search__collapse__replace button {
  margin-left:6px;
  padding:4px 8px;
  border:1px solid #e2e6ed;
  background:#fff;
  border-radius:2px;
  cursor:pointer;
}
.menu-item__search__collapse__options {
  margin-top:8px;
  display:flex;
  gap:12px;
  font-size:12px;
  color:#374151;
}
</style>

