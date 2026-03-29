import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'typeCode',
      label: '类型编码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入类型编码',
        allowClear: true,
      },
    },
    {
      fieldName: 'name',
      label: '类型名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入类型名称',
        allowClear: true,
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
  // 状态格式化
  const statusFormatter = ({ cellValue }: { cellValue: any }) => {
    return cellValue === 1 ? '启用' : '禁用';
  };

  return [
    {
      field: 'id',
      title: '编号',
      width: 80,
    },
    {
      field: 'typeCode',
      title: '类型编码',
      width: 150,
    },
    {
      field: 'typeValue',
      title: '类型值',
      width: 100,
    },
    {
      field: 'name',
      title: '类型名称',
      minWidth: 150,
    },
    {
      field: 'title',
      title: '显示标题',
      minWidth: 150,
    },
    {
      field: 'description',
      title: '描述',
      minWidth: 200,
      showOverflow: true,
    },
    {
      field: 'color',
      title: '颜色',
      width: 100,
      slots: { default: 'color' },
    },
    {
      field: 'sort',
      title: '排序',
      width: 80,
    },
    {
      field: 'status',
      title: '状态',
      width: 80,
      formatter: statusFormatter,
    },
    {
      field: 'createTime',
      title: '创建时间',
      width: 180,
      formatter: 'formatDateTime',
    },
    {
      field: 'actions',
      title: '操作',
      width: 150,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

/** 弹窗表单配置 */
export function useModalFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'typeCode',
      label: '类型编码',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入类型编码，如 PROJECT_01',
        disabled: true,
      },
    },
    {
      fieldName: 'typeValue',
      label: '类型值',
      component: 'InputNumber',
      rules: 'required',
      componentProps: {
        placeholder: '请输入类型值',
        min: 1,
        max: 99,
        disabled: true,
      },
    },
    {
      fieldName: 'name',
      label: '类型名称',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入类型名称，如 综合型',
      },
    },
    {
      fieldName: 'title',
      label: '显示标题',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入显示标题，如 综合型医院',
      },
    },
    {
      fieldName: 'description',
      label: '描述',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入类型描述',
        rows: 3,
      },
    },
    {
      fieldName: 'color',
      label: '主题颜色',
      component: 'Input',
      componentProps: {
        placeholder: '请输入颜色值，如 #1890ff',
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
