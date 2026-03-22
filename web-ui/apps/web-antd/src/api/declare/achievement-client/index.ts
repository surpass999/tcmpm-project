import type { DeclareAchievementApi } from '#/api/declare/achievement';

import { requestClient } from '#/api/request';

/**
 * 用户端 - 成果与推广 API（公开接口，无需登录）
 * 用于成果展示页面（achievement-public）
 */
export namespace AchievementClientApi {
  /** 成果信息（精简版，用于列表展示） */
  export interface AchievementSummary {
    id: number;
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
    dataName?: string;
    dataType?: string;
    shareScope?: number;
    flowType?: number;
    attachmentIds?: string;
    auditStatus?: number;
    recommendStatus?: number;
    createTime?: string;
  }

  /** 成果详情（完整版） */
  export interface AchievementDetail extends DeclareAchievementApi.Achievement {}
}

/** 获取已发布/已推荐的成果列表（公开接口） */
export function getPublishedAchievementList() {
  return requestClient.get<AchievementClientApi.AchievementSummary[]>(
    '/client/achievement/list',
  );
}

/** 获取成果详情（公开接口） */
export function getPublishedAchievement(id: number) {
  return requestClient.get<AchievementClientApi.AchievementDetail>(
    `/client/achievement/get?id=${id}`,
  );
}
