import { requestClient } from '#/api/request';

/**
 * 进度填报记录
 */
export interface DeclareProgressReport {
  id: number;
  hospitalProcessInstanceId?: string; // BPM 流程实例ID
  reportYear: number;
  reportBatch: number;
  hospitalId: number;
  deptId: number;
  hospitalName: string;
  provinceCode: string;
  provinceName: string;
  reportStatus: string;
  reportStatusName: string;
  provinceStatus: number;
  provinceStatusName: string;
  nationalReportStatus: number;
  nationalReportStatusName: string;
  nationalReportTime?: string;
  nationalReporterName?: string;
  /** 项目类型：1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型 */
  projectType?: number;
  /** 项目类型名称（来自 declare_project_type.title） */
  projectTypeName?: string;
  /** 项目类型简称（如"综合型"） */
  projectTypeShortName?: string;
  /** 填报窗口开始时间 */
  windowStart?: string;
  /** 填报窗口结束时间 */
  windowEnd?: string;
  /** 统一社会信用代码（从医院表关联获取） */
  unifiedSocialCreditCode?: string;
  /** 医疗机构执业许可证登记号（从医院表关联获取） */
  medicalLicenseNo?: string;
  /** 填报人（用户账号） */
  creator?: string;
  createTime: string;
  updateTime: string;
  /** 医院审核人姓名（提交审核时填写） */
  auditUserName?: string;
}

/**
 * 创建填报记录
 */
export function createProgressReport(data: {
  reportYear: number;
  reportBatch: number;
}) {
  return requestClient.post<number>('/declare/progress-report/create', data);
}

/**
 * 保存进度填报（报告数据 + 指标值一起提交）
 */
export function saveProgressReport(data: {
  id?: number;
  reportYear?: number;
  reportBatch?: number;
  reportStatus: string;
  values: Array<{
    indicatorId: number;
    indicatorCode: string;
    valueType: number;
    value: any;
  }>;
}) {
  return requestClient.post<number>('/declare/progress-report/save', data);
}

/**
 * 获取填报详情
 */
export function getProgressReport(id: number) {
  return requestClient.get<DeclareProgressReport>(
    `/declare/progress-report/get?id=${id}`,
  );
}

/**
 * 获取医院填报列表
 */
export function getHospitalReportList(hospitalId: number, reportYear?: number) {
  return requestClient.get<DeclareProgressReport[]>(
    `/declare/progress-report/hospital-list?hospitalId=${hospitalId}${
      reportYear ? `&reportYear=${reportYear}` : ''
    }`,
  );
}

/**
 * 获取省级填报列表
 */
export function getProvinceReportList(provinceCode: string, reportYear?: number) {
  return requestClient.get<DeclareProgressReport[]>(
    `/declare/progress-report/province-list?provinceCode=${provinceCode}${
      reportYear ? `&reportYear=${reportYear}` : ''
    }`,
  );
}

/**
 * 获取省级填报列表（通过 deptId 自动推导省份）
 */
export function getProvinceReportListByDept(reportYear?: number) {
  return requestClient.get<DeclareProgressReport[]>(
    `/declare/progress-report/province-list-by-dept${
      reportYear ? `?reportYear=${reportYear}` : ''
    }`,
  );
}

/**
 * 获取省级待审核列表
 */
export function getProvincePendingList(provinceCode: string) {
  return requestClient.get<DeclareProgressReport[]>(
    `/declare/progress-report/province-pending?provinceCode=${provinceCode}`,
  );
}

/**
 * 获取省级审核通过待上报列表
 */
export function getProvinceApprovedList(provinceCode: string) {
  return requestClient.get<DeclareProgressReport[]>(
    `/declare/progress-report/province-approved?provinceCode=${provinceCode}`,
  );
}

/**
 * 获取填报列表（国家局端）- 只显示医院已提交审核的填报
 */
export function getNationalReportList() {
  return requestClient.get<DeclareProgressReport[]>(
    '/declare/progress-report/national-list',
  );
}

/**
 * 提交审核
 */
export function submitProgressReport(id: number, auditUserName?: string) {
  const params = new URLSearchParams({ id: String(id) });
  if (auditUserName) {
    params.append('auditUserName', auditUserName);
  }
  return requestClient.post(`/declare/progress-report/submit?${params.toString()}`);
}

/**
 * 医院审核员审核
 */
export function hospitalAuditProgressReport(data: {
  id: number;
  approved: boolean;
  opinion?: string;
}) {
  return requestClient.post('/declare/progress-report/hospital-audit', data);
}

/**
 * 省级审核
 */
export function provinceAuditProgressReport(data: {
  id: number;
  approved: boolean;
  opinion?: string;
}) {
  return requestClient.post('/declare/progress-report/province-audit', data);
}

/**
 * 获取填报历史
 */
export function getReportHistory(hospitalId: number, reportYear?: number) {
  return requestClient.get<DeclareProgressReport[]>(
    `/declare/progress-report/history?hospitalId=${hospitalId}${
      reportYear ? `&reportYear=${reportYear}` : ''
    }`,
  );
}

/**
 * 检查填报时间窗口
 */
export function checkReportWindow(hospitalId: number) {
  return requestClient.get<boolean>(
    `/declare/progress-report/check-window?hospitalId=${hospitalId}`,
  );
}

/**
 * 获取医院当前可用的填报窗口
 */
export function getAvailableReportWindows(hospitalId: number) {
  return requestClient.get<
    {
      id: number;
      reportYear: number;
      reportBatch: number;
      windowStart: string;
      windowEnd: string;
      remark?: string;
      status: number;
    }[]
  >(`/declare/progress-report/available-windows?hospitalId=${hospitalId}`);
}

/**
 * 获取最新开放填报窗口
 */
export function getLatestOpenWindow() {
  return requestClient.get<{
    id: number;
    reportYear: number;
    reportBatch: number;
    windowStart: string;
    windowEnd: string;
    remark?: string;
    status: number;
  } | null>(`/declare/progress-report/latest-window`);
}

/**
 * 检查填报次数限制
 */
export function checkReportLimit(hospitalId: number, reportYear: number) {
  return requestClient.get<boolean>(
    `/declare/progress-report/check-limit?hospitalId=${hospitalId}&reportYear=${reportYear}`,
  );
}

/**
 * 填报状态选项（来自数据库字典 declare_project_status）
 */
export const REPORT_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已保存', value: 'SAVED' },
  { label: '待审批', value: 'SUBMITTED' },
  // BPM审批流返回的其他状态，如 HOSPITAL_AUDITING、HOSPITAL_APPROVED 等
];

/**
 * 判断填报状态是否可编辑
 */
export function canEditReport(reportStatus: string): boolean {
  return reportStatus === 'DRAFT' || reportStatus === 'SAVED';
}

/**
 * 删除填报记录
 */
export function deleteProgressReport(id: number) {
  return requestClient.delete(`/declare/progress-report/delete?id=${id}`);
}

/**
 * 省级审核状态选项
 */
export const PROVINCE_STATUS_OPTIONS = [
  { label: '未提交', value: 0 },
  { label: '省级审核中', value: 1 },
  { label: '省级通过', value: 2 },
  { label: '省级驳回', value: 3 },
];

/**
 * 国家局上报状态选项
 */
export const NATIONAL_REPORT_STATUS_OPTIONS = [
  { label: '未上报', value: 0 },
  { label: '已上报', value: 1 },
];

/**
 * 批量上报国家局
 */
export function batchNationalReport(reportIds: number[], remark?: string) {
  return requestClient.post<number>('/declare/national-report/batch-report', {
    reportIds,
    remark,
  });
}

/**
 * 对比数据行
 */
export interface CompareIndicatorRow {
  indicatorId: number;
  indicatorCode: string;
  indicatorName: string;
  unit: string;
  valueType: number;
  valueOptions?: string;
  groupId: number;
  groupName: string;
  parentGroupName: string;
  valueA: any;
  valueB: any;
  diffType: 'up' | 'down' | 'different' | 'equal' | 'none';
}

/**
 * 对比数据响应
 */
export interface CompareDataResponse {
  reportA: DeclareProgressReport;
  reportB: DeclareProgressReport;
  indicators: CompareIndicatorRow[];
}

/**
 * 获取两条申报记录的对比数据
 */
export function getCompareData(reportIdA: number, reportIdB: number) {
  return requestClient.get<CompareDataResponse>(
    `/declare/progress-report/compare-data?reportIdA=${reportIdA}&reportIdB=${reportIdB}`,
  );
}

/**
 * ========== 国家局高级搜索 ==========
 */

/** 单个指标条件 */
export interface IndicatorCondition {
  indicatorId: number;
  indicatorCode: string;
  valueType: number;
  operator: string;
  value: string;
  value2?: string;
  fieldCode?: string;
  fieldType?: string;
  indicatorOptions?: string;
}

/** 指标条件组 */
export interface IndicatorConditionGroup {
  innerLogic: 'AND' | 'OR';
  conditions: IndicatorCondition[];
}

/** 国家局高级搜索请求参数 */
export interface NationalSearchParams {
  hospitalName?: string;
  reportYear?: number;
  reportBatch?: number;
  reportStatus?: string;
  provinceStatus?: number;
  nationalReportStatus?: number;
  projectType?: number;
  indicatorGroups?: IndicatorConditionGroup[];
}

/** 国家局高级搜索 */
export function nationalSearch(params: NationalSearchParams) {
  return requestClient.post<DeclareProgressReport[]>(
    '/declare/progress-report/national-search',
    params,
  );
}

/**
 * 操作符配置（snake_case）
 */
export const OPERATORS: Record<number, { label: string; value: string }[]> = {
  1: [
    { label: '=', value: 'eq' },
    { label: '≠', value: 'neq' },
    { label: '>', value: 'gt' },
    { label: '≥', value: 'gte' },
    { label: '<', value: 'lt' },
    { label: '≤', value: 'lte' },
    { label: '为空', value: 'is_empty' },
    { label: '不为空', value: 'is_not_empty' },
  ],
  2: [
    { label: '等于', value: 'eq' },
    { label: '不等于', value: 'neq' },
    { label: '包含', value: 'contains' },
    { label: '开头是', value: 'starts_with' },
    { label: '为空', value: 'is_empty' },
    { label: '不为空', value: 'is_not_empty' },
  ],
  3: [
    { label: '是', value: 'eq' },
    { label: '否', value: 'neq' },
    { label: '为空', value: 'is_empty' },
    { label: '不为空', value: 'is_not_empty' },
  ],
  4: [
    { label: '等于', value: 'eq' },
    { label: '不等于', value: 'neq' },
    { label: '早于', value: 'lt' },
    { label: '晚于', value: 'gt' },
    { label: '区间', value: 'between' },
    { label: '为空', value: 'is_empty' },
    { label: '不为空', value: 'is_not_empty' },
  ],
  5: [
    { label: '包含', value: 'contains' },
    { label: '为空', value: 'is_empty' },
    { label: '不为空', value: 'is_not_empty' },
  ],
  6: [
    { label: '等于', value: 'eq' },
    { label: '不等于', value: 'neq' },
    { label: '为空', value: 'is_empty' },
    { label: '不为空', value: 'is_not_empty' },
  ],
  7: [
    { label: '包含任一', value: 'has_any' },
    { label: '包含全部', value: 'has_all' },
    { label: '为空', value: 'is_empty' },
    { label: '不为空', value: 'is_not_empty' },
  ],
  8: [
    { label: '相交', value: 'overlaps' },
    { label: '包含', value: 'contains' },
    { label: '为空', value: 'is_empty' },
    { label: '不为空', value: 'is_not_empty' },
  ],
};
// type=10 同 type=6, type=11 同 type=7
OPERATORS[10] = OPERATORS[6];
OPERATORS[11] = OPERATORS[7];

/** 值类型映射到组件 */
export const VALUE_TYPE_COMPONENTS: Record<number, string> = {
  1: 'number',
  2: 'text',
  3: 'boolean',
  4: 'date',
  5: 'textarea',
  6: 'select',
  7: 'multiSelect',
  8: 'dateRange',
  10: 'select',
  11: 'multiSelect',
  12: 'dynamicContainer',
};

// ========== 国家局导出 ==========

/** 导出请求参数 */
export interface NationalExportParams {
  /** 医院名称（模糊匹配） */
  hospitalName?: string;
  /** 填报年度 */
  reportYear?: number;
  /** 填报批次 */
  reportBatch?: number;
  /** 填报状态 */
  reportStatus?: string;
  /** 省级审核状态 */
  provinceStatus?: number;
  /** 国家局上报状态 */
  nationalReportStatus?: number;
  /** 项目类型 (1-6) */
  projectType?: number;
  /** 指标条件组列表 */
  indicatorGroups?: IndicatorConditionGroup[];
  /** 是否包含动态容器明细 */
  includeContainerDetail?: boolean;
  /** 填报记录ID列表（优先级最高） */
  reportIds?: number[];
}

/**
 * 导出国家局填报数据 (简单条件)
 * GET 请求，所有参数通过 queryString 传递
 */
export function exportNationalReport(params: NationalExportParams) {
  return requestClient.download('/declare/progress-report/export-national', { params });
}

/**
 * 导出国家局填报数据 (高级条件)
 * POST 请求，支持指标条件筛选
 */
export function exportNationalReportAdvanced(params: NationalExportParams) {
  return requestClient.download('/declare/progress-report/export-national-advanced', {
    method: 'POST',
    body: params,
  });
}
