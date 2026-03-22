/**
 * 成果与流通模块配置数据
 */

/** 成果类型选项 */
export const ACHIEVEMENT_TYPE_OPTIONS = [
  { label: '系统功能', value: 1 },
  { label: '数据集', value: 2 },
  { label: '科研成果', value: 3 },
  { label: '管理经验', value: 4 },
];

/** 可复制性评估选项 */
export const REPLICATION_VALUE_OPTIONS = [
  { label: '高', value: 1 },
  { label: '中', value: 2 },
  { label: '低', value: 3 },
];

/** 推广范围选项 */
export const PROMOTION_SCOPE_OPTIONS = [
  { label: '院内', value: 1 },
  { label: '省级', value: 2 },
  { label: '全国', value: 3 },
];

/** 转化类型选项 */
export const TRANSFORM_TYPE_OPTIONS = [
  { label: '标准规范', value: 1 },
  { label: '创新模式', value: 2 },
  { label: '典型案例', value: 3 },
];

/** 流通类型选项 */
export const FLOW_TYPE_OPTIONS = [
  { label: '内部使用', value: 1 },
  { label: '对外共享', value: 2 },
  { label: '交易', value: 3 },
];

/** 共享范围选项 */
export const SHARE_SCOPE_OPTIONS = [
  { label: '院内', value: 1 },
  { label: '省级', value: 2 },
  { label: '全国', value: 3 },
];

/** 数据质量评级选项 */
export const DATA_QUALITY_OPTIONS = [
  { label: '优', value: 1 },
  { label: '良', value: 2 },
  { label: '中', value: 3 },
  { label: '差', value: 4 },
];

/** 审批流状态选项 */
export const STATUS_OPTIONS = [
  { label: '草稿', value: 0, color: 'default' },
  { label: '已提交', value: 1, color: 'blue' },
  { label: '审核中', value: 2, color: 'orange' },
  { label: '已通过', value: 3, color: 'green' },
  { label: '退回', value: 4, color: 'red' },
];

/** 审核状态选项 */
export const AUDIT_STATUS_OPTIONS = [
  { label: '待审核', value: 0, color: 'default' },
  { label: '省级通过/待国家局审核', value: 1, color: 'blue' },
  { label: '国家局审核中', value: 2, color: 'orange' },
  { label: '已认定推广', value: 3, color: 'green' },
  { label: '退回', value: 4, color: 'red' },
];

/** 推荐状态选项 */
export const RECOMMEND_STATUS_OPTIONS = [
  { label: '未推荐', value: 0, color: 'default' },
  { label: '已推荐至国家局', value: 1, color: 'blue' },
  { label: '已纳入推广库', value: 2, color: 'green' },
];

/** 表单校验规则 */
export const FORM_RULES = {
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  achievementName: [{ required: true, message: '请输入成果名称', trigger: 'blur' }],
  achievementType: [{ required: true, message: '请选择成果类型', trigger: 'change' }],
  applicationField: [{ required: true, message: '请输入应用领域', trigger: 'blur' }],
};
