import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'businessType',
      label: '业务类型标识',
      component: 'Input',
      componentProps: {
        placeholder: '请输入业务类型标识',
        allowClear: true,
      },
    },
    {
      fieldName: 'businessName',
      label: '业务类型名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入业务类型名称',
        allowClear: true,
      },
    },
    {
      fieldName: 'processDefinitionKey',
      label: '流程定义Key',
      component: 'Input',
      componentProps: {
        placeholder: '请输入流程定义Key',
        allowClear: true,
      },
    },
    {
      fieldName: 'processCategory',
      label: '流程分类',
      component: 'Input',
      componentProps: {
        placeholder: '请输入流程分类',
        allowClear: true,
      },
    },
    {
      fieldName: 'enabled',
      label: '状态',
      component: 'Select',
      componentProps: {
        placeholder: '请选择状态',
        allowClear: true,
        options: [
          { label: '启用', value: 1 },
          { label: '禁用', value: 0 },
        ],
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      field: 'id',
      title: '编号',
      minWidth: 80,
    },
    {
      field: 'businessType',
      title: '业务类型标识',
      minWidth: 180,
    },
    {
      field: 'businessName',
      title: '业务类型名称',
      minWidth: 150,
    },
    {
      field: 'processDefinitionKey',
      title: '流程定义Key',
      minWidth: 150,
    },
    {
      field: 'processCategory',
      title: '流程分类',
      minWidth: 120,
    },
    {
      field: 'description',
      title: '描述',
      minWidth: 200,
    },
    {
      field: 'enabled',
      title: '状态',
      minWidth: 80,
      cellRender: {
        name: 'CellTag',
        props: {
          map: [
            { label: '禁用', value: 0, color: 'red' },
            { label: '启用', value: 1, color: 'green' },
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
      fieldName: 'businessType',
      label: '业务类型标识',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入业务类型标识，如 declare:filing:create',
      },
    },
    {
      fieldName: 'businessName',
      label: '业务类型名称',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入业务类型名称，如 备案申请',
      },
    },
    {
      fieldName: 'processDefinitionKey',
      label: '流程定义Key',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入流程定义Key，如 projectFiling',
      },
    },
    {
      fieldName: 'processCategory',
      label: '流程分类',
      component: 'Input',
      componentProps: {
        placeholder: '请输入流程分类，如 declare',
      },
    },
    {
      fieldName: 'description',
      label: '描述',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入描述',
        rows: 2,
      },
    },
    {
      fieldName: 'enabled',
      label: '是否启用',
      component: 'RadioGroup',
      defaultValue: 1,
      rules: 'required',
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '禁用', value: 0 },
        ],
      },
    },
    {
      fieldName: 'sort',
      label: '排序',
      component: 'InputNumber',
      defaultValue: 0,
      componentProps: {
        placeholder: '请输入排序',
        min: 0,
        max: 9999,
      },
    },
  ];
}
