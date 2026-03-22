package cn.gemrun.base.module.declare.enums.training;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 报名状态枚举
 *
 * @author Gemini
 */
@Getter
@AllArgsConstructor
public enum RegistrationStatusEnum {

    REGISTERED(1, "已报名"),
    SIGNED_IN(2, "已签到"),
    CANCELLED(3, "已取消"),
    ABSENT(4, "未出席");

    private final Integer code;
    private final String label;

    public static RegistrationStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (RegistrationStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

}
