import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'indicatorCode',
      label: '指标代号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入指标代号',
        allowClear: true,
      },
    },
    {
      fieldName: 'indicatorName',
      label: '指标名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入指标名称',
        allowClear: true,
      },
    },
    {
      fieldName: 'category',
      label: '指标分类',
      component: 'Select',
      componentProps: {
        placeholder: '请选择指标分类',
        allowClear: true,
        options: getDictOptions(DICT_TYPE.DECLARE_INDICATOR_CATEGORY, 'number'),
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
      fieldName: 'businessType',
      label: '业务类型',
      component: 'Input',
      componentProps: {
        placeholder: '请输入业务类型',
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
    },
    {
      field: 'indicatorCode',
      title: '指标代号',
      minWidth: 120,
    },
    {
      field: 'indicatorName',
      title: '指标名称',
      minWidth: 180,
    },
    {
      field: 'unit',
      title: '单位',
      minWidth: 80,
    },
    {
      field: 'category',
      title: '分类',
      minWidth: 100,
      cellRender: {
        map: [
          { label: '基本情况', value: 1 },
          { label: '项目管理', value: 2 },
          { label: '系统功能', value: 3 },
          { label: '建设成效', value: 4 },
          { label: '数据集建设', value: 5 },
          { label: '数据交易', value: 6 },
          { label: '信息安全', value: 7 },
        ],
        name: 'CellTag',
      },
    },
    {
      field: 'valueType',
      title: '值类型',
      minWidth: 100,
      cellRender: {
        map: [
          { label: '数字', value: 1 },
          { label: '字符串', value: 2 },
          { label: '布尔', value: 3 },
          { label: '日期', value: 4 },
          { label: '长文本', value: 5 },
          { label: '单选', value: 6 },
          { label: '多选', value: 7 },
          { label: '日期区间', value: 8 },
          { label: '文件上传', value: 9 },
        ],
        name: 'CellTag',
      },
    },
    {
      field: 'isRequired',
      title: '必填',
      minWidth: 80,
      cellRender: {
        map: [
          { label: '是', value: true, color: 'red' },
          { label: '否', value: false, color: 'green' },
        ],
        name: 'CellTag',
      },
    },
    {
      field: 'projectType',
      title: '项目类型',
      minWidth: 100,
      cellRender: {
        map: [
          { label: '全部', value: 0 },
          { label: '综合型', value: 1 },
          { label: '中医电子病历型', value: 2 },
          { label: '智慧中药房型', value: 3 },
          { label: '名老中医传承型', value: 4 },
          { label: '中医临床科研型', value: 5 },
          { label: '中医智慧医共体型', value: 6 },
        ],
        name: 'CellTag',
      },
    },
    {
      field: 'businessType',
      title: '业务类型',
      minWidth: 100,
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
      fieldName: 'indicatorCode',
      label: '指标代号',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入指标代号，如 101、20101',
      },
    },
    {
      fieldName: 'indicatorName',
      label: '指标名称',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入指标名称',
      },
    },
    {
      fieldName: 'unit',
      label: '计量单位',
      component: 'Input',
      componentProps: {
        placeholder: '如：人、万元、次',
      },
    },
    {
      fieldName: 'category',
      label: '指标分类',
      component: 'Select',
      rules: 'required',
      componentProps: {
        placeholder: '请选择指标分类',
        options: getDictOptions(DICT_TYPE.DECLARE_INDICATOR_CATEGORY, 'number'),
      },
    },
    {
      fieldName: 'valueType',
      label: '值类型',
      component: 'Select',
      rules: 'required',
      componentProps: {
        placeholder: '请选择值类型',
        options: getDictOptions(DICT_TYPE.DECLARE_INDICATOR_VALUE_TYPE, 'number'),
      },
    },
    {
      fieldName: 'valueOptions',
      label: '选项定义',
      component: 'Textarea',
      componentProps: {
        placeholder: 'JSON格式，如 [{"value":1,"label":"选项1"}]',
        rows: 3,
      },
    },
    {
      fieldName: 'isRequired',
      label: '是否必填',
      component: 'RadioGroup',
      defaultValue: false,
      componentProps: {
        options: [
          { label: '是', value: true },
          { label: '否', value: false },
        ],
      },
    },
    {
      fieldName: 'minValue',
      label: '最小值',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入最小值',
      },
    },
    {
      fieldName: 'maxValue',
      label: '最大值',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入最大值',
      },
    },
    {
      fieldName: 'projectType',
      label: '适用项目类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择适用项目类型',
        allowClear: true,
        options: getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number'),
      },
    },
    {
      fieldName: 'businessType',
      label: '适用业务类型',
      component: 'Input',
      componentProps: {
        placeholder: '如：filing、project、process',
      },
    },
    {
      fieldName: 'logicRule',
      label: '逻辑校验关系',
      component: 'Textarea',
      componentProps: {
        placeholder: '如：201>=20101、802>=80201+80202',
        rows: 2,
      },
    },
    {
      fieldName: 'calculationRule',
      label: '计算公式',
      component: 'Input',
      componentProps: {
        placeholder: '如：401=系统覆盖名老中医工作室数',
      },
    },
    {
      fieldName: 'showInList',
      label: '列表显示',
      component: 'RadioGroup',
      defaultValue: true,
      componentProps: {
        options: [
          { label: '是', value: true },
          { label: '否', value: false },
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
