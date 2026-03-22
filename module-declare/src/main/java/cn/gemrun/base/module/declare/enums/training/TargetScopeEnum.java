package cn.gemrun.base.module.declare.enums.training;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 目标范围枚举
 *
 * @author Gemini
 */
@Getter
@AllArgsConstructor
public enum TargetScopeEnum {

    NATIONAL(1, "全国"),
    PROVINCIAL(2, "全省");

    private final Integer code;
    private final String label;

    public static TargetScopeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TargetScopeEnum scope : values()) {
            if (scope.getCode().equals(code)) {
                return scope;
            }
        }
        return null;
    }

}
