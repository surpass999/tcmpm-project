import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

// 活动类型选项
export const TRAINING_TYPE_OPTIONS = getDictOptions(DICT_TYPE.DECLARE_TRAINING_TYPE, 'number');

// 目标范围选项
export const TARGET_SCOPE_OPTIONS = getDictOptions(DICT_TYPE.DECLARE_TARGET_SCOPE, 'number');

// 活动状态选项
export const TRAINING_STATUS_OPTIONS = getDictOptions(DICT_TYPE.DECLARE_TRAINING_STATUS, 'number');

// 报名状态选项
export const REGISTRATION_STATUS_OPTIONS = getDictOptions(DICT_TYPE.DECLARE_REGISTRATION_STATUS, 'number');

/** 获取活动类型标签 */
export function getTrainingTypeLabel(type?: number): string {
  const item = TRAINING_TYPE_OPTIONS.find((item) => item.value === type);
  return item?.label || '-';
}

/** 获取活动类型颜色 */
export function getTrainingTypeColor(type?: number): string {
  const item = TRAINING_TYPE_OPTIONS.find((item) => item.value === type);
  return item?.colorType || 'default';
}

/** 获取目标范围标签 */
export function getTargetScopeLabel(scope?: number): string {
  const item = TARGET_SCOPE_OPTIONS.find((item) => item.value === scope);
  return item?.label || '-';
}

/** 获取活动状态标签 */
export function getTrainingStatusLabel(status?: number): string {
  const item = TRAINING_STATUS_OPTIONS.find((item) => item.value === status);
  return item?.label || '-';
}

/** 获取活动状态颜色 */
export function getTrainingStatusColor(status?: number): string {
  const item = TRAINING_STATUS_OPTIONS.find((item) => item.value === status);
  return item?.colorType || 'default';
}

/** 获取报名状态标签 */
export function getRegistrationStatusLabel(status?: number): string {
  const item = REGISTRATION_STATUS_OPTIONS.find((item) => item.value === status);
  return item?.label || '-';
}

/** 获取报名状态颜色 */
export function getRegistrationStatusColor(status?: number): string {
  const item = REGISTRATION_STATUS_OPTIONS.find((item) => item.value === status);
  return item?.colorType || 'default';
}

/** 表格列配置 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { field: 'id', title: '编号', width: 80 },
    { type: 'checkbox', width: 60 },
    { field: 'name', title: '活动名称', minWidth: 200 },
    { field: 'typeLabel', title: '活动类型', width: 100 },
    { field: 'organizer', title: '组织单位', width: 150 },
    { field: 'speaker', title: '主讲人', width: 120 },
    { field: 'startTime', title: '开始时间', width: 160, formatter: 'formatDate' },
    { field: 'endTime', title: '结束时间', width: 160, formatter: 'formatDate' },
    { field: 'location', title: '活动地点', width: 150 },
    { field: 'currentParticipants', title: '报名人数', width: 100 },
    { field: 'maxParticipants', title: '最大人数', width: 100 },
    { field: 'statusLabel', title: '状态', width: 100 },
    { field: 'createTime', title: '创建时间', width: 160, formatter: 'formatDate' },
    {
      title: '操作',
      width: 200,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

/** 搜索表单配置 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'keyword',
      title: '关键词',
      fieldProps: { placeholder: '请输入活动名称/组织单位/主讲人' },
      component: 'Input',
    },
    {
      fieldName: 'type',
      title: '活动类型',
      component: 'Select',
      componentProps: {
        options: TRAINING_TYPE_OPTIONS,
        placeholder: '请选择活动类型',
        clearable: true,
      },
    },
    {
      fieldName: 'status',
      title: '活动状态',
      component: 'Select',
      componentProps: {
        options: TRAINING_STATUS_OPTIONS,
        placeholder: '请选择状态',
        clearable: true,
      },
    },
    {
      fieldName: 'targetScope',
      title: '目标范围',
      component: 'Select',
      componentProps: {
        options: TARGET_SCOPE_OPTIONS,
        placeholder: '请选择范围',
        clearable: true,
      },
    },
    {
      fieldName: 'startTimeStart',
      title: '开始时间',
      fieldProps: { placeholder: '请选择开始时间', type: 'datetime' },
      component: 'DatePicker',
      componentProps: {
        showTime: true,
        format: 'YYYY-MM-DD HH:mm:ss',
        clearable: true,
      },
    },
  ];
}

/** 报名管理表格列配置 */
export function useRegistrationGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { field: 'id', title: '编号', width: 80 },
    { field: 'trainingName', title: '活动名称', minWidth: 200 },
    { field: 'userName', title: '报名人', width: 100 },
    { field: 'organization', title: '所属单位', width: 150 },
    { field: 'position', title: '职位/职称', width: 120 },
    { field: 'phone', title: '联系电话', width: 130 },
    { field: 'statusLabel', title: '状态', width: 100 },
    { field: 'registerTime', title: '报名时间', width: 160, formatter: 'formatDate' },
    { field: 'signInTime', title: '签到时间', width: 160, formatter: 'formatDate' },
    { field: 'rating', title: '评分', width: 80 },
    {
      title: '操作',
      width: 120,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

/** 报名管理搜索表单配置 */
export function useRegistrationFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'trainingId',
      title: '活动名称',
      fieldProps: { placeholder: '请输入活动名称' },
      component: 'Input',
    },
    {
      fieldName: 'keyword',
      title: '关键词',
      fieldProps: { placeholder: '请输入姓名/单位' },
      component: 'Input',
    },
    {
      fieldName: 'status',
      title: '状态',
      component: 'Select',
      componentProps: {
        options: REGISTRATION_STATUS_OPTIONS,
        placeholder: '请选择状态',
        clearable: true,
      },
    },
  ];
}
