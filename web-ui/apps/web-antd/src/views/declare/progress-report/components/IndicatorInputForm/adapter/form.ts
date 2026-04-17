/**
 * IndicatorInputForm 表单适配器
 * 基于 VBenForm 规范，为 IndicatorInputForm 提供表单能力
 */

import type { VbenFormSchema as FormSchema, VbenFormProps } from '@vben-core/form-ui';

import type { ComponentType } from './component';

import { setupVbenForm, useVbenForm as useForm, z } from '@vben-core/form-ui';
import { $t } from '@vben/locales';
import { isMobile } from '@vben/utils';

import { initComponentAdapter } from './component';

export type { ComponentType };

async function initSetupVbenForm() {
  console.log('[initSetupVbenForm] 开始执行...');
  await initComponentAdapter();
  console.log('[initSetupVbenForm] initComponentAdapter 完成');

  setupVbenForm<ComponentType>({
    config: {
      emptyStateValue: null,
      baseModelPropName: 'value', // 默认所有组件使用 value/update:value
      modelPropNameMap: {
        // antd 组件使用 modelValue/update:modelValue
        Input: 'modelValue',
        Select: 'modelValue',
        Textarea: 'modelValue',
        DatePicker: 'modelValue',
        TimePicker: 'modelValue',
        RangePicker: 'modelValue',
        TimeRangePicker: 'modelValue',
        Cascader: 'modelValue',
        TreeSelect: 'modelValue',
        RadioGroup: 'modelValue',
        CheckboxGroup: 'modelValue',
        AutoComplete: 'modelValue',
        InputNumber: 'modelValue',
        Switch: 'modelValue',
        Rate: 'modelValue',
        Slider: 'modelValue',
        Upload: 'modelValue',
        // Checkbox/Radio 使用 checked 而非 value
        Checkbox: 'checked',
        Radio: 'checked',
      },
    },
    defineRules: {
      required: (value: any, _params: any, ctx: any) => {
        if (value === undefined || value === null || (typeof value === 'string' && value.length === 0) || (Array.isArray(value) && value.length === 0)) {
          return $t('ui.formRules.required', [ctx.label]);
        }
        return true;
      },
      selectRequired: (value: any, _params: any, ctx: any) => {
        if (value === undefined || value === null) {
          return $t('ui.formRules.selectRequired', [ctx.label]);
        }
        return true;
      },
      mobile: (value: any, _params: any, ctx: any) => {
        if (value === undefined || value === null || value.length === 0) {
          return true;
        }
        if (!isMobile(value)) {
          return $t('ui.formRules.mobile', [ctx.label]);
        }
        return true;
      },
      mobileRequired: (value: any, _params: any, ctx: any) => {
        if (value === undefined || value === null || value.length === 0) {
          return $t('ui.formRules.required', [ctx.label]);
        }
        if (!isMobile(value)) {
          return $t('ui.formRules.mobile', [ctx.label]);
        }
        return true;
      },
    },
  });
}

const useVbenForm = useForm<ComponentType>;

export { initSetupVbenForm, useVbenForm, z };

export type VbenFormSchema = FormSchema<ComponentType>;
export type { VbenFormProps };
