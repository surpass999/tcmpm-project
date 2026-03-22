<template>
  <div class="menu-item">
    <div class="menu-item__bold" title="加粗(Ctrl+B)" @click="$emit('bold')"><i></i></div>
    <div class="menu-item__italic" title="斜体(Ctrl+I)" @click="$emit('italic')"><i></i></div>
    <div class="menu-item__underline" title="下划线">
      <i @click="$emit('underline')"></i>
      <span class="select" @click.stop="toggleUnderlineOptions"></span>
      <div class="options" :class="{ visible: underlineOptionsVisible }" @click.stop="handleUnderlineOptionClick">
        <slot name="underline-options" />
      </div>
    </div>
    <div class="menu-item__strikeout" @click="$emit('strikeout')"><i></i></div>
    <div class="menu-item__superscript" @click="$emit('superscript')"><i></i></div>
    <div class="menu-item__subscript" @click="$emit('subscript')"><i></i></div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const underlineOptionsVisible = ref(false)

function toggleUnderlineOptions() {
  underlineOptionsVisible.value = !underlineOptionsVisible.value
}

function handleUnderlineOptionClick(event: Event) {
  const target = event.target as HTMLElement
  const li = target.closest('li')
  if (li && li.dataset.decorationStyle) {
    emit('underline-style', li.dataset.decorationStyle)
    underlineOptionsVisible.value = false
  }
}

const emit = defineEmits<{
  bold: []
  italic: []
  underline: []
  'underline-style': [style: string]
  strikeout: []
  superscript: []
  subscript: []
}>()
</script>
