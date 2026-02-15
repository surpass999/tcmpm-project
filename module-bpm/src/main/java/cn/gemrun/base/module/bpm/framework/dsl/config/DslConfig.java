package cn.gemrun.base.module.bpm.framework.dsl.config;

import lombok.Data;

import java.io.Serializable;

/**
 * DSL 配置根对象
 *
 * 对应 DSL 规范：
 * {
 *   "nodeKey": "province_audit",
 *   "cap": "AUDIT",
 *   "actions": "agree,reject,back",
 *   "roles": ["PROVINCE"],
 *   "assign": {...},
 *   "signRule": "MAJORITY",
 *   "backStrategy": "TO_START",
 *   "reSubmit": "RESTART",
 *   "bizStatus": "PRO_AUDIT",
 *   "vars": {...},
 *   "ui": {...}
 * }
 *
 * @author Gemini
 */
@Data
public class DslConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点Key（对应 Flowable 的 nodeId）
     */
    private String nodeKey;

    /**
     * 节点能力
     * AUDIT - 单人审批
     * COUNTERSIGN - 会签
     * EXPERT_SELECT - 选择专家
     * FILL - 填报
     * MODIFY - 补正
     * CONFIRM - 确认
     * ARCHIVE - 归档
     * PUBLISH - 发布
     */
    private String cap;

    /**
     * 按钮列表（逗号分隔）
     * 流程控制：submit, agree, reject, back, cancel, transfer, delegate, suspend, resume
     * 会签专用：addSign, reduceSign, signAgree, signReject, urge
     * 业务专用：selectExpert, reSelect, modify, confirm, archive, toProject, publish
     */
    private String actions;

    /**
     * 角色限制（哪些角色可以看到并操作）
     */
    private String[] roles;

    /**
     * 任务分配配置
     */
    private DslAssign assign;

    /**
     * 会签规则：ALL(全部), ANY(任一), MAJORITY(多数), 2/3(三分之二), CUSTOM(自定义)
     */
    private String signRule;

    /**
     * 退回策略：TO_START(退回起点), TO_PREV(退回上一节点), TO_ANY(退回任意节点), TO_ROLE(退回指定角色)
     */
    private String backStrategy;

    /**
     * 重新提交策略：RESTART(重新开始), RESUME(从当前节点继续)
     */
    private String reSubmit;

    /**
     * 业务状态（审批通过后更新业务的状态字段值）
     */
    private String bizStatus;

    /**
     * 变量配置
     */
    private DslVars vars;

    /**
     * UI 配置
     */
    private DslUi ui;

}
