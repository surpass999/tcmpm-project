<template>
  <div class="toolbar-group" :class="groupClasses">
    <div class="toolbar-items">
      <slot>
        <ToolbarItem
          v-for="item in items"
          :key="item.command"
          :type="item.type"
          :command="item.command"
          :icon="item.icon"
          :text="item.text"
          :title="item.title"
          :active="item.active"
          :disabled="item.disabled"
          :dropdown-options="item.dropdownOptions"
          :current-value="item.currentValue"
          :display-text="item.displayText"
          @click="handleItemClick"
          @select="handleItemSelect"
        />
      </slot>
    </div>

    <!-- 分组分隔符 -->
    <div v-if="showDivider" class="toolbar-divider"></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ToolbarItem from './ToolbarItem.vue'
import type { ToolbarItemProps } from './ToolbarItem.vue'

export interface ToolbarGroupProps {
  key: string
  title?: string
  items?: ToolbarItemProps[]
  showDivider?: boolean
  layout?: 'horizontal' | 'vertical'
}

const props = withDefaults(defineProps<ToolbarGroupProps>(), {
  showDivider: true,
  layout: 'horizontal'
})

const emit = defineEmits<{
  itemClick: [command: string, groupKey: string, target?: HTMLElement]
  itemSelect: [command: string, value: string | number, groupKey: string]
}>()

// 计算样式类名
const groupClasses = computed(() => {
  return [
    'toolbar-group',
    `toolbar-group--${props.key}`,
    `toolbar-group--${props.layout}`
  ]
})

// 工具栏项点击
const handleItemClick = (command: string, target?: HTMLElement) => {
  emit('itemClick', command, props.key, target)
}

// 工具栏项选择
const handleItemSelect = (command: string, value: string | number) => {
  emit('itemSelect', command, value, props.key)
}
</script>

<style scoped>
.toolbar-group {
  display: flex;
  align-items: center;
  margin: 0 4px;
}

.toolbar-group--horizontal {
  flex-direction: row;
}

.toolbar-group--vertical {
  flex-direction: column;
}

.toolbar-items {
  display: flex;
  align-items: center;
  gap: 2px;
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background-color: #ddd;
  margin: 0 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .toolbar-group {
    margin: 0 2px;
  }

  .toolbar-divider {
    margin: 0 4px;
  }
}
</style>
