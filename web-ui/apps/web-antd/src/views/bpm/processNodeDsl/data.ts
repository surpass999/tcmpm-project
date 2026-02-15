import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
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
      fieldName: 'nodeKey',
      label: '节点Key',
      component: 'Input',
      componentProps: {
        placeholder: '请输入节点Key',
        allowClear: true,
      },
    },
    {
      fieldName: 'nodeName',
      label: '节点名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入节点名称',
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
      field: 'processDefinitionKey',
      title: '流程定义Key',
      minWidth: 150,
    },
    {
      field: 'nodeKey',
      title: '节点Key',
      minWidth: 150,
    },
    {
      field: 'nodeName',
      title: '节点名称',
      minWidth: 120,
    },
    {
      field: 'cap',
      title: '节点能力',
      minWidth: 120,
      slots: { default: 'cap-default' },
    },
    {
      field: 'actions',
      title: '可用动作',
      minWidth: 180,
      slots: { default: 'actions-default' },
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
      field: 'remark',
      title: '备注',
      minWidth: 150,
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
      fieldName: 'processDefinitionKey',
      label: '流程定义Key',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入流程定义Key，如 proc_filing',
      },
    },
    {
      fieldName: 'nodeKey',
      label: '节点Key',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入节点Key，如 province_audit',
      },
    },
    {
      fieldName: 'nodeName',
      label: '节点名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入节点名称，如 省级审核',
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
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入备注',
        rows: 2,
      },
    },
  ];
}
