package cn.gemrun.base.module.declare.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 成果与流通状态枚举
 * 与 ProjectProcessStatus 保持一致，使用字符串状态值
 *
 * @author
 */
@Getter
@AllArgsConstructor
public enum AchievementStatus {

    DRAFT("DRAFT", "草稿"),
    SUBMITTED("SUBMITTED", "已提交"),
    AUDITING("AUDITING", "审核中"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "退回");

    /**
     * 状态值
     */
    private final String status;

    /**
     * 状态标签
     */
    private final String label;

    /**
     * 根据状态值获取枚举
     */
    public static AchievementStatus getByStatus(String status) {
        if (status == null) {
            return null;
        }
        for (AchievementStatus s : values()) {
            if (s.status.equals(status)) {
                return s;
            }
        }
        return null;
    }

    /**
     * 判断是否为草稿状态
     */
    public static boolean isDraft(String status) {
        return DRAFT.status.equals(status);
    }

    /**
     * 判断是否已通过
     */
    public static boolean isApproved(String status) {
        return APPROVED.status.equals(status);
    }

}
