import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace DeclareAchievementApi {
  /** 成果信息 */
  export interface Achievement {
    id: number;
    processInstanceId?: string;
    projectId?: number;
    projectName?: string;
    achievementName?: string;
    achievementType?: number;
    applicationField?: string;
    description?: string;
    effectDescription?: string;
    replicationValue?: number;
    promotionScope?: number;
    promotionCount?: number;
    transformType?: number;
    auditStatus?: number;
    auditOpinion?: string;
    auditorId?: number;
    auditTime?: string;
    recommendStatus?: number;
    recommendOpinion?: string;
    recommenderId?: number;
    recommendTime?: string;
    createTime?: string;
    creator?: number;
  }
}

/** 查询成果信息分页 */
export function getAchievementPage(params: PageParam) {
  return requestClient.get<PageResult<DeclareAchievementApi.Achievement>>(
    '/declare/achievement/page',
    { params },
  );
}

/** 查询成果信息详情 */
export function getAchievement(id: number) {
  return requestClient.get<DeclareAchievementApi.Achievement>(
    `/declare/achievement/get?id=${id}`,
  );
}

/** 创建成果信息 */
export function createAchievement(data: DeclareAchievementApi.Achievement) {
  return requestClient.post<number>('/declare/achievement/create', data);
}

/** 更新成果信息 */
export function updateAchievement(data: DeclareAchievementApi.Achievement) {
  return requestClient.put<boolean>('/declare/achievement/update', data);
}

/** 删除成果信息 */
export function deleteAchievement(id: number) {
  return requestClient.delete<boolean>(`/declare/achievement/delete?id=${id}`);
}

/** 提交成果审核 */
export function submitAchievement(id: number, processDefinitionKey?: string) {
  return requestClient.post<string>(
    '/declare/achievement/submit',
    null,
    { params: { id, processDefinitionKey } },
  );
}

/** 推荐成果至国家局 */
export function recommendToNation(id: number, opinion?: string) {
  return requestClient.post<boolean>(
    '/declare/achievement/recommend',
    null,
    { params: { id, opinion } },
  );
}
