import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace BpmProcessNodeDslApi {
  /** 流程节点DSL配置 */
  export interface Dsl {
    id?: number;
    processDefinitionKey: string;
    nodeKey: string;
    nodeName: string;
    dslConfig: string;
    enabled: number;
    remark: string;
    createTime?: string;
    updateTime?: string;
  }

  /** DSL配置保存请求 */
  export interface DslSaveParams {
    id?: number;
    processDefinitionKey: string;
    nodeKey: string;
    nodeName: string;
    dslConfig: string;
    enabled: number;
    remark: string;
  }
}

/** 获取DSL配置分页列表 */
export async function getDslPage(params: PageParam & {
  processDefinitionKey?: string;
  nodeKey?: string;
  nodeName?: string;
  enabled?: number;
}) {
  return requestClient.get<PageResult<BpmProcessNodeDslApi.Dsl>>('/bpm/process-node-dsl/page', {
    params,
  });
}

/** 获取DSL配置详情 */
export async function getDsl(id: number) {
  return requestClient.get<BpmProcessNodeDslApi.Dsl>(`/bpm/process-node-dsl/get?id=${id}`);
}

/** 根据流程定义Key和节点Key获取DSL配置 */
export async function getDslByNodeKey(processDefinitionKey: string, nodeKey: string) {
  return requestClient.get<BpmProcessNodeDslApi.Dsl>(
    `/bpm/process-node-dsl/get-by-node?processDefinitionKey=${processDefinitionKey}&nodeKey=${nodeKey}`
  );
}

/** 根据流程定义Key获取所有DSL配置 */
export async function getDslListByProcessKey(processDefinitionKey: string) {
  return requestClient.get<BpmProcessNodeDslApi.Dsl[]>(
    `/bpm/process-node-dsl/list-by-process?processDefinitionKey=${processDefinitionKey}`
  );
}

/** 获取所有已启用的DSL配置 */
export async function getAllEnabledDslList() {
  return requestClient.get<BpmProcessNodeDslApi.Dsl[]>('/bpm/process-node-dsl/list-all-enabled');
}

/** 创建DSL配置 */
export async function createDsl(data: BpmProcessNodeDslApi.DslSaveParams) {
  return requestClient.post('/bpm/process-node-dsl/create', data);
}

/** 更新DSL配置 */
export async function updateDsl(data: BpmProcessNodeDslApi.DslSaveParams) {
  return requestClient.put('/bpm/process-node-dsl/update', data);
}

/** 删除DSL配置 */
export async function deleteDsl(id: number) {
  return requestClient.delete(`/bpm/process-node-dsl/delete?id=${id}`);
}

/** 验证DSL配置JSON */
export async function validateDslConfig(dslConfig: string) {
  return requestClient.post('/bpm/process-node-dsl/validate', null, {
    params: { dslConfig },
  });
}
