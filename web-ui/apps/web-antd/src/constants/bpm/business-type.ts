/**
 * 业务类型常量定义
 * 与后端 bpm_business_type 表对应
 * 命名规范：固定值用英文 key，项目过程用 project_process:type:{字典ID}
 */
export const BUSINESS_TYPE = {
  // 备案相关（固定值）
  FILING: 'filing:approval', // 备案审批
  FILING_HOSPITAL: 'filing:hospital', // 医院备案

  // 成果相关（固定值）
  ACHIEVEMENT_SUBMIT: 'achievement:submit', // 成果提交

  // 评审相关（固定值）
  REVIEW: 'review:approval', // 评审审批
  // 注意：项目过程不使用固定常量，由 getBusinessTypeByProcessType() 动态生成
} as const;

export type BusinessType = (typeof BUSINESS_TYPE)[keyof typeof BUSINESS_TYPE];

/**
 * 业务类型选项（用于下拉选择）
 */
export const BUSINESS_TYPE_OPTIONS = [
  { label: '备案审批', value: BUSINESS_TYPE.FILING },
  { label: '医院备案', value: BUSINESS_TYPE.FILING_HOSPITAL },
  { label: '成果提交', value: BUSINESS_TYPE.ACHIEVEMENT_SUBMIT },
  { label: '评审审批', value: BUSINESS_TYPE.REVIEW },
  // 项目过程不列入常量选项，由字典动态生成
];

/**
 * 过程类型常量（用于项目过程记录，对应 declare_process_type 字典表）
 */
export const PROCESS_TYPE = {
  CONSTRUCTION: 1,   // 建设过程
  HALF_YEAR: 2,      // 半年报
  ANNUAL: 3,         // 年度总结
  MIDTERM: 4,        // 中期评估
  RECTIFICATION: 5,  // 整改记录
  ACCEPTANCE: 6,     // 验收申请
} as const;

export type ProcessType = (typeof PROCESS_TYPE)[keyof typeof PROCESS_TYPE];

/**
 * 过程类型选项
 */
export const PROCESS_TYPE_OPTIONS = [
  { label: '建设过程',   value: PROCESS_TYPE.CONSTRUCTION },
  { label: '半年报',     value: PROCESS_TYPE.HALF_YEAR },
  { label: '年度总结',   value: PROCESS_TYPE.ANNUAL },
  { label: '中期评估',   value: PROCESS_TYPE.MIDTERM },
  { label: '整改记录',   value: PROCESS_TYPE.RECTIFICATION },
  { label: '验收申请',   value: PROCESS_TYPE.ACCEPTANCE },
];

/**
 * 过程类型 ID → businessType 映射（字典驱动，动态生成）
 * 格式：project_process:type:{字典ID}
 * 字典 ID 来自 declare_process_type 表主键，管理员可在数据库中增删改
 */
export function getBusinessTypeByProcessType(processType: number): string {
  return `project_process:type:${processType}`;
}

/**
 * 根据 businessType 获取 processType（反向映射）
 */
export function getProcessTypeByBusinessType(businessType: string): ProcessType | null {
  const match = businessType.match(/^project_process:type:(\d+)$/);
  if (match) {
    const type = parseInt(match[1], 10);
    if (type in PROCESS_TYPE) {
      return type as ProcessType;
    }
  }
  return null;
}
