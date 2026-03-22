<template>
  <div class="menu-item">
    <div class="menu-item__color" title="字体颜色">
      <i @click="$refs.colorInput.click()"></i>
      <span></span>
      <input ref="colorInput" type="color" id="color" @input="$emit('color-change', $event.target.value)" />
    </div>
    <div class="menu-item__highlight" title="背景颜色">
      <i @click="$refs.highlightInput.click()"></i>
      <span></span>
      <input ref="highlightInput" type="color" id="highlight" @input="$emit('highlight-change', $event.target.value)" />
    </div>
  </div>
</template>

<script setup lang="ts">
const emit = defineEmits<{
  'color-change': [color: string]
  'highlight-change': [color: string]
}>()
</script>
