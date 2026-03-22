<template>
  <teleport to="body">
    <div class="toolbar-watermark-dialog" @click.stop :style="stylePosition" v-if="visible">
      <div class="watermark-header">
        <div class="watermark-title">添加水印</div>
        <div class="watermark-close" @click="close">×</div>
      </div>

      <div class="watermark-body">
        <label class="field">
          <div class="label">文本</div>
          <input v-model="text" class="input" placeholder="水印文本" />
        </label>
    <label class="field-inline">
      <div class="label">透明度</div>
      <input v-model.number="opacity" type="number" min="0" max="1" step="0.01" class="input-small" />
    </label>
    <label class="field-inline">
      <div class="label">字体大小</div>
      <input v-model.number="size" type="number" min="8" max="400" class="input-small" />
    </label>
    <label class="field-inline">
      <div class="label">颜色</div>
      <input v-model="color" type="color" class="input-small" />
    </label>
    <label class="field-inline" style="align-items:center;">
      <div class="label">重复</div>
      <input type="checkbox" v-model="repeat" />
    </label>
    <label class="field-inline">
      <div class="label">水平间隔</div>
      <input v-model.number="gapH" type="number" min="0" class="input-small" />
    </label>
    <label class="field-inline">
      <div class="label">垂直间隔</div>
      <input v-model.number="gapV" type="number" min="0" class="input-small" />
    </label>
      </div>

      <div class="watermark-footer">
        <button class="btn btn-cancel" @click="close">取消</button>
        <button class="btn btn-confirm" @click="confirm">添加</button>
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
  initialText?: string
}>(), {
  visible: false,
  anchorLeft: undefined,
  anchorTop: undefined,
  initialText: ''
})

const emit = defineEmits<{
  insert: [payload: { text: string; opacity?: number; size?: number; color?: string }]
  close: []
}>()

const text = ref(props.initialText || '')
const opacity = ref<number>(0.12)
const size = ref<number>(48)
const color = ref<string>('#000000')
const repeat = ref<boolean>(false)
const gapH = ref<number>(10)
const gapV = ref<number>(10)

watch(() => props.visible, (v) => {
  if (v) {
    text.value = props.initialText || ''
    opacity.value = 0.12
    size.value = 48
    color.value = '#000000'
    repeat.value = false
    gapH.value = 10
    gapV.value = 10
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
  if (!text.value) return
  emit('insert', { text: String(text.value), opacity: Number(opacity.value), size: Number(size.value), color: String(color.value), repeat: Boolean(repeat.value), gap: [Number(gapH.value), Number(gapV.value)] })
}

const close = () => {
  emit('close')
}
</script>

<style scoped>
.toolbar-watermark-dialog { position: fixed; width: 360px; background:#fff; border:1px solid #e6e9ef; border-radius:6px; box-shadow:0 8px 24px rgba(0,0,0,0.12); padding:8px; z-index:2000; }
.watermark-header{ display:flex; justify-content:space-between; align-items:center; padding:6px 8px; border-bottom:1px solid #f3f4f6; }
.watermark-title{ font-size:14px; color:#333; }
.watermark-close{ cursor:pointer; padding:4px 6px; }
.watermark-body{ padding:8px; display:flex; flex-direction:column; gap:8px; }
.field{ display:flex; flex-direction:column; gap:6px; }
.field-inline{ display:flex; gap:8px; align-items:center; height: 24px; margin:5px 0px; }
.label{ width: 30%; font-size:12px; color:#666; }
.input{ width:100%; padding:8px; border-radius:4px; border:1px solid #e6eef3; }
.input-small{ width:120px; padding:6px; border-radius:4px; border:1px solid #e6eef3; }
.watermark-footer{ display:flex; justify-content:flex-end; gap:8px; padding-top:8px; }
.btn{ padding:6px 12px; border-radius:4px; border:1px solid transparent; cursor:pointer; }
.btn-cancel{ background:#fff; border-color:#e6eef3; }
.btn-confirm{ background:#2d8cf0; color:#fff; border-color:#2d8cf0; }
</style>

