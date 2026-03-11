/**
 * BPM 按钮定义 API
 */

import { requestClient } from '#/api/request';

/** 按钮定义 */
export namespace BpmActionApi {
  export interface BpmAction {
    /** 按钮标识（对应 DSL 中的 actions 值） */
    key: string;
    /** 显示中文名称 */
    label: string;
    /** 默认业务状态值 */
    bizStatus?: string;
    /** 业务状态中文名称 */
    bizStatusLabel?: string;
    /** BPM 操作类型 */
    bpmAction?: string;
    /** DSL 变量配置 */
    vars?: {
      reason?: string;
      reasonRequired?: boolean;
      isReturned?: boolean;
      isStartNode?: boolean;
      backStrategy?: string;
      [key: string]: any;
    };
  }
}

/** 获取所有按钮定义列表 */
export function getBpmActionList() {
  return requestClient.get<BpmActionApi.BpmAction[]>('/bpm/action/list');
}

/** 根据 key 获取按钮定义 */
export function getBpmAction(key: string) {
  return requestClient.get<BpmActionApi.BpmAction>(`/bpm/action/${key}`);
}

/** 获取当前用户对指定业务可执行的操作列表 */
export function getAvailableActions(businessType: string, businessId: number) {
  return requestClient.get<(BpmActionApi.BpmAction & { taskId?: string; processInstanceId?: string })[]>('/bpm/action/available', {
    params: { businessType, businessId },
  });
}

/** 批量获取当前用户对多个业务可执行的操作列表 */
export function getAvailableActionsBatch(businessType: string, businessIds: number[]) {
  return requestClient.get<(BpmActionApi.BpmAction & { taskId?: string; processInstanceId?: string })[]>(
    '/bpm/action/available-batch',
    { params: { businessType, businessIds: businessIds.join(',') } }
  );
}

/** 提交操作请求参数 */
export interface SubmitActionParams {
  /** 业务类型 */
  businessType: string;
  /** 业务ID */
  businessId: number;
  /** 操作标识 */
  actionKey: string;
  /** 审批意见 */
  reason?: string;
  /** 选择的专家用户ID列表（选择专家操作时使用） */
  expertUserIds?: number[];
  /** 退回时的目标节点（退回操作时使用） */
  targetNodeKey?: string;
}

/** 提交操作（完成任务，推进流程） */
export function submitBpmAction(params: SubmitActionParams) {
  return requestClient.post<boolean>('/bpm/action/submit', params);
}
