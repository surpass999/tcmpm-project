package cn.gemrun.base.module.bpm.framework.dsl.config;

import lombok.Data;

import java.io.Serializable;

/**
 * DSL 任务分配配置
 *
 * @author Gemini
 */
@Data
public class DslAssign implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分配类型
     * STATIC_ROLE - 固定角色
     * DYNAMIC_USER - 动态用户
     * GROUP - 用户组
     */
    private String type;

    /**
     * 来源
     * 角色：provinceRole, nationalRole, expertRole
     * 动态用户：expertUsers, startUserDeptLeader
     * 用户组：userGroups
     */
    private String source;

}
