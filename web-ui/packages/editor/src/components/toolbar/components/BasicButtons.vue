<template>
  <div class="menu-item">
    <div class="menu-item__undo" title="撤销(Ctrl+Z)" @click="$emit('undo')"><i></i></div>
    <div class="menu-item__redo" title="重做(Ctrl+Y)" @click="$emit('redo')"><i></i></div>
    <div class="menu-item__painter" title="格式刷" @click="$emit('painter')"><i></i></div>
    <div class="menu-item__format" title="清除格式" @click="$emit('format')"><i></i></div>
  </div>
</template>

<script setup lang="ts">
defineEmits<{
  undo: []
  redo: []
  painter: []
  format: []
}>()
</script>
