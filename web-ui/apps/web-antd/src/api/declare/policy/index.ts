import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace DeclarePolicyApi {
  /** 政策通知 */
  export interface Policy {
    id?: number;
    policyTitle: string;
    policyContent: string;
    policySummary?: string;
    releaseDept: number;
    releaseTime?: string;
    policyType: number;
    targetScope: number;
    targetProjectTypes?: string;
    attachmentUrls?: string[];
    status?: number;
    publisherId?: number;
    publisherName?: string;
    createTime?: string;
  }
}

/** 查询政策通知分页 */
export function getPolicyPage(params: PageParam & {
  policyTitle?: string;
  policyType?: number;
  targetProjectTypes?: string;
  releaseDept?: string;
  status?: number;
}) {
  return requestClient.get<PageResult<DeclarePolicyApi.Policy>>(
    '/declare/policy/page',
    { params },
  );
}

/** 查询政策通知详情 */
export function getPolicy(id: number) {
  return requestClient.get<DeclarePolicyApi.Policy>(
    `/declare/policy/get?id=${id}`,
  );
}

/** 创建政策通知 */
export function createPolicy(data: DeclarePolicyApi.Policy) {
  return requestClient.post<number>('/declare/policy/create', data);
}

/** 更新政策通知 */
export function updatePolicy(data: DeclarePolicyApi.Policy) {
  return requestClient.put<boolean>('/declare/policy/update', data);
}

/** 删除政策通知 */
export function deletePolicy(id: number) {
  return requestClient.delete<boolean>(`/declare/policy/delete?id=${id}`);
}

/** 发布政策通知 */
export function publishPolicy(id: number) {
  return requestClient.put<boolean>('/declare/policy/publish', null, {
    params: { id },
  });
}

/** 下架政策通知 */
export function unpublishPolicy(id: number) {
  return requestClient.put<boolean>('/declare/policy/unpublish', null, {
    params: { id },
  });
}

/** 获取已发布的政策列表 */
export function getPublishedPolicyList() {
  return requestClient.get<DeclarePolicyApi.Policy[]>(
    '/declare/policy/list-published',
  );
}
