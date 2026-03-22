import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclarePolicyApi } from '#/api/declare/policy';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

import { getRangePickerDefaultProps } from '#/utils';

/** 状态选项 */
const POLICY_STATUS_OPTIONS = getDictOptions(DICT_TYPE.DECLARE_POLICY_STATUS, 'number');

/** 政策类型选项 */
const POLICY_TYPE_OPTIONS = getDictOptions(DICT_TYPE.DECLARE_POLICY_TYPE, 'number');

/** 项目类型选项 */
const PROJECT_TYPE_OPTIONS = getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number');

/** 目标范围选项 */
const TARGET_SCOPE_OPTIONS = getDictOptions(DICT_TYPE.DECLARE_TARGET_SCOPE, 'number');

/** 新增/修改的表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'policyTitle',
      label: '政策标题',
      rules: 'required',
      component: 'Input',
      componentProps: {
        placeholder: '请输入政策标题',
      },
    },
    {
      fieldName: 'policySummary',
      label: '政策摘要',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入政策摘要',
        rows: 3,
      },
    },
    {
      fieldName: 'policyType',
      label: '政策类型',
      rules: 'required',
      component: 'Select',
      componentProps: {
        options: POLICY_TYPE_OPTIONS,
        placeholder: '请选择政策类型',
      },
    },
    {
      fieldName: 'releaseDept',
      label: '发布单位',
      rules: 'required',
      component: 'Input',
      componentProps: {
        placeholder: '请输入发布单位',
      },
    },
    {
      fieldName: 'targetScope',
      label: '目标范围',
      rules: 'required',
      component: 'Select',
      componentProps: {
        options: TARGET_SCOPE_OPTIONS,
        placeholder: '请选择目标范围',
      },
    },
    {
      fieldName: 'targetProjectTypes',
      label: '适用项目类型',
      component: 'Select',
      componentProps: {
        mode: 'multiple',
        options: PROJECT_TYPE_OPTIONS,
        placeholder: '请选择适用项目类型（可多选）',
      },
    },
    {
      fieldName: 'policyContent',
      label: '政策正文',
      rules: 'required',
      component: 'RichTextarea',
      componentProps: {
        height: 360,
      },
    },
  ];
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'policyTitle',
      label: '政策标题',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入政策标题',
      },
    },
    {
      fieldName: 'policyType',
      label: '政策类型',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: POLICY_TYPE_OPTIONS,
        placeholder: '请选择政策类型',
      },
    },
    {
      fieldName: 'targetProjectTypes',
      label: '适用项目类型',
      component: 'Select',
      componentProps: {
        allowClear: true,
        mode: 'multiple',
        options: PROJECT_TYPE_OPTIONS,
        placeholder: '请选择适用项目类型',
      },
    },
    {
      fieldName: 'releaseDept',
      label: '发布单位',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入发布单位',
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: POLICY_STATUS_OPTIONS,
        placeholder: '请选择状态',
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions<DeclarePolicyApi.Policy>['columns'] {
  return [
    { field: 'id', title: '编号', width: 80 },
    { type: 'checkbox', width: 50 },
    {
      field: 'policyTitle',
      title: '政策标题',
      minWidth: 300,
      slots: { default: 'policyTitle_default' },
    },
    {
      field: 'policyType',
      title: '类型',
      width: 100,
      slots: { default: 'policyType_default' },
    },
    {
      field: 'targetProjectTypes',
      title: '适用项目类型',
      width: 180,
      slots: { default: 'targetProjectTypes_default' },
    },
    {
      field: 'targetScope',
      title: '目标范围',
      width: 100,
      slots: { default: 'targetScope_default' },
    },
    {
      field: 'status',
      title: '状态',
      width: 100,
      slots: { default: 'status_default' },
    },
    { field: 'publisherName', title: '发布人', width: 100 },
    { field: 'createTime', title: '创建时间', width: 160, formatter: 'formatDateTime' },
    {
      title: '操作',
      width: 200,
      fixed: 'right',
      slots: { default: 'actions_default' },
    },
  ];
}

/** 获取政策类型标签 */
export function getPolicyTypeLabel(type: number) {
  const item = POLICY_TYPE_OPTIONS.find((opt) => opt.value === type);
  return item?.label || '-';
}

/** 获取目标范围标签 */
export function getTargetScopeLabel(scope: number) {
  const item = TARGET_SCOPE_OPTIONS.find((opt) => opt.value === scope);
  return item?.label || '-';
}

/** 获取适用项目类型标签 */
export function getTargetProjectTypesLabel(targetProjectTypes: string) {
  if (!targetProjectTypes) return '-';
  const types = targetProjectTypes.split(',');
  const labels = types.map((type) => {
    const item = PROJECT_TYPE_OPTIONS.find((opt) => opt.value === Number(type));
    return item?.label || type;
  });
  return labels.join('、');
}

/** 获取状态标签 */
export function getStatusLabel(status: number) {
  const item = POLICY_STATUS_OPTIONS.find((opt) => opt.value === status);
  return item?.label || '-';
}

/** 获取状态颜色 */
export function getStatusColor(status: number) {
  const item = POLICY_STATUS_OPTIONS.find((opt) => opt.value === status);
  return item?.color || 'default';
}
