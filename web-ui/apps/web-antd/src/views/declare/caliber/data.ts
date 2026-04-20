import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { getIndicatorPage } from '#/api/declare/indicator';
import { getProjectTypeSimpleList } from '#/api/declare/project-type';

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'projectType',
      label: '项目类型',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择项目类型',
        allowClear: true,
        api: async () => {
          const list = await getProjectTypeSimpleList();
          return (list || []).map((item: any) => ({
            label: item.title,
            value: item.typeValue,
          }));
        },
      },
    },
    {
      fieldName: 'indicatorId',
      label: '指标',
      component: 'ApiSelect',
      dependencies: {
        triggerFields: ['projectType'],
        componentProps(values: any) {
          const pt = values.projectType;
          return {
            placeholder: pt ? '请选择或搜索指标' : '请先选择项目类型',
            allowClear: true,
            showSearch: true,
            filterOption: (input: string, option: any) => {
              const label = option.label || '';
              return label.toLowerCase().includes(input.toLowerCase());
            },
            labelField: 'label',
            valueField: 'value',
            // params 会触发 ApiSelect 重新请求
            params: pt ? { projectType: pt } : {},
            // API 加载指标列表
            api: async (p: any) => {
              const res = await getIndicatorPage({
                pageNo: 1,
                pageSize: 200,
                projectType: p.projectType,
              });
              return (res?.list || []).map((item: any) => ({
                label: `${item.indicatorCode} - ${item.indicatorName}`,
                value: item.id,
              }));
            },
          };
        },
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
    {
      fieldName: 'status',
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
      field: 'status',
      title: '状态',
      minWidth: 80,
      slots: { default: 'status' },
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
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        placeholder: '请选择状态',
        options: [
          { label: '启用', value: 1 },
          { label: '禁用', value: 0 },
        ],
      },
    },
  ];
}