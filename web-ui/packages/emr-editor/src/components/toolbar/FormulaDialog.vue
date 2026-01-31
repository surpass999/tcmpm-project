<template>
  <teleport to="body">
    <div class="toolbar-formula-dialog" @click.stop :style="stylePosition" v-if="visible">
      <div class="formula-header">
        <div class="formula-title">插入公式 (LaTeX)</div>
        <div class="formula-close" @click="close">×</div>
      </div>

      <div class="formula-body">
        <label class="field">
          <div class="label">LaTeX 内容</div>
          <textarea v-model="latex" class="textarea" placeholder="例如：\\frac{a}{b}"></textarea>
        </label>
        <label class="field-inline">
          <div class="label">字号</div>
          <input v-model.number="size" type="number" class="input-size" min="8" max="72" />
        </label>
      </div>

      <div class="formula-footer">
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
  initialLatex?: string
  initialSize?: number
}>(), {
  visible: false,
  anchorLeft: undefined,
  anchorTop: undefined,
  initialLatex: '',
  initialSize: 16
})

const emit = defineEmits<{
  insert: [payload: { latex: string; size?: number }]
  close: []
}>()

const latex = ref(props.initialLatex || '')
const size = ref<number>(props.initialSize || 16)

watch(() => props.visible, (v) => {
  if (v) {
    latex.value = props.initialLatex || ''
    size.value = props.initialSize || 16
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
  if (!latex.value) return
  emit('insert', { latex: latex.value, size: Number(size.value || 16) })
}

const close = () => {
  emit('close')
}
</script>

<style scoped>
.toolbar-formula-dialog {
  position: fixed;
  width: 360px;
  background: #fff;
  border: 1px solid #e6e9ef;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  padding: 8px;
  z-index: 2000;
}
.formula-header { display:flex; justify-content:space-between; align-items:center; padding:6px 8px; border-bottom:1px solid #f3f4f6; }
.formula-title { font-size:14px; color:#333; }
.formula-close { cursor:pointer; padding:4px 6px; }
.formula-body { padding:8px; display:flex; flex-direction:column; gap:8px; }
.field { display:flex; flex-direction:column; gap:6px; }
.field-inline { display:flex; align-items:center; gap:8px; }
.label { font-size:12px; color:#666; }
.textarea { width:100%; min-height:80px; padding:8px; border-radius:4px; border:1px solid #e6eef3; font-family: monospace; }
.input-size { width:72px; padding:6px; border-radius:4px; border:1px solid #e6eef3; }
.formula-footer { display:flex; justify-content:flex-end; gap:8px; padding-top:8px; }
.btn { padding:6px 12px; border-radius:4px; border:1px solid transparent; cursor:pointer; }
.btn-cancel { background:#fff; border-color:#e6eef3; }
.btn-confirm { background:#2d8cf0; color:#fff; border-color:#2d8cf0; }
</style>

