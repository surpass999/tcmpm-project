import type { PageParam, PageResult } from '@vben/request';

import type { BpmProcessInstanceApi } from '../processInstance';

import { requestClient } from '#/api/request';

export namespace BpmTaskApi {
  /** 流程任务 */
  export interface Task {
    id: string; // 编号
    name: string; // 任务名字
    status: number; // 任务状态
    createTime: number; // 创建时间
    endTime: number; // 结束时间
    durationInMillis: number; // 持续时间
    reason: string; // 审批理由
    ownerUser: any; // 负责人
    assigneeUser: any; // 处理人
    taskDefinitionKey: string; // 任务定义的标识
    processInstanceId: string; // 流程实例id
    processInstance: BpmProcessInstanceApi.ProcessInstance; // 流程实例
    parentTaskId: any; // 父任务id
    children: any; // 子任务
    formId: any; // 表单id
    formName: any; // 表单名称
    formConf: any; // 表单配置
    formFields: any; // 表单字段
    formVariables: any; // 表单变量
    buttonsSetting: any; // 按钮设置
    signEnable: any; // 签名设置
    reasonRequire: any; // 原因设置
    nodeType: any; // 节点类型
  }
}

/** 查询待办任务分页 */
export async function getTaskTodoPage(params: PageParam) {
  return requestClient.get<PageResult<BpmTaskApi.Task>>('/bpm/task/todo-page', {
    params,
  });
}

/** 查询已办任务分页 */
export async function getTaskDonePage(params: PageParam) {
  return requestClient.get<PageResult<BpmTaskApi.Task>>('/bpm/task/done-page', {
    params,
  });
}

/** 查询任务管理分页 */
export async function getTaskManagerPage(params: PageParam) {
  return requestClient.get<PageResult<BpmTaskApi.Task>>(
    '/bpm/task/manager-page',
    { params },
  );
}

/** 审批任务 */
export const approveTask = async (data: any) => {
  return await requestClient.put('/bpm/declare/task/approve', data);
};

/** 驳回任务 */
export const rejectTask = async (data: any) => {
  return await requestClient.put('/bpm/declare/task/reject', data);
};

// ========== 批量查询任务状态 API ==========

export namespace BpmTaskBatchStatusApi {
  export interface ReqVO {
    processInstanceIds: string[];
  }

  export interface ButtonSetting {
    displayName: string;
    enable: boolean;
    bizStatus?: string;
    rectifyProcessDefinitionKey?: string;
  }

  export interface TodoTask {
    taskId: string;
    taskName: string;
    taskDefinitionKey: string;
    createTime?: string;
    formId?: number;
    formName?: string;
    formConf?: string;
    buttonSettings?: Record<string, ButtonSetting>;
    signEnable?: boolean;
    reasonRequire?: boolean;
    nodeType?: string;
  }

  export interface DoneTask {
    taskId: string;
    taskName: string;
    taskDefinitionKey: string;
    endTime?: string;
    approved?: boolean;
    status?: number;
    reason?: string;
    assigneeUser?: any;
  }

  export interface RespVO {
    processInstanceId: string;
    hasTodoTask: boolean;
    todoTasks: TodoTask[];
    doneTasks: DoneTask[];
    allDoneTasks: DoneTask[];
  }
}

/**
 * 根据流程实例ID批量查询任务状态
 * 用于获取按钮配置
 */
export const getTaskBatchStatusByProcessInstanceIds = async (data: BpmTaskBatchStatusApi.ReqVO) => {
  return await requestClient.post<BpmTaskBatchStatusApi.RespVO[]>(
    '/bpm/declare/task-status/batch-query',
    data,
  );
};

/** 根据流程实例 ID 查询任务列表 */
export const getTaskListByProcessInstanceId = async (id: string) => {
  return await requestClient.get(
    `/bpm/task/list-by-process-instance-id?processInstanceId=${id}`,
  );
};

/** 获取所有可退回的节点 */
export const getTaskListByReturn = async (id: string) => {
  return await requestClient.get(`/bpm/declare/task/list-by-return?id=${id}`);
};

/** 退回任务 */
export const returnTask = async (data: any) => {
  return await requestClient.put('/bpm/declare/task/return', data);
};

/** 委派任务 */
export const delegateTask = async (data: any) => {
  return await requestClient.put('/bpm/declare/task/delegate', data);
};

/** 转派任务 */
export const transferTask = async (data: any) => {
  return await requestClient.put('/bpm/declare/task/transfer', data);
};

/** 加签任务 */
export const signCreateTask = async (data: any) => {
  return await requestClient.put('/bpm/declare/task/create-sign', data);
};

/** 减签任务 */
export const signDeleteTask = async (data: any) => {
  return await requestClient.delete('/bpm/declare/task/delete-sign', data);
};

/** 抄送任务 */
export const copyTask = async (data: any) => {
  return await requestClient.put('/bpm/declare/task/copy', data);
};

/** 获取加签任务列表 */
export const getChildrenTaskList = async (id: string) => {
  return await requestClient.get(
    `/bpm/task/list-by-parent-task-id?parentTaskId=${id}`,
  );
};

/** 撤回任务 */
export const withdrawTask = async (taskId: string) => {
  return await requestClient.put('/bpm/task/withdraw', null, {
    params: { taskId },
  });
};

/** 获取下一个审批节点信息（用于判断是否需要选择审批人） */
export const getNextApprovalNodes = async (params: {
  processInstanceId: string;
  taskId: string;
  processVariablesStr?: string;
}) => {
  return await requestClient.get('/bpm/process-instance/get-next-approval-nodes', {
    params,
  });
};

/** 设置下一个节点的审批人 */
export const setNextAssignees = async (data: {
  id: string;
  nextAssignees: Record<string, number[]>;
}) => {
  return await requestClient.put('/bpm/declare/task/next-assignees', data);
};

/** 选择专家 - 设置下一个节点审批人后自动通过当前任务 */
export const selectExpert = async (data: {
  id: string;
  userIds: number[];
  buttonId?: number;
}) => {
  return await requestClient.put('/bpm/declare/task/select-expert', data);
};

// ========== 批量审批 API ==========

export namespace BpmTaskBatchActionApi {
  /** 任务项 */
  export interface TaskItem {
    /** 任务编号 */
    id: string;
    /** 按钮 ID（对应按钮设置的 Id，用于获取 bizStatus） */
    buttonId?: number;
    /** 退回到的任务 Key（仅退回操作时使用） */
    targetTaskDefinitionKey?: string;
    /** 业务ID（用于前端定位记录） */
    businessId?: number;
  }

  /** 批量审批请求 VO */
  export interface ReqVO {
    /** 任务操作类型：1=通过, 2=驳回, 3=退回 */
    actionType: number;
    /** 任务列表 */
    tasks: TaskItem[];
    /** 公共审批意见 */
    reason?: string;
    /** 公共签名图片URL */
    signPicUrl?: string;
    /** 公共流程变量 */
    variables?: Record<string, any>;
    /** 公共下一个节点审批人 */
    nextAssignees?: Record<string, number[]>;
  }

  /** 任务结果 */
  export interface TaskResult {
    /** 任务编号 */
    taskId: string;
    /** 业务ID */
    businessId?: number;
    /** 是否成功 */
    success: boolean;
    /** 错误编码 */
    errorCode?: string;
    /** 错误信息 */
    errorMsg?: string;
  }

  /** 批量审批响应 VO */
  export interface RespVO {
    /** 总任务数 */
    totalCount: number;
    /** 成功任务数 */
    successCount: number;
    /** 失败任务数 */
    failCount: number;
    /** 跳过任务数 */
    skipCount: number;
    /** 任务详情列表 */
    results: TaskResult[];
  }
}

/**
 * 批量审批任务
 * 支持批量通过、批量驳回、批量退回等操作
 */
export const batchActionTask = async (data: BpmTaskBatchActionApi.ReqVO) => {
  return await requestClient.put<BpmTaskBatchActionApi.RespVO>(
    '/bpm/declare/task/batch-action',
    data,
  );
};

/** 根据业务ID批量查询任务状态 */
export namespace BpmTaskByBusinessApi {
  export interface ReqVO {
    /** 业务表分类（对应 bpm_business_type.process_category） */
    tableName: string;
    /** 业务类型（对应 bpm_business_type.business_type，如 project_process:type:1） */
    businessType?: string;
    /** 业务ID列表 */
    businessIds: number[];
  }

  export interface TodoTask {
    taskId: string;
    taskName: string;
    taskDefinitionKey: string;
    createTime: string;
    formId?: number;
    formName?: string;
    formConf?: string;
    buttonSettings?: Record<string, { displayName: string; enable: boolean; bizStatus?: string; rectifyProcessDefinitionKey?: string }>;
    signEnable?: boolean;
    reasonRequire?: boolean;
    nodeType?: string;
  }

  export interface DoneTask {
    taskId: string;
    taskName: string;
    taskDefinitionKey: string;
    endTime: string;
    approved?: boolean;
    status?: number; // 任务状态：2=通过, 3=不通过, 5=退回
    reason?: string;
    assigneeUser?: any;
  }

  export interface RespVO {
    businessId: number;
    processInstanceId?: string;
    hasTodoTask: boolean;
    todoTasks: TodoTask[];
    doneTasks: DoneTask[];
    allDoneTasks: DoneTask[];
  }
}

/**
 * 根据业务ID批量查询任务状态
 * 用于列表页面批量获取多个业务ID的待办/已办任务
 */
export const getTaskByBusiness = async (data: BpmTaskByBusinessApi.ReqVO) => {
  return await requestClient.post<BpmTaskByBusinessApi.RespVO[]>(
    '/bpm/declare/task-status/query-by-business',
    data,
  );
};

/** 根据业务ID查询流程信息 */
export namespace BpmProcessByBusinessApi {
  export interface ReqVO {
    /** 业务表分类（对应 bpm_business_type.process_category） */
    tableName: string;
    /** 业务类型（对应 bpm_business_type.business_type，如 project_process:type:1） */
    businessType?: string;
    /** 业务ID */
    businessId: number;
  }

  export interface ActivityNodeVO {
    id: string;
    name: string;
    nodeType: number;
    status: number;
    startTime?: string;
    endTime?: string;
    tasks?: ActivityNodeTaskVO[];
  }

  export interface ActivityNodeTaskVO {
    id: string;
    ownerUser?: any;
    assigneeUser?: any;
    reason?: string;
    endTime?: string;
  }

  export interface RespVO {
    processInstanceId: string;
    processDefinitionKey?: string;
    processDefinitionName?: string;
    status: string; // RUNNING: 运行中, ENDED: 已结束
    startTime?: string;
    endTime?: string;
    activityNodes: ActivityNodeVO[];
  }
}

/**
 * 根据业务ID查询流程信息
 * 返回所有关联的流程实例及节点信息，按创建时间倒序
 */
export const getProcessByBusiness = async (data: BpmProcessByBusinessApi.ReqVO) => {
  return await requestClient.post<BpmProcessByBusinessApi.RespVO[]>(
    '/bpm/declare/task-status/process-query-by-business',
    data,
  );
};
