package cn.gemrun.base.module.bpm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BPM 操作类型枚举
 *
 * 定义按钮对应的后端操作类型，用于任务完成时的业务处理
 *
 * @author Gemini
 */
@Getter
@AllArgsConstructor
public enum BpmAction {

    /**
     * 通过/完成
     */
    COMPLETE("complete"),

    /**
     * 拒绝
     */
    REJECT("reject"),

    /**
     * 退回
     */
    BACK("back"),

    /**
     * 转办
     */
    TRANSFER("transfer"),

    /**
     * 委派
     */
    DELEGATE("delegate"),

    /**
     * 取消/撤回
     */
    CANCEL("cancel"),

    /**
     * 挂起
     */
    SUSPEND("suspend"),

    /**
     * 恢复
     */
    RESUME("resume");

    /**
     * 操作标识
     */
    private final String value;

    /**
     * 根据 value 获取枚举
     *
     * @param value 操作标识
     * @return 枚举对象，如果不存在返回 null
     */
    public static BpmAction fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (BpmAction action : values()) {
            if (action.value.equals(value)) {
                return action;
            }
        }
        return null;
    }

}
