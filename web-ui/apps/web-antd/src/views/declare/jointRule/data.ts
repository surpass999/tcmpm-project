import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { getProjectTypeSimpleList } from '#/api/declare/project-type';

import {
  TRIGGER_TIMING_OPTIONS,
} from './types';

// 项目类型选项（从 API 加载）
let PROJECT_TYPE_OPTIONS: { label: string; value: number }[] = [];

// 加载项目类型选项（同步方式，在模块初始化时加载）
getProjectTypeSimpleList()
  .then((list) => {
    PROJECT_TYPE_OPTIONS = list.map((item) => ({
      label: item.title || item.name,
      value: item.typeValue,
    }));
  })
  .catch(() => {
    PROJECT_TYPE_OPTIONS = [];
  });

/** 获取项目类型选项 */
export function getProjectTypeOptions() {
  return PROJECT_TYPE_OPTIONS;
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'ruleName',
      label: '规则名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入规则名称',
        allowClear: true,
      },
    },
    {
      fieldName: 'projectType',
      label: '项目类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择项目类型',
        allowClear: true,
        options: getProjectTypeOptions(),
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        placeholder: '请选择状态',
        allowClear: true,
        options: [
          { label: '禁用', value: 0 },
          { label: '启用', value: 1 },
        ],
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  // 项目类型格式化
  const projectTypeFormatter = ({ cellValue }: { cellValue: any }) => {
    const option = PROJECT_TYPE_OPTIONS.find((item) => item.value === cellValue);
    return option?.label || cellValue || '-';
  };

  // 触发时机格式化
  const triggerTimingFormatter = ({ cellValue }: { cellValue: any }) => {
    const option = TRIGGER_TIMING_OPTIONS.find((o) => o.value === cellValue);
    return option?.label || cellValue;
  };

  // 状态格式化
  const statusFormatter = ({ cellValue }: { cellValue: any }) => {
    return cellValue === 1 ? '启用' : '禁用';
  };

  return [
    {
      field: 'id',
      title: '编号',
      minWidth: 80,
      sortable: true,
    },
    {
      field: 'ruleName',
      title: '规则名称',
      minWidth: 150,
    },
    {
      field: 'projectType',
      title: '适用项目类型',
      minWidth: 120,
      formatter: projectTypeFormatter,
    },
    {
      field: 'triggerTiming',
      title: '触发时机',
      minWidth: 120,
      formatter: triggerTimingFormatter,
    },
    {
      field: 'status',
      title: '状态',
      minWidth: 80,
      formatter: statusFormatter,
    },
    {
      field: 'createTime',
      title: '创建时间',
      minWidth: 180,
      formatter: 'formatDateTime',
    },
    {
      title: '操作',
      width: 180,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

/** 弹窗表单配置 */
export function useModalFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'ruleName',
      label: '规则名称',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入规则名称',
      },
    },
    {
      fieldName: 'projectType',
      label: '适用项目类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择适用项目类型',
        allowClear: true,
        options: [
          { label: '全部', value: 0 },
          ...getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number'),
        ],
      },
    },
    {
      fieldName: 'triggerTiming',
      label: '触发时机',
      component: 'Select',
      componentProps: {
        placeholder: '请选择触发时机',
        options: TRIGGER_TIMING_OPTIONS,
      },
    },
    {
      fieldName: 'processNode',
      label: '流程节点',
      component: 'Input',
      componentProps: {
        placeholder: '请输入流程节点（可选）',
      },
    },
    {
      fieldName: 'ruleConfig',
      label: '规则配置',
      component: 'Textarea',
      componentProps: {
        placeholder: 'JSON格式的规则配置',
        rows: 3,
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      defaultValue: 1,
      componentProps: {
        options: [
          { label: '禁用', value: 0 },
          { label: '启用', value: 1 },
        ],
      },
    },
  ];
}
