package cn.gemrun.base.module.declare.enums.training;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 活动类型枚举
 *
 * @author Gemini
 */
@Getter
@AllArgsConstructor
public enum TrainingTypeEnum {

    PROGRESS_MEETING(1, "推进会"),
    SEMINAR(2, "专题研讨"),
    DEMONSTRATION(3, "系统演示"),
    TRAINING(4, "业务培训");

    private final Integer code;
    private final String label;

    public static TrainingTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TrainingTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

}
