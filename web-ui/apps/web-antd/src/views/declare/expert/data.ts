import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { getDictOptions } from '@vben/hooks';

import { DICT_TYPE } from '@vben/constants';

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'expertName',
      label: '专家姓名',
      component: 'Input',
      componentProps: {
        placeholder: '请输入专家姓名',
        allowClear: true,
      },
    },
    {
      fieldName: 'expertType',
      label: '专家类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择专家类型',
        allowClear: true,
        options: getDictOptions(DICT_TYPE.DECLARE_EXPERT_TYPE, 'number'),
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        placeholder: '请选择状态',
        allowClear: true,
        options: getDictOptions(DICT_TYPE.DECLARE_EXPERT_STATUS, 'number'),
      },
    },
    {
      fieldName: 'workUnit',
      label: '工作单位',
      component: 'Input',
      componentProps: {
        placeholder: '请输入工作单位',
        allowClear: true,
      },
    },
    {
      fieldName: 'phone',
      label: '联系电话',
      component: 'Input',
      componentProps: {
        placeholder: '请输入联系电话',
        allowClear: true,
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(
  onEdit?: (row: any) => void,
  onDelete?: (row: any) => void,
  onStatusChange?: (status: number, row: any) => void,
): VxeTableGridOptions['columns'] {
  return [
    {
      field: 'id',
      title: '编号',
      minWidth: 80,
      sortable: true,
    },
    {
      field: 'expertName',
      title: '专家姓名',
      minWidth: 100,
    },
    {
      field: 'expertType',
      title: '专家类型',
      minWidth: 100,
      slots: { default: 'expertType' },
    },
    {
      field: 'phone',
      title: '联系电话',
      minWidth: 120,
    },
    {
      field: 'email',
      title: '电子邮箱',
      minWidth: 150,
    },
    {
      field: 'workUnit',
      title: '工作单位',
      minWidth: 180,
    },
    {
      field: 'professionalTitle',
      title: '职称',
      minWidth: 100,
    },
    {
      field: 'specialties',
      title: '专业领域',
      minWidth: 150,
    },
    {
      field: 'reviewCount',
      title: '评审次数',
      minWidth: 80,
    },
    {
      field: 'status',
      title: '状态',
      minWidth: 80,
      slots: { default: 'status' },
    },
    {
      field: 'createTime',
      title: '创建时间',
      minWidth: 160,
      sortable: true,
    },
    {
      field: 'action',
      title: '操作',
      width: 180,
      fixed: 'right',
      slots: { default: 'action' },
    },
  ];
}
