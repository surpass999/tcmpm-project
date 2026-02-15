package cn.gemrun.base.module.bpm.framework.dsl.config;

import lombok.Data;

import java.io.Serializable;

/**
 * DSL 变量配置
 *
 * @author Gemini
 */
@Data
public class DslVars implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目标变量（如专家列表）
     */
    private String targetVar;

    /**
     * 最小数量
     */
    private Integer min;

    /**
     * 最大数量
     */
    private Integer max;

    /**
     * 可修改字段列表（补正时使用）
     */
    private String[] modifyFields;

}
