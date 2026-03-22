<template>
  <div class="menu-item">
    <div class="menu-item__image" title="插入图片" @click="$refs.imageInput.click()">
      <i></i>
      <input
        ref="imageInput"
        type="file"
        id="image"
        accept=".png,.jpg,.jpeg,.svg,.gif"
        style="display:none"
        @change="onImageSelect"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
function onImageSelect(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files && target.files[0]
  if (file) {
    emit('image-select', file)
    target.value = ''
  }
}

const emit = defineEmits<{
  'image-select': [file: File]
}>()
</script>
