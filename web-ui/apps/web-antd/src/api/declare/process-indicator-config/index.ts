import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

/** 过程指标配置 */
export namespace ProcessIndicatorConfigApi {
  /** 过程指标配置保存请求 */
  export interface ConfigSaveParams {
    id?: number;
    processType: number;
    projectType: number;
    indicatorId: number;
    isRequired: boolean;
    sort: number;
  }

  /** 过程指标配置响应 */
  export interface ConfigResp {
    id?: number;
    processType: number;
    processTypeName: string;
    projectType: number;
    projectTypeName: string;
    indicatorId: number;
    indicatorCode: string;
    indicatorName: string;
    unit: string;
    category: number;
    valueType: number;
    isRequired: boolean;
    sort: number;
  }
}

/** 获取过程指标配置分页列表 */
export async function getProcessIndicatorConfigPage(params: PageParam & {
  processType?: number;
  projectType?: number;
  indicatorId?: number;
  indicatorName?: string;
}) {
  return requestClient.get<PageResult<ProcessIndicatorConfigApi.ConfigResp>>(
    '/declare/process-indicator-config/page',
    { params }
  );
}

/** 获取过程指标配置详情 */
export async function getProcessIndicatorConfig(id: number) {
  return requestClient.get<ProcessIndicatorConfigApi.ConfigResp>(
    `/declare/process-indicator-config/get?id=${id}`
  );
}

/** 根据过程类型和项目类型获取已配置的指标列表 */
export async function getProcessIndicatorConfigListByProcessType(processType: number, projectType?: number) {
  const params = new URLSearchParams({ processType: String(processType) });
  if (projectType !== undefined) {
    params.append('projectType', String(projectType));
  }
  return requestClient.get<ProcessIndicatorConfigApi.ConfigResp[]>(
    `/declare/process-indicator-config/list-by-process-type?${params.toString()}`
  );
}

/** 根据过程类型和项目类型获取已配置的指标ID列表 */
export async function getIndicatorIdsByProcessType(processType: number, projectType?: number) {
  const params = new URLSearchParams({ processType: String(processType) });
  if (projectType !== undefined) {
    params.append('projectType', String(projectType));
  }
  return requestClient.get<number[]>(
    `/declare/process-indicator-config/indicator-ids-by-process-type?${params.toString()}`
  );
}

/** 创建过程指标配置 */
export async function createProcessIndicatorConfig(data: ProcessIndicatorConfigApi.ConfigSaveParams) {
  return requestClient.post<number>(
    '/declare/process-indicator-config/create',
    data
  );
}

/** 更新过程指标配置 */
export async function updateProcessIndicatorConfig(data: ProcessIndicatorConfigApi.ConfigSaveParams) {
  return requestClient.put<boolean>(
    '/declare/process-indicator-config/update',
    data
  );
}

/** 删除过程指标配置 */
export async function deleteProcessIndicatorConfig(id: number) {
  return requestClient.delete<boolean>(
    `/declare/process-indicator-config/delete?id=${id}`
  );
}

/** 批量保存过程指标配置 */
export async function saveProcessIndicatorConfigs(processType: number, projectType: number, configs: ProcessIndicatorConfigApi.ConfigSaveParams[]) {
  return requestClient.put<boolean>(
    `/declare/process-indicator-config/save-batch?processType=${processType}&projectType=${projectType}`,
    configs
  );
}
