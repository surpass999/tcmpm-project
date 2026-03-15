package cn.gemrun.base.module.declare.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 项目类型枚举
 *
 * @author Gemini
 */
@Getter
@AllArgsConstructor
public enum ProjectTypeEnum {

    COMPREHENSIVE(0, "通用类型"),
    INTEGRATED(1, "综合型"),
    EMR(2, "中医电子病历型"),
    SMART_PHARMACY(3, "智慧中药房型"),
    FAMOUS_DOCTOR(4, "名老中医传承型"),
    CLINICAL_RESEARCH(5, "中医临床科研型"),
    MEDICAL_COMMUNITY(6, "中医智慧医共体型");

    /**
     * 类型值
     */
    private final Integer type;

    /**
     * 类型名称
     */
    private final String name;

    /**
     * 根据类型值获取枚举
     */
    public static ProjectTypeEnum valueOf(Integer type) {
        if (type == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getType().equals(type))
                .findFirst()
                .orElse(null);
    }

}
