import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { DICT_TYPE } from '@vben/constants';

import { z } from '#/adapter/form';

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'reportYear',
      label: '填报年度',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入填报年度',
        min: 2020,
        max: 2099,
      },
    },
    {
      fieldName: 'reportBatch',
      label: '填报批次',
      component: 'Select',
      componentProps: {
        placeholder: '请选择填报批次',
        options: [
          { label: '第一批', value: 1 },
          { label: '第二批', value: 2 },
          { label: '第三批', value: 3 },
          { label: '第四批', value: 4 },
        ],
        allowClear: true,
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        placeholder: '请选择状态',
        options: [
          { label: '启用', value: 1 },
          { label: '禁用', value: 0 },
        ],
        allowClear: true,
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', width: 60, title: '序号' },
    {
      field: 'reportYear',
      title: '填报年度',
      minWidth: 100,
    },
    {
      field: 'reportBatch',
      title: '填报批次',
      minWidth: 100,
      formatter: ({ row }) => `第${row.reportBatch}批`,
    },
    {
      field: 'windowStart',
      title: '开始时间',
      minWidth: 180,
      formatter: 'formatDateTime',
    },
    {
      field: 'windowEnd',
      title: '结束时间',
      minWidth: 180,
      formatter: 'formatDateTime',
    },
    {
      field: 'remark',
      title: '备注',
      minWidth: 200,
    },
    {
      field: 'status',
      title: '状态',
      minWidth: 80,
      align: 'center',
      cellRender: {
        name: 'CellTagMap',
        props: {
          map: [
            { label: '启用', value: 1, color: 'green' },
            { label: '禁用', value: 0, color: 'red' },
          ],
        },
      },
    },
    {
      title: '操作',
      width: 150,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

/** 新增/修改的表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'id',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'reportYear',
      label: '填报年度',
      component: 'DatePicker',
      componentProps: {
        placeholder: '请选择填报年度',
        picker: 'year',
        format: 'YYYY',
        valueFormat: 'YYYY',
        style: { width: '100%' },
      },
      rules: z.string().min(1, '请选择填报年度'),
    },
    {
      fieldName: 'reportBatch',
      label: '填报批次',
      component: 'Select',
      componentProps: {
        placeholder: '请选择填报批次',
        options: [
          { label: '第一批', value: 1 },
          { label: '第二批', value: 2 },
          { label: '第三批', value: 3 },
          { label: '第四批', value: 4 },
        ],
        style: { width: '100%' },
      },
      rules: z.number().min(1, '请选择填报批次'),
    },
    {
      fieldName: 'windowStart',
      label: '开始时间',
      component: 'DatePicker',
      componentProps: {
        placeholder: '请选择开始时间',
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
        style: { width: '100%' },
      },
      rules: z.string().min(1, '请选择开始时间'),
    },
    {
      fieldName: 'windowEnd',
      label: '结束时间',
      component: 'DatePicker',
      componentProps: {
        placeholder: '请选择结束时间',
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
        style: { width: '100%' },
      },
      rules: z.string().min(1, '请选择结束时间'),
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      defaultValue: 1,
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '禁用', value: 0 },
        ],
      },
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入备注说明',
        rows: 3,
      },
    },
  ];
}
