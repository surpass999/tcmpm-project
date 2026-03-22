import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

/** Java LocalDateTime 序列化后的格式 */
export interface LocalDateTime {
  year: number;
  monthValue: number;
  dayOfMonth: number;
  hour: number;
  minute: number;
  second: number;
  nano: number;
}

export namespace DeclareReviewApi {
  /** 评审任务 */
  export interface ReviewTask {
    id?: number;
    processInstanceId?: string;
    taskDefinitionKey?: string;
    taskType?: number;
    businessType?: number;
    businessId?: number;
    taskName?: string;
    startTime?: string;
    endTime?: string;
    expertIds?: string;
    reviewStandard?: string;
    reviewRequirement?: string;
    totalScore?: number;
    reviewConclusion?: string;
    status?: number;
    createTime?: string;
    // 扩展字段
    experts?: ExpertSimple[];
    businessName?: string;
    businessUnitName?: string;
    businessProvinceName?: string;
    results?: ReviewResult[];
  }

  /** 专家简单信息 */
  export interface ExpertSimple {
    id: number;
    expertName?: string;
    workUnit?: string;
    status?: number;
  }

  /** 评审结果 */
  export interface ReviewResult {
    id?: number;
    taskId?: number;
    processInstanceId?: string;
    flowableTaskId?: string;
    expertId?: number;
    businessType?: number;
    businessId?: number;
    processType?: number; // 过程类型
    projectType?: number; // 项目类型
    status?: number;
    isConflict?: boolean;
    conflictCheckResult?: string;
    isAvoid?: boolean;
    avoidReason?: string;
    scoreData?: string;
    // 结构化指标评分
    indicatorScores?: IndicatorScore[];
    totalScore?: number;
    conclusion?: string;
    opinion?: string;
    receiveTime?: string | LocalDateTime;
    submitTime?: string | LocalDateTime;
    createTime?: string;
    // 扩展字段
    expertName?: string;
    expertWorkUnit?: string;
    expertPhone?: string;
    // 任务相关扩展字段
    taskType?: number;
    businessName?: string;
  }

  /** 评分数据项 */
  export interface ScoreItem {
    indicatorId: number;
    indicatorCode: string;
    indicatorName?: string;
    score: number;
    comment?: string;
  }

  /** 指标评分（结构化） */
  export interface IndicatorScore {
    indicatorId: number;
    indicatorCode: string;
    maxScore?: number;
    score?: number;
    scoreLevel?: string;
    scoreRatio?: number;
    comment?: string;
    opinion?: string;
  }

  /** 评审结果保存请求 */
  export interface ReviewResultSaveParams {
    id?: number;
    taskId: number;
    scoreData?: string;
    indicatorScores?: IndicatorScore[];
    totalScore?: number;
    conclusion?: string;
    opinion?: string;
    buttonId?: number;
  }

  /** 评审汇总（项目详情页用） */
  export interface ReviewSummary {
    processType: number;
    processTypeName: string;
    expertCount: number;
    averageScore?: number;
    passStatus?: string;
    reviewTime?: string;
    results: ReviewResult[];
  }
}

// ========== 评审任务 API ==========

/** 查询评审任务分页 */
export function getReviewTaskPage(params: PageParam & {
  taskType?: number;
  businessType?: number;
  businessId?: number;
  taskName?: string;
  status?: number;
}) {
  return requestClient.get<PageResult<DeclareReviewApi.ReviewTask>>(
    '/declare/review-task/page',
    { params },
  );
}

/** 查询评审任务详情 */
export function getReviewTask(id: number) {
  return requestClient.get<DeclareReviewApi.ReviewTask>(
    `/declare/review-task/get?id=${id}`,
  );
}

/** 查询评审任务详情（含扩展信息） */
export function getReviewTaskDetail(id: number) {
  return requestClient.get<DeclareReviewApi.ReviewTask>(
    `/declare/review-task/get-detail?id=${id}`,
  );
}

/** 根据业务ID获取评审任务 */
export function getReviewTaskByBusiness(businessType: number, businessId: number) {
  return requestClient.get<DeclareReviewApi.ReviewTask[]>(
    '/declare/review-task/list-by-business',
    { params: { businessType, businessId } },
  );
}

/** 创建评审任务 */
export function createReviewTask(data: DeclareReviewApi.ReviewTask) {
  return requestClient.post<number>('/declare/review-task/create', data);
}

/** 更新评审任务 */
export function updateReviewTask(data: DeclareReviewApi.ReviewTask) {
  return requestClient.put<boolean>('/declare/review-task/update', data);
}

/** 删除评审任务 */
export function deleteReviewTask(id: number) {
  return requestClient.delete<boolean>(`/declare/review-task/delete?id=${id}`);
}

/** 分配评审专家 */
export function assignExperts(taskId: number, expertIds: string) {
  return requestClient.put<boolean>(
    '/declare/review-task/assign-experts',
    null,
    { params: { taskId, expertIds } },
  );
}

// ========== 评审结果 API ==========

/** 查询评审结果分页 */
export function getReviewResultPage(params: PageParam & {
  taskId?: number;
  expertId?: number;
  businessType?: number;
  businessId?: number;
  status?: number;
  isAvoid?: boolean;
  processInstanceId?: string;
}) {
  return requestClient.get<PageResult<DeclareReviewApi.ReviewResult>>(
    '/declare/review-task/result/page',
    { params },
  );
}

/** 获取任务的评审结果列表 */
export function getReviewResultByTaskId(taskId: number) {
  return requestClient.get<DeclareReviewApi.ReviewResult[]>(
    '/declare/review-task/result/list-by-task',
    { params: { taskId } },
  );
}

/** 查询评审结果详情 */
export function getReviewResult(id: number) {
  return requestClient.get<DeclareReviewApi.ReviewResult>(
    `/declare/review-result/get?id=${id}`,
  );
}

/** 查询评审结果详情（含专家信息） */
export function getReviewResultDetail(id: number) {
  return requestClient.get<DeclareReviewApi.ReviewResult>(
    `/declare/review-result/get-detail?id=${id}`,
  );
}

/** 根据Flowable任务ID获取评审结果 */
export function getReviewResultByFlowableTaskId(flowableTaskId: string) {
  return requestClient.get<DeclareReviewApi.ReviewResult>(
    '/declare/review-result/get-by-flowable-task',
    { params: { flowableTaskId } },
  );
}

/** 专家接收评审任务 */
export function receiveReviewTask(id: number) {
  return requestClient.post<boolean>(
    '/declare/review-result/receive',
    null,
    { params: { id } },
  );
}

/** 开始评审 */
export function startReview(id: number) {
  return requestClient.post<boolean>(
    '/declare/review-result/start',
    null,
    { params: { id } },
  );
}

/** 提交评审结果 */
export function submitReview(id: number, data: DeclareReviewApi.ReviewResultSaveParams) {
  return requestClient.post<boolean>(
    '/declare/review-result/submit',
    data,
    { params: { id } },
  );
}

/** 获取业务下的评审汇总（按流程阶段分组） */
export function getReviewSummaryByBusiness(businessType: number, businessId: number) {
  return requestClient.get<DeclareReviewApi.ReviewSummary[]>(
    '/declare/review-result/list-by-business',
    { params: { businessType, businessId } },
  );
}

/** 专家申请回避 */
export function applyAvoidReview(id: number, reason: string) {
  return requestClient.post<boolean>(
    '/declare/review-result/apply-avoid',
    null,
    { params: { id, reason } },
  );
}

/** 获取专家待评审数量 */
export function getExpertPendingCount(expertId: number) {
  return requestClient.get<number>(
    '/declare/review-result/pending-count',
    { params: { expertId } },
  );
}

/** 获取我的评审任务列表（专家工作台） */
export function getMyReviewTasks(params: PageParam & {
  taskId?: number;
  businessType?: number;
  businessId?: number;
  status?: number;
}) {
  return requestClient.get<PageResult<DeclareReviewApi.ReviewResult>>(
    '/declare/review-result/my-tasks',
    { params },
  );
}

/** 专家拒绝评审任务 */
export function rejectReviewTask(id: number, reason?: string) {
  return requestClient.post<boolean>(
    '/declare/review-result/reject',
    null,
    { params: { id, reason } },
  );
}
