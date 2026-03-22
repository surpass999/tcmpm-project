<script lang="ts" setup>
import type { Component } from 'vue';

import { markRaw, ref, watch } from 'vue';

import { CustomConfigMap } from './data';

defineOptions({ name: 'ElementCustomConfig' });

const props = defineProps({
  id: {
    type: String,
    default: '',
  },
  type: {
    type: String,
    default: '',
  },
  businessObject: {
    type: Object as () => BusinessObject,
    default: () => ({}),
  },
});

interface BusinessObject {
  eventDefinitions?: Array<{ $type: string }>;
  [key: string]: any;
}

// const bpmnInstances = () => (window as any)?.bpmnInstances;
const customConfigComponent = ref<Component | null>(null);

watch(
  () => props.businessObject,
  () => {
    if (props.type && props.businessObject) {
      let val = props.type;
      if (props.businessObject.eventDefinitions) {
        val +=
          props.businessObject.eventDefinitions[0]?.$type.split(':')[1] || '';
      }
      // @ts-ignore
      const component = (
        CustomConfigMap as Record<string, { component: Component }>
      )[val]?.component;
      // 使用 markRaw 避免 Vue 将组件标记为响应式对象
      customConfigComponent.value = component ? markRaw(component) : null;
    }
  },
  { immediate: true },
);
</script>

<template>
  <div class="panel-tab__content">
    <component :is="customConfigComponent" v-bind="$props" :key="props.id" />
  </div>
</template>

<style lang="scss" scoped></style>
