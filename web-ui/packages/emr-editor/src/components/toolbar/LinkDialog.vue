<template>
  <teleport to="body">
    <div class="toolbar-link-dialog" @click.stop :style="stylePosition" v-if="visible">
      <div class="link-header">
        <div class="link-title">插入链接</div>
        <div class="link-close" @click="close">×</div>
      </div>

      <div class="link-body">
        <label class="field">
          <div class="label">显示文本</div>
          <input v-model="name" class="input" placeholder="显示为文本（可选）" />
        </label>
        <label class="field">
          <div class="label">链接地址</div>
          <input v-model="url" class="input" placeholder="https://example.com" />
        </label>
      </div>

      <div class="link-footer">
        <button class="btn btn-cancel" @click="close">取消</button>
        <button class="btn btn-confirm" @click="confirm">插入</button>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = withDefaults(defineProps<{
  visible?: boolean
  anchorLeft?: number
  anchorTop?: number
  initialName?: string
}>(), {
  visible: false,
  anchorLeft: undefined,
  anchorTop: undefined,
  initialName: ''
})

const emit = defineEmits<{
  insert: [payload: { name?: string; url: string }]
  close: []
}>()

const name = ref(props.initialName || '')
const url = ref('')

watch(() => props.visible, (v) => {
  if (v) {
    // reset url, keep name from initialName
    name.value = props.initialName || ''
    url.value = ''
    // focus handling could be added
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

const confirm = () => {
  if (!url.value) return
  emit('insert', { name: name.value || '', url: url.value })
}

const close = () => {
  emit('close')
}
</script>

<style scoped>
.toolbar-link-dialog {
  position: fixed;
  width: 320px;
  background: #fff;
  border: 1px solid #e6e9ef;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  padding: 8px;
  z-index: 2000;
}
.link-header { display:flex; justify-content:space-between; align-items:center; padding:6px 8px; border-bottom:1px solid #f3f4f6; }
.link-title { font-size:14px; color:#333; }
.link-close { cursor:pointer; padding:4px 6px; }
.link-body { padding:8px; display:flex; flex-direction:column; gap:8px; }
.field { display:flex; flex-direction:column; gap:6px; }
.label { font-size:12px; color:#666; }
.input { width:100%; padding:8px; border-radius:4px; border:1px solid #e6eef3; }
.link-footer { display:flex; justify-content:flex-end; gap:8px; padding-top:8px; }
.btn { padding:6px 12px; border-radius:4px; border:1px solid transparent; cursor:pointer; }
.btn-cancel { background:#fff; border-color:#e6eef3; }
.btn-confirm { background:#2d8cf0; color:#fff; border-color:#2d8cf0; }
</style>

