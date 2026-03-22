/**
 * BPM 按钮定义 API
 */

import { requestClient } from '#/api/request';
import { getTaskByBusiness } from '../task';

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
      /** 整改流程定义 Key（发起整改按钮时使用） */
      rectifyProcessDefinitionKey?: string;
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
export async function getAvailableActions(businessType: string, businessId: number, processCategory?: string) {
  // processCategory 对应 bpm_business_type.process_category（如 project, filing）
  // businessType 对应 bpm_business_type.business_type（如 project_process:type:1）
  // 如果传入了 processCategory，则使用它作为 tableName，并可选传入 businessType 进行精确查询
  const tableName = processCategory || businessType;
  const result = await getTaskByBusiness({
    tableName,
    businessType: processCategory ? businessType : undefined,
    businessIds: [businessId],
  });

  const taskStatus = result?.[0];
  if (!taskStatus) {
    return [];
  }

  // 如果有待办任务，从 buttonSettings 构建操作按钮
  if (taskStatus.hasTodoTask && taskStatus.todoTasks?.[0]) {
    const todoTask = taskStatus.todoTasks[0];
    const buttons = todoTask.buttonSettings || {};

    // 返回对象格式，供 ActionButtonCmp 使用
    const actionsMap: Record<number, { displayName: string; enable: boolean; rectifyProcessDefinitionKey?: string }> = {};
    Object.entries(buttons).forEach(([key, setting]: [string, any]) => {
      actionsMap[parseInt(key)] = {
        displayName: setting.displayName,
        enable: setting.enable,
        rectifyProcessDefinitionKey: setting.rectifyProcessDefinitionKey,
      };
    });

    // 同时返回 taskInfo 供提交时使用
    return {
      actions: actionsMap,
      taskInfo: {
        taskId: todoTask.taskId,
        processInstanceId: taskStatus.processInstanceId,
      },
    };
  }

  // 无操作权限
  return [];
}

/** 批量获取当前用户对多个业务可执行的操作列表 */
export async function getAvailableActionsBatch(businessType: string, businessIds: number[], processCategory?: string) {
  // processCategory 对应 bpm_business_type.process_category（如 project, filing）
  // businessType 对应 bpm_business_type.business_type（如 project_process:type:1）
  // 如果传入了 processCategory，则使用它作为 tableName，并可选传入 businessType 进行精确查询
  const tableName = processCategory || businessType;
  const result = await getTaskByBusiness({
    tableName,
    businessType: processCategory ? businessType : undefined,
    businessIds,
  });

  if (!result || result.length === 0) {
    return [];
  }

  // 批量构建每个业务的操作按钮
  const allActions: (BpmActionApi.BpmAction & { taskId?: string; processInstanceId?: string; businessId?: number })[] = [];

  for (const taskStatus of result) {
    const businessId = taskStatus.businessId;

    // 如果有待办任务，从 buttonSettings 构建操作按钮
    if (taskStatus.hasTodoTask && taskStatus.todoTasks?.[0]) {
      const todoTask = taskStatus.todoTasks[0];
      const buttons = todoTask.buttonSettings || {};
      const actions = Object.entries(buttons)
        .filter(([_, setting]: [string, any]) => setting.enable)
        .map(([key, setting]: [string, any]) => ({
          key,
          label: setting.displayName,
          taskId: todoTask.taskId,
          processInstanceId: taskStatus.processInstanceId,
          businessId,
          vars: {
            reasonRequired: todoTask.reasonRequire,
            rectifyProcessDefinitionKey: setting.rectifyProcessDefinitionKey,
          },
        }));
      allActions.push(...actions);
    }
  }

  return allActions;
}

/** 提交操作请求参数 */
export interface SubmitActionParams {
  /** 业务类型 */
  businessType: string;
  /** 业务ID */
  businessId: number;
  /** 操作标识（按钮ID） */
  actionKey: string;
  /** 审批意见 */
  reason?: string;
  /** 选择的专家用户ID列表（选择专家操作时使用） */
  expertUserIds?: number[];
  /** 退回时的目标节点（退回操作时使用） */
  targetNodeKey?: string;
  /** 任务ID（由调用方传入） */
  taskId?: string;
  /** 流程变量（如 rectifyProcessDefinitionKey） */
  variables?: Record<string, any>;
}

/**
 * 提交操作（完成任务，推进流程）
 * 根据操作类型调用不同的后端接口
 */
export function submitBpmAction(params: SubmitActionParams) {
  const { actionKey, expertUserIds, targetNodeKey, reason, taskId, variables } = params;

  // 选择专家：调用 select-expert 接口
  if (expertUserIds && expertUserIds.length > 0) {
    return requestClient.put<boolean>('/bpm/declare/task/select-expert', {
      id: taskId,
      userIds: expertUserIds,
      buttonId: parseInt(actionKey),
    });
  }

  // 退回操作：调用 return 接口
  if (targetNodeKey) {
    return requestClient.put<boolean>('/bpm/declare/task/return', {
      id: taskId,
      targetTaskDefinitionKey: targetNodeKey,
      reason: reason,
      buttonId: parseInt(actionKey),
    });
  }

  // 根据 actionKey 判断操作类型
  const actionId = parseInt(actionKey);
  switch (actionId) {
    case 1:
      // 通过：调用 approve 接口
      return requestClient.put<boolean>('/bpm/declare/task/approve', {
        id: taskId,
        reason: reason,
        buttonId: actionId,
        variables,
      });
    case 2:
      // 拒绝：调用 reject 接口
      return requestClient.put<boolean>('/bpm/declare/task/reject', {
        id: taskId,
        reason: reason,
        buttonId: actionId,
        variables,
      });
    case 9:
      // 发起整改：同样调用 approve 接口，由 bizStatus=NEED_RECTIFY 触发子流程
      return requestClient.put<boolean>('/bpm/declare/task/approve', {
        id: taskId,
        reason: reason,
        buttonId: actionId,
        variables,
      });
    default:
      console.warn('[BPM] 未知的操作类型:', actionKey);
      return Promise.reject(new Error('未知的操作类型'));
  }
}
