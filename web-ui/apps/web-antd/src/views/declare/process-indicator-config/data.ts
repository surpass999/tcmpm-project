import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { getDictOptions } from '@vben/hooks';

import { ProcessIndicatorConfigApi } from '#/api/declare/process-indicator-config';

/** 过程类型选项 */
const PROCESS_TYPE_OPTIONS = [
  { label: '建设过程', value: 1 },
  { label: '半年报', value: 2 },
  { label: '年度总结', value: 3 },
  { label: '中期评估', value: 4 },
  { label: '整改记录', value: 5 },
  { label: '验收申请', value: 6 },
];

/** 项目类型选项 */
const PROJECT_TYPE_OPTIONS = [
  { label: '通用类型', value: 0 },
  { label: '综合型', value: 1 },
  { label: '中医电子病历型', value: 2 },
  { label: '智慧中药房型', value: 3 },
  { label: '名老中医传承型', value: 4 },
  { label: '中医临床科研型', value: 5 },
  { label: '中医智慧医共体型', value: 6 },
];

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'processType',
      label: '过程类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择过程类型',
        allowClear: true,
        options: PROCESS_TYPE_OPTIONS,
      },
    },
    {
      fieldName: 'projectType',
      label: '项目类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择项目类型',
        allowClear: true,
        options: PROJECT_TYPE_OPTIONS,
      },
    },
    {
      fieldName: 'indicatorName',
      label: '指标名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入指标名称',
        allowClear: true,
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions<ProcessIndicatorConfigApi.ConfigResp>['columns'] {
  return [
    {
      field: 'id',
      title: '编号',
      minWidth: 80,
    },
    {
      field: 'processTypeName',
      title: '过程类型',
      minWidth: 100,
    },
    {
      field: 'projectTypeName',
      title: '项目类型',
      minWidth: 120,
    },
    {
      field: 'indicatorCode',
      title: '指标代号',
      minWidth: 100,
    },
    {
      field: 'indicatorName',
      title: '指标名称',
      minWidth: 180,
    },
    {
      field: 'unit',
      title: '单位',
      minWidth: 80,
    },
    {
      field: 'isRequired',
      title: '必填',
      minWidth: 80,
      cellRender: {
        name: 'CellTag',
        props: {
          map: [
            { label: '是', value: true, color: 'red' },
            { label: '否', value: false, color: 'green' },
          ],
        },
      },
    },
    {
      field: 'sort',
      title: '排序',
      minWidth: 80,
    },
    {
      field: 'createTime',
      title: '创建时间',
      minWidth: 180,
      formatter: 'formatDateTime',
    },
  ];
}
