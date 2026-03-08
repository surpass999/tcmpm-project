package cn.gemrun.base.module.bpm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BPM 按钮定义枚举
 *
 * 定义 DSL 中 actions 可用的所有按钮配置：
 * - key: 按钮标识（对应 DSL 中的 actions 值）
 * - label: 显示中文名称
 * - bizStatus: 默认业务状态值（审批通过后更新业务表的状态）
 * - bizStatusLabel: 业务状态中文名称
 * - bpmAction: 对应的 BPM 操作类型
 *
 * @author Gemini
 */
@Getter
@AllArgsConstructor
public enum BpmActionDef {

    // ==================== 流程控制类 ====================

    /**
     * 提交
     */
    SUBMIT("submit", "提交", "SUBMITTED", "已提交", BpmAction.COMPLETE),

    /**
     * 通过
     */
    AGREE("agree", "通过", "APPROVED", "审批通过", BpmAction.COMPLETE),

    /**
     * 拒绝
     */
    REJECT("reject", "拒绝", "REJECTED", "审批拒绝", BpmAction.REJECT),

    /**
     * 退回
     */
    BACK("back", "退回", "RETURNED", "退回修改", BpmAction.BACK),

    /**
     * 撤回
     */
    CANCEL("cancel", "撤回", "CANCELLED", "已取消", BpmAction.CANCEL),

    /**
     * 转办
     */
    TRANSFER("transfer", "转办", null, null, BpmAction.TRANSFER),

    /**
     * 委派
     */
    DELEGATE("delegate", "委派", null, null, BpmAction.DELEGATE),

    /**
     * 挂起
     */
    SUSPEND("suspend", "挂起", "SUSPENDED", "已挂起", BpmAction.SUSPEND),

    /**
     * 恢复
     */
    RESUME("resume", "恢复", null, null, BpmAction.RESUME),

    // ==================== 会签专用类 ====================

    /**
     * 加签
     */
    ADD_SIGN("addSign", "加签", null, null, BpmAction.COMPLETE),

    /**
     * 减签
     */
    REDUCE_SIGN("reduceSign", "减签", null, null, BpmAction.COMPLETE),

    /**
     * 会签通过
     */
    SIGN_AGREE("signAgree", "会签通过", "SIGN_APPROVED", "会签同意", BpmAction.COMPLETE),

    /**
     * 会签拒绝
     */
    SIGN_REJECT("signReject", "会签拒绝", "SIGN_REJECTED", "会签拒绝", BpmAction.REJECT),

    /**
     * 催办
     */
    URGE("urge", "催办", null, null, null),

    // ==================== 业务专用类 ====================

    /**
     * 选择专家
     */
    SELECT_EXPERT("selectExpert", "选择专家", "EXPERT_REVIEWING", "专家评审中", BpmAction.COMPLETE),

    /**
     * 重选专家
     */
    RESELECT("reSelect", "重选", "EXPERT_REVIEWING", "专家评审中", BpmAction.COMPLETE),

    /**
     * 补正/修改
     */
    MODIFY("modify", "补正", "MODIFYING", "补正中", BpmAction.COMPLETE),

    /**
     * 确认
     */
    CONFIRM("confirm", "确认", "CONFIRMED", "已确认", BpmAction.COMPLETE),

    /**
     * 归档
     */
    ARCHIVE("archive", "归档", "ARCHIVED", "已归档", BpmAction.COMPLETE),

    /**
     * 转项目
     */
    TO_PROJECT("toProject", "转项目", null, null, BpmAction.COMPLETE),

    /**
     * 发布
     */
    PUBLISH("publish", "发布", "PUBLISHED", "已发布", BpmAction.COMPLETE);

    /**
     * 按钮标识（对应 DSL 中的 actions 值）
     */
    private final String key;

    /**
     * 显示中文名称
     */
    private final String label;

    /**
     * 默认业务状态值（审批通过后更新业务表的状态）
     */
    private final String bizStatus;

    /**
     * 业务状态中文名称
     */
    private final String bizStatusLabel;

    /**
     * 对应的 BPM 操作类型
     */
    private final BpmAction bpmAction;

    /**
     * 根据 key 获取按钮定义
     *
     * @param key 按钮标识
     * @return 按钮定义，如果不存在返回 null
     */
    public static BpmActionDef fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (BpmActionDef actionDef : values()) {
            if (actionDef.key.equals(key)) {
                return actionDef;
            }
        }
        return null;
    }

    /**
     * 根据 key 获取 BPM 操作类型
     *
     * @param key 按钮标识
     * @return BPM 操作类型，如果不存在返回 null
     */
    public static BpmAction getBpmAction(String key) {
        BpmActionDef actionDef = fromKey(key);
        return actionDef != null ? actionDef.getBpmAction() : null;
    }

    /**
     * 根据 key 获取业务状态
     *
     * @param key 按钮标识
     * @return 业务状态值，如果不存在返回 null
     */
    public static String getBizStatus(String key) {
        BpmActionDef actionDef = fromKey(key);
        return actionDef != null ? actionDef.getBizStatus() : null;
    }

}
