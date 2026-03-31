import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { getProjectTypeSimpleList } from '#/api/declare/project-type';

// 项目类型选项（从 API 加载）
let PROJECT_TYPE_OPTIONS: { label: string; value: number }[] = [];

// 加载项目类型选项（同步方式，在模块初始化时加载）
getProjectTypeSimpleList()
  .then((list) => {
    PROJECT_TYPE_OPTIONS = list.map((item) => ({
      label: item.title || item.name,
      value: item.typeValue,
    }));
  })
  .catch(() => {
    PROJECT_TYPE_OPTIONS = [];
  });

/** 获取项目类型选项 */
export function getProjectTypeOptions() {
  return PROJECT_TYPE_OPTIONS;
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'groupCode',
      label: '分组编码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入分组编码',
        allowClear: true,
      },
    },
    {
      fieldName: 'groupName',
      label: '分组名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入分组名称',
        allowClear: true,
      },
    },
    {
      fieldName: 'groupLevel',
      label: '分组层级',
      component: 'Select',
      componentProps: {
        placeholder: '请选择分组层级',
        allowClear: true,
        options: [
          { label: '一级分组', value: 1 },
          { label: '二级分组', value: 2 },
        ],
      },
    },
    {
      fieldName: 'projectType',
      label: '项目类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择项目类型',
        allowClear: true,
        options: PROJECT_TYPE_OPTIONS,
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  // 项目类型格式化函数
  const projectTypeFormatter = ({ cellValue }: { cellValue: any }) => {
    const option = PROJECT_TYPE_OPTIONS.find((item) => item.value === cellValue);
    return option ? option.label : cellValue || '-';
  };

  // 层级格式化函数
  const groupLevelFormatter = ({ cellValue }: { cellValue: any }) => {
    return cellValue === 1 ? '一级' : '二级';
  };

  // 状态格式化函数
  const statusFormatter = ({ cellValue }: { cellValue: any }) => {
    return cellValue === 1 ? '启用' : '禁用';
  };

  return [
    {
      field: 'id',
      title: '编号',
      width: 80,
    },
    {
      field: 'groupCode',
      title: '分组编码',
      width: 150,
    },
    {
      field: 'groupName',
      title: '分组名称',
      minWidth: 200,
    },
    {
      field: 'groupLevel',
      title: '层级',
      width: 100,
      formatter: groupLevelFormatter,
    },
    {
      field: 'projectType',
      title: '项目类型',
      width: 150,
      formatter: projectTypeFormatter,
    },
    {
      field: 'groupPrefix',
      title: '前缀标识',
      width: 120,
    },
    {
      field: 'sort',
      title: '排序',
      width: 80,
    },
    {
      field: 'status',
      title: '状态',
      width: 80,
      formatter: statusFormatter,
    },
    {
      field: 'createTime',
      title: '创建时间',
      width: 180,
      formatter: 'formatDateTime',
    },
    {
      field: 'actions',
      title: '操作',
      width: 150,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

/** 弹窗表单配置 */
export function useModalFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'parentId',
      label: '父分组',
      component: 'Select',
      componentProps: {
        placeholder: '请选择父分组（不选则为顶级）',
        allowClear: true,
      },
      defaultValue: 0,
    },
    {
      fieldName: 'groupCode',
      label: '分组编码',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入分组编码',
      },
    },
    {
      fieldName: 'groupName',
      label: '分组名称',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入分组名称',
      },
    },
    {
      fieldName: 'groupLevel',
      label: '分组层级',
      component: 'RadioGroup',
      rules: 'required',
      defaultValue: 1,
      componentProps: {
        options: [
          { label: '一级分组', value: 1 },
          { label: '二级分组', value: 2 },
        ],
      },
    },
    {
      fieldName: 'projectType',
      label: '关联项目类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择项目类型（一级分组必须选择）',
        allowClear: true,
        options: PROJECT_TYPE_OPTIONS,
      },
    },
    {
      fieldName: 'groupPrefix',
      label: '分组前缀',
      component: 'Input',
      componentProps: {
        placeholder: '如：101、10101，用于描述分组特征',
      },
    },
    {
      fieldName: 'description',
      label: '分组描述',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入分组描述',
        rows: 3,
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
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      defaultValue: 1,
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '禁用', value: 0 },
        ],
      },
    },
  ];
}
