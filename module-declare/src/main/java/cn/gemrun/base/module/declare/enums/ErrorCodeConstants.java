package cn.gemrun.base.module.declare.enums;

import cn.gemrun.base.framework.common.exception.ErrorCode;

/**
 * Declare 错误码枚举类
 * <p>
 * declare 系统，使用 1-010-000-000 段
 */
public interface ErrorCodeConstants {

    ErrorCode FILING_NOT_EXISTS = new ErrorCode(1-010-000-001, "项目备案核心信息不存在");
}