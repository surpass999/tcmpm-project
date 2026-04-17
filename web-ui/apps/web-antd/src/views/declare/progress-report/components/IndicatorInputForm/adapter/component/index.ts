/**
 * IndicatorInputForm 组件适配器
 * 为 VBenForm 注册自定义业务组件
 */

import type { BaseFormComponentType } from '@vben-core/form-ui';

import { defineComponent, h, ref } from 'vue';

import { IconPicker } from '@vben/common-ui';
import { globalShareState } from '@vben-core/shared/global-state';
import { $t } from '@vben/locales';

import {
  AutoComplete,
  Button,
  Cascader,
  Checkbox,
  CheckboxGroup,
  DatePicker,
  Divider,
  Input,
  InputNumber,
  InputPassword,
  Mentions,
  notification,
  Radio,
  RadioGroup,
  RangePicker,
  Rate,
  Select,
  Space,
  Switch,
  Textarea,
  TimePicker,
  TreeSelect,
  Upload,
} from 'ant-design-vue';

import { FileUpload, ImageUpload } from '#/components/upload';

import VbenNumberInput from '../../components/VbenNumberInput.vue';
import VbenSwitch from '../../components/VbenSwitch.vue';
import VbenRangePicker from '../../components/VbenRangePicker.vue';
import VbenMultiSelect from '../../components/VbenMultiSelect.vue';
import InputTypeRadioGroup from '../../components/InputTypeRadioGroup.vue';
import InputTypeCheckboxGroup from '../../components/InputTypeCheckboxGroup.vue';
import IndicatorContainer from '../../components/IndicatorContainer.vue';

import type { Recordable } from '@vben/types';

const withDefaultPlaceholder = <T extends object>(
  component: T,
  type: 'input' | 'select',
  componentProps: Recordable<any> = {},
) => {
  return defineComponent({
    name: (component as any).name,
    inheritAttrs: false,
    setup: (props: any, { attrs, expose, slots }) => {
      const placeholder =
        props?.placeholder ||
        attrs?.placeholder ||
        $t(`ui.placeholder.${type}`);
      const innerRef = ref();
      expose(
        new Proxy(
          {},
          {
            get: (_target: any, key: any) => innerRef.value?.[key],
            has: (_target: any, key: any) =>
              key in (innerRef.value || {}),
          },
        ),
      );
      return () =>
        h(component as any, { ...componentProps, placeholder, ...props, ...attrs, ref: innerRef }, slots);
    },
  });
};

export type ComponentType =
  | 'ApiCascader'
  | 'ApiSelect'
  | 'ApiTreeSelect'
  | 'AutoComplete'
  | 'Cascader'
  | 'Checkbox'
  | 'CheckboxGroup'
  | 'DatePicker'
  | 'DefaultButton'
  | 'Divider'
  | 'FileUpload'
  | 'IconPicker'
  | 'ImageUpload'
  | 'IndicatorContainer'
  | 'Input'
  | 'InputNumber'
  | 'InputPassword'
  | 'InputTypeCheckboxGroup'
  | 'InputTypeRadioGroup'
  | 'Mentions'
  | 'PrimaryButton'
  | 'Radio'
  | 'RadioGroup'
  | 'RangePicker'
  | 'Rate'
  | 'RichTextarea'
  | 'Select'
  | 'Space'
  | 'Switch'
  | 'Textarea'
  | 'TimePicker'
  | 'TimeRangePicker'
  | 'TreeSelect'
  | 'Upload'
  | 'VbenMultiSelect'
  | 'VbenNumberInput'
  | 'VbenRangePicker'
  | 'VbenSwitch'
  | BaseFormComponentType;

async function initComponentAdapter() {
  console.log('[initComponentAdapter] 开始执行...');

  const components: Partial<Record<ComponentType, any>> = {
    ApiCascader: withDefaultPlaceholder(
      (await import('@vben/common-ui')).ApiComponent,
      'select',
      {
        component: Cascader,
        fieldNames: { label: 'label', value: 'value', children: 'children' },
        loadingSlot: 'suffixIcon',
        modelPropName: 'value',
        visibleEvent: 'onVisibleChange',
      },
    ),
    ApiSelect: withDefaultPlaceholder(
      (await import('@vben/common-ui')).ApiComponent,
      'select',
      {
        component: Select,
        loadingSlot: 'suffixIcon',
        visibleEvent: 'onDropdownVisibleChange',
        modelPropName: 'value',
      },
    ),
    ApiTreeSelect: withDefaultPlaceholder(
      (await import('@vben/common-ui')).ApiComponent,
      'select',
      {
        component: TreeSelect,
        fieldNames: { label: 'label', value: 'value', children: 'children' },
        loadingSlot: 'suffixIcon',
        modelPropName: 'value',
        optionsPropName: 'treeData',
        visibleEvent: 'onVisibleChange',
      },
    ),
    AutoComplete,
    Cascader,
    Checkbox,
    CheckboxGroup,
    DatePicker,
    DefaultButton: (props: any, { attrs, slots }: any) =>
      h(Button, { ...props, ...attrs, type: 'default' }, slots),
    Divider,
    FileUpload,
    IconPicker: withDefaultPlaceholder(IconPicker, 'select', {
      iconSlot: 'addonAfter',
      inputComponent: Input,
      modelValueProp: 'value',
    }),
    ImageUpload,
    IndicatorContainer,
    Input: withDefaultPlaceholder(Input, 'input'),
    InputNumber: withDefaultPlaceholder(InputNumber, 'input'),
    InputPassword: withDefaultPlaceholder(InputPassword, 'input'),
    InputTypeCheckboxGroup,
    InputTypeRadioGroup,
    Mentions: withDefaultPlaceholder(Mentions, 'input'),
    PrimaryButton: (props: any, { attrs, slots }: any) =>
      h(Button, { ...props, ...attrs, type: 'primary' }, slots),
    Radio,
    RadioGroup,
    RangePicker,
    Rate,
    RichTextarea: (await import('#/components/tinymce')).Tinymce,
    Select: withDefaultPlaceholder(Select, 'select'),
    Space,
    Switch,
    Textarea: withDefaultPlaceholder(Textarea, 'input'),
    TimePicker,
    TimeRangePicker: withDefaultPlaceholder(
      (await import('ant-design-vue')).TimeRangePicker,
      'select',
    ),
    TreeSelect: withDefaultPlaceholder(TreeSelect, 'select'),
    Upload,
    VbenMultiSelect,
    VbenNumberInput,
    VbenRangePicker,
    VbenSwitch,
  };

  globalShareState.setComponents(components);

  globalShareState.defineMessage({
    copyPreferencesSuccess: (title: string, content?: string) => {
      notification.success({ description: content, message: title, placement: 'bottomRight' });
    },
  });
}

export { initComponentAdapter };
