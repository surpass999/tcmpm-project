package cn.gemrun.base.module.declare.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目状态枚举
 *
 * @author Gemini
 */
@Getter
@AllArgsConstructor
public enum ProjectStatus {

    INITIATION("INITIATION", "初始化"),
    FILING("FILING", "立项中"),
    CONSTRUCTION("CONSTRUCTION", "建设中"),
    MIDTERM("MIDTERM", "中期评估"),
    RECTIFICATION("RECTIFICATION", "整改中"),
    ACCEPTANCE("ACCEPTANCE", "验收中"),
    ACCEPTED("ACCEPTED", "已验收"),
    TERMINATED("TERMINATED", "已终止");

    /**
     * 状态值（字典值）
     */
    private final String status;

    /**
     * 状态标签
     */
    private final String label;

}
