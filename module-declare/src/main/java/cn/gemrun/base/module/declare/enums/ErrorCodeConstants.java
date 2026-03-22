package cn.gemrun.base.module.declare.enums;

import cn.gemrun.base.framework.common.exception.ErrorCode;

/**
 * Declare 错误码枚举类
 * <p>
 * declare 系统，使用 1-010-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 项目备案模块 1-010-000-000 ==========

    ErrorCode FILING_NOT_EXISTS = new ErrorCode(1_010_000_001, "项目备案核心信息不存在");

    ErrorCode FILING_SUBMIT_STATUS_ERROR = new ErrorCode(1_010_000_002, "只有草稿状态才能提交");

    ErrorCode FILING_WITHDRAW_STATUS_ERROR = new ErrorCode(1_010_000_003, "只有已提交状态才能撤回");

    ErrorCode FILING_RESUBMIT_STATUS_ERROR = new ErrorCode(1_010_000_004, "只有草稿或退回状态才能重新提交");

    // ========== 流程配置模块 1-010-001-000 ==========

    ErrorCode PROCESS_CONFIG_NOT_FOUND = new ErrorCode(1_010_001_001, "流程配置不存在");

    ErrorCode PROCESS_CONFIG_DISABLED = new ErrorCode(1_010_001_002, "流程配置已禁用");

    // ========== 指标体系模块 1-010-002-000 ==========

    ErrorCode INDICATOR_NOT_EXISTS = new ErrorCode(1_010_002_001, "指标不存在");

    ErrorCode INDICATOR_CODE_EXISTS = new ErrorCode(1_010_002_002, "指标代号已存在: {0}");

    ErrorCode INDICATOR_VALUE_OPTIONS_REQUIRED = new ErrorCode(1_010_002_003, "单选或多选类型必须配置选项定义");

    ErrorCode CALIBER_NOT_EXISTS = new ErrorCode(1_010_002_010, "指标口径不存在");

    ErrorCode JOINT_RULE_NOT_EXISTS = new ErrorCode(1_010_002_011, "指标联合规则不存在");

    // ========== 专家管理模块 1-010-003-000 ==========

    ErrorCode EXPERT_NOT_EXISTS = new ErrorCode(1_010_003_001, "专家不存在");

    ErrorCode EXPERT_USER_ALREADY_EXISTS = new ErrorCode(1_010_003_002, "该用户已存在专家记录");

    ErrorCode EXPERT_STATUS_ERROR = new ErrorCode(1_010_003_003, "专家状态不正确");

    // ========== 项目信息模块 1-010-004-000 ==========

    ErrorCode PROJECT_NOT_EXISTS = new ErrorCode(1_010_004_001, "项目信息不存在");

    // ========== 项目过程记录模块 1-010-005-000 ==========

    ErrorCode PROJECT_PROCESS_NOT_EXISTS = new ErrorCode(1_010_005_001, "项目过程记录不存在");

    ErrorCode PROCESS_STATUS_NOT_EDITABLE = new ErrorCode(1_010_005_002, "当前状态不允许编辑");

    ErrorCode PROCESS_STATUS_NOT_WITHDRAWABLE = new ErrorCode(1_010_005_003, "只有已提交状态才能撤回");

    ErrorCode PROCESS_STATUS_NOT_SUBMITTABLE = new ErrorCode(1_010_005_004, "只有已保存状态才能提交");

    // ========== 过程指标配置模块 1-010-005-100 ==========

    ErrorCode PROCESS_INDICATOR_CONFIG_NOT_EXISTS = new ErrorCode(1_010_005_101, "过程指标配置不存在");

    ErrorCode PROCESS_INDICATOR_CONFIG_EXISTS = new ErrorCode(1_010_005_102, "该过程类型已配置此指标");

    // ========== 整改记录模块 1-010_006-000 ==========

    ErrorCode RECTIFICATION_NOT_EXISTS = new ErrorCode(1_010_006_001, "整改记录不存在");

    // ========== 数据流通模块 1-010-007-000 ==========

    ErrorCode DATAFLOW_NOT_EXISTS = new ErrorCode(1_010_007_001, "数据流通记录不存在");

    ErrorCode DATAFLOW_SUBMIT_STATUS_ERROR = new ErrorCode(1_010_007_002, "只有草稿状态才能提交审核");

    // ========== 成果信息模块 1-010-008-000 ==========

    ErrorCode ACHIEVEMENT_NOT_EXISTS = new ErrorCode(1_010_008_001, "成果信息不存在");

    ErrorCode ACHIEVEMENT_SUBMIT_STATUS_ERROR = new ErrorCode(1_010_008_002, "只有草稿状态才能提交审核");

    ErrorCode ACHIEVEMENT_NO_DATA_FLOW = new ErrorCode(1_010_008_003, "无数据基础，不得填写转化成果");

    ErrorCode ACHIEVEMENT_NOT_RECOMMENDED_TO_NATION = new ErrorCode(1_010_008_004, "成果未被推荐至国家局，无法纳入推广库");

}
