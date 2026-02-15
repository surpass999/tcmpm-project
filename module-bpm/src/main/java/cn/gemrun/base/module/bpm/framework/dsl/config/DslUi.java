package cn.gemrun.base.module.bpm.framework.dsl.config;

import lombok.Data;

import java.io.Serializable;

/**
 * DSL UI 配置
 *
 * @author Gemini
 */
@Data
public class DslUi implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 表单标识（对应的表单配置）
     */
    private String form;

    /**
     * 是否只读
     */
    private Boolean readonly;

}
