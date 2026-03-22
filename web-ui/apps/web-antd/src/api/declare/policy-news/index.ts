import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace DeclarePolicyNewsApi {
  /** 政策资讯 */
  export interface PolicyNews {
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

/** 获取已发布的政策资讯列表 */
export function getPolicyNewsList() {
  return requestClient.get<DeclarePolicyNewsApi.PolicyNews[]>(
    '/declare/policy/list-published',
  );
}

/** 获取政策资讯详情 */
export function getPolicyNews(id: number) {
  return requestClient.get<DeclarePolicyNewsApi.PolicyNews>(
    `/declare/policy/get?id=${id}`,
  );
}
