package cn.gemrun.base.module.declare.enums.training;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 活动状态枚举
 *
 * @author Gemini
 */
@Getter
@AllArgsConstructor
public enum TrainingStatusEnum {

    DRAFT(1, "草稿"),
    REGISTRATION(2, "报名中"),
    IN_PROGRESS(3, "进行中"),
    ENDED(4, "已结束"),
    CANCELLED(5, "已取消");

    private final Integer code;
    private final String label;

    public static TrainingStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TrainingStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

}
