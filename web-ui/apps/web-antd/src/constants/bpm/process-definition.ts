import { BUSINESS_TYPE } from './business-type';

/**
 * 流程定义 Key 常量
 * 与 Flowable 流程定义 key 对应
 */
export const PROCESS_DEFINITION_KEY = {
  // 备案流程
  FILING: 'declare_filing',

  // 项目过程流程（每种类型独立）
  PROJECT_HALF_YEAR: 'declare_project_half_year',
  PROJECT_ANNUAL: 'declare_project_annual',
  PROJECT_MIDTERM: 'declare_project_midterm',
  PROJECT_RECTIFICATION: 'declare_project_rectification',
  PROJECT_ACCEPTANCE: 'declare_project_acceptance',

  // 成果流程
  ACHIEVEMENT: 'declare_achievement',

  // 评审流程
  REVIEW: 'declare_review',
} as const;

export type ProcessDefinitionKey =
  (typeof PROCESS_DEFINITION_KEY)[keyof typeof PROCESS_DEFINITION_KEY];

/**
 * 根据 businessType 获取流程定义 Key
 */
export function getProcessDefinitionKey(
  businessType: string
): ProcessDefinitionKey | null {
  const mapping: Record<string, ProcessDefinitionKey> = {
    [BUSINESS_TYPE.FILING]: PROCESS_DEFINITION_KEY.FILING,
    [BUSINESS_TYPE.FILING_HOSPITAL]: PROCESS_DEFINITION_KEY.FILING,
    [BUSINESS_TYPE.PROJECT_HALF_YEAR]:
      PROCESS_DEFINITION_KEY.PROJECT_HALF_YEAR,
    [BUSINESS_TYPE.PROJECT_ANNUAL]: PROCESS_DEFINITION_KEY.PROJECT_ANNUAL,
    [BUSINESS_TYPE.PROJECT_MIDTERM]:
      PROCESS_DEFINITION_KEY.PROJECT_MIDTERM,
    [BUSINESS_TYPE.PROJECT_RECTIFICATION]:
      PROCESS_DEFINITION_KEY.PROJECT_RECTIFICATION,
    [BUSINESS_TYPE.PROJECT_ACCEPTANCE]:
      PROCESS_DEFINITION_KEY.PROJECT_ACCEPTANCE,
    [BUSINESS_TYPE.ACHIEVEMENT_SUBMIT]:
      PROCESS_DEFINITION_KEY.ACHIEVEMENT,
    [BUSINESS_TYPE.REVIEW]: PROCESS_DEFINITION_KEY.REVIEW,
  };
  return mapping[businessType] ?? null;
}
