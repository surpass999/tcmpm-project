<template>
  <div class="menu-item">
    <div class="menu-item__hyperlink" title="超链接" @click="$emit('hyperlink')"><i></i></div>
    <div class="menu-item__control" title="插入控件" @click.stop="toggleControlOptions">
      <span class="select">控件</span>
      <div class="options" :class="{ visible: controlOptionsVisible }" @click.stop="handleControlOptionClick">
        <slot name="control-options" />
      </div>
    </div>
    <div class="menu-item__title" title="标题" @click.stop="toggleTitleOptions">
      <span class="select">正文</span>
      <div class="options" :class="{ visible: titleOptionsVisible }" @click.stop="handleTitleOptionClick">
        <slot name="title-options" />
      </div>
    </div>
    <div class="menu-item__left" title="左对齐" @click="$emit('row-flex', 'left')"><i></i></div>
    <div class="menu-item__center" title="居中对齐" @click="$emit('row-flex', 'center')"><i></i></div>
    <div class="menu-item__right" title="右对齐" @click="$emit('row-flex', 'right')"><i></i></div>
    <div class="menu-item__alignment" title="两端对齐" @click="$emit('row-flex', 'alignment')"><i></i></div>
    <div class="menu-item__justify" title="分散对齐" @click="$emit('row-flex', 'justify')"><i></i></div>
    <div class="menu-item__list" title="列表" @click.stop="toggleListOptions">
      <span class="select">列表</span>
      <div class="options" :class="{ visible: listOptionsVisible }" @click.stop="handleListOptionClick">
        <slot name="list-options" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const controlOptionsVisible = ref(false)
const titleOptionsVisible = ref(false)
const listOptionsVisible = ref(false)

function toggleControlOptions() {
  controlOptionsVisible.value = !controlOptionsVisible.value
  titleOptionsVisible.value = false
  listOptionsVisible.value = false
}

function toggleTitleOptions() {
  titleOptionsVisible.value = !titleOptionsVisible.value
  controlOptionsVisible.value = false
  listOptionsVisible.value = false
}

function toggleListOptions() {
  listOptionsVisible.value = !listOptionsVisible.value
  controlOptionsVisible.value = false
  titleOptionsVisible.value = false
}

function handleControlOptionClick(event: Event) {
  const target = event.target as HTMLElement
  const li = target.closest('li')
  if (li) {
    const type = li.getAttribute('data-control')
    if (type) {
      emit('insert-control', type)
      controlOptionsVisible.value = false
    }
  }
}

function handleTitleOptionClick(event: Event) {
  const target = event.target as HTMLElement
  const li = target.closest('li')
  if (li) {
    const level = li.dataset.level || null
    emit('title-select', level)
    titleOptionsVisible.value = false
  }
}

function handleListOptionClick(event: Event) {
  const target = event.target as HTMLElement
  const li = target.closest('li')
  if (li) {
    const listType = li.getAttribute('data-list-type') || null
    const listStyle = li.getAttribute('data-list-style') || null
    emit('list-select', { listType, listStyle })
    listOptionsVisible.value = false
  }
}

const emit = defineEmits<{
  hyperlink: []
  'insert-control': [type: string]
  'title-select': [level: string | null]
  'row-flex': [type: string]
  'list-select': [payload: { listType: string | null; listStyle: string | null }]
}>()
</script>
