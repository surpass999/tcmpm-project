import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeclareHospitalApi } from '#/api/declare/hospital';

import { CommonStatusEnum, DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import { handleTree } from '@vben/utils';

import { z } from '#/adapter/form';
import { getDeptList } from '#/api/system/dept';
import { getProjectTypeSimpleList } from '#/api/declare/project-type';
import { requestClient } from '#/api/request';

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'hospitalName',
      label: '医院名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入医院名称',
        allowClear: true,
      },
    },
    {
      fieldName: 'provinceCode',
      label: '省份',
      component: 'ApiSelect',
      componentProps: {
        api: async () => {
          const res = await requestClient.get('/system/area/tree');
          return (res as any[]).map((p: any) => ({
            label: p.name,
            value: String(p.id),
          }));
        },
        labelField: 'label',
        valueField: 'value',
        placeholder: '请选择省份',
      },
    },
    {
      fieldName: 'hospitalLevel',
      label: '医院等级',
      component: 'Select',
      componentProps: {
        placeholder: '请选择医院等级',
        options: [
          { label: '三级甲等', value: '三级甲等' },
          { label: '三级乙等', value: '三级乙等' },
          { label: '二级甲等', value: '二级甲等' },
          { label: '二级乙等', value: '二级乙等' },
          { label: '一级甲等', value: '一级甲等' },
          { label: '一级乙等', value: '一级乙等' },
          { label: '未定级', value: '未定级' },
        ],
        allowClear: true,
      },
    },
    {
      fieldName: 'projectType',
      label: '项目类型',
      component: 'ApiSelect',
      componentProps: {
        api: async () => {
          const list = await getProjectTypeSimpleList();
          return (list || []).map((item: any) => ({
            label: item.title,
            value: item.typeValue,
          }));
        },
        placeholder: '请选择项目类型',
        allowClear: true,
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        placeholder: '请选择状态',
        options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'),
        allowClear: true,
      },
    },
  ];
}

/** 导入表单 */
export function useImportFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'file',
      label: '医院数据',
      component: 'Upload',
      rules: 'required',
      help: '仅允许导入 xls、xlsx 格式文件',
    },
    {
      fieldName: 'updateSupport',
      label: '是否覆盖',
      component: 'Switch',
      componentProps: {
        checkedChildren: '是',
        unCheckedChildren: '否',
      },
      rules: z.boolean().default(false),
      help: '是否更新已存在的医院数据',
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    { type: 'seq', width: 60, title: '序号' },
    {
      field: 'hospitalName',
      title: '医院名称',
      minWidth: 200,
    },
    {
      field: 'shortName',
      title: '简称',
      minWidth: 120,
    },
    {
      field: 'deptName',
      title: '关联部门',
      minWidth: 150,
    },
    {
      field: 'provinceName',
      title: '省份',
      minWidth: 100,
    },
    {
      field: 'hospitalLevel',
      title: '等级',
      minWidth: 100,
    },
    {
      field: 'projectTypeName',
      title: '项目类型',
      minWidth: 130,
    },
    {
      field: 'tagNames',
      title: '标签',
      minWidth: 150,
      showOverflow: true,
    },
    {
      field: 'contactPerson',
      title: '联系人',
      minWidth: 100,
    },
    {
      field: 'contactPhone',
      title: '联系电话',
      minWidth: 120,
    },
    {
      field: 'status',
      title: '状态',
      minWidth: 80,
      align: 'center',
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.COMMON_STATUS },
      },
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

/** 新增/修改的表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'id',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'deptId',
      label: '关联部门',
      component: 'ApiTreeSelect',
      componentProps: {
        api: async () => {
          const data = await getDeptList();
          return handleTree(data);
        },
        labelField: 'name',
        valueField: 'id',
        childrenField: 'children',
        placeholder: '请选择关联部门',
        treeDefaultExpandAll: true,
      },
    },
    {
      fieldName: 'hospitalCode',
      label: '医院编码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入医院编码',
      },
    },
    {
      fieldName: 'hospitalName',
      label: '医院全称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入医院全称',
      },
      rules: z.string().min(1, '请输入医院全称'),
    },
    {
      fieldName: 'shortName',
      label: '医院简称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入医院简称',
      },
    },
    {
      fieldName: 'unifiedSocialCreditCode',
      label: '统一社会信用代码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入统一社会信用代码',
      },
    },
    {
      fieldName: 'medicalLicenseNo',
      label: '执业许可证号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入执业许可证号',
      },
    },
    {
      fieldName: 'medicalLicenseExpire',
      label: '许可证有效期',
      component: 'DatePicker',
      componentProps: {
        placeholder: '请选择许可证有效期',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'projectType',
      label: '项目类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择项目类型',
        options: [
          { label: '综合型', value: 1 },
          { label: '中医电子病历型', value: 2 },
          { label: '智慧中药房型', value: 3 },
          { label: '名老中医传承型', value: 4 },
          { label: '中医临床科研型', value: 5 },
          { label: '中医智慧医共体型', value: 6 },
        ],
      },
    },
    {
      fieldName: 'hospitalLevel',
      label: '医院等级',
      component: 'Select',
      componentProps: {
        placeholder: '请选择医院等级',
        options: [
          { label: '三级甲等', value: '三级甲等' },
          { label: '三级乙等', value: '三级乙等' },
          { label: '二级甲等', value: '二级甲等' },
          { label: '二级乙等', value: '二级乙等' },
          { label: '一级甲等', value: '一级甲等' },
          { label: '一级乙等', value: '一级乙等' },
          { label: '未定级', value: '未定级' },
        ],
      },
    },
    {
      fieldName: 'hospitalCategory',
      label: '医院类别',
      component: 'Select',
      componentProps: {
        placeholder: '请选择医院类别',
        options: [
          { label: '综合医院', value: '综合医院' },
          { label: '中医医院', value: '中医医院' },
          { label: '中西医结合医院', value: '中西医结合医院' },
          { label: '民族医医院', value: '民族医医院' },
          { label: '专科医院', value: '专科医院' },
        ],
      },
    },
    {
      fieldName: 'provinceCode',
      label: '省份',
      component: 'ApiSelect',
      componentProps: {
        api: async () => {
          const res = await requestClient.get('/system/area/tree');
          return (res as any[]).map((p: any) => ({
            label: p.name,
            value: String(p.id),
          }));
        },
        labelField: 'label',
        valueField: 'value',
        placeholder: '请选择省份',
      },
    },
    {
      fieldName: 'cityCode',
      label: '城市',
      component: 'ApiSelect',
      dependencies: {
        triggerFields: ['provinceCode'],
        async trigger(values: any, context: any) {
          const { componentRefMap } = context;
          if (!values.provinceCode) {
            values.cityCode = undefined;
            return;
          }
          try {
            const res = await requestClient.get(
              `/system/area/province/${values.provinceCode}/cities`,
            );
            const cities = (res as any[]) || [];
            const citySelect = componentRefMap?.get?.('cityCode') as any;
            if (citySelect && typeof citySelect.updateParam === 'function') {
              citySelect.updateParam({ _cities: cities });
            }
          } catch (e) {
            console.warn('获取城市列表失败:', e);
          }
        },
        componentProps(_values: any) {
          return {
            placeholder: _values.provinceCode ? '请选择城市' : '请先选择省份',
            labelField: 'label',
            valueField: 'value',
            api: async (extra: any) => {
              const cities = extra?._cities || [];
              return cities.map((c: any) => ({
                label: c.name,
                value: String(c.id),
              }));
            },
          };
        },
      },
    },
    {
      fieldName: 'districtCode',
      label: '区县',
      component: 'ApiSelect',
      dependencies: {
        triggerFields: ['cityCode'],
        async trigger(values: any, context: any) {
          const { componentRefMap } = context;
          if (!values.cityCode) {
            values.districtCode = undefined;
            return;
          }
          try {
            const res = await requestClient.get(
              `/system/area/province/${values.cityCode}/cities`,
            );
            const districts = (res as any[]) || [];
            const districtSelect = componentRefMap?.get?.('districtCode') as any;
            if (districtSelect && typeof districtSelect.updateParam === 'function') {
              districtSelect.updateParam({ _cities: districts });
            }
          } catch (e) {
            console.warn('获取区县列表失败:', e);
          }
        },
        componentProps(_values: any) {
          return {
            placeholder: _values.cityCode ? '请选择区县' : '请先选择城市',
            labelField: 'label',
            valueField: 'value',
            api: async (extra: any) => {
              const districts = extra?._cities || [];
              return districts.map((d: any) => ({
                label: d.name,
                value: String(d.id),
              }));
            },
          };
        },
      },
    },
    {
      fieldName: 'address',
      label: '详细地址',
      component: 'Input',
      componentProps: {
        placeholder: '请输入详细地址',
      },
    },
    {
      fieldName: 'contactPerson',
      label: '联系人',
      component: 'Input',
      componentProps: {
        placeholder: '请输入联系人',
      },
    },
    {
      fieldName: 'contactPhone',
      label: '联系电话',
      component: 'Input',
      componentProps: {
        placeholder: '请输入联系电话',
      },
    },
    {
      fieldName: 'contactEmail',
      label: '邮箱',
      component: 'Input',
      componentProps: {
        placeholder: '请输入邮箱',
      },
    },
    {
      fieldName: 'website',
      label: '官方网站',
      component: 'Input',
      componentProps: {
        placeholder: '请输入官方网站',
      },
    },
    {
      fieldName: 'bedCount',
      label: '编制床位数',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入编制床位数',
        min: 0,
      },
    },
    {
      fieldName: 'employeeCount',
      label: '在职职工人数',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入在职职工人数',
        min: 0,
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      componentProps: {
        options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'),
        buttonStyle: 'solid',
        optionType: 'button',
      },
      rules: z.number().default(CommonStatusEnum.ENABLE),
    },
  ];
}
