<template>
  <teleport to="body">
    <div class="toolbar-control-dialog" v-if="visible" :style="styleCenter" @click.stop>
      <div class="control-header">
        <div class="control-title">{{ title }}</div>
        <div class="control-close" @click="close">×</div>
      </div>

      <div class="control-body">
        <template v-if="type === 'text'">
          <label class="field"><div class="label">占位符</div><input v-model="fields.placeholder" class="input" /></label>
          <label class="field"><div class="label">默认值</div><input v-model="fields.value" class="input" /></label>
        </template>
        <template v-else-if="type === 'select'">
          <label class="field"><div class="label">占位符</div><input v-model="fields.placeholder" class="input" /></label>
          <label class="field"><div class="label">默认值</div><input v-model="fields.code" class="input" /></label>
          <label class="field"><div class="label">值集 (JSON)</div><textarea v-model="fields.valueSets" class="textarea" placeholder='[{"value":"有","code":"1"}]'></textarea></label>
        </template>
        <template v-else-if="type === 'checkbox' || type === 'radio'">
          <label class="field"><div class="label">默认值 (逗号分隔)</div><input v-model="fields.code" class="input" /></label>
          <label class="field"><div class="label">值集 (JSON)</div><textarea v-model="fields.valueSets" class="textarea" placeholder='[{"value":"有","code":"1"}]'></textarea></label>
        </template>
        <template v-else-if="type === 'date'">
          <label class="field"><div class="label">占位符</div><input v-model="fields.placeholder" class="input" /></label>
          <label class="field"><div class="label">默认值</div><input v-model="fields.value" class="input" /></label>
          <label class="field"><div class="label">格式</div><input v-model="fields.dateFormat" class="input" /></label>
        </template>
        <template v-else-if="type === 'number'">
          <label class="field"><div class="label">占位符</div><input v-model="fields.placeholder" class="input" /></label>
          <label class="field"><div class="label">默认值</div><input v-model="fields.value" class="input" /></label>
        </template>
      </div>

      <div class="control-footer">
        <div class="error" v-if="error">{{ error }}</div>
        <button class="btn btn-cancel" @click="close">取消</button>
        <button class="btn btn-confirm" @click="confirm">确定</button>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = withDefaults(defineProps<{
  visible?: boolean
  type?: string
  title?: string
  initialName?: string
  initialPayload?: any
}>(), {
  visible: false,
  type: 'text',
  title: '',
  initialName: '',
  initialPayload: null
})

const emit = defineEmits<{
  insert: [payload: any]
  close: []
}>()

const visible = ref(props.visible)
watch(() => props.visible, v => visible.value = !!v)

const type = computed(() => props.type || 'text')
const title = computed(() => props.title || (type.value === 'text' ? '文本控件' : type.value === 'select' ? '列举控件' : type.value === 'checkbox' ? '复选框控件' : type.value === 'radio' ? '单选控件' : type.value === 'date' ? '日期控件' : '数值控件'))

const fields = ref<any>({
  placeholder: props.initialName || '',
  value: '',
  code: '',
  valueSets: '',
  dateFormat: 'yyyy-MM-dd hh:mm:ss'
})

watch(() => props.initialPayload, (p) => {
  if (p && typeof p === 'object') {
    fields.value.placeholder = p.placeholder || p.placeholder === '' ? p.placeholder : (props.initialName || '')
    fields.value.value = p.value && Array.isArray(p.value) ? (p.value[0]?.value || '') : (p.value || '')
    fields.value.code = p.code || ''
    fields.value.valueSets = p.valueSets ? JSON.stringify(p.valueSets, null, 2) : ''
    fields.value.dateFormat = p.dateFormat || fields.value.dateFormat
  }
})

const error = ref('')

const styleCenter = computed(() => ({
  position: 'fixed',
  left: '50%',
  top: '50%',
  transform: 'translate(-50%, -50%)',
  zIndex: 2000
}))

const validateValueSets = (jsonStr: string) => {
  if (!jsonStr) return null
  try {
    const parsed = JSON.parse(jsonStr)
    if (!Array.isArray(parsed)) throw new Error('必须是数组')
    return parsed
  } catch (e) {
    error.value = '值集 JSON 解析失败: ' + (e as Error).message
    return null
  }
}

const confirm = () => {
  error.value = ''
  const t = props.type
  if (t === 'text') {
    if (!fields.value.placeholder) { error.value = '占位符不能为空'; return }
    emit('insert', { placeholder: fields.value.placeholder, value: fields.value.value, type: 'text' })
  } else if (t === 'select') {
    if (!fields.value.placeholder) { error.value = '占位符不能为空'; return }
    const vs = validateValueSets(fields.value.valueSets)
    if (vs === null) return
    emit('insert', { placeholder: fields.value.placeholder, code: fields.value.code, valueSets: vs, type: 'select' })
  } else if (t === 'checkbox' || t === 'radio') {
    const vs = validateValueSets(fields.value.valueSets)
    if (vs === null) return
    emit('insert', { code: fields.value.code, valueSets: vs, type: t })
  } else if (t === 'date') {
    if (!fields.value.placeholder) { error.value = '占位符不能为空'; return }
    emit('insert', { placeholder: fields.value.placeholder, value: fields.value.value, dateFormat: fields.value.dateFormat, type: 'date' })
  } else if (t === 'number') {
    if (!fields.value.placeholder) { error.value = '占位符不能为空'; return }
    emit('insert', { placeholder: fields.value.placeholder, value: fields.value.value, type: 'number' })
  }
  close()
}

const close = () => {
  emit('close')
}
</script>

<style scoped>
.toolbar-control-dialog { width: 520px; background: white; border: 1px solid #e6e9ef; border-radius:6px; box-shadow: 0 8px 24px rgba(0,0,0,0.12); padding:12px; }
.control-header{ display:flex; justify-content:space-between; align-items:center; padding-bottom:6px; border-bottom:1px solid #f3f4f6; }
.control-title{ font-size:14px; }
.control-close{ cursor:pointer; }
.control-body{ padding:8px 0; display:flex; flex-direction:column; gap:8px; }
.field{ display:flex; flex-direction:column; gap:6px; }
.label{ font-size:13px; color:#666; margin-bottom:6px; }
.input{ padding:8px; border:1px solid #e6eef3; border-radius:4px; }
.textarea{ min-height:100px; padding:8px; border:1px solid #e6eef3; border-radius:4px; font-family:monospace; }
.control-footer{ display:flex; justify-content:flex-end; gap:8px; align-items:center; padding-top:8px; }
.error{ color: #ff4d4f; margin-right:auto; }
.btn{ padding:6px 12px; border-radius:4px; cursor:pointer; }
.btn-cancel{ background:#fff; border:1px solid #e6eef3; }
.btn-confirm{ background:#2d8cf0; color:white; border:1px solid #2d8cf0; }
</style>

