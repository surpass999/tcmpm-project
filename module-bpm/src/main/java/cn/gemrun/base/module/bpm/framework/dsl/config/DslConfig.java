package cn.gemrun.base.module.bpm.framework.dsl.config;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

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
 * 完整流程 DSL（用于审批详情）：
 * {
 *   "nodes": [
 *     {"nodeKey": "start", "name": "发起人", "cap": "FILL", ...},
 *     {"nodeKey": "province_audit", "name": "省审核", "cap": "AUDIT", ...},
 *     {"nodeKey": "nation_audit", "name": "国家审核", "cap": "AUDIT", ...}
 *   ]
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
     * 节点名称
     */
    private String name;

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
     * 会签拒绝规则：ANY_REJECT(任一拒绝则结束), ALL_REJECT(全部拒绝才结束)
     */
    private String rejectRule;

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
     * 是否启用该节点配置
     */
    private Boolean enable;

    /**
     * 变量配置
     */
    private DslVars vars;

    /**
     * UI 配置
     */
    private DslUi ui;

    /**
     * 完整流程节点列表（用于审批详情）
     * 每个元素是单个节点的 DSL 配置
     */
    private List<DslConfig> nodes;

    /**
     * 判断是否为完整流程 DSL（包含 nodes 数组）
     */
    public boolean isFullFlowDsl() {
        return nodes != null && !nodes.isEmpty();
    }

}
