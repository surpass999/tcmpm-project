import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace DeclareIndicatorApi {
  /** 指标 */
  export interface Indicator {
    id?: number;
    indicatorCode: string;
    indicatorName: string;
    unit: string;
    caliberId?: number;
    logicRule: string;
    calculationRule: string;
    valueType: number;
    valueOptions: string;
    extraConfig?: string;
    isRequired: boolean;
    minValue?: number;
    maxValue?: number;
    sort: number;
    showInList: boolean;
    childrenIndicatorCodes: string;
    projectType?: number;
    businessType: string;
    groupId?: number;
    createTime?: string;

    // ========== 口径字段 ==========
    /** 指标解释（Hover显示） */
    definition?: string;
    /** 统计范围 */
    statisticScope?: string;
    /** 数据来源 */
    dataSource?: string;
    /** 填报要求 */
    fillRequire?: string;
    /** 计算示例 */
    calculationExample?: string;
  }

  /** 指标保存请求 */
  export interface IndicatorSaveParams {
    id?: number;
    indicatorCode: string;
    indicatorName: string;
    unit: string;
    caliberId?: number;
    logicRule: string;
    calculationRule: string;
    valueType: number;
    valueOptions: string;
    extraConfig?: string;
    isRequired: boolean;
    minValue?: number;
    maxValue?: number;
    sort: number;
    showInList: boolean;
    childrenIndicatorCodes: string;
    projectType?: number;
    businessType: string;
    groupId?: number;
  }
}

/** 获取指标分页列表 */
export async function getIndicatorPage(params: PageParam & {
  indicatorCode?: string;
  indicatorName?: string;
  projectType?: number;
  businessType?: string;
}) {
  return requestClient.get<PageResult<DeclareIndicatorApi.Indicator>>(
    '/declare/indicator/page',
    { params }
  );
}

/** 获取指标详情 */
export async function getIndicator(id: number) {
  return requestClient.get<DeclareIndicatorApi.Indicator>(
    `/declare/indicator/get?id=${id}`
  );
}

/** 根据项目类型和业务类型获取指标列表 */
export async function getIndicatorsByProjectType(projectType: number, businessType: string) {
  return requestClient.get<DeclareIndicatorApi.Indicator[]>(
    `/declare/indicator/list-by-project-type?projectType=${projectType}&businessType=${businessType}`
  );
}

/** 根据业务类型获取指标列表 */
export async function getIndicatorsByBusinessType(businessType: string) {
  return requestClient.get<DeclareIndicatorApi.Indicator[]>(
    `/declare/indicator/list-by-business-type?businessType=${businessType}`
  );
}

/** 创建指标 */
export async function createIndicator(data: DeclareIndicatorApi.IndicatorSaveParams) {
  return requestClient.post('/declare/indicator/create', data);
}

/** 更新指标 */
export async function updateIndicator(data: DeclareIndicatorApi.IndicatorSaveParams) {
  return requestClient.put('/declare/indicator/update', data);
}

/** 删除指标 */
export async function deleteIndicator(id: number) {
  return requestClient.delete(`/declare/indicator/delete?id=${id}`);
}

/** 获取需要在列表显示的指标（showInList=true） */
export async function getIndicatorsForListDisplay(businessType: string) {
  return requestClient.get<DeclareIndicatorApi.Indicator[]>(
    `/declare/indicator/list-for-list-display?businessType=${businessType}`
  );
}

/** 获取上期填报值（businessType 默认为 3：进度类型） */
export async function getLastPeriodValues(hospitalId: number, reportYear: number, reportBatch: number, businessType: number = 3) {
  return requestClient.get<Record<string, string>>(
    `/declare/indicator-value/last-period-values?hospitalId=${hospitalId}&reportYear=${reportYear}&reportBatch=${reportBatch}&businessType=${businessType}`,
  );
}

/** 指标值类型 */
export interface IndicatorValue {
  indicatorId: number;
  indicatorCode: string;
  valueType: number;
  valueNum?: string;
  valueStr?: string;
  valueBool?: boolean;
  valueDate?: string;
  valueDateStart?: string;
  valueDateEnd?: string;
  valueText?: string;
}

/** 获取进度填报的指标值列表（businessType 默认为 3：进度类型） */
export async function getProgressReportIndicatorValues(reportId: number, businessType: number = 3) {
  return requestClient.get<IndicatorValue[]>(
    `/declare/indicator-value/progress-report-values?reportId=${reportId}&businessType=${businessType}`,
  );
}

/** 保存进度填报指标值 */
export async function saveProgressReportIndicatorValues(reportId: number, values: IndicatorValue[]) {
  return requestClient.post('/declare/indicator-value/save', {
    businessType: 3,
    businessId: reportId,
    values,
  });
}
