<template>
  <div class="menu-item">
    <div class="menu-item__font" @click.stop="toggleFontOptions">
      <span class="select">微软雅黑</span>
      <div class="options" :class="{ visible: fontOptionsVisible }" @click.stop="handleFontOptionClick">
        <slot name="font-options" />
      </div>
    </div>
    <div class="menu-item__size" @click.stop="toggleSizeOptions">
      <span class="select">小四</span>
      <div class="options" :class="{ visible: sizeOptionsVisible }" @click.stop="handleSizeOptionClick">
        <slot name="size-options" />
      </div>
    </div>
    <div class="menu-item__size-add" title="增大字号(Ctrl+[)" @click="$emit('size-add')"><i></i></div>
    <div class="menu-item__size-minus" title="减小字号(Ctrl+])" @click="$emit('size-minus')"><i></i></div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const root = ref<HTMLElement | null>(null)
const fontOptionsVisible = ref(false)
const sizeOptionsVisible = ref(false)

function toggleFontOptions() {
  fontOptionsVisible.value = !fontOptionsVisible.value
  sizeOptionsVisible.value = false // close other dropdowns
}

function toggleSizeOptions() {
  sizeOptionsVisible.value = !sizeOptionsVisible.value
  fontOptionsVisible.value = false // close other dropdowns
}

function selectFont(family: string) {
  emit('font-select', family)
  fontOptionsVisible.value = false
}

function selectSize(size: number) {
  emit('size-select', size)
  sizeOptionsVisible.value = false
}

function handleFontOptionClick(event: Event) {
  const target = event.target as HTMLElement
  const li = target.closest('li')
  if (li && li.dataset.family) {
    emit('font-select', li.dataset.family)
    fontOptionsVisible.value = false
  }
}

function handleSizeOptionClick(event: Event) {
  const target = event.target as HTMLElement
  const li = target.closest('li')
  if (li && li.dataset.size) {
    emit('size-select', Number(li.dataset.size))
    sizeOptionsVisible.value = false
  }
}

const emit = defineEmits<{
  'font-select': [family: string]
  'size-select': [size: number]
  'size-add': []
  'size-minus': []
}>()
</script>
