import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import type { DeclareReviewApi } from '#/api/declare/review';

/** 任务类型选项 */
const TASK_TYPE_OPTIONS = [
  { label: '备案论证', value: 1 },
  { label: '中期评估', value: 2 },
  { label: '验收评审', value: 3 },
  { label: '成果审核', value: 4 },
];

/** 业务类型选项 */
const BUSINESS_TYPE_OPTIONS = [
  { label: '备案', value: 1 },
  { label: '项目', value: 2 },
  { label: '成果', value: 3 },
];

/** 评审状态选项 */
const REVIEW_STATUS_OPTIONS = [
  { label: '待分配', value: 0 },
  { label: '评审中', value: 1 },
  { label: '已完成', value: 2 },
];

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'taskType',
      label: '任务类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择任务类型',
        allowClear: true,
        options: TASK_TYPE_OPTIONS,
      },
    },
    {
      fieldName: 'businessType',
      label: '业务类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择业务类型',
        allowClear: true,
        options: BUSINESS_TYPE_OPTIONS,
      },
    },
    {
      fieldName: 'status',
      label: '评审状态',
      component: 'Select',
      componentProps: {
        placeholder: '请选择评审状态',
        allowClear: true,
        options: REVIEW_STATUS_OPTIONS,
      },
    },
    {
      fieldName: 'taskName',
      label: '任务名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入任务名称',
        allowClear: true,
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions<DeclareReviewApi.ReviewTask>['columns'] {
  return [
    { field: 'id', title: '编号', width: 80 },
    {
      field: 'taskName',
      title: '任务名称',
      minWidth: 180,
    },
    {
      field: 'taskType',
      title: '任务类型',
      width: 100,
      slots: { default: 'taskType_default' },
    },
    {
      field: 'businessType',
      title: '业务类型',
      width: 80,
      slots: { default: 'businessType_default' },
    },
    {
      field: 'businessName',
      title: '业务名称',
      minWidth: 150,
    },
    {
      field: 'status',
      title: '评审状态',
      width: 100,
      slots: { default: 'status_default' },
    },
    {
      field: 'totalScore',
      title: '综合评分',
      width: 100,
    },
    {
      field: 'reviewConclusion',
      title: '评审结论',
      width: 100,
    },
    {
      field: 'startTime',
      title: '开始时间',
      width: 160,
    },
    {
      field: 'endTime',
      title: '截止时间',
      width: 160,
    },
    {
      field: 'createTime',
      title: '创建时间',
      width: 160,
    },
    {
      title: '操作',
      width: 200,
      fixed: 'right',
      slots: { default: 'action_default' },
    },
  ];
}
