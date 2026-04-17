# VbenForm 动态 Schema 与数据回显实践

## 背景

在从 `IndicatorInputTable` 重构到 `IndicatorInputForm` 的过程中，遇到了一系列与 VbenForm 动态 Schema 更新和数据回显相关的错误，本文档记录踩坑过程和最终方案。

## 问题一：computed 未定义

### 错误信息

```
index.vue:364 Uncaught (in promise) ReferenceError: computed is not defined
    at setup (index.vue:364:20)
```

### 原因

从 `<script setup>` 的自动导入语法改为显式 `import` 语法时，漏掉了 `computed` 的导入：

```javascript
// ❌ 错误写法
import { ref, reactive, watch, onMounted, nextTick } from 'vue';

// ✅ 正确写法
import { ref, reactive, watch, onMounted, nextTick, computed } from 'vue';
```

`<script setup>` 会自动解析模板中使用的 `computed`，但显式导入时必须手动声明所有用到的 API。

### 经验总结

重构代码时，如果从 `<script setup>` 改为显式导入，或从其他文件复制代码过来，务必检查所有用到的 Vue API 是否都已导入。常见遗漏：`computed`、`watch`、`watchEffect`、`markRaw`、`toRaw`、`isRef`、`unref` 等。

---

## 问题二：Schema 变更后数据回显为空

### 表现

表单打开后，`props.form.values` 为空对象 `{}`，但后端数据确实存在。

### 根本原因

VbenForm 的 schema 初始为空数组，数据加载后通过 `formConfig.schema = formSchema.value` 更新。但此时 Form 组件已挂载，Schema 的变更不会自动触发 vee-validate Field 的重新挂载，导致回显失败。

### 解决方案：reactive + computed 响应式方案

```javascript
import { ref, reactive, computed, nextTick } from 'vue';

const [Form, formApi] = useVbenForm(formConfig);

// ✅ 方案：将 formConfig 改为响应式对象
const formConfig = reactive({
  showDefaultActions: false,
  layout: 'vertical' as const,
  schema: [] as VbenFormSchema[],  // 初始为空
});

const [Form, formApi] = useVbenForm(formConfig);

// ✅ schema 用 computed 派生
const formSchema = computed<VbenFormSchema[]>(() => {
  return getIndicatorsInDisplayOrder().map((ind) => buildSchemaForIndicator(ind));
});

// ✅ 在数据加载后更新 schema
async function doLoadData() {
  await loadIndicatorData(...);

  // 关键：将 computed 的结果赋值给 reactive schema
  formConfig.schema = formSchema.value;

  // 等待 Form 渲染完成
  await nextTick();
  await waitForFormMounted();

  // 回显数据
  if (formApi.form?.setValues) {
    await formApi.form.setValues(formValues);
  } else {
    await formApi.setValues(formValues, false);
  }
}
```

### 关键点

1. **`reactive` 包装**：`formConfig` 必须用 `reactive()` 包装，这样赋值 `formConfig.schema = [...]` 才会触发 Vue 的响应式更新。
2. **`computed` 派生 schema**：schema 本身用 `computed` 派生，保证它依赖的所有数据变化时 schema 自动更新。
3. **`formApi.form?.setValues`**：优先使用 vee-validate 原生的 `setValues` 方法，回显更可靠。
4. **多次 `nextTick`**：数据回显后多等待几个 tick，确保 reactive flush 完成。
5. **`formApi.isMounted`**：用 `formApi.isMounted` 判断表单是否已完全挂载，再执行回显。

---

## 相关文件

- `web-ui/apps/web-antd/src/views/declare/progress-report/components/IndicatorInputForm/index.vue` — 主组件
- `web-ui/apps/web-antd/src/views/declare/progress-report/components/IndicatorInputForm/adapter/form.ts` — 表单适配器
- `web-ui/packages/@core/ui-kit/form-ui/src/form-render/form.vue` — Form 渲染层
- `web-ui/packages/@core/ui-kit/form-ui/src/form-render/form-field.vue` — Field 渲染层

---

## 附录：form.vue 中的调试日志

在排查过程中，添加以下日志有助于定位问题：

```javascript
// schema 变更时
watch(
  () => formConfig.schema,
  (newSchema) => {
    console.log('[form.vue] schema 变更, 前3个:', newSchema.slice(0, 3));
  }
);

// props.form 初始值
onMounted(() => {
  console.log('[form.vue] props.form:', props.form);
  console.log('[form.vue] props.form.values:', props.form?.values);
});

// 是否编辑模式判断
const editMode = computed(() => {
  console.log('[form.vue] [是否编辑模式]', props.mode);
  return props.mode === 'edit' || props.mode === 'DRAFT';
});
```
