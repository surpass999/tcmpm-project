import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

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
        options: getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number'),
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
    if (!cellValue) return '-';
    const option = getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number').find(
      (item) => Number(item.value) === cellValue
    );
    return option?.label || cellValue;
  };

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
