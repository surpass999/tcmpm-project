import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

/**
 * 驾驶舱统计数据
 */
export interface DashboardStats {
    userRole: 'hospital' | 'province' | 'nation' | 'expert';
  taskCount: number;
  projectCount: number;
  projectProgress: number;
  fundExecutionRate: number;
  warningCount: number;
  noticeCount: number;
  hospitalStats?: HospitalStats;
  provinceStats?: ProvinceStats;
  nationalStats?: NationalStats;
  expertStats?: ExpertStats;
}

/**
 * 四维度进度
 */
export interface DimensionProgress {
  /** 系统功能建设进度(%) */
  systemProgress: number;
  /** 高质量数据集建设进度(%) */
  datasetProgress: number;
  /** 信息安全备案进度(%) */
  securityProgress: number;
  /** 成果转化进度(%) */
  achievementProgress: number;
}

export interface HospitalStats {
  myProjectCount: number;
  draftReportCount: number;
  reviewingReportCount: number;
  completedReportCount: number;
  urgentTaskCount: number;
  /** 四维度进度 */
  dimensionProgress?: DimensionProgress;
  /** 是否有未填报的开放填报窗口 */
  hasUnfilledOpenWindow?: boolean;
  /** 成果总数 */
  achievementTotalCount?: number;
  /** 已认定/已推广成果数 */
  achievementApprovedCount?: number;
}

export interface ProvinceStats {
  provinceCode: string;
  regionProjectCount: number;
  reportedHospitalCount: number;
  pendingReviewCount: number;
  regionProgress: number;
  highRiskCount: number;
  regionDistribution: RegionItem[];
  /** 各项目类型分布 */
  projectTypeDistribution: ProjectTypeItem[];
}

export interface RegionItem {
  regionName: string;
  regionId: number;
  projectCount: number;
  progress: number;
}

export interface NationalStats {
  totalProjectCount: number;
  pendingApprovalCount: number;
  nationalProgress: number;
  totalFundRate: number;
}

export interface ExpertStats {
  pendingReviewCount: number;
  completedReviewCount: number;
  monthlyReviewCount: number;
}

/**
 * 待办任务
 */
export interface DashboardTasks {
  bpmTasks: TaskList;
  draftTasks: TaskList;
  warningTasks: TaskList;
}

export interface TaskList {
  total: number;
  items: TaskItem[];
}

export interface TaskItem {
  taskId: string;
  bpmTaskId?: string;
  processInstanceId?: string;
  taskName: string;
  processTitle: string;
  projectName: string;
  projectId?: number;
  processId?: number;
  processType?: number;
  taskType: 'bpm_task' | 'draft_report' | 'warning';
  createTime?: string;
  deadline?: string;
  status: 'pending' | 'processing' | 'completed';
  urgent?: boolean;
  jumpUrl: string;
}

/**
 * 项目进度
 */
export interface ProjectProgress {
  totalProjectCount: number;
  averageProgress: number;
  onTimeCount: number;
  delayedCount: number;
  stageDistribution: StageItem[];
  provinceProgress: ProvinceProgressItem[];
}

export interface StageItem {
  stageName: string;
  stageValue: number;
  count: number;
  percentage: number;
}

export interface ProvinceProgressItem {
  provinceName: string;
  provinceId: number;
  progress: number;
  projectCount: number;
}

/**
 * 资金统计
 */
export interface FundStats {
  totalFund: number;
  executedFund: number;
  executionRate: number;
  hospitalFunds: HospitalFundItem[];
  monthlyTrend: MonthlyTrendItem[];
}

export interface HospitalFundItem {
  hospitalName: string;
  hospitalId: number;
  projectName: string;
  projectId: number;
  budgetFund: number;
  executedFund: number;
  executionRate: number;
}

export interface MonthlyTrendItem {
  month: string;
  plannedFund: number;
  actualFund: number;
}

/**
 * 风险预警
 */
export interface RiskWarnings {
  totalCount: number;
  highRiskCount: number;
  mediumRiskCount: number;
  lowRiskCount: number;
  warnings: WarningItem[];
}

export interface WarningItem {
  id: number;
  warningType: 'progress' | 'fund' | 'quality';
  level: 'high' | 'medium' | 'low';
  title: string;
  description: string;
  projectId: number;
  projectName: string;
  warningTime: string;
  status: 'pending' | 'handled' | 'ignored';
  jumpUrl: string;
}

/**
 * 全国统计
 */
export interface NationalStatsVO {
  totalProjectCount: number;
  averageProgress: number;
  totalFundRate: number;
  pendingApprovalCount: number;
  provinceDistribution: ProvinceItem[];
  projectTypeDistribution: ProjectTypeItem[];
  fundRanking: ProvinceItem[];
  progressRanking: ProvinceItem[];
  /** 已中期评估项目数 */
  midtermProjectCount: number;
  /** 已验收项目数 */
  acceptedProjectCount: number;
  /** 中央转移资金三年总额（万元），固定值 105600 */
  centralFundThreeYearTotal: number;
  /** 到账资金总额（万元） */
  centralFundArriveTotal: number;
}

export interface ProvinceItem {
  provinceName: string;
  provinceId: number;
  projectCount: number;
  progress: number;
  fundRate: number;
  highRiskCount?: number;
}

export interface ProjectTypeItem {
  typeName: string;
  typeValue: number;
  projectCount: number;
  percentage: number;
  /** 总投资额（万元） */
  totalInvestment: number;
  /** 到账资金（万元） */
  arrivedFund: number;
  /** 已完成项目数 */
  completedCount?: number;
  /** 完成率(%) */
  completionRate?: number;
}

/**
 * 填报窗口统计信息
 */
export interface ReportWindowStatsVO {
  hasOpenWindow: boolean;
  openWindowName: string;
  currentBatch: number;
  reportYear: number;
  startDate: string;
  endDate: string;
  reportedHospitalCount: number;
  totalHospitalCount: number;
}

/**
 * 获取驾驶舱统计数据
 */
export async function getDashboardStats(): Promise<DashboardStats> {
  return requestClient.get('/declare/dashboard/stats');
}

/**
 * 获取待办任务列表
 */
export async function getDashboardTasks(): Promise<DashboardTasks> {
  return requestClient.get('/declare/dashboard/tasks');
}

/**
 * 获取项目进度统计
 */
export async function getProjectProgress(): Promise<ProjectProgress> {
  return requestClient.get('/declare/dashboard/project-progress');
}

/**
 * 获取资金统计
 */
export async function getFundStats(): Promise<FundStats> {
  return requestClient.get('/declare/dashboard/fund-stats');
}

/**
 * 获取风险预警列表
 */
export async function getRiskWarnings(): Promise<RiskWarnings> {
  return requestClient.get('/declare/dashboard/risks');
}

/**
 * 获取全国统计数据（国家局专用）
 */
export async function getNationalStats(): Promise<NationalStatsVO> {
  return requestClient.get('/declare/dashboard/national-stats');
}

/**
 * 获取当前用户角色
 */
export async function getUserRole(): Promise<string> {
  return requestClient.get('/declare/dashboard/user-role');
}

/**
 * 获取填报窗口统计信息（国家局专用）
 */
export async function getReportWindowStats(): Promise<ReportWindowStatsVO> {
  return requestClient.get('/declare/dashboard/report-window-stats');
}
