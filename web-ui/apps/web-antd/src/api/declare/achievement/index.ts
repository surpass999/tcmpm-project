import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';
import { BUSINESS_TYPE } from '#/constants/bpm/business-type';

export namespace DeclareAchievementApi {
  /** 成果与流通信息 */
  export interface Achievement {
    // 基础字段（deptId/projectId 由后端自动设置，前端不填）
    id: number;
    processInstanceId?: string;
    deptId?: number;
    projectName?: string;

    // 成果基本信息
    achievementName?: string;
    achievementType?: number;
    applicationField?: string;
    description?: string;
    effectDescription?: string;
    replicationValue?: number;
    promotionScope?: number;
    promotionCount?: number;
    transformType?: number;

    // 数据流通信息
    dataName?: string;
    dataDescription?: string;
    dataType?: string;
    dataSource?: string;
    dataVolume?: string;
    dataQuality?: number;
    shareScope?: number;
    flowType?: number;
    flowObject?: string;
    flowPurpose?: string;
    securityFilingNo?: string;
    securityFilingTime?: string;
    startTime?: string;
    endTime?: string;

    // 证书信息（指标 601, 602）
    certificateCount?: number;
    propertyCount?: number;

    // 交易信息（指标 603, 604）
    transactionCount?: number;
    transactionAmount?: number;
    transactionObject?: string;
    transactionTime?: string;
    transactionContract?: string;

    // 附件和状态
    attachmentIds?: string;
    status?: string;
    auditOpinion?: string;

    // 审核状态
    auditStatus?: number;
    auditorId?: number;
    auditTime?: string;

    // 推荐状态
    recommendStatus?: number;
    recommendOpinion?: string;
    recommenderId?: number;
    recommendTime?: string;

    // 审计字段
    createTime?: string;
    creator?: number;
    creatorName?: string;
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

/** 提交成果审核（发起 BPM 流程，对齐备案模块） */
export function submitAchievement(id: number, processDefinitionKey?: string) {
  return requestClient.post<string>(
    '/bpm/process/start',
    {
      businessId: id,
      businessType: BUSINESS_TYPE.ACHIEVEMENT_SUBMIT,
      processDefinitionKey: processDefinitionKey || undefined,
    },
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

/** 推荐成果至推广库（遴选完成） */
export function recommendToLibrary(id: number) {
  return requestClient.post<boolean>(
    '/declare/achievement/recommend-to-library',
    null,
    { params: { id } },
  );
}
