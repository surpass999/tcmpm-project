import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace DeclareIndicatorCaliberApi {
  /** 指标口径 */
  export interface Caliber {
    id?: number;
    indicatorId: number;
    indicatorName?: string;
    indicatorCode?: string;
    definition: string;
    statisticScope: string;
    dataSource: string;
    fillRequire: string;
    calculationExample: string;
    status?: number;
    createTime?: string;
    projectType?: number;
  }

  /** 指标口径保存请求 */
  export interface CaliberSaveParams {
    id?: number;
    indicatorId: number;
    indicatorName?: string;
    definition: string;
    statisticScope: string;
    dataSource: string;
    fillRequire: string;
    calculationExample: string;
    status?: number;
    projectType?: number;
  }
}

/** 获取指标口径分页列表 */
export async function getCaliberPage(params: PageParam & {
  projectType?: number;
  indicatorId?: number;
  definition?: string;
}) {
  return requestClient.get<PageResult<DeclareIndicatorCaliberApi.Caliber>>(
    '/declare/indicator-caliber/page',
    { params }
  );
}

/** 获取指标口径详情 */
export async function getCaliber(id: number) {
  return requestClient.get<DeclareIndicatorCaliberApi.Caliber>(
    `/declare/indicator-caliber/get?id=${id}`
  );
}

/** 根据指标ID获取口径 */
export async function getCaliberByIndicatorId(indicatorId: number) {
  return requestClient.get<DeclareIndicatorCaliberApi.Caliber>(
    `/declare/indicator-caliber/get-by-indicator?indicatorId=${indicatorId}`
  );
}

/** 创建指标口径 */
export async function createCaliber(data: DeclareIndicatorCaliberApi.CaliberSaveParams) {
  return requestClient.post('/declare/indicator-caliber/create', data);
}

/** 更新指标口径 */
export async function updateCaliber(data: DeclareIndicatorCaliberApi.CaliberSaveParams) {
  return requestClient.put('/declare/indicator-caliber/update', data);
}

/** 删除指标口径 */
export async function deleteCaliber(id: number) {
  return requestClient.delete(`/declare/indicator-caliber/delete?id=${id}`);
}

/** 更新指标口径状态 */
export async function updateCaliberStatus(id: number, status: number) {
  return requestClient.put(`/declare/indicator-caliber/update-status?id=${id}&status=${status}`);
}

/** 获取指标口径列表 */
export async function getCaliberList() {
  return requestClient.get<DeclareIndicatorCaliberApi.Caliber[]>(
    '/declare/indicator-caliber/list'
  );
}
