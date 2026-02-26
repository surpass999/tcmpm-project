import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'indicatorId',
      label: '指标ID',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入指标ID',
        allowClear: true,
      },
    },
    {
      fieldName: 'definition',
      label: '指标解释',
      component: 'Input',
      componentProps: {
        placeholder: '请输入指标解释',
        allowClear: true,
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
      sortable: true,
    },
    {
      field: 'indicatorName',
      title: '指标',
      minWidth: 150,
    },
    {
      field: 'definition',
      title: '指标解释',
      minWidth: 200,
    },
    {
      field: 'statisticScope',
      title: '统计范围',
      minWidth: 150,
    },
    {
      field: 'dataSource',
      title: '数据来源',
      minWidth: 150,
    },
    {
      field: 'fillRequire',
      title: '填报要求',
      minWidth: 150,
    },
    {
      field: 'calculationExample',
      title: '计算公式',
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
      fieldName: 'indicatorId',
      label: '选择指标',
      component: 'Select',
      rules: 'required',
      componentProps: {},
    },
    {
      fieldName: 'definition',
      label: '指标解释',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入指标解释',
        rows: 2,
      },
    },
    {
      fieldName: 'statisticScope',
      label: '统计范围',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入统计范围',
        rows: 2,
      },
    },
    {
      fieldName: 'dataSource',
      label: '数据来源',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入数据来源',
        rows: 2,
      },
    },
    {
      fieldName: 'fillRequire',
      label: '填报要求',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入填报要求',
        rows: 2,
      },
    },
    {
      fieldName: 'calculationExample',
      label: '计算公式',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入计算公式',
        rows: 2,
      },
    },
  ];
}
