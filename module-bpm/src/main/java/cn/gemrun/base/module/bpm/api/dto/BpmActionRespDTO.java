package cn.gemrun.base.module.bpm.api.dto;

import lombok.Data;

/**
 * BPM 按钮定义 Response DTO
 *
 * 用于前端获取按钮配置列表
 *
 * @author Gemini
 */
@Data
public class BpmActionRespDTO {

    /**
     * 按钮标识（对应 DSL 中的 actions 值）
     */
    private String key;

    /**
     * 显示中文名称
     */
    private String label;

    /**
     * 默认业务状态值
     */
    private String bizStatus;

    /**
     * 业务状态中文名称
     */
    private String bizStatusLabel;

    /**
     * BPM 操作类型
     */
    private String bpmAction;

    /**
     * 当前任务ID（getAvailableActions 时填充，前端执行操作时需要）
     */
    private String taskId;

}
