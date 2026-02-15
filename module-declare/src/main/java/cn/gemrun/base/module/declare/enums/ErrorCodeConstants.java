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

}
