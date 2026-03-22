<template>
  <teleport to="body">
    <div class="toolbar-codeblock-dialog" @click.stop :style="stylePosition" v-if="visible">
      <div class="codeblock-header">
        <div class="codeblock-title">插入代码块</div>
        <div class="codeblock-close" @click="close">×</div>
      </div>

      <div class="codeblock-body">
        <label class="field">
          <div class="label">语言</div>
          <select v-model="language" class="select">
            <option value="javascript">JavaScript</option>
            <option value="typescript">TypeScript</option>
            <option value="python">Python</option>
            <option value="java">Java</option>
            <option value="plain">Plain Text</option>
          </select>
        </label>
        <label class="field">
          <div class="label">代码</div>
          <textarea v-model="code" class="textarea" placeholder="请输入代码"></textarea>
        </label>
      </div>

      <div class="codeblock-footer">
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
  initialCode?: string
  initialLanguage?: string
}>(), {
  visible: false,
  anchorLeft: undefined,
  anchorTop: undefined,
  initialCode: '',
  initialLanguage: 'javascript'
})

const emit = defineEmits<{
  insert: [payload: { language: string; code: string }]
  close: []
}>()

const code = ref(props.initialCode || '')
const language = ref(props.initialLanguage || 'javascript')

watch(() => props.visible, (v) => {
  if (v) {
    code.value = props.initialCode || ''
    language.value = props.initialLanguage || 'javascript'
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
  if (!code.value) return
  emit('insert', { language: String(language.value || 'plain'), code: String(code.value) })
}

const close = () => {
  emit('close')
}
</script>

<style scoped>
.toolbar-codeblock-dialog { position: fixed; width: 420px; background:#fff; border:1px solid #e6e9ef; border-radius:6px; box-shadow:0 8px 24px rgba(0,0,0,0.12); padding:8px; z-index:2000; }
.codeblock-header { display:flex; justify-content:space-between; align-items:center; padding:6px 8px; border-bottom:1px solid #f3f4f6; }
.codeblock-title{ font-size:14px; color:#333; }
.codeblock-close{ cursor:pointer; padding:4px 6px; }
.codeblock-body{ padding:8px; display:flex; flex-direction:column; gap:8px; }
.field{ display:flex; flex-direction:column; gap:6px; }
.label{ font-size:12px; color:#666; }
.select{ padding:8px; border-radius:4px; border:1px solid #e6eef3; width:160px; }
.textarea{ width:100%; min-height:120px; padding:8px; border-radius:4px; border:1px solid #e6eef3; font-family: monospace; }
.codeblock-footer{ display:flex; justify-content:flex-end; gap:8px; padding-top:8px; }
.btn{ padding:6px 12px; border-radius:4px; border:1px solid transparent; cursor:pointer; }
.btn-cancel{ background:#fff; border-color:#e6eef3; }
.btn-confirm{ background:#2d8cf0; color:#fff; border-color:#2d8cf0; }
</style>

