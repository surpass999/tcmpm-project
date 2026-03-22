import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareProjectApi } from '#/api/declare/project';

import { getDictOptions } from '@vben/hooks';
import { DICT_TYPE } from '@vben/constants';

import { getRangePickerDefaultProps } from '#/utils';

/** 固定状态选项（使用字符串 key） */
const FIXED_STATUS_OPTIONS = [
  { label: '初始化', value: 'INITIATION' },
  { label: '立项中', value: 'FILING' },
  { label: '建设中', value: 'CONSTRUCTION' },
  { label: '中期评估', value: 'MIDTERM' },
  { label: '整改中', value: 'RECTIFICATION' },
  { label: '验收中', value: 'ACCEPTANCE' },
  { label: '已验收', value: 'ACCEPTED' },
  { label: '已终止', value: 'TERMINATED' },
];

/** 项目状态选项（固定状态 + 过程类型状态） */
const PROJECT_STATUS_OPTIONS = [
  ...FIXED_STATUS_OPTIONS,
  // 过程类型状态（从字典动态获取）
  ...(getDictOptions(DICT_TYPE.DECLARE_PROCESS_TYPE, 'number') as any[])
    .filter((item: { value: number }) => item.value !== 7)
    .map((item: { value: number; label: string }) => ({
      label: item.label,
      value: String(item.value),
    })),
];

/** 状态映射表（用于格式化显示） */
const STATUS_LABEL_MAP: Record<string, string> = {
  ...Object.fromEntries(FIXED_STATUS_OPTIONS.map((s) => [s.value, s.label])),
  // 过程类型从字典获取
  ...Object.fromEntries(
    (getDictOptions(DICT_TYPE.DECLARE_PROCESS_TYPE, 'number') as any[])
      .filter((item: { value: number }) => item.value !== 7)
      .map((item: { value: number; label: string }) => [String(item.value), item.label])
  ),
};

/** 搜索表单配置 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'projectName',
      label: '项目名称',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入项目名称',
      },
    },
    {
      fieldName: 'projectStatus',
      label: '项目状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: PROJECT_STATUS_OPTIONS,
        placeholder: '请选择项目状态',
      },
    },
    {
      fieldName: 'leaderUserId',
      label: '项目负责人',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入负责人姓名',
      },
    },
    {
      fieldName: 'startTime',
      label: '立项时间',
      component: 'RangePicker',
      componentProps: {
        ...getRangePickerDefaultProps(),
        allowClear: true,
      },
    },
    {
      fieldName: 'planEndTime',
      label: '计划完成时间',
      component: 'RangePicker',
      componentProps: {
        ...getRangePickerDefaultProps(),
        allowClear: true,
      },
    },
    {
      fieldName: 'createTime',
      label: '创建时间',
      component: 'RangePicker',
      componentProps: {
        ...getRangePickerDefaultProps(),
        allowClear: true,
      },
    },
  ];
}

/** 表格列配置 */
export function useGridColumns(): VxeTableGridOptions<DeclareProjectApi.Project>['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    {
      field: 'id',
      title: '项目ID',
      minWidth: 80,
    },
    {
      field: 'projectName',
      title: '项目名称',
      minWidth: 200,
    },
    {
      field: 'projectStatus',
      title: '项目状态',
      minWidth: 120,
      formatter: ({ cellValue }) => {
        const label = STATUS_LABEL_MAP[cellValue];
        return label || cellValue || '-';
      },
    },
    {
      field: 'totalInvestment',
      title: '总投资(万元)',
      minWidth: 120,
      formatter: ({ cellValue }) => {
        if (cellValue === null || cellValue === undefined) return '-';
        return Number(cellValue).toFixed(2);
      },
    },
    {
      field: 'centralFundArrive',
      title: '中央资金到账(万元)',
      minWidth: 140,
      formatter: ({ cellValue }) => {
        if (cellValue === null || cellValue === undefined) return '-';
        return Number(cellValue).toFixed(2);
      },
    },
    {
      field: 'accumulatedInvestment',
      title: '累计完成投资(万元)',
      minWidth: 140,
      formatter: ({ cellValue }) => {
        if (cellValue === null || cellValue === undefined) return '-';
        return Number(cellValue).toFixed(2);
      },
    },
    {
      field: 'actualProgress',
      title: '实际进度',
      minWidth: 180,
      slots: { default: 'progress' },
    },
    {
      field: 'startTime',
      title: '立项时间',
      minWidth: 120,
      formatter: 'formatDateTime',
    },
    {
      field: 'planEndTime',
      title: '计划完成时间',
      minWidth: 120,
      formatter: 'formatDateTime',
    },
    {
      field: 'leaderName',
      title: '负责人',
      minWidth: 100,
    },
    {
      field: 'leaderMobile',
      title: '联系电话',
      minWidth: 120,
    },
    {
      field: 'createTime',
      title: '创建时间',
      minWidth: 120,
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
