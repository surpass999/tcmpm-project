package cn.gemrun.base.module.declare.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 项目过程类型枚举
 *
 * @author Gemini
 */
@Getter
@AllArgsConstructor
public enum ProcessType {

    CONSTRUCTION(1, "建设过程"),
    HALF_YEAR(2, "半年报"),
    ANNUAL(3, "年度总结"),
    MIDTERM(4, "中期评估"),
    RECTIFICATION(5, "整改记录"),
    ACCEPTANCE(6, "验收申请");

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
    public static ProcessType valueOf(Integer type) {
        if (type == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getType().equals(type))
                .findFirst()
                .orElse(null);
    }

}
