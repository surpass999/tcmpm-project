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
     * DEPT_POST - 本部门岗位（通过 parent_id 递归查找上级部门，再查找岗位用户）
     * DEPT_LEADER - 部门负责人
     * START_USER - 发起人本人
     * START_USER_SELECT - 发起人自选
     * DYNAMIC_USER - 动态用户
     * USER - 指定用户
     * USER_GROUP - 用户组
     * EXPRESSION - 流程表达式
     */
    private String type;

    /**
     * 来源（根据 type 不同，含义不同）
     *
     * STATIC_ROLE: 角色编码，如 "PROVINCE,NATION"
     * DEPT_POST: 岗位ID，如 "2"（第二个下拉框选择岗位，存储岗位ID）
     * DYNAMIC_USER: expertUsers / startUserDeptLeader
     * USER: 用户ID列表，用逗号分隔
     * USER_GROUP: 用户组编码列表，用逗号分隔
     * EXPRESSION: 表达式，如 ${xxx}
     */
    private String source;

    /**
     * 岗位编码（DEPT_POST 类型使用）
     * 如：PROJECT_MANAGER
     */
    private String postCode;

    /**
     * 部门层级（DEPT_POST 类型使用）
     * 通过 parent_id 向上查找的第 N 级部门
     * 例如：1 = 直接上级部门，2 = 上级的上级部门
     */
    private Integer level;

    /**
     * 角色编码列表（STATIC_ROLE 类型使用）
     */
    private String[] roleCodes;

    /**
     * 用户ID列表（USER 类型使用）
     */
    private Long[] userIds;

    /**
     * 用户组编码列表（USER_GROUP 类型使用）
     */
    private String[] userGroupCodes;

    /**
     * 审批模式
     * 0 = 抢签模式（默认）：所有候选人都能看到任务，谁先签收谁审批
     * 1 = 指定模式：随机选择一个候选人作为处理人
     */
    private Integer approvalMode;

}
