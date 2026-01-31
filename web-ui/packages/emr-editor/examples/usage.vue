<template>
  <div class="emr-editor-demo">
    <h2>EMR 编辑器使用示例</h2>

    <!-- 医生端使用示例 -->
    <div class="demo-section">
      <h3>医生端 - 完整功能</h3>
      <EMREditor
        ref="doctorEditor"
        :data="editorData"
        :config="DOCTOR_CONFIG"
        :theme="'medical'"
        :height="500"
        @change="handleChange"
        @command="handleCommand"
        @ready="handleReady"
        @error="handleError"
      />
    </div>

    <!-- 护士端使用示例 -->
    <div class="demo-section">
      <h3>护士端 - 简化功能</h3>
      <EMREditor
        :data="editorData"
        :config="NURSE_CONFIG"
        :theme="'compact'"
        :height="400"
        @change="handleNurseChange"
      />
    </div>

    <!-- 管理员预览示例 -->
    <div class="demo-section">
      <h3>管理员预览 - 只读模式</h3>
      <EMREditor
        :data="editorData"
        :config="ADMIN_CONFIG"
        :theme="'document'"
        :height="300"
      />
    </div>

    <!-- 自定义配置示例 -->
    <div class="demo-section">
      <h3>自定义配置</h3>
      <div class="config-controls">
        <label>
          <input v-model="customConfig.toolbar.enabled" type="checkbox" />
          显示工具栏
        </label>
        <label>
          <input v-model="customConfig.toolbar.groups.controls" type="checkbox" />
          显示医疗控件
        </label>
        <label>
          <input v-model="customConfig.toolbar.groups.text" type="checkbox" />
          显示文本格式
        </label>
      </div>
      <EMREditor
        :data="editorData"
        :config="customConfig"
        :height="350"
        @change="handleCustomChange"
      />
    </div>

    <!-- 数据预览 -->
    <div class="data-preview">
      <h3>编辑器数据预览</h3>
      <pre>{{ JSON.stringify(editorData, null, 2) }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import EMREditor from '../src/components/EMREditor.vue'
import {
  DOCTOR_CONFIG,
  NURSE_CONFIG,
  ADMIN_CONFIG,
  createConfig
} from '../src/config'

// 编辑器数据
const editorData = reactive({
  main: [
    { value: '患者主诉：', size: 14, bold: true },
    { value: '\n发热三天，咳嗽五天。\n\n' },
    { value: '现病史：', size: 14, bold: true },
    { value: '\n患者于三天前无明显诱因出现发热，体温最高达39.5℃，伴有咳嗽，咳出白色粘痰，无胸痛、呼吸困难等。\n\n' },
    { value: '体格检查：', size: 14, bold: true },
    { value: '\nT：39.2℃，P：90次/分，R：20次/分，BP：120/80mmHg\n' }
  ]
})

// 自定义配置
const customConfig = reactive({
  toolbar: {
    enabled: true,
    groups: {
      basic: true,
      text: false,
      structure: false,
      insert: false,
      controls: true,
      view: false,
      advanced: false
    }
  },
  features: {
    undo: true,
    redo: true,
    controls: {
      text: true,
      select: true,
      checkbox: true,
      radio: true,
      date: true,
      number: true
    }
  }
})

// 编辑器引用
const doctorEditor = ref()

// 事件处理
const handleChange = (data: any) => {
  console.log('医生编辑器数据变化:', data)
  Object.assign(editorData, data)
}

const handleNurseChange = (data: any) => {
  console.log('护士编辑器数据变化:', data)
}

const handleCustomChange = (data: any) => {
  console.log('自定义编辑器数据变化:', data)
}

const handleCommand = (command: string, params?: any) => {
  console.log('执行命令:', command, params)
}

const handleReady = (editor: any) => {
  console.log('编辑器初始化完成:', editor)
}

const handleError = (error: Error) => {
  console.error('编辑器错误:', error)
}

// 使用配置生成器
const generatedConfig = createConfig({
  role: 'doctor',
  customFeatures: {
    print: false  // 禁用打印功能
  }
})
</script>

<style scoped>
.emr-editor-demo {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.demo-section {
  margin-bottom: 40px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
}

.demo-section h3 {
  margin-top: 0;
  color: #374151;
  border-bottom: 2px solid #1890ff;
  padding-bottom: 8px;
}

.config-controls {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
}

.config-controls label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  cursor: pointer;
}

.data-preview {
  margin-top: 40px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
}

.data-preview h3 {
  margin-top: 0;
  color: #374151;
}

.data-preview pre {
  background: #f6f8fa;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 12px;
  color: #24292f;
  max-height: 400px;
  overflow-y: auto;
}
</style>
