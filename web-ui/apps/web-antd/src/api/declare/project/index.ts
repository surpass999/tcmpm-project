import type { PageParam, PageResult } from '@vben/request';
import type { Dayjs } from 'dayjs';

import { requestClient } from '#/api/request';

export namespace DeclareProjectApi {
  /** 项目信息 */
  export interface Project {
    id?: number;
    filingId?: number; // 关联备案ID
    projectName?: string; // 项目名称
    projectStatus?: number | string; // 项目状态
    totalInvestment?: string | number; // 总投资（万元）
    centralFundArrive?: string | number; // 中央转移支付到账金额
    accumulatedInvestment?: string | number; // 累计完成投资
    centralFundUsed?: string | number; // 中央转移支付累计使用金额
    startTime?: string | number | Dayjs; // 立项时间
    planEndTime?: string | number | Dayjs; // 计划完成时间
    actualEndTime?: string | number | Dayjs; // 实际完成时间
    actualProgress?: number; // 实际进度（%）
    leaderUserId?: number; // 项目负责人ID
    leaderMobile?: string; // 负责人手机号
    leaderName?: string; // 负责人姓名
    deptId?: number; // 所属部门ID
    createTime?: string | number | Dayjs; // 创建时间
    updateTime?: string | number | Dayjs; // 更新时间
    creator?: number; // 创建者
  }

  /** 项目过程记录信息 */
  export interface ProjectProcess {
    id?: number;
    projectId?: number; // 关联项目ID
    processType?: number; // 过程类型：1=建设过程，2=年度总结，3=中期评估，4=整改记录，5=验收申请
    processTitle?: string; // 过程标题
    processData?: string; // 过程数据JSON
    indicatorValues?: string; // 指标值JSON
    status?: number; // 状态：0=草稿，1=已提交，2=审核中，3=通过，4=退回
    statusReason?: string; // 状态变更原因
    reportPeriodStart?: string | number | Dayjs; // 报告周期开始时间
    reportPeriodEnd?: string | number | Dayjs; // 报告周期结束时间
    reportTime?: string | number | Dayjs; // 报告提交时间
    reviewOpinion?: string; // 审核意见
    reviewerId?: number; // 审核人ID
    reviewTime?: string | number | Dayjs; // 审核时间
    reviewResult?: number; // 审核结果：1=通过，2=退回
    createTime?: string | number | Dayjs;
    updateTime?: string | number | Dayjs;
  }
}

/** 查询项目分页 */
export function getProjectPage(params: PageParam & Partial<DeclareProjectApi.Project>) {
  return requestClient.get<PageResult<DeclareProjectApi.Project>>(
    '/declare/project/page',
    { params },
  );
}

/** 查询项目详情 */
export function getProject(id: number) {
  return requestClient.get<DeclareProjectApi.Project>(
    `/declare/project/get?id=${id}`,
  );
}

/** 根据备案ID查询项目列表 */
export function getProjectListByFilingId(filingId: number) {
  return requestClient.get<DeclareProjectApi.Project[]>(
    `/declare/project/list-by-filing?filingId=${filingId}`,
  );
}

/** 创建项目 */
export function createProject(data: DeclareProjectApi.Project) {
  return requestClient.post('/declare/project/create', data);
}

/** 更新项目 */
export function updateProject(data: DeclareProjectApi.Project) {
  return requestClient.put('/declare/project/update', data);
}

/** 删除项目 */
export function deleteProject(id: number) {
  return requestClient.delete(`/declare/project/delete?id=${id}`);
}

/** 批量删除项目 */
export function deleteProjectList(ids: number[]) {
  return requestClient.delete(
    `/declare/project/delete-batch?ids=${ids.join(',')}`,
  );
}

/** 导出项目 */
export function exportProject(params: any) {
  return requestClient.download('/declare/project/export-excel', { params });
}

// ========== 项目过程记录 API ==========

/** 查询项目过程记录分页 */
export function getProjectProcessPage(params: PageParam & Partial<DeclareProjectApi.ProjectProcess>) {
  return requestClient.get<PageResult<DeclareProjectApi.ProjectProcess>>(
    '/declare/project-process/page',
    { params },
  );
}

/** 查询项目过程记录详情 */
export function getProjectProcess(id: number) {
  return requestClient.get<DeclareProjectApi.ProjectProcess>(
    `/declare/project-process/get?id=${id}`,
  );
}

/** 根据项目ID查询过程记录列表 */
export function getProjectProcessListByProjectId(projectId: number) {
  return requestClient.get<DeclareProjectApi.ProjectProcess[]>(
    `/declare/project-process/list-by-project?projectId=${projectId}`,
  );
}

/** 创建项目过程记录 */
export function createProjectProcess(data: DeclareProjectApi.ProjectProcess) {
  return requestClient.post('/declare/project-process/create', data);
}

/** 更新项目过程记录 */
export function updateProjectProcess(data: DeclareProjectApi.ProjectProcess) {
  return requestClient.put('/declare/project-process/update', data);
}

/** 删除项目过程记录 */
export function deleteProjectProcess(id: number) {
  return requestClient.delete(`/declare/project-process/delete?id=${id}`);
}

/** 批量删除项目过程记录 */
export function deleteProjectProcessList(ids: number[]) {
  return requestClient.delete(
    `/declare/project-process/delete-batch?ids=${ids.join(',')}`,
  );
}
