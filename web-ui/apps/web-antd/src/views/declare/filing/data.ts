import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareFilingApi } from '#/api/declare/filing';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

import { getRangePickerDefaultProps } from '#/utils';

/** 新增/修改的表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'socialCreditCode',
      label: '社会信用代码',
      rules: 'required',
      component: 'Input',
      componentProps: {
        placeholder: '请输入社会信用代码',
      },
    },
    {
      fieldName: 'medicalLicenseNo',
      label: '执业许可证号',
      rules: 'required',
      component: 'Input',
      componentProps: {
        placeholder: '请输入执业许可证号',
      },
    },
    {
      fieldName: 'orgName',
      label: '机构名称',
      rules: 'required',
      component: 'Input',
      componentProps: {
        placeholder: '请输入机构名称',
      },
    },
    {
      fieldName: 'projectType',
      label: '项目类型',
      rules: 'required',
      component: 'Select',
      componentProps: {
        options: getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number'),
        placeholder: '请选择项目类型',
      },
    },
    {
      fieldName: 'validStartTime',
      label: '有效期开始时间',
      rules: 'required',
      component: 'DatePicker',
      componentProps: {
        showTime: true,
        format: 'YYYY-MM-DD',
        valueFormat: 'x',
      },
    },
    {
      fieldName: 'validEndTime',
      label: '有效期结束时间',
      rules: 'required',
      component: 'DatePicker',
      componentProps: {
        showTime: true,
        format: 'YYYY-MM-DD',
        valueFormat: 'x',
      },
    },
    {
      fieldName: 'constructionContent',
      label: '建设内容',
      rules: 'required',
      component: 'RichTextarea',
    },
    {
      fieldName: 'filingStatus',
      label: '备案状态',
      rules: 'required',
      component: 'Select',
      componentProps: {
        options: getDictOptions(DICT_TYPE.DECLARE_FILING_STATUS, 'number'),
        buttonStyle: 'solid',
        optionType: 'button',
      },
    },
    {
      fieldName: 'provinceReviewOpinion',
      label: '省审核意见',
      component: 'Input',
      componentProps: {
        placeholder: '请输入省审核意见',
      },
    },
    {
      fieldName: 'provinceReviewTime',
      label: '省审核时间',
      component: 'DatePicker',
      componentProps: {
        showTime: true,
        format: 'YYYY-MM-DD HH:mm:ss',
        valueFormat: 'x',
      },
    },
    {
      fieldName: 'provinceReviewerId',
      label: '省级审核人',
      component: 'Input',
      componentProps: {
        placeholder: '请输入省级审核人',
      },
    },
    {
      fieldName: 'expertReviewOpinion',
      label: '专家论证意见',
      component: 'Input',
      componentProps: {
        placeholder: '请输入专家论证意见',
      },
    },
    {
      fieldName: 'expertReviewerIds',
      label: '论证专家',
      component: 'Input',
      componentProps: {
        placeholder: '请输入论证专家',
      },
    },
    {
      fieldName: 'filingArchiveTime',
      label: '归档时间',
      component: 'DatePicker',
      componentProps: {
        showTime: true,
        format: 'YYYY-MM-DD HH:mm:ss',
        valueFormat: 'x',
      },
    },
  ];
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'socialCreditCode',
      label: '社会信用代码',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入社会信用代码',
      },
    },
    {
      fieldName: 'medicalLicenseNo',
      label: '执业许可证号',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入执业许可证号',
      },
    },
    {
      fieldName: 'orgName',
      label: '机构名称',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入机构名称',
      },
    },
    {
      fieldName: 'projectType',
      label: '项目类型',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number'),
        placeholder: '请选择项目类型',
      },
    },
    {
      fieldName: 'validStartTime',
      label: '有效期开始时间',
      component: 'RangePicker',
      componentProps: {
        ...getRangePickerDefaultProps(),
        allowClear: true,
      },
    },
    {
      fieldName: 'validEndTime',
      label: '有效期结束时间',
      component: 'RangePicker',
      componentProps: {
        ...getRangePickerDefaultProps(),
        allowClear: true,
      },
    },
    {
      fieldName: 'constructionContent',
      label: '建设内容',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入建设内容',
      },
    },
    {
      fieldName: 'filingStatus',
      label: '备案状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: getDictOptions(DICT_TYPE.DECLARE_FILING_STATUS, 'number'),
        placeholder: '请选择备案状态',
      },
    },
    {
      fieldName: 'provinceReviewOpinion',
      label: '省审核意见',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入省审核意见',
      },
    },
    {
      fieldName: 'provinceReviewTime',
      label: '省审核时间',
      component: 'RangePicker',
      componentProps: {
        ...getRangePickerDefaultProps(),
        allowClear: true,
      },
    },
    {
      fieldName: 'provinceReviewerId',
      label: '省级审核人',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入省级审核人',
      },
    },
    {
      fieldName: 'expertReviewOpinion',
      label: '专家论证意见',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入专家论证意见',
      },
    },
    {
      fieldName: 'expertReviewerIds',
      label: '论证专家',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入论证专家',
      },
    },
    {
      fieldName: 'filingArchiveTime',
      label: '归档时间',
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

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions<DeclareFilingApi.Filing>['columns'] {
  return [
  { type: 'checkbox', width: 40 },
    {
      field: 'id',
      title: '备案主键',
      minWidth: 120,
    },
    {
      field: 'socialCreditCode',
      title: '社会信用代码',
      minWidth: 120,
    },
    {
      field: 'medicalLicenseNo',
      title: '执业许可证号',
      minWidth: 120,
    },
    {
      field: 'orgName',
      title: '机构名称',
      minWidth: 120,
    },
    {
      field: 'projectType',
      title: '项目类型',
      minWidth: 120,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.DECLARE_PROJECT_TYPE },
      },
    },
    {
      field: 'validStartTime',
      title: '有效期开始时间',
      minWidth: 120,
      formatter: 'formatDateTime',
    },
    {
      field: 'validEndTime',
      title: '有效期结束时间',
      minWidth: 120,
      formatter: 'formatDateTime',
    },
    {
      field: 'constructionContent',
      title: '建设内容',
      minWidth: 120,
    },
    {
      field: 'filingStatus',
      title: '备案状态',
      minWidth: 120,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.DECLARE_FILING_STATUS },
      },
    },
    {
      field: 'provinceReviewOpinion',
      title: '省审核意见',
      minWidth: 120,
    },
    {
      field: 'provinceReviewTime',
      title: '省审核时间',
      minWidth: 120,
      formatter: 'formatDateTime',
    },
    {
      field: 'provinceReviewerId',
      title: '省级审核人',
      minWidth: 120,
    },
    {
      field: 'expertReviewOpinion',
      title: '专家论证意见',
      minWidth: 120,
    },
    {
      field: 'expertReviewerIds',
      title: '论证专家',
      minWidth: 120,
    },
    {
      field: 'filingArchiveTime',
      title: '归档时间',
      minWidth: 120,
      formatter: 'formatDateTime',
    },
    {
      field: 'createTime',
      title: '创建时间',
      minWidth: 120,
      formatter: 'formatDateTime',
    },
    {
      title: '操作',
      width: 200,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

