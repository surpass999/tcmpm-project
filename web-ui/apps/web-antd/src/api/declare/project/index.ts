import type { PageParam, PageResult } from '@vben/request';
import type { Dayjs } from 'dayjs';

import { requestClient } from '#/api/request';

export namespace DeclareProjectApi {
  /** 项目信息 */
  export interface Project {
    id?: number;
    filingId?: number; // 关联备案ID
    projectType?: number; // 项目类型
    processType?: number; // 过程类型
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
    // 备案冗余字段
    socialCreditCode?: string; // 统一社会信用代码
    medicalLicenseNo?: string; // 医疗机构执业许可证号
    orgName?: string; // 机构名称
    validStartTime?: string | number | Dayjs; // 有效期开始时间
    validEndTime?: string | number | Dayjs; // 有效期结束时间
    constructionContent?: string; // 建设内容
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
    attachmentIds?: string; // 附件IDs（逗号分隔）
    status?: number|string; // 状态：系统字典
    statusReason?: string; // 状态变更原因
    reportPeriodStart?: string | number | Dayjs; // 报告周期开始时间
    reportPeriodEnd?: string | number | Dayjs; // 报告周期结束时间
    reportTime?: string | number | Dayjs; // 报告提交时间
    reviewOpinion?: string; // 审核意见
    reviewerId?: number; // 审核人ID
    reviewTime?: string | number | Dayjs; // 审核时间
    reviewResult?: number; // 审核结果：1=通过，2=退回
    creatorId?: number; // 创建人ID
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

/** 获取项目列表（用于下拉选择） */
export function getProjectSimpleList() {
  return requestClient.get<DeclareProjectApi.Project[]>('/declare/project/simple-list');
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

/** 提交过程记录审核（发起流程） */
export function submitProcessReview(
  businessId: number,
  businessType: string,
  processDefinitionKey?: string,
) {
  return requestClient.post<string>('/bpm/process/start', {
    businessId,
    businessType,
    processDefinitionKey: processDefinitionKey || undefined,
  });
}

// ========== 过程填报 API ==========

/** 指标表单项配置 */
export interface IndicatorFormItem {
  configId: number;
  indicatorId: number;
  indicatorCode: string;
  indicatorName: string;
  category: number;
  valueType: number;
  unit: string;
  isRequired: boolean;
  sort: number;
  fillRequire: string;
  currentValue: any;
}

/** 文本字段配置 */
export interface TextFieldConfig {
  fieldCode: string;
  fieldName: string;
  isRequired: boolean;
  maxLength: number;
  fieldType: number;
}

/** 过程填报表单配置 Response */
export interface ProcessFormConfig {
  processId: number;
  processType: number;
  processTypeName: string;
  processTitle: string;
  reportPeriodStart?: string;
  reportPeriodEnd?: string;
  projectId: number;
  projectName: string;
  projectType: number;
  projectTypeName: string;
  projectLeader: string;
  constructionPeriod: string;
  projectStatus: string;
  projectStatusName: string;
  indicators: IndicatorFormItem[];
  textFields: TextFieldConfig[];
  status: string;
  statusName: string;
  createName: string;
  createTime: string;
  processData: string;
  indicatorValues: string;
  attachmentIds: string;
  indicatorVersion: number;
}

/** 获取表单配置 */
export function getProcessFormConfig(processId: number) {
  return requestClient.get<ProcessFormConfig>(
    `/project/process-form/config?processId=${processId}`,
  );
}

/** 获取填报数据 */
export function getProcessFormData(processId: number) {
  return requestClient.get<{
    processData: string;
    indicatorValues: string;
    attachmentIds: string;
  }>(`/project/process-form/data?processId=${processId}`);
}

/** 保存填报数据 */
export function saveProcessFormData(data: {
  processId: number;
  formData: Record<string, any>;
  indicatorValues: Record<string, any>;
  attachmentIds?: string;
  status: string; // DRAFT/SAVED/SUBMITTED
}) {
  return requestClient.post<number>('/project/process-form/save', data);
}

/** 验证填报数据 */
export function validateProcessFormData(data: {
  processId: number;
  formData: Record<string, any>;
  indicatorValues: Record<string, any>;
}) {
  return requestClient.post<{
    valid: boolean;
    errors: string[];
  }>('/project/process-form/validate', data);
}

/** 撤回填报数据 */
export function withdrawProcessFormData(processId: number) {
  return requestClient.post<boolean>(
    `/project/process-form/withdraw?processId=${processId}`,
  );
}

/** 手动同步指标 */
export function syncProcessIndicator(processId: number) {
  return requestClient.post<boolean>(
    `/project/process-form/sync-indicator?processId=${processId}`,
  );
}

// ========== 整改记录 API ==========

/** 整改记录列表项（包含过程记录和子流程） */
export interface RectificationRecord {
  recordType: number; // 1=过程记录，2=子流程
  processId?: number; // 过程记录ID（recordType=1时有值）
  childProcessInstanceId?: string; // 子流程实例ID（recordType=2时有值）
  parentProcessInstanceId?: string; // 主流程实例ID（recordType=2时关联的过程记录对应的流程实例ID）
  projectId?: number; // 关联项目ID
  projectName?: string; // 项目名称
  title?: string; // 过程标题（过程记录）或流程名称（子流程）
  status?: string; // 状态
  statusName?: string; // 状态名称
  reportPeriodStart?: string; // 报告周期开始时间
  reportPeriodEnd?: string; // 报告周期结束时间
  createTime?: string; // 创建/发起时间
  endTime?: string; // 结束时间（子流程有值）
  startUserId?: number; // 发起人ID
  startUserName?: string; // 发起人名称
  processDefinitionName?: string; // 流程定义名称（子流程有值）
  processDefinitionKey?: string; // 流程定义Key（子流程有值）
}

/** 根据项目ID查询整改记录列表（包含过程记录和子流程） */
export function getRectificationRecordList(projectId: number) {
  return requestClient.get<RectificationRecord[]>(
    `/declare/project-process/rectification-list?projectId=${projectId}`,
  );
}

/** 根据主流程实例ID获取子流程列表 */
export function getChildProcessList(parentProcessInstanceId: string) {
  return requestClient.get<any[]>(
    `/bpm/process-instance/child-list?parentProcessInstanceId=${parentProcessInstanceId}`,
  );
}
