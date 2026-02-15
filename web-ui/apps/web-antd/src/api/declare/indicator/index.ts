import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace DeclareIndicatorApi {
  /** 指标 */
  export interface Indicator {
    id?: number;
    indicatorCode: string;
    indicatorName: string;
    unit: string;
    category: number;
    caliberId?: number;
    logicRule: string;
    calculationRule: string;
    valueType: number;
    valueOptions: string;
    isRequired: boolean;
    minValue?: number;
    maxValue?: number;
    sort: number;
    showInList: boolean;
    childrenIndicatorCodes: string;
    projectType?: number;
    businessType: string;
    createTime?: string;
  }

  /** 指标保存请求 */
  export interface IndicatorSaveParams {
    id?: number;
    indicatorCode: string;
    indicatorName: string;
    unit: string;
    category: number;
    caliberId?: number;
    logicRule: string;
    calculationRule: string;
    valueType: number;
    valueOptions: string;
    isRequired: boolean;
    minValue?: number;
    maxValue?: number;
    sort: number;
    showInList: boolean;
    childrenIndicatorCodes: string;
    projectType?: number;
    businessType: string;
  }
}

/** 获取指标分页列表 */
export async function getIndicatorPage(params: PageParam & {
  indicatorCode?: string;
  indicatorName?: string;
  category?: number;
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
