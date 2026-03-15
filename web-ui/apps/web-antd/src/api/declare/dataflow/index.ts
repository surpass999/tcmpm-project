import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace DeclareDataFlowApi {
  /** 数据流通记录信息 */
  export interface DataFlow {
    id: number;
    processInstanceId?: string;
    projectId?: number;
    projectName?: string;
    dataName?: string;
    dataDescription?: string;
    dataType?: string;
    dataSource?: string;
    dataVolume?: string;
    dataQuality?: number;
    shareScope?: number;
    startTime?: string;
    endTime?: string;
    flowType?: number;
    flowObject?: string;
    flowPurpose?: string;
    securityFilingNo?: string;
    securityFilingTime?: string;
    certificateCount?: number;
    propertyCount?: number;
    status?: number;
    auditOpinion?: string;
    auditorId?: number;
    auditTime?: string;
    hasAchievement?: boolean;
    createTime?: string;
    creator?: number;
  }
}

/** 查询数据流通记录分页 */
export function getDataFlowPage(params: PageParam) {
  return requestClient.get<PageResult<DeclareDataFlowApi.DataFlow>>(
    '/declare/data-flow/page',
    { params },
  );
}

/** 查询数据流通记录详情 */
export function getDataFlow(id: number) {
  return requestClient.get<DeclareDataFlowApi.DataFlow>(
    `/declare/data-flow/get?id=${id}`,
  );
}

/** 创建数据流通记录 */
export function createDataFlow(data: DeclareDataFlowApi.DataFlow) {
  return requestClient.post<number>('/declare/data-flow/create', data);
}

/** 更新数据流通记录 */
export function updateDataFlow(data: DeclareDataFlowApi.DataFlow) {
  return requestClient.put<boolean>('/declare/data-flow/update', data);
}

/** 删除数据流通记录 */
export function deleteDataFlow(id: number) {
  return requestClient.delete<boolean>(`/declare/data-flow/delete?id=${id}`);
}

/** 提交数据流通审核 */
export function submitDataFlow(id: number, processDefinitionKey?: string) {
  return requestClient.post<string>(
    '/declare/data-flow/submit',
    null,
    { params: { id, processDefinitionKey } },
  );
}
