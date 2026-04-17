import type { FormRenderProps } from '../types';
import type { FormApi } from '../form-api';

import { computed } from 'vue';

import { createContext } from '@vben-core/shadcn-ui';

export const [injectRenderFormProps, provideFormRenderProps] =
  createContext<FormRenderProps>('FormRenderProps');

export const [injectFormApi, provideFormApi] =
  createContext<FormApi>('FormApi');

export const useFormContext = () => {
  const formRenderProps = injectRenderFormProps();

  const isVertical = computed(() => formRenderProps.layout === 'vertical');

  const componentMap = computed(() => formRenderProps.componentMap);
  const componentBindEventMap = computed(
    () => formRenderProps.componentBindEventMap,
  );
  return {
    componentBindEventMap,
    componentMap,
    isVertical,
  };
};
