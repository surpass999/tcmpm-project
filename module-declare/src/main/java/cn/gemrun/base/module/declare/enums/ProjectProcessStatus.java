package cn.gemrun.base.module.declare.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目过程记录状态枚举
 *
 * @author Gemini
 */
@Getter
@AllArgsConstructor
public enum ProjectProcessStatus {

    DRAFT(0, "草稿"),
    SUBMITTED(1, "已提交"),
    AUDITING(2, "审核中"),
    APPROVED(3, "通过"),
    REJECTED(4, "退回");

    /**
     * 状态值
     */
    private final Integer status;

    /**
     * 状态标签
     */
    private final String label;

}
